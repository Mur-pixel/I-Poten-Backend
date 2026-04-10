package com.cygnus.iptn.ebook.service;

import com.cygnus.iptn.ebook.entity.Ebook;

import java.nio.file.Path;
import java.util.List;

public interface EbookService {
    /** 전체 Ebook 목록을 최신 생성 순으로 반환 */
    List<Ebook> listAll();

    /** ebookId로 Ebook을 조회하고, 없으면 예외를 던짐 */
    Ebook getOrThrow(Long ebookId);

    /** ebookId의 fileKey를 storageDir 기준으로 안전하게 해석해 실제 PDF 파일 경로를 반환하고, 문제가 있으면 예외를 던짐 */
    Path resolveFilePathOrThrow(Long ebookId);
}
