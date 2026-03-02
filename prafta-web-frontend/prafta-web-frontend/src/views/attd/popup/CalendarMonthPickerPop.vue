<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow modal-content-month-picker"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>년·월 선택</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <div class="form-row-max editable-form">
            <label>년</label>
            <input
              v-model.number="yearVal"
              type="number"
              min="2000"
              max="2100"
              placeholder="예: 2026"
            />
          </div>
          <div class="form-row-max editable-form">
            <label>월</label>
            <select v-model.number="monthVal">
              <option v-for="m in 12" :key="m" :value="m">{{ m }}월</option>
            </select>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnConfirm">확인</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  getCurrentInstance,
  defineProps,
  defineEmits,
  onMounted,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const props = defineProps({
  year_p: { type: Number, default: () => new Date().getFullYear() },
  month_p: { type: Number, default: () => new Date().getMonth() + 1 },
  onConfirm: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const positionStyle = computed(() => {
  const padding = 16;
  const maxX = window.innerWidth - (320 + padding);
  const maxY = window.innerHeight - (280 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

const yearVal = ref(props.year_p);
const monthVal = ref(props.month_p);

onMounted(() => {
  yearVal.value = props.year_p;
  monthVal.value = props.month_p;
});

const fnConfirm = () => {
  const y = Number(yearVal.value);
  const m = Number(monthVal.value);
  if (Number.isNaN(y) || y < 2000 || y > 2100) {
    proxy.$alert("년도는 2000~2100 사이로 입력해주세요.");
    return;
  }
  if (Number.isNaN(m) || m < 1 || m > 12) {
    proxy.$alert("월은 1~12 사이로 선택해주세요.");
    return;
  }
  props.onConfirm?.(y, m);
  emit("close");
};
</script>

<style scoped>
.modal-content-month-picker {
  min-width: 280px;
}
</style>
