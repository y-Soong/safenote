package com.prafta.web.attd.leaveflow.vo;

/**
 * prafta-com-016-C-4: 소멸 임박 통합순 자동 차감 대상 부여(grant) 조회 결과.
 * 후보 휴가코드(연차/월차) 전체에서 만료 임박(AVAIL_TO_DATE) 우선 1건을 가리키며,
 * 어느 종류(leaveCd)에서 차감할지를 함께 돌려준다(종류 무관 통합 정렬).
 */
public record AutoDeductibleGrantVO(
      String grantId
    , String leaveCd
) {
}
