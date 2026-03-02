<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- 🔹 Title -->
        <div class="modal-header">
          <span>회원가입</span>
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
          <div class="form-row-max">
            <label>회사코드</label>
            <input
              class="row-short"
              id="cmpnyCd"
              ref="cmpnyCdFcs"
              v-model="cmpnyCd"
              placeholder="회사코드"
              @blur="focusKill"
              :disabled="!cmpnyCdDisabled"
            />
            <input
              class="row-short"
              v-model="cmpnyNm"
              disabled
              placeholder="회사명"
            />
          </div>

          <div class="form-row-max">
            <label>아이디</label>
            <input
              id="userId"
              ref="userIdFcs"
              v-model="userId"
              @blur="focusKill"
              :disabled="cmpnyCdDisabled"
              minlength="4"
              maxlength="10"
              placeholder="4 ~ 10자"
            />
            <span class="form-msg">{{ userIdMsg }}</span>
          </div>

          <div class="form-row-max">
            <label>비밀번호</label>
            <input
              class="row-short"
              id="userPw"
              ref="userPwFcs"
              v-model="userPw"
              type="password"
              @blur="focusKill"
              :disabled="cmpnyCdDisabled"
              placeholder="6 ~ 15자"
            />
            <span class="form-msg">{{ pwMsg }}</span>
          </div>

          <div class="form-row-max">
            <label>비밀번호 확인</label>
            <input
              class="row-short"
              id="userPwConfirm"
              ref="userPwConfirmFcs"
              v-model="userPwConfirm"
              type="password"
              @blur="focusKill"
              :disabled="cmpnyCdDisabled"
              placeholder="6 ~ 15자"
            />
            <span class="form-msg">{{ pwConfirmMsg }}</span>
          </div>

          <div class="form-row-max">
            <label>이름</label>
            <input
              id="userNm"
              ref="userNmFcs"
              v-model="userNm"
              placeholder="최대15자리"
              maxlength="15"
            />
          </div>

          <div class="form-row-max">
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

          <div class="form-row-max">
            <label>인증번호</label>
            <input
              id="certNo"
              ref="certNoFcs"
              v-model="certNo"
              placeholder="인증번호6자리"
              maxlength="6"
              :disabled="mblNoDisabled"
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

          <div class="form-row-max">
            <label>이메일</label>
            <input
              id="email"
              ref="emailFcs"
              v-model="email"
              @blur="focusKill"
              placeholder="이메일"
            />
          </div>

          <div class="form-row-max">
            <label>성별</label>
            <select id="gender" ref="genderFcs" v-model="gender">
              <option
                v-for="opt in (systCodeArr['SYS004'] || []).filter(
                  (o) => o.systValDCd != null
                )"
                :key="opt.systValDCd"
                :value="opt.systValDCd"
              >
                {{ opt.systValDNm }}
              </option>
            </select>
          </div>

          <div class="form-row-max">
            <label>사업장</label>
            <input v-model="siteNm" placeholder="사업장" :disabled="true" />
            <button
              id="siteSrchBtn"
              ref="siteSrchBtnFcs"
              class="btn btn-primary"
              @click="fnSiteSearchPopOpen"
              v-show="!siteDisabled"
            >
              찾기
            </button>
          </div>

          <div class="form-row-max">
            <label>소속부서</label>
            <input
              v-model="nodeNm"
              placeholder="소속부서"
              :disabled="nodeDisabled"
            />
            <button
              id="nodeSrchBtn"
              ref="nodeSrchBtnFcs"
              class="btn btn-primary"
              @click="fnSiteNodeSearchPopOpen"
              v-show="!nodeDisabled"
            >
              찾기
            </button>
          </div>

          <div class="form-row-max">
            <label>생년월일</label>
            <input
              id="birthDt"
              ref="birthDtFcs"
              v-model="birthDt"
              placeholder="YYMMDD"
              minlength="6"
              maxlength="6"
              @blur="focusKill"
            />
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="emit('reset')">
              초기화
            </button>
            <button class="btn btn-primary" @click="fnUserJoin">저장</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  defineProps,
  defineEmits,
  getCurrentInstance,
  watch,
  onMounted,
  onUnmounted,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";

defineProps({ visible: Boolean });
const emit = defineEmits(["close", "reset"]);

const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();

const systCodeArr = ref({});

