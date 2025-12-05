package com.cygnus.ipoten.studyroom.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TransferLeadershipRequest {
    private final Long newLeaderId;
}