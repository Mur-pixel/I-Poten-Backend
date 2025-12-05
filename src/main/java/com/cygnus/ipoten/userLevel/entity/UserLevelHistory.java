package com.cygnus.ipoten.userLevel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_level_history")
public class UserLevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저 구분
    private Long accountId;

    //달성한 레벨
    private int level;

    //달성 시각
    private LocalDateTime achievedAt;

    /** 팩토리 메소드 **/
    public static UserLevelHistory of(Long accountId, int level){
        return UserLevelHistory.builder()
                .accountId(accountId)
                .level(level)
                .achievedAt(LocalDateTime.now())
                .build();
    }
}