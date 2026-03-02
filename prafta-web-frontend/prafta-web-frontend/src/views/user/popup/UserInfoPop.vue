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
          <span>사용자정보</span>
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
            <label>사용자</label>
            <input
              class="row-short"
              v-model="userId"
              disabled
              placeholder="아이디"
            />
            <input
              class="row-short"
              v-model="userNm"
              disabled
              placeholder="이름"
            />
          </div>

          <div class="form-row-max">
            <label>권한</label>
            <BaseSelect
              id="authCd"
              v-model="authCd"
              :disabled="cboAuthCdDisabled"
            >
              <option
                v-for="opt in (baseInfoArr['COM005'] || []).filter(
                  (o) => o.baimValDCd != null
                )"
                :key="opt.baimValCd"
                :value="opt.baimValDCd"
              >
                {{ opt.baimValDNm }}
              </option>
            </BaseSelect>
          </div>

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
              ref="smsAuthReqBtnFcs"
              class="btn btn-primary"
              @click="fnSmsAuthReq"
              :disabled="timer > 0"
              v-show="btnAuthChkDisabledVisible"
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
              v-show="btnAuthChkDisabledVisible"
            >
              확인
            </button>
            <span class="form-msg" v-show="!btnAuthChkDisabledVisible">{{
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
            <BaseSelect id="gender" v-model="gender">
              <option
                v-for="opt in (systCodeArr['SYS004'] || []).filter(
                  (o) => o.systValDCd != null
                )"
                :key="opt.systValDCd"
                :value="opt.systValDCd"
              >
                {{ opt.systValDNm }}
              </option>
            </BaseSelect>
          </div>

          <div class="form-row-max">
            <label>사용여부</label>
            <BaseSelect id="useYn" v-model="useYn">
              <option
                v-for="opt in (systCodeArr['SYS003'] || []).filter(
                  (o) => o.systValDCd != null
                )"
                :key="opt.systValDCd"
                :value="opt.systValDCd"
              >
                {{ opt.systValDNm }}
              </option>
            </BaseSelect>
          </div>

          <div class="form-row-max">
            <label>사업장</label>
            <input v-model="siteNm" placeholder="사업장" />
            <button
              id="siteSrchBtn"
              ref="siteSrchBtnFcs"
              class="btn btn-primary"
              @click="fnSiteSearchPopOpen"
            >
              찾기
            </button>
          </div>

          <div class="form-row-max">
            <label>소속부서</label>
            <input v-model="nodeNm" placeholder="소속부서" />
            <button
              id="nodeSrchBtn"
              ref="nodeSrchBtnFcs"
              class="btn btn-primary"
              @click="fnSiteNodeSearchPopOpen"
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
            <button
              class="btn btn-primary"
              v-show="btnUserWithdrawalVisible"
              @click="fnUserWithdrawal"
            >
              계정삭제
            </button>
            <button class="btn btn-primary" @click="fnUserPwResetConf">
              비밀번호 초기화
            </button>
            <button class="btn btn-primary" @click="fnUserJoin">저장</button>
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
} from 'vue';
import { useModal } from '@/utils/useModal';
import { useCenteredDraggable } from '@/composables/useCenteredDraggable';
import axios from '@/api/axios';
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue';
import SiteNodeSearchPop from '@/components/popup/SiteNodeSearchPop.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';

// =========================== Define ===========================
const emit = defineEmits(['close']);
const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String,
  userId_p: String,
  callmethod_p: String,
  onSearch: Function,
});

// =========================== Ref ===========================
const modalRef = ref(null);
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const cmpnyCd = ref('');
const userId = ref('');
const userNm = ref('');
const mblNo = ref('');
const oriMblNo = ref('');
const mblNoFcs = ref('');
const certNo = ref('');
const certNoFcs = ref('');
const smsAuthReqBtnFcs = ref('');
const email = ref('');
const emailFcs = ref('');
const gender = ref('');
const siteCd = ref('');
const siteNo = ref('');
const siteNm = ref('');
const useYn = ref('');
const siteSrchBtnFcs = ref(false);
const nodeSrchBtnFcs = ref(false);
const birthDt = ref('');
const birthDtFcs = ref('');
const authCd = ref('');
const nodeCd = ref('');
const nodeNm = ref('');
const mblNoDisabled = ref(false);
const btnAuthChkDisabledVisible = ref(true);
const smsCertNoChk = ref(false);
const cboAuthCdDisabled = ref(true);
const smsAuthChkMsg = ref('');
const btnUserWithdrawalVisible = ref(false);
const timer = ref(0);
let timerInterval = null;

// =========================== Data ===========================
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  cmpnyCd.value = props.cmpnyCd_p;

  if (props.userId_p) {
    userId.value = props.userId_p;
    fnGetUserInfo(userId.value);
  }
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
});

// =========================== Methods ===========================
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/base-info-list', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        baseCodeList: ['COM005'],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (item.baimValDCd == null) return;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseInfoArr.value = grouped;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", { params: {
      systCodeList: ['SYS003', 'SYS004'],
    }});

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

