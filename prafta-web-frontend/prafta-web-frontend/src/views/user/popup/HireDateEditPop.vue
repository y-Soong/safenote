<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal hire-date-pop"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>입사일 수정</span>
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

        <!-- 🔹 Body -->
        <div class="hire-date-pop__body">
          <!-- 대상자 안내 -->
          <p class="hire-date-pop__desc">
            <strong>{{ userNm }}</strong>
            <span v-if="nodeNm"> · {{ nodeNm }}</span>
            <span v-if="userId"> · {{ userId }}</span>
            <br />
            입사일 변경은 연차 부여, 4대보험 신고, 근속 계산에 영향을 줍니다.
            신중히 검토 후 적용하세요.
          </p>

          <!-- 변경 이력 보기 -->
          <div class="hire-date-pop__history-bar">
            <button
              type="button"
              class="hire-date-pop__history-btn"
              @click="fnOpenHistory"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 3v5h5" />
                <path d="M3.05 13A9 9 0 1 0 6 5.3L3 8" />
                <path d="M12 7v5l3 2" />
              </svg>
              변경 이력 보기
            </button>
          </div>

          <!-- ① 입사일 변경 -->
          <div class="hire-date-pop__section">
            <p class="hire-date-pop__section-title">
              <span class="hire-date-pop__step">1</span>
              입사일 변경
            </p>
            <div class="hire-date-pop__date-row">
              <div class="hire-date-pop__field">
                <label class="hire-date-pop__label">기존 입사일</label>
                <CalendarSrch v-model="prevHireDate" :disabled="true" />
              </div>
              <div class="hire-date-pop__arrow" aria-hidden="true">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </div>
              <div class="hire-date-pop__field">
                <label class="hire-date-pop__label">
                  변경할 입사일<span class="hire-date-pop__required">*</span>
                </label>
                <!-- developer: 초기값은 기존 입사일(prevHireDate)과 동일하게 세팅 -->
                <CalendarSrch v-model="newHireDate" />
              </div>
            </div>
            <!-- 변경 요약 (근속 비교 등) -->
            <div class="hire-date-pop__summary" v-show="changeSummaryText">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
              </svg>
              <!-- developer: changeSummaryText는 prev/new 비교 결과로 채움 -->
              <span>{{ changeSummaryText }}</span>
            </div>
          </div>

          <!-- ② 현재 연차 상태 -->
          <div class="hire-date-pop__section">
            <p class="hire-date-pop__section-title">
              <span class="hire-date-pop__step">2</span>
              현재 연차 상태
              <span
                class="hire-date-pop__scenario"
                v-show="impact.scenarioLabel"
              >
                {{ impact.scenarioLabel }}
              </span>
            </p>

            <!-- 연차 상태 표 (법정 / 법정 외 / 전체) -->
            <div
              class="hire-date-pop__status-loading"
              v-show="leaveStatusLoading"
            >
              연차 상태 조회 중...
            </div>
            <table
              class="hire-date-pop__status-table"
              v-show="!leaveStatusLoading"
            >
              <thead>
                <tr>
                  <th>구분</th>
                  <th class="hire-date-pop__status-num">부여</th>
                  <th class="hire-date-pop__status-num">사용</th>
                  <th class="hire-date-pop__status-num">잔여</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>법정</td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.statutory.grant) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.statutory.used) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.statutory.remain) }}
                  </td>
                </tr>
                <tr>
                  <td>
                    법정 외
                    <span class="hire-date-pop__status-note">표시만</span>
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.agreed.grant) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.agreed.used) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.agreed.remain) }}
                  </td>
                </tr>
                <tr class="hire-date-pop__status-total">
                  <td>전체</td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.total.grant) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.total.used) }}
                  </td>
                  <td class="hire-date-pop__status-num">
                    {{ formatDays(leaveStatus.total.remain) }}
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 영향 분석 (누락된 부여 / 다음 부여 예정 — D1: 옵션 시뮬 제거, 유지 항목만) -->
            <div class="hire-date-pop__impact-loading" v-show="impactLoading">
              영향 분석 중...
            </div>
            <div class="hire-date-pop__impact-grid" v-show="!impactLoading">
              <div
                class="hire-date-pop__impact-card hire-date-pop__impact-card--warn"
              >
                <p class="hire-date-pop__impact-label">
                  누락된 부여 (변경 후 기준)
                </p>
                <p class="hire-date-pop__impact-value">
                  {{ impact.missingGrantText || "-" }}
                </p>
              </div>
              <div
                class="hire-date-pop__impact-card hire-date-pop__impact-card--ok"
              >
                <p class="hire-date-pop__impact-label">다음 부여 예정 시점</p>
                <p class="hire-date-pop__impact-value">
                  {{ impact.nextGrantText || "-" }}
                </p>
              </div>
            </div>

            <!-- FISCAL(회계연도) 정책일 때만: 본연차 다음 회계연도 발생예정 -->
            <div
              class="hire-date-pop__fiscal-note"
              v-show="!impactLoading && impact.fiscalNextGrantText"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              <span>{{ impact.fiscalNextGrantText }}</span>
            </div>

            <p class="hire-date-pop__impact-note">
              ※ 영향 분석은 정책(회계연도/입사일 기준)을 반영한 시뮬레이션입니다.
              실제 연차 부여/회수는 아래 ③ 연차 조정 입력과 변경 적용 시 처리됩니다.
            </p>
          </div>

          <!-- ③ 연차 조정 (선택) -->
          <div class="hire-date-pop__section">
            <p class="hire-date-pop__section-title">
              <span class="hire-date-pop__step">3</span>
              연차 조정 (선택)
            </p>
            <div class="hire-date-pop__adjust-row">
              <span class="hire-date-pop__adjust-label">법정 휴가 부여량</span>
              <span class="hire-date-pop__adjust-current">
                현재 {{ formatDays(leaveStatus.statutory.grant) }}
              </span>
              <span class="hire-date-pop__adjust-arrow" aria-hidden="true"
                >→</span
              >
              <!-- developer: 미입력 시 현재값 유지(차액 0). 0 이상 숫자만 허용 -->
              <input
                class="hire-date-pop__adjust-input"
                type="number"
                min="0"
                step="0.5"
                v-model="targetStatutoryGrantDays"
                placeholder="수정값"
              />
              <span class="hire-date-pop__adjust-unit">일</span>
            </div>

            <!-- 차액 표시 (추가 / 회수 / 변경 없음) -->
            <div
              class="hire-date-pop__diff"
              :class="diffClass"
              v-show="targetStatutoryGrantDays !== '' && targetStatutoryGrantDays !== null"
            >
              {{ diffText }}
            </div>

            <!-- 회수 발생 시(차액 < 0) 회수 사유 필수 -->
            <div class="hire-date-pop__field" v-show="isWithdraw">
              <label class="hire-date-pop__label">
                회수 사유<span class="hire-date-pop__required">*</span>
              </label>
              <textarea
                class="hire-date-pop__textarea"
                v-model="withdrawReason"
                maxlength="500"
                placeholder="연차 회수 사유를 입력하세요. 노무 감사 시 증빙 자료로 사용됩니다."
              ></textarea>
            </div>

            <div
              class="hire-date-pop__warn-box hire-date-pop__warn-box--warn"
              v-show="isWithdraw"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
                />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
              <p class="hire-date-pop__warn-text">
                <strong>회수 안내</strong>: 이미 사용했거나 사용 예정(승인 대기·
                승인완료 미래사용)인 연차는 회수할 수 없습니다. 회수 가능량을
                초과하면 저장이 차단됩니다.
              </p>
            </div>
          </div>

          <!-- ④ 변경 사유 (통합, 필수) -->
          <div class="hire-date-pop__section">
            <p class="hire-date-pop__section-title">
              <span class="hire-date-pop__step">4</span>
              변경 사유<span class="hire-date-pop__required">*</span>
            </p>
            <textarea
              class="hire-date-pop__textarea"
              v-model="changeReason"
              maxlength="1000"
              placeholder="입사일 변경 및 연차 조정 사유를 입력하세요. 노무 감사 시 증빙 자료로 사용됩니다."
            ></textarea>
          </div>

          <div class="hire-date-pop__warn-box hire-date-pop__warn-box--danger">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p class="hire-date-pop__warn-text">
              입사일 변경은 4대보험 신고, 임금 산정, 퇴직금 계산에도 영향을
              줍니다. 단순 경력 인정이 목적이라면 사용자정보 화면의 [경력 인정]
              항목을 사용하세요.
            </p>
          </div>
        </div>

        <!-- 🔹 Footer -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">취소</button>
            <button
              class="btn btn-primary"
              :disabled="!canApply || isSaving"
              @click="fnApplyHireDate"
            >
              변경 적용
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
  reactive,
  computed,
  watch,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import HireDateHistoryPop from "./HireDateHistoryPop.vue";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String, // 회사 코드
  userCd_p: String, // 대상 사용자 코드
  userId_p: String, // 대상 사용자 ID (안내용)
  userNm_p: String, // 대상 사용자명 (안내용)
  nodeNm_p: String, // 소속부서명 (안내용)
  hireDate_p: String, // 기존 입사일 (YYYY-MM-DD 또는 YYYYMMDD)
  onSaved: Function, // 적용 성공 후 부모(UserInfoPop) 갱신 콜백
});

