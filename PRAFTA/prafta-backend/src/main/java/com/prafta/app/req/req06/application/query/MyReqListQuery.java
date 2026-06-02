package com.prafta.app.req.req06.application.query;

import java.util.List;

import com.prafta.app.req.req06.application.param.MyReqListParam;

/**
 * prafta-app-006: mapper 입력 쿼리. {@link MyReqListParam} 에서 가공해 전달한다.
 *
 * <p>sort 는 mapper xml 의 &lt;choose&gt; 분기에 그대로 사용 (PENDING_FIRST/RECENT/TARGET_DATE).
 *
 * <p>limit/offset 은 페이지네이션. {@code hasMore} 판정용으로 mapper 는 limit+1 행을 가져온다.
 */
public record MyReqListQuery(
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

    public static MyReqListQuery from(MyReqListParam param) {
        return new MyReqListQuery(
                param.cmpnyCd(), param.siteCd(), param.userCd(),
                param.reqTypes(), param.reqStatuses(),
                param.targetYmdFrom(), param.targetYmdTo(),
                param.sort(),
                param.offset(),
                // hasMore 판정을 위해 1행 더 요청. Service 가 응답 직전에 마지막 행 잘라낸다.
                param.limit() + 1
        );
    }
}
