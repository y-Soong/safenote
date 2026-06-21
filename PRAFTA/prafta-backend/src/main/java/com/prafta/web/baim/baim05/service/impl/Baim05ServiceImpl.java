package com.prafta.web.baim.baim05.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.baim.BaimErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.baim.baim05.application.command.ClearSlotCommand;
import com.prafta.web.baim.baim05.application.command.CloseSlotHisCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotUpdCommand;
import com.prafta.web.baim.baim05.application.command.InsertDailyQrUserCommand;
import com.prafta.web.baim.baim05.application.command.InsertSlotHisCommand;
import com.prafta.web.baim.baim05.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotFixedCommand;
import com.prafta.web.baim.baim05.application.param.ClearDailyUserSlotsParam;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.SetSlotFixedParam;
import com.prafta.web.baim.baim05.application.param.SlotHisParam;
import com.prafta.web.baim.baim05.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.application.query.SlotHisQuery;
import com.prafta.web.baim.baim05.application.query.UserSlotCountQuery;
import com.prafta.web.baim.baim05.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.dto.response.InsertDailyQrUserResponse;
import com.prafta.web.baim.baim05.dto.response.SlotHisListResponse;
import com.prafta.web.baim.baim05.mapper.Baim05Mapper;
import com.prafta.web.baim.baim05.result.DailyUserLinkPolicyResult;
import com.prafta.web.baim.baim05.result.DailyUserQrInfoResult;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;
import com.prafta.web.baim.baim05.result.SlotHisResult;
import com.prafta.web.baim.baim05.service.Baim05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Baim05ServiceImpl implements Baim05Service{
	private final Baim05Mapper baim05Mapper;
	private final HmacSigner hmacSigner;
	private final AesGcmCrypto aesGcmCrypto;
	private final PasswordHasher passwordHasher;
	private final JwtUtil jwtUtil;

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");


	public DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param) {
		
		DailyUserLinkPoliciesResponse response = null;
		
		DailyUserLinkPolicyResult dailyUserLinkPolicy = baim05Mapper.selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery.from(param));
		
		if(dailyUserLinkPolicy != null) {
			response = DailyUserLinkPoliciesResponse.builder()
					.dailyUserLinkPolicy(dailyUserLinkPolicy)
					.build();
		}
		
		return response;
	}
	
	public DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param) {
		
		DailyUserSlotListResponse response = null;
		
		List<DailyUserSlotListResult> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(DailyUserSlotListQuery.from(param));
		
		if(dailyUserSlotList.size() > 0) {
			response = DailyUserSlotListResponse.builder()
					.dailyUserSlotList(dailyUserSlotList)
					.build();
		}
		
		return response;
	}
	
	public void saveDailyUserLinkPolicy(LinkPoliciesParam param) {

		// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용)
		assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		baim05Mapper.saveDailyUserLinkPolicy(LinkPoliciesCommand.from(param));
		
		int dayLimitCnt = Integer.parseInt(String.valueOf(param.dayLimitCnt()));
		int dailyUserSlotCnt = baim05Mapper.selectDailyUserSlotCnt(UserSlotCountQuery.from(param));
		
		if(dayLimitCnt > 0 || dailyUserSlotCnt > 0) {
			int limit = 0;
			
			if(dailyUserSlotCnt > 0) {
				if(dailyUserSlotCnt > dayLimitCnt) {
					limit = dailyUserSlotCnt;
				} else {
					limit = dayLimitCnt;
				}
			} else {
				limit = dayLimitCnt;
			}
			
			for(int i = 0; i < limit; i++) {
				String useYn = "";
				
				if(i < dayLimitCnt) {
					useYn = "Y";
				} 
				else {
					useYn = "N";
				}
				
				baim05Mapper.saveDailyUserSlot(DailyUserSlotCommand.from(param, i, useYn));
			}
		}
	}

	@Override
	@Transactional
	public InsertDailyQrUserResponse insertDailyQrUser(InsertDailyQrUserParam param) {

		// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용)
		assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		String userCd = baim05Mapper.selectDailyUserCd(param.gvCmpnyCd());
		// 1) 난수 해시값만 생성
		String userPw = passwordHasher.generateRandomHash();
		
		// 2) 정규화
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());

        // 3) ENC (AES-GCM)
        String phoneEnc = (phoneNorm == null) ? null : aesGcmCrypto.encrypt(phoneNorm);

        // 4) HMAC 인덱스 (equals/중복/계정찾기)
        // 회사 단위 유니크면 cmpnyCd 섞는 걸 추천
        String phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm);

        // 5) 파생값
        String phoneLast4 = Normalizers.last4(phoneNorm);
        
        InsertDailyQrUserCommand command = InsertDailyQrUserCommand.from(userCd, param, userPw, phoneEnc, phoneHmac, phoneLast4);
        
        // 휴대폰번호 기준 계정 중복 체크
        int userCnt = baim05Mapper.selectDailyUserDuplicateCnt(phoneHmac);
        
        if(userCnt > 0) {
        	throw new ApiException(BaimErrorCode.BAIM_400_003);
        }
		
        // 일일사용자 계정 생성
		baim05Mapper.insertDailyQrUser(command);
		
		// 일일사용자 계정 슬롯 할당
		baim05Mapper.updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand.from(param, userCd));

		// PRAFTA-055-1: 슬롯 점유 이력 INSERT(발급채널 '02'=QR발급, RELEASE_* = NULL).
		//               본 트랜잭션 포함(점유와 이력 정합 보장). USER_ID 는 QR 사용자의 USER_CD 동일값.
		String hisId = baim05Mapper.selectDailySlotHisId(param.gvCmpnyCd());
		baim05Mapper.insertSlotHis(new InsertSlotHisCommand(
			hisId
			, param.gvCmpnyCd()
			, param.siteCd()
			, param.slotNo()
			, LocalDate.now().format(YMD)	// WORK_DATE = 점유 시작 일자(YYYYMMDD)
			, userCd						// USER_ID = QR 사용자 USER_CD(USER_ID 동일값)
			, "02"							// ISSUE_CHANNEL[SYS014] : 02 = QR발급
			, param.gvUserCd()				// INSERT_NO = 발급한 관리자
		));

		return InsertDailyQrUserResponse.builder().dailyUserQrInfoResult(new DailyUserQrInfoResult(param.gvCmpnyCd(), param.siteCd(), userCd)).build();
	}

	/**
	 * 슬롯 비우기(단일/일괄). 슬롯마다 점유자를 서버에서 재조회(클라 제출 userCd 미신뢰, IDOR 방지)하여
	 * 점유 계정(TB_DAILY_USER + 통합형 TB_USER) 비활성 + 슬롯 초기화(CURR_USER_CD=NULL, SLOT_STATUS='01', FIXED_YN='N')를
	 * 수행한다. 비점유 슬롯은 점유자 없이 슬롯만 멱등 초기화한다(과오류 금지, graceful).
	 */
	@Override
	@Transactional
	public void clearDailyUserSlots(ClearDailyUserSlotsParam param) {
		log.info("일용직 슬롯 비우기 진입 - cmpnyCd={}, slotCnt={}", param.gvCmpnyCd(), param.slots().size());

		for (ClearDailyUserSlotsParam.SlotItem slot : param.slots()) {
			// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
			assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용). 슬롯 단위 사업장 기준.
			assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// 점유자 식별은 서버에서 슬롯 PK 로 재조회(클라 userCd 신뢰 금지)
			String currUserCd = baim05Mapper.selectSlotCurrUserCd(param.gvCmpnyCd(), slot.siteCd(), slot.slotNo());

			ClearSlotCommand command = new ClearSlotCommand(
				param.gvCmpnyCd()
				, slot.siteCd()
				, slot.slotNo()
				, currUserCd
				, param.gvUserCd()
			);

			// 점유자가 있으면 계정 비활성(통합형 TB_USER 는 EMPLOYMENT_TYPE='DAILY' 가드)
			if (currUserCd != null && !currUserCd.isBlank()) {
				baim05Mapper.deactivateDailyUser(command);
				baim05Mapper.deactivateTbUser(command);

				// PRAFTA-055-1: 점유 이력 닫기(열린 행 1건 UPDATE). RELEASE_TYPE '01'=관리자 점유해제.
				baim05Mapper.closeSlotHis(new CloseSlotHisCommand(
					param.gvCmpnyCd()
					, slot.siteCd()
					, slot.slotNo()
					, param.gvUserCd()		// RELEASE_USER = 비우기 수행 관리자
					, "01"					// RELEASE_TYPE[SYS016] : 01 = 관리자 점유해제
					, "관리자 슬롯 비우기"
				));
			}

			// 슬롯 초기화(점유자 유무 무관, 멱등)
			baim05Mapper.updateClearSlot(command);
		}
	}

	/**
	 * 슬롯 점유 고정여부 토글(단일/일괄). 점유 슬롯(SLOT_STATUS='02')만 대상.
	 * 'Y'=점유 유지, 'N'=점유 해지. 계정은 변경하지 않는다. 비점유 슬롯은 영향 0(graceful).
	 */
	@Override
	@Transactional
	public void setDailyUserSlotFixed(SetSlotFixedParam param) {
		log.info("일용직 슬롯 고정여부 토글 진입 - cmpnyCd={}, fixedYn={}, slotCnt={}", param.gvCmpnyCd(), param.fixedYn(), param.slots().size());

		for (SetSlotFixedParam.SlotItem slot : param.slots()) {
			// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
			assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용). 슬롯 단위 사업장 기준.
			assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			SetSlotFixedCommand command = new SetSlotFixedCommand(
				param.gvCmpnyCd()
				, slot.siteCd()
				, slot.slotNo()
				, param.fixedYn()
				, param.gvUserCd()
			);

			baim05Mapper.updateSlotFixed(command);
		}
	}

	/**
	 * PRAFTA-055-3 — 슬롯 사용 이력 조회(최근 30일).
	 *
	 * <p>조회 EP 이므로 파괴적 게이트(역할 게이트)는 적용하지 않고 사업장 접근 권한(assertSiteAccess)만 검증한다.
	 * PII(이름/휴대폰)는 SQL 에서 마스킹된 값만 응답한다(평문 금지).
	 */
	@Override
	public SlotHisListResponse selectDailyUserSlotHisList(SlotHisParam param) {
		// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 최근 30일 cutoff(yyyyMMdd) 서버 산출
		String cutoffYmd = LocalDate.now().minusDays(30).format(YMD);

		List<SlotHisResult> slotHisList = baim05Mapper.selectDailyUserSlotHisList(SlotHisQuery.from(param, cutoffYmd));

		return SlotHisListResponse.builder()
				.slotHisList(slotHisList)
				.build();
	}

	/**
	 * 슬롯 쓰기 EP 역할 게이트(PRAFTA-055-2 — 노드 관리자 보강).
	 *
	 * <p>슬롯 비우기/고정토글/링크정책 변경/QR 발급은 슬롯/계정 상태를 바꾸는 쓰기 동작이다.
	 * 사업장 스코프(assertSiteAccess)만으로는 해당 사업장에 배속된 일반 근로자도 호출 가능하므로
	 * (서비스 거부 위험), 역할을 토큰(JWT 도출 authCd)으로 선검증한다.
	 *
	 * <p>허용: 전사 권한(master/hr) 또는 해당 사업장의 노드(부서) 정/부 관리자.
	 * authCd 는 JWT 도출값만 신뢰한다(클라 바디 신뢰 금지). 미충족 시 BAIM_403_002.
	 */
	private void assertSlotWriteRole(String authCd, String userCd, String cmpnyCd, String siteCd) {
		// 전사 권한(master/hr): 즉시 허용
		if (AuthRoleUtils.isManager(authCd)) {
			return;
		}
		// 해당 사업장 노드(부서) 정/부 관리자: 허용
		if (siteCd != null && !siteCd.isBlank()
			&& baim05Mapper.countNodeAdminInSite(cmpnyCd, siteCd, userCd) > 0) {
			return;
		}
		log.warn("일용직 슬롯 쓰기 권한 없음(역할 게이트 차단) - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
		throw new ApiException(BaimErrorCode.BAIM_403_002);
	}

	/**
	 * 사업장 접근 권한 검증(cross-site IDOR 차단).
	 *
	 * <p>전사 권한(master/hr)은 모든 사업장 허용. 그 외 사용자는 TB_USER_SITE_AUTH(USE_YN='Y')
	 * 매핑 보유 시에만 허용. siteCd 미지정이면 차단. (nearmiss01 assertSiteAccess 패턴 미러)
	 */
	private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
		// 전사 권한(master/hr): 모든 사업장 접근 허용
		if (AuthRoleUtils.isManager(authCd)) {
			return;
		}
		// 사업장 미지정이면 사업장 단위 검증 불가 → 차단
		if (siteCd == null || siteCd.isBlank()) {
			log.warn("일용직 슬롯 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
			throw new ApiException(BaimErrorCode.BAIM_403_001);
		}
		// 그 외: tb_user_site_auth 매핑 보유 시에만 허용
		if (baim05Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
			log.warn("일용직 슬롯 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
			throw new ApiException(BaimErrorCode.BAIM_403_001);
		}
	}
}