const fnGetUserInfo = async (userId) => {
  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userId: userId,
      },
    });
    if (response.status === 200) {

      if (response.data.userInfoList.length == 1) {
        userNm.value = response.data.userInfoList[0].userNm;
        authCd.value = response.data.userInfoList[0].authCd;
        siteCd.value = response.data.userInfoList[0].siteCd;
        nodeCd.value = response.data.userInfoList[0].nodeCd;
        nodeNm.value = response.data.userInfoList[0].nodeNm;
        siteNm.value = response.data.userInfoList[0].siteNm;
        useYn.value = response.data.userInfoList[0].useYn;
        mblNo.value = proxy.$util.formatPhoneNumber(response.data.userInfoList[0].mblNo);
        oriMblNo.value = response.data.userInfoList[0].mblNo;
        email.value = response.data.userInfoList[0].email;
        gender.value = response.data.userInfoList[0].gender;
        birthDt.value = response.data.userInfoList[0].birthDt;

        if (
          sessionStorage.getItem("gv_authCd") == "999" ||
          sessionStorage.getItem("gv_authCd") > response.data.userInfoList[0].authCd
        ) {
          cboAuthCdDisabled.value = false;
          btnUserWithdrawalVisible.value = true;
        }
      }
    }
  } catch (err) {
    alert(err.response.data.message);
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
    const alertMsg = "인증번호 발송에 실패했습니다.\n관리자에게 문의해주세요.";
    fnAlertMsg(alertMsg);
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
      btnAuthChkDisabledVisible.value = false;
      mblNoDisabled.value = true;
      smsAuthChkMsg.value = "✅";
      smsCertNoChk.value = true;

      const alertMsg = "인증번호가 확인되었습니다.";
      fnAlertMsg(alertMsg, () => {
        emailFcs.value.focus();
      });
    }
  } catch (err) {
    console.log(err);
    fnAlertMsg(err.response.data.message, () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
};

const fnUserJoin = async () => {
  if (!fnUserInfoValidationChk()) {
    return;
  }

  const result = await proxy.$confirm("저장하시겠습니까 ?");

  console.log(props.callmethod_p);
  if (result) {
    const apiUrl =
      props.callmethod_p == "S"
        ? "/webApi/user01/update-user-infos"
        : "/webApi/user01/insertUserInfo";

    try {
      const response = await axios.post(apiUrl, [
        {
          cmpnyCd: cmpnyCd.value,
          userId: userId.value,
          userNm: userNm.value,
          siteCd: siteCd.value,
          nodeCd: nodeCd.value,
          useYn: useYn.value,
          authCd: authCd.value,
          mblNo: mblNo.value,
          email: email.value,
          gender: gender.value,
          birthDt: birthDt.value,
        },
      ]);
      if (response.status === 200) {
        const alertMsg = "처리됐습니다.";
        fnAlertMsg(alertMsg, () => {
          emit("close");
          props.onSearch();
        });
      }
    } catch (err) {
      const alertMsg =
        "요청처리에 실패했습니다.\n관리자에게 문의해주세요.";
      fnAlertMsg(alertMsg);
    }
  }
};

const fnUserPwReset = async () => {
  try {
    const response = await axios.post("/webApi/user01/update-user-passwd", {
      cmpnyCd: cmpnyCd.value,
      userId: userId.value,
    });
    if (response.status === 200) {
      const alertMsg = "처리됐습니다.";
      fnAlertMsg(alertMsg, () => {});
    }
  } catch (err) {
    const alertMsg =
      "요청처리에 실패했습니다.\n관리자에게 문의해주세요.";
    fnAlertMsg(alertMsg);
  }
};

const focusKill = (e) => {
  if (e.target.id == "mblNo") {
    if (proxy.$util.isNotEmpty(mblNo.value)) {
      mblNoFocusKill();
    }
  }
};

const mblNoFocusKill = () => {
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
};

const fnUserWithdrawal = () => {
  fnConfirmMsg(
    "사용자의 계정을 삭제합니다.\n삭제된 계정은 복구할 수 없습니다.",
    () => {
      console.log("ASD");
    }
  );
};

const fnUserPwResetConf = () => {
  fnConfirmMsg("사용자의 비밀번호를 초기화합니다.", () => {
    fnUserPwReset();
  });
};

const fnUserInfoValidationChk = () => {
  let retVal = true;

  if (
    mblNo.value.replaceAll("-", "") != oriMblNo.value.replaceAll("-", "") &&
    !smsCertNoChk.value
  ) {
    fnAlertMsg("휴대폰 번호를 인증해주세요.", () => {
      certNoFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(birthDt.value)) {
    fnAlertMsg("생년월일을 인증해주세요.", () => {
      birthDtFcs.value.focus();
    });
    retVal = false;
  }

  return retVal;
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: cmpnyCd.value,
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) { 
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }

  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    onSelect: onSiteNodeSelected,
  });
};

const onSiteNodeSelected = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal;
  nodeNm.value = nodeNmVal;
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};

const fnConfirmMsg = async (message, afterConfirmCallback) => {
  const result = await proxy.$confirm(message);
  if (result && afterConfirmCallback) {
    afterConfirmCallback();
  }
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
