package com.prafta.platform.sms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.application.result.SmsPolicyResult;
import com.prafta.platform.sms.application.result.SmsSendStatResult;

/**
 * Platform_05(SMS 발송 관리) 매퍼 — 조회/임계값 수정/킬스위치 해제.
 *
 * <p>★전역 상한 소진율에 쓰는 카운트는 여기서 다시 만들지 않고
 *    {@code SmsSendPolicyMapper.selectGlobalSentCnt()} 를 재사용한다.
 *    판정과 화면이 다른 규칙으로 세면 "화면은 여유 있다는데 킬스위치가 걸린" 상황이 생긴다.
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

    /** 임계값 수정. */
    int updatePolicy(SmsPolicyUpdateParam param);

    /**
     * 킬스위치 수동 해제(멱등 가드 포함 — 이미 'N' 이면 0행).
     * ★{@code KILL_SWITCH_AT}/{@code KILL_SWITCH_REASON} 은 지우지 않는다(마지막 발동 이력 보존).
     */
    int releaseKillSwitch(@Param("operatorUserCd") String operatorUserCd);
}
