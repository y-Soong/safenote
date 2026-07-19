package com.prafta.common.cmm.dailyjoin.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.dailyjoin.application.command.InsertDailyUserCommand;
import com.prafta.common.cmm.dailyjoin.application.command.TermsUserAgrCommand;
import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;
import com.prafta.common.cmm.dailyjoin.application.param.SiteInfoParam;
import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.dailyjoin.application.query.EmptySlotQuery;
import com.prafta.common.cmm.dailyjoin.application.query.LinkPolicyQuery;
import com.prafta.common.cmm.dailyjoin.application.query.MblNoDupleQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SiteInfoQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SmsConsumeQuery;
import com.prafta.common.cmm.dailyjoin.application.query.UserIdDupleCheckQuery;
import com.prafta.common.cmm.dailyjoin.dto.response.InsertDailyUserResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.dailyjoin.mapper.DailyJoinMapper;
import com.prafta.common.cmm.dailyjoin.result.RequiredTermsResult;
import com.prafta.common.cmm.dailyjoin.result.SiteInfoResult;
import com.prafta.common.cmm.dailyjoin.service.DailyJoinService;
import com.prafta.common.error.dailyjoin.DailyJoinErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyJoinServiceImpl implements DailyJoinService {

    private final DailyJoinMapper dailyJoinMapper;
    private final HmacSigner hmacSigner;
    private final AesGcmCrypto aesGcmCrypto;
    private final PasswordHasher passwordHasher;
    // 일용직 계약서+승인제(D6) — 가입 시 승인요청 생성(같은 트랜잭션, 가입 실패 시 함께 롤백).
    private final DailyEntryService dailyEntryService;

    /** [SYS081] 입장 승인요청 유형 — 01:신규가입(가입/재활성 가입 공통). */
    private static final String ENTRY_REQ_TYPE_JOIN = "01";

    @Override
    public SiteInfoResponse selectSiteInfo(SiteInfoParam param) {
        log.info("일일사용자 회원가입 - 회사/사업장 정보 조회 진입 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        SiteInfoResult result = dailyJoinMapper.selectSiteInfo(SiteInfoQuery.from(param));

        if (result == null) {
            // 회사 미존재/USE_YN='N', 사업장 미존재 시 유효하지 않은 링크로 처리
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_404_001);
        }

        log.info("일일사용자 회원가입 - 회사/사업장 정보 조회 종료 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        return SiteInfoResponse.builder()
                .cmpnyCd(result.cmpnyCd())
                .cmpnyNm(result.cmpnyNm())
                .siteCd(result.siteCd())
                .siteNm(result.siteNm())
                .build();
    }

    @Override
    public UserIdDupleCheckResponse checkUserIdDuple(UserIdDupleCheckParam param) {
        log.info("일일사용자 회원가입 - 사용자ID 중복체크 진입 cmpnyCd={}", param.cmpnyCd());

        // prafta-app-032 §2: 프리체크는 활성-only 유지(휴대폰 컨텍스트 없음). 제출 단계에서 전상태로 정직 차단.
        int cnt = dailyJoinMapper.selectActiveUserIdDupleCnt(UserIdDupleCheckQuery.from(param));
        String uniqueYn = (cnt > 0) ? "N" : "Y";

        log.info("일일사용자 회원가입 - 사용자ID 중복체크 종료 uniqueYn={}", uniqueYn);

        return UserIdDupleCheckResponse.builder()
                .uniqueYn(uniqueYn)
                .build();
    }

    @Override
    @Transactional
    public InsertDailyUserResponse insertDailyUser(InsertDailyUserParam param) {
        log.info("일일사용자 회원가입 진입 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        // a. SMS 인증 가입 시점 재검증 (휴대폰 HMAC + 인증번호 기준 최근 인증완료 행 확인)
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());
        if (phoneNorm == null) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

        // prafta-daily-blacklist: 블랙리스트 게이트 — 차단 휴대폰이면 SMS 소진/슬롯 점유 등 부수효과 발생 전에 즉시 차단.
        if (dailyJoinMapper.selectActiveBlacklistCnt(param.cmpnyCd(), phoneHmac) > 0) {
            log.info("일일사용자 회원가입 차단 - 블랙리스트 휴대폰 cmpnyCd={}", param.cmpnyCd());
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_007);
        }

        // 인증완료 레코드의 SMS_ID 확보 (소진 처리 대상)
        String smsId = dailyJoinMapper.selectSmsVerifiedSmsId(SmsConsumeQuery.of(phoneHmac, param.certNo()));
        if (smsId == null || smsId.isBlank()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }

        // SMS 인증코드 일회성 소진 (VERIFIED_YN='Y' -> 'C' 조건부 UPDATE, 영향행 0이면 롤백)
        int smsConsumed = dailyJoinMapper.consumeSmsAuth(SmsAuthConsumeCommand.from(smsId));
        if (smsConsumed <= 0) {
            // 동시 가입 등으로 이미 소진된 인증코드
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }

        // g-0. PRAFTA-SUBCON-T2-07: 연동 미러 사업장 가입 차단(정규 가입과 동일 가드 — 목록 우회 직접 호출 방어).
        //      거부 메시지는 미러 존재 사실을 상세 노출하지 않는다.
        if (dailyJoinMapper.selectMirrorSiteCnt(param.cmpnyCd(), param.siteCd()) > 0) {
            log.info("일일사용자 회원가입 차단 - 연동 미러 사업장 cmpnyCd={}, siteCd={}",
                    sanitizeForLog(param.cmpnyCd()), sanitizeForLog(param.siteCd()));
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_008);
        }

        // g-1. 가입 전 계정등록 토글 검증 (TB_DAILY_USER_LINK_POLICY.USE_YN OFF면 가입 차단)
        String linkPolicyUseYn = dailyJoinMapper.selectLinkPolicyUseYn(LinkPolicyQuery.of(param.cmpnyCd(), param.siteCd()));
        if (linkPolicyUseYn == null || !"Y".equals(linkPolicyUseYn)) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_004);
        }

        // prafta-app-032 A — 휴대폰 기반 재활성 대상 USER_CD 를 ID 중복검사보다 먼저 확정한다.
        //   ID 중복검사를 전상태(active+inactive)로 정직화하되, 같은 휴대폰 복귀자가 자기 옛 비활성 행을
        //   재활성하는 케이스(reuseUserCd)는 제외해 false-positive 차단을 방지한다.
        //   (활성 충돌은 아래 ID/휴대폰 중복검사에서 별도로 차단된다.)
        String reuseUserCd = dailyJoinMapper.selectReactivatableDailyUserCd(param.cmpnyCd(), phoneHmac);

        // b. ID 중복 재확인(전상태, reuseUserCd 제외) — TB_DAILY_USER.
        int userIdCnt = dailyJoinMapper.selectUserIdDupleCnt(
                UserIdDupleCheckQuery.of(param.cmpnyCd(), param.userId(), reuseUserCd));
        if (userIdCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_001);
        }

        // b. 휴대폰 중복(CMPNY_CD + MBL_NO_HMAC 기준 USE_YN='Y') 확인
        int mblNoCnt = dailyJoinMapper.selectMblNoDupleCnt(MblNoDupleQuery.of(param.cmpnyCd(), phoneHmac));
        if (mblNoCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_002);
        }

        // b-2. PRAFTA-app-027-3'(통합형) — TB_USER 중복도 사전 차단(UX_TB_USER_ID/UX_TB_USER_MBL_NO).
        //      UX_TB_USER_ID 는 활성/비활성 무관 절대 유니크 → 전상태로 검사(reuseUserCd 제외)해 비활성 잔존
        //      행과의 INSERT 충돌(500)을 사전 차단한다. (prafta-app-032 A 의 핵심 — Duplicate entry 500 제거)
        int tbUserIdCnt = dailyJoinMapper.selectTbUserIdDupleCnt(param.cmpnyCd(), param.userId(), reuseUserCd);
        if (tbUserIdCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_001);
        }
        int tbUserMblCnt = dailyJoinMapper.selectTbUserMblHmacDupleCnt(param.cmpnyCd(), phoneHmac);
        if (tbUserMblCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_002);
        }

        // g-2. 사업장 빈 슬롯 "존재" 검사만 수행 (없으면 가입 차단 — 정원 초과 가입 무한 허용 방지, plan §6 기본안 2).
        //      입장 승인제(D6): 실제 점유는 가입 시점이 아니라 승인 후 첫 로그인(DailyReentryProcessor)으로 이연.
        //      그 시점 정원 부족이면 DAILYLOGIN_400_004 로 차단된다.
        String emptySlotNo = dailyJoinMapper.selectFirstEmptySlotNo(EmptySlotQuery.of(param.cmpnyCd(), param.siteCd()));
        if (emptySlotNo == null || emptySlotNo.isBlank()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_005);
        }

        // d. 휴대폰 암호화/HMAC/마지막4자리 파생 (신규/재활성 공통)
        String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
        String phoneLast4 = Normalizers.last4(phoneNorm);

        // e. 비밀번호 - 사용자 입력 비밀번호 해시(평문 로깅 금지, 난수 아님)
        String userPw = passwordHasher.hash(param.userPw());

        // 옵션2 — 같은 휴대폰의 비활성(만료) 일용직 행이 있으면 새 USER_CD 채번 대신 재활성한다.
        //         (활성 중복은 위 b/b-2 에서 이미 차단되었으므로 여기 도달 시 활성 충돌은 없음)
        //         reuseUserCd 는 위 ID 중복검사 직전에 이미 확정했다(prafta-app-032 A).
        final String userCd;
        if (reuseUserCd != null && !reuseUserCd.isBlank()) {
            // (가) 재활성 경로 — 기존 USER_CD 재사용. 워커당 1행을 유지하고 재가입 차단을 해소한다.
            //      같은 휴대폰 비활성 행이 여러 개(레거시)면 최신 1건만 재활성, 나머지는 비활성 유지.
            userCd = reuseUserCd;
            InsertDailyUserCommand command =
                    InsertDailyUserCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4);

            // TB_DAILY_USER 기존 행 재활성(만료일/잠금 초기화 포함)
            dailyJoinMapper.reactivateDailyUser(command);

            // 통합형 TB_USER 재활성. 영향행 0이면 통합형 이전 레거시(TB_USER 없음) → 신규 INSERT 폴백.
            int tbUserUpd = dailyJoinMapper.reactivateTbUser(command);
            if (tbUserUpd <= 0) {
                dailyJoinMapper.insertDailyUserToTbUser(command);
            }

            // SITE_AUTH 는 사업장 변경 가능성을 고려해 upsert(기존 권한 행 회복 또는 신규 부여)
            dailyJoinMapper.upsertTbUserSiteAuth(param.cmpnyCd(), userCd, param.siteCd());
        } else {
            // (나) 신규 가입 경로 — USER_CD 채번 후 TB_DAILY_USER + 통합형 TB_USER/SITE_AUTH INSERT.
            userCd = dailyJoinMapper.selectDailyUserCd(EmptySlotQuery.of(param.cmpnyCd(), param.siteCd()));
            InsertDailyUserCommand command =
                    InsertDailyUserCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4);

            // f. TB_DAILY_USER insert
            dailyJoinMapper.insertDailyUser(command);

            // f-2. PRAFTA-app-027-3'(통합형) — 같은 트랜잭션에서 TB_USER + TB_USER_SITE_AUTH 동시 INSERT.
            //      EMPLOYMENT_TYPE='DAILY'/AUTH_CD='99999'/NODE_CD=NULL/ACCOUNT_STATUS='04'(승인대기 — D6).
            dailyJoinMapper.insertDailyUserToTbUser(command);
            dailyJoinMapper.insertTbUserSiteAuth(param.cmpnyCd(), userCd, param.siteCd());
        }

        // (구 g-3/g-3b/g-4 제거 — 입장 승인제 D6) 슬롯 점유/지정부서 매칭/점유 이력 적재는 가입 시점이 아니라
        // 관리자 승인 후 첫 로그인(DailyReentryProcessor.reactivateAndOccupy)으로 이연되었다.

        // h. 약관 동의 이력 insert (필수약관 검증 후 요청 항목 저장)
        insertTermsAgreement(userCd, param);

        // i. 입장 승인요청 생성(REQ_TYPE='01' 신규가입) + 사업장 관리자 푸시 outbox 적재 (D5).
        //    같은 트랜잭션 — 가입 실패 시 요청도 함께 롤백. open 중복은 멱등(no-op),
        //    당일 거부 이력이 있으면 007 로 가입 자체가 차단된다(트랜잭션 롤백).
        dailyEntryService.createEntryRequest(param.cmpnyCd(), param.siteCd(), userCd, ENTRY_REQ_TYPE_JOIN);

        log.info("일일사용자 회원가입 완료(승인대기) userCd={}, 재활성여부={}",
                userCd, (reuseUserCd != null && !reuseUserCd.isBlank()) ? "Y" : "N");

        // j. 응답으로 userId + 승인대기 플래그 반환(가입 완료 화면 "승인 대기" 안내용 — R4).
        return InsertDailyUserResponse.builder()
                .userId(param.userId())
                .pendingApprovalYn("Y")
                .build();
    }

    /** 로그 위조 방지용 외부 입력 정제 — 개행 제거 + 50자 상한 (subcon01 SEC-ADV-1 규약 미러). */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\r\\n]", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
    }

    /**
     * 약관 동의 이력 저장.
     * 약관 버전은 클라이언트 값이 아닌 서버 조회값(TB_TERMS.TERMS_VERSION)을 저장한다.
     */
    private void insertTermsAgreement(String userCd, InsertDailyUserParam param) {
        List<RequiredTermsResult> requiredTermsList = dailyJoinMapper.selectRequiredTermsList();
        if (requiredTermsList == null || requiredTermsList.isEmpty()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_500_001);
        }

        // termsId -> 서버 신뢰 약관버전 매핑
        Map<String, String> termsVersionMap = new HashMap<>();
        for (RequiredTermsResult required : requiredTermsList) {
            termsVersionMap.put(required.termsId(), required.termsVersion());
        }

        // 필수약관이 요청의 동의 목록에 모두 포함되어 있는지 검증
        for (RequiredTermsResult required : requiredTermsList) {
            boolean agreed = param.agrTermsList().stream()
                    .anyMatch(agr -> required.termsId().equals(agr.termsId()));
            if (!agreed) {
                throw new ApiException(DailyJoinErrorCode.DAILYJOIN_500_001);
            }
        }

        // 필수약관 동의 이력 저장 (서버 조회 버전으로 저장).
        // CMPNY_CD 는 가입 흐름의 회사(param.cmpnyCd(), joinCd 도출)로 적재 — 생성되는 TB_USER 와 동일 회사 스코프.
        for (RequiredTermsResult required : requiredTermsList) {
            String termsVersion = termsVersionMap.get(required.termsId());
            dailyJoinMapper.insertTermsUserAgrMgmt(
                    TermsUserAgrCommand.of(param.cmpnyCd(), userCd, required.termsId(), termsVersion));
        }
    }
}
