package com.prafta.common.cmm.leave.promotion.autobatch;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-5: 자동배치 대상 사용자 1명의 입력 메타 운반체.
 *
 * <p>{@code LeaveAutoBatchMapper.selectAutoBatchTargets} 결과 — 2차 도래(SECOND 마스터 보유) 대상자의
 * 잔여(r_i)·본연차 사용가능 구간(availFrom~availTo)을 싣는다. 가용일 산출({@link AssignableDateResolver})과
 * 전략(YEAR_END/MIN_OVERLAP)의 공통 입력이다.
 *
 * <p>잔여(remainingDays) = 본연차(STATUTORY_ANNUAL)+근속가산(STATUTORY_TENURE_BONUS) ACTIVE
 * (GRANT_DAYS-USED_DAYS) 합(미래 등록분=USED 반영, 확정-1). 1일 단위 정수 환산은 서비스가 수행한다.
 */
@Getter
@Setter
public class AutoBatchTargetVO {

    /** 사용자 코드 */
    private String userCd;

    /** 사업장 코드(대상자 소속, 서버 조회) */
    private String siteCd;

    /** 미지정 잔여 일수(r_i, 본연차+근속가산 ACTIVE 잔여 합) */
    private BigDecimal remainingDays;

    /** 본연차 사용가능 시작일 (YYYYMMDD, 가장 임박 grant) */
    private String availFromDate;

    /** 본연차 사용가능 종료일 = 만료일 (YYYYMMDD, 가장 임박 grant) */
    private String availToDate;
}
