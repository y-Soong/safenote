package com.prafta.platform.company.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.schedule.holiday.service.HolidaySyncService;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.PasswordHasher;
import com.prafta.platform.common.command.CompanyInsertCommand;
import com.prafta.platform.common.command.PlatformUserInsertCommand;
import com.prafta.platform.company.application.command.LeavePolicySeedCommand;
import com.prafta.platform.company.application.command.SiteInsertCommand;
import com.prafta.platform.company.application.command.SiteNodeInsertCommand;
import com.prafta.platform.company.application.command.WorktypeSeedCommand;
import com.prafta.platform.company.application.param.CompanyProvisionParam;
import com.prafta.platform.company.dto.response.CmpnyCdCheckResponse;
import com.prafta.platform.company.dto.response.CompanyProvisionResponse;
import com.prafta.platform.company.mapper.CompanyProvisionMapper;
import com.prafta.platform.company.service.CompanyProvisionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyProvisionServiceImpl implements CompanyProvisionService {

    private final CompanyProvisionMapper companyProvisionMapper;
    private final AesGcmCrypto aesGcmCrypto;
    private final HmacSigner hmacSigner;
    private final PasswordHasher passwordHasher;
    private final HolidaySyncService holidaySyncService;
    // 소정-03: master 계정 소정근로시간 이력 시드(계정 생성 3경로 중 프로비저닝 경로).
    private final StdWorkHoursService stdWorkHoursService;

    /** 템플릿(시드 원천) 회사코드 — 권한/운영사변수 복제 기준. */
    private static final String TEMPLATE_CMPNY_CD = "001";

    /**
     * 회사코드 허용 형식 — 영문 대문자·숫자 2~20자.
     *
     * <p>2026-08-16 운영자 직접 입력으로 전환하면서 도입. 종전에는 서버가 랜덤 20자를 발급했다
     * (추측 불가로 미승인 가입을 막으려던 설계). 모든 사용자가 가입 시 관리자 승인을 받는 구조가
     * 되어 코드 복잡도가 방어 수단일 이유가 사라졌다.
     *
     * <p>상한 20자는 종전 발급 길이를 그대로 이어받은 것이고, 하한 2자는 한 글자 코드가
     * 오타로 만들어지는 것을 막기 위한 최소선이다.
     */
    private static final Pattern CMPNY_CD_PATTERN = Pattern.compile("^[A-Z0-9]{2,20}$");

    /** 최초 노드 코드/타입(Baim01 최초 1depth 노드 패턴 미러). */
    private static final String FIRST_NODE_CD = "n1";
    private static final String FIRST_NODE_TYPE = "00001";

    /** 기본 근무타입(1구간 09:00~18:00) 시드 상수. */
    private static final String DEFAULT_SCH_CD = "ST001";
    private static final String DEFAULT_SCH_NO = "기본";
    private static final String DEFAULT_SCH_TYPE = "01"; // SYS019 1구간
    private static final String DEFAULT_SCH_STR_TIME = "0900";
    private static final String DEFAULT_SCH_END_TIME = "1800";

    /** 휴대폰 자리수 검증 범위(User01ServiceImpl 와 동일 규약). */
    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    /** 관리자 ID 길이 범위. */
    private static final int ADMIN_ID_MIN_LENGTH = 3;
    private static final int ADMIN_ID_MAX_LENGTH = 50;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyProvisionResponse provisionCompany(CompanyProvisionParam param) {

        // 1) 필수값 검증.
        if (isBlank(param.cmpnyNm()) || isBlank(param.bsnsLcnNo())
                || isBlank(param.adminNm()) || isBlank(param.adminId()) || isBlank(param.adminMbl())) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_001);
        }

        // 2) 사업자번호 정규화/형식 검증(숫자 10자리).
        String bsnsLcnNo = param.bsnsLcnNo().replaceAll("\\D", "");
        if (bsnsLcnNo.length() != 10) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_002);
        }

        // 3) 관리자 ID 형식 검증(3~50자, 영문/숫자/특수문자(_,-,.) 조합).
        String adminId = param.adminId().trim();
        if (adminId.length() < ADMIN_ID_MIN_LENGTH || adminId.length() > ADMIN_ID_MAX_LENGTH) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_006);
        }
        if (!adminId.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_007);
        }

        // 4) 관리자 휴대폰 정규화/형식 검증(숫자 10~11자리).
        String phoneNorm = Normalizers.normalizePhone(param.adminMbl());
        if (phoneNorm == null
                || phoneNorm.length() < PHONE_MIN_DIGITS
                || phoneNorm.length() > PHONE_MAX_DIGITS) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_003);
        }

        // 5) 휴대폰번호 중복 검사(전사 대상 — 다른 고객사의 master 계정 중복 여부).
        //    HMAC 은 DB 함수가 아니라 애플리케이션(HmacSigner)에서 시크릿 키로 계산해 비교한다(저장 시와 동일 규약).
        if (companyProvisionMapper.selectPhoneNumberExists(hmacSigner.hmacSha256Base64Url(phoneNorm)) > 0) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_008);
        }

        // 5-1) 관리자 ID 중복 검사(전사 대상).
        //      로그인은 회사코드 없이 USER_ID 만으로 사용자를 찾으므로 ID 는 전역 유일해야 한다
        //      (UNIQUE UX_TB_USER_ID(USER_ID)). 사전 검사가 없으면 INSERT 단계에서 제약 위반 500 이 난다.
        if (companyProvisionMapper.selectUserIdExists(null, adminId) > 0) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_009);
        }

        // 6) 계약 종료일 형식 검증(입력 시만, YYYYMMDD 8자리).
        String contractEndDate = normalizeYmdOrNull(param.contractEndDate());
        if (!isBlank(param.contractEndDate()) && contractEndDate == null) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_004);
        }

        // 7) 회사코드 확정 — 운영자 직접 입력(2026-08-16 전환, 종전 서버 랜덤 20자 발급).
        //    형식 검증 + 중복 검사를 여기서 한다. CMPNY_CD 는 22개 테이블 복합 PK 선두 컬럼이라
        //    한 번 저장되면 사실상 되돌릴 수 없다 — 저장 직전 마지막 관문이다.
        String cmpnyCd = resolveInputCmpnyCd(param.cmpnyCd());

        // 8) TB_CMPNY INSERT(계약 고객사 — CONTRACT_YN='Y', USE_YN='Y').
        companyProvisionMapper.insertCmpny(new CompanyInsertCommand(
                cmpnyCd
                , param.cmpnyNm().trim()
                , bsnsLcnNo
                , "Y"
                , "Y"
                , contractEndDate
                , param.gvUserCd()
        ));

        // 9) 채번: SITE_CD / USER_CD. NODE_CD 는 최초 노드 'n1' 고정(Baim01 패턴).
        String siteCd = companyProvisionMapper.selectNextSiteCd(cmpnyCd);
        String userCd = companyProvisionMapper.selectNextUserCd(cmpnyCd);
        String nodeCd = FIRST_NODE_CD;
        String todayYmd = LocalDate.now().format(YMD);

        // 10) master 계정 PII/비밀번호 가공(User01ServiceImpl 와 동일 규약).
        //     USER_ID = 관리자가 지정한 adminId, 초기 비밀번호 = 휴대폰번호(BCrypt).
        String userId = adminId;
        String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
        String phoneLast4 = Normalizers.last4(phoneNorm);
        String userPw = passwordHasher.hash(phoneNorm);

        // 11) TB_USER INSERT(master, ACCOUNT_STATUS='04' 인증대기 — 첫 로그인 SMS 본인인증).
        companyProvisionMapper.insertUser(new PlatformUserInsertCommand(
                cmpnyCd
                , userCd
                , userId
                , param.adminNm().trim()
                , userPw
                , siteCd
                , nodeCd
                , AuthRoleUtils.AUTH_MASTER
                , "04"
                , phoneEnc
                , phoneHmac
                , phoneLast4
                , param.gvUserCd()
        ));

        // 11-0) 통상근로자 주 소정근로 기준값(TB_CMPNY_STD_WORK_POLICY, COMPANY 스코프) 1행.
        //   ★미입력이면 행을 만들지 않는다 — 코드 폴백 2400분(주 40시간)이 그대로 적용된다(현행 동작 유지).
        //   값 범위 검증(0 초과 ~ 2400분)과 저장은 StdWorkHoursService 단일 출처가 담당한다.
        //   아래 master 시드가 이 값을 읽으므로 반드시 시드보다 먼저 저장해야 한다.
        //   saveWeekStdMinutesPolicy 는 @Transactional(REQUIRED) 라 본 프로비저닝 트랜잭션에 참여한다.
        if (param.weekStdMinutes() != null) {
            stdWorkHoursService.saveWeekStdMinutesPolicy(cmpnyCd, null, param.weekStdMinutes(), param.gvUserCd());
        }

        // 11-1) 소정-03: master 계정 소정근로시간 이력 시드(풀타임 NORMAL 1행).
        //   계정 생성 3경로 중 프로비저닝 경로 — 여기서 넣지 않으면 신규 고객사의 첫 계정만
        //   소정근로 미입력 상태로 남아 폴백(통상 간주)에 의존하게 된다.
        //   주 소정근로 분은 회사 통상 기준값에서 가져온다(11-0 에서 입력했으면 그 값, 아니면 2400 폴백).
        //   사업장 오버라이드는 이 시점에 존재할 수 없으므로(사업장은 12단계에서 생성) 회사 스코프로 조회한다.
        //   적용 시작일 = 프로비저닝 일자(master 계정은 입사일을 받지 않는다 — 입사일 폴백 규약과 동일 계열).
        //   register 는 @Transactional(REQUIRED) 라 본 프로비저닝 트랜잭션에 참여한다(실패 시 전체 롤백).
        int masterWeekStdMinutes = stdWorkHoursService.resolveCmpnyWeekStdMinutes(cmpnyCd);
        stdWorkHoursService.register(StdWorkHoursSaveCommand.builder()
                .cmpnyCd(cmpnyCd)
                .userCd(userCd)
                .applyStrDate(todayYmd)
                .applyEndDate(null)
                .weekStdMinutes(masterWeekStdMinutes)
                .reasonCd(StdWorkReasonCd.NORMAL)
                .reasonDetail(null)
                .actorNo(param.gvUserCd())
                .build());

        // 12) TB_SITE INSERT(최초 사업장 — SITE_NM=회사명, 관리자=master, SITE_NO=siteCd 기본).
        companyProvisionMapper.insertSite(new SiteInsertCommand(
                cmpnyCd
                , siteCd
                , siteCd
                , param.cmpnyNm().trim()
                , todayYmd
                , userCd
                , param.gvUserCd()
        ));

        // 13) TB_SITE_NODE INSERT(최초 노드 — MAIN_ADMIN_CD=master 로 노드-관리자 정합성 충족).
        companyProvisionMapper.insertSiteNode(new SiteNodeInsertCommand(
                cmpnyCd
                , siteCd
                , nodeCd
                , param.cmpnyNm().trim()
                , FIRST_NODE_TYPE
                , userCd
                , param.gvUserCd()
        ));

        // 14) TB_USER_SITE_AUTH INSERT(master ↔ 최초 사업장).
        companyProvisionMapper.insertUserSiteAuth(cmpnyCd, userCd, siteCd, param.gvUserCd());

        // 15) 권한 메뉴 복제('001' master/hr/safe/99999 → 신규 회사).
        companyProvisionMapper.copyAuthMenuFromTemplate(TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());

        // 16) 운영사변수(BAIM_VAL) 복제: 마스터(USE_YN='Y') → 상세.
        //     상세는 두 경로로 분리(BAIM_VAL_CD=COM005 / <>COM005 로 상호배타라 PK 충돌 없음):
        //       - COM005(권한) 은 원본 코드/마커(VAL_D_INFO_1='manage') 를 renumber 없이 그대로 보존해야 AUTH_CD 매칭이 유지된다.
        //       - 그 외 그룹은 기존대로 그룹별 00001 부터 재부여.
        companyProvisionMapper.copyBaimValMFromTemplate(TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());
        companyProvisionMapper.copyBaimValDPreserveAuth(TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());
        companyProvisionMapper.copyBaimValDFromTemplateRenumber(TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());

        // 17) tb_cmm_seq: 복제된 baim_val_d 그룹별 건수를 CURR_VAL 로 시드(그 외 키는 FNC get-or-create).
        companyProvisionMapper.seedCmmSeqFromBaimValD(cmpnyCd);

        // 18) 기본 근무타입(ST001, 1구간 09:00~18:00) 시드.
        companyProvisionMapper.insertWorktype(new WorktypeSeedCommand(
                cmpnyCd
                , siteCd
                , DEFAULT_SCH_CD
                , DEFAULT_SCH_NO
                , DEFAULT_SCH_TYPE
                , todayYmd
                , DEFAULT_SCH_STR_TIME
                , DEFAULT_SCH_END_TIME
                , param.gvUserCd()
        ));

        // 18-1) ★신규 고객사 필수 시드 — 시스템 연차 6종.
        //   이 6종(SYS_ANNUAL/MONTHLY/TENURE_BONUS/PREGRANT/PROMOTION/BIRTHDAY)은 SYSTEM_YN='Y' 라
        //   화면(Attd_03)에서 만들 수 없다(편집 차단 + 신규 생성은 항상 'N'+자동채번 코드).
        //   여기서 넣어주지 않으면 그 고객사는 연차 부여·신청이 영구 불가하다(ATTD_400_059 / ATTD_404_030).
        int leaveTypes = companyProvisionMapper.seedSystemLeaveTypes(cmpnyCd, param.gvUserCd());

        // 18-2) ★신규 고객사 필수 시드 — 기본 연차정책(7축 법정값) + 사용정책(1:1).
        //   정책이 없으면 연차 부여가 아예 동작하지 않는다. 관리자가 Baim_07 에서 언제든 변경할 수 있다.
        LeavePolicySeedCommand policySeed = new LeavePolicySeedCommand(cmpnyCd, todayYmd, param.gvUserCd());
        companyProvisionMapper.seedDefaultLeavePolicy(policySeed);   // POLICY_SEQ 회수(useGeneratedKeys)
        companyProvisionMapper.seedDefaultLeaveUsagePolicy(policySeed);

        // 18-3) 위험성평가 기준정보(Risk_01) — "공통관리" 항목만 복제(사업장 전용은 제외: 신규 회사에 그 사업장이 없다).
        //   화면이 2단(위험분류 + 유해위험요인)이라 둘 다 옮겨야 반쪽이 되지 않는다.
        //   카테고리(PROCESS_CD)는 재번호 대상이라 "이름 기준 매핑"으로 붙인다(매퍼 주석 참조).
        int riskTypes = companyProvisionMapper.copyCommonRiskTypeFromTemplate(
                TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());
        int riskHazards = companyProvisionMapper.copyCommonRiskHazardFromTemplate(
                TEMPLATE_CMPNY_CD, cmpnyCd, param.gvUserCd());
        companyProvisionMapper.seedRiskSeq(cmpnyCd);
        companyProvisionMapper.seedHazardSeq(cmpnyCd);

        log.info("신규 고객사 시드 완료 - cmpnyCd={}, 시스템연차={}건, 연차정책seq={}, 위험분류={}건, 유해위험요인={}건",
                cmpnyCd, leaveTypes, policySeed.getPolicySeq(), riskTypes, riskHazards);

        // 19) 휴일 동기화(올해+내년) — 외부 API 실패가 회사 생성 전체를 롤백시키지 않도록 best-effort.
        //     HolidaySyncService.syncYear 는 @Transactional(REQUIRED) 이라 본 트랜잭션에 참여하면
        //     실패 시 rollback-only 로 전이된다. 따라서 커밋 성공 이후(afterCommit)에 별도 트랜잭션으로 수행한다.
        //     (syncYear 는 전 회사 대상 멱등 upsert 라 신규 회사도 포함된다.)
        final int thisYear = LocalDate.now().getYear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncHolidaysBestEffort(thisYear);
                    syncHolidaysBestEffort(thisYear + 1);
                }
            });
        } else {
            // 트랜잭션 동기화 비활성(예외적 경로) — 즉시 best-effort 수행.
            syncHolidaysBestEffort(thisYear);
            syncHolidaysBestEffort(thisYear + 1);
        }

        log.info("신규 고객사 프로비저닝 완료 - cmpnyCd={}, siteCd={}, masterUserCd={}, adminId={}, mblLast4={}",
                cmpnyCd, siteCd, userCd, adminId, phoneLast4);

        return CompanyProvisionResponse.builder()
                .cmpnyCd(cmpnyCd)
                .masterUserId(userId)
                .initialPasswordGuide("초기 비밀번호는 관리자 휴대폰번호(하이픈 제외)입니다. 첫 로그인 시 본인인증 후 변경하세요.")
                .build();
    }

    @Override
    public CmpnyCdCheckResponse checkCmpnyCdAvailable(String cmpnyCd) {
        String normalized = (cmpnyCd == null) ? "" : cmpnyCd.trim().toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return new CmpnyCdCheckResponse(normalized, false, false, "회사코드를 입력해 주세요.");
        }
        if (!CMPNY_CD_PATTERN.matcher(normalized).matches()) {
            return new CmpnyCdCheckResponse(normalized, false, false,
                    PlatformErrorCode.PLATFORM_400_020.message());
        }
        if (companyProvisionMapper.selectCmpnyExists(normalized) > 0) {
            return new CmpnyCdCheckResponse(normalized, true, false,
                    PlatformErrorCode.PLATFORM_400_021.message());
        }
        return new CmpnyCdCheckResponse(normalized, true, true, "사용할 수 있는 회사코드입니다.");
    }

    /**
     * 운영자가 입력한 회사코드를 정규화·검증하고 확정한다(2026-08-16 — 종전 서버 랜덤 발급 대체).
     *
     * <p>대문자로 정규화한다. DB 콜레이션이 대소문자를 무시(utf8mb4_unicode_ci)해 'parma' 와
     * 'PARMA' 는 어차피 같은 값으로 충돌하므로, 표기를 하나로 고정해 혼동을 없앤다.
     *
     * <p>중복 검사도 같은 이유로 DB 가 대소문자 무시 비교를 해 준다 — 기존 랜덤 코드(혼합 대소문자)와의
     * 충돌도 정상적으로 걸린다.
     *
     * <p>⚠️ 이 검사는 프론트 중복확인과 별개인 <b>2차 방어</b>다. 프론트에서 확인한 뒤 저장까지
     * 사이에 다른 운영자가 같은 코드를 선점할 수 있으므로 저장 트랜잭션 안에서 다시 본다.
     * 동시 INSERT 경합의 최종 백스톱은 TB_CMPNY 의 PK 제약이다.
     */
    private String resolveInputCmpnyCd(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_001);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!CMPNY_CD_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_020);
        }
        if (companyProvisionMapper.selectCmpnyExists(normalized) > 0) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_021);
        }
        return normalized;
    }

    /** 휴일 동기화 best-effort 래퍼(실패는 경고 로그만, 트랜잭션 무영향). */
    private void syncHolidaysBestEffort(int year) {
        try {
            holidaySyncService.syncYear(year);
        } catch (Exception e) {
            log.warn("신규 고객사 휴일 동기화 실패(회사 생성 영향 없음) - year={}, msg={}", year, e.getMessage());
        }
    }

    /** '-' 제거 후 YYYYMMDD 8자리 숫자만 반환. 형식 불일치/빈 값은 null. */
    private String normalizeYmdOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String compact = raw.replace("-", "").trim();
        if (compact.length() != 8 || !compact.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return compact;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
