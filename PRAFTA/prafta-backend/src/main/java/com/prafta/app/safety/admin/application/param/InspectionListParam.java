package com.prafta.app.safety.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * H1 순회점검 결과 리스트 조회 Param.
 *
 * <p>식별자(cmpny/user/site/auth)는 JWT 클레임에서만 도출(IDOR). workMonth/siteCd 만 클라 입력.
 *    siteCd 는 리소스 사업장으로 화면이 전달 가능하나 서버가 멤버십을 재검증한다(미전달 시 토큰 gv_siteCd 폴백).
 */
public record InspectionListParam(
      String workMonth   // YYYYMM (없으면 당월)
    , String siteCd      // 리소스 사업장(없으면 토큰 사업장)
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static InspectionListParam of(String workMonth, String siteCd, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String wm = (workMonth == null || workMonth.isBlank()) ? null : workMonth.trim();
        if (wm != null && (wm.length() != 6 || !wm.chars().allMatch(Character::isDigit))) {
            // 조회 월(YYYYMM) 형식 오류는 잘못된 요청.
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new InspectionListParam(
                wm,
                (siteCd == null || siteCd.isBlank()) ? null : siteCd.trim(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
