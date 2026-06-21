<!--
  TimeStepperField.vue — 공통 시각 선택 필드 + 휠(드럼) 바텀시트
  - 목적: native <input type="time"> 의 OS 기본 피커를 토스 스타일 휠 바텀시트로 대체.
  - UI: 트리거 필드 → 하단 시트(직접입력 + 시·분 휠 2열) → [확인]. 24시간제(00~23시)로 오전/오후 구분 없음.
  - v-model: 'HH:MM' 24시간 문자열 (기존 native time input 과 동일 포맷 → 부모 로직 무변경).
  - 분은 1분 단위(0~59)로 정밀도 보존(근태/초과근무 시각 입력).
  - 색상: 앱 브랜드 초록 계열 리터럴 고정(시트가 body 로 teleport 되므로).
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
    <svg
      class="tsf-field__ic"
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
      <circle cx="12" cy="12" r="9" />
      <polyline points="12 7 12 12 15 14" />
    </svg>
  </button>

  <Teleport to="body">
    <div v-if="isOpen" class="wp-dim" :class="{ 'wp-dim--on': shown }" @click.self="onCancel">
      <div
        class="wp-sheet"
        :class="{ 'wp-sheet--on': shown }"
        role="dialog"
        aria-modal="true"
        aria-label="시각 선택"
      >
        <div class="wp-grip" aria-hidden="true"></div>
        <div class="wp-top">
          <span class="wp-top__title">시각 선택</span>
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

        <!-- 직접 입력 필드 (24시간제 — 오전/오후 구분 없음) -->
        <div class="wp-keyin">
          <input
            class="wp-keyin__in"
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
          <span class="wp-keyin__colon">:</span>
          <input
            class="wp-keyin__in"
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
        </div>

        <!-- 휠 -->
        <div class="wp-picker">
          <div class="wp-band" aria-hidden="true"></div>
          <div class="wp-wheels wp-wheels--time">
            <div ref="hEl" class="wp-wheel" aria-label="시 휠">
              <div class="wp-pad"></div>
              <div v-for="v in hours" :key="'h' + v" class="wp-item">{{ pad2(v) }}</div>
              <div class="wp-pad"></div>
            </div>
            <div ref="mEl" class="wp-wheel" aria-label="분 휠">
              <div class="wp-pad"></div>
              <div v-for="v in minutes" :key="'mi' + v" class="wp-item">{{ pad2(v) }}</div>
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
  // 'HH:MM' 24시간 문자열 (빈값이면 미선택)
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '시각 선택' },
  disabled: { type: Boolean, default: false },
  // 분 단위 스텝(예: 30 → [00, 30]). 기본 1 → 기존 호출부 전부 1분 단위 유지.
  step: { type: Number, default: 1 },
})
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const shown = ref(false)
const hour24 = ref(9) // 0~23 (24시간제 — 오전/오후 구분 없음)
const minute = ref(0) // 0~59

// 직접입력 편집 버퍼(문자열). 입력 중에는 pad2 로 강제 패딩하지 않는다.
//   (입력값을 pad2(hour24) 로 바인딩하면 "1" 입력 즉시 "01" 로 채워져 maxlength=2 가 차서
//    두 번째 자리(예: 18 의 '8')를 못 넣는 버그가 발생한다.)
//   확정 시점(blur/휠 정착/시트 열기)에만 pad2 동기화한다.
const hourText = ref('09')
const minuteText = ref('00')

const hEl = ref(null)
const mEl = ref(null)
let detachers = []

const hours = computed(() => {
  const arr = []
  for (let h = 0; h <= 23; h += 1) arr.push(h)
  return arr
})
const minutes = computed(() => {
  const arr = []
  const stp = Math.max(1, props.step)
  for (let m = 0; m <= 59; m += stp) arr.push(m)
  return arr
})

// 분 값을 step 경계로 스냅(가장 가까운 step 배수, 0~59 클램프).
const snapMinute = (m) => {
  const stp = Math.max(1, props.step)
  let v = Math.max(0, Math.min(59, m || 0))
  v = Math.round(v / stp) * stp
  if (v > 59) v -= stp
  return Math.max(0, v)
}

