package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.RenameWordbookRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameWordbookRequestForm {
    
    @NotBlank(message = "폴더 이름은 공백일 수 없습니다.")
    @Size(max = 50, message = "폴더 이름은 최대 50자입니다.")
    private String wordbookName;

    public RenameWordbookRequest toRequest(Long accountId, Long wordbookId) {
        return RenameWordbookRequest.builder()
                .accountId(accountId)
                .wordbookId(wordbookId)
                .wordbookName(wordbookName)
                .build();
    }
}
