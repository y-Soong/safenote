package com.prafta.common.cmm.leave.promotion.service;

import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;

/**
 * 연차 사용촉진 1차 통지 배치 서비스 (PRAFTA-COM-008-A-2).
 *
 * <p>1차 도래자에게 ① 촉진 진행 마스터({@code tb_leave_promotion_log}) 1행 멱등 적재
 * (STATUS=NOTICED, NOTICED_DATE=오늘) + ② SYS045 {@code LEAVE_PROMOTION_NOTICE} PUSH outbox
 * 적재(com-002 워커 게이트 off → 적재까지)를 수행한다.
 *
 * <p>멱등: DEDUP_KEY(회차 키)로 같은 사용자·같은 회차 중복 통지 차단. 같은 회차 1통(§6).
 *
 * <p>PII: 본문 합성에 평문 USER_NM 만 사용(복호화 금지).
 */
public interface LeavePromotionNotiService {

    /**
     * 1차 도래자 1명에 대해 마스터 기록 + PUSH outbox 적재(멱등).
     *
     * <p>이미 같은 회차로 통지된 경우(DEDUP_KEY 충돌) 흡수하고 no-op. 내부에서 예외를 흡수해
     * 배치 다음 사용자를 막지 않는다.
     *
     * @param target   1차 도래자(stage=FIRST 가정)
     * @param today    기준일(YYYYMMDD) — NOTICED_DATE
     * @param insertNo 등록자(배치=SYSTEM)
     * @return 신규 통지 발송(적재) 1, 멱등 스킵/실패 0
     */
    int notifyFirstPromotion(PromotionTargetResult target, String today, String insertNo);
}
