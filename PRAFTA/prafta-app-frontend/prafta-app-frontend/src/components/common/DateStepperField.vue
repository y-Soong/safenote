<!--
  DateStepperField.vue — 공통 날짜 선택 필드 + 스텝퍼 모달
  - 목적: 앱 webview 에서 native <input type="date"> 의 OS 기본 피커(+/- 스피너, 설정/삭제/취소)를
    앱 디자인(초록 계열)에 맞춘 인앱 모달로 대체한다.
  - UI: 년 / 월 / 일 3열, 각 열마다 값 박스 + (+,−) 버튼. 하단 액션은 [취소][설정] 2종.
  - v-model: 'YYYY-MM-DD' 문자열 (기존 native date input 과 동일 포맷 → 교체 시 부모 로직 무변경).
  - 색상: 부모 토큰에 의존하지 않도록 초록 계열을 리터럴로 고정(어느 화면에서도 동일).
-->
<template>
  <button
    type="button"
    class="dsf-field"
    :class="{ 'dsf-field--placeholder': !displayText, 'dsf-field--disabled': disabled }"
    :disabled="disabled"
    @click="open"
  >
    <span class="dsf-field__text">{{ displayText || placeholder }}</span>
    <svg class="dsf-field__ic" width="18" height="18" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  </button>

  <Teleport to="body">
    <div v-if="isOpen" class="dsp-dimmer" @click.self="onCancel">
      <div class="dsp-card" role="dialog" aria-modal="true" aria-label="날짜 선택">
        <p class="dsp-preview">{{ previewText }}</p>

        <div class="dsp-cols">
          <!-- 년 -->
          <div class="dsp-col dsp-col--year">
            <div class="dsp-box">
              <input
                class="dsp-val"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="4"
                :value="year"
                aria-label="연도"
                @focus="onFocusSelect"
                @input="onYearInput"
                @blur="onYearBlur"
              />
              <span class="dsp-unit">년</span>
            </div>
            <div class="dsp-pm">
              <button type="button" class="dsp-pm__btn" aria-label="연도 증가" @click="stepYear(1)">+</button>
              <button type="button" class="dsp-pm__btn" aria-label="연도 감소" @click="stepYear(-1)">−</button>
            </div>
          </div>
          <!-- 월 -->
          <div class="dsp-col">
            <div class="dsp-box">
              <input
                class="dsp-val"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="2"
                :value="month"
                aria-label="월"
                @focus="onFocusSelect"
                @input="onMonthInput"
                @blur="onMonthBlur"
              />
              <span class="dsp-unit">월</span>
            </div>
            <div class="dsp-pm">
              <button type="button" class="dsp-pm__btn" aria-label="월 증가" @click="stepMonth(1)">+</button>
              <button type="button" class="dsp-pm__btn" aria-label="월 감소" @click="stepMonth(-1)">−</button>
            </div>
          </div>
          <!-- 일 -->
          <div class="dsp-col">
            <div class="dsp-box">
              <input
                class="dsp-val"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="2"
                :value="day"
                aria-label="일"
                @focus="onFocusSelect"
                @input="onDayInput"
                @blur="onDayBlur"
              />
              <span class="dsp-unit">일</span>
            </div>
            <div class="dsp-pm">
              <button type="button" class="dsp-pm__btn" aria-label="일 증가" @click="stepDay(1)">+</button>
              <button type="button" class="dsp-pm__btn" aria-label="일 감소" @click="stepDay(-1)">−</button>
            </div>
          </div>
        </div>

        <div class="dsp-actions">
          <button type="button" class="dsp-btn dsp-btn--ghost" @click="onCancel">취소</button>
          <button type="button" class="dsp-btn dsp-btn--primary" @click="onConfirm">설정</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  // 'YYYY-MM-DD' 문자열 (빈값이면 미선택)
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '날짜 선택' },
  disabled: { type: Boolean, default: false },
  // 스텝퍼 연도 클램프 범위
  minYear: { type: Number, default: 1900 },
  maxYear: { type: Number, default: 2100 },
})
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const year = ref(2000)
const month = ref(1)
const day = ref(1)

// 'YYYY-MM-DD' → {y,m,d} | null
const parse = (s) => {
  if (!s || typeof s !== 'string') return null
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return null
  return { y: Number(m[1]), m: Number(m[2]), d: Number(m[3]) }
}

// 해당 연/월의 마지막 일수 (m: 1~12)
const daysInMonth = (y, mo) => new Date(y, mo, 0).getDate()

