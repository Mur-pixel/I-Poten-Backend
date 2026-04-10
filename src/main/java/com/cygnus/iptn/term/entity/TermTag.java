package com.cygnus.iptn.term.entity;

// TermId(FK)
// TagId(FK)

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TermTagId.class)
@Table(name = "term_tag",
       uniqueConstraints=@UniqueConstraint(name="uk_term_tag", columnNames={"term_id","tag_id"}))
public class TermTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

}
