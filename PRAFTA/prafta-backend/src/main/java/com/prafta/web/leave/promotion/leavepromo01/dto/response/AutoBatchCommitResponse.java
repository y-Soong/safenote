package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-5: 자동배치 커밋 결과(부분 성공 보고).
 *
 * <p>프리뷰~커밋 사이 변동(이미 등록·마감·잔여부족 등)은 멱등/가드로 흡수되어 사용자별로 집계된다.
 * 전체 신규 0건이면 컨트롤러가 400 으로 보고하거나(아래 totalDesignated=0) 본 결과로 안내한다.
 *
 * @param totalDesignated 신규 등록(REGISTERED) 합계
 * @param totalSkipped    멱등 스킵(이미 등록) 합계
 * @param totalFailed     실패(잔여부족/마감/비근무일) 합계
 * @param userResults     사용자별 결과
 */
@Getter
@Builder
public class AutoBatchCommitResponse {

    private final int totalDesignated;
    private final int totalSkipped;
    private final int totalFailed;
    private final List<UserResult> userResults;

    @Getter
    @Builder
    public static class UserResult {
        private final String userCd;
        private final List<String> designatedDates;
        private final List<String> skippedDates;
        private final List<String> failedDates;
    }
}
