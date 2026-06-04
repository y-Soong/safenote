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
          <span>{{ isCreate ? "사용자 생성" : "사용자정보" }}</span>
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
              :disabled="!isCreate"
              :maxlength="50"
              placeholder="아이디"
            />
            <input
              class="row-short"
              v-model="userNm"
              :disabled="!isCreate"
              :maxlength="50"
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
              v-if="!isCreate"
              ref="smsAuthReqBtnFcs"
              class="btn btn-primary"
              @click="fnSmsAuthReq"
              :disabled="timer > 0"
              v-show="btnAuthChkDisabledVisible"
            >
              {{ timer > 0 ? `${timer}초 후 재요청` : "인증요청" }}
            </button>
          </div>

          <!-- 인증번호 입력 영역: 생성 모드에서는 표시하지 않음(D1 — 관리자가 만든 계정은 휴대폰 미인증 상태로 INSERT). -->
          <div class="form-row-max" v-if="!isCreate">
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

          <!-- 생성 모드 안내: 초기 비밀번호는 휴대폰번호이며, 첫 로그인 시 본인인증이 필요함(D2/D3). -->
          <p class="leave-section-hint" v-if="isCreate">
            ⓘ 초기 비밀번호는 입력하신 휴대폰번호(하이픈 제외)로 설정됩니다.<br />
            계정 상태는 <strong>'인증대기'</strong>이며, 첫 로그인 시 휴대폰
            본인인증을 통과하면 활성화됩니다.
          </p>

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

          <!-- 사용여부: 생성 모드는 항상 'Y'(USE_YN) + ACCOUNT_STATUS='04 인증대기' 고정이라 미노출. -->
          <div class="form-row-max" v-if="!isCreate">
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

          <!-- 생성 모드 전용: 입사일 / 고용형태 / 계약종료일. 본 폼은 leave-info 가 없으므로 직접 입력. -->
          <div class="form-row-max" v-if="isCreate">
            <label>입사일</label>
            <input
              v-model="hireDateInput"
              type="date"
              class="dialog-date-input"
            />
          </div>

          <div class="form-row-max" v-if="isCreate">
            <label>고용형태</label>
            <BaseSelect v-model="employmentType">
              <option :value="''">-</option>
              <option value="REGULAR">정규직</option>
              <option value="CONTRACT">계약직</option>
              <option value="DAILY">일용직</option>
              <option value="EXECUTIVE">임원</option>
            </BaseSelect>
          </div>

          <div
            class="form-row-max"
            v-if="isCreate && employmentType === 'CONTRACT'"
          >
            <label>계약종료일</label>
            <input
              v-model="contractEndDateInput"
              type="date"
              class="dialog-date-input"
            />
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

          <!-- PRAFTA-037-F7: 생성 모드 전용 - 추가 사이트 권한. 기본 사이트 외에 다중 사이트 권한을 함께 부여. -->
          <div class="form-row-max" v-if="isCreate">
            <label>추가 사이트 권한</label>
            <div class="additional-sites-area">
              <div class="additional-site-chips">
                <span
                  v-for="(s, idx) in additionalSites"
                  :key="s.siteCd"
                  class="site-chip"
                >
                  {{ s.siteNm }}
                  <button
                    type="button"
                    class="site-chip-remove"
                    @click="fnRemoveAdditionalSite(idx)"
                    aria-label="삭제"
                  >
                    ×
                  </button>
                </span>
                <span
                  v-if="additionalSites.length === 0"
                  class="site-chip-empty"
                  >없음</span
                >
              </div>
              <button
                type="button"
                class="btn btn-primary"
                @click="fnAddAdditionalSite"
              >
                추가
              </button>
            </div>
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

          <!-- 근태/연차 정보 (master/hr 전용) — 생성 모드에서는 위 입사일 input 으로 대체되므로 미노출. -->
          <div class="leave-section" v-if="isHrOrMaster && !isCreate">
            <div class="leave-section-title">근태/연차 정보</div>

            <div class="form-row-max">
              <label>입사일</label>
              <input
                class="row-readonly"
                :value="hireDate"
                readonly
                placeholder="미설정"
              />
              <button class="btn btn-primary" @click="fnHireDateEditOpen">
                입사일 수정
              </button>
            </div>
            <p class="leave-section-hint">
              ⓘ 입사일 변경은 연차 부여 등 노무 계산에 영향을 줍니다.
            </p>
          </div>

          <!-- 경력 인정 (master/hr 전용) -->
          <div class="leave-section" v-if="isHrOrMaster">
            <div class="leave-section-title">경력 인정</div>

            <!-- 경력 인정이 연차에 미치는 영향 안내 (prafta-030) -->
            <div class="credit-notice">
              <p>
                경력 인정은 <strong>본연차·근속가산</strong> 산정에 반영됩니다.
                <span class="credit-notice-mono"
                  >산정 근속 = 실제 입사일 + 인정 경력</span
                >
              </p>
              <p>
                인정 경력으로 <strong>산정 근속이 1년 이상</strong>이 되면
                본연차가 발생하며, 이때 실제 근무 1년 미만이라도
                <strong>1년 미만 월차는 중복 부여되지 않습니다</strong>(고용승계
                등 — 재직자와 동일 대우).
              </p>
              <p>
                실제 연차 반영(소급·부여)은
                <strong>사용자 연차관리(Attd_09)의 '정책 기준 부여'</strong>에서
                처리됩니다. 입사일 자체를 변경하려면 입사일 수정 기능을
                사용하세요.
              </p>
            </div>

            <div class="credit-list">
              <div
                class="credit-item"
                v-for="(item, idx) in creditList"
                :key="idx"
              >
                <div class="credit-item-header">
                  <span class="credit-item-title"
                    >인정 항목 #{{ idx + 1 }}</span
                  >
                  <button
                    class="btn btn-danger btn-credit-del"
                    @click="fnRemoveCredit(idx)"
                  >
                    삭제
                  </button>
                </div>
                <div class="form-row-max">
                  <label>인정 개월</label>
                  <input
                    class="row-short"
                    type="number"
                    min="0"
                    v-model.number="item.creditMonths"
                    placeholder="0"
                  />
                  <span class="credit-suffix">개월</span>
                </div>
                <div class="form-row-max">
                  <label>상세 설명</label>
                  <input
                    v-model="item.reasonDetail"
                    maxlength="500"
                    placeholder="상세 설명"
                  />
                </div>
              </div>

              <!-- 생성 모드는 1건만 허용(다건 등록은 생성 후 상세 팝업에서). -->
              <button
                class="add-credit-btn"
                @click="fnAddCredit"
                v-if="!isCreate || creditList.length === 0"
              >
                + 인정 항목 추가
              </button>

              <div class="credit-summary">
                총 인정:
                <strong
                  >{{ creditTotalMonths }}개월({{ creditTotalYears }}년)</strong
                >
                · 법적 근속 기준일:
                <strong>{{ legalTenureBaseDate || "-" }}</strong>
              </div>
            </div>
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
              v-if="!isCreate"
              class="btn btn-danger"
              v-show="btnUserWithdrawalVisible && !withdrawalDate"
              @click="fnUserWithdrawal"
            >
              회원탈퇴
            </button>
            <button
              v-if="!isCreate"
              class="btn btn-primary"
              @click="fnUserPwResetConf"
            >
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
  computed,
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
import { resolveApiErrorMessage } from "@/utils/apiError";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import HireDateEditPop from "./HireDateEditPop.vue";

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
// PRAFTA-037-F7: 생성 모드 전용. 추가 사이트 권한 목록 (기본 siteCd 외).
// 각 요소: { siteCd, siteNo, siteNm }. 백엔드 전송 시 siteCd 만 추출.
const additionalSites = ref([]);
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

