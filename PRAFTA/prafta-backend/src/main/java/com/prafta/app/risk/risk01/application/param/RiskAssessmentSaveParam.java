package com.prafta.app.risk.risk01.application.param;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.risk.risk01.dto.request.RiskAssessmentRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B2: 위험성평가 저장 Param.
 * <p>multipart 요청 + 단일 파일 + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>file 은 null 허용(사진 미첨부 케이스).
 *
 * <p>initRiskLv 는 기존 dto 호환을 위해 보존 — SQL 본문에서는 사용되지 않고
 *   INIT_RISK_LV 컬럼이 IFNULL(initLikelihoodScore,1) * IFNULL(initSeverityScore,1) 로 계산된다.
 * <p>prafta-036-C(H-2): siteCd 는 tokenInfo.gv_siteCd() 로 강제 캐노니컬라이즈 (cross-site IDOR 차단).
 *   request.siteCd 필드는 FE 호환을 위해 수신은 하되 무시한다.
 *   이 변경으로 H-3(FileService 디렉토리 경로 공격) 도 자동 해소된다.
 */
public record RiskAssessmentSaveParam(
    String siteCd
    , String processCd
    , String riskTypeCd
    , String hazardCd
    , String assessmentDesc
    , String initLikelihoodScore
    , String initSeverityScore
    , String initRiskLv
    , String initDesc
    , MultipartFile file
    , TokenInfo tokenInfo
) {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(RiskAssessmentSaveParam.class);

    public static RiskAssessmentSaveParam from(
            RiskAssessmentRequest request
            , MultipartFile file
            , TokenInfo tokenInfo
    ) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // prafta-036-C(H-2): siteCd 토큰 캐노니컬라이즈
        String tokenSiteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(tokenSiteCd)) {
            // FE 가 사업장 선택 없이 호출한 경우 명확한 에러
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String reqSiteCd = request.getSiteCd();
        if (StringUtils.hasText(reqSiteCd) && !tokenSiteCd.equals(reqSiteCd)) {
            // 클라이언트가 다른 siteCd 를 보낸 경우 경고만(action: 토큰 값으로 강제)
            log.warn("[risk01] siteCd 캐노니컬라이즈: 요청={}, 토큰={} -> 토큰값 사용 (userCd={})",
                    reqSiteCd, tokenSiteCd, tokenInfo.gv_userCd());
        }

        if (request.getProcessCd() == null || request.getProcessCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getRiskTypeCd() == null || request.getRiskTypeCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getHazardCd() == null || request.getHazardCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskAssessmentSaveParam(
            tokenSiteCd
            , request.getProcessCd()
            , request.getRiskTypeCd()
            , request.getHazardCd()
            , request.getAssessmentDesc()
            , request.getInitLikelihoodScore()
            , request.getInitSeverityScore()
            , request.getInitRiskLv()
            , request.getInitDesc()
            , file
            , tokenInfo
        );
    }
}
