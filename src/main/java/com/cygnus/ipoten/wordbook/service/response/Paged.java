package com.cygnus.ipoten.wordbook.service.response;

import java.util.List;

public record Paged<T>(List<T> items, long total) {
}
