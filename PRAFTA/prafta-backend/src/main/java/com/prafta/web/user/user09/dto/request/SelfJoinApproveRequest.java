package com.prafta.web.user.user09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소정-09: 셀프가입 승인 요청 (User_09 승인 시트).
 *
 * <p>승인은 "계정 활성화 + 인사정보 보강 + 소정근로 이력 생성"을 한 트랜잭션으로 처리한다.
 * 셀프가입 폼에서 받지 않는 항목(입사일·고용형태·직급·소정근로시간)을 이 시트에서 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SelfJoinApproveRequest {

    /** 승인 대상 사용자 코드 (필수). 사업장/부서는 서버가 대상 행에서 재조회한다(클라 신뢰 금지). */
    private String userCd;

    /** 입사일 (YYYYMMDD, 필수) — 연차 부여 기준이자 소정근로 이력의 적용 시작일. */
    private String hireDate;

    /** 고용형태 [SYS041] REGULAR / CONTRACT / EXECUTIVE (필수). 일용직(DAILY)은 별 계통이라 불가. */
    private String employmentType;

    /** 직급 [COM007] (선택) */
    private String rankCd;

    /** 소정근로 입력 방식 — FULL:풀타임(회사 통상 기준값) / DIRECT:단시간(직접 입력) */
    private String stdWorkType;

    /** 주 소정근로 분 — stdWorkType=DIRECT 일 때 필수(2400 = 주 40시간). FULL 이면 무시된다. */
    private Integer stdWorkWeekMinutes;

    /**
     * 소정근로 사유코드 [SYS083] — stdWorkType=DIRECT 일 때만 의미 있다.
     *
     * <p>NORMAL(통상)과 단축 사유(육아기·임신기·가족돌봄)는 이 시트에서 선택할 수 없다.
     * 단축은 적용 기간이 필수라 승인 후 소정근로시간 관리(User_10)에서 기간과 함께 등록한다.
     */
    private String stdWorkReasonCd;
}
