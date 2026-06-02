package com.prafta.app.req.req07.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-007: 스케줄 수정 요청 등록 body (POST /appApi/req07/sched-modify).
 *
 * <p>REQ_TYPE='10' INSERT 입력값. plan §4.3 PRAFTA-APP-007-2 명세.
 *
 * <p>식별값 (cmpnyCd/siteCd/userCd) 은 JWT 토큰에서만 도출하며 본 DTO 에 포함하지 않는다 (IDOR 가드).
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedModifyRequest {

    /** 대상 근무일 (YYYYMMDD). */
    private String workYmd;

    /** 본인의 해당 일자 노드 코드. 서버가 검증한다 (P20). */
    private String nodeCd;

    /** 구간 배열 (1~2). 각 slot 의 workSeq + schCd 만 사용. */
    private List<SlotRequest> slots;

    /** 변경 사유 (필수, 최대 500자). */
    private String reqReason;
}
