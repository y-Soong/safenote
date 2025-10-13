<template>
  <div class="min-h-screen flex flex-col bg-white">
    <div class="flex items-center p-4 mt-10">
      <button @click="goBack" class="mr-2 text-gray-600 hover:text-gray-900">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-8 w-8"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M15 19l-7-7 7-7"
          />
        </svg>
      </button>
    </div>

    <div class="flex-1 px-6">
      <h3 class="text-gr-ay-800 font-semibold mb-3 text-2xl">회원가입</h3>

      <div class="mt-5 pr-4 relative">
        <label class="block mb-1 font-medium text-gray-700">
          회사코드
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2 pr-4">
        <input
          id="cmpnyCd"
          ref="cmpnyCdFcs"
          v-model="cmpnyCd"
          placeholder="회사코드"
          :disabled="!cmpnyCdDisabled"
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />

        <button
          class="px-4 py-3 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition whitespace-nowrap"
          @click="cmpnyInfoSrch"
        >
          회사코드 확인
        </button>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          회사명
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          v-model="cmpnyNm"
          placeholder="회사명"
          disabled
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          아이디
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="userId"
          ref="userIdFcs"
          v-model="userId"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          minlength="4"
          maxlength="10"
          placeholder="4 ~ 10자"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          비밀번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2" v-if="!cmpnyCdDisabled">
        <input
          id="userPw"
          ref="userPwFcs"
          v-model="userPw"
          type="password"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          placeholder="6 ~ 15자"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <span class="form-msg">{{ pwMsg }}</span>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          비밀번호 확인
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2" v-if="!cmpnyCdDisabled">
        <input
          id="userPwConfirm"
          ref="userPwConfirmFcs"
          v-model="userPwConfirm"
          type="password"
          @blur="focusKill"
          :disabled="cmpnyCdDisabled"
          placeholder="6 ~ 15자"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
        <span class="form-msg">{{ pwConfirmMsg }}</span>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          이름
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="userNm"
          ref="userNmFcs"
          v-model="userNm"
          placeholder="최대15자리"
          maxlength="15"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          휴대폰 번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2" v-if="!cmpnyCdDisabled">
        <input
          id="mblNo"
          ref="mblNoFcs"
          v-model="mblNo"
          @blur="focusKill"
          :disabled="mblNoDisabled"
          placeholder="최대12자리"
          maxlength="12"
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />

        <button
          ref="smsAuthReqBtnFcs"
          class="px-4 py-3 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition whitespace-nowrap"
          @click="fnSmsAuthReq"
          :disabled="timer > 0"
          v-show="btnAuthChkDisabled"
        >
          {{ timer > 0 ? `${timer}초 후 재요청` : '인증요청' }}
        </button>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          인증번호
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2" v-if="!cmpnyCdDisabled">
        <input
          id="certNo"
          ref="certNoFcs"
          v-model="certNo"
          placeholder="인증번호6자리"
          maxlength="6"
          :disabled="mblNoDisabled"
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />

        <button
          class="px-4 py-3 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition whitespace-nowrap"
          @click="fnSmsAuthChk"
          v-show="btnAuthChkDisabled"
        >
          확인
        </button>
        <span class="form-msg" v-show="!btnAuthChkDisabled">{{ smsAuthChkMsg }}</span>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700"> 이메일 </label>
        <input
          id="email"
          ref="emailFcs"
          v-model="email"
          @blur="focusKill"
          placeholder="이메일"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700"> 성별 </label>
        <select
          id="gender"
          ref="genderFcs"
          v-model="gender"
          class="flex-1 w-full py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        >
          <option
            v-for="opt in (systCodeArr['SYS004'] || []).filter((o) => o.SYST_VAL_D_CD != null)"
            :key="opt.SYST_VAL_D_CD"
            :value="opt.SYST_VAL_D_CD"
          >
            {{ opt.SYST_VAL_D_NM }}
          </option>
        </select>
      </div>

      <div class="mt-5 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          사업장
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
      </div>

      <div class="flex items-center space-x-2" v-if="!cmpnyCdDisabled">
        <input
          v-model="siteNm"
          placeholder="사업장"
          disabled
          class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />

        <button
          id="siteSrchBtn"
          ref="siteSrchBtnFcs"
          class="px-4 py-3 border border-green-700 text-green-700 rounded-md hover:bg-green-50 transition whitespace-nowrap"
          @click="openSiteSearch"
        >
          찾기
        </button>
      </div>

      <div class="mt-5 mb-4 relative" v-if="!cmpnyCdDisabled">
        <label class="block mb-1 font-medium text-gray-700">
          생년월일
          <span class="text-red-500 ml-1 text-base">*</span>
          <!-- 🔹 빨간 별표 추가 -->
        </label>
        <input
          id="birthDt"
          ref="birthDtFcs"
          v-model="birthDt"
          placeholder="YYMMDD"
          minlength="6"
          maxlength="6"
          @blur="focusKill"
          class="w-full px-4 py-3 pr-10 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600"
        />
      </div>
    </div>

    <div class="p-6" v-if="!cmpnyCdDisabled">
      <button
        class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
        @click="fnUserJoin()"
      >
        가입하기
      </button>
    </div>

    <!-- ✅ 공통 SidePanel 컴포넌트 -->
    <SidePanel
      :visible="showSitePanel"
      title="사업장 찾기"
      :showSearch="true"
      :hasResults="siteList.length > 0"
      :isLoading="isLoading"
      :isError="isError"
      :items="siteList"
      keyField="SITE_ID"
      labelField="SITE_NM"
      :multiple="false"
      v-model="selectedSites"
      @close="showSitePanel = false"
      @search="fetchSiteList"
      @confirm="selectSites"
    />
  </div>
