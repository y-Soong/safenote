<!--
  RequestDateRangeFilterSheet.vue — 기간 from-to (WORK_YMD 기준)
  - 작업 ID: PRAFTA-APP-006-4
  - 날짜 입력은 공통 DateStepperField(휠 바텀시트). modelValue 'YYYY-MM-DD' 계약 유지.
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="대상 기간"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 프리셋 칩 -->
    <div class="req-date-presets" role="group" aria-label="프리셋">
      <button
        v-for="p in PRESETS"
        :key="p.key"
        type="button"
        class="req-date-presets__chip"
        :class="{ 'req-date-presets__chip--on': activePreset === p.key }"
        @click="applyPreset(p.key)"
      >
        {{ p.label }}
      </button>
    </div>

    <!-- from-to 직접 입력 -->
    <div class="req-date-inputs">
      <label class="req-date-inputs__field">
        <span class="req-date-inputs__lbl">시작일</span>
        <DateStepperField
          :model-value="localFromInput"
          placeholder="시작일"
          @update:model-value="onFromChange"
        />
      </label>
      <span class="req-date-inputs__tilde" aria-hidden="true">~</span>
      <label class="req-date-inputs__field">
        <span class="req-date-inputs__lbl">종료일</span>
        <DateStepperField
          :model-value="localToInput"
          placeholder="종료일"
          @update:model-value="onToChange"
        />
      </label>
    </div>

    <template #footer>
      <div class="req-date-footer">
        <button type="button" class="req-date-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button type="button" class="req-date-footer__apply" @click="onApply">적용하기</button>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from './BaseBottomSheet.vue'
import DateStepperField from '@/components/common/DateStepperField.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  from: { type: String, default: '' }, // YYYYMMDD
  to: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const PRESETS = [
  { key: 'last7', label: '최근 7일' },
  { key: 'last30', label: '최근 30일' },
  { key: 'last90', label: '최근 3개월' },
  { key: 'thisYear', label: '올해' },
]

const localFromInput = ref('') // YYYY-MM-DD (DateStepperField 계약)
const localToInput = ref('')
const activePreset = ref('')

// YYYYMMDD ↔ YYYY-MM-DD 변환
const ymdToInput = (ymd) =>
  ymd && ymd.length === 8 ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}` : ''
const inputToYmd = (s) => (s ? s.replace(/-/g, '') : '')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      localFromInput.value = ymdToInput(props.from)
      localToInput.value = ymdToInput(props.to)
      activePreset.value = ''
    }
  },
)

// 오늘 기준 프리셋 계산
const todayDate = () => new Date()
const fmtInput = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const applyPreset = (key) => {
  activePreset.value = key
  const today = todayDate()
  const toStr = fmtInput(today)
  let fromStr = ''
  if (key === 'last7') {
    const d = new Date(today)
    d.setDate(d.getDate() - 7)
    fromStr = fmtInput(d)
  } else if (key === 'last30') {
    const d = new Date(today)
    d.setDate(d.getDate() - 30)
    fromStr = fmtInput(d)
  } else if (key === 'last90') {
    const d = new Date(today)
    d.setDate(d.getDate() - 90)
    fromStr = fmtInput(d)
  } else if (key === 'thisYear') {
    fromStr = `${today.getFullYear()}-01-01`
    localFromInput.value = fromStr
    localToInput.value = `${today.getFullYear()}-12-31`
    return
  }
  localFromInput.value = fromStr
  localToInput.value = toStr
}

// 사용자가 직접 날짜를 바꾸면 값 반영 + 프리셋 활성 해제
const onFromChange = (val) => {
  localFromInput.value = val
  activePreset.value = ''
}
const onToChange = (val) => {
  localToInput.value = val
  activePreset.value = ''
}

const onResetInternal = () => {
  localFromInput.value = ''
  localToInput.value = ''
  activePreset.value = ''
}

const onApply = () => {
  emit('apply', {
    from: inputToYmd(localFromInput.value),
    to: inputToYmd(localToInput.value),
  })
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-date-presets {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 4px 0 12px;
}

.req-date-presets__chip {
  height: 32px;
  padding: 0 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.req-date-presets__chip--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
}

.req-date-inputs {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: end;
  padding: 8px 0 16px;
}

.req-date-inputs__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.req-date-inputs__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.req-date-inputs__input {
  height: 40px;
  padding: 0 10px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: inherit;
}
.req-date-inputs__input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.req-date-inputs__tilde {
  padding-bottom: 10px;
  color: var(--color-text-tertiary);
}

.req-date-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-date-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-date-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
</style>
