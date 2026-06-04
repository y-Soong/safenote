package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/approve-sched-modify-requests (PRAFTA-APP-007).
 *
 * <p>스케줄 수정 요청(REQ_TYPE='10') 승인. body 의 키 필드
 * (siteCd / userCd / workYmd / workSeq / nodeCd)는 서버가 보관한 REQ row 와 일치해야
 * 하며, 불일치 시 변조(IDOR)로 간주한다(ATTD_400_005).
 *
 * <p>목표 스케줄 코드(SCH_CD)는 body 로 받지 않는다. 서버가 REQ row 의 SCH_CD 를 권위
 * 값으로 사용해 tb_user_work_plan 의 WORK_PLAN_CD 를 upsert 한다(클라가 보낸 값을
 * 신뢰하면 스케줄 변조 위험).
 */
@Getter
@Setter
@NoArgsConstructor
public class ApproveSchedModifyRequestRequest {

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
}
