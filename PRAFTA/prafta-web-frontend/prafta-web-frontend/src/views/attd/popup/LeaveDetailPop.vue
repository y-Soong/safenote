<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-wide leave-detail-modal">
        <!-- ============ 헤더 ============ -->
        <div class="modal-header">
          <span>직원 연차 상세</span>
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
        <div class="modal-body leave-detail">
          <!-- ===== 직원 헤더 ===== -->
          <div class="ldp-header">
            <div class="ldp-header-left">
              <div class="ldp-avatar">{{ avatarText }}</div>
              <div>
                <h2 class="ldp-name">{{ user.userNm }}</h2>
                <p class="ldp-subtitle">
                  {{ user.deptNm }}
                  <template v-if="user.employmentType">
                    · {{ fnEmploymentLabel(user.employmentType) }}
                  </template>
                </p>
              </div>
            </div>
            <div class="ldp-header-actions">
              <button
                class="btn btn-primary"
                type="button"
                @click="fnOpenGrant"
              >
                수동 부여
              </button>
              <!-- 요청서: 기존 [이력 상세] → [새로고침]으로 명칭 변경 + 상세 재조회 -->
              <button class="btn btn-second" type="button" @click="fnReload">
                새로고침
              </button>
            </div>
          </div>

          <!-- ===== 직원 정보 strip (직급 컬럼 제외 — 스키마 미보유) ===== -->
          <div class="ldp-info-strip">
            <div class="ldp-info-item">
              <p class="ldp-info-label">입사일</p>
              <p class="ldp-info-value">{{ fnFormatDate(user.hireDate) }}</p>
            </div>
            <div class="ldp-info-item">
              <p class="ldp-info-label">근속 (실제)</p>
              <p class="ldp-info-value">{{ user.tenureText || "-" }}</p>
            </div>
            <div class="ldp-info-item">
              <p class="ldp-info-label">부여 정책</p>
              <p class="ldp-info-value">{{ user.grantPolicyText || "-" }}</p>
            </div>
            <div class="ldp-info-item">
              <p class="ldp-info-label">다음 부여 예정일</p>
              <p class="ldp-info-value">{{ user.nextGrantDateText || "-" }}</p>
            </div>
          </div>

          <!-- ===== 통계 카드: 법정 휴가 ===== -->
          <!-- LC-09(§5-B): 소수점 노출 금지 — "N일 H시간 M분" 표기(단위 포함 조립) -->
          <div class="ldp-stat-section">
            <p class="ldp-stat-title">법정 휴가</p>
            <div class="ldp-stat-grid">
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">부여</p>
                <p class="ldp-stat-value">
                  {{ fnDays(legalSummary.granted) }}
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">사용</p>
                <p class="ldp-stat-value">
                  {{ fnDays(legalSummary.used) }}
                </p>
                <!-- 시간차 사용 원본 분 병기(LEAVE_MINUTES 합계, §5-B) -->
                <p v-if="hourlyUsedMinutes > 0" class="ldp-stat-sub">
                  시간차 사용 {{ fnMinutes(hourlyUsedMinutes) }} 포함
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">잔여</p>
                <p class="ldp-stat-value is-accent">
                  {{ fnDays(legalSummary.remaining) }}
                </p>
                <p v-if="legalSummary.expiresAt" class="ldp-stat-sub">
                  {{ fnFormatDate(legalSummary.expiresAt) }} 만료
                </p>
              </div>
            </div>
          </div>

          <!-- ===== 통계 카드: 법정 휴가 외 ===== -->
          <div class="ldp-stat-section ldp-stat-section--alt">
            <p class="ldp-stat-title">법정 휴가 외</p>
            <div class="ldp-stat-grid">
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">부여</p>
                <p class="ldp-stat-value">
                  {{ fnDays(nonLegalSummary.granted) }}
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">사용</p>
                <p class="ldp-stat-value">
                  {{ fnDays(nonLegalSummary.used) }}
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">잔여</p>
                <p class="ldp-stat-value">
                  {{ fnDays(nonLegalSummary.remaining) }}
                </p>
              </div>
            </div>
          </div>

          <!-- ===== 통계 카드: 신청형 휴가 (LEAVE_TYPE='01', 법정/법정외와 합산하지 않는 별도 섹션) ===== -->
          <div
            v-if="appliedLeaveTypes.length > 0"
            class="ldp-stat-section ldp-stat-section--applied"
          >
            <p class="ldp-stat-title">신청형 휴가</p>
            <div class="ldp-applied-grid">
              <div
                v-for="(t, idx) in appliedLeaveTypes"
                :key="idx"
                class="ldp-stat-card"
              >
                <p class="ldp-stat-label">{{ t.leaveNm }}</p>
                <p class="ldp-stat-value is-accent">
                  {{ fnDays(t.remainDays) }}
                </p>
                <p class="ldp-stat-sub">한도 {{ fnDays(t.maxAplyDays) }}</p>
              </div>
            </div>
          </div>

          <!-- ===== 부여 이력 ===== -->
          <p class="ldp-section-title">부여 이력</p>
          <div class="ldp-table-wrap">
            <table class="ldp-table">
              <colgroup>
                <col style="width: 13%" />
                <col style="width: 9%" />
                <col style="width: 16%" />
                <col style="width: 9%" />
                <col style="width: 9%" />
                <col style="width: 9%" />
                <col style="width: 12%" />
                <col style="width: 12%" />
                <col style="width: 11%" />
              </colgroup>
              <thead>
                <tr>
                  <th>부여일</th>
                  <th class="is-center">구분</th>
                  <th>사유</th>
                  <th class="is-right">부여</th>
                  <th class="is-right">사용</th>
                  <th class="is-right">잔여</th>
                  <th>만료일</th>
                  <th class="is-center">상태</th>
                  <th class="is-center">관리</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="g in pagedHistory" :key="g.grantId">
                  <td>{{ fnFormatDate(g.grantDate) }}</td>
                  <td class="is-center">
                    <span
                      class="ldp-grant-tag"
                      :class="
                        g.natureBadge === 'LEGAL' ? 'is-legal' : 'is-non-legal'
                      "
                    >
                      {{ g.natureBadge === "LEGAL" ? "법정" : "법정 외" }}
                    </span>
                  </td>
                  <td>{{ g.reason || "-" }}</td>
                  <td class="is-right">{{ fnDays(g.granted) }}</td>
                  <td class="is-right">{{ fnDays(g.used) }}</td>
                  <td class="is-right ldp-strong">{{ fnDays(g.remaining) }}</td>
                  <td class="is-secondary">{{ fnFormatDate(g.expiresAt) }}</td>
                  <td class="is-center">
                    <span
                      class="ldp-status-badge"
                      :class="fnStatusClass(g.status)"
                    >
                      {{ fnStatusLabel(g.status) }}
                    </span>
                  </td>
                  <td class="is-center">
                    <button
                      v-if="g.canRecall"
                      type="button"
                      class="ldp-recall-btn"
                      @click="fnOpenRecall(g)"
                    >
                      회수
                    </button>
                    <span v-else class="ldp-recall-na">-</span>
                  </td>
                </tr>

                <!-- empty -->
                <tr v-if="grantHistory.length === 0">
                  <td colspan="9" class="ldp-table-empty">
                    부여 이력이 없습니다.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 부여 이력 페이징 (클라이언트 사이드, 상세는 전량 조회 → 프론트 분할, pageSize=5) -->
          <div v-if="grantHistory.length > 0" class="ldp-pager">
            <button
              type="button"
              class="btn btn-second"
              :disabled="page <= 1"
              @click="fnGoPage(page - 1)"
            >
              이전
            </button>
            <span class="ldp-pager-info">
              {{ page }} / {{ totalPages }} (총 {{ grantHistory.length }}건)
            </span>
            <button
              type="button"
              class="btn btn-second"
              :disabled="page >= totalPages"
              @click="fnGoPage(page + 1)"
            >
              다음
            </button>
          </div>

          <!-- ===== 사용 이력 (연도별) ===== -->
          <div class="ldp-usage-header">
            <p class="ldp-section-title">사용 이력</p>
            <div class="ldp-usage-year-select">
              <BaseSelect v-model="usageYear">
                <option v-for="y in usageYearOptions" :key="y" :value="y">
                  {{ y }}년
                </option>
              </BaseSelect>
            </div>
          </div>
          <div class="ldp-table-wrap">
            <table class="ldp-table">
              <colgroup>
                <col style="width: 18%" />
                <col style="width: 30%" />
                <col style="width: 16%" />
                <col style="width: 16%" />
                <col style="width: 20%" />
              </colgroup>
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>연차 종류</th>
                  <th class="is-right">일수</th>
                  <th class="is-center">상태</th>
                  <th class="is-center">증빙</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(h, idx) in pagedUsageHistory" :key="idx">
                  <td>{{ fnFormatDate(h.dateYmd) }}</td>
                  <td>{{ h.leaveNm || "-" }}</td>
                  <td class="is-right">{{ fnDays(h.leaveDays) }}</td>
                  <td class="is-center">
                    <span
                      class="ldp-status-badge"
                      :class="
                        h.status === 'USED' ? 'is-active' : 'is-exhausted'
                      "
                    >
                      {{ h.status === "USED" ? "사용" : "사용예정" }}
                    </span>
                  </td>
                  <td class="is-center">
                    <!-- 증빙 첨부 건만 노출. 보기=이미지 오버레이/PDF 새 탭(그 외 저장 폴백), 저장=다운로드 -->
                    <template v-if="h.evidenceFileId">
                      <button
                        type="button"
                        class="ldp-evid-btn"
                        :disabled="evidenceBusy"
                        @click="fnViewEvidence(h)"
                      >
                        보기
                      </button>
                      <button
                        type="button"
                        class="ldp-evid-btn"
                        :disabled="evidenceBusy"
                        @click="fnDownloadEvidence(h)"
                      >
                        저장
                      </button>
                    </template>
                    <span v-else class="ldp-recall-na">-</span>
                  </td>
                </tr>

                <!-- empty -->
                <tr v-if="usageHistory.length === 0">
                  <td colspan="5" class="ldp-table-empty">
                    {{ usageYear }}년 사용 이력이 없습니다.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 사용 이력 페이징 (클라이언트 사이드, 연도 전량 조회 → 프론트 분할, pageSize=10) -->
          <div v-if="usageHistory.length > 0" class="ldp-pager">
            <button
              type="button"
              class="btn btn-second"
              :disabled="usagePage <= 1"
              @click="fnGoUsagePage(usagePage - 1)"
            >
              이전
            </button>
            <span class="ldp-pager-info">
              {{ usagePage }} / {{ totalUsagePages }} (총
              {{ usageHistory.length }}건)
            </span>
            <button
              type="button"
              class="btn btn-second"
              :disabled="usagePage >= totalUsagePages"
              @click="fnGoUsagePage(usagePage + 1)"
            >
              다음
            </button>
          </div>
        </div>

        <!-- 증빙 이미지 뷰어 오버레이 (클릭 시 닫기 — Attd_10 패턴) -->
        <div
          v-if="evidenceViewerSrc"
          class="ldp-evid-viewer"
          @click="fnCloseEvidenceViewer"
        >
          <img :src="evidenceViewerSrc" alt="증빙 자료 원본" />
        </div>

        <!-- ============ 푸터 ============ -->
        <div class="modal-footer">
          <button class="btn btn-second" type="button" @click="fnClose">
            닫기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  computed,
  watch,
  onMounted,
  onUnmounted,
  getCurrentInstance,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import {
  resolveApiErrorMessage,
  resolveBlobApiErrorMessage,
} from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
// 2026-08-09 규약: 일수 표기는 일 단위 단독(formatLeaveDaysOnly) — E4 분모 환산 제거.
import { formatLeaveDaysOnly, formatLeaveMinutes } from "@/utils/leaveFormat";
import BaseSelect from "@/components/common/BaseSelect.vue";
import ManualGrantPop from "./ManualGrantPop.vue";
import LeaveRecallPop from "./LeaveRecallPop.vue";

