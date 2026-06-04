package com.prafta.app.req.req09.service;

import java.util.List;

/**
 * 근태 요청 결재 분기/라인 INSERT 공용 서비스 (PRAFTA-APP-009-3).
 *
 * <p>req07 의 3 register 메서드(스케줄수정/근태보정/초과근무)가 tb_user_attd_req INSERT 직후
 * 본 서비스를 호출하여 결재 분기/라인을 처리한다. 호출부의 {@code @Transactional} 안에서 실행되어
 * 결재라인 INSERT 가 요청 INSERT 와 원자적으로 커밋/롤백된다(PUSH 적재만 예외 격리).
 *
 * <p>분기(신청자 소속 노드 SELF_ATTD_APPRV_YN):
 * <ul>
 *   <li>'Y' + 신청자=노드 정/부 관리자 → 즉시 자동승인(REQ_STATUS='02'), 결재라인 미INSERT, PUSH 미발송 (D4).</li>
 *   <li>'Y' + 일반 근로자 → 결재라인 미INSERT. 노드 관리자 0명이면 ATTD_400_105(D5), 1명 이상이면 승인 요망 PUSH(D3).</li>
 *   <li>'N' → 결재라인 다단계 INSERT(자기승인 자동처리·스코프 가드), 전 단계 자동승인이면 즉시 승인, 첫 수동단계 차례도래 PUSH (D6/D7/D8).</li>
 * </ul>
 *
 * <p>연차 {@code AppLeaveFlowServiceImpl#submitLeave}(336~392, 결재라인 생성) 미러.
 */
public interface AttdApprovalLineService {

    /**
     * 근태 요청 1건(REQ_ID)에 대한 결재 분기/라인 처리.
     *
     * <p>슬롯 다건은 호출부가 슬롯(REQ_ID)마다 본 메서드를 호출한다(각 REQ_ID 마다 결재라인 1벌).
     *
     * @param cmpnyCd         회사 코드(JWT 도출)
     * @param siteCd          사업장 코드(JWT 도출)
     * @param userCd          신청자 사용자 코드(JWT 도출)
     * @param reqId           대상 요청 ID(tb_user_attd_req — 방금 INSERT 됨)
     * @param approverUserCds 'N' 결재선 결재자 순서 목록(1차). 비면 presetId 폴백.
     * @param presetId        approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null).
     * @param insertNo        적재자(보통 신청자)
     */
    void applyApprovalFlow(String cmpnyCd, String siteCd, String userCd, String reqId,
                           List<String> approverUserCds, String presetId, String insertNo);
}
