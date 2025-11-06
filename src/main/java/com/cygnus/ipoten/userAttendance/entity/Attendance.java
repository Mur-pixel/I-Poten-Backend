package com.cygnus.ipoten.userAttendance.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 엔티티를 어카운트, Month, attendaceCount

/*
복합 유니크(account_id, login_date)
“하루 여러 번 로그인해도 1번만 카운트” → 같은 (account_id, login_date)가 절대 중복되면 안 됨.
 */

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_day", columnNames = {"account_id", "login_date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @Column(name = "login_date", nullable = false)
    private LocalDate loginDate;
}
