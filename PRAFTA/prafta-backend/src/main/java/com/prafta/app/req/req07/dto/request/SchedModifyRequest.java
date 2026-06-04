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

    /**
     * prafta-app-009: 'N' 결재선 결재자 순서 목록(1차). 비면 presetId 폴백.
     * 'Y'/즉시승인 케이스에서는 무시/빈 허용(서버가 노드 SELF_ATTD_APPRV_YN 으로 분기).
     */
    private List<String> approverUserCds;

    /** prafta-app-009: approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null). */
    private String presetId;
}
