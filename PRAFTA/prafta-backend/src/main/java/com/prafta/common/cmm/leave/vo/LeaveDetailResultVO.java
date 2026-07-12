package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 직원별 연차 상세(attd09) 조회 결과 집합(서비스 산출).
 *
 * <p>프론트 계약: {@code { user, legalSummary, nonLegalSummary, appliedLeaveTypes, grantHistory }}.
 */
@Getter
@Builder
public class LeaveDetailResultVO {

    /** 직원 헤더 정보 */
    private final LeaveDetailUserHeaderVO user;

    /** 법정 휴가 요약 (부여/사용/잔여 + 임박 만료일) */
    private final LeaveSummaryVO legalSummary;

    /** 법정외 휴가 요약 (부여/사용/잔여) */
    private final LeaveSummaryVO nonLegalSummary;

    /**
     * 신청형 휴가(사용자 신청 LEAVE_TYPE='01') 타입별 잔여 현황.
     * 법정/법정외 그룹과 합산하지 않고 별도 섹션으로 노출(타입별 한도/사용/잔여).
     */
    private final List<AppliedLeaveTypeVO> appliedLeaveTypes;

    /** 부여 이력 (GRANT_DATE 내림차순) */
    private final List<LeaveGrantHistoryRowVO> grantHistory;

    /**
     * LC-07(표기): 현재(오늘) 기준 1일 환산시간(분, 기본 480). FE 가 잔여/사용/부여 일수를
     * "N일 H시간 M분"으로 조립하는 분모(기존 필드 불변 — additive).
     */
    private final int convMinutes;

    /**
     * LC-07(표기): 시간차(02/03/04) CONFIRMED 사용 분 합계(전 기간). FE "시간차 사용 N시간 M분"
     * 원본 표기용 — 차감 일수 합계와 별개(잔여/부여 수치 무관).
     */
    private final int hourlyUsedMinutes;
}