// =========================== Ref ===========================
const modalRef = ref(null);
const cmpnyCd = ref("");
const userCd = ref("");
const userId = ref("");
const userNm = ref("");
const nodeNm = ref("");

// 입사일 변경
const prevHireDate = ref(""); // 기존 입사일 (readonly)
const newHireDate = ref(""); // 변경할 입사일 (초기값 = prevHireDate)
const changeReason = ref(""); // 변경 사유 (필수)

// 연차 조정 (D2: 법정 휴가 목표 부여량만 입력)
const targetStatutoryGrantDays = ref(""); // 수정할 법정 부여량 (미입력 시 현재값 유지)
const withdrawReason = ref(""); // 회수 사유 (회수 발생 시 필수)

// 변경 요약 / 영향 분석 (D1: 옵션 시뮬 제거, 누락/다음 부여/FISCAL만 유지)
const changeSummaryText = ref(""); // developer: prev/new 비교 텍스트
const impactLoading = ref(false);
const impact = reactive({
  scenarioLabel: "", // 예: "1년 미만 · 입사일 과거로"
  missingGrantText: "", // 누락된 부여 (변경 후 기준)
  nextGrantText: "", // 다음 부여 예정 시점
  fiscalNextGrantText: "", // FISCAL 정책 시 "본연차 다음 회계연도 발생 예정", 아니면 ""
});

