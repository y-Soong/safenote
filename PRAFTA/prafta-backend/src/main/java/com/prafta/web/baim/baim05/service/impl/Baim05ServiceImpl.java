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
import com.prafta.web.baim.baim05.application.command.SetSlotNodeCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotTypeCommand;
import com.prafta.web.baim.baim05.application.param.ClearDailyUserSlotsParam;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.SetSlotFixedParam;
import com.prafta.web.baim.baim05.application.param.SetSlotNodeParam;
import com.prafta.web.baim.baim05.application.param.SetSlotTypeParam;
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
	
	@Transactional
	public void saveDailyUserLinkPolicy(LinkPoliciesParam param) {

		// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용)
		assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		int dayLimitCnt = Integer.parseInt(String.valueOf(param.dayLimitCnt()));

		// 활성 계정 수 축소가 '점유 중인 슬롯'을 끄게 되면 차단한다(나안).
		//   끌 슬롯 = SLOT_NO >= 새 한도(dayLimitCnt). 그중 점유 슬롯(CURR_USER_CD 존재)이 하나라도 있으면 거부.
		//   (인덱스순으로 슬롯을 끄므로 단순 점유수 비교로는 '점유된 높은 번호 슬롯'이 꺼지는 케이스를 놓침)
		//   허용하면 슬롯만 풀리고 점유자 계정은 비활성화되지 않아 '고아 계정'(슬롯 없이 로그인 가능)이 된다.
		//   관리자는 해당 슬롯을 먼저 '비우기'(점유자 비활성화 동반)한 뒤 한도를 줄여야 한다.
		int occupiedOverLimit = baim05Mapper.selectOccupiedSlotCntOverLimit(param.gvCmpnyCd(), param.siteCd(), dayLimitCnt);
		if (occupiedOverLimit > 0) {
			log.warn("활성 계정 수 축소 차단(점유 슬롯 {}개가 새 한도 {} 인덱스 밖에 존재) - cmpnyCd={}, siteCd={}",
					occupiedOverLimit, dayLimitCnt, param.gvCmpnyCd(), param.siteCd());
			throw new ApiException(BaimErrorCode.BAIM_400_008);
		}

		baim05Mapper.saveDailyUserLinkPolicy(LinkPoliciesCommand.from(param));

		int dailyUserSlotCnt = baim05Mapper.selectDailyUserSlotCnt(UserSlotCountQuery.from(param));

		// 신규 슬롯(활성 계정 수 증가로 추가되는 슬롯)의 기본 소속부서 = 사업장 최상단(루트) 노드.
		//   기존 슬롯은 saveDailyUserSlot 의 ON DUPLICATE KEY UPDATE 에서 NODE_CD 를 건드리지 않으므로 보존된다.
		//   루트 노드가 없으면 null → 신규 슬롯 NODE_CD 는 미지정(이 경우 계정 매칭은 BAIM_400_007 로 차단됨).
		String topNodeCd = baim05Mapper.selectSiteTopNodeCd(param.gvCmpnyCd(), param.siteCd());

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

				baim05Mapper.saveDailyUserSlot(DailyUserSlotCommand.from(param, i, useYn, topNodeCd));
			}
		}
	}

	@Override
	@Transactional
	public InsertDailyQrUserResponse insertDailyQrUser(InsertDailyQrUserParam param) {

		// PRAFTA-055-2: 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용)
		assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// PRAFTA_COM_001 T1-07: 계정 등록 토글(USE_YN) 서버 게이트(fail-closed).
		//   계정 등록 OFF(USE_YN!='Y' 또는 정책 미존재)면 QR 발급 차단. dailyjoin 직접가입 차단과 정합.
		String linkPolicyUseYn = baim05Mapper.selectLinkPolicyUseYn(param.gvCmpnyCd(), param.siteCd());
		if (linkPolicyUseYn == null || !"Y".equals(linkPolicyUseYn)) {
			log.warn("일용직 QR 발급 차단(계정 등록 OFF) - cmpnyCd={}, siteCd={}, useYn={}",
					param.gvCmpnyCd(), param.siteCd(), linkPolicyUseYn);
			throw new ApiException(BaimErrorCode.BAIM_400_004);
		}

		// 요건1: 슬롯에 소속부서(NODE_CD)가 지정되지 않았으면 계정 매칭(QR 발급) 불가(fail-closed, 프론트 비활성과 이중 가드).
		//        부서 미지정 슬롯에 매칭하면 점유 일용직의 TB_USER.NODE_CD 가 빈 채로 남아 근태/조직 집계가 누락된다.
		String slotNodeCd = baim05Mapper.selectSlotNodeCd(param.gvCmpnyCd(), param.siteCd(), param.slotNo());
		if (slotNodeCd == null || slotNodeCd.isBlank()) {
			log.warn("일용직 QR 발급 차단(슬롯 소속부서 미지정) - cmpnyCd={}, siteCd={}, slotNo={}",
					param.gvCmpnyCd(), param.siteCd(), param.slotNo());
			throw new ApiException(BaimErrorCode.BAIM_400_007);
		}

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
        
        // 휴대폰번호 기준 계정 중복 체크(TB_DAILY_USER, USE_YN='Y')
        int userCnt = baim05Mapper.selectDailyUserDuplicateCnt(param.gvCmpnyCd(), phoneHmac);

        if(userCnt > 0) {
        	throw new ApiException(BaimErrorCode.BAIM_400_003);
        }

        // PRAFTA_COM_001 T1-04a: 통합형(TB_USER) 중복 사전검증(dailyjoin 패턴 미러, 활성 USE_YN='Y' 만).
        //   QR 사용자는 USER_ID = USER_CD 동일값이므로 userCd 로 ID 중복을, phoneHmac 로 휴대폰 중복을 확인한다.
        //   활성 중복만 차단하고, 비활성(만료/비우기) 행은 아래 재활성 분기에서 흡수한다(dailyjoin 동작 일치).
        int tbUserIdCnt = baim05Mapper.selectTbUserIdDupleCnt(param.gvCmpnyCd(), userCd);
        if (tbUserIdCnt > 0) {
        	throw new ApiException(BaimErrorCode.BAIM_400_003);
        }
        int tbUserMblCnt = (phoneHmac == null) ? 0 : baim05Mapper.selectTbUserMblHmacDupleCnt(param.gvCmpnyCd(), phoneHmac);
        if (tbUserMblCnt > 0) {
        	throw new ApiException(BaimErrorCode.BAIM_400_003);
        }

        // PRAFTA_COM_001 T1-04a(재작업): 비활성 일용직 재활성 분기(dailyjoin 옵션2 미러).
        //   과거 발급 후 슬롯 비우기/만료로 비활성('05', USE_YN='N')된 TB_USER/TB_DAILY_USER 행이 남아 있으면,
        //   DB 절대 유니크(UX_TB_USER_ID / UX_TB_USER_MBL_NO)와 충돌해 신규 INSERT 가 롤백된다.
        //   → 신규 INSERT 대신 기존 USER_CD 를 재활성한다. 활성 중복은 위에서 이미 차단되었으므로 여기 도달 시 활성 충돌은 없음.
        String reuseUserCd = (phoneHmac == null) ? null
        		: baim05Mapper.selectReactivatableDailyUserCd(param.gvCmpnyCd(), phoneHmac);

        final String finalUserCd;
        if (reuseUserCd != null && !reuseUserCd.isBlank()) {
        	// (가) 재활성 경로 — 기존 USER_CD 재사용. 워커당 1행 유지 + QR 재발급 롤백 해소.
        	//      QR 사용자는 USER_ID = USER_CD 동일값이므로, 재사용 USER_CD 로 Command 를 재구성한다(USER_PW 는 난수 재설정).
        	finalUserCd = reuseUserCd;
        	InsertDailyQrUserCommand reuseCommand =
        			InsertDailyQrUserCommand.from(finalUserCd, param, userPw, phoneEnc, phoneHmac, phoneLast4);

        	// TB_DAILY_USER 기존 행 재활성(만료일/잠금 초기화 포함)
        	baim05Mapper.reactivateDailyQrUser(reuseCommand);

        	// 통합형 TB_USER 재활성. 영향행 0이면 통합형 이전 레거시(TB_USER 없음) → 신규 INSERT 폴백.
        	int tbUserUpd = baim05Mapper.reactivateDailyQrUserTbUser(reuseCommand);
        	if (tbUserUpd <= 0) {
        		baim05Mapper.insertDailyQrUserToTbUser(reuseCommand);
        	}

        	// SITE_AUTH 는 사업장 변경 가능성을 고려해 upsert(기존 권한 행 회복 또는 신규 부여)
        	baim05Mapper.upsertDailyQrUserTbUserSiteAuth(reuseCommand);
        } else {
        	// (나) 신규 발급 경로 — 채번된 USER_CD 로 TB_DAILY_USER + 통합형 TB_USER/SITE_AUTH INSERT.
        	finalUserCd = userCd;

        	// 일일사용자 계정 생성(TB_DAILY_USER)
        	baim05Mapper.insertDailyQrUser(command);

        	// PRAFTA_COM_001 T1-04a: 같은 트랜잭션에서 통합형 TB_USER + TB_USER_SITE_AUTH 동시 INSERT.
        	//   EMPLOYMENT_TYPE='DAILY'/AUTH_CD='99999'/NODE_CD=NULL/ACCOUNT_STATUS='01'/USER_PW=동일 hash.
        	//   → QR 계정도 통합 로그인 가능 + 사용자 관리(TB_USER 기반)에 노출. dailyjoin 신규가입 경로 미러.
        	baim05Mapper.insertDailyQrUserToTbUser(command);
        	baim05Mapper.insertDailyQrUserTbUserSiteAuth(command);
        }

		// 일일사용자 계정 슬롯 할당(신규/재활성 공통, 최종 USER_CD 기준)
		baim05Mapper.updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand.from(param, finalUserCd));

		// PRAFTA-daily-user-dept-3: 점유 슬롯의 지정 부서(NODE_CD)를 점유 일용직 TB_USER.NODE_CD 로 매칭.
		//   위(요건1)에서 부서 미지정 슬롯은 이미 차단했으므로 여기 slotNodeCd 는 항상 유효값이다.
		//   EMPLOYMENT_TYPE='DAILY' 가드로 정규 사용자 오염 차단. dailyjoin 직접가입 경로와 동일 처리.
		baim05Mapper.updateTbUserNodeCdFromSlot(param.gvCmpnyCd(), finalUserCd, slotNodeCd, param.gvUserCd());

		// PRAFTA-055-1: 슬롯 점유 이력 INSERT(발급채널 '02'=QR발급, RELEASE_* = NULL).
		//               본 트랜잭션 포함(점유와 이력 정합 보장). USER_ID 는 QR 사용자의 USER_CD 동일값.
		String hisId = baim05Mapper.selectDailySlotHisId(param.gvCmpnyCd());
		baim05Mapper.insertSlotHis(new InsertSlotHisCommand(
			hisId
			, param.gvCmpnyCd()
			, param.siteCd()
			, param.slotNo()
			, LocalDate.now().format(YMD)	// WORK_DATE = 점유 시작 일자(YYYYMMDD)
			, finalUserCd					// USER_ID = QR 사용자 USER_CD(USER_ID 동일값, 신규/재활성 공통)
			, "02"							// ISSUE_CHANNEL[SYS014] : 02 = QR발급
			, param.gvUserCd()				// INSERT_NO = 발급한 관리자
		));

		return InsertDailyQrUserResponse.builder().dailyUserQrInfoResult(new DailyUserQrInfoResult(param.gvCmpnyCd(), param.siteCd(), finalUserCd)).build();
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
	 * 슬롯 구분(SLOT_TYPE, SYS014) 변경(단일/일괄). 비점유 슬롯만 대상.
	 * '01'=직접가입, '02'=QR발급. 점유 슬롯은 fail-closed 로 거부한다(프론트 잠금 + 서버 재조회 가드 + mapper WHERE 가드 3중 방어).
	 */
	@Override
	@Transactional
	public void setDailyUserSlotType(SetSlotTypeParam param) {
		log.info("일용직 슬롯 구분 변경 진입 - cmpnyCd={}, slotCnt={}", param.gvCmpnyCd(), param.slots().size());

		for (SetSlotTypeParam.SlotItem slot : param.slots()) {
			// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
			assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// 슬롯 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용). 슬롯 단위 사업장 기준.
			assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// 구분 값 검증: SYS014 의 '01'(직접가입) 또는 '02'(QR발급)만 허용
			String slotType = slot.slotType();
			if (!"01".equals(slotType) && !"02".equals(slotType)) {
				log.warn("일용직 슬롯 구분 변경 차단(유효하지 않은 구분 값) - siteCd={}, slotNo={}, slotType={}",
						slot.siteCd(), slot.slotNo(), slotType);
				throw new ApiException(BaimErrorCode.BAIM_400_006);
			}

			// 점유 가드(fail-closed): 서버에서 점유자를 재조회. 점유중이면 변경 거부(클라 신뢰 금지).
			String currUserCd = baim05Mapper.selectSlotCurrUserCd(param.gvCmpnyCd(), slot.siteCd(), slot.slotNo());
			if (currUserCd != null && !currUserCd.isBlank()) {
				log.warn("일용직 슬롯 구분 변경 차단(점유중 슬롯) - siteCd={}, slotNo={}", slot.siteCd(), slot.slotNo());
				throw new ApiException(BaimErrorCode.BAIM_400_005);
			}

			SetSlotTypeCommand command = new SetSlotTypeCommand(
				param.gvCmpnyCd()
				, slot.siteCd()
				, slot.slotNo()
				, slotType
				, param.gvUserCd()
			);

			// mapper UPDATE WHERE 절에도 점유 가드(CURR_USER_CD IS NULL AND SLOT_STATUS != '02')를 둬 TOCTOU 방어.
			baim05Mapper.updateSlotType(command);
		}
	}

	/**
	 * 슬롯 소속부서(NODE_CD) 지정/해제(단일/일괄). PRAFTA-daily-user-dept-2.
	 *
	 * <p>SetSlotType 과 달리 점유중 슬롯도 부서 지정을 허용한다(슬롯에 부서를 미리 배정하는 메타 설정이므로).
	 * nodeCd 가 빈값이면 부서 해제(NULL), 값이 있으면 그 슬롯 사업장에 속한 유효 노드인지 서버가 재검증한다
	 * (cross-site 노드 변조 차단). 각 슬롯마다 사업장 접근 권한 + 슬롯 쓰기 역할 게이트를 적용한다.
	 */
	@Override
	@Transactional
	public void setDailyUserSlotNode(SetSlotNodeParam param) {
		log.info("일용직 슬롯 소속부서 지정 진입 - cmpnyCd={}, slotCnt={}", param.gvCmpnyCd(), param.slots().size());

		for (SetSlotNodeParam.SlotItem slot : param.slots()) {
			// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
			assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// 슬롯 쓰기 EP 역할 게이트(master/hr 또는 해당 사업장 노드 관리자만 허용). 슬롯 단위 사업장 기준.
			assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), slot.siteCd());

			// 부서 지정(nodeCd != null)인 경우, 그 슬롯 사업장에 속한 유효 노드인지 서버 재검증(cross-site 변조 차단).
			if (slot.nodeCd() != null) {
				int siteNodeCnt = baim05Mapper.countSiteNode(param.gvCmpnyCd(), slot.siteCd(), slot.nodeCd());
				if (siteNodeCnt <= 0) {
					log.warn("일용직 슬롯 소속부서 지정 차단(사업장에 속하지 않은 노드) - siteCd={}, slotNo={}, nodeCd={}",
							slot.siteCd(), slot.slotNo(), slot.nodeCd());
					throw new ApiException(BaimErrorCode.BAIM_400_005);
				}
			}

			SetSlotNodeCommand command = new SetSlotNodeCommand(
				param.gvCmpnyCd()
				, slot.siteCd()
				, slot.slotNo()
				, slot.nodeCd()		// null 이면 부서 해제
				, param.gvUserCd()
			);

			baim05Mapper.updateSlotNode(command);
		}
	}

	/**
	 * PRAFTA-055-3 — 슬롯 사용 이력 조회(최근 30일).
	 *
	 * <p>PRAFTA_COM_003 3.3.3-1 보안 보강: 본 이력은 해제자(관리자) 실명+로그인ID(USER_NM(USER_ID))를
	 * 노출하므로, 사업장 스코프(assertSiteAccess)만으로는 같은 사업장 일반 근로자(AUTH_CD='99999')도
	 * 직접 호출해 관리자 PII 를 평문 획득할 수 있다. 계정슬롯 관리(Baim_05)는 관리자 운영 화면이므로
	 * 슬롯 쓰기 경로와 동일한 역할 게이트(assertSlotWriteRole)를 적용해 일반 근로자 직접 호출을 차단한다.
	 * 허용: 전사 권한(master/hr) 또는 해당 사업장 노드(부서) 정/부 관리자.
	 * PII(점유자 이름/휴대폰)는 SQL 에서 마스킹된 값만 응답한다(평문 금지).
	 */
	@Override
	public SlotHisListResponse selectDailyUserSlotHisList(SlotHisParam param) {
		// 사업장 접근 권한 검증(cross-site/cross-company IDOR 차단)
		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
		// 슬롯 운영 역할 게이트: 해제자(관리자) PII 노출 보호. 미충족 시 BAIM_403_002
		assertSlotWriteRole(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

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
