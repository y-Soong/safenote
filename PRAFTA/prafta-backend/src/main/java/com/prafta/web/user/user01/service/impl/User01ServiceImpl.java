package com.prafta.web.user.user01.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.login.LoginErrorCode;
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
import com.prafta.web.user.user01.result.MyProfileResult;
import com.prafta.web.user.user01.result.ServiceCreditResult;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User01ServiceImpl implements User01Service{
	private final User01Mapper user01Mapper;
	private final PasswordHasher passwordHasher;
	private final HmacSigner hmacSigner;
	private final AesGcmCrypto aesGcmCrypto;
	private final ObjectMapper objectMapper;
	// prafta-023 F: 입사일 변경 영향분석의 "누락된 부여"를 부여 엔진 산식(유효기간 내 소급)과 정합
	private final LeaveGrantEngineService leaveGrantEngineService;
	// PRAFTA-037-F5: 감사 로그 적재 (양식 다운로드부터 1차 적용)
	private final AuditLogService auditLogService;

	private static final int PW_MIN_LEN = 6;
	private static final int PW_MAX_LEN = 15;

	// REASON_DETAIL varchar(500) - 경력 인정 상세 설명 서버측 길이 상한
	private static final int REASON_DETAIL_MAX_LEN = 500;

		
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

		String userPw = passwordHasher.hash(param.userCd());

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

        user01Mapper.mergeUserInfo(UserInfoCommand.from(model, phoneEnc, phoneHmac, emailEnc, emailHmac, birthEnc));

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
	public void withdrawMyAccount(WithdrawMyAccountParam param) {
		
		int userNodeAdminCheck = user01Mapper.selectUserNodeAdminCheck(UserNodeAdminCheckQuery.from(param));
		
		if(userNodeAdminCheck > 0) {
			throw new ApiException(UserErrorCode.USER_400_005);
		}
		
		int updated = user01Mapper.withdrawMyAccount(WithdrawMyAccountCommand.from(param));
		if (updated == 0) {
			throw new ApiException(LoginErrorCode.LOGIN_400_002);
		}
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
			if (cr.creditMonths() != null) {
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
		for (UserCreditParam.CreditItem item : param.creditList()) {
			if (item.creditMonths() == null || item.creditMonths() < 0) {
				throw new ApiException(UserErrorCode.USER_400_008);
			}
			if (item.reasonDetail() != null && item.reasonDetail().length() > REASON_DETAIL_MAX_LEN) {
				throw new ApiException(UserErrorCode.USER_400_009);
			}
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

		for (UserCreditParam.CreditItem item : param.creditList()) {
			user01Mapper.insertUserServiceCredit(
					UserCreditInsertCommand.of(
							param.gvCmpnyCd()
							, param.userCd()
							, item.creditMonths()
							, item.reasonDetail()
							, param.gvCmpnyCd()
							, param.gvUserCd()
					)
			);
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

	// 고용형태 [SYS041] 허용 코드 (REGULAR/CONTRACT/DAILY/EXECUTIVE).
	private static final java.util.Set<String> ALLOWED_EMPLOYMENT_TYPES =
			java.util.Set.of("REGULAR", "CONTRACT", "DAILY", "EXECUTIVE");

	// 경력 인정 사유 유형 [SYS042] 허용 코드.
	private static final java.util.Set<String> ALLOWED_REASON_TYPES = java.util.Set.of(
			"CONTRACT_TO_REGULAR", "EXPERIENCE_DIFF", "EXPERIENCE_SAME",
			"GROUP_MOVE", "MA_TRANSFER", "OTHER");

	// 휴대폰 정규화 후 허용 자리수(10~11). 정책 §3.2 휴대폰 본인인증 대상.
	private static final int PHONE_MIN_DIGITS = 10;
	private static final int PHONE_MAX_DIGITS = 11;

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

		// 6) 권한코드 존재 + 권한레벨 이중 검증 (요청자 권한레벨 이상만 부여 가능 — sortIdx 가 크거나 같아야 함).
		String targetAuthLevelStr = user01Mapper.selectAuthLevelByAuthCd(param.gvCmpnyCd(), param.authCd());
		if (isBlank(targetAuthLevelStr)) {
			throw new ApiException(UserErrorCode.USER_400_045);
		}
		int targetAuthLevel = parseIntOrThrow(targetAuthLevelStr, UserErrorCode.USER_400_045);
		int gvAuthLevel = parseIntOrThrow(param.gvAuthLevel(), CommonErrorCode.COMMON_400_003);
		if (targetAuthLevel < gvAuthLevel) {
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

		// 11) 휴대폰 HMAC 중복 사전 진단 (UX_TB_USER_MBL_NO UNIQUE).
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
		int mblHmacExists = user01Mapper.selectUserMblHmacExists(param.gvCmpnyCd(), phoneHmac);
		if (mblHmacExists > 0) {
			throw new ApiException(UserErrorCode.USER_400_042);
		}

		// 12) 이메일/생년월일 정규화.
		String emailNorm = Normalizers.normalizeEmail(param.email());
		String birthNorm = Normalizers.normalizeBirth(param.birthDt());

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
				, param.gvUserCd()
		);
		user01Mapper.insertOneUser(command);

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
		if (param.creditMonths() != null && param.creditMonths() > 0) {
			String reasonType = isBlank(param.creditReasonType()) ? "OTHER" : param.creditReasonType();
			user01Mapper.insertUserServiceCredit(new UserCreditInsertCommand(
					param.gvCmpnyCd()
					, userCd
					, param.creditMonths()
					, reasonType
					, param.creditReasonDetail()
					, param.gvCmpnyCd()
					, param.gvUserCd()
			));
		}

		log.info("신규 계정 생성 완료 - 요청자={}, 신규userCd={}, userId={}, authCd={}, siteCd={}, nodeCd={}, accountStatus=04",
				param.gvUserCd(), userCd, param.userId(), param.authCd(), siteCd, param.nodeCd());
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
		return UserExcelTemplateBuilder.build();
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
