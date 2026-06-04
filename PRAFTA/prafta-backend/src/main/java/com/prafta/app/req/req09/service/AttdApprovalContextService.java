package com.prafta.app.req.req09.service;

import com.prafta.app.req.req09.dto.response.ApprovalContextResponse;

/**
 * prafta-app-009-8: 근태 요청 폼 결재선 분기용 컨텍스트 조회 서비스(읽기 전용).
 *
 * <p>009-3 의 {@code AppReq09Mapper.selectAttdSelfApprvYn}/{@code selectIsNodeAdmin} 를 재사용하여
 * 신청자 소속 노드의 자체근태승인 분기값을 폼에 제공한다. 신규 매퍼/SQL 없음.
 */
public interface AttdApprovalContextService {

    /**
     * 신청자 소속 노드의 결재선 분기 컨텍스트 조회.
     *
     * @param cmpnyCd 회사 코드(JWT)
     * @param siteCd  사업장 코드(JWT)
     * @param userCd  신청자 사용자 코드(JWT)
     * @return selfApprvYn('Y'/'N' — null 은 'N' 정규화) + isNodeAdmin
     */
    ApprovalContextResponse getApprovalContext(String cmpnyCd, String siteCd, String userCd);
}
