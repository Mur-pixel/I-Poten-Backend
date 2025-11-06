package com.cygnus.ipoten.userLevel.controller.response;

import com.cygnus.ipoten.userLevel.entity.UserLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLevelResponse {
    private int level;
    private int exp;
    private int totalExp;
    private LocalDateTime updatedAt;

    public static UserLevelResponse fromEntity(UserLevel entity) {
        return UserLevelResponse.builder()
                .level(entity.getLevel())
                .exp(entity.getExp())
                .totalExp(entity.getTotalExp())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
