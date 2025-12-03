//package com.cygnus.ipoten.config;
//
//import com.cygnus.ipoten.account.entity.LoginType;
//import com.cygnus.ipoten.account.service.AccountService;
//import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
//import com.cygnus.ipoten.administer.service.AdministratorService;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class AdministratorInitializer {
//
//    private final AdministratorService administratorService;
//    private final AccountProfileService accountProfileService;
//    private final AccountService accountService;
//    @Value("${admin.first-admin-email:}")
//    private String adminEmail;
//    @Value("${admin.first-admin-nickname:}")
//    private String adminNickname;
//    @Value("${admin.first-admin-logintype:}")
//    private LoginType adminLoginType;
//
//    @PostConstruct
//    public void initAdmin() {
//        if (isBlank(adminEmail) || isBlank(adminNickname)) {
//            log.info("[AdministratorInitializer] skipped: admin.* not set");
//            return;
//        }
//        try {
//            administratorService.createAdminIfNotExists(adminEmail, adminNickname, adminLoginType);
//            log.info("[AdministratorInitializer] executed with {}", adminEmail);
//        } catch (IllegalArgumentException e) {
//            log.error("[AdministratorInitializer] invalid loginType: {}", adminLoginType, e);
//        } catch (Exception e) {
//            // PostConstruct에서 예외 throw 하면 부팅 자체가 실패할 수 있음
//            log.error("[AdministratorInitializer] admin bootstrap failed", e);
//        }
//    }
//
//    private boolean isBlank(String s) { return s == null || s.isBlank(); }
//}
