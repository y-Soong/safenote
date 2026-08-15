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
                v-for="opt in authOptions"
                :key="opt.baimValDCd"
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
            <!-- PRAFTA-WEB_002-T1-07(1.2): 생성 모드 입사일은 '테두리 안에 캘린더가 든 박스'로 보이던
                 이중 테두리(외곽 래퍼 + 내부 input)를 제거하고, 다른 입력과 동일한 단일 캘린더 입력으로 표시.
                 (공용 CalendarSrch 미변경 — 본 화면 한정 .hire-date-field 로 외곽 래퍼 장식만 제거) -->
            <CalendarSrch v-model="hireDateInput" class="hire-date-field" />
          </div>

          <!-- PRAFTA_COM_003-B: 생성 팝업은 정규직(REGULAR) 고정. select 제거하고 읽기전용 표시.
               일용직은 QR 슬롯 발급 등 별도 경로로 생성된다. 계약직/임원 옵션 및 계약종료일 분기 제거. -->
          <div class="form-row-max" v-if="isCreate">
            <label>고용형태</label>
            <input class="row-readonly" value="정규직" readonly />
          </div>

          <!-- 생성 모드: 사업장/부서/기본 근무타입 직접 입력. -->
          <template v-if="isCreate">
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

            <!-- PRAFTA-COM-008-E-5: 기본 근무타입(사업장 활성 근무타입). 설정 시 당해 연말까지 평일 자동생성. -->
            <div class="form-row-max">
              <label>기본 근무타입</label>
              <BaseSelect
                id="defaultSchCd"
                v-model="defaultSchCd"
                :disabled="schTypeLoading || !siteCd"
              >
                <option :value="''">-</option>
                <option
                  v-for="opt in schTypeOptions"
                  :key="opt.schCd"
                  :value="opt.schCd"
                >
                  {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{
                    fnFmtSchTime(opt.fstSchEndTime)
                  }})
                </option>
              </BaseSelect>
            </div>
            <p class="default-sch-hint" v-if="defaultSchCd">
              ⓘ 기본 근무타입 설정 시 내일(명일)부터 당해 연말까지 평일
              근무계획이 자동 생성·갱신됩니다(빈 날·자동생성분만,
              휴일·연차·교대팀 구간 제외).
            </p>

            <!-- 소정-08(UI-A): 소정근로시간 필수 입력 — "풀타임 / 단시간(직접 입력)" 선택식.
                 풀타임의 주 소정근로 분은 서버가 회사 통상 기준값으로 채운다(화면 하드코딩 금지). -->
            <div class="form-row-max">
              <label>소정근로시간</label>
              <div class="std-work-radio-group">
                <label class="std-work-radio">
                  <input type="radio" value="FULL" v-model="stdWorkType" />
                  <span>풀타임 ({{ stdWorkFullTimeLabel }})</span>
                </label>
                <label class="std-work-radio">
                  <input type="radio" value="DIRECT" v-model="stdWorkType" />
                  <span>단시간(직접 입력)</span>
                </label>
              </div>
            </div>

            <div class="form-row-max" v-if="isStdWorkDirect">
              <label>주 소정근로</label>
              <input
                class="row-short"
                type="number"
                min="0"
                max="168"
                v-model.number="stdWorkHours"
                placeholder="시간"
              />
              <span class="std-work-suffix">시간</span>
              <input
                class="row-short"
                type="number"
                min="0"
                max="59"
                v-model.number="stdWorkMinutes"
                placeholder="분"
              />
              <span class="std-work-suffix">분</span>
            </div>

            <div class="form-row-max" v-if="isStdWorkDirect">
              <label>소정근로 사유</label>
              <BaseSelect id="stdWorkReasonCd" v-model="stdWorkReasonCd">
                <option
                  v-for="opt in stdWorkReasonOptions"
                  :key="opt.reasonCd"
                  :value="opt.reasonCd"
                >
                  {{ opt.reasonNm }}
                </option>
              </BaseSelect>
            </div>

            <p
              class="std-work-warning"
              v-for="(warn, idx) in stdWorkWarnings"
              :key="idx"
            >
              ⚠ {{ warn }}
            </p>

            <p class="default-sch-hint" v-if="isStdWorkDirect">
              ⓘ 육아기·임신기·가족돌봄 단축은 적용 기간이 필요해 계정 생성
              단계에서는 등록할 수 없습니다. 계정 생성 후 소정근로시간
              관리에서 기간과 함께 등록해 주세요.
            </p>
          </template>

          <!-- 수정 모드: 사업장/부서는 읽기전용(변경은 '소속이동'으로 일원화, PRAFTA-WEB_001-4).
               기본 근무타입은 F-8-1 로 독립 변경 경로 신설 — 생성 모드와 동일한 BaseSelect 재사용,
               대상 사업장은 현재 사용자의 SITE_CD 고정(사업장 자체 변경은 소속이동 전용). -->
          <template v-else>
            <div class="form-row-max">
              <label>사업장</label>
              <input
                class="row-readonly"
                :value="siteNm"
                readonly
                placeholder="미설정"
              />
            </div>
            <div class="form-row-max">
              <label>소속부서</label>
              <input
                class="row-readonly"
                :value="nodeNm"
                readonly
                placeholder="미설정"
              />
            </div>

            <!-- F-8-1: 기본 근무타입 편집 언락. 서버(User01ServiceImpl)는 이미 defaultSchCd
                 화이트리스트 검증 + 자동생성 갱신을 처리하므로 프론트만 언락하면 된다. -->
            <div class="form-row-max">
              <label>기본 근무타입</label>
              <BaseSelect
                id="defaultSchCdEdit"
                v-model="defaultSchCd"
                :disabled="schTypeLoading || !siteCd"
              >
                <option :value="''">-</option>
                <option
                  v-for="opt in schTypeOptions"
                  :key="opt.schCd"
                  :value="opt.schCd"
                >
                  {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{
                    fnFmtSchTime(opt.fstSchEndTime)
                  }})
                </option>
              </BaseSelect>
            </div>
            <p class="default-sch-hint" v-if="defaultSchCd">
              ⓘ 기본 근무타입 변경 시 내일(명일)부터 당해 연말까지 평일
              근무계획이 자동 생성·갱신됩니다(빈 날·자동생성분만,
              휴일·연차·교대팀 구간 제외).
            </p>

            <div class="form-row-max" v-if="canTransfer">
              <label>소속이동</label>
              <button class="btn btn-primary" @click="fnTransferOpen">
                소속이동
              </button>
            </div>
            <p class="default-sch-hint" v-if="canTransfer">
              ⓘ 사업장/부서 변경은 '소속이동'으로 처리됩니다(지정한
              이동일에 발효).
            </p>
          </template>

          <!-- PRAFTA_COM_003-B: "추가 사이트 권한"(PRAFTA-037-F7) 영역 제거(생성 팝업 한정). -->

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
                  <!-- com-014-2: CalendarSrch minDate(내일) 가드로 네이티브 input 교체.
                       modelValue 계약은 YYYY-MM-DD 동일(서버 페이로드 무변경). -->
                  <CalendarSrch
                    class="dialog-date-input"
                    v-model="newWithdrawalDate"
                    :minDate="tomorrowDate"
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
  watch,
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
import { createNotices } from "@/utils/validationNotice";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import HireDateEditPop from "./HireDateEditPop.vue";
import UserTransferPop from "./UserTransferPop.vue";

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
// PRAFTA-COM-008-E-5: 기본 근무타입 select 상태.
const defaultSchCd = ref("");
// F-8-1: 수정 모드 진입 시 조회값 스냅샷(변경 여부 판정 + 변경 확인창 게이팅용).
const oriDefaultSchCd = ref("");
const schTypeOptions = ref([]);
const schTypeLoading = ref(false);
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
// 대상 사용자 등급(표시/타 분기용). 수정 모드 진입 시 fnGetUserInfo 가 조회값으로 덮어쓴다.
const authLevel = ref(sessionStorage.getItem('gv_authLevel'));
// PRAFTA-WEB_002-T1-05(1.4-2): 권한 옵션 필터 임계 = "요청자(viewer) 세션 등급" 고정값.
//   authLevel(대상 사용자 등급)로 덮어쓰지 않는 별도 ref 로 분리한다(master 가 일반사용자 수정 시
//   옵션이 "일반사용자만" 으로 좁혀지던 버그 방지).
//   gv_authLevel 부재/비정상(NaN)이면 가장 제한적인 등급(999)으로 폴백한다 — 옵션이 과도하게
//   열리는 것을 막는 fail-safe(저장 가드는 서버 JWT 기준 fail-closed 라 권한 상승은 별도 차단됨).
const viewerAuthLevel = ref(
  Number.isFinite(Number(sessionStorage.getItem('gv_authLevel')))
    ? Number(sessionStorage.getItem('gv_authLevel'))
    : 999
);

