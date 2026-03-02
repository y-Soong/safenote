<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow modal-content-sch-info"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ isEditMode ? "스케줄 정보" : "스케줄 타입 생성" }}</span>
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

        <div class="info-banner">
          <span class="info-icon">ℹ</span>
          <span>스케줄은 최대 2구간까지 설정할 수 있습니다.</span>
        </div>

        <div class="form-container">
          <div class="form-row-max editable-form">
            <label>타입코드 *</label>
            <input
              v-model="schNo"
              placeholder="예: ST008"
              :disabled="isEditMode"
            />
          </div>

          <div class="form-row-max">
            <label>적용일 *</label>
            <div class="editable-form">
              <CalendarSrch
                :modelValue="applyDate"
                @update:modelValue="onApplyDateChange"
                :style="calendarInputStyle"
              />
            </div>
          </div>

          <div class="form-row-max editable-form">
            <label>기본근무</label>
            <select v-model="baseYn" :style="calendarInputStyle">
              <option value="Y">예</option>
              <option value="N">아니오</option>
            </select>
          </div>

          <div class="form-row-max">
            <label>구간수 *</label>
            <div class="section-toggle">
              <button
                type="button"
                :class="['btn-segment', { active: schType === '01' }]"
                :disabled="isEditMode"
                @click="schType = '01'"
              >
                1구간
              </button>
              <button
                type="button"
                :class="['btn-segment', { active: schType === '02' }]"
                :disabled="isEditMode"
                @click="schType = '02'"
              >
                2구간
              </button>
            </div>
          </div>

          <div class="section-block">
            <div class="section-block-title">구간1</div>
            <div class="work-time-block">
              <label class="work-time-label">근무시간 *</label>
              <div class="work-time-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput v-model="fstSchStrTime" :minute-step="10" />
                <span class="time-sep-label">~</span>
                <span class="work-time-sub">종료</span>
                <TimeInput v-model="fstSchEndTime" :minute-step="10" />
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간(분단위)</label>
              <div class="time-row-inline">
                <div class="break-time-input-wrap">
                  <input
                    type="text"
                    inputmode="numeric"
                    v-model="fstSchBrkMin"
                    @input="onBreakMinInput($event, 'fst')"
                  />
                </div>
              </div>
            </div>
            <p class="form-hint">
              휴게시간은 근무시간 산정 시 자동으로 적용됩니다.
            </p>
          </div>

          <div v-if="schType === '02'" class="section-block">
            <div class="section-block-title">구간2</div>
            <div class="work-time-block">
              <label class="work-time-label">근무시간 *</label>
              <div class="work-time-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput v-model="secSchStrTime" :minute-step="10" />
                <span class="time-sep-label">~</span>
                <span class="work-time-sub">종료</span>
                <TimeInput v-model="secSchEndTime" :minute-step="10" />
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간(분단위)</label>
              <div class="time-row-inline">
                <div class="break-time-input-wrap">
                  <input
                    type="text"
                    inputmode="numeric"
                    v-model="secSchBrkMin"
                    @input="onBreakMinInput($event, 'sec')"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="form-row-max editable-form">
            <label>사용여부</label>
            <BaseSelect v-model="useYn">
              <option
                v-for="opt in (systCodeArr['SYS003'] || []).filter(
                  (o) => o.systValDCd != null
                )"
                :key="opt.systValDCd"
                :value="opt.systValDCd"
              >
                {{ opt.systValDNm }}
              </option>
            </BaseSelect>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSave">
              {{ isEditMode ? "저장" : "생성" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
// ================ Imports ================
import { ref, computed, onMounted, getCurrentInstance, nextTick } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import BaseSelect from "@/components/common/BaseSelect.vue";
import TimeInput from "@/components/common/TimeInput.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";

// ================ Props & Emits ================
const props = defineProps({
  schData_p: { type: Object, default: null },
  siteCd_p: { type: String, default: "" },
  siteNm_p: { type: String, default: "" },
  systCodeArr_p: { type: Object, default: () => ({}) },
  onSave: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Utility (ref 초기화에 사용) ================
/** 시간 범위 문자열("09:00~18:00") 파싱 → [시작, 종료] */
const parseTimeRange = (v) => {
  if (!v || typeof v !== "string") return ["", ""];
  const raw = String(v)
    .replace(/[~-]/g, "~")
    .split("~")
    .map((s) => s.trim());
  return [raw[0] ?? "", raw[1] ?? ""];
};
/** 휴게시간(분): 숫자만 추출, 최대 3자리 */
const sanitizeBreakMin = (v) =>
  (v != null ? String(v).replace(/\D/g, "").slice(0, 3) : "") || "0";

// ================ Refs (Variables) ================
// 모달 위치/스타일
const positionStyle = computed(() => {
  const padding = 16;
  const maxX = window.innerWidth - (560 + padding);
  const maxY = window.innerHeight - (400 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});
const isEditMode = computed(() => !!props.schData_p);

// 공통코드/유틸
const systCodeArr = ref(props.systCodeArr_p);
const calendarInputStyle = { width: "10rem", height: "2rem" };

// 저장 변수
const cmpnyCd = ref("");
const siteCd = ref("");
const schCd = ref("");

// 폼 입력 변수
const schNo = ref("");
const schType = ref("01");
const applyDate = ref("");
const baseYn = ref(props.schData_p?.baseYn ?? "Y");
/** 적용일: 프로그래밍 방식 설정 시 검증 스킵 (무한루프 방지) */
const isProgrammaticApplyDate = ref(false);

const fstSchStrTime = ref(props.schData_p?.fstSchStrTime ?? "");
const fstSchEndTime = ref(props.schData_p?.fstSchEndTime ?? "");
const fstSchBrkMin = ref(
  sanitizeBreakMin(props.schData_p?.fstSchBrkMin) ?? "0"
);

const secSchStrTime = ref(props.schData_p?.secSchStrTime ?? "");
const secSchEndTime = ref(props.schData_p?.secSchEndTime ?? "");
const secSchBrkMin = ref(
  sanitizeBreakMin(props.schData_p?.secSchBrkMin) ?? "0"
);
const useYn = ref(
  props.schData_p?.useYn ?? props.systCodeArr_p?.SYS003?.[0]?.systValDCd ?? "Y"
);

// ================ Life Cycle Functions ================
onMounted(() => {
  systCodeArr.value = props.systCodeArr_p;

  if (props.systCodeArr_p?.SYS003?.length && !props.schData_p) {
    useYn.value = props.systCodeArr_p.SYS003[0]?.systValDCd ?? "Y";
  }
  if (props.systCodeArr_p?.SYS019?.length && !props.schData_p) {
    schType.value = props.systCodeArr_p.SYS019[0]?.systValDCd ?? "01";
  }
  if (props.schData_p) {
    cmpnyCd.value = props.schData_p.cmpnyCd;
    siteCd.value = props.schData_p.siteCd;
    schCd.value = props.schData_p.schCd;
    schNo.value = props.schData_p.schNo ?? "";
    schType.value = props.schData_p.schType ?? "01";
    baseYn.value = props.schData_p.baseYn ?? "Y";
    isProgrammaticApplyDate.value = true;
    applyDate.value =
      formatYyyyMmDd(
        props.schData_p.applyDate ??
          props.schData_p.applyDt ??
          props.schData_p.aplyDt
      ) ?? "";
    nextTick(() => {
      isProgrammaticApplyDate.value = false;
    });
  }
  if (!props.schData_p && !applyDate.value) {
    cmpnyCd.value = sessionStorage.getItem("gv_cmpnyCd");
    siteCd.value = props.siteCd_p;
    schCd.value = "";

    isProgrammaticApplyDate.value = true;
    applyDate.value = formatYyyyMmDd(new Date());
    nextTick(() => {
      isProgrammaticApplyDate.value = false;
    });
  }
});

// ================ API Functions ================
const fnSave = async () => {
  if (!schNo.value?.trim()) {
    proxy.$alert("타입코드를 입력해주세요.");
    return;
  }
  if (!applyDate.value?.trim()) {
    proxy.$alert("적용일을 선택해주세요.");
    return;
  }
  const timeValidation = validateWorkTime();
  if (!timeValidation.valid) {
    proxy.$alert(timeValidation.message);
    return;
  }
  if (isEditMode.value) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selected = new Date(applyDate.value);
    selected.setHours(0, 0, 0, 0);
    if (selected <= today) {
      proxy.$alert("수정 시 적용일은 내일 이후만 선택 가능합니다.");
      return;
    }
  }

  const ok = await proxy.$confirm(
    isEditMode.value ? "저장하시겠습니까?" : "생성하시겠습니까?"
  );
  if (!ok) return;

  const toHHmm = (v) =>
    v ? String(v).replace(/\D/g, "").slice(0, 4).padStart(4, "0") : "";
  const joinRange = (a, b) =>
    a && b ? `${toHHmm(a)}~${toHHmm(b)}` : toHHmm(a) || toHHmm(b) || "";
  const payload = {
    cmpnyCd: cmpnyCd.value,
    siteCd: siteCd.value,
    schCd: schCd.value,
    schNo: schNo.value.trim(),
    schType: schType.value,
    applyDate: applyDate.value.trim(),
    fstSchStrTime: fstSchStrTime.value,
    fstSchEndTime: fstSchEndTime.value,
    fstSchBrkMin: fstSchBrkMin.value,

    secSchStrTime: secSchStrTime.value,
    secSchEndTime: secSchEndTime.value,
    secSchBrkMin: schType.value === "02" ? secSchBrkMin.value : "",

    baseYn: baseYn.value,
    useYn: useYn.value,
  };

  try {
    const response = await axios.post(
      "/webApi/attd01/update-sch-infos",
      payload
    );
    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "처리 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
/** 적용일 변경 시: 수정 모드에서만 오늘 이전 선택 불가 */
const onApplyDateChange = (newVal) => {
  if (isProgrammaticApplyDate.value) {
    isProgrammaticApplyDate.value = false;
    applyDate.value = newVal ?? "";
    return;
  }
  if (newVal === applyDate.value) return;
  if (!newVal || typeof newVal !== "string") return;

  if (!isEditMode.value) {
    applyDate.value = newVal;
    return;
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const selected = new Date(newVal);
  selected.setHours(0, 0, 0, 0);

  if (selected <= today) {
    proxy.$alert("적용일은 내일 이후만 선택 가능합니다.");
    const prev = applyDate.value;
    isProgrammaticApplyDate.value = true;
    applyDate.value = "";
    nextTick(() => {
      applyDate.value = prev;
      setTimeout(() => {
        isProgrammaticApplyDate.value = false;
      }, 0);
    });
    return;
  }
  applyDate.value = newVal;
};

/** 근무시간 검증: 형식, 시작<종료, 구간 겹침 방지. { valid, message } 반환 */
const validateWorkTime = () => {
  const toMinutes = (v) => {
    if (!v || typeof v !== "string") return null;
    const s = String(v).trim().replace(/\D/g, "");
    if (s.length < 4) return null;
    const h = parseInt(s.slice(0, 2), 10);
    const m = parseInt(s.slice(2, 4), 10);
    if (h < 0 || h > 23 || m < 0 || m > 59) return null;
    return h * 60 + m;
  };

  const fstStart = toMinutes(fstSchStrTime.value);
  const fstEnd = toMinutes(fstSchEndTime.value);
  if (fstStart == null || fstEnd == null) {
    return { valid: false, message: "구간1 근무시간을 올바르게 입력해주세요." };
  }
  if (fstStart >= fstEnd) {
    return {
      valid: false,
      message: "구간1 종료시간은 시작시간보다 뒤여야 합니다.",
    };
  }

  if (schType.value === "02") {
    const secStart = toMinutes(secSchStrTime.value);
    const secEnd = toMinutes(secSchEndTime.value);
    if (secStart == null || secEnd == null) {
      return {
        valid: false,
        message: "구간2 근무시간을 올바르게 입력해주세요.",
      };
    }
    if (secStart >= secEnd) {
      return {
        valid: false,
        message: "구간2 종료시간은 시작시간보다 뒤여야 합니다.",
      };
    }
    if (fstStart < secEnd && secStart < fstEnd) {
      return {
        valid: false,
        message: "구간1과 구간2의 근무시간이 겹치면 안 됩니다.",
      };
    }
  }

  const fstWorkMin = fstEnd - fstStart;
  const fstBrk = parseInt(String(fstSchBrkMin.value || "0"), 10) || 0;
  if (fstBrk > fstWorkMin) {
    return {
      valid: false,
      message:
        "구간1 휴게시간은 근무시간(" + fstWorkMin + "분)보다 많을 수 없습니다.",
    };
  }

  if (schType.value === "02") {
    const secStart2 = toMinutes(secSchStrTime.value);
    const secEnd2 = toMinutes(secSchEndTime.value);
    const secWorkMin =
      secStart2 != null && secEnd2 != null ? secEnd2 - secStart2 : 0;
    const secBrk = parseInt(String(secSchBrkMin.value || "0"), 10) || 0;
    if (secBrk > secWorkMin) {
      return {
        valid: false,
        message:
          "구간2 휴게시간은 근무시간(" +
          secWorkMin +
          "분)보다 많을 수 없습니다.",
      };
    }
  }

  return { valid: true };
};

/** 날짜를 yyyy-mm-dd 형식으로 변환 (Date, 8자리 문자열 지원) */
const formatYyyyMmDd = (d) => {
  if (!d) return "";
  if (d instanceof Date) {
    if (isNaN(d.getTime())) return "";
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  }
  const s = String(d).replace(/\D/g, "");
  if (s.length >= 8) {
    const y = s.slice(0, 4);
    const m = s.slice(4, 6);
    const day = s.slice(6, 8);
    return `${y}-${m}-${day}`;
  }
  const date = new Date(d);
  if (isNaN(date.getTime())) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

/** 휴게시간 input: 숫자만, 3자리 제한 */
const onBreakMinInput = (e, which) => {
  const raw = (e.target?.value ?? "").replace(/\D/g, "").slice(0, 3);
  if (e.target) e.target.value = raw;
  if (which === "fst") fstSchBrkMin.value = raw;
  else secSchBrkMin.value = raw;
};
</script>

<style scoped>
.info-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  margin: 0 1.2rem;
  background: #ecfdf5;
  border-radius: 6px;
  font-size: 0.8125rem;
  color: #065f46;
}
.info-icon {
  flex-shrink: 0;
  width: 1.25rem;
  height: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #10b981;
  color: #fff;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 700;
}
.form-container {
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-x: hidden;
  min-width: 0;
}
.section-toggle {
  display: flex;
  gap: 0.25rem;
}
.btn-segment {
  padding: 0.4rem 1rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text, #374151);
}
.btn-segment.active {
  border-color: var(--color-primary, #16a34a);
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary);
}
.btn-segment:disabled {
  background: var(--color-bg, #f9fafb);
  cursor: not-allowed;
  opacity: 0.7;
}
.section-block {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 1rem;
  background: #fafafa;
}
.section-block-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
  color: var(--color-text, #374151);
}
.form-row-spaced {
  margin-top: 1rem;
}
.work-time-block {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.work-time-label {
  font-weight: 500;
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  flex-shrink: 0;
}
.work-time-inputs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.work-time-sub {
  font-size: 0.75rem;
  color: #6b7280;
  flex-shrink: 0;
}
.work-time-inputs :deep(.time-input-wrap) {
  min-width: 5rem;
}
.time-row-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  min-width: 0;
}
.break-time-input-wrap {
  width: 8.5rem;
  flex-shrink: 0;
}
.break-time-input-wrap input {
  width: 85%;
  box-sizing: border-box;
  padding: 0.4rem 0.6rem;
  font-size: 0.875rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 6px);
}
.time-sep-label {
  flex-shrink: 0;
  font-weight: 500;
  color: var(--color-text, #374151);
}
.form-row-max.form-row-spaced > label {
  flex: 0 0 120px;
}
.form-row-max.form-row-spaced .time-row-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.form-hint {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.5rem;
  margin-bottom: 0;
}
.modal-content-sch-info {
  max-width: 560px;
  width: min(92vw, 560px);
}
</style>
