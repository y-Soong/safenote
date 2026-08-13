<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>{{ isCorrect ? "소정근로시간 정정" : "소정근로시간 변경 등록" }}</span>
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

        <!-- Form -->
        <div class="form-container">
          <p class="reg-guide" v-if="isCorrect">
            오입력 정정입니다. 적용 시작일은 이력의 키라서 변경할 수 없습니다.<br />
            계약이 실제로 바뀐 경우에는 정정이 아니라 <strong>변경 등록</strong>으로 새 이력을
            쌓아 주세요(과거 값이 보존되어야 소급 재계산이 가능합니다).
          </p>
          <p class="reg-guide" v-else>
            변경 등록 시 직전 이력은 시작일 전날로 자동 마감됩니다.<br />
            단축 사유(육아기·임신기·가족돌봄)는 적용 종료일이 필수이며, 종료 다음 날부터
            직전 계약으로 복귀하는 이력이 자동 생성됩니다.
          </p>

          <div class="form-row-max">
            <label>대상</label>
            <input class="row-readonly" :value="props.userNm_p" readonly />
          </div>

          <div class="form-row-max">
            <label>적용 시작일 *</label>
            <CalendarSrch
              v-model="applyStrDateInput"
              class="date-field"
              :disabled="isCorrect"
            />
          </div>

          <div class="form-row-max">
            <label>적용 종료일</label>
            <CalendarSrch v-model="applyEndDateInput" class="date-field" />
            <button class="btn btn-sm btn-second" @click="applyEndDateInput = ''">
              무기한
            </button>
          </div>

          <div class="form-row-max">
            <label>주 소정근로 *</label>
            <input
              class="row-short"
              type="number"
              min="0"
              max="168"
              v-model.number="weekHours"
              placeholder="시간"
            />
            <span class="std-work-suffix">시간</span>
            <input
              class="row-short"
              type="number"
              min="0"
              max="59"
              v-model.number="weekMinutes"
              placeholder="분"
            />
            <span class="std-work-suffix">분</span>
          </div>

          <div class="form-row-max">
            <label>사유 *</label>
            <BaseSelect id="reasonCd" v-model="reasonCd">
              <option
                v-for="opt in reasonOptions"
                :key="opt.reasonCd"
                :value="opt.reasonCd"
              >
                {{ opt.reasonNm }}
              </option>
            </BaseSelect>
          </div>

          <div class="form-row-max form-row-top">
            <label>사유 상세</label>
            <textarea
              v-model.trim="reasonDetail"
              class="detail-input"
              placeholder="선택 입력 (최대 500자)"
              maxlength="500"
              rows="3"
            ></textarea>
          </div>

          <p class="std-work-warning" v-for="(warn, idx) in warnings" :key="idx">
            ⚠ {{ warn }}
          </p>

          <p class="reg-hint" v-if="cmpnyWeekStdMinutes">
            ⓘ 회사 통상근로자 기준은 {{ fnFmtMinutes(cmpnyWeekStdMinutes) }}입니다. 이보다
            짧으면 단시간근로자로 판정됩니다.
          </p>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(primary), 오른쪽=이탈(ghost), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" :disabled="saving" @click="fnSave">
              {{ isCorrect ? "정정 저장" : "등록" }}
            </button>
            <button class="btn btn-second" @click="$emit('close')">취소</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// [props] mode_p: 'REGISTER'(변경 등록) / 'CORRECT'(오입력 정정)
