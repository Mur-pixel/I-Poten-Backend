package com.cygnus.ipoten.quiz.entity;

import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="quiz_publication",
        uniqueConstraints=@UniqueConstraint(name="uk_pub_date_part_role",
                columnNames={"scheduled_date","part_type","job_role"}))
@Getter
@NoArgsConstructor
public class QuizPublication {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="scheduled_date", nullable=false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name="part_type", nullable=false, length=32)
    private QuizSetType partType;

    @Enumerated(EnumType.STRING)
    @Column(name="job_role", nullable=false, length=32)
    private JobRole jobRole = JobRole.GENERAL;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="quiz_set_id", nullable=false)
    private QuizSet quizSet;

    @Column(name="is_active", nullable=false, columnDefinition="TINYINT(1) DEFAULT 1")
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public QuizPublication(LocalDate d, QuizSetType p, JobRole r, QuizSet set) {
        this.scheduledDate = d; this.partType = p; this.jobRole = r; this.quizSet = set;
    }
}
