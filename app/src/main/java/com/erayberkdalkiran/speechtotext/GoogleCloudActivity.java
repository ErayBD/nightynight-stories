package com.erayberkdalkiran.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.erayberkdalkiran.speechtotext.databinding.ActivityGooglecloudBinding;
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
import java.util.List;

public class GoogleCloudActivity extends AppCompatActivity {

    ActivityGooglecloudBinding binding;

    private static final int SAMPLE_RATE = 16000;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean isRecording = false;
    private SpeechClient speechClient;
    private AudioRecord audioRecord;
    private TextView transcriptionText;
    private Button startButton;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGooglecloudBinding.inflate(getLayoutInflater());
        View activityView = binding.getRoot();
        setContentView(activityView);

        startButton = binding.startButton;
        transcriptionText = binding.transcriptionText;
        videoView = binding.videoView;

        // togglebutton settings
        binding.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.textHandleEt.setVisibility(View.VISIBLE);
                    binding.button.setVisibility(View.VISIBLE);
                    binding.textHandleTv.setVisibility(View.GONE);
                } else {
                    binding.textHandleEt.setVisibility(View.GONE);
                    binding.button.setVisibility(View.GONE);
                    binding.textHandleTv.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.button.setOnClickListener(view -> {
            String userPrompt = binding.textHandleEt.getText().toString().trim();
            if (!userPrompt.isEmpty()) {
                triggerImageToVideo(userPrompt);
            } else {
                Toast.makeText(GoogleCloudActivity.this, "Lütfen bir prompt girin!", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            initSpeechClient();
        }

        startButton.setOnClickListener(view -> {
            if (!isRecording) {
                startListening();
                startButton.setText("Stop Listening");
            } else {
                stopListening();
                startButton.setText("Start Listening");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSpeechClient();
            } else {
                Toast.makeText(this, "Audio permission is required!", Toast.LENGTH_SHORT).show();
                transcriptionText.setText("Audio permission is required!");
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
            transcriptionText.setText("Failed to create SpeechClient!");
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        isRecording = true;
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
                * 2);

        Log.d("AudioRecord", "Buffer size: " + bufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioRecord.startRecording();

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];

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
                                    transcriptionText.setText(transcript + " ");
                                    if (result.getIsFinal()) {
                                        processTranscript(transcript);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e("Streaming", "Error: " + t.getMessage());
                        runOnUiThread(() -> Toast.makeText(GoogleCloudActivity.this,
                                "Error during speech recognition: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCompleted() {
                        Log.d("Streaming", "Recognition completed.");
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

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Streaming", "Error: " + e.toString());
                runOnUiThread(() -> Toast.makeText(GoogleCloudActivity.this,
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

    private void processTranscript(String transcript) {
        String expectedText = "kedi atladı";
        if (transcript.trim().toLowerCase().contains(expectedText)) {
            Toast.makeText(this, "Dogru okudunuz, tebrikler!", Toast.LENGTH_SHORT).show();
            triggerImageToVideo(binding.textHandleEt.getText().toString());
        }
    }

    public void triggerImageToVideo(@Nullable String prompt) {
        String imageURL = "https://d2jqrm6oza8nb6.cloudfront.net/datasets/609a94dd-6604-43ef-a2dd-b7d2f37fd535.webp?_jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJrZXlIYXNoIjoiZjI1ZjE0ODFkNWJjYzkzMiIsImJ1Y2tldCI6InJ1bndheS1kYXRhc2V0cyIsInN0YWdlIjoicHJvZCIsImV4cCI6MTcyMzc2NjQwMH0.iQfehGQTGt-Q7HTU2PtwZ0LYAsX6JjhyuEygSwDiqGg";
        String prompt_speech = "The cat drives the tractor through a colorful field";
        String prompt_text = prompt;

        new Thread(() -> {
            try {
                JSONObject response;

                if (!prompt_text.isEmpty()) {
                    response = ImageToVideo.imageToVideoPOST(imageURL, prompt_text);
                }
                else {
                    response = ImageToVideo.imageToVideoPOST(imageURL, prompt_speech);
                }

                if (response != null && response.has("uuid")) {
                    String uuid = response.getString("uuid");

                    // Polling for the video URL
                    String videoUrl = null;
                    while (videoUrl == null) {
                        Thread.sleep(5000); // Wait for 5 seconds before checking again
                        videoUrl = ImageToVideo.imageToVideoGET(uuid);
                    }

                    // Run on UI thread to update UI components
                    String finalVideoUrl = videoUrl;
                    runOnUiThread(() -> {
                        playVideo(finalVideoUrl);
                    });
                }
            } catch (IOException | JSONException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(GoogleCloudActivity.this, "Failed to generate video!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void playVideo(String videoUrl) {
        binding.imageView.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechClient != null) {
            speechClient.close();
        }
    }
}
