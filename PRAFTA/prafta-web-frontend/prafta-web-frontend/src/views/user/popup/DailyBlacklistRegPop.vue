<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>블랙리스트 등록</span>
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
          <p class="reg-guide">
            등록한 휴대폰번호로는 일일계정 회원가입 및 자동 재활성(로그인)이
            차단됩니다.
          </p>

          <div class="form-row-max">
            <label>휴대폰 번호</label>
            <input
              id="mblNo"
              ref="mblNoFcs"
              v-model.trim="mblNo"
              placeholder="휴대폰번호 (예: 010-1234-5678)"
              maxlength="13"
            />
            <span class="form-msg">{{ mblNoMsg }}</span>
          </div>

          <div class="form-row-max form-row-top">
            <label>사유</label>
            <textarea
              id="reason"
              ref="reasonFcs"
              v-model.trim="reason"
              class="reason-input"
              placeholder="등록 사유 (필수, 최대 200자)"
              maxlength="200"
              rows="3"
            ></textarea>
          </div>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(저장, primary), 오른쪽=이탈(취소, ghost), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSave">저장</button>
            <button class="btn btn-second" @click="$emit('close')">
              취소
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, getCurrentInstance, nextTick } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// onSaved: 저장 성공 시 부모(User_06)가 목록을 재조회하도록 전달받는 콜백.
const props = defineProps({
  onSaved: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const mblNo = ref("");
const reason = ref("");
const mblNoMsg = ref("");

const mblNoFcs = ref(null);
const reasonFcs = ref(null);

// =========================== Methods ===========================
// TODO(developer):
//   1) 입력 검증: 전화번호 형식(proxy.$util.validatePhoneNumber), 사유 필수.
//      실패 시 proxy.$alert 후 해당 필드 focus.
//   2) POST /webApi/user06/blacklist
//      body: { mblNo, reason }  ← cmpnyCd 는 서버 JWT 클레임 사용(절대 전달 금지).
//      평문 전화번호는 서버에서 정규화→HMAC/ENC/LAST4 변환. 프론트는 평문 그대로 전송만.
//   3) 성공: proxy.$alert("등록되었습니다.") → props.onSaved?.() → emit("close")
//   4) 실패: resolveApiErrorMessage(err, ...) 로 메시지 노출
//      (중복/이미 등록된 번호 등 서버 ErrorCode 메시지 그대로 표시)
// 저장 중복 클릭 방지 플래그.
const saving = ref(false);

const fnSave = async () => {
  mblNoMsg.value = "";

  // 1) 입력 검증 — 휴대폰 형식, 사유 필수.
  if (!mblNo.value) {
    mblNoMsg.value = "휴대폰번호를 입력해주세요.";
    await nextTick();
    mblNoFcs.value?.focus();
    return;
  }
  if (!proxy.$util.validatePhoneNumber(mblNo.value)) {
    mblNoMsg.value = "휴대폰번호 형식이 올바르지 않습니다.";
    await nextTick();
    mblNoFcs.value?.focus();
    return;
  }
  if (!reason.value) {
    await proxy.$alert("등록 사유를 입력해주세요.");
    await nextTick();
    reasonFcs.value?.focus();
    return;
  }

  if (saving.value) return;
  saving.value = true;

  // 2) 등록 — 평문 전화번호는 서버에서 정규화→HMAC/ENC/LAST4 변환(cmpnyCd 는 서버 JWT).
  try {
    const response = await axios.post("/webApi/user06/blacklist", {
      mblNo: mblNo.value,
      reason: reason.value,
    });

    if (response.status === 200) {
      await proxy.$alert("등록되었습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "등록 중 오류가 발생했습니다.");
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

.reg-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}

.form-row-top {
  align-items: flex-start;
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}

.reason-input {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font: inherit;
  color: var(--color-text, #374151);
}
.reason-input:focus {
  outline: var(--focus-ring-width, 3px) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset, 2px);
}
</style>
