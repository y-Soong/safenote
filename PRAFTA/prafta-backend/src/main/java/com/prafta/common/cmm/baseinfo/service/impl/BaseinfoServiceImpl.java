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
import com.prafta.common.cmm.sms.AuthCodeSmsDispatcher;
import com.prafta.common.cmm.sms.policy.SmsRateLimitGuard;
import com.prafta.common.cmm.sms.policy.SmsVerifyGuard;
import com.prafta.common.cmm.sms.policy.SmsSendContext;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.sms.SmsErrorCode;
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
    /** SMS-PPURIO-04: 인증번호 실발송 디스패처(게이트 OFF 면 SKIPPED 기록 후 조용히 통과). */
    private final AuthCodeSmsDispatcher authCodeSmsDispatcher;
    /** SMS2-B4: 발송 다층 상한 가드(정책행 잠금으로 TOCTOU 봉인 + 인증코드 INSERT 를 함께 수행). */
    private final SmsRateLimitGuard smsRateLimitGuard;

    /** [3차] 인증번호 검증(대입) 방어. ★발송 축(SmsRateLimitGuard)과 별개 경로다(sec N-3). */
    private final SmsVerifyGuard smsVerifyGuard;
	
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
	
	/**
	 * 인증번호 발송(회원가입 본인인증 / 계정찾기 / 비밀번호 재설정 / 일용직 셀프가입 / 사용자관리 화면 공통).
	 *
	 * <p>★{@code @Transactional} 을 붙이지 말 것. 현재 이 메서드에는 트랜잭션이 없어 INSERT 가 즉시 커밋되고,
	 *    그 덕분에 "인증코드 커밋 → 외부 발송 → 결과 독립 기록" 경계가 이미 성립한다(요청서 §7-3).
	 *    붙이는 순간 외부 HTTP 호출이 트랜잭션 안으로 들어가고, 발송 실패 롤백으로 FAILED 기록이 사라진다.
	 */
	public void insertSmsAuthNo(UserSmsAuthNoParam param) {

		// SMS2-D3(sec L-3): mblNo 가 null 이면 기존 코드는 NPE → 500 이었다.
		// 컨트롤러 @Valid 가 1차로 걸러내지만, 검증을 우회하는 내부 호출 대비로 여기서도 방어한다.
		// ★정규화 후 길이 재확인: '01-0-1234-5678' 처럼 @Pattern 은 통과하지만 자릿수가 어긋나는 입력을 닫는다.
		//
		// ★★[3차 / qa Q-4] 하한을 10 → 9 로 낮춘다(회귀 수정).
		//   2차의 length < 10 은 '02-123-4567'(정규화 9자리)을 새로 400 으로 막았다. 1차까지는 통과했고
		//   프론트 validatePhoneNumber 의 areaRegex 도 허용하는 형식이라, 입력 검증이 <b>기존 사용자를 잠그는</b>
		//   회귀였다. 검증의 목적은 국제번호 발송(단가 수십 배)과 HMAC 버킷 회피 차단이며
		//   ('0' 으로 시작 + 9~11자리 로 둘 다 막힌다), "SMS 수신 불가 번호" 를 입력 단계에서 거르는 것이 아니다.
		//   유선번호로 발송하면 벤더가 실패로 응답하고 그 실패는 SMS_502_* 로 사용자에게 안내된다.
		if(param.mblNo() == null || param.mblNo().isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		String phoneNorm = Normalizers.normalizePhone(param.mblNo().replaceAll("-", ""));
		if(phoneNorm == null || phoneNorm.length() < 9 || phoneNorm.length() > 11) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
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

		// SMS-PPURIO-04: refKey 를 INSERT 전에 생성해 함께 저장한다(발송 결과 UPDATE 의 조인키).
		String refKey = authCodeSmsDispatcher.newRefKey();

		// SMS2-B4: 1차의 인라인 분당 레이트리밋(selectRecentSelfJoinSmsSendCnt)을 가드로 이관했다.
		//   가드가 [정책행 잠금 → 4축 카운트 → 기존코드 만료 → INSERT] 를 한 트랜잭션으로 묶어
		//   동시 요청이 카운트를 동시에 통과하는 TOCTOU 를 봉인한다.
		//   ★진입점 A 는 무인증이라 사용자 축(userCd)을 쓰지 않는다.
		//   ★★이 메서드에 @Transactional 을 붙이지 말 것 — 트랜잭션은 가드 빈 안에만 존재해야 하고,
		//     아래 dispatch(외부 HTTP)는 반드시 가드 트랜잭션 커밋 이후에 호출되어야 한다.
		final String certNoFinal = certNo;
		smsRateLimitGuard.guardAndInsert(
			SmsSendContext.of("SELF_JOIN", phoneHmac, param.ipHash(), null),
			() -> {
				// SMS2-D4(sec H-3): 신규 코드 INSERT 직전에 기존 미검증 코드를 만료시켜 "유효 코드는 항상 1건" 을 강제한다.
				// (상한 통과 후 · INSERT 직전이 유일하게 올바른 위치 — 차단된 요청은 기존 코드를 무효화하면 안 된다)
				baseinfoMapper.expireOldSelfJoinSmsAuth(phoneHmac);
				baseinfoMapper.insertSmsAuthNo(
					SmsAuthNoCommand.from(phoneEnc, phoneHmac, certNoFinal, refKey, param.ipHash()));
			});

		// 실발송 + 결과 기록. 게이트 OFF 면 SKIPPED 기록 후 조용히 통과(기존과 동일한 성공 응답).
		// 발송 실패 시 ApiException(SMS_502_*) 전파 → 컨트롤러가 502 응답 → 프론트 catch 진입(§7-7).
		// ★validMinutes=1 은 BaseinfoMapper.xml 의 EXPIRED_AT(INTERVAL 1 MINUTE)와 한 쌍이다.
		authCodeSmsDispatcher.dispatch(refKey, phoneNorm, certNo, 1);
	}
	
	/**
	 * 인증번호 검증(회원가입 본인인증 / 계정찾기 / 비밀번호 재설정 공통).
	 *
	 * <p>SMS2-A1: 이 엔드포인트(sms-auth-checks)는 {@code @NoAuth} 이며, 통과하면
	 *    비밀번호 재설정(updateUserPw)까지 이어지는 유일한 관문이다. 상한이 없으면 공격자가
	 *    문자를 받지 않고도 6자리를 무제한 대입해 계정을 탈취할 수 있다(sec C-2).
	 *    불일치 시 최신 미검증 코드의 FAIL_CNT 를 올리고, 상한 도달 시 그 코드를 잠근다
	 *    (플랫폼 위치열람 흐름 미러).
	 *
	 * <p>★★[3차] {@link SmsVerifyGuard} 로 두 가지가 추가/변경됐다.
	 *    <ul>
	 *      <li><b>sec N-3</b> — 이 EP 자체의 호출 상한이 전무했다. 번호(HMAC)별 시간당 검증 시도 상한을 신설했다.
	 *          {@code SmsRateLimitGuard}(발송 축)를 재사용하지 <b>않는다</b> — 발송 카운트가 오염된다.</li>
	 *      <li><b>sec N-2 / qa Q-8</b> — 상한 도달 시의 <b>영구 무효화를 시간 잠금으로 바꿨다</b>.
	 *          영구 무효화는 무인증 공격자가 표적 번호로 오답 5회만 던져 피해자의 계정 복구를
	 *          무기한 차단할 수 있는 서비스 거부였다. 이제 {@code VERIFY_LOCK_SEC} 경과 후
	 *          카운터가 0 으로 돌아간다.</li>
	 *    </ul>
	 *
	 * <p>★★[4차 / sec T-2] <b>판정 순서를 바꿨다 — 코드 매칭이 상한보다 먼저다.</b>
	 *    3차는 코드를 보기도 전에 "시간당 검증 시도 30회" 를 검사했다. 그 결과 무인증 공격자가
	 *    표적 번호로 30회를 소모시키면 <b>정답을 가진 정상 사용자까지</b> 반려됐고(재발송으로도 탈출 불가),
	 *    슬라이딩 창이라 2분에 1회만 던져도 무기한 유지됐다.
	 *    이제 상한은 <b>실패한 시도에만</b> 걸리고, 코드가 일치하면 상한과 무관하게 통과한다.
	 *    브루트포스 방어(오답 30회/시간)는 그대로이면서 표적 봉쇄만 사라진다.
	 *
	 * <p>★{@code @Transactional} 을 절대 붙이지 말 것.
	 *    현재 트랜잭션이 없어 {@code increaseSelfJoinSmsFailCnt} 가 statement 단위로 즉시 커밋된다.
	 *    붙이는 순간 {@link ApiException} 롤백으로 카운터 증가가 통째로 사라져 본 방어가 무력화된다.
	 *    부득이 붙여야 한다면 {@code PlatformLocationServiceImpl} 의
	 *    {@code @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiException.class)}
	 *    선례를 반드시 함께 적용할 것.
	 * <p>★★[4차 / qa R-2] <b>이 메서드에 트랜잭션이 없다는 것만으로는 부족하다 — 호출자도 확인해야 한다.</b>
	 *    이 메서드는 무트랜잭션이라 <u>호출자 트랜잭션에 참여</u>한다. 실제로
	 *    {@code LoginServiceImpl.verifyPhoneAuth}(PRAFTA-036)가 {@code @Transactional(rollbackFor=Exception.class)}
	 *    였고 {@code noRollbackFor} 가 없어, 그 EP 에서는 오답 시 카운터·시도 적재가 <b>전부 롤백</b>되어
	 *    6자리 무제한 대입이 가능했다. 4차에서 {@code noRollbackFor = ApiException.class} 를 추가해 닫았다.
	 *    <b>성공 시에는 커밋되므로 이 결함은 실동작 테스트로 잡히지 않는다 — 호출자 전수 코드 검사가 유일한 발견 수단이다.</b>
	 *    현재 호출자는 {@code BaseinfoController}(무트랜잭션) / {@code LoginServiceImpl.verifyPhoneAuth} 2곳뿐이다.
	 *
	 * <p>★{@code certNo} 형식 검증(6자리 숫자)을 여기에 추가하지 말 것.
	 *    카운터 증가보다 앞에 두면 형식 오류 요청이 조기 반환되어 오히려 무한 시도 우회가 생긴다(별건 백로그).
	 *
	 * <p>★잔존 인지(N3): 앱 마이페이지 휴대폰 변경(진입점 B)이 아직 PURPOSE_CD 기본값 'SELF_JOIN' 으로
	 *    적재되므로, 그 코드가 "최신 미검증 행"이 되면 본 카운터가 앱 코드를 무효화할 수 있다.
	 *    A 를 단독 배포할 때는 이 간섭을 감수하며, SMS2-D5(MOBILE_CHANGE 목적 분리)로 해소된다.
	 */
	public void userSmsAuthCheck(UserSmsAuthNoCheckParam param) {

		// SMS2-D3 미러(sec N-10 / qa Q-5): mblNo 가 null 이면 아래 replaceAll 에서 NPE → 500 이었다.
		// 컨트롤러 @Valid 가 1차로 걸러내지만, 검증을 우회하는 내부 호출 대비로 여기서도 방어한다.
		// ★certNo 형식 검증은 여기에 추가하지 말 것(아래 Javadoc 참조 — 카운터 회피 경로가 생긴다).
		if(param.mblNo() == null || param.mblNo().isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		String phoneNorm = Normalizers.normalizePhone(param.mblNo().replaceAll("-", ""));
		if(phoneNorm == null || phoneNorm.isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

		// [3차 / sec N-2] 만료된 대입 잠금 해제 + 실패 허용 횟수 조회.
		//   ★반드시 조회 이전에 호출한다 — 잠금이 만료된 코드를 되살린 뒤에 매칭해야 하기 때문이다.
		//   ★게이트/킬스위치와 무관하게 항상 동작한다(대입 공격은 문자 발송 없이 성립한다).
		//   ★★[4차 / sec T-2] 여기서 시간당 상한을 보지 않는다. 상한은 아래 "불일치" 분기에서만 판정한다.
		final int verifyFailLimit = smsVerifyGuard.beforeVerify(phoneHmac, "SELF_JOIN");

		String smsId = baseinfoMapper.selectCertNoSmsId(
				UserSmsAuthNoCheckQuery.from(param, phoneHmac, verifyFailLimit));

		if(smsId == null || smsId.isEmpty()) {
			// SMS2-A1: 불일치/만료/초과 → 최신 미검증 코드의 FAIL_CNT +1(즉시 커밋).
			//   ★[3차] 상한에 처음 도달하는 순간 FAIL_LOCKED_AT 도 함께 찍힌다(잠금 시작 시각).
			//   ★★아래 afterFailedVerify 보다 <b>먼저</b> 실행해야 한다 — 그쪽이 예외를 던지면
			//     이 카운터 증가가 실행되지 않아 "상한에 걸린 동안은 FAIL_CNT 가 멈추는" 구멍이 생긴다.
			baseinfoMapper.increaseSelfJoinSmsFailCnt(phoneHmac, verifyFailLimit);

			// [4차 / sec T-2 · T-3] 실패 시도 적재 + 시간당 실패 시도 상한 판정.
			//   ★코드가 일치한 요청은 이 경로에 오지 않으므로 정상 사용자는 상한을 소모하지도, 막히지도 않는다.
			smsVerifyGuard.afterFailedVerify(phoneHmac, "SELF_JOIN", SmsErrorCode.SMS_400_005);

			// 상한 도달 시에만 "시도 초과"로 구분해 안내한다(요청서 2차 A-4).
			// ★플랫폼 흐름(PLATFORM_400_010)은 열거 방지로 초과를 구분하지 않으나, 여기서는 의도적으로 다르게 간다:
			//   초과 상태는 공격자가 스스로 만든 상태라 새로 노출되는 정보가 없고,
			//   구분하지 않으면 정상 사용자가 올바른 코드를 넣어도 계속 "불일치"만 보게 되어 이탈한다.
			if(baseinfoMapper.selectSelfJoinFailExceeded(phoneHmac, verifyFailLimit) > 0) {
				log.warn("SMS 인증번호 대입 상한 도달(일시 잠금) - mblLast4={}", Normalizers.last4(phoneNorm));
				throw new ApiException(SmsErrorCode.SMS_400_002);
			}
        	throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

		baseinfoMapper.updateSmsAuthReq(
				MblUniqueCheckCommand.from(smsId, phoneHmac, param, verifyFailLimit));
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
