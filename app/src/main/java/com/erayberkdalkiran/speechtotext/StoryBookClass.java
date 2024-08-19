package com.erayberkdalkiran.speechtotext;

public class StoryBookClass {
    String chapterName;
    int image;
    int video;
    String textToRead;
    boolean videoPlayed;

    public StoryBookClass(String chapterName, int image, int video, String textToRead) {
        this.chapterName = chapterName;
        this.image = image;
        this.video = video;
        this.textToRead = textToRead;
        this.videoPlayed = false;
    }
}
