package com.prafta.web.user.user01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 요청(웹 내정보).
 * 대상 회사/사용자/사업장은 세션 토큰에서만 도출한다 — 여기서는 값만 받는다.
 *
 * <p>PRAFTA-001(기본근무타입-승인제, 2026-08-27): 즉시반영 → 요청등록 전환에 따라
 * 변경 사유(reqReason)가 필수로 추가됐다(TB_USER_ATTD_REQ.REQ_REASON).
 *
 * <p>PRAFTA-002(결재자 선택 UI, 2026-08-27): 결재선 결재자 순서 목록(approverUserCds)/프리셋 ID
 * (presetId) 를 추가한다 — app(UpdateDefaultSchRequest)/req07(SchedModifyRequest) 과 동일 계약.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateMyDefaultSchRequest {
    private String defaultSchCd;
    private String reqReason;

    /** PRAFTA-002: 결재선 결재자 순서 목록(1차). 비면 presetId 폴백. */
    private List<String> approverUserCds;

    /** PRAFTA-002: approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null). */
    private String presetId;
}
