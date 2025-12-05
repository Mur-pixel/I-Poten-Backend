package com.cygnus.ipoten.userSchedule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = true)
    private LocalDateTime startTime;

    @Column(nullable = true)
    private LocalDateTime endTime;

    private String location;

    private boolean allDay;

    @Column(length = 10)
    private String color; // "#3b82f6" 등 사용자 지정 색상

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // @Enumerated(EnumType.STRING)
    // private ScheduleCategory category; // PERSONAL, WORK, HEALTH, ETC

    // private boolean isPrivate; // 향후 공개/비공개 기능 대비

    // private boolean isRecurring; // 반복 일정 여부
    // private String recurrenceRule; // "FREQ=WEEKLY;BYDAY=MO"
}
