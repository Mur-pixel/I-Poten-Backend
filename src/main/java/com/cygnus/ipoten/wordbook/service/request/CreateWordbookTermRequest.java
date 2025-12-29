package com.cygnus.ipoten.wordbook.service.request;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateWordbookTermRequest {
    private Long accountId;
    private Long wordbookId;
    private Long termId;
}
