package com.prafta.web.user.user01.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.error.stdwork.StdWorkErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.user.user01.application.command.ScheduleWithdrawalCommand;
import com.prafta.web.user.user01.application.command.UserCreateInsertCommand;
import com.prafta.web.user.user01.application.command.UserCreditDeleteCommand;
import com.prafta.web.user.user01.application.command.UserCreditInsertCommand;
import com.prafta.web.user.user01.application.command.UserHireDateHistoryCommand;
import com.prafta.web.user.user01.application.command.UserHireDateUpdateCommand;
import com.prafta.web.user.user01.application.command.UserInfoCommand;
import com.prafta.web.user.user01.application.command.UserPasswdCommand;
import com.prafta.web.user.user01.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user01.application.command.WithdrawMyAccountCommand;
import com.prafta.web.user.user01.application.command.WithdrawalCancelCommand;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.application.param.HireDateHistoryParam;
import com.prafta.web.user.user01.application.param.HireDateImpactParam;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.application.param.UserCreditParam;
import com.prafta.web.user.user01.application.param.UserHireDateParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UpdateMyDefaultSchParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;
import com.prafta.web.user.user01.application.query.LeaveInfoQuery;
import com.prafta.web.user.user01.application.query.MyProfileQuery;
import com.prafta.web.user.user01.application.query.SiteNodeAdminCandidateListQuery;
import com.prafta.web.user.user01.application.query.UserInfoListQuery;
import com.prafta.web.user.user01.application.query.UserNodeAdminCheckQuery;
import com.prafta.web.user.user01.application.query.UserSiteInfoQuery;
import com.prafta.web.user.user01.dto.response.HireDateHistoryResponse;
import com.prafta.web.user.user01.dto.response.HireDateImpactResponse;
import com.prafta.web.user.user01.dto.response.LeaveInfoResponse;
import com.prafta.web.user.user01.dto.response.MyProfileResponse;
import com.prafta.web.user.user01.dto.response.SiteNodeAdminCandidateListResponse;
import com.prafta.web.user.user01.dto.response.UserInfoListResponse;
import com.prafta.web.user.user01.mapper.User01Mapper;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.MyProfileResult;
import com.prafta.web.user.user01.result.ServiceCreditResult;
import com.prafta.web.user.user01.result.TemplateAuthRow;
import com.prafta.web.user.user01.result.TemplateNodeRow;
import com.prafta.web.user.user01.result.TemplateRankRow;
import com.prafta.web.user.user01.result.TemplateSiteRow;
import com.prafta.web.user.user01.result.UserHireDateHistoryResult;
import com.prafta.web.user.user01.result.UserHireInfoResult;
import com.prafta.web.user.user01.result.UserInfoResult;
import com.prafta.web.user.user01.result.UserPwResult;
import com.prafta.web.user.user01.result.UserSiteInfoResult;
import com.prafta.web.user.user01.result.UserStatutoryLeaveSummaryResult;
import com.prafta.web.user.user01.service.User01Service;
import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.AuditResourceType;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.vo.HireDateAdjustResultVO;
import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user01.util.UserExcelTemplateBuilder;
import com.prafta.web.user.user01.util.UserExcelValueRestorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User01ServiceImpl implements User01Service{
	private final User01Mapper user01Mapper;
	// PRAFTA-WEB_002-T1-02(1.4-1): 무담당 부서 담당(정) 자동지정 공용 SQL(updateNodeMainAdminIfEmpty) 재사용
	//   — 소속이동 발효 경로와 동일 매퍼를 공유한다(중복 구현 금지).
	private final UserTransferMapper userTransferMapper;
	private final PasswordHasher passwordHasher;
	private final HmacSigner hmacSigner;
	private final AesGcmCrypto aesGcmCrypto;
	private final ObjectMapper objectMapper;
	// prafta-023 F: 입사일 변경 영향분석의 "누락된 부여"를 부여 엔진 산식(유효기간 내 소급)과 정합
	private final LeaveGrantEngineService leaveGrantEngineService;
	// PRAFTA-037-F5: 감사 로그 적재 (양식 다운로드부터 1차 적용)
	private final AuditLogService auditLogService;
	// PRAFTA-COM-008-E-5: 기본 근무타입 검증/자동생성 공용 서비스(common.cmm.sch).
	private final com.prafta.common.cmm.sch.service.DefaultSchOptionService defaultSchOptionService;
	private final com.prafta.common.cmm.sch.service.DefaultSchGenService defaultSchGenService;
	// F-8-2: 본인 기본 근무타입 자기변경 — SITE_CD 조회/저장에 공용 매퍼 재사용(LoginServiceImpl.setDefaultSch 패턴).
	private final com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper defaultSchGenMapper;
	// F1/QT-11-7: 비활성/탈퇴 시 진행중 대기요청 일괄 반려 + 연차 원장 원복(소속이동 발효와 단일 출처).
	private final com.prafta.web.user.user01.service.UserPendingRequestTerminationService userPendingRequestTerminationService;
	// 소정-03: 계정 생성 시 소정근로시간 이력 등록(겹침/범위/일용직 검증 포함) 공용 서비스(common.cmm.stdwork).
	private final com.prafta.common.cmm.stdwork.service.StdWorkHoursService stdWorkHoursService;

	private static final int PW_MIN_LEN = 6;
	private static final int PW_MAX_LEN = 15;

	// F1: 사용자 비활성(useYn→N) 시 관련 대기요청 자동 반려 사유(PII 금지, 처리 코멘트 기록용).
	private static final String REASON_DEACTIVATED = "신청자/결재자 비활성으로 자동 반려";
	// F1: 본인 탈퇴 시 관련 대기요청 자동 반려 사유.
	private static final String REASON_WITHDRAWN = "신청자/결재자 탈퇴로 자동 반려";

	// REASON_DETAIL varchar(500) - 경력 인정 상세 설명 서버측 길이 상한
	private static final int REASON_DETAIL_MAX_LEN = 500;

	// ===== 경력인정 이원화(2026-08-21, 지시서 §1-1) =====
	// LEAVE_CALC_YN: 'Y'=반영 모드(개월수를 연차 산식에 반영, 기본) / 'N'=일수 모드(기록용 + 연간 N일 부여).
	private static final String LEAVE_CALC_YN_REFLECT = "Y";
	private static final String LEAVE_CALC_YN_DAYS = "N";
	// EXTRA_LEAVE_DAYS decimal(4,1) — 일수 모드 연간 추가 부여 일수 상한/단위.
	private static final java.math.BigDecimal EXTRA_LEAVE_DAYS_MAX = new java.math.BigDecimal("25");
	private static final java.math.BigDecimal EXTRA_LEAVE_DAYS_UNIT = new java.math.BigDecimal("0.5");

	/**
	 * 연차 반영 모드 정규화 — 미전송(null/blank)은 'Y'(반영 모드)로 하위호환 처리(R-3).
	 * 'Y'/'N' 이외 값은 거부.
	 */
	private String normalizeLeaveCalcYn(String raw) {
		if (isBlank(raw)) {
			return LEAVE_CALC_YN_REFLECT;
		}
		String v = raw.trim();
		if (!LEAVE_CALC_YN_REFLECT.equals(v) && !LEAVE_CALC_YN_DAYS.equals(v)) {
			throw new ApiException(UserErrorCode.USER_400_081);
		}
		return v;
	}

	/**
	 * 일수 모드(N) 전용 연간 추가 부여 일수 검증·정규화.
	 * 반영 모드(Y)는 클라 입력을 신뢰하지 않고 NULL 강제(지시서 §1-1).
	 * 일수 모드(N)는 0.5일 단위·0일 초과·25일 이하 필수.
	 */
	private java.math.BigDecimal resolveExtraLeaveDays(String normalizedLeaveCalcYn, java.math.BigDecimal raw) {
		if (LEAVE_CALC_YN_REFLECT.equals(normalizedLeaveCalcYn)) {
			return null;
		}
		if (raw == null
				|| raw.compareTo(java.math.BigDecimal.ZERO) <= 0
				|| raw.compareTo(EXTRA_LEAVE_DAYS_MAX) > 0
				|| raw.remainder(EXTRA_LEAVE_DAYS_UNIT).compareTo(java.math.BigDecimal.ZERO) != 0) {
			throw new ApiException(UserErrorCode.USER_400_082);
		}
		return raw;
	}

	// PRAFTA-COM-008-E-5: 자동생성 트리거 운영 게이트. 미충족이면 설정만 저장하고 생성은 스킵.
	//   코드 기본값을 true 로 통일(로그인 게이트/배치 스케줄러와 정합). properties 에 명시값이 있으면 그 값 우선.
	//   기본 false 비대칭이 User_01 경로에서 운영 누락(생성 미동작)을 유발하던 문제 해소.
	@org.springframework.beans.factory.annotation.Value("${prafta.default-sch.gen.enabled:true}")
	private boolean defaultSchGenEnabled;

	@Override
	public java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getSchTypeOptions(String cmpnyCd, String siteCd) {
		// PRAFTA-COM-008-E-5: UserInfoPop 기본 근무타입 select 채움(사업장 활성 근무타입).
		return defaultSchOptionService.getActiveSchOptions(cmpnyCd, siteCd);
	}

	/**
	 * F-8-2: 본인 기본 근무타입 자기변경 — 선택지 조회.
	 *
	 * <p>사업장은 세션 토큰 식별 사용자의 SITE_CD 로만 도출한다(IDOR/사업장 이동 우회 방지, 파라미터로 받지 않음).
	 * 사업장 미상이면 빈 목록(LoginController.getDefaultSchOptions 와 동일 관례).
	 */
	@Override
	public java.util.List<com.prafta.common.cmm.sch.vo.SchOptionVO> getMyDefaultSchOptions(String cmpnyCd, String userCd) {
		String siteCd = defaultSchGenMapper.selectUserSiteCd(cmpnyCd, userCd);
		if (siteCd == null || siteCd.isBlank()) {
			return java.util.List.of();
		}
		return defaultSchOptionService.getActiveSchOptions(cmpnyCd, siteCd);
	}

	/**
	 * F-8-2: 본인 기본 근무타입 자기변경 — 저장.
	 *
	 * <p>LoginServiceImpl.setDefaultSch(로그인 게이트) 패턴을 정상 세션 토큰 경로로 재사용한다.
	 * 화이트리스트 검증(defaultSchOptionService.isValidDefaultSch) → DEFAULT_SCH_CD 저장
	 * → 즉시 자동생성(defaultSchGenService.applyDefaultSchChange, 실패는 격리 — 저장 자체엔 영향 없음).
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateMyDefaultSch(UpdateMyDefaultSchParam param) {

		String defaultSchCd = (param.defaultSchCd() == null || param.defaultSchCd().isBlank())
				? null : param.defaultSchCd().trim();
		if (defaultSchCd == null) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_141);
		}

		// 사업장은 세션에서만 도출(본인 사업장 변경은 소속이동 전용 — 여기서 받지 않음).
		String siteCd = defaultSchGenMapper.selectUserSiteCd(param.cmpnyCd(), param.userCd());
		if (siteCd == null || siteCd.isBlank()) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
		}

		// 화이트리스트 검증(클라 제출값 신뢰 금지).
		if (!defaultSchOptionService.isValidDefaultSch(param.cmpnyCd(), siteCd, defaultSchCd)) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
		}

		int updated = defaultSchGenMapper.updateUserDefaultSch(param.cmpnyCd(), param.userCd(), defaultSchCd, param.userCd());
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// 즉시 자동생성(미래 스케줄 갱신) — 실패는 격리(사용자 저장에 영향 없음, 기존 관례 승계).
		if (defaultSchGenEnabled) {
			try {
				defaultSchGenService.applyDefaultSchChange(param.cmpnyCd(), siteCd, param.userCd(), defaultSchCd);
			} catch (Exception e) {
				log.error("본인 기본 근무타입 변경 자동생성 실패(설정은 저장됨) - userCd={}, defaultSchCd={}",
						param.userCd(), defaultSchCd, e);
			}
		}

		log.info("본인 기본 근무타입 변경(자기결정) 완료 - userCd={}, siteCd={}, defaultSchCd={}",
				param.userCd(), siteCd, defaultSchCd);
	}

	public UserInfoListResponse selectUserInfoList(UserInfoListParam param) {
		
		UserInfoListResponse response = null;
		
		List<UserInfoResult> userInfoList = user01Mapper.selectUserInfoList(UserInfoListQuery.from(param));
		
		if(userInfoList != null && userInfoList.size() > 0) {
			response = UserInfoListResponse.builder()
						.userInfoList(userInfoList)
						.build();
		}
		
		return response;
	}
	
	public int updateUserPw(UserPasswdParam param) {

		// 새 비밀번호 = 대상 회원의 휴대폰번호(하이픈 제외) — 회원가입 초기 비밀번호 관례(D3)와 동일.
		String mblNoEnc = user01Mapper.selectUserMblNoEnc(param.cmpnyCd(), param.userCd());
		if (mblNoEnc == null || mblNoEnc.isBlank()) {
			throw new ApiException(UserErrorCode.USER_404_004);
		}
		String phoneNorm = aesGcmCrypto.decrypt(mblNoEnc);
		String userPw = passwordHasher.hash(phoneNorm);

		return user01Mapper.updateUserPw(UserPasswdCommand.from(param, userPw));
	}
	
	
	private static boolean isValidPassword(String password) {
		
	    if (password == null) return false;

	    int len = password.length();
	    if (len < PW_MIN_LEN || len > PW_MAX_LEN) return false;

	    int typeCount = 0;
	    if (password.matches(".*[0-9].*"))            typeCount++;
	    if (password.matches(".*[a-zA-Z].*"))         typeCount++;
	    if (password.matches(".*[^a-zA-Z0-9].*"))     typeCount++;
	    
	    return typeCount >= 2;
	}

	@Override
	public void updateMyPw(MyPasswdParam param) {

		// 1. Look up user by userId
		UserPwResult userPwResult = user01Mapper.selectUserPwByUserId(param.cmpnyCd(), param.userCd());
		if (userPwResult == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// 2. Verify current password
		if (!passwordHasher.matches(param.currentPw(), userPwResult.userPw())) {
			throw new ApiException(UserErrorCode.USER_400_003);
		}

		// 3. Validate new password policy
		if (param.newPw() == null || !isValidPassword(param.newPw())) {
			throw new ApiException(UserErrorCode.USER_400_004);
		}

		// 3.1 PRAFTA-037-F1: 동일 PW 거부 (현재 PW = 신규 PW).
		// 첫 로그인 강제 변경의 의도가 "휴대폰번호=PW 해소" 인데 동일 PW 재입력은 의미 무효.
		// 자발 변경(MyInfoPop)도 동일 가드 적용 — 일관성.
		if (passwordHasher.matches(param.newPw(), userPwResult.userPw())) {
			throw new ApiException(UserErrorCode.USER_400_054);
		}

		// 4. Hash and update
		String newHash = passwordHasher.hash(param.newPw());
		user01Mapper.updateMyPw(
			new UserPasswdCommand(param.cmpnyCd(), userPwResult.userCd(), newHash)
		);
	}
	
	@Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateOneUserInfo(UserInfoModel model) {
		if(
			(model.nodeCd() != null && model.oriNodeCd() != null && !model.nodeCd().equals(model.oriNodeCd()))
			|| (model.useYn().equals("N"))
		) {
			
			int userNodeAdminCheck = user01Mapper.selectUserNodeAdminCheck(UserNodeAdminCheckQuery.from(model));
			
			if(userNodeAdminCheck > 0) {
				if(model.useYn().equals("N")) {
					throw new ApiException(UserErrorCode.USER_400_002);
				} else {
					throw new ApiException(UserErrorCode.USER_400_001);
				}
			}
		}

		// PRAFTA-046 (A-3): 노드가 실제로 바뀌는 경우, 들어가는(새) 노드에 정/부 관리자가 있어야 한다.
		//   비활성(useYn='N')은 노드에서 빠지는 방향이므로 미적용 (들어가는 노드 없음).
		if (model.nodeCd() != null && model.oriNodeCd() != null
				&& !model.nodeCd().equals(model.oriNodeCd())
				&& !"N".equals(model.useYn())) {
			int nodeHasAdmin = user01Mapper.selectNodeHasAdmin(model.cmpnyCd(), model.siteCd(), model.nodeCd());
			if (nodeHasAdmin == 0) {
				throw new ApiException(UserErrorCode.USER_400_056);
			}
		}

        UserSiteInfoResult userInfoResult = user01Mapper.selectUserSiteInfo(UserSiteInfoQuery.from(model));

        if (userInfoResult == null) {
            throw new ApiException(LoginErrorCode.LOGIN_400_002);
        }

        boolean isMaster = "master".equals(model.authCd());
        String reqSiteCd = model.siteCd();
        String curSiteCd = userInfoResult.siteCd();

        // PRAFTA-042-5 (H-1): 변경 전 AUTH_CD 를 mergeUserInfo 호출 이전에 확보한다.
        //   merge 후 조회하면 같은 트랜잭션이라 prevAuthCd 가 신규 authCd 로 덮여
        //   진입/이탈 분기가 전부 dead code 가 된다.
        String prevAuthCd = user01Mapper.selectUserAuthCd(model.cmpnyCd(), model.userCd());

        // PRAFTA-WEB_002-T1-03(1.4-2): 권한 등급 escalation 서버 가드.
        //   권한(authCd)이 실제로 "변경"될 때만, 새 권한 등급이 요청자(viewer) 등급보다 "엄격히 낮은"(=AUTH_LEVEL
        //   숫자가 더 큰) 경우만 허용한다. 동일/상위 등급 부여는 차단(권한 상승 방지).
        //   viewer 등급/대상 등급 모두 서버에서 산출한다(클라가 보낸 등급/옵션을 신뢰하지 않음 — JWT userCd 기준).
        //   authCd 무변경(현재값 패스스루)은 가드 미적용(소속/이메일 등 일반 수정 무영향).
        if (model.authCd() != null && !model.authCd().equals(prevAuthCd)) {
            String viewerAuthCd = user01Mapper.selectUserAuthCd(model.cmpnyCd(), model.gvUserCd());
            Integer viewerLevel = parseAuthLevel(user01Mapper.selectAuthLevelByAuthCd(model.cmpnyCd(), viewerAuthCd));
            Integer newLevel = parseAuthLevel(user01Mapper.selectAuthLevelByAuthCd(model.cmpnyCd(), model.authCd()));
            if (viewerLevel == null || newLevel == null || newLevel <= viewerLevel) {
                log.warn("권한 등급 상승 차단 - 요청자={}, viewerAuthCd={}, viewerLevel={}, 대상userCd={}, prevAuthCd={}, newAuthCd={}, newLevel={}",
                        model.gvUserCd(), viewerAuthCd, viewerLevel, model.userCd(), prevAuthCd, model.authCd(), newLevel);
                throw new ApiException(UserErrorCode.USER_403_003);
            }
        }

        String phoneNorm = "";
        String emailNorm = Normalizers.normalizeEmail(model.email());
        String birthNorm = Normalizers.normalizeBirth(model.birthDt());

        String phoneEnc = "";
        String emailEnc = "";
        String birthEnc = "";

        String phoneHmac = "";
        String emailHmac = "";
        
        if(model.mblNo() != null && !model.mblNo().isBlank()) {
        	phoneNorm = Normalizers.normalizePhone(model.mblNo());
        	phoneEnc = (phoneNorm == null) ? null : aesGcmCrypto.encrypt(phoneNorm);
        	phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm);
        }
        
        if(model.email() != null && !model.email().isBlank()) {
        	emailNorm = Normalizers.normalizeEmail(model.email());
        	emailEnc = (emailNorm == null) ? null : aesGcmCrypto.encrypt(emailNorm);
        	emailHmac = (emailNorm == null) ? null : hmacSigner.hmacSha256Base64Url(emailNorm);
        }
        
        if(model.birthDt() != null && !model.birthDt().isBlank()) {
        	birthEnc = (birthNorm == null) ? null : aesGcmCrypto.encrypt(birthNorm);
        }

        if (!isMaster && reqSiteCd != null && curSiteCd != null && !reqSiteCd.equals(curSiteCd)) {
            user01Mapper.deleteUserSiteAuth(UserSiteAuthCommand.from(model));
            user01Mapper.insertUserSiteAuth(UserSiteAuthCommand.from(model));
        }

        // PRAFTA-COM-008-E-5: 기본 근무타입 입력 시 화이트리스트 검증(클라 신뢰 금지).
        //   대상 사업장 = 변경 후 SITE_CD(model.siteCd()). blank 면 미변경(mergeUserInfo 에서 미세팅).
        String editDefaultSchCd = (model.defaultSchCd() == null || model.defaultSchCd().isBlank())
                ? null : model.defaultSchCd().trim();
        if (editDefaultSchCd != null
                && !defaultSchOptionService.isValidDefaultSch(model.cmpnyCd(), model.siteCd(), editDefaultSchCd)) {
            throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
        }

        user01Mapper.mergeUserInfo(UserInfoCommand.from(model, phoneEnc, phoneHmac, emailEnc, emailHmac, birthEnc));

        // PRAFTA-COM-008-E-5: 기본 근무타입 설정/변경 시 즉시 미래 자동생성분 갱신 + 신규 생성(E-3 트리거2).
        //   교대 비소속·미마감월만 반영(서비스 내부 가드). 실패는 격리(사용자 저장 영향 없음).
        if (editDefaultSchCd != null && defaultSchGenEnabled) {
            try {
                defaultSchGenService.applyDefaultSchChange(model.cmpnyCd(), model.siteCd(), model.userCd(), editDefaultSchCd);
            } catch (Exception e) {
                log.error("기본근무 변경 자동생성 실패(사용자 저장 영향 없음) - userCd={}, defaultSchCd={}",
                        model.userCd(), editDefaultSchCd, e);
            }
        }

        // PRAFTA-042-5 (D3-②/D7): 역할 변경에 따른 전사 사업장권한 자동부여/회수.
        //   변경 전 AUTH_CD(prevAuthCd) vs 신규 AUTH_CD(model.authCd()) 비교로 진입/이탈 판정한다.
        //   - 비대상 → master/hr/safe 진입: 전 사업장 멱등 부여.
        //   - master/hr/safe → 비대상 이탈: 자동부여분 회수, 소속 사업장(변경 후 SITE_CD) 1건만 잔존.
        //   - 역할 변동 없음: 무처리(신규 사업장 백필은 042-4 가 담당).
        //   prevAuthCd 는 merge 이전 시점에 이미 확보했다(H-1 수정).
        String newAuthCd = model.authCd();
        boolean wasCompanyWide = isCompanyWideAuthRole(prevAuthCd);
        boolean nowCompanyWide = isCompanyWideAuthRole(newAuthCd);

        if (!wasCompanyWide && nowCompanyWide) {
            user01Mapper.insertAllSiteAuthForUser(model.cmpnyCd(), model.userCd(), model.gvUserCd());
            log.info("역할 변경(전사 접근 진입) - 전 사업장 권한 자동부여. userCd={}, prevAuthCd={}, newAuthCd={}",
                    model.userCd(), prevAuthCd, newAuthCd);
        } else if (wasCompanyWide && !nowCompanyWide) {
            // 회수 시 소속 사업장 = mergeUserInfo 가 방금 반영한 SITE_CD(=model.siteCd()).
            user01Mapper.deleteSiteAuthExceptHome(model.cmpnyCd(), model.userCd(), model.siteCd(), model.gvUserCd());
            log.info("역할 변경(전사 접근 이탈) - 자동부여 사업장권한 회수(소속 1건 잔존). userCd={}, prevAuthCd={}, newAuthCd={}, homeSiteCd={}",
                    model.userCd(), prevAuthCd, newAuthCd, model.siteCd());
        }

        // F1: 비활성(useYn Y→N) 전환 시, 대상자가 신청자/결재자인 진행중 대기요청을 양방향 자동 반려한다.
        //   잔존 대기요청이 월 마감을 영구 차단하는 교착(§13.3 마감 차단 조건) 제거.
        //   직전 상태가 활성일 때만 호출(이미 비활성 재저장 시 불필요 호출 방지 — 종결 자체는 멱등).
        //   동일 트랜잭션(REQUIRES_NEW 내부, 컴포넌트는 REQUIRED 합류) — 반려 실패 시 비활성 전체 롤백.
        if ("N".equals(model.useYn()) && !"N".equals(userInfoResult.useYn())) {
            userPendingRequestTerminationService.terminateAllPendingFor(
                    model.cmpnyCd(), model.userCd(), REASON_DEACTIVATED, model.gvUserCd());
            log.info("사용자 비활성 전환 - 진행중 대기요청 자동 반려 완료. cmpnyCd={}, userCd={}, actor={}",
                    model.cmpnyCd(), model.userCd(), model.gvUserCd());
        }
    }
	
	public SiteNodeAdminCandidateListResponse selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListParam param) {
		
		SiteNodeAdminCandidateListResponse siteNodeAdminCandidateListResponse = null;
		
		List<UserInfoResult> userInfoList = user01Mapper.selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListQuery.from(param));
			
		if(userInfoList != null && userInfoList.size() > 0) {
			siteNodeAdminCandidateListResponse = SiteNodeAdminCandidateListResponse.builder()
																					.userInfoList(userInfoList)
																					.build();
		}
		
		return siteNodeAdminCandidateListResponse;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void withdrawMyAccount(WithdrawMyAccountParam param) {

		int userNodeAdminCheck = user01Mapper.selectUserNodeAdminCheck(UserNodeAdminCheckQuery.from(param));

		if(userNodeAdminCheck > 0) {
			throw new ApiException(UserErrorCode.USER_400_005);
		}

		int updated = user01Mapper.withdrawMyAccount(WithdrawMyAccountCommand.from(param));
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// F1: 본인 탈퇴(USE_YN='N'/ACCOUNT_STATUS='03' 즉시 전환) 시, 대상자가 신청자/결재자인
		//   진행중 대기요청을 양방향 자동 반려한다(잔존 대기요청의 월 마감 교착 제거, §13.3).
		//   actor = 본인 userCd(자기 탈퇴). 동일 트랜잭션 — 반려 실패 시 탈퇴 전체 롤백(부분 처리 금지).
		userPendingRequestTerminationService.terminateAllPendingFor(
				param.cmpnyCd(), param.userCd(), REASON_WITHDRAWN, param.userCd());
		log.info("본인 탈퇴 - 진행중 대기요청 자동 반려 완료. cmpnyCd={}, userCd={}", param.cmpnyCd(), param.userCd());
	}

	@Override
	public void scheduleWithdrawal(ScheduleWithdrawalParam param) {
		
		int userNodeAdminCheck = user01Mapper.selectUserNodeAdminCheck(UserNodeAdminCheckQuery.from(param));
		
		if(userNodeAdminCheck > 0) {
			throw new ApiException(UserErrorCode.USER_400_005);
		}
		
		if (param.withdrawalDate() == null || param.withdrawalDate().isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}
		
		int updated = user01Mapper.scheduleWithdrawal(ScheduleWithdrawalCommand.from(param));
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
	}
	
	@Override
	public void cancelWithdrawal(WithdrawalCancelParam param) {
		user01Mapper.cancelWithdrawal(WithdrawalCancelCommand.from(param));
	}

	@Override
	public MyProfileResponse selectMyProfile(MyProfileParam param) {

		// 조회 대상 cmpnyCd/userCd는 param(=토큰) 값만 사용. 클라이언트 입력 미사용.
		MyProfileResult result = user01Mapper.selectMyProfile(MyProfileQuery.from(param));

		if (result == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		return MyProfileResponse.from(result);
	}

	// ====================================================================
	// PRAFTA-017-4 근태/연차 정보 (master/hr 전용, 정책서 §8.5.6 ~ §8.5.8)
	// ====================================================================

	@Override
	public LeaveInfoResponse selectLeaveInfo(LeaveInfoParam param) {

		// 권한 가드: master/hr만 조회 가능 (이중 방어, 정책서 §8.5.7)
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.error("근태/연차 정보 조회 권한 부족 - userCd={}, authCd={}", param.userCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		LeaveInfoQuery query = LeaveInfoQuery.from(param);

		UserHireInfoResult hireInfo = user01Mapper.selectUserHireInfo(query);
		if (hireInfo == null) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		List<ServiceCreditResult> creditResults = user01Mapper.selectUserServiceCreditList(query);
		if (creditResults == null) {
			creditResults = List.of();
		}

		List<LeaveInfoResponse.CreditItem> creditItems = new ArrayList<>();
		int totalMonths = 0;
		for (ServiceCreditResult cr : creditResults) {
			creditItems.add(LeaveInfoResponse.CreditItem.from(cr));
			// 경력인정 이원화(2026-08-21): 법적 근속 기준일은 반영 모드(LEAVE_CALC_YN='Y') 개월수만 가산한다.
			// 일수 모드(N)는 산정근속 미가산 약정이므로(정책 P-7) 여기 합계에서 제외 — selectCreditMonths 필터와 동일 기준.
			boolean isReflectMode = cr.leaveCalcYn() == null || LEAVE_CALC_YN_REFLECT.equals(cr.leaveCalcYn());
			if (isReflectMode && cr.creditMonths() != null) {
				totalMonths += cr.creditMonths();
			}
		}

		// 법적 근속 기준일 = 입사일 - 총 인정 개월 (입사일이 있을 때만 계산)
		String hireYmd = (hireInfo.hireDate() == null) ? "" : hireInfo.hireDate().replace("-", "");
		String legalTenureBaseDate = "";
		if (hireYmd.length() == 8 && totalMonths > 0) {
			legalTenureBaseDate = DateTimeUtils.toHyphenDate(DateTimeUtils.minusMonths(hireYmd, totalMonths));
		} else if (hireYmd.length() == 8) {
			legalTenureBaseDate = DateTimeUtils.toHyphenDate(hireYmd);
		}

		return LeaveInfoResponse.builder()
				.hireDate(hireInfo.hireDate() == null ? "" : hireInfo.hireDate())
				.employmentType(hireInfo.employmentType())
				.creditList(creditItems)
				.creditTotalMonths(totalMonths)
				.legalTenureBaseDate(legalTenureBaseDate)
				.build();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserCredit(UserCreditParam param) {

		// 권한 가드 (정책서 §8.5.7)
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.error("경력 인정 저장 권한 부족 - userCd={}, authCd={}", param.userCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		// 입력 검증: 인정 개월은 0 이상, 상세 설명은 500자 이내(REASON_DETAIL varchar(500))
		// + 사유 유형[SYS042] 허용코드 + 경력인정 이원화(2026-08-21) 반영/일수 모드 검증(지시서 §1-1).
		for (UserCreditParam.CreditItem item : param.creditList()) {
			if (item.creditMonths() == null || item.creditMonths() < 0) {
				throw new ApiException(UserErrorCode.USER_400_008);
			}
			if (item.reasonDetail() != null && item.reasonDetail().length() > REASON_DETAIL_MAX_LEN) {
				throw new ApiException(UserErrorCode.USER_400_009);
			}
			if (!isBlank(item.reasonType()) && !ALLOWED_REASON_TYPES.contains(item.reasonType())) {
				throw new ApiException(UserErrorCode.USER_400_048);
			}
			String normalizedLeaveCalcYn = normalizeLeaveCalcYn(item.leaveCalcYn());
			resolveExtraLeaveDays(normalizedLeaveCalcYn, item.extraLeaveDays());
		}

		// 대상 userCd가 자사(gvCmpnyCd)에 실제 존재하는 사용자인지 검증 (고아 경력 인정 레코드 방지)
		// updateUserHireDate의 자사 사용자 존재 확인 패턴과 동일하게 적용한다.
		int userExistCount = user01Mapper.selectUserExistCount(param.gvCmpnyCd(), param.userCd());
		if (userExistCount == 0) {
			log.error("경력 인정 저장 대상 사용자 미존재 - userCd={}", param.userCd());
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		// delete-and-insert: 기존 USE_YN='Y' 전량 소프트 삭제 후 신규 INSERT
		user01Mapper.deleteUserServiceCredit(UserCreditDeleteCommand.from(param));

		boolean hasDaysMode = false;
		for (UserCreditParam.CreditItem item : param.creditList()) {
			String reasonType = isBlank(item.reasonType()) ? "OTHER" : item.reasonType();
			String normalizedLeaveCalcYn = normalizeLeaveCalcYn(item.leaveCalcYn());
			java.math.BigDecimal normalizedExtraLeaveDays =
					resolveExtraLeaveDays(normalizedLeaveCalcYn, item.extraLeaveDays());
			if (LEAVE_CALC_YN_DAYS.equals(normalizedLeaveCalcYn)) {
				hasDaysMode = true;
			}
			user01Mapper.insertUserServiceCredit(
					UserCreditInsertCommand.of(
							param.gvCmpnyCd()
							, param.userCd()
							, item.creditMonths()
							, reasonType
							, item.reasonDetail()
							, normalizedLeaveCalcYn
							, normalizedExtraLeaveDays
							, param.gvCmpnyCd()
							, param.gvUserCd()
					)
			);
		}

		// 경력인정 이원화(2026-08-21, 지시서 §1-4 P-8): 일수 모드 항목이 하나라도 있으면 당해 회차분 즉시 부여.
		// delete-and-insert 이후 전체 활성 행 기준으로 재계산되므로(엔진이 SUM 재조회) 항목별이 아닌 1회 호출로 충분.
		if (hasDaysMode) {
			leaveGrantEngineService.grantManualCareerImmediate(param.gvCmpnyCd(), param.userCd(), param.gvUserCd());
		}

		log.info("경력 인정 저장 완료 - userCd={}, 항목수={}", param.userCd(), param.creditList().size());
	}

	@Override
	public HireDateImpactResponse analyzeHireDateImpact(HireDateImpactParam param) {

		// 권한 가드 (정책서 §8.5.7)
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.error("입사일 영향 분석 권한 부족 - userCd={}, authCd={}", param.userCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		String prevHireYmd = user01Mapper.selectUserHireDate(param.gvCmpnyCd(), param.userCd());
		String newHireYmd = param.newHireDate();

		return buildApproxImpact(param.gvCmpnyCd(), param.userCd(), prevHireYmd, newHireYmd);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserHireDate(UserHireDateParam param) {

		// 권한 가드 (정책서 §8.5.7)
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.error("입사일 변경 권한 부족 - userCd={}, authCd={}", param.userCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		// 변경 사유 필수 (CHANGE_REASON NOT NULL)
		if (param.changeReason() == null || param.changeReason().isBlank()) {
			throw new ApiException(UserErrorCode.USER_400_006);
		}

		// 변경할 입사일 형식 검증 (YYYYMMDD)
		if (DateTimeUtils.parseYyyymmdd(param.newHireDate()) == null) {
			throw new ApiException(UserErrorCode.USER_400_007);
		}

		// prafta-032 D1: 처리방식(handlingType) 검증 폐기. 연차 조정은 목표 법정 부여량(targetStatutoryGrantDays)
		//   과의 차액으로 추가/회수한다(D4/D5). 목표 미입력(null)이면 조정 없음.

		// 변경 전 입사일 조회 (이력 PREV_HIRE_DATE 기록 - NOT NULL)
		String prevHireYmd = user01Mapper.selectUserHireDate(param.gvCmpnyCd(), param.userCd());
		if (prevHireYmd == null) {
			// 기존 입사일 미설정 케이스: 변경 후 입사일을 PREV로도 기록하여 NOT NULL 충족(노무 감사 추적 일관성)
			prevHireYmd = param.newHireDate();
		}

		// prafta-032 D8: 입사일 UPDATE + 연차 조정(추가/회수) + 이력 INSERT를 단일 트랜잭션으로(@Transactional rollbackFor=Exception).
		//   1) HIRE_DATE UPDATE
		int updated = user01Mapper.updateUserHireDate(UserHireDateUpdateCommand.from(param));
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}

		//   2) 이력 HIST_ID 미리 채번 (연차 조정 멱등키 _HD{histId} 및 추적에 사용)
		String histId = user01Mapper.selectNextHireHistId(param.gvCmpnyCd());

		//   3) 수동 연차 조정 위임 (차액>0 추가 / 차액<0 회수 / 0 무처리). 회수가능 초과·회수사유 누락 시 예외 → 전체 롤백.
		//      엔진 메서드는 REQUIRED 전파라 본 트랜잭션에 합류한다.
		HireDateAdjustResultVO adjust = leaveGrantEngineService.adjustStatutoryGrantsByHireDateChange(
				param.gvCmpnyCd(), param.userCd(), param.newHireDate(),
				param.targetStatutoryGrantDays(), param.withdrawReason(), histId, param.gvUserCd());

		//   4) 영향 스냅샷 JSON: 조정 결과 스냅샷이 있으면 그것을, 없으면 기존 근사 영향분석 스냅샷을 저장.
		String snapshotJson = (adjust.getAffectedSnapshotJson() != null)
				? adjust.getAffectedSnapshotJson()
				: serializeImpactSnapshot(
						buildApproxImpact(param.gvCmpnyCd(), param.userCd(), prevHireYmd, param.newHireDate()));

		//   5) 이력 INSERT (HANDLING_TYPE='MANUAL' + 전/후 법정 총량 + 회수 사유). 회수 발생 시에만 회수 사유 기록.
		String withdrawReasonForHist = (adjust.getDiff() != null && adjust.getDiff().signum() < 0)
				? param.withdrawReason() : null;
		user01Mapper.insertUserHireDateHistory(
				UserHireDateHistoryCommand.of(param, histId, prevHireYmd, snapshotJson,
						adjust.getOldGrantTotal(), adjust.getNewGrantTotal(), withdrawReasonForHist)
		);

		log.info("입사일 변경 완료 - userCd={}, prev={}, new={}, 차액={}, 추가={}일, 회수={}일",
				param.userCd(), prevHireYmd, param.newHireDate(),
				adjust.getDiff() == null ? "0" : adjust.getDiff().stripTrailingZeros().toPlainString(),
				adjust.getAddedDays() == null ? "0" : adjust.getAddedDays().stripTrailingZeros().toPlainString(),
				adjust.getWithdrawnDays() == null ? "0" : adjust.getWithdrawnDays().stripTrailingZeros().toPlainString());
	}

	@Override
	public HireDateHistoryResponse selectHireDateHistory(HireDateHistoryParam param) {

		// 권한 가드: master/hr만 조회 가능 (입사일 변경/조회와 동일 기준, 정책서 §8.5.7)
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.error("입사일 변경 이력 조회 권한 부족 - userCd={}, authCd={}", param.userCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		List<UserHireDateHistoryResult> list =
				user01Mapper.selectUserHireDateHistory(param.gvCmpnyCd(), param.userCd());

		return HireDateHistoryResponse.builder()
				.list(list == null ? List.of() : list)
				.build();
	}

	// ====================================================================
	// PRAFTA-036 — 관리자 단건 사용자 생성 (단건 + 엑셀 행 단위 공통 진입점)
	// 정책서 공통 §3.1·§3.5·§8·§11.1, plan §1 D1~D7.
	// ====================================================================

	// 고용형태 [SYS041] 허용 코드 — REGULAR 단독.
	// §7-보충 B-1(2026-08-22): CONTRACT/EXECUTIVE 는 폐지된 개념이라 허용 목록에서 제거
	// (PRAFTA_COM_003-B 사용자 원문 "계약직, 임원 값 필요없음" · F-13 엑셀 고용형태 컬럼 제거 ·
	//  08-13 셀프가입 REGULAR 고정 확정 — 고용형태 축은 REGULAR 고정, 일용직(DAILY)만 별도 축).
	// 일용직(DAILY)은 QR/일용직 가입 별도 경로(dailyjoin) 전용 — 관리자 생성(단건/엑셀)에서는 거부한다(F-13 확장).
	// 이 상수는 신규 생성(insertUserOne) 검증 전용이며 기존 DAILY 계정의 수정 경로에는 관여하지 않는다.
	private static final java.util.Set<String> ALLOWED_EMPLOYMENT_TYPES =
			java.util.Set.of("REGULAR");

	// 경력 인정 사유 유형 [SYS042] 허용 코드.
	private static final java.util.Set<String> ALLOWED_REASON_TYPES = java.util.Set.of(
			"CONTRACT_TO_REGULAR", "EXPERIENCE_DIFF", "EXPERIENCE_SAME",
			"GROUP_MOVE", "MA_TRANSFER", "OTHER");

	// 휴대폰 정규화 후 허용 자리수(10~11). 정책 §3.2 휴대폰 본인인증 대상.
	private static final int PHONE_MIN_DIGITS = 10;
	private static final int PHONE_MAX_DIGITS = 11;

	// ===== 소정-03 : 계정 생성 시 소정근로시간 필수 입력 =====
	// 지시서 §0단계 "계정별 필수 입력(08-11 확정)" — UX 는 "풀타임 / 단시간(직접 입력)" 선택식이다.

	/** 풀타임 — 주 소정근로분을 회사 통상 기준값으로 서버가 채운다(기준값 하드코딩 금지, 지시서 B-1). */
	private static final String STD_WORK_TYPE_FULL = "FULL";

	/** 직접 입력 — 단건 폼의 "단시간" 선택과 엑셀 업로드가 쓴다(주 소정근로 분 필수). */
	private static final String STD_WORK_TYPE_DIRECT = "DIRECT";

	/** 소정근로 이력 적용 시작일 포맷(YYYYMMDD) — 입사일 미입력 시 가입일 폴백에 쓴다. */
	private static final DateTimeFormatter YMD_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void insertUserOne(UserCreateParam param) {

		// 1) 권한 가드 — master / hr 만 신규 사용자 생성 가능 (정책서 §8.5.7 패턴 차용).
		if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
			log.warn("신규 계정 생성 권한 부족 - 요청자={}, authCd={}", param.gvUserCd(), param.gvAuthCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		// 2) 필수값 검증 (사용자ID/이름/권한/사업장번호/부서/휴대폰/생년월일).
		if (isBlank(param.userId()) || isBlank(param.userNm())
				|| isBlank(param.authCd()) || isBlank(param.siteNo())
				|| isBlank(param.nodeCd()) || isBlank(param.mblNo())
				|| isBlank(param.birthDt())) {
			throw new ApiException(UserErrorCode.USER_400_040);
		}

		// 3) 고용형태 코드 검증 (선택 입력이지만 입력 시 SYS041 허용 코드).
		if (!isBlank(param.employmentType())
				&& !ALLOWED_EMPLOYMENT_TYPES.contains(param.employmentType())) {
			throw new ApiException(UserErrorCode.USER_400_047);
		}

		// 3-2) 소정-03: 소정근로시간 입력 검증(필수). 계정을 만들기 전에 먼저 막는다.
		//   FULL   : 추가 입력 없음(회사 통상 기준값을 서버가 채움).
		//   DIRECT : 주 소정근로 분 필수. 사유코드는 선택(미입력 시 통상 기준값 비교로 자동 판정).
		//   값 범위·사유코드 존재·일용직 차단 등 나머지 규칙은 StdWorkHoursService 가 단일 출처로 검증한다.
		String stdWorkType = isBlank(param.stdWorkType()) ? null : param.stdWorkType().trim();
		if (stdWorkType == null
				|| (!STD_WORK_TYPE_FULL.equals(stdWorkType) && !STD_WORK_TYPE_DIRECT.equals(stdWorkType))) {
			throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
		}
		if (STD_WORK_TYPE_DIRECT.equals(stdWorkType)
				&& (param.stdWorkWeekMinutes() == null || param.stdWorkWeekMinutes() <= 0)) {
			throw new ApiException(StdWorkErrorCode.STDWORK_400_004);
		}
		// 사유코드는 화면에 내려준 목록(selectableStdWorkReasonCds)으로 서버가 다시 검증한다.
		//   SYS083 존재 검증만으로는 NORMAL(통상)이 통과해 "주 20시간인데 사유는 통상"처럼
		//   단시간 판정을 왜곡하는 값이 저장될 수 있다(클라 바디 신뢰 금지).
		if (STD_WORK_TYPE_DIRECT.equals(stdWorkType) && !isBlank(param.stdWorkReasonCd())
				&& !selectableStdWorkReasonCds().contains(param.stdWorkReasonCd().trim())) {
			log.warn("신규 계정 소정근로 사유코드 거부(선택 불가 코드) - 요청자={}", param.gvUserCd());
			throw new ApiException(StdWorkErrorCode.STDWORK_400_005);
		}

		// 4) 경력 인정 사유 유형 검증 (입력 시만).
		if (!isBlank(param.creditReasonType())
				&& !ALLOWED_REASON_TYPES.contains(param.creditReasonType())) {
			throw new ApiException(UserErrorCode.USER_400_048);
		}

		// 5) 경력 인정 개월수 검증 (음수 금지).
		if (param.creditMonths() != null && param.creditMonths() < 0) {
			throw new ApiException(UserErrorCode.USER_400_008);
		}
		if (param.creditReasonDetail() != null
				&& param.creditReasonDetail().length() > REASON_DETAIL_MAX_LEN) {
			throw new ApiException(UserErrorCode.USER_400_009);
		}

		// 5-2) 경력인정 이원화(2026-08-21, 지시서 §1-1): 연차 반영 모드 검증.
		//   미전송(null/blank)은 'Y'(반영 모드)로 정규화(하위호환, R-3). 'N'(일수 모드)이면
		//   EXTRA_LEAVE_DAYS 필수(0.5 단위, 0 초과 25 이하) — resolveExtraLeaveDays 가 검증하고,
		//   'Y'이면 클라 입력값을 신뢰하지 않고 NULL 강제한다.
		String creditLeaveCalcYn = normalizeLeaveCalcYn(param.creditLeaveCalcYn());
		java.math.BigDecimal creditExtraLeaveDays =
				resolveExtraLeaveDays(creditLeaveCalcYn, param.creditExtraLeaveDays());

		// 6) 권한코드 존재 + 권한레벨 이중 검증 (요청자 권한레벨 이상만 부여 가능 — sortIdx 가 크거나 같아야 함).
		String targetAuthLevelStr = user01Mapper.selectAuthLevelByAuthCd(param.gvCmpnyCd(), param.authCd());
		if (isBlank(targetAuthLevelStr)) {
			throw new ApiException(UserErrorCode.USER_400_045);
		}
		int targetAuthLevel = parseIntOrThrow(targetAuthLevelStr, UserErrorCode.USER_400_045);
		int gvAuthLevel = parseIntOrThrow(param.gvAuthLevel(), CommonErrorCode.COMMON_400_003);
		// PRAFTA-WEB_002-T1-03(1.4-2): 권한 등급 escalation 가드 강화 — 본인(요청자)보다 "엄격히 낮은"
		//   등급(=AUTH_LEVEL 숫자가 더 큰) 권한만 부여 가능. 동일 등급 부여(예: master→master)도 차단한다.
		//   기존 '<'(동일 등급 허용)를 '<='(동일 등급도 차단)로 강화. 사유 코드는 배치 표시 일관성 위해 046(권한레벨초과) 유지.
		if (targetAuthLevel <= gvAuthLevel) {
			log.warn("신규 계정 권한레벨 초과 - 요청자={}, gvAuthLevel={}, targetAuthCd={}, targetAuthLevel={}",
					param.gvUserCd(), gvAuthLevel, param.authCd(), targetAuthLevel);
			throw new ApiException(UserErrorCode.USER_400_046);
		}

		// 7) 사업장번호 -> 사업장코드 매핑.
		String siteCd = user01Mapper.selectSiteCdBySiteNo(param.gvCmpnyCd(), param.siteNo());
		if (isBlank(siteCd)) {
			throw new ApiException(UserErrorCode.USER_400_043);
		}

		// 8) 부서코드 존재 검증.
		int nodeExists = user01Mapper.selectSiteNodeExists(param.gvCmpnyCd(), siteCd, param.nodeCd());
		if (nodeExists == 0) {
			throw new ApiException(UserErrorCode.USER_400_044);
		}

		// 8-2) PRAFTA-COM-008-E-5: 기본 근무타입 화이트리스트 검증(입력 시만, 클라 신뢰 금지).
		//   대상 사업장(siteCd) 활성(USE_YN='Y') 근무타입이 아니면 거부.
		String defaultSchCd = isBlank(param.defaultSchCd()) ? null : param.defaultSchCd().trim();
		if (defaultSchCd != null
				&& !defaultSchOptionService.isValidDefaultSch(param.gvCmpnyCd(), siteCd, defaultSchCd)) {
			throw new ApiException(com.prafta.common.error.attd.AttdErrorCode.ATTD_400_140);
		}

		// 8-1) PRAFTA-WEB_002-T1-02(1.4-1): 무담당 부서 배정 허용.
		//   기존 PRAFTA-046 의 "정/부 관리자 미지정 노드 생성 차단(USER_400_056)" 하드 게이트를 제거한다.
		//   대신 INSERT 직후(아래) 정/부 담당이 모두 비어있는 부서면 신규 사용자를 담당(정)으로 자동지정한다
		//   (소속이동 발효 경로와 동일 공용 로직 — 관리·승인 주체 부재 방지). 일용직 생성 경로(QR/dailyjoin)는 무관.

		// 9) USER_ID 중복 사전 진단 (UX_TB_USER_ID UNIQUE).
		int userIdExists = user01Mapper.selectUserIdExists(param.gvCmpnyCd(), param.userId());
		if (userIdExists > 0) {
			throw new ApiException(UserErrorCode.USER_400_041);
		}

		// 10) 휴대폰 정규화 + 자리수 검증.
		String phoneNorm = Normalizers.normalizePhone(param.mblNo());
		if (phoneNorm == null
				|| phoneNorm.length() < PHONE_MIN_DIGITS
				|| phoneNorm.length() > PHONE_MAX_DIGITS) {
			throw new ApiException(UserErrorCode.USER_400_049);
		}
		// 10-1) 형식 검증 — 자릿수만으로는 부족하다(2026-08-15 통합테스트).
		//   엑셀 서식 유실로 앞자리 0 이 빠진 "1077635257"(10자리)이 하한을 통과해 그대로 저장됐다.
		//   이 컬럼은 로그인 계정·초기 비밀번호·SMS 인증에 쓰이므로 이동전화만 허용한다
		//   (유선·인터넷전화는 복원은 되지만 용도상 부적합 — 사유를 구분해 안내한다).
		//   ★ Normalizers.normalizePhone 은 비숫자를 조용히 제거하므로 이 검증이 없으면
		//     "김철수010..." 같은 값도 멀쩡한 번호로 둔갑한다.
		if (!UserExcelValueRestorer.isKrMobile(phoneNorm)) {
			throw new ApiException(UserErrorCode.USER_400_079);
		}

		// 11) 휴대폰 HMAC 중복 사전 진단 (UX_TB_USER_MBL_NO UNIQUE).
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
		int mblHmacExists = user01Mapper.selectUserMblHmacExists(param.gvCmpnyCd(), phoneHmac);
		if (mblHmacExists > 0) {
			throw new ApiException(UserErrorCode.USER_400_042);
		}

		// 12) 이메일/생년월일 정규화 + 생년월일 유효성 검증.
		//   ★ normalizeBirth 는 비숫자를 제거만 하므로 "abc" 는 null 이 되어 조용히 미저장됐다
		//     (필수 검증은 원본 param.birthDt() 를 보므로 통과한다). 실제 달력 날짜인지까지 본다.
		String emailNorm = Normalizers.normalizeEmail(param.email());
		String birthNorm = Normalizers.normalizeBirth(param.birthDt());
		if (!UserExcelValueRestorer.isValidBirth(birthNorm)) {
			throw new ApiException(UserErrorCode.USER_400_080);
		}

		// 13) AES-GCM 암호화 (정책 §11.1 PII 평문 저장 금지).
		String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
		String emailEnc = (emailNorm == null) ? null : aesGcmCrypto.encrypt(emailNorm);
		String birthEnc = (birthNorm == null) ? null : aesGcmCrypto.encrypt(birthNorm);

		// 14) HMAC / 파생값.
		String emailHmac = (emailNorm == null) ? null : hmacSigner.hmacSha256Base64Url(emailNorm);
		String phoneLast4 = Normalizers.last4(phoneNorm);
		String emailDomain = Normalizers.emailDomain(emailNorm);

		// 15) 초기 비밀번호 = 휴대폰 정규화값(하이픈 제외) BCrypt 해시 (D3).
		String userPw = passwordHasher.hash(phoneNorm);

		// 16) USER_CD 채번 (LoginMapper.selectUserCd 와 동일 SQL).
		String userCd = user01Mapper.selectNextUserCd(param.gvCmpnyCd());

		// 17) TB_USER INSERT (ACCOUNT_STATUS='04' 인증대기 고정).
		UserCreateInsertCommand command = new UserCreateInsertCommand(
				param.gvCmpnyCd()
				, userCd
				, param.userId()
				, param.userNm()
				, userPw
				, siteCd
				, param.nodeCd()
				, param.authCd()
				, isBlank(param.rankCd()) ? null : param.rankCd()
				, phoneEnc
				, phoneHmac
				, phoneLast4
				, emailEnc
				, emailHmac
				, emailDomain
				, birthEnc
				, isBlank(param.hireDate()) ? null : param.hireDate()
				, isBlank(param.employmentType()) ? null : param.employmentType()
				, isBlank(param.contractEndDate()) ? null : param.contractEndDate()
				, isBlank(param.gender()) ? null : param.gender()
				, defaultSchCd
				, param.gvUserCd()
		);
		user01Mapper.insertOneUser(command);

		// 17-1) PRAFTA-WEB_002-T1-02(1.4-1): 배정 부서의 정(MAIN)·부(SUB) 담당이 모두 비어있으면
		//   신규 사용자를 담당(정)으로 자동지정한다. 소속이동 발효 경로와 동일 공용 SQL 재사용(중복 구현 금지).
		//   이미 담당(정 또는 부)이 있으면 0행(무변경).
		int autoMainAdmin = userTransferMapper.updateNodeMainAdminIfEmpty(
				param.gvCmpnyCd(), siteCd, param.nodeCd(), userCd, param.gvUserCd());
		if (autoMainAdmin > 0) {
			log.info("신규 계정 생성 - 무담당 부서 담당(정) 자동지정. userCd={}, siteCd={}, nodeCd={}", userCd, siteCd, param.nodeCd());
		}

		// 17-2) 소정-03: 소정근로시간 이력 INSERT — 계정 생성과 <b>같은 트랜잭션</b>.
		//   StdWorkHoursService.register 는 @Transactional(REQUIRED) 라 본 메서드의
		//   REQUIRES_NEW 트랜잭션에 참여한다. 즉 이력 등록이 실패하면 계정 생성 전체가 롤백되어
		//   "소정근로 미기록 계정"이 조용히 만들어지지 않는다(지시서 요구).
		insertStdWorkHoursOnCreate(param, userCd, siteCd, stdWorkType);

		// 18) TB_USER_SITE_AUTH INSERT (사용자 ↔ 사이트 권한 한 줄).
		user01Mapper.insertOneUserSiteAuth(param.gvCmpnyCd(), userCd, siteCd, param.gvUserCd());

		// 18.1) PRAFTA-037-F7 - 추가 사이트 권한 부여 (선택).
		// - 기본 siteCd 와 중복은 제거(이미 18단계에서 INSERT 됨).
		// - 동일 목록 안 중복도 제거(LinkedHashSet 으로 입력 순서 보존).
		// - 각 추가 siteCd 가 회사 내 존재(사용중)하는지 검증, 없으면 USER_400_055.
		if (param.additionalSiteCdList() != null && !param.additionalSiteCdList().isEmpty()) {
			java.util.LinkedHashSet<String> dedup = new java.util.LinkedHashSet<>();
			for (String extra : param.additionalSiteCdList()) {
				if (isBlank(extra)) continue;
				String trimmed = extra.trim();
				if (trimmed.equals(siteCd)) continue;   // 기본 사이트 중복 제거
				dedup.add(trimmed);
			}
			for (String extraSiteCd : dedup) {
				int siteExists = user01Mapper.selectSiteExistsBySiteCd(param.gvCmpnyCd(), extraSiteCd);
				if (siteExists == 0) {
					throw new ApiException(UserErrorCode.USER_400_055);
				}
				user01Mapper.insertOneUserSiteAuth(param.gvCmpnyCd(), userCd, extraSiteCd, param.gvUserCd());
			}
		}

		// 18.2) PRAFTA-042-5 (D3-②): 신규 권한이 전사 접근 역할(master/hr/safe)이면 전 사업장 권한 멱등 부여.
		//   18단계 소속 site 1건 INSERT 및 18.1 추가 site 와는 ON DUP UPDATE 로 충돌 없음.
		if (isCompanyWideAuthRole(param.authCd())) {
			user01Mapper.insertAllSiteAuthForUser(param.gvCmpnyCd(), userCd, param.gvUserCd());
			log.info("전사 접근 역할 신규 생성 - 전 사업장 권한 자동부여 완료. userCd={}, authCd={}", userCd, param.authCd());
		}

		// 19) 경력 인정 1건(개월 > 0 일 때만) INSERT. 사유 유형 미지정 시 'OTHER'.
		//   경력인정 이원화(2026-08-21): leaveCalcYn/extraLeaveDays 는 상단(5-2)에서 이미 정규화·검증된 값.
		if (param.creditMonths() != null && param.creditMonths() > 0) {
			String reasonType = isBlank(param.creditReasonType()) ? "OTHER" : param.creditReasonType();
			user01Mapper.insertUserServiceCredit(new UserCreditInsertCommand(
					param.gvCmpnyCd()
					, userCd
					, param.creditMonths()
					, reasonType
					, param.creditReasonDetail()
					, creditLeaveCalcYn
					, creditExtraLeaveDays
					, param.gvCmpnyCd()
					, param.gvUserCd()
			));

			// 19-2) 경력인정 이원화(2026-08-21, 지시서 §1-4 P-8): 일수 모드 등록 즉시 당해 회차분 부여.
			//   실패(시스템 예외)는 계정 생성 트랜잭션과 함께 롤백(REQUIRES_NEW 이므로 이 메서드 단위로만 롤백,
			//   다른 회사/사용자 영향 없음). 소정-05 OFF 등 정책상 스킵은 engine 내부에서 throw 없이 처리(R-5).
			if (LEAVE_CALC_YN_DAYS.equals(creditLeaveCalcYn)) {
				leaveGrantEngineService.grantManualCareerImmediate(param.gvCmpnyCd(), userCd, param.gvUserCd());
			}
		}

		// 20) PRAFTA-COM-008-E-5: 기본 근무타입 설정 시 즉시 자동생성(E-3 트리거2) — 교대 비소속·운영 게이트 on.
		//   실패는 격리(생성 실패가 사용자 생성 트랜잭션을 롤백하지 않게). 신규 사용자는 보통 인증대기('04')라
		//   당장은 비대상일 수 있으나, applyDefaultSchChange 가 멱등이라 무해하다.
		if (defaultSchCd != null && defaultSchGenEnabled) {
			try {
				defaultSchGenService.applyDefaultSchChange(param.gvCmpnyCd(), siteCd, userCd, defaultSchCd);
			} catch (Exception e) {
				log.error("신규 계정 기본근무 자동생성 실패(계정 생성 영향 없음) - userCd={}, defaultSchCd={}",
						userCd, defaultSchCd, e);
			}
		}

		log.info("신규 계정 생성 완료 - 요청자={}, 신규userCd={}, userId={}, authCd={}, siteCd={}, nodeCd={}, defaultSchCd={}, accountStatus=04",
				param.gvUserCd(), userCd, param.userId(), param.authCd(), siteCd, param.nodeCd(), defaultSchCd);
	}

	/**
	 * 소정-03: 신규 계정의 소정근로시간 이력 1행을 등록한다 (계정 생성 트랜잭션 내부).
	 *
	 * <p><b>적용 시작일</b> = 입사일(HIRE_DATE). 미입력이거나 형식이 올바르지 않으면
	 * <b>가입일(계정 생성일)</b>로 폴백한다(plan §4 소정-03). 적용 종료일은 두지 않는다 —
	 * 계약 변경은 관리 화면에서 새 이력 행으로 쌓이며, 이때 직전 열린 행이 자동 마감된다.
	 *
	 * <p><b>주 소정근로 분</b>
	 * <ul>
	 *   <li>풀타임(FULL) — <b>배정 사업장</b>의 통상 기준값(사업장 오버라이드 → 회사 기본값 →
	 *       2400분). 화면이 보낸 값을 쓰지 않는다(기준값 하드코딩·클라 신뢰 금지).</li>
	 *   <li>직접 입력(DIRECT) — 화면/엑셀 입력값.</li>
	 * </ul>
	 *
	 * <p><b>사유코드</b> — 풀타임은 NORMAL 고정. 직접 입력은 화면 선택값을 쓰고, 사유가 없으면
	 * (엑셀 업로드는 사유 컬럼이 없다) 통상 기준값과 비교해 판정한다: 본인 주 소정이 통상
	 * 기준보다 짧으면 PART_TIME(단시간계약), 아니면 NORMAL(지시서 B-2 단시간 파생 판정).
	 * ★비교 분모가 <b>배정 사업장 기준값</b>이어야 통상 40시간이 아닌 사업장의 통상근로자가
	 * 단시간으로 오분류되지 않는다.
	 *
	 * <p><b>일용직 제외</b> — 소정근로 개념이 없는 계정(DAILY)은 이력을 만들지 않는다.
	 *
	 * @param siteCd 검증을 마친 배정 사업장(클라 바디가 아니라 서비스가 확정한 값)
	 */
	private void insertStdWorkHoursOnCreate(UserCreateParam param, String userCd, String siteCd, String stdWorkType) {

		// 관리자 생성 경로는 ALLOWED_EMPLOYMENT_TYPES 로 이미 DAILY 를 거부하지만,
		// 고용형태 판정의 단일 출처(StdWorkHoursService)로 한 번 더 확인한다(fail-closed).
		if (!stdWorkHoursService.isEligible(param.gvCmpnyCd(), userCd)) {
			log.info("신규 계정 소정근로시간 이력 생략(관리 대상 아님) - userCd={}", userCd);
			return;
		}

		int cmpnyWeekStdMinutes = stdWorkHoursService.resolveSiteWeekStdMinutes(param.gvCmpnyCd(), siteCd);
		int weekStdMinutes = STD_WORK_TYPE_FULL.equals(stdWorkType)
				? cmpnyWeekStdMinutes
				: param.stdWorkWeekMinutes();

		String reasonCd;
		if (STD_WORK_TYPE_FULL.equals(stdWorkType)) {
			reasonCd = StdWorkReasonCd.NORMAL;
		} else if (!isBlank(param.stdWorkReasonCd())) {
			reasonCd = param.stdWorkReasonCd().trim();
		} else {
			reasonCd = (weekStdMinutes < cmpnyWeekStdMinutes)
					? StdWorkReasonCd.PART_TIME
					: StdWorkReasonCd.NORMAL;
		}

		String hireYmd = isBlank(param.hireDate()) ? null : param.hireDate().trim();
		String applyStrDate = (DateTimeUtils.parseYyyymmdd(hireYmd) != null)
				? hireYmd
				: LocalDate.now().format(YMD_BASIC);

		StdWorkHoursSaveResult stdWorkResult = stdWorkHoursService.register(StdWorkHoursSaveCommand.builder()
				.cmpnyCd(param.gvCmpnyCd())
				.userCd(userCd)
				.applyStrDate(applyStrDate)
				.applyEndDate(null)
				.weekStdMinutes(weekStdMinutes)
				.reasonCd(reasonCd)
				.reasonDetail(null)
				.actorNo(param.gvUserCd())
				.build());

		// ★사유코드는 로그에 남기지 않는다(security M-3): userCd + 사유(임신기·육아기·가족돌봄)
		//   조합은 건강·가족관계 정보에 해당한다(정책 §11.1). 감사 추적은 이력 테이블의
		//   INSERT_NO/INSERT_DATE 로 충분하다.
		log.info("신규 계정 소정근로시간 이력 등록 - userCd={}, 적용시작일={}(입사일폴백여부={}), 주소정={}분, 경고={}건",
				userCd, applyStrDate, hireYmd == null || DateTimeUtils.parseYyyymmdd(hireYmd) == null,
				weekStdMinutes,
				stdWorkResult.getWarnings() == null ? 0 : stdWorkResult.getWarnings().size());
	}

	/**
	 * 소정-03/08: 계정 생성 경로에서 <b>선택 가능한</b> 소정근로 사유코드 규칙 목록 (allow-list 단일 출처).
	 *
	 * <p>화면 셀렉트({@link #getStdWorkOptions})와 저장 검증({@code insertUserOne} 3-2 단계)이
	 * <b>같은 메서드</b>를 쓴다 — 목록에서만 빼고 서버가 강제하지 않으면 클라이언트가 임의 코드를
	 * 보내 데이터가 오염된다(security L-2). 코드 나열 복제 금지.
	 *
	 * <p>제외 대상
	 * <ul>
	 *   <li>{@code NORMAL} — 풀타임 라디오가 담당한다. 이 값이 단시간 입력과 함께 저장되면
	 *       "주 20시간인데 통상근로자"가 되어 2단계 단시간 판정·비례부여 분모가 틀어진다.</li>
	 *   <li>단축 사유 — 적용 종료일이 필수인데 생성 폼에는 기간 입력이 없다. 판정은
	 *       {@code StdWorkReasonCd.isReduced}(단축 여부 단일 출처).</li>
	 * </ul>
	 */
	private List<com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO> selectableStdWorkReasonRules() {

		List<com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO> rules = new ArrayList<>();
		for (com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO rule : stdWorkHoursService.findReasonRules()) {
			if (rule == null || rule.getReasonCd() == null) {
				continue;
			}
			if (StdWorkReasonCd.NORMAL.equals(rule.getReasonCd()) || StdWorkReasonCd.isReduced(rule.getReasonCd())) {
				continue;
			}
			rules.add(rule);
		}
		return rules;
	}

	/** {@link #selectableStdWorkReasonRules} 의 코드 집합 (저장 검증용 allow-list). */
	private java.util.Set<String> selectableStdWorkReasonCds() {

		java.util.Set<String> codes = new java.util.HashSet<>();
		for (com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO rule : selectableStdWorkReasonRules()) {
			codes.add(rule.getReasonCd());
		}
		return codes;
	}

	/**
	 * 소정-03/08: 계정 생성 폼의 소정근로 입력 옵션(회사 통상 기준값 + 사유 셀렉트).
	 *
	 * <p>사유 목록은 {@link #selectableStdWorkReasonRules} 단일 출처를 쓴다(저장 검증과 동일 집합).
	 */
	@Override
	public com.prafta.web.user.user01.dto.response.StdWorkOptionsResponse getStdWorkOptions(String cmpnyCd, String siteCd) {

		if (isBlank(cmpnyCd)) {
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		}

		List<com.prafta.web.user.user01.dto.response.StdWorkOptionsResponse.ReasonOption> options = new ArrayList<>();
		for (com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO rule : selectableStdWorkReasonRules()) {
			options.add(com.prafta.web.user.user01.dto.response.StdWorkOptionsResponse.ReasonOption.builder()
					.reasonCd(rule.getReasonCd())
					.reasonNm(rule.getReasonNm())
					.build());
		}

		return com.prafta.web.user.user01.dto.response.StdWorkOptionsResponse.builder()
				.cmpnyWeekStdMinutes(stdWorkHoursService.resolveSiteWeekStdMinutes(cmpnyCd, siteCd))
				.reasonOptions(options)
				.build();
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	/**
	 * PRAFTA-042-5: 전 사업장 접근(사업장권한 전사 자동부여 대상) 역할인지 판정한다.
	 * master / hr / safe 가 대상이다. (잠금/노드 스코프와 구분되는 사업장권한 부여용 집합)
	 */
	private static boolean isCompanyWideAuthRole(String authCd) {
		if (authCd == null || authCd.isEmpty()) {
			return false;
		}
		return AuthRoleUtils.AUTH_MASTER.equals(authCd)
				|| AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)
				|| AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd);
	}

	private static int parseIntOrThrow(String s, com.prafta.common.error.ApiErrorCode errorCode) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			throw new ApiException(errorCode);
		}
	}

	/**
	 * PRAFTA-WEB_002-T1-03(1.4-2): 권한 등급(AUTH_LEVEL/SORT_IDX) 문자열을 정수로 파싱한다.
	 * 값이 없거나 숫자가 아니면 null 을 반환한다(escalation 가드는 null 을 fail-closed 로 거부).
	 */
	private static Integer parseAuthLevel(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * PRAFTA-036 — 사용자 일괄 생성 엑셀(.xlsx) 양식 바이트를 생성한다.
	 * 권한 가드만 적용하고 실제 양식 생성은 {@link UserExcelTemplateBuilder} 에 위임한다.
	 */
	@Override
	public byte[] buildUserCreateTemplate(TokenInfo tokenInfo, AuditContext auditContext) {
		if (tokenInfo == null
				|| tokenInfo.gv_authCd() == null
				|| !AuthRoleUtils.isManager(tokenInfo.gv_authCd())) {
			log.warn("양식 다운로드 권한 부족 - 요청자={}",
					tokenInfo == null ? "(no-token)" : tokenInfo.gv_userCd());
			throw new ApiException(UserErrorCode.USER_403_001);
		}

		// PRAFTA-037-F5: 권한 가드 통과 직후 감사 로그 적재.
		// 적재 실패는 AuditLogServiceImpl 내부 try/catch 로 흡수되므로 본 양식 다운로드를 막지 않는다.
		auditLogService.record(
				AuditLogCommand.builder()
						.cmpnyCd(tokenInfo.gv_cmpnyCd())
						.userCd(tokenInfo.gv_userCd())
						.actionType(AuditActionType.DOWNLOAD)
						.resourceType(AuditResourceType.USER_CREATE_TEMPLATE)
						.resourceKey(null)
						.detailJson(null)
						.build(),
				auditContext
		);

		log.info("사용자 생성 양식 다운로드 - 요청자={}", tokenInfo.gv_userCd());

		// PRAFTA-049: 입력 코드 참조 시트②③④⑤ 데이터(회사 스코프) 조회 후 양식에 동봉.
		String cmpnyCd = tokenInfo.gv_cmpnyCd();

		List<String[]> siteRows = new ArrayList<>();
		for (TemplateSiteRow s : user01Mapper.selectTemplateSiteList(cmpnyCd)) {
			// 참조 시트는 입력 대상인 사업장번호만 노출(사업장코드는 내부 변환값이라 혼동 방지 위해 제외).
			siteRows.add(new String[] { s.siteNm(), s.siteNo() });
		}

		List<String[]> nodeRows = new ArrayList<>();
		for (TemplateNodeRow n : user01Mapper.selectTemplateNodeList(cmpnyCd)) {
			nodeRows.add(new String[] { n.siteNm(), n.nodeCd(), n.nodeNm() });
		}

		List<String[]> authRows = new ArrayList<>();
		for (TemplateAuthRow a : user01Mapper.selectTemplateAuthList(cmpnyCd)) {
			authRows.add(new String[] { a.authCd(), a.authNm() });
		}

		List<String[]> rankRows = new ArrayList<>();
		for (TemplateRankRow r : user01Mapper.selectTemplateRankList(cmpnyCd)) {
			rankRows.add(new String[] { r.rankCd(), r.rankNm() });
		}

		return UserExcelTemplateBuilder.build(siteRows, nodeRows, authRows, rankRows);
	}

	// 법정 본연차 기본 일수 (근로기준법 §60① 1년 이상 근로자, AXIS 정밀 반영 아님)
	private static final BigDecimal LEGAL_BASE_ANNUAL_DAYS = BigDecimal.valueOf(15);
	// 법정 월차 최대 일수 (근로기준법 §60② 1년 미만, 매월 만근 시 1일씩 최대 11일)
	private static final int LEGAL_MAX_MONTHLY_DAYS = 11;

	/**
	 * 입사일 변경 영향 분석을 생성한다.
	 *
	 * <p>부여/사용 합계는 {@code tb_user_leave_grant} 활성 법정부여(STATUTORY_*, ACTIVE/EXHAUSTED, DEL_YN='N')
	 * 의 <b>실집계</b>로 산출한다(정책서 §8.5.6 영향 스냅샷). 누락 부여(missingGrant)와 다음 부여 예정일(nextGrant)은
	 * <b>법정 기본 근사</b>이며, 정책(AXIS) 정밀 산정이 아니다 — 정밀 산정은 Attd_09 "정책 기준 부여" 버튼/스케줄러의 몫이다.
	 *
	 * <p>입사일 변경 자체는 grant를 조작하지 않고 기록만 유지한다(정책서 §8.5.8 기부여 보호).
	 */
	private HireDateImpactResponse buildApproxImpact(String cmpnyCd, String userCd, String prevHireYmd, String newHireYmd) {

		String scenarioLabel = "";
		String changeSummaryText = "";

		if (DateTimeUtils.parseYyyymmdd(prevHireYmd) != null
				&& DateTimeUtils.parseYyyymmdd(newHireYmd) != null) {

			int diff = DateTimeUtils.diffDays(newHireYmd, prevHireYmd); // new - prev
			String direction = diff < 0 ? "입사일 과거로" : (diff > 0 ? "입사일 미래로" : "변동 없음");

			// 현재(prev) 입사일 기준 근속 1년 미만/이상 (오늘 기준 근사)
			String todayYmd = String.format("%1$tY%1$tm%1$td", new java.util.Date());
			int tenureDays = DateTimeUtils.diffDays(todayYmd, prevHireYmd);
			String tenureLabel = tenureDays < 365 ? "1년 미만" : "1년 이상";

			scenarioLabel = tenureLabel + " · " + direction;

			if (diff == 0) {
				changeSummaryText = "입사일 변동이 없습니다.";
			} else {
				int absDays = Math.abs(diff);
				String moveLabel = diff < 0 ? "앞당김" : "미룸";
				changeSummaryText = "입사일 " + absDays + "일 " + moveLabel
						+ " · 근속 기준일이 함께 이동합니다.";
			}
		}

		// 활성 법정부여 실집계 (NULL 합계는 매퍼 COALESCE로 0)
		UserStatutoryLeaveSummaryResult summary =
				user01Mapper.selectUserStatutoryLeaveSummary(cmpnyCd, userCd);
		BigDecimal grantedSum = (summary == null || summary.grantedSum() == null)
				? BigDecimal.ZERO : summary.grantedSum();
		BigDecimal usedSum = (summary == null || summary.usedSum() == null)
				? BigDecimal.ZERO : summary.usedSum();

		// 누락 부여(변경 후 기준) = 부여 엔진 기준 유효기간 내 소급 부여 가능 일수(실제 정책 기준 부여로 들어갈 분).
		// prafta-023 F — 엔진 추정(computeBackfillPeriods)과 정합. HIRE_DATE는 입사 anniversary, FISCAL_YEAR는
		// 과거 회계연도(당해 제외) 소급분을 반영한다(#2). 미래 입사일은 과거 백필 비대상이라 0.
		BigDecimal missing = BigDecimal.valueOf(
				leaveGrantEngineService.estimateBackfillDays(cmpnyCd, userCd, newHireYmd));

		// prafta-032 D1: 옵션별 재할당 시뮬(options[]·reclaimNote) 폐기. FISCAL 다음 회계연도 발생예정만 유지(read-only).
		String fiscalNextGrantText = leaveGrantEngineService.fiscalNextGrantText(cmpnyCd);
		if (fiscalNextGrantText == null) {
			fiscalNextGrantText = "";
		}

		return HireDateImpactResponse.builder()
				.scenarioLabel(scenarioLabel)
				.existingGrantText(formatDays(grantedSum))     // 실집계: 활성 법정부여 GRANT_DAYS 합계
				.usedText(formatDays(usedSum))                 // 실집계: 활성 법정부여 USED_DAYS 합계
				.missingGrantText(formatDays(missing))         // 근사: 법정 기본 기대치 − 현재 부여합
				.nextGrantText(buildNextAnniversaryText(newHireYmd)) // 근사: 새 입사일 다음 anniversary
				.changeSummaryText(changeSummaryText)
				.fiscalNextGrantText(fiscalNextGrantText)      // prafta-032 D1: FISCAL 다음 회계연도 발생예정(유지)
				.build();
	}

	/**
	 * 변경 후 입사일 기준 법정 기본 기대 부여 일수(근사).
	 *
	 * <p>⚠️ 정책(AXIS) 정밀 반영이 아니라 <b>법정 기본 근사</b>다(정밀 산정은 "정책 기준 부여" 경로 책임).
	 * <ul>
	 *   <li>근속 12개월 미만: 월차 {@code min(경과개월, 11)}일 (근로기준법 §60②, 매월 만근 시 1일씩 최대 11일)</li>
	 *   <li>근속 12개월 이상: 본연차 15일 + 근속가산 기본(3년차부터 2년 주기 1일씩, 최대 25일)</li>
	 * </ul>
	 * 새 입사일이 미래이거나 파싱 불가하면 0을 반환한다.
	 */
	private BigDecimal expectedLegalBaseDays(String newHireYmd) {
		LocalDate hire = DateTimeUtils.parseYyyymmdd(newHireYmd);
		if (hire == null) {
			return BigDecimal.ZERO;
		}
		LocalDate today = LocalDate.now();
		if (hire.isAfter(today)) {
			return BigDecimal.ZERO;
		}
		long months = java.time.temporal.ChronoUnit.MONTHS.between(hire, today);
		if (months < 0) {
			months = 0;
		}
		if (months < 12) {
			// 1년 미만: 법정 월차만 (최대 11일)
			int monthly = (int) Math.min(months, LEGAL_MAX_MONTHLY_DAYS);
			return BigDecimal.valueOf(monthly);
		}
		// 1년 이상: 본연차 15일 + 근속가산 기본 (3년차부터 2년 주기 +1, 최대 25일)
		long years = months / 12;
		int bonus = 0;
		if (years >= 3) {
			bonus = (int) ((years - 1) / 2); // 3년차=1, 5년차=2, 7년차=3 ...
			if (bonus > 10) {
				bonus = 10; // 15 + 10 = 최대 25일
			}
		}
		return LEGAL_BASE_ANNUAL_DAYS.add(BigDecimal.valueOf(bonus));
	}

	/**
	 * 새 입사일 기준 다음 anniversary("YYYY-MM-DD", 근사).
	 *
	 * <p>대시보드 {@code buildNextGrantDateText}의 입사일 분기와 동일 개념: 새 입사일의 올해 anniversary가
	 * 이미 지났으면 내년으로 이동한다. 회계연도(FISCAL_YEAR) 정밀 산정은 본 작업 범위 외이므로 입사일 기준으로 통일한다.
	 * 파싱 불가 시 빈 문자열을 반환한다.
	 */
	private String buildNextAnniversaryText(String newHireYmd) {
		LocalDate hire = DateTimeUtils.parseYyyymmdd(newHireYmd);
		if (hire == null) {
			return "";
		}
		LocalDate today = LocalDate.now();
		LocalDate next;
		try {
			next = hire.withYear(today.getYear());
		} catch (java.time.DateTimeException e) {
			// 윤년 2/29 등 해당 연도에 없는 날짜는 말일 보정(LocalDate.minusMonths 규칙과 동일 안전 처리)
			next = hire.withYear(today.getYear()).withDayOfMonth(1).plusMonths(1).minusDays(1);
		}
		if (!next.isAfter(today)) {
			next = next.plusYears(1);
		}
		return String.format("%04d-%02d-%02d", next.getYear(), next.getMonthValue(), next.getDayOfMonth());
	}

	/** BigDecimal 일수를 "N일" 문자열로 표기한다(소수 끝자리 0 제거: 15.0 → "15일", 0.5 → "0.5일"). */
	private String formatDays(BigDecimal days) {
		BigDecimal v = (days == null) ? BigDecimal.ZERO : days.stripTrailingZeros();
		if (v.scale() < 0) {
			v = v.setScale(0);
		}
		return v.toPlainString() + "일";
	}

	/**
	 * 영향 분석 결과를 AFFECTED_GRANT_SNAPSHOT용 JSON 문자열로 직렬화한다.
	 */
	private String serializeImpactSnapshot(HireDateImpactResponse impact) {
		if (impact == null) {
			return null;
		}
		Map<String, Object> snap = new LinkedHashMap<>();
		snap.put("scenarioLabel", impact.getScenarioLabel());
		snap.put("existingGrantText", impact.getExistingGrantText());
		snap.put("usedText", impact.getUsedText());
		snap.put("missingGrantText", impact.getMissingGrantText());
		snap.put("nextGrantText", impact.getNextGrantText());
		snap.put("changeSummaryText", impact.getChangeSummaryText());
		// 부여/사용 합계는 실집계지만 missingGrant/nextGrant가 법정 기본 근사이므로 근사 플래그 유지
		snap.put("approximated", Boolean.TRUE);
		try {
			return objectMapper.writeValueAsString(snap);
		} catch (JsonProcessingException e) {
			log.error("입사일 변경 영향 스냅샷 JSON 직렬화 실패", e);
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}
	}
}
