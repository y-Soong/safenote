package com.prafta.common.cmm.dailyjoin.application.query;

import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;

/**
 * 일일사용자 회원가입 - 사용자ID 중복체크 쿼리.
 *
 * <p>prafta-app-032 A: 제출(insertDailyUser) 단계의 전상태 중복검사는 휴대폰 기반 재활성 대상 행
 * (reuseUserCd)을 제외해야 한다(같은 휴대폰 복귀자가 자기 옛 아이디를 그대로 재활성하는 케이스 보호).
 * 프리체크(checkUserIdDuple)는 휴대폰 컨텍스트가 없어 reuseUserCd=null 로 호출한다.
 */
public record UserIdDupleCheckQuery(
    String cmpnyCd
    , String userId
    , String reuseUserCd
) {
    public static UserIdDupleCheckQuery from(UserIdDupleCheckParam param) {
        return new UserIdDupleCheckQuery(
            param.cmpnyCd()
            , param.userId()
            , null
        );
    }

    public static UserIdDupleCheckQuery of(String cmpnyCd, String userId) {
        return new UserIdDupleCheckQuery(cmpnyCd, userId, null);
    }

    /** prafta-app-032 A — 제출 단계 전상태 검사용(재활성 대상 reuseUserCd 제외). */
    public static UserIdDupleCheckQuery of(String cmpnyCd, String userId, String reuseUserCd) {
        return new UserIdDupleCheckQuery(cmpnyCd, userId, reuseUserCd);
    }
}
