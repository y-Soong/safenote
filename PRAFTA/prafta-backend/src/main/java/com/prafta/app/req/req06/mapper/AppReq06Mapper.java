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
 * <p>모든 SQL 은 회사/사업장/사용자 스코프(CMPNY_CD + SITE_CD + USER_CD) + DEL_YN='N' 을 고정 적용하며,
 * TB_USER_ATTD_REQ(REQ_TYPE IN '01'~'06','10', 시스템 코드 07/08/09 응답 차단)와
 * TB_LEAVE_CHANGE_REQUEST(근로자 본인 발의 연차 이동/삭제, reqType 합성값 'LC_MOVE'/'LC_DELETE')를
 * UNION ALL 로 통합한 파생 테이블(myReqUnionSource) 위에서 필터/정렬/페이징한다(prafta-내승인요청연차통합-1).
 */
@Mapper
public interface AppReq06Mapper {

    /** 본인 전체 건수 (필터 무관) — TB_USER_ATTD_REQ 01~06,10 + TB_LEAVE_CHANGE_REQUEST(WORKER 발의) 통합. */
    int selectMyTotalCount(@Param("cmpnyCd") String cmpnyCd,
                           @Param("siteCd") String siteCd,
                           @Param("userCd") String userCd);

    /** 필터 적용 후 총합. */
    int selectMyFilteredCount(MyReqListQuery query);

    /** 페이지 행 조회 (limit+1 행으로 hasMore 판정). */
    List<MyReqItemResult> selectMyReqPage(MyReqListQuery query);

    /** SYS032 / SYS033 라벨 일괄 조회. key="SYS코드:디테일코드" 형식이 아니라 별도 호출 권장. */
    List<Map<String, String>> selectSystValDLabels(@Param("systValCd") String systValCd);

    /**
     * PRAFTA-내승인요청결재라인-1: 결재라인 상세 조회 전 소유권 검증(IDOR 가드).
     *
     * <p>{@code reqId} 가 본인 소유의 TB_USER_ATTD_REQ 행인지 확인한다. 0건이면 결재라인 상세를
     * 조회하지 않고 서비스 단계에서 즉시 403 처리한다. reqId 가 LC 접두(연차 이동/삭제 합성 ID)이면
     * TB_USER_ATTD_REQ 에 없으므로 자연히 0건 → 403(방어용).
     */
    int existsMyReqId(@Param("cmpnyCd") String cmpnyCd,
                      @Param("siteCd") String siteCd,
                      @Param("userCd") String userCd,
                      @Param("reqId") String reqId);
}
