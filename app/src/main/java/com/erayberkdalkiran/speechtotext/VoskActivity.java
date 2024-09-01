package com.erayberkdalkiran.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.erayberkdalkiran.speechtotext.databinding.ActivityVoskBinding;
import com.google.android.material.snackbar.Snackbar;

public class VoskActivity extends AppCompatActivity implements RecognitionListener {

    private ActivityVoskBinding binding;
    private ActivityResultLauncher<String> permissionLauncher; // mikrofon izni istemek icin kullanilir
    private SpeechService speechService; // mikrofon verilerini islemek ve ses tanima islemini gerceklestirmek icin kullanilir
    private Model model; // vosk api kapsaminda ses tanıma modeli için kullanılır.
    private boolean isRecording = false;

    // setUiState fonksiyonu icerisindeki switch-caseler icin kullanilir
    static private final int STATE_START = 0; // uygulama baslatilirken kullanilan baslangıc durumu
    static private final int STATE_READY = 1; // ses tanima icin hazir olundugunda kullanilan durum
    static private final int STATE_DONE = 2; // tanima islemi tamamlandiginda kullanilan durum
    static private final int STATE_MIC = 3; // mikrofon dinleme sirasinda kullanilan durum

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoskBinding.inflate(getLayoutInflater());
        View activityView = binding.getRoot();
        setContentView(activityView);