// PRAFTA-017-4 근태/연차 정보 (master/hr 전용)
const hireDate = ref("");          // 입사일 (YYYY-MM-DD)
const employmentType = ref("");    // 고용형태 [SYS041]
const creditList = ref([]);        // 경력 인정 항목 [{ creditMonths, reasonDetail }]
const legalTenureBaseDate = ref(""); // 법적 근속 기준일 (YYYY-MM-DD)

// PRAFTA-036 생성 모드 전용 입력값
const hireDateInput = ref("");          // type="date" — YYYY-MM-DD
const contractEndDateInput = ref("");   // type="date" — YYYY-MM-DD

// =========================== Data ===========================
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// =========================== Computed ===========================
// 근태/연차·경력 인정 섹션 게이트: master/hr 권한만
const isHrOrMaster = computed(() =>
  ["master", "hr"].includes(sessionStorage.getItem("gv_authCd"))
);

// PRAFTA-036 생성 모드 여부 (callmethod_p='C')
const isCreate = computed(() => props.callmethod_p === "C");

// 경력 인정 총 개월/년 (프론트 계산, 요약 표시용)
const creditTotalMonths = computed(() =>
  creditList.value.reduce(
    (sum, it) => sum + (Number(it.creditMonths) || 0),
    0
  )
);
const creditTotalYears = computed(() =>
  Math.floor(creditTotalMonths.value / 12)
);

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  cmpnyCd.value = props.cmpnyCd_p;

  if (isCreate.value) {
    // 생성 모드 초기값. 권한은 사용자가 직접 선택(빈 값). SMS 인증 UI 게이트 무력화.
    authCd.value = "";
    useYn.value = "Y";
    mblNoDisabled.value = false;
    btnAuthChkDisabledVisible.value = false;
    smsCertNoChk.value = true; // 검증 우회용(저장 시 사용 안 함)
    return;
  }

  if (props.userId_p) {
    userId.value = props.userId_p;
    await fnGetUserInfo(userId.value);

    // master/hr 권한일 때만 근태/연차 정보 추가 조회 (기본정보와 독립 호출)
    if (isHrOrMaster.value && userCd.value) {
      await fnGetLeaveInfo();
    }
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
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
    fnAlertMsg(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
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
    fnAlertMsg(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
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
    fnAlertMsg(resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다."));
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
    fnAlertMsg(resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다."), () => {
      smsAuthReqBtnFcs.value.focus();
    });
  }
};

const fnUserInfoSave = async () => {
  if (!fnUserInfoValidationChk()) {
    return;
  }

  const result = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!result) return;

  // 생성 모드: /insert-user-info 단건 호출. 경력은 같은 트랜잭션으로 서버에서 함께 INSERT.
  if (isCreate.value) {
    try {
      const credit = creditList.value[0];
      const payload = {
        userId: userId.value,
        userNm: userNm.value,
        authCd: authCd.value,
        siteNo: siteNo.value,
        nodeCd: nodeCd.value,
        mblNo: (mblNo.value || "").replace(/-/g, ""),
        email: email.value,
        gender: gender.value,
        birthDt: (birthDt.value || "").replace(/-/g, ""),
        rankCd: null,
        hireDate: (hireDateInput.value || "").replace(/-/g, ""),
        employmentType: employmentType.value || null,
        contractEndDate: (contractEndDateInput.value || "").replace(/-/g, ""),
        creditMonths: credit ? Number(credit.creditMonths) || 0 : 0,
        creditReasonType: null,
        creditReasonDetail: credit ? credit.reasonDetail : null,
        // PRAFTA-037-F7: 추가 사이트 권한 (선택). 빈 배열도 무방 — 백엔드 null/empty 처리.
        additionalSiteCdList: additionalSites.value.map((s) => s.siteCd),
      };

      const response = await axios.post("/webApi/user01/insert-user-info", payload);
      if (response.status === 200) {
        fnAlertMsg("사용자가 생성되었습니다. 첫 로그인 시 휴대폰 본인인증이 필요합니다.", () => {
          emit("close");
          if (props.onSearch) props.onSearch();
        });
      }
    } catch (err) {
      fnAlertMsg(resolveApiErrorMessage(err, "사용자 생성 중 오류가 발생했습니다."));
    }
    return;
  }

  // 조회/수정 모드(기존 로직)
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
      // master/hr면 기본정보 저장 성공 후 경력 인정 저장(분리 호출)
      if (isHrOrMaster.value) {
        const creditSaved = await fnSaveCredit();
        if (!creditSaved) return; // 경력 인정 저장 실패 시 알럿은 fnSaveCredit에서 처리
      }

      fnAlertMsg(getMessage(MSG.SAVE_SUCCESS), () => {
        emit("close");
        props.onSearch();
      });
    }
  } catch (err) {
    fnAlertMsg(getMessage(MSG.REQUEST_FAILED));
  }
};

