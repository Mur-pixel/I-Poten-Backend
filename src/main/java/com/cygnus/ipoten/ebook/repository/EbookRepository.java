package com.cygnus.ipoten.ebook.repository;

import com.cygnus.ipoten.ebook.entity.Ebook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EbookRepository extends JpaRepository<Ebook,Long> {
}
