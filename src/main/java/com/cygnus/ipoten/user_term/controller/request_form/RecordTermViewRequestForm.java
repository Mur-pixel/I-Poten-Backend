package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.entity.enums.ViewSource;
import com.cygnus.ipoten.user_term.service.request.RecordTermViewRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecordTermViewRequestForm {

    @NotNull(message = "source는 필수 값입니다.")
    private ViewSource source; // 용어를 열람한 경로 정보 : DETAIL, SEARCH, QUIZ, SHARE 중 하나

    public RecordTermViewRequest toRecordTermViewRequest(Long accountId, Long termId) {
        return new RecordTermViewRequest(accountId, termId, this.source);
    }

}
