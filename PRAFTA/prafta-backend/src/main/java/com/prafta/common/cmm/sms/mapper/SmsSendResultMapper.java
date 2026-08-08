package com.prafta.common.cmm.sms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * TB_SMS_AUTH_CODE 발송 결과 기록 매퍼(전 흐름 공통).
 *
 * <p>★XML 은 인터페이스 FQN 과 1:1 경로에 둔다:
 *    {@code resources/com/prafta/common/cmm/sms/mapper/SmsSendResultMapper.xml}.
 *    {@code DBConfig} 의 {@code classpath*:mapper/**&#47;*.xml} 패턴은 이 경로를 잡지 않으며,
 *    실제 로딩은 MyBatis 의 "인터페이스와 동일 경로 XML 자동 탐색"으로 이뤄진다(plan §3 T8).
 */
@Mapper
public interface SmsSendResultMapper {

    /**
     * 발송 결과 기록(refKey 단건). PENDING 행만 갱신해 멱등성을 확보한다.
     *
     * <p>★UPDATE_NO / UPDATE_DATE 는 절대 건드리지 않는다 — 인증 통과 시각 전용이며
     *    플랫폼 위치열람 10분 창·앱 검증토큰 5분 창의 기산점이다(plan §3 T3).
     *
     * @param sendRefKey 발송 추적키(INSERT 시 저장한 값)
     * @param sendStatus SENT / FAILED / SKIPPED
     * @param sendMsgKey 벤더 messageKey(없으면 null)
     * @param sendErrCd  실패 코드(없으면 null)
     * @param sendErrMsg 실패 사유(없으면 null, 500자 이내)
     * @return 갱신 행 수(0 이면 이미 결과가 기록되었거나 refKey 불일치)
     */
    int updateSmsSendResult(@Param("sendRefKey") String sendRefKey,
                            @Param("sendStatus") String sendStatus,
                            @Param("sendMsgKey") String sendMsgKey,
                            @Param("sendErrCd") String sendErrCd,
                            @Param("sendErrMsg") String sendErrMsg);
}
