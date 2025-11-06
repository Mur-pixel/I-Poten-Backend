package com.cygnus.ipoten.userDashboard.attendance.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.userAttendance.entity.Attendance;
import com.cygnus.ipoten.userAttendance.repository.AttendanceRepository;
import com.cygnus.ipoten.userAttendance.service.AttendanceServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    AttendanceRepository attendanceDayRepository;
    @Mock EntityManager em;

    @InjectMocks AttendanceServiceImpl service; // ← 생성자에 mock들이 주입됨

    @Test
    void 오늘_첫로그인_저장됨() {
        Long accountId = 1L;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        when(attendanceDayRepository.findByAccount_IdAndLoginDate(accountId, today))
                .thenReturn(Optional.empty());
        when(em.getReference(Account.class, accountId))
                .thenReturn(mock(Account.class));

        service.markLogin(accountId);

        verify(attendanceDayRepository).saveAndFlush(any(Attendance.class));
    }

    @Test
    void 오늘_이미기록있으면_저장안함() {
        Long accountId = 1L;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        when(attendanceDayRepository.findByAccount_IdAndLoginDate(accountId, today))
                .thenReturn(Optional.of(mock(Attendance.class)));

        service.markLogin(accountId);

        verify(attendanceDayRepository, never()).saveAndFlush(any());
    }

//    @Test
//    void 이번달_출석률_계산() {
//        Long accountId = 1L;
//        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Seoul"));
//        LocalDate s = ym.atDay(1), e = ym.atEndOfMonth();
//
//        when(attendanceDayRepository.countDaysInMonth(accountId, s, e)).thenReturn(10);
//
//        double rate = service.getThisMonthRate(accountId);
//
//        assertEquals(10 * 100.0 / ym.lengthOfMonth(), rate, 1e-9);
//    }
}
