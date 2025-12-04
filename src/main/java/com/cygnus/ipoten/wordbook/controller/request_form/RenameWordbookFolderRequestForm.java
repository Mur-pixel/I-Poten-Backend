package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.RenameWordbookFolderRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameWordbookFolderRequestForm {
    
    @NotBlank(message = "폴더 이름은 공백일 수 없습니다.")
    @Size(max = 50, message = "폴더 이름은 최대 50자입니다.")
    private String folderName;

    public RenameWordbookFolderRequest toRequest(Long accountId, Long folderId) {
        return RenameWordbookFolderRequest.builder()
                .accountId(accountId)
                .folderId(folderId)
                .folderName(folderName)
                .build();
    }
}