// ================ Props & Emits ================
const props = defineProps({
  // 대상 직원 코드 (대시보드 행 [>]에서 전달)
  userCd: { type: [String, Number], default: "" },
  // 수동 부여 성공 시 부모(대시보드) 재조회 신호 콜백
  //   (useModal이 onClose를 덮어쓰므로 재조회는 별도 콜백 prop으로 받는다)
  onGranted: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
// 중첩 모달용 — 본 컴포넌트 인스턴스 전용 useModal closure (대시보드 인스턴스와 독립)
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const user = ref({
  userCd: props.userCd,
  userNm: "",
  deptNm: "",
  employmentType: "",
  hireDate: "",
  tenureText: "",
  grantPolicyText: "",
  nextGrantDateText: "",
});

const legalSummary = ref({ granted: 0, used: 0, remaining: 0, expiresAt: "" });
const nonLegalSummary = ref({ granted: 0, used: 0, remaining: 0 });

// LC-09(§5-B): 시간차 CONFIRMED 사용 분 합계(원본 병기용 — 실사용 정확값이라 유지).
//   2026-08-09 규약: 구 환산 분모 ref(convMinutes)는 일 단위 단독 표기 전환으로 소비처 소멸 → 제거
//   (응답 convMinutes 필드는 구버전 호환으로 서버에 잔존 — FE 미사용).
const hourlyUsedMinutes = ref(0);

// 신청형 휴가(LEAVE_TYPE='01') 타입별 잔여 현황 — 법정/법정외와 합산하지 않는 별도 섹션.
//   각 항목: { leaveCd, leaveNm, maxAplyDays(한도), usedDays(사용), remainDays(잔여) } — 모두 서버 권위값.
const appliedLeaveTypes = ref([]);

// 부여 이력 (GRANT_DATE 내림차순; 백엔드 정렬)
//   각 행: { grantDate, natureBadge('LEGAL'|'NON_LEGAL'), reason, granted, used, remaining, expiresAt, status }
const grantHistory = ref([]);

// 부여 이력 클라이언트 사이드 페이징 (상세 API가 전량을 내려주므로 프론트에서 분할)
const page = ref(1);
const pageSize = ref(5);

// 사용 이력(연도별) — 각 행: { dateYmd, leaveCd, leaveNm, leaveDays, status('USED'|'SCHEDULED') }
const usageHistory = ref([]);
// 조회 연도(YYYY 문자열, BaseSelect option value와 타입 일치). 기본값=올해.
const currentYear = new Date().getFullYear();
const usageYear = ref(String(currentYear));
// 연도 선택지: 최근 5년 전 ~ 내년(스케줄 조회 여유분), 최신순.
const usageYearOptions = Array.from({ length: 7 }, (_, i) =>
  String(currentYear + 1 - i)
);
// 사용 이력 클라이언트 사이드 페이징 (연도 전량 조회 → 프론트에서 분할)
const usagePage = ref(1);
const usagePageSize = ref(10);

// 증빙 파일 뷰어/다운로드 상태 (연차 신청 증빙 필수화 2026-08-29)
const evidenceBusy = ref(false); // 로드 중 버튼 연타 방지
const evidenceViewerSrc = ref(""); // 이미지 오버레이 objectURL ('' = 닫힘)

const isLoading = ref(false);

// ================ Computed ================
// 아바타 표기 (이름 앞 2글자)
const avatarText = computed(() => {
  const nm = user.value.userNm || "";
  return nm.slice(0, 2) || "-";
});

// 부여 이력 총 페이지 수 (최소 1)
const totalPages = computed(() =>
  Math.max(1, Math.ceil(grantHistory.value.length / pageSize.value))
);

// 현재 페이지에 해당하는 부여 이력 슬라이스
const pagedHistory = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return grantHistory.value.slice(start, start + pageSize.value);
});

