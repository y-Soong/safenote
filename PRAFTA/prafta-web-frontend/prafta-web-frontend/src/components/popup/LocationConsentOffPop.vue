<!--
  LocationConsentOffPop.vue — 위치정보 동의 해제 방식 선택 팝업 (웹)

  - 앱 `views/mypage/components/LocationConsentOffSheet.vue` 의 웹 대응본.
    구성·문구·버튼 순서를 동일하게 맞춘다(같은 동의에 대해 채널별로 다른 고지를 하지 않기 위함).
  - ★openPop(useModal) 으로 열지 않는다 — openPop 은 기존 모달을 먼저 닫으므로 부모(MyInfoPop)가
    사라진다. 부모 템플릿 안에서 v-if 로 렌더링하는 중첩 오버레이다.
  - emits: close / suspend / withdraw — 실제 API 호출과 확인 팝업은 부모가 소유한다.
    이 컴포넌트는 "무엇을 고를지"만 보여준다.
-->
<template>
  <div class="loc-off-overlay" @click.self="$emit('close')">
    <div class="loc-off-pop" role="dialog" aria-modal="true">
      <div class="loc-off-pop__header">
        <span>위치정보 제공 해제</span>
        <button
          type="button"
          class="icon-button"
          aria-label="닫기"
          @click="$emit('close')"
        >
          ✕
        </button>
      </div>

      <div class="loc-off-pop__body">
        <p class="loc-off__lead">어떻게 멈출지 선택해 주세요.</p>

        <!-- 되돌릴 수 있는 쪽을 먼저 둔다(기본 선택지). -->
        <section class="loc-off__card">
          <h3 class="loc-off__title">일시 중지</h3>
          <p class="loc-off__summary">
            기록은 남기고, 앞으로의 수집만 멈춥니다.
          </p>
          <ul class="loc-off__points">
            <li v-for="(point, i) in suspendPoints" :key="`suspend-${i}`">
              {{ point }}
            </li>
          </ul>
          <button
            type="button"
            class="loc-off__btn"
            :disabled="saving"
            @click="$emit('suspend')"
          >
            일시 중지
          </button>
        </section>

        <section class="loc-off__card loc-off__card--danger">
          <h3 class="loc-off__title loc-off__title--danger">
            동의 철회
            <span class="loc-off__tag">복구 불가</span>
          </h3>
          <p class="loc-off__summary">수집된 위치정보를 모두 삭제합니다.</p>
          <ul class="loc-off__points">
            <li v-for="(point, i) in withdrawPoints" :key="`withdraw-${i}`">
              {{ point }}
            </li>
          </ul>
          <button
            type="button"
            class="loc-off__btn loc-off__btn--danger"
            :disabled="saving"
            @click="$emit('withdraw')"
          >
            동의 철회
          </button>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from "vue";
// 문구 단일 출처 — 확인 팝업(LOCATION_*_CONFIRM)과 같은 사실을 항목으로 끊어 쓴다.
import {
  LOCATION_SUSPEND_POINTS,
  LOCATION_WITHDRAW_POINTS,
} from "@/utils/locationConsent";

defineProps({
  // 전이 API 진행 중 — 중복 호출(중지 누르고 곧바로 철회) 방지용 잠금.
  saving: { type: Boolean, default: false },
});

defineEmits(["close", "suspend", "withdraw"]);

const suspendPoints = LOCATION_SUSPEND_POINTS;
const withdrawPoints = LOCATION_WITHDRAW_POINTS;
</script>

<style scoped>
/* 부모(MyInfoPop) 위에 겹치는 중첩 오버레이. 부모 모달보다 위에 와야 한다. */
.loc-off-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}
.loc-off-pop {
  width: min(32rem, 92vw);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border-radius: 0.5rem;
  overflow: hidden;
}
.loc-off-pop__header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
}
.loc-off-pop__body {
  flex: 1 1 auto;
  overflow: auto;
  padding: 1rem;
}
.loc-off__lead {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}
.loc-off__card {
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
  padding: 0.875rem;
}
.loc-off__card + .loc-off__card {
  margin-top: 0.75rem;
}
.loc-off__card--danger {
  border-color: var(--color-danger);
}
.loc-off__title {
  margin: 0 0 0.25rem;
  font-size: 0.9375rem;
  display: flex;
  align-items: center;
  gap: 0.375rem;
}
.loc-off__title--danger {
  color: var(--color-danger);
}
.loc-off__tag {
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 0.6875rem;
  font-weight: 400;
  background: #fef2f2;
  color: var(--color-danger);
}
.loc-off__summary {
  margin: 0 0 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  word-break: keep-all;
}
.loc-off__points {
  margin: 0 0 0.75rem;
  padding-left: 1.1rem;
  font-size: 0.8125rem;
  line-height: 1.7;
  color: var(--color-text-muted);
  word-break: keep-all;
}
.loc-off__btn {
  width: 100%;
  min-height: 36px;
  border: 1px solid var(--color-border);
  border-radius: 0.375rem;
  background: var(--color-surface, #fff);
  color: var(--color-text);
  font-size: 0.875rem;
  font-family: inherit;
  cursor: pointer;
}
.loc-off__btn:disabled {
  opacity: 0.5;
  cursor: default;
}
.loc-off__btn--danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}
</style>
