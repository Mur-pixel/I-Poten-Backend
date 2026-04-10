package com.cygnus.iptn.quiz_session_generator.value_objects;

import java.util.function.Function;

public record OptionArrangementPolicy(
        int optionCount,
        double lengthBiasTolerance,
        Function<Integer, String> fallbackGenerator
) {
}
