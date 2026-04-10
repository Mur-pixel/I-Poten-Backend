package com.cygnus.iptn.term.entity;

import java.io.Serializable;
import java.util.Objects;

public class TermTagId implements Serializable {

    private Long term;  // 필드명은 TermTag 엔티티의 필드명과 같아야 함
    private Long tag;

    public TermTagId() {}

    public TermTagId(Long termId, Long tagId) {
        this.term = termId;
        this.tag = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TermTagId)) return false;
        TermTagId that = (TermTagId) o;
        return Objects.equals(term, that.term) &&
                Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, tag);
    }


}
