package com.prafta.common.cmm.leave.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.LeaveGrantStatusVO;

/**
 * tb_user_leave_grant STATUS 동기화 전용 Mapper.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 */
@Mapper
public interface LeaveGrantStatusMapper {

    /**
     * PK 기준 단건 조회. STATUS 재평가 입력값.
     *
     * @return 행이 없으면 null
     */
    LeaveGrantStatusVO selectGrantById(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("grantId") String grantId);

    /**
     * tb_user_leave_use 기준 USED_DAYS 합계 산출.
     *
     * <p>필터:
     * <ul>
     *   <li>{@code GRANT_ID} 일치</li>
     *   <li>{@code CMPNY_CD} 일치</li>
     *   <li>{@code DEL_YN='N'} (사용 이력 보존 정책으로 인해 삭제는 없지만 안전장치)</li>
     * </ul>
     *
     * <p>TODO(developer): LEAVE_STATUS 컬럼의 코드값 종류가 정책서에 명시되지 않아
     * 본 집계에서는 LEAVE_STATUS 필터를 적용하지 않는다.
     * Attd_09(연차 사용 등록/취소) 화면 작업 시 LEAVE_STATUS 코드 카탈로그가 확정되면
     * 본 SQL에 {@code AND LEAVE_STATUS NOT IN (...)} 필터 추가 필요.
     *
     * @return 행이 0건이면 0
     */
    BigDecimal selectUsedDaysSum(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("grantId") String grantId);

    /**
     * STATUS + EXPIRE_YN + DEL_YN을 동시에 UPDATE.
     * 단방향 동기화 원칙(§8.5.8)에 따라 항상 함께 갱신한다.
     */
    int updateStatusWithSync(@Param("cmpnyCd") String cmpnyCd,
                             @Param("grantId") String grantId,
                             @Param("status") String status,
                             @Param("expireYn") String expireYn,
                             @Param("delYn") String delYn,
                             @Param("updateBy") String updateBy);

    /**
     * USED_DAYS만 단독 UPDATE. STATUS 재평가 직전에 사용.
     */
    int updateUsedDays(@Param("cmpnyCd") String cmpnyCd,
                       @Param("grantId") String grantId,
                       @Param("usedDays") BigDecimal usedDays,
                       @Param("updateBy") String updateBy);

    /**
     * 만료 배치: AVAIL_TO_DATE가 오늘 미만이면서 STATUS='ACTIVE'인 행을 EXPIRED로 일괄 전이.
     *
     * <p>AVAIL_TO_DATE 타입은 varchar(8) YYYYMMDD 이므로 {@code DATE_FORMAT(NOW(),'%Y%m%d')}로 직접 문자열 비교한다.
     *
     * @return 처리된 행 수
     */
    int expireOverdueGrants();
}
