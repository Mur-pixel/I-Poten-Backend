package com.cygnus.ipoten.studyroom.repository;

import com.cygnus.ipoten.studyroom.entity.Announcement;
import com.cygnus.ipoten.studyroom.entity.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Optional<StudyMember> findByStudyRoomIdAndAuthorId(Long studyRoomId, Long authorId);

    List<Announcement> findAllByStudyRoomId(Long studyRoomId);

    List<Announcement> findAllByAuthorId(Long authorId);
}
