package com.cygnus.ipoten.google_tts.service;

import java.util.List;

public interface GoogleTtsService {
    String synthesizeAndUpload(String text);
    String synthesizeAndUploadToPath(String text, String keyPrefix);
    List<String> synthesizeAndUploadList(List<String> texts);

}
