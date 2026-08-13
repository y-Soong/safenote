package com.prafta.web.user.user09.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.AuditResourceType;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;
import com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO;
import com.prafta.common.error.stdwork.StdWorkErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.user.user09.application.param.SelfJoinApproveParam;
import com.prafta.web.user.user09.application.param.SelfJoinListParam;
import com.prafta.web.user.user09.application.param.SelfJoinRejectParam;
import com.prafta.web.user.user09.application.query.SelfJoinListQuery;
import com.prafta.web.user.user09.dto.response.SelfJoinListResponse;
import com.prafta.web.user.user09.mapper.User09Mapper;
import com.prafta.web.user.user09.result.SelfJoinRowResult;
import com.prafta.web.user.user09.result.SelfJoinTargetResult;
import com.prafta.web.user.user09.service.User09Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-09: 셀프가입 승인/거부 서비스 구현 (User_09).
 *
 * <p>처리 순서(목록/승인/거부 공통)
 * <ol>
 *   <li>사업장 접근 인가 — {@code SiteAccessService.assertSiteAccess}(TB_USER_SITE_AUTH 원장 기반).</li>
 *   <li>부서 스코프 게이트 — {@code AttdCloseService.canManageNodeExcludeSafe}
 *       (master/hr 전사 + 해당·상위 부서 정·부 관리자. plan §8 Q1 확정).</li>
 *   <li>쓰기 경로는 <b>대상 계정의 사업장/부서를 DB 에서 재조회</b>해 게이트 입력으로 쓴다
 *       — 바디 값으로 게이트를 통과시키면 타 부서 신청을 승인할 수 있다.</li>
 * </ol>
 *
 * <p><b>로깅 규약</b> — 휴대폰/이메일 등 PII 평문과 소정근로 <b>사유코드</b>는 로그에 남기지 않는다
 * (사유코드+userCd 조합은 건강·가족관계 정보 — security M-3 규약).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User09ServiceImpl implements User09Service {

    private final User09Mapper user09Mapper;
    /** 사업장 접근 인가(공용 cmm 빈). */
    private final SiteAccessService siteAccessService;
    /** 부서 스코프 게이트 — 기존 빈 재사용(판정 로직 복제 금지). */
    private final AttdCloseService attdCloseService;
    /** 소정근로 이력 단일 출처(등록·검증). 승인 트랜잭션에 참여한다. */
    private final StdWorkHoursService stdWorkHoursService;
    /** 휴대폰 마스킹용 복호화(목록 표시 전용). */
    private final AesGcmCrypto aesGcmCrypto;
    /** 승인/거부 감사 로그(내부 REQUIRES_NEW + 예외 격리). */
    private final AuditLogService auditLogService;

    /** 계정상태[SYS013] '06' 가입승인대기 — 승인/거부의 유일한 출발 상태. */
    private static final String ACCOUNT_STATUS_JOIN_PENDING = "06";

    /** 풀타임 — 주 소정근로분을 회사 통상 기준값으로 서버가 채운다(기준값 하드코딩 금지). */
    private static final String STD_WORK_TYPE_FULL = "FULL";

    /** 직접 입력(단시간) — 주 소정근로 분 필수. */
    private static final String STD_WORK_TYPE_DIRECT = "DIRECT";

    /**
     * 고용형태 [SYS041] 허용 코드.
     *
     * <p>일용직(DAILY)은 QR/일용직 가입 별 계통이라 셀프가입 승인에서 부여할 수 없다
     * (부여되면 소정근로 이력 등록이 {@code STDWORK_403_001} 로 막혀 승인 자체가 실패한다).
     */
    private static final Set<String> ALLOWED_EMPLOYMENT_TYPES = Set.of("REGULAR", "CONTRACT", "EXECUTIVE");

    /** 거부 사유 최대 길이(감사 로그 detailJson 적재 상한). */
    private static final int REJECT_REASON_MAX_LEN = 200;

    // ====================================================================
    // 조회
    // ====================================================================

    @Override
    public SelfJoinListResponse selectSelfJoinList(SelfJoinListParam param) {

        // 1) 사업장 접근 인가.
        siteAccessService.assertSiteAccess(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 2) 부서 스코프 게이트 — 비전사 역할은 부서 미지정 조회가 차단된다(Attd_16 과 동일 UX).
        assertCanManageNode(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd());

        List<SelfJoinRowResult> rows = user09Mapper.selectSelfJoinList(SelfJoinListQuery.from(param));

        List<SelfJoinListResponse.Row> list = new ArrayList<>();
        if (rows != null) {
            for (SelfJoinRowResult row : rows) {
                list.add(SelfJoinListResponse.Row.builder()
                        .userCd(row.userCd())
                        .userId(row.userId())
                        .userNm(row.userNm())
                        .siteCd(row.siteCd())
                        .siteNm(row.siteNm())
                        .nodeCd(row.nodeCd())
                        .nodeNm(row.nodeNm())
                        .mblNo(maskMblNo(decryptMblNo(row.mblNoEnc()), row.mblNoLast4()))
                        .accountStatus(row.accountStatus())
                        .applyDtime(row.applyDtime())
                        .build());
            }
        }

        log.info("User_09 셀프가입 신청 목록 조회 - siteCd={}, nodeCd={}, 상태={}, {}건",
                param.siteCd(), param.nodeCd(), param.accountStatus(), list.size());

        return SelfJoinListResponse.builder()
                .selfJoinList(list)
                .build();
    }

    // ====================================================================
    // 승인
    // ====================================================================

    /**
     * ★트랜잭션 경계 — 계정 활성화와 소정근로 이력을 한 트랜잭션에 묶는다.
     *
     * <p>{@code StdWorkHoursService.register} 는 {@code @Transactional}(REQUIRED) 라 본 트랜잭션에
     * 참여한다. 즉 이력 등록이 어떤 이유로든 실패하면 상태 전이('06'→'01')와 인사정보 보강까지
     * 전부 롤백되어 "소정근로 미기록 활성 계정"이 생기지 않는다.
     *
     * <p>감사 로그는 {@code AuditLogService} 내부에서 REQUIRES_NEW + 예외 격리라 본 트랜잭션의
     * 성패에 영향을 주지 않는다(적재 실패가 승인을 막지 않는다).
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveSelfJoin(SelfJoinApproveParam param, AuditContext auditContext) {

        // 1) 대상 조회 + 권한 게이트(대상의 서버 권위 사업장/부서 기준).
        SelfJoinTargetResult target = loadPendingTarget(
                param.gvCmpnyCd(), param.userCd(), param.gvAuthCd(), param.gvUserCd(), param.gvSiteCd());

        // 2) 입력 검증 — 계정을 건드리기 전에 전부 막는다.
        String hireDate = param.hireDate();
        if (hireDate == null || DateTimeUtils.parseYyyymmdd(hireDate) == null) {
            throw new ApiException(UserErrorCode.USER_400_077);
        }
        if (param.employmentType() == null || !ALLOWED_EMPLOYMENT_TYPES.contains(param.employmentType())) {
            throw new ApiException(UserErrorCode.USER_400_047);
        }
        if (param.rankCd() != null
                && user09Mapper.selectRankCdExists(param.gvCmpnyCd(), param.rankCd()) == 0) {
            throw new ApiException(UserErrorCode.USER_400_078);
        }

        String stdWorkType = param.stdWorkType();
        if (stdWorkType == null
                || (!STD_WORK_TYPE_FULL.equals(stdWorkType) && !STD_WORK_TYPE_DIRECT.equals(stdWorkType))) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }
        if (STD_WORK_TYPE_DIRECT.equals(stdWorkType)
                && (param.stdWorkWeekMinutes() == null || param.stdWorkWeekMinutes() <= 0)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_004);
        }
        // 사유코드는 화면에 내려준 목록으로 서버가 다시 검증한다(클라 바디 신뢰 금지).
        //   SYS083 존재 검증만으로는 NORMAL 이나 단축 사유가 통과해 단시간 판정/기간 규칙이 왜곡된다.
        if (STD_WORK_TYPE_DIRECT.equals(stdWorkType) && param.stdWorkReasonCd() != null
                && !selectableStdWorkReasonCds().contains(param.stdWorkReasonCd())) {
            log.warn("셀프가입 승인 소정근로 사유코드 거부(선택 불가 코드) - 요청자={}", param.gvUserCd());
            throw new ApiException(StdWorkErrorCode.STDWORK_400_005);
        }

        // 3) 인사정보 보강 + 상태 전이('06' → '01').
        int approved = user09Mapper.updateApproveSelfJoin(
                param.gvCmpnyCd(), target.userCd(), hireDate, param.employmentType(),
                param.rankCd(), param.gvUserCd());
        if (approved == 0) {
            // 동시성: 다른 관리자가 먼저 승인/거부함.
            throw new ApiException(UserErrorCode.USER_400_074);
        }

        // 4) 소정근로 이력 INSERT (같은 트랜잭션).
        //    적용 시작일 = 입사일. 승인 시트에서 입사일이 필수라 폴백 분기가 필요 없다.
        //    ★통상 기준값(풀타임 값·단시간 판정 분모)은 대상자의 소속 사업장 기준으로 읽는다.
        //      사업장 오버라이드가 없으면 회사 기본값으로 폴백되어 종전과 결과가 같다.
        //      사업장은 서버 권위값(target)만 쓴다 — 요청 바디의 사업장은 신뢰하지 않는다.
        int cmpnyWeekStdMinutes =
                stdWorkHoursService.resolveSiteWeekStdMinutes(param.gvCmpnyCd(), target.siteCd());
        int weekStdMinutes = STD_WORK_TYPE_FULL.equals(stdWorkType)
                ? cmpnyWeekStdMinutes
                : param.stdWorkWeekMinutes();

        String reasonCd;
        if (STD_WORK_TYPE_FULL.equals(stdWorkType)) {
            reasonCd = StdWorkReasonCd.NORMAL;
        } else if (param.stdWorkReasonCd() != null) {
            reasonCd = param.stdWorkReasonCd();
        } else {
            // 사유 미선택 단시간은 회사 기준값 대비로 판정한다(소정-03 계정 생성 경로와 동일 규칙).
            reasonCd = (weekStdMinutes < cmpnyWeekStdMinutes)
                    ? StdWorkReasonCd.PART_TIME
                    : StdWorkReasonCd.NORMAL;
        }

        StdWorkHoursSaveResult stdWorkResult = stdWorkHoursService.register(StdWorkHoursSaveCommand.builder()
                .cmpnyCd(param.gvCmpnyCd())
                .userCd(target.userCd())
                .applyStrDate(hireDate)
                .applyEndDate(null)
                .weekStdMinutes(weekStdMinutes)
                .reasonCd(reasonCd)
                .reasonDetail(null)
                .actorNo(param.gvUserCd())
                .build());

        // ★사유코드는 로그에 남기지 않는다(security M-3).
        log.info("셀프가입 승인 완료 - 요청자={}, 대상userCd={}, 상태 06→01, 입사일={}, 주소정={}분, 경고={}건",
                param.gvUserCd(), target.userCd(), hireDate, weekStdMinutes,
                stdWorkResult.getWarnings() == null ? 0 : stdWorkResult.getWarnings().size());

        auditLogService.record(AuditLogCommand.builder()
                .cmpnyCd(param.gvCmpnyCd())
                .userCd(param.gvUserCd())
                .actionType(AuditActionType.STATUS_CHANGE)
                .resourceType(AuditResourceType.SELF_JOIN_APPROVAL)
                .resourceKey(target.userCd())
                .detailJson("{\"action\":\"APPROVE\",\"from\":\"06\",\"to\":\"01\"}")
                .build(), auditContext);
    }

    // ====================================================================
    // 거부
    // ====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSelfJoin(SelfJoinRejectParam param, AuditContext auditContext) {

        SelfJoinTargetResult target = loadPendingTarget(
                param.gvCmpnyCd(), param.userCd(), param.gvAuthCd(), param.gvUserCd(), param.gvSiteCd());

        String reason = param.rejectReason();
        if (reason == null || reason.isBlank()) {
            throw new ApiException(UserErrorCode.USER_400_075);
        }
        if (reason.length() > REJECT_REASON_MAX_LEN) {
            throw new ApiException(UserErrorCode.USER_400_076);
        }

        int rejected = user09Mapper.updateRejectSelfJoin(param.gvCmpnyCd(), target.userCd(), param.gvUserCd());
        if (rejected == 0) {
            throw new ApiException(UserErrorCode.USER_400_074);
        }

        log.info("셀프가입 거부 완료 - 요청자={}, 대상userCd={}, 상태 06→07(USE_YN=N)",
                param.gvUserCd(), target.userCd());

        // 거부 사유의 유일한 보존처 = 감사 로그. tb_user 에 사유 컬럼을 두면 재가입 시 행 재활용으로
        //   과거 사유가 덮어써져 이력 가치가 사라진다.
        auditLogService.record(AuditLogCommand.builder()
                .cmpnyCd(param.gvCmpnyCd())
                .userCd(param.gvUserCd())
                .actionType(AuditActionType.STATUS_CHANGE)
                .resourceType(AuditResourceType.SELF_JOIN_APPROVAL)
                .resourceKey(target.userCd())
                .detailJson("{\"action\":\"REJECT\",\"from\":\"06\",\"to\":\"07\",\"reason\":\""
                        + escapeJson(reason) + "\"}")
                .build(), auditContext);
    }

    // ====================================================================
    // 내부
    // ====================================================================

    /**
     * 승인/거부 대상 로드 + 인가 + 상태 검증 (쓰기 경로 공통 진입).
     *
     * <p>게이트 입력(사업장/부서)은 <b>대상 행의 서버 값</b>이다 — 요청 바디의 사업장/부서를 쓰면
     * 관리 가능한 부서 값을 실어 타 부서 신청을 처리할 수 있다.
     */
    private SelfJoinTargetResult loadPendingTarget(String cmpnyCd, String userCd,
                                                   String gvAuthCd, String gvUserCd, String gvSiteCd) {

        SelfJoinTargetResult target = user09Mapper.selectSelfJoinTarget(cmpnyCd, userCd);
        if (target == null) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        siteAccessService.assertSiteAccess(cmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, target.siteCd());
        assertCanManageNode(gvAuthCd, gvUserCd, cmpnyCd, target.siteCd(), target.nodeCd());

        if (!ACCOUNT_STATUS_JOIN_PENDING.equals(target.accountStatus())) {
            throw new ApiException(UserErrorCode.USER_400_074);
        }
        return target;
    }

    /** 부서 스코프 게이트 — master/hr 전사 통과, 그 외는 해당·상위 부서 정·부 관리자만(safe 제외). */
    private void assertCanManageNode(String gvAuthCd, String gvUserCd, String cmpnyCd, String siteCd, String nodeCd) {

        if (!attdCloseService.canManageNodeExcludeSafe(gvAuthCd, gvUserCd, cmpnyCd, siteCd, nodeCd)) {
            log.warn("User_09 접근 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    gvUserCd, gvAuthCd, siteCd, nodeCd);
            throw new ApiException(UserErrorCode.USER_403_002);
        }
    }

    /**
     * 승인 시트에서 <b>선택 가능한</b> 소정근로 사유코드 집합 (서버 재검증용 allow-list).
     *
     * <p>제외 대상
     * <ul>
     *   <li>{@code NORMAL} — 풀타임 라디오가 담당한다. 단시간 입력과 함께 저장되면
     *       "주 20시간인데 통상근로자"가 되어 단시간 파생 판정·비례부여 분모가 틀어진다.</li>
     *   <li>단축 사유(육아기·임신기·가족돌봄) — 적용 종료일이 필수인데 승인 시트에는 기간 입력이
     *       없다. 승인 후 소정근로시간 관리(User_10)에서 기간과 함께 등록한다.</li>
     * </ul>
     *
     * <p>★{@code User01ServiceImpl.selectableStdWorkReasonRules} 와 동일 규칙이다. 두 곳에 있는
     * 이유는 본 작업의 파일 스코프상 {@code common.cmm.stdwork} 를 수정할 수 없었기 때문이며,
     * <b>공통 승격 후보</b>다(예: {@code StdWorkReasonCd.isSelectableOnAccountCreate}).
     * 규칙 변경 시 두 곳을 함께 고칠 것.
     */
    private Set<String> selectableStdWorkReasonCds() {

        Set<String> codes = new HashSet<>();
        for (StdWorkReasonRuleVO rule : stdWorkHoursService.findReasonRules()) {
            if (rule == null || rule.getReasonCd() == null) {
                continue;
            }
            if (StdWorkReasonCd.NORMAL.equals(rule.getReasonCd()) || StdWorkReasonCd.isReduced(rule.getReasonCd())) {
                continue;
            }
            codes.add(rule.getReasonCd());
        }
        return codes;
    }

    /** MBL_NO_ENC(AES-GCM) 복호화. 실패는 목록 조회를 막지 않고 LAST4 폴백으로 넘긴다(평문 로깅 금지). */
    private String decryptMblNo(String mblNoEnc) {

        if (mblNoEnc == null || mblNoEnc.isBlank()) {
            return null;
        }
        try {
            return aesGcmCrypto.decrypt(mblNoEnc);
        } catch (Exception e) {
            log.warn("셀프가입 목록 휴대폰 복호화 실패(LAST4 기반 마스킹으로 대체) - {}", e.getMessage());
            return null;
        }
    }

    /** 휴대폰 가운데 마스킹(User_06 블랙리스트와 동일 규칙). 평문 노출 금지. */
    private String maskMblNo(String digits, String last4) {

        if (digits != null && !digits.isBlank()) {
            String d = digits.replaceAll("\\D", "");
            if (d.length() == 11) {
                return d.substring(0, 3) + "-****-" + d.substring(7);
            }
            if (d.length() == 10) {
                return d.substring(0, 3) + "-***-" + d.substring(6);
            }
        }
        if (last4 != null && !last4.isBlank()) {
            return "***-****-" + last4;
        }
        return "-";
    }

    /** 감사 detailJson 안전화 — 역슬래시/따옴표/제어문자 제거(JSON 파손 방지). */
    private String escapeJson(String value) {

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replaceAll("[\\r\\n\\t]", " ");
    }
}
