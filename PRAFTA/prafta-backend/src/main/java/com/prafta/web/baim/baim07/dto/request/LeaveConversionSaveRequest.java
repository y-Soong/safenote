package com.prafta.web.baim.baim07.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시간차 1일 환산시간 저장 요청 (LC-02). baim07 POST /conversion.
 *
 * <p>1차 검증(jakarta.validation)으로 형식만 점검하고, 비즈니스 검증(오늘 이후 적용일,
 * R2 유한소수 방어)은 {@code LeaveConversionPolicyServiceImpl.savePolicy}에서 일괄 처리한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveConversionSaveRequest {

    @FieldLabel("적용 시작일")
    @NotBlank
    @Size(max = 8)
    private String applyFromDate;

    // 유효범위 60~1440분(plan §8-②). 유한소수 방어(ATTD_400_192)는 서비스 검증.
    @FieldLabel("1일 환산시간(분)")
    @NotNull
    @Min(60)
    @Max(1440)
    private Integer dailyConvMinutes;
}
