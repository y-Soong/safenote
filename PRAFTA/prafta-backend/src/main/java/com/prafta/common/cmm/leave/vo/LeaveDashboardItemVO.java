package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 현황 대시보드(attd09) 직원 1행(응답용, 가공 완료).
 *
 * <p>매퍼 결과({@link LeaveDashboardRowVO})를 서비스 계층에서 가공한 결과로,
 * 법정/법정외 잔여, 사용률, 근속 텍스트가 산출되어 있다.
 */
@Getter
@Builder
public class LeaveDashboardItemVO {

    /** 사용자 코드 */
    private final String userCd;

    /** 사용자명 (PII 평문, 관리자 화면 한정) */
    private final String userNm;

    /** 소속 부서명 */
    private final String deptNm;

    /** 입사일 (YYYYMMDD) */
    private final String hireDate;

    /** 고용형태[SYS041] */
    private final String employmentType;

    /** 근속 텍스트 (예 "8년 2개월", 입사일 없으면 "-") */
    private final String tenureText;

    /** 경력 인정 개월 수 */
    private final int creditMonths;

    /** 법정 휴가 잔액 (부여/사용/잔여) */
    private final LeaveBalanceVO legal;

    /** 법정외 휴가 잔액 (부여/사용/잔여) */
    private final LeaveBalanceVO nonLegal;

    /** 전체 휴가 잔액 (법정 + 법정외 합산, 부여/사용/잔여) */
    private final LeaveBalanceVO total;

    /** 사용률 (0~100 정수, 법정+법정외 합산 기준) */
    private final int usageRate;

    /**
     * 가불 사용분 (prafta-com-011-7, 표시 전용 MVP — 결정 §5).
     *
     * <p>아직 발생하지 않은(진짜 당겨쓴) 가불 GRANT 의 USED_DAYS 합.
     * 0이면 가불 사용 없음. 정산/회수 등 액션은 없다(결정 §5).
     */
    private final BigDecimal borrowedDays;

    /**
     * PC-07(N8): 행별(대상 사용자) 1일 환산시간(분) — 오늘 기준 개인 분모(480 캡).
     * 산출 불가(교대 등 기본 근무타입 미지정)면 {@code null} — FE 는 480 폴백으로 표기
     * ({@code formatLeaveDays(v, rowConv ?? 480)}).
     */
    private final Integer convMinutes;
}
