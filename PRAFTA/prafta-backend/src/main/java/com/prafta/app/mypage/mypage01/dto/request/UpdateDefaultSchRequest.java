package com.prafta.app.mypage.mypage01.dto.request;

import java.util.List;

import lombok.Data;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 요청(앱 마이페이지).
 * 대상 회사/사용자/사업장은 세션 토큰에서만 도출한다 — 여기서는 값만 받는다.
 *
 * <p>PRAFTA-002(기본근무타입-승인제, 2026-08-26): 즉시 반영 → 요청 등록 전환에 따라
 * {@code reqReason}(변경 사유, 필수) 필드를 추가한다.
 */
@Data
public class UpdateDefaultSchRequest {
    private String defaultSchCd;

    /** 변경 사유(필수, 최대 500자 — TB_USER_ATTD_REQ.REQ_REASON). */
    private String reqReason;

    /**
     * PRAFTA-002(결재자 선택 UI, 2026-08-27): 결재선 결재자 순서 목록(1차). 비면 presetId 폴백.
     * req07(SchedModifyRequest) 과 동일 계약 — 스코프 가드는 결재 서비스가 수행.
     */
    private List<String> approverUserCds;

    /** PRAFTA-002: approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null). */
    private String presetId;
}
