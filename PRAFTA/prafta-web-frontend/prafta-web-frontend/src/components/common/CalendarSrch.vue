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
  // 선택 가능 하한/상한(YYYY-MM-DD 또는 YYYY-MM). flatpickr minDate/maxDate 로 전달.
  minDate: { type: String, default: null },
  maxDate: { type: String, default: null },
});

const emit = defineEmits(["update:modelValue"]);
const internalValue = ref(normalizeIncoming(props.modelValue));
const inputStyle = computed(() => props.style);

const computedDisabled = computed(() => props.disabled || props.readonly);

// 직전에 방출한 값. range 모드는 방출값이 매번 새 배열이라 부모 ref 의 identity 가 항상 바뀌고,
//   그대로 두면 [부모 watch → internalValue 갱신 → flatpickr.setDate(_, true) → onChange → 재방출]
//   이 서로를 무한히 깨운다(vue-flatpickr 의 `newValue === nullify($el.value)` 가드는 배열 대 문자열
//   비교라 절대 성립하지 않는다). single 모드는 방출값이 문자열 원시값이라 값 비교로 우연히 멈춰 있었다.
//   자기가 방출한 값이 부모를 거쳐 되돌아온 1회만 무시해 고리를 끊는다.
let lastEmitted = null;

// range 모드 방출값은 배열이므로 identity 가 아니라 원소로 비교한다.
const sameValue = (a, b) =>
  Array.isArray(a) && Array.isArray(b)
    ? a.length === b.length && a.every((v, i) => v === b[i])
    : a === b;

watch(
  () => props.modelValue,
  (val) => {
    if (sameValue(val, lastEmitted)) {
      lastEmitted = null;
      return;
    }
    internalValue.value = normalizeIncoming(val);
  }
);

// 무대시 컴팩트값(YYYYMMDD / 월모드 YYYYMM)을 flatpickr 가 파싱 가능한 대시 형식으로 변환.
//   DB 는 날짜를 varchar(8) YYYYMMDD 로 저장/반환한다. 이를 그대로 넘기면 flatpickr(dateFormat
//   Y-m-d)가 구분자 불일치로 오파싱(예: 20201001 → 엉뚱한 날짜)하므로 진입 지점에서 일괄 정규화한다.
//   이미 대시가 있거나 형식이 다르면 원본 유지. 방출값(updateValue)은 항상 대시 형식이라 무영향.
function toDisplayStr(str) {
  const s = String(str).trim();
  if (props.month) {
    return /^\d{6}$/.test(s) ? `${s.slice(0, 4)}-${s.slice(4, 6)}` : s;
  }
  return /^\d{8}$/.test(s)
    ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`
    : s;
}

// 모델값(문자열/배열/Date)을 flatpickr 표시용 문자열(또는 배열)로 정규화.
function normalizeIncoming(val) {
  if (Array.isArray(val)) {
    return val.map((v) =>
      typeof v === "string" ? toDisplayStr(v) : formatDate(v)
    );
  }
  return typeof val === "string" ? toDisplayStr(val) : formatDate(val);
}

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

  lastEmitted = formatted;
  emit("update:modelValue", formatted);
};

// 반응형 config: minDate/maxDate/clickOpens 가 부모 props 변경(예: 개시일 → 종료일 하한)
//   에 따라 flatpickr 에 실제 반영되도록 computed 로 둔다(plain object 면 setup 시점 값으로
//   동결되어 이후 props 변경이 flatpickr 에 적용되지 않음).
const pickerConfig = computed(() => ({
  mode: props.range ? "range" : "single",
  dateFormat: props.month ? "Y-m" : "Y-m-d",
  locale: Korean,
  clickOpens: !props.readonly, // 🔹 여기에 제어 옵션 추가
  // 선택 하한/상한 가드(있을 때만 전달)
  ...(props.minDate ? { minDate: props.minDate } : {}),
  ...(props.maxDate ? { maxDate: props.maxDate } : {}),
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
}));
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
