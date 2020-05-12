package com.aritrosaha.aritr.werecycleapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pl.droidsonroids.gif.GifImageView;


public class ScannerFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    private Executor executor = Executors.newSingleThreadExecutor();

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.INTERNET"};

    private TextureView textureView;
    private ImageButton imgButton;

    // TODO: Update this to work with real model
    private String[] labels = {"Recycling", "Garbage", "Compost"};

    private FirebaseModelInputOutputOptions inputOutputOptions;
    private FirebaseModelInterpreter interpreter;

    private View scannerFragmentView;

    public ScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textureView = view.findViewById(R.id.view_finder);
        imgButton = view.findViewById(R.id.imgCapture);

        // so i can ref in perms manager
        scannerFragmentView = view;

        // disable it so user cannot use model before it loads
        imgButton.setAlpha(.5f);
        imgButton.setClickable(false);


        // do stuff if all permissions have been granted
        if(allPermissionsGranted()){
            Log.d("ScannerFragment", "eee?");
            downloadModel(view);
        } else {
            // request permissions
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void startCamera(View view) {
        // unbind all other instances
        CameraX.unbindAll();

        // get aspect ratio of screen
        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        // build preview class to show user the camera feed
        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetResolution(screen)
                .setLensFacing(CameraX.LensFacing.BACK)
                .build();
        Preview preview = new Preview(pConfig);

        //to update the surface texture we  have to destroy it first then re-add it
        preview.setOnPreviewOutputUpdateListener(
                output -> {
                    ViewGroup parent = (ViewGroup) textureView.getParent();
                    parent.removeView(textureView);
                    parent.addView(textureView, 0);

                    textureView.setSurfaceTexture(output.getSurfaceTexture());
                    updateTransform();
                });

        // setup image cap
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(
                        Objects.requireNonNull(getActivity())
                                .getWindowManager()
                                .getDefaultDisplay()
                                .getRotation())
                .build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
        imgCap.setFlashMode(FlashMode.AUTO);

        // when imgcap button is clicked, get image, predict what it is, then send the info to ResultsDisplayFragment
        // to show to the user
        view.findViewById(R.id.imgCapture).setOnClickListener(v -> imgCap.takePicture(executor, new ImageCapture.OnImageCapturedListener() {
            @Override
            public void onCaptureSuccess(ImageProxy imageProxy, int rotationDegrees) {
                Fragment currentFragment = Objects.requireNonNull(Objects.requireNonNull(getActivity())
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.container));


                // dont put resources into prediction and decoding if used already switched
                if (!(currentFragment instanceof ScannerFragment)){
                    return;
                }

                // freeze frame in application thread
                new Handler(Looper.getMainLooper()).post(() -> CameraX.unbind(preview));


                // add loading placeholder
                GifImageView loadingPlace = view.findViewById(R.id.loadingPlaceholder);
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> loadingPlace.setVisibility(View.VISIBLE));

                //region Converting ImageProxy to a Bitmap object the model can use
                Image img = imageProxy.getImage();

                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                // i have to wrap it in a try catch statement of all things because if i only use
                // Objects.requireNonNull, it still glitches out which i don't want
                try {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                } catch (NullPointerException e) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), "Error! Was not able to take a photo.", Toast.LENGTH_SHORT).show());
                    return;
                }

                int batchNum = 0;
                float[][][][] input = new float[1][224][224][3];
                for (int x = 0; x < 224; x++) {
                    for (int y = 0; y < 224; y++) {
                        int pixel = bitmap.getPixel(x, y);
                        // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                        // model. For example, some models might require values to be normalized
                        // to the range [0.0, 1.0] instead.
                        input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
                        input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
                        input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;
                    }
                }
                //endregion

                //region Setting the inputs that will be given to the model
                FirebaseModelInputs inputs = null;
                try {
                    inputs = new FirebaseModelInputs.Builder()
                            .add(input)  // add() as many input arrays as your model requires
                            .build();
                } catch (FirebaseMLException e) {
                    e.printStackTrace();
                }
                //endregion

                currentFragment = Objects.requireNonNull(Objects.requireNonNull(getActivity())
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.container));


                // check 2, because i believe this function is async
                if (!(currentFragment instanceof ScannerFragment)){
                    return;
                }

                if (inputs == null) {
                    Log.e("ScannerFragment", "inputs are null");
                }

                if (inputOutputOptions == null) {
                    Log.e("ScannerFragment", "inputOutputOptions are null");
                }

                if (interpreter == null) {
                    Log.e("ScannerFragment", "interpreter is null");
                }

                // TODO: Set it to run with the model that actually guesses the bin.
                //region Predict what the image is
                interpreter.run(Objects.requireNonNull(inputs), inputOutputOptions)
                        .addOnSuccessListener(
                                result -> {
                                    float[][] output = result.getOutput(0);
                                    float[] probabilities = output[0];

                                    for (int i = 0; i < probabilities.length; i++) {
                                        String label = labels[i];
                                        Log.i("MLKit", String.format("%s: %1.4f", label, probabilities[i]));
                                    }

                                    // start results display fragment to show it all
                                    ResultsDisplayFragment resultsDisplayFragment = ResultsDisplayFragment.newInstance(labels, probabilities);
                                    final FragmentTransaction transaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, resultsDisplayFragment);
                                    transaction.addToBackStack(null);

                                    // have to use this because of crash that happens with commit,
                                    // check this link for more info: https://medium.com/@elye.project/handling-illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-d4ee8b630066
                                    transaction.commitAllowingStateLoss();
                                })
                        .addOnFailureListener(
                                e -> Log.e("MLKit", String.format("Error! Could not run interpeter. %s", e)));
                //endregion

                imageProxy.close();
            }

            @Override
            public void onError(@NonNull ImageCapture.ImageCaptureError captureError, @NonNull String message, @Nullable Throwable cause) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), "Error! Was not able to take a photo.", Toast.LENGTH_SHORT).show());
            }

        }));


        // bind to camerax lifecycle so it runs
        CameraX.bindToLifecycle(this, preview, imgCap);

        // enable the button (moved from onViewCreated to account for when user
        // clicks button but camera has not been loaded
        imgButton.setAlpha(1f);
        imgButton.setClickable(true);

        // remove loading placeholder
        GifImageView loadingPlace = view.findViewById(R.id.loadingPlaceholder);
        loadingPlace.setVisibility(View.GONE);
    }

    private void downloadModel(View view){
        //region Download Model
        FirebaseCustomRemoteModel  remoteModel =
                new FirebaseCustomRemoteModel.Builder("Bin-Differentiator-Teachable-Machine-Version").build();

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .build();

        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnSuccessListener(task -> {
                    // enabling button has been moved to startCamera function
                    try {
                        Log.d("ScannerFragment", "downloading this bitch ass machine learning model");
                        inputOutputOptions =
                                new FirebaseModelInputOutputOptions.Builder()
                                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 3})
                                        .build();

                        FirebaseModelInterpreterOptions options =
                                new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                        interpreter = FirebaseModelInterpreter.getInstance(options);

                        if (interpreter == null) {
                            // RESTART FRAGMENT
                            assert getFragmentManager() != null;
                            FragmentTransaction tr = getFragmentManager().beginTransaction();
                            tr.replace(R.id.container, new ScannerFragment());
                            tr.commit();
                            Log.e("ScannerFragment", "interpreter null");
                        }


                        // moved from start of if statement
                        // so picture cannot be taken even if model has
                        // not been loaded into memory
                        startCamera(view); //start camera if permission has been granted by user
                    } catch (FirebaseMLException e) {
                        e.printStackTrace();
                    }
                });

        //endregion
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                // restart fragment
                Log.d(getTag(), "your mom 69420");
                downloadModel(scannerFragmentView);

            } else{
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity(), "Please provide the necessary permissions for this function to work.", Toast.LENGTH_LONG).show());
                Objects.requireNonNull(getActivity())
                        .finish();
                Log.d(getTag(), "not your mom 69420");
            }
        }
    }

    // check if all required permissions have been accquired
    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), permission) != PackageManager.PERMISSION_GRANTED){
                Log.d("Did not work!", permission);
                return false;
            }
        }
        return true;
    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }
}
