<!--
  TimeStepperField.vue — 공통 시각 선택 필드 + 스텝퍼 모달
  - 목적: 앱 webview 에서 native <input type="time"> 의 OS 기본 피커를
    앱 디자인(초록 계열)에 맞춘 인앱 모달로 대체한다. DateStepperField 와 동형.
  - UI: 시 / 분 2열, 각 열마다 값 박스 + (+,−) 버튼. 하단 액션은 [취소][설정] 2종.
  - v-model: 'HH:MM' 24시간 문자열 (기존 native time input 과 동일 포맷 → 교체 시 부모 로직 무변경).
  - 색상: 부모 토큰에 의존하지 않도록 초록 계열을 리터럴로 고정(어느 화면에서도 동일).
-->
<template>
  <button
    type="button"
    class="tsf-field"
    :class="{ 'tsf-field--placeholder': !displayText, 'tsf-field--disabled': disabled }"
    :disabled="disabled"
    @click="open"
  >
    <span class="tsf-field__text">{{ displayText || placeholder }}</span>
    <svg class="tsf-field__ic" width="18" height="18" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <polyline points="12 7 12 12 15 14" />
    </svg>
  </button>

  <Teleport to="body">
    <div v-if="isOpen" class="tsp-dimmer" @click.self="onCancel">
      <div class="tsp-card" role="dialog" aria-modal="true" aria-label="시각 선택">
        <p class="tsp-preview">{{ previewText }}</p>

        <div class="tsp-cols">
          <!-- 시 -->
          <div class="tsp-col">
            <div class="tsp-box">
              <input
                class="tsp-val"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="2"
                :value="hourText"
                aria-label="시"
                @focus="onFocusSelect"
                @input="onHourInput"
                @blur="onHourBlur"
              />
              <span class="tsp-unit">시</span>
            </div>
            <div class="tsp-pm">
              <button type="button" class="tsp-pm__btn" aria-label="시 증가" @click="stepHour(1)">+</button>
              <button type="button" class="tsp-pm__btn" aria-label="시 감소" @click="stepHour(-1)">−</button>
            </div>
          </div>
          <!-- 분 -->
          <div class="tsp-col">
            <div class="tsp-box">
              <input
                class="tsp-val"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="2"
                :value="minuteText"
                aria-label="분"
                @focus="onFocusSelect"
                @input="onMinuteInput"
                @blur="onMinuteBlur"
              />
              <span class="tsp-unit">분</span>
            </div>
            <div class="tsp-pm">
              <button type="button" class="tsp-pm__btn" aria-label="분 증가" @click="stepMinute(1)">+</button>
              <button type="button" class="tsp-pm__btn" aria-label="분 감소" @click="stepMinute(-1)">−</button>
            </div>
          </div>
        </div>

        <div class="tsp-actions">
          <button type="button" class="tsp-btn tsp-btn--ghost" @click="onCancel">취소</button>
          <button type="button" class="tsp-btn tsp-btn--primary" @click="onConfirm">설정</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  // 'HH:MM' 24시간 문자열 (빈값이면 미선택)
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '시각 선택' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const hour = ref(0)
const minute = ref(0)

// 'HH:MM' → {h,m} | null
const parse = (s) => {
  if (!s || typeof s !== 'string') return null
  const m = s.match(/^(\d{1,2}):(\d{1,2})$/)
  if (!m) return null
  const h = Number(m[1])
  const mi = Number(m[2])
  if (h > 23 || mi > 59) return null
  return { h, m: mi }
}

const pad2 = (n) => String(n).padStart(2, '0')

// 표시용 텍스트 (필드)
const displayText = computed(() => {
  const p = parse(props.modelValue)
  if (!p) return ''
  return `${pad2(p.h)}:${pad2(p.m)}`
})

// 모달 입력칸 표시값 (편집 중 0 도 그대로 보이도록 2자리 패딩)
const hourText = computed(() => pad2(hour.value))
const minuteText = computed(() => pad2(minute.value))

