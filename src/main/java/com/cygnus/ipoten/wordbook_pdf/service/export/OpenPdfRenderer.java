package com.cygnus.ipoten.wordbook_pdf.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class OpenPdfRenderer implements PdfRenderer {

    @Value("${ebook.pdf.font-path:classpath:/fonts/NotoSansKR-Regular.ttf}")
    private String fontPath;

    private volatile BaseFont cached; // 성능 위해 캐시

    @Override
    public void render(String title, List<? extends TermView> terms, OutputStream out) throws IOException {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 48, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = (cached != null) ? cached : (cached = loadBaseFontAsFile(fontPath));
            Font h1   = new Font(bf, 16, Font.BOLD);
            Font cell = new Font(bf, 11);

            doc.add(new Paragraph((title == null || title.isBlank()) ? "내 단어장 PDF" : title, h1));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidths(new float[]{2f, 6f, 2f});
            table.setWidthPercentage(100);

            // 헤더
            Stream.of("용어", "설명", "태그").forEach(col -> {
                PdfPCell c = new PdfPCell(new Phrase(col, cell));
                c.setBackgroundColor(new Color(240,240,240));
                c.setPadding(6);
                table.addCell(c);
            });

            // 데이터
            for (TermView t : terms) {
                table.addCell(new PdfPCell(new Phrase(nz(t.getTerm()), cell)));
                table.addCell(new PdfPCell(new Phrase(nz(t.getDescription()), cell)));
                table.addCell(new PdfPCell(new Phrase(nz(t.getTags()), cell)));
            }

            doc.add(table);
            doc.close(); // 꼭 닫아야 완전한 PDF가 됨
        } catch (DocumentException e) {
            throw new IOException("PDF render failed", e);
        }
    }

    // 클래스패스/절대경로 모두 지원 → 임시파일로 저장 후 경로 기반 createFont 호출
    private BaseFont loadBaseFontAsFile(String location) throws IOException {
        byte[] bytes = readAll(location);
        Path tmp = Files.createTempFile("pdf-font-", ".ttf");
        Files.write(tmp, bytes);
        tmp.toFile().deleteOnExit();
        try {
            return BaseFont.createFont(tmp.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException e) {
            throw new IOException("Font load failed: " + location, e);
        }
    }

    private byte[] readAll(String location) throws IOException {
        if (location.startsWith("classpath:")) {
            String p = location.substring("classpath:".length());
            try (var in = getClass().getResourceAsStream(p)) {
                if (in == null) throw new IOException("Font not found in classpath: " + p);
                return in.readAllBytes();
            }
        } else {
            return Files.readAllBytes(Path.of(location));
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
