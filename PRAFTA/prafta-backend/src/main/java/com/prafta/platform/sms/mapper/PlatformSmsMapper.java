package com.prafta.platform.sms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.application.query.SmsHistoryListQuery;
import com.prafta.platform.sms.application.result.SmsHistoryRowResult;
import com.prafta.platform.sms.application.result.SmsPolicyResult;
import com.prafta.platform.sms.application.result.SmsSendStatResult;

/**
 * Platform_05(SMS 발송 관리) 매퍼 — 조회/발송 이력/임계값 수정/킬스위치 해제.
 *
 * <p>★전역 상한 소진율에 쓰는 카운트는 여기서 다시 만들지 않고
 *    {@code SmsSendPolicyMapper.selectGlobalSentCnt()} 를 재사용한다.
 *    판정과 화면이 다른 규칙으로 세면 "화면은 여유 있다는데 킬스위치가 걸린" 상황이 생긴다.
 *
 * <p>★★<b>[방침 변경]</b> 개별 발송 이력은 <b>플랫폼 운영자 콘솔에 한해</b> 노출한다(휴대폰은 서버 마스킹).
 *    단 <b>인증번호({@code AUTH_CD}) · {@code MBL_NO_HMAC} · {@code SEND_IP_HASH} 는 어떤 응답에도 담지 않는다.</b>
 *    이력 statement 는 {@code /platformApi} 게이트 뒤에서만 호출되며,
 *    {@code com.prafta.web.*} / {@code com.prafta.app.*} 어느 컨트롤러에서도 재사용을 금지한다.
 */
@Mapper
public interface PlatformSmsMapper {

    /** 정책 + 킬스위치 상태 조회(잠금 없음 — 화면 표시 전용). */
    SmsPolicyResult selectPolicy();

    /**
     * 정책 + 킬스위치 상태 조회(<b>배타 잠금</b>) — 임계값 수정 트랜잭션 전용. [4차 / sec T-9]
     *
     * <p>비잠금 조회로 {@code before} 를 읽으면 "킬스위치 발동 중 상향 거부" 판정과 UPDATE 사이에
     * 창이 생겨 발동과 저장이 겹칠 때 상향이 통과할 수 있다(킬스위치 무력화 조작).
     * ★화면 조회에는 절대 쓰지 말 것 — 조회가 발송 경로를 직렬화한다.
     */
    SmsPolicyResult selectPolicyForUpdate();

    /**
     * 최근 N시간 발송 상태 분포.
     *
     * @param hours 1(최근 1시간) 또는 24(최근 24시간)
     */
    SmsSendStatResult selectSendStat(@Param("hours") int hours);

    /**
     * 발송 이력 목록(기간 필터 + 서버 페이징, 최신순).
     *
     * <p>★★<b>{@code /platformApi} 게이트 뒤에서만 호출할 것.</b>
     * {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 컬럼 자체가 없어 <b>테넌트 경계를 걸 수단이 없다.</b>
     * 이 statement 를 {@code com.prafta.web.*} / {@code com.prafta.app.*} 컨트롤러 경로에서 호출하면
     * 회사 경계 없이 <b>전 고객사 휴대폰이 새어나간다.</b>
     * 게이트는 어노테이션이 아니라 <b>패키지/경로 기반 인터셉터</b>({@code PlatformOperatorGateInterceptor})다.
     *
     * <p>★{@code AUTH_CD} 는 SELECT 절에 넣지 않는다(6자리 인증번호 평문 = 유효 자격증명).
     */
    List<SmsHistoryRowResult> selectSendHistoryList(SmsHistoryListQuery query);

    /**
     * 발송 이력 건수(페이저 분모).
     *
     * <p>★목록과 <b>완전히 동일한 WHERE fragment</b>({@code historyWhere})를 공유한다 — 불일치 = 페이저 붕괴.
     * <p>★호출 위치 제약은 {@link #selectSendHistoryList} 와 동일하다.
     */
    int selectSendHistoryCount(SmsHistoryListQuery query);

    /** 임계값 수정. */
    int updatePolicy(SmsPolicyUpdateParam param);

    /**
     * 킬스위치 수동 해제(멱등 가드 포함 — 이미 'N' 이면 0행).
     * ★{@code KILL_SWITCH_AT}/{@code KILL_SWITCH_REASON} 은 지우지 않는다(마지막 발동 이력 보존).
     */
    int releaseKillSwitch(@Param("operatorUserCd") String operatorUserCd);
}
