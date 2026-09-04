package com.prafta.app.mypage.mypage01.service;

import com.prafta.app.mypage.mypage01.application.param.ApprovalCandidateParam;
import com.prafta.app.mypage.mypage01.application.param.BrkWaiveStandingUpdateParam;
import com.prafta.app.mypage.mypage01.application.param.MobileSendParam;
import com.prafta.app.mypage.mypage01.application.param.MobileVerifyParam;
import com.prafta.app.mypage.mypage01.application.param.PasswordChangeParam;
import com.prafta.app.mypage.mypage01.application.param.PresetActionParam;
import com.prafta.app.mypage.mypage01.application.param.PresetSaveParam;
import com.prafta.app.mypage.mypage01.application.param.ProfileUpdateParam;
import com.prafta.app.mypage.mypage01.application.param.UpdateDefaultSchParam;
import com.prafta.app.mypage.mypage01.dto.response.ApprovalCandidateListResponse;
import com.prafta.app.mypage.mypage01.dto.response.BrkWaiveStandingResponse;
import com.prafta.app.mypage.mypage01.dto.response.DefaultSchChangeRequestResponse;
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

    // ===== F-8-2: 본인 기본 근무타입 자기변경(세션 사업장 고정) =====

    /** 선택지 조회 — 대상 사업장은 세션 토큰 식별 사용자의 SITE_CD 로만 도출(파라미터 없음). */
    java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getDefaultSchOptions(TokenInfo tokenInfo);

    /**
     * PRAFTA-002(기본근무타입-승인제): 화이트리스트 검증 후 요청 등록(TB_USER_ATTD_REQ INSERT + 결재선
     * 적용). 즉시 반영하지 않는다 — 실제 반영은 승인 시점(Attd07Service.approveDefaultSchChangeRequest).
     */
    DefaultSchChangeRequestResponse updateDefaultSch(UpdateDefaultSchParam param);

    // ===== BW-12(§7-1): 휴게 미이용 상시 요청(근기법 제54조① 단서) — 근로자 본인 전용 =====

    /**
     * 현행값 조회. 노출 조건(eligibleYn)은 정규직 + 기본 근무타입 소정 240분·휴게 0 기준으로 서버가 판정한다.
     */
    BrkWaiveStandingResponse getBrkWaiveStanding(TokenInfo tokenInfo);

    /**
     * 상시 요청 저장(현행값 UPDATE + 이력 INSERT, 한 트랜잭션). 토큰 본인만 — 관리자 대리 경로 없음.
     * 값이 'Y'/'N' 이 아니거나 일용직(DAILY)이면 ATTD_400_220 으로 거부한다.
     */
    BrkWaiveStandingResponse updateBrkWaiveStanding(BrkWaiveStandingUpdateParam param);
}
