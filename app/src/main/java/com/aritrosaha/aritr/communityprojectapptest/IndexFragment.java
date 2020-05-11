package com.aritrosaha.aritr.communityprojectapptest;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Keep
public class IndexFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private String TAG = "IndexFragment";

    private List<Item> itemList;
    private List<Item> originalItemList;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;

    ProgressDialog mProgressDialog;

    private TextInputEditText searchInputField;

    @Keep
    public IndexFragment() {
        // Required empty public constructor
    }

    @Override
    @Keep
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    @Keep
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_index, container, false);
    }

    @Override
    @Keep
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //region My Own Code

        // initialize variables for further use
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        itemList = new ArrayList<>();
        originalItemList = new ArrayList<>();
        adapter = new RecyclerViewAdapter(getContext(), itemList);
        searchInputField = view.findViewById(R.id.searchInputEditText);

        // remove keyboard after pressing enter on input fields
        searchInputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(final Editable s) {
                itemList.clear();
                // TODO: add code
                if (!s.toString().equals(""))
                {
                    // extremely important as removes previous list entries and gives next code a blank canvas to paint on
                    itemList.clear();
                    for (Item item : originalItemList)
                    {
                        if (item.getName().toLowerCase().contains(s.toString().toLowerCase())) {
                            itemList.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else
                {
                    // extremely important as removes previous list entries and gives next code a blank canvas to paint on
                    itemList.clear();
                    itemList.addAll(originalItemList);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        // initialize recyclerView
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.swapAdapter(adapter, true);

        // create progress dialog while loading database info
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setTitle("Loading...");
        mProgressDialog.setMessage("Loading Items...");
        mProgressDialog.setIndeterminate(true);
        try{
            mProgressDialog.show();
        } catch (Exception ignored) { }

        itemList.clear();

        // get all data into original item list
        db.collection("more_info")
                .get()
                .addOnCompleteListener(task -> {
                    // extremely important as removes previous list entries and gives next code a blank canvas to paint on
                    itemList.clear();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Item item = new Item(
                                    (String) document.get("img"),
                                    (String) document.get("name"),
                                    (String) document.get("type"),
                                    (String)document.get("info"));
                            // Item item = document.toObject(Item.class);
                            // assert item != null;
                            if (item.getName() == null){
                                continue;
                            }
                            originalItemList.add(item);
                            //Log.d("MainActivityInfRetrieve", item.getName());
                        }

                        // make sure that items are in alphabetical order
                        Collections.sort(originalItemList, (o1, o2) -> o1.getName().compareTo(o2.getName()));

                        itemList.addAll(originalItemList);

                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                    dismiss(mProgressDialog);
                });

        itemList.addAll(originalItemList);
        originalItemList.addAll(itemList);
        Log.d("itemList", Integer.toString(itemList.size()));
        adapter.notifyDataSetChanged();

        //endregion
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    @Keep
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    @Keep
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mProgressDialog != null) mProgressDialog.dismiss();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mProgressDialog != null) mProgressDialog.dismiss();
        mProgressDialog = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    // i have to add this because life hates me / if the fragment closes
    // before the dismiss, it crashes
    @Keep
    public void dismiss(ProgressDialog progressDialog) {
        // can sometimes error out into making the activity null, somehow
        if (getActivity() == null) return;

        Window window = getActivity().getWindow();
        if (window == null) {
            return;
        }
        View decor = window.getDecorView();
        if (decor.getParent() != null) {
            progressDialog.dismiss();
        }
    }

}
