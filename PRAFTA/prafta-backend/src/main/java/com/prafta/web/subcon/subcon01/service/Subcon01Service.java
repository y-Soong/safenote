package com.prafta.web.subcon.subcon01.service;

import com.prafta.web.subcon.subcon01.application.param.CmpnyExactSearchParam;
import com.prafta.web.subcon.subcon01.application.param.RelationCreateParam;
import com.prafta.web.subcon.subcon01.application.param.RelationHistParam;
import com.prafta.web.subcon.subcon01.application.param.RelationListParam;
import com.prafta.web.subcon.subcon01.application.param.RelationProcessParam;
import com.prafta.web.subcon.subcon01.dto.response.CmpnyExactSearchResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationCreateResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationHistResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationListResponse;
import com.prafta.web.subcon.subcon01.dto.response.TerminateSummaryResponse;

public interface Subcon01Service {

    /** 회사 정확일치 조회(3필드 한정, 미존재/비활성/자기회사 → 동일한 cmpny=null). */
    CmpnyExactSearchResponse selectCmpnyExact(CmpnyExactSearchParam param);

    /** 연동 관계 목록(자사 당사자 전 관계 — 프론트 3분류: 연동중/보낸/받은). */
    RelationListResponse selectRelationList(RelationListParam param);

    /** 연동 관계 이력(당사자 검증 + 상대사 행위자 마스킹). */
    RelationHistResponse selectRelationHists(RelationHistParam param);

    /** 해지 영향 요약(당사자 검증, T1 시점 impacts=[]). */
    TerminateSummaryResponse selectTerminateSummary(RelationHistParam param);

    /** 연동 관계 요청 생성(중복 가드 + HIST 동일 트랜잭션). */
    RelationCreateResponse insertRelationRequest(RelationCreateParam param);

    /** 수락(REQUESTED→ACCEPTED, TGT 소속만). */
    void acceptRelation(RelationProcessParam param);

    /** 거부(REQUESTED→REJECTED, TGT 소속만, 사유 필수 ≤500자). */
    void rejectRelation(RelationProcessParam param);

    /** 취소(REQUESTED→CANCELLED, REQ 소속만). */
    void cancelRelation(RelationProcessParam param);

    /** 해지(ACCEPTED→TERMINATED, 양측 가능, 해지 훅 동일 트랜잭션 호출). */
    void terminateRelation(RelationProcessParam param);
}
