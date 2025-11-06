package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.RenameUserWordbookFolderRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameUserWordbookFolderRequestForm {
    
    @NotBlank(message = "폴더 이름은 공백일 수 없습니다.")
    @Size(max = 50, message = "폴더 이름은 최대 50자입니다.")
    private String folderName;

    public RenameUserWordbookFolderRequest toRequest(Long accountId, Long folderId) {
        return RenameUserWordbookFolderRequest.builder()
                .accountId(accountId)
                .folderId(folderId)
                .folderName(folderName)
                .build();
    }
}
