package com.prafta.common.cmm.stdwork.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 소정-02: 소정근로시간 이력 등록/정정 커맨드.
 *
 * <p>호출부(소정-03 User_01 계정 생성, 소정-04 셀프가입 승인, 소정-10 관리 화면)는
 * 반드시 <b>토큰에서 도출한</b> cmpnyCd / actorNo 를 넣는다(클라 바디 신뢰 금지 — IDOR 차단).
 */
@Getter
@Builder
public class StdWorkHoursSaveCommand {

    /** 회사 코드 (토큰 gv_cmpnyCd 출처) */
    private final String cmpnyCd;

    /** 대상 사용자 코드 */
    private final String userCd;

    /** 적용 시작일 (YYYYMMDD, 필수) */
    private final String applyStrDate;

    /** 적용 종료일 (YYYYMMDD, null = 무기한. 단축 사유는 필수) */
    private final String applyEndDate;

    /** 주 소정근로 분 (필수, 2400 = 주 40시간) */
    private final Integer weekStdMinutes;

    /** 사유코드 [SYS083] (필수) */
    private final String reasonCd;

    /** 사유 상세 (선택) */
    private final String reasonDetail;

    /** 작업자 코드 (토큰 gv_userCd 출처, INSERT_NO/UPDATE_NO 에 기록) */
    private final String actorNo;
}
