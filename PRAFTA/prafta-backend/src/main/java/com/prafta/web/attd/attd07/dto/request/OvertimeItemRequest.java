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

    // com-013-06 A - 기존 OT 행을 in-place 수정할 때만 전달(관리자 직접수정). 신규 등록이면 null/미전달.
    //   실질 방어는 서버 UPDATE WHERE(cmpny/site/user/attdId/otId) 스코프(0행→거부)가 담당하나,
    //   방어심층/조기거부 목적으로 채번 형식(yyyyMMdd+시퀀스)에 맞춰 영숫자 화이트리스트를 강제한다.
    @Pattern(regexp = "^[0-9A-Za-z]{1,50}$")
    private String otId;

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
