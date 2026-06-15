package com.prafta.common.cmm.dailylogin.application.query;

import java.util.Objects;

import com.prafta.common.cmm.dailylogin.application.param.DailyLoginParam;

/**
 * PRAFTA-app-027-2 — 일용직 로그인 조회 조건.
 *
 * <p>cmpnyCd 는 nullable — 매퍼에서 입력 시에만 AND CMPNY_CD 제약을 추가한다.
 */
public record DailyLoginQuery(
    String userId
    , String cmpnyCd
) {
    public static DailyLoginQuery from(DailyLoginParam param) {
        Objects.requireNonNull(param, "param is required");

        return new DailyLoginQuery(
            param.userId()
            , param.cmpnyCd()
        );
    }
}
