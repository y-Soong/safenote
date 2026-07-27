package com.prafta.common.cmm.dailycontract.result;

/**
 * 미서명 계약서 in-place 정정 사전 점검 결과 (승인시점 버전확정 T3 / K6·J10).
 *
 * <p>MyBatis 매핑 대상이 아니므로 컴포넌트 순서 제약은 없다(서비스가 조립).
 *
 * <p>{@code pinnedApprovedCnt} 와 {@code pendingCnt} 를 <b>분리</b>하는 이유: pin 은 승인 시점에만
 * 기록되므로 대기('01') 요청에는 버전 정보가 없다. 요청서 J10 문면("해당 버전을 pin 한 대기·승인 N건")은
 * 대기 건에도 pin 이 있다는 전제라 부정확하다. 화면 문구는 두 값을 합산해 표기한다(T7).
 *
 * @param amendable         정정 가능 여부(= {@code signCnt == 0}). 최종 방어는 서버측 정정 API 재검증
 * @param signCnt           해당 (사업장, 버전) 서명 행 수. 1 이상이면 정정 불가(새 버전으로 등록)
 * @param pinnedApprovedCnt 해당 버전을 pin 한 승인('02') 요청 수 — 정정된 내용으로 서명하게 되는 대상
 * @param pendingCnt        같은 사업장 대기('01') 요청 수(버전 무관 — 승인 시 확정될 예정)
 */
public record ContractAmendPrecheckResult(
    boolean amendable
    , int signCnt
    , int pinnedApprovedCnt
    , int pendingCnt
) {
}
