<!--
  ReqProcessedHistoryPop.vue — 요청 승인 관리(Attd_10) "내 처리 이력" 팝업
  유형: frontend-component (웹 관리자 팝업)
  참조 패턴: views/attd/popup/LeaveChangeConfirmPop.vue (modal 셸 + 목록 표시)
  역할: 현재 로그인한 관리자가 활성 탭 유형에서 승인/반려 처리했던 요청 이력을 열람(읽기 전용).
        서버가 처리자=본인 스코프를 강제하므로 타 관리자 처리분은 나오지 않는다. 최근 300건.
-->
<template>
  <div class="modal-overlay" @click.self="onClose">
    <div class="modal-content rph-pop">
      <header class="modal-header">
        <h2 class="modal-title">내 처리 이력 — {{ tabLabel }}</h2>
        <button
          type="button"
          class="modal-close"
          aria-label="닫기"
          @click="onClose"
        >
          ×
        </button>
      </header>

      <div class="modal-body rph-body">
        <p v-if="loading" class="rph-state">불러오는 중...</p>

        <template v-else>
          <!-- 상태 필터 (클라이언트 필터 — 서버는 승인+반려 전체 반환) -->
          <div class="rph-filter">
            <button
              v-for="f in filters"
              :key="f.key"
              type="button"
              class="rph-filter__btn"
              :class="{ active: statusFilter === f.key }"
              @click="statusFilter = f.key"
            >
              {{ f.label }}
            </button>
            <span class="rph-filter__count">{{ filteredList.length }}건</span>
          </div>

          <div v-if="filteredList.length === 0" class="rph-state">
            처리한 이력이 없습니다.
          </div>

          <div v-else class="rph-list">
            <div v-for="row in filteredList" :key="rowKey(row)" class="rph-row">
              <div class="rph-row__main">
                <span
                  class="rph-badge"
                  :class="row.procStatus === '02' ? 'approve' : 'reject'"
                >
                  {{ row.procStatus === "02" ? "승인" : "반려" }}
                </span>
                <span class="rph-row__name">{{ row.userNm }}</span>
                <span class="rph-row__dept">{{ row.nodeNm || "-" }}</span>
                <span class="rph-chip">{{ reqTypeNm(row.reqType) }}</span>
                <span v-if="row.approvalStep" class="rph-chip step"
                  >{{ row.approvalStep }}단계</span
                >
              </div>
              <div class="rph-row__sub">
                {{ rowSummary(row) }}
              </div>
              <div v-if="row.reqReason" class="rph-row__reason">
                요청 사유: {{ row.reqReason }}
              </div>
              <div class="rph-row__proc">
                처리 {{ fmtDt(row.processDate) }}
                <template v-if="row.processComment">
                  · {{ row.processComment }}
                </template>
              </div>
            </div>
          </div>

          <!-- 연차 탭 전용: 연차 변경(이동/삭제) 확인 이력 -->
          <template v-if="reqTypeGroup === 'leave' && leaveChangeList.length > 0">
            <div class="rph-sec__title">연차 변경 요청 처리 이력</div>
            <div class="rph-list">
              <div
                v-for="row in leaveChangeList"
                :key="row.changeReqId"
                class="rph-row"
              >
                <div class="rph-row__main">
                  <span
                    class="rph-badge"
                    :class="row.reqStatus === 'CONFIRMED' ? 'approve' : 'reject'"
                  >
                    {{ row.reqStatus === "CONFIRMED" ? "확인" : "반려" }}
                  </span>
                  <span class="rph-row__name">{{ row.targetUserNm }}</span>
                  <span class="rph-chip">{{ changeTypeNm(row.reqType) }}</span>
                  <span class="rph-chip step">{{
                    row.initiatorType === "ADMIN" ? "관리자 발의" : "근로자 발의"
                  }}</span>
                </div>
                <div class="rph-row__sub">
                  {{ fmtYmd(row.targetStartDate) }}
                  <template v-if="row.reqType === 'MOVE'">
                    → {{ fmtYmd(row.moveTargetDate) }}
                    <!-- 위치선택 확장(2026-08-18): 지정 파트/시각 병기(미지정이면 종전 표시 그대로) -->
                    <template v-if="leaveChangeMovePosLabel(row)">
                      · {{ leaveChangeMovePosLabel(row) }}
                    </template>
                  </template>
                </div>
                <div v-if="row.reqReason" class="rph-row__reason">
                  요청 사유: {{ row.reqReason }}
                </div>
                <div class="rph-row__proc">
                  처리 {{ fmtDt(row.confirmDate) }}
                  <template v-if="row.rejectReason">
                    · {{ row.rejectReason }}
                  </template>
                </div>
              </div>
            </div>
          </template>
        </template>
      </div>

      <footer class="modal-footer rph-footer">
        <button type="button" class="btn btn-ghost" @click="onClose">
          닫기
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

