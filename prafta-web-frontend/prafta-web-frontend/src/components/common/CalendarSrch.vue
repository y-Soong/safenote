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

const props = defineProps({
  modelValue: { type: [String, Array, Date], default: "" },
  range: { type: Boolean, default: false },
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

function formatDate(date) {
  if (!(date instanceof Date)) return "";
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

const updateValue = (selectedDates) => {
  const formatDate = (date) => {
    if (!(date instanceof Date)) return "";
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const dd = String(date.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
  };

  const formatted = props.range
    ? selectedDates.map((d) => formatDate(d))
    : formatDate(selectedDates[0]);

  emit("update:modelValue", formatted);
};

// const updateValue = (selectedDates) => {
//   emit("update:modelValue", props.range ? selectedDates : selectedDates[0]);
// };

const pickerConfig = {
  mode: props.range ? "range" : "single",
  dateFormat: "Y-m-d",
  locale: Korean,
  clickOpens: !props.readonly, // 🔹 여기에 제어 옵션 추가
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
