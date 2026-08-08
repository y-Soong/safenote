package com.prafta.common.cmm.sms.policy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.sms.policy.SmsAxisCount;
import com.prafta.common.cmm.sms.policy.SmsSendPolicy;

/**
 * SMS 발송 상한 정책 조회 + 다축 카운트 매퍼(SMS2-B1·B3).
 *
 * <p>★패키지가 {@code .mapper} 로 끝나야 {@code MainApplication} 의
 *    {@code @MapperScan("com.prafta.**.**.mapper")} 에 잡힌다.
 * <p>★XML 은 인터페이스 FQN 과 1:1 경로에 둔다
 *    ({@code resources/com/prafta/common/cmm/sms/policy/mapper/SmsSendPolicyMapper.xml}).
 *    {@code DBConfig} 의 {@code classpath*:mapper/**}{@code /*.xml} 패턴은 {@code com/prafta/...} 를 잡지 못하며,
 *    실제로는 MyBatis 가 인터페이스 FQN 과 같은 경로의 XML 을 자동 로딩하는 규칙으로 매핑된다.
 *    경로를 어기면 기동 시 {@code BindingException} 이다.
 */
@Mapper
public interface SmsSendPolicyMapper {

    /**
     * 정책행을 <b>배타 잠금</b>하며 조회(TOCTOU 봉인의 핵심).
     *
     * <p>이 SELECT ... FOR UPDATE 가 전역 직렬화 지점이다. 같은 트랜잭션 안에서
     * [정책 읽기 → 다축 카운트 → 인증코드 INSERT] 를 수행하므로, 동시 요청이 카운트를 동시에 통과하는
     * 레이스가 구조적으로 불가능해진다.
     *
     * <p>행이 없으면 null → 호출자는 <b>차단하지 않고</b> 통과시킨다(fail-open).
     */
    SmsSendPolicy selectPolicyForUpdate();

    /**
     * 정책행 조회(<b>잠금 없음</b>) — [3차 / qa Q-2] 번호 창 선검사 전용.
     *
     * <p>정책행 X 잠금을 잡기 전에 55초 창을 먼저 확인해 조기 반려하기 위한 읽기다.
     * 무인증 EP 로 요청이 몰려도 대부분 여기서 끊겨 전역 직렬화 지점의 경합이 줄어든다.
     *
     * <p>★이 값으로 최종 판정하지 않는다. 통과한 요청은 {@link #selectPolicyForUpdate()} 이후
     *    잠금 구간 안에서 반드시 재검사한다(TOCTOU).
     *
     * <p>행이 없으면 null → 선검사를 건너뛴다(fail-open).
     */
    SmsSendPolicy selectPolicyNoLock();

    /**
     * 번호 축 — 연속 발송 간격 창(초) 안의 발송 건수. 1 이상이면 "방금 보냈다".
     *
     * @param windowSec 정책값 {@code PHONE_WINDOW_SEC}
     */
    int selectPhoneWindowCnt(@Param("mblNoHmac") String mblNoHmac
            , @Param("purposeCd") String purposeCd
            , @Param("windowSec") int windowSec);

    /** 번호 축 — 최근 1시간/1일 발송 건수(목적별). */
    SmsAxisCount selectPhoneAxisCount(@Param("mblNoHmac") String mblNoHmac
            , @Param("purposeCd") String purposeCd);

    /** IP 축 — 최근 1시간/1일 발송 건수(목적 무관 전체). */
    SmsAxisCount selectIpAxisCount(@Param("ipHash") String ipHash);

    /** 사용자 축 — 최근 1시간/1일 발송 건수(목적 무관 전체). */
    SmsAxisCount selectUserAxisCount(@Param("userCd") String userCd);

    /**
     * 전역 축 — 최근 1시간 <b>실발송</b> 건수(과금 방어 대상).
     * ★{@code SEND_DATE} 가 NULL 인 PENDING 행을 놓치지 않도록 보정된 판정식이다(XML 주석 참조).
     */
    int selectGlobalSentCnt();

    /**
     * 전역 축 — 게이트 OFF 드라이런용 카운트(SKIPPED 포함).
     * 게이트 OFF 면 전 행이 SKIPPED 라 실발송 카운트는 항상 0 이 되어 드라이런이 무의미해진다.
     */
    int selectGlobalDryRunCnt();

    /**
     * 킬스위치 발동(멱등 가드 포함 — 이미 'Y' 면 0행).
     *
     * @return 실제로 발동시킨 행 수(1 = 이번 호출이 발동시킴, 0 = 이미 발동 상태)
     */
    int fireKillSwitch(@Param("reason") String reason);
}
