package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소속이동 예약 등록 요청 — PRAFTA-WEB_001-1.
 *
 * <p>회사 스코프/등록자/권한은 클라가 보내지 않는다(서버 JWT 클레임 사용).
 * userCd 는 대상 사용자(타인) 코드이며, master/hr 만 호출 가능하다.
 * moveDate 는 YYYYMMDD(내일 이후), toDefaultSchCd 는 정규직만 필수(일용직 null).
 */
@Getter
@Setter
@NoArgsConstructor
public class TransferReservationRequest {
    private String userCd;
    private String toSiteCd;
    private String toNodeCd;
    private String moveDate;
    private String toDefaultSchCd;
    private String moveReason;
}
