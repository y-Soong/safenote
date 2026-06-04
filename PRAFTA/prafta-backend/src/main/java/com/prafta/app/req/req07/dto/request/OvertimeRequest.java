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

    /**
     * 구간 배열 (1~2). 각 slot 의 workSeq + startDate/startTime/endDate/endTime 사용.
     * prafta-043: 초과근무 유형(OT_TYPE) 전면 파기 — 유형 입력/저장 없음.
     */
    private List<SlotRequest> slots;

    /** 신청 사유 (필수, 최대 500자). */
    private String reqReason;

    /**
     * prafta-app-009: 'N' 결재선 결재자 순서 목록(1차). 비면 presetId 폴백.
     * 'Y'/즉시승인 케이스에서는 무시/빈 허용(서버가 노드 SELF_ATTD_APPRV_YN 으로 분기).
     */
    private List<String> approverUserCds;

    /** prafta-app-009: approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null). */
    private String presetId;
}