// 사용 이력 총 페이지 수 (최소 1)
const totalUsagePages = computed(() =>
  Math.max(1, Math.ceil(usageHistory.value.length / usagePageSize.value))
);

// 현재 페이지에 해당하는 사용 이력 슬라이스
const pagedUsageHistory = computed(() => {
  const start = (usagePage.value - 1) * usagePageSize.value;
  return usageHistory.value.slice(start, start + usagePageSize.value);
});

// ================ Life Cycle Functions ================
onMounted(() => {
  fnLoadDetail();
  fnLoadUsageHistory();
});

// 조회 연도 변경 시 사용 이력 재조회 (BaseSelect v-model 갱신 이후 시점 보장을 위해 watch 사용)
watch(usageYear, () => {
  fnLoadUsageHistory();
});

// 팝업 종료 시 증빙 오버레이 objectURL 누수 방지
onUnmounted(() => {
  if (evidenceViewerSrc.value) URL.revokeObjectURL(evidenceViewerSrc.value);
});

// ================ API Functions ================
// 상세 조회 (최초 진입 + [새로고침] 공통)
const fnLoadDetail = async () => {
  const targetUserCd = user.value.userCd ?? props.userCd;
  if (!targetUserCd) {
    proxy.$alert("대상 직원 정보가 없습니다.");
    return;
  }
  isLoading.value = true;
  try {
    const response = await axios.get(
      `/webApi/attd09/leave-dashboard/${encodeURIComponent(targetUserCd)}/detail`
    );
    const data = response.data || {};

    user.value = {
      userCd: data.user?.userCd ?? targetUserCd,
      userNm: data.user?.userNm ?? "",
      deptNm: data.user?.deptNm ?? "",
      employmentType: data.user?.employmentType ?? "",
      hireDate: data.user?.hireDate ?? "",
      tenureText: data.user?.tenureText ?? "",
      grantPolicyText: data.user?.grantPolicyText ?? "",
      nextGrantDateText: data.user?.nextGrantDateText ?? "",
    };
    legalSummary.value = {
      granted: data.legalSummary?.granted ?? 0,
      used: data.legalSummary?.used ?? 0,
      remaining: data.legalSummary?.remaining ?? 0,
      expiresAt: data.legalSummary?.expiresAt ?? "",
    };
    nonLegalSummary.value = {
      granted: data.nonLegalSummary?.granted ?? 0,
      used: data.nonLegalSummary?.used ?? 0,
      remaining: data.nonLegalSummary?.remaining ?? 0,
    };
    // LC-09: 시간차 원본 분 합계(응답 convMinutes 는 2026-08-09 규약으로 미소비)
    hourlyUsedMinutes.value = data.hourlyUsedMinutes ?? 0;
    // 신청형 휴가: 서버 산출값(한도/사용/잔여)을 그대로 렌더(프론트 재계산 금지).
    //   한도(maxAplyDays)가 null로 내려오면 표기 안정성을 위해 0으로만 폴백(잔여는 서버값 유지).
    appliedLeaveTypes.value = Array.isArray(data.appliedLeaveTypes)
      ? data.appliedLeaveTypes.map((t) => ({
          leaveCd: t.leaveCd ?? "",
          leaveNm: t.leaveNm ?? "",
          maxAplyDays: t.maxAplyDays ?? 0,
          usedDays: t.usedDays ?? 0,
          remainDays: t.remainDays ?? 0,
        }))
      : [];
    grantHistory.value = Array.isArray(data.grantHistory)
      ? data.grantHistory
      : [];
    // 재조회(최초/새로고침/부여/회수 후) 시 항상 1페이지부터 표시
    page.value = 1;
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "상세 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// [새로고침] — 상세 데이터 재조회 (요청서: 기존 [이력 상세] 명칭/동작 변경)
const fnReload = () => {
  fnLoadDetail();
  fnLoadUsageHistory();
};

// 연도별 사용 이력 조회 (연도 변경 시 / 새로고침 시 공통)
const fnLoadUsageHistory = async () => {
  const targetUserCd = user.value.userCd ?? props.userCd;
  if (!targetUserCd) return;
  try {
    const response = await axios.get(
      `/webApi/attd09/leave-dashboard/${encodeURIComponent(targetUserCd)}/usage-history`,
      { params: { year: usageYear.value } }
    );
    usageHistory.value = Array.isArray(response.data?.usageHistory)
      ? response.data.usageHistory
      : [];
    // 연도 변경/재조회 시 항상 1페이지부터 표시
    usagePage.value = 1;
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "사용 이력 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

// 부여 이력 페이지 이동 (범위 가드)
const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
};

// 사용 이력 페이지 이동 (범위 가드)
const fnGoUsagePage = (target) => {
  if (target < 1 || target > totalUsagePages.value) return;
  usagePage.value = target;
};

// ── 증빙 파일 보기/저장 (연차 신청 증빙 필수화 2026-08-29) ──────────
// 서버 화이트리스트상 올 수 있는 형식: 이미지/PDF/텍스트/동영상/음성.
// 보기 = 이미지는 오버레이, PDF는 새 탭(브라우저 내장 뷰어), 그 외는 저장 폴백.
const fnLoadEvidenceBlob = async (fileMgmtCd) => {
  const targetUserCd = user.value.userCd ?? props.userCd;
  const response = await axios.get(
    `/webApi/attd09/leave-dashboard/${encodeURIComponent(targetUserCd)}/evidence-file/${encodeURIComponent(fileMgmtCd)}`,
    { responseType: "blob" }
  );
  return response.data;
};

// blob MIME → 저장 파일 확장자 (미지 형식은 bin 폴백 — 서버 화이트리스트상 발생 희박)
const fnEvidenceExt = (mime) => {
  const map = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/gif": "gif",
    "image/bmp": "bmp",
    "image/webp": "webp",
    "image/tiff": "tif",
    "application/pdf": "pdf",
    "text/plain": "txt",
    "text/csv": "csv",
  };
  if (map[mime]) return map[mime];
  const sub = String(mime || "").split("/")[1];
  return sub || "bin";
};

const fnDownloadBlob = (blob, fileMgmtCd) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `증빙_${user.value.userNm || props.userCd}_${fileMgmtCd}.${fnEvidenceExt(blob.type)}`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  // click 처리 이후 해제(즉시 revoke 하면 일부 브라우저에서 다운로드가 끊김)
  setTimeout(() => URL.revokeObjectURL(url), 10000);
};

const fnViewEvidence = async (h) => {
  if (evidenceBusy.value) return;
  evidenceBusy.value = true;
  try {
    const blob = await fnLoadEvidenceBlob(h.evidenceFileId);
    if (blob.type.startsWith("image/")) {
      // 이전 오버레이 URL 정리 후 교체
      if (evidenceViewerSrc.value) URL.revokeObjectURL(evidenceViewerSrc.value);
      evidenceViewerSrc.value = URL.createObjectURL(blob);
    } else if (blob.type === "application/pdf") {
      // 새 탭에서 브라우저 내장 PDF 뷰어로 열기 (탭이 로드된 뒤 해제)
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } else {
      // 미리보기 미지원 형식(동영상/음성/텍스트 등) — 저장으로 폴백
      fnDownloadBlob(blob, h.evidenceFileId);
    }
  } catch (err) {
    // responseType:blob 요청의 에러 본문은 blob 으로 오므로 blob 전용 해석기 사용(운영 실장애 교훈)
    const msg = await resolveBlobApiErrorMessage(
      err,
      "증빙 자료를 불러오는 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    evidenceBusy.value = false;
  }
};

const fnDownloadEvidence = async (h) => {
  if (evidenceBusy.value) return;
  evidenceBusy.value = true;
  try {
    const blob = await fnLoadEvidenceBlob(h.evidenceFileId);
    fnDownloadBlob(blob, h.evidenceFileId);
  } catch (err) {
    const msg = await resolveBlobApiErrorMessage(
      err,
      "증빙 자료를 내려받는 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    evidenceBusy.value = false;
  }
};

const fnCloseEvidenceViewer = () => {
  if (evidenceViewerSrc.value) URL.revokeObjectURL(evidenceViewerSrc.value);
  evidenceViewerSrc.value = "";
};

// ================ Methods/Functions ================
// 수동 부여 모달 열기 (상세 위에 중첩) — 단일 대상
const fnOpenGrant = () => {
  openPop(ManualGrantPop, {
    targetUsers: [
      {
        userCd: user.value.userCd,
        userNm: user.value.userNm,
        deptNm: user.value.deptNm,
      },
    ],
    onGranted: () => {
      // 부여 성공 → 상세 재조회 + 부모(대시보드)에도 신호 전파
      fnLoadDetail();
      if (typeof props.onGranted === "function") props.onGranted();
    },
  });
};

// 부여 연차 회수 모달 열기 (상세 위에 중첩) — 회수 가능 행(g.canRecall)만 진입
const fnOpenRecall = (g) => {
  openPop(LeaveRecallPop, {
    grant: {
      grantId: g.grantId,
      userNm: user.value.userNm,
      grantDate: g.grantDate,
      granted: g.granted,
      used: g.used,
    },
    onRecalled: () => {
      // 회수 성공 → 상세 재조회 + 부모(대시보드)에도 신호 전파
      fnLoadDetail();
      if (typeof props.onGranted === "function") props.onGranted();
    },
  });
};

// 모달 닫기
const fnClose = () => {
  emit("close");
};

// --- 고용형태 라벨 ---
const fnEmploymentLabel = (type) => {
  const map = {
    REGULAR: "정규직",
    CONTRACT: "계약직",
    DAILY: "일용직",
    EXECUTIVE: "임원",
  };
  return map[type] || type || "-";
};

// --- 상태 라벨/클래스 ---
const fnStatusLabel = (status) => {
  const map = {
    ACTIVE: "사용중",
    EXHAUSTED: "소진완료",
    EXPIRED: "만료",
    CANCELED: "취소됨",
  };
  return map[status] || status || "-";
};

const fnStatusClass = (status) => {
  switch (status) {
    case "ACTIVE":
      return "is-active";
    case "EXHAUSTED":
      return "is-exhausted";
    case "EXPIRED":
      return "is-expired";
    case "CANCELED":
      return "is-canceled";
    default:
      return "is-exhausted";
  }
};

// ================ 내부 유틸 ================
// YYYYMMDD → "YYYY.MM.DD" 표기. 빈값/형식불충분은 "-".
const fnFormatDate = (yyyymmdd) => {
  const s = String(yyyymmdd || "");
  if (s.length !== 8) return s || "-";
  return formatYmdDot(s);
};

// 2026-08-09 규약: 일수 표기 — 일 단위 단독(formatLeaveDaysOnly, 2자리 반올림 trim).
//   구 "N일 H시간 M분"(E4 분모 환산)은 실차감 분모(E1)와 편차가 있어 표기 폐지.
const fnDays = (v) => formatLeaveDaysOnly(v);

// LC-09(§5-B): 분 → "H시간 M분" (시간차 LEAVE_MINUTES 원본 병기)
const fnMinutes = (v) => formatLeaveMinutes(v);
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-detail-modal {
  width: 100%;
  max-width: 1080px;
}

.leave-detail {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  /* com-013-08-2: 부여 이력 영역 잘림 수정 (popup-layout-containment 규약).
     .modal-body(flex:1 1 auto; overflow-y:auto; min-height:0)의 자식 flex 컬럼이
     min-height 기본값(auto)으로 인해 축소되지 않아 마지막 섹션(부여 이력)이 잘리는 것을 방지한다.
     - min-height:0 : 부모 스크롤 컨테이너가 정상 축소/스크롤되도록 허용.
     - flex:1 1 auto : 본문 가용 세로 공간을 채움(짧은 내용일 때 레이아웃 안정). */
  flex: 1 1 auto;
  min-height: 0;
}

/* ===== 직원 헤더 ===== */
.ldp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.ldp-header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.ldp-avatar {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.ldp-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0 0 0.125rem;
}

.ldp-subtitle {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0;
}

.ldp-header-actions {
  display: flex;
  gap: 0.375rem;
  flex-shrink: 0;
}

/* ===== 정보 strip ===== */
.ldp-info-strip {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.875rem 1.125rem;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.875rem;
}

.ldp-info-label {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0 0 0.125rem;
}

.ldp-info-value {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

/* ===== 통계 섹션 ===== */
.ldp-stat-section {
  display: flex;
  flex-direction: column;
}

.ldp-stat-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  margin: 0 0 0.5rem;
  padding-left: 0.5rem;
  border-left: 3px solid var(--color-primary);
}

.ldp-stat-section--alt .ldp-stat-title {
  border-left-color: var(--color-primary-pressed);
}

.ldp-stat-section--applied .ldp-stat-title {
  border-left-color: var(--color-primary);
}

/* 신청형 휴가: 타입 개수가 가변이라 auto-fill 그리드(법정/법정외 3분할 카드와 동일 스타일 재사용) */
.ldp-applied-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(11rem, 1fr));
  gap: 0.625rem;
}

.ldp-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.625rem;
}