//         row_p : 정정 대상 이력 행(CORRECT 일 때만). onSaved: 부모 갱신 콜백.
const props = defineProps({
  mode_p: { type: String, default: "REGISTER" },
  userCd_p: String,
  userNm_p: String,
  row_p: Object,
  onSaved: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const applyStrDateInput = ref(""); // YYYY-MM-DD
const applyEndDateInput = ref("");
const weekHours = ref(null);
const weekMinutes = ref(0);
const reasonCd = ref("");
const reasonDetail = ref("");

const reasonOptions = ref([]);
const cmpnyWeekStdMinutes = ref(null);
// 경고 임계값은 서버가 내려준다(화면 하드코딩 시 정책 변경에 어긋난다).
const minWarnWeekMinutes = ref(0);
const childcareMinWeekMinutes = ref(0);
const childcareMaxWeekMinutes = ref(0);

const saving = ref(false);

// =========================== Computed ===========================
const isCorrect = computed(() => props.mode_p === "CORRECT");

const inputMinutes = computed(() => {
  const h = Number(weekHours.value) || 0;
  const m = Number(weekMinutes.value) || 0;
  return h * 60 + m;
});

const selectedReason = computed(() =>
  reasonOptions.value.find((o) => o.reasonCd === reasonCd.value)
);

// 단축 사유 여부는 서버가 내려준 플래그로 판정한다(코드 나열 하드코딩 금지).
const isReducedReason = computed(() => !!selectedReason.value?.reduced);

// 저장 전 실시간 경고(서버 판정과 동일 규약 — 경고일 뿐 저장은 허용된다).
const warnings = computed(() => {
  const list = [];
  const total = inputMinutes.value;
  if (total > 0 && minWarnWeekMinutes.value > 0 && total < minWarnWeekMinutes.value) {
    list.push(
      "주 소정근로시간이 15시간 미만입니다. 초단시간근로자는 연차·주휴 적용 대상에서 제외될 수 있으니 계약 내용을 확인해 주세요."
    );
  }
  if (
    reasonCd.value === "CHILDCARE" &&
    total > 0 &&
    childcareMaxWeekMinutes.value > 0 &&
    (total < childcareMinWeekMinutes.value || total > childcareMaxWeekMinutes.value)
  ) {
    list.push(
      "육아기 근로시간 단축은 주 15시간 이상 35시간 이하가 원칙입니다. 입력값이 범위를 벗어났습니다."
    );
  }
  if (isReducedReason.value && !applyEndDateInput.value) {
    list.push("단축 사유는 적용 종료일이 필요합니다. 종료일을 선택해 주세요.");
  }
  return list;
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnLoadOptions();

  if (isCorrect.value && props.row_p) {
    applyStrDateInput.value = fnToInputDate(props.row_p.applyStrDate);
    applyEndDateInput.value = fnToInputDate(props.row_p.applyEndDate);
    const minutes = Number(props.row_p.weekStdMinutes) || 0;
    weekHours.value = Math.floor(minutes / 60);
    weekMinutes.value = minutes % 60;
    reasonCd.value = props.row_p.reasonCd || "";
    reasonDetail.value = props.row_p.reasonDetail || "";
    return;
  }

  // 등록 기본값: 적용 시작일 = 오늘. 주 소정 = 회사 통상 기준값(관리자가 조정).
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  applyStrDateInput.value = `${yyyy}-${mm}-${dd}`;

  const base = Number(cmpnyWeekStdMinutes.value) || 0;
  weekHours.value = base > 0 ? Math.floor(base / 60) : null;
  weekMinutes.value = base > 0 ? base % 60 : 0;
});

// =========================== Methods ===========================
// 사유 옵션(SYS083 전 사유 — 단축 포함) + 회사 기준값 + 경고 임계값.
const fnLoadOptions = async () => {
  try {
    const response = await axios.get("/webApi/user10/std-work-reason-options");
    if (response.status === 200) {
      const data = response.data || {};
      reasonOptions.value = data.reasonOptions || [];
      cmpnyWeekStdMinutes.value = data.cmpnyWeekStdMinutes ?? null;
      minWarnWeekMinutes.value = data.minWarnWeekMinutes ?? 0;
      childcareMinWeekMinutes.value = data.childcareMinWeekMinutes ?? 0;
      childcareMaxWeekMinutes.value = data.childcareMaxWeekMinutes ?? 0;
      if (!reasonCd.value && reasonOptions.value.length > 0) {
        reasonCd.value = reasonOptions.value[0].reasonCd;
      }
    }
  } catch (err) {
    reasonOptions.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "사유 코드 조회 중 오류가 발생했습니다.")
    );
  }
};

