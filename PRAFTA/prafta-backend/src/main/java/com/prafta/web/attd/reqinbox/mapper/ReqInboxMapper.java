package com.prafta.web.attd.reqinbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;

/**
 * 요청 승인 관리 통합 대기요청 조회 Mapper (prafta-019 후속).
 */
@Mapper
public interface ReqInboxMapper {

    /**
     * 매니저 스코프(회사+사업장) 내 대기('01') 요청 목록.
     *
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03)
     */
    List<PendingReqResult> selectPendingRequests(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("siteCd") String siteCd,
                                                 @Param("reqTypes") List<String> reqTypes);

    /**
     * 스케줄 수정('10') 대기 목록 + 현재→요청 스케줄 비교값.
     *
     * <p>기존 {@link #selectPendingRequests} 와 컬럼 세트가 달라 statement 를 분리한다
     * (기존 3탭 무회귀 — plan 결정 B). 스코프는 동일하게 CMPNY_CD(토큰) + SITE_CD(토큰).
     *
     * @param reqType 조회 대상 REQ_TYPE (AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY)
     */
    List<PendingSchedReqResult> selectPendingSchedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCd") String siteCd,
                                                           @Param("reqType") String reqType);
}
