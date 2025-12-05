package com.cygnus.ipoten.userTitle.controller.response;

import com.cygnus.ipoten.userTitle.entity.UserTitle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTitleResponse {
    private Long id;
    private String code;
    private String displayName;
    private String description;
    private boolean isEquipped;
    private LocalDateTime acquiredAt;

    public static UserTitleResponse fromEntity(UserTitle entity) {
        return UserTitleResponse.builder()
                .id(entity.getId())
                .code(entity.getTitleCode().name())
                .displayName(entity.getTitleCode().getDisplayName())
                .description(entity.getTitleCode().getDescription())
                .isEquipped(entity.isEquipped())
                .acquiredAt(entity.getAcquiredAt())
                .build();
    }
}
