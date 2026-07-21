package com.prafta.web.leave.promotion.leavepromo01.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionTargetSearchParam;
import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.dto.PromotionExcelFailItem;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchCommitRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchPreviewRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.AutoBatchCommitResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionDesignateResultResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionExcelUploadResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionTargetListResponse;
import com.prafta.web.leave.promotion.leavepromo01.mapper.WebLeavePromo01Mapper;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.service.PromotionExcelFailStore;
import com.prafta.web.leave.promotion.leavepromo01.service.WebLeavePromo01Service;
import com.prafta.web.leave.promotion.leavepromo01.util.PromotionExcelRowParser;
import com.prafta.web.leave.promotion.leavepromo01.util.PromotionExcelTemplateBuilder;
import com.prafta.web.leave.promotion.leavepromo01.vo.DesignateTargetMetaVO;

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
    private static final String STAGE_SECOND = "SECOND";
    private static final String DESIGNATOR_COMPANY = "COMPANY";
    private static final String REASON_SECOND = "연차 사용촉진 2차 지정(회사직권)";

    // 엑셀 업로드 제한(User_01 동일 가드).
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int MAX_DATA_ROWS = 2000;

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

        // 3) 2차 도래 기준 만료일 확정(§3-3 재산정 교차체크).
        String baseAvailTo = meta.getBaseAvailToDate();
        if (baseAvailTo == null || baseAvailTo.isBlank()) {
            PromotionTargetResult recomputed =
                    leavePromotionService.recomputeForUser(cmpnyCd, targetUserCd, LocalDate.now());
            if (recomputed == null
                    || recomputed.stage() != PromotionTargetResult.PromotionStage.SECOND) {
                log.info("[webLeavePromo] 2차 미도래 대상 — 지정 거부. target={}", targetUserCd);
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_002);
            }
            baseAvailTo = recomputed.baseAvailToDate();
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
        String today = LocalDate.now().format(YMD);
        String dedupKey = buildDesignateDedupKey(targetUserCd, baseAvailTo);
        upsertSecondMaster(cmpnyCd, meta.getSiteCd(), targetUserCd, baseAvailTo, today, dedupKey,
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
}
