package com.prafta.common.cmm.location.service;

import com.prafta.common.cmm.location.result.LocationConsentStatusResult;

/**
 * 위치정보 동의(005) 상태 판정 및 전이 — 위치정보 동의철회·중지 S3.
 *
 * <h3>★판정의 단일 출처</h3>
 * 근태·TBM 등 위치를 쓰는 모든 경로가 이 서비스를 경유한다. 각 모듈이 직접 약관 테이블을 보면
 * 화면마다 판정이 갈린다(연차 면제 판정이 6곳에서 갈렸던 선례).
 *
 * <h3>★배경</h3>
 * 종전에는 005 가 로그인 게이트에 잡혀 <b>미동의 상태로는 서비스에 진입 자체가 불가능</b>했다.
 * 그래서 백엔드에 005 를 참조하는 코드가 한 줄도 없었다(게이트가 강제하니 확인할 필요가 없었다).
 * S2 에서 005 를 게이트에서 빼는 순간, "동의한 사람에게만 좌표를 수집한다"는 판정을 여기서
 * 새로 책임진다.
 */
public interface LocationConsentService {

    /**
     * 현재 동의 상태와 약관 버전.
     *
     * @return 4-state 중 하나. 미응답도 {@code PENDING_REAGREE} 로 본다(재동의를 요구해야 하는 점이 같다)
     */
    LocationConsentStatusResult resolveStatus(String cmpnyCd, String userCd);

    /**
     * 위치정보 수집이 허용되는가 — <b>{@code AGREED} 일 때만 true</b>.
     *
     * <p>신규 이벤트(출근, TBM 입실 등)의 판정에 쓴다.
     */
    boolean isCollectAllowed(String cmpnyCd, String userCd);

    /**
     * 진행 중인 근태의 후속 동작(퇴근)에 대한 수집 허용 판정 — <b>② 오버나이트 예외</b>.
     *
     * <p>{@code TB_TERMS.STR_DATE} 가 {@code varchar(8)} 날짜라 약관 시행 시점이 <b>자정</b>이다.
     * 오버나이트 근무자는 정의상 자정을 넘기므로, 약관을 개정할 때마다 <b>오버나이트 근무자 전원이
     * 퇴근 시점에 재동의 팝업을 만난다.</b> 재동의를 미루면 퇴근을 못 찍고 근태가 열린 채로 남아
     * 관리자 보정으로 넘어간다.
     *
     * <p>그래서 <b>{@code PENDING_REAGREE}(회사 사정) 에 한해</b> 진행 중인 근태의 퇴근까지는 허용한다.
     * {@code WITHDRAWN}·{@code SUSPENDED} 는 <b>본인 의사</b>이므로 예외를 적용하지 않는다.
     */
    boolean isCollectAllowedForOngoing(String cmpnyCd, String userCd);

    /**
     * 동의 철회(법 제24조①) — <b>수집된 위치정보를 전부 파기</b>하고 상태를 {@code WITHDRAWN} 으로 전이.
     *
     * <p>법 문언이 "수집된 개인위치정보"로 과거형이고 기간 단서가 없어, 마감 여부와 무관하게
     * 해당 계정의 보유분 전부가 대상이다. <b>되돌릴 수 없다</b> — 재동의해도 좌표는 복구되지 않는다.
     *
     * <p>★상태 전이·파기·파기 이력이 한 트랜잭션이다. 파기가 실패하면 상태 전이도 롤백된다
     * ("철회는 됐는데 안 지워진" 상태를 만들지 않는다).
     *
     * <p>★본인만 호출한다. 관리자 대행 철회는 만들지 않는다(오조작 시 복구 불가).
     *
     * @param userTypeCd 계정 계통(REGULAR/DAILY) — USER_CD 가 계통별 채번이라 파기 범위 산정에 필요
     * @return 전이 후 상태
     */
    LocationConsentStatusResult withdraw(String cmpnyCd, String userCd, String userTypeCd);

    /**
     * 일시 중지(법 제24조②) — 과거 좌표는 <b>유지</b>하고 이후 수집만 중단.
     *
     * <p>대부분의 이용자가 원하는 것은 "앞으로 그만"이다. 버튼이 철회 하나뿐이면 그걸 누른 순간
     * 한 달치가 날아가고 되돌릴 수 없다. 그래서 중지를 별도 권리로 제공한다.
     */
    LocationConsentStatusResult suspend(String cmpnyCd, String userCd);

    /**
     * 재동의 — 상태를 {@code AGREED} 로 전이하고 이후 수집을 재개한다.
     *
     * <p>중지/재동의대기/철회 어느 상태에서도 호출할 수 있다.
     * <b>철회로 파기된 좌표는 복구되지 않는다.</b>
     */
    LocationConsentStatusResult resume(String cmpnyCd, String userCd);
}
