<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>휴대폰 본인인증</span>
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
          <p class="auth-notice">
            ⓘ 관리자가 생성한 계정은 첫 로그인 시 본인인증이 필요합니다.<br />
            등록된 휴대폰번호로 인증을 진행해 주세요.
          </p>

          <div class="form-row-max">
            <label>휴대폰 번호</label>
            <input
              id="mblNo"
              ref="mblNoFcs"
              v-model="mblNo"
              @blur="focusKill"
              :disabled="mblNoDisabled"
              placeholder="최대11자리"
              maxlength="13"
            />
            <button
              class="btn btn-primary auth-req-btn"
              @click="fnSmsAuthReq"
              :disabled="timer > 0 || !phoneLoaded"
              v-show="!verified"
            >
              {{ timer > 0 ? `${timer}초 후 재요청` : "인증요청" }}
            </button>
          </div>

          <div class="form-row-max">
            <label>인증번호</label>
            <input
              id="certNo"
              ref="certNoFcs"
              v-model="certNo"
              placeholder="인증번호6자리"
              maxlength="6"
              :disabled="!authReqSent"
            />
            <button
              class="btn btn-primary"
              @click="fnVerify"
              :disabled="!authReqSent"
              v-show="!verified"
            >
              확인
            </button>
            <span class="form-msg" v-show="verified">✅</span>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-secondary" @click="fnCancel">취소</button>
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
  defineProps,
  defineEmits,
  onMounted,
  onUnmounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  // 로그인 응답으로 받은 임시 scope=PHONE_AUTH JWT (10분 유효).
  phoneAuthToken_p: { type: String, required: true },
  // 회사코드 (SMS 발송 요청에 필요).
  cmpnyCd_p: { type: String, required: true },
  // 인증 성공 시 정식 LoginResponse 를 부모로 전달하는 콜백.
  onSuccess: { type: Function, required: true },
});

// =========================== Ref ===========================
const modalRef = ref(null);
const mblNo = ref("");
const mblNoFcs = ref(null);
const mblNoDisabled = ref(false);
const certNo = ref("");
const certNoFcs = ref(null);
const authReqSent = ref(false);
const verified = ref(false);
// 등록 휴대폰 로드 완료 플래그(자동기입). 로드 전에는 인증요청 버튼 비활성.
const phoneLoaded = ref(false);
const timer = ref(0);
let timerInterval = null;

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  // 인터셉터가 임시 토큰을 자동 부착하도록 sessionStorage 에 일시적으로 보관.
  // 인증 성공 시 정식 token 으로 교체되고, 취소/실패 시 fnCleanupToken 에서 제거된다.
  if (props.phoneAuthToken_p) {
    sessionStorage.setItem("token", props.phoneAuthToken_p);
  }

  // 관리자가 등록한 휴대폰을 불러와 자동기입 + 잠금(수정 불가).
  //   사용자가 임의 번호로 바꿔 SMS 인증 후 계정 휴대폰을 탈취하는 것을 방지한다.
  //   조회 실패/미등록 시에는 잠그지 않고 수동 입력 폴백(사용자가 진행 가능하도록).
  try {
    const res = await axios.get("/comApi/login/phone-auth-phone");
    const raw = res.data?.mblNo || "";
    if (raw) {
      mblNo.value = proxy.$util.formatPhoneNumber(raw) ?? raw;
      mblNoDisabled.value = true; // 입력 잠금(읽기 전용)
    }
  } catch (err) {
    // 폴백: 잠그지 않음(수동 입력 허용).
  } finally {
    phoneLoaded.value = true;
  }
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
});

// =========================== Methods ===========================
const fnCleanupToken = () => {
  // 인증 실패/취소 시 임시 토큰을 sessionStorage 에서 제거(정식 토큰이 아니므로 잔존 금지).
  sessionStorage.removeItem("token");
};

const fnSmsAuthReq = async () => {
  if (
    proxy.$util.isEmpty(mblNo.value) ||
    !proxy.$util.validatePhoneNumber(mblNo.value)
  ) {
    await proxy.$alert("휴대폰번호를 올바르게 입력해 주세요.");
    mblNo.value = "";
    if (mblNoFcs.value) mblNoFcs.value.focus();
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/sms-auth-sends", {
      cmpnyCd: props.cmpnyCd_p,
      mblNo: mblNo.value.replace(/-/g, ""),
    });

    if (response.status === 200) {
      authReqSent.value = true;
      await proxy.$alert("인증번호가 발송되었습니다.\n6자리 인증번호를 입력해 주세요.");
      if (certNoFcs.value) certNoFcs.value.focus();

      timer.value = 60;
      timerInterval = setInterval(() => {
        timer.value--;
        if (timer.value <= 0) clearInterval(timerInterval);
      }, 1000);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "인증번호 발송 중 오류가 발생했습니다.")
    );
  }
};

const fnVerify = async () => {
  if (proxy.$util.isEmpty(certNo.value)) {
    await proxy.$alert("인증번호를 입력해 주세요.");
    if (certNoFcs.value) certNoFcs.value.focus();
    return;
  }

  try {
    const response = await axios.post("/comApi/login/verify-phone-auth", {
      mblNo: mblNo.value.replace(/-/g, ""),
      certNo: certNo.value,
    });

    if (response.status === 200) {
      verified.value = true;
      mblNoDisabled.value = true;
      // 부모에게 정식 LoginResponse 전달 → 부모가 sessionStorage/userStore 세팅 + 라우팅.
      // sessionStorage.token 은 부모 콜백이 정식 token 으로 교체한다.
      props.onSuccess(response.data);
      emit("close");
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "인증에 실패했습니다.")
    );
  }
};

const fnCancel = async () => {
  const ok = await proxy.$confirm("본인인증을 취소하면 로그인 화면으로 돌아갑니다. 계속하시겠습니까?");
  if (!ok) return;
  fnCleanupToken();
  emit("close");
};

const focusKill = (e) => {
  if (e.target.id === "mblNo") {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      if (proxy.$util.validatePhoneNumber(mblNo.value)) {
        mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value);
      }
    }
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

.auth-notice {
  background: var(--color-info-bg, #eff6ff);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-info-text, #1d4ed8);
  line-height: 1.5;
  margin: 0;
}

.form-msg {
  font-size: 1rem;
  color: var(--color-success, #16a34a);
}

.btn-secondary {
  background: #ffffff;
  border: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
}
.btn-secondary:hover {
  background: var(--color-bg, #f9fafb);
}

/* 인증요청/확인 버튼은 회원가입 팝업(JoinUserPop)과 동일하게 글자 폭 기준(전역 .btn 규격).
   "인증요청"↔"60초 후 재요청" 텍스트 전환으로 버튼 폭이 변해도
   입력이 남는 공간에서 줄어들어 행이 모달 폭을 넘치지 않는다. */
.form-row-max .btn {
  flex: 0 0 auto;
}
.form-row-max input {
  min-width: 0;
}
</style>
