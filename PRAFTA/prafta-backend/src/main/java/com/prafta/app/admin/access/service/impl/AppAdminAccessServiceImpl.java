package com.prafta.app.admin.access.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.access.application.helper.AdminAccessResolver;
import com.prafta.app.admin.access.application.param.AdminAccessParam;
import com.prafta.app.admin.access.application.query.AdminAccessQuery;
import com.prafta.app.admin.access.dto.response.AdminAccessContextResponse;
import com.prafta.app.admin.access.dto.response.AdminRoleAxisResponse;
import com.prafta.app.admin.access.mapper.AppAdminAccessMapper;
import com.prafta.app.admin.access.result.AccessibleSiteResult;
import com.prafta.app.admin.access.service.AppAdminAccessService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.admin.AdminErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P1-B1: 관리자 모드 진입판정(access-context) 서비스 구현.
 *
 * <p>확정 결정 반영:
 *   <ul>
 *     <li>D1: appApi 전용. 식별자는 JWT 클레임에서만(IDOR 차단).</li>
 *     <li>D5: 선택 siteCd 는 accessibleSites(USE_YN='Y') 멤버십으로 검증 후 채택(assertSiteAccess). 토큰 재발급 없음.</li>
 *     <li>D6: 사업장명 소스 = TB_SITE.SITE_NM (TB_USER_SITE_AUTH ⨝ TB_SITE).</li>
 *   </ul>
 *
 * <p>노드축은 두 기준으로 분리한다:
 *   <ul>
 *     <li>진입조건(canEnterAdmin) — 전사(사업장 무관) 노드관리자 존재(existsNodeAdminAnySite).</li>
 *     <li>모듈 활성/스코프 — 현재 선택 사업장 기준 노드관리자(existsNodeAdminInSite). 타 사업장 권한 누수 차단.</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminAccessServiceImpl implements AppAdminAccessService {

    private final AppAdminAccessMapper appAdminAccessMapper;

    @Override
    public AdminAccessContextResponse selectAccessContext(AdminAccessParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String userCd = token.gv_userCd();
        String authCd = token.gv_authCd();

        log.info("관리자 진입판정 조회 시작 — cmpnyCd={}, userCd={}, selectedSiteCd={}",
                cmpnyCd, userCd, param.selectedSiteCd());

        // 1) 접근 가능 사업장 조회(현장전환 셀렉터 + 선택 사업장 검증의 단일 출처).
        List<AccessibleSiteResult> accessibleSites =
                appAdminAccessMapper.selectAccessibleSites(AdminAccessQuery.ofCompany(cmpnyCd, userCd));

        // 2) 현재 기준 사업장 결정(accessibleSites 조회 이후 확정).
        //    - 선택 siteCd 가 있으면 USE_YN='Y' 멤버십 검증(D5/IDOR) 후 채택(검증 약화 금지).
        //    - 선택값이 없으면 토큰 gv_siteCd 를 멤버십과 정합 검증한 뒤 폴백을 결정한다.
        String baseSiteCd;
        if (param.selectedSiteCd() != null) {
            assertSiteAccess(param.selectedSiteCd(), accessibleSites);
            baseSiteCd = param.selectedSiteCd();
        } else {
            baseSiteCd = resolveDefaultSiteCd(token.gv_siteCd(), accessibleSites);
        }

        // 3) 역할코드 축 산출(DB 추가조회 없음 — AuthRoleUtils 상수 비교).
        AdminRoleAxisResponse roleAxis = AdminAccessResolver.buildRoleAxis(authCd);

        // 4) 노드축 — 진입조건(전사) / 모듈산출(현재 사업장) 분리 판정.
        boolean isNodeAdminAnySite =
                appAdminAccessMapper.existsNodeAdminAnySite(AdminAccessQuery.ofCompany(cmpnyCd, userCd)) == 1;

        boolean isNodeAdminInSite = false;
        if (StringUtils.hasText(baseSiteCd)) {
            isNodeAdminInSite = appAdminAccessMapper.existsNodeAdminInSite(
                    AdminAccessQuery.ofSite(cmpnyCd, userCd, baseSiteCd)) == 1;
        }

        // 5) 진입 가능 + 모듈 활성/스코프 맵 산출([권한매트릭스 §3] 단일 출처).
        boolean canEnterAdmin = AdminAccessResolver.canEnterAdmin(roleAxis, isNodeAdminAnySite);
        Map<String, Boolean> moduleActiveMap = AdminAccessResolver.buildModuleActiveMap(roleAxis, isNodeAdminInSite);
        Map<String, Boolean> moduleScopedMap = AdminAccessResolver.buildModuleScopedMap(roleAxis, isNodeAdminInSite);

        boolean siteSwitchEnabled = accessibleSites.size() > 1;

        log.info("관리자 진입판정 완료 — canEnterAdmin={}, isNodeAdminInSite={}, baseSiteCd={}, sites={}",
                canEnterAdmin, isNodeAdminInSite, baseSiteCd, accessibleSites.size());

        return AdminAccessContextResponse.builder()
                .canEnterAdmin(canEnterAdmin)
                .roleAxis(roleAxis)
                .nodeAdmin(isNodeAdminInSite)
                .currentSiteCd(baseSiteCd)
                .siteSwitchEnabled(siteSwitchEnabled)
                .accessibleSites(accessibleSites)
                .moduleActiveMap(moduleActiveMap)
                .moduleScopedMap(moduleScopedMap)
                .build();
    }

    /**
     * 선택값이 없을 때(현장전환 미사용) 기준 사업장을 결정한다.
     *
     * <p>토큰 gv_siteCd 가 현재 멤버십(USE_YN='Y')과 정합하지 않으면(누락/회수) 멤버십 기준으로 폴백하여,
     * 멤버십이 회수된 사업장 기준으로 모듈이 산출되거나 단일 사업장 노드관리자가 모듈이 닫힌 채 진입하는
     * 공백을 제거한다. 여기서 다루는 후보는 모두 멤버십(USE_YN='Y') 검증을 통과한 사업장이므로 자동 채택해도 IDOR 아님.
     *
     * <ul>
     *   <li>토큰 사업장이 멤버십에 존재 → 그대로 채택.</li>
     *   <li>(a)/(b) 토큰 사업장이 없거나 멤버십에 없고, 멤버십이 정확히 1건 → 그 사업장으로 폴백.</li>
     *   <li>멤버십이 다중이고 토큰 사업장이 부적합 → 자동 선택하지 않고 비움(null). 현장전환으로 명시 선택 유도.</li>
     * </ul>
     */
    private String resolveDefaultSiteCd(String tokenSiteCd, List<AccessibleSiteResult> accessibleSites) {
        boolean tokenSiteIsMember = StringUtils.hasText(tokenSiteCd)
                && accessibleSites.stream().anyMatch(s -> tokenSiteCd.equals(s.getSiteCd()));
        if (tokenSiteIsMember) {
            return tokenSiteCd;
        }
        if (accessibleSites.size() == 1) {
            return accessibleSites.get(0).getSiteCd();
        }
        // 멤버십 다중 + 토큰 사업장 부적합 → 비움(모듈은 닫힌 상태로 노출, 현장전환 후 재조회).
        return null;
    }

    /**
     * D5: 선택 사업장이 접근 가능 목록(USE_YN='Y')에 포함되는지 검증한다. 미포함이면 IDOR 차단(403).
     */
    private void assertSiteAccess(String selectedSiteCd, List<AccessibleSiteResult> accessibleSites) {
        boolean allowed = accessibleSites.stream()
                .anyMatch(s -> selectedSiteCd.equals(s.getSiteCd()));
        if (!allowed) {
            log.info("현장전환 사업장 접근 거부 — selectedSiteCd={}", selectedSiteCd);
            throw new ApiException(AdminErrorCode.ADMIN_403_001);
        }
    }
}
