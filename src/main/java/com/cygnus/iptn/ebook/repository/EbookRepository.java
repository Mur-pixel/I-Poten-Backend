package com.cygnus.iptn.ebook.repository;

import com.cygnus.iptn.ebook.entity.Ebook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EbookRepository extends JpaRepository<Ebook,Long> {
}
