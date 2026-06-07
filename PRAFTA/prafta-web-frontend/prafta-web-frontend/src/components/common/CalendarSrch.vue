<template>
  <div class="calendar-search">
    <flat-pickr
      v-model="internalValue"
      class="calendar-input"
      placeholder="📅"
      :config="pickerConfig"
      :style="inputStyle"
      :disabled="computedDisabled"
      @on-change="updateValue"
    >
    </flat-pickr>
  </div>
</template>

<script setup>
import { ref, watch, defineEmits, defineProps, computed } from "vue";
import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";
import { Korean } from "flatpickr/dist/l10n/ko.js";
// 월 모드용 flatpickr monthSelect 플러그인 (month=true 일 때만 사용)
import monthSelectPlugin from "flatpickr/dist/plugins/monthSelect/index.js";
import "flatpickr/dist/plugins/monthSelect/style.css";

const props = defineProps({
  modelValue: { type: [String, Array, Date], default: "" },
  range: { type: Boolean, default: false },
  // month=true 면 일(day) 대신 연-월(YYYY-MM)만 선택. 모델/방출값도 YYYY-MM.
  month: { type: Boolean, default: false },
  style: { type: [String, Object], default: () => ({}) },
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);
const internalValue = ref(props.modelValue);
const inputStyle = computed(() => props.style);

const computedDisabled = computed(() => props.disabled || props.readonly);

watch(
  () => props.modelValue,
  (val) => {
    // 강제로 String으로 변환
    internalValue.value = Array.isArray(val)
      ? val.map((v) => (typeof v === "string" ? v : formatDate(v)))
      : typeof val === "string"
        ? val
        : formatDate(val);
  }
);

// month 모드면 YYYY-MM, 아니면 YYYY-MM-DD 로 포맷.
function formatDate(date) {
  if (!(date instanceof Date)) return "";
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  if (props.month) return `${yyyy}-${mm}`;
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

const updateValue = (selectedDates) => {
  const formatted = props.range
    ? selectedDates.map((d) => formatDate(d))
    : formatDate(selectedDates[0]);

  emit("update:modelValue", formatted);
};

const pickerConfig = {
  mode: props.range ? "range" : "single",
  dateFormat: props.month ? "Y-m" : "Y-m-d",
  locale: Korean,
  clickOpens: !props.readonly, // 🔹 여기에 제어 옵션 추가
  // 월 모드: monthSelect 플러그인으로 월 단위 선택(YYYY-MM)
  ...(props.month
    ? {
        plugins: [
          monthSelectPlugin({
            shorthand: false,
            dateFormat: "Y-m",
            altFormat: "Y-m",
          }),
        ],
      }
    : {}),
};
</script>

<style scoped>
.calendar-search {
  gap: 4px;
}
.calendar-input {
  padding: 0.32rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
