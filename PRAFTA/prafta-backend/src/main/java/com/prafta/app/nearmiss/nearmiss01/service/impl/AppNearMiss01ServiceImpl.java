package com.prafta.app.nearmiss.nearmiss01.service.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.nearmiss.nearmiss01.application.command.ChangeStatusCommand;
import com.prafta.app.nearmiss.nearmiss01.application.command.InsertReportCommand;
import com.prafta.app.nearmiss.nearmiss01.application.command.NotiOutboxCommand;
import com.prafta.app.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.IncidentDetailParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.MyReportListParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.ReportParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.SiteIncidentListParam;
import com.prafta.app.nearmiss.nearmiss01.application.query.IncidentDetailQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.MyReportListQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.NearMissIdSeqQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.SiteIncidentListQuery;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.ReportResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.StatusCountResponse;
import com.prafta.app.nearmiss.nearmiss01.mapper.AppNearMiss01Mapper;
import com.prafta.app.nearmiss.nearmiss01.result.IncidentResult;
import com.prafta.app.nearmiss.nearmiss01.result.ReportMetaResult;
import com.prafta.app.nearmiss.nearmiss01.result.StatusCountResult;
import com.prafta.app.nearmiss.nearmiss01.service.AppNearMiss01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.nearmiss.NearMissErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아차사고/사건 보고 (앱) 서비스 구현.
 *
 * <p>웹(com.prafta.web.nearmiss) 채번/사업장권한/상태전이 로직을 앱에 미러링하되,
 *    앱 관리자 조치 범위는 100->200(검토중) 전환과 임시조치 메모, 반려(900)로 한정한다(plan §4.5).
 * <p>식별자는 모두 JWT 클레임(TokenInfo)에서만 도출하고 본문값은 신뢰하지 않는다(IDOR 차단).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppNearMiss01ServiceImpl implements AppNearMiss01Service {

    // 보고 사진 FILE_TYPE[SYS010] — 004:아차사고 (prafta-app-012-sys010-filetype.sql, D-A3)
    private static final String FILE_TYPE_NEAR_MISS = "004";

    private static final String STATUS_RECEIVED  = "100"; // 접수
    private static final String STATUS_REVIEWING = "200"; // 검토중
    private static final String STATUS_REJECTED  = "900"; // 반려

    // 잠재중대성 ≥ 중대 (SYS062 200 중대 / 300 치명) → 안전관리자 푸시 대상
    private static final String SEVERITY_SERIOUS = "200";
    private static final String SEVERITY_CRITICAL = "300";

    private static final String NOTI_TYPE_REPORTED = "NEAR_MISS_REPORTED";

    /**
     * 사업장 안전관리자 판정 AUTH_CD 집합 (D-A4).
     * prafta-040 권한 시드(NearMiss_01 USE_YN='Y')와 정합:
     *   master/safe/system + 안전직군 사번권한 00001/00004/00006/00008.
     */
    private static final List<String> SAFETY_AUTH_CDS = List.of(
        AuthRoleUtils.AUTH_MASTER   // master
        , AuthRoleUtils.AUTH_SAFETY_MANAGER // safe
        , "system"
        , "00001"
        , "00004"
        , "00006"
        , "00008"
    );

    private final AppNearMiss01Mapper appNearMiss01Mapper;
    private final FileService fileService;
    private final FileMapper fileMapper;
    private final ObjectMapper objectMapper;

    // ── A1 보고 등록 ───────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportResponse report(ReportParam param) {
        TokenInfo tokenInfo = param.tokenInfo();
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        log.info("아차사고 보고 등록 진입 - cmpnyCd={}, siteCd={}, userCd={}, severity={}",
            cmpnyCd, siteCd, userCd, param.potentialSeverityCd());

        try {
            // 1) 사진 첨부가 있으면 fileMgmtCd 발급 + 저장 (단일 파일).
            String fileMgmtCd = null;
            MultipartFile file = param.file();
            if (file != null && !file.isEmpty()) {
                fileMgmtCd = fileMapper.selectFileMgmtCd(
                    FileInfoQuery.from(cmpnyCd, FILE_TYPE_NEAR_MISS));
                fileService.fileSave(FileInfoParam.from(
                    cmpnyCd, userCd, siteCd, FILE_TYPE_NEAR_MISS, fileMgmtCd, file));
            }

            // 2) 채번 (NM + YYYYMMDD + 3자리 SEQ, 사업장+당일 기준).
            String nearMissId = appNearMiss01Mapper.selectNextNearMissId(NearMissIdSeqQuery.from(param));

            // 3) tb_near_miss INSERT (REPORT_STATUS_CD='100', REPORTER_ID/SITE_CD=JWT).
            appNearMiss01Mapper.insertReport(InsertReportCommand.from(param, nearMissId, fileMgmtCd));

            // 4) 잠재중대성 ≥ 중대 신규 보고 시 사업장 안전관리자 푸시 outbox 적재(대상 0명이어도 보고는 성공).
            enqueueSafetyManagerPush(param, nearMissId);

            log.info("아차사고 보고 등록 완료 - nearMissId={}, photo={}",
                nearMissId, fileMgmtCd != null);

            return ReportResponse.builder().nearMissId(nearMissId).build();

        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("아차사고 보고 등록 실패 - siteCd={}, userCd={}", siteCd, userCd, e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    // ── A2 내 보고 목록 ────────────────────────────────────────────
    @Override
    public IncidentListResponse selectMyReports(MyReportListParam param) {
        log.info("아차사고 내 보고 목록 조회 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        List<IncidentResult> list = nullSafe(
            appNearMiss01Mapper.selectMyReportList(MyReportListQuery.from(param)));

        return IncidentListResponse.builder().incidentResultList(list).build();
    }

    // ── A3 사업장 사건 목록 (관리자) ───────────────────────────────
    @Override
    public IncidentListResponse selectSiteIncidents(SiteIncidentListParam param) {
        log.info("아차사고 사업장 목록 조회 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<IncidentResult> list = nullSafe(
            appNearMiss01Mapper.selectSiteIncidentList(SiteIncidentListQuery.from(param)));

        return IncidentListResponse.builder().incidentResultList(list).build();
    }

    // ── A4 상태별 카운트 (관리자) ──────────────────────────────────
    @Override
    public StatusCountResponse selectStatusCounts(SiteIncidentListParam param) {
        log.info("아차사고 상태카운트 조회 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        StatusCountResult statusCount =
            appNearMiss01Mapper.selectStatusCounts(SiteIncidentListQuery.from(param));

        return StatusCountResponse.builder().statusCount(statusCount).build();
    }

    // ── A5 단건 상세 (D-A2: 본인 보고건 OR 사업장 관리자) ───────────
    @Override
    public IncidentInfoResponse selectIncidentInfo(IncidentDetailParam param) {
        log.info("아차사고 상세 조회 - cmpnyCd={}, siteCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        if (!StringUtils.hasText(param.nearMissId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        IncidentDetailQuery query =
            new IncidentDetailQuery(param.siteCd(), param.nearMissId(), param.gvCmpnyCd());

        // 존재 + 보고자 메타 조회(사업장 스코프). 없으면 404.
        ReportMetaResult meta = appNearMiss01Mapper.selectReportMeta(query);
        if (meta == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        // D-A2 접근 게이트: 본인 보고건(REPORTER_ID=gvUserCd) 이면 허용,
        //   아니면 사업장 권한(assertSiteAccess) 보유자만 허용. 둘 다 아니면 403.
        boolean isOwner = param.gvUserCd() != null && param.gvUserCd().equals(meta.reporterId());
        if (!isOwner) {
            assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        }

        IncidentResult incidentInfo = appNearMiss01Mapper.selectIncidentInfo(query);
        if (incidentInfo == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        return IncidentInfoResponse.builder().incidentInfo(incidentInfo).build();
    }

    // ── A6 상태전환 (100->200 + 임시조치 / 900 반려) ───────────────
    @Override
    @Transactional
    public void changeStatus(ChangeStatusParam param) {
        log.info("아차사고 상태전환 진입 - cmpnyCd={}, siteCd={}, nearMissId={}, target={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId(), param.reportStatusCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        if (!StringUtils.hasText(param.nearMissId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String target = param.reportStatusCd();
        if (!StringUtils.hasText(target)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 현재 상태 조회(사업장 스코프). 없으면 404.
        ReportMetaResult meta = appNearMiss01Mapper.selectReportMeta(
            new IncidentDetailQuery(param.siteCd(), param.nearMissId(), param.gvCmpnyCd()));
        if (meta == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
        String current = meta.reportStatusCd();

        ChangeStatusCommand command = ChangeStatusCommand.from(param);

        if (STATUS_REJECTED.equals(target)) {
            // 반려: 사유 필수. 앱은 접수(100) 건만 반려 허용(updateReject WHERE 100 가드).
            if (!StringUtils.hasText(param.rejectReason())) {
                throw new ApiException(NearMissErrorCode.NEARMISS_400_001);
            }
            if (!STATUS_RECEIVED.equals(current)) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
            int updated = appNearMiss01Mapper.updateReject(command);
            if (updated == 0) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
        } else if (STATUS_REVIEWING.equals(target)) {
            // 앱 한정 정방향 전이: 100 -> 200 만 허용.
            if (!STATUS_RECEIVED.equals(current)) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
            int updated = appNearMiss01Mapper.updateFirstReview(command);
            if (updated == 0) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
        } else {
            // 300/400 등 그 외 전이는 앱 비허용(웹 처리).
            throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
        }

        log.info("아차사고 상태전환 완료 - nearMissId={}, {} -> {}",
            param.nearMissId(), current, target);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────

    /**
     * 잠재중대성 ≥ 중대(200/300) 신규 보고 시 사업장 안전관리자에게 푸시 outbox 적재.
     *
     * <p>대상: tb_user_site_auth(SITE_CD, USE_YN='Y') ∩ 안전직군 AUTH_CD(D-A4).
     *    대상자별 1행 INSERT(SEND_STATUS='PENDING'). consumer 미구현 → 적재까지만.
     *    대상 0명이어도 보고는 성공. DEDUP_KEY 충돌(중복 이벤트)은 흡수하고 진행.
     */
    private void enqueueSafetyManagerPush(ReportParam param, String nearMissId) {
        String severity = param.potentialSeverityCd();
        if (!SEVERITY_SERIOUS.equals(severity) && !SEVERITY_CRITICAL.equals(severity)) {
            return; // 경미(100) 또는 미지정 → 미발송(목록 배지만)
        }

        TokenInfo tokenInfo = param.tokenInfo();
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();

        List<String> targets = nullSafe(
            appNearMiss01Mapper.selectSiteSafetyManagers(cmpnyCd, siteCd, SAFETY_AUTH_CDS));
        if (targets.isEmpty()) {
            log.info("아차사고 푸시 대상 없음 - siteCd={}, nearMissId={}", siteCd, nearMissId);
            return;
        }

        String siteNm = StringUtils.hasText(tokenInfo.gv_siteNm()) ? tokenInfo.gv_siteNm() : siteCd;
        String severityNm = SEVERITY_CRITICAL.equals(severity) ? "치명" : "중대";
        String title = String.format("[%s] %s 잠재 아차사고 접수", siteNm, severityNm);
        String body = "현장 아차사고 보고가 접수되었습니다. 확인이 필요합니다.";
        String payload = buildPayload(nearMissId, param.incidentTypeCd(), severity);

        for (String targetUserCd : targets) {
            NotiOutboxCommand cmd = new NotiOutboxCommand(
                cmpnyCd
                , siteCd
                , targetUserCd
                , NOTI_TYPE_REPORTED
                , title
                , body
                , payload
                , "NEARMISS_REPORTED_" + nearMissId + "_" + targetUserCd
                , tokenInfo.gv_userCd()
            );
            try {
                appNearMiss01Mapper.insertNotiOutbox(cmd);
            } catch (DuplicateKeyException dup) {
                // 동일 이벤트 중복 적재(UNIQUE CMPNY_CD,DEDUP_KEY) → 흡수하고 다음 대상 진행
                log.info("아차사고 푸시 중복 적재 무시 - nearMissId={}, target={}", nearMissId, targetUserCd);
            }
        }
        log.info("아차사고 푸시 적재 완료 - nearMissId={}, 대상수={}", nearMissId, targets.size());
    }

    /** DATA_PAYLOAD JSON 직렬화(Jackson). 실패 시 빈 객체로 폴백(푸시 적재 자체는 막지 않음). */
    private String buildPayload(String nearMissId, String incidentTypeCd, String potentialSeverityCd) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("nearMissId", nearMissId);
        data.put("incidentTypeCd", incidentTypeCd);
        data.put("potentialSeverityCd", potentialSeverityCd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("아차사고 푸시 payload 직렬화 실패 - nearMissId={}", nearMissId, e);
            return "{}";
        }
    }

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단).
     * 웹 NearMiss01ServiceImpl.assertSiteAccess 와 동형: 전사권한(master/hr)은 전체 허용,
     *   그 외는 tb_user_site_auth(USE_YN='Y') 매핑 보유 시에만 허용.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        if (AuthRoleUtils.isManager(authCd)) {
            return;
        }
        if (!StringUtils.hasText(siteCd)) {
            log.warn("아차사고 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
        if (appNearMiss01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("아차사고 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
