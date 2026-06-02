package com.prafta.app.mypage.mypage01.service;

import com.prafta.app.mypage.mypage01.application.param.ApprovalCandidateParam;
import com.prafta.app.mypage.mypage01.application.param.MobileSendParam;
import com.prafta.app.mypage.mypage01.application.param.MobileVerifyParam;
import com.prafta.app.mypage.mypage01.application.param.PasswordChangeParam;
import com.prafta.app.mypage.mypage01.application.param.PresetActionParam;
import com.prafta.app.mypage.mypage01.application.param.PresetSaveParam;
import com.prafta.app.mypage.mypage01.application.param.ProfileUpdateParam;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateListResponse;
import com.prafta.app.mypage.mypage01.dto.response.MobileSendResponse;
import com.prafta.app.mypage.mypage01.dto.response.MobileVerifyResponse;
import com.prafta.app.mypage.mypage01.dto.response.MypageProfileEditResponse;
import com.prafta.app.mypage.mypage01.dto.response.MypageProfileResponse;
import com.prafta.app.mypage.mypage01.dto.response.PresetItemResult;
import com.prafta.app.mypage.mypage01.dto.response.PresetListResponse;
import com.prafta.app.mypage.mypage01.dto.response.PresetSaveResponse;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-app-010: 앱 마이페이지 서비스.
 */
public interface AppMypage01Service {

    /** 010-01: 마이페이지 메인 프로필(마스킹). */
    MypageProfileResponse getProfile(TokenInfo tokenInfo);

    /** 010-01b: 개인정보 수정 진입 전용 프로필(복호화 전체). */
    MypageProfileEditResponse getProfileForEdit(TokenInfo tokenInfo);

    /** 010-02: 프로필 저장. */
    void updateProfile(ProfileUpdateParam param);

    /** 010-03a: 휴대폰 변경 인증번호 발송(앱 전용). */
    MobileSendResponse sendMobileVerification(MobileSendParam param);

    /** 010-03b: 휴대폰 변경 인증 검증(앱 전용, 로그인 토큰 미발급). */
    MobileVerifyResponse verifyMobile(MobileVerifyParam param);

    /** 010-04: 비밀번호 변경. */
    void changePassword(PasswordChangeParam param);

    /** 010-05: 프리셋 목록. */
    PresetListResponse getPresets(TokenInfo tokenInfo);

    /** 010-05: 프리셋 단건. */
    PresetItemResult getPreset(TokenInfo tokenInfo, String presetId);

    /** 010-05: 프리셋 저장(신규/수정). */
    PresetSaveResponse savePreset(PresetSaveParam param);

    /** 010-05: 프리셋 기본 지정. */
    void setDefaultPreset(PresetActionParam param);

    /** 010-05: 프리셋 삭제(hard delete). */
    void deletePreset(PresetActionParam param);

    /** 010-05: 결재자 후보 목록. */
    ApprovalCandidateListResponse getApprovalCandidates(ApprovalCandidateParam param);
}
