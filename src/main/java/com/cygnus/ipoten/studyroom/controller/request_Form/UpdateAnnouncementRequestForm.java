package com.cygnus.ipoten.studyroom.controller.request_Form;

import com.cygnus.ipoten.studyroom.service.request.UpdateAnnouncementRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateAnnouncementRequestForm {
    private final String title;
    private final String content;

    public UpdateAnnouncementRequest toServiceRequest(){
        return new UpdateAnnouncementRequest(this.title,this.content);
    }
}