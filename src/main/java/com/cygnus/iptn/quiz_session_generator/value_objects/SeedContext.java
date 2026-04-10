package com.cygnus.iptn.quiz_session_generator.value_objects;

import java.util.Random;

public record SeedContext(long seed) {
    public Random newRandom() {
        return new Random(seed);
    }
}