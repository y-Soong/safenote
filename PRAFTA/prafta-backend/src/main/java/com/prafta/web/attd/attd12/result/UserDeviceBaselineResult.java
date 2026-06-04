package com.prafta.web.attd.attd12.result;

/**
 * prafta-com-003 C6 - 사용자별 baseline 디바이스(로그인 이력에서 관측된 사용 기기).
 *
 * <p>tb_user_device_login_hist 에서 (USER_CD, DEVICE_UUID) distinct 로 추출.
 *   규칙2(평소 기기와 다름)/규칙3(한 번도 본 적 없는 기기) 판정의 기준 집합이다.
 *
 * <p>⚠️ SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record UserDeviceBaselineResult(
        String userCd
        , String deviceUuid
) {
}
