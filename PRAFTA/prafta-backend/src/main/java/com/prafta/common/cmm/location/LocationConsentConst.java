package com.prafta.common.cmm.location;

/**
 * 위치정보 동의(005) 공통 상수 — 위치정보 동의철회·중지 S2/S3.
 *
 * <p>약관ID·상태값·응답경로를 각 모듈에 흩뿌리지 않고 여기 한 곳에서만 정의한다
 * ({@code ConsentConst} 와 같은 취지 — 클라가 termsId 나 상태를 임의로 주입하지 못하게 서버 상수로 고정).
 *
 * <h3>★상태 4종과 AGR_YN 의 관계</h3>
 * <pre>
 *   AGREED           AGR_YN='Y'   동의 — 수집 허용
 *   SUSPENDED        AGR_YN='N'   본인 중지(법 제24조②) — 과거 좌표 유지, 이후 미수집
 *   PENDING_REAGREE  AGR_YN='N'   약관 개정에 따른 재동의 대기 — 과거 좌표 유지, 이후 미수집
 *   WITHDRAWN        AGR_YN='N'   철회(법 제24조①) — 과거 좌표 전부 파기, 이후 미수집
 * </pre>
 * ★{@code AGREED} 만 {@code AGR_YN='Y'} 이고 나머지 셋은 전부 {@code 'N'} 이다. 그래야 기존
 * {@code AGR_YN='Y'} 조건을 쓰는 모든 쿼리가 수정 없이 정확하게 동작한다(무회귀의 핵심 장치).
 *
 * <h3>★SUSPENDED 와 PENDING_REAGREE 를 나누는 이유</h3>
 * 법 제24조②는 본인의 일시 중지 요구에 대해 <b>"거절하여서는 아니 되며"</b> 라고 명시한다.
 * 본인 중지에 대한 이벤트 차단은 향후 재검토 여지가 있는 반면, 재동의 대기는 유효한 동의가 없는
 * 상태라 차단이 명백히 타당하다. <b>지금 나눠 두지 않으면 나중에 분리할 수 없다.</b>
 */
public final class LocationConsentConst {

    private LocationConsentConst() {
    }

    /** 위치기반서비스 이용약관 약관ID(SYS008 '005'). */
    public static final String LOCATION_TERMS_ID = "005";

    /** 동의 — 위치정보 수집 허용. */
    public static final String STATE_AGREED = "AGREED";

    /** 본인 중지(법 제24조②) — 과거 좌표 유지, 이후 미수집. */
    public static final String STATE_SUSPENDED = "SUSPENDED";

    /** 재동의 대기(약관 개정) — 과거 좌표 유지, 이후 미수집. ★철회가 아니다. */
    public static final String STATE_PENDING_REAGREE = "PENDING_REAGREE";

    /** 철회(법 제24조①) — 과거 좌표 전부 파기, 이후 미수집. */
    public static final String STATE_WITHDRAWN = "WITHDRAWN";

    /** 파기 사유: 동의 철회. */
    public static final String PURGE_REASON_WITHDRAW = "WITHDRAW";

    /** 파기 사유: 보존기간(3년) 경과 — {@code GpsRetentionScheduler} 용(현재 이력 미기록, 확장 예약). */
    public static final String PURGE_REASON_RETENTION = "RETENTION";

    /** 계정 계통: 정규직({@code TB_USER}) — SYS050. */
    public static final String USER_TYPE_REGULAR = "REGULAR";

    /** 계정 계통: 일용직({@code TB_DAILY_USER}) — SYS050. */
    public static final String USER_TYPE_DAILY = "DAILY";

    /** 응답 경로: 시스템(약관 버전업에 따른 재동의 대기 전이). */
    public static final String SOURCE_SYSTEM = "SYSTEM";
}
