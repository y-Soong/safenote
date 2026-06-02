package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C: 세션/출결 조회 공용 Query(mapper 진입).
 * <p>cmpnyCd / siteCd 는 token 출처(조직 스코프 IDOR 차단). userCd 는 본인 출결 조회 시 사용한다.
 * <p>siteCd 는 세션 조회 WHERE 에 강제되어 타 사업장 세션 접근을 차단한다(무음 폴백 금지).
 * <p>USER_TYPE_CD='REGULAR' 고정(MVP).
 */
public record TbmSessionQuery(
    String cmpnyCd
    , String siteCd
    , String sessionCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    /**
     * 세션/출결 조회 Query 생성.
     * <p>cmpnyCd / siteCd / sessionCd 는 필수. siteCd 부재(token 미보유) 시 무음 폴백 없이 거부한다.
     */
    public static TbmSessionQuery from(String cmpnyCd, String siteCd, String sessionCd, String userCd) {

        // 조직 스코프 키(cmpnyCd/siteCd) 또는 세션키 부재 시 즉시 거부(무음 폴백 금지).
        if (cmpnyCd == null || cmpnyCd.isBlank()
                || siteCd == null || siteCd.isBlank()
                || sessionCd == null || sessionCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmSessionQuery(cmpnyCd, siteCd, sessionCd, userCd, USER_TYPE_REGULAR);
    }
}