// 현재 연차 상태 (② 법정/법정 외/전체 × 부여/사용/잔여)
const leaveStatusLoading = ref(false);
const leaveStatus = reactive({
  statutory: { grant: 0, used: 0, remain: 0 },
  agreed: { grant: 0, used: 0, remain: 0 },
  total: { grant: 0, used: 0, remain: 0 },
});

const isSaving = ref(false);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

// 변경 이력 팝업 오픈 (중첩 openPop)
const fnOpenHistory = () => {
  openPop(HireDateHistoryPop, {
    cmpnyCd_p: cmpnyCd.value,
    userCd_p: userCd.value,
    userId_p: userId.value,
    userNm_p: userNm.value,
    nodeNm_p: nodeNm.value,
  });
};

// =========================== Computed ===========================
// 차액 = 목표 법정 부여량 − 현재 법정 부여량 (미입력 시 null → 무처리)
const grantDiff = computed(() => {
  const v = targetStatutoryGrantDays.value;
  if (v === "" || v === null || v === undefined) return null;
  const target = Number(v);
  if (Number.isNaN(target)) return null;
  const current = Number(leaveStatus.statutory.grant) || 0;
  return target - current;
});

// 회수 발생 여부 (차액 < 0)
const isWithdraw = computed(() => grantDiff.value !== null && grantDiff.value < 0);

