<template>
  <!--
    UI-002 TransferNoticePopup — 로그인 시 소속이동 안내 팝업 (PRAFTA-WEB_001-4)
    참조 패턴: NoticePopupCarousel.vue (LoginView 가 로그인 후 데이터 조회 → openPop 으로 호출)
    ※ 본 파일은 planner 가 작성한 "골격"이다. template + scoped style 만 완성돼 있고,
      ack POST 등 API 연동은 developer 가 채운다. (// TODO(developer) 참고)

    [opener 패턴(권장)] LoginView fnMoveMainPath 흐름에서:
      GET /webApi/user01/my-transfer-notice → hasNotice 면
      openPop(TransferNoticePopup, { notice_p: reservation })
      (NoticePopupCarousel 가 notices 프롭을 받아 열리는 방식과 동일)
  -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow transfer-notice-pop"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title (안내 팝업은 닫기[x] 미제공 — 확인으로만 종료) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>소속이동 안내</span>
        </div>

        <!-- 🔹 Body -->
        <div class="transfer-notice-pop__body">
          <p class="transfer-notice-pop__lead">
            회원님은 아래와 같이 소속이동 예정입니다.
          </p>

          <dl class="transfer-notice-pop__list">
            <div class="transfer-notice-pop__row">
              <dt>이동일</dt>
              <dd>{{ notice.moveDate || "-" }}</dd>
            </div>
            <div class="transfer-notice-pop__row">
              <dt>이동 사업장</dt>
              <dd>{{ notice.toSiteNm || "-" }}</dd>
            </div>
            <div class="transfer-notice-pop__row">
              <dt>이동 부서</dt>
              <dd>{{ notice.toNodeNm || "-" }}</dd>
            </div>
            <div class="transfer-notice-pop__row" v-if="notice.defaultSchNm">
              <dt>기본 근무타입</dt>
              <dd>{{ notice.defaultSchNm }}</dd>
            </div>
            <div class="transfer-notice-pop__row">
              <dt>사유</dt>
              <dd>{{ notice.moveReason || "-" }}</dd>
            </div>
          </dl>

          <!-- 안내 사항(진행중 결재 종료 등) -->
          <ul
            class="transfer-notice-pop__guide"
            v-if="notice.guideMessages && notice.guideMessages.length"
          >
            <li v-for="(g, idx) in notice.guideMessages" :key="idx">{{ g }}</li>
          </ul>
        </div>

        <!-- 🔹 Footer -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="acking"
              @click="fnAck"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
/*
 * ─────────────────────────────────────────────────────────────────────────
 *  developer 가 script 에서 채워야 할 항목 (planner 골격에는 미포함)
 *
 *  [props]  notice_p : 미확인 소속이동 예약 1건
 *           { reservationId, moveDate, toSiteNm, toNodeNm, defaultSchNm, moveReason, guideMessages:[] }
 *           (LoginView 가 GET /my-transfer-notice 조회 후 openPop 으로 전달 — NoticePopupCarousel 패턴)
 *  [emit]   close
 *
 *  [필요 API]
 *   - (opener) GET  /webApi/user01/my-transfer-notice
 *              → { hasNotice, reservation:{...} }  ※ 조회는 LoginView 가 수행, 본 팝업은 prop 으로 수신
 *   - POST /webApi/user01/transfer-notice/ack   body { reservationId }
 *
 *  [연결 체크리스트]
 *   1) onMounted: props.notice_p 를 notice 로 복사(없으면 안전 기본값)
 *   2) fnAck: POST ack → 성공 시 emit('close')  (실패해도 메인 진입을 막지 않도록 처리)
 *   3) (게이트 강도 미확정 OPEN Q11) 단순 advisory 면 확인=닫기만, 강제 게이트면 ack 성공 전까지 닫기 차단
 *   ※ 자체 조회(self-fetch) 방식으로 바꾸려면 GET /my-transfer-notice 를 onMounted 에서 호출해도 됨.
 * ─────────────────────────────────────────────────────────────────────────
 */
import { ref, defineProps, defineEmits, onMounted } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  // 미확인 소속이동 예약(LoginView 가 조회해 전달). 형식은 상단 주석 참고.
  notice_p: { type: Object, default: () => ({}) },
  // 본 안내 팝업 종료(확인) 후 이어서 실행할 후속 콜백(예: 로그인 공지 캐러셀).
  //   useModal 하네스의 onClose 와 이름이 겹치지 않도록 onClosed 사용.
  onClosed: Function,
});

// =========================== Ref ===========================
const modalRef = ref(null);
const acking = ref(false);
// { reservationId, moveDate, toSiteNm, toNodeNm, defaultSchNm, moveReason, guideMessages:[] }
const notice = ref({});

// =========================== Data ===========================
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

// =========================== Life Cycle ===========================
onMounted(() => {
  notice.value = props.notice_p || {};
  // TODO(developer): self-fetch 방식 채택 시 GET /webApi/user01/my-transfer-notice 호출로 대체 가능
});

// =========================== Methods ===========================
const fnAck = async () => {
  const reservationId = notice.value?.reservationId;
  acking.value = true;
  try {
    // 안내 확인 기록(NOTICE_ACK_YN='Y'). 본인 예약 대상은 서버가 토큰으로 강제.
    if (reservationId) {
      await axios.post("/webApi/user01/transfer-notice/ack", { reservationId });
    }
  } catch (err) {
    // advisory(OPEN Q11 결정): ack 실패해도 메인 진입을 막지 않는다(닫기 허용).
  } finally {
    acking.value = false;
    // 순차 처리: 본 팝업을 먼저 닫고(하네스 unmount) 후속 콜백 실행.
    //   (동일 useModal 하네스라 순서 반대면 후속 팝업이 닫혀 버린다.)
    emit("close");
    if (props.onClosed) props.onClosed();
  }
};
</script>

<style scoped>
.transfer-notice-pop {
  width: 90%;
  max-width: 420px;
}

.transfer-notice-pop__body {
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  font-family: "Pretendard", sans-serif;
}

.transfer-notice-pop__lead {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-strong);
}

.transfer-notice-pop__list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  padding: 0.75rem 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.transfer-notice-pop__row {
  display: flex;
  gap: 0.75rem;
  font-size: 0.8125rem;
}
.transfer-notice-pop__row dt {
  width: 6rem;
  flex-shrink: 0;
  color: var(--color-text-muted);
}
.transfer-notice-pop__row dd {
  margin: 0;
  color: var(--color-text-strong);
  word-break: break-all;
}

/* 안내 사항(결재 종료 등) — 경고 톤(warning) */
.transfer-notice-pop__guide {
  margin: 0;
  padding: 0.625rem 0.875rem 0.625rem 1.75rem;
  background: var(--color-warning-bg);
  border-radius: var(--input-radius);
  font-size: 0.75rem;
  color: var(--color-warning-text);
  line-height: 1.6;
}

.modal-footer {
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}
</style>
