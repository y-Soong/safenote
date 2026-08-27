package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/approve-default-sch-requests (PRAFTA-003, 기본근무타입-승인제).
 *
 * <p>기본 근무타입 변경 요청(REQ_TYPE='14') 승인. body 의 키 필드(siteCd/userCd)는 서버가 보관한
 * REQ row 와 일치해야 하며, 불일치 시 변조(IDOR)로 간주한다(ATTD_400_005).
 *
 * <p>이 요청 유형은 특정 근무일에 종속되지 않으므로 workYmd/workSeq/nodeCd 는 body 로 받지 않는다
 * (ApproveSchedModifyRequestRequest 와의 차이 — 웹 신규 탭도 이 계약을 재사용한다).
 * 목표 근무타입 코드(SCH_CD)도 body 로 받지 않는다 — 서버가 REQ row 의 SCH_CD 를 권위 값으로 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApproveDefaultSchChangeRequestRequest {

    @NotBlank
    private String reqId;

    @NotBlank
    private String siteCd;

    @NotBlank
    private String userCd;
}