// 차액 표시 텍스트
const diffText = computed(() => {
  const d = grantDiff.value;
  if (d === null) return "";
  if (d > 0) return `차액: +${d}일 추가 예정`;
  if (d < 0) return `차액: ${d}일 회수 예정`;
  return "차액: 변경 없음";
});

// 차액 표시 색상 클래스
const diffClass = computed(() => {
  const d = grantDiff.value;
  if (d === null) return "";
  if (d > 0) return "hire-date-pop__diff--add";
  if (d < 0) return "hire-date-pop__diff--withdraw";
  return "hire-date-pop__diff--none";
});

// 적용 가능 조건 (단순 form validation — 회수가능량 초과 차단은 서버 검증, D3)
const canApply = computed(() => {
  if (!newHireDate.value) return false;
  if (!changeReason.value || !changeReason.value.trim()) return false;
  // 입사일 미변경 + 연차 조정 없음이면 적용 비활성
  const hireChanged = toYmd(newHireDate.value) !== toYmd(prevHireDate.value);
  const leaveChanged = grantDiff.value !== null && grantDiff.value !== 0;
  if (!hireChanged && !leaveChanged) return false;
  // 회수 발생 시 회수 사유 필수
  if (isWithdraw.value && (!withdrawReason.value || !withdrawReason.value.trim())) {
    return false;
  }
  return true;
});

// =========================== Life Cycle ===========================
onMounted(() => {
  cmpnyCd.value = props.cmpnyCd_p || "";
  userCd.value = props.userCd_p || "";
  userId.value = props.userId_p || "";
  userNm.value = props.userNm_p || "";
  nodeNm.value = props.nodeNm_p || "";
  // 변경할 입사일 초기값 = 기존 입사일
  prevHireDate.value = props.hireDate_p || "";
  newHireDate.value = props.hireDate_p || "";
  // 진입 시 현재 연차 상태 조회 (② 법정/법정 외/전체)
  fnGetLeaveStatus();
  // 입사일이 있으면 영향 분석 1회 시도 (초기값은 prev=new이므로 fnGetImpact 내부에서 스킵·초기화됨)
  if (newHireDate.value) {
    fnGetImpact(newHireDate.value);
  }
});

// =========================== Watch ===========================
// 변경할 입사일이 바뀌면 영향 분석 재요청
watch(newHireDate, (val) => {
  // 입사일 미입력/기존과 동일이면 fnGetImpact 내부에서 영향분석·요약을 초기화, 아니면 재요청
  fnGetImpact(val);
});

// =========================== Methods ===========================
// YYYY-MM-DD(또는 YYYYMMDD) -> YYYYMMDD 정규화
const toYmd = (val) => {
  if (!val) return "";
  return String(val).replace(/-/g, "");
};

// 일수(number) -> "N일" 포맷. 값 없으면 "-", 소수 보존(0.5일 등).
const formatDays = (days) => {
  if (days === null || days === undefined || days === "") return "-";
  const num = Number(days);
  if (Number.isNaN(num)) return "-";
  return `${num}일`;
};

