package com.cygnus.iptn.schedule.entity;

import com.cygnus.iptn.account.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "schedule",
        indexes = {
                @Index(name = "idx_schedule_account_start_at", columnList = "account_id, start_at"),
                @Index(name = "idx_schedule_account_end_at", columnList = "account_id, end_at")
        }
)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedule_account"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String memo;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Schedule(Account account, String title, String memo, Instant startAt, Instant endAt, boolean allDay) {
        this.account = account;
        this.title = title;
        this.memo = memo;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String title, String memo, Instant startAt, Instant endAt, boolean allDay) {
        this.title = title;
        this.memo = memo;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
    }
}