</template>

<script setup>
/* eslint-disable */
/* dot */
import { ref, getCurrentInstance, onUnmounted, onMounted, watch } from 'vue'
import axios from "@/api/axios";
import { useRouter } from 'vue-router'
import SidePanel from "@/components/common/SidePanel.vue";

const { proxy } = getCurrentInstance();

/* side panel 변수 */
const showSitePanel = ref(false);
const isLoading = ref(false);
const isError = ref(false);
const siteList = ref([]);
const selectedSites = ref([]); // ✅ v-model로 연결됨

const router = useRouter()
const systCodeArr = ref({});
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
const birthDt = ref("");
const pwMsg = ref("");
const pwConfirmMsg = ref("");
const userIdMsg = ref("");
const smsAuthChkMsg = ref("");
const smsCertNoChk = ref(false);

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
const birthDtFcs = ref(null);

/* disabled 변수 */
const cmpnyCdDisabled = ref(true);
const btnAuthChkDisabled = ref(true);
const siteDisabled = ref(true);
const mblNoDisabled = ref(false);

/* 타이머 변수 */
const timer = ref(0)
let timerInterval = null

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
  if (timerInterval) clearInterval(timerInterval)
})

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == "userId") {
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
  const systCodeList = ["SYS004"];

  try {
    const response = await axios.post("/comApi/baseinfo/getSystinfoList", {
      systCodeList: systCodeList,
    });

    if (response.status === 200) {
      const grouped = {};
      response.data.forEach((item) => {
        const key = item.SYST_VAL_CD;
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
  if(proxy.$util.isEmpty(cmpnyCd.value)) {
    proxy.$alert("회사코드를 입력해주세요.");
    return;
  }

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
    const response = await axios.post("/comApi/login/insertUserInfo", {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
      userPw: userPw.value,
      userNm: userNm.value,
      siteCd: siteCd.value,
      mblNo: mblNo.value,
      email: email.value,
      gender: gender.value,
      birthDt: birthDt.value,
    });
    if (response.status === 200) {
      const alertMsg = "회원가입에 성공했습니다.\n로그인 해주세요.";
      fnAlertMsg(alertMsg, () => {
        router.push({
          path: '/',
          query: {
            userId: userId.value,
          },
        })
      });
    }
  } catch (err) {
    const alertMsg = "회원가입에 실패했습니다.\n관리자에게 문의해주세요.";
    fnAlertMsg(alertMsg);
  }
};

/* User Function */
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

async function cmpnyInfoSrch() {
  await fnGetCmpnyInfo();

  if(proxy.$util.isNotEmpty(cmpnyNm.value)) {
    userIdFcs.value.focus();
  }
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}

const goBack = () => {
  router.back()
}

function openSiteSearch() {
  showSitePanel.value = true;
  fetchSiteList();
}

async function fetchSiteList(keyword) {
  isLoading.value = true;
  isError.value = false;
  try {
    const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
      cmpnyCd: cmpnyCd.value,
      siteNm: keyword,
    });

    if(response.status === 200) {
      siteList.value = response.data;
    }
  } catch (err) {
    isError.value = true;
  } finally {
    isLoading.value = false;
  }
}

function selectSites(selected) {
  siteCd.value = selected.SITE_CD;
  siteNo.value = selected.SITE_NO;
  siteNm.value = selected.SITE_NM;
  showSitePanel.value = false;
}
</script>
