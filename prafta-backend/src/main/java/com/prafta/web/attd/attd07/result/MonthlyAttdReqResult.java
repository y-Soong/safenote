package com.prafta.web.attd.attd07.result;

public record MonthlyAttdReqResult(
	/** 회사코드 */
    String cmpnyCd

    /** 신청고유ID */
    , String reqId

    /** 사업장코드 */
    , String siteCd

    /** 소속부서 */
    , String nodeCd

    /** 사용자코드 */
    , String userCd

    /** 신청유형 [SYS032] */
    , String reqType

    /** 신청유형명 (공통코드 변환) */
    , String reqTypeNm

    /** 신청상태 [SYS033] */
    , String reqStatus

    /** 신청상태명 (공통코드 변환) */
    , String reqStatusNm
    
    /** 근무 구간 */
    , String workSeq

    /** 근무일 (yyyyMMdd) */
    , String workYmd

    /** 출근일자 (yyyyMMdd) */
    , String checkInDate

    /** 출근시간 (HHmm) */
    , String checkInTime

    /** 퇴근일자 (yyyyMMdd) */
    , String checkOutDate

    /** 퇴근시간 (HHmm) */
    , String checkOutTime
    
    /** 사유 */
    , String reqReason

    /** 입력일시 */
    , String insertDate
) {
}
