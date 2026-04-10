package com.cygnus.ipoten.infrastructure.external.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsSesEmailServiceImpl implements EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    private static final String BRAND_COLOR = "#3b82f6";
    private static final String BRAND_GRADIENT = "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)";

    @Override
    public void sendInterviewResultNotification(String to, Long interviewId) {
        try {
            String subject = "[I-Ptn] AI 면접 평가 리포트가 도착했습니다";
            String htmlBody = buildNotificationEmail(interviewId);
            sendEmail(to, subject, htmlBody);
            log.info("✅ 리포트 알림 메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("❌ 리포트 알림 메일 발송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void sendErrorNotification(String to, Long interviewId) {
        try {
            String subject = "[I-Ptn] AI 면접 평가 처리 지연 안내";
            String htmlBody = buildErrorEmail(interviewId);
            sendEmail(to, subject, htmlBody);
            log.info("✅ 오류 알림 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("❌ 오류 알림 발송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void sendSignupWelcomeEmail(String to, String nickname) {
        try {
            String subject = "[I-Ptn] 회원가입을 진심으로 환영합니다";
            String htmlBody = buildSignupEmail(nickname);
            sendEmail(to, subject, htmlBody);
            log.info("✅ 웰컴 메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("❌ 웰컴 메일 발송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void sendWithdrawalConfirmationEmail(String to, String nickname) {
        try {
            String subject = "[I-Ptn] 회원 탈퇴 처리가 완료되었습니다";
            String htmlBody = buildWithdrawalEmail(nickname);
            sendEmail(to, subject, htmlBody);
            log.info("✅ 탈퇴 확인 메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("❌ 탈퇴 확인 메일 발송 실패: {}", e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().charset("UTF-8").data(subject).build())
                        .body(Body.builder().html(Content.builder().charset("UTF-8").data(htmlBody).build()).build())
                        .build())
                .build();
        sesClient.sendEmail(request);
    }

    private String getHeader() {
        return "<div style='text-align: center; padding: 40px 0;'><img src='https://i-poten.com/assets/Logo.png' alt='I-Ptn' style='height: 32px; width: auto;'></div>";
    }

    private String getFooter() {
        return "<div style='margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e7eb; text-align: center;'>" +
               "<p style='font-size: 13px; color: #9ca3af; margin: 0;'>본 메일은 발신 전용이며, 회신되지 않습니다.<br>" +
               "문의사항: <a href='mailto:support@i-poten.com' style='color: #3b82f6; text-decoration: none;'>support@i-poten.com/a></p>" +
               "<p style='font-size: 12px; color: #d1d5db; margin-top: 12px;'>© 2025 I-Ptn. All rights reserved.</p></div>";
    }

    private String buildNotificationEmail(Long interviewId) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <body style="font-family: 'Pretendard', sans-serif; background-color: #f9fafb; margin: 0; padding: 40px 20px; color: #1f2937;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; padding: 40px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);">
                    %s
                    <div style="text-align: center;">
                        <h1 style="font-size: 24px; font-weight: 800; color: #111827; margin-bottom: 16px;">AI 면접 분석이 완료되었습니다</h1>
                        <p style="font-size: 16px; color: #4b5563; line-height: 1.6; margin-bottom: 32px;">방금 진행하신 면접의 평가 결과가 생성되었습니다.<br>나의 역량을 확인하고 한 단계 더 성장해 보세요!</p>
                        <div style="background-color: #f3f4f6; border-radius: 12px; padding: 24px; margin-bottom: 32px; text-align: left;">
                            <h3 style="font-size: 14px; font-weight: 700; color: #374151; margin: 0 0 12px;">📊 리포트 하이라이트</h3>
                            <ul style="margin: 0; padding-left: 20px; font-size: 14px; color: #6b7280; line-height: 1.8;">
                                <li>질문별 상세 피드백 및 모범 답안 첨삭</li>
                                <li>시각화된 육각형 역량 지표 차트</li>
                                <li>전문가 수준의 전체 면접 총평</li>
                            </ul>
                        </div>
                        <a href="https://i-poten.com/vue-ai-interview/ai-interview/result/%d" 
                           style="display: inline-block; padding: 16px 48px; background: %s; color: #ffffff; text-decoration: none; border-radius: 50px; font-size: 16px; font-weight: 700;">결과 확인하기</a>
                    </div>
                    %s
                </div>
            </body>
            </html>
            """.formatted(getHeader(), interviewId, BRAND_GRADIENT, getFooter());
    }

    private String buildErrorEmail(Long interviewId) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <body style="font-family: 'Pretendard', sans-serif; background-color: #f9fafb; margin: 0; padding: 40px 20px;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; padding: 40px; border: 1px solid #fee2e2;">
                    %s
                    <div style="text-align: center;">
                        <div style="font-size: 48px; margin-bottom: 20px;">⚠️</div>
                        <h1 style="font-size: 22px; font-weight: 800; color: #991b1b; margin-bottom: 16px;">평가 처리 중 기술적 문제가 발생했습니다</h1>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.6; margin-bottom: 24px;">불편을 드려 죄송합니다. AI 평가 엔진에서 일시적인 오류가 발생하여 현재 복구 중에 있습니다.</p>
                        <div style="background-color: #fef2f2; color: #b91c1c; padding: 12px; border-radius: 8px; font-size: 13px; font-family: monospace;">참조 코드: #%d</div>
                    </div>
                    %s
                </div>
            </body>
            </html>
            """.formatted(getHeader(), interviewId, getFooter());
    }

    private String buildSignupEmail(String nickname) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <body style="font-family: 'Pretendard', sans-serif; background-color: #f9fafb; margin: 0; padding: 40px 20px;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 24px; padding: 48px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05);">
                    %s
                    <div style="text-align: center;">
                        <h1 style="font-size: 26px; font-weight: 800; color: #111827; margin-bottom: 8px;">환영합니다, %s님!</h1>
                        <p style="font-size: 17px; color: #3b82f6; font-weight: 600; margin-bottom: 32px;">JobSpoon의 새로운 여정이 시작되었습니다.</p>
                        <div style="text-align: left; background-color: #fafafa; border: 1px solid #f0f0f0; border-radius: 16px; padding: 28px; margin-bottom: 32px;">
                            <div style="margin-bottom: 20px;">
                                <b style="color: #111827; display: block; margin-bottom: 4px;">🚀 스마트한 면접 준비</b>
                                <span style="font-size: 14px; color: #6b7280;">최신 AI 기술을 활용하여 실전과 같은 긴장감 속에서 연습하세요.</span>
                            </div>
                            <div>
                                <b style="color: #111827; display: block; margin-bottom: 4px;">🎯 정교한 역량 분석</b>
                                <span style="font-size: 14px; color: #6b7280;">나의 답변을 분석하여 개선점과 합격 팁을 정밀하게 가이드해 드립니다.</span>
                            </div>
                        </div>
                        <a href="https://i-poten.com/vue-ai-interview/ai-interview/select" style="display: inline-block; padding: 18px 56px; background: %s; color: #ffffff; text-decoration: none; border-radius: 12px; font-size: 16px; font-weight: 700;">첫 면접 시작하기</a>
                    </div>
                    %s
                </div>
            </body>
            </html>
            """.formatted(getHeader(), nickname, BRAND_GRADIENT, getFooter());
    }

    private String buildWithdrawalEmail(String nickname) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <body style="font-family: 'Pretendard', sans-serif; background-color: #f9fafb; margin: 0; padding: 40px 20px;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; padding: 48px;">
                    %s
                    <div style="text-align: center;">
                        <h1 style="font-size: 22px; font-weight: 800; color: #111827; margin-bottom: 16px;">회원 탈퇴가 완료되었습니다</h1>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.7; margin-bottom: 32px;">그동안 I-Ptn과 함께해 주셔서 진심으로 감사드립니다.<br>%s님께서 남겨주신 소중한 시간들을 기억하겠습니다.</p>
                        <a href="https://i-poten.com/vue-account/account/signup" style="color: #3b82f6; font-size: 14px; font-weight: 700;">나중에 다시 가입하기</a>
                    </div>
                    %s
                </div>
            </body>
            </html>
            """.formatted(getHeader(), nickname, getFooter());
    }
}