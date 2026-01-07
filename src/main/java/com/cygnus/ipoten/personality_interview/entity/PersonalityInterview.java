package com.cygnus.ipoten.personality_interview.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class PersonalityInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String description;



}
