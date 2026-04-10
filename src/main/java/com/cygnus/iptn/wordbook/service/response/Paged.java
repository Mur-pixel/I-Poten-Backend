package com.cygnus.iptn.wordbook.service.response;

import java.util.List;

public record Paged<T>(List<T> items, long total) {
}
