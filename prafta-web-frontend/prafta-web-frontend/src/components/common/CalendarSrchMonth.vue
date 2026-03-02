<template>
  <div class="month-calendar-search">
    <flat-pickr
      v-model="internalValue"
      class="calendar-input"
      placeholder="📅"
      :config="pickerConfig"
      :style="inputStyle"
      :readonly="props.readonly"
      @on-change="updateValue"
    />
  </div>
</template>

<script setup>
import { ref, watch, defineEmits, defineProps, computed } from "vue";
import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";
import "flatpickr/dist/plugins/monthSelect/style.css"; // monthSelect 플러그인 CSS
import { Korean } from "flatpickr/dist/l10n/ko.js";
import monthSelectPlugin from "flatpickr/dist/plugins/monthSelect";

const props = defineProps({
  modelValue: { type: [String, Date], default: "" },
  style: { type: [String, Object], default: () => ({}) },
  readonly: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);
const internalValue = ref(props.modelValue);
const inputStyle = computed(() => props.style);

watch(
  () => props.modelValue,
  (val) => {
    internalValue.value = typeof val === "string" ? val : formatMonth(val);
  }
);

function formatMonth(date) {
  if (!(date instanceof Date)) return "";
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  return `${yyyy}-${mm}`; // 🔹 연-월 포맷
}

const updateValue = (selectedDates) => {
  emit("update:modelValue", formatMonth(selectedDates[0]));
};

const pickerConfig = {
  dateFormat: "Y-m", // 결과값 형식
  locale: Korean,
  clickOpens: !props.readonly,
  plugins: [
    monthSelectPlugin({
      shorthand: true, // Jan, Feb 형식 (false면 "1월", "2월")
      dateFormat: "Y-m", // 출력되는 값
      theme: "light", // light / dark 테마
    }),
  ],
};
</script>

<style scoped>
.month-calendar-search {
  gap: 2px;
}
.calendar-input {
  padding: 0.32rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
