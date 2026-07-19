package com.prafta.web.chkLst.chkLst01.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.application.command.ChkptInfoCommand;
import com.prafta.web.chkLst.chkLst01.application.model.ChkptInfoModel;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptInfoParam;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptListParam;
import com.prafta.web.chkLst.chkLst01.application.query.ChkptListQuery;
import com.prafta.web.chkLst.chkLst01.dto.response.ChkptListResponse;
import com.prafta.web.chkLst.chkLst01.mapper.ChkLst01Mapper;
import com.prafta.web.chkLst.chkLst01.result.ChkptResult;
import com.prafta.web.chkLst.chkLst01.result.ChkptRowRaw;
import com.prafta.web.chkLst.chkLst01.service.ChkLst01Service;
import com.prafta.web.subcon.subcon02.service.ChkptLinkPropagationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst01ServiceImpl implements ChkLst01Service{

	/** [보안검토 High-2] 메뉴 권한 게이트 대상 화면(TB_SYST_AUTH_MENU.MENU_D_ID). */
	private static final String MENU_D_ID = "ChkLst_01";

	private static final String BTN_SAVE = "SAVE";
	private static final String BTN_DELT = "DELT";

	private final ChkLst01Mapper chkLst01Mapper;

	/** PRAFTA-SUBCON-T6-04: 원본 점검대상 변경의 체인 재귀 전파(연동 없으면 no-op). */
	private final ChkptLinkPropagationService chkptLinkPropagationService;

	public ChkptListResponse selectChkptList(ChkptListParam param) {

		ChkptListResponse response = null;

		List<ChkptResult> chkptResultList = chkLst01Mapper.selectChkptList(ChkptListQuery.from(param));

		if(chkptResultList != null && chkptResultList.size() > 0) {
			response = ChkptListResponse.builder().chkptResultList(chkptResultList).build();
		}

		return response;
	}

	/**
	 * 점검대상 저장.
	 *
	 * <p>PRAFTA-SUBCON-T6-03 미러 잠금: 연동 미러 행(LINK_SRC_CMPNY_CD NOT NULL)은
	 * 명칭/비고/사용여부를 바꾸면 403 으로 거부하고, 점검 담당자(MGMT_USER_CD) 지정만 전용 UPDATE 로 허용한다.
	 * (미러 사업장에 수신사가 <b>신규</b> 점검대상을 추가하는 것은 허용 — 자체 점검대상 공존.)
	 *
	 * <p>PRAFTA-SUBCON-T6-04 전파: 자체 행 저장 후 점검연동 ACTIVE 하위 체인으로 재귀 전파한다.
	 *
	 * <p>[보안검토 High-2] 인가 가드(신설): 이 endpoint 에는 메뉴/버튼 권한도, 사업장 인가도 없었다.
	 * T6 전파가 붙으면서 blast radius 가 회사 경계를 넘는다(저장이 수신 테넌트 점검대상까지 UPDATE/INSERT).
	 * 따라서 (a) 행별 사업장 인가(TB_USER_SITE_AUTH) + (b) 메뉴/버튼 권한(ChkLst_01 · SAVE)을 강제한다.
	 */
	@Override
	@Transactional
	public void updateChkptList(ChkptInfoParam param) {

		assertMenuButton(param, BTN_SAVE);
		assertModelSiteAuth(param.chkptInfoModelList());

		for(ChkptInfoModel model : param.chkptInfoModelList()) {
			ChkptInfoCommand command = ChkptInfoCommand.from(model);

			// 신규 행: 전파 대상 식별을 위해 점검대상코드를 선채번한 뒤 저장한다.
			if (isBlank(command.chkptCd())) {
				String newChkptCd = chkLst01Mapper.selectNextChkptCd(command.gvCmpnyCd());
				command = withChkptCd(command, newChkptCd);
				chkLst01Mapper.mergeChkptList(command);
				chkptLinkPropagationService.propagateChkpt(
						command.gvCmpnyCd(), command.siteCd(), command.chkptCd());
				continue;
			}

			ChkptRowRaw existing = chkLst01Mapper.selectChkptRow(
					command.gvCmpnyCd(), command.siteCd(), command.chkLstType(), command.chkptCd());

			// 연동 미러 행 — 잠금 필드 변경은 거부, 담당자 지정만 허용(전파 대상 아님: 수신사 운영 필드).
			if (existing != null && existing.linkSrcCmpnyCd() != null) {
				if (isLockedFieldChanged(existing, command)) {
					log.error("미러 점검대상 수정 거부 - cmpnyCd={}, siteCd={}, chkptCd={}",
							command.gvCmpnyCd(), command.siteCd(), command.chkptCd());
					throw new ApiException(SubconErrorCode.SUBCON_403_004);
				}
				chkLst01Mapper.updateChkptMgmtUser(command);
				continue;
			}

			chkLst01Mapper.mergeChkptList(command);
			chkptLinkPropagationService.propagateChkpt(
					command.gvCmpnyCd(), command.siteCd(), command.chkptCd());
		}
	}

	/**
	 * 점검대상 삭제(USE_YN='N').
	 *
	 * <p>PRAFTA-SUBCON-T6-03: 미러 행은 전면 거부(사용중지도 제공 회사에서 관리).
	 * PRAFTA-SUBCON-T6-04: 자체 행의 사용중지는 하위 체인으로 전파한다.
	 *
	 * <p>[보안검토 High-2] 사업장 인가 + 메뉴/버튼 권한(ChkLst_01 · DELT) 강제.
	 * 사용중지는 하위 체인으로 전파되어 수신사 앱 점검목록에서 대상이 사라지므로(타 테넌트 영향)
	 * 저장보다 게이트가 더 필요하다.
	 */
	@Override
	@Transactional
	public void deleteChkptList(ChkptInfoParam param) {

		assertMenuButton(param, BTN_DELT);
		assertModelSiteAuth(param.chkptInfoModelList());

		for(ChkptInfoModel model : param.chkptInfoModelList()) {
			ChkptInfoCommand command = ChkptInfoCommand.from(model);

			ChkptRowRaw existing = chkLst01Mapper.selectChkptRow(
					command.gvCmpnyCd(), command.siteCd(), command.chkLstType(), command.chkptCd());

			if (existing != null && existing.linkSrcCmpnyCd() != null) {
				log.error("미러 점검대상 삭제 거부 - cmpnyCd={}, siteCd={}, chkptCd={}",
						command.gvCmpnyCd(), command.siteCd(), command.chkptCd());
				throw new ApiException(SubconErrorCode.SUBCON_403_004);
			}

			chkLst01Mapper.updateChkptList(command);
			chkptLinkPropagationService.propagateChkpt(
					command.gvCmpnyCd(), command.siteCd(), command.chkptCd());
		}
	}

	/**
	 * [보안검토 High-2] 메뉴/버튼 권한 게이트 — ChkLst_01 화면의 해당 버튼권한(SAVE/DELT)을 가진 역할만 통과.
	 * authCd 는 JWT 클레임 도출값만 신뢰한다(Subcon02ServiceImpl.assertMenuButton 패턴 동형).
	 */
	private void assertMenuButton(ChkptInfoParam param, String btnType) {

		String authCd = param.gvAuthCd();
		List<ChkptInfoModel> models = param.chkptInfoModelList();
		if (models == null || models.isEmpty()) {
			return; // 대상 없음 = 수행할 쓰기도 없다(빈 요청은 권한 판단 대상이 아니다).
		}

		// gvCmpnyCd/gvUserCd 는 토큰 유래로 전 행 동일 — 첫 행 기준 사용.
		ChkptInfoModel first = models.get(0);

		if (authCd == null || authCd.isBlank()
				|| chkLst01Mapper.selectMenuButtonAuthCnt(first.gvCmpnyCd(), authCd, MENU_D_ID, btnType) <= 0) {
			log.error("점검대상 {} 권한 없음(메뉴 게이트 차단) - userCd={}, authCd={}",
					btnType, first.gvUserCd(), authCd);
			throw new ApiException(ChkLstErrorCode.CHKLST_403_004);
		}
	}

	/**
	 * [보안검토 High-2] 저장/삭제 행 목록의 사업장 인가 가드(행별 siteCd 전수, 중복 사업장은 1회만 검증).
	 * chkLst02 의 assertSiteAuth 와 동형(TB_USER_SITE_AUTH 보유 사업장만 통과).
	 */
	private void assertModelSiteAuth(List<ChkptInfoModel> models) {
		if (models == null || models.isEmpty()) {
			return;
		}
		ChkptInfoModel first = models.get(0);
		models.stream()
			.map(ChkptInfoModel::siteCd)
			.distinct()
			.forEach(siteCd -> assertSiteAuth(first.gvCmpnyCd(), first.gvUserCd(), siteCd));
	}

	/** 사업장 인가 — siteCd 누락 400, 권한 없음 403. */
	private void assertSiteAuth(String gvCmpnyCd, String gvUserCd, String siteCd) {
		if (isBlank(siteCd)) {
			throw new ApiException(CommonErrorCode.COMMON_400_001, "사업장코드는 필수입니다.");
		}
		int authCnt = chkLst01Mapper.countUserSiteAuth(gvCmpnyCd, gvUserCd, siteCd);
		if (authCnt <= 0) {
			log.error("점검대상 사업장 인가 거부 - gvUserCd={}, siteCd={}", gvUserCd, siteCd);
			throw new ApiException(ChkLstErrorCode.CHKLST_403_003);
		}
	}

	/** 잠금 필드(명칭/비고/사용여부) 변경 여부 — null 과 빈 문자열은 동치로 본다(오탐 403 방지). */
	private boolean isLockedFieldChanged(ChkptRowRaw existing, ChkptInfoCommand command) {
		String newUseYn = isBlank(command.useYn()) ? "Y" : command.useYn();
		String curUseYn = isBlank(existing.useYn()) ? "Y" : existing.useYn();

		return !Objects.equals(nz(existing.chkptNm()), nz(command.chkptNm()))
				|| !Objects.equals(nz(existing.chkptDesc()), nz(command.chkptDesc()))
				|| !Objects.equals(curUseYn, newUseYn);
	}

	/** record 인 command 에 선채번한 점검대상코드를 채운 사본 생성 */
	private ChkptInfoCommand withChkptCd(ChkptInfoCommand command, String chkptCd) {
		return new ChkptInfoCommand(
			command.chk()
			, command.siteCd()
			, command.siteNm()
			, command.chkLstType()
			, chkptCd
			, command.chkptNm()
			, command.chkptDesc()
			, command.useYn()
			, command.mgmtUserCd()
			, command.mgmtUserNm()
			, command.gvCmpnyCd()
			, command.gvUserCd()
		);
	}

	private String nz(String value) {
		return value == null ? "" : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
