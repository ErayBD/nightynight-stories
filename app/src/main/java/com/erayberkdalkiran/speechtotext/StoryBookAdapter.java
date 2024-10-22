package com.erayberkdalkiran.speechtotext;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
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
        holder.binding.storybookChapterName.setText(storyBook.title);
        holder.imageView.setImageResource(storyBook.image);
        holder.videoView.setVideoURI(Uri.parse("android.resource://" + holder.itemView.getContext().getPackageName() + "/" + storyBook.video));
        holder.binding.storybookPromptText.setText(storyBook.textToRead);

        if (storyBook.isActive) {
            // Bölüm aktifse, normal görünüm ve tıklanabilirlik ayarları
            holder.binding.storybookPromptMic.setImageResource(R.drawable.mic_off);
            holder.binding.storybookPromptMic.setClickable(true);
            holder.binding.storybookPromptMic.setFocusable(true);
            holder.binding.storybookChapterName.setAlpha(1.0f);
            holder.binding.storybookImg.setAlpha(1.0f);
            holder.binding.storybookPromptText.setAlpha(1.0f);
            holder.binding.storybookPromptMic.setAlpha(1.0f);

            // Bulanıklığı kaldırmak için
            holder.binding.storybookPromptText.getPaint().setMaskFilter(null);
        } else {
            // Bölüm inaktifse, gri tonlar ve tıklanamaz halde
            holder.binding.storybookPromptMic.setImageResource(R.drawable.mic_locked);
            holder.binding.storybookPromptMic.setClickable(false);
            holder.binding.storybookPromptMic.setFocusable(false);
            holder.binding.storybookChapterName.setAlpha(0.5f);
            holder.binding.storybookImg.setAlpha(0.5f);
            holder.binding.storybookPromptText.setAlpha(0.5f);
            holder.binding.storybookPromptMic.setAlpha(0.5f);

            // Metni bulanıklaştırmak için
            holder.binding.storybookPromptText.getPaint().setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        }

        holder.binding.storybookPromptMic.setOnClickListener(v -> {
            if (micClickListener != null && storyBook.isActive) {
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
