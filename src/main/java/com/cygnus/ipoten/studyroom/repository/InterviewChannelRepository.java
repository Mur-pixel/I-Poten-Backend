package com.cygnus.ipoten.studyroom.repository;

import com.cygnus.ipoten.studyroom.entity.InterviewChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewChannelRepository extends JpaRepository<InterviewChannel, Long> {
    // 특정 스터디룸에 속한 모든 채널 정보를 조회
    List<InterviewChannel> findAllByStudyRoomId(Long studyRoomId);
}