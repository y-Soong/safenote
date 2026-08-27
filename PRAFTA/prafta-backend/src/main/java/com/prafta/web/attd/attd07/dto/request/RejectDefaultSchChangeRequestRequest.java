package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/reject-default-sch-requests (PRAFTA-003, 기본근무타입-승인제).
 *
 * <p>기본 근무타입 변경 요청(REQ_TYPE='14') 반려. body 의 키 필드(siteCd/userCd)는 서버가 보관한
 * REQ row 와 일치해야 하며, 불일치 시 변조로 간주한다.
 *
 * <p>{@code RejectUserAttdRequestRequest} 는 workYmd 가 {@code @NotBlank @Pattern} 필수라
 * WORK_YMD 가 null 인 이 요청 유형에는 재사용할 수 없다 — 전용 DTO 를 둔다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RejectDefaultSchChangeRequestRequest {

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
