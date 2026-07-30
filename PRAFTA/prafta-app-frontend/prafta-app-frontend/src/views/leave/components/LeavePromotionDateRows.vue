<!--
  LeavePromotionDateRows.vue — 계획서 선택 날짜 행 목록 + 날짜 키인 (모바일 앱, 신규)
  - 작업 ID: prafta-com-008-A-7 (UI 명세: UI-app-008-A-1)
  - 정책 출처: 작업지시서 §2-2(캘린더 하단 행 추가 + 날짜 키인), §2-2(1일 단위만)
  - 참조 패턴: views/leave/components/LeaveApplyForm.vue (행/입력 + 디자인 토큰)
  - 동작: 캘린더로 고른 날짜를 행으로 보여주고, 직접 키인(추가)·행 삭제. 캘린더와 동일 모델 공유.
  - 날짜 키인은 공통 DateStepperField(휠 바텀시트) 사용. modelValue 'YYYY-MM-DD' 계약 유지.
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO.
  - developer 라운드: 키인 추가 시 중복/선택가능 검증(부모 selectableYmds 기준) 후 emit.
-->
<template>
  <div class="lpd">
    <p class="lpd__title">
      선택한 날짜 ({{ modelValue.length }}<template v-if="maxCount != null">/{{ maxCount }}</template>)
    </p>

    <!-- 선택 날짜 행 목록 -->
    <ul v-if="modelValue.length > 0" class="lpd__list">
      <li v-for="ymd in sortedYmds" :key="ymd" class="lpd__row">
        <span class="lpd__date">{{ formatYmd(ymd) }}</span>
        <button
          type="button"
          class="lpd__del"
          aria-label="삭제"
          @click="onRemove(ymd)"
        >
          삭제
        </button>
      </li>
    </ul>
    <p v-else class="lpd__empty">캘린더에서 날짜를 선택하거나 아래에서 직접 추가하세요.</p>

    <!-- 행 추가 (날짜 키인) — 공통 날짜 휠 필드(modelValue 'YYYY-MM-DD'). 최종 가용성은 onAdd 검증. -->
    <div class="lpd__add">
      <DateStepperField
        v-model="keyinYmd"
        class="lpd__add-input"
        placeholder="날짜 선택"
        :min-year="minYear"
        :max-year="maxYear"
      />
      <button
        type="button"
        class="lpd__add-btn"
        :disabled="!keyinYmd"
        @click="onAdd"
      >
        + 행 추가
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const props = defineProps({
  // 선택 날짜 (YYYYMMDD) — v-model, 캘린더와 공유
  modelValue: {
    type: Array,
    default: () => [],
  },
  // 선택 가능한 날짜 (YYYYMMDD) — 키인 검증용 (A-3 가공값)
  selectableYmds: {
    type: Array,
    default: () => [],
  },
  // 키인 가능 최소/최대일 (YYYY-MM-DD) — 부모가 촉진 기간으로 전달(휠 연도 범위 도출)
  minDate: {
    type: String,
    default: '',
  },
  maxDate: {
    type: String,
    default: '',
  },
  // 선택 가능 최대 일수(미지정 잔여 연차) — null 이면 상한 없음
  maxCount: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue'])

// 날짜 휠 필드(YYYY-MM-DD) 바인딩 임시값
const keyinYmd = ref('')

// minDate/maxDate(YYYY-MM-DD)에서 연도 범위 도출 — 휠 연도 클램프용(최종 가용성은 onAdd 검증).
const yearOf = (dash, fallback) => {
  const y = Number(String(dash || '').slice(0, 4))
  return Number.isFinite(y) && y > 0 ? y : fallback
}
const minYear = computed(() => yearOf(props.minDate, 1900))
const maxYear = computed(() => yearOf(props.maxDate, 2100))

// 표시용 오름차순 정렬
const sortedYmds = computed(() => [...props.modelValue].sort())

// YYYYMMDD → "YYYY.MM.DD" 표시 (D1 점 통일, 표시 단일 출처 위임).
const formatYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return formatYmdDisplay(ymd)
}

// 행 삭제
const onRemove = (ymd) => {
  emit('update:modelValue', props.modelValue.filter((d) => d !== ymd))
}

// 날짜 키인 추가 — 중복/선택가능/잔여 상한 검증 후 추가.
const onAdd = () => {
  if (!keyinYmd.value) return
  // 'YYYY-MM-DD' → 'YYYYMMDD'
  const ymd = keyinYmd.value.replace(/-/g, '')
  // 단순 UI 검증(중복·선택가능). 상세 사유 메시지는 developer 가 보완.
  if (props.modelValue.includes(ymd)) {
    showAlert('이미 선택한 날짜입니다.')
    return
  }
  if (props.selectableYmds.length > 0 && !props.selectableYmds.includes(ymd)) {
    showAlert('선택할 수 없는 날짜입니다. (휴일/주말/근무일 아님/만료 초과)')
    return
  }
  // 미지정 잔여 연차 개수를 넘는 날짜 지정 차단(캘린더 토글과 동일 상한).
  if (props.maxCount != null && props.modelValue.length >= props.maxCount) {
    showAlert(`미지정 잔여 연차 ${props.maxCount}일까지만 선택할 수 있습니다.`)
    return
  }
  emit('update:modelValue', [...props.modelValue, ymd])
  keyinYmd.value = ''
}
</script>

<style scoped>
.lpd {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --radius-md: 10px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;

  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  color: var(--color-text-primary);
}

.lpd__title {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.lpd__list {
  list-style: none;
  margin: 0 0 var(--space-sm);
  padding: 0;
}
.lpd__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 0.5px solid var(--color-border-light);
}
.lpd__date {
  font-size: 14px;
  color: var(--color-text-primary);
}
.lpd__del {
  background: transparent;
  border: 0;
  color: var(--color-danger);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.lpd__empty {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.lpd__add {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.lpd__add-input {
  flex: 1;
  height: 40px;
  padding: 0 var(--space-md);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.lpd__add-btn {
  flex: 0 0 auto;
  height: 40px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.lpd__add-btn:disabled {
  border-color: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: default;
}
</style>
