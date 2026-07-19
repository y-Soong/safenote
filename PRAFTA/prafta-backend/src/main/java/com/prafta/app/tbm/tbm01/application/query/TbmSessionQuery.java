package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C: 세션/출결 조회 공용 Query(mapper 진입).
 *
 * <p>cmpnyCd / siteCd 는 token 출처(조직 스코프 IDOR 차단). userCd 는 본인 출결 조회 시 사용한다.
 * <p>USER_TYPE_CD='REGULAR' 고정(MVP).
 *
 * <p><b>PRAFTA-SUBCON-T5</b>: 세션 키와 출결 키를 분리한다.
 * <ul>
 *   <li>{@code sessionCmpnyCd} = 세션 개설사(TbmSessionShareService.assertViewable 이 돌려준 값).
 *       세션/콘텐츠/위험성/자료 조회에 쓴다.</li>
 *   <li>{@code cmpnyCd} = 내 회사(토큰). 출결행 조회/기록에 쓴다.</li>
 * </ul>
 * 자사 세션이면 둘이 같다. 타사(지정 체인) 세션이면 다르다. 세션 조회의 사업장 강제는 공통 게이트가
 * 대체하므로(자사 세션에 한해 사업장 스코프 유지) SQL 에서는 SITE_CD 조건을 걸지 않는다.
 */
public record TbmSessionQuery(
    String cmpnyCd
    , String sessionCmpnyCd
    , String siteCd
    , String sessionCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    /**
     * 세션/출결 조회 Query 생성.
     *
     * @param sessionCmpnyCd 세션 개설사(게이트 통과값). null 이면 자사 세션으로 간주.
     */
    public static TbmSessionQuery of(String cmpnyCd, String sessionCmpnyCd, String siteCd,
            String sessionCd, String userCd) {

        // 조직 스코프 키(cmpnyCd) 또는 세션키 부재 시 즉시 거부(무음 폴백 금지).
        if (cmpnyCd == null || cmpnyCd.isBlank()
                || sessionCd == null || sessionCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String hostCmpnyCd = (sessionCmpnyCd == null || sessionCmpnyCd.isBlank())
                ? cmpnyCd : sessionCmpnyCd;

        return new TbmSessionQuery(cmpnyCd, hostCmpnyCd, siteCd, sessionCd, userCd, USER_TYPE_REGULAR);
    }
}
