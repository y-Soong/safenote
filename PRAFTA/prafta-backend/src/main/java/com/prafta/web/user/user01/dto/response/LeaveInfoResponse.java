package com.prafta.web.user.user01.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.web.user.user01.result.ServiceCreditResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근태/연차 정보 조회 응답 (PRAFTA-017-4).
 *
 * - hireDate: 입사일 (YYYY-MM-DD, 없으면 빈 문자열)
 * - employmentType: 고용형태 [SYS041]
 * - creditList: 경력 인정 항목(USE_YN='Y')
 * - creditTotalMonths: 인정 개월 합계
 * - legalTenureBaseDate: 법적 근속 기준일 (HIRE_DATE - 총 인정 개월, YYYY-MM-DD)
 */
@Getter
@Builder
public class LeaveInfoResponse {
    private String hireDate;
    private String employmentType;
    private List<CreditItem> creditList;
    private int creditTotalMonths;
    private String legalTenureBaseDate;

    @Getter
    @Builder
    public static class CreditItem {
        private String creditId;
        private Integer creditMonths;
        private String reasonType;
        private String reasonDetail;
        // 경력인정 이원화(2026-08-21, 지시서 §1-1) — 'Y'(반영 모드)/'N'(일수 모드).
        private String leaveCalcYn;
        // 일수 모드(N) 전용 연간 추가 부여 일수. 반영 모드에서는 NULL.
        private BigDecimal extraLeaveDays;

        public static CreditItem from(ServiceCreditResult result) {
            return CreditItem.builder()
                .creditId(result.creditId())
                .creditMonths(result.creditMonths())
                .reasonType(result.reasonType())
                .reasonDetail(result.reasonDetail())
                .leaveCalcYn(result.leaveCalcYn())
                .extraLeaveDays(result.extraLeaveDays())
                .build();
        }
    }
}
