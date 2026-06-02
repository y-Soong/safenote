<template>
  <div class="month-day-picker" :class="{ disabled: disabled || readonly }">
    <div class="picker-body">
      <!-- 월 선택 패널 -->
      <div class="panel month-panel">
        <div class="panel-header">
          <button
            type="button"
            class="nav-btn"
            :disabled="disabled || readonly"
            @click="prevMonth"
          >
            ‹
          </button>
          <span class="panel-title">{{ selectedMonth }}월</span>
          <button
            type="button"
            class="nav-btn"
            :disabled="disabled || readonly"
            @click="nextMonth"
          >
            ›
          </button>
        </div>
        <div class="panel-grid month-grid">
          <button
            v-for="m in 12"
            :key="m"
            type="button"
            class="grid-cell"
            :class="{ selected: selectedMonth === m }"
            :disabled="disabled || readonly"
            @click="selectMonth(m)"
          >
            {{ m }}
          </button>
        </div>
      </div>

      <!-- 일 선택 패널 -->
      <div class="panel day-panel">
        <div class="panel-header">
          <button
            type="button"
            class="nav-btn"
            :disabled="disabled || readonly"
            @click="prevDay"
          >
            ‹
          </button>
          <span class="panel-title">{{ selectedDay }}일</span>
          <button
            type="button"
            class="nav-btn"
            :disabled="disabled || readonly"
            @click="nextDay"
          >
            ›
          </button>
        </div>
        <div class="panel-grid day-grid">
          <button
            v-for="d in maxDaysInMonth"
            :key="d"
            type="button"
            class="grid-cell"
            :class="{ selected: selectedDay === d }"
            :disabled="disabled || readonly"
            @click="selectDay(d)"
          >
            {{ d }}
          </button>
        </div>
      </div>
    </div>
    <div class="picker-footer">
      <button
        type="button"
        class="select-btn"
        :disabled="disabled || readonly"
        @click="confirmSelect"
      >
        선택
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "" }, // MMDD 4자리 (예: 0101, 1231)
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);

const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

const parseModel = (val) => {
  if (!val || val.length < 4) return { month: 1, day: 1 };
  const mm = parseInt(val.slice(0, 2), 10) || 1;
  const dd = parseInt(val.slice(2, 4), 10) || 1;
  const month = Math.max(1, Math.min(12, mm));
  const maxD = DAYS_IN_MONTH[month - 1];
  const day = Math.max(1, Math.min(maxD, dd));
  return { month, day };
};

const selectedMonth = ref(1);
const selectedDay = ref(1);

const maxDaysInMonth = computed(() => DAYS_IN_MONTH[selectedMonth.value - 1]);

const clampDay = () => {
  const max = maxDaysInMonth.value;
  if (selectedDay.value > max) {
    selectedDay.value = max;
  }
};

const emitValue = () => {
  const mm = String(selectedMonth.value).padStart(2, "0");
  const dd = String(selectedDay.value).padStart(2, "0");
  emit("update:modelValue", mm + dd);
};

const selectMonth = (m) => {
  selectedMonth.value = m;
  clampDay();
};

const selectDay = (d) => {
  selectedDay.value = d;
};

const prevMonth = () => {
  selectedMonth.value = selectedMonth.value <= 1 ? 12 : selectedMonth.value - 1;
  clampDay();
};

const nextMonth = () => {
  selectedMonth.value = selectedMonth.value >= 12 ? 1 : selectedMonth.value + 1;
  clampDay();
};

const prevDay = () => {
  const max = maxDaysInMonth.value;
  selectedDay.value = selectedDay.value <= 1 ? max : selectedDay.value - 1;
};

const nextDay = () => {
  const max = maxDaysInMonth.value;
  selectedDay.value = selectedDay.value >= max ? 1 : selectedDay.value + 1;
};

const confirmSelect = () => {
  emitValue();
};

watch(
  () => props.modelValue,
  (val) => {
    const { month, day } = parseModel(val);
    if (month !== selectedMonth.value || day !== selectedDay.value) {
      selectedMonth.value = month;
      selectedDay.value = day;
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.month-day-picker {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: #fff;
  min-width: 30rem;
  width: max-content;
}

.picker-body {
  display: flex;
  gap: 1rem;
}

.month-day-picker.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.panel {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  overflow: visible;
}

.month-panel {
  min-width: 9rem;
  flex: 1;
}

.day-panel {
  min-width: 19rem; /* 7열 × 2.35rem + gap + padding */
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  background: #0d9488;
  color: #fff;
  font-weight: 600;
  font-size: 0.875rem;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  padding: 0;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: inherit;
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.35);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.panel-title {
  flex: 1;
  text-align: center;
}

.panel-grid {
  display: grid;
  gap: 0.25rem;
  padding: 0.5rem;
  background: #f8fafc;
  min-width: 0;
}

.month-grid {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.month-grid .grid-cell {
  width: 100%;
  min-width: 0;
  height: auto;
  aspect-ratio: 1;
}

.day-grid {
  grid-template-columns: repeat(7, 2.35rem);
}

.grid-cell {
  padding: 0.35rem 0.25rem;
  box-sizing: border-box;
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 4px;
  background: #fff;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #334155;
  text-align: center;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.day-grid .grid-cell {
  width: 2.35rem;
  min-width: 2.35rem;
  height: 2.35rem;
}

.grid-cell:hover:not(:disabled):not(.selected) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.grid-cell.selected {
  background: #0d9488;
  border-color: #0d9488;
  color: #fff;
}

.grid-cell:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.picker-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.select-btn {
  padding: 0.3rem 1rem;
  border: 1px solid #0d9488;
  border-radius: 6px;
  background: #0d9488;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}

.select-btn:hover:not(:disabled) {
  background: #0f766e;
  border-color: #0f766e;
}

.select-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
