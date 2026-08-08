package com.prafta.common.cmm.sms.policy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.sms.policy.SmsVerifyPolicy;

/**
 * 인증번호 <b>검증(대입)</b> 방어 매퍼 — 3차 / sec N-2 · N-3, 4차 / sec T-2 · T-3 · T-11.
 *
 * <p>★{@code SmsSendPolicyMapper}(발송 축)와 <b>의도적으로 분리</b>했다.
 *    검증 시도를 발송 카운트에 섞으면 발송 상한이 검증 트래픽으로 오염된다(sec N-3 명시 금지).
 *    저장소도 다르다 — 검증 시도는 {@code TB_SMS_VERIFY_ATTEMPT}, 발송은 {@code TB_SMS_AUTH_CODE}.
 *
 * <p>★★[4차 / sec T-2] {@code TB_SMS_VERIFY_ATTEMPT} 에는 이제 <b>실패한 시도만</b> 적재한다.
 *    3차는 모든 시도를 적재하고 상한을 걸어, 공격자가 표적 번호로 30회를 소모시키면
 *    <b>정답을 가진 정상 사용자까지</b> 코드 조회 이전에 반려됐다(표적 DoS).
 *    실패한 시도만 세면 브루트포스 방어(오답 30회/시간)는 그대로이면서
 *    정답을 아는 사용자는 상한과 무관하게 통과하므로 표적 봉쇄가 원천 불성립이다.
 *
 * <p>★패키지가 {@code .mapper} 로 끝나야 {@code MainApplication} 의
 *    {@code @MapperScan("com.prafta.**.**.mapper")} 에 잡힌다.
 *    XML 은 인터페이스 FQN 과 1:1 경로에 둔다
 *    ({@code resources/com/prafta/common/cmm/sms/policy/mapper/SmsVerifyLimitMapper.xml}).
 */
@Mapper
public interface SmsVerifyLimitMapper {

    /**
     * 검증 방어 임계값 조회(<b>잠금 없음</b>).
     * 행이 없으면 null → 호출자가 {@link SmsVerifyPolicy#fallback()} 을 쓴다(검증 방어는 fail-open 하지 않는다).
     */
    SmsVerifyPolicy selectVerifyPolicy();

    /**
     * 최근 {@code windowSec} 초 안의 해당 번호(HMAC) <b>실패</b> 검증 시도 건수.
     * ★목적 무관 전체(목적 교체 우회 차단).
     */
    int countFailedVerifyAttempts(@Param("mblNoHmac") String mblNoHmac
            , @Param("windowSec") int windowSec);

    /**
     * <b>실패한</b> 검증 시도 1건 적재(상한 판정 재료).
     *
     * <p>★[4차 / sec T-3] "그 번호에 최근 발급된 코드가 존재할 때" 만 적재한다.
     *    3차는 무인증 EP 에서 요청당 무조건 INSERT 였다 → 번호를 회전시키면 상한이 전혀 걸리지 않은 채
     *    행이 무제한 증식했다(상한이 번호별이라 매번 카운트 0). 코드가 발급된 적 없는 번호에는
     *    애초에 맞힐 대상이 없어 브루트포스가 성립하지 않으므로, 세지 않아도 방어에 구멍이 생기지 않는다.
     *
     * @return 적재된 행 수(0 = 해당 번호에 최근 코드가 없어 적재하지 않음)
     */
    int insertFailedVerifyAttempt(@Param("mblNoHmac") String mblNoHmac
            , @Param("purposeCd") String purposeCd
            , @Param("codeLookbackSec") int codeLookbackSec);

    /**
     * 잠금 시간이 지난 대입 카운터를 되돌린다(sec N-2 핵심).
     *
     * <p>{@code FAIL_LOCKED_AT + lockSec} 이 지난 행의 {@code FAIL_CNT} 를 0 으로,
     * {@code FAIL_LOCKED_AT} 을 NULL 로 되돌려 <b>같은 코드로 재시도 가능</b>하게 한다.
     *
     * <p>★{@code UPDATE_NO}/{@code UPDATE_DATE} 를 절대 건드리지 않는다(인증 창 4개 기산점 보호).
     *
     * @return 잠금이 해제된 행 수
     */
    int resetExpiredVerifyLock(@Param("mblNoHmac") String mblNoHmac
            , @Param("purposeCd") String purposeCd
            , @Param("lockSec") int lockSec);

    /**
     * 검증 시도 테이블 존재/컬럼 정합 확인용 프로브(행을 읽지 않는다). [4차 / qa R-4b]
     *
     * <p>기동 검증이 3차·4차 DDL 미적용 환경을 <b>기동 시점에</b> 잡아내기 위한 것이다.
     * 3차까지는 부팅 검증이 발송 정책행만 봐서, {@code VERIFY_*} 컬럼과
     * {@code TB_SMS_VERIFY_ATTEMPT} 가 없는 환경도 조용히 통과한 뒤
     * <b>첫 검증 요청에서 1054/1146 → 500</b> 으로 터졌다.
     *
     * @return 항상 0. 테이블/컬럼이 없으면 예외가 난다(그것이 이 메서드의 목적이다)
     */
    int probeVerifyAttemptTable();
}
