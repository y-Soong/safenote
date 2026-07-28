package com.prafta.app.leave.leaveflow.dto.response;

import java.util.List;

/**
 * prafta-app-018-A: 연차 신청 폼 메타 응답 (GET /appApi/leaveflow/apply-meta).
 *
 * <p>키명은 018-C(FE)가 그대로 소비하므로 임의 변경 금지. record 사용으로 boolean is- 접두 탈락 이슈 없음.
 *
 * <p>{@code convMinutes} : 1일 환산시간(분) — <b>오늘 기준</b> 유효값(additive, 연차 시간차 환산 개편).
 * 신청 폼의 잔여(balanceDays) "N일 H시간 M분" 표기용 근사치다(신청 대상일 기준이 아님 — 정확한 분모는
 * preview 응답의 convMinutes 가 권위). 미설정 시 480.
 */
public record LeaveApplyMetaResponse(
      List<LeaveTypeItem> leaveTypes
    , int convMinutes
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
