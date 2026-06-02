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

		String leaveCd = null;

		if(param.leaveCd() != null && param.leaveCd() != "") {
			leaveCd = param.leaveCd();
		} else {
			leaveCd = attd03Mapper.selectLeaveCd(param.gvCmpnyCd());
		}

		LeaveNoDupChk leaveNoDupChk = attd03Mapper.selectLeaveNoDupChkResult(LeaveNoDupCheckQuery.from(param));

		if(leaveNoDupChk != null) {
			throw new ApiException(AttdErrorCode.ATTD_400_003);
		}

		attd03Mapper.updateLeaveType(LeaveTypeCommand.from(param));

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
