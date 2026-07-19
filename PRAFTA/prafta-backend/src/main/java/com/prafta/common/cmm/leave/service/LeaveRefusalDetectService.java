package com.prafta.common.cmm.leave.service;

/**
 * 노무수령거부 원천 차단 가드 (PRAFTA-COM-008-B, com-001 detect→block 전환).
 *
 * <p>출퇴근·근태 등록 진입부에서 레코드 생성 <b>이전</b>에 호출되는 내부 서비스다(API 아님).
 * 대상이면 차단 증빙(BLOCKED 이력 + 관리자 PUSH)을 독립 트랜잭션으로 선커밋한 뒤 차단 예외를
 * throw 하여 출근/근태 트랜잭션을 롤백시킨다("막되 반드시 기록").
 *
 * <p>판정 소스는 {@code tb_user_leave_use} 촉진단계다(NOTICED 로그 의존 폐지). web 기능1(통지)과
 * 공용 매퍼({@code LeaveRefusalMapper})를 공유하며, app→web 호출 금지 원칙에 따라 공용 영역
 * ({@code com.prafta.common.cmm.leave})에 둔다.
 */
public interface LeaveRefusalDetectService {

    /**
     * 시도일이 노무수령거부 차단 대상(촉진 1·2차 확정 법정 연차일 · 비휴일)인지 판정하고,
     * 대상이면 BLOCKED 이력 + 관리자 PUSH 를 독립 트랜잭션(REQUIRES_NEW)으로 선커밋한 뒤
     * 차단 예외(ATTD_400_150)를 throw 한다.
     *
     * <p>대상이 아니면(자발/비법정/휴일/연차 없음) 아무 일도 하지 않고 정상 반환한다 → 호출부 정상 진행.
     * 출근 원본(tb_user_attd_mgmt)은 읽지도 쓰지도 않으며 전달받은 식별자만 사용한다.
     *
     * @param cmpnyCd        회사 코드 (JWT 출처)
     * @param siteCd         사업장 코드 (JWT 출처)
     * @param userCd         대상 근로자 코드
     * @param nodeCd         소속 노드 코드 (현재 미사용, 추후 페이로드용으로 보존)
     * @param workYmd        시도 근무일 = 대상일 후보 (YYYYMMDD)
     * @param attemptType    시도 유형 (CHECK_IN/CHECK_OUT/ATTD_CREATE/ADMIN_ENTRY)
     * @param operatorUserCd 시도 주체 (근로자 본인 또는 관리자 USER_CD)
     */
    void guardAndRecord(String cmpnyCd, String siteCd, String userCd, String nodeCd,
                        String workYmd, String attemptType, String operatorUserCd);

    /**
     * 순수 판정(부작용 없음): 시도일이 노무수령거부 차단 대상(촉진 1·2차 확정 법정 연차일 · 비휴일)인지 여부.
     *
     * <p>{@link #guardAndRecord}와 <b>동일 술어</b>({@code LeaveRefusalMapper.selectLaborRefusalTarget})를
     * 공유하되, BLOCKED 이력/PUSH 적재나 예외 throw 를 하지 않는다. 화면(홈 카드 등)이 촉진 연차일을
     * 사전에 인지해 "출근하기" 대신 차단 안내를 노출하기 위한 조회 전용 계약이다(근태 E2E F2).
     *
     * <p>차단의 최종 권위는 여전히 {@code guardAndRecord}(ATTD_400_150)이며, 본 메서드는 표시 보조일 뿐이다.
     *
     * @return 촉진 차단 대상이면 true, 그 외(자발/비법정/휴일/연차 없음)면 false.
     */
    boolean isRefusalTarget(String cmpnyCd, String siteCd, String userCd, String workYmd);
}
