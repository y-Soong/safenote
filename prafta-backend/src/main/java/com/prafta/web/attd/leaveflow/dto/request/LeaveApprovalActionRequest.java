package com.prafta.web.attd.leaveflow.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연차 결재 단계 처리(승인/반려) 요청 (prafta-019-E).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveApprovalActionRequest {

    @FieldLabel("요청ID")
    @NotBlank
    @Size(max = 20)
    private String reqId;

    @FieldLabel("결재단계")
    @NotNull
    private Integer approvalStep;

    @FieldLabel("코멘트")
    @Size(max = 500)
    private String comment;
}
