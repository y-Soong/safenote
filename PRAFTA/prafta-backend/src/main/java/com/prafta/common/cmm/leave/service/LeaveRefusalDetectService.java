package com.prafta.common.cmm.leave.service;

/**
 * 노무수령거부 대상일 출근 감지 + 관리자 PUSH (PRAFTA-COM-001 기능2/3).
 *
 * <p>출근 체크인 성공 직후 hook 에서 호출되는 내부 서비스다(API 아님). 호출부(체크인)는
 * 본 메서드 호출을 try-catch 로 전체 격리하여, 감지/알림 실패가 체크인 트랜잭션을
 * 롤백하거나 실패시키지 않도록 한다. 본 서비스 내부도 방어적으로 예외를 흡수한다.
 *
 * <p>web 기능1(통지)과 공용 매퍼({@code LeaveRefusalMapper})를 공유하며, app→web 호출
 * 금지 원칙에 따라 공용 영역({@code com.prafta.common.cmm.leave})에 둔다.
 */
public interface LeaveRefusalDetectService {

    /**
     * 방금 체크인한 근무일이 노무수령거부 대상일(=통지된 사용지정일, 비휴일)인지 감지하고,
     * 대상이면 CHECKIN_DETECTED 사실 기록 후 관리자 PUSH(기능3)를 발송한다.
     *
     * <p>대상이 아니면(미통지/휴일) 아무 일도 하지 않는다. 관리자 0명이어도 감지 기록은 남는다.
     * 출근 원본(tb_user_attd_mgmt)은 읽지도 쓰지도 않으며 전달받은 식별자만 사용한다.
     *
     * @param cmpnyCd  회사 코드 (JWT 출처)
     * @param siteCd   사업장 코드 (JWT 출처)
     * @param userCd   대상 근로자 코드 (JWT 출처)
     * @param nodeCd   소속 노드 코드 (현재 사용 안 함, 추후 페이로드용으로 보존)
     * @param workYmd  출근 근무일 = 대상일 후보 (YYYYMMDD)
     * @param attdId   방금 INSERT 된 근태 ID (RELATED_ATTD_ID)
     * @param insertNo 등록자 (=USER_CD)
     */
    void detectAndAlert(String cmpnyCd, String siteCd, String userCd, String nodeCd,
                        String workYmd, String attdId, String insertNo);
}
