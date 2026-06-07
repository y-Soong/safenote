<!--
  TbmExitSignSheet.vue — 퇴실 비밀번호 + 서명패드 바텀시트
  - 작업 ID: PRAFTA-TBM-EXIT-SIGN (분해: prafta-app-tbm-user-detail-plan.md §4 F7)
  - 트리거: 교육중 화면에서 "교육완료" → 상태조회(A5) ENDED 확인 후 부모가 open.
  - 비번 + 서명(PNG) 수집 → submit emit. 실제 POST /appApi/tbm/exit(multipart)는 부모/developer 담당.
  - 서명 캔버스(pointer 드로잉)는 TbmEntryView.vue 의 검증된 로직을 이식(UI 캡처 영역, 비즈니스 아님).
  - 참조 패턴: BaseBottomSheet.vue, TbmEntryView 서명 캔버스.
  - planner 라운드 스코프: template + style + 서명 캔버스 UI 로직(드로잉/지우기/파일변환) 완성.
    submit 시 비번/서명파일을 부모로 emit 만 하고 API 호출은 하지 않는다.
-->
<template>
  <transition name="tbm-sheet-fade">
    <div
      v-if="modelValue"
      class="exit-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="퇴실 서명"
      @click.self="onClose"
    >
      <div class="exit-sheet">
        <div class="exit-sheet__handle" aria-hidden="true"></div>

        <header class="exit-sheet__header">
          <h2 class="exit-sheet__title">교육 완료</h2>
          <button type="button" class="exit-sheet__close" aria-label="닫기" @click="onClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="exit-sheet__body">
          <label class="exit-field">
            <span class="exit-field__label">종료 비밀번호</span>
            <input
              v-model="exitPwd"
              class="exit-field__input"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="off"
              placeholder="6자리 숫자"
            />
          </label>

          <div class="sign-box">
            <div class="sign-box__head">
              <p class="sign-box__hint">서명<span class="exit-req">*</span></p>
              <button type="button" class="sign-box__clear" @click="clearSignature">지우기</button>
            </div>
            <canvas
              ref="signCanvasRef"
              class="sign-box__pad"
              aria-label="서명 영역"
              @pointerdown="onSignPointerDown"
              @pointermove="onSignPointerMove"
              @pointerup="onSignPointerUp"
              @pointerleave="onSignPointerUp"
              @pointercancel="onSignPointerUp"
            ></canvas>
          </div>

          <p v-if="errorMsg" class="exit-form-error">{{ errorMsg }}</p>
        </div>

        <footer class="exit-sheet__footer">
          <button
            type="button"
            class="exit-sheet__btn exit-sheet__btn--primary"
            :disabled="!canSubmit || submitting"
            @click="onSubmit"
          >
            완료하기
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
})

// submit: { exitPwd, signFile(File|null) } — exit API(multipart)는 부모/developer 가 처리
const emit = defineEmits(['update:modelValue', 'submit'])

// 입력 바인딩
const exitPwd = ref('')

// 서명 캔버스
const signCanvasRef = ref(null)
const hasSignature = ref(false)

const canSubmit = computed(() => exitPwd.value.length === 6 && hasSignature.value)

const onClose = () => {
  emit('update:modelValue', false)
}

// ── 서명 캔버스(pointer 드로잉 — TbmEntryView 이식) ─────────────────
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
      resolve(new File([blob], 'tbm-exit-signature.png', { type: 'image/png' }))
    }, 'image/png')
  })

const onSubmit = async () => {
  if (!canSubmit.value || props.submitting) return
  const signFile = await signatureToFile()
  emit('submit', { exitPwd: exitPwd.value, signFile })
}

// 열릴 때 초기화 + 캔버스 셋업(레이아웃 후)
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      exitPwd.value = ''
      hasSignature.value = false
      signCtx = null
      await nextTick()
      setupSignCanvas()
    }
  },
)
</script>

<style scoped>
.exit-sheet__dimmer {
  /* 토큰 자급 */
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
.exit-sheet {
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
.exit-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.exit-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.exit-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.exit-sheet__close {
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
.exit-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.exit-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.exit-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.exit-field__input {
  height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.exit-field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.sign-box {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.sign-box__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sign-box__hint {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.exit-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.sign-box__clear {
  background: transparent;
  border: 0;
  padding: var(--space-xs) var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.sign-box__pad {
  display: block;
  width: 100%;
  height: 160px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  touch-action: none;
}
.exit-form-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}
.exit-sheet__footer {
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.exit-sheet__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.exit-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.exit-sheet__btn--primary:disabled {
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
