package com.prafta.web.user.user01.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 경력 인정 저장 요청 (PRAFTA-017-4).
 * creditList 전량을 delete-and-insert(USE_YN='Y' 교체)한다.
 * cmpnyCd는 서버가 토큰으로 강제하므로 신뢰하지 않는다(IDOR 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCreditRequest {
    private String cmpnyCd;

    @FieldLabel("사용자코드")
    @NotBlank
    @Size(max = 20)
    private String userCd;

    @Valid
    private List<CreditItem> creditList;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreditItem {
        @FieldLabel("인정 개월 수")
        @Min(0)
        private Integer creditMonths;

        /** 경력 인정 사유 유형 [SYS042] — 미입력 시 서버가 'OTHER'로 기본 처리(하위호환). */
        @FieldLabel("사유 유형")
        private String reasonType;

        @FieldLabel("상세 설명")
        @Size(max = 500)
        private String reasonDetail;

        /**
         * 경력인정 이원화(2026-08-21, 지시서 §1-1) — 연차 산정 반영 여부.
         * 'Y'(반영 모드, 기본) / 'N'(일수 모드). 미전송 시 서버가 'Y'로 기본 처리(하위호환).
         */
        @FieldLabel("연차 반영 모드")
        private String leaveCalcYn;

        /** 일수 모드(N) 전용 연간 추가 부여 일수. 반영 모드(Y)에서는 서버가 무시(NULL 강제)한다. */
        @FieldLabel("연간 추가 부여 일수")
        private BigDecimal extraLeaveDays;
    }
}
