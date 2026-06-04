package com.prafta.common.schedule.push;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.util.ReflectionTestUtils;

import com.prafta.common.cmm.push.PushSenderService;

/**
 * PRAFTA-COM-002 — FCM 전송 워커 스케줄러 게이트 단위테스트.
 */
class PushSendSchedulerTest {

    private PushSenderService senderService;
    private PushSendScheduler scheduler;

    @BeforeEach
    void setUp() {
        senderService = org.mockito.Mockito.mock(PushSenderService.class);
        scheduler = new PushSendScheduler(senderService);
    }

    @Test
    @DisplayName("게이트 off: enabled=false 면 dispatchPending 미호출")
    void gateOff_doesNotDispatch() {
        ReflectionTestUtils.setField(scheduler, "workerEnabled", false);

        scheduler.runPushWorker();

        verify(senderService, never()).dispatchPending();
    }

    @Test
    @DisplayName("게이트 on: enabled=true 면 dispatchPending 호출")
    void gateOn_dispatches() {
        ReflectionTestUtils.setField(scheduler, "workerEnabled", true);
        when(senderService.dispatchPending()).thenReturn(2);

        scheduler.runPushWorker();

        verify(senderService, times(1)).dispatchPending();
    }

    @Test
    @DisplayName("dispatchPending 예외는 삼켜져 스케줄러 루프가 죽지 않는다")
    void exceptionSwallowed() {
        ReflectionTestUtils.setField(scheduler, "workerEnabled", true);
        when(senderService.dispatchPending()).thenThrow(new RuntimeException("boom"));

        Assertions.assertDoesNotThrow(() -> scheduler.runPushWorker());
    }
}
