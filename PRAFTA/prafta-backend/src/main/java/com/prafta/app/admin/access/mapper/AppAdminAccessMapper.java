package com.prafta.app.admin.access.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.admin.access.application.query.AdminAccessQuery;
import com.prafta.app.admin.access.result.AccessibleSiteResult;

/**
 * 001-P1-B1: 관리자 진입판정(access-context) Mapper.
 *
 * <p>식별자(cmpnyCd/userCd/siteCd)는 token 출처(IDOR 차단). leading 콤마, #{} 바인딩, SELECT * 금지.
 */
@Mapper
public interface AppAdminAccessMapper {

    /**
     * 전사(사업장 무관) 노드 정/부 관리자 존재 여부([권한매트릭스 §1] 진입조건).
     * <p>EXISTS 결과 0/1 반환. siteCd 미사용(ofCompany Query).
     */
    int existsNodeAdminAnySite(AdminAccessQuery query);

    /**
     * 현재 선택 사업장 기준 노드 정/부 관리자 존재 여부(moduleActiveMap 산출 입력, D5 per-site 재산출).
     * <p>EXISTS 결과 0/1 반환.
     */
    int existsNodeAdminInSite(AdminAccessQuery query);

    /**
     * 접근 가능 사업장 목록([권한매트릭스 §3.1], D6).
     * <p>TB_USER_SITE_AUTH(USE_YN='Y') ⨝ TB_SITE(USE_YN='Y'). siteCd/siteNo/siteNm 반환.
     */
    List<AccessibleSiteResult> selectAccessibleSites(AdminAccessQuery query);
}
