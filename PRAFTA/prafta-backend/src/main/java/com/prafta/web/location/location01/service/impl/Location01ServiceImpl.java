package com.prafta.web.location.location01.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.consent.mapper.ConsentMapper;
import com.prafta.common.cmm.location.LocationConsentConst;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.location.LocationErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.location.location01.application.param.LocationConsentStatusParam;
import com.prafta.web.location.location01.dto.response.LocationConsentHistResponse;
import com.prafta.web.location.location01.dto.response.LocationConsentStatusResponse;
import com.prafta.web.location.location01.mapper.Location01Mapper;
import com.prafta.web.location.location01.service.Location01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위치정보 동의 현황(Location_01) 조회 구현 — 위치정보 동의철회·중지 S5.
 *
 * <h3>★인가 3중</h3>
 * <ol>
 *   <li>{@code assertSiteAccess} — cross-site IDOR 차단(User_03 원장 기반)</li>
 *   <li>{@code AuthRoleUtils.isManager}(master/hr) 만 사업장 전체.
 *       그 외는 {@code canManageNode} 로 검증한 <b>본인 부서</b>만</li>
 *   <li>상세 조회는 대상이 그 범위 안에 있는지 {@code countUserInScope} 로 재확인</li>
 * </ol>
 *
 * <p>★{@code canManageAllNodes}(master/hr/<b>safe</b>) 가 아니라 {@code isManager}(master/hr) 를 쓴다.
 * 이 화면은 "누가 위치정보 동의를 철회했는가" 라는 <b>인사·개인정보 성격</b>의 정보를 다루므로
 * 안전관리자에게까지 열 이유가 없다. 근태 화면들이 safe 를 포함하는 것과 의도적으로 다르다.
 *
 * <p>★{@code assertSiteAccess} 만으로는 부족하다 — "자기 사업장 fast path" 때문에 일반 사원도
 * 인증만 되면 EP 를 직접 호출해 전 직원 목록을 열람할 수 있다(본 프로젝트에서 3회 재발한 함정).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Location01ServiceImpl implements Location01Service {

    private final Location01Mapper location01Mapper;
    private final ConsentMapper consentMapper;
    private final SiteAccessService siteAccessService;
    private final AttdCloseService attdCloseService;

    @Override
    public LocationConsentStatusResponse selectConsentStatusList(LocationConsentStatusParam param) {

        String scopeNodeCd = resolveScopeNodeCd(param);
        String termsVersion = resolveCurrentVersion();

        return LocationConsentStatusResponse.builder()
                .consentStatusList(location01Mapper.selectConsentStatusList(
                        param.gvCmpnyCd(), param.siteCd(), termsVersion, scopeNodeCd, param.stateFilter()))
                .build();
    }

    @Override
    public LocationConsentHistResponse selectConsentHistories(
            LocationConsentStatusParam param, String targetUserCd) {

        String scopeNodeCd = resolveScopeNodeCd(param);

        // ★상세 IDOR 가드 — 목록과 상세가 다른 EP 라, 여기서 대상 범위를 재확인하지 않으면
        //   userCd 만 바꿔 범위 밖 직원의 철회 이력을 열람할 수 있다.
        if (targetUserCd == null || targetUserCd.isBlank()
                || location01Mapper.countUserInScope(
                        param.gvCmpnyCd(), param.siteCd(), targetUserCd, scopeNodeCd) == 0) {
            log.warn("위치정보 동의 이력 조회 거부: 범위 밖 대상 - 요청자={}, siteCd={}, 대상={}"
                    , param.gvUserCd(), param.siteCd(), targetUserCd);
            throw new ApiException(LocationErrorCode.LOCATION_403_002);
        }

        // ★열람 기록 — 위치정보 관련 개인정보를 누가 언제 봤는지 남긴다(시행령 제20조 관리적 조치).
        //   조회 대상이 민감하므로 로그로라도 흔적을 남긴다(전용 열람 로그는 후속 과제).
        log.info("위치정보 동의 이력 열람 - 요청자={}, 권한={}, siteCd={}, 대상={}"
                , param.gvUserCd(), param.gvAuthCd(), param.siteCd(), targetUserCd);

        return LocationConsentHistResponse.builder()
                .consentHistList(location01Mapper.selectConsentHistList(param.gvCmpnyCd(), targetUserCd))
                .purgeHistList(location01Mapper.selectPurgeHistList(param.gvCmpnyCd(), targetUserCd))
                .build();
    }

    /**
     * 조회 부서 스코프 확정.
     *
     * @return null 이면 사업장 전체(master/hr), 아니면 본인 부서
     */
    private String resolveScopeNodeCd(LocationConsentStatusParam param) {

        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd()
                , param.gvSiteCd(), param.siteCd());

        if (AuthRoleUtils.isManager(param.gvAuthCd())) {
            return null;
        }

        // ★nodeCd 를 요청 파라미터로 받지 않는다 — 임의로 넓힐 여지를 없애고 세션 클레임을 앵커로 쓴다.
        if (param.gvNodeCd() == null || param.gvNodeCd().isBlank()
                || !attdCloseService.canManageNode(param.gvAuthCd(), param.gvUserCd()
                        , param.gvCmpnyCd(), param.siteCd(), param.gvNodeCd())) {
            log.warn("위치정보 동의 현황 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}"
                    , param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.gvNodeCd());
            throw new ApiException(LocationErrorCode.LOCATION_403_002);
        }
        return param.gvNodeCd();
    }

    /** 현재 시행 중인 005 버전. 없으면 제도 자체가 가동 불가이므로 예외. */
    private String resolveCurrentVersion() {
        String version = consentMapper.selectTermsCurrentVersion(LocationConsentConst.LOCATION_TERMS_ID);
        if (version == null || version.isBlank()) {
            log.error("위치기반서비스 약관(005) 활성 행이 없다 - 동의 현황 조회 불가");
            throw new ApiException(LocationErrorCode.LOCATION_500_001);
        }
        return version;
    }
}
