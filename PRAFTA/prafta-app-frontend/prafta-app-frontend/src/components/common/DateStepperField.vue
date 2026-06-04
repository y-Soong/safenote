<!--
  DateStepperField.vue — 공통 날짜 선택 필드 + 휠(드럼) 바텀시트
  - 목적: 앱 webview 에서 native <input type="date"> 의 OS 기본 피커를
    토스 스타일 휠 바텀시트(연/월/일 드럼 + 직접입력)로 대체한다.
  - UI: 트리거 필드 → 하단 시트(키보드 직접입력 + 연/월/일 휠 3열) → [확인].
  - v-model: 'YYYY-MM-DD' 문자열 (기존 native date input 과 동일 포맷 → 교체 시 부모 로직 무변경).
  - 색상: 앱 브랜드 초록 계열을 리터럴로 고정(시트가 body 로 teleport 되어 화면 토큰 밖이므로).
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
    <svg
      class="dsf-field__ic"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  </button>

  <Teleport to="body">
    <div v-if="isOpen" class="wp-dim" :class="{ 'wp-dim--on': shown }" @click.self="onCancel">
      <div
        class="wp-sheet"
        :class="{ 'wp-sheet--on': shown }"
        role="dialog"
        aria-modal="true"
        aria-label="날짜 선택"
      >
        <div class="wp-grip" aria-hidden="true"></div>
        <div class="wp-top">
          <span class="wp-top__title">날짜 선택</span>
          <button type="button" class="wp-top__close" aria-label="닫기" @click="onCancel">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <!-- 직접 입력 필드 -->
        <div class="wp-keyin">
          <input
            class="wp-keyin__in wp-keyin__in--y"
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
          <span class="wp-keyin__dot">.</span>
          <input
            class="wp-keyin__in"
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
          <span class="wp-keyin__dot">.</span>
          <input
            class="wp-keyin__in"
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
        </div>

        <!-- 휠 -->
        <div class="wp-picker">
          <div class="wp-band" aria-hidden="true"></div>
          <div class="wp-wheels wp-wheels--date">
            <div ref="yEl" class="wp-wheel" aria-label="연도 휠">
              <div class="wp-pad"></div>
              <div v-for="v in years" :key="'y' + v" class="wp-item">{{ v }}</div>
              <div class="wp-pad"></div>
            </div>
            <div ref="mEl" class="wp-wheel" aria-label="월 휠">
              <div class="wp-pad"></div>
              <div v-for="v in months" :key="'m' + v" class="wp-item">{{ v }}</div>
              <div class="wp-pad"></div>
            </div>
            <div ref="dEl" class="wp-wheel" aria-label="일 휠">
              <div class="wp-pad"></div>
              <div v-for="v in days" :key="'d' + v" class="wp-item">{{ v }}</div>
              <div class="wp-pad"></div>
            </div>
          </div>
        </div>

        <button type="button" class="wp-confirm" @click="onConfirm">확인</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { scrollToIndex, centerIndex, attachWheelScroll } from '@/utils/wheelPicker'

const props = defineProps({
  // 'YYYY-MM-DD' 문자열 (빈값이면 미선택)
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '날짜 선택' },
  disabled: { type: Boolean, default: false },
  // 휠/입력 연도 클램프 범위
  minYear: { type: Number, default: 1900 },
  maxYear: { type: Number, default: 2100 },
})
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const shown = ref(false) // 슬라이드 업 애니메이션용
const year = ref(2000)
const month = ref(1)
const day = ref(1)

const yEl = ref(null)
const mEl = ref(null)
const dEl = ref(null)
let detachers = []

