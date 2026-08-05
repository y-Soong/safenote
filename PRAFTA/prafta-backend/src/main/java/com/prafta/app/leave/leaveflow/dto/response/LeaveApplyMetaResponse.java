package com.prafta.app.leave.leaveflow.dto.response;

import java.util.List;

/**
 * prafta-app-018-A: 연차 신청 폼 메타 응답 (GET /appApi/leaveflow/apply-meta).
 *
 * <p>키명은 018-C(FE)가 그대로 소비하므로 임의 변경 금지. record 사용으로 boolean is- 접두 탈락 이슈 없음.
 *
 * <p>{@code convMinutes} : 1일 환산시간(분) — <b>오늘 기준 본인 참고 분모</b>(E4 규약: 기본 근무타입
 * 소정근로분 근사치, 480 캡, 미산출 480 폴백 — 실스케줄과 편차 허용, 사용자 확정 2026-08-03).
 * 신청 폼의 잔여(balanceDays) "N일 H시간 M분" 표기용이다(신청 대상일 기준이 아님 — 실차감 분모는
 * 당일 배정 스케줄(E1)이며 preview 응답의 convMinutes 가 권위).
 *
 * <p>{@code hourlyBlocked} : (E5 해제 — 항상 false) 구 D2 의 사용자 속성 기반 시간차 차단 플래그.
 * 당일분모 전환(E1)으로 차단 근거가 소멸해 판정·strip 을 제거했다. 필드는 구 앱 FE 하위호환용으로
 * 유지한다(P4 릴리즈 시차 — 구 클라이언트도 서버 검증(ATTD_400_110/194)이 최종 판정하므로 안전).
 */
public record LeaveApplyMetaResponse(
      List<LeaveTypeItem> leaveTypes
    , int convMinutes
    , boolean hourlyBlocked
) {
    /**
     * 신청 가능 연차종류 1건.
     *
     * <ul>
     *   <li>{@code systemYn} : 법정여부 'Y'/'N'(원본 문자열).</li>
     *   <li>{@code aprvRequired} : 결재필요(법정=policy.APRV_USE_YN / 비법정=type.APRV_USE_YN, 'Y'→true).</li>
     *   <li>{@code allowedUnits} : 허용 사용단위 SYS025 코드 목록(D2-a 계층, 굵→잘게: 00,01,02,03,04 부분집합.
     *       LC-10: 반반차 '05'는 계층 밖 특례 — 법정=USAGE_UNIT='QUARTER_DAY' / 비법정=타입 USE_UNIT_TYPE='05'
     *       일 때 [00,01,05] 로 산출되며, 이 경우 시간차(02~04)는 포함되지 않는다).</li>
     *   <li>{@code balanceDays} : 현재 잔여(부여-사용 합, 활성집합, 소수1자리).</li>
     *   <li>{@code applicable} : 신청가능(잔여>0). false 면 FE disabled.</li>
     *   <li>{@code borrowable} : 가불 가능 여부(prafta-com-011-2). 시스템 법정 월차/본연차이고 borrowQuota>0 일 때 true.</li>
     *   <li>{@code borrowQuota} : 가불 가능 일수(prafta-com-011-2, computeBorrowQuota 결과, 소수1자리). 비대상이면 0.</li>
     *   <li>{@code borrowExpiryYmd} : 가불분 만료(소멸)일 YYYYMMDD(prafta-com-011-5 FE 표시·만료초과 alert 가드용).
     *       월차=입사+1년−1일, 본연차=차기 부여 본연차 정상 만료일. 비대상/산정 불가면 null. 서버도 fail-closed 검증(ATTD_400_181).</li>
     * </ul>
     */
    public record LeaveTypeItem(
          String leaveCd
        , String leaveNm
        , String systemYn
        , boolean aprvRequired
        , List<String> allowedUnits
        , double balanceDays
        , boolean applicable
        , boolean borrowable
        , double borrowQuota
        , String borrowExpiryYmd
    ) {
    }
}
