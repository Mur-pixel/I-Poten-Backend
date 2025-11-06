package com.cygnus.ipoten.term.repository;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.entity.TermTag;
import com.cygnus.ipoten.term.entity.TermTagId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TermTagRepository extends JpaRepository<TermTag, TermTagId> {
    List<TermTag> findAllByTerm(Term term);

    @Modifying
    @Transactional
    @Query("DELETE FROM TermTag tt WHERE tt.term = :term")
    void deleteByTerm(@Param("term") Term term);

    interface Row {
        Long getTermId();
        String getTagName();
    }

    @Query("""
       select tt.term.id as termId, tg.name as tagName
       from TermTag tt
       join tt.tag tg
       where tt.term.id in :ids
    """)
    List<Row> findTermIdAndTagNameByTermIdIn(@Param("ids") List<Long> ids);

    // termId 하나에 대한 태그 이름 목록 (정렬은 이름 ASC)
    @Query("""
        select tg.name
        from TermTag tt
        join tt.tag tg
        where tt.term.id = :termId
        order by tg.name asc
    """)
    List<String> findAllNamesByTermId(@Param("termId") Long termId);

    // 태그 자동완성/추천 (q prefix 일치 + 사용빈도 순)
    @Query("""
        select tg.name
        from TermTag tt
        join tt.tag tg
        where (:q is null or :q = '' or tg.name like concat(:q, '%'))
        group by tg.name
        order by count(distinct tt.term.id) desc, tg.name asc
    """)
    List<String> suggestTagNames(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

}
