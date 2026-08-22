<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-normal cover-grant-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header">
          <span>입사일 기준 차액 보전 부여</span>
          <button class="icon-button" type="button" @click="fnClose">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- ============ 바디 ============ -->
        <div class="modal-body cg-pop">
          <div class="cg-target-card">
            <p class="cg-target-name">{{ targetUser.userNm }}</p>
            <p class="cg-target-info">
              {{ targetUser.deptNm || "-" }} · {{ targetUser.userCd }}
            </p>
          </div>

          <div class="cg-summary">
            <span class="cg-summary-label"
              >남은 부족분 (기준일 {{ baseYmdDisplay }})</span
            >
            <strong class="cg-summary-value">{{ remainingShortfall }}일</strong>
          </div>

          <div class="cg-field">
            <label class="cg-label"
              >부여 일수<span class="cg-required">*</span></label
            >
            <input
              v-model.number="grantDays"
              class="cg-input"
              type="number"
              min="0.5"
              step="0.5"
              placeholder="0.0"
            />
            <!-- P2-D2 재작업: 부여 상한 = 오늘 기준 부족분(서버 고정) 안내 — 기준일과 무관 -->
            <p class="cg-hint">
              0.5일 단위 · 부여 상한은 기준일과 무관하게 <strong>오늘 기준 남은 부족분</strong>으로
              서버에서 검증되며, 초과하면 거부됩니다.
            </p>
          </div>

          <div class="cg-field">
            <label class="cg-label"
              >사유<span class="cg-required">*</span></label
            >
            <input
              v-model.trim="reason"
              class="cg-input"
              type="text"
              maxlength="200"
              placeholder="예: 입사일 기준 차액 보전 (2026-08 기준)"
            />
          </div>

          <!-- 소정-05 OFF 차단 사유(서버 응답 안내) — disabled 로 숨기지 않고 클릭 시 서버 메시지 그대로 안내 -->
          <div v-if="blockedNotice" class="cg-blocked-notice">
            {{ blockedNotice }}
          </div>

          <div class="cg-notice">
            <p>
              ⓘ 법정연차(STATUTORY_ANNUAL)로 부여되며, 유효기간은
              <strong>지급일 기준</strong>으로 기산됩니다.
            </p>
            <p>ⓘ 부여분은 법정 집계·사용촉진·퇴직정산 기준선에 자동 포함됩니다.</p>
          </div>
        </div>

        <!-- ============ 푸터 ============ -->
        <div class="modal-footer">
          <!-- 소정-05 OFF 여도 활성 유지 — 클릭 시 서버 차단 사유를 그대로 안내(disabled 금지) -->
          <button
            class="btn btn-primary"
            type="button"
            :disabled="isLoading"
            @click="fnSubmit"
          >
            부여
          </button>
          <button class="btn btn-second" type="button" @click="fnClose">
            취소
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// ================ Props & Emits ================
const props = defineProps({
  // 보전 대상 { userCd, userNm, deptNm }
  targetUser: { type: Object, required: true },
  // 서버 재계산 기준(화면에서 조회한 값 — 서버가 baseYmd 로 다시 계산하므로 참고 표시 전용)
  remainingShortfall: { type: [Number, String], required: true },
  // 조회 기준일 (YYYYMMDD)
  baseYmd: { type: String, required: true },
  // 부여 성공 시 부모(Attd_09_Shortfall) 재조회 신호 콜백
  onGranted: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Refs (Variables) ================
const grantDays = ref(null);
const reason = ref("");
const isLoading = ref(false);
// 소정-05(법정 자동부여 OFF) 등 서버 차단 사유 — 클릭 시 노출(사전 disabled 아님, 무반응 방지)
const blockedNotice = ref("");

// ================ Computed ================
const baseYmdDisplay = computed(() => {
  const s = String(props.baseYmd || "");
  return s.length === 8 ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}` : s;
});

// ================ API Functions ================
// 보전 부여 실행 — 상한/소정-05 차단은 서버가 최종 판정(클라 값 불신)
const fnSubmit = async () => {
  if (isLoading.value) return;
  blockedNotice.value = "";

  if (!fnValidate()) return;

  isLoading.value = true;
  try {
    await axios.post("/webApi/attd09/leave-grant/cover-grant", {
      userCd: props.targetUser.userCd,
      grantDays: grantDays.value,
      reason: reason.value,
      baseYmd: props.baseYmd,
    });
    await proxy.$alert("보전 부여가 완료되었습니다.");
    if (typeof props.onGranted === "function") props.onGranted();
    emit("close");
  } catch (err) {
    // 상한 초과(ATTD_400_210)/소정-05 차단(ATTD_400_211)/입력 불량(ATTD_400_212) 등은
    //   resolveApiErrorMessage 로 서버 메시지를 그대로 안내한다(무반응 금지 — disabled 숨김 아님).
    const msg = resolveApiErrorMessage(err, "보전 부여 중 오류가 발생했습니다.");
    blockedNotice.value = msg;
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// ================ Methods/Functions ================
const fnValidate = () => {
  const days = Number(grantDays.value);
  if (!grantDays.value || Number.isNaN(days) || days <= 0) {
    proxy.$alert("부여 일수는 0보다 커야 합니다.");
    return false;
  }
  // 0.5일 단위(정수 또는 .5) 검증
  if (Math.round(days * 2) !== days * 2) {
    proxy.$alert("부여 일수는 0.5일 단위로 입력해 주세요.");
    return false;
  }
  if (!reason.value) {
    proxy.$alert("사유를 입력해 주세요.");
    return false;
  }
  return true;
};

const fnClose = () => {
  emit("close");
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.cover-grant-modal {
  width: 100%;
  max-width: 480px;
}

.cg-pop {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.cg-target-card {
  padding: 0.625rem 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.cg-target-name {
  margin: 0;
  font-weight: 600;
  font-size: 0.8125rem;
  color: var(--color-text-strong);
}

.cg-target-info {
  margin: 0.0625rem 0 0;
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.cg-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.625rem 0.875rem;
  background: var(--color-warning-bg);
  border-radius: var(--input-radius);
}

.cg-summary-label {
  font-size: 0.75rem;
  color: var(--color-warning-text);
}

.cg-summary-value {
  font-size: 0.9375rem;
  color: var(--color-warning-text);
}

.cg-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.cg-label {
  font-size: 0.75rem;
  color: var(--color-text-strong);
}

.cg-required {
  color: var(--color-danger);
  margin-left: 0.125rem;
}

.cg-input {
  width: 100%;
  height: 2.125rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 0.625rem;
  font-size: 0.8125rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}

.cg-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

.cg-hint {
  margin: 0;
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.cg-blocked-notice {
  padding: 0.5rem 0.75rem;
  background: rgba(239, 68, 68, 0.08);
  border-radius: var(--input-radius);
  font-size: 0.75rem;
  color: var(--color-danger);
}

.cg-notice {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.cg-notice p {
  margin: 0.125rem 0;
}
</style>
