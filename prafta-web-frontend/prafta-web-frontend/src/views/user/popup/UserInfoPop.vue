<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
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
            <BaseSelect id="authCd" v-model="authCd">
              <option
                v-for="opt in (baseInfoArr['COM005'] || []).filter(
                  (o) => o.baimValDCd != null && o.sortIdx >= authLevel
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

          <!-- 탈퇴예정일 (withdrawalDate 값이 있을 때만 표시) -->
          <div class="form-row-max withdrawal-date-row" v-show="withdrawalDate">
            <label>탈퇴예정일</label>
            <CalendarSrch v-model="withdrawalDate" :disabled="true" />
            <button
              class="btn btn-danger"
              :disabled="accountStatus === '03'"
              @click="fnCancelWithdrawal"
            >
              탈퇴취소
            </button>
          </div>
        </div>

        <!-- 탈퇴예정일 입력 다이얼로그 -->
        <Transition name="dialog-fade">
          <div
            v-if="withdrawalDialogVisible"
            class="withdrawal-dialog-overlay"
            @click.self="fnCloseWithdrawalDialog"
          >
            <div class="withdrawal-dialog">
              <div class="withdrawal-dialog-header">
                <span>탈퇴예정일 설정</span>
                <button class="icon-button" @click="fnCloseWithdrawalDialog">
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
              <div class="withdrawal-dialog-body">
                <div class="form-row-max">
                  <label>탈퇴예정일</label>
                  <input
                    class="dialog-date-input"
                    type="date"
                    v-model="newWithdrawalDate"
                    :min="tomorrowDate"
                  />
                </div>
              </div>
              <div class="withdrawal-dialog-footer">
                <div class="btn-group">
                  <button class="btn btn-danger" @click="fnScheduleWithdrawal">
                    확정
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-danger"
              v-show="btnUserWithdrawalVisible && !withdrawalDate"
              @click="fnUserWithdrawal"
            >
              회원탈퇴
            </button>
            <button class="btn btn-primary" @click="fnUserPwResetConf">
              비밀번호 초기화
            </button>
            <button class="btn btn-primary" @click="fnUserInfoSave">
              저장
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
  defineProps,
  defineEmits,
  onMounted,
  onUnmounted,
  getCurrentInstance,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
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
const cmpnyCd = ref("");
const userCd = ref("");
const userId = ref("");
const userNm = ref("");
const mblNo = ref("");
const oriMblNo = ref("");
const mblNoFcs = ref("");
const certNo = ref("");
const certNoFcs = ref("");
const smsAuthReqBtnFcs = ref("");
const email = ref("");
const emailFcs = ref("");
const gender = ref("");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const useYn = ref("");
const birthDt = ref("");
const birthDtFcs = ref("");
const authCd = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const accountStatus = ref("");
const withdrawalDate = ref("");
const mblNoDisabled = ref(false);
const btnAuthChkDisabledVisible = ref(true);
const smsCertNoChk = ref(false);
const cboAuthCdDisabled = ref(true);
const smsAuthChkMsg = ref("");
const btnUserWithdrawalVisible = ref(false);
const withdrawalDialogVisible = ref(false);
const newWithdrawalDate = ref("");
const tomorrowDate = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
})();
const timer = ref(0);
let timerInterval = null;
const authLevel = ref(sessionStorage.getItem('gv_authLevel'));

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
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM005"],
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
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003", "SYS004"],
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

