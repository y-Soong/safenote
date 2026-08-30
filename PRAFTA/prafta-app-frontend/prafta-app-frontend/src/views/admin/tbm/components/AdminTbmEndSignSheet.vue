<!--
  AdminTbmEndSignSheet.vue — 주관자(개설자) 서명 바텀시트
  - 작업: tbm04-manager-sign T5/T6 (분해: tbm04-manager-sign.plan.md)
  - 용도: ①교육 종료(AdminTbmLiveView — 서명 후 종료) ②이력 상세 사후서명(AdminTbmHistoryDetailView).
  - TbmExitSignSheet.vue 검증본을 복제 후 간소화(종료 비밀번호 블록 제거, canSubmit=서명 단독).
    서명 캔버스(pointer 드로잉) 로직은 원본 그대로 이식(신규 작성 금지 — 골격 로직버그 QA 사각 방지).
  - submit 시 { signFile } 만 emit. 실제 POST(multipart 'item')는 부모가 처리.
  - title/notice/submitLabel 은 사용처(종료/사후서명)별 문구가 달라 prop 화(기본값=종료 플로우).
-->
<template>
  <transition name="tbm-sheet-fade">
    <div
      v-if="modelValue"
      class="end-sign-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="주관자 서명"
      @click.self="onClose"
    >
      <div class="end-sign-sheet">
        <div class="end-sign-sheet__handle" aria-hidden="true"></div>

        <header class="end-sign-sheet__header">
          <h2 class="end-sign-sheet__title">{{ title }}</h2>
          <button type="button" class="end-sign-sheet__close" aria-label="닫기" @click="onClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="end-sign-sheet__body">
          <p class="end-sign-sheet__notice">{{ notice }}</p>

          <div class="end-sign-box">
            <div class="end-sign-box__head">
              <p class="end-sign-box__hint">주관자 서명<span class="end-sign-req">*</span></p>
              <button type="button" class="end-sign-box__clear" @click="clearSignature">지우기</button>
            </div>
            <canvas
              ref="signCanvasRef"
              class="end-sign-box__pad"
              aria-label="서명 영역"
              @pointerdown="onSignPointerDown"
              @pointermove="onSignPointerMove"
              @pointerup="onSignPointerUp"
              @pointerleave="onSignPointerUp"
              @pointercancel="onSignPointerUp"
            ></canvas>
          </div>

          <p v-if="errorMsg" class="end-sign-form-error">{{ errorMsg }}</p>
        </div>

        <footer class="end-sign-sheet__footer">
          <button
            type="button"
            class="end-sign-sheet__btn end-sign-sheet__btn--primary"
            :disabled="!canSubmit || submitting"
            @click="onSubmit"
          >
            {{ submitLabel }}
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  errorMsg: { type: String, default: '' },
  title: { type: String, default: '교육 종료' },
  notice: {
    type: String,
    default:
      '주관자 서명 후 교육이 종료됩니다. 종료 후에도 근로자가 직접 완료(서명)해야 이수 처리돼요.',
  },
  submitLabel: { type: String, default: '서명하고 종료하기' },
})

// submit: { signFile(File|null) } — POST(multipart 'item')는 부모가 처리
const emit = defineEmits(['update:modelValue', 'submit'])

// 서명 캔버스
const signCanvasRef = ref(null)
const hasSignature = ref(false)

const canSubmit = computed(() => hasSignature.value)

const onClose = () => {
  emit('update:modelValue', false)
}

// ── 서명 캔버스(pointer 드로잉 — TbmExitSignSheet 검증본 이식) ─────────
let signCtx = null
let signDrawing = false

const setupSignCanvas = () => {
  const canvas = signCanvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.max(1, Math.round(rect.width * dpr))
  canvas.height = Math.max(1, Math.round(rect.height * dpr))
  signCtx = canvas.getContext('2d')
  signCtx.scale(dpr, dpr)
  signCtx.lineWidth = 2
  signCtx.lineCap = 'round'
  signCtx.lineJoin = 'round'
  signCtx.strokeStyle = '#111827' // 서명 잉크(고정 — 서명 이미지 표준 색)
  hasSignature.value = false
}

const pointFromEvent = (e) => {
  const canvas = signCanvasRef.value
  const rect = canvas.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

const onSignPointerDown = (e) => {
  if (!signCtx) setupSignCanvas()
  if (!signCtx) return
  signDrawing = true
  const p = pointFromEvent(e)
  signCtx.beginPath()
  signCtx.moveTo(p.x, p.y)
  try {
    signCanvasRef.value.setPointerCapture(e.pointerId)
  } catch (err) {
    // setPointerCapture 미지원 환경 무시
  }
}

const onSignPointerMove = (e) => {
  if (!signDrawing || !signCtx) return
  const p = pointFromEvent(e)
  signCtx.lineTo(p.x, p.y)
  signCtx.stroke()
  hasSignature.value = true
}

const onSignPointerUp = () => {
  signDrawing = false
}

const clearSignature = () => {
  const canvas = signCanvasRef.value
  if (!canvas || !signCtx) return
  signCtx.save()
  signCtx.setTransform(1, 0, 0, 1, 0, 0)
  signCtx.clearRect(0, 0, canvas.width, canvas.height)
  signCtx.restore()
  hasSignature.value = false
}

const signatureToFile = () =>
  new Promise((resolve) => {
    const canvas = signCanvasRef.value
    if (!canvas) {
      resolve(null)
      return
    }
    canvas.toBlob((blob) => {
      if (!blob) {
        resolve(null)
        return
      }
      resolve(new File([blob], 'tbm-manager-signature.png', { type: 'image/png' }))
    }, 'image/png')
  })

const onSubmit = async () => {
  if (!canSubmit.value || props.submitting) return
  const signFile = await signatureToFile()
  emit('submit', { signFile })
}

// 열릴 때 초기화 + 캔버스 셋업(레이아웃 후)
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      hasSignature.value = false
      signCtx = null
      await nextTick()
      setupSignCanvas()
    }
  },
)
</script>

<style scoped>
.end-sign-sheet__dimmer {
  /* 토큰 자급(앱 프론트 :root 없음 — 루트마다 선언 필수) */
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-danger-text: #b91c1c;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 120;
}
.end-sign-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 88vh;
}
.end-sign-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.end-sign-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.end-sign-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.end-sign-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.end-sign-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.end-sign-sheet__notice {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  word-break: keep-all;
}
.end-sign-box {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.end-sign-box__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.end-sign-box__hint {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.end-sign-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.end-sign-box__clear {
  background: transparent;
  border: 0;
  padding: var(--space-xs) var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.end-sign-box__pad {
  display: block;
  width: 100%;
  height: 160px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  touch-action: none;
}
.end-sign-form-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}
.end-sign-sheet__footer {
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.end-sign-sheet__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.end-sign-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.end-sign-sheet__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.tbm-sheet-fade-enter-active,
.tbm-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.tbm-sheet-fade-enter-from,
.tbm-sheet-fade-leave-to {
  opacity: 0;
}
</style>
