package com.prafta.app.selfjoin.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 관리자 모드 — 셀프가입 승인 요청(승인 시트).
 *
 * <p>승인 대상의 사업장/부서는 담지 않는다 — 서버가 대상 행에서 재조회한다(IDOR 차단).
 *
 * <p>고용형태는 앱 시트에 입력란이 없다. 컨트롤러가 {@code "REGULAR"} 를 서버 상수로 채운다
 * (2026-08-13 사용자 확정). 값 검증은 웹 {@code User09ServiceImpl} 이 단일 출처로 수행한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppSelfJoinApproveRequest {

    /** 승인 대상 사용자 코드 (필수). */
    private String userCd;

    /** 입사일 (YYYYMMDD, 필수). */
    private String hireDate;

    /** 직급 [COM007] (선택 — 미선택이면 null). */
    private String rankCd;

    /** 소정근로 입력 방식 — FULL:풀타임(회사 통상 기준값) / DIRECT:단시간(직접 입력). */
    private String stdWorkType;

    /** 주 소정근로 분 — stdWorkType=DIRECT 일 때 필수. FULL 이면 무시된다. */
    private Integer stdWorkWeekMinutes;

    /** 소정근로 사유코드 [SYS083] — stdWorkType=DIRECT 일 때만 의미 있다. */
    private String stdWorkReasonCd;
}
