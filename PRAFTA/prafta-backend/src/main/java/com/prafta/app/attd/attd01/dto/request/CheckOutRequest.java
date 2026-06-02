package com.prafta.app.attd.attd01.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app check-out: 셀프 퇴근 요청 본문(camelCase, JSON @RequestBody).
 *
 * <p>IDOR 가드: USER_CD/CMPNY_CD/SITE_CD 는 본문으로 받지 않는다(JWT 출처만 사용).
 *   workSeq 도 받지 않는다(서버가 열린 구간을 판정).
 *
 * <p>GPS 정책(prafta-app-003 확정 모델): 위치권한은 앱 기동 시 하드게이트로 보장되므로 앱은 항상
 *   현재 좌표를 보낸다. 서버는 사업장 지오펜스로 외근(범위 밖) 여부만 판정한다.
 *   <ul>
 *     <li>lat/lon: 위도/경도(decimal(10,7)). 권한 보장 전제로 통상 존재하나, 측위 실패 등 결측 시
 *         거부하지 않고 온사이트(정상)로 폴백한다(A안). 좌표가 있으면 지오펜스 판정에 사용.</li>
 *     <li>accuracy: 정확도(m, decimal(7,2)). nullable.</li>
 *     <li>isMocked: Mock 위치 여부("Y"/"N"). 'Y' 면 폴백과 무관하게 퇴근 거부(부정 방지).</li>
 *     <li>workYmd: 퇴근 대상 근무일(YYYYMMDD, Low-1). 미전달 시 서버가 최신 열린건으로 폴백.</li>
 *   </ul>
 *
 * <p>prafta-app-008 확장:
 *   <ul>
 *     <li>offsiteReason: 외근(지오펜스 밖) 사유. 외근일 때 필수(미작성 시 거부, P2-D3).
 *         온사이트(범위 안)이면 무시(GPS 행 미저장). varchar(500).</li>
 *   </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class CheckOutRequest {
    private BigDecimal lat;
    private BigDecimal lon;
    private BigDecimal accuracy;
    private String isMocked;
    private String workYmd;
    private String offsiteReason;
}
