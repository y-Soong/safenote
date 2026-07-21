<!--
  AiTokenQuotaPop.vue — 회사별 AI 토큰 한도 변경 팝업 (플랫폼 운영자 전용)
  - 호출: Platform_03 행 [변경] 버튼. props 로 대상 회사/현재 한도/당월 사용량 수신.
  - 저장: POST /platformApi/customer/token-quota { cmpnyCd, quotaMode, limitMan }
  - 골격: planner 작성(template + scoped style). script 로직: developer 작성(plan T5).
-->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>AI 토큰 한도 변경</span>
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
          <!-- 대상 회사 / 당월 사용량 요약 -->
          <div class="quota-summary">
            <div class="quota-summary-row">
              <span class="quota-summary-label">회사</span>
              <span class="quota-summary-value"
                >{{ cmpnyNm }} ({{ cmpnyCd }})</span
              >
            </div>
            <div class="quota-summary-row">
              <span class="quota-summary-label">당월 사용량</span>
              <span class="quota-summary-value">{{ usedManLabel }} 토큰</span>
            </div>
          </div>

          <!-- 한도 방식 라디오 (LIMIT / UNLIMITED / BLOCK) -->
          <div
            class="quota-mode-group"
            role="radiogroup"
            aria-label="한도 방식"
          >
            <label class="quota-mode-row">
              <input type="radio" value="LIMIT" v-model="quotaMode" />
              <span class="quota-mode-label">한도 설정</span>
              <span
                class="quota-limit-input-wrap"
                v-show="quotaMode === 'LIMIT'"
              >
                <input
                  ref="limitManFcs"
                  v-model.number="limitMan"
                  type="number"
                  min="1"
                  max="1000000"
                  step="1"
                  class="quota-limit-input"
                  placeholder="80"
                />
                <span class="quota-limit-unit">만 토큰</span>
              </span>
            </label>
            <span class="form-msg" v-show="limitMsg">{{ limitMsg }}</span>

            <label class="quota-mode-row">
              <input type="radio" value="UNLIMITED" v-model="quotaMode" />
              <span class="quota-mode-label">무제한</span>
              <span class="quota-mode-desc">자사/특별 고객용</span>
            </label>

            <label class="quota-mode-row">
              <input type="radio" value="BLOCK" v-model="quotaMode" />
              <span class="quota-mode-label">완전 차단</span>
              <span class="quota-mode-desc">해당 회사 AI 기능 잠금</span>
            </label>
          </div>

          <p class="quota-guide">
            기본값은 80만 토큰(월)입니다. 한도는 입력+출력 토큰의 합 기준이며,
            매월 1일 0시에 사용량이 초기화됩니다.
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
import { ref, computed, defineProps, defineEmits, getCurrentInstance, nextTick, onMounted } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// 대상 회사 정보(Platform_03 행 전달). tokenLimit: -1 무제한 / 0 차단 / 양수 원시 토큰 수.
const props = defineProps({
  cmpnyCd: { type: String, required: true },
  cmpnyNm: { type: String, default: "" },
  tokenLimit: { type: Number, default: 800000 },
  usedTokens: { type: Number, default: 0 },
  onSaved: Function, // 저장 성공 시 부모(Platform_03) 재조회 콜백
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const quotaMode = ref("LIMIT");   // 'LIMIT' | 'UNLIMITED' | 'BLOCK'
const limitMan = ref(null);       // 만 단위 정수(1~1,000,000). 저장 시 서버가 *10000
const limitMsg = ref("");
const saving = ref(false);
const limitManFcs = ref(null);

// 당월 사용량 만 단위 라벨 (소수 1자리 반올림 — 표시 전용. 예: 234567 → "23.5만", ".0" 은 생략)
const usedManLabel = computed(() => {
  const man = (props.usedTokens ?? 0) / 10000;
  return (Math.round(man * 10) / 10).toFixed(1).replace(/\.0$/, "") + "만";
});

// =========================== Methods ===========================
// 프리필: 현재 한도로 모드/값 결정(-1 무제한 / 0 차단 / 양수 한도설정+만 단위 정수)
onMounted(() => {
  if (props.tokenLimit === -1) {
    quotaMode.value = "UNLIMITED";
  } else if (props.tokenLimit === 0) {
    quotaMode.value = "BLOCK";
  } else {
    quotaMode.value = "LIMIT";
    limitMan.value = Math.floor(props.tokenLimit / 10000);
  }
});

// 저장: 검증(클라 1차 — 서버 PLATFORM_400_014 와 동일 기준) → 차단 confirm → POST → 부모 재조회
const fnSave = async () => {
  limitMsg.value = "";

  // 1) LIMIT 모드 검증: 정수 1~1,000,000 (만 단위)
  if (quotaMode.value === "LIMIT") {
    if (
      !Number.isInteger(limitMan.value) ||
      limitMan.value < 1 ||
      limitMan.value > 1000000
    ) {
      limitMsg.value = "1 이상 1,000,000 이하의 정수(만 단위)를 입력해 주세요.";
      await nextTick();
      limitManFcs.value?.focus();
      return;
    }
  }

  // 2) 완전 차단은 파급이 크므로 확인 confirm
  if (quotaMode.value === "BLOCK") {
    const ok = await proxy.$confirm(
      "해당 회사의 모든 AI 기능이 차단됩니다. 진행할까요?"
    );
    if (!ok) return;
  }

  if (saving.value) return;
  saving.value = true;

  // 3) 저장 — 만 단위 정수 전송(원시 토큰 변환은 서버가 수행)
  try {
    const response = await axios.post("/platformApi/customer/token-quota", {
      cmpnyCd: props.cmpnyCd,
      quotaMode: quotaMode.value,
      limitMan: quotaMode.value === "LIMIT" ? limitMan.value : null,
    });

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

/* 대상 회사/사용량 요약 박스 */
.quota-summary {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-surface-muted, #f9fafb);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius, 10px);
}
.quota-summary-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.85rem;
}
.quota-summary-label {
  flex: 0 0 5.5rem;
  color: var(--color-text-muted);
}
.quota-summary-value {
  color: var(--color-text);
  font-weight: 600;
}

/* 한도 방식 라디오 그룹 */
.quota-mode-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.quota-mode-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text);
}
.quota-mode-label {
  font-weight: 600;
}
.quota-mode-desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

/* 만 단위 입력 */
.quota-limit-input-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
.quota-limit-input {
  width: 7rem;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  padding: 0.35rem 0.6rem;
  font: inherit;
  color: var(--color-text);
  text-align: right;
}
.quota-limit-input:focus {
  outline: var(--focus-ring-width, 3px) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset, 2px);
}
.quota-limit-unit {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

/* 안내문 (DailyBlacklistRegPop .reg-guide 전례) */
.quota-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}
</style>
