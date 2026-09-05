package com.prafta.app.mypage.mypage01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.mypage.mypage01.application.command.DefaultSchChangeReqInsertCommand;
import com.prafta.app.mypage.mypage01.result.ApprovalCandidateResult;
import com.prafta.app.mypage.mypage01.result.PresetMasterResult;
import com.prafta.app.mypage.mypage01.result.PresetStepResult;
import com.prafta.app.mypage.mypage01.result.UserProfileResult;

/**
 * prafta-app-010: 앱 마이페이지 Mapper (mypage01).
 *
 * <p>D2: web user04 의 SQL 을 참고하되 앱 전용으로 신규 작성한다(테이블만 공유).
 * 모든 조회/수정은 토큰 회사/사용자 스코프로 격리하고 단건 액션은 소유권을 재확인한다(IDOR 방지).
 */
@Mapper
public interface AppMypage01Mapper {

    // ============================================================
    // 프로필 (010-01 / 010-01b / 010-02)
    // ============================================================

    /**
     * 본인 프로필 1행. PII(mblNo/email/birthDate)는 SELECT 자동 복호화 TypeHandler 로 평문 매핑.
     * 식별자는 토큰값(cmpnyCd/userCd)만 사용.
     */
    UserProfileResult selectMyProfile(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("userCd") String userCd);

    /** 본인 소유 활성 프리셋 개수(메인 메타). */
    int countMyPresets(@Param("cmpnyCd") String cmpnyCd,
                       @Param("userCd") String userCd);

    /** 휴대폰(HMAC) 사용 중인 다른 사용자 수(본인 제외). 변경 발송 시 중복 검사. */
    int countOtherUserByMblHmac(@Param("cmpnyCd") String cmpnyCd,
                                @Param("userCd") String userCd,
                                @Param("mblNoHmac") String mblNoHmac);

    /** 이름/성별/생년월일/이메일 갱신(변경 필드만 UPDATE). */
    int updateProfileBasic(@Param("cmpnyCd") String cmpnyCd,
                           @Param("userCd") String userCd,
                           @Param("userNm") String userNm,
                           @Param("genderCode") String genderCode,
                           @Param("birthEnc") String birthEnc,
                           @Param("setBirth") boolean setBirth,
                           @Param("emailEnc") String emailEnc,
                           @Param("emailHmac") String emailHmac,
                           @Param("emailDomain") String emailDomain,
                           @Param("setEmail") boolean setEmail,
                           @Param("updateNo") String updateNo);

    /** 휴대폰 ENC/HMAC/LAST4 동시 갱신(휴대폰 변경 검증 통과 시에만). */
    int updateProfileMobile(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd,
                            @Param("mblNoEnc") String mblNoEnc,
                            @Param("mblNoHmac") String mblNoHmac,
                            @Param("mblNoLast4") String mblNoLast4,
                            @Param("updateNo") String updateNo);

    // ============================================================
    // 휴대폰 변경 인증 (010-03, tb_sms_auth_code 재사용)
    // ============================================================

    /**
     * 인증번호 발송 INSERT(만료 3분).
     * SMS-PPURIO-05: sendRefKey = 발송 결과 UPDATE 의 조인키(INSERT 전에 생성해 함께 저장).
     * SMS2-D5: PURPOSE_CD 는 XML 고정 리터럴 'MOBILE_CHANGE'. ipHash/userCd 는 IP축·사용자축 상한 재료.
     *
     * @param ipHash 요청 IP 해시(확정 불가 시 null — IP 축 스킵)
     * @param userCd 발송 요청 사용자(토큰에서만 결정)
     */
    int insertSmsAuthCode(@Param("mblNoEnc") String mblNoEnc,
                          @Param("mblNoHmac") String mblNoHmac,
                          @Param("authCode") String authCode,
                          @Param("sendRefKey") String sendRefKey,
                          @Param("ipHash") String ipHash,
                          @Param("userCd") String userCd);

