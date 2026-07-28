package com.prafta.common.cmm.leave.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.RemnantCoverInsertVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverListRowVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverRowVO;
import com.prafta.common.cmm.leave.vo.RemnantDeductibleGrantVO;
import com.prafta.common.cmm.leave.vo.RemnantLeaveUseVO;
import com.prafta.common.cmm.leave.vo.RemnantPolicyVO;
import com.prafta.common.cmm.leave.vo.RemnantUserRemainVO;

/**
 * PC-05/06/07: 짜투리 잔여 보전 전용 Mapper (발동 D5·D6 / 회수 D7 / 집계·리포트 D9-②③).
 *
 * <p>use 행 INSERT·채번·GRANT 재집계는 웹/앱 매퍼와 동일 SQL 을 매퍼별 보유한다
 * (common → web 참조 금지, LeaveHourlyResettleMapper 선례).
 *
 * <p>정책서: attd/08-leave.md §8.5.5(대상 5종)·§8.5.7(권한)·§8.5.8(이력 보존)
 */
@Mapper
public interface LeaveRemnantCoverMapper {

    /** 활성 정책의 짜투리 보전 옵션 + 사용 단위. 활성 정책 미존재 시 null(발동 비대상 fail-closed). */
    RemnantPolicyVO selectRemnantPolicy(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 대상 5종 활성 부여(잔여&gt;0, 대상일 유효, 만료 임박순, FOR UPDATE) —
     * 발동(D6)·회수(D7)의 분할 차감 대상. LEAVE_CD 를 함께 싣는다(use 행 귀속용).
     */
    List<RemnantDeductibleGrantVO> selectRemnantDeductibleGrants(@Param("cmpnyCd") String cmpnyCd,
                                                                 @Param("userCd") String userCd,
                                                                 @Param("workYmd") String workYmd,
                                                                 @Param("leaveCds") List<String> leaveCds);

    /**
     * D5-ⓔ: 대상 5종 CONFIRMED 사용기록 중 실사용일 미도래(START_DATE &gt;= 오늘) 건수.
     * 0 초과면 발동 비대상(먼저 예정 연차부터 소진).
     */
    int countUpcomingLeaveUse(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd,
                              @Param("fromYmd") String fromYmd,
                              @Param("leaveCds") List<String> leaveCds);

    /** 사용기록 ID 채번 (LeaveFlowMapper.selectNextLeaveId 미러 — 동일 시퀀스 공유, PK 충돌 방지). */
    String selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

    /** 보전 이력 ID 채번 (RC + YYYYMMDD + 시퀀스 'LEAVE_REMNANT_COVER'). */
    String selectNextCoverId(@Param("cmpnyCd") String cmpnyCd);

    /** use 행 INSERT (LeaveFlowMapper.insertLeaveUse 미러 — 매퍼별 보유). */
    int insertLeaveUse(RemnantLeaveUseVO vo);

    /** TB_LEAVE_REMNANT_COVER INSERT (발동 시 회사 부담분 기록, ACTIVE). */
    int insertCover(RemnantCoverInsertVO vo);

    /** USED_DAYS 재집계 (LeaveFlowMapper.recomputeGrantUsedDays 미러 — 매퍼별 보유). */
    int recomputeGrantUsedDays(@Param("cmpnyCd") String cmpnyCd,
                               @Param("grantId") String grantId,
                               @Param("updateNo") String updateNo);

    /** D7 회수 판정 대상: 사용자의 ACTIVE COVER 전량(WORK_YMD ASC, FOR UPDATE). */
    List<RemnantCoverRowVO> selectActiveCovers(@Param("cmpnyCd") String cmpnyCd,
                                               @Param("userCd") String userCd);

    /**
     * D7 회수 반영: COVER_DAYS/COVER_MINUTES 갱신 + 전액 회수 시 상태 RECLAIMED·RECLAIM_DATE 기록.
     * {@code reclaimed}=true 일 때만 상태 전이(부분 회수는 ACTIVE 유지).
     */
    int updateCoverReclaim(@Param("cmpnyCd") String cmpnyCd,
                           @Param("coverId") String coverId,
                           @Param("coverDays") BigDecimal coverDays,
                           @Param("coverMinutes") int coverMinutes,
                           @Param("reclaimed") boolean reclaimed,
                           @Param("updateNo") String updateNo);

    /** D9-②: 연간 회사 부담 상세 목록 (COVER_DAYS&gt;0, 사용자명 조인 — 관리자 화면 한정). */
    List<RemnantCoverListRowVO> selectCoverSummaryRows(@Param("cmpnyCd") String cmpnyCd,
                                                       @Param("year") String year);

    /**
     * D9-③: 사용자별 대상 5종 합산 잔여(오늘 유효 부여, 잔여&gt;0) + 최근접 소멸일 —
     * 활성 사용자 한정. 최소단위 요금 미만 필터·절사 끝수 구분은 서비스 계층에서 수행.
     */
    List<RemnantUserRemainVO> selectRemnantRemainByUser(@Param("cmpnyCd") String cmpnyCd,
                                                        @Param("todayYmd") String todayYmd,
                                                        @Param("leaveCds") List<String> leaveCds);

    /** N9 직렬화 advisory lock 획득 (leaveRemnant:{cmpny}:{user}). 1=획득, 0=타임아웃, null=오류. */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** advisory lock 해제. */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);
}
