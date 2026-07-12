<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>비밀번호 변경 (필수)</span>
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
          <p class="forced-notice">
            ⓘ 첫 로그인 시 보안을 위해 비밀번호 변경이 필요합니다.<br />
            변경을 완료하지 않으면 메인 화면으로 진입할 수 없습니다.
          </p>

          <div class="form-row-max">
            <label>현재 비밀번호</label>
            <input
              ref="currentPwFcs"
              type="password"
              v-model="currentPw"
              placeholder="현재 비밀번호"
              autocomplete="current-password"
              :disabled="loading"
            />
          </div>

          <div class="form-row-max">
            <label>새 비밀번호</label>
            <input
              type="password"
              v-model="newPw"
              placeholder="6~15자, 숫자/영문/특수 중 2가지 이상"
              autocomplete="new-password"
              :disabled="loading"
            />
          </div>

          <div class="form-row-max">
            <label>비밀번호 확인</label>
            <input
              type="password"
              v-model="newPwConfirm"
              placeholder="새 비밀번호 재입력"
              autocomplete="new-password"
              :disabled="loading"
            />
          </div>
        </div>

        <div class="modal-footer">
          <button
            class="btn btn-secondary"
            :disabled="loading"
            @click="fnCancel"
          >
            취소
          </button>
          <button
            class="btn btn-primary"
            :disabled="loading"
            @click="fnChangePassword"
          >
            변경
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
// PRAFTA-037-F1: 첫 로그인 강제 비밀번호 변경 팝업.
// 로그인 응답 nextStep='PASSWORD_CHANGE' (또는 mustChangePassword=true) 분기에서 LoginView 가 띄움.
// 거부 시 토큰 폐기 + 로그인 화면 복귀 (부모 콜백 onCancel 위임).
// 성공 시 부모 콜백 onSuccess 위임 — 약관 체크 → 메인.

import {
  ref,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  // 변경 성공 시 부모(LoginView) 콜백 — 약관 체크 / 메인 라우팅.
  onSuccess: { type: Function, required: true },
  // 취소 시 부모 콜백 — 토큰 폐기 + 로그아웃 + 로그인 화면 복귀.
  onCancel: { type: Function, required: true },
});

// =========================== Ref ===========================
const modalRef = ref(null);
const currentPw = ref("");
const newPw = ref("");
const newPwConfirm = ref("");
const currentPwFcs = ref(null);
const loading = ref(false);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3,
});

// =========================== Life Cycle ===========================
onMounted(() => {
  if (currentPwFcs.value) currentPwFcs.value.focus();
});

// =========================== Methods ===========================
const fnChangePassword = async () => {
  if (loading.value) return;

  if (!currentPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_CURRENT_PW_REQUIRED));
    return;
  }
  if (!newPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_NEW_PW_REQUIRED));
    return;
  }
  if (newPw.value !== newPwConfirm.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_MISMATCH));
    return;
  }

  loading.value = true;
  try {
    await axios.post("/webApi/user01/update-my-passwd", {
      currentPw: currentPw.value,
      newPw: newPw.value,
    });
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_CHANGED));
    emit("close");
    props.onSuccess();
  } catch (err) {
    // 백엔드 표준 에러 메시지 우선:
    //   USER_400_003 현재 비밀번호 불일치
    //   USER_400_004 PW 정책 위반
    //   USER_400_054 (PRAFTA-037-F1) 동일 PW 거부
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.MY_INFO_PW_CHANGE_FAILED))
    );
  } finally {
    loading.value = false;
  }
};

const fnCancel = async () => {
  if (loading.value) return;
  const ok = await proxy.$confirm(
    "비밀번호 변경을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?"
  );
  if (!ok) return;
  emit("close");
  props.onCancel();
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

.forced-notice {
  background: var(--color-info-bg, #eff6ff);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-info-text, #1d4ed8);
  line-height: 1.5;
  margin: 0;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  padding: 0.75rem 1.2rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
  gap: 0.5rem;
}

.btn {
  flex: 0 0 auto;
  width: auto;
  align-self: center;
  min-height: 36px;
  height: 36px;
  padding: 0 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.btn-primary {
  background: var(--color-primary, #16a34a);
  color: #ffffff;
}
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover, #15803d);
}
.btn-primary:disabled {
  background: rgba(22, 163, 74, 0.35);
  color: rgba(255, 255, 255, 0.85);
  cursor: not-allowed;
}

.btn-secondary {
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
}
.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg, #f9fafb);
}
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
