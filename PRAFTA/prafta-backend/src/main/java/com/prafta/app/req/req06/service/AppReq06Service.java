package com.prafta.app.req.req06.service;

import com.prafta.app.req.req06.application.param.MyReqListParam;
import com.prafta.app.req.req06.dto.response.MyReqListResponse;

/**
 * prafta-app-006: 본인 요청 목록 조회 서비스.
 *
 * <p>인증/권한은 컨트롤러+param.from 단계에서 처리되며, 본 서비스는 식별값을 신뢰한다.
 */
public interface AppReq06Service {

    /**
     * 본인 요청 목록 페이지 조회.
     *
     * <p>응답 가공: REQ_TYPE/REQ_STATUS 라벨 매핑, 요일/날짜 한국어 디스플레이, summary.lines 단순 가공.
     */
    MyReqListResponse selectMyReqList(MyReqListParam param);
}
