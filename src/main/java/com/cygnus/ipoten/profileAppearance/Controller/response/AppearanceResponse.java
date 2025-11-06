package com.cygnus.ipoten.profileAppearance.Controller.response;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.profileAppearance.Entity.ProfileAppearance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppearanceResponse {
    private String photoUrl;
    private String nickname; // AccountProfile의 닉네임
    private String email;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PhotoResponse{
        private String photoUrl;
    }

    public static AppearanceResponse of(ProfileAppearance pa, AccountProfile ap, String presignedUrl) {
        String photoUrl = (pa.getPhotoKey() != null && !pa.getPhotoKey().isEmpty())
                ? presignedUrl
                : "/images/default_profile.png";

        return AppearanceResponse.builder()
                .photoUrl(photoUrl)
                .nickname(ap.getNickname())
                .email(ap.getEmail())
                .build();
    }
}