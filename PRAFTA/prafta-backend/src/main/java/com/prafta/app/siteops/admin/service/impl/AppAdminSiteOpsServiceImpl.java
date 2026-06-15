package com.prafta.app.siteops.admin.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.siteops.admin.application.command.SiteOpsCheckInCommand;
import com.prafta.app.siteops.admin.application.command.SiteOpsCheckOutCommand;
import com.prafta.app.siteops.admin.application.param.SiteOpsQrParam;
import com.prafta.app.siteops.admin.dto.response.SiteOpsAttendanceResponse;
import com.prafta.app.siteops.admin.mapper.AppAdminSiteOpsMapper;
import com.prafta.app.siteops.admin.result.SiteOpsOpenAttdResult;
import com.prafta.app.siteops.admin.service.AppAdminSiteOpsService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 서비스 구현.
 *
 * <p>정책 출처: attd §7.1(출퇴근 기본)/§7.4(멱등)/§7.5(스케줄 없는 날=표준화/지각/조퇴/연차게이트 미적용).
 * 일용직은 스케줄이 없으므로 정규직 §7.1/§7.5 를 준용하되 단순화한다(NODE_CD=NULL, WORK_SEQ=1,
 * GPS/DEVICE_UUID 미저장, 표준화/지각/조퇴/연차게이트 전부 제외).
 *
 * <p>보안(C1/A-1 비상속/IDOR):
 * <ul>
 *   <li>진입 게이트(SITE_OPS): master∥hr∥safe(canManageAllNodes) ∥ 현재 사업장 nodeAdmin 을 EP 에서 직접 강제.
 *       (AdminAccessResolver SITE_OPS 활성식과 정확히 일치 — 화면 게이팅에 의존하지 않는다.)</li>
 *   <li>식별자(cmpnyCd/userCd/authCd)는 JWT 클레임만 사용. 현장전환 사업장(siteCd)은 바디로 받되 서버가
 *       접근가능 사업장(USE_YN='Y') 멤버십으로 재검증한 뒤 권위로 사용한다(전환 미반영/타사업장 차단).</li>
 *   <li>QR 의 cmpnyCd/siteCd 는 신뢰하지 않고 토큰/확정 사업장과 일치하는지만 검증한다(위·변조 거부).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminSiteOpsServiceImpl implements AppAdminSiteOpsService {

    private final AppAdminSiteOpsMapper appAdminSiteOpsMapper;
    private final AdminScopeMapper adminScopeMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HHmm");

    /** QR 페이로드 방어적 길이 상한(정상 ~3KB). TBM 일용직 QR 입실과 동일. */
    private static final int QR_PAYLOAD_MAX_LENGTH = 4096;

    /**
     * CHECK_IN/OUT_METHOD(SYS031) — 관리자 현장 QR 등록 전용 코드값.
     * self check-in('01')과 통계/감사상 구분되어야 한다(self='01'을 재사용하지 않는다).
     * SYS031 코드 마스터는 비어 있고 실데이터는 '01'만 존재 → 본 프로젝트는 출퇴근방법을 코드 상수로 관리하며
     * 일용직 관리자 QR 등록에 신규 값 '03'을 부여한다(DB 시드 불필요).
     */
    private static final String CHECK_METHOD_ADMIN_QR = "03";

    @Override
    @Transactional
    public SiteOpsAttendanceResponse checkIn(SiteOpsQrParam param) {
        ResolvedTarget t = resolveAndValidate(param, "출근");

        LocalDateTime now = LocalDateTime.now();
        String today = now.format(YMD);
        String checkInTime = now.format(HHMM);

        // 동시 처리(TOCTOU) 직렬화: 일용직 행 비관적 잠금(자연키 UNIQUE 부재 보완).
        appAdminSiteOpsMapper.lockDailyUserForCheckIn(param.gvCmpnyCd(), t.siteCd, t.userCd);

        // 멱등: 당일 열린(미퇴근) 출근행이 이미 있으면 INSERT 하지 않고 안내(2중 열림 방지).
        int open = appAdminSiteOpsMapper.countOpenAttdToday(param.gvCmpnyCd(), t.siteCd, t.userCd, today);
        if (open > 0) {
            log.info("[siteops] 현장 출근 멱등(이미 출근 처리됨) - manager={}, userCd={}, site={}",
                    param.gvUserCd(), t.userCd, t.siteCd);
            throw new ApiException(AttdErrorCode.ATTD_409_080);
        }

        // WORK_SEQ = 당일 기존 출근행 수 + 1(재출근 시 2차 기록 허용). MVP 상한 없음(열린 행 멱등으로 2중 열림 차단).
        int existing = appAdminSiteOpsMapper.countAttdToday(param.gvCmpnyCd(), t.siteCd, t.userCd, today);
        int workSeq = existing + 1;

        String attdId = appAdminSiteOpsMapper.selectAttdId(param.gvCmpnyCd());
        SiteOpsCheckInCommand cmd = new SiteOpsCheckInCommand(
                attdId, param.gvCmpnyCd(), t.siteCd, t.userCd, today, workSeq,
                today, checkInTime, CHECK_METHOD_ADMIN_QR, param.gvUserCd());
        appAdminSiteOpsMapper.insertDailyCheckIn(cmd);

        log.info("[siteops] 현장 출근 등록 완료 - manager={}, userCd={}, site={}, attdId={}, workSeq={}, time={}",
                param.gvUserCd(), t.userCd, t.siteCd, attdId, workSeq, checkInTime);

        return SiteOpsAttendanceResponse.builder()
                .result("CHECKED_IN")
                .userCd(t.userCd)
                .userNmMasked(t.userNmMasked)
                .processedTime(checkInTime)
                .build();
    }

    @Override
    @Transactional
    public SiteOpsAttendanceResponse checkOut(SiteOpsQrParam param) {
        ResolvedTarget t = resolveAndValidate(param, "퇴근");

        LocalDateTime now = LocalDateTime.now();
        String today = now.format(YMD);
        String checkOutTime = now.format(HHMM);

        // 동시 처리(TOCTOU) 직렬화.
        appAdminSiteOpsMapper.lockDailyUserForCheckIn(param.gvCmpnyCd(), t.siteCd, t.userCd);

        // 퇴근 대상 = 당일 열린(미퇴근) 출근행 1건.
        SiteOpsOpenAttdResult openRow =
                appAdminSiteOpsMapper.selectOpenAttdToday(param.gvCmpnyCd(), t.siteCd, t.userCd, today);
        if (openRow == null) {
            int todayCnt = appAdminSiteOpsMapper.countAttdToday(param.gvCmpnyCd(), t.siteCd, t.userCd, today);
            if (todayCnt <= 0) {
                // 당일 출근 기록 자체가 없음.
                log.info("[siteops] 현장 퇴근 거부(출근 기록 없음) - manager={}, userCd={}, site={}",
                        param.gvUserCd(), t.userCd, t.siteCd);
                throw new ApiException(AttdErrorCode.ATTD_409_081);
            }
            // 당일 출근행은 있으나 모두 퇴근됨 → 멱등.
            log.info("[siteops] 현장 퇴근 멱등(이미 퇴근 처리됨) - manager={}, userCd={}, site={}",
                    param.gvUserCd(), t.userCd, t.siteCd);
            throw new ApiException(AttdErrorCode.ATTD_409_082);
        }

        SiteOpsCheckOutCommand cmd = new SiteOpsCheckOutCommand(
                openRow.attdId(), param.gvCmpnyCd(), param.gvUserCd(),
                today, checkOutTime, CHECK_METHOD_ADMIN_QR);
        int updated = appAdminSiteOpsMapper.updateDailyCheckOut(cmd);
        if (updated == 0) {
            // 동시 처리로 이미 퇴근됨(WHERE CHECK_OUT_TIME IS NULL 가드 0건) → 멱등.
            log.info("[siteops] 현장 퇴근 멱등(동시 퇴근 경합) - manager={}, userCd={}, site={}, attdId={}",
                    param.gvUserCd(), t.userCd, t.siteCd, openRow.attdId());
            throw new ApiException(AttdErrorCode.ATTD_409_082);
        }

        log.info("[siteops] 현장 퇴근 등록 완료 - manager={}, userCd={}, site={}, attdId={}, time={}",
                param.gvUserCd(), t.userCd, t.siteCd, openRow.attdId(), checkOutTime);

        return SiteOpsAttendanceResponse.builder()
                .result("CHECKED_OUT")
                .userCd(t.userCd)
                .userNmMasked(t.userNmMasked)
                .processedTime(checkOutTime)
                .build();
    }

    // ====================================================================
    // 공통: 진입 게이트 + 사업장 권위 재검증 + QR 파싱/위변조 + 대상 유효성
    // ====================================================================

    /** 검증 통과 후 확정된 대상 정보(서버 권위 사업장 + 대상 일용직). */
    private record ResolvedTarget(String siteCd, String userCd, String userNmMasked) {
    }

    /**
     * S1/S2 공통 전처리. 통과 시 확정 사업장(siteCd)·대상 일용직(userCd)·마스킹 이름을 반환한다.
     * 실패는 모두 예외(진입 거부=403_002, 사업장/대상 거부=403_040, QR 형식=400_171, 길이=400_170).
     */
    private ResolvedTarget resolveAndValidate(SiteOpsQrParam param, String modeLabel) {
        String cmpnyCd = param.gvCmpnyCd();
        String userCd = param.gvUserCd();
        String authCd = param.gvAuthCd();

        // 1) 진입 게이트(SITE_OPS) — EP 직접 강제(A-1 비상속).
        //    현장전환 사업장(reqSiteCd)이 비면 토큰 사업장(gv_siteCd)으로 폴백.
        String siteCd = StringUtils.hasText(param.reqSiteCd()) ? param.reqSiteCd() : param.gvSiteCd();
        if (!StringUtils.hasText(siteCd)) {
            log.warn("[siteops] 현장 {} 거부: 대상 사업장 미확정 - manager={}", modeLabel, userCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("[siteops] 현장 {} 거부: 접근 차단 권한 - manager={}, authCd={}", modeLabel, userCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        boolean companyWide = AuthRoleUtils.canManageAllNodes(authCd);   // master/hr/safe
        if (!companyWide) {
            // 노드관리자 축: 현재 사업장 기준 자기노드+자손 NODE_CD 집합이 비어있지 않아야 통과.
            List<String> scopedNodeCds = adminScopeMapper.selectScopedNodeCds(
                    ScopedNodeQuery.of(cmpnyCd, siteCd, userCd));
            if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
                log.warn("[siteops] 현장 {} 거부: 진입 권한 없음(노드관리자 아님) - manager={}, site={}",
                        modeLabel, userCd, siteCd);
                throw new ApiException(AttdErrorCode.ATTD_403_002);
            }
        }

        // 2) 현장전환 사업장 권위 재검증(IDOR): 접근가능 사업장(USE_YN='Y') 멤버십이어야 한다.
        //    companyWide 라도 화면이 보낸 siteCd 를 그대로 신뢰하지 않고 멤버십을 확인한다.
        if (appAdminSiteOpsMapper.existsAccessibleSite(cmpnyCd, userCd, siteCd) != 1) {
            log.warn("[siteops] 현장 {} 거부: 접근 불가 사업장(현장전환 권위 검증 실패) - manager={}, site={}",
                    modeLabel, userCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 3) 바디 검증(qrPayload) — 공백/길이 상한.
        String qrPayload = param.qrPayload();
        if (!StringUtils.hasText(qrPayload)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (qrPayload.length() > QR_PAYLOAD_MAX_LENGTH) {
            log.warn("[siteops] 현장 {} 거부: QR 페이로드 길이 초과({}자) - manager={}", modeLabel, qrPayload.length(), userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_170);
        }

        // 4) QR 파싱 — 식별키 userCd(필수). QR 의 cmpnyCd/siteCd 는 신뢰하지 않고 검증용으로만 본다.
        String qrUserCd;
        String qrCmpnyCd;
        String qrSiteCd;
        try {
            JsonNode node = objectMapper.readTree(qrPayload);
            qrUserCd = textOrNull(node, "userCd");
            qrCmpnyCd = textOrNull(node, "cmpnyCd");
            qrSiteCd = textOrNull(node, "siteCd");
        } catch (Exception e) {
            // 파싱 실패/형식오류(직접가입 QR 등) — 평문 PII/payload 원문 로깅 금지.
            log.warn("[siteops] 현장 {} 거부: QR 파싱 실패 - manager={}", modeLabel, userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_171);
        }
        if (!StringUtils.hasText(qrUserCd)) {
            log.warn("[siteops] 현장 {} 거부: QR 식별키(userCd) 누락 - manager={}", modeLabel, userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_171);
        }

        // 5) QR 위변조 검증: QR 의 회사/사업장이 토큰 회사 + 확정 사업장과 일치해야 한다(불일치=타사/타현장 거부).
        if (StringUtils.hasText(qrCmpnyCd) && !qrCmpnyCd.equals(cmpnyCd)) {
            log.warn("[siteops] 현장 {} 거부: QR 회사 불일치 - manager={}, site={}", modeLabel, userCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_040);
        }
        if (StringUtils.hasText(qrSiteCd) && !qrSiteCd.equals(siteCd)) {
            log.warn("[siteops] 현장 {} 거부: QR 사업장 불일치 - manager={}, site={}", modeLabel, userCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_040);
        }

        // 6) 대상 유효성: 확정 사업장 소속 활성 일용직(USE_YN/ACCOUNT_STATUS/WITHDRAWAL/WORK_EXPIRE)인지 서버 재검증.
        int valid = appAdminSiteOpsMapper.countValidDailyUser(cmpnyCd, siteCd, qrUserCd);
        if (valid <= 0) {
            log.warn("[siteops] 현장 {} 거부: 대상 부적합(사업장 밖/만료/탈퇴) - manager={}, userCd={}, site={}",
                    modeLabel, userCd, qrUserCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_040);
        }

        // 7) 슬롯 점유 보강 검증(옵션B): 일용직 계정이 해당 사업장에 활성 바인딩(SLOT_STATUS='02')인지.
        int occupied = appAdminSiteOpsMapper.countOccupiedSlot(cmpnyCd, siteCd, qrUserCd);
        if (occupied <= 0) {
            log.warn("[siteops] 현장 {} 거부: 슬롯 미점유(비활성 바인딩) - manager={}, userCd={}, site={}",
                    modeLabel, userCd, qrUserCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_040);
        }

        // 8) 토스트용 이름(마스킹). 평문 PII 는 응답/로그에 노출하지 않는다.
        String userNm = appAdminSiteOpsMapper.selectDailyUserNm(cmpnyCd, siteCd, qrUserCd);
        return new ResolvedTarget(siteCd, qrUserCd, maskUserNm(userNm));
    }

    /** QR JSON 노드에서 텍스트 필드를 안전 추출(없거나 null 이면 null). TBM textOrNull 동형. */
    private String textOrNull(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    /**
     * 이름 마스킹(가운데 글자 마스킹). 예: 홍길동→홍*동, 김철수→김*수, 박민→박*, 김→김.
     * 4글자 이상은 가운데 전체를 마스킹한다(예: 남궁민수→남**수). null/공백은 빈 표시.
     */
    private String maskUserNm(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim();
        int len = n.length();
        if (len <= 1) {
            return n;
        }
        if (len == 2) {
            return n.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(n.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append('*');
        }
        sb.append(n.charAt(len - 1));
        return sb.toString();
    }
}
