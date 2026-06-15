package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-5: 자동배치 커밋 요청(관리자 확인본 proposal).
 *
 * <p>프론트는 프리뷰 응답 proposal 을 그대로 {@code { proposal: {...} }} 로 되돌린다. 서버는 assignments
 * (userCd+ymds)만 신뢰 입력으로 사용하고, 각 사용자 스코프/권한·등록 가드는 공용 등록 헬퍼와 메타
 * 재조회로 재검증한다(IDOR/TOCTOU). dailyLoad/peakLoad/shortages 는 표시값이라 커밋에 사용하지 않는다.
 */
@Getter
@Setter
public class AutoBatchCommitRequest {

    /** 프리뷰 proposal(assignments 만 사용). */
    private Proposal proposal;

    @Getter
    @Setter
    public static class Proposal {
        private List<AssignmentItem> assignments;
    }

    @Getter
    @Setter
    public static class AssignmentItem {
        private String userCd;
        private List<String> ymds;
    }
}
