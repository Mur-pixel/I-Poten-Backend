package com.cygnus.ipoten.personality_interview;

import com.cygnus.ipoten.personality_interview.entity.PersonalityInterview;
import com.cygnus.ipoten.personality_interview.repository.PersonalityInterviewRepository;
import com.cygnus.ipoten.personality_interview.service.PersonalityInterviewServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class PersonalityServiceTest {

    @InjectMocks
    PersonalityInterviewServiceImpl personalityInterviewService;

    @Mock
    private PersonalityInterviewRepository personalityInterviewRepository;


    @Test
    @DisplayName("인성_면접_데이터들을_가져옵니다")
    void 인성_면접_데이터들을_가져옵니다() {

        // given
        List<PersonalityInterview> personalityInterviews = new ArrayList<>();
        personalityInterviews.add(new PersonalityInterview());
        personalityInterviews.add(new PersonalityInterview());
        personalityInterviews.add(new PersonalityInterview());
        personalityInterviews.add(new PersonalityInterview());
        personalityInterviews.add(new PersonalityInterview());
        personalityInterviews.add(new PersonalityInterview());

        given(personalityInterviewRepository
                .getPersonalityInterviewInInterview(PageRequest.of(0, 6)))
                .willReturn(personalityInterviews);


        // when
        List<PersonalityInterview> personalityInterviews1 = personalityInterviewService.getPersonalityInterviews();

        // then
        Assertions.assertEquals(6, personalityInterviews1.size());
        Assertions.assertEquals(personalityInterviews.size(), personalityInterviews1.size());



    }



}