// PRAFTA-017-4 근태/연차 정보 (master/hr 전용)
const hireDate = ref("");          // 입사일 (YYYY-MM-DD)
// PRAFTA_COM_003-B: 생성 팝업은 정규직(REGULAR) 고정. 수정 모드에서는 fnGetLeaveInfo 가 조회값으로 덮어쓴다.
const employmentType = ref("REGULAR"); // 고용형태 [SYS041]
const creditList = ref([]);        // 경력 인정 항목 [{ creditMonths, reasonDetail }]
const legalTenureBaseDate = ref(""); // 법적 근속 기준일 (YYYY-MM-DD)

// PRAFTA-036 생성 모드 전용 입력값
const hireDateInput = ref("");          // CalendarSrch — YYYY-MM-DD

// 소정-08(UI-A) 생성 모드 소정근로시간 입력값
const stdWorkType = ref("FULL");        // FULL:풀타임 / DIRECT:단시간(직접 입력)
const stdWorkHours = ref(null);         // 단시간 주 소정근로 시간 부분
const stdWorkMinutes = ref(0);          // 단시간 주 소정근로 분 부분
const stdWorkReasonCd = ref("");        // 사유코드 [SYS083] — 단시간 선택 시만 사용
const stdWorkReasonOptions = ref([]);   // 사유 셀렉트 옵션(서버 제공 — 코드 하드코딩 금지)
const cmpnyWeekStdMinutes = ref(null);  // 통상 기준값(분, 배정 사업장 오버라이드 반영) — 풀타임 라벨 표기용

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