// 표시용 텍스트 (필드)
const displayText = computed(() => {
  const p = parse(props.modelValue)
  if (!p) return ''
  return `${p.y}-${String(p.m).padStart(2, '0')}-${String(p.d).padStart(2, '0')}`
})

// 모달 상단 미리보기
const previewText = computed(() => `${year.value}년 ${month.value}월 ${day.value}일`)

const open = () => {
  if (props.disabled) return
  const init = parse(props.modelValue) || (() => {
    const t = new Date()
    return { y: t.getFullYear(), m: t.getMonth() + 1, d: t.getDate() }
  })()
  year.value = Math.min(props.maxYear, Math.max(props.minYear, init.y))
  month.value = init.m
  day.value = init.d
  isOpen.value = true
}

// 연/월 변경 후 일자가 말일을 넘으면 말일로 보정
const clampDay = () => {
  const max = daysInMonth(year.value, month.value)
  if (day.value > max) day.value = max
}

const stepYear = (delta) => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value + delta))
  clampDay()
}
const stepMonth = (delta) => {
  let m = month.value + delta
  if (m > 12) m = 1
  if (m < 1) m = 12
  month.value = m
  clampDay()
}
const stepDay = (delta) => {
  const max = daysInMonth(year.value, month.value)
  let d = day.value + delta
  if (d > max) d = 1
  if (d < 1) d = max
  day.value = d
}

// ── 키인(직접 입력) ──────────────────────────────────────
// 값을 누르면 전체 선택 → 사용자가 입력한 값으로 즉시 치환되도록.
const onFocusSelect = (e) => {
  e.target.select()
}
// 입력 중에는 숫자만 허용/자릿수 제한만 하고, 범위 보정은 blur 에서.
const onYearInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 4)
  year.value = digits === '' ? 0 : Number(digits)
}
const onMonthInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  month.value = digits === '' ? 0 : Number(digits)
}
const onDayInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  day.value = digits === '' ? 0 : Number(digits)
}
// blur 시 유효 범위로 보정.
const onYearBlur = () => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  clampDay()
}
const onMonthBlur = () => {
  month.value = Math.min(12, Math.max(1, month.value || 1))
  clampDay()
}
const onDayBlur = () => {
  const max = daysInMonth(year.value, month.value)
  day.value = Math.min(max, Math.max(1, day.value || 1))
}

// 설정 직전 최종 보정(입력 도중 blur 없이 확정하는 경우 방어).
const normalize = () => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  month.value = Math.min(12, Math.max(1, month.value || 1))
  const max = daysInMonth(year.value, month.value)
  day.value = Math.min(max, Math.max(1, day.value || 1))
}

const onConfirm = () => {
  normalize()
  const mm = String(month.value).padStart(2, '0')
  const dd = String(day.value).padStart(2, '0')
  emit('update:modelValue', `${year.value}-${mm}-${dd}`)
  isOpen.value = false
}
const onCancel = () => {
  isOpen.value = false
}
</script>

<style scoped>
/* ── 필드(트리거) ───────────────────────────────────────── */
.dsf-field {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  /* 토큰 정의 화면이면 토큰, 아니면 리터럴 폴백 */
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
.dsf-field--placeholder {
  color: var(--color-text-tertiary, #9ca3af);
}
.dsf-field--disabled {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}
.dsf-field__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dsf-field__ic {
  flex-shrink: 0;
  color: #16a34a;
}

/* ── 모달 ───────────────────────────────────────────────── */
.dsp-dimmer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}
.dsp-card {
  width: 100%;
  max-width: 360px;
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 16px 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}
.dsp-preview {
  margin: 0 0 16px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.dsp-cols {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.dsp-col {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  min-width: 0;
}
.dsp-col--year {
  flex: 1.6 1 0; /* 연도 4자리 + 단위라 더 넓게 */
}
/* 값 입력칸 + 단위(년/월/일) 가로 배치 */
.dsp-box {
  display: flex;
  align-items: center;
  gap: 6px;
}
.dsp-unit {
  flex-shrink: 0;
  font-size: 13px;
  color: #374151;
}
/* 값 박스 = 키인 가능한 input. 흰 배경 + 초록 테두리 + 검정 텍스트. */
.dsp-val {
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
.dsp-val:focus {
  border-color: #15803d;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.18);
}
.dsp-pm {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.dsp-pm__btn {
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
.dsp-pm__btn:active {
  background: #dcfce7;
}

.dsp-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 18px;
}
.dsp-btn {
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.dsp-btn--ghost {
  background: #f3f4f6;
  border: 0;
  color: #6b7280;
}
.dsp-btn--primary {
  background: #16a34a;
  border: 0;
  color: #ffffff;
}
</style>
