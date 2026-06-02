package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/reject-user-overtime-requests.
 *
 * PRAFTA-010 - 초과근무(REQ_TYPE='03', 초과근무생성) 요청 반려.
 * 근태 요청 반려와 달리 TB_USER_ATTD_HIST 에는 이력을 남기지 않으며,
 * TB_USER_ATTD_REQ 의 처리 컬럼(REQ_STATUS / PROCESS_*)만 갱신한다.
 *
 * body 의 siteCd / userCd 는 서버가 보관한 REQ row 와 일치해야 하며,
 * 불일치 시 변조로 간주한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RejectUserOvertimeRequestRequest {

    @NotBlank
    private String reqId;

    @NotBlank
    private String siteCd;

    @NotBlank
    private String userCd;

    /** 반려사유 - 필수 입력. */
    @NotBlank
    @Size(max = 500)
    private String rejectReason;
}
