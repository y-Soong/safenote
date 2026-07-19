package com.prafta.common.cmm.push.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.push.vo.DeviceTokenVO;
import com.prafta.common.cmm.push.vo.PushOutboxRowVO;

/**
 * FCM 공용 PUSH 전송 워커 Mapper (PRAFTA-COM-002, consumer).
 *
 * <p>{@code tb_noti_outbox} 의 PENDING 행을 claim → 전송 → 상태전이하고,
 * {@code tb_user_device} 에서 대상 사용자의 활성 토큰을 조회/무효화한다.
 * 생산자(INSERT)는 본 매퍼 책임이 아니다(각 기능이 별도 적재).
 *
 * <p>워커 주체는 {@code 'PUSH_WORKER'}(상수 {@code PushWorkerConst.WORKER_ACTOR}).
 * 멱등: claim(affected=1) + 상태가드(WHERE SEND_STATUS ...)로 중복 발송을 차단한다.
 */
@Mapper
public interface PushOutboxMapper {

    /**
     * 발송 대기 outbox 배치 조회.
     * {@code SEND_STATUS='PENDING' AND DEL_YN='N' AND RETRY_CNT < maxRetry},
     * INSERT_DATE 오름차순(IX_NOTI_OUTBOX_PENDING 활용), 최대 batchSize 건.
     *
     * @param batchSize 1주기 처리 건수 상한
     * @param maxRetry  재시도 한도(이상이면 이미 FAILED 전이되어 제외)
     */
    List<PushOutboxRowVO> selectPendingForSend(@Param("batchSize") int batchSize,
                                               @Param("maxRetry") int maxRetry);

    /**
     * claim: PENDING → SENDING 조건부 전이(크래시복구/중복발송 방지).
     * {@code WHERE CMPNY_CD=#{cmpnyCd} AND NOTI_ID=#{notiId} AND SEND_STATUS='PENDING'}.
     *
     * <p>★테넌트 격리: NOTI_ID 는 회사별 채번이라 전역 유일하지 않다. 회사코드 없이 전이하면
     *   다른 회사의 같은 NOTI_ID 행을 건드린다.
     *
     * @return 1이면 claim 성공(이 워커가 처리), 0이면 이미 다른 주기/상태가 가져감(skip).
     */
    int claimSending(@Param("cmpnyCd") String cmpnyCd,
                     @Param("notiId") String notiId,
                     @Param("actor") String actor);

    /**
     * 대상 사용자의 활성 FCM 디바이스 토큰 조회.
     * {@code WHERE CMPNY_CD=#{cmpnyCd} AND USER_CD=#{targetUserCd} AND DEL_YN='N' AND PUSH_TOKEN IS NOT NULL}.
     *
     * <p>★테넌트 격리: USER_CD 도 회사별 채번이라 전역 유일하지 않다(서로 다른 회사가 같은 USER_CD 를 가진다).
     *   종전 구현은 USER_CD 단일 키로 조회해 <b>다른 회사 동일 USER_CD 사용자의 기기로 푸시가 오배송</b>됐다.
     *
     * @return 활성 디바이스 토큰 목록(0건이면 NO_DEVICE_TOKEN 처리).
     */
    List<DeviceTokenVO> selectDeviceTokens(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("targetUserCd") String targetUserCd);

    /**
     * 발송 성공 전이. SEND_STATUS='SENT', SENT_DATE=NOW().
     * 멱등: {@code WHERE SEND_STATUS IN ('SENDING','PENDING')} (SENT 재처리 방지). CMPNY_CD 동반(테넌트 격리).
     *
     * @return 갱신된 행 수(0이면 이미 종료 상태).
     */
    int markSent(@Param("cmpnyCd") String cmpnyCd,
                 @Param("notiId") String notiId,
                 @Param("actor") String actor);

    /**
     * 발송 영구 실패 전이. SEND_STATUS='FAILED', RETRY_CNT, ERROR_MSG(500자 가드).
     * 멱등: {@code WHERE SEND_STATUS IN ('SENDING','PENDING')}. CMPNY_CD 동반(테넌트 격리).
     *
     * @param retryCnt 최종 재시도 횟수
     * @param errorMsg 실패 사유(서비스에서 500자 substring)
     */
    int markFailed(@Param("cmpnyCd") String cmpnyCd,
                   @Param("notiId") String notiId,
                   @Param("retryCnt") int retryCnt,
                   @Param("errorMsg") String errorMsg,
                   @Param("actor") String actor);

    /**
     * 일시 실패 → PENDING 복귀(다음 주기 재시도). RETRY_CNT+1, ERROR_MSG 보존.
     * claim 으로 SENDING 이 된 행을 다음 주기에 다시 집기 위해 PENDING 으로 되돌린다.
     * 멱등: {@code WHERE SEND_STATUS='SENDING'}. CMPNY_CD 동반(테넌트 격리).
     */
    int incrementRetryAndRevertPending(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("notiId") String notiId,
                                       @Param("errorMsg") String errorMsg,
                                       @Param("actor") String actor);

    /**
     * 무효 토큰 soft-delete (B-2 옵션A). DEL_YN='Y' 마킹.
     * PUSH_TOKEN 자체는 보존(감사). 이후 조회에서 DEL_YN 으로 제외된다.
     * DEVICE_UUID 는 클라이언트 제공값이라 전역 유일을 신뢰할 수 없어 CMPNY_CD 를 동반한다.
     */
    int softDeleteDeviceToken(@Param("cmpnyCd") String cmpnyCd,
                              @Param("deviceUuid") String deviceUuid,
                              @Param("actor") String actor);

    /**
     * 발송 억제(suppress) 여부 판정 (PRAFTA-APP-021-2, enforce).
     *
     * <p>대상 사용자의 tb_user_push_setting 에 마스터 OFF('__MASTER__' USE_YN='N')
     * 또는 해당 NOTI_TYPE OFF(USE_YN='N') 행이 존재하면 1(suppress). opt-out 정합:
     * 행이 없으면 0(발송). EXISTS 단일 쿼리.
     *
     * <p>cmpnyCd 동반(스코프 일관). 설정 테이블 부재 시 SQL 예외 → 서비스에서 graceful 폴백(발송 유지).
     *
     * @return 1이면 발송 억제, 0이면 정상 발송.
     */
    int isSuppressed(@Param("cmpnyCd") String cmpnyCd,
                     @Param("targetUserCd") String targetUserCd,
                     @Param("notiType") String notiType);

    /**
     * 발송 억제 전이. SEND_STATUS='SUPPRESSED' + SENT_DATE(미발송 종료 시각).
     * 멱등: {@code WHERE SEND_STATUS='SENDING'}(claim 후만). RETRY_CNT 미증가.
     *
     * @param errorMsg suppress 사유 라벨(PushWorkerConst.SUPPRESS_REASON)
     * @return 갱신된 행 수(0이면 이미 종료 상태/다른 전이).
     */
    int markSuppressed(@Param("cmpnyCd") String cmpnyCd,
                       @Param("notiId") String notiId,
                       @Param("errorMsg") String errorMsg,
                       @Param("actor") String actor);
}
