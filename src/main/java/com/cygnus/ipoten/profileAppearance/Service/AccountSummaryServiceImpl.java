package com.cygnus.ipoten.profileAppearance.Service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.profileAppearance.Controller.response.AccountSummaryResponse;
import com.cygnus.ipoten.userAttendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountSummaryServiceImpl implements AccountSummaryService {
    private final AccountRepository accountRepository;
    private final AttendanceService attendanceService;

    @Override
    @Transactional(readOnly = true)
    public AccountSummaryResponse getBasicSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        String loginType = account.getAccountLoginType().getLoginType().name();
        int consecutiveAttendanceDays = attendanceService.getConsecutiveDays(accountId);

        return new AccountSummaryResponse(loginType, consecutiveAttendanceDays);
    }
}
