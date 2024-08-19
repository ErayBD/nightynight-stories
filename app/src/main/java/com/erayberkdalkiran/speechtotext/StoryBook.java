package com.erayberkdalkiran.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
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
import java.util.List;

public class StoryBook extends AppCompatActivity {

    private ActivityStorybookBinding binding;
    private ArrayList<StoryBookClass> storyBookList;
    private StoryBookAdapter adapter;
    private SpeechClient speechClient;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private StringBuilder fullTranscript = new StringBuilder();
    private int totalWordCount = 0;
    private int correctWordCount = 0;
    private boolean videoPlayed = false;

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
        fullTranscript.setLength(0);  // fullTranscript'i sıfırla

        if (isRecording) {
            stopListening();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        isRecording = true;

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

    private void stopListening() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void processTranscript(String transcript, int position, boolean isFinal) {
        if (isFinal) {
            fullTranscript.append(transcript).append(" "); // Final sonuçları fullTranscript'e ekle
        } else {
            // Sadece geçici transkript ise currentText'i güncelle
            fullTranscript.append(transcript).append(" ");
        }

        // Şu anki gösterilecek metin, final sonuçsa fullTranscript, değilse geçici transkript
        String currentText = isFinal ? fullTranscript.toString().trim() : transcript.trim();

        Log.d("FullTranscript", "Current Transcript: " + currentText);

        String expectedText = storyBookList.get(position).textToRead;

        // Temizlenmiş metinler (noktalama işaretleri ve küçük harfler)
        String cleanedExpectedText = expectedText.replaceAll("[^a-zA-ZğüşıöçĞÜŞİÖÇ\\s]", "").toLowerCase();
        String cleanedCurrentText = currentText.replaceAll("[^a-zA-ZğüşıöçĞÜŞİÖÇ\\s]", "").toLowerCase();

        // Loglar
        Log.d("wordsInExpectedText", "Expected Text: " + cleanedExpectedText);
        Log.d("wordsInTranscript", "Speech to Text: " + cleanedCurrentText);

        // Kelimeleri doğru eşleştir
        String[] wordsInExpectedText = cleanedExpectedText.split("\\s+");
        String[] wordsInCurrentText = cleanedCurrentText.split("\\s+");

        totalWordCount = wordsInExpectedText.length;
        correctWordCount = 0;  // Doğru kelime sayısını sıfırla
        StringBuilder highlightedText = new StringBuilder();
        int originalWordIndex = 0;

        for (int i = 0; i < wordsInExpectedText.length; i++) {
            boolean wordMatched = false;
            for (int j = originalWordIndex; j < wordsInCurrentText.length; j++) {
                if (wordsInExpectedText[i].equalsIgnoreCase(wordsInCurrentText[j])) {
                    highlightedText.append("<font color='#00FF00'>").append(expectedText.split("\\s+")[i]).append("</font> ");
                    originalWordIndex = j + 1;
                    wordMatched = true;
                    correctWordCount++;
                    break;
                }
            }
            if (!wordMatched) {
                highlightedText.append(expectedText.split("\\s+")[i]).append(" ");
            }
        }

        // Metni yeşile dönüştür
        runOnUiThread(() -> {
            TextView promptTextView = binding.recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.storybook_promptText);
            promptTextView.setText(Html.fromHtml(highlightedText.toString()), TextView.BufferType.SPANNABLE);
            promptTextView.setLineSpacing(0, 1.5f);
        });

        // Doğru okunan kelimelerin yüzdesini hesapla ve %80'den fazlaysa videoyu oynat
        StoryBookClass currentStoryBook = storyBookList.get(position);
        if (isFinal && !currentStoryBook.videoPlayed) {  // Her bölüm için video oynatılıp oynatılmadığını kontrol edin
            float correctPercentage = ((float) correctWordCount / totalWordCount) * 100;
            if (correctPercentage >= 80) {
                currentStoryBook.videoPlayed = true;  // Bu bölümde video oynatıldığını işaretleyin
                StoryBookAdapter.StoryBookHolder holder = (StoryBookAdapter.StoryBookHolder) binding.recyclerView.findViewHolderForAdapterPosition(position);
                playVideo(holder.imageView, holder.videoView, "android.resource://" + getPackageName() + "/" + currentStoryBook.video);
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


