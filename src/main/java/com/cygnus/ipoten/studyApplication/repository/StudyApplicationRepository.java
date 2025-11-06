package com.cygnus.ipoten.studyApplication.repository;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyApplicationRepository extends JpaRepository<StudyApplication, Long> {

    // 특정 스터디와 지원자로 지원 내역이 존재하는지 확인(중복지원 방지용)
    boolean existsByStudyRoomAndApplicant(StudyRoom studyRoom, AccountProfile applicant);

    @Query("SELECT sa FROM StudyApplication sa " +
            "JOIN FETCH sa.studyRoom " +
            "WHERE sa.applicant.id = :applicantId")
    List<StudyApplication> findAllByApplicantIdWithStudyRoom(@Param("applicantId") Long applicantId);

    Optional<StudyApplication> findByStudyRoomAndApplicant(StudyRoom studyRoom, AccountProfile applicant);

    List<StudyApplication> findAllByStudyRoomId(Long studyRoomId);

    @Query("SELECT sa FROM StudyApplication sa " +
            "JOIN FETCH sa.studyRoom sr " +
            "LEFT JOIN FETCH sr.studyMembers " + // studyMembers가 없을 수도 있으니 LEFT JOIN FETCH 사용
            "JOIN FETCH sa.applicant " +
            "WHERE sa.id = :applicationId")
    Optional<StudyApplication> findByIdWithAllDetails(@Param("applicationId") Long applicationId);

    void deleteAllByApplicantId(Long applicantId);
}
