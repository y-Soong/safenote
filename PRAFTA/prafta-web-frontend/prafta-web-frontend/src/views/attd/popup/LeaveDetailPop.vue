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
          <div class="ldp-stat-section">
            <p class="ldp-stat-title">법정 휴가</p>
            <div class="ldp-stat-grid">
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">부여</p>
                <p class="ldp-stat-value">
                  {{ legalSummary.granted
                  }}<span class="ldp-stat-unit">일</span>
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">사용</p>
                <p class="ldp-stat-value">
                  {{ legalSummary.used }}<span class="ldp-stat-unit">일</span>
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">잔여</p>
                <p class="ldp-stat-value is-accent">
                  {{ legalSummary.remaining
                  }}<span class="ldp-stat-unit">일</span>
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
                  {{ nonLegalSummary.granted
                  }}<span class="ldp-stat-unit">일</span>
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">사용</p>
                <p class="ldp-stat-value">
                  {{ nonLegalSummary.used
                  }}<span class="ldp-stat-unit">일</span>
                </p>
              </div>
              <div class="ldp-stat-card">
                <p class="ldp-stat-label">잔여</p>
                <p class="ldp-stat-value">
                  {{ nonLegalSummary.remaining
                  }}<span class="ldp-stat-unit">일</span>
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
                  {{ t.remainDays }}<span class="ldp-stat-unit">일</span>
                </p>
                <p class="ldp-stat-sub">한도 {{ t.maxAplyDays }}일</p>
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
                  <td class="is-right">{{ g.granted }}일</td>
                  <td class="is-right">{{ g.used }}일</td>
                  <td class="is-right ldp-strong">{{ g.remaining }}일</td>
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
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
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

// 신청형 휴가(LEAVE_TYPE='01') 타입별 잔여 현황 — 법정/법정외와 합산하지 않는 별도 섹션.
//   각 항목: { leaveCd, leaveNm, maxAplyDays(한도), usedDays(사용), remainDays(잔여) } — 모두 서버 권위값.
const appliedLeaveTypes = ref([]);

// 부여 이력 (GRANT_DATE 내림차순; 백엔드 정렬)
//   각 행: { grantDate, natureBadge('LEGAL'|'NON_LEGAL'), reason, granted, used, remaining, expiresAt, status }
const grantHistory = ref([]);

// 부여 이력 클라이언트 사이드 페이징 (상세 API가 전량을 내려주므로 프론트에서 분할)
const page = ref(1);
const pageSize = ref(5);

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

// ================ Life Cycle Functions ================
onMounted(() => {
  fnLoadDetail();
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
};

// 부여 이력 페이지 이동 (범위 가드)
const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
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
