package com.prafta.app.leave.leaveflow.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-018-A: 결재자 검색(approver-search) Param.
 *
 * <p>식별값(cmpnyCd/siteCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시 — 사업장 cross-site IDOR 차단).
 *   keyword/page/size 만 쿼리스트링으로 받으며, size 는 상한 캡(50)·기본 20, page 는 0-base 음수 보정한다.
 */
public record LeaveApproverSearchParam(
      String cmpnyCd
    , String siteCd
    , String excludeUserCd
    , String keyword
    , int page
    , int size
    , TokenInfo tokenInfo
) {
    /** 기본 페이지 크기. */
    private static final int DEFAULT_SIZE = 20;
    /** 페이지 크기 상한(대량조회 DoS 방지). */
    private static final int MAX_SIZE = 50;

    public static LeaveApproverSearchParam from(TokenInfo tokenInfo, String keyword, Integer page, Integer size) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        // 회사/사업장/사용자 식별값이 토큰에 없으면 명확한 에러(사업장 스코프 강제)
        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // keyword 공백 정규화(빈/공백 → null = LIKE 생략)
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        // page 음수 보정(0-base)
        int safePage = (page == null || page < 0) ? 0 : page;

        // size 캡(기본 20, 상한 50, 1 미만 보정)
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : size;
        if (safeSize > MAX_SIZE) {
            safeSize = MAX_SIZE;
        }

        return new LeaveApproverSearchParam(
                cmpnyCd, siteCd, userCd, normalizedKeyword, safePage, safeSize, tokenInfo);
    }

    /** LIMIT 값 = size + 1 (hasNext 판정용 1건 추가 조회). */
    public int limitWithLookahead() {
        return size + 1;
    }

    /** OFFSET 값 = page * size. */
    public int offset() {
        return page * size;
    }
}
