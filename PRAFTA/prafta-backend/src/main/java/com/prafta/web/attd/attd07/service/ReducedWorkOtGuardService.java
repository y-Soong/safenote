package com.prafta.web.attd.attd07.service;

import java.util.List;

/**
 * 소정-07: 단축근무자(육아기·임신기·가족돌봄) 초과근무 게이트 — 컴플라이언스 핵심.
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §단축근무자 "OT 게이트",
 * plan §4 소정-07.
 *
 * <p><b>법적 근거</b>
 * <ul>
 *   <li><b>임신기 단축(PREGNANCY)</b>: 연장근로 <b>전면 금지</b>(근기법 제74조) → 무조건 거부.</li>
 *   <li><b>육아기(CHILDCARE)·가족돌봄(FAMILY_CARE) 단축</b>: 사업주가 연장근로를 요구할 수 없고,
 *       <b>근로자가 명시적으로 청구한 경우에만 주 12시간(720분) 이내</b> 허용.
 *       위반 시 1천만원 이하 벌금 → 명시 청구 확인 값이 없으면 거부(fail-closed) + 주 720분 초과 거부.</li>
 * </ul>
 *
 * <p><b>★명시 청구 = "신청 시점의 사실"</b> (2026-08-12 확정) — 승인자가 재확인하는 값이 아니다.
 * <ul>
 *   <li><b>근로자 신청(REQ) 경유 승인</b>(웹 인박스 03 생성승인 / 04 수정승인 / 앱 관리자 승인):
 *       그 REQ 는 신청 시점에 이미 본 게이트를 통과했으므로 청구 사실이 성립한다 →
 *       <b>청구 확인 검사를 건너뛴다</b>({@code claimVerifiedAtRequest=true}).
 *       단 <b>주 720분 한도는 승인 시점에도 그대로 재검사</b>한다 — 신청과 승인 사이에
 *       다른 초과근무가 쌓였을 수 있기 때문이다.</li>
 *   <li><b>관리자 직접 등록</b>(REQ 없이 OT 를 바로 생성·수정): 근로자의 청구가 시스템에 남은 적이
 *       없으므로 청구 확인을 계속 요구한다({@code claimVerifiedAtRequest=false}).</li>
 *   <li>통상(NORMAL)·단시간계약(PART_TIME) 사유는 본 게이트의 대상이 아니다.</li>
 * </ul>
 *
 * <p><b>배치</b> — 웹({@code Attd07ServiceImpl.updateUserOvertimeRequests})과
 * 앱({@code AppReq07ServiceImpl.registerOvertime})이 같은 판정을 써야 하므로 로직을 복제하지 않고
 * 공용 빈으로 둔다. app 모듈이 web attd07 의 서비스 빈을 주입해 쓰는 것은 기존 선례
 * ({@link AttdCloseService})와 동일하다.
 *
 * <p><b>기존 OT 창(등록 가능 범위) 로직과의 관계</b> — 고정연장 2단계에서 확정된
 * {@code raw 실근태 − (소정 ∪ 고정연장 ∪ 연차 면제)} 창 계산은 <b>건드리지 않는다.</b>
 * 본 게이트는 그 앞단에 붙는 <b>추가 거부 조건</b>일 뿐이다.
 *
 * <p><b>무회귀 보장</b> — 근무일 기준 소정근로 이력 행이 없거나(대다수 근로자) 사유가 단축이 아니면
 * 이력 조회 1회 후 즉시 통과한다. 주 합계 쿼리는 단축 사유 행이 확인된 뒤에만 실행된다.
 */
public interface ReducedWorkOtGuardService {

    /** 육아기·가족돌봄 단축 기간의 주간 연장근로 한도(분) = 주 12시간. */
    int WEEKLY_OT_LIMIT_MINUTES = 720;

    /**
     * 단축근무자 초과근무 게이트. 위반이면 {@code ApiException} 을 던지고, 대상이 아니면 조용히 통과한다.
     *
     * <p>거부 코드
     * <ul>
     *   <li>{@code ATTD_400_200} — 임신기 단축(연장근로 전면 금지)</li>
     *   <li>{@code ATTD_400_201} — 육아기·가족돌봄 단축인데 근로자 명시 청구 확인 없음</li>
     *   <li>{@code ATTD_400_202} — 육아기·가족돌봄 단축 주 720분 초과</li>
     * </ul>
     *
     * @param cmpnyCd              회사 코드(토큰 도출값)
     * @param siteCd               사업장 코드. <b>로그 컨텍스트 전용</b>이며 주 한도 집계에는 쓰지 않는다
     *                             (M-1: 연장근로 한도는 근로자 기준이라 사업장으로 좁히면 다중 사업장
     *                             권한 근로자가 사업장마다 720분씩 등록해 법정 한도를 넘길 수 있다).
     * @param userCd               <b>대상 근로자</b> 코드(처리자가 아님 — 웹 관리자 등록 경로 주의)
     * @param workYmd              근무일(YYYYMMDD). 주 경계는 이 날짜가 속한 주(월~일)로 산정한다.
     * @param requestMinutes       이번 요청분 초과근무 합계(분). 음수는 0으로 클램프한다.
     * @param workerClaimConfirmed   근로자 명시 청구 확인 여부(요청 DTO {@code reducedWorkOtClaimYn='Y'}).
     *                               구버전 클라이언트가 미전송하면 false → 거부(fail-safe 방향).
     *                               {@code claimVerifiedAtRequest=true} 이면 이 값은 보지 않는다.
     * @param claimVerifiedAtRequest 근로자 신청(REQ) 경유 여부. true 면 청구가 <b>신청 시점에 이미 검증</b>된
     *                               것으로 보아 청구 확인 검사를 건너뛴다(주 720분 한도는 그대로 검사).
     *                               호출부는 {@code reqId} 유무로 판정한다.
     * @param excludeOtIds           이번 등록에서 in-place 갱신될 기존 OT_ID 목록(주 합계 이중 계상 방지). null 허용
     * @param excludeReqId           이번 승인으로 닫힐 REQ_ID(주 합계 이중 계상 방지). null 허용
     */
    void assertOvertimeAllowed(String cmpnyCd,
                               String siteCd,
                               String userCd,
                               String workYmd,
                               int requestMinutes,
                               boolean workerClaimConfirmed,
                               boolean claimVerifiedAtRequest,
                               List<String> excludeOtIds,
                               String excludeReqId);
}
