package com.prafta.platform.sms.service;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.platform.sms.application.param.SmsHistoryListParam;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;
import com.prafta.platform.sms.dto.response.SmsHistoryListResponse;

/**
 * Platform_05(SMS 발송 관리) 서비스 — 플랫폼 운영자 전용.
 *
 * <p>인가는 {@code PlatformOperatorGateInterceptor} 가 {@code /prafta/platformApi/**} 에서 강제한다
 * (패키지를 {@code com.prafta.platform.*} 아래 두는 것만으로 자동 적용).
 */
public interface PlatformSmsService {

    /** 현황 + 상태 + 임계값 1회 조회. */
    SmsConsoleResponse selectConsole();

    /**
     * 발송 이력 목록 조회(기간 필터 + 서버 페이징).
     *
     * <p>★휴대폰은 <b>서버에서 복호 후 마스킹</b>해 내려보낸다. 인증번호({@code AUTH_CD})·HMAC·IP 해시는
     * 응답에 담지 않는다. 마스킹 조회라 감사 로그(§11.3) 대상이 아니다 — 서버 로그만 남긴다.
     *
     * <p>★★{@code TB_SMS_AUTH_CODE} 에 {@code CMPNY_CD} 가 없어 테넌트 술어를 걸 수 없다.
     * 이 메서드는 <b>{@code /platformApi} 게이트 뒤</b>(플랫폼 운영자 전용)에서만 호출할 것.
     */
    SmsHistoryListResponse selectSendHistory(SmsHistoryListParam param);

    /** 임계값 수정(감사 로그 1건 — 변경 전·후 값 포함). */
    void updatePolicy(SmsPolicyUpdateParam param, AuditContext auditContext);

    /**
     * 킬스위치 수동 해제(감사 로그 1건).
     *
     * @param operatorUserCd 토큰에서 추출한 운영자 코드
     */
    void releaseKillSwitch(String operatorUserCd, AuditContext auditContext);
}
