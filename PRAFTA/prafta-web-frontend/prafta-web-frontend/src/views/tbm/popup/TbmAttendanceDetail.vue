<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>출결 상세 - {{ props.sessionTitle_p || sessionCd }}</span>
          <div class="header-actions">
            <!-- 프린트 버튼 제거(2026-08-30 요청) — 닫기만 유지 -->
            <button class="icon-button no-print" @click="$emit('close')">
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
        </div>

        <div class="detail-wrapper">
          <!-- 출결 요약 -->
          <div class="att-summary">
            <span class="meta-item">참여 {{ totalCount }}명</span>
            <span class="meta-item">이수 {{ completedCount }}명</span>
            <span class="meta-item meta-danger"
              >미이수 {{ notCompletedCount }}명</span
            >
            <span class="meta-item">
              이수율 {{ completionRate(completedCount, totalCount) }}%
            </span>
          </div>

          <!-- 유형 필터 -->
          <div class="att-filter no-print">
            <label>대상</label>
            <select v-model.trim="userTypeCd" @change="fnSearch">
              <option value="">전체</option>
              <option value="REGULAR">정규직</option>
              <option value="DAILY">일용직</option>
            </select>
            <label>이수</label>
            <select v-model.trim="completionStatusCd" @change="fnSearch">
              <option value="">전체</option>
              <option value="COMPLETED">이수</option>
              <option value="NOT_COMPLETED">미이수</option>
            </select>
          </div>

          <!-- 참여자 그리드 -->
          <div class="table-box" style="--box-h: auto; --box-ox: auto">
            <table class="data-grid w-full text-sm text-left">
              <thead>
                <tr>
                  <th style="width: 4%; text-align: center">No</th>
                  <th style="width: 7%">유형</th>
                  <th style="width: 14%">이름</th>
                  <!-- 소속 회사(PRAFTA-SUBCON-T5): 서버 relabel 값(1차 회사명). 자사 참석자는 자사명. -->
                  <th style="width: 10%">소속 회사</th>
                  <th style="width: 13%">소속/끝4자리</th>
                  <th style="width: 11%">입실</th>
                  <th style="width: 11%">종료</th>
                  <th style="width: 8%; text-align: center">입실거리</th>
                  <th style="width: 8%; text-align: center">앱실행시간</th>
                  <th style="width: 11%">이상신호</th>
                  <th style="width: 7%; text-align: center">이수</th>
                  <th style="width: 15%; text-align: center" class="no-print">
                    액션
                  </th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!attendanceList || attendanceList.length === 0">
                  <tr>
                    <td colspan="12" class="edu-grid-empty">
                      출결 명단이 없습니다. (실시간 진행/모바일 앱 이후
                      채워집니다)
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <template
                    v-for="(row, idx) in attendanceList"
                    :key="row.attendanceCd"
                  >
                    <tr :class="rowClass(row)">
                      <td style="text-align: center">{{ idx + 1 }}</td>
                      <td>
                        <span
                          class="type-badge"
                          :class="
                            row.userTypeCd === 'DAILY'
                              ? 'type-daily'
                              : 'type-regular'
                          "
                        >
                          {{ row.userTypeNm || typeNm(row.userTypeCd) }}
                        </span>
                      </td>
                      <td>
                        <!-- 타사(연동) 참석자는 서버가 userCd 를 내리지 않는다(최소 노출).
                             내 회사 직원이 아니므로 사용자별 이수 이력 드릴다운도 열지 않는다. -->
                        <button
                          v-if="row.userCd"
                          type="button"
                          class="title-link"
                          @click="fnUserHistory(row)"
                        >
                          {{ row.userNm }}
                        </button>
                        <span v-else>{{ row.userNm }}</span>
                      </td>
                      <td>{{ row.affilCmpnyNm || "-" }}</td>
                      <td>
                        <template v-if="row.userTypeCd === 'DAILY'">
                          ****{{ row.mblNoLast4 || "" }}
                        </template>
                        <template v-else>
                          {{ row.deptNm || "-" }}
                        </template>
                      </td>
                      <td>{{ row.entryAt || "-" }}</td>
                      <td>
                        <template v-if="row.exited">{{ row.exitAt }}</template>
                        <span v-else class="not-exited">미종료</span>
                      </td>
                      <td style="text-align: center">
                        {{ distanceText(row.entryDistanceM) }}
                      </td>
                      <td style="text-align: center">
                        {{ foregroundText(row.appForegroundSec) }}
                      </td>
                      <td>
                        <span
                          class="anomaly-badge"
                          :class="anomalyClass(row.anomalyLevel)"
                        >
                          {{ anomalyNm(row.anomalyLevel) }}
                        </span>
                        <span v-if="row.eventCount > 0" class="anomaly-sub">
                          (이벤트 {{ row.eventCount }})
                        </span>
                      </td>
                      <td style="text-align: center">
                        <span
                          class="comp-mark"
                          :class="
                            row.completionStatusCd === 'COMPLETED'
                              ? 'comp-ok'
                              : row.completionStatusCd === 'NOT_COMPLETED'
                                ? 'comp-no'
                                : 'comp-none'
                          "
                          :title="row.notCompletedReason || ''"
                        >
                          {{ compMark(row.completionStatusCd) }}
                        </span>
                      </td>
                      <td style="text-align: center" class="no-print">
                        <button
                          type="button"
                          class="btn btn-second btn-xs"
                          title="이상신호 이벤트 타임라인(앱 이탈/GPS 벗어남/네트워크 끊김 기록)을 펼칩니다"
                          @click="fnToggleEvents(row)"
                        >
                          이벤트
                        </button>
                        <button
                          type="button"
                          class="btn btn-second btn-xs"
                          @click="fnCompletion(row)"
                        >
                          이수처리
                        </button>
                        <button
                          v-if="hasSignature(row)"
                          type="button"
                          class="btn btn-second btn-xs"
                          @click="fnSignature(row)"
                        >
                          서명
                        </button>
                      </td>
                    </tr>
                    <!-- 이벤트 타임라인 토글 행 -->
                    <tr v-if="expandedCd === row.attendanceCd">
                      <td colspan="12" class="event-cell-row">
                        <TbmEventTimeline
                          :attendanceCd_p="row.attendanceCd"
                          :userNm_p="row.userNm"
                          embedded
                        />
                      </td>
                    </tr>
                  </template>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer no-print">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>

        <!-- 서명 이미지 오버레이(2026-08-30) — 파일코드 텍스트 alert 대신 실제 서명 이미지 표시.
             이미지는 인증 스트림 EP(blob)로만 로드한다(공개 정적 URL 금지). -->
        <div
          v-if="signTarget"
          class="sign-overlay"
          @click.self="fnCloseSignature"
        >
          <div class="sign-panel">
            <p class="sign-panel__title">서명 확인 - {{ signTarget.userNm }}</p>
            <p v-if="signLoading" class="sign-panel__empty">불러오는 중...</p>
            <template v-else>
              <div v-if="signEntryUrl" class="sign-panel__item">
                <span class="sign-panel__label">입실 서명</span>
                <img :src="signEntryUrl" alt="입실 서명" class="sign-panel__img" />
              </div>
              <div v-if="signExitUrl" class="sign-panel__item">
                <span class="sign-panel__label">종료 서명</span>
                <img :src="signExitUrl" alt="종료 서명" class="sign-panel__img" />
              </div>
              <p v-if="signTarget.forcedEnd" class="sign-panel__note">
                종료: 관리자 강제 종료(종료 서명 없음)
              </p>
              <p
                v-if="!signEntryUrl && !signExitUrl"
                class="sign-panel__empty"
              >
                표시할 서명 이미지가 없습니다.
              </p>
            </template>
            <div class="sign-panel__btns">
              <button class="btn btn-second" @click="fnCloseSignature">
                닫기
              </button>
            </div>
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
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import TbmEventTimeline from "./TbmEventTimeline.vue";
import TbmCompletionModal from "./TbmCompletionModal.vue";
import TbmUserAttendance from "./TbmUserAttendance.vue";

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const props = defineProps({
  sessionCd_p: String,
  sessionTitle_p: String,
  sessionStatusCd_p: String,
  onSearch: Function,
});
defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 1.5,
  verticalRatio: 5,
});

