package com.prafta.common.cmm.push;

/**
 * 지각/조퇴 감지 통보(M1) PUSH 생산자(outbox PENDING 적재) (PRAFTA-APP-021-3c).
 *
 * <p>출근 체크인 직후(지각)·퇴근 체크아웃 직후(조퇴) hook 에서 호출된다. 호출부가 try-catch 로
 * 전체 격리하지만, 본 구현도 내부에서 예외를 흡수(로그만)하여 출퇴근 본 흐름에 절대 영향을 주지 않는다.
 *
 * <p>판정은 raw 실근태(표준화 미적용)이며, 자정 넘김 오판 방지를 위해 (일자+시각) 통합 분 stamp 로
 * 비교한다(웹 Attd_11/앱 주간탭과 동일). 수신 대상 = 근로자 소속 노드 main/sub 관리자(본인 제외).
 */
public interface AttdLateEarlyNotiService {

    /**
     * 지각 감지/통보. 출근 INSERT 직후 호출. {@code 실제 출근(checkInDate+checkInTime) > 스케줄 시작(workYmd+schStartHhmm)}
     * 이면 지각으로 보고 노드 관리자에게 적재한다(아니면 no-op).
     *
     * @param cmpnyCd      회사 코드
     * @param siteCd       사업장 코드
     * @param workerUserCd 근로자(감지 대상) 코드
     * @param nodeCd       근로자 노드 코드(로그/payload 용, 대상 산출은 selectNodeAdmins)
     * @param workYmd      근무일(YYYYMMDD)
     * @param attdId       근태 레코드 ID(dedupKey)
     * @param checkInDate  실제 출근 일자(YYYYMMDD)
     * @param checkInTime  실제 출근 시각(HHMM)
     * @param schStartHhmm 선택 구간 스케줄 시작 시각(HHMM)
     * @param actorUserCd  적재 INSERT_NO
     */
    void detectLate(String cmpnyCd, String siteCd, String workerUserCd, String nodeCd,
                    String workYmd, String attdId, String checkInDate, String checkInTime,
                    String schStartHhmm, String actorUserCd);

    /**
     * 조퇴 감지/통보. 퇴근 UPDATE 직후 호출. {@code 실제 퇴근(checkOutDate+checkOutTime) < 스케줄 종료}
     * 이면 조퇴로 본다(야간=종료<시작이면 종료를 익일로 보정). 아니면 no-op.
     *
     * @param schStartHhmm 선택 구간 스케줄 시작 시각(HHMM) — 야간 자정 넘김 판정용
     * @param schEndHhmm   선택 구간 스케줄 종료 시각(HHMM)
     */
    void detectEarly(String cmpnyCd, String siteCd, String workerUserCd, String nodeCd,
                     String workYmd, String attdId, String checkOutDate, String checkOutTime,
                     String schStartHhmm, String schEndHhmm, String actorUserCd);
}
