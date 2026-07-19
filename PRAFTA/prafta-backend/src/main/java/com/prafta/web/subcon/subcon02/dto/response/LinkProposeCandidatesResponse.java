package com.prafta.web.subcon.subcon02.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon02.result.MySiteResult;
import com.prafta.web.subcon.subcon02.result.RelationCmpnyResult;

import lombok.Builder;
import lombok.Value;

/**
 * 연동 제안 후보 응답(PRAFTA-SUBCON-T2 §5-2).
 *
 * <p>cmpnyList = 관계 ACCEPTED 상대 회사, siteList = 내 활성 사업장(미러 포함 — 재제안 허용).
 * 루프 판정은 이 목록에서 하지 않는다(제안 시 서버 최종 검증).
 */
@Value
@Builder
public class LinkProposeCandidatesResponse {
    List<RelationCmpnyResult> cmpnyList;
    List<MySiteResult> siteList;
}