/* 회원가입정보 */
const cmpnyCd = ref("");
const cmpnyNm = ref("");
const userId = ref("");
const userPw = ref("");
const userPwConfirm = ref("");
const userNm = ref("");
const mblNo = ref("");
const certNo = ref("");
const email = ref("");
const gender = ref("100");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const birthDt = ref("");
const pwMsg = ref("");
const pwConfirmMsg = ref("");
const userIdMsg = ref("");
const smsAuthChkMsg = ref("");
const smsCertNoChk = ref(false);

/* disabled 변수 */
const btnAuthChkDisabled = ref(true);
const siteDisabled = ref(true);
const nodeDisabled = ref(true);
const mblNoDisabled = ref(false);
const cmpnyCdDisabled = ref(true);

/* focus 변수 */
const cmpnyCdFcs = ref(null);
const userIdFcs = ref(null);
const userPwConfirmFcs = ref(null);
const userPwFcs = ref(null);
const userNmFcs = ref(null);
const mblNoFcs = ref(null);
const smsAuthReqBtnFcs = ref(null);
const certNoFcs = ref(null);
const emailFcs = ref(null);
const genderFcs = ref(null);
const siteSrchBtnFcs = ref(null);
const nodeSrchBtnFcs = ref(null);
const birthDtFcs = ref(null);

/* 타이머 변수 */
const timer = ref(0);
let timerInterval = null;

watch(userPw, (newVal) => {
  if (proxy.$util.validatePasswordRule(newVal)) {
    pwMsg.value = "✅";
  } else {
    pwMsg.value = "❌";
  }
});

watch(userPwConfirm, (newVal) => {
  if (newVal == userPw.value) {
    pwConfirmMsg.value = "✅";
  } else {
    pwConfirmMsg.value = "❌";
  }
});

onMounted(async () => {
  await fnGetSystinfoList();
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
});

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == "cmpnyCd") {
    if (proxy.$util.isNotEmpty(cmpnyCd.value)) {
      cmpnyCdFocusKill();
    }
  } else if (e.target.id == "userId") {
    if (proxy.$util.isNotEmpty(userId.value)) {
      userIdFocusKill();
    }
  } else if (e.target.id == "userPw") {
    if (proxy.$util.isNotEmpty(userPw.value)) {
      userPwFocusKill();
    }
  } else if (e.target.id == "userPwConfirm") {
    if (proxy.$util.isNotEmpty(userPwConfirm.value)) {
      userPwConfirmFocusKill();
    }
  } else if (e.target.id == "mblNo") {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      mblNoFocusKill();
    }
  } else if (e.target.id == "email") {
    if (proxy.$util.isNotEmpty(email.value)) {
      emailFocusKill();
    }
  } else if (e.target.id == "birthDt") {
    if (proxy.$util.isNotEmpty(birthDt.value)) {
      birthDtFocusKill();
    }
  }
}

async function cmpnyCdFocusKill() {
  await fnGetCmpnyInfo();
  userIdFcs.value.focus();
}

function userIdFocusKill() {
  if (userId.value.length < 4 || userId.value.length > 20) {
    fnAlertMsg("아이디는 4 ~ 20자 사이의 값만 입력 가능합니다.");
    userId.value = "";
  } else {
    fnUserIdDupleChk();
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

function userPwConfirmFocusKill() {
  if (userPw.value != userPwConfirm.value) {
    const alertMsg = "비밀번호를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      userPwConfirm.value = "";
      userPwConfirmFcs.value.focus();
    });
  }
}

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

function emailFocusKill() {
  if (proxy.$util.validateEmail(email.value)) {
    genderFcs.value.focus();
  } else {
    const alertMsg = "이메일주소를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      email.value = "";
      emailFcs.value.focus();
    });
  }
}

function birthDtFocusKill() {
  if (
    proxy.$util.isEmpty(birthDt.value) ||
    proxy.$util.isValidbirthDtdate(birthDt.value)
  ) {
    const alertMsg = "생년월일을 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      birthDt.value = "";
      birthDtFcs.value.focus();
    });
  }
}

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS004"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnGetCmpnyInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getCmpnyInfo", {
      cmpnyCd: cmpnyCd.value,
    });
    if (response.status === 200) {
      cmpnyCdDisabled.value = !response.data.USE_YN;
      cmpnyNm.value = response.data.CMPNY_NM;
      siteDisabled.value = !response.data.USE_YN;
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      cmpnyCd.value = "";
      cmpnyCdFcs.value.focus();
    });
  }
};

