<template>
  <div class="time-input-wrap">
    <div class="time-select-wrap" ref="hourWrapRef">
      <button
        type="button"
        class="time-select"
        :class="{ 'is-readonly': readonly }"
        :disabled="computedDisabled"
        @click="toggleHour"
      >
        {{ hourVal }}
      </button>
      <Teleport to="body">
        <ul
          v-show="hourOpen"
          class="time-select-dropdown time-select-dropdown-fixed"
          :style="hourDropdownStyle"
          ref="hourListRef"
        >
          <li
            v-for="opt in hourOptions"
            :key="opt"
            :class="{ active: hourVal === opt }"
            @mousedown.prevent="selectHour(opt)"
          >
            {{ opt }}
          </li>
        </ul>
      </Teleport>
    </div>
    <span class="time-sep">:</span>
    <div class="time-select-wrap" ref="minuteWrapRef">
      <button
        type="button"
        class="time-select"
        :class="{ 'is-readonly': readonly }"
        :disabled="minuteDisabled"
        @click="toggleMinute"
      >
        {{ minuteVal }}
      </button>
      <Teleport to="body">
        <ul
          v-show="minuteOpen"
          class="time-select-dropdown time-select-dropdown-fixed"
          :style="minuteDropdownStyle"
          ref="minuteListRef"
        >
          <li
            v-for="opt in minuteOptions"
            :key="opt"
            :class="{ active: minuteVal === opt }"
            @mousedown.prevent="selectMinute(opt)"
          >
            {{ opt }}
          </li>
        </ul>
      </Teleport>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  computed,
  watch,
  defineProps,
  defineEmits,
  onMounted,
  onBeforeUnmount,
  nextTick,
} from "vue";

