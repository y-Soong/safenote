package com.prafta.common.cmm.tbmshare.command;

import java.util.List;

/**
 * 연동 회사 지정 해제 커맨드(PRAFTA-SUBCON-T5).
 *
 * <p>{@code shareCmpnyCds} 는 직접 해제 대상 + 하위 캐스케이드 대상을 합친 집합이다.
 * {@code releaseReasonCd} 는 MANUAL(수동) / CASCADE(상위 전파) / RELATION_TERMINATED(관계 해지).
 *
 * <p>참석행(TB_TBM_ATTENDANCE)은 단 한 행도 건드리지 않는다(기존 참석자 유지 — 요청서 §3.1).
 */
public record ShareReleaseCommand(
    String sessionCd
    , List<String> shareCmpnyCds
    , String releaseReasonCd
    , String releasedByCmpnyCd
    , String releasedByUserCd
) {
}
