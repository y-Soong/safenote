package com.prafta.app.req.req07.dto.response;

import java.util.List;

/**
 * prafta-app-007: 근태 요청 등록 응답 (3 endpoint 공통).
 *
 * <p>plan §4.3 명세:
 * <ul>
 *   <li>reqId: 채번된 단일 REQ_ID (1·2구간 공유).</li>
 *   <li>reqType: 단일 구간이면 실제 REQ_TYPE 값 ('01', '02', '03', '10').
 *       근태 보정의 2구간이 서로 다른 REQ_TYPE (1구간 행 존재 + 2구간 행 부재) 면 'MIXED'.</li>
 *   <li>reqStatus: '01' 신청 (등록 직후 고정).</li>
 *   <li>workSeqs: INSERT 된 slot 의 workSeq 목록 (등록 순).</li>
 * </ul>
 */
public record RegisterReqResponse(
        String reqId
        , String reqType
        , String reqStatus
        , List<Integer> workSeqs
) {
}