// ================ Props & Emits ================
const props = defineProps({
  modelValue: { type: String, default: "" }, // "HH:mm" 또는 "0900" 24시간 형식
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  hourStep: { type: Number, default: 1 },
  minuteStep: { type: Number, default: 1 },
  /** 종료시간 등 24:00 표현 허용 시 true. 24 선택 시 분은 00 고정 */
  allow24: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);

/** "0900" → "09:00" 정규화 (24시간제), "~" 포함 값은 앞부분만 사용. allow24 시 24:00 허용 */
const normalizeToHHmm = (v, allow24 = false) => {
  if (!v || typeof v !== "string") return "";
  let s = String(v).trim().replace(/\s/g, "");
  if (s.includes("~")) s = s.split("~")[0] || s;
  if (s.includes(":")) {
    const [hPart, mPart] = s.split(":");
    const h = parseInt(hPart || "0", 10);
    const m = parseInt(mPart || "0", 10);
    if (allow24 && h === 24 && m === 0) return "24:00";
    const hOk = Math.min(allow24 ? 24 : 23, Math.max(0, h));
    const mOk = hOk === 24 ? 0 : Math.min(59, Math.max(0, m));
    return `${String(hOk).padStart(2, "0")}:${String(mOk).padStart(2, "0")}`;
  }
  const digits = s.replace(/\D/g, "");
  if (digits.length >= 3) {
    let h = parseInt(digits.slice(0, -2) || "0", 10);
    let m = parseInt(digits.slice(-2) || "0", 10);
    if (allow24 && h === 24 && m === 0) return "24:00";
    h = Math.min(allow24 ? 24 : 23, Math.max(0, h));
    m = h === 24 ? 0 : Math.min(59, Math.max(0, m));
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
  }
  if (digits.length >= 1) {
    const h = Math.min(allow24 ? 24 : 23, Math.max(0, parseInt(digits, 10)));
    return `${String(h).padStart(2, "0")}:00`;
  }
  return "";
};

/** step에 맞는 옵션으로 스냅 */
const snapToStep = (value, step, max) => {
  const n = Math.min(max, Math.max(0, parseInt(value, 10) || 0));
  const snapped = Math.floor(n / step) * step;
  return String(Math.min(max, snapped)).padStart(2, "0");
};

const hourOptions = computed(() => {
  const step = Math.max(1, Math.min(24, Math.floor(props.hourStep) || 1));
  const opts = [];
  for (let i = 0; i < 24; i += step) {
    opts.push(String(i).padStart(2, "0"));
  }
  if (props.allow24 && !opts.includes("24")) opts.push("24");
  return opts;
});

const minuteOptions = computed(() => {
  const step = Math.max(1, Math.min(60, Math.floor(props.minuteStep) || 1));
  const opts = [];
  for (let i = 0; i < 60; i += step) {
    opts.push(String(i).padStart(2, "0"));
  }
  return opts;
});

/** ref 사용 - v-model이 select에서 정상 동작하도록 (24시간 00~23, 분 표시) */
const hourVal = ref("00");
const minuteVal = ref("00");

const syncFromProps = () => {
  const n = normalizeToHHmm(props.modelValue, props.allow24) || "00:00";
  const [h, m] = n.split(":");
  const hourStep = Math.max(1, Math.min(24, Math.floor(props.hourStep) || 1));
  const minuteStep = Math.max(
    1,
    Math.min(60, Math.floor(props.minuteStep) || 1)
  );
  const hClean = String(h || "0").replace(/\D/g, "") || "0";
  const mClean = String(m || "0").replace(/[^\d]/g, "") || "0";
  const maxH = props.allow24 ? 24 : 23;
  const hSnap = snapToStep(hClean, hourStep, maxH);
  const hOk = hourOptions.value.includes(hSnap) ? hSnap : hourOptions.value[0];
  const mOk =
    hOk === "24"
      ? "00"
      : minuteOptions.value.includes(snapToStep(mClean, minuteStep, 59))
        ? snapToStep(mClean, minuteStep, 59)
        : minuteOptions.value[0];
  hourVal.value = hOk;
  minuteVal.value = mOk;
};

watch(
  () => props.modelValue,
  (v) => {
    const n = normalizeToHHmm(v, props.allow24);
    if (n && n !== v) emit("update:modelValue", n);
    syncFromProps();
  },
  { immediate: true }
);

watch(
  () => [props.hourStep, props.minuteStep, props.allow24],
  () => syncFromProps(),
  { deep: true }
);

/** 현재 hourVal/minuteVal 을 부모로 밀어올린다(readonly/disabled 이면 무시). */
const emitCurrent = () => {
  if (props.readonly || props.disabled) return;
  const val = hourVal.value === "24" ? "24:00" : `${hourVal.value}:${minuteVal.value}`;
  if (val !== props.modelValue) {
    emit("update:modelValue", val);
  }
};

watch(
  [hourVal, minuteVal],
  () => {
    // disabled 상태에서는 내부 기본값("00:00")을 부모로 밀어올리지 않는다.
    // immediate:true 라 마운트 직후 1회 무조건 실행되는데, disabled 시점엔 사용자가
    // 아직 아무 값도 선택하지 않은 "미설정" 의미이므로 modelValue 를 건드리면 안 된다
    // (근무타입 생성 팝업의 휴게시간 시작처럼, 휴게(분)=0이라 비활성화된 필드가
    //  본인도 모르게 "00:00"으로 채워져 서버 검증에서 거부되는 결함의 원인이었다).
    emitCurrent();
  },
  { immediate: true }
);

const computedDisabled = computed(() => props.disabled || props.readonly);
/** 24 선택 시 분은 00 고정, 분 셀렉트 비활성화 */
const minuteDisabled = computed(
  () => computedDisabled.value || hourVal.value === "24"
);

const hourOpen = ref(false);
const minuteOpen = ref(false);
const hourWrapRef = ref(null);
const minuteWrapRef = ref(null);
const hourListRef = ref(null);
const minuteListRef = ref(null);

const hourDropdownStyle = ref({});
const minuteDropdownStyle = ref({});

const updateDropdownPosition = async (isHour) => {
  await nextTick();
  const wrap = isHour ? hourWrapRef.value : minuteWrapRef.value;
  if (!wrap) return;
  const rect = wrap.getBoundingClientRect();
  const style = {
    top: `${rect.bottom + 2}px`,
    left: `${rect.left}px`,
    minWidth: `${rect.width}px`,
  };
  if (isHour) {
    hourDropdownStyle.value = style;
  } else {
    minuteDropdownStyle.value = style;
  }
};

const closeAll = () => {
  hourOpen.value = false;
  minuteOpen.value = false;
};

const toggleHour = async () => {
  if (computedDisabled.value) return;
  minuteOpen.value = false;
  hourOpen.value = !hourOpen.value;
  if (hourOpen.value) await updateDropdownPosition(true);
};

const toggleMinute = async () => {
  if (computedDisabled.value) return;
  hourOpen.value = false;
  minuteOpen.value = !minuteOpen.value;
  if (minuteOpen.value) await updateDropdownPosition(false);
};

const selectHour = (opt) => {
  hourVal.value = opt;
  if (opt === "24") minuteVal.value = "00";
  hourOpen.value = false;
  // ref 값이 클릭 전과 동일하면(예: 비활성 상태에서 보이던 기본값 "00"을 활성화 후 그대로 선택)
  // watch([hourVal, minuteVal])가 발화하지 않아 부모에 값이 전달되지 않는다 — 명시적 클릭은
  // 항상 즉시 반영되도록 직접 호출한다(오버나이트 휴게시작 00:00 등 "표시값=선택값" 케이스 대응).
  emitCurrent();
};

const selectMinute = (opt) => {
  minuteVal.value = opt;
  minuteOpen.value = false;
  emitCurrent();
};

const onDocClick = (e) => {
  const wrap = e.target?.closest?.(".time-select-wrap");
  const inDropdown =
    hourListRef.value?.contains?.(e.target) ||
    minuteListRef.value?.contains?.(e.target);
  if (wrap || inDropdown) return;
  closeAll();
};

const onScroll = (e) => {
  const target = e?.target;
  if (
    hourListRef.value?.contains?.(target) ||
    minuteListRef.value?.contains?.(target)
  ) {
    return;
  }
  if (hourOpen.value || minuteOpen.value) closeAll();
};

onMounted(() => {
  document.addEventListener("click", onDocClick);
  window.addEventListener("scroll", onScroll, true);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", onDocClick);
  window.removeEventListener("scroll", onScroll, true);
});
</script>

<style scoped>
.time-input-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 8.5rem;
}
.time-select-wrap {
  position: relative;
}
.time-select {
  flex: 0 1 auto;
  min-width: 3.9rem;
  width: 3.9rem;
  padding: 0.4rem 1.75rem 0.4rem 0.6rem;
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 6px);
  background: #fff;
  box-sizing: border-box;
  cursor: pointer;
  text-align: left;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%234b5563' d='M6 8L2 4h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.4rem center;
}
.time-select:disabled,
.time-select.is-readonly {
  background-color: var(--color-bg, #f9fafb);
  cursor: not-allowed;
  opacity: 0.8;
}
.time-select-dropdown {
  padding: 4px 0;
  max-height: 180px;
  overflow-y: auto;
  list-style: none;
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 6px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 10002;
}
.time-select-dropdown-fixed {
  position: fixed;
  margin: 0;
}
.time-select-dropdown li {
  padding: 0.35rem 0.6rem;
  font-size: 0.875rem;
  cursor: pointer;
}
.time-select-dropdown li:hover,
.time-select-dropdown li.active {
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-primary, #16a34a);
}
.time-sep {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--color-text, #374151);
  font-size: 0.875rem;
}
</style>
