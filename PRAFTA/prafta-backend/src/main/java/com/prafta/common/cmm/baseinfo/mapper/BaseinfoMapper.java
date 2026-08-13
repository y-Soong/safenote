package com.prafta.common.cmm.baseinfo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.baseinfo.application.command.MblUniqueCheckCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthNoCommand;
import com.prafta.common.cmm.baseinfo.application.command.UserPasswordCommand;
import com.prafta.common.cmm.baseinfo.application.query.AppMenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.BaseInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.CmpnyInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.MblUniqueCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.MenuListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.SmsVerifiedCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.SiteNodeListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.SystInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.TermsDetailInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserIdDupleCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserIdInfoQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserInfoListQuery;
import com.prafta.common.cmm.baseinfo.application.query.UserSmsAuthNoCheckQuery;
import com.prafta.common.cmm.baseinfo.application.query.WebMenuListQuery;
import com.prafta.common.cmm.baseinfo.result.AppMenuResult;
import com.prafta.common.cmm.baseinfo.result.BaseInfoResult;
import com.prafta.common.cmm.baseinfo.result.CmpnyInfoResult;
import com.prafta.common.cmm.baseinfo.result.MenuInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteNodeInfoResult;
import com.prafta.common.cmm.baseinfo.result.SystInfoResult;
import com.prafta.common.cmm.baseinfo.result.JoinTermsResult;
import com.prafta.common.cmm.baseinfo.result.TermsDetailInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserIdInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserInfoResult;
import com.prafta.common.cmm.baseinfo.result.WebMenuResult;

@Mapper
public interface BaseinfoMapper {
	List<SystInfoResult> selectSystinfoList(SystInfoListQuery reqDto);
	
	List<SystInfoResult> selectSystinfo(SystInfoQuery dto);
	
	List<BaseInfoResult> selectBaseinfoList(BaseInfoListQuery query);
	
	List<BaseInfoResult> selectBaseinfo(BaseInfoQuery query);
	
	CmpnyInfoResult selectCmpnyInfo(CmpnyInfoQuery query);
	
	String getUserIdDupleCheck(UserIdDupleCheckQuery query);
	
	int selectMblUniqChk(MblUniqueCheckQuery dto);
	
	void insertSmsAuthNo(SmsAuthNoCommand dto);

	/**
	 * SMS-PPURIO-04: 최근 1분 내 동일 휴대폰(SELF_JOIN 목적) 발송 건수 — 서버측 레이트리밋.
	 * 1 이상이면 신규 발송 거부(SMS_400_001). 발송 실패(FAILED) 행은 카운트에서 제외한다.
	 */
	int selectRecentSelfJoinSmsSendCnt(@Param("mblNoHmac") String mblNoHmac);
	
	int updateSmsAuthReq(MblUniqueCheckCommand mblUniqueCheckCommand);
	
	String selectCertNoSmsId(UserSmsAuthNoCheckQuery dto);
	
	List<SiteInfoResult> selectSiteInfoList(SiteInfoQuery query);
	
	List<SiteNodeInfoResult> selectSiteNodeList(SiteNodeListQuery query);
	
	List<WebMenuResult> selectWebMenuList(WebMenuListQuery query);
	
	List<AppMenuResult> selectAppMenuList(AppMenuListQuery query);
	
	List<MenuInfoResult> selectMenuList(MenuListQuery query);

	/**
	 * 메뉴 조회 시 isFavorite 세팅용 — 사용자별 즐겨찾기 MENU_D_ID 목록.
	 * USER_CD/CMPNY_CD 는 JWT 도출값만 전달(IDOR 방지).
	 */
	List<String> selectMyFavoriteMenuDIds(
			@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd
			, @org.apache.ibatis.annotations.Param("userCd") String userCd);

	List<UserInfoResult> selectUserInfoList(UserInfoListQuery query);

	UserIdInfoResult selectUserIdInfo(UserIdInfoQuery query);
	
	void updateUserPw(UserPasswordCommand command);

	/* prafta-app-032 D: 비밀번호 재설정 시 일용직 로그인 테이블(TB_DAILY_USER.USER_PW) 동기 갱신. 정규 사용자엔 0행 no-op. */
	int updateDailyUserPw(UserPasswordCommand command);

	/* 비밀번호 재설정 진입 시 대상 사용자의 최근 SMS 인증 성공(미만료/미소비) 레코드 SMS_ID 조회 */
	String selectSmsVerifiedSmsId(SmsVerifiedCheckQuery query);

	/* 비밀번호 재설정 검증에 사용된 SMS 인증 레코드를 소비(consume) 처리 */
	int consumeSmsAuth(SmsAuthConsumeCommand command);
	
	TermsDetailInfoResult selectTermsDetailInfo(TermsDetailInfoQuery query);

	/**
	 * 회원가입 화면용 필수약관 목록 (TB_TERMS.REQUIRED_YN='Y').
	 *
	 * <p>가입 시 동의 행을 넣는 {@code LoginMapper.selectRequiredTermsList} 와 판정 조건이
	 * 같다. 화면 목록과 저장 목록이 어긋나지 않도록 한쪽만 바꾸지 말 것.
	 */
	List<JoinTermsResult> selectJoinTermsList();

	/**
	 * SMS2-A1: 인증번호 불일치/만료/초과 시 최신 미검증 SELF_JOIN 레코드의 FAIL_CNT +1.
	 *
	 * <p>★UPDATE_NO / UPDATE_DATE 는 건드리지 않는다(XML 주석 참조).
	 *    UPDATE_DATE 는 비밀번호 재설정 10분 창의 기산점이라 카운터가 갱신하면 인증 우회가 된다.
	 * <p>★[3차 / sec N-2] 상한에 처음 도달하는 순간 {@code FAIL_LOCKED_AT} 을 함께 찍는다(잠금 시작 시각).
	 *
	 * @param verifyFailLimit 정책값 {@code TB_SMS_SEND_POLICY.VERIFY_FAIL_LIMIT}
	 */
	int increaseSelfJoinSmsFailCnt(@Param("mblNoHmac") String mblNoHmac
			, @Param("verifyFailLimit") int verifyFailLimit);

	/**
	 * SMS2-A1: 최신 미검증 SELF_JOIN 레코드가 대입 상한에 도달(=현재 잠금 상태)했는지. 도달했으면 1, 아니면 0.
	 * 초과 시에만 사용자에게 SMS_400_002 를 내려 정상 사용자의 이탈을 막는다.
	 *
	 * <p>★[3차] 잠금 만료분은 {@code SmsVerifyGuard} 가 이 호출 전에 이미 0 으로 되돌린다.
	 *
	 * @param verifyFailLimit 정책값 {@code TB_SMS_SEND_POLICY.VERIFY_FAIL_LIMIT}
	 */
	int selectSelfJoinFailExceeded(@Param("mblNoHmac") String mblNoHmac
			, @Param("verifyFailLimit") int verifyFailLimit);

	/**
	 * SMS2-D4: 신규 인증코드 INSERT 직전에 동일 휴대폰(SELF_JOIN 목적)의 기존 미검증·미만료 코드를 만료 처리.
	 * "동시에 유효한 코드 N개" 상태를 없애 무작위 대입 적중률이 N배로 커지는 것을 막는다(sec H-3).
	 */
	int expireOldSelfJoinSmsAuth(@Param("mblNoHmac") String mblNoHmac);
}
