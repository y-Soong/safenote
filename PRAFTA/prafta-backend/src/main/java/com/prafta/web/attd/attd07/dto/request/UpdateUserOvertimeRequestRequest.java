package com.prafta.web.attd.attd07.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/update-user-overtime-requests.
 * Registers one or more overtime (TB_USER_OVERTIME_MGMT) rows for a worker on a given work day.
 *
 * Each element of {@link #overtimes} represents one OT segment. Validation:
 *   - all four (startDate/startTime/endDate/endTime) must be present and formatted.
 *   - server-side will additionally enforce the "allowed window" rule
 *     (overtime is only allowed in standardized-work-time minus scheduled-time).
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserOvertimeRequestRequest {

    @NotBlank
    private String userCd;

    @NotBlank
    private String siteCd;

    /** Optional - node code where the OT was performed. */
    private String nodeCd;

    @NotBlank
    @Pattern(regexp = "^[0-9]{8}$")
    private String workYmd;

    /** Optional - link to the related TB_USER_ATTD_MGMT row. */
    private String attdId;

    /** Optional - link to the related TB_USER_ATTD_REQ row when OT is registered through a worker request. */
    private String reqId;

    @NotEmpty
    @Valid
    private List<OvertimeItemRequest> overtimes;

    @Size(max = 500)
    private String reqReason;

    /**
     * 소정-07 - 근로자 명시 청구 확인 값 ('Y' 만 확인으로 인정).
     *
     * <p>육아기·가족돌봄 근로시간 단축 기간의 연장근로는 사업주가 요구할 수 없고 근로자가 명시적으로
     * 청구한 경우에만 가능하다(위반 시 1천만원 이하 벌금). 단축 기간이 아닌 근로자(대다수)에게는
     * 값이 무엇이든 아무 영향이 없다 — 게이트 진입 자체가 없다.
     *
     * <p><b>additive 필드</b>: 구버전 클라이언트가 미전송하면 null → 확인 없음 → 단축 기간 한정 거부
     * (ATTD_400_201). 허용이 아니라 거부가 기본값인 fail-safe 방향이다.
     */
    @Size(max = 1)
    private String reducedWorkOtClaimYn;
}
