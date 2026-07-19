package com.prafta.common.cmm.dailyentry.service;

import java.util.List;

import com.prafta.common.cmm.dailyentry.application.query.EntryRequestListQuery;
import com.prafta.common.cmm.dailyentry.result.EntryLoginDecision;
import com.prafta.common.cmm.dailyentry.result.EntryRequestRow;

/**
 * 일용직 입장 승인요청 core 서비스 (웹 User_08 / 앱 entryadmin01 / 로그인·가입 플로우 공용).
 *
 * <p>출처: 일용직 계약서+승인제 요청서 §2 D5·D6·D7·D9·D10, §4-1 / plan §1(상태 모델)·§T2.
 * 정책서: {@code common/03-account-auth.md} §3.2·§3.5, {@code common/05-slot-management.md} §5.6,
 * {@code common/10-notifications.md} §10.3.
 */
public interface DailyEntryService {

    /**
     * 로그인 시 입장 승인 판정(plan §1 판단 규칙). 읽기 전용 — 상태를 변경하지 않는다.
     *
     * @return APPROVED(소진 대상 reqId 포함) / PENDING / REJECTED_TODAY / NONE
     */
    EntryLoginDecision findLoginDecision(String cmpnyCd, String userCd);

    /**
     * 승인요청 생성 + 사업장 관리자 푸시 outbox 적재(NOTI_TYPE='DAILY_ENTRY_REQ').
     *
     * <p>open('01'/'02') 요청 존재 시 신규 생성 없이 no-op(멱등).
     * 당일 거부('03') 이력 존재 시 DAILYLOGIN_400_007 예외(당일 재요청 금지 — 익일부터 허용).
     *
     * @param reqType [SYS081] 01:신규가입 / 02:재입장
     */
    void createEntryRequest(String cmpnyCd, String siteCd, String userCd, String reqType);

    /**
     * 승인 소진 — '02'(승인) → '05'(소진) 조건부 UPDATE (D6).
     * 호출자(재활성/활성화 트랜잭션)와 같은 트랜잭션에서 수행되며, 0행 반환 시 호출자가 전체 롤백해야 한다.
     *
     * @return 영향행 수 (0 = 판정~소진 사이 상태 변경 경합)
     */
    int consumeApprovedRequest(String cmpnyCd, String reqId, String userCd);

    /**
     * 승인요청 목록 조회 (사업장 인가 가드 포함 — 처리자의 SITE 권한 검증).
     *
     * @param procUserCd 조회자(관리자) USER_CD — JWT 클레임 도출값만 신뢰
     * @param procAuthCd 조회자 역할코드 — JWT 클레임 도출값만 신뢰
     */
    List<EntryRequestRow> selectEntryRequests(EntryRequestListQuery query, String procUserCd, String procAuthCd);

    /**
     * 일괄/개별 승인 처리 (D9). 요청별로 사업장 인가 가드 + 대기('01') 상태 검증 후 '02' 전이.
     * 하나라도 실패하면 전체 롤백(all-or-nothing).
     *
     * @return 처리 건수
     */
    int approveRequests(String cmpnyCd, List<String> reqIds, String procUserCd, String procAuthCd);

    /**
     * 거부 처리 (D10). 사업장 인가 가드 + 대기('01') 상태 검증 후 '03' 전이 + 사유 기록(필수, 200자 이하).
     */
    void rejectRequest(String cmpnyCd, String reqId, String reason, String procUserCd, String procAuthCd);

    /**
     * 자정 만료(D7) — 요청일이 지난 대기('01')/승인('02') 요청 일괄 '04' 전이.
     * 일용직 만료 배치(자정)에 편승 호출된다.
     *
     * @return 처리 건수
     */
    int expireOverdueRequests();

    /**
     * 사업장 인가 보유 여부 — master/hr 역할 + (master 외) 해당 사업장 TB_USER_SITE_AUTH(USE_YN='Y') 보유.
     *
     * <p>T3(계약서 도메인) 확장: 승인/거부 가드와 동일 기준을 계약서 관리·서명본 열람 인가에 재사용한다
     * (단일 출처 — 예외 코드는 호출 도메인이 각자 매핑).
     *
     * @param userCd JWT 클레임 도출 처리자 USER_CD
     * @param authCd JWT 클레임 도출 역할코드
     */
    boolean hasSiteAuthority(String cmpnyCd, String userCd, String authCd, String siteCd);
}
