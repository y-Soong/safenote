<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow" ref="modalRef">
        <div class="modal-header">
          <span>기본 근무타입 설정</span>
          <!-- 닫기: 미설정 상태로 로그인 화면 복귀(PhoneAuthPop 패턴). 임시 토큰은 정리. -->
          <button class="icon-button" @click="fnCancel">
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
          <p class="leave-section-hint">
            ⓘ 서비스 이용을 위해 기본 근무타입을 먼저 설정해야 합니다. 설정한
            근무타입으로 당해 연말까지 평일 근무계획이 자동 생성됩니다.
          </p>

          <div class="form-row-max">
            <label>기본 근무타입</label>
            <BaseSelect
              id="gateDefaultSchCd"
              v-model="defaultSchCd"
              :disabled="isLoading || isSaving"
            >
              <option :value="''">-</option>
              <option
                v-for="opt in filteredSchTypeOptions"
                :key="opt.schCd"
                :value="opt.schCd"
              >
                {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{
                  fnFmtSchTime(opt.fstSchEndTime)
                }})
              </option>
            </BaseSelect>
          </div>

          <span class="form-msg" v-show="errorMsg">{{ errorMsg }}</span>
        </div>

        <div class="modal-footer">
          <button
            class="btn btn-primary"
            :disabled="!defaultSchCd || isSaving || isLoading"
            @click="fnSave"
          >
            설정하고 시작하기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  // 로그인 응답 scope=DEFAULT_SCH 임시 JWT (짧은 만료). PhoneAuthPop.phoneAuthToken_p 패턴.
  defaultSchToken_p: { type: String, required: true },
  cmpnyCd_p: { type: String, default: "" },
  // 게이트 통과 후 정식 LoginResponse 를 부모로 전달하는 콜백.
  onSuccess: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const defaultSchCd = ref("");
const schTypeOptions = ref([]);
// 반영 시점은 항상 명일(오늘+1, applyDefaultSchChange 규칙) — 적용일이 명일보다 미래인
//   근무타입은 노출하지 않는다(2026-08-22, 최종 판정은 서버 isValidDefaultSch).
const tomorrowYmd = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
})();
const filteredSchTypeOptions = computed(() =>
  schTypeOptions.value.filter(
    (o) => !o.earliestApplyDate || o.earliestApplyDate <= tomorrowYmd
  )
);
const isLoading = ref(false);
const isSaving = ref(false);
const errorMsg = ref("");

// 'HHmm' → 'HH:mm' 라벨 포맷.
const fnFmtSchTime = (t) => {
  if (!t || t.length < 4) return t || "";
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`;
};

onMounted(async () => {
  // 임시 토큰을 sessionStorage 에 일시 점유 → axios 인터셉터가 자동 부착(PhoneAuthPop 패턴).
  // 성공 시 정식 token 으로 교체된다. 게이트는 강제라 별도 cleanup 경로(취소) 없음.
  if (props.defaultSchToken_p) {
    sessionStorage.setItem("token", props.defaultSchToken_p);
  }
  await fnLoadOptions();
});

const fnCleanupToken = () => {
  // 미설정 취소 시 임시 토큰을 sessionStorage 에서 제거(정식 토큰이 아니므로 잔존 금지).
  sessionStorage.removeItem("token");
};

const fnCancel = async () => {
  const ok = await proxy.$confirm(
    "기본 근무타입을 설정하지 않으면 로그인 화면으로 돌아갑니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  fnCleanupToken();
  emit("close");
};

const fnLoadOptions = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get("/comApi/login/default-sch-options");
    schTypeOptions.value = response.data?.schedules ?? [];
    if (schTypeOptions.value.length === 0) {
      errorMsg.value =
        "선택 가능한 근무타입이 없습니다. 관리자에게 문의해 주세요.";
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(
      err,
      "근무타입 목록 조회 중 오류가 발생했습니다."
    );
  } finally {
    isLoading.value = false;
  }
};

const fnSave = async () => {
  if (!defaultSchCd.value) {
    errorMsg.value = "기본 근무타입을 선택해 주세요.";
    return;
  }
  isSaving.value = true;
  errorMsg.value = "";
  try {
    const response = await axios.post("/comApi/login/set-default-sch", {
      defaultSchCd: defaultSchCd.value,
    });
    if (response.status === 200) {
      // 정식 LoginResponse → 부모가 sessionStorage/userStore 세팅 + 라우팅(token 교체 포함).
      if (props.onSuccess) props.onSuccess(response.data);
      emit("close");
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(
      err,
      "기본 근무타입 설정 중 오류가 발생했습니다."
    );
  } finally {
    isSaving.value = false;
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
.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.8rem;
  border-top: 1px solid var(--color-border);
}
.leave-section-hint {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
}
.form-msg {
  font-size: 0.85rem;
  color: var(--color-danger, #ef4444);
}
</style>
