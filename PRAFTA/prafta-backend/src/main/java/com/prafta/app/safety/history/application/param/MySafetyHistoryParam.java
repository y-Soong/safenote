package com.prafta.app.safety.history.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 내 안전활동 이력 조회 Param (prafta-app-025 J1-10 B-6).
 *
 * <p>식별자(cmpny/user/site)는 JWT 클레임에서만 도출(IDOR). 본인 이력 = INSERT_NO(점검)/INIT_ASSESSOR_ID(위험성)
 *    = JWT gv_userCd 로 서버가 강제하며, 클라가 USER_CD 를 전달하는 경로를 두지 않는다.
 *    kind/page/pageSize 만 클라 입력(표시 필터/페이징). siteCd 는 토큰 사업장(누수 방지 동봉 필터).
 *
 * <p>kind 화이트리스트: ALL(기본) / INSPECT(순회점검) / RISK(위험성평가). 그 외 입력은 ALL 로 정규화.
 */
public record MySafetyHistoryParam(
      String kind        // ALL | INSPECT | RISK (정규화 완료)
    , int page           // 0-base 페이지(음수 → 0)
    , int pageSize       // 1~100 (범위 밖 → 20)
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
) {
    /** 표시 필터 종류 상수(서비스 분기/매퍼 호출 가드). */
    public static final String KIND_ALL = "ALL";
    public static final String KIND_INSPECT = "INSPECT";
    public static final String KIND_RISK = "RISK";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public static MySafetyHistoryParam of(String kind, Integer page, Integer pageSize, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // kind 정규화(화이트리스트 외 → ALL).
        String normalizedKind = KIND_ALL;
        if (kind != null) {
            String upper = kind.trim().toUpperCase();
            if (KIND_INSPECT.equals(upper) || KIND_RISK.equals(upper)) {
                normalizedKind = upper;
            }
        }

        int safePage = (page == null || page < 0) ? 0 : page;
        int safePageSize = (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE)
                ? DEFAULT_PAGE_SIZE
                : pageSize;

        return new MySafetyHistoryParam(
                normalizedKind,
                safePage,
                safePageSize,
                token.gv_cmpnyCd(),
                token.gv_userCd(),
                token.gv_siteCd());
    }
}
