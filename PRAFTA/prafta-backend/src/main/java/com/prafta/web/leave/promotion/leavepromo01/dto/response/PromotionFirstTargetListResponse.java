package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 사용촉진 1차 현황 조회 응답(행 목록 + 요약 카운트).
 *
 * <p>요약 카운트는 <b>회차 유효 필터(D8)를 통과한 행 기준</b>으로 서버가 산출한다. 화면은 뱃지에
 * 그대로 표기하며 재집계하지 않는다.
 */
@Getter
@Builder
public class PromotionFirstTargetListResponse {

    /** 1차 통지 대상자 목록(파생값 포함). 없으면 빈 목록. */
    private List<PromotionFirstTargetView> targetList;

    /** 요약 카운트 6종. */
    private Summary summary;

    /** 상단 요약 뱃지용 카운트(표시 전용). */
    @Getter
    @Builder
    public static class Summary {

        /** 대상 인원(회차 유효 대상자 수). */
        private int totalCount;

        /** 미제출(기한 내). */
        private int notSubmittedCount;

        /** 기한초과 미제출(2차 직권지정 예고 대상). */
        private int overdueNotSubmittedCount;

        /** 지연 통지(회사 귀책 표식). */
        private int lateNoticeCount;

        /** 제출완료(기한 내 + 기한후 제출 합). */
        private int submittedCount;

        /** 독촉 가능 인원(미제출자 수 — 일괄 독촉 버튼 활성 판정). */
        private int remindableCount;
    }
}
