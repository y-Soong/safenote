package com.prafta.app.req.req07.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-007: 초과근무 신청 등록 body (POST /appApi/req07/overtime).
 *
 * <p>REQ_TYPE='03' 고정 (초과근무 생성 요청). 수정은 본 작업 범위 외 (원본 요청서 명시).
 * plan §4.3 PRAFTA-APP-007-4 명세.
 */
@Getter
@Setter
@NoArgsConstructor
public class OvertimeRequest {

    /** 대상 근무일 (YYYYMMDD). */
    private String workYmd;

    /** 본인의 해당 일자 노드 코드. */
    private String nodeCd;

    /** 구간 배열 (1~2). 각 slot 의 workSeq + startDate/startTime/endDate/endTime + otType 사용. */
    private List<SlotRequest> slots;

    /** 신청 사유 (필수, 최대 500자). */
    private String reqReason;
}
