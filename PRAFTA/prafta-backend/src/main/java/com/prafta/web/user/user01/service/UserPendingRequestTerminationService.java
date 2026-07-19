package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.result.PendingRequestTerminationResult;

/**
 * 사용자 대기요청 일괄 종결 공유 서비스 (F1 / QT-11-7).
 *
 * <p>특정 사용자가 회사 스코프 내에서 사라지는(소속이동 발효 · 비활성 전환 · 탈퇴) 시점에,
 * 그 사용자가 <b>①신청자</b>이거나 <b>②결재자</b>인 모든 진행중 요청
 * (근태보정·초과근무·연차·스케줄수정·연차변경)을 양방향으로 종결하여, 잔존 대기요청이
 * 월 마감을 영구 차단하는 교착(§13.3 마감 차단 조건)을 원천 제거한다.
 *
 * <p>상태 전이 SQL은 정상 반려/취소의 캐노니컬 컬럼 집합을 미러한 회사+사용자 스코프 일괄 UPDATE
 * ({@code UserTransferMapper})를 재사용하며, 종결되는 연차('05') 요청은 반드시 원장 원복
 * ({@code LeaveFlowService#restoreLeaveLedgerOnTerminate})을 동반한다(차감만 남는 상태 금지).
 * 소속이동 발효 경로와 F1(비활성/탈퇴)이 <b>단일 출처</b>를 공유하도록 하여 상태 전이 코드가
 * 두 곳으로 갈라지지 않게 한다.
 */
public interface UserPendingRequestTerminationService {

    /**
     * 대상 사용자의 진행중 대기요청을 양방향(신청자·결재자)으로 일괄 종결한다.
     *
     * <p>QT-11-7 시퀀스 보존: 종결되는 연차 요청의 REQ_ID를 상태 UPDATE <b>이전에</b> 스냅샷하고,
     * 상태 UPDATE <b>직후</b> 정상 반려와 동일한 원복 시퀀스를 태운다(스냅샷을 안 하면
     * REQ_STATUS='01' 조건이 깨져 대상을 다시 찾을 수 없다).
     *
     * <p>호출자의 트랜잭션에 합류한다(REQUIRED). 종결/원복 중 실패 시 호출자 전체가 롤백되어
     * 비활성/탈퇴/발효가 부분 처리되지 않는다.
     *
     * @param cmpnyCd 회사 코드(스코프 강제, cross-tenant 방지)
     * @param userCd  대상 사용자 코드(신청자·결재자 양방향 스코프)
     * @param reason  종결 사유(처리 코멘트/취소 사유에 기록, PII 금지)
     * @param actor   처리자 USER_CD(감사 컬럼)
     * @return 방향/유형별 종결 건수 요약
     */
    PendingRequestTerminationResult terminateAllPendingFor(String cmpnyCd, String userCd, String reason, String actor);
}
