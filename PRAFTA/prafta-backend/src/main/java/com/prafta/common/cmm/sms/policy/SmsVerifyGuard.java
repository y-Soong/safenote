package com.prafta.common.cmm.sms.policy;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.sms.policy.mapper.SmsVerifyLimitMapper;
import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증번호 <b>검증(대입)</b> 방어 가드 — 3차 / sec N-2 · N-3 · N-4, 4차 / sec T-2 · T-3 · T-8 · qa R-2 · R-4.
 *
 * <p>진입점 A({@code /comApi/baseinfo/sms-auth-checks}, {@code @NoAuth})와
 * B({@code /appApi/mypage/mobile/verify})가 공통으로 사용한다.
 * 두 EP 모두 "검증 통과 = 권한 획득" 이라 대입 방어가 유일 관문이다
 * (A → 비밀번호 재설정 / B → {@code PHONE_CHANGE_AUTH} scope 토큰).
 *
 * <h3>★★판정 순서 — "코드 매칭이 먼저다" (4차 / sec T-2)</h3>
 * <pre>
 *   ① beforeVerify()      : 만료된 대입 잠금 해제 + 실패 허용 횟수 반환   ← 상한을 <b>보지 않는다</b>
 *   ② (호출부) 코드 매칭
 *   ③ 일치  → 시간당 상한과 <b>무관하게</b> 성공
 *   ④ 불일치 → afterFailedVerify() : 실패 시도 적재 + 시간당 상한 초과면 반려
 * </pre>
 * 3차는 ①에서 모든 시도를 세고 상한을 걸었다. 그 결과 무인증 공격자가 표적 번호로 시간당 30회를
 * 소모시키면 <b>정답을 가진 정상 사용자까지</b> 코드 조회 이전에 반려됐고, 슬라이딩 창이라
 * 2분에 1회만 던져도 무기한 유지됐다(재발송으로도 탈출 불가 = 2차보다 후퇴).
 * <p><b>상한을 "실패한 시도" 에만 걸면 브루트포스 방어(오답 30회/시간)는 그대로면서
 * 정답을 아는 사용자는 절대 막히지 않아 표적 DoS 가 원천 불성립이다.</b>
 * 부수적으로 "모르면 막는다" 원칙의 부작용(§조회 실패 시 보수적 차단)도 정상 사용자에게 닿지 않는다 —
 * 정상 사용자는 ④ 경로에 진입하지 않기 때문이다.
 *
 * <h3>★{@link SmsRateLimitGuard}(발송 축)를 재사용하지 않는 이유</h3>
 * 발송 축 카운트를 오염시키기 때문이다(sec N-3 명시 금지).
 * 검증 시도가 발송 상한을 소진하면 "인증번호를 틀렸더니 재발송이 막히는" 역전이 생기고,
 * 반대로 발송 상한이 소진된 상태에서 정상 사용자의 검증까지 막힌다.
 * 저장소도 다르다 — 검증 시도는 {@code TB_SMS_VERIFY_ATTEMPT}, 발송은 {@code TB_SMS_AUTH_CODE}.
 *
 * <h3>★★{@code @Transactional} 을 붙이지 않는다 — 그리고 <u>호출자</u>도 확인해야 한다</h3>
 * 이 가드의 쓰기(시도 적재 / 잠금 해제)와 호출부의 카운터 증가는 <b>statement 단위로 즉시 커밋</b>되어야 한다.
 * 트랜잭션을 두르면 호출부가 던지는 {@link ApiException} 롤백으로 카운터가 통째로 사라져 방어가 무력화된다.
 * <p>★★[4차 / qa R-2] <b>자기 자신에게 {@code @Transactional} 이 없는 것만으로는 부족하다.</b>
 * 트랜잭션이 있는 <u>상위 메서드</u>가 이 가드를 호출하면 가드의 쓰기가 그 트랜잭션에 참여하고,
 * 상위가 {@code rollbackFor = Exception.class} 이면 오답 시 <b>전부 롤백</b>된다.
 * 실제로 {@code LoginServiceImpl.verifyPhoneAuth}(PRAFTA-036)가 정확히 그 상태였고,
 * 그 EP 에서는 {@code FAIL_CNT} 증가·시도 적재가 모두 사라져 6자리 <b>무제한 대입</b>이 가능했다.
 * <b>성공하면 커밋되므로 실동작 테스트로는 절대 잡히지 않는다 — 코드 검사로만 발견된다.</b>
 * <p>→ 이 가드(또는 {@code increase*SmsFailCnt})를 직·간접으로 호출하는 메서드에 {@code @Transactional} 을
 * 추가할 때는 반드시 {@code noRollbackFor = ApiException.class} 를 함께 붙일 것
 * (선례: {@code PlatformLocationServiceImpl.verifySmsAuth}, {@code LoginServiceImpl.verifyPhoneAuth}).
 *
 * <h3>★게이트와 무관하게 항상 동작한다</h3>
 * 대입 공격은 문자를 받지 않고도 성립하므로 {@code prafta.sms.enabled} 게이트나 킬스위치로 가려선 안 된다.
 *
 * <h3>★[4차 / qa R-4] 조회 실패는 500 이 아니라 보수적 기본값으로 흡수한다</h3>
 * 3차는 예외를 잡지 않아 DDL 미적용 환경에서 1054/1146 이 그대로 <b>500</b> 으로 표출됐고
 * {@code SmsVerifyPolicy.fallback()} 은 도달 불가능한 죽은 코드였다.
 * 이제 각 쿼리를 개별로 감싸되 <b>fail-open 하지 않는다</b> — 상한 조회가 실패하면 "상한 미도달" 이 아니라
 * <b>"상한 초과"</b> 로 간주한다(모르면 막는다). 위 판정 순서 덕분에 이 보수적 기본값은
 * 오답을 낸 요청에만 적용되고 정상 사용자에게는 닿지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsVerifyGuard {

    /** 실패 시도 상한의 판정 창(초) = 1시간. 상한 건수만 정책값이고 창 길이는 축 정의 자체다. */
    private static final int VERIFY_WINDOW_SEC = 3600;

    /**
     * "그 번호에 코드가 발급된 적이 있는가" 를 판정하는 소급 구간(초) = 1일. [4차 / sec T-3]
     * ★코드 TTL 은 1~3분이라 1일보다 오래된 코드는 대입 가치가 없다. 인덱스 구간을 좁게 유지하려는 값이며
     *   차단 임계값이 아니라 축 정의라 정책 테이블로 올리지 않는다.
     */
    private static final int CODE_LOOKBACK_SEC = 86400;

    private final SmsVerifyLimitMapper smsVerifyLimitMapper;

    /**
     * <b>검증 시도 직전</b>에 호출한다. 만료된 대입 잠금을 해제하고,
     * 호출부가 SQL 바인딩에 쓸 <b>대입 실패 허용 횟수</b>를 돌려준다.
     *
     * <p>★★여기서 시간당 상한을 보지 않는다(4차 / sec T-2). 상한 판정은 코드가 <b>불일치할 때</b>
     *    {@link #afterFailedVerify} 에서만 한다. 순서를 되돌리면 표적 DoS 가 즉시 부활한다.
     *
     * @param mblNoHmac 검증 대상 휴대폰 HMAC
     * @param purposeCd 인증 목적(SELF_JOIN / MOBILE_CHANGE)
     * @return 대입 실패 허용 횟수({@code VERIFY_FAIL_LIMIT}). 호출부가 조회/증가 SQL 에 바인딩한다
     */
    public int beforeVerify(String mblNoHmac, String purposeCd) {

        SmsVerifyPolicy policy = loadPolicy();

        // 만료된 대입 잠금 해제(sec N-2).
        //   ★반드시 코드 매칭 <b>이전</b>이어야 한다 — 잠금이 풀린 코드를 되살린 뒤에 매칭해야 하기 때문이다.
        int lockSec = policy.effectiveLockSec();
        if (lockSec != policy.verifyLockSec()) {
            // [4차 / sec T-8] 0 이하는 "무제한 잠금"(=영구 무효화 부활)이라 하한을 강제한다.
            log.error("[SMS검증상한] VERIFY_LOCK_SEC={} 는 유효하지 않다 - 기본 {}초를 적용한다."
                    + " 0 이하를 그대로 두면 FAIL_CNT 가 영원히 풀리지 않아 2차 N-2(영구 무효화)가 부활한다.",
                    policy.verifyLockSec(), lockSec);
        }
        try {
            int unlocked = smsVerifyLimitMapper.resetExpiredVerifyLock(mblNoHmac, purposeCd, lockSec);
            if (unlocked > 0) {
                log.info("[SMS검증상한] 대입 잠금 해제(시간 경과) - 목적={}, 해제행={}, 잠금시간={}초",
                        purposeCd, unlocked, lockSec);
            }
        } catch (RuntimeException e) {
            // ★잠금 해제 실패는 "덜 풀린다"(엄격 방향)이라 그대로 진행한다. 500 으로 흐름을 죽이지 않는다.
            //   단 FAIL_LOCKED_AT 컬럼 부재(1054)라면 잠금이 영원히 풀리지 않으므로 error 로 남긴다.
            log.error("[SMS검증상한] 대입 잠금 해제 실패 - 목적={}, 예외={}."
                    + " prafta-sms-ppurio-3(FAIL_LOCKED_AT) 적용 여부를 확인할 것",
                    purposeCd, e.getClass().getSimpleName());
        }

        return effectiveFailLimit(policy.verifyFailLimit());
    }

    /**
     * <b>코드가 불일치한 직후</b>에 호출한다. 실패 시도를 적재하고 시간당 상한을 강제한다.
     *
     * <p>★호출부는 이 메서드보다 <b>먼저</b> {@code FAIL_CNT} 를 증가시켜야 한다.
     *    여기서 예외를 던지면 그 뒤의 카운터 증가가 실행되지 않기 때문이다.
     *
     * @param mblNoHmac           검증 대상 휴대폰 HMAC
     * @param purposeCd           인증 목적(SELF_JOIN / MOBILE_CHANGE)
     * @param tooManyAttemptsCode 시간당 실패 시도 상한 초과 시 사용자에게 내려줄 에러코드(진입점별로 다르다)
     * @throws ApiException 시간당 실패 시도 상한 초과
     */
    public void afterFailedVerify(String mblNoHmac, String purposeCd, ApiErrorCode tooManyAttemptsCode) {

        SmsVerifyPolicy policy = loadPolicy();
        if (policy.verifyHourLimit() <= 0) {
            // 0 이하 = 무제한(운영자가 명시적으로 끈 경우). 적재도 무의미하므로 생략한다.
            return;
        }

        // ★★[5차 / sec U-2·qa N-2] 반드시 "판정 → 적재" 순서다. 되돌리지 말 것.
        //   4차는 "적재 → 판정" 이었는데, 그러면 상한을 넘긴 뒤에도 요청마다 1행씩 계속 쌓인다.
        //   결과 ① 무인증 EP 로 표적 번호에 행 무제한 증식 ② 슬라이딩 창이 영원히 드레인되지 않아
        //   공격이 지속되는 한 그 번호의 실패 검증이 무기한 차단된다(3차가 의도적으로 피했던 형태의 재발).
        //   판정을 먼저 하면 상한 도달 시점에 적재가 멈추므로 창이 정상적으로 흘러간다.
        //   방어력은 동일하다 — 상한 이하 구간에서는 모든 실패가 그대로 적재된다.

        // ① 시간당 실패 시도 상한 판정(적재보다 먼저).
        int attempts;
        try {
            attempts = smsVerifyLimitMapper.countFailedVerifyAttempts(mblNoHmac, VERIFY_WINDOW_SEC);
        } catch (RuntimeException e) {
            // ★★fail-open 하지 않는다(4차 / qa R-4). 상한을 셀 수 없으면 "초과" 로 간주한다 —
            //   모르면 막는다. 이 경로는 오답을 낸 요청에만 닿으므로 정상 사용자는 영향이 없다.
            log.error("[SMS검증상한] 실패 시도 조회 불가 - 보수적으로 차단한다. 목적={}, 예외={}",
                    purposeCd, e.getClass().getSimpleName());
            throw new ApiException(tooManyAttemptsCode);
        }

        if (attempts >= policy.verifyHourLimit()) {
            // ★초과 시에는 적재하지 않고 반려한다(위 U-2 주석 참조).
            // ★번호 평문·인증번호는 로그에 남기지 않는다(건수/상한만).
            log.warn("[SMS검증상한] 검증 시도 차단 - 목적={}, 실패시도={}, 상한={}(시간당)",
                    purposeCd, attempts, policy.verifyHourLimit());
            throw new ApiException(tooManyAttemptsCode);
        }

        // ② 실패 시도 적재. ★그 번호에 최근 발급된 코드가 있을 때만 적재된다(sec T-3).
        try {
            smsVerifyLimitMapper.insertFailedVerifyAttempt(mblNoHmac, purposeCd, CODE_LOOKBACK_SEC);
        } catch (RuntimeException e) {
            // 적재 실패는 상한을 느슨하게 만들 뿐 인증을 통과시키지는 않는다(호출부가 이미 불일치로 반려 중).
            log.error("[SMS검증상한] 실패 시도 적재 실패 - 목적={}, 예외={}."
                    + " prafta-sms-ppurio-3(TB_SMS_VERIFY_ATTEMPT) 적용 여부를 확인할 것",
                    purposeCd, e.getClass().getSimpleName());
        }
    }

    /**
     * 임계값 조회. 행이 없거나 조회가 실패하면 폴백(=DDL DEFAULT 와 동일 값)을 쓴다.
     *
     * <p>★[4차 / qa R-4] 3차는 {@code null} 만 처리하고 예외를 잡지 않았다.
     *    컬럼 부재(1054)·테이블 부재(1146)는 null 이 아니라 <b>예외</b>라 폴백이 죽은 코드였고,
     *    실제 증상은 검증 5흐름 전부 <b>500</b> 이었다.
     * <p>★발송 상한과 달리 fail-open 하지 않는다 — 대입 방어가 사라지면 계정 탈취가 열린다.
     */
    private SmsVerifyPolicy loadPolicy() {
        try {
            SmsVerifyPolicy policy = smsVerifyLimitMapper.selectVerifyPolicy();
            if (policy != null) {
                return policy;
            }
            log.error("[SMS검증상한] 정책행 부재(TB_SMS_SEND_POLICY.POLICY_ID='DEFAULT') - 폴백 임계값 적용."
                    + " prafta-sms-ppurio-2 의 시드 INSERT 적용 여부를 확인할 것");
        } catch (RuntimeException e) {
            log.error("[SMS검증상한] 정책 조회 실패 - 폴백 임계값 적용. 예외={}."
                    + " prafta-sms-ppurio-3/4(VERIFY_* 컬럼) 적용 여부를 확인할 것",
                    e.getClass().getSimpleName());
        }
        return SmsVerifyPolicy.fallback();
    }

    /**
     * "0 이하 = 무제한" 을 SQL 이 아니라 여기서 흡수한다.
     *
     * <p>매퍼가 {@code AND #{verifyFailLimit} > 0} 같은 <b>파라미터 대 리터럴</b> 비교를 하지 않게 하려는 것이다
     * (드라이버 prepare 모드에 따라 타입 추론이 흔들릴 수 있고, 조건이 매퍼 4곳에 흩어지면 규칙이 갈린다).
     * {@link Integer#MAX_VALUE} 를 넘기면 {@code FAIL_CNT < limit} 은 항상 참,
     * {@code FAIL_CNT >= limit} 은 항상 거짓이 되어 "제한 없음" 과 정확히 같은 동작이 된다.
     */
    private int effectiveFailLimit(int configured) {
        return configured > 0 ? configured : Integer.MAX_VALUE;
    }
}
