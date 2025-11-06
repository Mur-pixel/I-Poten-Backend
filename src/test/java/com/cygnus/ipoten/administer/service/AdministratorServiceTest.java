package com.cygnus.ipoten.administer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@TestPropertySource(properties = {
        "admin.secret-id-key=asdf1234",
        "admin.secret-password-key=asdf12345"
})
class AdministratorServiceTest {

    @Autowired
    private AdministratorService adminService;
    @Autowired org.springframework.core.env.Environment env;

    @Test
    void sanity_check_properties() {
        //테스트 프로퍼티가 진짜로 들어왔는지 먼저 확인
        org.assertj.core.api.Assertions.assertThat(env.getProperty("admin.secret-id-key")).isEqualTo("asdf1234");
        org.assertj.core.api.Assertions.assertThat(env.getProperty("admin.secret-password-key")).isEqualTo("asdf12345");
    }
    @Test
    @DisplayName("입력된값이_ADMIN값과_같으면_TRUE")
    void 입력된값이_ADMIN값과_같으면_TRUE(){
        //given
        String input_id= "asdf1234";
        String input_password= "asdf12345";
        //when
        boolean result= adminService.validateKey(input_id,input_password);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(true);
    }

    @Test
    @DisplayName("입력된값이 프로퍼티와 하나라도 다르면 false")
    void 입려된값이_하나라도다르면_FALSE(){
        String input_id= "asdf1234";
        String input_password= "asdf12346";
        boolean result= adminService.validateKey(input_id,input_password);
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(false);
    }
}