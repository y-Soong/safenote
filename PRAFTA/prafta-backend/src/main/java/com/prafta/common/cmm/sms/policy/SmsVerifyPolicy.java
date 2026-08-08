package com.prafta.common.cmm.sms.policy;

/**
 * 인증번호 <b>검증(대입)</b> 방어 임계값 스냅샷(TB_SMS_SEND_POLICY 단일행 'DEFAULT').
 *
 * <p>{@link SmsSendPolicy}(발송 상한)와 분리한 이유: 검증 경로는 정책행을 <b>잠그지 않고</b> 읽으며
 * 발송 상한 컬럼을 하나도 쓰지 않는다. 판정용 record 를 공유하면 잠금 구간의 SELECT 가
 * 불필요하게 넓어지고, 반대로 검증 경로가 발송 축과 결합된다.
 *
 * <p>★record 매핑은 SELECT 컬럼 순서에 의존한다(메모리 {@code feedback_mybatis_record_column_order}).
 *    {@code SmsVerifyLimitMapper.xml} 의 {@code selectVerifyPolicy} SELECT 순서와
 *    아래 컴포넌트 순서를 <b>항상 함께</b> 유지할 것.
 *
 * @param verifyFailLimit 인증번호 대입 실패 허용 횟수(도달 시 잠금). 0 이하 = 무제한(권장하지 않음)
 * @param verifyLockSec   대입 상한 도달 시 검증 거부 시간(초). ★영구 무효화가 아니다 — sec N-2 참조.
 *                        0 이하는 "무제한 잠금" 이 아니라 <b>설정 오류</b>로 취급한다({@link #effectiveLockSec()})
 * @param verifyHourLimit 번호(HMAC)별 시간당 <b>실패한</b> 검증 시도 상한(건). 0 이하 = 무제한
 */
public record SmsVerifyPolicy(
    int verifyFailLimit
    , int verifyLockSec
    , int verifyHourLimit
) {

    /**
     * 잠금 시간 기본값(초). ★DDL DEFAULT({@code VERIFY_LOCK_SEC})와 <b>반드시 같은 값</b>으로 유지할 것.
     *
     * <p>★★[4차 / qa R-6] 3차의 180 에서 60 으로 낮췄다.
     *    잠금이 코드 TTL(진입점 A 60초 / B 180초) <b>이상</b>이면 잠금이 풀릴 때 코드가 이미 만료되어
     *    "기다렸다가 같은 코드로 재시도" 라는 잠금의 존재 이유가 어느 진입점에서도 성립하지 않는다.
     *    (3차 dev-progress §10-2-1 의 "진입점 B 에서만 성립" 은 <b>사실이 아니다</b> — B 도 성립하지 않았다.)
     *    60 으로 낮추면 B(TTL 180초)에서는 실제로 성립하고, A(TTL 60초)에서는 여전히 재발송이 필요하다.
     *    <p>브루트포스 방어의 본체는 잠금이 아니라 <b>시간당 실패 시도 상한</b>({@code VERIFY_HOUR_LIMIT})이다.
     *    잠금은 "한 코드에 대한 연속 대입 속도" 만 늦추는 보조 장치이므로 짧아도 방어가 약해지지 않는다.
     */
    public static final int DEFAULT_LOCK_SEC = 60;

    /** 대입 실패 허용 횟수 기본값. ★DDL DEFAULT 와 동일. */
    public static final int DEFAULT_FAIL_LIMIT = 5;

    /** 시간당 실패 시도 상한 기본값. ★DDL DEFAULT 와 동일. */
    public static final int DEFAULT_HOUR_LIMIT = 30;

    /**
     * 정책행 부재/조회 실패 시 폴백.
     *
     * <p>★발송 상한은 정책행이 없으면 fail-open(차단 안 함)이지만, <b>검증(대입) 방어는 fail-open 하지 않는다</b>.
     *    대입 방어는 "문자를 받지 않고도 계정을 탈취할 수 있는" 경로를 막는 유일한 관문이라
     *    (sec C-2 — {@code sms-auth-checks} 통과가 비밀번호 재설정의 유일 관문),
     *    시드 누락 한 번으로 사라지면 안 된다.
     * <p>값은 마이그레이션 DDL 의 DEFAULT 와 동일하게 유지할 것
     *    ({@code prafta-sms-ppurio-3/4}). 정상 경로의 유일 출처는 DB 다.
     * <p>★[4차 / qa R-4 정정] 3차 주석의 "이 경로는 부팅 검증(N-9)이 즉시 드러낸다" 는 <b>사실이 아니었다</b> —
     *    부팅 검증이 {@code VERIFY_*} 컬럼과 {@code TB_SMS_VERIFY_ATTEMPT} 를 보지 않았고,
     *    게이트 OFF 면 아예 실행되지도 않았다. 4차에서 둘 다 보완했으므로 이제는 성립한다
     *    ({@code SmsPolicyBootstrapValidator} — 게이트 상태와 무관하게 검증 스키마를 확인한다).
     */
    public static SmsVerifyPolicy fallback() {
        return new SmsVerifyPolicy(DEFAULT_FAIL_LIMIT, DEFAULT_LOCK_SEC, DEFAULT_HOUR_LIMIT);
    }

    /**
     * 실효 잠금 시간(초). [4차 / sec T-8]
     *
     * <p>{@code VERIFY_LOCK_SEC <= 0} 을 "잠금 해제를 하지 않는다" 로 해석하면
     * {@code FAIL_CNT} 가 영원히 5 로 남아 <b>2차 N-2(영구 무효화)가 그대로 부활</b>한다.
     * 공격자가 표적 번호로 오답 5회만 던지면 피해자의 계정 복구가 무기한 차단되는 바로 그 상태다.
     * {@code GLOBAL_HOUR_LIMIT} 하한(1)과 같은 논리로 <b>하한을 코드에서 강제</b>한다.
     *
     * <p>★"0 = 무제한" 규칙을 여기만 반대로 적용하는 이유: 다른 임계값의 0 은 "덜 막는다"(안전 방향)지만
     *    잠금 시간의 0 은 "영원히 막는다"(위험 방향)라 의미가 정반대다.
     */
    public int effectiveLockSec() {
        return verifyLockSec > 0 ? verifyLockSec : DEFAULT_LOCK_SEC;
    }
}
