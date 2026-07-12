package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.result.TransferReservationExecRow;

/**
 * 사용자 소속이동 발효(실행) 서비스 — PRAFTA-WEB_001-2(Terminal B).
 *
 * <p>발효일(MOVE_DATE == 오늘) 도래 예약을 실제 적용한다. 자정 스케줄러
 * ({@code UserTransferScheduler})가 발효 대상을 조회해 예약 1건씩 본 서비스로 위임한다.
 *
 * <p>발효 단계(예약 1건, @Transactional 원자 처리, 멱등):
 * <ol>
 *   <li>(a) tb_user.SITE_CD/NODE_CD ← 예약 TO_*. 정규직이면 DEFAULT_SCH_CD/SET_DATE 갱신.</li>
 *   <li>(b) tb_user_site_auth: 신규 사업장 권한 활성(UPSERT). 비전사역할은 D7 회수 패턴으로 신소속 1건만 잔존.</li>
 *   <li>(c) 담당 정 자동등록(정규직): 이동 노드 MAIN_ADMIN_CD 비어있으면 대상자로 세팅.</li>
 *   <li>(d) 기본근무 발효(정규직): {@code DefaultSchGenService.applyDefaultSchChange} 재사용.</li>
 *   <li>(e) 진행중 요청 일괄 반려/취소(대상자=신청자 + 대상자=승인대기 결재자).</li>
 *   <li>(f) TBM: 진행 관리자 시작·미종료 세션 종료처리 + 대상자 참석·미서명 미이수 처리.</li>
 *   <li>(g) 예약 STATUS='APPLIED' + EXECUTED_DATE.</li>
 * </ol>
 */
public interface User01TransferExecutionService {

    /**
     * 예약 1건을 발효 처리한다(@Transactional 원자 처리). 단계 중 하나라도 실패하면 전체 롤백되어
     * 예외가 전파되며, 호출자(스케줄러)가 격리 후 {@link #markFailed} 로 해당 예약만 FAILED 처리한다.
     *
     * <p>진입 시 예약 행을 FOR UPDATE 로 선점 잠그고 STATUS='RESERVED' 를 재확인한다(다중 인스턴스 이중작업 방지).
     * 이미 처리된(RESERVED 아님) 행이면 무동작으로 즉시 빠진다.
     *
     * <p>또한 발효 첫 단계에서 등록~발효 사이 상태변화를 재검증한다(이동 사업장/부서 실재 + 5종 불가케이스 재실행).
     * 재검증 실패 시 예외를 던져 전체 롤백 → 스케줄러가 FAILED+FAIL_REASON 으로 격리한다.
     *
     * @param row 발효 대상 예약(회사/사용자 스코프의 단일 출처)
     * @return 실제 발효(APPLIED)했으면 true, 선점 패배/이미 처리로 빠졌으면 false(skip)
     */
    boolean executeReservation(TransferReservationExecRow row);

    /**
     * 발효 실패 격리 마킹(별도 트랜잭션). 실행 트랜잭션 롤백 후 호출되어 해당 예약만 STATUS='FAILED' 로 전이한다.
     *
     * @param cmpnyCd       회사 코드(예약 스코프)
     * @param reservationId 예약 ID
     * @param failReason    실패 사유(최대 500자, 초과 시 절단)
     * @param actor         감사 actor(등록자)
     */
    void markFailed(String cmpnyCd, String reservationId, String failReason, String actor);
}
