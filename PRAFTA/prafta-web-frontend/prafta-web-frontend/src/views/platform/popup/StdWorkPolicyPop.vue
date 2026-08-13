<!--
  StdWorkPolicyPop.vue — 회사 통상근로시간 기준값 변경 팝업 (플랫폼 운영자 전용)
  - 호출: Platform_03 행 [통상근로시간] 셀. props 로 대상 회사/현재 기준값 수신.
  - 저장: POST /platformApi/customer/std-work-policy { cmpnyCd, policyMode, weekStdMinutes }
  - 구조: AiTokenQuotaPop 전례를 그대로 미러링(요약 박스 + 모드 라디오 + 안내문).
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>통상근로시간 변경</span>
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
          <!-- 대상 회사 / 현재 적용값 요약 -->
          <div class="policy-summary">
            <div class="policy-summary-row">
              <span class="policy-summary-label">회사</span>
              <span class="policy-summary-value"
                >{{ cmpnyNm }} ({{ cmpnyCd }})</span
              >
            </div>
            <div class="policy-summary-row">
              <span class="policy-summary-label">현재 적용</span>
              <span class="policy-summary-value">{{ currentLabel }}</span>
            </div>
          </div>

          <!-- 기준값 방식 라디오 (DEFAULT / DIRECT) -->
          <div
            class="policy-mode-group"
            role="radiogroup"
            aria-label="통상근로시간 방식"
          >
            <label class="policy-mode-row">
              <input type="radio" value="DEFAULT" v-model="policyMode" />
              <span class="policy-mode-label">기본값 사용</span>
              <span class="policy-mode-desc">주 40시간 (법정 상한)</span>
            </label>

            <label class="policy-mode-row">
              <input type="radio" value="DIRECT" v-model="policyMode" />
              <span class="policy-mode-label">직접 지정</span>
              <span
                class="policy-input-wrap"
                v-show="policyMode === 'DIRECT'"
              >
                주
                <input
                  ref="hoursFcs"
                  v-model.number="weekHours"
                  type="number"
                  min="0"
                  max="40"
                  step="1"
                  class="policy-num-input"
                  placeholder="40"
                />
                시간
                <input
                  v-model.number="weekMinutes"
                  type="number"
                  min="0"
                  max="59"
                  step="1"
                  class="policy-num-input"
                  placeholder="0"
                />
                분
              </span>
            </label>
            <span class="form-msg" v-show="policyMsg">{{ policyMsg }}</span>
          </div>

          <p class="policy-guide">
            통상근로자(풀타임)의 주 소정근로시간입니다. <b>단시간근로자 판정</b>과
            <b>연차 비례부여</b>의 기준이 되므로 실제 계약과 다르면 연차 부여량이
            틀어집니다. 사업장별로 다르면 고객사 관리자가 사업장 관리에서 따로
            지정할 수 있습니다.<br />
            법정 상한은 주 40시간(근로기준법 제50조)입니다 — 40시간을 넘는 근무는
            연장근로이므로 고정연장근무 근무타입으로 관리합니다.
          </p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button class="btn btn-primary" :disabled="saving" @click="fnSave">
              저장
            </button>
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
  getCurrentInstance,
  nextTick,
  onMounted,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// 대상 회사 정보(Platform_03 행 전달).
