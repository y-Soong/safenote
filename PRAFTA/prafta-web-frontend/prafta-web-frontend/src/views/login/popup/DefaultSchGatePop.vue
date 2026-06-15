<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow" ref="modalRef">
        <div class="modal-header">
          <span>기본 근무타입 설정</span>
          <!-- 게이트: 닫기 버튼 없음(강제). 미설정 시 진입 불가. -->
        </div>

        <div class="form-container">
          <p class="leave-section-hint">
            ⓘ 서비스 이용을 위해 기본 근무타입을 먼저 설정해야 합니다.
            설정한 근무타입으로 당해 연말까지 평일 근무계획이 자동 생성됩니다.
          </p>

          <div class="form-row-max">
            <label>기본 근무타입</label>
            <BaseSelect id="gateDefaultSchCd" v-model="defaultSchCd" :disabled="isLoading || isSaving">
              <option :value="''">-</option>
              <option
                v-for="opt in schTypeOptions"
                :key="opt.schCd"
                :value="opt.schCd"
              >
                {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{ fnFmtSchTime(opt.fstSchEndTime) }})
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
import { ref, onMounted, getCurrentInstance } from "vue";
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
