<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-wide impact-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header impact-modal-header">
          <span>정책 변경 영향 분석</span>
          <button class="icon-button" type="button" @click="fnClose">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- ============ 바디 ============ -->
        <div class="modal-body impact-analysis">
          <button type="button" class="ia-back-link" @click="fnClose">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <polyline points="15 18 9 12 15 6" />
            </svg>
            연차 정책으로 돌아가기
          </button>

          <p class="ia-desc">
            [연차 부여 정책] 화면에서 설정한 정책 변경이 영향받는 직원과 회사
            부담을 미리 분석합니다. 신중한 결정에 활용하세요.
          </p>

          <!-- ============ 타깃 없음(직접 호출) 안내 ============ -->
          <div v-if="!hasTarget" class="ia-empty-guide">
            <p class="ia-empty-guide__text">
              연차 정책 화면에서 <strong>[분석 실행]</strong>으로 진입해 주세요.
            </p>
            <button class="btn btn-primary" type="button" @click="fnClose">
              연차 정책으로 이동
            </button>
          </div>

          <!-- ============ 정책 요약 + 변경 적용일 + 분석 실행 ============ -->
          <section v-else class="ia-input-card">
            <div class="ia-policy-row">
              <div class="ia-policy-item">
                <p class="ia-policy-label">현재 정책</p>
                <p class="ia-policy-value">{{ currentPolicySummary }}</p>
              </div>
              <span class="ia-policy-arrow" aria-hidden="true">
                <svg
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </span>
              <div class="ia-policy-item">
                <p class="ia-policy-label">변경할 정책</p>
                <p class="ia-policy-value">{{ targetPolicySummary }}</p>
              </div>

              <div class="ia-policy-actions">
                <label class="ia-apply-label">변경 적용일</label>
                <CalendarSrch class="ia-date-input" v-model="applyDate" />
                <button
                  class="btn btn-primary"
                  type="button"
                  :disabled="isLoading"
                  @click="fnAnalyze"
                >
                  분석 실행
                </button>
              </div>
            </div>

            <!-- ===== 변경 사항 상세 토글 (기본 펼침, §9.5.3) ===== -->
            <button
              type="button"
              class="ia-diff-toggle"
              :class="{ 'is-open': diffOpen }"
              @click="fnToggleDiff"
            >
              <span>변경 사항 상세 보기</span>
              <svg
                viewBox="0 0 24 24"
                width="14"
                height="14"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            <!-- diff 패널: Baim_07 UI 7-axis 순서 (가드레일 2). 백엔드 diff[]를 그대로 표시 -->
            <div v-show="diffOpen" class="ia-diff-panel">
              <div
                v-for="item in diffList"
                :key="item.axisNum"
                class="ia-diff-item"
                :class="{ 'is-unchanged': item.changeType === 'UNCHANGED' }"
              >
                <span class="ia-diff-num" aria-hidden="true">{{
                  item.axisNum
                }}</span>
                <div class="ia-diff-content">
                  <p class="ia-diff-axis-title">{{ item.axisName }}</p>

                  <!-- 변경 없음 -->
                  <p
                    v-if="item.changeType === 'UNCHANGED'"
                    class="ia-diff-change ia-diff-unchanged-text"
                  >
                    변경 없음 ·
                    {{ item.note || item.toValue || item.fromValue }}
                  </p>

                  <!-- 변경/비활성/활성 -->
                  <p v-else class="ia-diff-change">
                    <span class="ia-diff-from">{{ item.fromValue }}</span>
                    <svg
                      viewBox="0 0 24 24"
                      width="12"
                      height="12"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <line x1="5" y1="12" x2="19" y2="12" />
                      <polyline points="12 5 19 12 12 19" />
                    </svg>
                    <span
                      class="ia-diff-to"
                      :class="{
                        'is-disabled': item.changeType === 'DEACTIVATED',
                      }"
                    >
                      {{ item.toValue }}
                    </span>
                  </p>
                </div>
              </div>
            </div>
          </section>

          <!-- ============ 근사치 안내 ============ -->
          <div v-if="hasTarget" class="ia-approx-note">
            <svg
              viewBox="0 0 24 24"
              width="13"
              height="13"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span>
              직원별 표·요약은
              <strong>1년치 부여 시뮬레이션 기반 근사치</strong>이며 실제 부여
              결과와 차이가 있을 수 있습니다.
            </span>
          </div>

          <!-- ============ 분석 전 placeholder ============ -->
          <div v-if="hasTarget && !analyzed" class="ia-before-analyze">
            변경 적용일을 입력한 뒤 <strong>[분석 실행]</strong>을 눌러 주세요.
          </div>

          <!-- ============ 요약 카드 4개 ============ -->
          <div v-if="hasTarget && analyzed" class="ia-summary-grid">
            <div class="ia-summary-card">
              <p class="ia-summary-label">전체 직원</p>
              <p class="ia-summary-value">
                {{ summary.totalEmployees
                }}<span class="ia-summary-unit">명</span>
              </p>
            </div>
            <div class="ia-summary-card">
              <p class="ia-summary-label">정상 적용</p>
              <p class="ia-summary-value">
                {{ summary.normalCount }}<span class="ia-summary-unit">명</span>
              </p>
            </div>
            <div class="ia-summary-card is-warning">
              <p class="ia-summary-label">주의 필요</p>
              <p class="ia-summary-value">
                {{ summary.affectedCount
                }}<span class="ia-summary-unit">명</span>
              </p>
            </div>
            <div class="ia-summary-card is-danger">
              <p class="ia-summary-label">추가 부담 합계</p>
              <p class="ia-summary-value">
                {{ summary.additionalDaysTotal
                }}<span class="ia-summary-unit">일</span>
              </p>
            </div>
          </div>

          <!-- ============ 분석 결과 없음 사유 안내 (noResultReason) ============ -->
          <div
            v-if="hasTarget && analyzed && noResultMessage"
            class="ia-no-result"
          >
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span>{{ noResultMessage }}</span>
          </div>

          <!-- ============ 영향받는 직원 테이블 ============ -->
          <section
            v-if="
              hasTarget && analyzed && summary.noResultReason !== 'NO_TARGET'
            "
            class="ia-table-section"
          >
            <p class="ia-section-title">{{ affectedCountLabel }}</p>

            <div class="ia-table-wrap">
              <table class="ia-table">
                <colgroup>
                  <col style="width: 22%" />
                  <col style="width: 13%" />
                  <col style="width: 14%" />
                  <col style="width: 14%" />
                  <col style="width: 14%" />
                  <col style="width: 23%" />
                </colgroup>
                <thead>
                  <tr>
                    <th>직원</th>
                    <th>입사일</th>
                    <th class="is-right">기존 부여</th>
                    <th class="is-right">기존 사용</th>
                    <th class="is-right">예상 추가</th>
                    <th>주요 영향</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="emp in affectedEmployees" :key="emp.userCd">
                    <td>
                      <p class="ia-emp-name">{{ emp.userNm }}</p>
                      <p class="ia-emp-info">
                        {{ emp.deptNm
                        }}<template v-if="emp.positionNm">
                          / {{ emp.positionNm }}</template
                        >
                      </p>
                    </td>
                    <td class="is-secondary">
                      {{ fnFormatDate(emp.hireDate) }}
                    </td>
                    <td class="is-right">{{ emp.currentGrant }}일</td>
                    <td class="is-right">{{ emp.currentUsed }}일</td>
                    <td class="is-right ia-additional">
                      +{{ emp.expectedAdditional }}일
                    </td>
                    <td>{{ emp.mainImpact }}</td>
                  </tr>
                  <!-- empty: 영향 직원 0명 -->
                  <tr v-if="affectedEmployees.length === 0">
                    <td colspan="6" class="ia-table-empty">
                      영향받는 직원이 없습니다.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <!-- ============ 푸터 버튼 ============ -->
        <div class="modal-footer impact-footer">
          <button class="btn btn-second" type="button" @click="fnClose">
            취소
          </button>
          <button
            v-if="hasTarget && analyzed"
            class="btn btn-second"
            type="button"
            @click="fnDownloadReport"
          >
            상세 리포트 다운로드
          </button>
          <button
            v-if="hasTarget && analyzed"
            class="btn btn-primary"
            type="button"
            :disabled="!canProceed || isLoading"
            @click="fnProceedChange"
          >
            정책 변경 진행
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

