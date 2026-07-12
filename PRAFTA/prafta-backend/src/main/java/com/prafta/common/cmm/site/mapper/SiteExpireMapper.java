package com.prafta.common.cmm.site.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * PRAFTA-COM-001-T2-3 — 사업장(TB_SITE) 만료 비활성 배치 매퍼.
 */
@Mapper
public interface SiteExpireMapper {

    /**
     * 종료일이 도래(END_DATE &lt;= 오늘) AND 활성(USE_YN='Y') 인 사업장을 USE_YN='N' 으로 일괄 전이한다.
     *
     * <p>이미 'N' 인 행, 종료일 미설정(NULL) 행은 조건에서 제외되어 영향 0(멱등).
     *
     * @param todayYmd 서버 기준 오늘(YYYYMMDD)
     * @param updateNo 수정자(시스템 배치 = "SYSTEM")
     * @return 전이된 행 수
     */
    int updateExpireOverdueSites(@Param("todayYmd") String todayYmd, @Param("updateNo") String updateNo);
}