const fnUserIdDupleChk = async () => {
  if (proxy.$util.isEmpty(userId.value)) {
    const alertMsg = "아이디를 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      userIdFcs.value.focus();
    });
  }

  try {
    const response = await axios.post("/comApi/baseinfo/getUserIdDupleChk", {
      cmpnyCd: cmpnyCd.value,
      userId: proxy.$util.toUpperCase(userId.value),
    });
    if (response.status === 200) {
      if (response.data.UNIQUE_YN == "N") {
        userIdMsg.value = "❌";
        const alertMsg = "이미 사용중인 아이디 입니다.";
        fnAlertMsg(alertMsg, () => {
          userId.value = "";
          userIdFcs.value.focus();
        });
      } else {
        userId.value = proxy.$util.toUpperCase(userId.value);
        userIdMsg.value = "✅";
      }
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      cmpnyCd.value = "";
      cmpnyCdFcs.value.focus();
    });
  }
};

const fnSmsAuthReq = async () => {
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

  try {
    const response = await axios.post("/comApi/baseinfo/getSmsAuthReq", {
      mblNo: mblNo.value,
      dupChkYn: "Y",
    });

    if (response.status === 200) {
      const alertMsg = "인증번호가 발송되었습니다.";
      fnAlertMsg(alertMsg, () => {
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
};

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
      mblNoDisabled.value = true;
      smsAuthChkMsg.value = "✅";
      smsCertNoChk.value = true;

      const alertMsg = "인증번호가 확인되었습니다.";
      fnAlertMsg(alertMsg, () => {
        emailFcs.value.focus();
      });
    }
  } catch (err) {
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
};

const fnUserJoin = async () => {
  if (!fnUserInfoValidationChk()) {
    return;
  }

  try {
    const response = await axios.post("/comApi/login/insert-user-info", {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
      userPw: userPw.value,
      userNm: userNm.value,
      siteCd: siteCd.value,
      nodeCd: nodeCd.value,
      mblNo: mblNo.value,
      email: email.value,
      gender: gender.value,
      birthDt: birthDt.value,
    });
    if (response.status === 200) {
      const alertMsg = "회원가입에 성공했습니다.\n로그인 해주세요.";
      fnAlertMsg(alertMsg, () => {
        emit("close");
      });
    }
  } catch (err) {
    const alertMsg = "회원가입에 실패했습니다.\n관리자에게 문의해주세요.";
    fnAlertMsg(alertMsg);
  }
};

/* user function */
function fnUserInfoValidationChk() {
  let alertMsg = "";
  let retVal = true;

  if (proxy.$util.isEmpty(cmpnyCd.value)) {
    alertMsg = "회사코드를 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      cmpnyCdFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(userId.value)) {
    alertMsg = "아이디를 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      userIdFcs.value.focus();
    });
    retVal = false;
  } else if (
    proxy.$util.isEmpty(userPw.value) ||
    proxy.$util.isEmpty(userPwConfirm.value)
  ) {
    alertMsg = "비밀번호를 확인해주세요.";

    fnAlertMsg(alertMsg, () => {
      if (proxy.$util.isEmpty(userPw.value)) {
        userPwFcs.value.focus();
      } else if (proxy.$util.isEmpty(userPwConfirm.value)) {
        userPwConfirmFcs.value.focus();
      }
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(userNm.value)) {
    alertMsg = "이름을 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      userNmFcs.value.focus();
    });
    retVal = false;
  } else if (!smsCertNoChk.value) {
    alertMsg = "휴대폰 번호를 인증해주세요.";

    fnAlertMsg(alertMsg, () => {
      certNoFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(siteCd.value)) {
    alertMsg = "사업장을 선택해주세요.";

    fnAlertMsg(alertMsg, () => {
      siteSrchBtnFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(birthDt.value)) {
    alertMsg = "생년월일을 인증해주세요.";

    fnAlertMsg(alertMsg, () => {
      birthDtFcs.value.focus();
    });
    retVal = false;
  }

  return retVal;
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}

function fnSiteSearchPopOpen() {
  openPop(SiteSearchPop, {
    cmpnyCd_p: cmpnyCd.value,
    onSelect: onSiteSelected,
  });
}

const fnSiteNodeSearchPopOpen = () => {
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: cmpnyCd.value,
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: onSiteNodeSelected,
  });
};

function onSiteSelected(siteCdVal, siteNoVal, siteNmVal) {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeSrchBtnFcs.value.focus();
}

const onSiteNodeSelected = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal;
  nodeNm.value = nodeNmVal;
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 500px;
  margin: 0 auto;
}
</style>
