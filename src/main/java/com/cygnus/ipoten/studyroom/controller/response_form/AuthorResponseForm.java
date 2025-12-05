package com.cygnus.ipoten.studyroom.controller.response_form;

import com.cygnus.ipoten.studyroom.service.response.ListAnnouncementResponse;
import com.cygnus.ipoten.studyroom.service.response.ReadAnnouncementResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 여러 응답에서 재사용될 작성자 정보 Form
@Getter
@RequiredArgsConstructor
public class AuthorResponseForm {
    private final Long id;
    private final String nickname;

    public static AuthorResponseForm from(ListAnnouncementResponse.AuthorResponse author) {
        return new AuthorResponseForm(author.getId(), author.getNickname());
    }

    public static AuthorResponseForm from(ReadAnnouncementResponse.AuthorResponse author) {
        return new AuthorResponseForm(author.getId(), author.getNickname());
    }
}