// 휠 항목 배열 — 연도는 최신이 위(내림차순), 월/일은 오름차순
const years = computed(() => {
  const arr = []
  for (let y = props.maxYear; y >= props.minYear; y -= 1) arr.push(y)
  return arr
})
const months = computed(() => {
  const arr = []
  for (let m = 1; m <= 12; m += 1) arr.push(m)
  return arr
})
const daysInMonth = (y, mo) => new Date(y, mo, 0).getDate()
const days = computed(() => {
  const max = daysInMonth(year.value, month.value)
  const arr = []
  for (let d = 1; d <= max; d += 1) arr.push(d)
  return arr
})

// 'YYYY-MM-DD' → {y,m,d} | null
const parse = (s) => {
  if (!s || typeof s !== 'string') return null
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return null
  return { y: Number(m[1]), m: Number(m[2]), d: Number(m[3]) }
}

// 표시용 텍스트 (트리거 필드)
const displayText = computed(() => {
  const p = parse(props.modelValue)
  if (!p) return ''
  return `${p.y}-${String(p.m).padStart(2, '0')}-${String(p.d).padStart(2, '0')}`
})

const clampDayToMonth = () => {
  const max = daysInMonth(year.value, month.value)
  if (day.value > max) day.value = max
}

// 현재 state 값에 맞춰 세 휠을 정렬
const positionWheels = () => {
  scrollToIndex(yEl.value, years.value.indexOf(year.value))
  scrollToIndex(mEl.value, months.value.indexOf(month.value))
  scrollToIndex(dEl.value, days.value.indexOf(day.value))
}

// 휠 settle 콜백 (스크롤이 멈추면 중앙값을 state 로 반영)
const onYearSettle = () => {
  year.value = years.value[centerIndex(yEl.value, years.value.length)]
  clampDayToMonth()
  nextTick(() => scrollToIndex(dEl.value, days.value.indexOf(day.value)))
}
const onMonthSettle = () => {
  month.value = months.value[centerIndex(mEl.value, months.value.length)]
  clampDayToMonth()
  nextTick(() => scrollToIndex(dEl.value, days.value.indexOf(day.value)))
}
const onDaySettle = () => {
  day.value = days.value[centerIndex(dEl.value, days.value.length)]
}

const attach = () => {
  detachers = [
    attachWheelScroll(yEl.value, onYearSettle),
    attachWheelScroll(mEl.value, onMonthSettle),
    attachWheelScroll(dEl.value, onDaySettle),
  ]
}
const detachAll = () => {
  detachers.forEach((fn) => fn && fn())
  detachers = []
}

const open = () => {
  if (props.disabled) return
  const init =
    parse(props.modelValue) ||
    (() => {
      const t = new Date()
      return { y: t.getFullYear(), m: t.getMonth() + 1, d: t.getDate() }
    })()
  year.value = Math.min(props.maxYear, Math.max(props.minYear, init.y))
  month.value = Math.min(12, Math.max(1, init.m))
  day.value = Math.min(daysInMonth(year.value, month.value), Math.max(1, init.d))
  isOpen.value = true
  nextTick(() => {
    requestAnimationFrame(() => {
      shown.value = true
      positionWheels()
      attach()
    })
  })
}

const close = () => {
  shown.value = false
  setTimeout(() => {
    detachAll()
    isOpen.value = false
  }, 280)
}

// ── 키보드 직접 입력 ─────────────────────────────────────
const onFocusSelect = (e) => e.target.select()
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
const onYearBlur = () => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  clampDayToMonth()
  nextTick(positionWheels)
}
const onMonthBlur = () => {
  month.value = Math.min(12, Math.max(1, month.value || 1))
  clampDayToMonth()
  nextTick(positionWheels)
}
const onDayBlur = () => {
  const max = daysInMonth(year.value, month.value)
  day.value = Math.min(max, Math.max(1, day.value || 1))
  nextTick(positionWheels)
}

// 확정 직전 최종 보정
const normalize = () => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  month.value = Math.min(12, Math.max(1, month.value || 1))
  const max = daysInMonth(year.value, month.value)
  day.value = Math.min(max, Math.max(1, day.value || 1))
}

