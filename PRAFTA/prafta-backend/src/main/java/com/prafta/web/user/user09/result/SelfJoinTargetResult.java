package com.prafta.web.user.user09.result;

/**
 * 소정-09: 승인/거부 대상 계정의 <b>서버 권위값</b> (권한 게이트·상태 검증 입력).
 *
 * <p>사업장/부서는 클라이언트가 보낸 값을 쓰지 않고 반드시 이 조회 결과를 쓴다 —
 * 요청 바디의 siteCd/nodeCd 로 게이트를 통과시키면 자기 부서 값을 실어 타 부서 신청을
 * 승인할 수 있다(com-013-06-FU 보안 재작업과 동일 원칙).
 *
 * <p>★record 컬럼 순서 = 매퍼 SELECT 순서와 1:1.
 */
public record SelfJoinTargetResult(
        String userCd
        , String userId
        , String userNm
        , String siteCd
        , String nodeCd
        , String accountStatus
        , String useYn
) {
}
