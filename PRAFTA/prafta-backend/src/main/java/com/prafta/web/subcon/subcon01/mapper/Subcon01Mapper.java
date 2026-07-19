package com.prafta.web.subcon.subcon01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.subcon.subcon01.application.command.RelationHistInsertCommand;
import com.prafta.web.subcon.subcon01.application.command.RelationInsertCommand;
import com.prafta.web.subcon.subcon01.application.command.RelationProcessCommand;
import com.prafta.web.subcon.subcon01.result.CmpnyExactResult;
import com.prafta.web.subcon.subcon01.result.RelationHistRaw;
import com.prafta.web.subcon.subcon01.result.RelationPartyRaw;
import com.prafta.web.subcon.subcon01.result.RelationResult;

@Mapper
public interface Subcon01Mapper {

    /**
     * 메뉴 버튼 권한 보유 카운트(서버측 역할 게이트 — Subcon_01 메뉴).
     * TB_SYST_AUTH_MENU 에 CMPNY_CD + AUTH_CD + MENU_D_ID + USE_YN='Y' 이고 지정 버튼플래그가 'Y'인 행이 있으면 1 이상.
     * btnType 은 서비스 상수('SRCH'/'NEW'/'SAVE'/'DELT')만 전달하며, XML 에서 고정 컬럼으로 분기(동적 ${} 미사용).
     */
    int selectMenuButtonAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd,
            @Param("menuDId") String menuDId, @Param("btnType") String btnType);

    /**
     * 회사 정확일치 단건 조회(등호 비교만 — LIKE/부분검색 금지, 열거 방지).
     * 자기 회사 제외 + 활성(IFNULL(USE_YN,'N')='Y') 만. 미충족 시 null.
     * 유효 술어는 로그인/일용직 가입 게이트 실측 준용(USE_YN 단일 — CONTRACT_* 미사용, Q5).
     */
    CmpnyExactResult selectCmpnyExact(@Param("cmpnyCd") String cmpnyCd, @Param("gvCmpnyCd") String gvCmpnyCd);

    /** 자사가 당사자(REQ 또는 TGT)인 관계 목록 + 상대 회사명 조인. 상한(limit) 필수. */
    List<RelationResult> selectRelationList(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("limit") int limit);

    /**
     * 관계 당사자/상태 단건(당사자 스코프 — gvCmpnyCd 가 REQ/TGT 아니면 null → 404 존재 비노출).
     */
    RelationPartyRaw selectRelationPartyRaw(@Param("relationId") Long relationId, @Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * 관계 당사자/상태 단건(내부 전용 — 전이 성공 직후 훅/이력용 양측 회사코드 회수.
     * 호출 전 조건부 UPDATE 성공으로 당사자성이 이미 증명된 경로에서만 사용).
     */
    RelationPartyRaw selectRelationById(@Param("relationId") Long relationId);

    /**
     * 관계 이력 목록. 행위자명은 자사 소속만 SQL 에서 해석하고 상대사는 NULL
     * (타 테넌트 인명 DB 밖 반출 차단 — 서비스에서 "상대사 처리" 치환, Q4).
     * 당사자 검증은 selectRelationPartyRaw 선행 호출로 강제한다.
     */
    List<RelationHistRaw> selectRelationHists(@Param("relationId") Long relationId, @Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * 활성 관계(REQUESTED/ACCEPTED) 쌍 카운트 — 방향 불문, FOR UPDATE 잠금(중복 가드 §7-2).
     * DB 백스톱은 ACTIVE_PAIR_KEY UNIQUE(동시 INSERT 는 DuplicateKeyException 으로 수렴).
     */
    int selectActivePairCntForUpdate(@Param("cmpnyCdA") String cmpnyCdA, @Param("cmpnyCdB") String cmpnyCdB);

    /** 관계 INSERT(STATUS='REQUESTED' 고정, useGeneratedKeys 로 relationId 회수). */
    void insertRelation(RelationInsertCommand command);

    /** 수락: REQUESTED→ACCEPTED. 당사자 조건 TGT_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int acceptRelation(RelationProcessCommand command);

    /** 거부: REQUESTED→REJECTED. 당사자 조건 TGT_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int rejectRelation(RelationProcessCommand command);

    /** 취소: REQUESTED→CANCELLED. 당사자 조건 REQ_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int cancelRelation(RelationProcessCommand command);

    /** 해지: ACCEPTED→TERMINATED. 당사자 조건 REQ 또는 TGT=actor(양측 가능). 영향행 수 반환(0=404). */
    int terminateRelation(RelationProcessCommand command);

    /** 관계 이력 INSERT(모든 전이와 동일 트랜잭션 — plan §7-1). */
    void insertRelationHist(RelationHistInsertCommand command);
}