    /**
     * SMS2-D5(D4 미러): 신규 코드 INSERT 직전에 기존 미검증 MOBILE_CHANGE 코드를 만료 처리.
     * "동시에 유효한 코드 N개" 상태를 없앤다(sec H-3).
     */
    int expireOldMobileChangeSmsAuth(@Param("mblNoHmac") String mblNoHmac);

    /**
     * 미만료/미검증 인증코드의 SMS_ID 조회(검증용). 없으면 null.
     *
     * @param verifyFailLimit [3차 / sec N-4] 대입 실패 허용 횟수(정책값). 도달한 코드는 매칭되지 않는다
     */
    Long selectValidSmsId(@Param("mblNoHmac") String mblNoHmac,
                          @Param("authCode") String authCode,
                          @Param("verifyFailLimit") int verifyFailLimit);

    /** 미만료/미검증 인증코드 존재 여부(코드 불일치 시 만료/시도 구분용). 없으면 0. */
    int countUnverifiedByMblHmac(@Param("mblNoHmac") String mblNoHmac);

    /**
     * [3차 / sec N-4] 인증번호 불일치/만료/초과 시 최신 미검증 MOBILE_CHANGE 레코드의 FAIL_CNT +1.
     *
     * <p>★UPDATE_NO / UPDATE_DATE 는 건드리지 않는다 — UPDATE_DATE 는 {@link #selectRecentVerifiedSmsId}
     *    의 5분 창 기산점이라 카운터가 갱신하면 인증 우회가 된다(XML 주석 참조).
     */
    int increaseMobileChangeSmsFailCnt(@Param("mblNoHmac") String mblNoHmac,
                                       @Param("verifyFailLimit") int verifyFailLimit);

    /** [3차 / sec N-4] 최신 미검증 MOBILE_CHANGE 레코드가 대입 상한에 도달(=현재 잠금)했으면 1, 아니면 0. */
    int selectMobileChangeFailExceeded(@Param("mblNoHmac") String mblNoHmac,
                                       @Param("verifyFailLimit") int verifyFailLimit);

    /**
     * 검증 성공 처리(VERIFIED_YN='Y').
     *
     * @param verifyFailLimit [3차 / sec N-4] select → update 사이 TOCTOU 차단용(조회와 같은 값일 것)
     */
    int markSmsVerified(@Param("smsId") Long smsId,
                        @Param("mblNoHmac") String mblNoHmac,
                        @Param("authCode") String authCode,
                        @Param("verifyFailLimit") int verifyFailLimit);

    /** 최근(5분 내) 검증완료·미소비 SMS_ID(저장 시 토큰-휴대폰 바인딩 검증). 없으면 null. */
    Long selectRecentVerifiedSmsId(@Param("mblNoHmac") String mblNoHmac);

    /** 검증완료 인증코드 소비(VERIFIED_YN='C'). 동시성 1건만 통과. */
    int consumeSmsAuth(@Param("smsId") Long smsId);

    // ============================================================
    // 비밀번호 변경 (010-04)
    // ============================================================

    /** 현재 비밀번호 해시 조회. */
    String selectUserPw(@Param("cmpnyCd") String cmpnyCd,
                        @Param("userCd") String userCd);

    /** 비밀번호 + 변경일시 갱신. */
    int updateUserPw(@Param("cmpnyCd") String cmpnyCd,
                     @Param("userCd") String userCd,
                     @Param("userPw") String userPw);

    // ============================================================
    // 결재선 프리셋 CRUD (010-05) — D2 앱 전용 신규
    // ============================================================

    List<PresetMasterResult> selectPresetMasters(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd);

    List<PresetStepResult> selectPresetStepsByUser(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("userCd") String userCd);

    /** 단건 프리셋 마스터(소유자 스코프). 없으면 null. */
    PresetMasterResult selectPresetById(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("userCd") String userCd,
                                        @Param("presetId") String presetId);

