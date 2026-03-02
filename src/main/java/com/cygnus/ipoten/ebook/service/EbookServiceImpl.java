package com.cygnus.ipoten.ebook.service;

import com.cygnus.ipoten.ebook.entity.Ebook;
import com.cygnus.ipoten.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EbookServiceImpl implements EbookService {

    private final EbookRepository ebookRepository;
    private final EbookProperties ebookProperties;

    @Override
    public List<Ebook> listAll() {
        return ebookRepository.findAll().stream()
                .sorted(Comparator.comparing(Ebook::getCreatedAt).reversed()).toList();
    }

    @Override
    public Ebook getOrThrow(Long ebookId) {
        return ebookRepository.findById(ebookId).orElseThrow(() -> new NoSuchElementException("ebook not found. id=" + ebookId));
    }

    @Override
    public Path resolveFilePathOrThrow(Long ebookId) {
        Ebook ebook = getOrThrow(ebookId);

        String storageDir = ebookProperties.storageDir();
        if (storageDir == null || storageDir.isBlank()) {
            throw new NoSuchElementException("ebook.storage-dir is not configured");
        }

        Path base = Paths.get(storageDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(ebook.getFileKey()).normalize();

        if (!resolved.startsWith(base)) {
            throw new SecurityException("invalid fileKey (path traversla). ebookId=" + ebookId);
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new NoSuchElementException("ebook file not found. ebookId=" + ebookId);
        }

        return resolved;
    }
}
