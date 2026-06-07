package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm: 탭별 세션 리스트(A1/A2/A3) 조회 Query.
 *
 * <p>식별자(cmpnyCd/siteCd/userCd)는 token 출처(IDOR 차단). USER_TYPE_CD='REGULAR' 고정.
 * <p>tab 별로 매퍼가 분기되며 본 Query 는 공통 스코프 키만 운반한다.
 */
public record TbmSessionListQuery(
    String cmpnyCd
    , String siteCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    /**
     * 세션 리스트 Query 생성.
     * <p>cmpnyCd/siteCd/userCd 필수. 부재 시 무음 폴백 없이 거부(조직 스코프 IDOR 차단).
     */
    public static TbmSessionListQuery from(String cmpnyCd, String siteCd, String userCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || siteCd == null || siteCd.isBlank()
                || userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmSessionListQuery(cmpnyCd, siteCd, userCd, USER_TYPE_REGULAR);
    }
}