const onConfirm = () => {
  // 휠이 settle 전이어도 현재 중앙값을 확정값으로 채택
  if (yEl.value) year.value = years.value[centerIndex(yEl.value, years.value.length)]
  if (mEl.value) month.value = months.value[centerIndex(mEl.value, months.value.length)]
  clampDayToMonth()
  if (dEl.value) day.value = days.value[centerIndex(dEl.value, days.value.length)]
  normalize()
  const mm = String(month.value).padStart(2, '0')
  const dd = String(day.value).padStart(2, '0')
  emit('update:modelValue', `${year.value}-${mm}-${dd}`)
  close()
}
const onCancel = () => close()

onBeforeUnmount(detachAll)
</script>

<style scoped>
/* ── 트리거 필드 ─────────────────────────────────────────── */
.dsf-field {
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

/* ── 휠 바텀시트 (공통) ──────────────────────────────────── */
.wp-dim {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
  opacity: 0;
  transition: opacity 0.25s;
}
.wp-dim--on {
  opacity: 1;
}
.wp-sheet {
  width: 100%;
  max-width: 414px;
  background: #ffffff;
  border-radius: 22px 22px 0 0;
  padding: 8px 20px calc(24px + env(safe-area-inset-bottom, 0px));
  transform: translateY(110%);
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  box-sizing: border-box;
}
.wp-sheet--on {
  transform: translateY(0);
}
.wp-grip {
  width: 38px;
  height: 4px;
  background: #d1d6db;
  border-radius: 2px;
  margin: 8px auto 16px;
}
.wp-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.wp-top__title {
  font-size: 17px;
  font-weight: 600;
  color: #191f28;
}
.wp-top__close {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #6b7684;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

/* 직접 입력 필드 */
.wp-keyin {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  border-bottom: 2px solid #16a34a;
  padding: 6px 4px 10px;
  margin: 4px 0 8px;
}
.wp-keyin__in {
  border: 0;
  background: transparent;
  font-family: 'SF Mono', 'Roboto Mono', ui-monospace, monospace;
  font-weight: 600;
  font-size: 24px;
  color: #191f28;
  outline: none;
  padding: 0;
  text-align: center;
  width: 46px;
}
.wp-keyin__in--y {
  width: 78px;
}
.wp-keyin__in::placeholder {
  color: #b0b8c1;
  font-weight: 400;
}
.wp-keyin__dot {
  font-size: 20px;
  color: #b0b8c1;
  padding-bottom: 2px;
}

/* 휠 영역 */
.wp-picker {
  position: relative;
  height: 200px;
  margin: 8px 0 20px;
}
.wp-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 80px;
  height: 40px;
  background: #f0fdf4;
  border-radius: 12px;
  z-index: 0;
}
.wp-wheels {
  position: relative;
  z-index: 1;
  display: grid;
  height: 100%;
}
.wp-wheels--date {
  grid-template-columns: 1.3fr 1fr 1fr;
}
.wp-wheel {
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
  text-align: center;
  -webkit-overflow-scrolling: touch;
}
.wp-wheel::-webkit-scrollbar {
  display: none;
}
.wp-item {
  height: 40px;
  line-height: 40px;
  font-size: 18px;
  scroll-snap-align: center;
  color: #b0b8c1;
  font-family: 'SF Mono', 'Roboto Mono', ui-monospace, monospace;
  font-variant-numeric: tabular-nums;
  opacity: 0.55;
  transition:
    color 0.12s,
    transform 0.12s,
    opacity 0.12s;
}
.wp-item--on {
  color: #16a34a;
  font-weight: 700;
  opacity: 1;
  transform: scale(1.06);
}
.wp-pad {
  height: 80px;
}

.wp-confirm {
  width: 100%;
  height: 54px;
  border: 0;
  background: #16a34a;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  cursor: pointer;
  font-family: inherit;
  transition: opacity 0.15s;
}
.wp-confirm:active {
  opacity: 0.85;
}
</style>
