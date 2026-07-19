package com.prafta.web.chkLst.chkLst02.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.command.ChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.command.CopyChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.model.ChkptInspectItemModel;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemHistListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.param.CopyChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemHistListQuery;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemListQuery;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemHistListResponse;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;
import com.prafta.web.chkLst.chkLst02.mapper.ChkLst02Mapper;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemHistResult;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;
import com.prafta.web.chkLst.chkLst02.service.ChkLst02Service;
import com.prafta.web.subcon.subcon02.service.ChkptLinkChainCache;
import com.prafta.web.subcon.subcon02.service.ChkptLinkPropagationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst02ServiceImpl implements ChkLst02Service{
	private final ChkLst02Mapper chkLst02Mapper;

	/** PRAFTA-SUBCON-T6-04: 원본 점검문항 변경의 체인 재귀 전파(연동 없으면 no-op). */
	private final ChkptLinkPropagationService chkptLinkPropagationService;

	/** 변경유형 코드: TB_CHKPT_INSPECT_ITEM_HIST.CHG_TYPE */
	private static final String CHG_TYPE_REGIST = "01";	// 등록
	private static final String CHG_TYPE_MODIFY = "02";	// 수정(명칭/시행일/정렬순서)
	private static final String CHG_TYPE_DISABLE = "03";	// 사용중지(Y→N)
	private static final String CHG_TYPE_ENABLE = "04";	// 재사용(N→Y)

	public ChkptInspectItemListResponse selectChkptInspectItemList(ChkptInspectItemListParam param) {

		// PRAFTA-SUBCON-T0-02: 조회 대상 사업장 인가 가드(공통 §8.3)
		assertSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd());

		ChkptInspectItemListResponse response = null;

		List<ChkptInspectItemResult> chkptInspectItemResultList = chkLst02Mapper.selectChkptInspectItemList(ChkptInspectItemListQuery.from(param));

		if(chkptInspectItemResultList != null && chkptInspectItemResultList.size() > 0) {
			response = ChkptInspectItemListResponse.builder().chkptInspectItemResultList(chkptInspectItemResultList).build();
		}

		return response;
	}

	@Transactional
	public void updateChkptInspectItemList(ChkptInspectItemParam param) {
		// PRAFTA-SUBCON-T0-02: 저장 대상 사업장 인가 가드(행별 siteCd 전수)
		assertModelSiteAuth(param.chkptInspectItemModelList());

		// [qa M-3] 링크 위상은 이 트랜잭션 동안 불변 — 행마다 재조회하지 않도록 캐시를 1회만 연다.
		ChkptLinkChainCache linkCache = chkptLinkPropagationService.openChainCache();

		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			ChkptInspectItemCommand command = ChkptInspectItemCommand.from(model);

			// 신규 문항: 이력 적재를 위해 문항코드를 선채번한 뒤 저장 + 등록 이력
			//   (미러 사업장에도 수신사 자체 문항 신규 추가는 허용한다 — 자체/미러 공존)
			if (command.inspectItemCd() == null || command.inspectItemCd().isBlank()) {
				String newItemCd = chkLst02Mapper.selectNextInspectItemCd(command);
				command = withInspectItemCd(command, newItemCd);
				chkLst02Mapper.mergeChkptInspectItemList(command);
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_REGIST);
				propagate(command, CHG_TYPE_REGIST, linkCache);
				continue;
			}

			ChkptInspectItemResult existing = chkLst02Mapper.selectChkptInspectItem(command);

			// 코드가 지정됐지만 미존재(방어): 등록으로 처리
			if (existing == null) {
				chkLst02Mapper.mergeChkptInspectItemList(command);
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_REGIST);
				propagate(command, CHG_TYPE_REGIST, linkCache);
				continue;
			}

			// PRAFTA-SUBCON-T6-03: 연동 미러 문항은 수정 전면 잠금(제공 회사에서 관리 — 운영 예외 없음)
			assertNotMirror(existing, command);

			// 변경 없는 행은 저장/이력 모두 생략(감사 이력 오염 방지)
			String newStrDate = normalizeYmd(command.strDate());
			String newUseYn = (command.useYn() == null || command.useYn().isBlank()) ? "Y" : command.useYn();
			boolean useYnChanged = !Objects.equals(existing.useYn(), newUseYn);
			boolean changed = useYnChanged
					|| !Objects.equals(existing.inspectItemSubj(), command.inspectItemSubj())
					|| !Objects.equals(existing.strDate(), newStrDate)
					|| existing.sortIdx() != command.sortIdx();
			if (!changed) {
				continue;
			}

			chkLst02Mapper.mergeChkptInspectItemList(command);

			String chgType = CHG_TYPE_MODIFY;
			if (useYnChanged) {
				chgType = "N".equals(newUseYn) ? CHG_TYPE_DISABLE : CHG_TYPE_ENABLE;
			}
			chkLst02Mapper.insertChkptInspectItemHist(command, chgType);
			propagate(command, chgType, linkCache);
		}
	}

	@Transactional
	public void deleteChkptInspectItemList(ChkptInspectItemParam param) {
		// PRAFTA-SUBCON-T0-02: 삭제 대상 사업장 인가 가드(행별 siteCd 전수)
		assertModelSiteAuth(param.chkptInspectItemModelList());

		// [qa M-3] 링크 위상 캐시 1회 오픈(행마다 재조회 금지)
		ChkptLinkChainCache linkCache = chkptLinkPropagationService.openChainCache();

		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			ChkptInspectItemCommand command = ChkptInspectItemCommand.from(model);

			ChkptInspectItemResult existing = chkLst02Mapper.selectChkptInspectItem(command);

			// PRAFTA-SUBCON-T6-03: 연동 미러 문항은 삭제(사용중지)도 제공 회사에서 관리한다
			assertNotMirror(existing, command);

			chkLst02Mapper.updateChkptInspectItemList(command);

			// 이미 미사용이던 행은 이력 생략
			if (existing != null && !"N".equals(existing.useYn())) {
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_DISABLE);
				propagate(command, CHG_TYPE_DISABLE, linkCache);
			}
		}
	}

	public ChkptInspectItemHistListResponse selectChkptInspectItemHistList(ChkptInspectItemHistListParam param) {

		// PRAFTA-SUBCON-T0-02: 이력 조회 대상 사업장 인가 가드
		assertSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd());

		List<ChkptInspectItemHistResult> chkptInspectItemHistResultList = chkLst02Mapper.selectChkptInspectItemHistList(ChkptInspectItemHistListQuery.from(param));

		return ChkptInspectItemHistListResponse.builder()
				.chkptInspectItemHistResultList(chkptInspectItemHistResultList)
				.build();
	}

	/**
	 * PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기(선택 복사).
	 * 복사 정책(요청서 §3): 대상 사업장 같은 점검구분 뒤 SORT_IDX append,
	 * 문항코드 신규 채번(회사 단위 시퀀스 — 전 사업장 유일), 시행일 = 실행일(KST),
	 * 이력은 CHG_TYPE '01'(등록)부터 새로 시작. 같은 원본 재가져오기 = 중복 append 허용(§5.5 의도된 동작).
	 */
	@Override
	@Transactional
	public void copyChkptInspectItemList(CopyChkptInspectItemParam param) {

		CopyChkptInspectItemCommand command = CopyChkptInspectItemCommand.from(param);

		// 인가: 원본·대상 사업장 모두 요청자 권한 검증(공통 §8.1/§8.3)
		assertSiteAuth(command.gvCmpnyCd(), command.gvUserCd(), command.srcSiteCd());
		assertSiteAuth(command.gvCmpnyCd(), command.gvUserCd(), command.dstSiteCd());

		// 진입 로그는 인가 통과 이후에만 남긴다 (미인가 입력의 로그 위조 방지 — 보안 리뷰 반영)
		log.info("타 사업장 점검문항 가져오기 진입 - srcSiteCd={}, dstSiteCd={}, chkLstType={}, 요청 문항수={}",
				command.srcSiteCd(), command.dstSiteCd(), command.chkLstType(), command.inspectItemCdList().size());

		// 복사 원본 조회(원본 사업장 정렬순서 순 — append 순서 보존)
		List<ChkptInspectItemResult> sourceItems = chkLst02Mapper.selectCopySourceItemList(command);
		if (sourceItems == null || sourceItems.isEmpty()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001, "가져올 문항이 없습니다.");
		}

		// 대상 사업장의 같은 점검구분 MAX(SORT_IDX) 이후로 이어서 채번(append)
		int sortIdx = chkLst02Mapper.selectMaxSortIdx(command);

		// 시행일 = 가져오기 실행일(서버 KST 강제 — UTC 밀림 방지)
		String today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE);

		// [qa M-3] 링크 위상 캐시 1회 오픈(복사 행마다 재조회 금지)
		ChkptLinkChainCache linkCache = chkptLinkPropagationService.openChainCache();

		for (ChkptInspectItemResult src : sourceItems) {
			ChkptInspectItemCommand copyCommand = new ChkptInspectItemCommand(
				command.gvCmpnyCd()
				, command.dstSiteCd()
				, command.chkLstType()
				, null					// 문항코드는 아래에서 선채번(원본 코드 미복사)
				, src.inspectItemSubj()
				, ++sortIdx
				, today
				, src.useYn()
				, command.gvCmpnyCd()
				, command.gvUserCd()
			);

			String newItemCd = chkLst02Mapper.selectNextInspectItemCd(copyCommand);
			copyCommand = withInspectItemCd(copyCommand, newItemCd);

			chkLst02Mapper.mergeChkptInspectItemList(copyCommand);
			chkLst02Mapper.insertChkptInspectItemHist(copyCommand, CHG_TYPE_REGIST);
			// PRAFTA-SUBCON-T6-04: 가져오기도 대상 사업장의 자체 문항 신설이므로 하위 체인 전파 대상이다
			propagate(copyCommand, CHG_TYPE_REGIST, linkCache);
		}

		log.info("타 사업장 점검문항 가져오기 완료 - dstSiteCd={}, 복사 건수={}", command.dstSiteCd(), sourceItems.size());
	}

	/**
	 * PRAFTA-SUBCON-T6-03: 연동 미러 문항 수정/삭제 잠금.
	 * 미러 문항(LINK_SRC_CMPNY_CD NOT NULL)은 제공 회사에서만 관리한다(운영 예외 없음).
	 */
	private void assertNotMirror(ChkptInspectItemResult existing, ChkptInspectItemCommand command) {
		if (existing != null && existing.linkSrcCmpnyCd() != null) {
			log.error("미러 점검문항 수정/삭제 거부 - cmpnyCd={}, siteCd={}, inspectItemCd={}",
					command.gvCmpnyCd(), command.siteCd(), command.inspectItemCd());
			throw new ApiException(SubconErrorCode.SUBCON_403_005);
		}
	}

	/**
	 * PRAFTA-SUBCON-T6-04: 점검문항 변경의 체인 재귀 전파(점검연동 ACTIVE 하위 미러 + 미러 테넌트 이력).
	 * [qa M-3] 링크 위상 캐시를 전 행이 공유한다(행마다 링크 재조회 = N+1 제거).
	 */
	private void propagate(ChkptInspectItemCommand command, String chgType, ChkptLinkChainCache linkCache) {
		chkptLinkPropagationService.propagateInspectItem(
				command.gvCmpnyCd(), command.siteCd(), command.inspectItemCd(), chgType, linkCache);
	}

	/** PRAFTA-SUBCON-T0-02: 사업장 인가 가드 — siteCd 누락 400, 권한 없음 403 */
	private void assertSiteAuth(String gvCmpnyCd, String gvUserCd, String siteCd) {
		if (siteCd == null || siteCd.isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001, "사업장코드는 필수입니다.");
		}
		int authCnt = chkLst02Mapper.countUserSiteAuth(gvCmpnyCd, gvUserCd, siteCd);
		if (authCnt <= 0) {
			log.error("점검문항 사업장 인가 거부 - gvUserCd={}, siteCd={}", gvUserCd, siteCd);
			throw new ApiException(ChkLstErrorCode.CHKLST_403_003);
		}
	}

	/** PRAFTA-SUBCON-T0-02: 저장/삭제 행 목록의 사업장 인가 가드(행별 siteCd 전수, 중복 사업장은 1회만 검증) */
	private void assertModelSiteAuth(List<ChkptInspectItemModel> models) {
		if (models == null || models.isEmpty()) {
			return;
		}
		// gvCmpnyCd/gvUserCd 는 토큰 유래로 전 행 동일 — 첫 행 기준 사용
		ChkptInspectItemModel first = models.get(0);
		models.stream()
			.map(ChkptInspectItemModel::siteCd)
			.distinct()
			.forEach(siteCd -> assertSiteAuth(first.gvCmpnyCd(), first.gvUserCd(), siteCd));
	}

	/** 시행일 비교용 정규화: 'YYYY-MM-DD' → 'YYYYMMDD' (merge SQL 의 REPLACE/SUBSTRING 과 동일 규칙) */
	private String normalizeYmd(String value) {
		if (value == null) return null;
		String compact = value.replace("-", "");
		return compact.length() > 8 ? compact.substring(0, 8) : compact;
	}

	/** record 인 command 에 선채번한 문항코드를 채운 사본 생성 */
	private ChkptInspectItemCommand withInspectItemCd(ChkptInspectItemCommand command, String inspectItemCd) {
		return new ChkptInspectItemCommand(
			command.cmpnyCd()
			, command.siteCd()
			, command.chkLstType()
			, inspectItemCd
			, command.inspectItemSubj()
			, command.sortIdx()
			, command.strDate()
			, command.useYn()
			, command.gvCmpnyCd()
			, command.gvUserCd()
		);
	}
}
