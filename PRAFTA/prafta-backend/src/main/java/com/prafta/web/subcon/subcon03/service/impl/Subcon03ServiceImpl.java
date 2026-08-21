package com.prafta.web.subcon.subcon03.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.attd.util.FixedOtMinutesUtils;
import com.prafta.common.cmm.attd.util.RecognizedMinutesUtils;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon03.application.command.BundleInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.ShareReqInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.ShareReqProcessCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotAttdInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotNearmissInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotRiskImproveInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotRiskInsertCommand;
import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveInfoParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCandidatesParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCreateParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqProcessParam;
import com.prafta.web.subcon.subcon03.application.param.ShareScopeParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotDetailParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotFileParam;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveInfoResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCandidatesResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCreateResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqListResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotDetailResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotListResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotNearmissDetailResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotRiskDetailResponse;
import com.prafta.web.subcon.subcon03.mapper.Subcon03Mapper;
import com.prafta.web.subcon.subcon03.result.ChainSiteResult;
import com.prafta.web.subcon.subcon03.result.FixedOtScheduleRow;
import com.prafta.web.subcon.subcon03.result.CloseGateResult;
import com.prafta.web.subcon.subcon03.result.CoverageMonthResult;
import com.prafta.web.subcon.subcon03.result.CoverageResult;
import com.prafta.web.subcon.subcon03.result.HalfLeaveWindowRow;
import com.prafta.web.subcon.subcon03.result.NearmissSourceRow;
import com.prafta.web.subcon.subcon03.result.RelayCandidateResult;
import com.prafta.web.subcon.subcon03.result.RiskImproveSourceRow;
import com.prafta.web.subcon.subcon03.result.RiskSourceRow;
import com.prafta.web.subcon.subcon03.result.ShareCmpnyResult;
import com.prafta.web.subcon.subcon03.result.ShareReqRaw;
import com.prafta.web.subcon.subcon03.result.ShareReqResult;
import com.prafta.web.subcon.subcon03.result.SiteNodeResult;
import com.prafta.web.subcon.subcon03.result.SnapshotDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotNearmissDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotResult;
import com.prafta.web.subcon.subcon03.result.SnapshotRiskDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotRiskImproveResult;
import com.prafta.web.subcon.subcon03.result.SnapshotSourceRow;
import com.prafta.web.subcon.subcon03.service.ShareCloseGateService;
import com.prafta.web.subcon.subcon03.service.SnapshotFileCopyService;
import com.prafta.web.subcon.subcon03.service.Subcon03Service;
import com.prafta.web.subcon.subcon03.service.ThirdPartyConsentProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 공유(Subcon_03 / Subcon_04) 서비스 — PRAFTA-SUBCON-T3.
 *
 * <p>보안 원칙(T1·T2 승계 + T3 요청서 §6):
 * <ul>
 *   <li>회사 스코프는 JWT 클레임(gvCmpnyCd)만 신뢰한다(클라 바디 회사코드 불신).</li>
 *   <li>상태 전이는 조건부 UPDATE(0행 = 404 통합 — 미존재/타사/기처리 무구분, IDOR 열거 방지).</li>
 *   <li>제공측 대상 사업장(TARGET_SITE_CD)은 클라 입력이 아니라 서버가 사업장 연동 체인으로 해석한다.</li>
 *   <li>스냅샷 헤더의 OWNER_CMPNY_CD 는 DB 의 SHARE_REQ.REQ_CMPNY_CD 에서만 온다.</li>
 *   <li>릴레이 대상(bundleSnapshotIds)은 승인 트랜잭션 안에서 후보 4조건을 전부 재검증한다.</li>
 *   <li>PII 최소수집: 상세행에 저장하는 인적 정보는 성명 + 소속표시뿐. USER_CD 는 서버 내부(동의 필터,
 *       WORKER_SEQ 채번)에서만 쓰고 저장/응답하지 않는다. 로그에 성명을 출력하지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Subcon03ServiceImpl implements Subcon03Service {

    private final Subcon03Mapper subcon03Mapper;
    private final ShareCloseGateService shareCloseGateService;

    /** [PS-04] 커버리지 요약 메타(COVERAGE_META) JSON 직렬화 — 다른 서비스 공용 빈 재사용(신규 유틸 불필요). */
    private final ObjectMapper objectMapper;

    /** [T7] 위험성평가/아차사고 첨부 물리 복제(수신사 소유 신규 파일). */
    private final SnapshotFileCopyService snapshotFileCopyService;

    /** [T7] 첨부 서빙용 파일 바이트 로더(범용 — base-dir traversal 방어 내장). */
    private final FileService fileService;

    /**
     * 제3자 제공 동의 필터 확장점(D7) — 구현체 빈이 없으면 전원 포함(T3 스펙).
     * List 직접 주입은 후보 빈 0개일 때 기동 실패하므로 ObjectProvider 로 빈 스트림을 허용한다(T1 훅 패턴).
     */
    private final ObjectProvider<ThirdPartyConsentProvider> consentProviders;

    /** 화면 메뉴 식별자(서버측 역할 게이트 기준). */
    private static final String MENU_D_ID_REQ = "Subcon_03";
    private static final String MENU_D_ID_SNAPSHOT = "Subcon_04";

    /** 메뉴 버튼 권한 종류(고정 상수 — 동적 컬럼 주입 금지). 조회=SRCH, 요청 생성=NEW, 승인·거부·취소=SAVE. */
    private static final String BTN_SRCH = "SRCH";
    private static final String BTN_NEW = "NEW";
    private static final String BTN_SAVE = "SAVE";

    /** 목록 조회 상한(전수조회 방지 — Subcon01/02 LIST_LIMIT 준용). */
    private static final int LIST_LIMIT = 500;

    /** 상세 페이지 크기(수신 자료 그리드). */
    private static final int DETAIL_PAGE_SIZE = 200;

    /** 상세 조회 페이지 상한(오버플로우·과대 offset 방어 — 20만 행 상당). */
    private static final int MAX_DETAIL_PAGE = 1000;

    /** 처리 코멘트/제공 목적 최대 길이(DDL varchar(500) 정합). */
    private static final int TEXT_MAX_LEN = 500;

    /** 요청 가능 기간 상한(개월 — 전수 복사 방지. 사용자 확정 2026-07-13). */
    private static final int MAX_PERIOD_MONTHS = 12;

    /** 상세행 배치 INSERT 청크 크기(파라미터 개수 상한/패킷 크기 보호). */
    private static final int INSERT_CHUNK_SIZE = 500;

    /** 지원 데이터 유형 화이트리스트(T7 = 근태 + 위험성평가 + 아차사고). */
    private static final Set<String> SUPPORTED_DATA_TYPES = Set.of("ATTD", "RISK", "NEARMISS");

    /** 데이터 유형 상수. */
    private static final String TYPE_ATTD = "ATTD";
    private static final String TYPE_RISK = "RISK";
    private static final String TYPE_NEARMISS = "NEARMISS";
    /** PRAFTA-FIXEDOT-3(M21): 상세행 유형 — 근태행(OT_ONLY/LEAVE_ONLY 는 실근태 스탬프가 없어 실적 0). */
    private static final String ROW_TYPE_ATTD = "ATTD";

    /**
     * [T7·§5-1] 확정 위험성평가 상태 필터(메인 세션 Q3 확정) — 001 검토요청(미검토) 제외.
     * 사용자 결정 변경 시 이 상수 1곳만 수정하면 뒤집힌다.
     */
    private static final List<String> RISK_CONFIRMED_STATUS = List.of("002", "003", "004", "005");

    /**
     * [T7·§5-1] 확정 아차사고 상태 필터(메인 세션 Q2 확정) — 400 미처리대상(반려) 제외.
     */
    private static final List<String> NEARMISS_CONFIRMED_STATUS = List.of("100", "200", "300");

    /** [T7] 첨부 파일 유형[SYS010] — 위험성평가 '002' / 아차사고 '004'. */
    private static final String FILE_TYPE_RISK = "002";
    private static final String FILE_TYPE_NEARMISS = "004";

    /** [T7] 첨부 복제 건수 관측 소프트 상한(초과 시 WARN 만 — 하드 상한/차단 없음, 메인 세션 Q5). */
    private static final int FILE_COPY_SOFT_LIMIT = 200;

    /** [T7] 근로자 구분 — 위험성평가 작성자/아차사고 제보자는 정규직 동의 경로. */
    private static final String WORKER_TYPE_REGULAR = "REGULAR";

    private static final DateTimeFormatter YMD_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** [PS-03/04/06] 마감 커버리지 판정/META 계약값 포맷(YYYYMM) — approve/approve-info 공용. */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    /** [PS-02] 커버리지 META 월별 제외 부서명 캡(초과 시 "외 N개 부서" 1항목으로 요약). */
    private static final int COVERAGE_DEPT_NAME_CAP = 20;

    /** 로그 위조 방지용 외부 입력 정제 — 개행 제거 + 50자 상한(T1 SEC-ADV-1 승계. 성명에는 쓰지 않는다). */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\r\\n]", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
    }

    // =========================== 조회(T3-02) ===========================

    @Override
    public ShareReqListResponse selectShareReqList(ShareScopeParam param) {
        log.info("데이터 공유 요청 목록 조회 진입 - gvCmpnyCd={}", param.gvCmpnyCd());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_SRCH);

        List<ShareReqResult> reqs = subcon03Mapper.selectShareReqList(param.gvCmpnyCd(), LIST_LIMIT);

        log.info("데이터 공유 요청 목록 조회 종료 - gvCmpnyCd={}, rows={}", param.gvCmpnyCd(), reqs.size());

        return ShareReqListResponse.builder()
                .reqs(reqs)
                .build();
    }

    @Override
    public ShareReqCandidatesResponse selectShareReqCandidates(ShareReqCandidatesParam param) {
        // 요청 생성 플로우 전용 조회 → BTN_NEW 게이트.
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_NEW);
        log.info("공유 요청 후보 조회 진입 - gvCmpnyCd={}, prvCmpnyCd={}",
                param.gvCmpnyCd(), sanitizeForLog(param.prvCmpnyCd()));

        List<ShareCmpnyResult> cmpnyList = subcon03Mapper.selectActiveRelationCmpnyList(param.gvCmpnyCd());

        // 상대 회사 미지정이면 회사 목록만 반환한다(제공사 사업장 목록은 어떤 경우에도 노출하지 않는다).
        List<ChainSiteResult> siteList = Collections.emptyList();
        if (param.prvCmpnyCd() != null && !param.prvCmpnyCd().isBlank()) {
            String prvCmpnyCd = param.prvCmpnyCd().trim();

            // 관계 ACCEPTED 상대가 아니면 체인 조회 자체를 하지 않는다(무관계 회사 탐색 차단).
            boolean isPartner = cmpnyList.stream().anyMatch(c -> prvCmpnyCd.equals(c.cmpnyCd()));
            if (isPartner) {
                siteList = subcon03Mapper.selectChainSiteList(param.gvCmpnyCd(), prvCmpnyCd);
            }
        }

        log.info("공유 요청 후보 조회 종료 - gvCmpnyCd={}, cmpny={}건, site={}건",
                param.gvCmpnyCd(), cmpnyList.size(), siteList.size());

        return ShareReqCandidatesResponse.builder()
                .cmpnyList(cmpnyList)
                .siteList(siteList)
                .build();
    }

    // =========================== 요청 생성(T3-02, §5-3) ===========================

    @Override
    @Transactional
    public ShareReqCreateResponse createShareReq(ShareReqCreateParam param) {
        // 0) 서버측 메뉴 권한 게이트(요청 생성 — BTN_NEW). 진입 로그는 게이트 후 + 정제값.
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_NEW);
        log.info("데이터 공유 요청 생성 진입 - gvCmpnyCd={}, prvCmpnyCd={}, siteCd={}",
                param.gvCmpnyCd(), sanitizeForLog(param.prvCmpnyCd()), sanitizeForLog(param.siteCd()));

        // 1) 필수값 + 유형 화이트리스트.
        if (param.prvCmpnyCd() == null || param.prvCmpnyCd().isBlank()
                || param.siteCd() == null || param.siteCd().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
        String prvCmpnyCd = param.prvCmpnyCd().trim();
        String siteCd = param.siteCd().trim();

        String dataType = param.dataType() == null ? "" : param.dataType().trim();
        if (!SUPPORTED_DATA_TYPES.contains(dataType)) {
            throw new ApiException(SubconErrorCode.SUBCON_400_005);
        }

        String purpose = param.purpose() == null ? "" : param.purpose().trim();
        if (purpose.isBlank() || purpose.length() > TEXT_MAX_LEN) {
            throw new ApiException(SubconErrorCode.SUBCON_400_007);
        }

        // 2) 자기 회사 금지.
        if (prvCmpnyCd.equals(param.gvCmpnyCd())) {
            throw new ApiException(SubconErrorCode.SUBCON_400_002);
        }

        // 3) 기간 유효성(형식/순서/미래/상한).
        String periodStr = validateYmd(param.periodStr());
        String periodEnd = validateYmd(param.periodEnd());
        validatePeriodRange(periodStr, periodEnd);

        // 4) 관계 ACCEPTED(방향 불문) — RELATION_ID 기록.
        Long relationId = subcon03Mapper.selectActiveRelationId(param.gvCmpnyCd(), prvCmpnyCd);
        if (relationId == null) {
            throw new ApiException(SubconErrorCode.SUBCON_409_004);
        }

        // 5) 사업장 소유 검증 + 체인 해석(제공측 대응 사업장은 서버만 결정한다).
        if (subcon03Mapper.selectMySiteActiveCnt(param.gvCmpnyCd(), siteCd) <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_004);
        }
        String targetSiteCd = subcon03Mapper.selectTargetSiteCdByChain(param.gvCmpnyCd(), siteCd, prvCmpnyCd);
        if (targetSiteCd == null || targetSiteCd.isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_409_006);
        }

        // 6) 중복 REQUESTED 가드(종결 상태 행은 재요청 허용 — 마스터 §4 소결정 ①).
        if (subcon03Mapper.selectRequestedDupCnt(param.gvCmpnyCd(), prvCmpnyCd, targetSiteCd,
                dataType, periodStr, periodEnd) > 0) {
            throw new ApiException(SubconErrorCode.SUBCON_409_005);
        }

        // 마감 옵션은 ATTD 전용(§5-2, D8) — RISK/NEARMISS 는 값이 와도 서버가 'N' 강제(무의미 값 무시).
        String closedOnlyYn = TYPE_ATTD.equals(dataType)
                ? ("N".equals(param.closedOnlyYn()) ? "N" : "Y") // 기본 Y(마스터 §1-6)
                : "N";

        ShareReqInsertCommand command = new ShareReqInsertCommand(
                relationId
                , param.gvCmpnyCd()
                , siteCd
                , prvCmpnyCd
                , targetSiteCd
                , dataType
                , periodStr
                , periodEnd
                , closedOnlyYn
                , purpose
                , param.gvUserCd()
                , param.gvUserCd());
        subcon03Mapper.insertShareReq(command);

        log.info("데이터 공유 요청 생성 종료 - shareReqId={}, gvCmpnyCd={}, prvCmpnyCd={}, 기간={}~{}, 마감만={}",
                command.getShareReqId(), param.gvCmpnyCd(), prvCmpnyCd, periodStr, periodEnd, closedOnlyYn);

        return ShareReqCreateResponse.builder()
                .shareReqId(command.getShareReqId())
                .build();
    }

    // =========================== 취소 / 거부(T3-02) ===========================

    @Override
    @Transactional
    public void cancelShareReq(ShareReqProcessParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_SAVE);
        log.info("데이터 공유 요청 취소 진입 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());

        validateShareReqId(param.shareReqId());
        validateCommentLength(param.comment());

        int updated = subcon03Mapper.cancelShareReq(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_005);
        }

        log.info("데이터 공유 요청 취소 종료 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());
    }

    @Override
    @Transactional
    public void rejectShareReq(ShareReqProcessParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_SAVE);
        log.info("데이터 공유 요청 거부 진입 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());

        validateShareReqId(param.shareReqId());

        // 거부 사유 필수 + 길이 제한.
        if (param.comment() == null || param.comment().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_003);
        }
        validateCommentLength(param.comment());

        int updated = subcon03Mapper.rejectShareReq(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_005);
        }

        log.info("데이터 공유 요청 거부 종료 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());
    }

    // =========================== 승인 사전정보(T3-03·05) ===========================

    @Override
    public ShareReqApproveInfoResponse selectApproveInfo(ShareReqApproveInfoParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_SRCH);
        log.info("공유 승인 사전정보 조회 진입 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());

        validateShareReqId(param.shareReqId());

        // 제공측 소속 + REQUESTED 조건 포함 조회 — 비당사자/기처리는 0행 → 404 통합(존재 비노출).
        ShareReqRaw req = subcon03Mapper.selectRequestedShareReqForProvider(param.shareReqId(), param.gvCmpnyCd());
        if (req == null) {
            throw new ApiException(SubconErrorCode.SUBCON_404_005);
        }

        // 마감 게이팅은 ATTD 전용(§5-2) — RISK/NEARMISS 는 마감 개념이 없어 closedAll=true 고정(프론트 마감 블록 미표시).
        CloseGateResult gate = TYPE_ATTD.equals(req.dataType())
                ? shareCloseGateService.evaluate(req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd())
                : new CloseGateResult(true, Collections.emptyList());

        List<RelayCandidateResult> relayCandidates = subcon03Mapper.selectRelayCandidates(
                param.gvCmpnyCd(), req.targetSiteCd(), req.dataType(),
                req.periodStr(), req.periodEnd(), req.closedOnlyYn());

        // [PS-06] 포함/제외 범위 예고(D-1/D-2) — ATTD && closedOnlyYn='Y' 일 때만. approve(PS-04)와
        //   같은 computeCoverage 를 재사용해 예고≠실제 불일치를 막는다. 동의 필터는 여기서 미적용
        //   (실제 승인 시 미동의 제외로 건수가 더 줄 수 있음 — 팝업 문구에 반영).
        List<CoverageMonthResult> coverageMonths = null;
        Integer includedRowCnt = null;
        String expectedEmptyYn = null;

        // [동의 미필 사각지대 개선, 2026-07-23] 동의 필터까지 반영한 예상 포함 건수(예고) — ATTD 는
        //   closedOnlyYn 값과 무관하게 계산한다(동의 필터가 마감 옵션과 별개 축으로 항상 적용되므로).
        //   RISK/NEARMISS 도 동일 동의 필터 경로를 타므로 함께 계산한다(마감 개념은 없음).
        Integer consentIncludedRowCnt = null;
        Integer consentExcludedCandidateCnt = null;
        String consentPreviewEmptyYn = null;

        if (TYPE_ATTD.equals(req.dataType())) {
            CoverageResult coverage = computeCoverage(req, loadAttdSourceRows(req));
            if ("Y".equals(req.closedOnlyYn())) {
                coverageMonths = coverage.months().stream()
                        .map(m -> new CoverageMonthResult(fmtYm(m.ym()), m.status(), m.excludedDeptNms(), m.orphanUnclosedYn()))
                        .collect(Collectors.toList());
                includedRowCnt = coverage.includedRows().size();
                expectedEmptyYn = includedRowCnt == 0 ? "Y" : "N";
            }

            try {
                Set<String> excludedUserCds = resolveConsentExcluded(req, coverage.includedRows());
                long consentPassCnt = coverage.includedRows().stream()
                        .filter(r -> !excludedUserCds.contains(r.userCd()))
                        .count();
                consentIncludedRowCnt = (int) consentPassCnt;
                consentExcludedCandidateCnt = excludedUserCds.size();
                consentPreviewEmptyYn = consentPassCnt == 0 ? "Y" : "N";
            } catch (ApiException e) {
                if (SubconErrorCode.SUBCON_409_009.equals(e.getErrorCode())) {
                    // [E-4] 약관 비활성(fail-closed) — 예고 계산만 스킵, 조회 자체는 막지 않는다.
                    //   실제 승인 시점에는 여전히 409 로 차단된다(동작 변경 없음).
                    log.warn("승인 사전정보 — 동의 예상치 계산 스킵(약관 비활성) - shareReqId={}", req.shareReqId());
                } else {
                    throw e;
                }
            }
        } else if (TYPE_RISK.equals(req.dataType())) {
            try {
                RiskCollected riskData = collectRiskSource(req);
                consentIncludedRowCnt = riskData.parents().size();
                consentExcludedCandidateCnt = riskData.excludedCnt();
                consentPreviewEmptyYn = riskData.parents().isEmpty() ? "Y" : "N";
            } catch (ApiException e) {
                if (SubconErrorCode.SUBCON_409_009.equals(e.getErrorCode())) {
                    log.warn("승인 사전정보 — 동의 예상치 계산 스킵(약관 비활성) - shareReqId={}", req.shareReqId());
                } else {
                    throw e;
                }
            }
        } else if (TYPE_NEARMISS.equals(req.dataType())) {
            try {
                NearmissCollected nmData = collectNearmissSource(req);
                consentIncludedRowCnt = nmData.rows().size();
                consentExcludedCandidateCnt = nmData.excludedCnt();
                consentPreviewEmptyYn = nmData.rows().isEmpty() ? "Y" : "N";
            } catch (ApiException e) {
                if (SubconErrorCode.SUBCON_409_009.equals(e.getErrorCode())) {
                    log.warn("승인 사전정보 — 동의 예상치 계산 스킵(약관 비활성) - shareReqId={}", req.shareReqId());
                } else {
                    throw e;
                }
            }
        }

        log.info("공유 승인 사전정보 조회 종료 - shareReqId={}, 마감완료={}, 릴레이 후보 {}건, 예상포함 {}건, 동의반영예상포함 {}건",
                req.shareReqId(), gate.closedAll(), relayCandidates.size(), includedRowCnt, consentIncludedRowCnt);

        return ShareReqApproveInfoResponse.builder()
                .shareReqId(req.shareReqId())
                .reqCmpnyNm(subcon03Mapper.selectCmpnyNm(req.reqCmpnyCd()))
                .siteNm(subcon03Mapper.selectSiteNm(param.gvCmpnyCd(), req.targetSiteCd()))
                .dataType(req.dataType())
                .periodStr(req.periodStr())
                .periodEnd(req.periodEnd())
                .periodLabel(periodLabel(req.periodStr(), req.periodEnd()))
                .closedOnlyYn(req.closedOnlyYn())
                .purpose(req.purpose())
                .closedAll(gate.closedAll())
                .unclosedYms(gate.unclosedYms())
                .relayCandidates(relayCandidates)
                .coverageMonths(coverageMonths)
                .includedRowCnt(includedRowCnt)
                .expectedEmptyYn(expectedEmptyYn)
                .consentIncludedRowCnt(consentIncludedRowCnt)
                .consentExcludedCandidateCnt(consentExcludedCandidateCnt)
                .consentPreviewEmptyYn(consentPreviewEmptyYn)
                .build();
    }

    // =========================== 승인 = 스냅샷 생성(T3-04·05, §5-6) ===========================

    @Override
    @Transactional
    public ShareReqApproveResponse approveShareReq(ShareReqApproveParam param) {
        // 1) 서버측 메뉴 권한 게이트(승인 — BTN_SAVE).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_REQ, BTN_SAVE);
        log.info("데이터 공유 승인 진입 - gvCmpnyCd={}, shareReqId={}", param.gvCmpnyCd(), param.shareReqId());

        validateShareReqId(param.shareReqId());

        // 2) 선점(조건부 UPDATE: REQUESTED → APPROVED, 제공측 소속만) — 동시 승인 레이스 차단.
        int updated = subcon03Mapper.approveShareReqPreempt(new ShareReqProcessCommand(
                param.shareReqId(), param.gvCmpnyCd(), param.gvUserCd(), null));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_005);
        }

        // 선점 성공으로 당사자성 기증명 — 요청 원시행 회수(스냅샷 경계의 유일한 근거).
        ShareReqRaw req = subcon03Mapper.selectShareReqById(param.shareReqId());

        // 3) 관계 ACCEPTED 재검증(조회~승인 사이 해지 레이스) — 실패 시 전체 롤백.
        if (subcon03Mapper.selectRelationAcceptedCnt(req.relationId()) <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_409_004);
        }

        // 4) 유형별 마감 게이팅(§5-2) — ATTD 만 마감 검사. RISK/NEARMISS 는 마감 개념이 없어 skip(closedAll 간주).
        //    [D-1/D-2, 2026-07-22] "마감분만" 은 더 이상 승인 차단 게이트가 아니다 — 부분 포함 필터로
        //    재정의(PS-04)되어 여기서는 closedAll 만 산출한다(closedOnlyYn='N' 경로의 UNCLOSED_INCLUDED_YN
        //    산정에만 쓰인다). 구 차단(SUBCON_409_007)은 미사용 전환(SubconErrorCode 주석 참조 — enum 유지).
        String dataType = req.dataType();
        boolean isAttd = TYPE_ATTD.equals(dataType);

        boolean closedAll = true;
        if (isAttd) {
            CloseGateResult gate = shareCloseGateService.evaluate(
                    req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd());
            closedAll = gate.closedAll();
        }

        // 5) 릴레이 후보 서버 재검증(클라 목록 불신 — 타사 스냅샷ID 주입 = IDOR). DATA_TYPE 격리는 SQL 내부.
        List<RelayCandidateResult> bundles = resolveBundles(param, req);
        boolean relayUnclosed = bundles.stream().anyMatch(b -> "Y".equals(b.unclosedIncludedYn()));
        // [PS-05, D-3] 하위 스냅샷 중 부분 포함(마감분만 필터로 일부 제외) 이 있었는지 — 상위 표식에 병합.
        boolean relayPartialIncluded = bundles.stream().anyMatch(b -> "Y".equals(b.closedPartialYn()));

        // 미마감 포함 표식(D-3 재정의) — closedOnlyYn='Y' 는 커버리지 필터로 자체 기여가 항상 'N'
        // (마감분만 담김 — 미마감이 아니다). closedOnlyYn='N' 은 기존식(!closedAll) 유지. 릴레이 기여는 공통.
        boolean selfUnclosedContribution = isAttd && !"Y".equals(req.closedOnlyYn()) && !closedAll;
        String unclosedIncludedYn = isAttd ? ((selfUnclosedContribution || relayUnclosed) ? "Y" : "N") : "N";
        String relayIncludedYn = bundles.isEmpty() ? "N" : "Y";

        // 소속표시 = 제공사 회사명(자체행/릴레이 relabel 공용).
        String affilCmpnyNm = subcon03Mapper.selectCmpnyNm(req.prvCmpnyCd());

        // 6) 유형별 원천 수집 + 커버리지 필터(D-2, 동의 필터 이전 단계 — AND 결합) + 동의 필터
        //    (헤더 CONSENT_EXCLUDED_CNT/CLOSED_PARTIAL_YN/COVERAGE_META 근거 — 헤더 INSERT 전에 확정).
        List<SnapshotSourceRow> attdIncluded = null;
        RiskCollected riskData = null;
        NearmissCollected nmData = null;
        CoverageResult coverage = null;
        int consentExcludedCnt;

        if (isAttd) {
            List<SnapshotSourceRow> sourceRows = loadAttdSourceRows(req);

            // [PS-04, D-2] 커버리지 필터 — closedOnlyYn='N' 이면 no-op(전량 통과, coverage.partial()=false).
            //   동의 필터의 입력이 된다(AND 결합 — 커버 통과분이어도 미동의면 계속 제외, security §5).
            coverage = computeCoverage(req, sourceRows);

            Set<String> excludedUserCds = resolveConsentExcluded(req, coverage.includedRows());
            attdIncluded = coverage.includedRows().stream()
                    .filter(r -> !excludedUserCds.contains(r.userCd()))
                    .collect(Collectors.toList());
            consentExcludedCnt = excludedUserCds.size();
        } else if (TYPE_RISK.equals(dataType)) {
            riskData = collectRiskSource(req);
            consentExcludedCnt = riskData.excludedCnt();
        } else {
            nmData = collectNearmissSource(req);
            consentExcludedCnt = nmData.excludedCnt();
        }

        // 7) VERSION 채번(D5 — 동일 조건 키의 기존 최대 VERSION + 1).
        //    COUNT 가 아니라 MAX 기준이다: 서로 다른 SHARE_REQ_ID 두 건이 동시 승인될 때
        //    COUNT 기반이면 같은 VERSION 이 중복 채번된다(UNIQUE 가 (SHARE_REQ_ID, VERSION) 이라 DB 도 못 막음).
        int version = subcon03Mapper.selectMaxSnapshotVersionByCondition(req.reqCmpnyCd(), req.prvCmpnyCd(),
                req.targetSiteCd(), req.dataType(), req.periodStr(), req.periodEnd()) + 1;

        // [PS-04, D-3] 부분 포함 표식 — 자체(커버리지 필터로 실제 제외 발생) OR 릴레이 병합.
        //   closedOnlyYn='N'/RISK/NEARMISS 는 필터 미적용이라 항상 'N'(NULL 은 구본 전용 — 신규분 금지).
        boolean selfPartial = isAttd && "Y".equals(req.closedOnlyYn()) && coverage != null && coverage.partial();
        String closedPartialYn = (selfPartial || relayPartialIncluded) ? "Y" : "N";

        // [PS-04, PS-02 스키마] 커버리지 요약 META — closedOnlyYn='Y' && ATTD 일 때만 기록(그 외 NULL —
        //   가이드 대상 아님). 메타에 성명/USER_CD 는 절대 담지 않는다(부서명·월·건수까지, 공통 §11).
        String coverageMeta = (isAttd && "Y".equals(req.closedOnlyYn()))
                ? buildCoverageMeta(coverage, relayPartialIncluded)
                : null;

        // 8) 헤더 INSERT — OWNER_CMPNY_CD 는 DB 의 REQ_CMPNY_CD 만 사용(클라 바디 불신).
        SnapshotInsertCommand header = new SnapshotInsertCommand(
                req.shareReqId()
                , req.reqCmpnyCd()
                , version
                , unclosedIncludedYn
                , closedPartialYn
                , coverageMeta
                , consentExcludedCnt
                , relayIncludedYn
                , param.gvUserCd());
        subcon03Mapper.insertSnapshot(header);
        Long snapshotId = header.getSnapshotId();

        // 9) 자체 상세행 INSERT(유형별 — 소속표시 = 제공사, 로컬 SEQ 채번, 첨부는 수신사 소유로 복제).
        int ownRowCnt;
        if (isAttd) {
            ownRowCnt = insertOwnRows(snapshotId, req, attdIncluded, affilCmpnyNm, param.gvUserCd());
        } else if (TYPE_RISK.equals(dataType)) {
            ownRowCnt = insertRiskOwnRows(snapshotId, req, riskData, affilCmpnyNm, param.gvUserCd());
        } else {
            ownRowCnt = insertNearmissOwnRows(snapshotId, req, nmData, affilCmpnyNm, param.gvUserCd());
        }

        // 10) 릴레이 relabel 복사 + 번들 감사(유형별 — RISK/NEARMISS 는 첨부 물리 재복제 포함).
        int relayRowCnt = 0;
        for (RelayCandidateResult bundle : bundles) {
            int copied;
            if (isAttd) {
                int offset = subcon03Mapper.selectMaxWorkerSeq(snapshotId); // 하위 스냅샷마다 누적 오프셋
                copied = subcon03Mapper.copyRelayRows(snapshotId, bundle.snapshotId(), param.gvCmpnyCd(),
                        affilCmpnyNm, offset, param.gvUserCd());
            } else if (TYPE_RISK.equals(dataType)) {
                copied = copyRelayRiskRows(snapshotId, bundle.snapshotId(), req, affilCmpnyNm, param.gvUserCd());
            } else {
                copied = copyRelayNearmissRows(snapshotId, bundle.snapshotId(), req, affilCmpnyNm, param.gvUserCd());
            }
            relayRowCnt += copied;

            subcon03Mapper.insertSnapshotBundle(new BundleInsertCommand(
                    snapshotId, bundle.snapshotId(), param.gvCmpnyCd(), copied, param.gvUserCd()));
        }

        // 11) ROW_CNT 확정(스냅샷 불변 원칙의 유일한 예외 — 생성 트랜잭션 내 1회).
        int totalRowCnt = ownRowCnt + relayRowCnt;
        subcon03Mapper.updateSnapshotRowCnt(snapshotId, totalRowCnt, param.gvUserCd());

        int coverageExcludedCnt = (coverage != null) ? coverage.excludedRowCnt() : 0;
        log.info("공유 스냅샷 생성 — shareReqId={}, type={}, snapshotId={}, ver={}, 자체 {}행, 릴레이 {}행, "
                        + "미동의 제외 {}, 미마감포함={}, 마감분만부분포함={}, 커버리지제외 {}건",
                req.shareReqId(), dataType, snapshotId, version, ownRowCnt, relayRowCnt, consentExcludedCnt,
                unclosedIncludedYn, closedPartialYn, coverageExcludedCnt);

        return ShareReqApproveResponse.builder()
                .snapshotId(snapshotId)
                .version(version)
                .rowCnt(totalRowCnt)
                .consentExcludedCnt(consentExcludedCnt)
                .unclosedIncludedYn(unclosedIncludedYn)
                .closedPartialYn(closedPartialYn)
                .build();
    }

    // =========================== 수신 자료 조회(T3-06, §5-8) ===========================

    @Override
    public SnapshotListResponse selectSnapshotList(ShareScopeParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_SNAPSHOT, BTN_SRCH);
        log.info("공유받은 자료 목록 조회 진입 - gvCmpnyCd={}", param.gvCmpnyCd());

        List<SnapshotResult> snapshots = subcon03Mapper.selectSnapshotList(param.gvCmpnyCd(), LIST_LIMIT);

        log.info("공유받은 자료 목록 조회 종료 - gvCmpnyCd={}, rows={}", param.gvCmpnyCd(), snapshots.size());

        return SnapshotListResponse.builder()
                .snapshots(snapshots)
                .build();
    }

    @Override
    public SnapshotDetailResponse selectSnapshotDetail(SnapshotDetailParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_SNAPSHOT, BTN_SRCH);
        log.info("공유받은 자료 상세 조회 진입 - gvCmpnyCd={}, snapshotId={}", param.gvCmpnyCd(), param.snapshotId());

        if (param.snapshotId() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }

        // 페이지 상한 — 무제한 허용 시 (page-1)*PAGE_SIZE 가 int 오버플로우로 음수 offset 이 되어
        //   MySQL 이 거부한다(500). 상세행 상한(LIST_LIMIT * PAGE_SIZE)을 넘는 페이지는 마지막 페이지로 클램프.
        int page = (param.page() == null || param.page() < 1) ? 1 : param.page();
        if (page > MAX_DETAIL_PAGE) {
            page = MAX_DETAIL_PAGE;
        }
        long offset = (long) (page - 1) * DETAIL_PAGE_SIZE;

        // 소유 검증(OWNER_CMPNY_CD = gv)은 SQL 안에서 강제 — 타사 스냅샷ID 는 빈 목록(존재 비노출).
        List<SnapshotDetailResult> rows = subcon03Mapper.selectSnapshotDetail(
                param.gvCmpnyCd(), param.snapshotId(), offset, DETAIL_PAGE_SIZE);

        log.info("공유받은 자료 상세 조회 종료 - gvCmpnyCd={}, snapshotId={}, page={}, rows={}",
                param.gvCmpnyCd(), param.snapshotId(), page, rows.size());

        return SnapshotDetailResponse.builder()
                .rows(rows)
                .page(page)
                .pageSize(DETAIL_PAGE_SIZE)
                .build();
    }

    // =========================== T7 수신 상세 조회 + 첨부 서빙(§5-8·§5-9) ===========================

    @Override
    public SnapshotRiskDetailResponse selectSnapshotRiskDetail(SnapshotDetailParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_SNAPSHOT, BTN_SRCH);
        log.info("공유 위험성평가 상세 조회 진입 - gvCmpnyCd={}, snapshotId={}", param.gvCmpnyCd(), param.snapshotId());

        if (param.snapshotId() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }

        // 소유 검증(OWNER=gv)은 SQL 안에서 강제 — 타사 스냅샷ID 는 빈 목록(존재 비노출).
        List<SnapshotRiskDetailResult> rows = subcon03Mapper.selectSnapshotRiskRows(param.gvCmpnyCd(), param.snapshotId());
        if (!rows.isEmpty()) {
            List<SnapshotRiskImproveResult> improves =
                    subcon03Mapper.selectSnapshotRiskImproveRows(param.gvCmpnyCd(), param.snapshotId());
            Map<Long, List<SnapshotRiskImproveResult>> byDetail = new HashMap<>();
            for (SnapshotRiskImproveResult imp : improves) {
                byDetail.computeIfAbsent(imp.detailId(), k -> new ArrayList<>()).add(imp);
            }
            for (SnapshotRiskDetailResult row : rows) {
                row.setImproves(byDetail.getOrDefault(row.getDetailId(), Collections.emptyList()));
            }
        }

        log.info("공유 위험성평가 상세 조회 종료 - gvCmpnyCd={}, snapshotId={}, rows={}",
                param.gvCmpnyCd(), param.snapshotId(), rows.size());

        return SnapshotRiskDetailResponse.builder().rows(rows).build();
    }

    @Override
    public SnapshotNearmissDetailResponse selectSnapshotNearmissDetail(SnapshotDetailParam param) {
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_SNAPSHOT, BTN_SRCH);
        log.info("공유 아차사고 상세 조회 진입 - gvCmpnyCd={}, snapshotId={}", param.gvCmpnyCd(), param.snapshotId());

        if (param.snapshotId() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }

        List<SnapshotNearmissDetailResult> rows =
                subcon03Mapper.selectSnapshotNearmissRows(param.gvCmpnyCd(), param.snapshotId());

        log.info("공유 아차사고 상세 조회 종료 - gvCmpnyCd={}, snapshotId={}, rows={}",
                param.gvCmpnyCd(), param.snapshotId(), rows.size());

        return SnapshotNearmissDetailResponse.builder().rows(rows).build();
    }

    @Override
    public FileBytesResult selectSnapshotFile(SnapshotFileParam param) {
        // 버튼 권한 게이트(Subcon_04 조회) — 프론트 숨김 의존 금지.
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), MENU_D_ID_SNAPSHOT, BTN_SRCH);

        if (param.snapshotId() == null || param.fileMgmtCd() == null || param.fileMgmtCd().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_404_007);
        }

        // 소유(snapshotId OWNER=gv) + 참조(fileMgmtCd 가 그 스냅샷 첨부 집합에 존재) 검증 — SQL 내부 강제(IDOR 봉인).
        //   cmpnyCd 는 토큰 gv 만 사용(클라 파라미터 회사코드 금지) → 타사 파일 접근 불가.
        Integer refCnt = subcon03Mapper.selectSnapshotFileRefCnt(
                param.gvCmpnyCd(), param.snapshotId(), param.fileMgmtCd());
        if (refCnt == null || refCnt <= 0) {
            log.warn("공유 첨부 서빙 거부(소유/참조 검증 실패) - gvCmpnyCd={}, snapshotId={}, fileMgmtCd={}",
                    param.gvCmpnyCd(), param.snapshotId(), sanitizeForLog(param.fileMgmtCd()));
            throw new ApiException(SubconErrorCode.SUBCON_404_007);
        }

        // 파일 바이트 로드 — cmpnyCd 는 반드시 토큰 gv(수신사 소유). loadFileBytes 가 base-dir traversal 방어 내장.
        FileBytesResult file = fileService.loadFileBytes(new FileReadQuery(param.gvCmpnyCd(), param.fileMgmtCd()));
        if (file == null || file.data() == null || file.data().length == 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_007);
        }
        return file;
    }

    // =========================== private — 스냅샷 생성 보조 ===========================

    /**
     * 릴레이 대상 재검증(§5-7) — 클라가 보낸 snapshotId 가 후보 4조건(소유/사업장체인/기간포함/미마감표식)을
     * 전부 충족하는지 승인 트랜잭션 안에서 재확인한다. 하나라도 미매칭이면 승인 전체를 거부한다.
     */
    private List<RelayCandidateResult> resolveBundles(ShareReqApproveParam param, ShareReqRaw req) {
        if (param.bundleSnapshotIds() == null || param.bundleSnapshotIds().isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> requestedIds = new LinkedHashSet<>();
        for (Long id : param.bundleSnapshotIds()) {
            if (id != null) {
                requestedIds.add(id);
            }
        }
        if (requestedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RelayCandidateResult> candidates = subcon03Mapper.selectRelayCandidates(
                param.gvCmpnyCd(), req.targetSiteCd(), req.dataType(),
                req.periodStr(), req.periodEnd(), req.closedOnlyYn());

        Map<Long, RelayCandidateResult> byId = new HashMap<>();
        for (RelayCandidateResult candidate : candidates) {
            byId.put(candidate.snapshotId(), candidate);
        }

        List<RelayCandidateResult> bundles = new ArrayList<>();
        for (Long id : requestedIds) {
            RelayCandidateResult matched = byId.get(id);
            if (matched == null) {
                log.warn("릴레이 후보 재검증 실패(부적격 스냅샷 포함) - shareReqId={}, gvCmpnyCd={}, snapshotId={}",
                        req.shareReqId(), param.gvCmpnyCd(), id);
                throw new ApiException(SubconErrorCode.SUBCON_409_008);
            }
            bundles.add(matched);
        }
        return bundles;
    }

    // =========================== private — 마감 커버리지 필터(PS-03/04/06, D-1/D-2) ===========================

    /** ATTD 원천 3쿼리(근태/OT_ONLY/LEAVE_ONLY) 로드 — approve(PS-04)·approve-info(PS-06) 공용. */
    private List<SnapshotSourceRow> loadAttdSourceRows(ShareReqRaw req) {
        List<SnapshotSourceRow> rows = new ArrayList<>();
        // NF-2a: 근태행의 ATTD_STATUS_CD 는 SQL CASE(반차 미반영) 결과이므로, 확정 부분연차가 있는 날만
        //   PartialLeaveWindowUtils 단일 출처로 재판정해 덮어쓴다(웹 Attd_08/Attd_11·앱과 동일 답).
        rows.addAll(applyHalfLeaveAttdStatus(
                subcon03Mapper.selectAttdSourceRows(
                        req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd()),
                subcon03Mapper.selectHalfLeaveWindows(
                        req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd())));
        rows.addAll(subcon03Mapper.selectOtOnlySourceRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd()));
        rows.addAll(subcon03Mapper.selectLeaveOnlySourceRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd()));
        return rows;
    }

    /**
     * NF-2a(2026-08-07): 확정 부분연차(반차)가 있는 근태행의 {@code ATTD_STATUS_CD} 재판정.
     *
     * <p><b>왜 SQL 이 아니라 Java 인가</b> — 반차 반영 판정은 연차 시각을 그날 <b>원 스케줄 프레임</b>으로
     * 정렬해야 하는데(야간 스케줄에서 스케줄 시작보다 이른 시각은 익일), SQL 의 문자열 CONCAT 비교로는
     * 이 구분이 불가능하다. 산식을 SQL 에 재구현하면 웹 Attd_08/Attd_11·앱과 답이 갈린다(2차 D-1 재발).
     *
     * <p>반차가 없는 날은 SQL CASE 결과를 <b>그대로</b> 둔다(회귀 0).
     * {@code WORK_SEQ} 가 1·2 가 아닌 행도 종전 값을 유지한다 — SQL CASE 가 그 분기에서 판정하지 않으므로
     * (항상 {@code NORMAL}) 재판정 대상이 아니다.
     *
     * <p>스케줄 시각이 빈 문자열인 행(개발 DB 실측: {@code TB_SCH_MGMT.SEC_SCH_STR_TIME=''} 19/47)은
     * SQL 의 {@code IS NOT NULL} 가드를 통과해 <b>무조건 LATE</b> 로 찍히는데, 유틸은 웹 Attd_08 과 같이
     * 공백을 판정 불가로 보고 {@code NORMAL} 을 낸다. 반차일에 한해 유틸 답을 채택한다(6경로 일치가 목적).
     */
    private List<SnapshotSourceRow> applyHalfLeaveAttdStatus(List<SnapshotSourceRow> attdRows,
                                                             List<HalfLeaveWindowRow> windows) {
        if (attdRows == null || attdRows.isEmpty()) {
            return Collections.emptyList();
        }
        if (windows == null || windows.isEmpty()) {
            return attdRows;
        }

        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd = new HashMap<>();
        for (HalfLeaveWindowRow w : windows) {
            if (w.userCd() == null || w.workYmd() == null) {
                continue;
            }
            leaveByUserYmd.computeIfAbsent(w.userCd() + "|" + w.workYmd(), k -> new ArrayList<>())
                    .add(new PartialLeaveWindowUtils.LeaveWindow(w.startTime(), w.endTime()));
        }

        int overridden = 0;
        List<SnapshotSourceRow> out = new ArrayList<>(attdRows.size());
        for (SnapshotSourceRow r : attdRows) {
            List<PartialLeaveWindowUtils.LeaveWindow> leaves =
                    leaveByUserYmd.get(r.userCd() + "|" + r.workYmd());
            boolean judgeable = leaves != null && !leaves.isEmpty()
                    && r.workSeq() != null && (r.workSeq() == 1 || r.workSeq() == 2);
            if (!judgeable) {
                out.add(r);
                continue;
            }
            String status = PartialLeaveWindowUtils.resolveAttdStatus(
                    r.workYmd(), r.planStrTime(), r.planEndTime(), leaves,
                    r.checkInDate(), r.checkInTime(), r.checkOutDate(), r.checkOutTime());
            if (status == null || status.equals(r.attdStatusCd())) {
                out.add(r);
                continue;
            }
            overridden++;
            out.add(r.withAttdStatusCd(status));
        }
        if (overridden > 0) {
            log.info("[subcon03] 반차 반영 근태판정 재산출 - 대상행={}건(전체 {}건)", overridden, attdRows.size());
        }
        return out;
    }

    /**
     * [PS-04·PS-06 공용] 마감 커버리지 필터 + 월별 요약(D-1/D-2) — approve(실제 스냅샷 생성)와
     * approve-info(승인 전 예고)가 <b>동일 계산</b>을 쓴다(예고≠실제 불일치 방지). closedOnlyYn='N'
     * 이면 필터 없이 전량 통과(no-op — 그 옵션은 가이드 대상이 아니므로 월별 요약도 만들지 않는다).
     *
     * <p>필터 순서(§5-4, D-2): 동의 필터(resolveConsentExcluded) <b>이전</b> 단계 — 호출부가
     * {@link CoverageResult#includedRows()} 를 동의 필터의 입력으로 넘긴다(AND 결합, 대체 아님).
     *
     * <p>월 status 판정: 그 달 원천행이 0건이거나 제외행이 0건이면 FULL(제외할 것이 없다 — 데이터가
     * 원래 없던 달을 "미마감" 으로 오표시하지 않는다), 제외 후 포함행이 0건이면 NONE, 그 외 PARTIAL.
     */
    private CoverageResult computeCoverage(ShareReqRaw req, List<SnapshotSourceRow> sourceRows) {
        if (!"Y".equals(req.closedOnlyYn())) {
            return new CoverageResult(sourceRows, Collections.emptyList(), false, 0);
        }

        List<SiteNodeResult> nodeList = subcon03Mapper.selectSiteNodeList(req.prvCmpnyCd(), req.targetSiteCd());
        Set<String> validNodeCds = new HashSet<>();
        Map<String, String> nodeNmByCd = new HashMap<>();
        for (SiteNodeResult n : nodeList) {
            validNodeCds.add(n.nodeCd());
            nodeNmByCd.put(n.nodeCd(), n.nodeNm());
        }

        // (월×유효노드) 판정 메모이즈 — 이 계산 1회(승인 또는 예고) 스코프의 로컬 캐시(필드 캐시 금지).
        Map<String, Boolean> gateCache = new HashMap<>();

        Map<String, Integer> totalByYm = new TreeMap<>();
        Map<String, Integer> excludedByYm = new TreeMap<>();
        Map<String, TreeSet<String>> excludedDeptsByYm = new TreeMap<>();
        Set<String> orphanYms = new TreeSet<>();

        List<SnapshotSourceRow> included = new ArrayList<>();
        int excludedRowCnt = 0;

        for (SnapshotSourceRow row : sourceRows) {
            String ym = row.workYmd().substring(0, 6);
            totalByYm.merge(ym, 1, Integer::sum);

            boolean covered = shareCloseGateService.isRowCovered(
                    req.prvCmpnyCd(), req.targetSiteCd(), row.nodeCd(), ym, validNodeCds, gateCache);
            if (covered) {
                included.add(row);
                continue;
            }

            excludedRowCnt++;
            excludedByYm.merge(ym, 1, Integer::sum);
            boolean orphan = row.nodeCd() == null || row.nodeCd().isBlank() || !validNodeCds.contains(row.nodeCd());
            if (orphan) {
                orphanYms.add(ym);
            } else {
                excludedDeptsByYm.computeIfAbsent(ym, k -> new TreeSet<>())
                        .add(nodeNmByCd.getOrDefault(row.nodeCd(), row.nodeCd()));
            }
        }

        List<CoverageMonthResult> months = new ArrayList<>();
        for (YearMonth ym : monthsOfRange(req.periodStr(), req.periodEnd())) {
            String key = ym.format(YM_FMT);
            int total = totalByYm.getOrDefault(key, 0);
            int excluded = excludedByYm.getOrDefault(key, 0);

            String status;
            if (excluded == 0) {
                status = "FULL";
            } else if (total - excluded == 0) {
                status = "NONE";
            } else {
                status = "PARTIAL";
            }

            List<String> deptNms = capDeptNames(excludedDeptsByYm.get(key));
            String orphanYn = orphanYms.contains(key) ? "Y" : null;
            months.add(new CoverageMonthResult(key, status, deptNms, orphanYn));
        }

        return new CoverageResult(included, months, excludedRowCnt > 0, excludedRowCnt);
    }

    /** 제외 부서명 캡(D-2 META 계약 — 최대 20개 + 초과 시 "외 N개 부서" 1항목). */
    private List<String> capDeptNames(TreeSet<String> deptNms) {
        if (deptNms == null || deptNms.isEmpty()) {
            return Collections.emptyList();
        }
        if (deptNms.size() <= COVERAGE_DEPT_NAME_CAP) {
            return new ArrayList<>(deptNms);
        }
        List<String> result = new ArrayList<>(new ArrayList<>(deptNms).subList(0, COVERAGE_DEPT_NAME_CAP));
        result.add("외 " + (deptNms.size() - COVERAGE_DEPT_NAME_CAP) + "개 부서");
        return result;
    }

    /** 기간이 걸치는 월 집합(YYYYMM 순회) — ShareCloseGateService#monthsOf 와 동일 로직(단, 커버리지
     *  월별 요약 전용이라 별도 서비스로 공용화하지 않는다 — 소규모 순수함수, 중복 비용 낮음). */
    private List<YearMonth> monthsOfRange(String periodStr, String periodEnd) {
        YearMonth start = YearMonth.parse(periodStr.substring(0, 6), YM_FMT);
        YearMonth end = YearMonth.parse(periodEnd.substring(0, 6), YM_FMT);
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth cur = start; !cur.isAfter(end); cur = cur.plusMonths(1)) {
            months.add(cur);
        }
        return months;
    }

    /** 커버리지 META YYYYMM → 응답 "YYYY-MM" 변환(PS-06 — META 저장값은 YYYYMM 그대로 유지). */
    private String fmtYm(String yyyymm) {
        if (yyyymm == null || yyyymm.length() != 6) {
            return yyyymm;
        }
        return yyyymm.substring(0, 4) + "-" + yyyymm.substring(4, 6);
    }

    /**
     * [PS-02 스키마] 커버리지 요약 META JSON 조립 — 월별 status/제외 부서명(실제 제외 행 기준)만
     * 담는다. FULL 월은 excludedDeptNms/orphanUnclosedYn 을 생략한다(불필요한 빈 필드 노출 방지).
     * 메타에 성명·USER_CD 는 절대 담지 않는다(공통 §11 — 부서명·월·건수까지).
     */
    private String buildCoverageMeta(CoverageResult coverage, boolean relayPartialIncludedYn) {
        try {
            List<Map<String, Object>> monthsJson = new ArrayList<>();
            for (CoverageMonthResult m : coverage.months()) {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("ym", m.ym());
                mm.put("status", m.status());
                if (!"FULL".equals(m.status())) {
                    if (m.excludedDeptNms() != null && !m.excludedDeptNms().isEmpty()) {
                        mm.put("excludedDeptNms", m.excludedDeptNms());
                    }
                    if ("Y".equals(m.orphanUnclosedYn())) {
                        mm.put("orphanUnclosedYn", "Y");
                    }
                }
                monthsJson.add(mm);
            }

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("closedOnly", "Y");
            meta.put("months", monthsJson);
            meta.put("relayPartialIncludedYn", relayPartialIncludedYn ? "Y" : "N");
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            // 메타 직렬화 실패는 스냅샷 생성 자체를 막지 않는다(가이드 표시 보조 정보일 뿐 — 승인 롤백 대상 아님).
            log.error("커버리지 요약 META 직렬화 실패(META=null 로 진행) - shareReqId 처리 중", e);
            return null;
        }
    }

    /**
     * 제3자 제공 동의 필터(§5-5, D7) — 구현체 빈이 없으면 전원 포함(T3 스펙).
     *
     * <p>구현체(T4)가 예외를 던지면 <b>삼키지 않는다</b> — 승인 트랜잭션 전체를 롤백해 미동의자 데이터가
     * 반출되는 fail-open 을 원천 차단한다(security 인계 §11-8).
     *
     * @return 제외 대상 사용자코드 집합(동의하지 않은 근로자)
     */
    private Set<String> resolveConsentExcluded(ShareReqRaw req, List<SnapshotSourceRow> sourceRows) {
        // 근로자 구분(REGULAR/DAILY)별로 후보를 나눠 각 동의 경로에 위임한다.
        Map<String, Set<String>> candidatesByType = new HashMap<>();
        for (SnapshotSourceRow row : sourceRows) {
            candidatesByType
                    .computeIfAbsent(row.workerType(), k -> new TreeSet<>())
                    .add(row.userCd());
        }
        return filterExcludedByType(req, candidatesByType);
    }

    /**
     * 동의 필터 코어(§5-7) — 근로자 구분별 후보 집합을 T4 구현체에 위임해 미동의 사용자코드를 반환한다.
     * RISK(INIT_ASSESSOR_ID)/NEARMISS(REPORTER_ID)도 REGULAR 경로로 이 메서드를 공용한다.
     * 구현체 빈이 없으면(T4 미연결) 전원 포함(빈 집합). 예외는 삼키지 않는다(fail-closed — 승인 롤백).
     */
    private Set<String> filterExcludedByType(ShareReqRaw req, Map<String, Set<String>> candidatesByType) {
        List<ThirdPartyConsentProvider> providers = consentProviders.stream().collect(Collectors.toList());
        if (providers.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> excluded = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : candidatesByType.entrySet()) {
            String workerType = entry.getKey();
            Set<String> candidates = entry.getValue();
            if (candidates.isEmpty()) {
                continue;
            }

            Set<String> consented = new HashSet<>(candidates);
            for (ThirdPartyConsentProvider provider : providers) {
                Set<String> filtered = provider.filterConsented(
                        req.prvCmpnyCd(), req.targetSiteCd(), workerType, candidates);
                consented.retainAll(filtered == null ? Collections.emptySet() : filtered);
            }

            for (String userCd : candidates) {
                if (!consented.contains(userCd)) {
                    excluded.add(userCd);
                }
            }
        }
        return excluded;
    }

    // =========================== private — T7 RISK 스냅샷 생성 ===========================

    /** 위험성평가 원천 수집 결과(동의 필터 적용 후 — 헤더 CONSENT_EXCLUDED_CNT 근거 포함). */
    private record RiskCollected(
            List<RiskSourceRow> parents,
            Map<String, List<RiskImproveSourceRow>> improvesByKey,
            Set<String> revalMaskedAssessorIds,
            int excludedCnt) {
    }

    /** 아차사고 원천 수집 결과. */
    private record NearmissCollected(
            List<NearmissSourceRow> rows,
            int excludedCnt) {
    }

    private static String riskKey(String processCd, String assessmentCd) {
        return processCd + "|" + assessmentCd;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    /**
     * 위험성평가 원천 수집 + 동의 필터(§5-4·§5-7, 메인 세션 Q3).
     *
     * <p>동의: INIT_ASSESSOR_ID 미동의 → 평가 레코드(부모+개선항목) 전체 제외. REVAL_ASSESSOR_ID 가 별도이고
     *    미동의면 그 성명만 마스킹(revalAssessorNm=null)하되 재평가 점수/설명은 유지한다.
     *    CONSENT_EXCLUDED_CNT = INIT 미동의로 제외된 작성자 수(인원 기준, T3 패턴).
     */
    private RiskCollected collectRiskSource(ShareReqRaw req) {
        List<RiskSourceRow> allParents = subcon03Mapper.selectRiskSourceRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd(), RISK_CONFIRMED_STATUS);
        List<RiskImproveSourceRow> allImproves = subcon03Mapper.selectRiskImproveSourceRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd(), RISK_CONFIRMED_STATUS);

        // 동의 후보 = INIT/REVAL 작성자 USER_CD 합집합(정규직 경로).
        Set<String> candidateIds = new TreeSet<>();
        for (RiskSourceRow r : allParents) {
            if (r.initAssessorId() != null && !r.initAssessorId().isBlank()) {
                candidateIds.add(r.initAssessorId());
            }
            if (r.revalAssessorId() != null && !r.revalAssessorId().isBlank()) {
                candidateIds.add(r.revalAssessorId());
            }
        }
        Set<String> excludedIds = candidateIds.isEmpty()
                ? Collections.emptySet()
                : filterExcludedByType(req, Map.of(WORKER_TYPE_REGULAR, candidateIds));

        // 포함 부모 = INIT 작성자 미제외. CONSENT_EXCLUDED_CNT = INIT 미동의로 실제 제외된 작성자 수.
        List<RiskSourceRow> included = new ArrayList<>();
        Set<String> excludedInitAssessors = new HashSet<>();
        Set<String> revalMasked = new HashSet<>();
        for (RiskSourceRow r : allParents) {
            String initId = r.initAssessorId();
            if (initId != null && excludedIds.contains(initId)) {
                excludedInitAssessors.add(initId);
                continue; // 레코드 전체 제외
            }
            included.add(r);
            // REVAL 작성자 미동의 → 성명만 마스킹(재평가 데이터는 유지).
            String revalId = r.revalAssessorId();
            if (revalId != null && !revalId.isBlank() && excludedIds.contains(revalId)) {
                revalMasked.add(revalId);
            }
        }

        // 개선항목을 (PROCESS_CD, ASSESSMENT_CD)로 그룹핑(포함 부모만 최종 사용).
        Map<String, List<RiskImproveSourceRow>> improvesByKey = new LinkedHashMap<>();
        for (RiskImproveSourceRow imp : allImproves) {
            improvesByKey.computeIfAbsent(riskKey(imp.processCd(), imp.assessmentCd()), k -> new ArrayList<>()).add(imp);
        }

        return new RiskCollected(included, improvesByKey, revalMasked, excludedInitAssessors.size());
    }

    /**
     * 위험성평가 자체 상세행 INSERT(§5-5) — 평가행 단건 INSERT(부모 DETAIL_ID 회수) + 개선항목 자식 배치.
     * INIT/REVAL/개선항목 첨부는 수신사 소유로 복제. ASSESSOR_SEQ 는 INIT 작성자 로컬 채번(원본 USER_CD 미저장).
     */
    private int insertRiskOwnRows(Long snapshotId, ShareReqRaw req, RiskCollected data,
            String affilCmpnyNm, String actorUserCd) {
        List<RiskSourceRow> parents = data.parents();
        if (parents.isEmpty()) {
            return 0;
        }

        // 작성자 로컬 번호 채번(INIT_ASSESSOR_ID 정렬 → 1..N).
        Set<String> assessorIds = new TreeSet<>();
        for (RiskSourceRow r : parents) {
            if (r.initAssessorId() != null && !r.initAssessorId().isBlank()) {
                assessorIds.add(r.initAssessorId());
            }
        }
        Map<String, Integer> assessorSeqMap = new HashMap<>();
        int seq = 0;
        for (String id : assessorIds) {
            assessorSeqMap.put(id, ++seq);
        }

        int fileCopyCnt = 0;
        int rowSeq = 0;
        int unknownNmCnt = 0;
        for (RiskSourceRow r : parents) {
            rowSeq++;
            int assessorSeq = assessorSeqMap.getOrDefault(r.initAssessorId(), 0);

            String initFile = snapshotFileCopyService.copyFileForOwner(
                    req.prvCmpnyCd(), r.initFileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
            if (r.initFileMgmtCd() != null && !r.initFileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }
            String revalFile = snapshotFileCopyService.copyFileForOwner(
                    req.prvCmpnyCd(), r.revalFileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
            if (r.revalFileMgmtCd() != null && !r.revalFileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }

            // REVAL 미동의 마스킹 — 성명만 null, 재평가 데이터는 유지.
            String revalAssessorNm = (r.revalAssessorId() != null
                    && data.revalMaskedAssessorIds().contains(r.revalAssessorId()))
                    ? null : r.revalAssessorNm();

            if (r.initAssessorNm() == null) {
                unknownNmCnt++;
            }

            SnapshotRiskInsertCommand command = new SnapshotRiskInsertCommand(
                    snapshotId
                    , rowSeq
                    , affilCmpnyNm
                    , assessorSeq
                    , r.processNm()
                    , r.riskTypeNm()
                    , r.hazardNm()
                    , r.assessmentDesc()
                    , r.assessmentStatusNm()
                    , r.initAssessorNm()
                    , r.initLikelihood()
                    , r.initSeverity()
                    , r.initRiskLv()
                    , r.initDesc()
                    , r.initAssessDate()
                    , blankToNull(initFile)
                    , revalAssessorNm
                    , r.revalLikelihood()
                    , r.revalSeverity()
                    , r.revalRiskLv()
                    , r.revalDesc()
                    , r.revalAssessDate()
                    , blankToNull(revalFile)
                    , actorUserCd);
            subcon03Mapper.insertSnapshotRisk(command);
            Long detailId = command.getDetailId();

            // 개선항목 자식 — 부모 DETAIL_ID 아래 배치 INSERT(첨부 복제 후 코드 기록).
            List<RiskImproveSourceRow> improves = data.improvesByKey()
                    .getOrDefault(riskKey(r.processCd(), r.assessmentCd()), Collections.emptyList());
            if (!improves.isEmpty()) {
                List<SnapshotRiskImproveInsertCommand> impCommands = new ArrayList<>(improves.size());
                for (RiskImproveSourceRow imp : improves) {
                    String impFile = snapshotFileCopyService.copyFileForOwner(
                            req.prvCmpnyCd(), imp.fileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
                    if (imp.fileMgmtCd() != null && !imp.fileMgmtCd().isBlank()) {
                        fileCopyCnt++;
                    }
                    impCommands.add(new SnapshotRiskImproveInsertCommand(
                            detailId
                            , snapshotId
                            , imp.improveSeq() == null ? 0 : imp.improveSeq()
                            , imp.improveDate()
                            , imp.improveDesc()
                            , imp.likelihood()
                            , imp.severity()
                            , imp.riskLv()
                            , blankToNull(impFile)
                            , actorUserCd));
                }
                subcon03Mapper.insertSnapshotRiskImproveRows(impCommands);
            }
        }

        if (unknownNmCnt > 0) {
            log.warn("위험성평가 스냅샷 작성자 성명 해석 실패 {}건(정리된 계정 추정) - snapshotId={}", unknownNmCnt, snapshotId);
        }
        warnIfFileCopyExcessive(snapshotId, fileCopyCnt);
        return parents.size();
    }

    /**
     * 릴레이 위험성평가 relabel 복사(§5-6) — 하위(수신 보유) 스냅샷 상세행/개선항목을 상위 새 스냅샷으로 복사한다.
     * 첨부는 수신사(gv) 소유 파일을 상위 수신사(요청자) 소유로 <b>물리 재복제</b>한다. 하위 회사/USER_CD/SNAPSHOT_ID
     * /원본 경로는 어떤 컬럼에도 남기지 않는다(AFFIL 은 제공사 relabel, SEQ 는 오프셋 재채번).
     *
     * @return 복사된 부모 상세행 수
     */
    private int copyRelayRiskRows(Long snapshotId, Long srcSnapshotId, ShareReqRaw req,
            String affilCmpnyNm, String actorUserCd) {
        String gv = req.prvCmpnyCd(); // 승인자(제공사) = 하위 수신 스냅샷 소유자
        List<SnapshotRiskDetailResult> srcRows = subcon03Mapper.selectSnapshotRiskRows(gv, srcSnapshotId);
        if (srcRows.isEmpty()) {
            return 0;
        }
        List<SnapshotRiskImproveResult> srcImproves = subcon03Mapper.selectSnapshotRiskImproveRows(gv, srcSnapshotId);
        Map<Long, List<SnapshotRiskImproveResult>> improvesByDetail = new HashMap<>();
        for (SnapshotRiskImproveResult imp : srcImproves) {
            improvesByDetail.computeIfAbsent(imp.detailId(), k -> new ArrayList<>()).add(imp);
        }

        int rowSeqOffset = subcon03Mapper.selectMaxRiskRowSeq(snapshotId);
        int assessorOffset = subcon03Mapper.selectMaxRiskAssessorSeq(snapshotId);
        int fileCopyCnt = 0;

        for (SnapshotRiskDetailResult r : srcRows) {
            rowSeqOffset++;
            int newAssessorSeq = (r.getAssessorSeq() == null ? 0 : r.getAssessorSeq()) + assessorOffset;

            String initFile = snapshotFileCopyService.copyFileForOwner(
                    gv, r.getInitFileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
            if (r.getInitFileMgmtCd() != null && !r.getInitFileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }
            String revalFile = snapshotFileCopyService.copyFileForOwner(
                    gv, r.getRevalFileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
            if (r.getRevalFileMgmtCd() != null && !r.getRevalFileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }

            SnapshotRiskInsertCommand command = new SnapshotRiskInsertCommand(
                    snapshotId
                    , rowSeqOffset
                    , affilCmpnyNm
                    , newAssessorSeq
                    , r.getProcessNm()
                    , r.getRiskTypeNm()
                    , r.getHazardNm()
                    , r.getAssessmentDesc()
                    , r.getAssessmentStatusNm()
                    , r.getInitAssessorNm()
                    , r.getInitLikelihood()
                    , r.getInitSeverity()
                    , r.getInitRiskLv()
                    , r.getInitDesc()
                    , r.getInitAssessDate()
                    , blankToNull(initFile)
                    , r.getRevalAssessorNm()
                    , r.getRevalLikelihood()
                    , r.getRevalSeverity()
                    , r.getRevalRiskLv()
                    , r.getRevalDesc()
                    , r.getRevalAssessDate()
                    , blankToNull(revalFile)
                    , actorUserCd);
            subcon03Mapper.insertSnapshotRisk(command);
            Long newDetailId = command.getDetailId();

            List<SnapshotRiskImproveResult> improves = improvesByDetail.getOrDefault(r.getDetailId(), Collections.emptyList());
            if (!improves.isEmpty()) {
                List<SnapshotRiskImproveInsertCommand> impCommands = new ArrayList<>(improves.size());
                for (SnapshotRiskImproveResult imp : improves) {
                    String impFile = snapshotFileCopyService.copyFileForOwner(
                            gv, imp.fileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_RISK);
                    if (imp.fileMgmtCd() != null && !imp.fileMgmtCd().isBlank()) {
                        fileCopyCnt++;
                    }
                    impCommands.add(new SnapshotRiskImproveInsertCommand(
                            newDetailId
                            , snapshotId
                            , imp.improveSeq() == null ? 0 : imp.improveSeq()
                            , imp.improveDate()
                            , imp.improveDesc()
                            , imp.likelihood()
                            , imp.severity()
                            , imp.riskLv()
                            , blankToNull(impFile)
                            , actorUserCd));
                }
                subcon03Mapper.insertSnapshotRiskImproveRows(impCommands);
            }
        }

        warnIfFileCopyExcessive(snapshotId, fileCopyCnt);
        return srcRows.size();
    }

    // =========================== private — T7 NEARMISS 스냅샷 생성 ===========================

    /**
     * 아차사고 원천 수집 + 동의 필터(§5-4·§5-7, 메인 세션 Q2).
     * 동의: REPORTER_ID 미동의 → 사고 레코드 전체 제외. CONSENT_EXCLUDED_CNT = 제외된 제보자 수.
     */
    private NearmissCollected collectNearmissSource(ShareReqRaw req) {
        List<NearmissSourceRow> all = subcon03Mapper.selectNearmissSourceRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd(), NEARMISS_CONFIRMED_STATUS);

        Set<String> candidateIds = new TreeSet<>();
        for (NearmissSourceRow r : all) {
            if (r.reporterId() != null && !r.reporterId().isBlank()) {
                candidateIds.add(r.reporterId());
            }
        }
        Set<String> excludedIds = candidateIds.isEmpty()
                ? Collections.emptySet()
                : filterExcludedByType(req, Map.of(WORKER_TYPE_REGULAR, candidateIds));

        List<NearmissSourceRow> included = new ArrayList<>();
        Set<String> excludedReporters = new HashSet<>();
        for (NearmissSourceRow r : all) {
            String rid = r.reporterId();
            if (rid != null && excludedIds.contains(rid)) {
                excludedReporters.add(rid);
                continue;
            }
            included.add(r);
        }
        return new NearmissCollected(included, excludedReporters.size());
    }

    /** 아차사고 자체 상세행 INSERT(§5-5) — 첨부 복제 + REPORTER_SEQ 로컬 채번(원본 USER_CD 미저장). 배치. */
    private int insertNearmissOwnRows(Long snapshotId, ShareReqRaw req, NearmissCollected data,
            String affilCmpnyNm, String actorUserCd) {
        List<NearmissSourceRow> rows = data.rows();
        if (rows.isEmpty()) {
            return 0;
        }

        Set<String> reporterIds = new TreeSet<>();
        for (NearmissSourceRow r : rows) {
            if (r.reporterId() != null && !r.reporterId().isBlank()) {
                reporterIds.add(r.reporterId());
            }
        }
        Map<String, Integer> reporterSeqMap = new HashMap<>();
        int seq = 0;
        for (String id : reporterIds) {
            reporterSeqMap.put(id, ++seq);
        }

        List<SnapshotNearmissInsertCommand> commands = new ArrayList<>(rows.size());
        int fileCopyCnt = 0;
        int rowSeq = 0;
        int unknownNmCnt = 0;
        for (NearmissSourceRow r : rows) {
            rowSeq++;
            String file = snapshotFileCopyService.copyFileForOwner(
                    req.prvCmpnyCd(), r.fileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_NEARMISS);
            if (r.fileMgmtCd() != null && !r.fileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }
            if (r.reporterNm() == null) {
                unknownNmCnt++;
            }
            commands.add(new SnapshotNearmissInsertCommand(
                    snapshotId
                    , rowSeq
                    , affilCmpnyNm
                    , reporterSeqMap.getOrDefault(r.reporterId(), 0)
                    , r.reporterNm()
                    , r.occurDtime()
                    , r.processNm()
                    , r.locationDesc()
                    , r.description()
                    , r.potentialSeverityNm()
                    , r.immediateActionDesc()
                    , r.adminTempActionDesc()
                    , r.causeDesc()
                    , r.preventionDesc()
                    , r.reportStatusNm()
                    , blankToNull(file)
                    , actorUserCd));
        }

        int inserted = 0;
        for (int i = 0; i < commands.size(); i += INSERT_CHUNK_SIZE) {
            List<SnapshotNearmissInsertCommand> chunk =
                    commands.subList(i, Math.min(i + INSERT_CHUNK_SIZE, commands.size()));
            inserted += subcon03Mapper.insertSnapshotNearmissRows(chunk);
        }

        if (unknownNmCnt > 0) {
            log.warn("아차사고 스냅샷 제보자 성명 해석 실패 {}건(정리된 계정 추정) - snapshotId={}", unknownNmCnt, snapshotId);
        }
        warnIfFileCopyExcessive(snapshotId, fileCopyCnt);
        return rows.size();
    }

    /** 릴레이 아차사고 relabel 복사(§5-6) — 첨부 물리 재복제 + REPORTER_SEQ 오프셋 재채번. */
    private int copyRelayNearmissRows(Long snapshotId, Long srcSnapshotId, ShareReqRaw req,
            String affilCmpnyNm, String actorUserCd) {
        String gv = req.prvCmpnyCd();
        List<SnapshotNearmissDetailResult> srcRows = subcon03Mapper.selectSnapshotNearmissRows(gv, srcSnapshotId);
        if (srcRows.isEmpty()) {
            return 0;
        }

        int rowSeqOffset = subcon03Mapper.selectMaxNearmissRowSeq(snapshotId);
        int reporterOffset = subcon03Mapper.selectMaxNearmissReporterSeq(snapshotId);
        int fileCopyCnt = 0;

        List<SnapshotNearmissInsertCommand> commands = new ArrayList<>(srcRows.size());
        for (SnapshotNearmissDetailResult r : srcRows) {
            rowSeqOffset++;
            int newReporterSeq = (r.getReporterSeq() == null ? 0 : r.getReporterSeq()) + reporterOffset;
            String file = snapshotFileCopyService.copyFileForOwner(
                    gv, r.getFileMgmtCd(), req.reqCmpnyCd(), req.reqSiteCd(), FILE_TYPE_NEARMISS);
            if (r.getFileMgmtCd() != null && !r.getFileMgmtCd().isBlank()) {
                fileCopyCnt++;
            }
            commands.add(new SnapshotNearmissInsertCommand(
                    snapshotId
                    , rowSeqOffset
                    , affilCmpnyNm
                    , newReporterSeq
                    , r.getReporterNm()
                    , r.getOccurDtime()
                    , r.getProcessNm()
                    , r.getLocationDesc()
                    , r.getDescription()
                    , r.getPotentialSeverityNm()
                    , r.getImmediateActionDesc()
                    , r.getAdminTempActionDesc()
                    , r.getCauseDesc()
                    , r.getPreventionDesc()
                    , r.getReportStatusNm()
                    , blankToNull(file)
                    , actorUserCd));
        }

        int inserted = 0;
        for (int i = 0; i < commands.size(); i += INSERT_CHUNK_SIZE) {
            List<SnapshotNearmissInsertCommand> chunk =
                    commands.subList(i, Math.min(i + INSERT_CHUNK_SIZE, commands.size()));
            inserted += subcon03Mapper.insertSnapshotNearmissRows(chunk);
        }

        warnIfFileCopyExcessive(snapshotId, fileCopyCnt);
        return srcRows.size();
    }

    /** 첨부 복제 건수 관측(§5-5, Q5) — 소프트 상한 초과 시 WARN 만(하드 상한/차단 없음). */
    private void warnIfFileCopyExcessive(Long snapshotId, int fileCopyCnt) {
        if (fileCopyCnt > FILE_COPY_SOFT_LIMIT) {
            log.warn("스냅샷 첨부 복제 건수 과다(성능 관측) - snapshotId={}, 복제 시도 {}건(소프트 상한 {})",
                    snapshotId, fileCopyCnt, FILE_COPY_SOFT_LIMIT);
        }
    }

    /**
     * 자체 생성 상세행 INSERT — WORKER_SEQ 는 스냅샷 스코프 로컬 번호(제공측 USER_CD 정렬 후 1..N)로 채번한다.
     * <b>USER_CD 는 어떤 컬럼에도 저장하지 않는다</b>(D8).
     */
    private int insertOwnRows(Long snapshotId, ShareReqRaw req, List<SnapshotSourceRow> rows,
                              String affilCmpnyNm, String actorUserCd) {
        if (rows.isEmpty()) {
            return 0;
        }

        // PRAFTA-FIXEDOT-3(M21): (근로자, 근무일) 고정연장 구간 맵 — 상세행별 "고정연장 실적(분)" 산출용.
        //   고정연장이 설정된 배정일만 조회되므로 미사용 사업장은 빈 맵(현행 완전 동일).
        Map<String, FixedOtScheduleRow> fixedOtSchByKey = loadFixedOtScheduleMap(req);

        // PRAFTA-SUBCON-T8-1(D-1): (근로자, 근무일) 확정 시각연차 창 맵 — 상세행별 "인정시간(분)" 산출용.
        //   NF-2a 재판정과 동일 술어(selectHalfLeaveWindows)를 같은 트랜잭션에서 재조회하므로 항상 같은 답.
        //   시각연차가 없는 기간은 빈 맵(차감 0 — 현행 산식 그대로).
        Map<String, List<String[]>> halfLeaveWinByKey = loadHalfLeaveWindowMap(req);

        // 로컬 인물 번호 채번(정규/일용 동명이인도 USER_CD 가 다르면 다른 번호).
        Set<String> userCds = new TreeSet<>();
        for (SnapshotSourceRow row : rows) {
            userCds.add(row.userCd());
        }
        Map<String, Integer> workerSeqMap = new HashMap<>();
        int seq = 0;
        for (String userCd : userCds) {
            workerSeqMap.put(userCd, ++seq);
        }

        List<SnapshotAttdInsertCommand> commands = new ArrayList<>(rows.size());
        int unknownNmCnt = 0;
        for (SnapshotSourceRow row : rows) {
            if (row.workerNm() == null) {
                // 정리된 계정 등 성명 해석 실패 — 누락을 은폐하지 않고 행은 포함하되 경고한다(성명 미출력).
                unknownNmCnt++;
            }
            commands.add(new SnapshotAttdInsertCommand(
                    snapshotId
                    , workerSeqMap.get(row.userCd())
                    , row.workerType()
                    , row.workerNm()
                    , affilCmpnyNm
                    , row.rowType()
                    , row.workYmd()
                    , row.workSeq() == null ? 0 : row.workSeq()
                    , row.schNm()
                    , row.schType()
                    , row.planStrTime()
                    , row.planEndTime()
                    , row.planBrkMin()
                    , row.checkInDate()
                    , row.checkInTime()
                    , row.checkOutDate()
                    , row.checkOutTime()
                    , row.attdStatusCd()
                    , row.otMinutes() == null ? 0 : row.otMinutes()
                    , resolveFixedOtMinutes(fixedOtSchByKey, row)
                    , resolveRecogMinutes(halfLeaveWinByKey, row)
                    , row.leaveNm()
                    , row.leaveDays()
                    , row.leaveMinutes()
                    , row.leaveEndYmd()
                    , actorUserCd));
        }

        if (unknownNmCnt > 0) {
            log.warn("스냅샷 상세행 성명 해석 실패 {}건(정리된 계정 추정) - snapshotId={}", unknownNmCnt, snapshotId);
        }

        int inserted = 0;
        for (int i = 0; i < commands.size(); i += INSERT_CHUNK_SIZE) {
            List<SnapshotAttdInsertCommand> chunk =
                    commands.subList(i, Math.min(i + INSERT_CHUNK_SIZE, commands.size()));
            inserted += subcon03Mapper.insertSnapshotAttdRows(chunk);
        }
        return inserted;
    }

    /**
     * PRAFTA-FIXEDOT-3(M21): 기간 내 고정연장 보유 배정일의 스케줄 시각을 (userCd|workYmd) 맵으로 적재한다.
     * 고정연장 미사용 사업장은 0건 → 빈 맵(스냅샷 생성 경로 현행 완전 동일).
     */
    private Map<String, FixedOtScheduleRow> loadFixedOtScheduleMap(ShareReqRaw req) {
        List<FixedOtScheduleRow> rows = subcon03Mapper.selectFixedOtScheduleRows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, FixedOtScheduleRow> map = new HashMap<>(rows.size() * 2);
        for (FixedOtScheduleRow r : rows) {
            map.put(r.userCd() + "|" + r.workYmd(), r);
        }
        return map;
    }

    /**
     * PRAFTA-FIXEDOT-3(M21): 상세행 1건의 고정연장 실적(분) = 그 행 실근태 구간 ∩ 그날 고정연장 구간
     * (정책 ① 커버분만 — {@code FixedOtMinutesUtils} 단일 출처).
     *
     * <p>행(차수)별로 계산하므로 같은 날 1·2차 합이 곧 그날 실적이다(차수 구간은 서로 겹치지 않음 —
     * 겹침 가드 전제). OT_ONLY/LEAVE_ONLY 행과 미완결(미출근·미퇴근) 행은 0.
     *
     * <p>"연장 미이행" 배지는 스냅샷에 넣지 않는다(plan §5-2 — 판정 보조 정보라 실적 분만 공유).
     */
    private int resolveFixedOtMinutes(Map<String, FixedOtScheduleRow> fixedOtSchByKey, SnapshotSourceRow row) {
        if (fixedOtSchByKey.isEmpty() || !ROW_TYPE_ATTD.equals(row.rowType()) || row.workYmd() == null) {
            return 0;
        }
        FixedOtScheduleRow sch = fixedOtSchByKey.get(row.userCd() + "|" + row.workYmd());
        if (sch == null) {
            return 0; // 그날 배정 근무타입에 고정연장 없음.
        }
        int[] actSeg = FixedOtMinutesUtils.actualSegment(
                FixedOtMinutesUtils.dayAnchorMinutes(row.workYmd(), row.checkInDate(), blankToNull(row.checkInTime())),
                FixedOtMinutesUtils.dayAnchorMinutes(row.workYmd(), row.checkOutDate(), blankToNull(row.checkOutTime())));
        if (actSeg == null) {
            return 0;
        }
        return FixedOtMinutesUtils.dayFixedOtActualMinutes(
                sch.fstSchStrTime(), sch.fstSchEndTime(), sch.secSchStrTime(), sch.secSchEndTime(),
                sch.preFixedOtStrTime(), sch.preFixedOtEndTime(),
                sch.fixedOtStrTime(), sch.fixedOtEndTime(),
                List.of(actSeg));
    }

    /**
     * PRAFTA-SUBCON-T8-1(D-1): 기간 내 확정 시각연차(반차 01 + 시간차 02/03/04) 창을
     * (userCd|workYmd) 맵으로 적재한다 — 상세행별 "인정시간(분)" 산출 입력.
     *
     * <p>NF-2a 재판정({@code applyHalfLeaveAttdStatus})이 쓰는 {@code selectHalfLeaveWindows} 와
     * 동일 술어를 같은 트랜잭션에서 재조회하므로 판정에 쓴 창과 항상 같은 답이다
     * ({@code loadFixedOtScheduleMap} 패턴 미러). 시각연차 없는 기간은 빈 맵.
     */
    private Map<String, List<String[]>> loadHalfLeaveWindowMap(ShareReqRaw req) {
        List<HalfLeaveWindowRow> rows = subcon03Mapper.selectHalfLeaveWindows(
                req.prvCmpnyCd(), req.targetSiteCd(), req.periodStr(), req.periodEnd());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String[]>> map = new HashMap<>(rows.size() * 2);
        for (HalfLeaveWindowRow r : rows) {
            if (r.userCd() == null || r.workYmd() == null) {
                continue;
            }
            map.computeIfAbsent(r.userCd() + "|" + r.workYmd(), k -> new ArrayList<>())
                    .add(new String[] { r.startTime(), r.endTime() });
        }
        return map;
    }

    /**
     * PRAFTA-SUBCON-T8-1: 상세행 1건의 정상근무 인정시간(분) = (실제∩스케줄) − 휴게 − 확정 시각연차 겹침
     * ({@code RecognizedMinutesUtils} 단일 출처 — 웹 Attd_08 recognizedMin 파리티).
     *
     * <p>★null 계약: OT_ONLY/LEAVE_ONLY 행·WORK_SEQ 1/2 외·미출근·미퇴근·스케줄 시각 공백('' 포함)은
     * 전부 <b>null (0 아님)</b> — {@code resolveFixedOtMinutes} 의 0 반환과 계약이 다르다.
     * OT_ONLY 의 인정분은 이미 "초과(분)" 축이 보여주므로 중복 표기하지 않는다(축 분리).
     */
    private Integer resolveRecogMinutes(Map<String, List<String[]>> halfLeaveWinByKey, SnapshotSourceRow row) {
        if (!ROW_TYPE_ATTD.equals(row.rowType())) {
            return null;
        }
        if (row.workSeq() == null || (row.workSeq() != 1 && row.workSeq() != 2)) {
            return null;
        }
        return RecognizedMinutesUtils.recognizedMinutes(
                row.workYmd(),
                row.checkInDate(), blankToNull(row.checkInTime()),
                row.checkOutDate(), blankToNull(row.checkOutTime()),
                blankToNull(row.planStrTime()), blankToNull(row.planEndTime()),
                row.planBrkMin(),
                halfLeaveWinByKey.getOrDefault(row.userCd() + "|" + row.workYmd(), List.of()));
    }

    // =========================== private — 검증/공통 ===========================

    /**
     * 서버측 메뉴 권한 게이트 — 해당 메뉴의 지정 버튼권한을 보유한 역할만 통과(프론트 버튼 숨김에 의존 금지).
     * authCd 는 JWT 클레임 도출값만 신뢰한다(Subcon01/02 패턴 미러).
     */
    private void assertMenuButton(String cmpnyCd, String authCd, String userCd, String menuDId, String btnType) {
        if (authCd == null || authCd.isBlank()
                || subcon03Mapper.selectMenuButtonAuthCnt(cmpnyCd, authCd, menuDId, btnType) <= 0) {
            log.warn("데이터 공유 권한 없음(역할 게이트 차단) - userCd={}, authCd={}, menuDId={}, btnType={}",
                    userCd, authCd, menuDId, btnType);
            throw new ApiException(SubconErrorCode.SUBCON_403_001);
        }
    }

    private void validateShareReqId(Long shareReqId) {
        if (shareReqId == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
    }

    /** 코멘트 길이 제한(DDL varchar(500) — truncation/500 방지). null/빈 값은 통과(거부만 별도 필수 검증). */
    private void validateCommentLength(String comment) {
        if (comment != null && comment.length() > TEXT_MAX_LEN) {
            throw new ApiException(SubconErrorCode.SUBCON_400_004);
        }
    }

    /** YYYYMMDD 형식 검증(정규식+실제 달력일). 실패 시 기간 오류로 통합(형식/범위 무구분). */
    private String validateYmd(String ymd) {
        if (ymd == null || !ymd.matches("^\\d{8}$")) {
            throw new ApiException(SubconErrorCode.SUBCON_400_006);
        }
        try {
            LocalDate.parse(ymd, YMD_FMT);
        } catch (DateTimeParseException e) {
            throw new ApiException(SubconErrorCode.SUBCON_400_006);
        }
        return ymd;
    }

    /** 기간 규칙: 시작 ≤ 종료, 미래 기간 금지(종료 ≤ 오늘/KST), 최대 12개월. */
    private void validatePeriodRange(String periodStr, String periodEnd) {
        LocalDate start = LocalDate.parse(periodStr, YMD_FMT);
        LocalDate end = LocalDate.parse(periodEnd, YMD_FMT);

        if (start.isAfter(end)) {
            throw new ApiException(SubconErrorCode.SUBCON_400_006);
        }
        if (end.isAfter(LocalDate.now(KST))) {
            throw new ApiException(SubconErrorCode.SUBCON_400_006);
        }
        if (end.isAfter(start.plusMonths(MAX_PERIOD_MONTHS))) {
            throw new ApiException(SubconErrorCode.SUBCON_400_006);
        }
    }

    /** 기간 표시 라벨("YYYY-MM-DD ~ YYYY-MM-DD") — 승인 팝업 표시용. */
    private String periodLabel(String periodStr, String periodEnd) {
        return fmtYmd(periodStr) + " ~ " + fmtYmd(periodEnd);
    }

    private String fmtYmd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return ymd == null ? "" : ymd;
        }
        return ymd.substring(0, 4) + "-" + ymd.substring(4, 6) + "-" + ymd.substring(6, 8);
    }

    private ShareReqProcessCommand toProcessCommand(ShareReqProcessParam param) {
        return new ShareReqProcessCommand(
                param.shareReqId()
                , param.gvCmpnyCd()
                , param.gvUserCd()
                , param.comment());
    }
}