    /** 단건 프리셋 스텝(소유자 스코프). */
    List<PresetStepResult> selectPresetStepsById(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd,
                                                 @Param("presetId") String presetId);

    /** 프리셋 소유자 USER_CD(소유권 검증용). 없으면 null. */
    String selectPresetOwner(@Param("cmpnyCd") String cmpnyCd,
                             @Param("presetId") String presetId);

    /** 동일 소유자 내 이름 중복 개수(presetId 제외). */
    int countPresetNameDup(@Param("cmpnyCd") String cmpnyCd,
                           @Param("userCd") String userCd,
                           @Param("presetNm") String presetNm,
                           @Param("excludePresetId") String excludePresetId);

    /** 프리셋 ID 채번(P + YYYYMMDD + 시퀀스). */
    String selectNextPresetId(@Param("cmpnyCd") String cmpnyCd);

    /** 결재자 후보 유효성: 본인 사업장 활성 사용자(system 제외) 여부 카운트. */
    int countActiveCandidate(@Param("cmpnyCd") String cmpnyCd,
                             @Param("siteCd") String siteCd,
                             @Param("userCd") String userCd);

    int insertPresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId,
                           @Param("userCd") String userCd,
                           @Param("presetNm") String presetNm,
                           @Param("defaultYn") String defaultYn,
                           @Param("insertNo") String insertNo);

    int updatePresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId,
                           @Param("presetNm") String presetNm,
                           @Param("defaultYn") String defaultYn,
                           @Param("updateNo") String updateNo);

    int insertPresetStep(@Param("cmpnyCd") String cmpnyCd,
                         @Param("presetId") String presetId,
                         @Param("stepNo") int stepNo,
                         @Param("approverUserCd") String approverUserCd,
                         @Param("insertNo") String insertNo);

    int deletePresetSteps(@Param("cmpnyCd") String cmpnyCd,
                          @Param("presetId") String presetId);

    int deletePresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId);

    int clearDefaultForUser(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd);

    int setDefault(@Param("cmpnyCd") String cmpnyCd,
                   @Param("presetId") String presetId,
                   @Param("updateNo") String updateNo);

    // ============================================================
    // 결재자 후보 (010-05)
    // ============================================================

    List<ApprovalCandidateResult> selectApprovalCandidates(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCd") String siteCd,
                                                           @Param("excludeUserCd") String excludeUserCd,
                                                           @Param("nodeCd") String nodeCd,
                                                           @Param("userNm") String userNm);

    Integer selectUserRankSortIdx(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("userCd") String userCd);

    // ============================================================
    // PRAFTA-002: 기본 근무타입 변경 요청(REQ_TYPE='14') — 요청등록 전환
    // ============================================================

    /** REQ_ID 채번(AppReq07Mapper.selectNextReqId 와 동일 시퀀스 'ATTD_REQ_ID' — 단일 PK 공간 공유). */
    String selectNextDefaultSchReqId(@Param("cmpnyCd") String cmpnyCd);

    /** F15 advisory lock 획득(AppReq07Mapper.getAdvisoryLock 미러 — MD5 래핑, 락 이름 64자 제한 대응). */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** F15 advisory lock 해제(AppReq07Mapper.releaseAdvisoryLock 미러). */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);

    /**
     * 동일 사용자의 대기중(REQ_STATUS='01') 기본 근무타입 변경 요청 개수(P10 중복 차단).
     * WORK_YMD 조건 없음(이 요청 유형은 특정 근무일에 종속되지 않음) — 기존 countDuplicateReq 재사용 불가.
     */
    int countPendingDefaultSchChangeReq(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("siteCd") String siteCd,
                                        @Param("userCd") String userCd);

    /** tb_user_attd_req INSERT(REQ_TYPE='14' 전용). */
    int insertDefaultSchChangeReq(DefaultSchChangeReqInsertCommand cmd);

    // (v2 2026-09-05 BW2-08: 휴게 미이용 상시 요청 select/update/insertHist 3본은 §7-1 폐지로 제거됨)
}
