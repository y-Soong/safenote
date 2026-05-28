package com.prafta.common.cmm.dailyjoin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.dailyjoin.application.command.DailyUserSlotUpdCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertDailyUserCommand;
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

    /** 빈 슬롯 점유(CURR_USER_CD 매핑). 영향받은 행 수를 반환(동시성 충돌 판정용). */
    int updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand command);

    /** 필수약관 목록 조회 (TB_TERMS REQUIRED_YN='Y' AND USE_YN='Y'). */
    List<RequiredTermsResult> selectRequiredTermsList();

    /** 약관 동의 이력 insert (TB_TERMS_USER_AGR_MGMT). */
    void insertTermsUserAgrMgmt(TermsUserAgrCommand command);
}
