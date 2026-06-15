package com.prafta.app.safety.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * H2 순회점검 상세(일자별 답변 + 불량 사진/비고) 조회 Param.
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임. siteCd/chkLstType/chkptCd 는 리소스 키이며
 *    서버가 CMPNY+SITE WHERE 로 스코프 강제(스코프 밖이면 빈 결과 — IDOR 차단). workMonth(YYYYMM) 필수.
 */
public record InspectionDetailParam(
      String siteCd
    , String chkLstType
    , String chkptCd
    , String workMonth   // YYYYMM
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static InspectionDetailParam of(String siteCd, String chkLstType, String chkptCd,
            String workMonth, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (isBlank(chkLstType) || isBlank(chkptCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String wm = (workMonth == null || workMonth.isBlank()) ? null : workMonth.trim();
        if (wm == null || wm.length() != 6 || !wm.chars().allMatch(Character::isDigit)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new InspectionDetailParam(
                (siteCd == null || siteCd.isBlank()) ? null : siteCd.trim(),
                chkLstType.trim(),
                chkptCd.trim(),
                wm,
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
