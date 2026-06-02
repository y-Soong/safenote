package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 기준 부여 프리뷰(dry-run) 응답 (prafta-022 작업 D).
 * POST /attd09/leave-grant/policy-grant/preview.
 *
 * <p>적용 전 집계 프리뷰. 요약(선택/신규부여/변경없음)과 직원별 행을 반환한다.
 *
 * <p>prafta-032 D6: 입사일 변경 처리방식 자동계산(KEEP 계열/RESET_ALL) 폐기로
 * "재발급(reissueCount)" 요약과 행의 "처리방식(handlingType)"·"취소예정(cancelCount)"을 제거했다.
 */
@Value
@Builder
public class PolicyGrantPreviewResponse {

    /** 선택된 직원 수 */
    int selectedCount;

    /** 신규 부여 직원 수(추가 일수>0) */
    int newGrantCount;

    /** 변경 없음 직원 수(추가 일수==0) */
    int noChangeCount;

    /** 직원별 프리뷰 행 */
    List<Row> rows;

    /** 직원 1명 프리뷰 행. */
    @Value
    @Builder
    public static class Row {
        /** 대상 직원 코드 */
        String userCd;

        /** 실제 신규 부여될 일수 합(멱등 skip 분 제외) */
        int addDays;

        /** 제외/안내 사유(없으면 null) */
        String note;
    }
}
