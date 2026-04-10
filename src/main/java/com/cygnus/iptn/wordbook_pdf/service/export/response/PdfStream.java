package com.cygnus.iptn.wordbook_pdf.service.export.response;


import java.io.OutputStream;

@FunctionalInterface
public interface PdfStream {
    void writeTo(OutputStream out) throws Exception;
}
