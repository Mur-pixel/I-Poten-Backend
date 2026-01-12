package com.cygnus.ipoten.wordbook_pdf.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;



import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class OpenPdfRenderer implements PdfRenderer {

    @Value("${ebook.pdf.font-path:classpath:/fonts/NotoSansKR-Regular.ttf}")
    private String fontPath;

    private volatile BaseFont cached;

    private static final String KW_LABEL_TAG = "KW_LABEL";
    private static final Color KW_BG = new Color(245, 245, 245);
    private static final float KW_PAD_R = 6f;
    private static final float KW_LABEL_TO_TAG_GAP_PT = 6f;

    @Override
    public void render(String title, List<? extends TermView> terms, OutputStream out) throws IOException {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 48, 48);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new KeywordLabelPillEvent());

            doc.open();

            BaseFont bf = (cached != null) ? cached : (cached = loadBaseFontAsFile(fontPath));

            Font h1        = new Font(bf, 16, Font.BOLD);
            Font titleFont = new Font(bf, 15, Font.BOLD);
            Font descFont  = new Font(bf, 11, Font.NORMAL);
            Font tagFont   = new Font(bf, 9, Font.NORMAL, new Color(120, 120, 120));

            doc.add(new Paragraph((title == null || title.isBlank()) ? "내 단어장 PDF" : title, h1));
            doc.add(Chunk.NEWLINE);

            for (TermView t : terms) {
                renderTermBlock(doc, t, titleFont, descFont, tagFont);
            }

            doc.close();
        } catch (DocumentException e) {
            throw new IOException("PDF render failed", e);
        }
    }

    private void renderTermBlock(Document doc, TermView t, Font titleFont, Font descFont, Font tagFont)
            throws DocumentException {

        Paragraph termTitle = new Paragraph(nz(t.getTerm()), titleFont);
        termTitle.setSpacingBefore(12);
        termTitle.setSpacingAfter(6);
        doc.add(termTitle);

        LineSeparator line = new LineSeparator();
        line.setLineWidth(0.8f);
        line.setLineColor(Color.BLACK);
        doc.add(line);

        Paragraph desc = new Paragraph(nz(t.getDescription()), descFont);
        desc.setSpacingBefore(8);
        desc.setSpacingAfter(10);
        desc.setLeading(16);
        doc.add(desc);

        // "연관 키워드"만 라운드 박스, 해시태그는 그냥 텍스트
        if (t.getTags() != null && !t.getTags().isBlank()) {
            Paragraph tags = buildTagLine(nz(t.getTags()), tagFont, doc);
            tags.setSpacingAfter(16);
            doc.add(tags);
        }
    }

    private Paragraph buildTagLine(String rawTags, Font tagFont, Document doc) {
        Phrase ph = new Phrase();

        String labelText = "연관 키워드";

        Chunk label = new Chunk(labelText, tagFont);
        label.setGenericTag(KW_LABEL_TAG);
        ph.add(label);

        // 라벨 박스 오른쪽(padR) + 원하는 gap 만큼만 띄우기
        ph.add(makeGapChunk(tagFont, KW_PAD_R + KW_LABEL_TO_TAG_GAP_PT));

        // 태그는 # 붙여서 출력
        ph.add(new Chunk(formatHashtags(rawTags), tagFont));

        Paragraph p = new Paragraph(ph);
        p.setLeading(14);
        return p;
    }

    private Chunk makeGapChunk(Font font, float gapPt) {
        // NBSP(줄어들지 않는 공백)로 폭을 안정적으로 확보
        float spaceW = cached.getWidthPoint("\u00A0", font.getSize());
        int n = Math.max(1, Math.round(gapPt / Math.max(spaceW, 0.1f)));

        String spaces = "\u00A0".repeat(n); // Java 11+
        return new Chunk(spaces, font);
    }

    /**
     * GenericTag가 걸린 Chunk 위치(rect)를 받아 라운드 박스를 그려줌
     * - 배경은 DirectContentUnder에 그려서 텍스트 뒤로 들어감
     */
    private static class KeywordLabelPillEvent extends PdfPageEventHelper {
        @Override
        public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
            if (!KW_LABEL_TAG.equals(text)) return;

            // 가로 패딩(좌우)
            float padL = 6f;
            float padR = 6f;

            // 세로 패딩(위/아래) - 아래를 더 크게 주면 글씨가 가운데로 올라와 보임
            float padTop = 2.2f;
            float padBottom = 4.0f;

            float x = rect.getLeft() - padL;
            float y = rect.getBottom() - padBottom;
            float w = rect.getWidth() + padL + padR;
            float h = rect.getHeight() + padTop + padBottom;

            PdfContentByte cb = writer.getDirectContentUnder();
            cb.saveState();
            cb.setColorFill(KW_BG);

            // 테두리 없음: fill만
            cb.roundRectangle(x, y, w, h, 7f);
            cb.fill();

            cb.restoreState();
        }
    }

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

    private String formatHashtags(String raw) {
        List<String> tags = parseTags(raw);
        if (tags.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("#").append(tags.get(i));
        }
        return sb.toString();
    }

    private List<String> parseTags(String raw) {
        // 콤마/개행/슬래시/파이프 등 → 공백으로 통일
        String s = nz(raw).trim()
                .replace(",", " ")
                .replace("/", " ")
                .replace("|", " ")
                .replace("\n", " ");

        String[] tokens = s.split("\\s+");
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();

        for (String tok : tokens) {
            if (tok == null) continue;
            String t = tok.trim();
            if (t.isEmpty()) continue;

            while (t.startsWith("#")) t = t.substring(1);

            // 한글/영문/숫자/_-만 남김
            t = t.replaceAll("[^\\p{L}\\p{N}_-]", "");
            if (!t.isEmpty()) set.add(t);
        }
        return new java.util.ArrayList<>(set);
    }

}
