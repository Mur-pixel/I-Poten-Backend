package com.cygnus.iptn.schedule.controller.response_form;

import com.cygnus.iptn.schedule.service.response.DeleteScheduleResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteScheduleResponseForm {

    private final Long scheduleId;
    private final boolean deleted;

    public static DeleteScheduleResponseForm from(DeleteScheduleResponse response) {
        return new DeleteScheduleResponseForm(response.getScheduleId(), response.isDeleted());
    }
}
