package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.LeaveConversionPolicyVO;

/**
 * TB_LEAVE_CONVERSION_POLICY 전용 Mapper (연차 시간차 환산 개편 LC-02).
 *
 * <p>회사 단위 "1일 환산시간(분)" + 적용일 이력(effective-dating, F4).
 * DDL: {@code sql/migration/prafta-leave-conv-1-ddl.sql} (LC-01, 별도 세션 산출).
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 T0·F4 / plan §1(테이블 확정)·§2 LC-02
 */
@Mapper
public interface LeaveConversionPolicyMapper {

    /**
     * 신청 대상일(WORK_YMD) 기준 유효 환산시간(분) 조회.
     * {@code APPLY_FROM_DATE <= workYmd} 최신 1행. 행 미존재 시 {@code null}
     * (폴백 480은 서비스 레이어 — 시드 불필요, 8시간 사업장 결과 불변).
     */
    Integer selectConversionMinutes(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("workYmd") String workYmd);

    /** 회사의 환산시간 변경 이력 전체(적용일 내림차순). */
    List<LeaveConversionPolicyVO> selectConversionHistory(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 같은 적용일 행 값 갱신(재저장 = UPDATE). 대상 없으면 0 → 호출부에서 INSERT.
     *
     * @return UPDATE 된 행 수
     */
    int updateConversionPolicy(@Param("cmpnyCd") String cmpnyCd,
                               @Param("applyFromDate") String applyFromDate,
                               @Param("dailyConvMinutes") int dailyConvMinutes,
                               @Param("updateNo") String updateNo);

    /** 신규 적용일 행 INSERT. PK(CMPNY_CD, APPLY_FROM_DATE) 충돌 시 DuplicateKeyException. */
    int insertConversionPolicy(@Param("cmpnyCd") String cmpnyCd,
                               @Param("applyFromDate") String applyFromDate,
                               @Param("dailyConvMinutes") int dailyConvMinutes,
                               @Param("insertNo") String insertNo);
}
