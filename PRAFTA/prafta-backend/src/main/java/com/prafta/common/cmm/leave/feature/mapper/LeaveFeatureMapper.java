package com.prafta.common.cmm.leave.feature.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 소정-06: 연차 기능 노출 판정 전용 Mapper.
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §연차 부여 on/off 토글
 * ("자동 부여 off + 부여 이력 0 인 회사는 연차 카드·신청 진입점 숨김").
 *
 * <p>SQL 규칙: leading comma, {@code #{...}} 바인딩, SELECT * 금지(명시 컬럼). MySQL 8.
 * 모든 조회는 CMPNY_CD 스코프로 격리한다.
 */
@Mapper
public interface LeaveFeatureMapper {

    /**
     * 회사에 연차 부여 이력이 1건이라도 있는지 여부.
     *
     * <p>"부여 이력"의 정의: 삭제되지 않은(DEL_YN='N') TB_USER_LEAVE_GRANT 행.
     * 법정/약정(GRANT_TYPE), 상태(ACTIVE/EXPIRED/EXHAUSTED/CANCELED)를 가리지 않는다 —
     * 한 번이라도 연차를 운영한 회사에서 화면을 숨기면 과거 이력 조회 자체가 막히기 때문이다.
     *
     * @return 존재하면 1, 없으면 0
     */
    int existsLeaveGrant(@Param("cmpnyCd") String cmpnyCd);
}
