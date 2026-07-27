package com.prafta.web.user.user07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 in-place 정정 사전 점검 응답 (GET /webApi/user07/contract-amend-precheck — T3 / J10).
 *
 * <p>{@code pinnedApprovedCnt} 와 {@code pendingCnt} 를 분리해 내려 준다 — pin 은 승인 시점에만
 * 기록되므로 대기('01') 요청에는 버전 정보가 없다. 화면 경고 문구는 두 값을 합산해 표기한다.
 *
 * <p>★{@code amendable} 은 래퍼 {@code Boolean} 으로 선언한다. 원시 {@code boolean} 이면 Lombok 이
 * {@code isAmendable()} 게터를 생성해 직렬화 키가 흔들릴 여지가 생기는데(프로젝트 기존 함정 —
 * Lombok+Jackson boolean is- 접두), 래퍼는 {@code getAmendable()} 이 생성되어 JSON 키가
 * {@code "amendable"} 로 고정된다. 프론트가 {@code amendable} 을 읽으므로 키가 어긋나면
 * 정정 버튼이 영구 비활성된다.
 */
@Value
@Builder
public class ContractAmendPrecheckResponse {
    Boolean amendable;          // 정정 가능 여부(= signCnt == 0). 최종 방어는 서버측 정정 API 재검증
    Integer signCnt;            // 해당 (사업장, 버전) 서명 행 수. 1 이상이면 정정 불가
    Integer pinnedApprovedCnt;  // 해당 버전을 pin 한 승인('02') 요청 수
    Integer pendingCnt;         // 같은 사업장 대기('01') 요청 수(pin 미기록이라 버전 무관)
}