const props = defineProps({
  // Attd_10 활성 탭 키: correction | overtime | schedule | leave
  reqTypeGroup: { type: String, required: true },
  // 팝업 제목에 붙일 탭 라벨 (예: "연차 상신")
  tabLabel: { type: String, default: "" },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

const loading = ref(true);
const processedList = ref([]);
const leaveChangeList = ref([]);
const statusFilter = ref("all");

const filters = [
  { key: "all", label: "전체" },
  { key: "02", label: "승인" },
  { key: "03", label: "반려" },
];

const filteredList = computed(() => {
  if (statusFilter.value === "all") return processedList.value;
  return processedList.value.filter((r) => r.procStatus === statusFilter.value);
});

// REQ_TYPE(SYS032) → 라벨. 이 팝업에서 나올 수 있는 유형만 매핑.
const REQ_TYPE_NM = {
  "01": "근태 생성",
  "02": "근태 수정",
  "03": "초과근무 생성",
  "04": "초과근무 수정",
  "05": "연차 사용",
  "06": "연차 수정",
  10: "스케줄 수정",
  14: "기본 근무타입 변경",
};
const reqTypeNm = (t) => REQ_TYPE_NM[t] || t;

const CHANGE_TYPE_NM = { MOVE: "이동", DELETE: "삭제" };
const changeTypeNm = (t) => CHANGE_TYPE_NM[t] || t;

// ── 위치선택 확장(2026-08-18): 연차 변경 처리 이력에 지정 파트/시각 병기 ──
//   미지정(null/구서버 미수신)이면 빈 값 → 종전 표시 바이트 그대로(무회귀). Attd_10 로컬 헬퍼 미러.
//   반차 파트는 대상일 경계 조회 없이 "시작 기준(늦게 출근)/종료 기준(일찍 퇴근)" 고정 표기.
//   시간차 종료는 시작+원 분량(leaveMinutes) 클라 파생(표시 전용), 분량 결손 시 "HH:MM 시작" 폴백.
const LEAVE_CHANGE_HALF_PART_NM = {
  START: "시작 기준(늦게 출근)",
  END: "종료 기준(일찍 퇴근)",
};
const lcHhmmToMin = (hhmm) => {
  const v = String(hhmm ?? "");
  if (v.length !== 4) return null;
  const h = parseInt(v.slice(0, 2), 10);
  const m = parseInt(v.slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};
const leaveChangeMovePosLabel = (row) => {
  if (row?.moveTargetHalfPart)
    return LEAVE_CHANGE_HALF_PART_NM[row.moveTargetHalfPart] || "";
  const s = lcHhmmToMin(row?.moveTargetStartTime);
  if (s == null) return "";
  const dur = Number(row?.leaveMinutes);
  if (!Number.isFinite(dur) || dur <= 0)
    return `${fmtTime(row.moveTargetStartTime)} 시작`;
  // 자정 넘김(END<START)은 익일 저장 규약 — 시각만 모듈러 표기
  const e = (s + dur) % 1440;
  const pad = (n) => String(n).padStart(2, "0");
  return `${fmtTime(row.moveTargetStartTime)}~${pad(Math.floor(e / 60))}:${pad(e % 60)}`;
};

const fmtYmd = (ymd) => {
  if (!ymd || String(ymd).length !== 8) return ymd ?? "";
  return formatYmdDot(ymd);
};

// "2026-08-17T09:30:00" → "2026.08.17 09:30"
const fmtDt = (iso) => {
  if (!iso) return "-";
  const [d, t] = String(iso).split("T");
  return `${(d || "").replaceAll("-", ".")} ${(t || "").slice(0, 5)}`.trim();
};

const fmtTime = (hhmm) => {
  const v = String(hhmm ?? "");
  return v.length >= 4 ? `${v.slice(0, 2)}:${v.slice(2, 4)}` : "";
};

// 탭 유형별 요약 한 줄 — 대상일 + 내용
const rowSummary = (row) => {
  const parts = [];
  if (row.workYmd) parts.push(fmtYmd(row.workYmd));
  else if (row.startDate) parts.push(fmtYmd(row.startDate));

  if (props.reqTypeGroup === "leave") {
    if (row.leaveTypeNm) parts.push(row.leaveTypeNm);
    if (row.unitNm) parts.push(row.unitNm);
    if (row.leaveDays !== null && row.leaveDays !== undefined) {
      parts.push(`${Number(row.leaveDays)}일`);
    }
  } else if (props.reqTypeGroup === "schedule") {
    if (row.schNo) parts.push(`요청 스케줄 ${row.schNo}`);
  } else if (props.reqTypeGroup === "defaultSchChange") {
    if (row.schNo) parts.push(`요청 근무타입 ${row.schNo}`);
  } else if (row.startTime && row.endTime) {
    parts.push(`${fmtTime(row.startTime)}~${fmtTime(row.endTime)}`);
  }
  return parts.join(" · ");
};

// 반려 후 재상신 등으로 같은 REQ_ID 가 결재 단계별로 중복될 수 있어 단계까지 키에 포함.
const rowKey = (row) => `${row.reqId}-${row.approvalStep ?? 0}`;

const onClose = () => emit("close");

const fnLoad = async () => {
  loading.value = true;
  try {
    const r = await axios.get("/webApi/reqinbox/processed", {
      params: { reqTypeGroup: props.reqTypeGroup },
    });
    processedList.value = r.data?.processedList || [];
    leaveChangeList.value = r.data?.leaveChangeList || [];
  } catch (e) {
    processedList.value = [];
    leaveChangeList.value = [];
    proxy.$alert(
      resolveApiErrorMessage(e, "처리 이력을 불러오지 못했습니다.")
    );
  } finally {
    loading.value = false;
  }
};

onMounted(fnLoad);
</script>

<style scoped>
.rph-pop {
  width: 560px;
  max-width: 92vw;
  max-height: 80vh;
  /* modal-content 기본 패딩 제거 — 헤더/본문/푸터 밀착(LeaveChangeConfirmPop 동형) */
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.rph-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: var(--card-padding, 20px);
}

.rph-state {
  text-align: center;
  padding: var(--card-padding, 20px);
  color: var(--color-text-muted);
  font-size: var(--btn-font, 11px);
}

.rph-filter {
  display: flex;
  align-items: center;
  gap: var(--space-xs, 4px);
  margin-bottom: var(--space-sm, 8px);
}
.rph-filter__btn {
  border: 1px solid var(--color-border);
  background: var(--color-surface, transparent);
  color: var(--color-text-muted);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}
.rph-filter__btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}
.rph-filter__count {
  margin-left: auto;
  color: var(--color-text-muted);
  font-size: var(--btn-font, 11px);
}

.rph-sec__title {
  margin: var(--space-md, 16px) 0 var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
  font-weight: 700;
  color: var(--color-text);
}

.rph-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 4px);
}

