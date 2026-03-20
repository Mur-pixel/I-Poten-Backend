package com.cygnus.ipoten.interest.mapper;

import com.cygnus.ipoten.interest.controller.response_form.InterestResponseForm;
import com.cygnus.ipoten.interest.controller.response_form.InterestTagResponseForm;
import com.cygnus.ipoten.interest.controller.response_form.MyInterestsResponseForm;
import com.cygnus.ipoten.interest.controller.response_form.UpdateMyInterestsRequestForm;
import com.cygnus.ipoten.interest.service.request.UpdateMyInterestsRequest;
import com.cygnus.ipoten.interest.service.response.InterestResponse;
import com.cygnus.ipoten.interest.service.response.InterestTagResponse;
import com.cygnus.ipoten.interest.service.response.MyInterestsResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterestMapper {

    public UpdateMyInterestsRequest toRequest(UpdateMyInterestsRequestForm requestForm) {
        return UpdateMyInterestsRequest.builder()
                .interestIds(requestForm.getInterestsIds())
                .interestTagIds(requestForm.getInterestTagIds())
                .build();
    }

    public MyInterestsResponseForm toResponseForm(MyInterestsResponse response) {
        return new MyInterestsResponseForm(response.getInterestIds(), response.getInterestTagIds());
    }

    public List<InterestResponseForm> toResponseFormList(List<InterestResponse> responses) {
        return responses.stream()
                .map(this::toResponseForm)
                .toList();
    }

    public InterestResponseForm toResponseForm(InterestResponse response) {
        return InterestResponseForm.builder()
                .id(response.getId())
                .name(response.getName())
                .iconUrl(response.getIconUrl())
                .sortOrder(response.getSortOrder())
                .active(response.isActive())
                .tags(
                        response.getTags().stream()
                                .map(this::toResponseForm)
                                .toList()
                )
                .build();
    }

    public InterestTagResponseForm toResponseForm(InterestTagResponse response) {
        return InterestTagResponseForm.builder()
                .id(response.getId())
                .name(response.getName())
                .sortOrder(response.getSortOrder())
                .active(response.isActive())
                .build();
    }
}