        setUiState(STATE_START); // arayuz durumu baslangic konumuna getirilir
        permissionLauncherInit(); // mic icin izin isteme baslaticisi ayarlanır.

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
                Toast.makeText(VoskActivity.this, "Lütfen bir prompt girin!", Toast.LENGTH_SHORT).show();
            }
        });

        // mikrofon butonuna basilinca calisir
        binding.micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    binding.micButton.setText("Stop Listening");
                    recognizeMicrophone();
                    isRecording = true;
                } else {
                    recognizeMicrophone();
                    binding.micButton.setText("Start Listening");
                    isRecording = false;
                }
            }
        });

        // vosk kutuphanesi icin islemlerin loglanma seviyesi ayarlanir
        LibVosk.setLogLevel(LogLevel.INFO);

        // mikrofon kullanimi icin izin kontrolu saglanir, varsa baslatilir yoksa istenir
        askForPermission();
    }

    // mikrofon kullanimi icin izin kontrolu saglanir, varsa baslatilir yoksa istenir
    private void askForPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Mikrofon izni zaten verilmişse modeli başlatın
            initModel();
        } else {
            // İzin verilmemişse izin isteyin
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Snackbar.make(binding.getRoot(), "Bu işlem için mikrofon iznine ihtiyaç vardır.", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    }
                }).show();
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    // mikrofon izni varsa model baslatilir
    private void permissionLauncherInit() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                if (o) {
                    initModel();
                }
                else {
                    Toast.makeText(VoskActivity.this, "Bu işlem için mikrofon iznine ihtiyaç vardır.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // model SDK olarak iceri aktarilir
    private void initModel() {
        StorageService.unpack(this,
                "model-tr",
                "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }

    // konusma tanimlama zaten calisiyorsa kapatilir, degilse baslatilir
    private void recognizeMicrophone() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
            setUiState(STATE_DONE);
        }
        else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            }
            catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    // olusan hata mesajini ekranda gosterir ve mic butonunu devre disi birakir
    private void setErrorState(String message) {
        binding.resultText.setText(message);
        binding.micButton.setEnabled(false);
    }

    // konusma tanimlamayi durdurup baslatmaya yarar, toggleButton sayesinde sirayla true ve false olur
    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }

    // uygulamanin yasam dongusu
    private void setUiState(int state) {
        switch (state) {
            // model iceri aktarilana kadar arayuz elemanlarini gecersiz kilar
            case STATE_START:
                binding.resultText.setText(R.string.preparing);
                binding.resultText.setMovementMethod(new ScrollingMovementMethod());
                binding.micButton.setEnabled(false);
                break;

            // model iceri aktarildiktan sonra arayuz elemanları tekrardan aktif olur
            case STATE_READY:
                binding.resultText.setText("");
                binding.micButton.setEnabled(true);
                break;

            // mikrofon dinleme sirasinda butonlar etkinlestirilir ve metin guncellenir
            case STATE_MIC:
                binding.micButton.setEnabled(true);

                break;

            // tanima tamamlandiginda butonlar yeniden etkinlestirilir
            case STATE_DONE:
                binding.micButton.setEnabled(true);
                break;

            // beklenmedik bir durum degeri girildiginde hata firlatilir
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    // her bir kelimenin tanimlandigi durum
    @Override
    public void onResult(String hypothesis) {
        binding.resultText.append(hypothesis + "\n");
//        try {
//            JSONObject jsonObject = new JSONObject(hypothesis);
//            String textValue = jsonObject.getString("text");
//
//            if (!binding.resultText.getText().toString().isEmpty()) {
//                binding.resultText.append(textValue + " ");
//            }
//            processTranscript(textValue);
//        }
//        catch (Exception e) {
//            // JSON parse işlemi sırasında bir hata olursa, hata mesajını yazdırıyoruz
//            e.printStackTrace();
//        }
    }

    // soylenmis ve algilanmis olan soz ekrana tek tek yazilirkenki durum
    @Override
    public void onPartialResult(String hypothesis) {
        binding.resultText.append(hypothesis + "\n");
//        try {
//            JSONObject jsonObject = new JSONObject(hypothesis);
//            String textValue = jsonObject.getString("text");
//
//            if (!binding.resultText.getText().toString().isEmpty()) {
//                binding.resultText.append(textValue + " ");
//            }
//            processTranscript(textValue);
//        }
//        catch (Exception e) {
//            // JSON parse işlemi sırasında bir hata olursa, hata mesajını yazdırıyoruz
//            e.printStackTrace();
//        }
    }

    // soylenen cumle tamamlandigindaki durum
    @Override
    public void onFinalResult(String hypothesis) {
        binding.resultText.append(hypothesis + "\n");
//        try {
//            JSONObject jsonObject = new JSONObject(hypothesis);
//            String textValue = jsonObject.getString("text");
//
//            if (!binding.resultText.getText().toString().isEmpty()) {
//                binding.resultText.append(textValue + "\n");
//            }
//            processTranscript(textValue); // Nihai sonuç için çağrılıyor
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    // tanimlama sirasinda mikrofon izni veya modelin yuklenmemesi gibi hataların alinmasi durumunu
    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    // belli bir sure boyunca ses algilanmazsa durumu
    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    // uygulama sonlandiginda speechService durdurulur ve kapatilir, kaynak yonetimi
    @Override
    public void onDestroy() {
        super.onDestroy();
        // konusma tanimlama calisiyor mu diye sorgular, calisiyorsa kapatir
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
    }

    private void processTranscript(String transcript) {
        String expectedText = "kedi atladı";
        if (transcript.trim().toLowerCase().contains(expectedText)) {
            Toast.makeText(this, "Doğru okudunuz, tebrikler!", Toast.LENGTH_SHORT).show();
            triggerImageToVideo(binding.textHandleEt.getText().toString());
        }
    }

    private void triggerImageToVideo(@Nullable String prompt) {
        String imageURL = "https://drive.google.com/file/d/1H08G4T6DU0GNMZ4Xn0_GBlw55LUmkdjE/view";
        String prompt_speech = "Animate a scene in a cozy bunny village during a bright, sunny day. The blue bunny named Mavi is happily playing with his mother in a lush green meadow. The wind gently blows through colorful flowers and small wooden houses in the background. Mavi hops around joyfully, while his mother watches with a loving smile. Birds chirp in the distance, and the sunlight filters through the trees, creating a warm and cheerful atmosphere.";
        String prompt_text = prompt;

        new Thread(() -> {
            try {
                JSONObject response;

                if (!prompt_text.isEmpty()) {
                    response = ImageToVideo.imageToVideoPOST(imageURL, prompt_text);
                } else {
                    response = ImageToVideo.imageToVideoPOST(imageURL, prompt_speech);
                }

                if (response != null && response.has("uuid")) {
                    String uuid = response.getString("uuid");

                    String videoUrl = null;
                    while (videoUrl == null) {
                        Thread.sleep(5000);
                        videoUrl = ImageToVideo.imageToVideoGET(uuid);
                    }

                    String finalVideoUrl = videoUrl;
                    runOnUiThread(() -> {
                        playVideo(finalVideoUrl);
                    });
                }
            } catch (IOException | JSONException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(VoskActivity.this, "Failed to generate video!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void playVideo(String videoUrl) {
        binding.imageView.setVisibility(View.GONE);
        binding.videoView.setVisibility(View.VISIBLE);

        binding.videoView.setVideoPath(videoUrl);
        binding.videoView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setLooping(true);
            binding.videoView.start();
        });

        binding.videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Video oynatılırken hata oluştu!", Toast.LENGTH_SHORT).show();
            return true;
        });
    }
}