package com.prafta.common.cmm.dailyjoin.application.query;

import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;

/**
 * 일일사용자 회원가입 - 사용자ID 중복체크 쿼리.
 */
public record UserIdDupleCheckQuery(
    String cmpnyCd
    , String userId
) {
    public static UserIdDupleCheckQuery from(UserIdDupleCheckParam param) {
        return new UserIdDupleCheckQuery(
            param.cmpnyCd()
            , param.userId()
        );
    }

    public static UserIdDupleCheckQuery of(String cmpnyCd, String userId) {
        return new UserIdDupleCheckQuery(cmpnyCd, userId);
    }
}
