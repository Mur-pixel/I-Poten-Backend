package com.cygnus.ipoten.ebook.service.export.dto;


import java.io.OutputStream;

@FunctionalInterface
public interface PdfStream {
    void writeTo(OutputStream out) throws Exception;
}
