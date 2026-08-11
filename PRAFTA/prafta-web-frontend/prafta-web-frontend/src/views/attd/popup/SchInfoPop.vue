<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow modal-content-sch-info"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ isEditMode ? "근무타입 정보" : "근무타입 생성" }}</span>
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
                <TimeInput v-model="fstSchEndTime" :minute-step="10" allow24 />
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간(분단위)</label>
              <div class="time-row-inline">
                <div class="break-time-input-wrap">
                  <input
                    type="text"
                    inputmode="numeric"
                    :value="fstSchBrkMin"
                    @input="onBreakMinInput($event, 'fst')"
                  />
                </div>
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간 시작</label>
              <div class="work-time-inputs break-start-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput
                  v-model="fstBrkStrTime"
                  :minute-step="10"
                  :disabled="!fstBrkEnabled"
                />
              </div>
            </div>
            <p class="form-hint">
              휴게시간은 근무시간 산정 시 자동으로 적용됩니다. 휴게 시작시각을
              입력하면 종료시각은 휴게시간(분)만큼 자동 계산되며, 시간단위 연차
              신청 시 휴게 가로지름을 차단합니다.
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
                <TimeInput v-model="secSchEndTime" :minute-step="10" allow24 />
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간(분단위)</label>
              <div class="time-row-inline">
                <div class="break-time-input-wrap">
                  <input
                    type="text"
                    inputmode="numeric"
                    :value="secSchBrkMin"
                    @input="onBreakMinInput($event, 'sec')"
                  />
                </div>
              </div>
            </div>
            <div class="form-row-max form-row-spaced">
              <label>휴게시간 시작</label>
              <div class="work-time-inputs break-start-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput
                  v-model="secBrkStrTime"
                  :minute-step="10"
                  :disabled="!secBrkEnabled"
                />
              </div>
            </div>
          </div>

          <!-- PRAFTA-FIXEDOT-1: 고정연장근무(포괄임금형 고정 OT) — 후방(퇴근 후) 기본 노출, 전방(출근 전)은 접힘 옵션.
               "야간" 용어 금지(법정 야간 22~06시 가산 축과 무관 — 정책 ⑤). -->
          <div class="section-block">
            <div class="section-block-title">고정연장근무</div>
            <div class="form-row-max">
              <label>고정연장근무 사용</label>
              <div class="section-toggle">
                <button
                  type="button"
                  :class="['btn-segment', { active: fixedOtEnabled }]"
                  @click="toggleFixedOt(true)"
                >
                  사용
                </button>
                <button
                  type="button"
                  :class="['btn-segment', { active: !fixedOtEnabled }]"
                  @click="toggleFixedOt(false)"
                >
                  미사용
                </button>
              </div>
            </div>
            <!-- 후방(퇴근 후): 토글 ON 시에만 렌더 — disabled TimeInput 기본값 밀어올림 함정 회피(커밋 ee85a483 규약) -->
            <div v-if="fixedOtEnabled" class="work-time-block form-row-spaced">
              <label class="work-time-label">퇴근 후 고정연장 *</label>
              <div class="work-time-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput v-model="fixedOtStrTime" :minute-step="10" />
                <span class="time-sep-label">~</span>
                <span class="work-time-sub">종료</span>
                <TimeInput v-model="fixedOtEndTime" :minute-step="10" />
              </div>
            </div>
            <p v-if="fixedOtEnabled" class="form-hint">
              고정연장근무는 소정 근무 종료 이후 구간만 입력할 수 있습니다. 종료
              시각이 시작 시각보다 빠르면 자정을 넘기는 근무로 처리됩니다.
            </p>
            <!-- 전방(출근 전): 접힘 옵션 — 펼칠 때만 입력 렌더, 접으면 값 초기화 -->
            <div class="form-row-spaced">
              <button
                type="button"
                class="fixed-ot-collapse-toggle"
                @click="togglePreFixedOt"
              >
                <span aria-hidden="true">{{ preFixedOtOpen ? "▾" : "▸" }}</span>
                출근 전 고정연장 추가
              </button>
            </div>
            <div v-if="preFixedOtOpen" class="work-time-block form-row-spaced">
              <label class="work-time-label">출근 전 고정연장 *</label>
              <div class="work-time-inputs">
                <span class="work-time-sub">시작</span>
                <TimeInput v-model="preFixedOtStrTime" :minute-step="10" />
                <span class="time-sep-label">~</span>
                <span class="work-time-sub">종료</span>
                <TimeInput v-model="preFixedOtEndTime" :minute-step="10" />
              </div>
            </div>
            <p v-if="preFixedOtOpen" class="form-hint">
              출근 전 고정연장은 소정 근무 시작 이전의 당일 구간만 입력할 수
              있습니다.
            </p>
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
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

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
/** 적용일: 프로그래밍 방식 설정 시 검증 스킵 (무한루프 방지) */
const isProgrammaticApplyDate = ref(false);

