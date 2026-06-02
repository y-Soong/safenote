package com.prafta.app.chkLst.chkLst01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.chkLst.chkLst01.dto.request.ChecklistInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B1: 체크리스트 정보 조회 Param.
 * <p>request + tokenInfo 를 합쳐 service 진입 인자로 단일화한다.
 * <p>prafta-app-011: siteCd 불일치 시 service 레이어에서 403 차단을 위해
 *   reqSiteCd (클라이언트 원본값) 와 siteCdMismatch 플래그를 보존한다.
 * <p>prafta-036-C(H-3): cmpnyCd 도 tokenInfo.gv_cmpnyCd() 로 강제 캐노니컬라이즈
 *   (파일 디렉토리 경로 첫 세그먼트가 공격자 통제 가능했던 cross-tenant 파일 경로 공격 차단).
 *   request.cmpnyCd 필드는 FE 호환을 위해 수신은 하되 무시한다.
 */
public record ChecklistInfoParam(
    String cmpnyCd
    , String siteCd
    , String reqSiteCd
    , boolean siteCdMismatch
    , String chkptCd
    , String chkptNm
    , String gvCmpnyCd
    , String gvUserCd
    , TokenInfo tokenInfo
) {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ChecklistInfoParam.class);

    public static ChecklistInfoParam from(ChecklistInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 토큰 캐노니컬라이즈 — 불일치 여부는 service 에서 판단
        String tokenSiteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(tokenSiteCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String reqSiteCd = request.getSiteCd();
        boolean mismatch = StringUtils.hasText(reqSiteCd) && !tokenSiteCd.equals(reqSiteCd);
        if (mismatch) {
            log.warn("[chkLst01] siteCd 불일치 감지: 요청={}, 토큰={} (userCd={})",
                    reqSiteCd, tokenSiteCd, tokenInfo.gv_userCd());
        }

        if (request.getChkptCd() == null || request.getChkptCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // prafta-036-C(H-3): cmpnyCd 도 토큰 캐노니컬라이즈
        return new ChecklistInfoParam(
            tokenInfo.gv_cmpnyCd()
            , tokenSiteCd
            , reqSiteCd
            , mismatch
            , request.getChkptCd()
            , request.getChkptNm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo
        );
    }
}
