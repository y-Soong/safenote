package com.prafta.web.attd.reqinbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.reqinbox.result.PendingReqResult;

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
}
