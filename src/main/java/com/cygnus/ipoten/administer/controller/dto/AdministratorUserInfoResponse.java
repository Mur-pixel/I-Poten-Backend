package com.cygnus.ipoten.administer.controller.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdministratorUserInfoResponse {
    private Long accountId;
    private String nickname;
    private String email;

    public String getEmail(){
        return maskEmail(email);
    }

    /** 이메일의 로컬 파트 마지막 4글자를 **** 로 치환 */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 4) {
            return "****@" + domainPart;
        }

        String visible = localPart.substring(0, localPart.length() - 4); // 앞부분
        return visible + "****@" + domainPart;
    }
}