const fstSchStrTime = ref(props.schData_p?.fstSchStrTime ?? "00:00");
const fstSchEndTime = ref(props.schData_p?.fstSchEndTime ?? "18:00");
const fstSchBrkMin = ref(
  sanitizeBreakMin(props.schData_p?.fstSchBrkMin) ?? "0"
);
const fstBrkStrTime = ref(props.schData_p?.fstBrkStrTime ?? "");

const secSchStrTime = ref(props.schData_p?.secSchStrTime ?? "00:00");
const secSchEndTime = ref(props.schData_p?.secSchEndTime ?? "18:00");
const secSchBrkMin = ref(
  sanitizeBreakMin(props.schData_p?.secSchBrkMin) ?? "0"
);
const secBrkStrTime = ref(props.schData_p?.secBrkStrTime ?? "");
const useYn = ref(
  props.schData_p?.useYn ?? props.systCodeArr_p?.SYS003?.[0]?.systValDCd ?? "Y"
);

// PRAFTA-FIXEDOT-1: 고정연장근무 — 후방(퇴근 후) 쌍 + 전방(출근 전) 쌍(HHMM).
// 수정 모드에서 저장값이 있으면 해당 영역을 펼친 상태로 프리필한다.
const fixedOtStrTime = ref(props.schData_p?.fixedOtStrTime ?? "");
const fixedOtEndTime = ref(props.schData_p?.fixedOtEndTime ?? "");
const preFixedOtStrTime = ref(props.schData_p?.preFixedOtStrTime ?? "");
const preFixedOtEndTime = ref(props.schData_p?.preFixedOtEndTime ?? "");
const fixedOtEnabled = ref(
  !!(props.schData_p?.fixedOtStrTime && props.schData_p?.fixedOtEndTime)
);
const preFixedOtOpen = ref(
  !!(props.schData_p?.preFixedOtStrTime && props.schData_p?.preFixedOtEndTime)
);

/**
 * 후방(퇴근 후) 고정연장 토글. ON 시 시작·종료를 소정 마지막 구간 종료 시각으로 프리필한다
 * (같은 시각이면 저장 검증에서 차단되므로 사용자가 종료를 반드시 명시하게 된다).
 * OFF 시 값 초기화 — 토글 OFF 시 값 초기화 규약(TimeInput disabled 함정, 커밋 ee85a483).
 */
const toggleFixedOt = (on) => {
  if (fixedOtEnabled.value === on) return;
  fixedOtEnabled.value = on;
  if (on) {
    const base =
      schType.value === "02" ? secSchEndTime.value : fstSchEndTime.value;
    if (!fixedOtStrTime.value) fixedOtStrTime.value = base || "";
    if (!fixedOtEndTime.value) fixedOtEndTime.value = base || "";
  } else {
    fixedOtStrTime.value = "";
    fixedOtEndTime.value = "";
  }
};

/**
 * 전방(출근 전) 고정연장 접힘 토글. 펼칠 때 시작·종료를 소정 1구간 시작 시각으로 프리필,
 * 접으면 값 초기화(미입력 상태로 복귀).
 */
