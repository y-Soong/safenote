package com.prafta.app.req.req09.dto.response;

/**
 * prafta-app-009-8: 근태 요청 폼 결재선 분기용 컨텍스트 메타 응답.
 *
 * <p>{@code GET /appApi/req09/approval-context?workYmd=YYYYMMDD} 응답. 신청 폼(스케줄수정/근태보정/
 * 초과근무)이 진입 시 호출하여 결재선 섹션 노출/숨김을 분기한다.
 *
 * <ul>
 *   <li>{@code selfApprvYn='N'} → 일반 근로자 결재 필요. 폼에 결재선 섹션 노출(결재자 필수).</li>
 *   <li>{@code selfApprvYn='Y'} + {@code isNodeAdmin=false} → 자체근태승인 부서 일반 근로자. 결재선 섹션
 *       숨김 + "노드 관리자 승인" 안내(서버가 결재라인 미INSERT, 승인 요망 PUSH).</li>
 *   <li>{@code selfApprvYn='Y'} + {@code isNodeAdmin=true} → 신청자=노드 관리자. 결재선 섹션 숨김 +
 *       "즉시 승인" 안내(서버가 즉시 자동승인 D4).</li>
 * </ul>
 *
 * <p>식별값은 JWT 도출(IDOR). 키명은 FE 가 그대로 소비하므로 임의 변경 금지.
 */
public record ApprovalContextResponse(
      String selfApprvYn
    , boolean isNodeAdmin
) {
}
