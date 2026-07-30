package com.prafta.web.leave.promotion.leavepromo01.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal;
import com.prafta.common.cmm.leave.promotion.autobatch.LeaveAutoBatchService;
import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionNotiConst;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionRegistrationService;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLogInsertVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionRegisterResult;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.leavepromo.LeavePromoErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionDesignateParam;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionFirstTargetSearchParam;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionTargetSearchParam;
import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionFirstTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.dto.PromotionExcelFailItem;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchCommitRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchPreviewRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionRemindRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.AutoBatchCommitResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionDesignateResultResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionExcelUploadResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionFirstTargetListResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionFirstTargetView;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionRemindResultResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionTargetListResponse;
import com.prafta.web.leave.promotion.leavepromo01.mapper.WebLeavePromo01Mapper;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionFirstTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.service.PromotionExcelFailStore;
import com.prafta.web.leave.promotion.leavepromo01.service.WebLeavePromo01Service;
import com.prafta.web.leave.promotion.leavepromo01.util.PromotionExcelRowParser;
import com.prafta.web.leave.promotion.leavepromo01.util.PromotionExcelTemplateBuilder;
import com.prafta.web.leave.promotion.leavepromo01.vo.DesignateTargetMetaVO;
import com.prafta.web.leave.promotion.leavepromo01.vo.FirstRoundMetaVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-4: 2차 회사직권 연차 사용촉진 웹 서비스 구현.
 *
 * <p>권한: 조회는 canManageNode(master/hr/safe 전사 또는 노드 정·부), 지정은 canManageUser(대상자 소속
 * 부서 관리 권한)로 서버 강제(IDOR). 등록은 공용 헬퍼(stage=SECOND/COMPANY) 재사용. 마스터는 SECOND
 * 회차 행을 STATUS=DESIGNATED 로 갱신(없으면 신규 INSERT — A-2 보고 #5 규약). PUSH 는 afterCommit 적재.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebLeavePromo01ServiceImpl implements WebLeavePromo01Service {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String STAGE_FIRST = "FIRST";
    private static final String STAGE_SECOND = "SECOND";
    private static final String DESIGNATOR_COMPANY = "COMPANY";
    private static final String REASON_SECOND = "연차 사용촉진 2차 지정(회사직권)";

    // 엑셀 업로드 제한(User_01 동일 가드).
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int MAX_DATA_ROWS = 2000;

    // ── 1차 현황/독촉 파생 기준(작업지시서 §4·§5, 확정 D2·D6·D8) ──
    //   ⚠️ 날짜 오프셋 연산은 본 서비스(Java)에서만 수행한다. SQL 에 DATE_ADD/INTERVAL 로 옮기지 말 것
    //      (경계 불일치). 판정 엔진(LeavePromotionServiceImpl)과 같은 값이며 변경 시 양쪽 동시 수정.
    /** 1차 통지 구간 시작: 만료 6개월 전(지연 통지 판별 기준일 산출에 사용). */
    private static final int STAGE1_MONTHS_BEFORE = 6;
    /** 2차 도래 예정: 만료 3개월 전. */
    private static final int STAGE2_MONTHS_BEFORE = 3;
    /** 촉진 절차 상한: 만료 2개월 전(D8 — 이 날 포함 이후는 목록·독촉에서 제외). */
    private static final int PROMOTION_HARD_STOP_MONTHS_BEFORE = 2;
    /** 근로자 계획 제출 기한(역일, D2 — 근로기준법 제61조 "촉구받은 때부터 10일 이내"). */
    private static final int PLAN_SUBMIT_DEADLINE_DAYS = 10;
    /** 구 1차 통지 창 길이(일). 통지일이 (만료-6개월 + 9일)을 넘으면 "지연 통지"(§4 파생 규칙). */
    private static final int LATE_NOTICE_WINDOW_DAYS = 10;
    /** 독촉 일괄 요청 상한(남용 방지). */
    private static final int MAX_REMIND_TARGETS = 200;

    // ── 1차 현황 상태 코드/라벨(서버 산출 — 프론트 재판정 금지) ──
    private static final String STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";
    private static final String STATUS_OVERDUE_NOT_SUBMITTED = "OVERDUE_NOT_SUBMITTED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_LATE_SUBMITTED = "LATE_SUBMITTED";

    // ── 독촉 스킵 사유 코드 ──
    private static final String SKIP_NOT_TARGET = "NOT_TARGET";
    private static final String SKIP_NO_AUTH = "NO_AUTH";
    private static final String SKIP_ROUND_CLOSED = "ROUND_CLOSED";
    private static final String SKIP_ALREADY_SUBMITTED = "ALREADY_SUBMITTED";
    private static final String SKIP_ALREADY_REMINDED_TODAY = "ALREADY_REMINDED_TODAY";

    private final WebLeavePromo01Mapper webLeavePromo01Mapper;
    private final LeavePromotionMapper leavePromotionMapper;
    private final LeavePromotionRegistrationService promotionRegistrationService;
    private final LeavePromotionService leavePromotionService;
    private final LeaveAutoBatchService leaveAutoBatchService;
    private final PromotionExcelFailStore promotionExcelFailStore;
    private final AttdCloseService attdCloseService;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final ObjectMapper objectMapper;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    @Override
    public PromotionTargetListResponse getDesignateTargets(PromotionTargetSearchParam param) {
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        // 노드 권한 게이트 — master/hr/safe 전사 또는 노드 관리자만(PII 노출 조회 화면). 프론트 가드 우회 방어.
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("[webLeavePromo] 2차 대상자 조회 권한 없음 — userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
        }

        String oneYearAgo = LocalDate.now().minusYears(1).format(YMD);
        PromotionTargetSearchQuery query = PromotionTargetSearchQuery.from(param, oneYearAgo);
        List<PromotionTargetRowResult> rows = webLeavePromo01Mapper.selectDesignateTargets(query);

        log.info("[webLeavePromo] 2차 대상자 조회 — siteCd={}, nodeCd={}, incSub={}, tenure={}, 결과 {}건",
                param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.tenureFilter(),
                rows == null ? 0 : rows.size());

        return PromotionTargetListResponse.builder()
                .targetList(rows == null ? new ArrayList<>() : rows)
                .build();
    }

    @Override
    @Transactional
    public PromotionDesignateResultResponse designate(PromotionDesignateParam param) {
        DesignateOutcome outcome = designateOneUser(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(),
                param.targetUserCd(), param.dates(), true);

        return PromotionDesignateResultResponse.builder()
                .designatedDates(outcome.designated())
                .skippedDates(outcome.skipped())
                .failedDates(outcome.failed())
                .build();
    }

    /** 한 사용자에 대한 직권지정 처리 결과(designate/commit/upload 공용). */
    private record DesignateOutcome(
            List<String> designated,
            List<String> skipped,
            List<String> failed
    ) {
    }

    /**
     * 한 사용자에 날짜 다건을 2차/회사직권으로 등록하는 공용 코어(designate/commit/upload 재사용).
     *
     * <p>① 대상자 메타 서버 재조회(스코프) → ② 노드 권한 재검증(canManageUser) → ③ 2차 도래 기준
     * 만료일 확정(재산정 교차체크) → ④ register(SECOND/COMPANY) 다건(멱등=TOCTOU 방어) → ⑤ 신규>0이면
     * 마스터 upsert + PUSH. {@code throwOnAllFail}=true(단건 EP)면 전부 실패 시 예외, false(일괄)면 결과만.
     *
     * @param gvSiteCd 세션 고정 사업장(메타 조회 스코프 — 대상자가 그 사업장 소속이어야 함)
     */
    private DesignateOutcome designateOneUser(String cmpnyCd, String adminUserCd, String adminAuthCd,
                                              String gvSiteCd, String targetUserCd, List<String> dates,
                                              boolean throwOnAllFail) {
        // 1) 대상자 메타 서버 재조회(사업장/부서/SECOND 회차 기준). 세션 고정 사업장 스코프.
        DesignateTargetMetaVO meta =
                webLeavePromo01Mapper.selectDesignateTargetMeta(cmpnyCd, gvSiteCd, targetUserCd);
        if (meta == null) {
            // 대상 사용자 없음/스코프 밖(타 사업장 등).
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_404_001);
        }

        // 2) 노드 권한 재검증 — 대상자 소속부서(서버 조회) 관리 권한(master/hr/safe 전사 또는 노드 정·부).
        if (!attdCloseService.canManageUser(adminAuthCd, adminUserCd, cmpnyCd, meta.getSiteCd(), targetUserCd)) {
            log.warn("[webLeavePromo] 2차 직권지정 권한 없음 — admin={}, target={}, siteCd={}",
                    adminUserCd, targetUserCd, meta.getSiteCd());
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
        }

        // 기준일은 진입 시 1회만 산출(재산정·회차 종료 판정·마스터 기록이 자정 경계에서 갈리지 않게).
        LocalDate today = LocalDate.now();

        // 3) 2차 도래 기준 만료일 확정(§3-3 재산정 교차체크).
        String baseAvailTo = meta.getBaseAvailToDate();
        if (baseAvailTo == null || baseAvailTo.isBlank()) {
            PromotionTargetResult recomputed =
                    leavePromotionService.recomputeForUser(cmpnyCd, targetUserCd, today);
            if (recomputed == null
                    || recomputed.stage() != PromotionTargetResult.PromotionStage.SECOND) {
                log.info("[webLeavePromo] 2차 미도래 대상 — 지정 거부. target={}", targetUserCd);
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_002);
            }
            baseAvailTo = recomputed.baseAvailToDate();
        }

        // 3-1) 회차 종료 게이트(확정 D8) — 기준 만료일 2개월 전 당일 이후는 촉진 절차 자체가 불가하다
        //      (근로기준법 §61 2차 통보 기한). 마스터에 만료일이 있으면 위 재산정을 타지 않으므로,
        //      정상 경로 대부분이 이 검사를 건너뛰고 있었다(qa 지적). 1차 판정·1차 현황 조회·독촉과
        //      동일 술어를 쓴다 — 셋 중 하나만 바꾸면 경계가 어긋나니 변경 시 4곳을 함께 수정할 것.
        LocalDate roundAvailTo = parsePromotionYmd(baseAvailTo);
        if (roundAvailTo == null
                || !today.isBefore(roundAvailTo.minusMonths(PROMOTION_HARD_STOP_MONTHS_BEFORE))) {
            log.info("[webLeavePromo] 2차 직권지정 거부(회차 종료 D8) — target={}, baseAvailTo={}",
                    targetUserCd, baseAvailTo);
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_003);
        }

        // 4) 날짜 다건 직권지정(SECOND/COMPANY). 대상자 소속 사업장으로 등록(서버 메타).
        //    register 는 별도 빈(@Transactional)이라 호출마다 독립 커밋 + DIRECT_USE_KEY 멱등(TOCTOU 방어).
        List<String> designated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (String workYmd : dates) {
            PromotionRegisterResult r = promotionRegistrationService.register(
                    cmpnyCd, meta.getSiteCd(), targetUserCd, workYmd,
                    STAGE_SECOND, DESIGNATOR_COMPANY, REASON_SECOND, adminUserCd);
            switch (r) {
                case REGISTERED -> designated.add(workYmd);
                case SKIPPED_DUP -> skipped.add(workYmd);
                case ATTENDANCE_EXISTS -> {
                    // §9.4 상호배제(B-M1): 출근 기록이 있는 날은 촉진 연차 등록 불가 → 실패 집계.
                    log.info("[webLeavePromo] 출근 기록 존재로 촉진 등록 불가 — target={}, workYmd={}", targetUserCd, workYmd);
                    failed.add(workYmd);
                }
                default -> failed.add(workYmd);
            }
        }

        if (designated.isEmpty()) {
            log.info("[webLeavePromo] 2차 직권지정 신규 0건 — target={}, 스킵 {}, 실패 {}",
                    targetUserCd, skipped.size(), failed.size());
            if (throwOnAllFail && skipped.isEmpty()) {
                // 단건 EP 에서 전부 실패(잔여부족/마감/비근무일)면 4xx.
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_002);
            }
            return new DesignateOutcome(designated, skipped, failed);
        }

        // 5) SECOND 회차 마스터 갱신(STATUS=DESIGNATED). 없으면 신규 INSERT(A-2 보고 #5 규약).
        //    기록일은 위에서 1회 산출한 today 를 재사용한다(재산정·판정과 같은 날짜 보장).
        String todayYmd = today.format(YMD);
        String dedupKey = buildDesignateDedupKey(targetUserCd, baseAvailTo);
        upsertSecondMaster(cmpnyCd, meta.getSiteCd(), targetUserCd, baseAvailTo, todayYmd, dedupKey,
                BigDecimal.valueOf(designated.size()), adminUserCd);

        // 6) PUSH 적재(afterCommit 격리 — 지정 본 흐름에 영향 금지). 신규 지정일만 나열.
        //    PUSH dedupKey 는 이번에 지정된 날짜셋 단위로 분리(M1 수정) — 같은 사용자에 날짜를 나눠
        //    여러 번 지정해도 매 발송 이벤트가 통보된다(마스터 회차 멱등 dedupKey 와 분리).
        String pushDedupKey = buildDesignatePushDedupKey(targetUserCd, baseAvailTo, designated);
        registerDesignatePushAfterCommit(cmpnyCd, meta.getSiteCd(), targetUserCd, designated, pushDedupKey, adminUserCd);

        log.info("[webLeavePromo] 2차 직권지정 — admin={}, target={}, 지정 {}, 스킵 {}, 실패 {}",
                adminUserCd, targetUserCd, designated.size(), skipped.size(), failed.size());

        return new DesignateOutcome(designated, skipped, failed);
    }

    /**
     * SECOND 회차 마스터 upsert. 기존 행 갱신(0행이면 신규 INSERT). 멱등은 UNIQUE(CMPNY_CD,DEDUP_KEY).
     */
    private void upsertSecondMaster(String cmpnyCd, String siteCd, String userCd, String baseAvailTo,
                                    String today, String dedupKey, BigDecimal addDays, String operatorNo) {
        int updated = leavePromotionMapper.markStage2Designated(
                cmpnyCd, dedupKey, today, operatorNo);
        if (updated > 0) {
            return;
        }
        // SECOND 마스터 행이 아직 없음 → 신규 적재(STATUS=DESIGNATED).
        PromotionLogInsertVO vo = new PromotionLogInsertVO();
        vo.setPromoId(leavePromotionMapper.selectNextPromoId(cmpnyCd));
        vo.setCmpnyCd(cmpnyCd);
        vo.setSiteCd(siteCd);
        vo.setUserCd(userCd);
        vo.setBaseGrantId(null);
        vo.setBaseAvailToDate(baseAvailTo);
        vo.setPromoStage(STAGE_SECOND);
        vo.setNoticedDate(null);
        vo.setStage1DesignatedDays(BigDecimal.ZERO);
        vo.setStage2TargetDays(addDays);
        vo.setStage2DesignatedDate(today);
        vo.setStatus("DESIGNATED");
        vo.setDedupKey(dedupKey);
        vo.setInsertNo(operatorNo);
        try {
            leavePromotionMapper.insertPromotionLog(vo);
        } catch (DuplicateKeyException dup) {
            // 동시 적재 경합 — 직전에 다른 트랜잭션이 만든 행을 갱신으로 흡수.
            leavePromotionMapper.markStage2Designated(cmpnyCd, dedupKey, today, operatorNo);
        }
    }

    /** 2차 회차 멱등 키(A-2 보고 #5 규약). 같은 사용자·같은 본연차 만료일 = 같은 2차 회차. */
    private String buildDesignateDedupKey(String userCd, String availTo) {
        return "PROMO_DESIG_" + userCd + "_" + availTo;
    }

    /**
     * 2차 지정 PUSH outbox 멱등 키(M1 수정). 발송 이벤트 단위 분리 — 이번에 새로 지정된 날짜셋의
     * 정렬 해시를 suffix 로 붙인다. (a) 동일 날짜셋 재시도는 멱등 흡수, (b) 서로 다른 날짜셋의 추가
     * 지정은 매번 통보. 마스터 회차 dedupKey(PROMO_DESIG_{user}_{availTo})는 그대로 둔다.
     */
    private String buildDesignatePushDedupKey(String userCd, String availTo, List<String> designatedDates) {
        List<String> sorted = new ArrayList<>(designatedDates);
        sorted.sort(null);
        String joined = String.join(",", sorted);
        // 날짜 다건 시 키 길이 폭주를 막기 위해 정렬조인을 해시(동일 셋=동일 해시로 멱등 유지).
        String hash = Integer.toHexString(joined.hashCode());
        return "PROMO_DESIG_PUSH_" + userCd + "_" + availTo + "_" + hash;
    }

    /**
     * 지정 통보 PUSH outbox 적재를 afterCommit 으로 등록(예외 격리). 트랜잭션 미동기 환경이면 즉시 적재 시도.
     */
    private void registerDesignatePushAfterCommit(String cmpnyCd, String siteCd, String userCd,
                                                  List<String> designatedDates, String dedupKey, String operatorNo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeInsertDesignatePush(cmpnyCd, siteCd, userCd, designatedDates, dedupKey, operatorNo);
                }
            });
        } else {
            safeInsertDesignatePush(cmpnyCd, siteCd, userCd, designatedDates, dedupKey, operatorNo);
        }
    }

    /** 2차 지정 통보 PUSH 1행 적재(PENDING). 예외 흡수(지정 결과에 영향 금지). 멱등 키 = 발송 이벤트 키(M1). */
    private void safeInsertDesignatePush(String cmpnyCd, String siteCd, String userCd,
                                         List<String> designatedDates, String dedupKey, String operatorNo) {
        try {
            String userNm = resolveUserNm(cmpnyCd, userCd);
            String dateText = formatDateList(designatedDates);
            String title = LeavePromotionNotiConst.DESIGNATED_TITLE;
            String body = String.format(LeavePromotionNotiConst.DESIGNATED_BODY_FORMAT, userNm, dateText);

            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(leaveDashboardMapper.selectNextNotiId(cmpnyCd));
            outbox.setCmpnyCd(cmpnyCd);
            outbox.setSiteCd(siteCd);
            outbox.setTargetUserCd(userCd);
            outbox.setNotiType(LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_DESIGNATED);
            outbox.setChannel(LeavePromotionNotiConst.CHANNEL_PUSH);
            outbox.setTitle(title);
            outbox.setBody(body);
            outbox.setDataPayload(buildDesignatePayload(userCd, designatedDates));
            outbox.setSendStatus(LeavePromotionNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(operatorNo);
            leaveDashboardMapper.insertNotiOutbox(outbox);
        } catch (DuplicateKeyException dup) {
            log.info("[webLeavePromo] 2차 지정 PUSH 중복 적재 무시 — userCd={}, dedupKey={}", userCd, dedupKey);
        } catch (Exception e) {
            log.error("[webLeavePromo] 2차 지정 PUSH 적재 실패(지정 영향 없음) — userCd={}", userCd, e);
        }
    }

    /** 근로자명 평문 조회. 미존재/스코프 밖이면 빈 문자열(본문 합성 안전 폴백). */
    private String resolveUserNm(String cmpnyCd, String userCd) {
        String nm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        return (nm == null) ? "" : nm;
    }

    /** 날짜 목록을 "M월 D일, M월 D일" 형태로 합성. */
    private String formatDateList(List<String> dates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dates.size(); i++) {
            String d = dates.get(i);
            if (d == null || d.length() != 8) {
                continue;
            }
            int month = Integer.parseInt(d.substring(4, 6));
            int day = Integer.parseInt(d.substring(6, 8));
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(month).append("월 ").append(day).append("일");
        }
        return sb.toString();
    }

    /** DATA_PAYLOAD(라우팅 키 + 날짜 목록, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildDesignatePayload(String userCd, List<String> dates) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_DESIGNATED);
        data.put("userCd", userCd);
        data.put("stage", STAGE_SECOND);
        data.put("dates", dates);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[webLeavePromo] payload 직렬화 실패 — userCd={}", userCd, e);
            return "{}";
        }
    }

    // ============================================================
    // A-5: 자동배치 프리뷰 / 커밋
    // ============================================================

    @Override
    public BatchProposal previewAutoBatch(AutoBatchPreviewRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = request.getSiteCd();
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
        siteAccessService.assertSiteAccess(cmpnyCd, tokenInfo.gv_userCd(), tokenInfo.gv_authCd(), tokenInfo.gv_siteCd(), siteCd);
        String nodeCd = request.getNodeCd();
        // 노드 권한 게이트 — designate/targets 와 동일(master/hr/safe 전사 또는 노드 관리자).
        if (!attdCloseService.canManageNode(
                tokenInfo.gv_authCd(), tokenInfo.gv_userCd(), cmpnyCd, siteCd, nodeCd)) {
            log.warn("[autobatch] 프리뷰 권한 없음 — userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    tokenInfo.gv_userCd(), tokenInfo.gv_authCd(), siteCd, nodeCd);
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
        }

        String tenure = resolveTenureFilter(request);
        String incSub = (request.getIncSubNodeYn() == null) ? "N" : request.getIncSubNodeYn();
        String userNm = (request.getUserNm() == null) ? "" : request.getUserNm();

        return leaveAutoBatchService.preview(
                cmpnyCd, siteCd, nodeCd, incSub, userNm, tenure,
                request.getStrategy(), request.getWindowFrom(), request.getWindowTo(), LocalDate.now());
    }

    @Override
    public AutoBatchCommitResponse commitAutoBatch(AutoBatchCommitRequest request, TokenInfo tokenInfo) {
        if (request == null || request.getProposal() == null
                || request.getProposal().getAssignments() == null
                || request.getProposal().getAssignments().isEmpty()
                || tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String gvSiteCd = tokenInfo.gv_siteCd();
        String adminUserCd = tokenInfo.gv_userCd();
        String adminAuthCd = tokenInfo.gv_authCd();

        int totalDesignated = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        List<AutoBatchCommitResponse.UserResult> userResults = new ArrayList<>();

        // 사용자별 독립 처리(한 사용자 실패가 타 사용자에 영향 없음 — designateOneUser 내부 register 독립 커밋).
        for (AutoBatchCommitRequest.AssignmentItem item : request.getProposal().getAssignments()) {
            if (item == null || item.getUserCd() == null || item.getUserCd().isBlank()
                    || item.getYmds() == null || item.getYmds().isEmpty()) {
                continue;
            }
            // 날짜 형식 검증(클라 입력 불신뢰).
            List<String> dates = new ArrayList<>();
            for (String d : item.getYmds()) {
                if (d != null && d.length() == 8 && d.chars().allMatch(Character::isDigit)) {
                    dates.add(d);
                }
            }
            if (dates.isEmpty()) {
                continue;
            }
            try {
                // 일괄 모드(throwOnAllFail=false): 권한/스코프 위반은 예외, 잔여부족 등은 결과 집계.
                DesignateOutcome outcome = designateOneUser(
                        cmpnyCd, adminUserCd, adminAuthCd, gvSiteCd, item.getUserCd(), dates, false);
                totalDesignated += outcome.designated().size();
                totalSkipped += outcome.skipped().size();
                totalFailed += outcome.failed().size();
                userResults.add(AutoBatchCommitResponse.UserResult.builder()
                        .userCd(item.getUserCd())
                        .designatedDates(outcome.designated())
                        .skippedDates(outcome.skipped())
                        .failedDates(outcome.failed())
                        .build());
            } catch (ApiException e) {
                // 권한/스코프/미도래 — 해당 사용자만 전부 실패로 집계(타 사용자 진행).
                log.warn("[autobatch] 커밋 사용자 처리 거부 — target={}, code={}",
                        item.getUserCd(), e.getErrorCode() == null ? null : e.getErrorCode().code());
                totalFailed += dates.size();
                userResults.add(AutoBatchCommitResponse.UserResult.builder()
                        .userCd(item.getUserCd())
                        .designatedDates(new ArrayList<>())
                        .skippedDates(new ArrayList<>())
                        .failedDates(dates)
                        .build());
            }
        }

        log.info("[autobatch] 커밋 완료 — admin={}, 사용자 {}명, 지정 {}, 스킵 {}, 실패 {}",
                adminUserCd, userResults.size(), totalDesignated, totalSkipped, totalFailed);

        return AutoBatchCommitResponse.builder()
                .totalDesignated(totalDesignated)
                .totalSkipped(totalSkipped)
                .totalFailed(totalFailed)
                .userResults(userResults)
                .build();
    }

    // ============================================================
    // A-6: 엑셀 양식 / 업로드 / 실패행 2시트
    // ============================================================

    @Override
    public byte[] buildExcelTemplate(PromotionTargetSearchParam param) {
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        // 노드 권한 게이트(조회 화면과 동일).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("[promoExcel] 양식 다운로드 권한 없음 — userCd={}, siteCd={}",
                    param.gvUserCd(), param.siteCd());
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
        }
        String oneYearAgo = LocalDate.now().minusYears(1).format(YMD);
        PromotionTargetSearchQuery query = PromotionTargetSearchQuery.from(param, oneYearAgo);
        List<PromotionTargetRowResult> rows = webLeavePromo01Mapper.selectDesignateTargets(query);
        return PromotionExcelTemplateBuilder.build(rows == null ? new ArrayList<>() : rows);
    }

    @Override
    public PromotionExcelUploadResponse uploadExcel(MultipartFile file, TokenInfo tokenInfo) {
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String gvSiteCd = tokenInfo.gv_siteCd();
        String adminUserCd = tokenInfo.gv_userCd();
        String adminAuthCd = tokenInfo.gv_authCd();

        // 파일 검증.
        if (file == null || file.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".xlsx")) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }

        // 파싱.
        List<PromotionExcelRowParser.ParsedRow> parsed;
        try (InputStream in = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
            }
            Sheet sheet = workbook.getSheetAt(0);
            parsed = PromotionExcelRowParser.parse(sheet);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[promoExcel] 업로드 파싱 실패 — file={}, size={}",
                    originalName, file == null ? null : file.getSize(), e);
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        if (parsed.size() > MAX_DATA_ROWS) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }

        // 행별 등록. 노드 권한/IDOR/마감/잔여 가드는 designateOneUser → register 가 서버 강제.
        int successCount = 0;
        List<PromotionExcelFailItem> fails = new ArrayList<>();
        for (PromotionExcelRowParser.ParsedRow row : parsed) {
            if (row.userCd() == null || row.userCd().isBlank()
                    || row.workYmd() == null || row.workYmd().length() != 8
                    || !row.workYmd().chars().allMatch(Character::isDigit)) {
                fails.add(new PromotionExcelFailItem(row.index(), row.userCd(),
                        "USER_CD 또는 연차사용날짜(YYYYMMDD) 형식 오류", row.sourceRow()));
                continue;
            }
            try {
                DesignateOutcome outcome = designateOneUser(
                        cmpnyCd, adminUserCd, adminAuthCd, gvSiteCd, row.userCd(),
                        List.of(row.workYmd()), false);
                if (!outcome.designated().isEmpty()) {
                    successCount++;
                } else if (!outcome.skipped().isEmpty()) {
                    // 이미 등록(멱등) — 성공으로 간주(중복 재업로드 허용).
                    successCount++;
                } else {
                    fails.add(new PromotionExcelFailItem(row.index(), row.userCd(),
                            "등록 불가(잔여 부족/마감/비근무일)", row.sourceRow()));
                }
            } catch (ApiException e) {
                String msg = e.getResolvedMessage();
                fails.add(new PromotionExcelFailItem(row.index(), row.userCd(),
                        msg == null ? "등록 권한 없음 또는 대상 아님" : msg, row.sourceRow()));
            } catch (Exception e) {
                log.error("[promoExcel] 업로드 행 처리 실패 — index={}, userCd={}",
                        row.index(), row.userCd(), e);
                fails.add(new PromotionExcelFailItem(row.index(), row.userCd(),
                        "처리 중 오류가 발생했습니다.", row.sourceRow()));
            }
        }

        // 실패행 AES-GCM 보관(2시트 다운로드 토큰). 실패 0건이면 토큰 null.
        String failsToken = promotionExcelFailStore.store(cmpnyCd, adminUserCd, fails);

        List<PromotionExcelUploadResponse.FailItemView> failViews = new ArrayList<>();
        for (PromotionExcelFailItem f : fails) {
            failViews.add(PromotionExcelUploadResponse.FailItemView.builder()
                    .userCd(f.userCd()).reason(f.reason()).sourceRow(f.sourceRow()).build());
        }

        log.info("[promoExcel] 업로드 처리 — admin={}, 전체 {}, 성공 {}, 실패 {}",
                adminUserCd, parsed.size(), successCount, fails.size());

        return PromotionExcelUploadResponse.builder()
                .totalCount(parsed.size())
                .successCount(successCount)
                .failCount(fails.size())
                .failItems(failViews)
                .failsToken(failsToken)
                .build();
    }

    @Override
    public byte[] downloadFails(String token, TokenInfo tokenInfo) {
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            return null;
        }
        List<PromotionExcelFailItem> fails =
                promotionExcelFailStore.consume(token, tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        if (fails == null || fails.isEmpty()) {
            return null;
        }
        List<List<String>> sourceRows = new ArrayList<>();
        List<String[]> reasons = new ArrayList<>();
        for (PromotionExcelFailItem f : fails) {
            sourceRows.add(f.sourceRow() == null ? new ArrayList<>() : f.sourceRow());
            reasons.add(new String[] { f.userCd() == null ? "" : f.userCd(), f.reason() == null ? "" : f.reason() });
        }
        return PromotionExcelTemplateBuilder.buildFails(sourceRows, reasons);
    }

    /** 프론트 호환 tenure 별칭 처리(tenureFilter 우선, 비면 tenureType). 기본 ALL. */
    private static String resolveTenureFilter(AutoBatchPreviewRequest request) {
        String t = request.getTenureFilter();
        if (t == null || t.isBlank()) {
            t = request.getTenureType();
        }
        return (t == null || t.isBlank()) ? "ALL" : t;
    }

    // ============================================================
    // 1차 현황 조회(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §5-1)
    // ============================================================

    @Override
    public PromotionFirstTargetListResponse getFirstTargets(PromotionFirstTargetSearchParam param) {
        // 사업장 접근 인가(User_03 원장 기반) — 2차 조회와 완전 동일 게이트.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
                param.gvSiteCd(), param.siteCd());
        // 노드 권한 게이트 — master/hr/safe 전사 또는 노드 관리자만(PII 노출 조회 화면).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("[webLeavePromo] 1차 현황 조회 권한 없음 — userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
        }

        List<PromotionFirstTargetRowResult> rows =
                webLeavePromo01Mapper.selectFirstTargets(PromotionFirstTargetSearchQuery.from(param));

        // 기준일은 진입 시 1회만 산출(행별 재산출 금지 — 경계 판정 결정성).
        LocalDate today = LocalDate.now();

        List<PromotionFirstTargetView> views = new ArrayList<>();
        int notSubmittedCount = 0;
        int overdueNotSubmittedCount = 0;
        int lateNoticeCount = 0;
        int submittedCount = 0;
        int remindableCount = 0;

        if (rows != null) {
            for (PromotionFirstTargetRowResult r : rows) {
                PromotionFirstTargetView v = toFirstTargetView(r, today);
                if (v == null) {
                    // 회차 종료(D8) 또는 통지일/만료일 비정상 — 목록에서 제외.
                    continue;
                }
                views.add(v);
                if (STATUS_NOT_SUBMITTED.equals(v.getStatusCd())) {
                    notSubmittedCount++;
                } else if (STATUS_OVERDUE_NOT_SUBMITTED.equals(v.getStatusCd())) {
                    overdueNotSubmittedCount++;
                } else {
                    // SUBMITTED / LATE_SUBMITTED 는 "제출완료" 로 합산.
                    submittedCount++;
                }
                if ("Y".equals(v.getLateNoticeYn())) {
                    lateNoticeCount++;
                }
                if ("Y".equals(v.getRemindableYn())) {
                    remindableCount++;
                }
            }
        }

        log.info("[webLeavePromo] 1차 현황 조회 — siteCd={}, nodeCd={}, incSub={}, 결과 {}건(미제출 {}, 기한초과 {})",
                param.siteCd(), param.nodeCd(), param.incSubNodeYn(),
                views.size(), notSubmittedCount, overdueNotSubmittedCount);

        return PromotionFirstTargetListResponse.builder()
                .targetList(views)
                .summary(PromotionFirstTargetListResponse.Summary.builder()
                        .totalCount(views.size())
                        .notSubmittedCount(notSubmittedCount)
                        .overdueNotSubmittedCount(overdueNotSubmittedCount)
                        .lateNoticeCount(lateNoticeCount)
                        .submittedCount(submittedCount)
                        .remindableCount(remindableCount)
                        .build())
                .build();
    }

    /**
     * raw 1행을 화면 View 로 변환(모든 날짜 오프셋 연산은 여기서만). 회차 종료/데이터 비정상이면 null.
     *
     * <p>지정 일수(STAGE1_DESIGNATED_DAYS)는 {@code submittedYn} 파생에만 쓰고 View 에 싣지 않는다(D1).
     */
    private PromotionFirstTargetView toFirstTargetView(PromotionFirstTargetRowResult r, LocalDate today) {
        LocalDate noticed = parsePromotionYmd(r.noticedDate());
        LocalDate availTo = parsePromotionYmd(r.baseAvailToDate());
        if (noticed == null || availTo == null) {
            // 통지일 미기록(비정상) 행은 제출 기한 산출이 불가하므로 조회·독촉 대상에서 제외한다.
            log.warn("[webLeavePromo] 1차 현황 행 제외(통지일/기준 만료일 비정상) — userCd={}", r.userCd());
            return null;
        }

        // D8 — 만료 2개월 전 당일부터는 촉진 절차 불가(경계 = 제외).
        if (!today.isBefore(availTo.minusMonths(PROMOTION_HARD_STOP_MONTHS_BEFORE))) {
            return null;
        }

        LocalDate deadline = noticed.plusDays(PLAN_SUBMIT_DEADLINE_DAYS);
        String deadlineYmd = deadline.format(YMD);
        String stage2DueYmd = availTo.minusMonths(STAGE2_MONTHS_BEFORE).format(YMD);

        // 지연 통지 = 통지일이 구 10일 창 종료일(만료-6개월 + 9일)을 넘김(§4 파생 규칙).
        LocalDate lateNoticeLimit = availTo.minusMonths(STAGE1_MONTHS_BEFORE)
                .plusDays(LATE_NOTICE_WINDOW_DAYS - 1L);
        String lateNoticeYn = noticed.isAfter(lateNoticeLimit) ? "Y" : "N";

        BigDecimal designatedDays = r.stage1DesignatedDays();
        boolean submitted = designatedDays != null && designatedDays.signum() > 0;

        // 회차 스코프(이전 회차분 배제) — 통지일 이전 기록은 이번 회차 것이 아니다.
        String firstSubmitDate = scopeToRound(r.firstSubmitDateRaw(), r.noticedDate());
        String lastRemindDate = scopeToRound(r.lastRemindDateRaw(), r.noticedDate());
        int remindCnt = (lastRemindDate == null || r.remindCntRaw() == null) ? 0 : r.remindCntRaw();

        String statusCd;
        String statusNm;
        if (!submitted) {
            if (!today.isAfter(deadline)) {
                statusCd = STATUS_NOT_SUBMITTED;
                statusNm = "미제출";
            } else {
                statusCd = STATUS_OVERDUE_NOT_SUBMITTED;
                statusNm = "기한초과 미제출";
            }
        } else if (firstSubmitDate != null && firstSubmitDate.compareTo(deadlineYmd) > 0) {
            statusCd = STATUS_LATE_SUBMITTED;
            statusNm = "기한후 제출완료";
        } else {
            // 제출 흔적은 있으나 등록일을 특정할 수 없으면 기한 내 제출로 관대 분류(근로자 불이익 추정 금지).
            statusCd = STATUS_SUBMITTED;
            statusNm = "제출완료";
        }

        return PromotionFirstTargetView.builder()
                .userCd(r.userCd())
                .userNm(r.userNm())
                .nodeCd(r.nodeCd())
                .nodeNm(r.nodeNm())
                .siteCd(r.siteCd())
                .siteNm(r.siteNm())
                .noticedDate(r.noticedDate())
                .deadlineDate(deadlineYmd)
                .baseAvailToDate(r.baseAvailToDate())
                .stage2DueDate(stage2DueYmd)
                .submittedYn(submitted ? "Y" : "N")
                .firstSubmitDate(firstSubmitDate)
                .lateNoticeYn(lateNoticeYn)
                .loginNotifiedYn(r.loginNotifiedYn())
                .remindCnt(remindCnt)
                .lastRemindDate(lastRemindDate)
                .remindableYn(submitted ? "N" : "Y")
                .dDay(ChronoUnit.DAYS.between(today, deadline))
                .statusCd(statusCd)
                .statusNm(statusNm)
                .build();
    }

    /** 집계값(YYYYMMDD)의 회차 스코프 적용. 통지일보다 이전이면 이전 회차분이므로 null. */
    private String scopeToRound(String ymd, String noticedYmd) {
        if (ymd == null || ymd.isBlank() || noticedYmd == null || noticedYmd.isBlank()) {
            return null;
        }
        return (ymd.compareTo(noticedYmd) < 0) ? null : ymd;
    }

    /** 촉진 날짜(YYYYMMDD) 파싱. 형식 오류/미기록이면 null. */
    private LocalDate parsePromotionYmd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(ymd, YMD);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // 1차 독촉 재발송(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §5-2, 확정 D4)
    // ============================================================

    @Override
    public PromotionRemindResultResponse remind(PromotionRemindRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 중복 제거(같은 대상 2회 발송 방지) + null/blank 무시.
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        if (request.getUserCds() != null) {
            for (String u : request.getUserCds()) {
                if (u != null && !u.isBlank()) {
                    targets.add(u.trim());
                }
            }
        }
        if (targets.isEmpty() || targets.size() > MAX_REMIND_TARGETS) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String gvSiteCd = tokenInfo.gv_siteCd();
        String adminUserCd = tokenInfo.gv_userCd();
        String adminAuthCd = tokenInfo.gv_authCd();
        // 대상 사업장 = 화면 조회 조건(미전달이면 세션 사업장). 1차 현황 조회가 원장 인가만 통과하면
        // 타 사업장을 반환하므로, 여기서도 같은 사업장을 대상으로 삼아야 조회/발송 범위가 어긋나지 않는다.
        String targetSiteCd = (request.getSiteCd() != null && !request.getSiteCd().isBlank())
                ? request.getSiteCd().trim()
                : gvSiteCd;
        // 사업장 접근 인가(User_03 원장 기반) — 조회 EP 와 동일 게이트. 클라이언트가 보낸 siteCd 는
        // 이 검증을 통과한 뒤에만 대상자 재조회 스코프로 쓰인다(IDOR 방어).
        siteAccessService.assertSiteAccess(cmpnyCd, adminUserCd, adminAuthCd, gvSiteCd, targetSiteCd);

        // 기준일/키 접미는 진입 시 1회 산출(결정성 — 자정 경계에서 키가 갈리지 않게).
        LocalDate today = LocalDate.now();
        String todayYmd = today.format(YMD);
        boolean singleRequest = targets.size() == 1;

        int sentCount = 0;
        int failedCount = 0;
        List<PromotionRemindResultResponse.SkippedItem> skippedItems = new ArrayList<>();
        Map<String, Integer> reasonStat = new LinkedHashMap<>();

        for (String targetUserCd : targets) {
            try {
                // ① 대상자 메타 서버 재조회(사업장/부서/회차) — 클라이언트 값 불신뢰(IDOR).
                //    스코프 사업장은 위에서 인가 검증을 마친 targetSiteCd 다.
                FirstRoundMetaVO meta =
                        webLeavePromo01Mapper.selectFirstRoundMeta(cmpnyCd, targetSiteCd, targetUserCd);
                if (meta == null) {
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_NOT_TARGET);
                    continue;
                }

                // ② 대상자 소속 부서 관리 권한 재검증(서버 조회 사업장 기준).
                if (!attdCloseService.canManageUser(
                        adminAuthCd, adminUserCd, cmpnyCd, meta.getSiteCd(), targetUserCd)) {
                    log.warn("[webLeavePromo] 1차 독촉 권한 없음 — admin={}, target={}, siteCd={}",
                            adminUserCd, targetUserCd, meta.getSiteCd());
                    if (singleRequest) {
                        throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_403_001);
                    }
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_NO_AUTH);
                    continue;
                }

                LocalDate availTo = parsePromotionYmd(meta.getBaseAvailToDate());
                LocalDate noticed = parsePromotionYmd(meta.getNoticedDate());
                if (availTo == null || noticed == null) {
                    // 통지일 미기록(비정상) — 제출 기한 안내가 불가하므로 발송하지 않는다.
                    log.warn("[webLeavePromo] 1차 독촉 제외(통지일/기준 만료일 비정상) — target={}", targetUserCd);
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_NOT_TARGET);
                    continue;
                }

                // ③ 회차 유효(D8) — 만료 2개월 전 당일 이후는 촉진 절차 불가.
                if (!today.isBefore(availTo.minusMonths(PROMOTION_HARD_STOP_MONTHS_BEFORE))) {
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_ROUND_CLOSED);
                    continue;
                }

                // ④ 미제출자만 — 이미 이행한 근로자에 대한 오발송 금지(공통 §10.3).
                BigDecimal designatedDays = meta.getStage1DesignatedDays();
                if (designatedDays != null && designatedDays.signum() > 0) {
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_ALREADY_SUBMITTED);
                    continue;
                }

                // ⑤ 1일 1회 — 사전 count 는 UX 용이고 최종 권위는 UK_NOTI_OUTBOX_DEDUP 이다.
                String dedupKey = buildRemindDedupKey(targetUserCd, meta.getBaseAvailToDate(), todayYmd);
                if (webLeavePromo01Mapper.countRemindToday(cmpnyCd, dedupKey) > 0) {
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_ALREADY_REMINDED_TODAY);
                    continue;
                }

                // ⑥ outbox 1행 적재(PENDING). 마스터(NOTICED_DATE 등)는 어떤 경우에도 갱신하지 않는다(D4).
                boolean inserted = insertRemindOutbox(cmpnyCd, meta.getSiteCd(), targetUserCd,
                        noticed.plusDays(PLAN_SUBMIT_DEADLINE_DAYS), dedupKey, adminUserCd);
                if (inserted) {
                    sentCount++;
                } else {
                    // 동시 클릭 경합 — UNIQUE 충돌을 "오늘 이미 발송" 으로 흡수.
                    addSkip(skippedItems, reasonStat, targetUserCd, SKIP_ALREADY_REMINDED_TODAY);
                }
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                // 사용자 단위 격리 — 한 건 실패가 나머지 발송을 막지 않는다.
                log.error("[webLeavePromo] 1차 독촉 처리 실패 — target={}", targetUserCd, e);
                failedCount++;
            }
        }

        log.info("[webLeavePromo] 1차 독촉 발송 — admin={}, 요청 {}건, 발송 {}, 스킵 {}, 실패 {}, 사유별={}",
                adminUserCd, targets.size(), sentCount, skippedItems.size(), failedCount, reasonStat);

        return PromotionRemindResultResponse.builder()
                .sentCount(sentCount)
                .skippedCount(skippedItems.size())
                .failedCount(failedCount)
                .skippedItems(skippedItems)
                .build();
    }

    /**
     * 독촉 1일 1회 멱등 키(대상·회차·날짜 단위). 신규 접두 PROMO_REMIND_ 를 쓰고, 기존 회차 키
     * (PROMO_NOTICE_ 접두 / PROMO_DESIG_ 접두) 포맷은 절대 건드리지 않는다 — 앱이 같은 포맷을 조립한다.
     */
    private String buildRemindDedupKey(String userCd, String availTo, String todayYmd) {
        return "PROMO_REMIND_" + userCd + "_" + availTo + "_" + todayYmd;
    }

    /** 스킵 1건 집계(사유 코드/라벨). 응답·로그 어디에도 평문 이름을 넣지 않는다. */
    private void addSkip(List<PromotionRemindResultResponse.SkippedItem> items,
                         Map<String, Integer> reasonStat, String userCd, String reasonCd) {
        items.add(PromotionRemindResultResponse.SkippedItem.builder()
                .userCd(userCd)
                .reasonCd(reasonCd)
                .reasonNm(resolveSkipReasonNm(reasonCd))
                .build());
        reasonStat.merge(reasonCd, 1, Integer::sum);
    }

    /** 스킵 사유 라벨(한국어). */
    private String resolveSkipReasonNm(String reasonCd) {
        return switch (reasonCd) {
            case SKIP_NOT_TARGET -> "1차 통지 대상이 아닙니다.";
            case SKIP_NO_AUTH -> "해당 근로자에 대한 관리 권한이 없습니다.";
            case SKIP_ROUND_CLOSED -> "촉진 가능 기간이 지났습니다.";
            case SKIP_ALREADY_SUBMITTED -> "이미 사용 계획을 제출했습니다.";
            case SKIP_ALREADY_REMINDED_TODAY -> "오늘 이미 독촉을 발송했습니다.";
            default -> "발송 대상이 아닙니다.";
        };
    }

    /**
     * 독촉 PUSH outbox 1행 적재(PENDING). 채번/INSERT 는 LeaveDashboardMapper 재사용.
     *
     * @return 신규 적재 true / DEDUP_KEY UNIQUE 충돌(오늘 이미 발송) false
     */
    private boolean insertRemindOutbox(String cmpnyCd, String siteCd, String userCd,
                                       LocalDate deadline, String dedupKey, String operatorNo) {
        String userNm = resolveUserNm(cmpnyCd, userCd);
        String deadlineYmd = deadline.format(YMD);
        String body = String.format(LeavePromotionNotiConst.REMIND_BODY_FORMAT,
                userNm, formatDateList(List.of(deadlineYmd)));

        NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
        outbox.setNotiId(leaveDashboardMapper.selectNextNotiId(cmpnyCd));
        outbox.setCmpnyCd(cmpnyCd);
        outbox.setSiteCd(siteCd);
        outbox.setTargetUserCd(userCd);
        outbox.setNotiType(LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_REMIND);
        outbox.setChannel(LeavePromotionNotiConst.CHANNEL_PUSH);
        outbox.setTitle(LeavePromotionNotiConst.REMIND_TITLE);
        outbox.setBody(body);
        outbox.setDataPayload(buildRemindPayload(userCd, deadlineYmd));
        outbox.setSendStatus(LeavePromotionNotiConst.SEND_STATUS_PENDING);
        outbox.setDedupKey(dedupKey);
        outbox.setInsertNo(operatorNo);
        try {
            leaveDashboardMapper.insertNotiOutbox(outbox);
            return true;
        } catch (DuplicateKeyException dup) {
            log.info("[webLeavePromo] 1차 독촉 중복 적재 흡수(오늘 이미 발송) — userCd={}", userCd);
            return false;
        }
    }

    /** 독촉 DATA_PAYLOAD(라우팅 키만, 평문 이름 미포함). 실패 시 빈 객체 폴백. */
    private String buildRemindPayload(String userCd, String deadlineYmd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_REMIND);
        data.put("userCd", userCd);
        data.put("stage", STAGE_FIRST);
        data.put("deadlineDate", deadlineYmd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[webLeavePromo] 독촉 payload 직렬화 실패 — userCd={}", userCd, e);
            return "{}";
        }
    }
}
