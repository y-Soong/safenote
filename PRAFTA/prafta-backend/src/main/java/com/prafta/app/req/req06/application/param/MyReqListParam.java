package com.prafta.app.req.req06.application.param;

import java.util.List;

import org.springframework.util.StringUtils;

import com.prafta.app.req.req06.dto.request.MyReqListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-006: 본인 요청 목록 조회 Param.
 *
 * <p>식별값(cmpnyCd/siteCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시).
 * cross-site/cross-tenant IDOR 차단 — AppHome01 패턴 동일.
 *
 * <p>limit 는 서버 고정 20 — 클라이언트가 보내도 무시한다.
 *
 * <p>작업지시서_소속이동-이력가시성-보정: {@code siteCd} 는 더 이상 조회 필터에 쓰이지 않는다(소속이동 전
 * 이력도 본인은 항상 조회 가능해야 함). IDOR 가드 원칙 유지 차원에서 JWT 파생값 자체는 계속 보존한다.
 */
public record MyReqListParam(
        String cmpnyCd
        , String siteCd
        , String userCd
        , List<String> reqTypes
        , List<String> reqStatuses
        , String targetYmdFrom
        , String targetYmdTo
        , String sort
        , int offset
        , int limit
) {

    /** 서버 고정 페이지 크기 (Q5 — 무한 스크롤 20건 단위). */
    public static final int PAGE_SIZE = 20;

    /** 정렬 기본값. */
    public static final String DEFAULT_SORT = "PENDING_FIRST";

    public static MyReqListParam from(MyReqListRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // reqTypes / reqStatuses 는 쉼표 분리. null/빈값이면 필터 미적용(서비스 SQL 에서 조건 생략).
        List<String> reqTypes = splitCsv(request == null ? null : request.getReqTypes());
        List<String> reqStatuses = splitCsv(request == null ? null : request.getReqStatuses());

        String targetYmdFrom = trimToNull(request == null ? null : request.getTargetYmdFrom());
        String targetYmdTo = trimToNull(request == null ? null : request.getTargetYmdTo());

        String sort = (request == null) ? null : trimToNull(request.getSort());
        if (sort == null) sort = DEFAULT_SORT;
        if (!isAllowedSort(sort)) sort = DEFAULT_SORT;

        int offset = (request == null || request.getOffset() == null) ? 0 : Math.max(0, request.getOffset());

        return new MyReqListParam(
                cmpnyCd, siteCd, userCd,
                reqTypes, reqStatuses,
                targetYmdFrom, targetYmdTo,
                sort, offset, PAGE_SIZE
        );
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String[] parts = csv.split(",");
        List<String> result = new java.util.ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p == null ? null : p.trim();
            if (t != null && !t.isEmpty()) result.add(t);
        }
        return result;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isAllowedSort(String sort) {
        return "PENDING_FIRST".equals(sort)
                || "RECENT".equals(sort)
                || "TARGET_DATE".equals(sort);
    }
}
