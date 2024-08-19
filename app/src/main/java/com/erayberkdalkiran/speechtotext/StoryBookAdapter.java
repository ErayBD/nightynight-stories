package com.erayberkdalkiran.speechtotext;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.erayberkdalkiran.speechtotext.databinding.RecyclerviewStorybookBinding;

import java.util.ArrayList;

public class StoryBookAdapter extends RecyclerView.Adapter<StoryBookAdapter.StoryBookHolder> {

    private ArrayList<StoryBookClass> storyBookArrayList;
    private OnMicClickListener micClickListener;

    public StoryBookAdapter(ArrayList<StoryBookClass> storyBookArrayList, OnMicClickListener micClickListener) {
        this.storyBookArrayList = storyBookArrayList;
        this.micClickListener = micClickListener;
    }

    // gorunumleri tutan yardimci bir sinif
    public class StoryBookHolder extends RecyclerView.ViewHolder {
        RecyclerviewStorybookBinding binding;
        ImageView imageView;
        VideoView videoView;

        public StoryBookHolder(@NonNull RecyclerviewStorybookBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            imageView = binding.storybookImg;
            videoView = binding.storybookVideo;
        }
    }

    @NonNull
    @Override
    public StoryBookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerviewStorybookBinding binding = RecyclerviewStorybookBinding.inflate(inflater, parent, false);
        return new StoryBookHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryBookHolder holder, int position) {
        StoryBookClass storyBook = storyBookArrayList.get(position);
        holder.binding.storybookChapterName.setText(storyBook.chapterName);
        holder.imageView.setImageResource(storyBook.image);
        holder.videoView.setVideoURI(Uri.parse("android.resource://" + holder.itemView.getContext().getPackageName() + "/" + storyBook.video));
        holder.binding.storybookPromptText.setText(storyBook.textToRead);

        holder.binding.storybookPromptMic.setOnClickListener(v -> {
            if (micClickListener != null) {
                micClickListener.onMicClick(position, storyBook);
            }
        });
    }

    @Override
    public int getItemCount() {
        return storyBookArrayList.size();
    }

    public interface OnMicClickListener {
        void onMicClick(int position, StoryBookClass storyBook);
    }
}
