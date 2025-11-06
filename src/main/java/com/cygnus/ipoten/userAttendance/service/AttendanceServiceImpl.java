package com.cygnus.ipoten.userAttendance.service;

import com.cygnus.ipoten.userAttendance.entity.Attendance;
import com.cygnus.ipoten.userDashboard.controller.response.AttendanceRateResponse;
import com.cygnus.ipoten.userAttendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 로그인 성공 시 호출 : 오늘 로그인했는가? 하루 1회만 기록 **/
    @Override
    @Transactional
    public boolean markLogin(Long accountId) {
        LocalDate todayKst = LocalDate.now(KST);

        int rows = attendanceRepository.insertIgnore(accountId, todayKst);

        return rows > 0; // 1이면 새로 찍힘, 0이면 이미 있었음
    }

    /** 이번 달 출석률(%) - 달력 기준 **/
    @Override
    @Transactional(readOnly = true)
    public AttendanceRateResponse getThisMonthRate(Long accountId) {

        YearMonth yearMonth = YearMonth.now(KST);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        int attended = attendanceRepository.countDaysInMonth(accountId, startDate, endDate);
        int total = yearMonth.lengthOfMonth();

        double attendanceRate = (attended * 100.0) / total;

        return new AttendanceRateResponse(attendanceRate, attended, total);
    }

    @Override
    @Transactional(readOnly = true)
    public int getConsecutiveDays(Long accountId) {
        // 최신 출석 기록 순으로 정렬해서 조회
        List<Attendance> records = attendanceRepository
                .findTop30ByAccount_IdOrderByLoginDateDesc(accountId);

        // 오늘 날짜부터 거꾸로 연속성 계산
        int streak = 0;
        LocalDate today = LocalDate.now();

        for (Attendance a : records) {
            // 로그인 날짜 기준으로 비교
            if (a.getLoginDate().equals(today.minusDays(streak))) {
                streak++;
            } else break; // 연속성이 끊기면 중단
        }

        return streak;
    }
}
