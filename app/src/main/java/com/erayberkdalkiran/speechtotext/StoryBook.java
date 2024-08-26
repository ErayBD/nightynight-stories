package com.erayberkdalkiran.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.text.LineBreaker;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.erayberkdalkiran.speechtotext.databinding.ActivityGooglecloudBinding;
import com.erayberkdalkiran.speechtotext.databinding.ActivityStorybookBinding;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoryBook extends AppCompatActivity {

    private ActivityStorybookBinding binding;
    private ArrayList<StoryBookClass> storyBookList;
    private StoryBookAdapter adapter;
    private SpeechClient speechClient;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private StringBuilder fullTranscript = new StringBuilder();

    private static final int SAMPLE_RATE = 16000;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStorybookBinding.inflate(getLayoutInflater());
        View activityView = binding.getRoot();
        setContentView(activityView);

        // gerekli string ve görsellerin alınması
        List<String> textToReadList = new ArrayList<>();
        List<Integer> imageList = new ArrayList<>();
        List<Integer> videoList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            // String kaynaklarını ekle
            int stringId = getResources().getIdentifier("storypart_" + i, "string", getPackageName());
            textToReadList.add(getString(stringId));

            // Drawable kaynaklarını ekle
            int drawableId = getResources().getIdentifier("bluebunny_" + i, "drawable", getPackageName());
            imageList.add(drawableId);

            // Video kaynaklarını ekle
            int videoId = getResources().getIdentifier("video_" + i, "raw", getPackageName());
            videoList.add(videoId);
        }

        // StoryBookClass nesnelerinin oluşturulması ve listeye eklenmesi
        storyBookList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            storyBookList.add(new StoryBookClass(
                    "Bölüm: " + (i + 1),
                    imageList.get(i),
                    videoList.get(i),
                    textToReadList.get(i)
            ));
            // İlk hikayeyi her zaman aktif yap
            if (i == 0) {
                storyBookList.get(0).isActive = true;
            }
        }

        // Adapter ve RecyclerView yapılandırma
        adapter = new StoryBookAdapter(storyBookList, this::startListening);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        else {
            initSpeechClient();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSpeechClient();
            } else {
                Toast.makeText(this, "Audio permission is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initSpeechClient() {
        try {
            InputStream credentialsStream = getResources().openRawResource(R.raw.speech2text);
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
            speechClient = SpeechClient.create(settings);
            Log.d("SpeechClient", "SpeechClient created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SpeechClient", "Failed to create SpeechClient: " + e.getMessage());
            Toast.makeText(this, "SpeechClient kurulumu başarısız!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startListening(int position, StoryBookClass storyBook) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        fullTranscript.setLength(0);  // fullTranscript'i sıfırla

        // dinleme aciksa durdur
        if (isRecording) {
            stopListening();
            updateMicButtonVisual(position, R.drawable.mic_off);
            return;
        }
        // dinleme kapaliysa baslat
        else {
            isRecording = true;
            resetTextHighlighting(position); // dinleme baslatildiginda metni sifirla
            updateMicButtonVisual(position, R.drawable.mic_on);
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        Log.d("AudioRecord", "Buffer size: " + bufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed");
        } else {
            Log.d("AudioRecord", "AudioRecord initialized successfully");
        }

        audioRecord.startRecording();

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            String expectedText = storyBook.textToRead;
            Log.d("wordsInExpectedText", "Selected Text: " + expectedText); // Log expected text

            try {
                RecognitionConfig recognitionConfig =
                        RecognitionConfig.newBuilder()
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setLanguageCode("tr-TR")
                                .setSampleRateHertz(SAMPLE_RATE)
                                .build();

                StreamingRecognitionConfig streamingRecognitionConfig =
                        StreamingRecognitionConfig.newBuilder()
                                .setConfig(recognitionConfig)
                                .setInterimResults(true)
                                .build();

                ApiStreamObserver<StreamingRecognizeResponse> responseObserver = new ApiStreamObserver<StreamingRecognizeResponse>() {
                    @Override
                    public void onNext(StreamingRecognizeResponse response) {
                        List<StreamingRecognitionResult> results = response.getResultsList();
                        for (StreamingRecognitionResult result : results) {
                            if (result.getAlternativesCount() > 0) {
                                String transcript = result.getAlternatives(0).getTranscript();
                                runOnUiThread(() -> {
                                    Log.d("Real-Time", "Transcript: " + transcript);
                                    // Her geçici sonucu işliyoruz
                                    processTranscript(transcript, position, result.getIsFinal());
                                });
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e("Streaming", "Error: " + t.getMessage());
                        runOnUiThread(() -> Toast.makeText(StoryBook.this,
                                "Error during speech recognition: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCompleted() {
                        Log.d("Streaming", "Recognition completed.");
                        runOnUiThread(() -> stopListening());
                    }
                };

                BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
                        speechClient.streamingRecognizeCallable();

                ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
                        callable.bidiStreamingCall(responseObserver);

                requestObserver.onNext(
                        StreamingRecognizeRequest.newBuilder()
                                .setStreamingConfig(streamingRecognitionConfig)
                                .build());

                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);

                    if (read > 0) {
                        ByteString audioBytes = ByteString.copyFrom(buffer, 0, read);
                        requestObserver.onNext(
                                StreamingRecognizeRequest.newBuilder()
                                        .setAudioContent(audioBytes)
                                        .build());
                    }
                }
                requestObserver.onCompleted();

            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e("Streaming", "Error: " + e.toString());
                runOnUiThread(() -> Toast.makeText(StoryBook.this,
                        "Error during speech recognition: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateMicButtonVisual(int position, int drawableResId) {
        runOnUiThread(() -> {
            RecyclerView.ViewHolder viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                ImageView micImageView = viewHolder.itemView.findViewById(R.id.storybook_promptMic); // Buton yerine ImageView bul
                micImageView.setImageResource(drawableResId); // Görseli güncelle
            }
        });
    }

    private void stopListening() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void resetTextHighlighting(int position) {
        StoryBookClass storyBook = storyBookList.get(position);

        // Seçilen StoryBookClass için match durumlarını sıfırla
        Collections.fill(storyBook.wordMatchStatus, Boolean.FALSE);
        storyBook.permanentlyMatchedIndices.clear();

        // Metni eski haline getir
        String originalText = storyBook.textToRead;
        StringBuilder normalText = new StringBuilder();

        String[] words = originalText.split("\\s+");
        for (String word : words) {
            normalText.append(word).append(" ");
        }

        // Metni güncelle
        runOnUiThread(() -> {
            RecyclerView.ViewHolder viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                TextView promptTextView = viewHolder.itemView.findViewById(R.id.storybook_promptText);
                promptTextView.setText(normalText.toString().trim());
                promptTextView.setLineSpacing(0, 1.5f);
            } else {
                // Eğer viewHolder null ise, adaptöre pozisyonun değiştiğini bildir
                adapter.notifyItemChanged(position);
            }
        });
    }


    private void processTranscript(String transcript, int position, boolean isFinal) {
        String expectedText = storyBookList.get(position).textToRead;
        StoryBookClass currentStoryBook = storyBookList.get(position);

        if (isFinal) {
            fullTranscript.append(transcript).append(" ");
        }

        String workingText = isFinal ? fullTranscript.toString().trim() : transcript.trim();
        String cleanedExpectedText = expectedText.replaceAll("[^a-zA-ZğüşıöçĞÜŞİÖÇ\\s]", "").toLowerCase();
        String cleanedWorkingText = workingText.replaceAll("[^a-zA-ZğüşıöçĞÜŞİÖÇ\\s]", "").toLowerCase();

        String[] wordsInExpectedText = cleanedExpectedText.split("\\s+");
        String[] wordsInWorkingText = cleanedWorkingText.split("\\s+");

        StringBuilder highlightedText = new StringBuilder();
        int matchCount = 0;

        String[] originalWords = expectedText.split("\\s+");
        int originalWordIndex = 0;
        int workingWordIndex = 0;

        for (String expectedWord : wordsInExpectedText) {
            boolean wordMatched = false;

            if (currentStoryBook.permanentlyMatchedIndices.contains(originalWordIndex)) {
                highlightedText.append("<font color='#00FF00'>").append(originalWords[originalWordIndex]).append("</font> ");
                wordMatched = true;
            } else if (currentStoryBook.wordMatchStatus.get(originalWordIndex)) {
                highlightedText.append("<font color='#00FF00'>").append(originalWords[originalWordIndex]).append("</font> ");
                wordMatched = true;
                currentStoryBook.permanentlyMatchedIndices.add(originalWordIndex);
            } else {
                while (workingWordIndex < wordsInWorkingText.length) {
                    if (wordsInWorkingText[workingWordIndex].equalsIgnoreCase(expectedWord)) {
                        highlightedText.append("<font color='#00FF00'>").append(originalWords[originalWordIndex]).append("</font> ");
                        currentStoryBook.wordMatchStatus.set(originalWordIndex, true);
                        currentStoryBook.permanentlyMatchedIndices.add(originalWordIndex);
                        wordMatched = true;
                        workingWordIndex++;
                        break;
                    }
                    workingWordIndex++;
                }
            }

            if (!wordMatched) {
                highlightedText.append(originalWords[originalWordIndex]).append(" ");
            } else {
                matchCount++;
            }
            originalWordIndex++;
        }

        runOnUiThread(() -> {
            TextView promptTextView = binding.recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.storybook_promptText);
            promptTextView.setText(Html.fromHtml(highlightedText.toString()), TextView.BufferType.SPANNABLE);
            promptTextView.setLineSpacing(0, 1.5f);
        });

        if (isFinal) {
            float correctPercentage = ((float) matchCount / wordsInExpectedText.length) * 100;
            if (!currentStoryBook.videoPlayed && correctPercentage >= 80) {
                currentStoryBook.videoPlayed = true;
                StoryBookAdapter.StoryBookHolder holder = (StoryBookAdapter.StoryBookHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);
                playVideo(holder.imageView, holder.videoView, "android.resource://" + getPackageName() + "/" + currentStoryBook.video);

                // Bir sonraki bölümü aktif hale getirme
                if (position + 1 < storyBookList.size()) {
                    storyBookList.get(position + 1).isActive = true;
                    runOnUiThread(() -> adapter.notifyItemChanged(position + 1));
                }
            }
        }
    }
    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechClient != null) {
            speechClient.close();
        }
    }


    private void playVideo(ImageView imageView, VideoView videoView, String videoUrl) {
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setLooping(true);
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e("VideoViewError", "Error: " + what + ", " + extra);
            return true;
        });
    }
}


