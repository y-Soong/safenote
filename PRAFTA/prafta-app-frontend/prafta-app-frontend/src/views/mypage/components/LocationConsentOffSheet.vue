<!--
  LocationConsentOffSheet.vue — 위치정보 동의(005) 해제 방식 선택 시트 (마이페이지)

  2026-09-02 UI 개선. 종전에는 마이페이지 행에 [일시 중지][동의 철회] 버튼을 나란히 뒀는데,
  ★두 선택지는 결과가 완전히 다르다 — 중지는 기록을 남기고 되돌릴 수 있지만, 철회는 수집된
    좌표를 파기하며 되돌릴 수 없다. 버튼만 나란히 놓으면 그 차이가 화면에 드러나지 않는다.
  그래서 체크박스를 해제할 때 이 시트를 띄우고, 각 선택지에 무슨 일이 일어나는지 적어 고르게 한다.

  - 닫기(X)/배경 탭/ESC = 취소. 어떤 전이도 일어나지 않고 체크 상태는 그대로다.
  - props: modelValue(v-model), saving(전이 API 진행 중 → 버튼 잠금)
  - emits: suspend / withdraw — 실제 API 호출과 철회 확인 팝업은 부모(MyPageView)가 소유한다.
    이 컴포넌트는 "무엇을 고를지"만 받는다(표시 전용).
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="위치정보 제공 해제"
    :show-footer="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <p class="loc-off__lead">어떻게 멈출지 선택해 주세요.</p>

    <!-- 되돌릴 수 있는 쪽을 먼저 둔다(기본 선택지). -->
    <section class="loc-off__card">
      <h3 class="loc-off__title">일시 중지</h3>
      <p class="loc-off__summary">기록은 남기고, 앞으로의 수집만 멈춥니다.</p>
      <ul class="loc-off__points">
        <li v-for="(point, i) in suspendPoints" :key="`suspend-${i}`">{{ point }}</li>
      </ul>
      <button type="button" class="loc-off__btn" :disabled="saving" @click="$emit('suspend')">
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
        <li v-for="(point, i) in withdrawPoints" :key="`withdraw-${i}`">{{ point }}</li>
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
  </BaseBottomSheet>
</template>

<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
// 문구 단일 출처 — 확인 팝업(LOCATION_*_CONFIRM)과 같은 사실을 항목으로 끊어 쓴다.
import { LOCATION_SUSPEND_POINTS, LOCATION_WITHDRAW_POINTS } from '@/utils/locationConsent'

defineProps({
  modelValue: { type: Boolean, default: false },
  // 전이 API 진행 중 — 중복 호출(중지 누르고 곧바로 철회) 방지용 잠금.
  saving: { type: Boolean, default: false },
})

defineEmits(['update:modelValue', 'suspend', 'withdraw'])

const suspendPoints = LOCATION_SUSPEND_POINTS
const withdrawPoints = LOCATION_WITHDRAW_POINTS
</script>

<style scoped>
/* ★토큰 폴백 필수 — 이 앱 프론트에는 :root 전역 토큰이 없고 화면 루트마다 선언한다.
   토큰 미선언 화면에서 열리면 색이 통째로 풀리므로 공용 시트와 같은 폴백 규약을 따른다. */
.loc-off__lead {
  margin: 0 0 var(--space-md, 12px);
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary, #6b7280);
}

.loc-off__card {
  padding: var(--space-md, 12px);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  background: var(--color-surface, #ffffff);
}
.loc-off__card + .loc-off__card {
  margin-top: var(--space-sm, 8px);
}
.loc-off__card--danger {
  border-color: var(--color-danger, #ef4444);
}

.loc-off__title {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 8px);
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}
.loc-off__title--danger {
  color: var(--color-danger, #ef4444);
}
/* 철회에만 붙는 경고 태그 — 되돌릴 수 없다는 사실을 제목 줄에서 먼저 보게 한다. */
.loc-off__tag {
  padding: 1px 8px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-danger, #ef4444);
  color: var(--color-on-danger, #ffffff);
  font-size: 11px;
  font-weight: 500;
  line-height: 1.6;
}

.loc-off__summary {
  margin: var(--space-xs, 4px) 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary, #6b7280);
  word-break: keep-all;
}

.loc-off__points {
  margin: var(--space-sm, 8px) 0 0;
  padding-left: 18px;
  list-style: disc;
}
.loc-off__points li {
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-secondary, #6b7280);
  word-break: keep-all;
}

.loc-off__btn {
  width: 100%;
  min-height: 40px;
  margin-top: var(--space-md, 12px);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text-primary, #111827);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
}
.loc-off__btn:disabled {
  opacity: 0.5;
  cursor: default;
}
.loc-off__btn--danger {
  border-color: var(--color-danger, #ef4444);
  background: var(--color-danger, #ef4444);
  color: var(--color-on-danger, #ffffff);
}
</style>
