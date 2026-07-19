package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm: 세션 단건 상세 조회(A4 참석자/A5 상태/A6 콘텐츠/A7 위험성/A10 완료) 공용 Query.
 *
 * <p>식별자(cmpnyCd/siteCd/userCd)는 token 출처(IDOR 차단). USER_TYPE_CD='REGULAR' 고정.
 *
 * <p><b>PRAFTA-SUBCON-T5</b>: 세션 키({@code sessionCmpnyCd} = 개설사)와 출결 키({@code cmpnyCd} =
 * 내 회사)를 분리한다. 세션/콘텐츠/위험성/자료는 개설사 기준으로, 출결은 내 회사 기준으로 조회한다.
 * 타 세션 접근 차단은 서비스 진입부의 {@code TbmSessionShareService.assertViewable} 이 담당한다
 * (자사 세션은 사업장 스코프 유지, 타사 세션은 회사 단위 지정이라 사업장 검사 없음 — plan D4).
 */
public record TbmDetailQuery(
    String cmpnyCd
    , String sessionCmpnyCd
    , String siteCd
    , String sessionCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    public static TbmDetailQuery of(String cmpnyCd, String sessionCmpnyCd, String siteCd,
            String sessionCd, String userCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || sessionCd == null || sessionCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String hostCmpnyCd = (sessionCmpnyCd == null || sessionCmpnyCd.isBlank())
                ? cmpnyCd : sessionCmpnyCd;

        return new TbmDetailQuery(cmpnyCd, hostCmpnyCd, siteCd, sessionCd, userCd, USER_TYPE_REGULAR);
    }
}
