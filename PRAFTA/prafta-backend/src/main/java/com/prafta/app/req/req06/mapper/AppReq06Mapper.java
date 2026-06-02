package com.prafta.app.req.req06.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.req.req06.application.query.MyReqListQuery;
import com.prafta.app.req.req06.result.MyReqItemResult;

/**
 * prafta-app-006: 본인 요청 목록 mapper.
 *
 * <p>모든 SQL 은 회사/사업장/사용자 스코프(CMPNY_CD + SITE_CD + USER_CD) + DEL_YN='N'
 * + REQ_TYPE IN ('01'~'06') 을 고정 적용한다(시스템 코드 07/08/09 응답 차단).
 */
@Mapper
public interface AppReq06Mapper {

    /** 본인 전체 건수 (필터 무관) — 06 SYS032 6종 한정. */
    int selectMyTotalCount(@Param("cmpnyCd") String cmpnyCd,
                           @Param("siteCd") String siteCd,
                           @Param("userCd") String userCd);

    /** 필터 적용 후 총합. */
    int selectMyFilteredCount(MyReqListQuery query);

    /** 페이지 행 조회 (limit+1 행으로 hasMore 판정). */
    List<MyReqItemResult> selectMyReqPage(MyReqListQuery query);

    /** SYS032 / SYS033 라벨 일괄 조회. key="SYS코드:디테일코드" 형식이 아니라 별도 호출 권장. */
    List<Map<String, String>> selectSystValDLabels(@Param("systValCd") String systValCd);
}
