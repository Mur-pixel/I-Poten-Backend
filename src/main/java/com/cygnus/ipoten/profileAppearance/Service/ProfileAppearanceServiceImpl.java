package com.cygnus.ipoten.profileAppearance.Service;

import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.profileAppearance.Controller.response.AppearanceResponse;
import com.cygnus.ipoten.profileAppearance.Entity.ProfileAppearance;
import com.cygnus.ipoten.profileAppearance.Repository.ProfileAppearanceRepository;
import com.cygnus.ipoten.userAttendance.repository.AttendanceRepository;
import com.cygnus.ipoten.userLevel.repository.UserLevelHistoryRepository;
import com.cygnus.ipoten.userSchedule.repository.UserScheduleRepository;
import com.cygnus.ipoten.userTitle.repository.UserTitleRepository;
import com.cygnus.ipoten.userTrustscore.repository.TrustScoreHistoryRepository;
import com.cygnus.ipoten.userTrustscore.repository.TrustScoreRepository;
import com.cygnus.ipoten.userLevel.repository.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProfileAppearanceServiceImpl implements ProfileAppearanceService {

    private final ProfileAppearanceRepository appearanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserLevelHistoryRepository userLevelHistoryRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserTitleRepository titleRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final TrustScoreHistoryRepository trustScoreHistoryRepository;

    private final AccountRepository accountRepository;
    private final AccountProfileRepository accountProfileRepository;
    private final S3Service s3Service;

    /** 회원 가입 시 호출 **/
    @Override
    @Transactional
    public Optional<ProfileAppearance> create(Long accountId) {
        ProfileAppearance pa = ProfileAppearance.init(accountId);
        return Optional.of(appearanceRepository.save(pa));
    }

    /** 회원 탈퇴 시 호출 **/
    @Override
    @Transactional
    public void delete(Long accountId) {
        // 1️⃣ 존재 확인
        if (!accountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Account not found for id=" + accountId);
        }

        // 2️⃣ 활동/참여 관련 데이터
        attendanceRepository.deleteAllByAccount_Id(accountId);
        userScheduleRepository.deleteAllByAccountId(accountId);

        // 3️⃣ 성장/성과 관련 데이터
        titleRepository.deleteAllByAccountId(accountId);
        trustScoreHistoryRepository.deleteAllByAccountId(accountId);
        trustScoreRepository.deleteAllByAccountId(accountId);
        userLevelHistoryRepository.deleteAllByAccountId(accountId);
        userLevelRepository.deleteAllByAccountId(accountId);

        // 4️⃣ 프로필 관련
        appearanceRepository.deleteByAccountId(accountId);
    }

    /** 프로필 조회 **/
    @Override
    @Transactional
    public AppearanceResponse getMyAppearance(Long accountId) {

        // 1️⃣ 외형 정보: 없으면 자동 생성
        ProfileAppearance pa = appearanceRepository.findByAccountId(accountId)
                .orElseGet(() -> appearanceRepository.save(ProfileAppearance.init(accountId)));

        // 2️⃣ 프로필 정보: 반드시 존재 (없으면 오류)
        AccountProfile ap = accountProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("AccountProfile not found"));

        // 3️⃣ Presigned URL 생성
        String presignedUrl = (pa.getPhotoKey() != null)
                ? s3Service.generateDownloadUrl(pa.getPhotoKey())
                : null;

        return AppearanceResponse.of(pa, ap, presignedUrl);
    }

    /** 사진 업데이트 (photoKey 저장) **/
    @Override
    public AppearanceResponse.PhotoResponse updatePhoto(Long accountId, String photoKey) {
        ProfileAppearance pa = appearanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("ProfileAppearance not found"));

        pa.setPhotoKey(photoKey);
        appearanceRepository.save(pa);

        return new AppearanceResponse.PhotoResponse(pa.getPhotoKey());
    }

    // Presigned Upload URL 발급 + 기존 파일 삭제 + DB 저장
    public String generateUploadUrl(Long accountId, String filename, String contentType) {
        // 기존 파일 삭제
        String oldKey = null;

        try {
            oldKey = getPhotoKey(accountId);
            if( oldKey != null && !oldKey.isBlank() ) {
                s3Service.deleteFile(oldKey);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 기존 프로필 삭제 실패: " + e.getMessage());
        }

        // 확장자 추출 (없으면 기본 .png)
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex); // ".png"
        } else {
            extension = ".png";
        }

        // 새로운 key 생성
        String key = String.format("profile/%d/%s%s", accountId, UUID.randomUUID(), extension);

        // Presigned URL 발급
        String presignedUrl = s3Service.generateUploadUrl(key, contentType);

        // DB에 새 key 저장
        updatePhoto(accountId, key);

        return presignedUrl;
    }

    /** 사진 key 조회 **/
    @Override
    @Transactional(readOnly = true)
    public String getPhotoKey(Long accountId) {
        ProfileAppearance pa = appearanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("ProfileAppearance not found"));
        return pa.getPhotoKey();
    }

    // Presigned Download URL 발급
    public String generateDownloadUrl(Long accountId) {
        String key = getPhotoKey(accountId);

        return s3Service.generateDownloadUrl(key);
    }
}
