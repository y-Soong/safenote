package com.prafta.app.approval.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P2-B4: 앱 관리자 승인 처리(A-3) 응답.
 *
 * <p>처리 결과 요약(에코). 상세/대기 목록 재조회는 프론트가 별도 호출한다.
 */
@Getter
@Builder
public class ApprovalProcessResponse {

    private final String reqId;
    private final String group;
    private final String decision;
    /** 정상 처리(트랜잭션 커밋) 여부. 실패는 예외(409/403/400)로 전달된다. */
    private final boolean processed;
}
