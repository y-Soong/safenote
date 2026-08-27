package com.prafta.app.req.req09.service;

import java.util.List;

/**
 * 근태 요청 결재 분기/라인 INSERT 공용 서비스 (PRAFTA-APP-009-3).
 *
 * <p>req07 의 3 register 메서드(스케줄수정/근태보정/초과근무)가 tb_user_attd_req INSERT 직후
 * 본 서비스를 호출하여 결재 분기/라인을 처리한다. 호출부의 {@code @Transactional} 안에서 실행되어
 * 결재라인 INSERT 가 요청 INSERT 와 원자적으로 커밋/롤백된다(PUSH 적재만 예외 격리).
 *
 * <p>근태결재선통합 P1-2(2026-08-23): 구 SELF_ATTD_APPRV_YN 'Y'/'N' 조직도 위임 분기를 폐지하고
 * 항상 결재선(다단계) 경로로 통일한다.
 * <ul>
 *   <li>결재선 다단계 INSERT(자기지정 자격 게이트·스코프 가드), 첫 단계 차례도래 PUSH (D6/D7/D8).</li>
 *   <li>결재자 미지정(approverUserCds 도 presetId 도 없음) → 신청자 소속 노드의 기본 결재자(정 관리자
 *       우선, 없으면 부 관리자) 1인을 단일 결재선으로 사용(§0-5) — 구 'Y' 즉시확정(D4) 체감을 재현한다.
 *       기본 결재자가 없으면 ATTD_400_105(구 D5).</li>
 * </ul>
 *
 * <p>PRAFTA-001/004(2026-08-27, 결재선 필수화): "본인 + 자체근태승인 ON" 이어도 신청 즉시
 * 자동확정하지 않는다 — 결재선 생성만 수행하며, REQ_STATUS 즉시확정은 절대 발생하지 않는다.
 * 본인 지정 자격 게이트(ATTD_400_056)는 유지한다. 확정은 오직 정상 승인 API 호출을 통해서만
 * 이뤄진다(2026-08-16 "관리자 본인결재 허용" 정책이 이미 self/non-self 를 구분 않는 게이트로
 * 지원한다).
 *
 * <p>연차 {@code AppLeaveFlowServiceImpl#submitLeave}(336~392, 결재라인 생성) 미러.
 */
public interface AttdApprovalLineService {

    /**
     * 근태 요청 1건(REQ_ID)에 대한 결재 분기/라인 처리(항상 결재선 생성만 수행, REQ_STATUS 즉시확정 없음).
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
