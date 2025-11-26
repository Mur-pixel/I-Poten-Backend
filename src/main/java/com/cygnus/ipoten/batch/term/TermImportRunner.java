package com.cygnus.ipoten.batch.term;

import com.cygnus.ipoten.term.service.TermService;
import com.cygnus.ipoten.term.service.request.CreateTermRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TermImportRunner implements CommandLineRunner {

    private final TermService termService;

    @Override
    public void run(String... args) throws Exception {
        String filePath = null;
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                filePath = arg.substring("--file=".length());
            }
        }
        if (filePath == null) {
            log.info("ImporterRunner skipped (no --file=...)");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("File not found: {}", filePath);
            return;
        }

        log.info("Importing terms from {}", filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
            String header = br.readLine(); // 첫 줄은 헤더
            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {
                String[] cols = line.split("\t", -1); // 탭 구분
                if (cols.length < 5) continue;

                Long categoryId = cols[0].isBlank() ? null : Long.valueOf(cols[0]);
                String termId = cols[1].isBlank() ? UUID.randomUUID().toString() : cols[1];
                String title = cols[2].trim();
                String description = cols[3].trim();
                String tags = cols[4].trim();

                CreateTermRequest request = new CreateTermRequest(categoryId, title, description, tags);
                termService.register(request);
                count++;
            }

            log.info("Import 완료: {} rows", count);
        }
    }
}
