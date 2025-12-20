package com.cygnus.ipoten.term_topic_tag.repository;

import com.cygnus.ipoten.term_topic_tag.entity.TopicTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicTagRepository extends JpaRepository<TopicTag, Long> {
    Optional<TopicTag> findByKey(String key);
}
