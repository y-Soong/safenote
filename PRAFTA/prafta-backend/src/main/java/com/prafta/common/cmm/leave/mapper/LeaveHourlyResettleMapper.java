package com.prafta.common.cmm.leave.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.HourlyLeaveUseRowVO;

/**
 * 시간차 연차 취소·반려 재정산 전용 Mapper (연차 시간차 환산 개편 LC-05, F1).
 *
 * <p>정책서: attd/08-leave.md §8.5.8(이력 보존) / plan §2 LC-05-②(재정산 알고리즘)
 */
@Mapper
public interface LeaveHourlyResettleMapper {

    /**
     * 그날 잔존 시간차(02/03/04, CONFIRMED) 사용행 — 시간순(START_TIME, REQ_ID, LEAVE_ID) 재적용 대상.
     * 반차/반반차/종일은 고정요금이라 재정산 비대상(조회 제외).
     *
     * <p>PC-01(N1): REQ_ID(분할 INSERT 묶음 키)와 부여 만료일(AVAIL_TO_DATE, 묶음 내 배분 정렬 키)을
     * 함께 싣는다 — 분할행은 LEAVE_MINUTES 를 첫 행만 보유하므로 재정산은 REQ 묶음 합산으로 계산한다.
     */
    List<HourlyLeaveUseRowVO> selectHourlyLeaveRowsOnDate(@Param("cmpnyCd") String cmpnyCd,
                                                          @Param("userCd") String userCd,
                                                          @Param("workYmd") String workYmd);

    /**
     * PC-05: 그날 짜투리 보전(COVER)이 걸린 REQ_ID 목록 — 발동 건의 use 행 합은 "잔여 전액"이라
     * 정상 요금과 다르다. 재정산이 그 묶음을 정상 요금으로 재배분하면 잔여(0) 초과 차감이 되므로
     * 해당 묶음은 행 재배분을 건너뛴다(누적 분·가상 정상요금 누적에는 포함 — 후속 묶음 무영향).
     * COVER_STATUS 불문(부분/전액 회수분도 행 구성이 고정 의미).
     */
    List<String> selectRemnantCoveredReqIds(@Param("cmpnyCd") String cmpnyCd,
                                            @Param("userCd") String userCd,
                                            @Param("workYmd") String workYmd);

    /**
     * 재정산 결과 차감 일수 반영 — LEAVE_DAYS 만 변경(생성컬럼 DIRECT_USE_KEY 는 REQ_ID·상태·
     * 단위 기반이라 불변, 멱등키 충돌 없음 — plan §8 추가 확정).
     */
    int updateLeaveUseDays(@Param("cmpnyCd") String cmpnyCd,
                           @Param("leaveId") String leaveId,
                           @Param("leaveDays") BigDecimal leaveDays,
                           @Param("updateNo") String updateNo);

    /**
     * USED_DAYS = 해당 부여에 연결된 CONFIRMED 사용 합계 재집계.
     * LeaveFlowMapper.recomputeGrantUsedDays 와 동일 SQL(모듈 관례상 매퍼별 보유).
     */
    int recomputeGrantUsedDays(@Param("cmpnyCd") String cmpnyCd,
                               @Param("grantId") String grantId,
                               @Param("updateNo") String updateNo);

    /**
     * F5 직렬화 advisory lock 획득 — 신청 흐름(leaveDay 키)과 상호 배타.
     * 1=획득, 0=타임아웃, null=오류. 세션 단위 → 호출부가 트랜잭션 완료(afterCompletion)
     * 시점에 releaseAdvisoryLock (등록 불가 시 finally 폴백 — 보안리뷰 Medium).
     * (LeaveFlowMapper.getAdvisoryLock 미러)
     */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** advisory lock 해제. */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);
}
