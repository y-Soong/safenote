package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
// prafta-043: 초과근무 유형(OT_TYPE) 전면 파기 — otType 필드/검증 제거.
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single overtime segment element of {@link UpdateUserOvertimeRequestRequest#getOvertimes()}.
 * Field validation is enforced via Bean Validation.
 */
@Getter
@Setter
@NoArgsConstructor
public class OvertimeItemRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{8}$")
    private String startDate;

    @NotBlank
    @Pattern(regexp = "^[0-9]{4}$")
    private String startTime;

    @NotBlank
    @Pattern(regexp = "^[0-9]{8}$")
    private String endDate;

    @NotBlank
    @Pattern(regexp = "^[0-9]{4}$")
    private String endTime;
}
