package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/reject-user-attd-requests.
 *
 * PRAFTA-008 - 근태(ATTD_MODIFY / ATTD_CREATE) 요청 반려.
 * 승인(update-user-attd-requests)과 동일한 권위 검증을 거치되, 출퇴근 값을
 * 실제 반영하지 않고 TB_USER_ATTD_REQ 를 REJECTED 로 전이하고
 * TB_USER_ATTD_HIST 에 반려 이력만 남긴다.
 *
 * body 의 키 필드(siteCd / userCd / workYmd / workSeq / nodeCd)는 서버가 보관한
 * REQ row 와 일치해야 하며, 불일치 시 변조로 간주한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RejectUserAttdRequestRequest {

    @NotBlank
    private String reqId;

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

    /** 반려사유 - 필수 입력. */
    @NotBlank
    @Size(max = 500)
    private String rejectReason;
}
