package com.aritrosaha.aritr.communityprojectapptest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ProductViewHolder> {

    private Context mContext;
    private List<Item> itemList;
    private final String TAG = "RecyclerViewAdapter";

    RecyclerViewAdapter(Context mContext, List<Item> itemList) {
        this.itemList = itemList;
        this.mContext = mContext;
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mContext = parent.getContext();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.recycler_view_item, null);

        return new ProductViewHolder(view);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        // get item and set textViews on card
        Item item = itemList.get(position);

        holder.textView_itemName.setText(item.getName());
        holder.textView_itemDescription.setText(item.getInfo());

        // switch case to set colors
        switch (item.getType()) {
            // garbage
            case "g":
                holder.materialCardView.setCardBackgroundColor(Color.parseColor("#546e7a"));
                holder.textView_itemName.setTextColor(Color.WHITE);
                holder.textView_itemDescription.setTextColor(Color.WHITE);
                break;

            // recycling
            case "r":
                holder.materialCardView.setCardBackgroundColor(Color.parseColor("#03a9f4"));
                break;

            // compost
            case "c":
                holder.materialCardView.setCardBackgroundColor(Color.parseColor("#4caf50"));
                break;

            // yard waste
            case "yw":
                holder.materialCardView.setCardBackgroundColor(Color.parseColor("#8d6e63"));
                holder.textView_itemName.setTextColor(Color.WHITE);
                holder.textView_itemDescription.setTextColor(Color.WHITE);
                break;
        }

        // use glide to load image into imageView
        Glide.with(mContext)
                .load(item.getImg())
                .apply(new RequestOptions()
                        .placeholder(R.drawable.loading_placeholder)
                        .fitCenter())
                .thumbnail(Glide.with(mContext).load(R.drawable.loading_placeholder))
                .into(holder.itemImage);
    }

    @Override
    public void onViewRecycled(@NonNull ProductViewHolder holder) {
        super.onViewRecycled(holder);

        holder.itemImage.setImageBitmap(null);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView textView_itemName, textView_itemDescription;
        MaterialCardView materialCardView;

        ProductViewHolder(View itemView) {
            super(itemView);

            // get views for use later
            itemImage = itemView.findViewById(R.id.itemImage);

            textView_itemName = itemView.findViewById(R.id.textView_itemName);
            textView_itemDescription = itemView.findViewById(R.id.textView_itemInfo);
            materialCardView = itemView.findViewById(R.id.materialCard);

            // add for anything, possibly a moreinfo fragment
            itemView.setOnClickListener(view -> {

            });
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
