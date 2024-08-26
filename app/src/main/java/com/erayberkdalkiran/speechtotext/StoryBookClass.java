package com.erayberkdalkiran.speechtotext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoryBookClass {
    String title;
    int image;
    int video;
    String textToRead;
    boolean videoPlayed;
    List<Boolean> wordMatchStatus; // Her bir kelimenin eşleşip eşleşmediğini takip eder
    Set<Integer> permanentlyMatchedIndices; // Kalıcı olarak doğru eşleşmiş kelimeleri tutar
    boolean isActive;  // Bölümün aktif olup olmadığını belirten özellik

    public StoryBookClass(String title, int image, int video, String textToRead) {
        this.title = title;
        this.image = image;
        this.video = video;
        this.textToRead = textToRead;
        this.videoPlayed = false;
        this.isActive = false; // Varsayılan olarak inaktif

        // Metindeki her bir kelime için false ile başlatılmış bir liste
        String[] words = textToRead.split("\\s+");
        this.wordMatchStatus = new ArrayList<>(Arrays.asList(new Boolean[words.length]));
        Collections.fill(this.wordMatchStatus, Boolean.FALSE);

        // Kalıcı olarak eşleşen kelimeleri tutan set
        this.permanentlyMatchedIndices = new HashSet<>();
    }
}
