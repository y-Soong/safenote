package com.prafta.common.cmm.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.push.impl.PushSenderServiceImpl;
import com.prafta.common.cmm.push.mapper.PushOutboxMapper;
import com.prafta.common.cmm.push.vo.DeviceTokenVO;
import com.prafta.common.cmm.push.vo.PushOutboxRowVO;

/**
 * PRAFTA-COM-002 — FCM 전송 워커 핵심 로직 단위테스트.
 *
 * <p>{@link FcmClient} 와 {@link PushOutboxMapper} 는 Mockito mock — 실제 FCM/DB 미접속.
 * 상태전이/재시도누적/무효토큰 soft-delete/토큰0건/claim 멱등을 검증한다.
 */
class PushSenderServiceImplTest {

    private static final int MAX_RETRY = 3;
    private static final String NOTI = "N20260603001";
    private static final String USER = "U001";
    private static final String DEV1 = "DEVICE-UUID-1";
    private static final String DEV2 = "DEVICE-UUID-2";
    private static final String ACTOR = PushWorkerConst.WORKER_ACTOR;

    private PushOutboxMapper mapper;
    private FcmClient fcmClient;
    private PushSenderServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(PushOutboxMapper.class);
        fcmClient = org.mockito.Mockito.mock(FcmClient.class);
        service = new PushSenderServiceImpl(mapper, fcmClient, new ObjectMapper());
        ReflectionTestUtils.setField(service, "batchSize", 50);
        ReflectionTestUtils.setField(service, "maxRetry", MAX_RETRY);
        lenient().when(fcmClient.isAvailable()).thenReturn(true);
        // 기본 claim 성공.
        lenient().when(mapper.claimSending(anyString(), eq(ACTOR))).thenReturn(1);
    }

    private PushOutboxRowVO row(int retryCnt, String payload) {
        PushOutboxRowVO r = new PushOutboxRowVO();
        r.setNotiId(NOTI);
        r.setCmpnyCd("C001");
        r.setTargetUserCd(USER);
        r.setNotiType("LEAVE_GRANT_RECALLED");
        r.setTitle("title");
        r.setBody("body");
        r.setDataPayload(payload);
        r.setRetryCnt(retryCnt);
        return r;
    }

    private DeviceTokenVO device(String uuid, String token) {
        DeviceTokenVO d = new DeviceTokenVO();
        d.setDeviceUuid(uuid);
        d.setPushToken(token);
        return d;
    }

    @Test
    @DisplayName("T1 성공: 토큰 1건 SUCCESS → markSent(SENT) 1건")
    void success_marksSent() {
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.selectDeviceTokens(USER)).thenReturn(List.of(device(DEV1, "tokenAAAA")));
        when(fcmClient.send(eq("tokenAAAA"), anyString(), anyString(), any()))
                .thenReturn(FcmSendResult.SUCCESS);

        int sent = service.dispatchPending();

        assertEquals(1, sent);
        verify(mapper).markSent(NOTI, ACTOR);
        verify(mapper, never()).markFailed(anyString(), anyInt(), anyString(), anyString());
        verify(mapper, never()).incrementRetryAndRevertPending(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("T2 일시실패: TRANSIENT → 한도 미만이면 PENDING 복귀(재시도 누적), 한도 도달이면 FAILED")
    void transient_revertsThenFailsAtLimit() {
        // (a) retryCnt=0 → nextRetry=1 < 3 → incrementRetryAndRevertPending.
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.selectDeviceTokens(USER)).thenReturn(List.of(device(DEV1, "tokenAAAA")));
        when(fcmClient.send(anyString(), anyString(), anyString(), any()))
                .thenReturn(FcmSendResult.TRANSIENT_FAILURE);

        int sent = service.dispatchPending();

        assertEquals(0, sent);
        verify(mapper).incrementRetryAndRevertPending(eq(NOTI), anyString(), eq(ACTOR));
        verify(mapper, never()).markFailed(anyString(), anyInt(), anyString(), anyString());

        // (b) retryCnt=2 → nextRetry=3 >= 3 → markFailed(retry=3).
        org.mockito.Mockito.reset(mapper);
        when(mapper.claimSending(anyString(), eq(ACTOR))).thenReturn(1);
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(2, null)));
        when(mapper.selectDeviceTokens(USER)).thenReturn(List.of(device(DEV1, "tokenAAAA")));

        service.dispatchPending();

        verify(mapper).markFailed(eq(NOTI), eq(MAX_RETRY), anyString(), eq(ACTOR));
        verify(mapper, never()).incrementRetryAndRevertPending(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("T3 무효토큰: INVALID_TOKEN → softDeleteDeviceToken 호출. 다른 디바이스 SUCCESS면 markSent")
    void invalidToken_softDeletesAndSucceedsViaOther() {
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.selectDeviceTokens(USER))
                .thenReturn(List.of(device(DEV1, "badAAAA"), device(DEV2, "goodAAAA")));
        when(fcmClient.send(eq("badAAAA"), anyString(), anyString(), any()))
                .thenReturn(FcmSendResult.INVALID_TOKEN);
        when(fcmClient.send(eq("goodAAAA"), anyString(), anyString(), any()))
                .thenReturn(FcmSendResult.SUCCESS);

        int sent = service.dispatchPending();

        assertEquals(1, sent);
        verify(mapper).softDeleteDeviceToken(DEV1, ACTOR);
        verify(mapper, never()).softDeleteDeviceToken(eq(DEV2), anyString());
        verify(mapper).markSent(NOTI, ACTOR);
    }

    @Test
    @DisplayName("T3b 전부 무효: 모든 토큰 INVALID → soft-delete 후 FAILED(ALL_TOKENS_INVALID)")
    void allInvalid_marksFailed() {
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.selectDeviceTokens(USER))
                .thenReturn(List.of(device(DEV1, "badAAAA"), device(DEV2, "bad2AAAA")));
        when(fcmClient.send(anyString(), anyString(), anyString(), any()))
                .thenReturn(FcmSendResult.INVALID_TOKEN);

        service.dispatchPending();

        verify(mapper).softDeleteDeviceToken(DEV1, ACTOR);
        verify(mapper).softDeleteDeviceToken(DEV2, ACTOR);
        verify(mapper).markFailed(eq(NOTI), eq(0), eq(PushWorkerConst.ERR_ALL_TOKENS_INVALID), eq(ACTOR));
    }

    @Test
    @DisplayName("T4 토큰 0건: selectDeviceTokens 빈 리스트 → markFailed(NO_DEVICE_TOKEN), 전송 미호출")
    void noDeviceToken_marksFailed() {
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.selectDeviceTokens(USER)).thenReturn(List.of());

        service.dispatchPending();

        verify(mapper).markFailed(eq(NOTI), eq(0), eq(PushWorkerConst.ERR_NO_DEVICE_TOKEN), eq(ACTOR));
        verify(fcmClient, never()).send(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("T5 claim 멱등: claimSending affected=0 이면 처리 skip(토큰조회/전송/상태전이 없음)")
    void claimZero_skipsProcessing() {
        when(mapper.selectPendingForSend(50, MAX_RETRY)).thenReturn(List.of(row(0, null)));
        when(mapper.claimSending(NOTI, ACTOR)).thenReturn(0);

        int sent = service.dispatchPending();

        assertEquals(0, sent);
        verify(mapper, never()).selectDeviceTokens(anyString());
        verify(fcmClient, never()).send(anyString(), anyString(), anyString(), any());
        verify(mapper, never()).markSent(anyString(), anyString());
        verify(mapper, never()).markFailed(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("T6 게이트(FCM 미가용): isAvailable=false → 아무것도 조회/전송하지 않음")
    void fcmUnavailable_skipsAll() {
        when(fcmClient.isAvailable()).thenReturn(false);

        int sent = service.dispatchPending();

        assertEquals(0, sent);
        verify(mapper, never()).selectPendingForSend(anyInt(), anyInt());
        verify(fcmClient, never()).send(anyString(), anyString(), anyString(), any());
    }
}
