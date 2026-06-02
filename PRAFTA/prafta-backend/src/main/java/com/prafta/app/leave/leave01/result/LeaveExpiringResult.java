package com.prafta.app.leave.leave01.result;

import java.math.BigDecimal;

/**
 * prafta-app-005: 소멸 임박(D-30) 집계 결과(그룹 무관, 활성집합 동일).
 * <p>대상: 활성 부여(STATUS='ACTIVE' AND EXPIRE_YN='N' AND DEL_YN='N')
 *   AND (GRANT_DAYS-USED_DAYS) &gt; 0
 *   AND AVAIL_TO_DATE BETWEEN todayYmd AND today+30일.
 * <p>매핑(AppLeave01Mapper.selectExpiringSoon):
 * <pre>
 *   COUNT(*)                      AS targetCount        (1건 이상이면 exists=true)
 *   MIN(AVAIL_TO_DATE)            AS nearestExpiryYmd   (가장 임박한 소멸일)
 *   DATEDIFF(MIN(AVAIL_TO_DATE), today) AS daysUntilExpiry
 *   SUM(GRANT_DAYS - USED_DAYS)   AS totalRemainingDays
 * </pre>
 * 대상이 0건이면 targetCount=0, 나머지 null → 서비스에서 exists=false 로 처리.
 */
public record LeaveExpiringResult(
    int targetCount
    , String nearestExpiryYmd
    , Integer daysUntilExpiry
    , BigDecimal totalRemainingDays
) {
}
