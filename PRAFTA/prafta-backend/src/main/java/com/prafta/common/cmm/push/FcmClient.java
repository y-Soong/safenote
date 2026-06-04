package com.prafta.common.cmm.push;

import java.util.Map;

/**
 * FCM 전송 래퍼 (PRAFTA-COM-002).
 *
 * <p>Firebase Admin SDK 호출을 이 인터페이스 구현({@code FcmClientImpl})에만 격리한다.
 * 서비스/테스트는 본 인터페이스에만 의존하므로 단위테스트에서 실제 FCM 미호출(mock)이 가능하다.
 */
public interface FcmClient {

    /**
     * 워커 사용 가능 여부. credentials-path 가 비었거나 파일 부재/초기화 실패면 false.
     * 서비스/스케줄러는 false 면 전송을 시도하지 않고 skip 한다(부팅 영향 0).
     */
    boolean isAvailable();

    /**
     * 단건 토큰으로 FCM 메시지 전송.
     *
     * @param token 디바이스 FCM 토큰
     * @param title notification.title
     * @param body  notification.body
     * @param data  추가 data 페이로드(null/빈맵이면 notification 만 전송)
     * @return 전송 결과 분기({@link FcmSendResult})
     */
    FcmSendResult send(String token, String title, String body, Map<String, String> data);
}
