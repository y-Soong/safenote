package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;

/**
 * 연차 결재 PUSH 생산자(outbox PENDING 적재) 공용 서비스 (PRAFTA-COM-004).
 *
 * <p>web 신청·승인(LeaveFlowServiceImpl)과 app 신청(AppLeaveFlowServiceImpl) hook 이 공용 호출한다.
 * 구현은 {@code @Transactional} 을 부여하지 않으며, 내부에서 예외를 흡수(로그만)하여 연차 신청/결재
 * 본 흐름에 절대 영향을 주지 않는다(노무수령거부 hook 예외 격리 패턴). dedup UNIQUE 로 멱등.
 *
 * <p>실제 FCM 발송은 prafta-com-002 공용 워커가 담당한다(본 서비스는 생산자만).
 */
public interface LeaveApprovalNotiService {

    /**
     * 시나리오 A — 연차 결재 차례 도래. 해당 단계로 차례가 넘어온 결재자 1인에게 outbox 1건 적재.
     *
     * <p>자동승인 skip 단계·본인 단계·반려·최종승인은 호출부에서 제외한다(본 메서드는 1인 적재만).
     * dedupKey = {@code "LV_TURN_" + reqId + "_" + approvalStep}.
     *
     * @param cmpnyCd          회사 코드
     * @param siteCd           사업장 코드(없으면 null)
     * @param applicantUserCd  신청자 코드(본문 실명 조회용)
     * @param reqId            연차 요청 ID
     * @param approvalStep     차례가 도래한 결재 단계(1-based)
     * @param approverUserCd   해당 단계 지정 결재자(수신 대상)
     * @param insertNo         적재자(=신청자/처리자 코드)
     */
    void notifyApprovalTurn(String cmpnyCd, String siteCd, String applicantUserCd,
                            String reqId, int approvalStep, String approverUserCd, String insertNo);

    /**
     * 시나리오 B — 무결재(aprvRequired=false) 연차 즉시 확정 시, 신청자 소속 노드 main/sub 관리자에게
     * "누가/언제/어떤 단위의 연차를 사용했다"를 통보. 관리자별 outbox 1건씩 적재.
     *
     * <p>신청자 본인이 노드 관리자에 포함되면 제외(자기 알림 방지). master/hr 제외(노드 관리자만).
     * dedupKey = {@code "LV_USED_" + leaveId + "_" + targetUserCd}.
     *
     * @param cmpnyCd          회사 코드
     * @param siteCd           사업장 코드(없으면 null)
     * @param applicantUserCd  신청자 코드(수신자 산출 + 본문 실명 조회 + 자기 알림 제외용)
     * @param leaveId          연차 사용기록 ID(dedup/payload 라우팅 키)
     * @param useUnitType      사용 단위[SYS025] 00종일/01반차/02·03·04 시간차
     * @param leaveDays        차감 일수(종일 본문 표기용)
     * @param workYmd          사용 일자(YYYYMMDD)
     * @param startTime        시간차 시작(HHMM, 시간차 외 null)
     * @param endTime          시간차 종료(HHMM, 시간차 외 null)
     * @param insertNo         적재자(=신청자 코드)
     */
    void notifyLeaveUsedNoAprv(String cmpnyCd, String siteCd, String applicantUserCd,
                               String leaveId, String useUnitType, BigDecimal leaveDays,
                               String workYmd, String startTime, String endTime, String insertNo);
}
