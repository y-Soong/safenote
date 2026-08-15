<!--
  BaseBottomSheet.vue — 바텀시트 공통 베이스 (전 화면 공통)
  - 작업 ID: PRAFTA-APP-006-2 / HB-14(F-6): views/req/components → components/common 으로 이동(Q7-b).
    공용 컴포넌트가 views 하위를 참조하지 않도록 위치만 옮겼고 내용은 불변이다.
  - props: modelValue (v-model), title, showFooter
  - slots: default(옵션 영역), footer (showFooter 일 때만 노출)
  - ⚠️ 배경 스크롤 잠금/해제는 이 컴포넌트 소유다. 사용처에서 중복 구현 금지(과거 2회 재발 — 잔결함 F-6).
-->
<template>
  <transition name="req-sheet-fade">
    <div
      v-if="modelValue"
      class="req-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      @click.self="onClose"
      @touchmove.self.prevent
    >
      <div class="req-sheet" :aria-label="title">
        <div class="req-sheet__handle" aria-hidden="true"></div>
        <header class="req-sheet__header">
          <h2 class="req-sheet__title">{{ title }}</h2>
          <button
            ref="closeBtnRef"
            type="button"
            class="req-sheet__close"
            aria-label="닫기"
            @click="onClose"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="req-sheet__body">
          <slot />
        </div>

        <footer v-if="showFooter" class="req-sheet__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  showFooter: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])

const closeBtnRef = ref(null)

// 배경(뒤 화면) 스크롤 잠금 — 시트가 열려 있는 동안 document 스크롤을 막아
//   시트 대신 뒤 화면이 스크롤되던 문제를 차단한다(핸들/헤더/푸터 드래그 포함 전 영역).
let prevBodyOverflow = ''
let prevHtmlOverflow = ''
const lockBackgroundScroll = () => {
  prevBodyOverflow = document.body.style.overflow
  // 일부 WebView 는 <html>(documentElement)이 스크롤 컨테이너이므로 둘 다 잠근다.
  prevHtmlOverflow = document.documentElement.style.overflow
  document.body.style.overflow = 'hidden'
  document.documentElement.style.overflow = 'hidden'
}
const unlockBackgroundScroll = () => {
  document.body.style.overflow = prevBodyOverflow
  document.documentElement.style.overflow = prevHtmlOverflow
}

const onClose = () => {
  emit('update:modelValue', false)
}

const onKeyDown = (e) => {
  if (e.key === 'Escape' && props.modelValue) {
    onClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  // 마운트 시점에 이미 열려 있으면(초기 open=true) 즉시 잠금.
  if (props.modelValue) lockBackgroundScroll()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  // 열린 채로 언마운트되어도 잠금이 남지 않도록 해제.
  unlockBackgroundScroll()
})

// 시트 열림/닫힘에 따라 배경 스크롤 잠금 토글 + 열림 시 닫기 버튼 포커스(1차 단순 포커스 트랩).
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      lockBackgroundScroll()
      await nextTick()
      try {
        closeBtnRef.value?.focus?.()
      } catch (_e) {
        // 포커스 실패 무시
      }
    } else {
      unlockBackgroundScroll()
    }
  },
)
</script>

<style scoped>
.req-sheet__dimmer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;
}

/* ★토큰 폴백 필수 — 이 앱 프론트에는 :root 전역 토큰이 없고 화면 루트마다 선언한다.
   토큰을 선언하지 않은 화면에서 이 시트를 열면 background 가 값 없음으로 풀려 투명하게 뜬다
   (2026-08-15 기간 연차 신청 화면 실기기 결함). 공용 인프라라 폴백으로 fail-safe 를 건다. */
.req-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface, #ffffff);
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  padding: 8px 0 calc(16px + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.req-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border, #e5e7eb);
  border-radius: var(--radius-full, 9999px);
  margin: 4px auto 8px;
}

.req-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px 8px;
}

.req-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary, #111827);
}

.req-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary, #6b7280);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.req-sheet__body {
  flex: 1;
  overflow-y: auto;
  /* 시트 내부 스크롤이 끝에 닿아도 뒤 화면으로 스크롤이 전파(chaining)되지 않게 한다.
     (결재자 추가 등에서 시트 대신 뒤 화면이 스크롤되던 문제 해결) */
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 4px 16px 12px;
}

.req-sheet__footer {
  padding: 8px 16px 0;
  border-top: 0.5px solid var(--color-border-light, #f3f4f6);
}

.req-sheet-fade-enter-active,
.req-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.req-sheet-fade-enter-from,
.req-sheet-fade-leave-to {
  opacity: 0;
}
</style>