const togglePreFixedOt = () => {
  preFixedOtOpen.value = !preFixedOtOpen.value;
  if (preFixedOtOpen.value) {
    if (!preFixedOtStrTime.value)
      preFixedOtStrTime.value = fstSchStrTime.value || "";
    if (!preFixedOtEndTime.value)
      preFixedOtEndTime.value = fstSchStrTime.value || "";
  } else {
    preFixedOtStrTime.value = "";
    preFixedOtEndTime.value = "";
  }
};

/**
 * 휴게 종료시각 자동 계산: 시작시각 + 휴게시간(분).
 * 시작 미입력 시 빈 문자열. 24:00(1440분) 상한.
 */
const addMinutesToHHmm = (hhmm, addMin) => {
  const s = String(hhmm ?? "").replace(/\D/g, "");
  if (s.length < 4) return "";
  const h = parseInt(s.slice(0, 2), 10);
  const m = parseInt(s.slice(2, 4), 10);
  if (isNaN(h) || isNaN(m)) return "";
  let total = h * 60 + m + (parseInt(String(addMin ?? "0"), 10) || 0);
  if (total > 1440) total = 1440;
  if (total < 0) total = 0;
  const hh = String(Math.floor(total / 60)).padStart(2, "0");
  const mm = String(total % 60).padStart(2, "0");
  return `${hh}:${mm}`;
};
/** 구간1 휴게 종료시각(시작+분 자동계산). 시작 미입력이면 빈값. */
const fstBrkEndDerived = computed(() =>
  fstBrkStrTime.value ? addMinutesToHHmm(fstBrkStrTime.value, fstSchBrkMin.value) : ""
);
/** 구간2 휴게 종료시각(시작+분 자동계산). 시작 미입력이면 빈값. */
const secBrkEndDerived = computed(() =>
  secBrkStrTime.value ? addMinutesToHHmm(secBrkStrTime.value, secSchBrkMin.value) : ""
);

