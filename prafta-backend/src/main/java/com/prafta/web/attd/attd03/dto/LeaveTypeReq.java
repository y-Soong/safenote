package com.prafta.web.attd.attd03.dto;

import lombok.Data;

@Data
public class LeaveTypeReq{
	String leaveCd;
	
	// A. 타입구분	
    /** 타입구분 (01:사용자 신청, 02:관리자 부여) */
    String leaveType;

    /** 연차 부여 방식 (leaveType=02일 때 사용) */
    String grantType;

    // B. 기본구분
    /** 연차코드 */
    String leaveNo;

    /** 연차명 */
    String leaveNm;

    /** SYS023 유급구분 */
    String paidType;

    /** SYS024 휴가성격 */
    String leaveNatureType;

    /** 사용여부 (Y/N) */
    String useYn;

    /** 비고(설명) */
    String leaveDesc;

    // C. 사용규칙 - 사용자 신청 타입(leaveType=01)
    /** 최대 신청일수 */
    Integer maxAplyDays;

    /** 연차 사용 단위 */
    String useUnitType;

    /** 사용가능기간 */
    String availTermType;

    /** 기간설정 시작일 (availTermType=03일 때) */
    String availFromDt;

    /** 기간설정 종료일 (availTermType=03일 때) */
    String availToDt;

    // C. 사용규칙 - 관리자 부여 타입(수동부여: leaveType=02 & grantType=02)
    /** 부여일수 */
    Integer grantDays;

    /** 사용가능기간 */
    String adminAvailTermType;

    /** 기간설정 시작일 (adminAvailTermType=03일 때) */
    String adminAvailFromDt;

    /** 기간설정 종료일 (adminAvailTermType=03일 때) */
    String adminAvailToDt;

    // C. 사용규칙 - 관리자 부여 타입(자동부여: leaveType=02 & grantType=01)
    /** 자동 부여 기준일 */
    String grantBaseType;

    /** 실행 시점 (개월 전 1일) */
    Integer grantOffsetMonth;

    // D. 결재 및 증빙
    /** 결재 여부 (Y/N) */
    String aprvUseYn;

    /** 결재 단계 수 (aprvUseYn=Y일 때) */
    Integer aprvStepCnt;

    /** 인사팀 최종 승인 여부 (Y/N, aprvUseYn=Y일 때) */
    String hrFinalAprvYn;

    /** 증빙 여부 (Y/N) */
    String evidenceYn;

    /** 증빙 안내 문구 (evidenceYn=Y일 때) */
    String evidenceGuideMsg;
}
