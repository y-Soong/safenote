package com.prafta.common.cmm.dailyjoin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.dailyjoin.application.command.DailyUserSlotUpdCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertDailyUserCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertSlotHisCommand;
import com.prafta.common.cmm.dailyjoin.application.command.TermsUserAgrCommand;
import com.prafta.common.cmm.dailyjoin.application.query.EmptySlotQuery;
import com.prafta.common.cmm.dailyjoin.application.query.LinkPolicyQuery;
import com.prafta.common.cmm.dailyjoin.application.query.MblNoDupleQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SiteInfoQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SmsConsumeQuery;
import com.prafta.common.cmm.dailyjoin.application.query.UserIdDupleCheckQuery;
import com.prafta.common.cmm.dailyjoin.result.RequiredTermsResult;
import com.prafta.common.cmm.dailyjoin.result.SiteInfoResult;

@Mapper
public interface DailyJoinMapper {

    /** 회사/사업장 정보 조회 (TB_CMPNY + TB_SITE, 비로그인 단독 조회). */
    SiteInfoResult selectSiteInfo(SiteInfoQuery query);

    /** 사용자ID 중복 카운트 (TB_DAILY_USER, USE_YN='Y'). */
    int selectUserIdDupleCnt(UserIdDupleCheckQuery query);

    /** 휴대폰번호 중복 카운트 (TB_DAILY_USER, CMPNY_CD + MBL_NO_HMAC 기준 USE_YN='Y'). */
    int selectMblNoDupleCnt(MblNoDupleQuery query);

    /** 가입 시점 SMS 인증 재검증 - 인증완료(VERIFIED_YN='Y') 레코드의 SMS_ID 조회. 없으면 null. */
    String selectSmsVerifiedSmsId(SmsConsumeQuery query);

    /** SMS 인증 레코드 소진 처리 (VERIFIED_YN='Y' -> 'C' 조건부 UPDATE). 영향행 수 반환. */
    int consumeSmsAuth(SmsAuthConsumeCommand command);

    /** 사업장 계정등록 정책 사용여부 조회 (TB_DAILY_USER_LINK_POLICY.USE_YN). */
    String selectLinkPolicyUseYn(LinkPolicyQuery query);

    /** 사업장 첫 빈 슬롯 번호 조회 (TB_DAILY_USER_SLOT). 없으면 null. */
    String selectFirstEmptySlotNo(EmptySlotQuery query);

    /** USER_CD 채번 (CONCAT('D', YYYYMMDD, 시퀀스)). */
    String selectDailyUserCd(EmptySlotQuery query);

    /** 일일사용자 계정 insert. */
    void insertDailyUser(InsertDailyUserCommand command);

    // ===== PRAFTA-app-027-3'(통합형) — TB_USER 통합 INSERT + 중복검증 확장 =====

    /** TB_USER 사용자ID 중복 카운트(UX_TB_USER_ID 사전 차단, 활성 USE_YN='Y'만 — 옵션2 재활성 허용). */
    int selectTbUserIdDupleCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userId") String userId);

    /** TB_USER 휴대폰 HMAC 중복 카운트(UX_TB_USER_MBL_NO 사전 차단, 활성 USE_YN='Y'만 — 옵션2 재활성 허용). */
    int selectTbUserMblHmacDupleCnt(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

    /** 통합형 — TB_USER INSERT(EMPLOYMENT_TYPE='DAILY'/AUTH_CD='99999'/NODE_CD=NULL/ACCOUNT_STATUS='01'). */
    void insertDailyUserToTbUser(InsertDailyUserCommand command);

    /** 통합형 — TB_USER_SITE_AUTH INSERT(해당 사업장 1행, USE_YN='Y'). */
    void insertTbUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd);

    /** 빈 슬롯 점유(CURR_USER_CD 매핑). 영향받은 행 수를 반환(동시성 충돌 판정용). */
    int updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand command);

    /** 필수약관 목록 조회 (TB_TERMS REQUIRED_YN='Y' AND USE_YN='Y'). */
    List<RequiredTermsResult> selectRequiredTermsList();

    /** 약관 동의 이력 insert (TB_TERMS_USER_AGR_MGMT). */
    void insertTermsUserAgrMgmt(TermsUserAgrCommand command);

    // ===== 옵션2 — 재활성 재가입 =====

    /** 재활성 대상 USER_CD 조회(TB_DAILY_USER 비활성 USE_YN='N' 중 최신 1건). 없으면 null. */
    String selectReactivatableDailyUserCd(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

    /** 재활성 — TB_DAILY_USER 기존 행 재사용 UPDATE. 영향행 수 반환. */
    int reactivateDailyUser(InsertDailyUserCommand command);

    /** 재활성 — TB_USER 기존 통합행 재사용 UPDATE(EMPLOYMENT_TYPE='DAILY'). 영향행 0이면 레거시 폴백 신호. */
    int reactivateTbUser(InsertDailyUserCommand command);

    /** 재활성 — TB_USER_SITE_AUTH upsert(사업장 권한 행 활성). */
    int upsertTbUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd);

    // ===== PRAFTA-055-1: 슬롯 사용 이력(TB_DAILY_USER_SLOT_HIS) 적재(자가가입 점유) =====

    /** 이력ID 채번('H' + YYYYMMDD + 시퀀스). */
    String selectDailySlotHisId(@Param("cmpnyCd") String cmpnyCd);

    /** 점유 시작 INSERT(RELEASE_* = NULL, ISSUE_CHANNEL='01' 직접가입). */
    void insertSlotHis(InsertSlotHisCommand command);
}