/** 휴게시간(분)이 1 이상일 때만 휴게시간 시작 입력 활성화 */
const fstBrkEnabled = computed(
  () => (parseInt(String(fstSchBrkMin.value || "0"), 10) || 0) >= 1
);
const secBrkEnabled = computed(
  () => (parseInt(String(secSchBrkMin.value || "0"), 10) || 0) >= 1
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
    useYn.value = props.schData_p.useYn ?? "Y";
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
    proxy.$alert(getMessage(MSG.TYPE_CODE_REQUIRED));
    return;
  }
  if (!applyDate.value?.trim()) {
    proxy.$alert(getMessage(MSG.APPLY_DATE_REQUIRED));
    return;
  }
  const timeValidation = validateWorkTime();
  if (!timeValidation.valid) {
    proxy.$alert(timeValidation.message);
    return;
  }
  // 오버나이트(시작 > 종료) 후보 구간은 자정 넘김 근무인지 사용자에게 컨펌받는다.
  if (timeValidation.fstOvernight) {
    const okOvernight = await proxy.$confirm(
      getMessage(MSG.OVERNIGHT_CONFIRM, { section: "구간1" })
    );
    if (!okOvernight) return;
  }
  if (schType.value === "02" && timeValidation.secOvernight) {
    const okOvernight2 = await proxy.$confirm(
      getMessage(MSG.OVERNIGHT_CONFIRM, { section: "구간2" })
    );
    if (!okOvernight2) return;
  }
  // PRAFTA-FIXEDOT-1: 고정연장근무 프리체크(V1~V6 — 백엔드 ATTD_400_198 룰과 동일 문구).
  const fixedOtValidation = validateFixedOt();
  if (!fixedOtValidation.valid) {
    proxy.$alert(fixedOtValidation.message);
    return;
  }
  // V5: 후방 고정연장이 자정을 넘기면(종료<시작) 오버나이트 여부 컨펌.
  if (fixedOtValidation.rearOvernight) {
    const okFixedOtOvernight = await proxy.$confirm(
      getMessage(MSG.OVERNIGHT_CONFIRM, { section: "고정연장근무" })
    );
    if (!okFixedOtOvernight) return;
  }
  if (isEditMode.value) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selected = new Date(applyDate.value);
    selected.setHours(0, 0, 0, 0);

    if (selected <= today) {
      proxy.$alert(getMessage(MSG.APPLY_DATE_FUTURE));
      return;
    }
  }

  // PC-09(N6): 소정근로(근무시간 − 휴게시간 합) 8시간(480분) 초과 시 저장 전 경고.
  //   저장은 허용(차단 아님) — 시간차 연차 분모는 8시간 캡으로 계산됨을 안내한다.
  const stdWorkMin = calcStdWorkMinutes();
  if (stdWorkMin != null && stdWorkMin > 480) {
    await proxy.$alert(
      "소정근로가 8시간을 초과합니다. 휴게시간 입력을 확인하세요.\n(시간차 연차 분모는 8시간으로 계산됩니다)"
    );
  }

  // PRAFTA-FIXEDOT-1(V7): 고정연장 일 합계 4시간(240분) 초과 시 경고 — 차단 아님.
  if (fixedOtValidation.totalFixedOtMin > 240) {
    await proxy.$alert(
      "고정연장근무 합계가 1일 4시간을 초과합니다. 추가 휴게시간 의무와 주 12시간 연장 한도를 확인하세요.\n(저장은 가능합니다)"
    );
  }

  const ok = await proxy.$confirm(
    isEditMode.value ? getMessage(MSG.SAVE_CONFIRM) : getMessage(MSG.CREATE_CONFIRM)
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
    fstBrkStrTime: fstBrkStrTime.value,
    fstBrkEndTime: fstBrkEndDerived.value,

    // 1구간(schType='01')일 때는 2구간 시각을 빈값으로 보낸다.
    // (ref 기본값 "00:00"/"18:00" 이 그대로 저장돼 앱에서 2구간으로 오표시되는 결함 방지.)
    secSchStrTime: schType.value === "02" ? secSchStrTime.value : "",
    secSchEndTime: schType.value === "02" ? secSchEndTime.value : "",
    secSchBrkMin: schType.value === "02" ? secSchBrkMin.value : "",
    secBrkStrTime: schType.value === "02" ? secBrkStrTime.value : "",
    secBrkEndTime: schType.value === "02" ? secBrkEndDerived.value : "",

    // PRAFTA-FIXEDOT-1: 고정연장근무 — 미사용/접힘 상태는 빈값으로 보내 서버에서 NULL 저장.
    preFixedOtStrTime: preFixedOtOpen.value ? preFixedOtStrTime.value : "",
    preFixedOtEndTime: preFixedOtOpen.value ? preFixedOtEndTime.value : "",
    fixedOtStrTime: fixedOtEnabled.value ? fixedOtStrTime.value : "",
    fixedOtEndTime: fixedOtEnabled.value ? fixedOtEndTime.value : "",

    useYn: useYn.value,
  };

  console.log("Saving schedule info with payload:", payload);

  try {
    const response = await axios.post(
      "/webApi/attd01/update-sch-infos",
      payload
    );
    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.");
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
    proxy.$alert(getMessage(MSG.APPLY_DATE_FUTURE_ONLY));
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

/**
 * PC-09(N6): 소정근로시간(분) 계산 — 구간별 (근무 길이 − 휴게시간)의 합.
 * 오버나이트(종료<=시작)는 자정 넘김으로 보고 1440을 더한다(validateWorkTime 과 동일 규칙).
 * 시각 파싱 불가 시 null(경고 판정 생략 — validateWorkTime 이 이미 형식을 걸러준 뒤 호출됨).
 */
const calcStdWorkMinutes = () => {
  const toMinutes = (v) => {
    if (!v || typeof v !== "string") return null;
    const s = String(v).trim().replace(/\D/g, "");
    if (s.length < 4) return null;
    const h = parseInt(s.slice(0, 2), 10);
    const m = parseInt(s.slice(2, 4), 10);
    if (h === 24 && m === 0) return 24 * 60;
    if (h < 0 || h > 23 || m < 0 || m > 59) return null;
    return h * 60 + m;
  };
  const span = (start, end) => (end > start ? end - start : end + 1440 - start);

  const fstStart = toMinutes(fstSchStrTime.value);
  const fstEnd = toMinutes(fstSchEndTime.value);
  if (fstStart == null || fstEnd == null || fstStart === fstEnd) return null;
  const fstBrk = parseInt(String(fstSchBrkMin.value || "0"), 10) || 0;
  let total = Math.max(0, span(fstStart, fstEnd) - fstBrk);

  if (schType.value === "02") {
    const secStart = toMinutes(secSchStrTime.value);
    const secEnd = toMinutes(secSchEndTime.value);
    if (secStart == null || secEnd == null || secStart === secEnd) return null;
    const secBrk = parseInt(String(secSchBrkMin.value || "0"), 10) || 0;
    total += Math.max(0, span(secStart, secEnd) - secBrk);
  }
  return total;
};

/**
 * 근무시간 검증: 형식, 구간 겹침, 휴게시간 범위.
 * 시작 >= 종료 인 경우는 오류가 아니라 "오버나이트(자정 넘김) 후보"로 분류한다.
 * 반환: { valid, message, fstOvernight, secOvernight }
 *  - fstOvernight/secOvernight: 해당 구간이 오버나이트 후보이면 true (저장 전 사용자 컨펌 대상)
 */
const validateWorkTime = () => {
  const toMinutes = (v) => {
    if (!v || typeof v !== "string") return null;
    const s = String(v).trim().replace(/\D/g, "");
    if (s.length < 4) return null;
    const h = parseInt(s.slice(0, 2), 10);
    const m = parseInt(s.slice(2, 4), 10);
    if (h === 24 && m === 0) return 24 * 60; // 24:00 = 1440분
    if (h < 0 || h > 23 || m < 0 || m > 59) return null;
    return h * 60 + m;
  };

  /** 오버나이트 고려 근무 길이(분): 종료<=시작이면 자정을 넘긴 것으로 보고 1440 더함 */
  const workSpan = (start, end) => (end > start ? end - start : end + 1440 - start);
  /** 오버나이트 고려 시각 포함 여부: 휴게시작이 근무 [start, end) 구간(자정 넘김 포함) 안인지 */
  const withinSpan = (start, end, t) => {
    if (end > start) return t >= start && t < end;
    // 오버나이트: [start, 24:00) ∪ [00:00, end)
    return t >= start || t < end;
  };

  const fstStart = toMinutes(fstSchStrTime.value);
  const fstEnd = toMinutes(fstSchEndTime.value);
  if (fstStart == null || fstEnd == null) {
    return { valid: false, message: "구간1 근무시간을 올바르게 입력해주세요." };
  }
  if (fstStart === fstEnd) {
    return {
      valid: false,
      message: "구간1 시작시간과 종료시간이 같을 수 없습니다.",
    };
  }
  const fstOvernight = fstStart > fstEnd;

  let secStart = null;
  let secEnd = null;
  let secOvernight = false;
  if (schType.value === "02") {
    secStart = toMinutes(secSchStrTime.value);
    secEnd = toMinutes(secSchEndTime.value);
    if (secStart == null || secEnd == null) {
      return {
        valid: false,
        message: "구간2 근무시간을 올바르게 입력해주세요.",
      };
    }
    if (secStart === secEnd) {
      return {
        valid: false,
        message: "구간2 시작시간과 종료시간이 같을 수 없습니다.",
      };
    }
    secOvernight = secStart > secEnd;

    // 두 구간 모두 자정을 넘기지 않는 일반 케이스에서만 단순 겹침 검사를 수행한다.
    // 오버나이트가 끼면 구간 경계가 자정을 가로질러 단순 비교로 겹침을 판정할 수 없으므로 생략한다.
    if (!fstOvernight && !secOvernight) {
      if (fstStart < secEnd && secStart < fstEnd) {
        return {
          valid: false,
          message: "구간1과 구간2의 근무시간이 겹치면 안 됩니다.",
        };
      }
    }
  }

  const fstWorkMin = workSpan(fstStart, fstEnd);
  const fstBrk = parseInt(String(fstSchBrkMin.value || "0"), 10) || 0;
  if (fstBrk > fstWorkMin) {
    return {
      valid: false,
      message:
        "구간1 휴게시간은 근무시간(" + fstWorkMin + "분)보다 많을 수 없습니다.",
    };
  }
  // 휴게시간(분)이 입력되면 휴게시간 시작은 필수이며 근무시간 범위 안이어야 한다.
  if (fstBrk > 0) {
    const fstBrkStart = toMinutes(fstBrkStrTime.value);
    if (fstBrkStart == null) {
      return {
        valid: false,
        message: "구간1 휴게시간 시작 시각을 입력해주세요.",
      };
    }
    if (!withinSpan(fstStart, fstEnd, fstBrkStart)) {
      return {
        valid: false,
        message: "구간1 휴게시간 시작 시각은 근무시간 범위 안이어야 합니다.",
      };
    }
    // F-2(갭2): 휴게 시작 + 휴게분(=종료)도 근무시간 범위(오버나이트 포함) 안이어야 한다.
    const fstBrkOffset = (fstBrkStart - fstStart + 1440) % 1440;
    if (fstBrkOffset + fstBrk > fstWorkMin) {
      return {
        valid: false,
        message: "구간1 휴게시간 종료 시각이 근무 종료 시각을 초과합니다.",
      };
    }
  }

  if (schType.value === "02") {
    const secWorkMin = workSpan(secStart, secEnd);
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
    // 휴게시간(분)이 입력되면 휴게시간 시작은 필수이며 근무시간 범위 안이어야 한다.
    if (secBrk > 0) {
      const secBrkStart = toMinutes(secBrkStrTime.value);
      if (secBrkStart == null) {
        return {
          valid: false,
          message: "구간2 휴게시간 시작 시각을 입력해주세요.",
        };
      }
      if (!withinSpan(secStart, secEnd, secBrkStart)) {
        return {
          valid: false,
          message: "구간2 휴게시간 시작 시각은 근무시간 범위 안이어야 합니다.",
        };
      }
      // F-2(갭2): 휴게 시작 + 휴게분(=종료)도 근무시간 범위(오버나이트 포함) 안이어야 한다.
      const secBrkOffset = (secBrkStart - secStart + 1440) % 1440;
      if (secBrkOffset + secBrk > secWorkMin) {
        return {
          valid: false,
          message: "구간2 휴게시간 종료 시각이 근무 종료 시각을 초과합니다.",
        };
      }
    }
  }

  return { valid: true, fstOvernight, secOvernight };
};

/**
 * PRAFTA-FIXEDOT-1: 고정연장근무 검증(plan §1-2 V1~V6 프리체크 — 백엔드 ATTD_400_198 룰과 동일 문구).
 *  - V1 쌍 완결성은 UI 구조상(펼침 시 프리필) 자동 충족되나 방어적으로 검사한다.
 *  - V2 전방: 당일 내(시작<종료) + 종료<=소정 1구간 시작.
 *  - V3/V5 후방: 시작>=소정 마지막 구간 종료, 종료<시작이면 자정 넘김(+1440) — rearOvernight 로 반환해
 *    저장 전 컨펌. 소정 구간이 하나라도 자정을 넘기면(anyWrap) 후방은 일자 프레임의
 *    [마지막 소정 종료, 1구간 시작) 빈 구간 안에서만 시작 가능(qa G1)하고 재차 자정 넘김 불가.
 *  - V4(일반화 — qa G1~G3 봉합): 소정 1·2구간 + 전방·후방 점유를 일자 프레임 [0,1440) 구간으로
 *    전개(자정 넘김은 [시작,24:00)∪[00:00,종료) 분할)해 전 쌍(pairwise) 겹침 검사 — 방향성 검사의 안전망.
 *  - V6 휴게 적법성: 소정+고정연장 합산 근로시간 기준(4h 이상 30분·8h 이상 60분) — 고정연장 있을 때만.
 * 반환: { valid, message, rearOvernight, totalFixedOtMin }
 */
const validateFixedOt = () => {
  const result = {
    valid: true,
    message: "",
    rearOvernight: false,
    totalFixedOtMin: 0,
  };
  const usePre = preFixedOtOpen.value;
  const useRear = fixedOtEnabled.value;
  if (!usePre && !useRear) return result;

  const invalid = (message) => ({ ...result, valid: false, message });

  const toMinutes = (v) => {
    if (!v || typeof v !== "string") return null;
    const s = String(v).trim().replace(/\D/g, "");
    if (s.length < 4) return null;
    const h = parseInt(s.slice(0, 2), 10);
    const m = parseInt(s.slice(2, 4), 10);
    if (h === 24 && m === 0) return 24 * 60;
    if (h < 0 || h > 23 || m < 0 || m > 59) return null;
    return h * 60 + m;
  };
  const span = (start, end) => (end > start ? end - start : end + 1440 - start);

  const fstStart = toMinutes(fstSchStrTime.value);
  const fstEnd = toMinutes(fstSchEndTime.value);
  if (fstStart == null || fstEnd == null || fstStart === fstEnd) {
    return invalid("소정 근무시간이 올바르지 않아 고정연장근무를 설정할 수 없습니다.");
  }
  const twoSeg = schType.value === "02";
  let secStart = null;
  let secEnd = null;
  if (twoSeg) {
    secStart = toMinutes(secSchStrTime.value);
    secEnd = toMinutes(secSchEndTime.value);
    if (secStart == null || secEnd == null || secStart === secEnd) {
      return invalid("소정 근무시간이 올바르지 않아 고정연장근무를 설정할 수 없습니다.");
    }
  }
  // 소정 구간 자정 넘김 여부 — 하나라도 넘기면(anyWrap) 후방은 일자 프레임의
  // [마지막 소정 종료, 1구간 시작) 빈 구간 안에만 허용된다(qa G1 봉합).
  const seg1Wrap = fstEnd < fstStart;
  const seg2Wrap = twoSeg && secEnd < secStart;
  const anyWrap = seg1Wrap || seg2Wrap;
  const lastEnd = twoSeg ? secEnd : fstEnd;

  let preStr = null;
  let preEnd = null;
  let rearStr = null;
  let rearEnd = null;
  let preDur = 0;
  let rearDur = 0;

  if (usePre) {
    preStr = toMinutes(preFixedOtStrTime.value);
    preEnd = toMinutes(preFixedOtEndTime.value);
    if (preStr == null || preEnd == null) {
      return invalid("전방 고정연장근무는 시작·종료 시각을 모두 입력해야 합니다.");
    }
    if (preStr >= preEnd) {
      return invalid(
        "전방 고정연장근무는 당일 내 구간이어야 합니다(시작 시각이 종료 시각보다 빨라야 합니다)."
      );
    }
    // 소정 새벽 잔여 점유와의 겹침은 아래 pairwise 전수 검사가 잡는다(qa G2).
    if (preEnd > fstStart) {
      return invalid(
        "전방 고정연장근무 종료 시각은 소정 근무 시작 시각 이전이어야 합니다."
      );
    }
    preDur = preEnd - preStr;
  }

  if (useRear) {
    rearStr = toMinutes(fixedOtStrTime.value);
    rearEnd = toMinutes(fixedOtEndTime.value);
    if (rearStr == null || rearEnd == null) {
      return invalid("후방 고정연장근무는 시작·종료 시각을 모두 입력해야 합니다.");
    }
    if (rearStr === rearEnd) {
      return invalid("후방 고정연장근무 시작 시각과 종료 시각이 같을 수 없습니다.");
    }
    if (!anyWrap) {
      if (rearStr < lastEnd) {
        return invalid(
          "후방 고정연장근무 시작 시각은 소정 근무 종료 시각 이후여야 합니다."
        );
      }
      result.rearOvernight = rearEnd < rearStr;
    } else {
      // 소정이 자정을 넘기는 타입: 후방은 일자 프레임 빈 구간 [마지막 소정 종료, 1구간 시작)
      // 안에서 시작해야 한다(qa G1 — 2구간 사이·1구간 내부 배치 차단).
      if (rearStr < lastEnd || rearStr >= fstStart) {
        return invalid(
          "후방 고정연장근무 시작 시각은 소정 근무 종료 시각 이후여야 합니다."
        );
      }
      if (rearEnd < rearStr) {
        return invalid(
          "소정 근무가 자정을 넘기는 근무타입에서는 후방 고정연장근무가 다시 자정을 넘길 수 없습니다."
        );
      }
    }
    rearDur = span(rearStr, rearEnd);
  }

  // V4(일반화 — qa G1~G3 봉합): 점유를 일자 프레임 [0,1440) 구간으로 전개해 전 쌍 겹침 검사.
  // 원소: [구간ID, 시작, 종료) — 자정 넘김은 [시작,24:00)∪[00:00,종료) 두 조각으로 분할.
  const occupancies = [];
  const addDayFrameOccupancy = (segId, s, e) => {
    if (e > s) {
      occupancies.push([segId, s, e]);
      return;
    }
    occupancies.push([segId, s, 1440]);
    if (e > 0) occupancies.push([segId, 0, e]);
  };
  addDayFrameOccupancy(0, fstStart, fstEnd);
  if (twoSeg) addDayFrameOccupancy(1, secStart, secEnd);
  if (usePre) addDayFrameOccupancy(2, preStr, preEnd);
  if (useRear) addDayFrameOccupancy(3, rearStr, rearEnd);
  const segLabels = ["소정 1구간", "소정 2구간", "전방 고정연장", "후방 고정연장"];
  for (let i = 0; i < occupancies.length; i++) {
    for (let j = i + 1; j < occupancies.length; j++) {
      const a = occupancies[i];
      const b = occupancies[j];
      // 같은 구간의 분할 조각끼리는 비교하지 않는다.
      if (a[0] === b[0]) continue;
      if (a[1] < b[2] && b[1] < a[2]) {
        return invalid(
          segLabels[a[0]] + " 시간과 " + segLabels[b[0]] + " 시간이 겹칩니다."
        );
      }
    }
  }

  result.totalFixedOtMin = preDur + rearDur;

  // V6: 소정+고정연장 합산 근로 기준 법정 휴게 검증(고정연장 존재 시에만 — 기존 타입 무회귀).
  const fstBrk = parseInt(String(fstSchBrkMin.value || "0"), 10) || 0;
  const secBrk = twoSeg
    ? parseInt(String(secSchBrkMin.value || "0"), 10) || 0
    : 0;
  let workMin = Math.max(0, span(fstStart, fstEnd) - fstBrk);
  if (twoSeg) workMin += Math.max(0, span(secStart, secEnd) - secBrk);
  const totalWorkMin = workMin + preDur + rearDur;
  const requiredBreakMin = totalWorkMin >= 480 ? 60 : totalWorkMin >= 240 ? 30 : 0;
  if (fstBrk + secBrk < requiredBreakMin) {
    return {
      ...result,
      valid: false,
      message:
        "소정+고정연장 합산 근로시간(" +
        totalWorkMin +
        "분) 기준 법정 휴게시간(" +
        requiredBreakMin +
        "분) 이상을 입력해야 합니다.",
    };
  }

  return result;
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
  const minVal = parseInt(raw || "0", 10) || 0;
  if (which === "fst") {
    fstSchBrkMin.value = raw;
    if (minVal < 1) fstBrkStrTime.value = "";
  } else {
    secSchBrkMin.value = raw;
    if (minVal < 1) secBrkStrTime.value = "";
  }
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
/* 휴게시간 시작: 분 select 를 ":" 우측에 바짝 붙여 정렬 */
.break-start-inputs :deep(.time-input-wrap) {
  min-width: 0;
  gap: 0;
}
/* ":" 좌측(시 select 와의 사이)만 약간 띄우고, 우측(분 select)은 밀착 */
.break-start-inputs :deep(.time-sep) {
  margin-left: 0.2rem;
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
.break-end-derived {
  display: inline-flex;
  align-items: center;
  min-width: 5rem;
  height: 2rem;
  padding: 0 0.6rem;
  box-sizing: border-box;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 6px);
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
/* PRAFTA-FIXEDOT-1: 전방(출근 전) 고정연장 접힘 토글 */
.fixed-ot-collapse-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-primary, #16a34a);
}
.modal-content-sch-info {
  max-width: 560px;
  width: min(92vw, 560px);
}
</style>
