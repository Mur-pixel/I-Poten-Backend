package com.cygnus.ipoten.userLevel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_level")
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long accountId;

    private int level;
    private int exp;
    private int totalExp;

    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 경험치 추가 */
    public void addExp(int amount){
        this.exp += amount;
        this.totalExp += amount;
        checkLevelUp();
        this.updatedAt = LocalDateTime.now();
    }

    /** 레벨업 체크 */
    private void checkLevelUp(){
        // 예시: 레벨업 기준 = level * 100
        while (exp >= level * 100) {
            exp -= level * 100;
            level++;
        }
    }

    /** 초기화 팩토리 */
    public static UserLevel init(Long accountId) {
        return new UserLevel(null, accountId, 1, 0, 0, LocalDateTime.now());
    }
}