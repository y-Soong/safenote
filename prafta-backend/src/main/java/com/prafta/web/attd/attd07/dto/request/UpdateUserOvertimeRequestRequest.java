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
 *   - otType must be EXTEND / NIGHT / HOLIDAY.
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
}
