package com.prafta.web.attd.attd03.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeRequest {

    @FieldLabel("연차코드")
    @NotBlank
    @Size(max = 20)
    private String leaveCd;
    
    // A. 타입구분    
    /** 타입구분 (01:사용자 신청, 02:관리자 부여) */
    @FieldLabel("타입구분")
    @NotBlank
    @Size(max = 2)
    private String leaveType;

    /** 연차 부여 방식 (leaveType=02일 때 사용) */
    @FieldLabel("연차 부여 방식")
    @Size(max = 2)
    private String grantType;

    // B. 기본구분
    /** 연차코드 */
    @FieldLabel("연차번호")
    @NotBlank
    @Size(max = 20)
    private String leaveNo;

    /** 연차명 */
    @FieldLabel("연차명")
    @NotBlank
    @Size(max = 200)
    private String leaveNm;

    /** SYS023 유급구분 */
    @FieldLabel("유급구분")
    @NotBlank
    @Size(max = 2)
    private String paidType;

    /** SYS024 휴가성격 */
    @FieldLabel("휴가성격")
    @NotBlank
    @Size(max = 2)
    private String leaveNatureType;

    /** 사용여부 (Y/N) */
    @FieldLabel("사용여부")
    @Size(max = 1)
    private String useYn;

    /** 비고(설명) */
    @FieldLabel("비고")
    @Size(max = 500)
    private String leaveDesc;

    // C. 사용규칙 - 사용자 신청 타입(leaveType=01)
    /** 최대 신청일수 */
    @FieldLabel("최대 신청일수")
    @Max(255)
    private Integer maxAplyDays;

    /** 연차 사용 단위 */
    @FieldLabel("연차 사용 단위")
    @Size(max = 2)
    private String useUnitType;

    /** 사용가능기간 */
    @FieldLabel("사용가능기간")
    @Size(max = 2)
    private String availTermType;

    /** 기간설정 시작일 (availTermType=03일 때) */
    @FieldLabel("사용기간 시작일")
    @Size(max = 4)
    private String availFromDt;

    /** 기간설정 종료일 (availTermType=03일 때) */
    @FieldLabel("사용기간 종료일")
    @Size(max = 4)
    private String availToDt;

    // C. 사용규칙 - 관리자 부여 타입(수동부여: leaveType=02 & grantType=02)
    /** 사용가능기간 */
    @FieldLabel("관리자 사용가능기간")
    @Size(max = 2)
    private String adminAvailTermType;

    /** 기간설정 시작일 (adminAvailTermType=03일 때) */
    @FieldLabel("관리자 사용기간 시작일")
    @Size(max = 6)
    private String adminAvailFromDt;

    /** 기간설정 종료일 (adminAvailTermType=03일 때) */
    @FieldLabel("관리자 사용기간 종료일")
    @Size(max = 6)
    private String adminAvailToDt;

    // C. 사용규칙 - 관리자 부여 타입(자동부여: leaveType=02 & grantType=01)
    /** 자동 부여 기준일 */
    @FieldLabel("자동 부여 기준일")
    @Size(max = 2)
    private String grantBaseType;

    /** 실행 시점 (개월 전 1일) - grantBaseType '01','02'일 때만 사용 */
    @FieldLabel("자동부여 실행시점")
    @Max(255)
    private Integer grantOffsetMonth;

    /** 자동부여 지정일 (MMDD) - grantBaseType '03'(부여일지정)일 때만 사용 */
    @FieldLabel("자동부여 지정일")
    @Size(max = 4)
    private String grantAssignMmdd;

    // D. 결재 및 증빙
    /** 결재 여부 (Y/N) */
    @FieldLabel("결재 여부")
    @Size(max = 1)
    private String aprvUseYn;

    /** 증빙 여부 (Y/N) */
    @FieldLabel("증빙 여부")
    @Size(max = 1)
    private String evidenceYn;

    /** 증빙 안내 문구 (evidenceYn=Y일 때) */
    @FieldLabel("증빙 안내 문구")
    @Size(max = 500)
    private String evidenceGuideMsg;
}