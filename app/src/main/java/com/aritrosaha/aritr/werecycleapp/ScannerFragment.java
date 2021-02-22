package com.aritrosaha.aritr.werecycleapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pl.droidsonroids.gif.GifImageView;


public class ScannerFragment extends Fragment {
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.INTERNET"};

    private PreviewView previewView;
    private ImageButton imgButton;

    private final String[] labels = {"Recycling", "Garbage", "Compost"};

    private Interpreter tfliteInterpreter;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private View scannerFragmentView;

    public ScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
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

        previewView = view.findViewById(R.id.view_finder);
        imgButton = view.findViewById(R.id.imgCapture);

        // so i can ref in perms manager
        scannerFragmentView = view;

        // Disable the button so the user can't use it while everything's being setup
        imgButton.setAlpha(.5f);
        imgButton.setClickable(false);

        // Download model if permissions granted
        if(allPermissionsGranted()){
            Log.d("ScannerFragment", "Permissions acquired, downloading model...");
            downloadModel();
        } else {
            // Request permissions
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // Setup preview settings
        Preview preview = new Preview.Builder()
                .build();

        // Setup camera side (front or back) settings
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Set surface provider
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Set img cap settings
        ImageCapture imageCapture =
                new ImageCapture.Builder()
                        .setTargetRotation(scannerFragmentView.getDisplay().getRotation())
                        .build();

        imgButton.setOnClickListener(view -> imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                // Freeze frame by unbinding the preview
                new Handler(Looper.getMainLooper()).post(() -> cameraProvider.unbind(preview));

                // Make sure that we haven't already switched to another fragment
                Fragment currentFragment = Objects.requireNonNull(requireActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.container));

                if (!(currentFragment instanceof ScannerFragment)){
                    return;
                }

                // Show the loading animation while we run the prediction
                GifImageView loadingPlace = scannerFragmentView.findViewById(R.id.loadingPlaceholder);
                requireActivity().runOnUiThread(() -> loadingPlace.setVisibility(View.VISIBLE));

                // Convert the imageProxy into a bitmap
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                // Resize the bitmap to fit the model's constraints
                bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

                // Convert the bitmap into a byte buffer so the model can use it
                ByteBuffer input = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder());
                for (int y = 0; y < 224; y++) {
                    for (int x = 0; x < 224; x++) {
                        int px = bitmap.getPixel(x, y);

                        // Get channel values from pixel value
                        int r = Color.red(px);
                        int g = Color.green(px);
                        int b = Color.blue(px);

                        // Normalize channel values to [-1.0, 1.0]. This requirement depends
                        // on the model. For example, some models might require values to be
                        // normalized to the range [0.0, 1.0] instead.
                        float rf = (r - 127) / 255.0f;
                        float gf = (g - 127) / 255.0f;
                        float bf = (b - 127) / 255.0f;

                        input.putFloat(rf);
                        input.putFloat(gf);
                        input.putFloat(bf);
                    }
                }

                // Prepare another buffer for the output and run the model
                int bufferSize = 1000 * Float.SIZE / Byte.SIZE;
                ByteBuffer modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
                tfliteInterpreter.run(input, modelOutput);

                // Read the buffer from the start, convert it to a float buffer, and fetch the actual values from the buffer
                modelOutput.rewind();
                float[] probabilities = new float[modelOutput.asFloatBuffer().limit()];
                modelOutput.asFloatBuffer().get(probabilities);

                // Send the probabilities to the results fragment
                ResultsDisplayFragment resultsDisplayFragment = ResultsDisplayFragment.newInstance(labels, probabilities);
                final FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container, resultsDisplayFragment);
                transaction.addToBackStack(null);

                // have to use this because of crash that happens with commit,
                // check this link for more info: https://medium.com/@elye.project/handling-illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-d4ee8b630066
                transaction.commitAllowingStateLoss();

                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                // Let the user know
                Log.d("ScannerFragment", Objects.requireNonNull(exception.getMessage()));
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), "Error! Was not able to take a photo.", Toast.LENGTH_SHORT).show());
            }
        }));

        // Remove all other bindings before adding new ones
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);

        imgButton.setAlpha(1f);
        imgButton.setClickable(true);

        // remove loading placeholder
        GifImageView loadingPlace = scannerFragmentView.findViewById(R.id.loadingPlaceholder);
        loadingPlace.setVisibility(View.GONE);
    }

    private void downloadModel(){
        //region Download Model
        FirebaseCustomRemoteModel remoteModel =
                new FirebaseCustomRemoteModel.Builder("Bin-Differentiator-v2").build();

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnSuccessListener(task -> FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
                        .addOnCompleteListener(latestModelTask -> {
                            File modelFile = latestModelTask.getResult();
                            if (modelFile != null) {
                                tfliteInterpreter = new Interpreter(modelFile);
                                Log.d("ScannerFragment", "Created interpreter!");

                                // Start the binding process for the camera
                                cameraProviderFuture.addListener(() -> {
                                    try {
                                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                        bindPreview(cameraProvider);
                                    } catch (ExecutionException | InterruptedException e) {
                                        // No errors need to be handled for this Future.
                                        // This should never be reached.
                                    }
                                }, ContextCompat.getMainExecutor(requireContext()));
                            }
                        })).addOnFailureListener(task -> {
                    Log.d("ScannerFragment", Objects.requireNonNull(task.getMessage()));
                    task.printStackTrace();
        });

        //endregion
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                // restart fragment
                Log.d(getTag(), "Permissions provided!");
                downloadModel();

            } else{
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity(), "Please provide the necessary permissions for this function to work.", Toast.LENGTH_LONG).show());
                requireActivity()
                        .finish();
                Log.d(getTag(), "Permissions not provided.");
            }
        }
    }

    // check if all required permissions have been accquired
    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED){
                Log.d("ScannerFragment", permission);
                return false;
            }
        }
        return true;
    }
}
