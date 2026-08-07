package com.prafta.web.attd.attd03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.MmddValidator;
import com.prafta.web.attd.attd03.application.command.LeaveTypeCommand;
import com.prafta.web.attd.attd03.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;
import com.prafta.web.attd.attd03.application.query.LeaveNoDupCheckQuery;
import com.prafta.web.attd.attd03.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd03.dto.response.LeaveTypeListResponse;
import com.prafta.web.attd.attd03.mapper.Attd03Mapper;
import com.prafta.web.attd.attd03.result.LeaveTypeResult;
import com.prafta.web.attd.attd03.service.Attd03Service;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd03ServiceImpl implements Attd03Service{

	// 연차 사용 단위 화이트리스트 [SYS025]: 00=1일 / 01=반차 / 02=시간차(2h) / 03=시간차(1h) / 04=시간차(30분)
	//   ★ D-4(2026-08-07): 반반차 '05' 는 폐지되어 화이트리스트에서 제외한다(HB-04).
	//     남겨두면 마이그레이션 직후 관리자가 다시 '05' 휴가종류를 만들 수 있고, 그 종류는 신청 경로에서
	//     거부되어(ATTD_400_102/054) "종일만 신청 가능한 정체불명" 상태가 된다.
	//     구 '05' 입력은 거부하지 않고 반차('01')로 축소 해석한다 — 신청 경로 정규화
	//     (LeaveUnitGranularity.USAGE_UNIT_TO_CODE / LeavePolicyServiceImpl)와 동일 규약.
	private static final java.util.Set<String> ALLOWED_USE_UNIT_TYPES =
			java.util.Set.of("00", "01", "02", "03", "04");

	/** [폐지] 반반차 코드값(SYS025 '05') — 입력으로 들어오면 반차로 축소 해석한다. */
	private static final String USE_UNIT_TYPE_QUARTER_RETIRED = "05";
	/** 반차 코드값(SYS025 '01'). */
	private static final String USE_UNIT_TYPE_HALF = "01";

	// 연차 사용가능기간 타입 화이트리스트 [SYS026]: 01=설정안함 / 02=해당 연도 내 / 03=기간설정
	// (DB 실측: .claude/context/policies/attd/_audit/prafta-018-syst-val-audit.md §SYS026)
	private static final java.util.Set<String> ALLOWED_AVAIL_TERM_TYPES =
			java.util.Set.of("01", "02", "03");

	// SYS026='03'(기간설정) 코드값.
	//  - 사용자 신청('01'): 이 값일 때만 from/to(MMDD) 영속.
	//  - 관리자 부여('02'): 이 값일 때만 adminAvailMonths(N개월) 영속(prafta-com-016-B).
	private static final String AVAIL_TERM_PERIOD = "03";

	private final Attd03Mapper attd03Mapper;
		
	public Attd03ServiceImpl(Attd03Mapper attd03Mapper) {
		this.attd03Mapper = attd03Mapper;
	}
	
	public void updateLeaveType(LeaveTypeParam param) {

		// PRAFTA-018: 시스템 시드(SYSTEM_YN='Y') 가드 (정책서 §8.5.5)
		// 기존 행 수정/덮어쓰기 시도일 때만 검사. 신규 채번(leaveCd 없음)은 시드 LEAVE_CD와
		// 충돌하지 않는 시퀀스 채번을 사용하므로 시스템 시드를 침해할 가능성 없음.
		guardSystemSeedReadOnly(param);

		// PRAFTA-017: 자동부여 규칙 cross-field 검증 (정책서 §8.1.2)
		validateLeaveTypeRule(param);

		// prafta-044-1: 사용 단위(USE_UNIT_TYPE) 화이트리스트 검증 (서버 권위)
		// prafta-044-FU: 자동부여도 사용단위 입력 분기로 편입 (null 강제 제거)
		String normalizedUseUnitType = normalizeUseUnitType(param);

		// prafta-044-FU(검토 후속): 사용가능기간 타입(SYS026) 화이트리스트 검증 + 날짜 null 강제/형식 검증 (서버 권위)
		//  - availTermType    : 사용자 신청(leaveType='01') → availFromDt/availToDt(MMDD)
		//  - adminAvailTermType: 관리자 부여(leaveType='02', 자동/수동 모두) → adminAvailMonths(부여일+N개월, prafta-com-016-B)
		validateAvailTermType(param);

		String[] normalizedAvail = normalizeUserAvailDates(param);          // [availFromDt, availToDt]
		Integer normalizedAdminMonths = normalizeAdminAvailMonths(param);   // 관리자 '03' N개월(그 외 null)

		// com-013-03(03-1): 신규 등록(leaveCd 미전송/빈값)이면 서버 채번, 수정이면 전달된 코드 유지.
		//   기존 `!= ""` 참조 비교는 항상 true 라 빈 문자열도 그대로 INSERT 키로 쓰여
		//   채번 분기를 타지 못하던 결함이 있었다. isBlank() 로 교정한다.
		String leaveCd = null;

		if (param.leaveCd() != null && !param.leaveCd().isBlank()) {
			leaveCd = param.leaveCd();
		} else {
			leaveCd = attd03Mapper.selectLeaveCd(param.gvCmpnyCd());
		}

		// prafta-app-026 검수정정(attd03/F-02): 번호 중복검사에 자기 자신(resolved leaveCd) 제외 전달.
		//   기존엔 수정 시 자기 행과 충돌해 "중복"으로 거부되던 결함을 해소한다.
		LeaveNoDupChk leaveNoDupChk = attd03Mapper.selectLeaveNoDupChkResult(LeaveNoDupCheckQuery.from(param, leaveCd));

		if(leaveNoDupChk != null) {
			throw new ApiException(AttdErrorCode.ATTD_400_003);
		}

		// com-013-03(03-1): 채번/전달된 leaveCd 를 INSERT 키로 명시 전달.
		//   기존엔 param.leaveCd()(신규=null)를 그대로 영속해 PK 가 null 로 들어가던 결함을 해소한다.
		attd03Mapper.updateLeaveType(LeaveTypeCommand.from(
				param
				, normalizedUseUnitType
				, normalizedAvail[0]
				, normalizedAvail[1]
				, normalizedAdminMonths
				, leaveCd));

	}

	/**
	 * 연차 사용가능기간 타입(SYS026) 화이트리스트 검증 (prafta-044-FU 검토 후속).
	 *
	 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1(사용가능기간 속성).
	 * SYS026 코드값은 DB 실측(prafta-018 syst-val 감사)으로 확인: 01=설정안함 / 02=해당 연도 내 / 03=기간설정.
	 *
	 * <p>보안 검토 지적(useUnitType 과 동일한 무검증 영속 패턴): 화면이 보내는 termType 을
	 * 서버에서 화이트리스트 검증한다. 위반 시 {@link AttdErrorCode#ATTD_400_032}
	 * (사용 가능일을 올바르게 입력해 주세요).
	 *
	 * <ul>
	 *   <li>사용자 신청(leaveType='01'): availTermType 필수 + SYS026 화이트리스트.</li>
	 *   <li>관리자 부여(leaveType='02', 자동/수동 모두): adminAvailTermType 은 "선택"(미입력 허용).
	 *       값이 있으면 SYS026 화이트리스트 검증.</li>
	 *   <li>그 외 leaveType: 본 검증 범위 밖.</li>
	 * </ul>
	 */
	private void validateAvailTermType(LeaveTypeParam param) {
		String leaveType = param.leaveType();

		// 사용자 신청 타입: availTermType 필수 + 화이트리스트
		if ("01".equals(leaveType)) {
			String availTermType = param.availTermType();
			if (availTermType == null || !ALLOWED_AVAIL_TERM_TYPES.contains(availTermType)) {
				log.warn("연차 타입 사용가능기간 검증 실패 - 허용되지 않은 availTermType: leaveNo={}, availTermType={}",
						param.leaveNo(), availTermType);
				throw new ApiException(AttdErrorCode.ATTD_400_032);
			}
			return;
		}

		// 관리자 부여 타입(자동/수동): adminAvailTermType 은 선택. 값이 있으면 화이트리스트 검증.
		if ("02".equals(leaveType)) {
			String adminAvailTermType = param.adminAvailTermType();
			if (adminAvailTermType != null && !adminAvailTermType.isBlank()
					&& !ALLOWED_AVAIL_TERM_TYPES.contains(adminAvailTermType)) {
				log.warn("연차 타입 관리자 사용가능기간 검증 실패 - 허용되지 않은 adminAvailTermType: leaveNo={}, adminAvailTermType={}",
						param.leaveNo(), adminAvailTermType);
				throw new ApiException(AttdErrorCode.ATTD_400_032);
			}
		}
		// leaveType '01'/'02' 외이면 본 검증 범위 밖
	}

	/**
	 * 사용자 신청(leaveType='01') 사용기간 from/to(MMDD) 정규화 (prafta-044-FU).
	 *
	 * <p>availTermType != '03' 이면 from/to 를 null 로 강제(설정안함/해당 연도 내는 기간 미사용).
	 * '03'(기간설정)이면 from/to(MMDD 4자리)를 {@link MmddValidator} 로 형식 검증하고 from<=to 를 검증한다.
	 * (AVAIL_FROM_DT/TO_DT 는 schema 상 varchar(4) MMDD — 화면도 MMDD 로 전송.)
	 *
	 * <p>위반 시 {@link AttdErrorCode#ATTD_400_032}.
	 *
	 * @return {@code [availFromDt, availToDt]} (영속할 정규화 값)
	 */
	private String[] normalizeUserAvailDates(LeaveTypeParam param) {
		// 사용자 신청 타입이 아니면 입력값 그대로(다른 분기에서 처리)
		if (!"01".equals(param.leaveType())) {
			return new String[] { param.availFromDt(), param.availToDt() };
		}

		if (!AVAIL_TERM_PERIOD.equals(param.availTermType())) {
			// 기간설정이 아니면 from/to null 강제
			return new String[] { null, null };
		}

		String from = param.availFromDt();
		String to = param.availToDt();

		if (!MmddValidator.isValid(from) || !MmddValidator.isValid(to)) {
			log.warn("연차 타입 사용기간(MMDD) 형식 검증 실패 - leaveNo={}, from={}, to={}",
					param.leaveNo(), from, to);
			throw new ApiException(AttdErrorCode.ATTD_400_032);
		}
		// MMDD 4자리 zero-padded 이므로 사전식 비교로 from<=to 판정 가능
		if (from.compareTo(to) > 0) {
			log.warn("연차 타입 사용기간 순서 검증 실패(from>to) - leaveNo={}, from={}, to={}",
					param.leaveNo(), from, to);
			throw new ApiException(AttdErrorCode.ATTD_400_032);
		}
		return new String[] { from, to };
	}

	/**
	 * 관리자 부여(leaveType='02') 사용가능 개월수 정규화 (prafta-com-016-B 3-2).
	 *
	 * <p>관리자 부여 '03'(기간설정)의 의미를 절대 날짜 범위에서 "부여일로부터 N개월"(상대기간)으로
	 * 변경한다. 만료는 부여 시점에 {@code LeaveDashboardServiceImpl.resolveManualAvailToDate} 가
	 * {@code 부여일 + N개월}로 산출한다.
	 *
	 * <ul>
	 *   <li>관리자 부여 타입('02')이 아니면 입력값 그대로(다른 분기).</li>
	 *   <li>adminAvailTermType != '03' 이면 개월수 null 강제(설정안함/해당연도내는 개월 미사용).</li>
	 *   <li>'03'(기간설정)이면 개월수 필수 + 1~99 정수.</li>
	 * </ul>
	 *
	 * <p>위반 시 {@link AttdErrorCode#ATTD_400_032}.
	 *
	 * @return 영속할 정규화된 adminAvailMonths(그 외 null)
	 */
	private Integer normalizeAdminAvailMonths(LeaveTypeParam param) {
		// 관리자 부여 타입이 아니면 입력값 그대로
		if (!"02".equals(param.leaveType())) {
			return param.adminAvailMonths();
		}

		if (!AVAIL_TERM_PERIOD.equals(param.adminAvailTermType())) {
			// 기간설정이 아니면 개월수 null 강제
			return null;
		}

		Integer months = param.adminAvailMonths();
		// '03'(기간설정): 1~99 정수 필수
		if (months == null || months < 1 || months > 99) {
			log.warn("관리자 부여 사용가능 개월수 검증 실패(1~99 아님) - leaveNo={}, months={}",
					param.leaveNo(), months);
			throw new ApiException(AttdErrorCode.ATTD_400_032);
		}
		return months;
	}

	/**
	 * 연차 사용 단위(USE_UNIT_TYPE) 서버측 정규화 + 화이트리스트 검증 (prafta-044-1).
	 *
	 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1(사용단위 속성),
	 * §8.5.9(SYS025 사용 단위 정책 00~04). USE_UNIT_TYPE 는 소비 단위(leaveflow) 결정의
	 * 원천이므로, 화면이 보내는 값을 무검증 영속하지 않고 서버에서 권위 있게 검증한다.
	 *
	 * <p>입력 분기별 처리 (prafta-044-FU 로 자동부여 포함하도록 확대):
	 * <ul>
	 *   <li>폐지된 반반차('05') 입력은 반차('01')로 축소 해석한다(D-4, 2026-08-07).</li>
	 *   <li>사용자 신청(leaveType='01') · 관리자 부여(leaveType='02', 자동/수동 모두):
	 *       useUnitType 입력 분기 → SYS025 허용 코드(00~04) 화이트리스트 검증 후 영속.
	 *       위반 시 {@link AttdErrorCode#ATTD_400_054} (허용되지 않은 연차 사용 단위).
	 *       (prafta-044-FU: 자동부여 화면에 사용단위 입력을 추가했으므로 자동부여도 영속 허용)</li>
	 *   <li>그 외 leaveType: 입력값을 검증 없이 통과(기존 동작 유지, 별도 validation 영역).</li>
	 * </ul>
	 *
	 * @return 영속할 정규화된 useUnitType
	 */
	private String normalizeUseUnitType(LeaveTypeParam param) {
		String leaveType   = param.leaveType();
		String useUnitType = param.useUnitType();

		// 사용자 신청(01) · 관리자 부여(02, 자동/수동 모두): useUnitType 영속 분기 → 화이트리스트 검증
		boolean isUnitInputBranch =
				"01".equals(leaveType) || "02".equals(leaveType);

		// D-4: 폐지된 반반차('05') 입력은 반차('01')로 축소 해석(구 화면/구 앱 잔재 방어).
		if (USE_UNIT_TYPE_QUARTER_RETIRED.equals(useUnitType)) {
			log.info("연차 타입 사용 단위 정규화 - 폐지된 반반차('05') 입력을 반차('01')로 축소. leaveNo={}, leaveType={}",
					param.leaveNo(), leaveType);
			useUnitType = USE_UNIT_TYPE_HALF;
		}

		if (isUnitInputBranch) {
			if (useUnitType == null || !ALLOWED_USE_UNIT_TYPES.contains(useUnitType)) {
				log.warn("연차 타입 사용 단위 검증 실패 - 허용되지 않은 USE_UNIT_TYPE: leaveNo={}, leaveType={}, grantType={}, useUnitType={}",
						param.leaveNo(), leaveType, param.grantType(), useUnitType);
				throw new ApiException(AttdErrorCode.ATTD_400_054);
			}
			return useUnitType;
		}

		// 그 외 leaveType: 기존 동작 유지(입력값 그대로)
		return useUnitType;
	}

	/**
	 * 시스템 시드 행(SYSTEM_YN='Y') 수정/삭제 차단 (PRAFTA-018).
	 *
	 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.5
	 *
	 * <p>{@code tb_leave_type_mgmt.SYSTEM_YN='Y'} 행은 PRAFTA-018 법정 연차용으로 자동
	 * 시드된 행이며, attd03 연차 타입 관리 화면에서 readonly 로 유지된다. 본 메서드는
	 * upsert(updateLeaveType) 진입 시 호출되며 다음 두 케이스에서 차단한다:
	 *
	 * <ol>
	 *   <li>요청 leaveCd 가 기존 SYSTEM_YN='Y' 행과 매칭되는 경우 (UPDATE 침해 시도)</li>
	 *   <li>요청에서 SYSTEM_YN='Y' 로 신규 행을 만들려는 케이스는 본 가드 범위 밖이며,
	 *       command 매핑에 SYSTEM_YN 필드 자체가 없으므로 INSERT 시 항상 DB DEFAULT 'N'
	 *       으로 들어간다 (mapper.xml updateLeaveType SQL 본문 참조).</li>
	 * </ol>
	 *
	 * <p>위반 시 {@link AttdErrorCode#ATTD_403_010} 를 던진다.
	 */
	private void guardSystemSeedReadOnly(LeaveTypeParam param) {
		String leaveCd = param.leaveCd();

		// 신규 채번(INSERT) 경로는 검사 대상 외 — 신규 LEAVE_CD 는 FNC_CMM_SEQ_NEXTVAL 로
		// 생성되어 시스템 시드 코드(SYS_ANNUAL 등)와 형식이 다르다.
		if (leaveCd == null || leaveCd.isBlank()) {
			return;
		}

		String systemYn = attd03Mapper.selectSystemYn(param.gvCmpnyCd(), leaveCd);

		if ("Y".equals(systemYn)) {
			log.warn("시스템 시드 연차 타입 수정 시도 차단 - cmpnyCd={}, leaveCd={}, userCd={}",
					param.gvCmpnyCd(), leaveCd, param.gvUserCd());
			throw new ApiException(AttdErrorCode.ATTD_403_010);
		}
	}

	/**
	 * 연차 타입 자동부여 규칙 cross-field 검증 (PRAFTA-017).
	 *
	 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.2
	 *
	 * <ul>
	 *   <li>사용자 신청 타입(leaveType='01'): 자동부여 관련 필드는 검증 대상 외</li>
	 *   <li>관리자 부여 + 수동부여(leaveType='02' AND grantType='02'):
	 *       grantBaseType / grantOffsetMonth / grantAssignMmdd 모두 null이어야 함</li>
	 *   <li>관리자 부여 + 자동부여(leaveType='02' AND grantType='01'):
	 *     <ul>
	 *       <li>grantBaseType IN ('01','02') → grantOffsetMonth 필수(1-11) + grantAssignMmdd null</li>
	 *       <li>grantBaseType = '03' → grantAssignMmdd 필수(MMDD 유효) + grantOffsetMonth null</li>
	 *       <li>그 외 grantBaseType 값은 거부</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <p>위반 시 {@link AttdErrorCode#ATTD_400_015} 를 던진다.
	 */
	private void validateLeaveTypeRule(LeaveTypeParam param) {
		String leaveType       = param.leaveType();
		String grantType       = param.grantType();
		String grantBaseType   = param.grantBaseType();
		Integer grantOffsetMon = param.grantOffsetMonth();
		String grantAssignMmdd = param.grantAssignMmdd();

		// 1) 사용자 신청 타입은 자동부여 규칙 검증 대상 외 (기존 동작 유지)
		if ("01".equals(leaveType)) {
			return;
		}

		// 2) 관리자 부여 타입(leaveType='02')
		if ("02".equals(leaveType)) {

			// 2-1) 수동부여(grantType='02'): 자동부여 관련 필드 모두 null이어야 함
			if ("02".equals(grantType)) {
				if (grantBaseType != null && !grantBaseType.isBlank()) {
					log.warn("연차 타입 검증 실패 - 수동부여인데 grantBaseType이 채워짐: leaveNo={}, grantBaseType={}",
							param.leaveNo(), grantBaseType);
					throw new ApiException(AttdErrorCode.ATTD_400_015);
				}
				if (grantOffsetMon != null) {
					log.warn("연차 타입 검증 실패 - 수동부여인데 grantOffsetMonth가 채워짐: leaveNo={}, grantOffsetMonth={}",
							param.leaveNo(), grantOffsetMon);
					throw new ApiException(AttdErrorCode.ATTD_400_015);
				}
				if (grantAssignMmdd != null && !grantAssignMmdd.isBlank()) {
					log.warn("연차 타입 검증 실패 - 수동부여인데 grantAssignMmdd가 채워짐: leaveNo={}, grantAssignMmdd={}",
							param.leaveNo(), grantAssignMmdd);
					throw new ApiException(AttdErrorCode.ATTD_400_015);
				}
				return;
			}

			// 2-2) 자동부여(grantType='01'): grantBaseType별 분기
			if ("01".equals(grantType)) {

				// 입사일('01') 또는 생일('02') 기준
				if ("01".equals(grantBaseType) || "02".equals(grantBaseType)) {
					// grantOffsetMonth 필수 (1-11)
					if (grantOffsetMon == null || grantOffsetMon < 1 || grantOffsetMon > 11) {
						log.warn("연차 타입 검증 실패 - 기준일={} 자동부여인데 grantOffsetMonth가 1-11 범위를 벗어남: leaveNo={}, grantOffsetMonth={}",
								grantBaseType, param.leaveNo(), grantOffsetMon);
						throw new ApiException(AttdErrorCode.ATTD_400_015);
					}
					// grantAssignMmdd는 null이어야 함
					if (grantAssignMmdd != null && !grantAssignMmdd.isBlank()) {
						log.warn("연차 타입 검증 실패 - 기준일={} 자동부여인데 grantAssignMmdd가 채워짐: leaveNo={}, grantAssignMmdd={}",
								grantBaseType, param.leaveNo(), grantAssignMmdd);
						throw new ApiException(AttdErrorCode.ATTD_400_015);
					}
					return;
				}

				// 부여일지정('03') 기준
				if ("03".equals(grantBaseType)) {
					// grantAssignMmdd 필수 + MMDD 유효성 검증
					if (!MmddValidator.isValid(grantAssignMmdd)) {
						log.warn("연차 타입 검증 실패 - 부여일지정 자동부여인데 grantAssignMmdd가 유효하지 않음: leaveNo={}, grantAssignMmdd={}",
								param.leaveNo(), grantAssignMmdd);
						throw new ApiException(AttdErrorCode.ATTD_400_015);
					}
					// grantOffsetMonth는 null이어야 함
					if (grantOffsetMon != null) {
						log.warn("연차 타입 검증 실패 - 부여일지정 자동부여인데 grantOffsetMonth가 채워짐: leaveNo={}, grantOffsetMonth={}",
								param.leaveNo(), grantOffsetMon);
						throw new ApiException(AttdErrorCode.ATTD_400_015);
					}
					return;
				}

				// 정의되지 않은 grantBaseType 값
				log.warn("연차 타입 검증 실패 - 자동부여인데 grantBaseType이 정의되지 않은 값: leaveNo={}, grantBaseType={}",
						param.leaveNo(), grantBaseType);
				throw new ApiException(AttdErrorCode.ATTD_400_015);
			}
		}
		// leaveType이 '01'/'02' 외이면 본 검증 범위 밖 (다른 validation에서 처리)
	}
	
	public LeaveTypeListResponse getLeaves(LeaveTypeListParam param) {
		
		LeaveTypeListResponse leaveTypeListRes = null;
		
		List<LeaveTypeResult> leaveTypeResultList = attd03Mapper.selectLeaves(LeaveTypeListQuery.from(param));
		
		if(leaveTypeResultList != null && leaveTypeResultList.size() > 0) {
			leaveTypeListRes = LeaveTypeListResponse.builder()
												.leaveTypeResultList(leaveTypeResultList)
												.build();
		}
		
		return leaveTypeListRes;
	}
}
