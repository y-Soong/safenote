package com.prafta.common.cmm.siteauth.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.siteauth.mapper.SiteAccessMapper;
import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteAccessServiceImpl implements SiteAccessService {

    private final SiteAccessMapper siteAccessMapper;

    @Override
    public boolean hasSiteAccess(String gvCmpnyCd, String gvUserCd, String gvAuthCd, String gvSiteCd, String targetSiteCd) {
        if (targetSiteCd == null || targetSiteCd.isBlank()) {
            return false;
        }
        if (gvCmpnyCd == null || gvCmpnyCd.isBlank() || gvUserCd == null || gvUserCd.isBlank()) {
            return false;
        }
        // 1) master/hr — 전 사업장 접근 허용. 원장 자동부여(mergeMasterSiteAuthSet)와 별개로
        //    역할 자체를 전사 통과시켜 원장 누락(신규 사업장 생성 직후 등)에도 관리 업무가 끊기지 않게 한다.
        if (AuthRoleUtils.isManager(gvAuthCd)) {
            return true;
        }
        // 2) 자기 사업장 fast path — 토큰 사업장은 항상 접근 가능(기존 등식 가드와 동등, DB 미조회).
        if (targetSiteCd.equals(gvSiteCd)) {
            return true;
        }
        // 3) User_03 사업장 권한 원장(TB_USER_SITE_AUTH USE_YN='Y').
        return siteAccessMapper.selectUserSiteAuthCnt(gvCmpnyCd, gvUserCd, targetSiteCd) > 0;
    }

    @Override
    public void assertSiteAccess(String gvCmpnyCd, String gvUserCd, String gvAuthCd, String gvSiteCd, String targetSiteCd) {
        if (!hasSiteAccess(gvCmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, targetSiteCd)) {
            log.warn("사업장 접근 거부 - cmpnyCd={}, userCd={}, authCd={}, tokenSite={}, targetSite={}",
                    gvCmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, targetSiteCd);
            throw new ApiException(CommonErrorCode.COMMON_403_003);
        }
    }

    @Override
    public List<AccessibleSiteResult> getAccessibleSites(String gvCmpnyCd, String gvUserCd, String gvAuthCd) {
        // master/hr — hasSiteAccess 의 role fast-path 와 동일 원칙. 원장 미존재(§8.5 "기본 자동
        // 매핑" 문구를 곧이곧대로 믿지 않음) 케이스를 방어하기 위해 원장을 거치지 않고 전사 목록을 바로 반환.
        if (AuthRoleUtils.isManager(gvAuthCd)) {
            return siteAccessMapper.selectAllCompanySites(gvCmpnyCd);
        }
        return siteAccessMapper.selectAccessibleSites(gvCmpnyCd, gvUserCd);
    }
}
