package com.cygnus.iptn.personality_interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class PersonalityInterviewAudio {

    @Id
    @Column(name = "personality_interview_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "personality_interview_id")
    private PersonalityInterview personalityInterview;

    @Column(nullable = false, length = 500)
    private String audioUrl;

    public PersonalityInterviewAudio(PersonalityInterview personalityInterview, String audioUrl) {
        this.personalityInterview = personalityInterview;
        this.audioUrl = audioUrl;
    }

    public void updateAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}