// 모달 상단 미리보기
const previewText = computed(() => `${pad2(hour.value)}시 ${pad2(minute.value)}분`)

const open = () => {
  if (props.disabled) return
  const init = parse(props.modelValue) || (() => {
    const t = new Date()
    return { h: t.getHours(), m: t.getMinutes() }
  })()
  hour.value = init.h
  minute.value = init.m
  isOpen.value = true
}

// 시: 0~23 순환
const stepHour = (delta) => {
  let h = hour.value + delta
  if (h > 23) h = 0
  if (h < 0) h = 23
  hour.value = h
}
// 분: 0~59 순환
const stepMinute = (delta) => {
  let m = minute.value + delta
  if (m > 59) m = 0
  if (m < 0) m = 59
  minute.value = m
}

// ── 키인(직접 입력) ──────────────────────────────────────
const onFocusSelect = (e) => {
  e.target.select()
}
const onHourInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  hour.value = digits === '' ? 0 : Number(digits)
}
const onMinuteInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  minute.value = digits === '' ? 0 : Number(digits)
}
const onHourBlur = () => {
  hour.value = Math.min(23, Math.max(0, hour.value || 0))
}
const onMinuteBlur = () => {
  minute.value = Math.min(59, Math.max(0, minute.value || 0))
}

// 설정 직전 최종 보정(입력 도중 blur 없이 확정하는 경우 방어).
const normalize = () => {
  hour.value = Math.min(23, Math.max(0, hour.value || 0))
  minute.value = Math.min(59, Math.max(0, minute.value || 0))
}

const onConfirm = () => {
  normalize()
  emit('update:modelValue', `${pad2(hour.value)}:${pad2(minute.value)}`)
  isOpen.value = false
}
const onCancel = () => {
  isOpen.value = false
}
</script>

<style scoped>
/* ── 필드(트리거) ───────────────────────────────────────── */
.tsf-field {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: var(--color-surface, #ffffff);
  border: 0.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  padding: 0 12px;
  font-size: 14px;
  color: var(--color-text-primary, #111827);
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  text-align: left;
  cursor: pointer;
  box-sizing: border-box;
}
.tsf-field--placeholder {
  color: var(--color-text-tertiary, #9ca3af);
}
.tsf-field--disabled {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}
.tsf-field__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tsf-field__ic {
  flex-shrink: 0;
  color: #16a34a;
}

/* ── 모달 ───────────────────────────────────────────────── */
.tsp-dimmer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}
.tsp-card {
  width: 100%;
  max-width: 360px;
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 16px 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}
.tsp-preview {
  margin: 0 0 16px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.tsp-cols {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.tsp-col {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  min-width: 0;
}
.tsp-box {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tsp-unit {
  flex-shrink: 0;
  font-size: 13px;
  color: #374151;
}
.tsp-val {
  flex: 1;
  width: 100%;
  min-width: 0;
  height: 56px;
  text-align: center;
  background: #ffffff;
  color: #111827;
  border: 1.5px solid #16a34a;
  border-radius: 10px;
  padding: 0 4px;
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  font-family: inherit;
  box-sizing: border-box;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
}
.tsp-val:focus {
  border-color: #15803d;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.18);
}
.tsp-pm {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.tsp-pm__btn {
  height: 40px;
  border: 1px solid #16a34a;
  border-radius: 8px;
  background: #f0fdf4;
  color: #16a34a;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  font-family: inherit;
  -webkit-user-select: none;
  user-select: none;
}
.tsp-pm__btn:active {
  background: #dcfce7;
}

.tsp-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 18px;
}
.tsp-btn {
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.tsp-btn--ghost {
  background: #f3f4f6;
  border: 0;
  color: #6b7280;
}
.tsp-btn--primary {
  background: #16a34a;
  border: 0;
  color: #ffffff;
}
</style>