.ldp-stat-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 1rem 1.125rem;
}

.ldp-stat-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0 0 0.375rem;
}

.ldp-stat-value {
  font-size: 1.625rem;
  font-weight: 600;
  color: var(--color-text-strong);
  line-height: 1.1;
  margin: 0 0 0.375rem;
}

.ldp-stat-value.is-accent {
  color: var(--color-primary);
}

.ldp-stat-unit {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  font-weight: 500;
  margin-left: 0.125rem;
}

.ldp-stat-sub {
  font-size: 0.625rem;
  color: var(--color-text-muted);
  margin: 0;
}

/* ===== 부여 이력 테이블 ===== */
.ldp-section-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

/* ===== 사용 이력 헤더(제목 + 연도 선택) ===== */
.ldp-usage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.ldp-usage-year-select {
  width: 6.5rem;
  flex-shrink: 0;
}

.ldp-table-wrap {
  overflow-x: auto;
  /* com-013-08-2 후속: 부여 이력(마지막 섹션) 세로 잘림 수정.
     overflow-x:auto 로 이 래퍼가 스크롤 컨테이너가 되면, flex 컬럼(.leave-detail) 안에서
     flex 아이템의 자동 최소높이(min-height:auto)가 0으로 계산돼(CSS Flexbox 사양) 세로로 찌그러진다.
     flex-shrink:0 으로 내용(테이블) 높이를 그대로 유지하고, 세로 스크롤은 부모(.leave-detail)가 담당한다. */
  flex-shrink: 0;
}