.rph-row {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 8px);
  padding: var(--space-sm, 8px) var(--space-md, 12px);
}

.rph-row__main {
  display: flex;
  align-items: center;
  gap: var(--space-xs, 6px);
  flex-wrap: wrap;
}
.rph-row__name {
  font-weight: 600;
  font-size: var(--btn-font, 11px);
  color: var(--color-text);
}
.rph-row__dept {
  font-size: var(--btn-font, 11px);
  color: var(--color-text-muted);
}

.rph-badge {
  font-size: 10px;
  border-radius: var(--radius-xs, 4px);
  padding: 1px 6px;
  font-weight: 700;
  flex-shrink: 0;
}
.rph-badge.approve {
  background: var(--color-success-bg, rgba(34, 197, 94, 0.12));
  color: var(--color-success, #16a34a);
}
.rph-badge.reject {
  background: var(--color-danger-bg, rgba(239, 68, 68, 0.12));
  color: var(--color-danger, #dc2626);
}

.rph-chip {
  font-size: 10px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 0 6px;
  color: var(--color-text-muted);
}
.rph-chip.step {
  border-style: dashed;
}

.rph-row__sub {
  margin-top: 2px;
  font-size: var(--btn-font, 11px);
  color: var(--color-text);
}
.rph-row__reason {
  margin-top: 2px;
  font-size: var(--btn-font, 11px);
  color: var(--color-text-muted);
  word-break: break-all;
}
.rph-row__proc {
  margin-top: 2px;
  font-size: 10px;
  color: var(--color-text-muted);
  word-break: break-all;
}

.rph-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
