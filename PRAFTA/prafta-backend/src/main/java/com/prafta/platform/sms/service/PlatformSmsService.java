package com.prafta.platform.sms.service;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;

/**
 * Platform_05(SMS 발송 관리) 서비스 — 플랫폼 운영자 전용.
 *
 * <p>인가는 {@code PlatformOperatorGateInterceptor} 가 {@code /prafta/platformApi/**} 에서 강제한다
 * (패키지를 {@code com.prafta.platform.*} 아래 두는 것만으로 자동 적용).
 */
public interface PlatformSmsService {

    /** 현황 + 상태 + 임계값 1회 조회. */
    SmsConsoleResponse selectConsole();

    /** 임계값 수정(감사 로그 1건 — 변경 전·후 값 포함). */
    void updatePolicy(SmsPolicyUpdateParam param, AuditContext auditContext);

    /**
     * 킬스위치 수동 해제(감사 로그 1건).
     *
     * @param operatorUserCd 토큰에서 추출한 운영자 코드
     */
    void releaseKillSwitch(String operatorUserCd, AuditContext auditContext);
}
