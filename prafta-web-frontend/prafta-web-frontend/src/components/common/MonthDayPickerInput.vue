<template>
  <div class="month-day-picker-input-wrap" ref="wrapRef">
    <button
      type="button"
      class="month-day-input"
      :class="{ 'is-readonly': readonly, 'is-disabled': disabled }"
      :disabled="disabled"
      @click="toggle"
    >
      <span class="input-value">{{ displayValue }}</span>
      <span class="input-icon" aria-hidden="true">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
          <line x1="16" y1="2" x2="16" y2="6" />
          <line x1="8" y1="2" x2="8" y2="6" />
          <line x1="3" y1="10" x2="21" y2="10" />
        </svg>
      </span>
    </button>
    <Teleport to="body">
      <div
        v-show="open"
        ref="popoverRef"
        class="month-day-picker-popover"
        :style="popoverStyle"
      >
        <MonthDayPicker
          :model-value="modelValue"
          :disabled="disabled"
          :readonly="readonly"
          @update:model-value="onSelect"
        />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from "vue";
import MonthDayPicker from "./MonthDayPicker.vue";

const props = defineProps({
  modelValue: { type: String, default: "" }, // MMDD 4자리
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);

const wrapRef = ref(null);
const popoverRef = ref(null);
const open = ref(false);
const popoverStyle = ref({});

const displayValue = computed(() => {
  const v = props.modelValue;
  if (!v || v.length < 4) return "MM-DD";
  const mm = v.slice(0, 2);
  const dd = v.slice(2, 4);
  return `${mm}-${dd}`;
});

const updatePosition = async () => {
  await nextTick();
  const wrap = wrapRef.value;
  if (!wrap) return;
  const rect = wrap.getBoundingClientRect();
  popoverStyle.value = {
    position: "fixed",
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    zIndex: 9999,
  };
};

const toggle = async () => {
  if (props.disabled || props.readonly) return;
  open.value = !open.value;
  if (open.value) await updatePosition();
};

const onSelect = (val) => {
  emit("update:modelValue", val);
  open.value = false;
};

const closePopover = () => {
  open.value = false;
};

const onDocClick = (e) => {
  const wrap = wrapRef.value;
  const popover = popoverRef.value;
  if (
    wrap?.contains?.(e.target) ||
    popover?.contains?.(e.target)
  ) {
    return;
  }
  closePopover();
};

const onScroll = () => {
  closePopover();
};

/** 스크롤 가능한 부모 요소들 찾기 */
const getScrollParents = (el) => {
  const parents = [];
  let p = el?.parentElement;
  while (p) {
    const style = getComputedStyle(p);
    const overflow = style.overflow + style.overflowY + style.overflowX;
    if (/(auto|scroll|overlay)/.test(overflow)) {
      parents.push(p);
    }
    p = p.parentElement;
  }
  return parents;
};

const scrollParents = ref([]);

watch(open, (v) => {
  if (v) {
    document.addEventListener("click", onDocClick, true);
    window.addEventListener("scroll", onScroll, true);
    scrollParents.value = getScrollParents(wrapRef.value);
    scrollParents.value.forEach((el) => el.addEventListener("scroll", onScroll, true));
  } else {
    document.removeEventListener("click", onDocClick, true);
    window.removeEventListener("scroll", onScroll, true);
    scrollParents.value.forEach((el) => el.removeEventListener("scroll", onScroll, true));
    scrollParents.value = [];
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("click", onDocClick, true);
  window.removeEventListener("scroll", onScroll, true);
  scrollParents.value.forEach((el) => el.removeEventListener("scroll", onScroll, true));
});
</script>

<style scoped>
.month-day-picker-input-wrap {
  position: relative;
  display: inline-block;
  flex: 1;
  min-width: 0;
}

.month-day-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
  min-width: 7rem;
  padding: 0.5rem 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 0.875rem;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.month-day-input:hover:not(.is-readonly):not(.is-disabled) {
  border-color: #cbd5e1;
}

.month-day-input:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.month-day-input.is-readonly,
.month-day-input.is-disabled {
  cursor: not-allowed;
  background: #f8fafc;
  color: #64748b;
}

.input-value {
  flex: 1;
  min-width: 0;
}

.input-icon {
  flex-shrink: 0;
  color: #94a3b8;
}

.month-day-picker-popover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  overflow: visible;
  width: fit-content;
}
</style>
