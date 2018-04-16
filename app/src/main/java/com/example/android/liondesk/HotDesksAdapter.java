package com.example.android.liondesk;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by Caroline on 22/03/2018.
 * <p>
 * Adapted from AlbumsAdapter.java from
 * https://www.androidhive.info/2016/05/android-working-with-card-view-and-recycler-view/
 */

public class HotDesksAdapter extends RecyclerView.Adapter<HotDesksAdapter.MyViewHolder> {

    private static final String TAG = HotDesksAdapter.class.getSimpleName();
    private Context mContext;
    private List<HotDesk> hotdeskList;
    // solution to select just 1 card from click adapted from
    // https://stackoverflow.com/questions/28972049/single-selection-in-recyclerview#35060634
    private int lastCheckedPosition = -1;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mDeskID, mFloor, mStatus;
        public ImageView mDeskThumbnail, mFloorIcon;
        public CardView mCard;
        //  int backgroundColor = YELLOW;

        public MyViewHolder(View view) {
            super(view);

            mDeskID = view.findViewById(R.id.textView_deskID);
            mFloor = view.findViewById(R.id.textView_floor);
            mStatus = view.findViewById(R.id.textView_status);

            mDeskThumbnail = view.findViewById(R.id.imageView_desk);
            mFloorIcon = view.findViewById(R.id.floor_icon);
            mCard = view.findViewById(R.id.card_view);

            // Define click listener for the ViewHolder's View.
            // code copied from https://github.com/googlesamples/android-RecyclerView/
            mCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastCheckedPosition = getAdapterPosition();
                    notifyDataSetChanged();
                    // send the text to the listener, i.e Activity.
                    mListener.onItemClicked(mDeskID.getText().toString());
                }
            });

            this.mDeskID.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        // do I need these getters?
        public TextView getDeskID() {
            return mDeskID;
        }

        public TextView getFloor() {
            return mFloor;
        }

        public TextView getStatus() {
            return mStatus;
        }
    }


    public HotDesksAdapter(Context mContext, List<HotDesk> hotdeskList) {
        this.mContext = mContext;
        this.hotdeskList = hotdeskList;
    }

    // code from https://stackoverflow.com/questions/41892648/get-value-of-textview-on-item-click-inside-a-recycler-view
    // to get deskID value from the adapter to the Activity

    // Define listener member variable
    private static OnRecyclerViewItemClickListener mListener;

    // Define the listener interface
    public interface OnRecyclerViewItemClickListener {
        void onItemClicked(String text);
    }

    // Define the method that allows the parent activity or fragment to define the listener.
    public void setOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_hotdesk, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        HotDesk hotdesk = hotdeskList.get(position);
        holder.mDeskID.setText(hotdesk.getID());
        holder.mFloor.setText("# " + hotdesk.getFloorNumber());
        holder.mStatus.setText(hotdesk.getStatus());

        holder.mCard.setSelected(position == lastCheckedPosition);

        // loading the hotdesk picture using Glide library
        Glide.with(mContext).load(hotdesk.getThumbnail()).into(holder.mDeskThumbnail);

    }

    //Not sure the use of that, can I delete it?
    @Override
    public int getItemCount() {
        return hotdeskList.size();
    }

}
