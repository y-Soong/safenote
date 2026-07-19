package com.prafta.web.subcon.subcon01.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon01.application.command.RelationHistInsertCommand;
import com.prafta.web.subcon.subcon01.application.command.RelationInsertCommand;
import com.prafta.web.subcon.subcon01.application.command.RelationProcessCommand;
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
import com.prafta.web.subcon.subcon01.mapper.Subcon01Mapper;
import com.prafta.web.subcon.subcon01.result.CmpnyExactResult;
import com.prafta.web.subcon.subcon01.result.RelationHistResult;
import com.prafta.web.subcon.subcon01.result.RelationPartyRaw;
import com.prafta.web.subcon.subcon01.result.RelationResult;
import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;
import com.prafta.web.subcon.subcon01.service.RelationTerminationHandler;
import com.prafta.web.subcon.subcon01.service.Subcon01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회사 간 연동 관계(Subcon_01) 서비스.
 *
 * <p>보안 원칙(PRAFTA-SUBCON-T1 §6/§7):
 * 회사 스코프는 JWT 클레임(gvCmpnyCd)만 신뢰, 상태 전이는 조건부 UPDATE(0행=404 존재 비노출),
 * 회사 조회는 정확일치 + 결과 동일화(열거 방지), 이력 행위자는 자사만 실명(Q4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Subcon01ServiceImpl implements Subcon01Service {

    private final Subcon01Mapper subcon01Mapper;

    /**
     * 해지 자동 종결 핸들러(T2/T3/T5 구현체 — T1 시점 0개 = no-op).
     * List 직접 주입은 후보 빈 0개일 때 기동 실패하므로 ObjectProvider 로 빈 스트림을 허용한다.
     */
    private final ObjectProvider<RelationTerminationHandler> terminationHandlers;

    /** 본 화면 메뉴 식별자(서버측 역할 게이트 기준). */
    private static final String MENU_D_ID = "Subcon_01";
    /** 메뉴 버튼 권한 종류(고정 상수 — 동적 컬럼 주입 금지). §5 매핑: 조회/이력=SRCH, 요청=NEW, 수락/거부/취소=SAVE, 해지=DELT. */
    private static final String BTN_SRCH = "SRCH";
    private static final String BTN_NEW = "NEW";
    private static final String BTN_SAVE = "SAVE";
    private static final String BTN_DELT = "DELT";

    /** 목록 조회 상한(전수조회 방지 — user06 PAGE_SIZE 준용). */
    private static final int LIST_LIMIT = 500;

    /** 처리 코멘트(거부 사유 등) 최대 길이(DDL varchar(500) 정합). */
    private static final int COMMENT_MAX_LEN = 500;

    /** 상대사 행위자 마스킹 라벨(Q4 확정 — 자사만 실명). */
    private static final String OTHER_CMPNY_ACTOR_LABEL = "상대사 처리";

    /** HIST ACTION_TYPE 상수(§7-1 전이표). */
    private static final String ACTION_REQUEST = "REQUEST";
    private static final String ACTION_ACCEPT = "ACCEPT";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_CANCEL = "CANCEL";
    private static final String ACTION_TERMINATE = "TERMINATE";

    /** 로그 위조 방지용 외부 입력 정제 — 개행 제거 + 50자 상한 (보안 리뷰 SEC-ADV-1 반영) */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\r\\n]", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
    }

    @Override
    public CmpnyExactSearchResponse selectCmpnyExact(CmpnyExactSearchParam param) {
        // 서버측 메뉴 권한 게이트 — 조회 자체가 연동 요청 UI 전용이므로 BTN_NEW 로 게이트(§6-1 #5).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_NEW);

        // 진입 로그는 게이트 통과 후 + 정제값만 기록(미검증 입력의 로그 위조 방지 — 보안 리뷰 반영).
        String cmpnyCdForLog = sanitizeForLog(param.cmpnyCd());
        log.info("회사 정확일치 조회 진입 - gvCmpnyCd={}, 대상={}", param.gvCmpnyCd(), cmpnyCdForLog);

        // 빈 입력/자기 회사 포함 모든 미충족 케이스를 동일한 200 + cmpny=null 로 응답(§6-1 #4 — 사유 무구분).
        CmpnyExactResult cmpny = null;
        if (param.cmpnyCd() != null && !param.cmpnyCd().isBlank()) {
            cmpny = subcon01Mapper.selectCmpnyExact(param.cmpnyCd().trim(), param.gvCmpnyCd());
        }

        // 결과 유무는 로그에만 남기고 응답 형태는 동일 유지(§6-1 #6).
        log.info("회사 정확일치 조회 종료 - gvCmpnyCd={}, 대상={}, 결과={}",
                param.gvCmpnyCd(), cmpnyCdForLog, cmpny != null ? "Y" : "N");

        return CmpnyExactSearchResponse.builder()
                .cmpny(cmpny)
                .build();
    }

    @Override
    public RelationListResponse selectRelationList(RelationListParam param) {
        log.info("연동 관계 목록 조회 진입 - gvCmpnyCd={}", param.gvCmpnyCd());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SRCH);

        List<RelationResult> relations = subcon01Mapper.selectRelationList(param.gvCmpnyCd(), LIST_LIMIT);

        log.info("연동 관계 목록 조회 종료 - gvCmpnyCd={}, rows={}", param.gvCmpnyCd(), relations.size());

        return RelationListResponse.builder()
                .relations(relations)
                .build();
    }

    @Override
    public RelationHistResponse selectRelationHists(RelationHistParam param) {
        log.info("연동 관계 이력 조회 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SRCH);

        if (param.relationId() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }

        // 당사자 검증(§7-3) — 비당사자/미존재는 동일 404(존재 비노출).
        assertRelationParty(param.relationId(), param.gvCmpnyCd());

        // 행위자명: 자사 소속만 SQL 해석(상대사는 NULL 로 수신) → 고정 라벨 치환(Q4).
        List<RelationHistResult> hists = subcon01Mapper.selectRelationHists(param.relationId(), param.gvCmpnyCd())
                .stream()
                .map(r -> new RelationHistResult(
                        r.histId()
                        , r.actionType()
                        , r.actionUserNm() != null ? r.actionUserNm() : OTHER_CMPNY_ACTOR_LABEL
                        , r.actionDtime()
                        , r.actionDesc()))
                .toList();

        log.info("연동 관계 이력 조회 종료 - gvCmpnyCd={}, relationId={}, rows={}",
                param.gvCmpnyCd(), param.relationId(), hists.size());

        return RelationHistResponse.builder()
                .hists(hists)
                .build();
    }

    @Override
    public TerminateSummaryResponse selectTerminateSummary(RelationHistParam param) {
        log.info("해지 영향 요약 조회 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        // 해지 플로우 전용 조회 → BTN_DELT 게이트(§5 버튼-액션 매핑).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_DELT);

        if (param.relationId() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }

        // 당사자 검증 + 양측 회사코드 회수(§8 — 비당사자/미존재는 동일 404).
        RelationPartyRaw relation = assertRelationParty(param.relationId(), param.gvCmpnyCd());

        // 전 핸들러 summarize 합산(T1 시점 구현체 0개 → 빈 배열, 부작용 금지 계약).
        List<TerminationImpactItem> impacts = new ArrayList<>();
        terminationHandlers.orderedStream().forEach(handler ->
                impacts.addAll(handler.summarize(param.relationId(), relation.reqCmpnyCd(), relation.tgtCmpnyCd())));

        log.info("해지 영향 요약 조회 종료 - gvCmpnyCd={}, relationId={}, impacts={}",
                param.gvCmpnyCd(), param.relationId(), impacts.size());

        return TerminateSummaryResponse.builder()
                .impacts(impacts)
                .build();
    }

    @Override
    @Transactional
    public RelationCreateResponse insertRelationRequest(RelationCreateParam param) {
        // 0) 서버측 메뉴 권한 게이트(요청 생성 — BTN_NEW). 진입 로그는 게이트 후 + 정제값(보안 리뷰 반영).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_NEW);
        log.info("연동 관계 요청 생성 진입 - gvCmpnyCd={}, tgtCmpnyCd={}", param.gvCmpnyCd(), sanitizeForLog(param.tgtCmpnyCd()));

        // 1) 필수값 + 자기 회사 대상 금지(§7-1 #1).
        if (param.tgtCmpnyCd() == null || param.tgtCmpnyCd().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
        String tgtCmpnyCd = param.tgtCmpnyCd().trim();
        if (tgtCmpnyCd.equals(param.gvCmpnyCd())) {
            throw new ApiException(SubconErrorCode.SUBCON_400_002);
        }

        // 2) 대상 회사 유효성 재검증(§7-2 — 조회 시점과 요청 시점 사이 비활성화 가능).
        //    미존재/비활성 무구분 단일 404(열거 방지).
        if (subcon01Mapper.selectCmpnyExact(tgtCmpnyCd, param.gvCmpnyCd()) == null) {
            throw new ApiException(SubconErrorCode.SUBCON_404_002);
        }

        // 3) 활성 관계 중복 가드(쌍당 1건, 방향 불문 — FOR UPDATE 직렬화, §7-2).
        if (subcon01Mapper.selectActivePairCntForUpdate(param.gvCmpnyCd(), tgtCmpnyCd) > 0) {
            throw new ApiException(SubconErrorCode.SUBCON_409_001);
        }

        // 4) INSERT — 동시 요청(A→B, B→A) 레이스는 ACTIVE_PAIR_KEY UNIQUE(DB 백스톱, Q3)가
        //    DuplicateKeyException 으로 수렴 → 사용자 친화 메시지로 변환.
        RelationInsertCommand command = new RelationInsertCommand(
                param.gvCmpnyCd()
                , tgtCmpnyCd
                , param.gvUserCd()
                , param.gvUserCd());
        try {
            subcon01Mapper.insertRelation(command);
        } catch (DuplicateKeyException e) {
            log.info("연동 관계 요청 생성 - 활성 중복(UNIQUE 백스톱) gvCmpnyCd={}, tgtCmpnyCd={}",
                    param.gvCmpnyCd(), tgtCmpnyCd);
            throw new ApiException(SubconErrorCode.SUBCON_409_001);
        }

        // 5) HIST 기록(동일 트랜잭션 — §7-1 공통).
        subcon01Mapper.insertRelationHist(new RelationHistInsertCommand(
                command.getRelationId()
                , ACTION_REQUEST
                , param.gvCmpnyCd()
                , param.gvUserCd()
                , null
                , param.gvUserCd()));

        log.info("연동 관계 요청 생성 종료 - gvCmpnyCd={}, tgtCmpnyCd={}, relationId={}",
                param.gvCmpnyCd(), tgtCmpnyCd, command.getRelationId());

        return RelationCreateResponse.builder()
                .relationId(command.getRelationId())
                .build();
    }

    @Override
    @Transactional
    public void acceptRelation(RelationProcessParam param) {
        log.info("연동 관계 수락 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateRelationId(param.relationId());
        validateCommentLength(param.comment());

        // 조건부 UPDATE(REQUESTED + TGT 소속) — 0행이면 미존재/타사/이미 처리 통합 404.
        int updated = subcon01Mapper.acceptRelation(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_001);
        }

        insertHist(param, ACTION_ACCEPT, param.comment());

        log.info("연동 관계 수락 종료 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());
    }

    @Override
    @Transactional
    public void rejectRelation(RelationProcessParam param) {
        log.info("연동 관계 거부 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateRelationId(param.relationId());

        // 거부 사유 필수 + 길이 제한(§7-1 #3).
        if (param.comment() == null || param.comment().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_003);
        }
        validateCommentLength(param.comment());

        int updated = subcon01Mapper.rejectRelation(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_001);
        }

        insertHist(param, ACTION_REJECT, param.comment());

        log.info("연동 관계 거부 종료 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());
    }

    @Override
    @Transactional
    public void cancelRelation(RelationProcessParam param) {
        log.info("연동 관계 취소 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateRelationId(param.relationId());
        validateCommentLength(param.comment());

        int updated = subcon01Mapper.cancelRelation(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_001);
        }

        insertHist(param, ACTION_CANCEL, param.comment());

        log.info("연동 관계 취소 종료 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());
    }

    @Override
    @Transactional
    public void terminateRelation(RelationProcessParam param) {
        log.info("연동 관계 해지 진입 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_DELT);
        validateRelationId(param.relationId());
        validateCommentLength(param.comment());

        // 조건부 UPDATE(ACCEPTED + 양측 중 어느 쪽이든) — 0행이면 통합 404.
        int updated = subcon01Mapper.terminateRelation(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_001);
        }

        insertHist(param, ACTION_TERMINATE, param.comment());

        // 해지 자동 종결 훅(§8) — UPDATE 성공으로 당사자성 기증명. 양측 회사코드 회수 후
        // 동일 트랜잭션 내 호출(핸들러 예외 시 해지 전체 롤백 — 반쪽 해지 방지).
        RelationPartyRaw relation = subcon01Mapper.selectRelationById(param.relationId());
        terminationHandlers.orderedStream().forEach(handler ->
                handler.onTerminated(param.relationId(), relation.reqCmpnyCd(), relation.tgtCmpnyCd(), param.gvUserCd()));

        log.info("연동 관계 해지 종료 - gvCmpnyCd={}, relationId={}", param.gvCmpnyCd(), param.relationId());
    }

    // =========================== private ===========================

    /**
     * 서버측 메뉴 권한 게이트 — Subcon_01 메뉴의 지정 버튼권한(BTN_SRCH/NEW/SAVE/DELT)을 보유한 역할만 통과.
     *
     * <p>권한 판정은 AuthRoleUtils 단독이 아니라 TB_SYST_AUTH_MENU 조회 기반이다(User06 패턴 미러 —
     * 시드는 Q2 확정대로 master/system 만이나, 추후 화면에서 확대 가능). authCd 는 JWT 클레임 도출값만 신뢰한다.
     */
    private void assertMenuButton(String cmpnyCd, String authCd, String userCd, String btnType) {
        if (authCd == null || authCd.isBlank()
                || subcon01Mapper.selectMenuButtonAuthCnt(cmpnyCd, authCd, MENU_D_ID, btnType) <= 0) {
            log.warn("연동회사 관리 권한 없음(역할 게이트 차단) - userCd={}, authCd={}, btnType={}", userCd, authCd, btnType);
            throw new ApiException(SubconErrorCode.SUBCON_403_001);
        }
    }

    /** 관계 당사자 검증(§7-3) — 비당사자/미존재는 동일 404(존재 비노출). 검증 통과 시 관계 원시행 반환. */
    private RelationPartyRaw assertRelationParty(Long relationId, String gvCmpnyCd) {
        RelationPartyRaw relation = subcon01Mapper.selectRelationPartyRaw(relationId, gvCmpnyCd);
        if (relation == null) {
            throw new ApiException(SubconErrorCode.SUBCON_404_001);
        }
        return relation;
    }

    private void validateRelationId(Long relationId) {
        if (relationId == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
    }

    /** 코멘트 길이 제한(DDL varchar(500) — truncation/500 방지). null/빈 값은 통과(거부만 별도 필수 검증). */
    private void validateCommentLength(String comment) {
        if (comment != null && comment.length() > COMMENT_MAX_LEN) {
            throw new ApiException(SubconErrorCode.SUBCON_400_004);
        }
    }

    private RelationProcessCommand toProcessCommand(RelationProcessParam param) {
        return new RelationProcessCommand(
                param.relationId()
                , param.gvCmpnyCd()
                , param.gvUserCd()
                , param.comment());
    }

    /** 전이 이력 INSERT(동일 트랜잭션 — 행위자 소속 회사는 토큰 gv_cmpnyCd 만 주입, Q1). */
    private void insertHist(RelationProcessParam param, String actionType, String actionDesc) {
        subcon01Mapper.insertRelationHist(new RelationHistInsertCommand(
                param.relationId()
                , actionType
                , param.gvCmpnyCd()
                , param.gvUserCd()
                , actionDesc
                , param.gvUserCd()));
    }
}