// 현재 연차 상태 조회 (② 법정/법정 외/전체)
const fnGetLeaveStatus = async () => {
  leaveStatusLoading.value = true;
  try {
    // 기존 연차 상세 API 재사용 (Attd_09 / LeaveDashboardServiceImpl.getDetail). 권한(MASTER/HR)은 서버 가드.
    // 응답 키: legalSummary/nonLegalSummary 각각 { granted, used, remaining }. total은 응답에 없으므로 합산.
    const response = await axios.get(
      `/webApi/attd09/leave-dashboard/${encodeURIComponent(userCd.value)}/detail`
    );
    const data = response.data || {};
    const legal = data.legalSummary || {};
    const nonLegal = data.nonLegalSummary || {};

    // 법정 (legalSummary → statutory)
    const sGrant = Number(legal.granted) || 0;
    const sUsed = Number(legal.used) || 0;
    const sRemain = legal.remaining !== undefined && legal.remaining !== null
      ? Number(legal.remaining) || 0
      : sGrant - sUsed;
    leaveStatus.statutory.grant = sGrant;
    leaveStatus.statutory.used = sUsed;
    leaveStatus.statutory.remain = sRemain;

    // 법정 외 / 약정 (nonLegalSummary → agreed, 표시만)
    const aGrant = Number(nonLegal.granted) || 0;
    const aUsed = Number(nonLegal.used) || 0;
    const aRemain = nonLegal.remaining !== undefined && nonLegal.remaining !== null
      ? Number(nonLegal.remaining) || 0
      : aGrant - aUsed;
    leaveStatus.agreed.grant = aGrant;
    leaveStatus.agreed.used = aUsed;
    leaveStatus.agreed.remain = aRemain;

    // 전체 (백엔드 total 필드 없음 → 법정 + 법정 외 합산)
    leaveStatus.total.grant = sGrant + aGrant;
    leaveStatus.total.used = sUsed + aUsed;
    leaveStatus.total.remain = sRemain + aRemain;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "연차 상태 조회 중 오류가 발생했습니다.")
    );
  } finally {
    leaveStatusLoading.value = false;
  }
};

// 입사일 변경 영향 분석 조회 (D1: 누락/다음 부여/FISCAL만)
const fnGetImpact = async (val) => {
  const ymd = toYmd(val);
  if (!ymd || ymd === toYmd(prevHireDate.value)) {
    impact.scenarioLabel = "";
    impact.missingGrantText = "";
    impact.nextGrantText = "";
    impact.fiscalNextGrantText = "";
    changeSummaryText.value = "";
    return;
  }
  impactLoading.value = true;
  try {
    // 영향 분석 (D1: 누락된 부여 / 다음 부여 예정 / FISCAL 다음 회계연도 + 변경 요약). 옵션 시뮬은 폐기됨.
    const response = await axios.get(
      `/webApi/user01/${encodeURIComponent(userCd.value)}/hire-date-impact`,
      { params: { newDate: ymd } }
    );
    const data = response.data || {};
    impact.scenarioLabel = data.scenarioLabel || "";
    impact.missingGrantText = data.missingGrantText || "";
    impact.nextGrantText = data.nextGrantText || "";
    impact.fiscalNextGrantText = data.fiscalNextGrantText || "";
    changeSummaryText.value = data.changeSummaryText || "";
  } catch (err) {
    // 실패 시 직전 분석 결과를 비워 오래된 값이 남지 않게 한다.
    impact.scenarioLabel = "";
    impact.missingGrantText = "";
    impact.nextGrantText = "";
    impact.fiscalNextGrantText = "";
    changeSummaryText.value = "";
    await proxy.$alert(
      resolveApiErrorMessage(err, "영향 분석 중 오류가 발생했습니다.")
    );
  } finally {
    impactLoading.value = false;
  }
};