// 저장 — 등록/정정 모두 서버가 검증(겹침·단축 종료일·값 범위)과 부수효과(복귀 행 생성/이동)를 처리한다.
const fnSave = async () => {
  if (proxy.$util.isEmpty(applyStrDateInput.value)) {
    await proxy.$alert("적용 시작일을 선택해 주세요.");
    return;
  }
  if (inputMinutes.value <= 0) {
    await proxy.$alert("주 소정근로시간을 입력해 주세요.");
    return;
  }
  if (proxy.$util.isEmpty(reasonCd.value)) {
    await proxy.$alert("사유를 선택해 주세요.");
    return;
  }
  if (isReducedReason.value && proxy.$util.isEmpty(applyEndDateInput.value)) {
    await proxy.$alert("단축 사유는 적용 종료일을 반드시 입력해야 합니다.");
    return;
  }
  if (
    applyEndDateInput.value &&
    applyEndDateInput.value < applyStrDateInput.value
  ) {
    await proxy.$alert("적용 종료일은 적용 시작일보다 빠를 수 없습니다.");
    return;
  }

  if (saving.value) return;
  saving.value = true;

  const url = isCorrect.value
    ? "/webApi/user10/std-work-correct"
    : "/webApi/user10/std-work-register";

  try {
    const response = await axios.post(url, {
      userCd: props.userCd_p,
      applyStrDate: fnToYmd(applyStrDateInput.value),
      applyEndDate: fnToYmd(applyEndDateInput.value),
      weekStdMinutes: inputMinutes.value,
      reasonCd: reasonCd.value,
      reasonDetail: proxy.$util.isEmpty(reasonDetail.value) ? null : reasonDetail.value,
    });

    if (response.status === 200) {
      await proxy.$alert(fnBuildResultMessage(response.data || {}));
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};

// 서버 결과의 부수효과(자동 마감 · 복귀 행 생성/이동 · 경고)를 사용자에게 그대로 알린다.
//   이 안내가 없으면 관리자는 자기 조작이 다른 이력 행을 만들거나 옮겼다는 사실을 모른다.
const fnBuildResultMessage = (data) => {
  const lines = [isCorrect.value ? "정정되었습니다." : "등록되었습니다."];

  if (data.closedPrevEndDate) {
    lines.push(`· 직전 이력이 ${fnFmtYmd(data.closedPrevEndDate)}로 마감되었습니다.`);
  }
  if (data.restoreStrDate) {
    lines.push(
      `· 단축 종료 다음 날(${fnFmtYmd(data.restoreStrDate)})부터 이전 소정근로시간(${fnFmtMinutes(
        data.restoreWeekStdMinutes
      )})으로 복귀하는 이력이 자동 생성되었습니다.`
    );
  }
  if (data.movedRestoreStrDate) {
    lines.push(
      `· 뒤따르는 이력의 시작일이 ${fnFmtYmd(data.movedRestoreFromStrDate)} → ${fnFmtYmd(
        data.movedRestoreStrDate
      )}로 함께 조정되었습니다.`
    );
  }
  (data.warnings || []).forEach((w) => lines.push(`⚠ ${w}`));

  return lines.join("\n");
};

// ── 표기/변환 헬퍼 ────────────────────────────────────────
const fnToYmd = (input) => {
  if (!input) return null;
  return String(input).replace(/-/g, "");
};

const fnToInputDate = (ymd) => {
  if (!ymd || ymd.length !== 8) return "";
  return `${ymd.substring(0, 4)}-${ymd.substring(4, 6)}-${ymd.substring(6, 8)}`;
};

const fnFmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return String(ymd || "");
  return `${ymd.substring(0, 4)}-${ymd.substring(4, 6)}-${ymd.substring(6, 8)}`;
};

const fnFmtMinutes = (minutes) => {
  const m = Number(minutes);
  if (!m || m <= 0) return "-";
  const h = Math.floor(m / 60);
  const rest = m % 60;
  return rest === 0 ? `주 ${h}시간` : `주 ${h}시간 ${rest}분`;
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  /* ★가로 넘침 방지: 팝업(.modal-content-narrow)이 max-width 500px 라 여기서 520px 를 잡으면
     팝업보다 넓어져 가로 스크롤이 생기고 입력이 잘린다. 폭은 팝업이 결정한다. */
  width: 100%;
  box-sizing: border-box;
  /* 팝업 높이 바운딩 — 내용이 길어지면 본문만 스크롤(modal-popup-guide) */
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ★공용 .form-row-max 는 라벨 120px + 입력 flex 1 1 150px 로 입력 1개를 전제한다.
   입력이 2개인 행(주 소정근로 = 시간/분)에서 min-width:0 이 없으면 flex 가 축소되지 못해 넘친다. */
.form-container :deep(.form-row-max) {
  min-width: 0;
}
.form-container :deep(.form-row-max input),
.form-container :deep(.form-row-max select) {
  min-width: 0;
}

.reg-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-surface-muted, #f3f4f6);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}

.reg-hint {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  line-height: 1.5;
}

.form-row-top {
  align-items: flex-start;
}

.std-work-suffix {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

.std-work-warning {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-radius: var(--input-radius, 10px);
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
  font-size: 0.6875rem;
  line-height: 1.5;
}

.detail-input {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font: inherit;
  color: var(--color-text, #374151);
}
.detail-input:focus {
  outline: var(--focus-ring-width, 3px) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset, 2px);
}

/* 날짜 입력 — UserInfoPop 입사일 필드와 동일 처리(이중 테두리 제거) */
.date-field {
  flex: 1;
  padding: 0;
  background: transparent;
  border: none;
}
.date-field :deep(.calendar-input) {
  width: 100%;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.date-field :deep(.calendar-input):focus {
  border-color: var(--color-border-strong, #d1d5db);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}
</style>