const fnGetUserInfo = async (userId) => {
  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userId: userId,
      },
    });
    if (response.status === 200) {
      if (response.data.userInfoList.length == 1) {

        if(response.data.userInfoList[0].accountStatus == "03") {
          proxy.$alert("탈퇴된 계정은 상세보기를 지원하지 않습니다.");
          emit("close");;
        }

        userCd.value = response.data.userInfoList[0].userCd;
        userNm.value = response.data.userInfoList[0].userNm;
        authCd.value = response.data.userInfoList[0].authCd;
        siteCd.value = response.data.userInfoList[0].siteCd;
        nodeCd.value = response.data.userInfoList[0].nodeCd;
        nodeNm.value = response.data.userInfoList[0].nodeNm;
        siteNm.value = response.data.userInfoList[0].siteNm;
        useYn.value = response.data.userInfoList[0].useYn;
        mblNo.value = proxy.$util.formatPhoneNumber(
          response.data.userInfoList[0].mblNo
        );
        oriMblNo.value = response.data.userInfoList[0].mblNo;
        email.value = response.data.userInfoList[0].email;
        gender.value = response.data.userInfoList[0].gender;
        birthDt.value = response.data.userInfoList[0].birthDt;
        authLevel.value = response.data.userInfoList[0].authLevel;
        accountStatus.value = response.data.userInfoList[0].accountStatus;
        withdrawalDate.value = response.data.userInfoList[0].withdrawalDate;

        if (
          sessionStorage.getItem("gv_authCd") == "system" ||
          sessionStorage.getItem("gv_authLevel") <
            authLevel.value
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
    fnAlertMsg(getMessage(MSG.PHONE_VERIFY), () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/sms-auth-sends", {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
    });

    if (response.status === 200) {
      fnAlertMsg(getMessage(MSG.USER_INFO_SMS_SENT), () => {
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
    const alertMsg = err.response.data.message;
    fnAlertMsg(alertMsg);
  }
};

const fnSmsAuthChk = async () => {
  if (proxy.$util.isEmpty(certNo.value)) {
    fnAlertMsg(getMessage(MSG.USER_INFO_CERT_NO_REQUIRED), () => {
      certNo.value = "";
      certNoFcs.value.focus();
    });
    return;
  } else if (proxy.$util.isEmpty(mblNo.value)) {
    fnAlertMsg(getMessage(MSG.USER_INFO_PHONE_REQUIRED), () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/sms-auth-checks", {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
      certNo: certNo.value,
    });
    if (response.status === 200) {
      btnAuthChkDisabledVisible.value = false;
      mblNoDisabled.value = true;
      smsAuthChkMsg.value = "✅";
      smsCertNoChk.value = true;

      fnAlertMsg(getMessage(MSG.USER_INFO_SMS_VERIFIED), () => {
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

const fnUserInfoSave = async () => {
  if (!fnUserInfoValidationChk()) {
    return;
  }

  const result = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));

  if (result) {
    try {
      const response = await axios.post("/webApi/user01/update-user-infos", [
        {
          cmpnyCd: cmpnyCd.value,
          userCd: userCd.value,
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
        fnAlertMsg(getMessage(MSG.SAVE_SUCCESS), () => {
          emit("close");
          props.onSearch();
        });
      }
    } catch (err) {
      fnAlertMsg(getMessage(MSG.REQUEST_FAILED));
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
      fnAlertMsg(getMessage(MSG.SAVE_SUCCESS), () => {});
    }
  } catch (err) {
      fnAlertMsg(getMessage(MSG.REQUEST_FAILED));
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
    fnAlertMsg(getMessage(MSG.PHONE_VERIFY), () => {
      mblNo.value = "";
      mblNoFcs.value.focus();
    });
  }
};

const fnUserWithdrawal = () => {
  newWithdrawalDate.value = "";
  withdrawalDialogVisible.value = true;
};

const fnCloseWithdrawalDialog = () => {
  withdrawalDialogVisible.value = false;
  newWithdrawalDate.value = "";
};

const fnScheduleWithdrawal = async () => {
  if (!newWithdrawalDate.value) {
    await proxy.$alert(getMessage(MSG.USER_INFO_WITHDRAWAL_DATE_REQUIRED));
    return;
  }

  const confirmed = await proxy.$confirm(
    getMessage(MSG.USER_INFO_WITHDRAWAL_DATE_CONFIRM, {
      withdrawalDate: newWithdrawalDate.value,
    })
  );
  if (!confirmed) return;

  try {
    await axios.post("/webApi/user01/schedule-withdrawal", {
      cmpnyCd: cmpnyCd.value,
      userCd: userCd.value,
      withdrawalDate: newWithdrawalDate.value,
    });
    fnAlertMsg(getMessage(MSG.USER_INFO_WITHDRAWAL_DATE_SET), () => {
      withdrawalDialogVisible.value = false;
      newWithdrawalDate.value = "";
      emit("close");
      props.onSearch();
    });
  } catch (err) {
    fnAlertMsg(
      err?.response?.data?.message ||
        getMessage(MSG.USER_INFO_WITHDRAWAL_DATE_FAILED)
    );
  }
};

const fnCancelWithdrawal = async () => {
  const confirmed = await proxy.$confirm("탈퇴 예정을 취소하시겠습니까?");
  if (!confirmed) return;

  try {
    await axios.post("/webApi/user01/cancel-withdrawal", {
      cmpnyCd: cmpnyCd.value,
      userCd: userCd.value,
    });
    fnAlertMsg("탈퇴가 취소되었습니다.", () => {
      withdrawalDate.value = "";
      emit("close");
      props.onSearch();
    });
  } catch (err) {
    fnAlertMsg(
      err?.response?.data?.message || getMessage(MSG.REQUEST_FAILED)
    );
  }
};

const fnUserPwResetConf = () => {
  fnConfirmMsg(getMessage(MSG.USER_PW_RESET_CONFIRM), () => {
    fnUserPwReset();
  });
};

const fnUserInfoValidationChk = () => {
  let retVal = true;

  if (
    mblNo.value.replaceAll("-", "") != oriMblNo.value.replaceAll("-", "") &&
    !smsCertNoChk.value
  ) {
    fnAlertMsg(getMessage(MSG.PHONE_AUTH_REQUIRED), () => {
      certNoFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(birthDt.value)) {
    fnAlertMsg(getMessage(MSG.BIRTH_AUTH_REQUIRED), () => {
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
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
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

.withdrawal-date-row {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.75rem;
}

.btn-danger {
  background: #ef4444;
  color: #fff;
  border: none;
}
.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}
.btn-danger:disabled {
  background: rgba(239, 68, 68, 0.35);
  cursor: not-allowed;
}

.withdrawal-dialog-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: inherit;
  z-index: 10;
}

.withdrawal-dialog {
  background: var(--card-bg, #ffffff);
  border: var(--card-border);
  border-radius: var(--card-radius, 16px);
  box-shadow: var(--card-shadow);
  width: 320px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.withdrawal-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--color-bg, #f9fafb);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  font-size: 1rem;
  font-weight: 600;
  font-family: "Pretendard", sans-serif;
  color: var(--color-text-strong, #111827);
}

.withdrawal-dialog-body {
  padding: 1.2rem 1rem;
}

.withdrawal-dialog-footer {
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.dialog-date-input {
  flex: 1;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.dialog-date-input:focus {
  border-color: var(--color-border-strong, #d1d5db);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
</style>
