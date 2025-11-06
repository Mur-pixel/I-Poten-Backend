package com.cygnus.ipoten.ebook.service.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface PdfRenderer {
    interface TermView {
        Long getId();
        String getTerm();
        String getDescription();
        String getTags();
    }
    void render(String title,
                List<? extends TermView> terms,
                OutputStream out) throws IOException;
}