const fnApplyHireDate = async () => {
  if (!canApply.value || isSaving.value) return;

  const confirmed = await proxy.$confirm(
    "입사일을 변경하시겠습니까? 변경 이력은 영구 보관됩니다."
  );
  if (!confirmed) return;

  isSaving.value = true;
  try {
    // 목표 법정 부여량: 미입력이면 null(무처리), 입력값이 있으면 숫자로 전송.
    const rawTarget = targetStatutoryGrantDays.value;
    const hasTarget = rawTarget !== "" && rawTarget !== null && rawTarget !== undefined;
    const targetDays = hasTarget ? Number(rawTarget) : null;

    // 입사일 변경 + 연차 조정 통합 저장 (cmpnyCd는 서버가 토큰으로 강제하지만 DTO 계약상 전송).
    const payload = {
      cmpnyCd: cmpnyCd.value,
      userCd: userCd.value,
      newHireDate: toYmd(newHireDate.value),
      targetStatutoryGrantDays: targetDays,
      changeReason: changeReason.value ? changeReason.value.trim() : "",
    };
    // 회수 발생(차액 < 0) 시에만 회수 사유 동봉.
    if (isWithdraw.value) {
      payload.withdrawReason = withdrawReason.value ? withdrawReason.value.trim() : "";
    }

    await axios.post("/webApi/user01/update-user-hire-date", payload);

    await proxy.$alert("입사일이 변경되었습니다.");
    // 부모(UserInfoPop) 갱신 콜백 후 모달 닫기.
    if (typeof props.onSaved === "function") props.onSaved();
    emit("close");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "입사일 변경 중 오류가 발생했습니다.")
    );
  } finally {
    isSaving.value = false;
  }
};
</script>

<style scoped>
.hire-date-pop {
  width: 90%;
  max-width: 720px;
  max-height: 90vh;
}

.hire-date-pop__body {
  padding: 1.25rem 1.5rem;
  overflow-y: auto;
  font-family: "Pretendard", sans-serif;
  color: var(--color-text);
}

.hire-date-pop__desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-bottom: 1.25rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg);
  border-radius: var(--input-radius);
  line-height: 1.5;
}
.hire-date-pop__desc strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

/* 변경 이력 보기 버튼 */
.hire-date-pop__history-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: -0.5rem;
  margin-bottom: 1rem;
}
.hire-date-pop__history-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  transition: border-color 0.15s, color 0.15s;
}
.hire-date-pop__history-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.hire-date-pop__history-btn svg {
  width: 14px;
  height: 14px;
}

.hire-date-pop__section {
  margin-bottom: 1.25rem;
}
.hire-date-pop__section:last-child {
  margin-bottom: 0;
}

.hire-date-pop__section-title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin-bottom: 0.625rem;
  padding-bottom: 0.375rem;
  border-bottom: 1px solid var(--color-border);
}

.hire-date-pop__step {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 0.625rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hire-date-pop__required {
  color: var(--color-danger);
  margin-left: 2px;
}

/* 입사일 변경 행 */
.hire-date-pop__date-row {
  display: grid;
  grid-template-columns: 1fr 28px 1fr;
  gap: 0.75rem;
  align-items: end;
}
.hire-date-pop__field {
  display: flex;
  flex-direction: column;
}
.hire-date-pop__label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-bottom: 0.375rem;
  font-weight: 500;
}
.hire-date-pop__arrow {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}
.hire-date-pop__arrow svg {
  width: 18px;
  height: 18px;
}

.hire-date-pop__summary {
  margin-top: 0.75rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  font-size: 0.75rem;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.hire-date-pop__summary svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--color-primary);
}

/* 영향 분석 */
.hire-date-pop__scenario {
  margin-left: 0.375rem;
  padding: 0.1875rem 0.5rem;
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  color: var(--color-primary);
  font-size: 0.6875rem;
  font-weight: 500;
}
.hire-date-pop__impact-loading,
.hire-date-pop__status-loading,
.hire-date-pop__impact-note {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}
.hire-date-pop__impact-note {
  margin-top: 0.5rem;
  line-height: 1.5;
}
.hire-date-pop__impact-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
  margin-top: 0.75rem;
}
.hire-date-pop__impact-card {
  padding: 0.625rem 0.75rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.hire-date-pop__impact-card--warn {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-bg);
}
.hire-date-pop__impact-card--ok {
  background: var(--color-bg);
  border-color: var(--color-primary);
}
.hire-date-pop__impact-label {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin-bottom: 0.25rem;
}
.hire-date-pop__impact-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
}
.hire-date-pop__impact-card--warn .hire-date-pop__impact-value {
  color: var(--color-warning-text);
}
.hire-date-pop__impact-card--ok .hire-date-pop__impact-value {
  color: var(--color-primary);
}

