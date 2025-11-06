package com.cygnus.ipoten.studyschedule.repository;

import com.cygnus.ipoten.studyschedule.entity.StudySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyScheduleRepository extends JpaRepository<StudySchedule,Long> {

    List<StudySchedule> findAllByStudyRoomId(Long studyRoomId);

    // 특정 사용자 기준 전체 일정 조회
    @Query("""
    SELECT s FROM StudySchedule s
    JOIN FETCH s.studyRoom r
    JOIN r.studyMembers m
    WHERE m.accountProfile.id = :accountId
    """)
    List<StudySchedule> findAllByMemberAccountId(Long accountId);

    List<StudySchedule> findAllByAuthorId(Long authorId);

}
