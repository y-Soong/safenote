package com.prafta.common.cmm.sms.policy;

import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.error.sms.SmsErrorCode;

/**
 * 발송 상한 판정 컨텍스트. 진입점(A/B/C)이 자기 축 값을 채워 {@link SmsRateLimitGuard} 에 넘긴다.
 *
 * <p>축 규칙
 * <ul>
 *   <li><b>번호 축</b>은 {@code purposeCd} 별로 센다 — 진입점 A·B·C 가 서로 간섭하지 않아야 한다
 *       (앱에서 휴대폰을 바꾸는 중이라고 웹 계정찾기가 막히면 안 된다).</li>
 *   <li><b>IP·사용자 축</b>은 목적과 무관하게 전체를 센다 — 공격자가 목적을 바꿔가며 우회하는 것을 막는다.</li>
 *   <li>{@code ipHash} 또는 {@code userCd} 가 null 이면 <b>그 축은 판정하지 않는다</b>(fail-open).</li>
 * </ul>
 *
 * <p>★[3차 / qa Q-2] {@code windowAlwaysEnforced} 플래그를 제거했다.
 * 번호 창(55초)은 <b>전 진입점에서 게이트와 무관하게 항상 강제</b>된다.
 * 2차는 게이트 OFF 에서 진입점 A·B 의 창까지 드라이런으로 돌렸는데,
 * 진입점 A 는 {@code @NoAuth} 라 서버측 상한이 전면 소멸해
 * 무인증으로 {@code TB_SMS_AUTH_CODE} 무한 증식 + 전역 정책행 직렬화가 가능해졌다(1차 코드는 차단했다).
 * 시간/일/전역 축만 게이트 OFF 에서 드라이런으로 남는다.
 *
 * @param purposeCd  인증 목적 코드(SELF_JOIN / MOBILE_CHANGE / PLATFORM_LOCATION)
 * @param mblNoHmac  수신번호 HMAC(번호 축 키). null 이면 번호 축 스킵
 * @param ipHash     요청 IP 해시({@link SmsClientIpResolver} 결과). null 이면 IP 축 스킵
 * @param userCd     로그인 사용자 코드. 무인증 흐름(A)은 null → 사용자 축 스킵
 * @param windowErrorCode 번호 창(연속 발송 간격) 초과 시 사용자에게 내려줄 에러코드.
 *                        ★진입점 C 는 기존 {@code PLATFORM_400_013} 을 유지해야 무회귀다.
 *                        코드 변환 로직이 진입점마다 흩어지지 않도록 컨텍스트에 실어 보낸다
 */
public record SmsSendContext(
    String purposeCd
    , String mblNoHmac
    , String ipHash
    , String userCd
    , ApiErrorCode windowErrorCode
) {

    /**
     * 진입점 A(무인증 셀프가입·계정찾기·비밀번호재설정) / B(앱 휴대폰 변경)용.
     *
     * <p>번호 창 초과 시 {@link SmsErrorCode#SMS_400_001}.
     */
    public static SmsSendContext of(String purposeCd, String mblNoHmac, String ipHash, String userCd) {
        return new SmsSendContext(
            purposeCd, mblNoHmac, ipHash, userCd, SmsErrorCode.SMS_400_001);
    }

    /**
     * 진입점 C(플랫폼 위치열람)용 — 창 초과 시의 사용자 노출 코드만 기존 값으로 바꾼다.
     *
     * <p>진입점 C 의 1분 창은 SMS 연동 이전부터 존재하던 보안 통제이고(보안 리뷰 V-2),
     * 그 사용자 노출 코드는 {@code PLATFORM_400_013} 이다. 가드 기본 코드({@code SMS_400_001})가
     * 나가면 프론트 문구/분기가 달라져 무회귀가 깨진다.
     *
     * @param windowErrorCode 기존 {@code PLATFORM_400_013}
     */
    public static SmsSendContext ofWithWindowErrorCode(
            String purposeCd, String mblNoHmac, String ipHash, String userCd, ApiErrorCode windowErrorCode) {
        return new SmsSendContext(
            purposeCd, mblNoHmac, ipHash, userCd, windowErrorCode);
    }
}