// PRAFTA-WEB_001-4: 소속이동 가능 게이트(master/hr & 수정 모드). 생성 모드는 직접 입력 사용.
const canTransfer = computed(() => isHrOrMaster.value && !isCreate.value);

// ── PRAFTA-COM-008-E-5: 기본 근무타입 ────────────────────────
// 'HHmm' → 'HH:mm' 라벨 포맷(4자리 미만이면 원본 반환).
const fnFmtSchTime = (t) => {
  if (!t || t.length < 4) return t || "";
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`;
};

// 사업장 활성 근무타입 옵션 조회(siteCd 기준). 회사 스코프는 서버가 토큰에서 강제.
const fnLoadSchTypeOptions = async (targetSiteCd) => {
  if (!targetSiteCd) {
    schTypeOptions.value = [];
    return;
  }
  schTypeLoading.value = true;
  try {
    const response = await axios.get("/webApi/user01/sch-type-options", {
      params: { siteCd: targetSiteCd },
    });
    schTypeOptions.value = response.data ?? [];
    // 현재 선택값이 목록에 없으면(사업장 변경 등) 선택 해제.
    if (
      defaultSchCd.value &&
      !schTypeOptions.value.some((o) => o.schCd === defaultSchCd.value)
    ) {
      defaultSchCd.value = "";
    }
  } catch (err) {
    schTypeOptions.value = [];
    fnAlertMsg(resolveApiErrorMessage(err, "근무타입 목록 조회 중 오류가 발생했습니다."));
  } finally {
    schTypeLoading.value = false;
  }
};

// 사업장 선택/변경 시 근무타입 옵션 재조회.
//   통상근로시간 기준값도 사업장별로 다를 수 있어 함께 재조회한다(풀타임 라벨/저장값 정합).
watch(siteCd, (newSiteCd) => {
  fnLoadSchTypeOptions(newSiteCd);
  if (isCreate.value) {
    fnLoadStdWorkOptions();
  }
});

// ── 소정-08(UI-A): 소정근로시간 선택식 입력 ────────────────
// 단시간 선택 여부. 풀타임이면 시간/사유 입력을 노출하지 않는다(값도 전송하지 않음).
const isStdWorkDirect = computed(() => stdWorkType.value === "DIRECT");

// 풀타임 라벨은 서버가 내려준 회사 통상 기준값으로 만든다("주 40시간" 하드코딩 금지 — 지시서 B-1).
const stdWorkFullTimeLabel = computed(() => {
  const total = Number(cmpnyWeekStdMinutes.value);
  if (!Number.isFinite(total) || total <= 0) return "회사 기준";
  const hours = Math.floor(total / 60);
  const minutes = total % 60;
  return minutes === 0 ? `주 ${hours}시간` : `주 ${hours}시간 ${minutes}분`;
});

// 단시간 입력값(시간/분)의 합 — 서버에는 항상 분 단위로 보낸다.
const stdWorkInputMinutes = computed(() => {
  const hours = Number(stdWorkHours.value) || 0;
  const minutes = Number(stdWorkMinutes.value) || 0;
  return Math.round(hours * 60 + minutes);
});

// 경고 배너(저장은 허용 — plan §8 Q4 확정). 주 15시간(900분) 미만 = 초단시간 경계.
//   ★육아기 주 15~35시간 범위 경고는 이 폼의 대상이 아니다: 단축 사유(육아기·임신기·가족돌봄)는
//   적용 종료일이 필수라 사유 셀렉트 옵션(서버 std-work-options)에서 제외되며, 기간과 함께
//   등록하는 소정근로시간 관리 화면이 그 경고를 담당한다.
const stdWorkWarnings = computed(() => {
  const warnings = [];
  if (!isStdWorkDirect.value) return warnings;
  const total = stdWorkInputMinutes.value;
  if (total > 0 && total < 900) {
    warnings.push(
      "주 소정근로시간이 15시간 미만입니다. 초단시간근로자는 연차·주휴 적용 대상에서 제외될 수 있으니 계약 내용을 확인해 주세요."
    );
  }
  return warnings;
});

// 소정근로 입력 옵션(통상 기준값 + 사유 셀렉트) 조회. 회사 스코프는 서버가 토큰에서 강제.
//   siteCd 는 배정 사업장 — 사업장 오버라이드가 있으면 그 기준값이 내려온다(없으면 회사 기본값).
const fnLoadStdWorkOptions = async () => {
  try {
    const response = await axios.get("/webApi/user01/std-work-options", {
      params: { siteCd: siteCd.value || "" },
    });
    const data = response.data || {};
    cmpnyWeekStdMinutes.value = data.cmpnyWeekStdMinutes ?? null;
    stdWorkReasonOptions.value = data.reasonOptions ?? [];
    // 기본 선택 = 첫 옵션(단시간계약). 옵션이 없으면 빈 값으로 두고 저장 시 검증에서 막는다.
    if (!stdWorkReasonCd.value && stdWorkReasonOptions.value.length > 0) {
      stdWorkReasonCd.value = stdWorkReasonOptions.value[0].reasonCd;
    }
  } catch (err) {
    stdWorkReasonOptions.value = [];
    fnAlertMsg(
      resolveApiErrorMessage(err, "소정근로시간 옵션 조회 중 오류가 발생했습니다.")
    );
  }
};

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

// PRAFTA-WEB_002-T1-05(1.4-2): 권한 select 옵션.
//   = COM005 중 "옵션 등급(sortIdx) > viewer 세션 등급" 인 항목(strict >). 본인과 동일/상위 등급은 부여 불가.
//   단, 현재 부여된 권한(authCd)이 필터에서 빠져도 select 가 빈값이 되지 않도록 현재값 옵션은 항상 포함한다.
//   옵션 등급(sortIdx)은 base-info-lists(=AUTH_LEVEL 동일값)에서 내려온다(서버 escalation 가드와 동일 기준).
const authOptions = computed(() => {
  const all = (baseInfoArr.value["COM005"] || []).filter(
    (o) => o.baimValDCd != null
  );
  const threshold = Number(viewerAuthLevel.value);
  return all.filter(
    (o) => Number(o.sortIdx) > threshold || o.baimValDCd === authCd.value
  );
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  cmpnyCd.value = props.cmpnyCd_p;

  if (isCreate.value) {
    // 생성 모드 초기값. SMS 인증 UI 게이트 무력화.
    useYn.value = "Y";
    mblNoDisabled.value = false;
    btnAuthChkDisabledVisible.value = false;
    smsCertNoChk.value = true; // 검증 우회용(저장 시 사용 안 함)

    // PRAFTA_COM_003-B 3.1.1: 권한 기본값 = "일반사용자"(COM005). 명칭 매칭으로 코드값 도출.
    //   PRAFTA-WEB_002-T1-05(1.4-2): 옵션 필터와 동일 기준(viewer 등급 strict >)으로 가용 여부 판정.
    //   옵션에서 빠지면 세팅 생략(빈 값).
    const com005 = baseInfoArr.value["COM005"] || [];
    const generalAuth = com005.find(
      (o) =>
        o.baimValDCd != null &&
        Number(o.sortIdx) > Number(viewerAuthLevel.value) &&
        (o.baimValDNm || "").trim() === "일반사용자"
    );
    authCd.value = generalAuth ? generalAuth.baimValDCd : "";

    // PRAFTA_COM_003-B 3.1.2: 성별 기본값 = "남성"(SYS004). 명칭 매칭으로 코드값 도출.
    const sys004 = systCodeArr.value["SYS004"] || [];
    const maleGender = sys004.find(
      (o) => o.systValDCd != null && (o.systValDNm || "").trim() === "남성"
    );
    if (maleGender) gender.value = maleGender.systValDCd;

    // PRAFTA_COM_003-B 3.1.3: 입사일 기본값 = 당일(YYYY-MM-DD, 로컬 기준).
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, "0");
    const dd = String(today.getDate()).padStart(2, "0");
    hireDateInput.value = `${yyyy}-${mm}-${dd}`;

    // PRAFTA_COM_003-B 3.1.4: 고용형태는 정규직(REGULAR) 고정.
    employmentType.value = "REGULAR";

    // 소정-08(UI-A): 소정근로시간 기본 선택 = 풀타임. 라벨/사유 옵션은 서버에서 받아온다.
    stdWorkType.value = "FULL";
    await fnLoadStdWorkOptions();
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
        // PRAFTA-COM-008-E-5: 기본 근무타입 prefill. siteCd 세팅으로 옵션이 비동기 로드되며,
        //   동일 사업장이라 선택값이 목록에 포함되어 watch 의 reset 가드에 걸리지 않는다.
        defaultSchCd.value = response.data.userInfoList[0].defaultSchCd || "";
        // F-8-1: 변경 여부 판정용 원본값 스냅샷.
        oriDefaultSchCd.value = defaultSchCd.value;

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

  // ★통합 안내(2026-08-11 UX 규약): 비차단 안내는 개별 확인창 연쇄 대신 저장 컨펌 하나에 병합한다.
  //   (작업지시서_연쇄-alert-전수조사-통합표시-전환 / @/utils/validationNotice)
  const notices = createNotices({
    alert: proxy.$alert,
    confirm: proxy.$confirm,
  });

  // F-8-1: 기본 근무타입 변경은 미래 스케줄 대량 갱신을 유발하므로 저장 전 확인을 받는다(문구 원문 보존).
  if (!isCreate.value && defaultSchCd.value !== oriDefaultSchCd.value) {
    notices.note(
      "기본 근무타입 변경 시 명일부터 연말까지 근무계획이 자동 생성·갱신됩니다. 계속하시겠습니까?"
    );
  }

  const result = await notices.resolve(getMessage(MSG.SAVE_CONFIRM));
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
        // PRAFTA_COM_003-B 3.1.4: 고용형태 정규직 고정. 계약종료일은 정규직이라 미전송(null).
        employmentType: "REGULAR",
        contractEndDate: null,
        creditMonths: credit ? Number(credit.creditMonths) || 0 : 0,
        creditReasonType: null,
        creditReasonDetail: credit ? credit.reasonDetail : null,
        // PRAFTA_COM_003-B: 추가 사이트 권한 제거(생성 팝업 한정). 백엔드는 빈 목록 정상 처리.
        additionalSiteCdList: [],
        // PRAFTA-COM-008-E-5: 기본 근무타입(선택). 빈값이면 미설정.
        defaultSchCd: defaultSchCd.value || null,
        // 소정-08(UI-A): 소정근로시간(필수). 풀타임은 회사 통상 기준값을 서버가 채우므로
        //   시간/사유를 보내지 않는다(클라 값 신뢰 금지).
        stdWorkType: stdWorkType.value,
        stdWorkWeekMinutes: isStdWorkDirect.value ? stdWorkInputMinutes.value : null,
        stdWorkReasonCd: isStdWorkDirect.value ? stdWorkReasonCd.value || null : null,
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
    // PRAFTA-WEB_001-4: 사업장/부서는 직접 수정 UI 제거 → '소속이동'으로만 변경.
    //   단 update-user-infos 매퍼가 SITE_CD/NODE_CD 를 무조건 SET 하므로(조건절 없음),
    //   전송을 누락하면 NULL 로 덮여 소속이 지워진다. 따라서 읽기전용으로 표시 중인 '현재값'을
    //   그대로 패스스루하여 동일값 재기록(무변경)되게 한다. (직접 변경 UI 부재로 값은 바뀌지 않음)
    // F-8-1: 기본 근무타입(defaultSchCd)은 이제 이 화면에서 직접 변경 가능 — 아래 defaultSchCd 는
    //   위 BaseSelect 의 편집값을 그대로 전송한다(서버가 화이트리스트 검증 + 자동생성 갱신 수행).
    const response = await axios.post("/webApi/user01/update-user-infos", [
      {
        cmpnyCd: cmpnyCd.value,
        userCd: userCd.value,
        userId: userId.value,
        userNm: userNm.value,
        useYn: useYn.value,
        authCd: authCd.value,
        mblNo: mblNo.value,
        email: email.value,
        gender: gender.value,
        birthDt: birthDt.value,
        // 소속/기본근무 = 현재값 패스스루(변경 아님, NULL 덮어쓰기 방지).
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        defaultSchCd: defaultSchCd.value || null,
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

// PRAFTA-WEB_001-4: 소속이동 팝업 오픈 (중첩 openPop — HireDateEditPop 패턴).
//   예약 성공(onSaved) 시 본 팝업 닫기 + 부모 목록 갱신(발효일 적용이라 현재 표시값은 불변).
const fnTransferOpen = () => {
  openPop(UserTransferPop, {
    cmpnyCd_p: cmpnyCd.value,
    userCd_p: userCd.value,
    userId_p: userId.value,
    userNm_p: userNm.value,
    employmentType_p: employmentType.value,
    // PRAFTA-WEB_002-T1-04(1.3-2): 대상자 현재 사업장 — 이동 사업장 검색에서 현재 사업장 제외에 사용.
    siteCd_p: siteCd.value,
    onSaved: () => {
      emit("close");
      if (props.onSearch) props.onSearch();
    },
  });
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
      userCd: userCd.value,
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
    // 소정-08(UI-A): 소정근로시간 필수. 단시간은 시간과 사유가 모두 있어야 한다.
    //   (15시간 미만은 경고일 뿐 저장 허용 — plan §8 Q4)
    if (stdWorkType.value !== "FULL" && stdWorkType.value !== "DIRECT") {
      fnAlertMsg("소정근로시간을 선택해 주세요.");
      return false;
    }
    if (isStdWorkDirect.value) {
      if (stdWorkInputMinutes.value <= 0) {
        fnAlertMsg("주 소정근로시간을 입력해 주세요.");
        return false;
      }
      if (proxy.$util.isEmpty(stdWorkReasonCd.value)) {
        fnAlertMsg("소정근로 사유를 선택해 주세요.");
        return false;
      }
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
    // PRAFTA-WEB_002-T1-02(1.4-1): 생성 폼은 정규직 고정 → 담당 미지정 부서도 노출(자동 담당 정 지정 대상).
    includeNoAdmin_p: true,
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
  // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 타 사업장 부서코드로 저장되는 것 방지)
  nodeCd.value = "";
  nodeNm.value = "";
};

// PRAFTA_COM_003-B: 추가 사이트 권한 관련 함수(fnAddAdditionalSite/onAdditionalSiteSelected/fnRemoveAdditionalSite) 제거.

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

/* 소정-08(UI-A): 소정근로시간 선택식 입력 */
.std-work-radio-group {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.std-work-radio {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  /* 라벨이 글자 단위로 쪼개져 3줄이 되던 것을 막는다. 폭이 모자라면 그룹의 flex-wrap 이
     라디오 단위로 줄바꿈하므로 문구는 온전히 유지된다. */
  white-space: nowrap;
  font-size: 0.8125rem;
  color: var(--color-text-strong, #111827);
  cursor: pointer;
}

.std-work-suffix {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

.std-work-warning {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-radius: var(--input-radius, 10px);
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
  font-size: 0.6875rem;
  line-height: 1.5;
}

/* PRAFTA-COM-008-E-5: 기본 근무타입 안내 문구 */
.default-sch-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  line-height: 1.5;
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

/* 네이티브 date input(탈퇴예정일 유지분) + CalendarSrch(입사일/계약종료일 교체분) 공용 사이즈 */
.dialog-date-input {
  flex: 1;
}
.dialog-date-input,
.dialog-date-input :deep(.calendar-input) {
  padding: 0.4rem 0.6rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.dialog-date-input :deep(.calendar-input) {
  width: 100%;
}
.dialog-date-input:focus,
.dialog-date-input :deep(.calendar-input):focus {
  border-color: var(--color-border-strong, #d1d5db);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* PRAFTA-WEB_002-T1-07(1.2): 생성 모드 입사일 — 외곽 래퍼(.calendar-search) 장식 제거하고
   내부 input(.calendar-input)에만 다른 입력과 동일한 스타일을 적용해 단일 깔끔한 캘린더 입력으로 표시.
   (이중 테두리/박스 제거. 본 화면 한정 — 공용 CalendarSrch·타 화면 무영향) */
.hire-date-field {
  flex: 1;
  padding: 0;
  background: transparent;
  border: none;
}
.hire-date-field :deep(.calendar-input) {
  width: 100%;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-strong, #111827);
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
.hire-date-field :deep(.calendar-input):focus {
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
</style>
