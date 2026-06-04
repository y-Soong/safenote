package com.prafta.app.req.req09.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.app.req.req09.dto.response.ApprovalContextResponse;
import com.prafta.app.req.req09.mapper.AppReq09Mapper;
import com.prafta.app.req.req09.service.AttdApprovalContextService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-009-8: 근태 요청 폼 결재선 분기 컨텍스트 조회 구현(읽기 전용).
 *
 * <p>{@code AttdApprovalLineServiceImpl#applyApprovalFlow} 의 분기 판정과 동일한 매퍼 조회를
 * 신청 전 폼 단계에서 미리 수행하여, 결재선 섹션 노출/숨김을 결정하도록 메타만 반환한다.
 * 쓰기/적재 없음 — {@code @Transactional} 미부여.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdApprovalContextServiceImpl implements AttdApprovalContextService {

    private final AppReq09Mapper appReq09Mapper;

    @Override
    public ApprovalContextResponse getApprovalContext(String cmpnyCd, String siteCd, String userCd) {
        // 신청자 소속 노드의 자체근태승인 여부(D2). 노드 미존재/미배정/컬럼 null → 'N' 정규화(결재라인 다단계).
        String selfApprvYn = appReq09Mapper.selectAttdSelfApprvYn(cmpnyCd, siteCd, userCd);
        boolean isYes = "Y".equalsIgnoreCase(selfApprvYn);

        // 'Y' 부서일 때만 노드 관리자 여부가 의미 있다('N' 부서는 항상 결재선 다단계).
        boolean isNodeAdmin = isYes && appReq09Mapper.selectIsNodeAdmin(cmpnyCd, siteCd, userCd) > 0;

        return new ApprovalContextResponse(isYes ? "Y" : "N", isNodeAdmin);
    }
}
