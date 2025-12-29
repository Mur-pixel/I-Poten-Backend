package com.cygnus.ipoten.quiz_session_generator.value_objects;

public record InitialsHintPolicy(
        boolean revealLength,
        int revealInitialsCount,
        int maxDescriptionLength
) {
}
