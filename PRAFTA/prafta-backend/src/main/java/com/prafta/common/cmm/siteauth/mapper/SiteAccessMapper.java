package com.prafta.common.cmm.siteauth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
