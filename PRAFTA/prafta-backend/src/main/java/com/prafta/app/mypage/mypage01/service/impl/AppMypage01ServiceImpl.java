package com.prafta.app.mypage.mypage01.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.mypage.mypage01.application.command.DefaultSchChangeReqInsertCommand;
import com.prafta.app.mypage.mypage01.application.param.ApprovalCandidateParam;
import com.prafta.app.mypage.mypage01.application.param.MobileSendParam;
import com.prafta.app.mypage.mypage01.application.param.MobileVerifyParam;
import com.prafta.app.mypage.mypage01.application.param.PasswordChangeParam;
import com.prafta.app.mypage.mypage01.application.param.PresetActionParam;
import com.prafta.app.mypage.mypage01.application.param.PresetSaveParam;
import com.prafta.app.mypage.mypage01.application.param.ProfileUpdateParam;
import com.prafta.app.mypage.mypage01.application.param.UpdateDefaultSchParam;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateItem;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateListResponse;
import com.prafta.app.mypage.mypage01.dto.response.DefaultSchChangeRequestResponse;
import com.prafta.app.mypage.mypage01.dto.response.MobileSendResponse;
import com.prafta.app.mypage.mypage01.dto.response.MobileVerifyResponse;
import com.prafta.app.mypage.mypage01.dto.response.MypageProfileEditResponse;
import com.prafta.app.mypage.mypage01.dto.response.MypageProfileResponse;
import com.prafta.app.mypage.mypage01.dto.response.PresetItemResult;
import com.prafta.app.mypage.mypage01.dto.response.PresetListResponse;
import com.prafta.app.mypage.mypage01.dto.response.PresetSaveResponse;
import com.prafta.app.mypage.mypage01.dto.response.PresetStepItem;
import com.prafta.app.mypage.mypage01.mapper.AppMypage01Mapper;
import com.prafta.app.mypage.mypage01.result.ApprovalCandidateResult;
import com.prafta.app.mypage.mypage01.result.PresetMasterResult;
import com.prafta.app.mypage.mypage01.result.PresetStepResult;
import com.prafta.app.mypage.mypage01.result.UserProfileResult;
import com.prafta.app.mypage.mypage01.service.AppMypage01Service;
import com.prafta.app.req.req09.service.AttdApprovalLineService;
import com.prafta.common.cmm.sms.AuthCodeSmsDispatcher;
import com.prafta.common.cmm.sms.policy.SmsRateLimitGuard;
import com.prafta.common.cmm.sms.policy.SmsSendContext;
import com.prafta.common.cmm.sms.policy.SmsVerifyGuard;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.mypage.MypageErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-010: 앱 마이페이지 서비스 구현.
 *
 * <p>식별자(cmpnyCd/userCd)는 토큰 출처만 사용(IDOR 차단). PII 평문은 마스킹 응답·로그에 노출하지 않으며,
 * 복호화 전체값은 010-01b 응답에만 싣는다. D2: web user04 호출/의존 없이 앱 매퍼로 전부 신규 작성.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppMypage01ServiceImpl implements AppMypage01Service {

    private final AppMypage01Mapper appMypage01Mapper;
    private final AesGcmCrypto aesGcmCrypto;
    private final HmacSigner hmacSigner;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;
    /** SMS-PPURIO-05: 인증번호 실발송 디스패처(게이트 OFF 면 SKIPPED 기록 후 조용히 통과). */
    private final AuthCodeSmsDispatcher authCodeSmsDispatcher;
    /** SMS2-B4: 발송 다층 상한 가드(정책행 잠금으로 TOCTOU 봉인 + 인증코드 INSERT 를 함께 수행). */
    private final SmsRateLimitGuard smsRateLimitGuard;

    /** [3차 / sec N-4] 인증번호 검증(대입) 방어. ★발송 축(SmsRateLimitGuard)과 별개 경로다(sec N-3). */
    private final SmsVerifyGuard smsVerifyGuard;

    // F-8-2: 본인 기본 근무타입 자기변경 — 검증/자동생성 공용 서비스(common.cmm.sch). 웹 User01ServiceImpl 과 동일 재사용.
    // PRAFTA-002(승인제 전환) 이후 defaultSchGenService/defaultSchGenMapper 의 즉시반영 호출부는 제거됐으며,
    // defaultSchOptionService(화이트리스트 검증)만 신청 시점에 계속 사용한다. 승인 시점 반영은
    // Attd07ServiceImpl.approveDefaultSchChangeRequest(PRAFTA-003)에서 defaultSchGenService 를 재사용한다.
    private final com.prafta.common.cmm.sch.service.DefaultSchOptionService defaultSchOptionService;
    private final com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper defaultSchGenMapper;
    /** PRAFTA-002: 결재 분기/라인 INSERT 공용 서비스(req07 과 동일 재사용, 같은 @Transactional 참여). */
    private final AttdApprovalLineService attdApprovalLineService;

    /** PRAFTA-002 F15: advisory lock 타임아웃(초). req07 DUP_LOCK_TIMEOUT_SEC 과 동일 정책값. */
    private static final int DUP_LOCK_TIMEOUT_SEC = 3;

    /** 기본 근무타입 변경 신청 사유 최대 길이(TB_USER_ATTD_REQ.REQ_REASON varchar(500)). */
    private static final int REQ_REASON_MAX_LEN = 500;

    // 휴대폰 정규화 후 허용 자리수(10~11). 정책 §3.2.
    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    // 휴대폰 변경 검증 토큰 만료(분). plan §4: 5분.
    private static final int PHONE_CHANGE_TOKEN_TTL_MIN = 5;

    // 휴대폰 변경 검증 토큰에 바인딩하는 "검증한 번호" HMAC 클레임 키.
    // 저장 시 입력 번호 HMAC 과 동등 비교하여 번호 치환·재사용을 차단한다.
    private static final String PHONE_CHANGE_MBL_HMAC_CLAIM = "gv_mblHmac";

    // 비밀번호 정책(로그인 정책과 정합: 6~15자, 2종 이상 조합).
    private static final int PW_MIN_LEN = 6;
    private static final int PW_MAX_LEN = 15;

    private static final Set<String> ALLOWED_GENDER = Set.of("100", "200");

    // ============================================================
    // 프로필 (010-01 / 010-01b)
    // ============================================================

    @Override
    public MypageProfileResponse getProfile(TokenInfo tokenInfo) {
        UserProfileResult r = loadProfile(tokenInfo);
        int presetCount = appMypage01Mapper.countMyPresets(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());

        return MypageProfileResponse.builder()
                .userId(r.userId())
                .userNm(r.userNm())
                .siteNm(r.siteNm())
                .nodeNm(r.nodeNm())
                .hireDate(blankToNull(r.hireDate()))
                .mblNoMasked(maskPhone(r.mblNo(), r.mblNoLast4()))
                .emailMasked(maskEmail(r.email(), r.emailDomain()))
                .genderCode(blankToNull(r.genderCode()))
                .genderNm(blankToNull(r.genderNm()))
                .birthDateMasked(maskBirth(r.birthDate()))
                .lastLoginDtime(r.lastLoginDtime())
                .presetCount(presetCount)
                .defaultSchCd(r.defaultSchCd())
                .defaultSchNo(r.defaultSchNo())
                .defaultSchStrTime(r.defaultSchStrTime())
                .defaultSchEndTime(r.defaultSchEndTime())
                .build();
    }

    @Override
    public MypageProfileEditResponse getProfileForEdit(TokenInfo tokenInfo) {
        UserProfileResult r = loadProfile(tokenInfo);

        return MypageProfileEditResponse.builder()
                .userId(r.userId())
                .userNm(r.userNm())
                .siteNm(r.siteNm())
                .nodeNm(r.nodeNm())
                .hireDate(blankToNull(r.hireDate()))
                .mblNo(blankToNull(r.mblNo()))
                .email(blankToNull(r.email()))
                .genderCode(blankToNull(r.genderCode()))
                .birthDate(blankToNull(r.birthDate()))
                .lastLoginDtime(r.lastLoginDtime())
                .build();
    }

    private UserProfileResult loadProfile(TokenInfo tokenInfo) {
        UserProfileResult r = appMypage01Mapper.selectMyProfile(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        if (r == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_004);
        }
        return r;
    }

    // ============================================================
    // 프로필 저장 (010-02)
    // ============================================================

    @Override
    @Transactional
    public void updateProfile(ProfileUpdateParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();

        // 1) 이름 검증 (1~50자).
        String userNm = trimToEmpty(param.userNm());
        if (userNm.isEmpty() || userNm.length() > 50) {
            throw new ApiException(MypageErrorCode.INVALID_USER_NM);
        }

        // 2) 성별 검증 (100/200/빈값=NULL).
        String genderInput = trimToEmpty(param.genderCode());
        String genderCode = genderInput.isEmpty() ? null : genderInput;
        if (genderCode != null && !ALLOWED_GENDER.contains(genderCode)) {
            throw new ApiException(MypageErrorCode.INVALID_GENDER);
        }

        // 3) 생년월일 검증 (빈값 허용, 입력 시 YYYYMMDD·미래 불가).
        String birthInput = trimToEmpty(param.birthDate());
        boolean setBirth = !birthInput.isEmpty();
        String birthEnc = null;
        if (setBirth) {
            String birthNorm = Normalizers.normalizeBirth(birthInput);
            if (birthNorm == null || birthNorm.length() != 8 || !isValidYmd(birthNorm) || isFutureYmd(birthNorm)) {
                throw new ApiException(MypageErrorCode.INVALID_BIRTH_DATE);
            }
            birthEnc = aesGcmCrypto.encrypt(birthNorm);
        }

        // 4) 이메일 검증 (빈값 허용, 입력 시 형식).
        String emailInput = trimToEmpty(param.email());
        boolean setEmail = true; // 이메일은 빈값(=NULL)도 명시적 저장 허용.
        String emailEnc = null;
        String emailHmac = null;
        String emailDomain = null;
        if (!emailInput.isEmpty()) {
            String emailNorm = Normalizers.normalizeEmail(emailInput);
            if (emailNorm == null || !isValidEmail(emailNorm)) {
                throw new ApiException(MypageErrorCode.INVALID_EMAIL);
            }
            emailEnc = aesGcmCrypto.encrypt(emailNorm);
            emailHmac = hmacSigner.hmacSha256Base64Url(emailNorm);
            emailDomain = Normalizers.emailDomain(emailNorm);
        }

        // 5) 휴대폰 변경 여부 판정 — 입력 휴대폰의 HMAC 이 현재값과 다르면 변경.
        UserProfileResult current = loadProfile(param.tokenInfo());
        String mblInput = trimToEmpty(param.mblNo());
        boolean mobileChanged = false;
        String mblNoEnc = null;
        String mblNoHmac = null;
        String mblNoLast4 = null;
        if (!mblInput.isEmpty()) {
            String phoneNorm = Normalizers.normalizePhone(mblInput);
            if (phoneNorm == null
                    || phoneNorm.length() < PHONE_MIN_DIGITS
                    || phoneNorm.length() > PHONE_MAX_DIGITS) {
                throw new ApiException(MypageErrorCode.INVALID_MOBILE);
            }
            String inputHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
            String currentNorm = Normalizers.normalizePhone(current.mblNo());
            String currentHmac = (currentNorm == null) ? null : hmacSigner.hmacSha256Base64Url(currentNorm);
            mobileChanged = !inputHmac.equals(currentHmac);

            if (mobileChanged) {
                // 5-1) 검증 토큰 필수 + scope 검증 + 회사/사용자 바인딩 + 토큰-번호(HMAC) 바인딩.
                requirePhoneChangeToken(param.mobileVerificationToken(), cmpnyCd, userCd, inputHmac);

                // 5-2) 토큰-휴대폰 바인딩: 최근 검증완료·미소비 SMS 레코드를 소비한다.
                Long verifiedSmsId = appMypage01Mapper.selectRecentVerifiedSmsId(inputHmac);
                if (verifiedSmsId == null
                        || appMypage01Mapper.consumeSmsAuth(verifiedSmsId) != 1) {
                    throw new ApiException(MypageErrorCode.MOBILE_VERIFICATION_INVALID);
                }

                // 5-3) 변경 대상 휴대폰이 본인 외 다른 사용자에게 사용 중이면 차단.
                if (appMypage01Mapper.countOtherUserByMblHmac(cmpnyCd, userCd, inputHmac) > 0) {
                    throw new ApiException(MypageErrorCode.MOBILE_DUP);
                }

                mblNoEnc = aesGcmCrypto.encrypt(phoneNorm);
                mblNoHmac = inputHmac;
                mblNoLast4 = Normalizers.last4(phoneNorm);
            }
        }

        // 6) 기본 정보 UPDATE.
        appMypage01Mapper.updateProfileBasic(cmpnyCd, userCd, userNm, genderCode,
                birthEnc, setBirth, emailEnc, emailHmac, emailDomain, setEmail, userCd);

        // 7) 휴대폰 변경 시 ENC/HMAC/LAST4 동시 갱신.
        if (mobileChanged) {
            appMypage01Mapper.updateProfileMobile(cmpnyCd, userCd, mblNoEnc, mblNoHmac, mblNoLast4, userCd);
        }

        log.info("마이페이지 프로필 저장 - userCd={}, 휴대폰변경={}, 성별설정={}, 생년월일설정={}",
                userCd, mobileChanged, genderCode != null, setBirth);
    }

    // ============================================================
    // 휴대폰 변경 인증 (010-03, 앱 전용, D4)
    // ============================================================

    /**
     * 휴대폰 변경 인증번호 발송(앱 전용, 유효 3분).
     *
     * <p>★{@code @Transactional} 을 붙이지 말 것. 트랜잭션이 없어 INSERT 가 즉시 커밋되고,
     *    그 덕분에 "인증코드 커밋 → 외부 발송 → 결과 독립 기록" 경계가 이미 성립한다(요청서 §7-3).
     */
    @Override
    public MobileSendResponse sendMobileVerification(MobileSendParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();

        String phoneNorm = Normalizers.normalizePhone(trimToEmpty(param.mblNo()));
        if (phoneNorm == null
                || phoneNorm.length() < PHONE_MIN_DIGITS
                || phoneNorm.length() > PHONE_MAX_DIGITS) {
            throw new ApiException(MypageErrorCode.INVALID_MOBILE);
        }
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

        // 본인 외 사용 중이면 발송 거부(중복).
        if (appMypage01Mapper.countOtherUserByMblHmac(cmpnyCd, userCd, phoneHmac) > 0) {
            throw new ApiException(MypageErrorCode.MOBILE_DUP);
        }

        String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
        String authCode = generateAuthCode();

        // SMS-PPURIO-05: refKey 를 INSERT 전에 생성해 함께 저장한다(발송 결과 UPDATE 의 조인키).
        String refKey = authCodeSmsDispatcher.newRefKey();

        // SMS2-B4(sec H-4): 이 흐름에는 서버측 발송 상한이 전혀 없었다(로그인 상태라도 무제한 재발송 가능).
        //   가드가 [정책행 잠금 → 4축 카운트 → 기존코드 만료 → INSERT] 를 한 트랜잭션으로 묶는다.
        //   ★목적 코드는 MOBILE_CHANGE 라 진입점 A 의 번호 축과 서로 간섭하지 않는다(SMS2-D5).
        //   ★로그인 흐름이므로 사용자 축(userCd)을 채운다.
        //   ★★이 메서드에 @Transactional 을 붙이지 말 것 — 아래 dispatch(외부 HTTP)는 반드시
        //     가드 트랜잭션 커밋 이후에 호출되어야 한다.
        smsRateLimitGuard.guardAndInsert(
                SmsSendContext.of("MOBILE_CHANGE", phoneHmac, param.ipHash(), userCd),
                () -> {
                    // SMS2-D4 미러: 신규 코드 INSERT 직전에 기존 미검증 MOBILE_CHANGE 코드를 만료(유효 코드 항상 1건).
                    appMypage01Mapper.expireOldMobileChangeSmsAuth(phoneHmac);
                    appMypage01Mapper.insertSmsAuthCode(
                            phoneEnc, phoneHmac, authCode, refKey, param.ipHash(), userCd);
                });

        // 실발송 + 결과 기록. 게이트 OFF 면 SKIPPED 기록 후 조용히 통과(기존과 동일한 성공 응답).
        // ★validMinutes=3 — 이 흐름만 유효시간이 3분이다(AppMypage01Mapper.xml INTERVAL 3 MINUTE +
        //   아래 expiresInSeconds(180) 와 한 세트). 1 을 넣으면 문자가 "1분 내 유효"라고 거짓 안내를 한다.
        // 발송 실패 시 ApiException(SMS_502_*) 전파 → 앱 ProfileEditView 의 catch 진입.
        // 인증코드(authCode)는 어떤 로그에도 남기지 않는다(prafta-app-032 규약).
        authCodeSmsDispatcher.dispatch(refKey, phoneNorm, authCode, 3);

        log.info("마이페이지 휴대폰 변경 인증번호 발송 - userCd={}, mblLast4={}", userCd, Normalizers.last4(phoneNorm));

        return MobileSendResponse.builder().expiresInSeconds(180).build();
    }

    /**
     * 휴대폰 변경 인증번호 검증(앱 전용).
     *
     * <p>★★[3차 / sec N-4] 대입 방어를 추가했다. 2차의 C-2 수정은 진입점 A 에만 적용됐고
     *    이 흐름에는 {@code FAIL_CNT} 조건도 카운터 증가도 없어, 로그인 계정 1개만 있으면
     *    3분 동안 6자리를 <b>무제한 대입</b>할 수 있었다. 성공하면 {@code PHONE_CHANGE_AUTH} scope 토큰이
     *    발급되어 미소유 번호를 자기 프로필에 결속할 수 있다
     *    (타 사용자가 쓰는 번호는 {@code countOtherUserByMblHmac} 가 막으므로 표적은 미등록 번호에 한정).
     *
     * <p>★★[4차 / sec T-2] 판정 순서를 바꿨다 — <b>코드 매칭이 시간당 상한보다 먼저다.</b>
     *    3차는 코드를 보기도 전에 시간당 검증 시도 상한을 검사해, 공격자가 표적 번호로 상한을
     *    소모시키면 정답을 가진 정상 사용자까지 반려됐다. 이제 상한은 <b>실패한 시도에만</b> 걸린다.
     *
     * <p>★{@code @Transactional} 을 절대 붙이지 말 것.
     *    현재 트랜잭션이 없어 {@code increaseMobileChangeSmsFailCnt} 가 statement 단위로 즉시 커밋된다.
     *    붙이는 순간 {@link ApiException} 롤백으로 카운터 증가가 통째로 사라져 본 방어가 무력화된다.
     *    부득이 붙여야 한다면 {@code PlatformLocationServiceImpl} 의
     *    {@code @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiException.class)}
     *    선례를 반드시 함께 적용할 것(진입점 A 와 동일 경고).
     * <p>★★[4차 / qa R-2] <b>호출자에도 같은 규칙이 적용된다.</b> 이 메서드가 무트랜잭션이어도
     *    트랜잭션을 가진 상위 메서드가 호출하면 카운터가 그 트랜잭션에 참여해 통째로 롤백된다
     *    (진입점 A 에서 {@code LoginServiceImpl.verifyPhoneAuth} 가 실제로 그랬다).
     *    현재 이 메서드의 호출자는 {@code AppMypage01Controller} 하나뿐이며 트랜잭션이 없다.
     *    새 호출자를 추가할 때 반드시 재확인할 것 — <b>성공 시엔 커밋되므로 실동작 테스트로는 잡히지 않는다.</b>
     *
     * <p>★{@code verificationCode} 형식 검증(6자리 숫자)을 카운터 증가보다 앞에 두지 말 것 —
     *    형식 오류 요청이 조기 반환되어 카운터 회피 우회가 생긴다.
     *    (현재의 {@code code.isEmpty()} 검사는 빈 값이라 어떤 코드와도 매칭되지 않으므로 oracle 가치가 없다.)
     */
    @Override
    public MobileVerifyResponse verifyMobile(MobileVerifyParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();

        String phoneNorm = Normalizers.normalizePhone(trimToEmpty(param.mblNo()));
        String code = trimToEmpty(param.verificationCode());
        if (phoneNorm == null || code.isEmpty()) {
            throw new ApiException(MypageErrorCode.INVALID_CODE);
        }
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

        // [3차 / sec N-2] 만료된 대입 잠금 해제 + 실패 허용 횟수 조회.
        //   ★반드시 조회 이전에 호출한다(잠금이 만료된 코드를 되살린 뒤 매칭해야 한다).
        //   ★발송 축(SmsRateLimitGuard)을 재사용하지 않는다 — 검증 시도가 발송 카운트를 오염시킨다.
        //   ★★[4차 / sec T-2] 여기서 시간당 상한을 보지 않는다. 상한은 아래 "불일치" 분기에서만 판정한다.
        final int verifyFailLimit = smsVerifyGuard.beforeVerify(phoneHmac, "MOBILE_CHANGE");

        Long smsId = appMypage01Mapper.selectValidSmsId(phoneHmac, code, verifyFailLimit);
        if (smsId == null) {
            // [3차 / sec N-4] 불일치/만료/초과 → 최신 미검증 코드의 FAIL_CNT +1(즉시 커밋).
            //   ★상한에 처음 도달하는 순간 FAIL_LOCKED_AT 도 함께 찍힌다(잠금 시작 시각).
            //   ★★아래 afterFailedVerify 보다 먼저 실행해야 한다(그쪽이 예외를 던지면 여기가 실행되지 않는다).
            appMypage01Mapper.increaseMobileChangeSmsFailCnt(phoneHmac, verifyFailLimit);

            // [4차 / sec T-2 · T-3] 실패 시도 적재 + 시간당 실패 시도 상한 판정.
            //   ★코드가 일치한 요청은 이 경로에 오지 않으므로 정상 사용자는 상한을 소모하지도, 막히지도 않는다.
            smsVerifyGuard.afterFailedVerify(phoneHmac, "MOBILE_CHANGE", MypageErrorCode.TOO_MANY_ATTEMPTS);

            // 상한 도달(=현재 잠금)이면 그 사실을 구분해 안내한다 — 진입점 A(SMS_400_002)와 동일 취지.
            if (appMypage01Mapper.selectMobileChangeFailExceeded(phoneHmac, verifyFailLimit) > 0) {
                log.warn("휴대폰 변경 인증번호 대입 상한 도달(일시 잠금) - userCd={}, mblLast4={}",
                        userCd, Normalizers.last4(phoneNorm));
                throw new ApiException(MypageErrorCode.TOO_MANY_ATTEMPTS);
            }

            // 코드 불일치 vs 만료 구분: 미만료/미검증 레코드 존재 여부로 판단.
            int unverified = appMypage01Mapper.countUnverifiedByMblHmac(phoneHmac);
            if (unverified == 0) {
                throw new ApiException(MypageErrorCode.EXPIRED);
            }
            throw new ApiException(MypageErrorCode.INVALID_CODE);
        }

        // 검증 성공 처리(VERIFIED_YN='Y'). 동시성으로 이미 처리됐으면 만료/재시도로 간주.
        if (appMypage01Mapper.markSmsVerified(smsId, phoneHmac, code, verifyFailLimit) != 1) {
            throw new ApiException(MypageErrorCode.EXPIRED);
        }

        // 로그인 토큰을 발급하지 않는다(D4). 단발성 scope 토큰만 반환.
        // 토큰-번호 바인딩: 검증한 휴대폰의 HMAC 을 gv_mblHmac 클레임에 담아, 저장 시 입력 번호와의 일치를 강제한다.
        Map<String, Object> extraClaims = Map.of(PHONE_CHANGE_MBL_HMAC_CLAIM, phoneHmac);
        String verificationToken = jwtUtil.generateScopeToken(
                cmpnyCd, userCd, JwtScope.PHONE_CHANGE_AUTH, PHONE_CHANGE_TOKEN_TTL_MIN, extraClaims);

        log.info("마이페이지 휴대폰 변경 인증 성공 - userCd={}, mblLast4={}", userCd, Normalizers.last4(phoneNorm));

        return MobileVerifyResponse.builder()
                .verified(true)
                .verificationToken(verificationToken)
                .build();
    }

    /**
     * 휴대폰 변경 검증 토큰(scope=PHONE_CHANGE_AUTH) 유효성 + 회사/사용자 바인딩 + 토큰-번호(HMAC) 바인딩 검증.
     *
     * <p>토큰의 gv_mblHmac 클레임이 입력 휴대폰의 HMAC 과 일치해야 한다. 이로써 "사용자가 인증한 바로 그 번호"
     * 로만 저장이 가능해져, 다른(검증된) 번호로의 치환·재사용 레이스를 차단한다.
     *
     * @param expectedMblHmac 저장하려는 입력 휴대폰의 HMAC
     */
    private void requirePhoneChangeToken(String token, String cmpnyCd, String userCd, String expectedMblHmac) {
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            throw new ApiException(MypageErrorCode.MOBILE_VERIFICATION_REQUIRED);
        }
        Claims claims = jwtUtil.parseToken(token);
        String scope = claims.get("gv_scope", String.class);
        String tokenCmpnyCd = claims.get("gv_cmpnyCd", String.class);
        String tokenUserCd = claims.get("gv_userCd", String.class);
        String tokenMblHmac = claims.get(PHONE_CHANGE_MBL_HMAC_CLAIM, String.class);
        if (!JwtScope.PHONE_CHANGE_AUTH.equals(scope)
                || !cmpnyCd.equals(tokenCmpnyCd)
                || !userCd.equals(tokenUserCd)
                || tokenMblHmac == null
                || !tokenMblHmac.equals(expectedMblHmac)) {
            throw new ApiException(MypageErrorCode.MOBILE_VERIFICATION_INVALID);
        }
    }

    // ============================================================
    // 비밀번호 변경 (010-04)
    // ============================================================

    @Override
    @Transactional
    public void changePassword(PasswordChangeParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();

        String currentPw = param.currentPassword();
        String newPw = param.newPassword();

        String storedHash = appMypage01Mapper.selectUserPw(cmpnyCd, userCd);
        if (storedHash == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_004);
        }

        // 1) 현재 비밀번호 일치.
        if (!passwordHasher.matches(currentPw, storedHash)) {
            throw new ApiException(MypageErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 2) 새 비밀번호 규칙.
        if (!isValidPassword(newPw)) {
            throw new ApiException(MypageErrorCode.PASSWORD_RULE_VIOLATION);
        }

        // 3) 현재 == 새 금지.
        if (passwordHasher.matches(newPw, storedHash)) {
            throw new ApiException(MypageErrorCode.SAME_AS_CURRENT);
        }

        String newHash = passwordHasher.hash(newPw);
        appMypage01Mapper.updateUserPw(cmpnyCd, userCd, newHash);

        log.info("마이페이지 비밀번호 변경 - userCd={}", userCd);
    }

    // ============================================================
    // 결재선 프리셋 CRUD (010-05) — D2 앱 전용 신규
    // ============================================================

    @Override
    public PresetListResponse getPresets(TokenInfo tokenInfo) {
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();

        List<PresetMasterResult> masters = appMypage01Mapper.selectPresetMasters(cmpnyCd, userCd);
        List<PresetStepResult> steps = appMypage01Mapper.selectPresetStepsByUser(cmpnyCd, userCd);

        Map<String, List<PresetStepItem>> stepsByPreset = groupSteps(steps);

        List<PresetItemResult> presets = new ArrayList<>(masters.size());
        for (PresetMasterResult m : masters) {
            presets.add(PresetItemResult.builder()
                    .presetId(m.presetId())
                    .presetNm(m.presetNm())
                    .defaultYn(m.defaultYn())
                    .steps(stepsByPreset.getOrDefault(m.presetId(), new ArrayList<>()))
                    .build());
        }
        return PresetListResponse.builder().presets(presets).build();
    }

    @Override
    public PresetItemResult getPreset(TokenInfo tokenInfo, String presetId) {
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        if (presetId == null || presetId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        PresetMasterResult m = appMypage01Mapper.selectPresetById(cmpnyCd, userCd, presetId);
        if (m == null) {
            throw new ApiException(MypageErrorCode.PRESET_NOT_FOUND);
        }
        List<PresetStepResult> steps = appMypage01Mapper.selectPresetStepsById(cmpnyCd, userCd, presetId);
        List<PresetStepItem> stepItems = new ArrayList<>(steps.size());
        for (PresetStepResult s : steps) {
            stepItems.add(toStepItem(s));
        }
        return PresetItemResult.builder()
                .presetId(m.presetId())
                .presetNm(m.presetNm())
                .defaultYn(m.defaultYn())
                .steps(stepItems)
                .build();
    }

    @Override
    @Transactional
    public PresetSaveResponse savePreset(PresetSaveParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();
        String siteCd = param.tokenInfo().gv_siteCd();

        // 1) 이름 검증.
        String presetNm = trimToEmpty(param.presetNm());
        if (presetNm.isEmpty()) {
            throw new ApiException(MypageErrorCode.PRESET_NAME_REQUIRED);
        }

        // 2) 결재자 정제(trim·빈값 제거·중복 차단).
        List<String> approvers = sanitizeApprovers(param.approverUserCds());
        if (approvers.isEmpty()) {
            throw new ApiException(MypageErrorCode.PRESET_APPROVER_REQUIRED);
        }

        // 3) 결재자 유효성 — 본인 사업장 활성 후보 + 자기결재 차단(근태 §9.5).
        for (String a : approvers) {
            if (a.equals(userCd)) {
                throw new ApiException(MypageErrorCode.PRESET_SELF_APPROVAL);
            }
            if (appMypage01Mapper.countActiveCandidate(cmpnyCd, siteCd, a) < 1) {
                log.warn("앱 프리셋 저장 - 후보 밖 결재자. cmpnyCd={}, approver={}", cmpnyCd, a);
                throw new ApiException(MypageErrorCode.PRESET_APPROVER_INVALID);
            }
        }

        boolean makeDefault = "Y".equals(param.defaultYn());
        String presetId = trimToNull(param.presetId());

        // 4) 이름 중복 검사(소유자 스코프, 수정 시 자기 자신 제외).
        if (appMypage01Mapper.countPresetNameDup(cmpnyCd, userCd, presetNm, presetId) > 0) {
            throw new ApiException(MypageErrorCode.PRESET_NAME_DUPLICATED);
        }

        if (presetId == null) {
            // 5-A) 신규.
            presetId = appMypage01Mapper.selectNextPresetId(cmpnyCd);
            if (makeDefault) {
                appMypage01Mapper.clearDefaultForUser(cmpnyCd, userCd);
            }
            appMypage01Mapper.insertPresetMaster(cmpnyCd, presetId, userCd, presetNm,
                    makeDefault ? "Y" : "N", userCd);
        } else {
            // 5-B) 수정 — 소유권 재확인.
            requireOwnership(cmpnyCd, presetId, userCd);
            if (makeDefault) {
                appMypage01Mapper.clearDefaultForUser(cmpnyCd, userCd);
            }
            appMypage01Mapper.updatePresetMaster(cmpnyCd, presetId, presetNm,
                    makeDefault ? "Y" : "N", userCd);
            appMypage01Mapper.deletePresetSteps(cmpnyCd, presetId);
        }

        // 6) 스텝 재구성.
        int stepNo = 1;
        for (String a : approvers) {
            appMypage01Mapper.insertPresetStep(cmpnyCd, presetId, stepNo++, a, userCd);
        }

        log.info("앱 결재라인 프리셋 저장. userCd={}, presetId={}, 단계={}, 기본={}",
                userCd, presetId, approvers.size(), makeDefault);
        return PresetSaveResponse.builder().presetId(presetId).build();
    }

    @Override
    @Transactional
    public void setDefaultPreset(PresetActionParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();
        requireOwnership(cmpnyCd, param.presetId(), userCd);
        appMypage01Mapper.clearDefaultForUser(cmpnyCd, userCd);
        appMypage01Mapper.setDefault(cmpnyCd, param.presetId(), userCd);
        log.info("앱 결재라인 프리셋 기본지정. userCd={}, presetId={}", userCd, param.presetId());
    }

    @Override
    @Transactional
    public void deletePreset(PresetActionParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();
        requireOwnership(cmpnyCd, param.presetId(), userCd);
        appMypage01Mapper.deletePresetSteps(cmpnyCd, param.presetId());
        appMypage01Mapper.deletePresetMaster(cmpnyCd, param.presetId());
        log.info("앱 결재라인 프리셋 삭제. userCd={}, presetId={}", userCd, param.presetId());
    }

    @Override
    public ApprovalCandidateListResponse getApprovalCandidates(ApprovalCandidateParam param) {
        TokenInfo t = param.tokenInfo();
        Integer myRankSortIdx = appMypage01Mapper.selectUserRankSortIdx(t.gv_cmpnyCd(), t.gv_userCd());

        List<ApprovalCandidateResult> candidates = appMypage01Mapper.selectApprovalCandidates(
                t.gv_cmpnyCd(), t.gv_siteCd(), t.gv_userCd(), param.nodeCd(), param.userNm());

        List<ApprovalCandidateItem> items = new ArrayList<>(candidates.size());
        for (ApprovalCandidateResult c : candidates) {
            items.add(ApprovalCandidateItem.builder()
                    .userCd(c.userCd())
                    .userNm(c.userNm())
                    .rankNm(c.rankNm())
                    .nodeNm(c.nodeNm())
                    .build());
        }
        return ApprovalCandidateListResponse.builder()
                .myRankSortIdx(myRankSortIdx)
                .candidates(items)
                .build();
    }

    // ============================================================
    // F-8-2: 본인 기본 근무타입 자기변경(세션 사업장 고정)
    // ============================================================

    @Override
    public java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getDefaultSchOptions(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String siteCd = resolveSiteCd(tokenInfo);
        if (siteCd == null || siteCd.isBlank()) {
            return java.util.List.of();
        }
        return defaultSchOptionService.getActiveSchOptions(tokenInfo.gv_cmpnyCd(), siteCd);
    }

    /**
     * PRAFTA-002(기본근무타입-승인제, 2026-08-26): 즉시 반영 → 요청 등록 전환.
     *
     * <p>"승인 전 미반영" 설계 원칙(웹 문서 §3) — 이 메서드는 {@code TB_USER_ATTD_REQ} 에 요청
     * 레코드만 INSERT 하고 결재선을 적용한다. {@code TB_USER.DEFAULT_SCH_CD}/{@code TB_USER_WORK_PLAN}
     * 등 실제 반영 대상은 전혀 건드리지 않는다 — 반영은 승인 시점(Attd07Service.approveDefaultSchChangeRequest)
     * 에서만 수행된다(§0 무회귀 대상: applyDefaultSchChange 자체는 무수정 재사용).
     *
     * <p>req07(AppReq07ServiceImpl.registerSchedModify)와 동일 골격: 구조 검증 → 화이트리스트 검증 →
     * advisory lock 획득 → 중복 신청 차단(P10) → INSERT → 결재선 적용(AttdApprovalLineService 재사용) → lock 해제.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DefaultSchChangeRequestResponse updateDefaultSch(UpdateDefaultSchParam param) {
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
        String userCd = param.tokenInfo().gv_userCd();
        String nodeCd = param.tokenInfo().gv_nodeCd();

        // ----- 구조 검증 -----
        String defaultSchCd = trimToNull(param.defaultSchCd());
        if (defaultSchCd == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_141);
        }
        if (!StringUtils.hasText(param.reqReason())) {
            throw new ApiException(AttdErrorCode.ATTD_400_096);
        }
        if (param.reqReason().length() > REQ_REASON_MAX_LEN) {
            throw new ApiException(AttdErrorCode.ATTD_400_096);
        }

        // 사업장은 세션에서만 도출(본인 사업장 변경은 이 흐름 범위 밖).
        String siteCd = resolveSiteCd(param.tokenInfo());
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(AttdErrorCode.ATTD_400_140);
        }

        // 화이트리스트 검증(클라 제출값 신뢰 금지). 승인 시점에도 재검증(이중 검증, 웹 문서 §3).
        if (!defaultSchOptionService.isValidDefaultSch(cmpnyCd, siteCd, defaultSchCd)) {
            throw new ApiException(AttdErrorCode.ATTD_400_140);
        }

        // ----- F15 advisory lock(중복 차단 race window 직렬화) -----
        String lockKey = "ATTD_REQ:" + cmpnyCd + ":" + siteCd + ":" + userCd + ":"
                + AttdReqTypeUtils.REQ_TYPE_DEFAULT_SCH_CHANGE;
        acquireDupLock(lockKey);
        String reqId;
        try {
            // ----- 중복 요청 차단 (P10) — WORK_YMD 조건 없음(이 요청 유형은 근무일 무관). -----
            int dup = appMypage01Mapper.countPendingDefaultSchChangeReq(cmpnyCd, siteCd, userCd);
            if (dup > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_090);
            }

            // ----- INSERT -----
            reqId = appMypage01Mapper.selectNextDefaultSchReqId(cmpnyCd);
            DefaultSchChangeReqInsertCommand cmd = new DefaultSchChangeReqInsertCommand(
                    reqId, cmpnyCd, siteCd, userCd, nodeCd, defaultSchCd, param.reqReason(), userCd);
            appMypage01Mapper.insertDefaultSchChangeReq(cmd);

            // ----- 결재선 적용(req07 과 동일 재사용 — approverUserCds/presetId 미지정 → 기본 결재자 폴백) -----
            // PRAFTA-004(2026-08-27, 결재선 필수화): applyApprovalFlow 는 이제 결재선 생성만 수행하며
            //   신청 즉시 REQ_STATUS 를 확정하는 경로가 없다(PRAFTA-001) — 즉시확정 전용 반영 훅
            //   (reflectSelfApprovedDefaultSchChange) 호출은 더 이상 필요 없다(죽은 코드로 판정되어
            //   제거됨 — 실제 반영은 항상 approveDefaultSchChangeRequest 를 통해서만).
            attdApprovalLineService.applyApprovalFlow(
                    cmpnyCd, siteCd, userCd, reqId, param.approverUserCds(), param.presetId(), userCd);
        } finally {
            releaseDupLock(lockKey);
        }

        log.info("앱 본인 기본 근무타입 변경 요청 등록(승인제) - reqId={}, userCd={}, siteCd={}, defaultSchCd={}",
                reqId, userCd, siteCd, defaultSchCd);

        return DefaultSchChangeRequestResponse.builder()
                .reqId(reqId)
                .reqStatus(AttdReqTypeUtils.REQ_STATUS_REQUESTED)
                .build();
    }

    /**
     * F15 advisory lock 획득(AppReq07ServiceImpl.acquireDupLock 미러). 타임아웃/오류면 동시 처리로 보고
     * ATTD_400_090(중복 요청)으로 변환.
     */
    private void acquireDupLock(String lockKey) {
        Integer got = appMypage01Mapper.getAdvisoryLock(lockKey, DUP_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[PRAFTA-002] 중복차단 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_090);
        }
    }

    /** advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseDupLock(String lockKey) {
        try {
            appMypage01Mapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[PRAFTA-002] 중복차단 advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /** 세션 클레임(gv_siteCd) 우선, 비어 있으면 DB 조회로 폴백(레거시 토큰 대비, setDefaultSch 패턴 미러). */
    private String resolveSiteCd(TokenInfo tokenInfo) {
        String siteCd = tokenInfo.gv_siteCd();
        if (siteCd == null || siteCd.isBlank()) {
            siteCd = defaultSchGenMapper.selectUserSiteCd(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        }
        return siteCd;
    }

    // ============================================================
    // helpers
    // ============================================================

    private Map<String, List<PresetStepItem>> groupSteps(List<PresetStepResult> steps) {
        Map<String, List<PresetStepItem>> map = new LinkedHashMap<>();
        for (PresetStepResult s : steps) {
            map.computeIfAbsent(s.presetId(), k -> new ArrayList<>()).add(toStepItem(s));
        }
        return map;
    }

    private PresetStepItem toStepItem(PresetStepResult s) {
        return PresetStepItem.builder()
                .stepNo(s.stepNo() == null ? 0 : s.stepNo())
                .approverUserCd(s.approverUserCd())
                .userNm(s.userNm())
                .userId(s.userId())
                .rankNm(s.rankNm())
                .nodeNm(s.nodeNm())
                .build();
    }

    /** 프리셋이 본인 소유인지 검증. 미존재 404, 타인 소유 403. */
    private void requireOwnership(String cmpnyCd, String presetId, String userCd) {
        if (presetId == null || presetId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String owner = appMypage01Mapper.selectPresetOwner(cmpnyCd, presetId);
        if (owner == null) {
            throw new ApiException(MypageErrorCode.PRESET_NOT_FOUND);
        }
        if (!owner.equals(userCd)) {
            log.warn("앱 프리셋 소유권 위반. cmpnyCd={}, presetId={}, requester={}", cmpnyCd, presetId, userCd);
            throw new ApiException(MypageErrorCode.PRESET_FORBIDDEN);
        }
    }

    /** trim + 빈값 제거 + 중복 차단. 입력 순서 보존. */
    private List<String> sanitizeApprovers(List<String> raw) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (raw == null) {
            return result;
        }
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (!seen.add(t)) {
                throw new ApiException(MypageErrorCode.PRESET_APPROVER_DUPLICATED);
            }
            result.add(t);
        }
        return result;
    }

    private boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        int len = password.length();
        if (len < PW_MIN_LEN || len > PW_MAX_LEN) {
            return false;
        }
        int typeCount = 0;
        if (password.matches(".*[0-9].*")) {
            typeCount++;
        }
        if (password.matches(".*[a-zA-Z].*")) {
            typeCount++;
        }
        if (password.matches(".*[^a-zA-Z0-9].*")) {
            typeCount++;
        }
        return typeCount >= 2;
    }

    private boolean isValidEmail(String email) {
        // 보수적 RFC5322 근사: local@domain.tld.
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidYmd(String ymd) {
        try {
            java.time.LocalDate.parse(ymd, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isFutureYmd(String ymd) {
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(ymd, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            return d.isAfter(java.time.LocalDate.now());
        } catch (Exception e) {
            return true;
        }
    }

    private String generateAuthCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        return Integer.toString(100000 + random.nextInt(900000));
    }

    // ----- 마스킹 (PII 평문 노출 금지) -----

    /** 휴대폰 마스킹: 010-****-8295. LAST4 우선, 없으면 평문에서 산출. */
    private String maskPhone(String mblNoPlain, String last4) {
        String l4 = last4;
        if ((l4 == null || l4.isBlank())) {
            String norm = Normalizers.normalizePhone(mblNoPlain);
            l4 = Normalizers.last4(norm);
        }
        if (l4 == null || l4.isBlank()) {
            return null;
        }
        return "010-****-" + l4;
    }

    /** 이메일 마스킹: t***@domain. local 첫 글자만 노출. */
    private String maskEmail(String emailPlain, String domain) {
        String local = null;
        String dom = domain;
        if (emailPlain != null && !emailPlain.isBlank()) {
            int at = emailPlain.lastIndexOf('@');
            if (at > 0) {
                local = emailPlain.substring(0, at);
                if (dom == null || dom.isBlank()) {
                    dom = emailPlain.substring(at + 1);
                }
            }
        }
        if (local == null || local.isEmpty() || dom == null || dom.isBlank()) {
            return null;
        }
        String head = local.substring(0, 1);
        return head + "***@" + dom;
    }

    /** 생년월일 마스킹: 1993-**-**. */
    private String maskBirth(String birthPlain) {
        String norm = Normalizers.normalizeBirth(birthPlain);
        if (norm == null || norm.length() < 4) {
            return null;
        }
        return norm.substring(0, 4) + "-**-**";
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
