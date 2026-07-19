package com.prafta.web.baim.baim01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim01.application.command.MasterSiteAuthSetCommand;
import com.prafta.web.baim.baim01.application.command.SiteAdminSiteAuthCommand;
import com.prafta.web.baim.baim01.application.command.SiteInfoCommand;
import com.prafta.web.baim.baim01.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim01.application.query.SiteInfoListQuery;
import com.prafta.web.baim.baim01.result.SiteInfoResult;

@Mapper
public interface Baim01Mapper {
	List<SiteInfoResult> selectSiteInfoList(SiteInfoListQuery query);
	
	String selectSiteCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	void insertSiteNodeInfo(SiteNodeInfoCommand command);
	
	void mergeSiteInfo(SiteInfoCommand command);
	
	void mergeMasterSiteAuthSet(MasterSiteAuthSetCommand command);

	void mergeSiteAdminSiteAuth(SiteAdminSiteAuthCommand command);

	// ===== PRAFTA-SUBCON-T2-04 — 미러 사업장 잠금 가드 =====

	/** 사업장의 연동 원본 회사코드(NULL=일반, NOT NULL=미러=잠금). 행 미존재 시 null. */
	String selectSiteLinkSrcCmpny(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("siteCd") String siteCd);

	/**
	 * 미러 사업장 잠금 필드 diff 카운트(plan D5) — 입력값(§5-5 잠금 필드, mergeSiteInfo 와 동일
	 * 정규화)과 DB 현재값이 하나라도 다르면 1, 동일하거나 미러가 아니면 0.
	 * LAT/LON 은 주소 지오코딩 파생값(팝업 재오픈마다 재산출)이라 diff 대상에서 제외하되,
	 * 미러 저장 경로는 어떤 잠금 필드도 쓰지 않으므로 잠금 자체는 유지된다.
	 */
	int selectMirrorLockedFieldChangedCnt(SiteInfoCommand command);

	/** 미러 사업장 SITE_ADMIN_CD 단독 UPDATE(잠금 예외 운영 필드 — 전체 UPSERT 경유 금지, 전파값 오염 방지). */
	void updateSiteAdminOnly(SiteInfoCommand command);
}
