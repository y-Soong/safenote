<!--
  MonthPickerSheet.vue — 연/월 휠 바텀시트 (월단위 선택)
  - 목적: 월단위 캘린더(예: AttendanceMonthCalendar)에서 월 라벨 탭 시 임의의 연/월로 점프.
  - DateStepperField 와 동일한 토스 스타일 휠 양식의 축소판(연/월 2열, 일자 없음).
  - v-model: 표시 여부(Boolean). 값은 prop yearMonth('YYYYMM')로 받고 confirm 이벤트로 'YYYYMM' 반환.
  - 색상: 앱 브랜드 초록 계열 리터럴 고정(시트가 body 로 teleport 되므로).
-->
<template>
  <Teleport to="body">
    <div v-if="isOpen" class="wp-dim" :class="{ 'wp-dim--on': shown }" @click.self="close">
      <div
        class="wp-sheet"
        :class="{ 'wp-sheet--on': shown }"
        role="dialog"
        aria-modal="true"
        aria-label="연월 선택"
      >
        <div class="wp-grip" aria-hidden="true"></div>
        <div class="wp-top">
          <span class="wp-top__title">연월 선택</span>
          <button type="button" class="wp-top__close" aria-label="닫기" @click="close">
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
        </div>

        <!-- 휠 -->
        <div class="wp-picker">
          <div class="wp-band" aria-hidden="true"></div>
          <div class="wp-wheels wp-wheels--month">
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
          </div>
        </div>

        <button type="button" class="wp-confirm" @click="onConfirm">확인</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { scrollToIndex, centerIndex, attachWheelScroll } from '@/utils/wheelPicker'

const props = defineProps({
  modelValue: { type: Boolean, default: false }, // 표시 여부
  yearMonth: { type: String, default: '' }, // 'YYYYMM'
  minYear: { type: Number, default: 2015 },
  maxYear: { type: Number, default: 2100 },
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const isOpen = ref(false)
const shown = ref(false)
const year = ref(2025)
const month = ref(1)

const yEl = ref(null)
const mEl = ref(null)
let detachers = []

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

// 'YYYYMM' → {y,m} | null
const parse = (s) => {
  if (!s || typeof s !== 'string' || s.length < 6) return null
  const y = Number(s.slice(0, 4))
  const m = Number(s.slice(4, 6))
  if (!y || m < 1 || m > 12) return null
  return { y, m }
}

const positionWheels = () => {
  scrollToIndex(yEl.value, years.value.indexOf(year.value))
  scrollToIndex(mEl.value, months.value.indexOf(month.value))
}

const onYearSettle = () => {
  year.value = years.value[centerIndex(yEl.value, years.value.length)]
}
const onMonthSettle = () => {
  month.value = months.value[centerIndex(mEl.value, months.value.length)]
}

const attach = () => {
  detachers = [
    attachWheelScroll(yEl.value, onYearSettle),
    attachWheelScroll(mEl.value, onMonthSettle),
  ]
}
const detachAll = () => {
  detachers.forEach((fn) => fn && fn())
  detachers = []
}

const openInternal = () => {
  const init =
    parse(props.yearMonth) ||
    (() => {
      const t = new Date()
      return { y: t.getFullYear(), m: t.getMonth() + 1 }
    })()
  year.value = Math.min(props.maxYear, Math.max(props.minYear, init.y))
  month.value = Math.min(12, Math.max(1, init.m))
  isOpen.value = true
  nextTick(() => {
    requestAnimationFrame(() => {
      shown.value = true
      positionWheels()
      attach()
    })
  })
}
const closeInternal = () => {
  shown.value = false
  setTimeout(() => {
    detachAll()
    isOpen.value = false
  }, 280)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) openInternal()
    else if (isOpen.value) closeInternal()
  },
)

// 닫기는 v-model 만 갱신 → watch 가 애니메이션 처리
const close = () => emit('update:modelValue', false)

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
const onYearBlur = () => {
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  nextTick(positionWheels)
}
const onMonthBlur = () => {
  month.value = Math.min(12, Math.max(1, month.value || 1))
  nextTick(positionWheels)
}

const onConfirm = () => {
  if (yEl.value) year.value = years.value[centerIndex(yEl.value, years.value.length)]
  if (mEl.value) month.value = months.value[centerIndex(mEl.value, months.value.length)]
  year.value = Math.min(props.maxYear, Math.max(props.minYear, year.value || props.minYear))
  month.value = Math.min(12, Math.max(1, month.value || 1))
  const yyyymm = `${year.value}${String(month.value).padStart(2, '0')}`
  emit('confirm', yyyymm)
  emit('update:modelValue', false)
}

onBeforeUnmount(detachAll)
</script>

<style scoped>
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
.wp-keyin__dot {
  font-size: 20px;
  color: #b0b8c1;
  padding-bottom: 2px;
}
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
.wp-wheels--month {
  grid-template-columns: 1.3fr 1fr;
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
