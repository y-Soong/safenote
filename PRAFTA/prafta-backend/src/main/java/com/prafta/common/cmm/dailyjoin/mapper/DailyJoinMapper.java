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

    /**
     * 사용자ID 중복 카운트 (TB_DAILY_USER, 제출 단계용).
     * prafta-app-032 A: 활성/비활성 무관(전상태) + 재활성 대상 행(reuseUserCd) 제외.
     */
    int selectUserIdDupleCnt(UserIdDupleCheckQuery query);

    /**
     * 사용자ID 중복 카운트 (TB_DAILY_USER, 프리체크 전용 — 활성 USE_YN='Y' 만).
     * prafta-app-032 §2: 프리체크(checkUserIdDuple)는 활성-only 유지(휴대폰 컨텍스트 없음).
     */
    int selectActiveUserIdDupleCnt(UserIdDupleCheckQuery query);

    /** 휴대폰번호 중복 카운트 (TB_DAILY_USER, CMPNY_CD + MBL_NO_HMAC 기준 USE_YN='Y'). */
    int selectMblNoDupleCnt(MblNoDupleQuery query);

    /**
     * prafta-daily-blacklist: 활성 블랙리스트 등록 여부 카운트(가입/재활성 게이트 공용).
     * TB_DAILY_BLACKLIST 에 CMPNY_CD + MBL_NO_HMAC + USE_YN='Y' 행이 있으면 1 이상 → 차단.
     */
    int selectActiveBlacklistCnt(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

    /** 가입 시점 SMS 인증 재검증 - 인증완료(VERIFIED_YN='Y') 레코드의 SMS_ID 조회. 없으면 null. */
    String selectSmsVerifiedSmsId(SmsConsumeQuery query);

    /** SMS 인증 레코드 소진 처리 (VERIFIED_YN='Y' -> 'C' 조건부 UPDATE). 영향행 수 반환. */
    int consumeSmsAuth(SmsAuthConsumeCommand command);

    /** 사업장 계정등록 정책 사용여부 조회 (TB_DAILY_USER_LINK_POLICY.USE_YN). */
    String selectLinkPolicyUseYn(LinkPolicyQuery query);

    /** 사업장 첫 빈 슬롯 번호 조회 (TB_DAILY_USER_SLOT). 없으면 null. */
    String selectFirstEmptySlotNo(EmptySlotQuery query);

    /**
     * prafta-app-032 보강1(a): 해당 사용자가 사업장에서 이미 점유 중인 슬롯 번호 조회. 없으면 null.
     * 동시 재입장 시 1인 다중 슬롯 점유를 막기 위해 신규 점유 전에 멱등 재사용 대상을 확인한다.
     * FOR UPDATE 잠금 읽기로 직전 트랜잭션의 커밋된 점유를 반드시 관측한다(REPEATABLE READ 스냅샷 우회).
     */
    String selectSlotNoOccupiedByUser(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("userCd") String userCd);

    /**
     * prafta-app-032 보강2: 같은 회사·같은 휴대폰 HMAC 의 다른 활성 일용직 수(대상 USER_CD 제외).
     * 재활성(USE_YN N→Y) 직전 검사로, UX_TB_DAILY_USER_MBL(활성 한정 유니크) 위반 500 을 사전 차단한다.
     */
    int selectActiveDailyUserMblDupleCntExcept(@Param("cmpnyCd") String cmpnyCd,
            @Param("mblNoHmac") String mblNoHmac, @Param("userCd") String userCd);

    /** USER_CD 채번 (CONCAT('D', YYYYMMDD, 시퀀스)). */
    String selectDailyUserCd(EmptySlotQuery query);

    /** 일일사용자 계정 insert. */
    void insertDailyUser(InsertDailyUserCommand command);

    // ===== PRAFTA-app-027-3'(통합형) — TB_USER 통합 INSERT + 중복검증 확장 =====

    /**
     * TB_USER 사용자ID 중복 카운트(UX_TB_USER_ID 사전 차단).
     * prafta-app-032 A: UX_TB_USER_ID 는 활성/비활성 무관 절대 유니크이므로 전상태로 검사하되,
     * 휴대폰 기반 재활성 대상 행(reuseUserCd)은 제외한다(같은 휴대폰 복귀자가 자기 옛 USER_CD 를 재활성하는 케이스).
     */
    int selectTbUserIdDupleCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userId") String userId,
            @Param("reuseUserCd") String reuseUserCd);

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

    // ===== PRAFTA-daily-user-dept-3: 가입 점유 시 슬롯 지정부서 → TB_USER.NODE_CD 매칭 =====

    /** 점유한 슬롯의 지정 부서(NODE_CD) 조회. 미존재/미지정이면 null. */
    String selectSlotNodeCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("slotNo") String slotNo);

    /** 슬롯 지정부서를 점유 일용직(EMPLOYMENT_TYPE='DAILY')의 TB_USER.NODE_CD 로 무조건 세팅. */
    void updateTbUserNodeCdFromSlot(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("nodeCd") String nodeCd);
}
