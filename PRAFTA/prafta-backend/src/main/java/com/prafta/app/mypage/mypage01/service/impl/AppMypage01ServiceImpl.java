package com.prafta.app.mypage.mypage01.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.mypage.mypage01.application.param.ApprovalCandidateParam;
import com.prafta.app.mypage.mypage01.application.param.MobileSendParam;
import com.prafta.app.mypage.mypage01.application.param.MobileVerifyParam;
import com.prafta.app.mypage.mypage01.application.param.PasswordChangeParam;
import com.prafta.app.mypage.mypage01.application.param.PresetActionParam;
import com.prafta.app.mypage.mypage01.application.param.PresetSaveParam;
import com.prafta.app.mypage.mypage01.application.param.ProfileUpdateParam;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateItem;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateListResponse;
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
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.mypage.MypageErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;

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
        appMypage01Mapper.insertSmsAuthCode(phoneEnc, phoneHmac, authCode);

        // TODO(developer): 실제 문자 게이트웨이 발송은 baseinfo SMS 인프라와 동일하게 별도 연동 예정.
        //  현재는 인증코드 레코드만 적재(기존 baseinfo insertSmsAuthNo 와 동일 수준). 코드 자체는 로그 금지(콘솔 노출 X).
        log.info("마이페이지 휴대폰 변경 인증번호 발송 - userCd={}, mblLast4={}", userCd, Normalizers.last4(phoneNorm));

        return MobileSendResponse.builder().expiresInSeconds(180).build();
    }

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

        Long smsId = appMypage01Mapper.selectValidSmsId(phoneHmac, code);
        if (smsId == null) {
            // 코드 불일치 vs 만료 구분: 미만료/미검증 레코드 존재 여부로 판단.
            int unverified = appMypage01Mapper.countUnverifiedByMblHmac(phoneHmac);
            if (unverified == 0) {
                throw new ApiException(MypageErrorCode.EXPIRED);
            }
            throw new ApiException(MypageErrorCode.INVALID_CODE);
        }

        // 검증 성공 처리(VERIFIED_YN='Y'). 동시성으로 이미 처리됐으면 만료/재시도로 간주.
        if (appMypage01Mapper.markSmsVerified(smsId, phoneHmac, code) != 1) {
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
