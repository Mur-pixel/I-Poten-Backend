package com.cygnus.ipoten.inquiry.repository;

import com.cygnus.ipoten.inquiry.entity.Inquiry;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByAccount_Id(Long accountId, Sort sort);
}