.ldp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  table-layout: fixed;
  border-top: 2px solid var(--color-primary);
  min-width: 780px;
}

.ldp-table thead {
  background: rgba(22, 163, 74, 0.06);
}

.ldp-table th {
  text-align: left;
  padding: 0.625rem 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.ldp-table th.is-right {
  text-align: right;
}

.ldp-table th.is-center {
  text-align: center;
}

.ldp-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  color: var(--color-text);
}

.ldp-table td.is-right {
  text-align: right;
}

.ldp-table td.is-center {
  text-align: center;
}

.ldp-table td.is-secondary {
  color: var(--color-text-muted);
}

.ldp-strong {
  font-weight: 600;
  color: var(--color-text-strong);
}

.ldp-table-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2rem 0.75rem;
}

/* ===== 배지 ===== */
.ldp-grant-tag {
  font-size: 0.625rem;
  padding: 0.125rem 0.375rem;
  border-radius: var(--btn-radius);
  font-weight: 500;
}

.ldp-grant-tag.is-legal {
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary-pressed);
}

.ldp-grant-tag.is-non-legal {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

.ldp-status-badge {
  font-size: 0.625rem;
  padding: 0.125rem 0.5rem;
  border-radius: var(--btn-radius);
  font-weight: 500;
}

.ldp-status-badge.is-active {
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary-pressed);
}

