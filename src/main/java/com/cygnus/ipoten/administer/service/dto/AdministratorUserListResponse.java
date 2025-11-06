package com.cygnus.ipoten.administer.service.dto;

import com.cygnus.ipoten.administer.controller.dto.AdministratorUserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdministratorUserListResponse {
    private List<AdministratorUserInfoResponse> items;
    private int pageSize;
    private boolean hasNext;
    private Long nextCursor;
}