package com.prafta.app.req.req07.application.command;

/**
 * prafta-app-007: tb_user_attd_req INSERT 커맨드 (3 endpoint 공통).
 *
 * <p>3 endpoint (스케줄 수정 / 근태 보정 / 초과근무) 가 동일 INSERT SQL 을 공유한다.
 * 각 endpoint 가 사용하지 않는 컬럼은 null 로 전달한다.
 *
 * <ul>
 *   <li>스케줄 수정 (REQ_TYPE='10'): startDate/startTime/endDate/endTime=null, otType=null, schCd 채움.</li>
 *   <li>근태 보정 (REQ_TYPE='01' or '02'): 시각 4종 채움, otType=null, schCd=null.
 *       REQ_TYPE='02' 면 targetId=기존 ATTD_ID, '01' 이면 null.</li>
 *   <li>초과근무 (REQ_TYPE='03'): 시각 4종 채움, otType 채움, schCd=null, targetId=null.</li>
 * </ul>
 *
 * <p>공통 컬럼: reqId / cmpnyCd / siteCd / userCd / workYmd / nodeCd / workSeq / reqReason / insertNo.
 */
public record AttdReqInsertCommand(
        String reqId
        , String cmpnyCd
        , String siteCd
        , String userCd
        , String reqType
        , String targetId
        , String reqStatus
        , String reqReason
        , String workYmd
        , String nodeCd
        , Integer workSeq
        , String startDate
        , String startTime
        , String endDate
        , String endTime
        , String otType
        , String schCd
        , String insertNo
) {
}