.ldp-status-badge.is-exhausted {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

.ldp-status-badge.is-expired {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

.ldp-status-badge.is-canceled {
  background: rgba(239, 68, 68, 0.08);
  color: var(--color-danger);
}

/* ===== 회수 버튼 (관리 컬럼) ===== */
.ldp-recall-btn {
  font-size: 0.6875rem;
  padding: 0.1875rem 0.5rem;
  border-radius: var(--btn-radius);
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  background: var(--color-surface);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}

.ldp-recall-btn:hover {
  background: rgba(239, 68, 68, 0.08);
}

.ldp-recall-na {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

/* ===== 사용 이력 증빙 보기/저장 버튼 (연차 신청 증빙 필수화 2026-08-29) ===== */
.ldp-evid-btn {
  font-size: 0.6875rem;
  padding: 0.1875rem 0.5rem;
  border-radius: var(--btn-radius);
  border: 1px solid var(--color-border-strong);
  color: var(--color-text);
  background: var(--color-surface);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}

.ldp-evid-btn + .ldp-evid-btn {
  margin-left: 0.25rem;
}

.ldp-evid-btn:hover {
  background: var(--color-bg);
}

.ldp-evid-btn:disabled {
  opacity: 0.5;
  cursor: progress;
}

/* 증빙 이미지 뷰어 오버레이 — 클릭 시 닫기 (Attd_10 패턴) */
.ldp-evid-viewer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}

.ldp-evid-viewer img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 4px;
}

/* ===== 부여 이력 페이저 ===== */
.ldp-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
  /* flex 컬럼(.leave-detail) 안에서 찌그러지지 않도록 고정 */
  flex-shrink: 0;
}

.ldp-pager-info {
  font-size: var(--btn-font, 0.6875rem);
  color: var(--color-text-muted);
}

.ldp-pager .btn {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .ldp-info-strip {
    grid-template-columns: repeat(2, 1fr);
  }

  .ldp-stat-grid {
    grid-template-columns: 1fr;
  }

  .ldp-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
