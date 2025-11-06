package com.cygnus.ipoten.studyroom.repository;

import com.cygnus.ipoten.studyroom.entity.StudyLocation;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {

    @Query("SELECT sr FROM StudyRoom sr " +
            "JOIN FETCH sr.host " +
            "LEFT JOIN FETCH sr.skillStack " +      // ✅ 추가
            "LEFT JOIN FETCH sr.recruitingRoles " + // ✅ 추가
            "WHERE sr.id = :id")
    Optional<StudyRoom> findByIdWithHost(@Param("id") Long id);

    @Query("SELECT sr.id FROM StudyRoom sr WHERE sr.id < :lastStudyId ORDER BY sr.id DESC")
    Slice<Long> findIdsByIdLessThan(@Param("lastStudyId") Long lastStudyId, Pageable pageable);

    @Query("SELECT sr.id FROM StudyRoom sr ORDER BY sr.id DESC")
    Slice<Long> findIds(Pageable pageable);

    // 2단계: ID 목록을 기반으로 모든 데이터 Fetch
    @Query("SELECT DISTINCT sr FROM StudyRoom sr " +
            "LEFT JOIN FETCH sr.skillStack " +
            "LEFT JOIN FETCH sr.recruitingRoles " +
            "WHERE sr.id IN :ids " +
            "ORDER BY sr.id DESC")
    List<StudyRoom> findAllWithDetailsByIds(@Param("ids") List<Long> ids);

    List<StudyRoom> findByLocation(StudyLocation location);

    // ID로 조회할 때 모임장(host)과 멤버 목록(studyMembers)을 함께 즉시 로딩하는 쿼리
    @Query("SELECT sr FROM StudyRoom sr " +
            "JOIN FETCH sr.host " +
            "LEFT JOIN FETCH sr.studyMembers " +
            "WHERE sr.id = :id")
    Optional<StudyRoom> findByIdWithHostAndMembers(@Param("id") Long id);

    List<StudyRoom> findAllByHostId(Long hostId);

    // =========================
    // 👇 대시보드용 메소드 추가
    // =========================
    /** 특정 accountId가 Host인 StudyRoom 개수 카운트 */
    long countByHost_Account_Id(Long accountId);
}