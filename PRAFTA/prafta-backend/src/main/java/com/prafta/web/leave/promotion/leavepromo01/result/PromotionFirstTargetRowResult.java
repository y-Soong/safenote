package com.prafta.web.leave.promotion.leavepromo01.result;

import java.math.BigDecimal;

/**
 * 연차 사용촉진 1차 현황 raw 1행 결과(매퍼 전용).
 *
 * <p>★ MyBatis record 매핑 — <b>SELECT 컬럼 순서 = 생성자 인자 순서</b>(위치 기반, 메모리
 * feedback_mybatis_record_column_order). 컬럼 추가/삭제 시
 * {@code WebLeavePromo01Mapper.xml selectFirstTargets} 의 SELECT 를 동일 위치로 맞출 것.
 *
 * <p>본 record 는 <b>DB 원본값만</b> 담는다. 제출 기한·D-day·상태 4분류 등 파생값은 서비스가 Java 로
 * 계산해 {@code PromotionFirstTargetView} 로 옮긴다(record 순서 함정을 파생 계산에서 격리).
 *
 * <p>{@code stage1DesignatedDays} 는 {@code submittedYn} 파생 전용이며 <b>응답/화면에 노출하지 않는다</b>
 * (확정 D1 — 2차 발동 요건은 미통보이지 지정 일수가 아니다).
 */
public record PromotionFirstTargetRowResult(
        String userCd,
        String userNm,
        String nodeCd,
        String nodeNm,
        String siteCd,
        String siteNm,
        String noticedDate,
        String baseAvailToDate,
        BigDecimal stage1DesignatedDays,
        String loginNotifiedYn,
        String firstSubmitDateRaw,
        Integer remindCntRaw,
        String lastRemindDateRaw
) {
}
