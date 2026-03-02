<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow modal-content-holiday-register"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ isEditMode ? "휴일 수정" : "휴일 등록" }}</span>
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
            <label>휴일명 <span class="required">*</span></label>
            <input v-model="holidayNm" placeholder="예: 창립기념일" />
          </div>

          <div class="form-row-max form-row-date">
            <label>일자 <span class="required">*</span></label>
            <div class="editable-form date-input-wrap">
              <CalendarSrch
                :modelValue="holidayYmd"
                @update:modelValue="holidayYmd = $event ?? ''"
              />
            </div>
          </div>

          <div class="form-row-max form-row-checkbox">
            <label></label>
            <label class="checkbox-wrap">
              <input
                v-model="repeatYearly"
                type="checkbox"
                :disabled="isEditMode"
              />
              <span class="checkbox-label">매년 반복</span>
            </label>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="isSubmitDisabled"
              @click="fnRegister"
            >
              {{ isEditMode ? "수정" : "등록" }}
            </button>
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
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";

const props = defineProps({
  siteCd_p: { type: String, default: "" },
  onSearch: { type: Function, default: null },
  editData_p: { type: Object, default: null },
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
  const maxX = window.innerWidth - (420 + padding);
  const maxY = window.innerHeight - (420 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

const holidayId = ref("");
const holidayNm = ref("");
const holidayYmd = ref("");
const repeatYearly = ref(false);

const isEditMode = computed(() => !!props.editData_p);
const isPublicHoliday = computed(() => props.editData_p?.holidayType === "01");
const isSubmitDisabled = computed(
  () => isEditMode.value && isPublicHoliday.value
);

/** 오늘 날짜 YYYY-MM-DD 형식 */
const getTodayYmd = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(
    2,
    "0"
  )}-${String(now.getDate()).padStart(2, "0")}`;
};

onMounted(() => {
  if (props.editData_p) {
    holidayId.value = props.editData_p.holidayId ?? "";
    holidayNm.value = props.editData_p.holidayNm ?? "";
    holidayYmd.value = props.editData_p.holidayYmd
      ? String(props.editData_p.holidayYmd).slice(0, 10)
      : getTodayYmd();
    repeatYearly.value = props.editData_p.repeatYearly ?? false;
  } else {
    holidayYmd.value = getTodayYmd();
  }
});

const fnRegister = async () => {
  if (isSubmitDisabled.value) return;
  if (!holidayNm.value?.trim()) {
    proxy.$alert("휴일명을 입력해주세요.");
    return;
  }
  if (!holidayYmd.value?.trim()) {
    proxy.$alert("일자를 선택해주세요.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    // TODO: API 연동 시 주석 해제
    const response = await axios.post("/webApi/attd02/update-holiday-infos", {
      siteCd: props.siteCd_p,
      holidayId: holidayId.value,
      holidayNm: holidayNm.value.trim(),
      holidayYmd: holidayYmd.value,
      holidayType: "02",
      repeatYearly: repeatYearly.value,
      useYn: "Y",
    });
    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      props.onSearch?.();
      emit("close");
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "등록 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
.modal-content-holiday-register {
  min-width: 380px;
}
.required {
  color: #dc2626;
}
/* 일자 - CalendarSrch 가로 꽉 차게 */
.form-row-date .date-input-wrap {
  flex: 1;
  min-width: 0;
}
.form-row-date .date-input-wrap :deep(.calendar-search) {
  width: 100%;
}
.form-row-date .date-input-wrap :deep(.calendar-input) {
  width: 100%;
  box-sizing: border-box;
}
/* 매년 반복 - 텍스트 개행 방지 */
.form-row-checkbox .checkbox-wrap {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: 400;
}
.form-row-checkbox .checkbox-wrap .checkbox-label {
  white-space: nowrap;
}
.form-row-checkbox input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}
</style>
