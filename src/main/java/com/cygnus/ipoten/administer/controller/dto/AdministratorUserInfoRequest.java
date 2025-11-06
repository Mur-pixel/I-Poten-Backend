package com.cygnus.ipoten.administer.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.security.InvalidParameterException;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
public class AdministratorUserInfoRequest {
    @NotNull
    private Integer pageSize;
    @PositiveOrZero
    private Long lastAccountId;

    private static final Set<Integer> ALLOWED_SIZES = Set.of(30,50,70);
    public long normalizedLastId() {
        return lastAccountId == null ? 0L : lastAccountId.longValue(); }
    public void validate() {
        if(!ALLOWED_SIZES.contains(pageSize)) {
            throw new InvalidParameterException("Invalid pagesize ->size must be one of 30,50,70");
        }
    }
}
