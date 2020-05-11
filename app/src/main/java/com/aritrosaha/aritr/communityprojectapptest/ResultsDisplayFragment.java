package com.aritrosaha.aritr.communityprojectapptest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Objects;

public class ResultsDisplayFragment extends Fragment {

    // get index of target in, useful in getting most probable class
    private static int find(float[] a, float target)
    {
        for (int i = 0; i < a.length; i++)
            if (a[i] == target)
                return i;

        return -1;
    }

    // get maximum amount in array, useful in getting most probable class
    private static float arrayMax(float[] arr) {
        double max = Double.NEGATIVE_INFINITY;

        for(double cur: arr)
            max = Math.max(max, cur);

        return (float) max;
    }

    private static float[] probabilities = {};
    private static String[] labels = {};

    private OnFragmentInteractionListener mListener;

    public ResultsDisplayFragment() {
        // Required empty public constructor
    }

    // allow new instance to be only made with arguments
    static ResultsDisplayFragment newInstance(String[] _labels, float[] _probabilities) {
        ResultsDisplayFragment fragment = new ResultsDisplayFragment();
        Bundle args = new Bundle();
        args.putFloatArray("probabilities", _probabilities);
        args.putStringArray("labels", _labels);
        fragment.setArguments(args);
        return fragment;
    }

    // get arguments and set them into class variables
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // set variables to bundle args
            labels = getArguments().getStringArray("labels");
            probabilities = getArguments().getFloatArray("probabilities");
        } else {
            // if label and probabilities arguments are not passed, throw an illegal argument exception as there is no point of the fragment then
            throw new IllegalArgumentException();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_results_display, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // get max probability and its label
        float maxProbability = arrayMax(probabilities);
        String labelForProbability = labels[find(probabilities, maxProbability)];

        TextView scannerTextView = view.findViewById(R.id.scannerTextView);
        TextView topTextView = view.findViewById(R.id.topText);
        ImageView imageRepresentation = view.findViewById(R.id.imageScannerView);

        //region old
        if (maxProbability <= 0.65){
            // set text
            topTextView.setText("I'm not sure");
            scannerTextView.setText("what this is :(");

            // set img to question mark
            Glide.with(Objects.requireNonNull(getContext()))
                    .load(R.drawable.ic_question_mark)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.loading_placeholder)
                            .fitCenter())
                    .thumbnail(Glide.with(getContext()).load(R.drawable.loading_placeholder))
                    .into(imageRepresentation);

        } else {
            // tell user what object has been decided
            scannerTextView.setText("the " + labelForProbability + " bin!");

            // set image to a bin, and change color based on result
            Log.d("MLLabel", labelForProbability);
            // set colors
            if (labelForProbability.equals("Recycling")) {
                imageRepresentation.setColorFilter(getResources().getColor(R.color.recycling));
            } else if (labelForProbability.equals("Compost")) {
                imageRepresentation.setColorFilter(getResources().getColor(R.color.compost));
            } else {
                imageRepresentation.setColorFilter(getResources().getColor(R.color.garbage));
            }
        }


        //endregion
        // retry button, if clicked then will redirect back to scanner
        Button retryButton = view.findViewById(R.id.scanAgainButton);
        retryButton.setOnClickListener(view1 -> {
            ScannerFragment scannerFragment = new ScannerFragment();
            final FragmentTransaction transaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, scannerFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

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
}
