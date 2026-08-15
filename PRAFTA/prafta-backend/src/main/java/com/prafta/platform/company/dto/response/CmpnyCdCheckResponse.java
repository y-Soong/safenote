package com.prafta.platform.company.dto.response;

/**
 * 회사코드 사용 가능 여부 확인 응답 (GET /platformApi/company/cmpny-cd-available).
 *
 * <p>★1차 확인용이다. 확인과 저장 사이에 다른 운영자가 같은 코드를 선점할 수 있으므로,
 * {@code available=true} 가 저장 성공을 보장하지 않는다. 최종 판정은 저장 트랜잭션이 한다.
 *
 * @param normalized 서버가 대문자로 정규화한 코드(형식 오류여도 정규화 결과는 돌려준다 — 화면 입력칸 갱신용)
 * @param valid      형식 유효 여부(영문·숫자 2~20자)
 * @param available  사용 가능 여부(형식 유효 + 미사용). 형식이 틀리면 항상 false
 * @param message    화면에 그대로 노출할 안내 문구
 */
public record CmpnyCdCheckResponse(
        String normalized
        , boolean valid
        , boolean available
        , String message
) {
}