// =========================== PRAFTA-017-4 Methods ===========================
// 근태/연차 정보(입사일/고용형태/경력 인정) 조회 (master/hr 전용)
const fnGetLeaveInfo = async () => {
  try {
    const response = await axios.get(
      `/webApi/user01/${userCd.value}/leave-info`
    );
    if (response.status === 200) {
      const data = response.data || {};
      hireDate.value = data.hireDate || "";
      employmentType.value = data.employmentType || "";
      legalTenureBaseDate.value = data.legalTenureBaseDate || "";
      creditList.value = (data.creditList || []).map((it) => ({
        creditMonths: it.creditMonths,
        reasonDetail: it.reasonDetail,
      }));
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "근태/연차 정보 조회 중 오류가 발생했습니다.")
    );
  }
};

// 경력 인정 항목 추가
const fnAddCredit = () => {
  creditList.value.push({ creditMonths: 0, reasonDetail: "" });
};

// 경력 인정 항목 삭제
const fnRemoveCredit = (idx) => {
  creditList.value.splice(idx, 1);
};

// 경력 인정 저장 (delete-and-insert 전량 교체). 성공 시 true 반환.
const fnSaveCredit = async () => {
  // 인정 개월 검증: 0 이상 정수
  for (const it of creditList.value) {
    const months = Number(it.creditMonths);
    if (!Number.isFinite(months) || months < 0) {
      await proxy.$alert("인정 개월 수는 0 이상으로 입력해 주세요.");
      return false;
    }
  }

  try {
    const response = await axios.post("/webApi/user01/update-user-credit", {
      cmpnyCd: cmpnyCd.value,
      userCd: userCd.value,
      creditList: creditList.value.map((it) => ({
        creditMonths: Number(it.creditMonths) || 0,
        reasonDetail: it.reasonDetail,
      })),
    });
    return response.status === 200;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "경력 인정 저장 중 오류가 발생했습니다.")
    );
    return false;
  }
};

