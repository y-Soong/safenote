package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 변경 시 수동 연차 조정(추가/회수) 결과 (prafta-032 D4/D5).
 *
 * <p>입사일 UPDATE 트랜잭션 내에서 호출되며, 결과는 호출부(User01ServiceImpl)가
 * tb_user_hire_date_history 의 OLD_GRANT_TOTAL/NEW_GRANT_TOTAL/AFFECTED_GRANT_SNAPSHOT 기록에 사용한다.
 *
 * <ul>
 *   <li>{@code oldGrantTotal}      : 조정 전 현재 법정(STATUTORY_*) 부여 총량(ACTIVE 기준)</li>
 *   <li>{@code newGrantTotal}      : 조정 후 목표 법정 부여 총량(관리자 입력값)</li>
 *   <li>{@code diff}               : 차액(목표 − 현재). >0 추가, <0 회수, 0 무처리</li>
 *   <li>{@code addedDays}          : 실제 추가 부여된 총 일수(차액>0일 때)</li>
 *   <li>{@code withdrawnDays}      : 실제 회수된 총 일수(차액<0일 때, 양수)</li>
 *   <li>{@code recallableDays}     : 회수 가능량(잔여=ACTIVE 법정 GRANT_DAYS−USED_DAYS 합) — 차단 메시지용</li>
 *   <li>{@code canceledGrantCount} : 회수로 STATUS='CANCELED' 전환된 부여 건수</li>
 *   <li>{@code reducedGrantCount}  : 회수로 GRANT_DAYS 부분 차감된 부여 건수</li>
 *   <li>{@code addedGrantCount}    : 추가 부여로 신규 INSERT된 부여 건수</li>
 *   <li>{@code affectedSnapshotJson} : 영향받은 부여행 전/후 스냅샷 JSON(nullable)</li>
 * </ul>
 */
@Getter
@Builder
public class HireDateAdjustResultVO {

    private BigDecimal oldGrantTotal;
    private BigDecimal newGrantTotal;
    private BigDecimal diff;
    private BigDecimal addedDays;
    private BigDecimal withdrawnDays;
    private BigDecimal recallableDays;
    private int canceledGrantCount;
    private int reducedGrantCount;
    private int addedGrantCount;
    private String affectedSnapshotJson;
}
