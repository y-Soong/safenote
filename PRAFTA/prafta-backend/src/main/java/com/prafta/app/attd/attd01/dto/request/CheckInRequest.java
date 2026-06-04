package com.prafta.app.attd.attd01.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-003 A1: 셀프 출근(check-in) 요청 본문(camelCase, JSON @RequestBody).
 *
 * <p>IDOR 가드: USER_CD/CMPNY_CD/SITE_CD 는 본문으로 받지 않는다(JWT 출처만 사용).
 *   WORK_SEQ 도 받지 않는다(서버가 그 일자 기존 근태 개수로 산정).
 *
 * <p>GPS 정책(prafta-app-003 확정 모델): 위치권한은 앱 기동 시 하드게이트로 보장되므로 앱은 항상
 *   현재 좌표를 보낸다. 서버는 사업장 지오펜스로 외근(범위 밖) 여부만 판정한다.
 *   <ul>
 *     <li>lat/lon: 위도/경도(decimal(10,7)). 권한 보장 전제로 통상 존재하나, 측위 실패 등 결측 시
 *         거부하지 않고 온사이트(정상)로 폴백한다(A안). 좌표가 있으면 지오펜스 판정에 사용.</li>
 *     <li>accuracy: 정확도(m, decimal(7,2)). nullable.</li>
 *     <li>isMocked: Mock 위치 여부("Y"/"N"). 'Y' 면 폴백과 무관하게 출근 거부(부정 방지).</li>
 *     <li>workYmd: 출근 대상 근무일(YYYYMMDD). 미전달 시 서버 today(통상 당일 출근).</li>
 *   </ul>
 *
 * <p>prafta-app-008 확장:
 *   <ul>
 *     <li>offsiteReason: 외근(지오펜스 밖) 사유. 외근일 때 필수(미작성 시 거부, P2-D3).
 *         온사이트(범위 안)이면 무시(GPS 행 미저장). varchar(500).</li>
 *   </ul>
 *
 * <p>prafta-app-015 변경: 2구간 스케줄 출근 구간 자동추정 폐기 → 사용자 명시 선택.
 *   <ul>
 *     <li>targetWorkSeq: 2구간 스케줄에서 출근할 구간(1=1구간, 2=2구간). 1구간 스케줄/스케줄 없는
 *         날은 무시한다(서버가 강제 무시). 선택 구간이 곧 WORK_SEQ 로 채번된다(순서 자유).</li>
 *     <li>(폐기) confirmSkipPrevSlot: §5.5 Case C 확인 플래그. 자동추정/Case A/B/C 가 폐기되어
 *         더 이상 사용하지 않는다(필드 제거).</li>
 *   </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class CheckInRequest {
    private BigDecimal lat;
    private BigDecimal lon;
    private BigDecimal accuracy;
    private String isMocked;
    private String workYmd;
    private String offsiteReason;
    // prafta-app-015: 2구간 스케줄 출근 구간 명시 선택(1|2). 그 외 스케줄에서는 무시.
    private Integer targetWorkSeq;
    // prafta-com-003 D3: 출근 실행 디바이스UUID(클라 제공값, 부정탐지 보조). axios 가 보내는 키는 gv_deviceId.
    //   신뢰경계 밖(위조 가능) — 식별/인가에는 쓰지 않고 표시·탐지 보조용으로만 CHECK_IN_DEVICE_UUID 에 도장한다.
    @JsonProperty("gv_deviceId")
    private String deviceId;
}
