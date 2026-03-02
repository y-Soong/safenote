<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title  v-if="visible" -->
        <div class="modal-header" @mousedown="startDrag">
          <span>아이디/비밀번호 찾기</span>
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

        <!-- 🔹 Form -->
        <div class="form-container">
          <div class="form-row-short">
            <label>이름</label>
            <input
              id="userNm"
              v-model="userNm"
              placeholder="이름"
              :disabled="mblNoDisabled"
            />
          </div>

          <div class="form-row-short">
            <label>휴대폰 번호</label>
            <input
              id="mblNo"
              ref="mblNoFcs"
              v-model="mblNo"
              @blur="focusKill"
              :disabled="mblNoDisabled"
              placeholder="최대12자리"
              maxlength="12"
            />
            <button
              ref="smsAuthReqBtnFcs"
              class="btn btn-primary"
              @click="fnSmsAuthReq"
              :disabled="timer > 0"
              v-show="btnAuthChkDisabled"
            >
              {{ timer > 0 ? `${timer}초 후 재요청` : "인증요청" }}
            </button>
          </div>

          <div class="form-row-short">
            <label>인증번호</label>
            <input
              id="certNo"
              ref="certNoFcs"
              v-model="certNo"
              placeholder="인증번호6자리"
              maxlength="6"
              :disabled="certNoDisabled"
            />
            <button
              class="btn btn-primary"
              @click="fnSmsAuthChk"
              v-show="btnAuthChkDisabled"
            >
              확인
            </button>
            <span class="form-msg" v-show="!btnAuthChkDisabled">{{
              smsAuthChkMsg
            }}</span>
          </div>

          <div class="form-row-short">
            <label>계정 아이디</label>
            <div>
              <input id="userId" v-model="userId" @blur="focusKill" disabled />
            </div>
          </div>

          <div class="form-row-short" v-show="userPwResetShow">
            <label>비밀번호</label>
            <input
              id="userPw"
              ref="userPwFcs"
              v-model="userPw"
              type="password"
              @blur="focusKill"
              placeholder="6 ~ 15자"
            />
            <span class="form-msg">{{ pwMsg }}</span>
          </div>

          <div class="form-row-short" v-show="userPwResetShow">
            <label>비밀번호 확인</label>
            <input
              id="userPwConfirm"
              ref="userPwConfirmFcs"
              v-model="userPwConfirm"
              type="password"
              @blur="focusKill"
              placeholder="6 ~ 15자"
            />
            <span class="form-msg">{{ pwConfirmMsg }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnUserIdSet">로그인</button>
            <button class="btn btn-primary" @click="fnUserPwReset">
              {{ userPwResetBtnNm }}
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
  watch,
  defineProps,
  defineEmits,
  onUnmounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const emit = defineEmits(["close"]);
const modalRef = ref(null);

const props = defineProps({
  fnUserIdSet: Function,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

/* 회원가입정보 */
const cmpnyCd = ref("");
const userNm = ref("");
const userId = ref("");
const userIdTmp = ref("");
const mblNo = ref("");
const mblNoFcs = ref(null);
const certNo = ref("");
const certNoFcs = ref(null);
const smsAuthReqBtnFcs = ref(null);
const smsAuthChkMsg = ref("");
const btnAuthChkDisabled = ref(true);
const mblNoDisabled = ref(false);
const certNoDisabled = ref(false);
const smsCertNoChk = ref(false);
const userPwResetShow = ref(false);
const userPwResetBtnNm = ref("비밀번호 초기화");
const pwConfirmMsg = ref("");
const userPw = ref("");
const userPwFcs = ref("");
const userPwConfirm = ref("");
const userPwConfirmFcs = ref("");

/* 타이머 변수 */
const timer = ref(0);
let timerInterval = null;

const { proxy } = getCurrentInstance();

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
});

watch(userPwConfirm, (newVal) => {
  if (newVal == userPw.value) {
    pwConfirmMsg.value = "✅";
  } else {
    pwConfirmMsg.value = "❌";
  }
});

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == "mblNo") {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      mblNoFocusKill();
    }
  } else if (e.target.id == "userPw") {
    if (proxy.$util.isNotEmpty(userPw.value)) {
      userPwFocusKill();
    }
  } else if (e.target.id == "userPwConfirm") {
    if (proxy.$util.isNotEmpty(userPwConfirm.value)) {
      userPwConfirmFocusKill();
    }
  }
}