// ================ Props & Emits ================
// 메인 세션 결정 D-1: 본 영향분석 모달(LeavePolicyImpactPop)은 풀스크린 모달. 타깃 정책은 props로 직접 전달(store/route/TB_MENU 미사용).
// Baim_07 fnGoImpactAnalysis 가 openPop(LeavePolicyImpactPop, { ...타깃 axis, applyFromDate, policySeq }) 로 띄운다.
const props = defineProps({
  // 타깃 정책 axis 조합 (Baim_07 fnBuildSaveRequest 결과와 동일 형태, applyFromDate/changeReason 제외 가능)
  targetPolicy: { type: Object, default: null },
  // 현재 활성 정책 식별자. 존재 시 [정책 변경 진행]은 PUT, 없으면 POST.
  policySeq: { type: [Number, String], default: null },
});
// 'saved': [정책 변경 진행] 성공 시 부모(Baim_07) 재조회 트리거.
//   useModal이 onClose를 덮어쓰므로 재조회 신호는 별도 이벤트로 분리한다.
const emit = defineEmits(["close", "saved"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

// ================ Refs (Variables) ================
// 타깃 axis 조합 (props 복사 — 분석/진행 호출 body의 기준)
const targetPolicy = ref(props.targetPolicy ? { ...props.targetPolicy } : null);

// 정책 한 줄 요약 (현재 / 변경할). 현재 정책 요약은 analyze-impact 응답에서 채움.
const currentPolicySummary = ref("");
const targetPolicySummary = ref("");

// 변경 적용일 (YYYY-MM-DD). 호출 직전 YYYYMMDD 변환
const applyDate = ref("");

// diff 패널 펼침 상태 (기본 펼침, §9.5.3)
const diffOpen = ref(true);

// diff 목록: Baim_07 UI 순서로 백엔드가 정렬해 반환. 프론트는 표시만
const diffList = ref([]);

// 요약 카드 4개
//   noResultReason: 백엔드 신규 필드.
//   "NO_TARGET"(대상 직원 없음=입사일 미입력/비활성) / "NO_ADDITIONAL"(추가 부여 0건) / null(정상)
const summary = ref({
  totalEmployees: 0,
  normalCount: 0,
  affectedCount: 0,
  additionalDaysTotal: 0,
  noResultReason: null,
});

// 영향받는 직원 목록
const affectedEmployees = ref([]);

// 분석 실행 여부 (실행 전엔 요약/표 영역 placeholder)
const analyzed = ref(false);

// 로딩
const isLoading = ref(false);

// ================ Computed ================
// 타깃 정책 존재 여부 (없으면 본문 비활성 + 복귀 안내)
const hasTarget = computed(() => targetPolicy.value != null);

// 직원표 섹션 타이틀
const affectedCountLabel = computed(
  () => `영향받는 직원 (주의 필요 ${summary.value.affectedCount}명)`
);

// 분석 완료 후 "결과 없음" 사유 안내 문구. null이면 정상(표/요약 그대로).
const noResultMessage = computed(() => {
  switch (summary.value.noResultReason) {
    case "NO_TARGET":
      return "분석 대상 직원이 없습니다. (입사일 미입력 또는 비활성 직원 제외)";
    case "NO_ADDITIONAL":
      return "이 정책 변경으로 추가 부여가 발생하는 직원이 없습니다.";
    default:
      return "";
  }
});

// [정책 변경 진행] 활성 조건: 분석 완료 + 변경사항 존재(UNCHANGED 외 diff 1건 이상)
const canProceed = computed(() => {
  if (!analyzed.value) return false;
  return diffList.value.some((d) => d.changeType !== "UNCHANGED");
});

// ================ Life Cycle ================
onMounted(() => {
  fnInitFromProps();
});

// ================ Methods/Functions ================
// props.targetPolicy → 화면 상태 매핑. 없으면 복귀 안내(직접 호출 방어).
const fnInitFromProps = () => {
  if (!targetPolicy.value) {
    proxy.$alert("연차 정책 화면에서 [분석 실행]으로 진입해 주세요.");
    return;
  }
  // 타깃 한 줄 요약은 클라이언트에서 즉시 구성(분석 전에도 표시). 분석 후 서버 요약으로 갱신.
  targetPolicySummary.value = fnBuildPolicySummary(targetPolicy.value);
  // 변경 적용일 기본값 = 오늘(YYYY-MM-DD). 과거 소급 금지(§8.5.8)와 정합하며, 사용자가 바로 분석 실행할 수 있게 한다.
  if (!applyDate.value) {
    applyDate.value = fnTodayYmdDash();
  }
};

// 영향 분석 실행: POST /webApi/baim07/policy/analyze-impact
const fnAnalyze = async () => {
  // 1) applyDate 1차 검증 (오늘 이후, §9.10)
  if (!fnValidateApplyDate()) return;

  // 2) body: 타깃 axis + applyFromDate(YYYYMMDD)
  const body = {
    ...targetPolicy.value,
    applyFromDate: fnToYyyymmdd(applyDate.value),
  };

  isLoading.value = true;
  try {
    const res = await axios.post("/webApi/baim07/policy/analyze-impact", body);
    // 응답: { impact: { summary, diff, affectedEmployees, currentPolicySummary, targetPolicySummary } }
    const impact = res.data?.impact ?? {};
    summary.value = impact.summary ?? summary.value;
    diffList.value = impact.diff ?? [];
    affectedEmployees.value = impact.affectedEmployees ?? [];
    if (impact.currentPolicySummary) {
      currentPolicySummary.value = impact.currentPolicySummary;
    }
    if (impact.targetPolicySummary) {
      targetPolicySummary.value = impact.targetPolicySummary;
    }
    analyzed.value = true;
  } catch (err) {
    // "변경 사항이 없습니다"(400) 등은 resolveApiErrorMessage로 표시
    await proxy.$alert(
      resolveApiErrorMessage(err, "영향 분석 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// 정책 변경 진행: 신규 저장 엔드포인트 금지 → 기존 POST/PUT 재사용
const fnProceedChange = () => {
  if (!canProceed.value) return;
  openPop(ReasonInputModal, {
    title: "정책 변경 사유 입력",
    message: "연차 부여 정책 변경 사유를 입력해 주세요.",
    placeholder: "변경 사유를 입력해 주세요.",
    required: true,
    onConfirm: async (reason) => {
      closePop();
      await fnSubmitChange(reason);
    },
    onCancel: () => {
      closePop();
    },
  });
};

// 실제 저장 (기존 /webApi/baim07/policy 재사용, policySeq 유무로 POST/PUT 분기)
const fnSubmitChange = async (reason) => {
  const body = {
    ...targetPolicy.value,
    applyFromDate: fnToYyyymmdd(applyDate.value),
    changeReason: reason,
  };
  isLoading.value = true;
  try {
    if (props.policySeq != null) {
      await axios.put(`/webApi/baim07/policy/${props.policySeq}`, body);
    } else {
      await axios.post("/webApi/baim07/policy", body);
    }
    await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    // 성공 신호 → Baim_07 재조회(onSaved). 이어서 모달 닫기.
    //   useModal이 onClose를 덮어쓰므로 재조회는 'saved'로 분리 전달한다.
    emit("saved");
    emit("close");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// 상세 리포트 다운로드 (D-3: 화면 데이터 클라이언트 CSV)
const fnDownloadReport = () => {
  if (affectedEmployees.value.length === 0) {
    proxy.$alert("다운로드할 영향 직원 데이터가 없습니다.");
    return;
  }
  const header = [
    "사용자코드",
    "직원",
    "부서",
    "입사일",
    "기존부여(근사)",
    "기존사용(근사)",
    "예상추가(근사)",
    "주요영향",
  ];
  const rows = affectedEmployees.value.map((e) => [
    e.userCd,
    e.userNm,
    e.deptNm,
    fnFormatDate(e.hireDate),
    e.currentGrant,
    e.currentUsed,
    e.expectedAdditional,
    e.mainImpact,
  ]);
  // 근사치 안내 주석 행을 상단에 포함 (D-3 / §13)
  const noteRow = [
    "※ 본 리포트의 부여/사용/추가 수치는 1년치 부여 시뮬레이션 기반 근사치입니다.",
  ];
  const csvBody = [noteRow, header, ...rows]
    .map((cols) => cols.map(fnCsvCell).join(","))
    .join("\r\n");
  // Excel 한글 깨짐 방지용 BOM
  const blob = new Blob(["﻿" + csvBody], {
    type: "text/csv;charset=utf-8;",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `정책변경_영향분석_${fnToYyyymmdd(applyDate.value)}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

// diff 토글
const fnToggleDiff = () => {
  diffOpen.value = !diffOpen.value;
};

// 모달 닫기 (취소 / 연차 정책으로 돌아가기). 부모(Baim_07)가 close에서 재조회.
const fnClose = () => {
  emit("close");
};

// applyDate 1차 검증 (오늘 이후 / 12개월 이내 경고는 백엔드 위임)
const fnValidateApplyDate = () => {
  if (!applyDate.value) {
    proxy.$alert("변경 적용일을 입력해 주세요.");
    return false;
  }
  // 오늘 이전이면 차단 (백엔드 ATTD_400_020 최종 권위, 프론트 1차 게이트)
  const today = fnTodayYyyymmdd();
  if (fnToYyyymmdd(applyDate.value) < today) {
    proxy.$alert("변경 적용일은 오늘 이후로 입력해 주세요.");
    return false;
  }
  return true;
};

// ================ 내부 유틸 ================
// YYYY-MM-DD → YYYYMMDD
const fnToYyyymmdd = (ymd) => String(ymd || "").replace(/-/g, "");
// 오늘 YYYYMMDD
const fnTodayYyyymmdd = () => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
};
// 오늘 YYYY-MM-DD (CalendarSrch v-model 기본값용)
const fnTodayYmdDash = () => {
  const s = fnTodayYyyymmdd();
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
};
// YYYYMMDD → YYYY-MM-DD 표기 (입사일 표시)
const fnFormatDate = (yyyymmdd) => {
  const s = String(yyyymmdd || "");
  if (s.length !== 8) return s || "-";
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
};
// CSV 셀 escape
//   1) CSV Injection 방어: 수식 트리거 문자(= + - @, 탭/CR)로 시작하면 앞에 작은따옴표를 붙여
//      Excel/스프레드시트가 수식으로 평가하지 못하게 중화한다(직원명 등 사용자 입력 기원 값 대응).
//   2) 콤마/따옴표/개행 포함 시 따옴표로 감싸고 내부 따옴표는 두 번으로 escape.
const fnCsvCell = (v) => {
  let s = v == null ? "" : String(v);
  if (/^[=+\-@\t\r]/.test(s)) {
    s = `'${s}`;
  }
  if (/[",\r\n]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
};
// 타깃 axis 조합 → 한 줄 요약 ("회계연도 기준 (비례 부여)" 형태). 분석 전 임시 표시용.
const fnBuildPolicySummary = (policy) => {
  if (!policy) return "";
  const axis1Map = { HIRE_DATE: "입사일 기준", FISCAL_YEAR: "회계연도 기준" };
  const axis3Map = {
    MONTHLY_ONLY: "월차만 부여",
    PRORATE: "비례 부여",
    NEXT_YEAR_BULK: "차년도 일괄 부여",
  };
  const a1 = axis1Map[policy.axis1GrantBase] || policy.axis1GrantBase || "-";
  const a3 =
    axis3Map[policy.axis3FirstYearMethod] || policy.axis3FirstYearMethod || "-";
  return `${a1} (${a3})`;
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.impact-modal {
  width: 100%;
  max-width: 1180px;
}

.impact-modal-header {
  background: rgba(22, 163, 74, 0.08);
}

/* 바디는 modal-popup-guide의 .modal-body 패딩/스크롤을 그대로 사용하고,
   내부 레이아웃만 세로 스택으로 정의 */
.impact-analysis {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* ===== 헤더 (back-link + 설명) ===== */
.ia-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  align-self: flex-start;
}

.ia-back-link:hover {
  color: var(--color-text-strong);
}

.ia-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0 0 0.5rem;
}

/* ===== 타깃 없음 안내 ===== */
.ia-empty-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2.5rem 1rem;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
}

.ia-empty-guide__text {
  font-size: 0.875rem;
  color: var(--color-text);
  margin: 0;
}

.ia-empty-guide__text strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

/* ===== 정책 요약 카드 ===== */
.ia-input-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 1rem 1.25rem;
}

.ia-policy-row {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.ia-policy-item {
  flex: 0 0 auto;
}

.ia-policy-label {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  font-weight: 500;
  letter-spacing: 0.3px;
  margin: 0 0 0.25rem;
}

.ia-policy-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

.ia-policy-arrow {
  color: var(--color-primary);
  display: flex;
  align-items: flex-end;
  padding-bottom: 0.125rem;
}

.ia-policy-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ia-apply-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* 네이티브 date input → CalendarSrch 교체. 내부 input 셀렉터로 스타일 유지 */
.ia-date-input :deep(.calendar-input) {
  height: 2rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 0.625rem;
  font-size: 0.8125rem;
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}

.ia-date-input :deep(.calendar-input):focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* ===== 변경 사항 상세 토글 ===== */
.ia-diff-toggle {
  width: 100%;
  margin-top: 1rem;
  padding-top: 0.875rem;
  border: none;
  border-top: 1px dashed var(--color-border);
  background: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  font-family: "Pretendard", sans-serif;
}

.ia-diff-toggle:hover {
  color: var(--color-text-strong);
}

.ia-diff-toggle svg {
  transition: transform 0.2s ease;
}

.ia-diff-toggle.is-open svg {
  transform: rotate(180deg);
}

/* ===== diff 패널 (2열 그리드) ===== */
.ia-diff-panel {
  margin-top: 0.75rem;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.ia-diff-item {
  display: flex;
  gap: 0.625rem;
  align-items: flex-start;
  padding: 0.625rem 0.75rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.ia-diff-item.is-unchanged {
  background: var(--color-bg);
}

.ia-diff-num {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 0.6875rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 0.0625rem;
}

.ia-diff-item.is-unchanged .ia-diff-num {
  background: var(--color-border-strong);
}

.ia-diff-content {
  flex: 1;
  min-width: 0;
}

.ia-diff-axis-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0 0 0.1875rem;
}

.ia-diff-change {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
  margin: 0;
}

.ia-diff-change svg {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.ia-diff-from {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.ia-diff-to {
  color: var(--color-primary);
  font-weight: 500;
}

.ia-diff-to.is-disabled {
  color: var(--color-text-muted);
  font-style: italic;
  font-weight: 400;
}

.ia-diff-unchanged-text {
  color: var(--color-text-muted);
  font-style: italic;
}

/* ===== 근사치 안내 ===== */
.ia-approx-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-radius: var(--btn-radius);
  padding: 0.5rem 0.75rem;
  font-size: 0.6875rem;
  line-height: 1.5;
}

.ia-approx-note svg {
  flex-shrink: 0;
  margin-top: 0.0625rem;
}

.ia-approx-note strong {
  font-weight: 600;
}

/* ===== 분석 전 placeholder ===== */
.ia-before-analyze {
  padding: 2rem 1rem;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
}

.ia-before-analyze strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

/* ===== 요약 카드 4개 ===== */
.ia-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.625rem;
}

.ia-summary-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.875rem 1rem;
}

.ia-summary-card.is-warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-bg);
}

.ia-summary-card.is-danger {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.2);
}

.ia-summary-label {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0 0 0.25rem;
}

.ia-summary-card.is-warning .ia-summary-label {
  color: var(--color-warning-text);
}

.ia-summary-card.is-danger .ia-summary-label {
  color: var(--color-danger);
}

.ia-summary-value {
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

.ia-summary-card.is-warning .ia-summary-value {
  color: var(--color-warning-text);
}

.ia-summary-card.is-danger .ia-summary-value {
  color: var(--color-danger);
}

.ia-summary-unit {
  font-size: 0.8125rem;
  font-weight: 500;
  margin-left: 0.125rem;
}

/* ===== 직원 테이블 ===== */
.ia-table-section {
  display: flex;
  flex-direction: column;
}

.ia-section-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0 0 0.625rem;
}

.ia-table-wrap {
  overflow-x: auto;
}

.ia-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  table-layout: fixed;
  border-top: 2px solid var(--color-primary);
  min-width: 720px;
}

.ia-table thead {
  background: rgba(22, 163, 74, 0.06);
}

.ia-table th {
  text-align: left;
  padding: 0.625rem 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.ia-table th.is-right {
  text-align: right;
}

.ia-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  color: var(--color-text);
}

.ia-table td.is-right {
  text-align: right;
}

.ia-table td.is-secondary {
  color: var(--color-text-muted);
}

.ia-emp-name {
  margin: 0;
  color: var(--color-text-strong);
  font-weight: 500;
}

.ia-emp-info {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0.125rem 0 0;
}

.ia-additional {
  font-weight: 600;
  color: var(--color-warning-text);
}

.ia-table-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2rem 0.75rem;
}

/* ===== 분석 결과 없음 사유 안내 ===== */
.ia-no-result {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
  padding: 0.875rem 1rem;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--color-text);
}

.ia-no-result svg {
  flex-shrink: 0;
  margin-top: 0.125rem;
  color: var(--color-text-muted);
}

/* ===== 푸터 버튼 ===== */
.impact-footer {
  gap: 0.5rem;
}

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .ia-diff-panel {
    grid-template-columns: 1fr;
  }

  .ia-summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .ia-policy-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .ia-policy-actions {
    margin-left: 0;
    flex-wrap: wrap;
  }
}
</style>
