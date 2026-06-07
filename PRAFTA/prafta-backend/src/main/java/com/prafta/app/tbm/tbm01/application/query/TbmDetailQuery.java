package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm: 세션 단건 상세 조회(A4 참석자/A5 상태/A6 콘텐츠/A7 위험성/A10 완료) 공용 Query.
 *
 * <p>식별자(cmpnyCd/siteCd/userCd)는 token 출처(IDOR 차단). USER_TYPE_CD='REGULAR' 고정.
 * <p>siteCd 를 세션 조회 WHERE 에 강제하여 타 사업장 세션 접근을 차단한다(무음 폴백 금지).
 */
public record TbmDetailQuery(
    String cmpnyCd
    , String siteCd
    , String sessionCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    public static TbmDetailQuery from(String cmpnyCd, String siteCd, String sessionCd, String userCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || siteCd == null || siteCd.isBlank()
                || sessionCd == null || sessionCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmDetailQuery(cmpnyCd, siteCd, sessionCd, userCd, USER_TYPE_REGULAR);
    }
}
