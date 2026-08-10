<!--
  LeaveExpiryCallout.vue — 소멸 임박 콜아웃 (조건부, 세션 한정 닫기)
  - 작업: prafta-app-005 슬롯 D (UI 명세: UI-A005)
  - 시안 §4.3 / 노출 조건은 부모(전체 토글 + info.exists)에서 판정. 본 컴포넌트는 표시 + 닫기만.
  - "N일 후 소멸되는 연차 X일" 한 줄. 닫기(×) → emit('close') (세션 한정 — §3.3).
  - 토큰은 부모(.my-leave-view) 루트에서 상속.
-->
<template>
  <div class="warn">
    <svg class="warn__ico icon" width="20" height="20" aria-hidden="true">
      <use href="#i-lv-warn" />
    </svg>
    <div class="warn__body">
      <p class="warn__title">{{ titleText }}</p>
    </div>
    <button type="button" class="warn__close" aria-label="닫기" @click="$emit('close')">
      <svg class="icon" width="18" height="18" aria-hidden="true">
        <use href="#i-lv-x" />
      </svg>
    </button>

    <!-- 본 컴포넌트 전용 sprite -->
    <svg width="0" height="0" class="warn-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-lv-warn"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 9v4" />
          <path
            d="M10.363 3.591l-8.106 13.534a1.914 1.914 0 0 0 1.636 2.871h16.214a1.914 1.914 0 0 0 1.636 -2.87l-8.106 -13.536a1.914 1.914 0 0 0 -3.274 0z"
          />
          <path d="M12 16h.01" />
        </symbol>
        <symbol
          id="i-lv-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M18 6l-12 12" />
          <path d="M6 6l12 12" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatLeaveDaysOnly } from '@/utils/leaveFormat'

const props = defineProps({
  // expiringSoon: { exists, daysUntilExpiry, totalRemainingDays, expiryDate }
  info: {
    type: Object,
    default: null,
  },
})

defineEmits(['close'])

// "N일 후 소멸되는 연차 X일" (시안 §4.3 고정 문구)
//   2026-08-09 규약: 일 단위 단독 표기 — 구 E4 분모(convMinutes prop) 시간 환산 제거.
const titleText = computed(() => {
  const days = props.info?.daysUntilExpiry ?? 0
  const remain = formatLeaveDaysOnly(props.info?.totalRemainingDays ?? 0)
  return `${days}일 후 소멸되는 연차 ${remain}`
})
</script>

<style scoped>
.warn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 8px 12px 14px;
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border);
  border-radius: 12px;
}
.warn__ico {
  flex-shrink: 0;
  color: var(--color-warning);
}
.warn__body {
  flex: 1;
  min-width: 0;
}
.warn__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-warning-text);
}
.warn__close {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--color-warning-text);
  cursor: pointer;
  font-family: inherit;
}
.warn-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
