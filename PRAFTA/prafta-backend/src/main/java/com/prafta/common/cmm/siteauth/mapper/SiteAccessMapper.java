package com.prafta.common.cmm.siteauth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;

/**
 * 사업장 접근 인가(TB_USER_SITE_AUTH) 공용 Mapper.
 *
 * <p>User_03 화면에서 관리하는 사용자별 사업장 권한 원장을 인가 판정에 사용한다.
 * 판정 로직은 {@link com.prafta.common.cmm.siteauth.service.SiteAccessService} 로 단일화한다.
 */
@Mapper
public interface SiteAccessMapper {

    /** 사용자의 사업장 권한(USE_YN='Y') 보유 카운트. 1 이상이면 접근 가능. */
    int selectUserSiteAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("siteCd") String siteCd);

    /**
     * 사용자가 원장(TB_USER_SITE_AUTH, USE_YN='Y')으로 접근 가능한 사업장 목록.
     * (접수함다중사업장권한확장-001) {@code AppAdminAccessMapper.selectAccessibleSites}와
     * 동일 SQL 형태 — 이미 검증된 패턴을 web 계층에도 그대로 둔다(app 파일은 손대지 않음).
     */
    List<AccessibleSiteResult> selectAccessibleSites(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd);

    /** 회사 전체 사업장(USE_YN='Y') 목록. master/hr 전사 role fast-path 용. */
    List<AccessibleSiteResult> selectAllCompanySites(@Param("cmpnyCd") String cmpnyCd);
}
