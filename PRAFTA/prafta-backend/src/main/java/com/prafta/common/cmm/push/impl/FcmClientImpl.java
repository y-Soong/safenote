package com.prafta.common.cmm.push.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.prafta.common.cmm.push.FcmClient;
import com.prafta.common.cmm.push.FcmSendResult;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Admin SDK 격리 래퍼 구현 (PRAFTA-COM-002).
 *
 * <p>초기화는 lazy/조건부다. credentials-path 가 비었거나 파일 부재/초기화 실패면
 * {@link #isAvailable()} 가 false 가 되어 워커가 조용히 skip 한다(앱 부팅 영향 0).
 * SDK 호출은 본 클래스에만 격리하여 테스트에서 mock 으로 대체 가능하게 한다.
 *
 * <p>전용 FirebaseApp 인스턴스 이름({@value #APP_NAME})으로 초기화하여 다른 Firebase
 * 사용처와 충돌하지 않게 한다.
 */
@Slf4j
@Component
public class FcmClientImpl implements FcmClient {

    /** 워커 전용 FirebaseApp 인스턴스 이름. */
    private static final String APP_NAME = "prafta-push-worker";

    /** 토큰 로그 마스킹 시 노출할 접두 길이. */
    private static final int TOKEN_PREFIX_LEN = 4;

    /** FCM 서비스 계정 키 JSON 파일 경로. 빈 값이면 미초기화(skip). */
    @Value("${push.fcm.credentials-path:}")
    private String credentialsPath;

    /** 초기화 성공 시에만 set 되는 메시징 핸들. 미초기화면 null → isAvailable=false. */
    private volatile FirebaseMessaging messaging;

    /**
     * 부팅 시 1회 초기화 시도. 키 부재/실패는 로그(warn)만 남기고 삼킨다(부팅 차단 금지).
     */
    @PostConstruct
    void init() {
        if (!StringUtils.hasText(credentialsPath)) {
            log.info("[push] FCM credentials-path 미설정 — FCM 미초기화(워커 skip).");
            return;
        }
        try (InputStream sa = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(sa))
                    .build();
            FirebaseApp app = findOrInitApp(options);
            this.messaging = FirebaseMessaging.getInstance(app);
            log.info("[push] FCM 초기화 완료(app={}).", APP_NAME);
        } catch (Exception e) {
            // 키 부재/무효/네트워크 등 — 부팅을 막지 않는다. 워커는 isAvailable=false 로 skip.
            log.warn("[push] FCM 초기화 실패 — 워커 비활성(skip). 사유: {}", e.getMessage());
        }
    }

    /** 동일 이름 FirebaseApp 이 이미 있으면 재사용, 없으면 신규 초기화. */
    private FirebaseApp findOrInitApp(FirebaseOptions options) {
        for (FirebaseApp existing : FirebaseApp.getApps()) {
            if (APP_NAME.equals(existing.getName())) {
                return existing;
            }
        }
        return FirebaseApp.initializeApp(options, APP_NAME);
    }

    @Override
    public boolean isAvailable() {
        return messaging != null;
    }

    @Override
    public FcmSendResult send(String token, String title, String body, Map<String, String> data) {
        if (messaging == null) {
            // 방어적: isAvailable 미검사 호출 시에도 일시 실패로 취급(재시도).
            log.warn("[push] FCM 미초기화 상태에서 send 호출 — TRANSIENT 처리.");
            return FcmSendResult.TRANSIENT_FAILURE;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }
            messaging.send(builder.build());
            return FcmSendResult.SUCCESS;
        } catch (FirebaseMessagingException e) {
            return classify(e, token);
        } catch (Exception e) {
            // SDK 외 예외(직렬화/런타임) — 일시 실패로 보고 재시도.
            log.warn("[push] FCM 전송 일반 예외(token={}): {}", maskToken(token), e.getMessage());
            return FcmSendResult.TRANSIENT_FAILURE;
        }
    }

    /** FCM 오류 코드를 영구 무효(INVALID_TOKEN) vs 일시 실패(TRANSIENT)로 분기. */
    private FcmSendResult classify(FirebaseMessagingException e, String token) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            log.info("[push] 무효 토큰(soft-delete 대상) code={}, token={}", code, maskToken(token));
            return FcmSendResult.INVALID_TOKEN;
        }
        // SENDER_ID_MISMATCH/THIRD_PARTY_AUTH_ERROR/QUOTA_EXCEEDED/UNAVAILABLE/INTERNAL 등은 재시도.
        log.warn("[push] FCM 일시 실패 code={}, token={}", code, maskToken(token));
        return FcmSendResult.TRANSIENT_FAILURE;
    }

    /** 토큰 평문 로그 금지 — 앞 4자만 노출하고 마스킹. */
    private String maskToken(String token) {
        if (token == null || token.length() <= TOKEN_PREFIX_LEN) {
            return "****";
        }
        return token.substring(0, TOKEN_PREFIX_LEN) + "****";
    }
}
