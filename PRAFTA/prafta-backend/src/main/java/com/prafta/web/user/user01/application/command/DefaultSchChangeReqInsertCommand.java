package com.prafta.web.user.user01.application.command;

/**
 * PRAFTA-001(기본근무타입-승인제, 웹): tb_user_attd_req INSERT 커맨드(REQ_TYPE='14' 전용).
 *
 * <p>기본 근무타입 변경 요청은 특정 근무일(WORK_YMD)/구간(WORK_SEQ)에 종속되지 않으므로
 * 기존 근태 요청(req07) 커맨드를 재사용하지 않고 전용 커맨드를 둔다.
 * WORK_YMD/WORK_SEQ/TARGET_ID/시각 4종은 항상 NULL 로 INSERT 된다.
 *
 * <p>앱 트랙 {@code com.prafta.app.mypage.mypage01.application.command.DefaultSchChangeReqInsertCommand}
 * 와 동형이나, D2 원칙(테이블만 공유, 코드는 트랙별 독립 작성)에 따라 웹 패키지에 독립 작성한다.
 */
public record DefaultSchChangeReqInsertCommand(
        String reqId
        , String cmpnyCd
        , String siteCd
        , String userCd
        , String nodeCd
        , String schCd
        , String reqReason
        , String insertNo
) {
}
