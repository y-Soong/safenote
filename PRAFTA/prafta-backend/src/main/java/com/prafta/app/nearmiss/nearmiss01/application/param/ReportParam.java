package com.prafta.app.nearmiss.nearmiss01.application.param;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.nearmiss.nearmiss01.dto.request.ReportRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A1 근로자 아차사고 보고 등록 Param.
 *
 * <p>multipart 요청 + 단일 파일 + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>식별자(cmpnyCd/siteCd/userCd)는 본문값을 무시하고 JWT 클레임에서만 도출(IDOR 차단).
 *    siteCd 는 tokenInfo.gv_siteCd() 로 강제 캐노니컬라이즈.
 * <p>필수 검증: occurDtime, description (UI 명세 validation 과 동일).
 *    file 은 null 허용(사진 미첨부 케이스).
 */
public record ReportParam(
    String processCd
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String immediateActionDesc
    , MultipartFile file
    , TokenInfo tokenInfo
) {
    public static ReportParam from(ReportRequest request, MultipartFile file, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 는 토큰에서만 도출(미지정이면 사업장 식별 불가 → 차단)
        if (!StringUtils.hasText(tokenInfo.gv_siteCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // 필수값 검증(서버측 1차 방어)
        if (!StringUtils.hasText(request.getOccurDtime()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(request.getDescription()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ReportParam(
            request.getProcessCd()
            , request.getOccurDtime()
            , request.getLocationDesc()
            , request.getDescription()
            , request.getPotentialSeverityCd()
            , request.getImmediateActionDesc()
            , file
            , tokenInfo
        );
    }
}