/* 현재 연차 상태 표 */
.hire-date-pop__status-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  overflow: hidden;
  font-size: 0.75rem;
}
.hire-date-pop__status-table th,
.hire-date-pop__status-table td {
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
  color: var(--color-text);
}
.hire-date-pop__status-table thead th {
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-weight: 600;
}
.hire-date-pop__status-table tbody tr:last-child td {
  border-bottom: none;
}
.hire-date-pop__status-num {
  text-align: right;
}
.hire-date-pop__status-total td {
  background: var(--color-bg);
  font-weight: 600;
  color: var(--color-text-strong);
}
.hire-date-pop__status-note {
  margin-left: 0.375rem;
  padding: 0.0625rem 0.375rem;
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  font-size: 0.625rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

/* 연차 조정 입력 행 */
.hire-date-pop__adjust-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.75rem 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.hire-date-pop__adjust-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
}
.hire-date-pop__adjust-current {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
.hire-date-pop__adjust-arrow {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}
.hire-date-pop__adjust-input {
  width: 80px;
  padding: 0.375rem 0.5rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  color: var(--color-text-strong);
  font-size: 0.8125rem;
  font-family: "Pretendard", sans-serif;
  text-align: right;
}
.hire-date-pop__adjust-input:focus {
  border-color: var(--color-border-strong);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.hire-date-pop__adjust-unit {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

/* 차액 표시 */
.hire-date-pop__diff {
  margin-top: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
}
.hire-date-pop__diff--add {
  color: var(--color-primary);
}
.hire-date-pop__diff--withdraw {
  color: var(--color-danger);
}
.hire-date-pop__diff--none {
  color: var(--color-text-muted);
}

/* 경고 박스 */
.hire-date-pop__warn-box {
  display: flex;
  gap: 0.5rem;
  border-radius: var(--input-radius);
  padding: 0.625rem 0.875rem;
  margin-top: 0.75rem;
}
.hire-date-pop__warn-box--warn {
  background: var(--color-warning-bg);
}
.hire-date-pop__warn-box--danger {
  background: rgba(239, 68, 68, 0.12);
}
.hire-date-pop__warn-box svg {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  margin-top: 2px;
}
.hire-date-pop__warn-box--warn svg {
  color: var(--color-warning-text);
}
.hire-date-pop__warn-box--danger svg {
  color: var(--color-danger);
}
.hire-date-pop__warn-text {
  font-size: 0.75rem;
  line-height: 1.6;
}
.hire-date-pop__warn-box--warn .hire-date-pop__warn-text {
  color: var(--color-warning-text);
}
.hire-date-pop__warn-box--danger .hire-date-pop__warn-text {
  color: var(--color-danger);
}
.hire-date-pop__warn-text strong {
  font-weight: 600;
}

/* 변경 사유 / 회수 사유 텍스트영역 */
.hire-date-pop__textarea {
  width: 100%;
  min-height: 80px;
  resize: vertical;
  padding: 0.625rem 0.75rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  color: var(--color-text-strong);
  font-size: 0.8125rem;
  font-family: "Pretendard", sans-serif;
  line-height: 1.5;
}
.hire-date-pop__textarea::placeholder {
  color: var(--color-text-muted);
}
.hire-date-pop__textarea:focus {
  border-color: var(--color-border-strong);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}

/* FISCAL 다음 회계연도 발생예정 줄 */
.hire-date-pop__fiscal-note {
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg);
  border: 1px dashed var(--color-primary);
  border-radius: var(--input-radius);
  font-size: 0.6875rem;
  color: var(--color-primary);
}
.hire-date-pop__fiscal-note svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.modal-footer {
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}
</style>
