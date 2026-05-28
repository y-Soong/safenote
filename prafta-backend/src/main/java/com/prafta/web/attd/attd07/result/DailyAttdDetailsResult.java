package com.prafta.web.attd.attd07.result;

public record DailyAttdDetailsResult(
    /* 사용자/사업장 기본 정보 */
      String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String siteCd
    , String nodeCd
    , String workYmd

    /* 스케줄 정보 (1·2구간) */
    , String schType
    , String plan1Start
    , String plan1End
    , Integer plan1BreakMin
    , String plan2Start
    , String plan2End
    , Integer plan2BreakMin

    /* 근태 1차 */
    , String attd1Id
    , String act1InDate
    , String act1InTime
    , String act1InStdDate    // PRAFTA-015 - 표준화 출근 일자(YYYYMMDD), 자정 넘김 보정 반영
    , String act1InStdTime
    , String act1InMethod
    , String act1OutDate
    , String act1OutTime
    , String act1OutStdDate   // PRAFTA-015 - 표준화 퇴근 일자(YYYYMMDD)
    , String act1OutStdTime
    , String act1OutMethod

    , String oriAct1InDate
    , String oriAct1InTime
    , String oriAct1OutDate
    , String oriAct1OutTime

    /* 근태 2차 */
    , String attd2Id
    , String act2InDate
    , String act2InTime
    , String act2InStdDate    // PRAFTA-015 - 표준화 출근 일자(YYYYMMDD)
    , String act2InStdTime
    , String act2InMethod
    , String act2OutDate
    , String act2OutTime
    , String act2OutStdDate   // PRAFTA-015 - 표준화 퇴근 일자(YYYYMMDD)
    , String act2OutStdTime
    , String act2OutMethod
    
    , String oriAct2InDate
    , String oriAct2InTime
    , String oriAct2OutDate
    , String oriAct2OutTime

    /* PRAFTA-011 - 구간별 외근 플래그.
       해당 구간 ATTD_ID 에 연결된 TB_USER_ATTD_GPS 행이 1건이라도 존재하면 'Y',
       없으면 'N'. GPS 테이블에는 WORK_SEQ 컬럼이 없으므로 구간 구분은 ATTD_ID 로만 한다. */
    , String attd1OutsideYn
    , String attd2OutsideYn
) {
}
