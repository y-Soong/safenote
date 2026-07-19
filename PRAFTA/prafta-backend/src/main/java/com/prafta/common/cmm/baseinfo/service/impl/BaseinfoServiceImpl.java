package com.prafta.common.cmm.baseinfo.service.impl;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.baseinfo.application.command.MblUniqueCheckCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.baseinfo.application.command.SmsAuthNoCommand;
import com.prafta.common.cmm.baseinfo.application.command.UserPasswordCommand;
import com.prafta.common.cmm.baseinfo.application.param.AppMenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.CmpnyInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.MenuListParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.SiteNodeListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.SystInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.TermsDetailInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserIdInfoParam;
import com.prafta.common.cmm.baseinfo.application.param.UserInfoListParam;
import com.prafta.common.cmm.baseinfo.application.param.UserPasswordParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoParam;
import com.prafta.common.cmm.baseinfo.application.param.WebMenuListParam;
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
import com.prafta.common.cmm.baseinfo.dto.response.AppMenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.CmpnyInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.MenuListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SiteNodeListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.SystInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.TermsDetailInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserIdInfoResponse;
import com.prafta.common.cmm.baseinfo.dto.response.UserInfoListResponse;
import com.prafta.common.cmm.baseinfo.dto.response.WebMenuListResponse;
import com.prafta.common.cmm.baseinfo.mapper.BaseinfoMapper;
import com.prafta.common.cmm.baseinfo.result.AppMenuResult;
import com.prafta.common.cmm.baseinfo.result.BaseInfoResult;
import com.prafta.common.cmm.baseinfo.result.CmpnyInfoResult;
import com.prafta.common.cmm.baseinfo.result.MenuInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteInfoResult;
import com.prafta.common.cmm.baseinfo.result.SiteNodeInfoResult;
import com.prafta.common.cmm.baseinfo.result.SystInfoResult;
import com.prafta.common.cmm.baseinfo.result.TermsDetailInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserIdInfoResult;
import com.prafta.common.cmm.baseinfo.result.UserInfoResult;
import com.prafta.common.cmm.baseinfo.result.WebMenuResult;
import com.prafta.common.cmm.baseinfo.service.BaseinfoService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.MenuListResBuilder;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseinfoServiceImpl implements BaseinfoService{
	
	private final BaseinfoMapper baseinfoMapper;
	private final AesGcmCrypto aesGcmCrypto;
    private final HmacSigner hmacSigner;
    private final PasswordHasher passwordHasher;
	
	public SystInfoListResponse selectSystinfoList(SystInfoListParam param) {

		SystInfoListResponse response = null;
		
		List<SystInfoResult> systInfoList = baseinfoMapper.selectSystinfoList(SystInfoListQuery.from(param)); 
		
		if(systInfoList.size() > 0) {
			response = SystInfoListResponse.builder()
					.systInfoList(systInfoList)
					.build();
		}
		
		return response;
	}
	
	public SystInfoResponse selectSystinfo(SystInfoParam param) {
		
		SystInfoResponse response = null;
		
		List<SystInfoResult> systInfoList = baseinfoMapper.selectSystinfo(SystInfoQuery.from(param)); 
		
		if(systInfoList.size() > 0) {
			response = SystInfoResponse.builder()
					.systInfoList(systInfoList)
					.build();
		}
		
		return response;
	}
	
	public BaseInfoListResponse selectBaseinfoList(BaseInfoListParam param) {
		
		BaseInfoListResponse response = null;
		
		List<BaseInfoResult> baseInfoList = baseinfoMapper.selectBaseinfoList(BaseInfoListQuery.from(param)); 
		
		if(baseInfoList.size() > 0) {
			response = BaseInfoListResponse.builder()
					.baseInfoList(baseInfoList)
					.build();
		}
		
		return response;
	}
	
	public BaseInfoResponse selectBaseinfo(BaseInfoParam param) {		
		BaseInfoResponse response = null;
		
		List<BaseInfoResult> baseInfoList = baseinfoMapper.selectBaseinfo(BaseInfoQuery.from(param)); 
		
		if(baseInfoList.size() > 0) {
			response = BaseInfoResponse.builder()
					.baseInfoList(baseInfoList)
					.build();
		}
		
		return response;
	}
	
	public CmpnyInfoResponse selectCmpnyInfo(CmpnyInfoParam param) {
		
		CmpnyInfoResponse response = null;
		
		CmpnyInfoResult cmpnyInfoResult = baseinfoMapper.selectCmpnyInfo(CmpnyInfoQuery.from(param));
		
		if(cmpnyInfoResult != null) {
			response = CmpnyInfoResponse.builder()
													.cmpnyInfoResult(cmpnyInfoResult)
													.build();
		}
		
		return response;
	}
	
	public UserIdDupleCheckResponse getUserIdDupleCheck(UserIdDupleCheckParam param) {
		
		UserIdDupleCheckResponse response = null;
		
		String uniqueYn = baseinfoMapper.getUserIdDupleCheck(UserIdDupleCheckQuery.from(param)); 
		
		if(uniqueYn != null && uniqueYn != "") {
			response = UserIdDupleCheckResponse.builder()
												.uniqueYn(uniqueYn)
												.build();
		}
		
		
		return response;
	}
	
	public void insertSmsAuthNo(UserSmsAuthNoParam param) {
		
		String phoneNorm = Normalizers.normalizePhone(param.mblNo().replaceAll("-", ""));
		String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
		String certNo = "";
		
		if(param.dupChkYn() != null && param.dupChkYn().equals("Y")) {
			int mblCnt = baseinfoMapper.selectMblUniqChk(
					MblUniqueCheckQuery.from(param.cmpnyCd(), phoneHmac));
			
			if(mblCnt > 0) {
				throw new ApiException(CommonErrorCode.COMMON_400_001, "이미 등록된 휴대폰번호입니다.\\n 확인 후 다시 시도해주세요.");
			}
		}
		
		SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        // prafta-app-032 보강3: OTP 평문 stdout 제거(절대 미출력). 인증번호는 어떤 로그에도 남기지 않는다.
        certNo = Integer.toString(code);
		
		baseinfoMapper.insertSmsAuthNo(SmsAuthNoCommand.from(phoneEnc, phoneHmac, certNo));
	}
	
	public void userSmsAuthCheck(UserSmsAuthNoCheckParam param) {
		
		String phoneNorm = Normalizers.normalizePhone(param.mblNo().replaceAll("-", ""));
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm); 
		
		String smsId = baseinfoMapper.selectCertNoSmsId(UserSmsAuthNoCheckQuery.from(param, phoneHmac));
		
		if(smsId == null || smsId == "") {
        	throw new ApiException(CommonErrorCode.COMMON_400_002);
        }
		
		baseinfoMapper.updateSmsAuthReq(MblUniqueCheckCommand.from(smsId, phoneHmac, param));
	}
	
	public SiteInfoResponse selectSiteInfoList(SiteInfoParam param) {
		
		SiteInfoResponse response = null;
		
		List<SiteInfoResult> siteInfoResultList = baseinfoMapper.selectSiteInfoList(SiteInfoQuery.from(param)); 
		
		if(siteInfoResultList != null && siteInfoResultList.size() > 0) {
			response = SiteInfoResponse.builder()
										.siteInfoResultList(siteInfoResultList)
										.build();
		}
		
		return response; 
	}
	
	public SiteNodeListResponse selectSiteNodeList(SiteNodeListParam param) {

		SiteNodeListResponse response = null;

		List<SiteNodeInfoResult> siteNodeInfoList = baseinfoMapper.selectSiteNodeList(SiteNodeListQuery.from(param));

		if(siteNodeInfoList != null && siteNodeInfoList.size() > 0) {
			response = SiteNodeListResponse.builder()
									.siteNodeInfoList(siteNodeInfoList)
									.build();

		}

		return response;
	}

	/**
	 * 회원가입(비로그인) 단계 사업장 조회. 인증 후 endpoint 와 동일 SQL 을 재사용하되 userCd 권한 조인을
	 * 생략(userCd=null)하여 회사 내 활성 사업장 전체를 반환한다.
	 */
	@Override
	public SiteInfoResponse selectJoinSiteInfoList(com.prafta.common.cmm.baseinfo.application.param.JoinSiteListParam param) {

		SiteInfoResponse response = null;

		com.prafta.common.cmm.baseinfo.application.query.SiteInfoQuery query =
				new com.prafta.common.cmm.baseinfo.application.query.SiteInfoQuery(
						param.cmpnyCd()
						, null // 회원가입 시 userCd 미존재 — TB_USER_SITE_AUTH 조인 생략
						, param.siteNo()
						, param.siteNm()
						, null // 회원가입은 사용여부 선택 필터 미사용(joinMode 가 'Y' 강제)
						, "Y" // 회원가입 전용: 활성기간(개시일~종료일) 밖 + USE_YN!='Y' 사업장 제외
				);

		List<SiteInfoResult> siteInfoResultList = baseinfoMapper.selectSiteInfoList(query);

		if (siteInfoResultList != null && siteInfoResultList.size() > 0) {
			response = SiteInfoResponse.builder()
					.siteInfoResultList(siteInfoResultList)
					.build();
		}

		return response;
	}

	/**
	 * 회원가입(비로그인) 단계 사업장 소속 부서 조회.
	 * userCd 권한 조인 없이 회사+사업장 스코프 내 활성 부서 전체를 반환한다.
	 */
	@Override
	public SiteNodeListResponse selectJoinSiteNodeList(com.prafta.common.cmm.baseinfo.application.param.JoinSiteNodeListParam param) {

		SiteNodeListResponse response = null;

		com.prafta.common.cmm.baseinfo.application.query.SiteNodeListQuery query =
				new com.prafta.common.cmm.baseinfo.application.query.SiteNodeListQuery(
						param.cmpnyCd()
						, null
						, param.siteCd()
						, param.nodeCd()
						, param.nodeType()
						, param.nodeNm()
						, param.parentNodeNm()
						// 회원가입(비로그인) 노드 조회는 현행 유지(담당 지정 노드만) — includeNoAdmin=false.
						, false
				);

		List<SiteNodeInfoResult> siteNodeInfoList = baseinfoMapper.selectSiteNodeList(query);

		if (siteNodeInfoList != null && siteNodeInfoList.size() > 0) {
			response = SiteNodeListResponse.builder()
					.siteNodeInfoList(siteNodeInfoList)
					.build();
		}

		return response;
	}
	
	public WebMenuListResponse selectWebMenuList(WebMenuListParam param) {
		
		WebMenuListResponse response = null;
		
		List<WebMenuResult> webMenuResultList = baseinfoMapper.selectWebMenuList(WebMenuListQuery.from(param));
		
		if(webMenuResultList != null && webMenuResultList.size() > 0) {
			response = WebMenuListResponse.builder()
											.webMenuResultList(webMenuResultList)
											.build();
		}
		return response;
	}
	
	public AppMenuListResponse selectAppMenuList(AppMenuListParam param) {
		AppMenuListResponse response = null;
		
		List<AppMenuResult> appMenuResultList = baseinfoMapper.selectAppMenuList(AppMenuListQuery.from(param));
		
		if(appMenuResultList != null && appMenuResultList.size() > 0) {
			response = AppMenuListResponse.builder()
											.appMenuResultList(appMenuResultList)
											.build();
		}
		return response;
	}
	
	public MenuListResponse selectMenuList(MenuListParam param) {
		
		MenuListResponse retDto = null;
		
		List<MenuInfoResult> menuInfoList = baseinfoMapper.selectMenuList(MenuListQuery.from(param));
		
		if(menuInfoList != null && menuInfoList.size() > 0) {

			Map<String, String> topLabelMap = Map.of();

			// 사용자별 즐겨찾기 MENU_D_ID 집합 조회(IDOR 방지: cmpnyCd/userCd 는 JWT 도출값만 사용).
			//   빌더에서 item.route(==MENU_D_ID) 가 이 집합에 포함되면 isFavorite=true 로 세팅한다.
			Set<String> favoriteMenuDIds = new HashSet<>(
					baseinfoMapper.selectMyFavoriteMenuDIds(param.cmpnyCd(), param.userCd()));

			retDto = MenuListResBuilder.build(
					menuInfoList
					, keyId -> topLabelMap.get(keyId)
					, favoriteMenuDIds
					);
		}

		return retDto;
	}
	
	public UserInfoListResponse selectUserInfoList(UserInfoListParam param) {

		UserInfoListResponse response = null;
		List<UserInfoResult> userInfoList = baseinfoMapper.selectUserInfoList(UserInfoListQuery.from(param));

		if(userInfoList != null && userInfoList.size() > 0) {
			response = UserInfoListResponse.builder()
						.userInfoList(userInfoList)
						.build();
		}

		return response;
	}

	public UserIdInfoResponse selectUserIdInfo(UserIdInfoParam param) {
		// prafta-app-032 보강3: 휴대폰 평문 stdout 제거(PII 미출력).
		String phoneNorm = Normalizers.normalizePhone(param.mblNo());
		String phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm);
		
		UserIdInfoResult userIdInfoResult = baseinfoMapper.selectUserIdInfo(UserIdInfoQuery.from(param, phoneHmac));
		
		if(userIdInfoResult == null) {
			throw new ApiException(CommonErrorCode.COMMON_400_004);
		}
		return UserIdInfoResponse.builder().userIdInfoResult(userIdInfoResult).build();
	}
	
	@Transactional
	public void updateUserPw(UserPasswordParam param) {

		// 005-1-C : 비로그인 비밀번호 찾기 흐름이므로 JWT 강제가 불가하다.
		// 대상 사용자의 최근 SMS 인증이 성공/미만료/미소비 상태인지 서버측에서 검증한다.
		String smsId = baseinfoMapper.selectSmsVerifiedSmsId(SmsVerifiedCheckQuery.from(param));

		if(smsId == null || smsId.isBlank()) {
			log.info("비밀번호 재설정 거부 - SMS 인증 미통과 (cmpnyCd={}, userCd={})", param.cmpnyCd(), param.userCd());
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}

		// 인증 레코드 소비(consume) - 동시 요청 시 단 1건만 통과하도록 VERIFIED_YN='Y' 조건부 갱신.
		int consumed = baseinfoMapper.consumeSmsAuth(SmsAuthConsumeCommand.from(smsId));

		if(consumed != 1) {
			log.info("비밀번호 재설정 거부 - SMS 인증 레코드 소비 실패 (smsId={})", smsId);
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}

		String userPwHash = null;
		if(param.userPw() != null) { userPwHash = passwordHasher.hash(param.userPw()); }

		UserPasswordCommand command = UserPasswordCommand.from(param, userPwHash);
		baseinfoMapper.updateUserPw(command);

		// prafta-app-032 D: 일용직 로그인은 TB_DAILY_USER.USER_PW 로 인증하므로, 동일 USER_CD 의 일용직 행이 있으면
		//   같은 해시로 동기 갱신한다(같은 트랜잭션). 정규 사용자엔 daily 행이 없어 0행 no-op.
		baseinfoMapper.updateDailyUserPw(command);
	}
	
	public TermsDetailInfoResponse selectTermsDetailInfo(TermsDetailInfoParam param) {
		
		TermsDetailInfoResponse response = null;
		
		TermsDetailInfoResult termsDetailInfoResult = baseinfoMapper.selectTermsDetailInfo(TermsDetailInfoQuery.from(param));
		
		if(termsDetailInfoResult != null) {
			response = TermsDetailInfoResponse.builder()
												.termsDetailInfoResult(termsDetailInfoResult)
												.build();
		} else {
			throw new ApiException(CommonErrorCode.COMMON_400_401);
		}
		
		return response;
	}
}
