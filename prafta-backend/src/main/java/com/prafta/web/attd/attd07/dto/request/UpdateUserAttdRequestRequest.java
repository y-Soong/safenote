package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserAttdRequestRequest {

    @NotBlank
    private String reqId;

    private String attdId;

    @NotBlank
    private String siteCd;

    @NotBlank
    private String userCd;

    @NotBlank
    @Pattern(regexp = "^[0-9]{8}$")
    private String workYmd;

    @NotBlank
    @Pattern(regexp = "^[12]$")
    private String workSeq;

    @NotBlank
    private String nodeCd;

    @Pattern(regexp = "^([0-9]{8})?$")
    private String checkInDate;

    @Pattern(regexp = "^([0-9]{4})?$")
    private String checkInTime;

    private String checkInMethod;

    @Pattern(regexp = "^([0-9]{8})?$")
    private String checkOutDate;

    @Pattern(regexp = "^([0-9]{4})?$")
    private String checkOutTime;

    private String checkOutMethod;

    /* values before approve (for HIST BEF*) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String oriCheckInDate;

    @Pattern(regexp = "^([0-9]{4})?$")
    private String oriCheckInTime;

    @Pattern(regexp = "^([0-9]{8})?$")
    private String oriCheckOutDate;

    @Pattern(regexp = "^([0-9]{4})?$")
    private String oriCheckOutTime;

    /* optional admin comment recorded on TB_USER_ATTD_REQ.PROCESS_COMMENT */
    @Size(max = 500)
    private String processComment;
}