// 입사일 수정 모달 오픈 (중첩 openPop). 성공 시 onSaved 콜백으로 leave-info 재조회.
const fnHireDateEditOpen = () => {
  openPop(HireDateEditPop, {
    cmpnyCd_p: cmpnyCd.value,
    userCd_p: userCd.value,
    userId_p: userId.value,
    userNm_p: userNm.value,
    nodeNm_p: nodeNm.value,
    hireDate_p: hireDate.value,
    onSaved: fnGetLeaveInfo,
  });
};

const fnUserPwReset = async () => {
  try {
    // cmpnyCd는 서버가 토큰으로 강제하므로 전송하지 않는다.
    const response = await axios.post("/webApi/user01/update-user-passwd", {
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
      resolveApiErrorMessage(
        err,
        getMessage(MSG.USER_INFO_WITHDRAWAL_DATE_FAILED)
      )
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
    fnAlertMsg(resolveApiErrorMessage(err, getMessage(MSG.REQUEST_FAILED)));
  }
};

const fnUserPwResetConf = () => {
  fnConfirmMsg(getMessage(MSG.USER_PW_RESET_CONFIRM), () => {
    fnUserPwReset();
  });
};

const fnUserInfoValidationChk = () => {
  // 생성 모드 검증 (D7): SMS 본인인증은 우회. 필수값/사업장번호/부서/휴대폰 형식만 검사.
  if (isCreate.value) {
    if (proxy.$util.isEmpty(userId.value)) {
      fnAlertMsg("사용자ID를 입력해 주세요.");
      return false;
    }
    if (proxy.$util.isEmpty(userNm.value)) {
      fnAlertMsg("사용자명을 입력해 주세요.");
      return false;
    }
    if (proxy.$util.isEmpty(authCd.value)) {
      fnAlertMsg("권한을 선택해 주세요.");
      return false;
    }
    if (proxy.$util.isEmpty(siteNo.value)) {
      fnAlertMsg("사업장을 선택해 주세요.");
      return false;
    }
    if (proxy.$util.isEmpty(nodeCd.value)) {
      fnAlertMsg("소속부서를 선택해 주세요.");
      return false;
    }
    const phoneDigits = (mblNo.value || "").replace(/\D/g, "");
    if (phoneDigits.length < 10 || phoneDigits.length > 11) {
      fnAlertMsg("휴대폰번호를 올바르게 입력해 주세요.");
      return false;
    }
    if (proxy.$util.isEmpty(birthDt.value)) {
      fnAlertMsg("생년월일을 입력해 주세요.");
      return false;
    }
    return true;
  }

  // 조회/수정 모드(기존 로직): 휴대폰 변경 시 SMS 본인인증 + 생년월일 필수.
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

// PRAFTA-037-F7: 추가 사이트 권한 — 검색 팝업으로 선택 후 칩 추가.
const fnAddAdditionalSite = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: cmpnyCd.value,
    onSelect: onAdditionalSiteSelected,
  });
};

const onAdditionalSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  if (!siteCdVal) return;
  // 기본 사이트 중복 차단
  if (siteCd.value && siteCdVal === siteCd.value) {
    proxy.$alert("이미 기본 사이트로 선택된 사업장입니다.");
    return;
  }
  // 추가 목록 내 중복 차단
  if (additionalSites.value.some((s) => s.siteCd === siteCdVal)) {
    proxy.$alert("이미 추가된 사업장입니다.");
    return;
  }
  additionalSites.value.push({
    siteCd: siteCdVal,
    siteNo: siteNoVal,
    siteNm: siteNmVal,
  });
};

const fnRemoveAdditionalSite = (idx) => {
  additionalSites.value.splice(idx, 1);
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

/* PRAFTA-017-4 근태/연차·경력 인정 섹션 (master/hr 전용) */
.leave-section {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.leave-section-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  padding-bottom: 0.375rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.leave-section-hint {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
  line-height: 1.5;
}

.row-readonly {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
}

.credit-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.75rem;
}

.credit-item {
  background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.credit-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.credit-item-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}

.btn-credit-del {
  height: 1.75rem;
  padding: 0 0.625rem;
  font-size: 0.75rem;
}

.credit-suffix {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

.add-credit-btn {
  width: 100%;
  height: 2rem;
  border: 1px dashed var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
}

.add-credit-btn:hover {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-strong, #111827);
}

.credit-summary {
  background: var(--color-info-bg, #eff6ff);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-info-text, #1d4ed8);
}

.credit-summary strong {
  font-weight: 600;
}

/* 경력 인정 → 연차 영향 안내 (prafta-030) */
.credit-notice {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
  padding: 0.625rem 0.75rem;
  background: var(--color-info-bg, #eff6ff);
  border-radius: var(--input-radius, 10px);
  font-size: 0.6875rem;
  line-height: 1.5;
  color: var(--color-info-text, #1d4ed8);
}

.credit-notice strong {
  font-weight: 600;
}

.credit-notice-mono {
  display: inline-block;
  margin-left: 0.25rem;
  padding: 0 0.375rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text-muted, #4b5563);
  font-size: 0.625rem;
}

/* PRAFTA-037-F7: 추가 사이트 권한 영역 */
.additional-sites-area {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  flex: 1;
}
.additional-site-chips {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  padding: 0.375rem;
  min-height: 2.25rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  background: var(--color-bg, #f9fafb);
}
.site-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.125rem 0.5rem;
  background: var(--color-info-bg, #eff6ff);
  color: var(--color-info-text, #1d4ed8);
  border-radius: 999px;
  font-size: 0.8rem;
}
.site-chip-remove {
  background: transparent;
  border: none;
  color: var(--color-info-text, #1d4ed8);
  font-size: 1rem;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}
.site-chip-remove:hover {
  color: var(--color-danger, #dc2626);
}
.site-chip-empty {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.8rem;
  padding: 0 0.375rem;
}
</style>