const pad2 = (n) => String(n).padStart(2, '0')

// 'HH:MM'(24h) → {h,m} | null
const parse = (s) => {
  if (!s || typeof s !== 'string') return null
  const m = s.match(/^(\d{1,2}):(\d{1,2})$/)
  if (!m) return null
  const h = Number(m[1])
  const mi = Number(m[2])
  if (h > 23 || mi > 59) return null
  return { h, m: mi }
}

// 표시용 텍스트 (트리거 필드) — 24시간 HH:MM
const displayText = computed(() => {
  const p = parse(props.modelValue)
  if (!p) return ''
  return `${pad2(p.h)}:${pad2(p.m)}`
})

const positionWheels = () => {
  scrollToIndex(hEl.value, hours.value.indexOf(hour24.value))
  scrollToIndex(mEl.value, minutes.value.indexOf(minute.value))
}

const onHourSettle = () => {
  hour24.value = hours.value[centerIndex(hEl.value, hours.value.length)]
  hourText.value = pad2(hour24.value) // 휠 정착 시 직접입력 칸 동기화
}
const onMinuteSettle = () => {
  minute.value = minutes.value[centerIndex(mEl.value, minutes.value.length)]
  minuteText.value = pad2(minute.value) // 휠 정착 시 직접입력 칸 동기화
}

const attach = () => {
  detachers = [
    attachWheelScroll(hEl.value, onHourSettle),
    attachWheelScroll(mEl.value, onMinuteSettle),
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
      return { h: t.getHours(), m: t.getMinutes() }
    })()
  hour24.value = Math.min(23, Math.max(0, init.h))
  // 초기 분을 step 경계로 스냅(휠 인덱스 정합). step=1 이면 그대로.
  minute.value = snapMinute(init.m)
  // 직접입력 버퍼 초기 동기화(시트 열 때 현재값 표시).
  hourText.value = pad2(hour24.value)
  minuteText.value = pad2(minute.value)
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
const onHourInput = (e) => {
  // 입력 중에는 raw 숫자(미패딩)를 그대로 버퍼에 보존 → "1" 입력 후 "8" 추가 입력 가능.
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  hourText.value = digits
  // 비숫자 입력이 섞이면 DOM 값도 즉시 정리(커서/표시 일관성).
  if (e.target.value !== digits) e.target.value = digits
  hour24.value = digits === '' ? 0 : Number(digits)
}
const onMinuteInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '').slice(0, 2)
  minuteText.value = digits
  if (e.target.value !== digits) e.target.value = digits
  minute.value = digits === '' ? 0 : Number(digits)
}
const onHourBlur = () => {
  hour24.value = Math.min(23, Math.max(0, hour24.value || 0))
  hourText.value = pad2(hour24.value) // 포커스 해제 시 2자리로 정규화.
  nextTick(positionWheels)
}
const onMinuteBlur = () => {
  // step 경계로 스냅(가장 가까운 배수). step=1 이면 기존과 동일.
  minute.value = snapMinute(minute.value)
  minuteText.value = pad2(minute.value) // 포커스 해제 시 2자리로 정규화.
  nextTick(positionWheels)
}

const normalize = () => {
  hour24.value = Math.min(23, Math.max(0, hour24.value || 0))
  // step 경계로 스냅(가장 가까운 배수). step=1 이면 기존과 동일.
  minute.value = snapMinute(minute.value)
}

const onConfirm = () => {
  if (hEl.value) hour24.value = hours.value[centerIndex(hEl.value, hours.value.length)]
  if (mEl.value) minute.value = minutes.value[centerIndex(mEl.value, minutes.value.length)]
  normalize()
  emit('update:modelValue', `${pad2(hour24.value)}:${pad2(minute.value)}`)
  close()
}
const onCancel = () => close()

onBeforeUnmount(detachAll)
</script>

<style scoped>
/* ── 트리거 필드 ─────────────────────────────────────────── */
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

/* ── 휠 바텀시트 ─────────────────────────────────────────── */
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
  gap: 10px;
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
.wp-keyin__colon {
  font-size: 22px;
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
.wp-wheels--time {
  grid-template-columns: 1fr 1fr;
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