const sessionCd = ref(props.sessionCd_p || "");
const attendanceList = ref([]);
const totalCount = ref(0);
const completedCount = ref(0);
const notCompletedCount = ref(0);

// 필터
const userTypeCd = ref("");
const completionStatusCd = ref("");

// 이벤트 타임라인 토글 대상
const expandedCd = ref("");

onMounted(async () => {
  await fnSearch();
});

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm04/session-attendances", {
      params: {
        sessionCd: sessionCd.value,
        userTypeCd: userTypeCd.value,
        completionStatusCd: completionStatusCd.value,
        includeEventSummary: true,
      },
    });

    if (response.status === 200) {
      const data = response.data || {};
      attendanceList.value = data.attendanceList || [];
      totalCount.value = data.totalCount || 0;
      completedCount.value = data.completedCount || 0;
      notCompletedCount.value = data.notCompletedCount || 0;
    }
  } catch (err) {
    attendanceList.value = [];
    totalCount.value = 0;
    completedCount.value = 0;
    notCompletedCount.value = 0;
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// 이벤트 타임라인 펼침/접힘
const fnToggleEvents = (row) => {
  expandedCd.value =
    expandedCd.value === row.attendanceCd ? "" : row.attendanceCd;
};

// W-14 미이수 처리 모달 호출 → 성공 시 그리드 갱신
const fnCompletion = (row) => {
  openPop(TbmCompletionModal, {
    attendanceCd_p: row.attendanceCd,
    userNm_p: row.userNm,
    currentStatusCd_p: row.completionStatusCd || "",
    currentReason_p: row.notCompletedReason || "",
    onUpdated: () => {
      fnSearch();
      if (typeof props.onSearch === "function") props.onSearch();
    },
  });
};

// W-15 사용자별 이수 이력(이름 클릭 진입). 유형별 엔드포인트 분기.
const fnUserHistory = (row) => {
  openPop(TbmUserAttendance, {
    userCd_p: row.userCd,
    userTypeCd_p: row.userTypeCd || "REGULAR",
  });
};

// ─────────── 서명 이미지 오버레이(2026-08-30) ───────────
// 종전엔 파일코드 텍스트만 alert 로 보여줬다 → 인증 스트림 EP 로 실제 서명 이미지를 표시한다.
const signTarget = ref(null);
const signLoading = ref(false);
const signEntryUrl = ref("");
const signExitUrl = ref("");

// 서명 이미지 blob 단건 로드. 실패(404 등)는 해당 칸만 비운다(오버레이 자체는 유지).
const fnLoadSignBlobUrl = async (attendanceCd, kind) => {
  try {
    const response = await axios.get("/webApi/tbm04/attendance-sign-image", {
      params: { attendanceCd, kind },
      responseType: "blob",
    });
    return URL.createObjectURL(response.data);
  } catch (e) {
    return "";
  }
};

const fnSignature = async (row) => {
  signTarget.value = row;
  signLoading.value = true;
  signEntryUrl.value = "";
  signExitUrl.value = "";
  try {
    if (row.entrySignFileMgmtCd) {
      signEntryUrl.value = await fnLoadSignBlobUrl(row.attendanceCd, "ENTRY");
    }
    if (row.exitSignFileMgmtCd) {
      signExitUrl.value = await fnLoadSignBlobUrl(row.attendanceCd, "EXIT");
    }
  } finally {
    signLoading.value = false;
  }
};

const fnCloseSignature = () => {
  if (signEntryUrl.value) URL.revokeObjectURL(signEntryUrl.value);
  if (signExitUrl.value) URL.revokeObjectURL(signExitUrl.value);
  signEntryUrl.value = "";
  signExitUrl.value = "";
  signTarget.value = null;
};

const hasSignature = (row) =>
  !!(row.entrySignFileMgmtCd || row.exitSignFileMgmtCd);

// 행 색상: 미이수=빨강 / 강제종료=회색 / 이상신호 HIGH=노랑 / 정상=기본
const rowClass = (row) => {
  if (row.completionStatusCd === "NOT_COMPLETED") return "row-not-completed";
  if (row.forcedEnd) return "row-forced";
  if (row.anomalyLevel === "HIGH" || row.anomalyLevel === "LOW")
    return "row-anomaly";
  return "";
};

const completionRate = (completed, total) => {
  const c = Number(completed) || 0;
  const t = Number(total) || 0;
  if (t <= 0) return 0;
  return Math.round((c / t) * 1000) / 10;
};

const typeNm = (code) => (code === "DAILY" ? "일용직" : "정규직");

// prafta-051-16: 입실 거리(m). null/미수신은 '-' (대리/검색입실은 거리 없음)
const distanceText = (m) => {
  if (m == null || m === "") return "-";
  return `${m}m`;
};

// prafta-051-16: 앱 포그라운드 누적초 → MM:SS(1시간 이상은 HH:MM:SS). null/대리입실은 '-'
const foregroundText = (sec) => {
  if (sec == null || sec === "") return "-";
  const total = Math.max(0, Number(sec) || 0);
  const h = Math.floor(total / 3600);
  const mm = String(Math.floor((total % 3600) / 60)).padStart(2, "0");
  const ss = String(total % 60).padStart(2, "0");
  if (h > 0) return `${String(h).padStart(2, "0")}:${mm}:${ss}`;
  return `${mm}:${ss}`;
};

const compMark = (code) => {
  if (code === "COMPLETED") return "✓";
  if (code === "NOT_COMPLETED") return "✕";
  return "-";
};

const anomalyNm = (level) => {
  switch (level) {
    case "HIGH":
      return "이상 높음";
    case "LOW":
      return "이상 경미";
    default:
      return "정상";
  }
};

const anomalyClass = (level) => {
  switch (level) {
    case "HIGH":
      return "anomaly-high";
    case "LOW":
      return "anomaly-low";
    default:
      return "anomaly-none";
  }
};
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.detail-wrapper {
  padding: 1.2rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.att-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.meta-item {
  font-size: var(--btn-font);
  font-weight: 600;
}

.meta-danger {
  color: var(--color-danger);
}

.att-filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.att-filter label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 유형 배지 */
.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  white-space: nowrap;
}

.type-regular {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
}

.type-daily {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

/* 이상신호 배지 */
.anomaly-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.anomaly-none {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.anomaly-low {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.anomaly-high {
  background: var(--color-danger);
  color: var(--color-surface);
}

.anomaly-sub {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  margin-left: 0.25rem;
}

/* 이수 마크 */
.comp-mark {
  font-weight: 700;
}

.comp-ok {
  color: var(--color-primary);
}

.comp-no {
  color: var(--color-danger);
}

.comp-none {
  color: var(--color-text-muted);
}

.not-exited {
  color: var(--color-text-muted);
}

/* 행 색상 */
.row-not-completed {
  background: var(--color-danger-bg, var(--color-warning-bg));
}

.row-forced {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

.row-anomaly {
  background: var(--color-warning-bg);
}

.event-cell-row {
  background: var(--color-bg);
  padding: 0.5rem 1rem;
}

.btn-xs {
  height: var(--btn-height-sm);
  padding: 0 0.5rem;
  font-size: var(--btn-font-sm);
  margin: 0 0.15rem;
}

/* ─── 서명 이미지 오버레이 ─── */
.sign-overlay {
  position: fixed;
  inset: 0;
  background: var(--color-overlay, rgba(0, 0, 0, 0.45));
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 95;
}

.sign-panel {
  width: min(440px, calc(100vw - 2rem));
  max-height: 85vh;
  overflow-y: auto;
  background: var(--color-surface, #ffffff);
  border-radius: var(--btn-radius, 8px);
  padding: 1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.sign-panel__title {
  margin: 0;
  font-weight: 700;
  color: var(--color-text, #374151);
}

.sign-panel__item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.sign-panel__label {
  font-size: var(--btn-font-sm);
  font-weight: 600;
  color: var(--color-text-muted);
}

.sign-panel__img {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-bg, #f9fafb);
}

.sign-panel__note,
.sign-panel__empty {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.sign-panel__btns {
  display: flex;
  justify-content: flex-end;
}

/* 이름 링크(W-15 사용자별 이수 진입) */
.title-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
  text-decoration: underline;
  font: inherit;
}

.title-link:hover {
  color: var(--color-primary-hover);
}

/* 인쇄 레이아웃 */
@media print {
  .no-print {
    display: none !important;
  }

  .modal-overlay {
    position: static;
    background: none;
  }

  .modal-content-wide {
    position: static !important;
    width: 100% !important;
    box-shadow: none !important;
  }

  .detail-wrapper {
    height: auto;
    overflow: visible;
  }
}
</style>
