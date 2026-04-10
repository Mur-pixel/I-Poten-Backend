package com.cygnus.iptn.interview.service.response;

public class InterviewWithAudio {
    private final String question;
    private final String audioUrl;

    public InterviewWithAudio(String question, String audioUrl) {
        this.question = question;
        this.audioUrl = audioUrl;
    }

    public String getQuestion() {
        return question;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}