//   weekStdMinutes: null = 직접 지정 없음(기본 주 40시간 폴백) / 양수 = 직접 지정된 분.
const props = defineProps({
  cmpnyCd: { type: String, required: true },
  cmpnyNm: { type: String, default: "" },
  weekStdMinutes: { type: Number, default: null },
  onSaved: Function, // 저장 성공 시 부모(Platform_03) 재조회 콜백
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Const ===========================
// 법정 상한(근로기준법 제50조 1주 40시간). 서버 STDWORK_400_007 과 동일 기준의 1차 검증.
const LEGAL_MAX_WEEK_MINUTES = 2400;
// 초단시간 경계(주 15시간). 미만이면 경고 후 확인(저장은 허용 — 서버 규약과 동일).
const MIN_WARN_WEEK_MINUTES = 900;

// =========================== Ref ===========================
const policyMode = ref("DEFAULT"); // 'DEFAULT' | 'DIRECT'
const weekHours = ref(null);
const weekMinutes = ref(0);
const policyMsg = ref("");
const saving = ref(false);
const hoursFcs = ref(null);

// =========================== Computed ===========================
// 분 → "주 N시간 M분" 라벨.
const fnFmtMinutes = (total) => {
  const m = Number(total);
  if (!Number.isFinite(m) || m <= 0) return "-";
  const h = Math.floor(m / 60);
  const rem = m % 60;
  return rem === 0 ? `주 ${h}시간` : `주 ${h}시간 ${rem}분`;
};

// 현재 적용값 — 미지정이면 폴백값임을 명시한다(직접 지정 40시간과 구분).
const currentLabel = computed(() => {
  if (props.weekStdMinutes == null) {
    return "주 40시간 (미지정 — 기본값)";
  }
  return fnFmtMinutes(props.weekStdMinutes);
});

// 입력값 → 분.
const inputMinutes = computed(() => {
  const h = Number(weekHours.value) || 0;
  const m = Number(weekMinutes.value) || 0;
  return Math.round(h * 60 + m);
});

// =========================== Methods ===========================
// 프리필: 직접 지정 행이 있으면 DIRECT + 시간/분 분해, 없으면 DEFAULT.
onMounted(() => {
  if (props.weekStdMinutes != null && props.weekStdMinutes > 0) {
    policyMode.value = "DIRECT";
    weekHours.value = Math.floor(props.weekStdMinutes / 60);
    weekMinutes.value = props.weekStdMinutes % 60;
  } else {
    policyMode.value = "DEFAULT";
  }
});

// 저장: 검증(클라 1차 — 서버 STDWORK_400_007/400_004 와 동일 기준) → 확인 → POST → 부모 재조회
const fnSave = async () => {
  policyMsg.value = "";

  if (policyMode.value === "DIRECT") {
    if (inputMinutes.value <= 0) {
      policyMsg.value =
        "통상근로시간을 입력해 주세요. 기본값(주 40시간)을 쓰려면 '기본값 사용'을 선택하세요.";
      await nextTick();
      hoursFcs.value?.focus();
      return;
    }
    if (inputMinutes.value > LEGAL_MAX_WEEK_MINUTES) {
      policyMsg.value =
        "주 40시간을 초과할 수 없습니다(근로기준법 제50조). 초과분은 고정연장근무로 관리하세요.";
      await nextTick();
      hoursFcs.value?.focus();
      return;
    }
    // 초단시간 경계 — 차단이 아니라 확인(서버도 경고 로그만 남기고 저장을 허용한다).
    if (inputMinutes.value < MIN_WARN_WEEK_MINUTES) {
      const ok = await proxy.$confirm(
        "통상근로시간이 주 15시간 미만입니다.\n회사 통상 기준값으로는 비정상적인 값입니다. 그대로 저장할까요?"
      );
      if (!ok) return;
    }
  }

  // 기준값 변경은 단시간 판정·연차 비례부여 분모를 바꾸므로 파급을 명시하고 확인받는다.
  const nextLabel =
    policyMode.value === "DIRECT"
      ? fnFmtMinutes(inputMinutes.value)
      : "주 40시간(기본값)";
  const ok = await proxy.$confirm(
    `'${props.cmpnyNm}' 의 통상근로시간을 ${nextLabel} 로 변경합니다.\n` +
      "단시간근로자 판정과 연차 비례부여의 기준이 바뀝니다. 진행할까요?"
  );
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post(
      "/platformApi/customer/std-work-policy",
      {
        cmpnyCd: props.cmpnyCd,
        policyMode: policyMode.value,
        weekStdMinutes:
          policyMode.value === "DIRECT" ? inputMinutes.value : null,
      }
    );

    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
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
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 460px;
  margin: 0 auto;
}

/* 대상 회사/현재값 요약 박스 (AiTokenQuotaPop .quota-summary 전례) */
.policy-summary {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-surface-muted, #f9fafb);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
}
.policy-summary-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.85rem;
}
.policy-summary-label {
  flex: 0 0 5.5rem;
  color: var(--color-text-muted);
}
.policy-summary-value {
  color: var(--color-text);
  font-weight: 600;
}

/* 기준값 방식 라디오 그룹 */
.policy-mode-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.policy-mode-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text);
}
.policy-mode-label {
  font-weight: 600;
}
.policy-mode-desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

/* 시간/분 입력 */
.policy-input-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.policy-num-input {
  width: 4.5rem;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  padding: 0.35rem 0.6rem;
  font: inherit;
  color: var(--color-text);
  text-align: right;
}
.policy-num-input:focus {
  outline: var(--focus-ring-width, 3px) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset, 2px);
}

/* 안내문 (AiTokenQuotaPop .quota-guide 전례) */
.policy-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}
</style>
