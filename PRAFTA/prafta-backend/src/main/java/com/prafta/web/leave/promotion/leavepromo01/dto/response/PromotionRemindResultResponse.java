package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 1차 독촉 발송 결과 응답(발송/스킵/실패 집계 + 스킵 사유).
 *
 * <p>PII 최소화: 스킵 목록에 <b>근로자명을 담지 않는다</b>(화면이 이미 조회 결과로 이름을 보유).
 */
@Getter
@Builder
public class PromotionRemindResultResponse {

    /** 실제 outbox 적재(발송 예약) 건수. */
    private int sentCount;

    /** 가드로 건너뛴 건수(사유별 목록은 skippedItems). */
    private int skippedCount;

    /** 처리 중 오류로 실패한 건수. */
    private int failedCount;

    /** 스킵 상세(사유 코드/라벨). 이름 미포함. */
    private List<SkippedItem> skippedItems;

    /** 스킵 1건. */
    @Getter
    @Builder
    public static class SkippedItem {

        /** 대상 근로자 코드. */
        private String userCd;

        /**
         * 스킵 사유 코드: NOT_TARGET(1차 대상 아님/스코프 밖) / NO_AUTH(부서 관리 권한 없음) /
         * ROUND_CLOSED(촉진 회차 종료) / ALREADY_SUBMITTED(이미 제출) /
         * ALREADY_REMINDED_TODAY(오늘 이미 발송).
         */
        private String reasonCd;

        /** 스킵 사유 라벨(한국어). */
        private String reasonNm;
    }
}