// API 호출
const fnSmsAuthReq = async () => {
  if(proxy.$util.isEmpty(userNm.value)) {
    const alertMsg = "이름을 먼저 입력해주세요.";
    fnAlertMsg(alertMsg, () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  if (
    proxy.$util.isEmpty(mblNo.value) ||
    !proxy.$util.validatePhoneNumber(mblNo.value)
  ) {
    const alertMsg = "휴대폰 번호를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  fnUserIdSrch();
};

const fnUserIdSrch = async() => {
  if (proxy.$util.isEmpty(userNm.value)) {
    const alertMsg = "사용자 이름을 입력해주세요.";
    fnAlertMsg(alertMsg, () => {
      certNo.value = "";
      certNoFcs.value.focus();
    });
    return;
  } else if (proxy.$util.isEmpty(mblNo.value)) {
    const alertMsg = "휴대폰번호를 입력해주세요.";
    fnAlertMsg(alertMsg, () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/getUserIdInfo", {
      userNm: userNm.value,
      mblNo: mblNo.value,
    });
    if (response.status === 200) {
      userIdTmp.value = response.data.USER_ID;
      cmpnyCd.value = response.data.CMPNY_CD;

      fnSmsAuthReqApi();
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
};

const fnSmsAuthReqApi = async() => {
    try {
    const response = await axios.post("/comApi/baseinfo/getSmsAuthReq", {
      mblNo: mblNo.value,
      dupChkYn: "N"
    });

    if (response.status === 200) {
      const alertMsg = "인증번호가 발송되었습니다.";
      fnAlertMsg(alertMsg, () => {
        mblNoDisabled.value = true;
        certNoFcs.value.focus();
      });

      // ✅ 타이머 시작
      timer.value = 60;
      timerInterval = setInterval(() => {
        timer.value--;
        if (timer.value <= 0) {
          clearInterval(timerInterval);
        }
      }, 1000);
    }
  } catch (err) {
    // const alertMsg = "인증번호 발송에 실패했습니다.\n관리자에게 문의해주세요.";
    if (err.response?.data?.message) {
      mblNo.value = "";
      fnAlertMsg(err.response.data.message);
    }
  }
}

const fnSmsAuthChk = async () => {
  if (proxy.$util.isEmpty(certNo.value)) {
    const alertMsg = "인증번호를 입력해주세요.";
    fnAlertMsg(alertMsg, () => {
      certNo.value = "";
      certNoFcs.value.focus();
    });
    return;
  } else if (proxy.$util.isEmpty(mblNo.value)) {
    const alertMsg = "휴대폰번호를 입력해주세요.";
    fnAlertMsg(alertMsg, () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/getSmsAuthChk", {
      mblNo: mblNo.value,
      certNo: certNo.value,
    });
    if (response.status === 200) {
      btnAuthChkDisabled.value = false;
      certNoDisabled.value = true;
      smsAuthChkMsg.value = "✅";
      smsCertNoChk.value = true;

      userId.value = userIdTmp.value;

      const alertMsg = "인증번호가 확인되었습니다.";
      fnAlertMsg(alertMsg);
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
};

const fnUserPwUpdate = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/updateUserPw", {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
      userPw: userPwConfirm.value,
    });
    if (response.status === 200) {
      const alertMsg = "비밀번호가 재성정 되었습니다.";
      fnAlertMsg(alertMsg);

      props.fnUserIdSet(userId.value);
      emit('close');
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
}

/* user function */
function mblNoFocusKill() {
  if (proxy.$util.validatePhoneNumber(mblNo.value)) {
    mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value);
    smsAuthReqBtnFcs.value.focus();
  } else {
    const alertMsg = "휴대폰 번호를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
  }
}

function fnUserIdSet() {
  if(proxy.$util.isEmpty(userId.value)) {
    const alertMsg = "계정 아이디 정보를 입력해주세요.";
    fnAlertMsg(alertMsg);
  } else {
    props.fnUserIdSet(userId.value);
    emit('close');
  }
}

function fnUserPwReset() {
  if(
    !userPwResetShow.value && 
    smsCertNoChk.value && 
    proxy.$util.isNotEmpty(userId.value)
  ) {
    userPwResetBtnNm.value = "저장";
    userPwResetShow.value = true;
  } else {
    if (
      proxy.$util.isEmpty(userPw.value) ||
      proxy.$util.isEmpty(userPwConfirm.value)
    ) {
      const alertMsg = "비밀번호를 확인해주세요.";

      fnAlertMsg(alertMsg, () => {
        if (proxy.$util.isEmpty(userPw.value)) {
          userPwFcs.value.focus();
        } else if (proxy.$util.isEmpty(userPwConfirm.value)) {
          userPwConfirmFcs.value.focus();
        }
      });
      return;
    }
    fnConfirmMsg("비밀번호를 재설정 하시겠습니까 ?", async () => {
      fnUserPwUpdate();
    });
  }
}

function userPwFocusKill() {
  if (userPw.value != userPwConfirm.value) {
    userPwConfirm.value = "";
  }

  if (!proxy.$util.validatePasswordRule(userPw.value)) {
    const alertMsg =
      "비밀번호는 숫자, 영문자, 특수문자 중 2가지\n이상을 포함하여 6~15자로 작성해 주세요.";
    fnAlertMsg(alertMsg, () => {
      userPw.value = "";
      userPwFcs.value.focus();
    });
  }
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}

function userPwConfirmFocusKill() {
  if (userPw.value != userPwConfirm.value) {
    const alertMsg = "비밀번호를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      userPwConfirm.value = "";
      userPwConfirmFcs.value.focus();
    });
  }
}

async function fnConfirmMsg(message, afterConfirmCallback) {
  const result = await proxy.$confirm(message);
  if (result && afterConfirmCallback) {
    afterConfirmCallback(); // ✅ 확인 눌렀을 때만 실행
  }
}
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 1000px;
  margin: 0 auto;
}
</style>
