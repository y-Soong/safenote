<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>연계 데이터 확정 · {{ acctId }}</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">
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

        <!-- 2. 수평선 (도메인 노드) -->
        <div class="lc-timeline">
          <div class="lc-line"></div>
          <button
            v-for="d in domains"
            :key="d.code"
            class="lc-node"
            :class="{
              active: activeDomain === d.code,
              done: confirmedCount[d.code] > 0,
            }"
            @click="fnSelectDomain(d.code)"
          >
            <span class="lc-circle">
              <span v-if="confirmedCount[d.code] > 0" class="lc-badge">
                {{ confirmedCount[d.code] }}
              </span>
            </span>
            <span class="lc-label">{{ d.label }}</span>
          </button>
        </div>

        <!-- 3. 도메인별 조회결과 패널 -->
        <div class="lc-body">
          <div class="lc-notice" v-if="currentNotice">
            ⓘ {{ currentNotice }}
          </div>

          <!-- 로딩 -->
          <div v-if="loading" class="lc-state">조회 중...</div>

          <!-- 근태 (ATTD) -->
          <template v-else-if="activeDomain === 'ATTD'">
            <div class="lc-sub">
              발생 시각 마커: <b>{{ fmtHm(attd.occurTime) }}</b>
              <span v-if="!attd.hasSchedule" class="lc-muted">
                · {{ attd.scheduleNote || "스케줄 없음" }}
              </span>
            </div>
            <div v-if="attd.schedule" class="lc-card">
              <div class="lc-card-h">정규 당일 스케줄</div>
              <div class="lc-kv">
                <span>1구간</span>
                <span
                  >{{ fmtHm(attd.schedule.fstSchStrTime) }} ~
                  {{ fmtHm(attd.schedule.fstSchEndTime) }}</span
                >
              </div>
              <div
                class="lc-kv"
                v-if="
                  attd.schedule.secSchStrTime || attd.schedule.secSchEndTime
                "
              >
                <span>2구간</span>
                <span
                  >{{ fmtHm(attd.schedule.secSchStrTime) }} ~
                  {{ fmtHm(attd.schedule.secSchEndTime) }}</span
                >
              </div>
            </div>
            <table class="data-grid lc-grid">
              <thead>
                <tr>
                  <th class="check-col"></th>
                  <th>구간</th>
                  <th>출근</th>
                  <th>퇴근</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!attd.records || attd.records.length === 0">
                  <td colspan="4" class="edu-grid-empty">
                    당일 실근태 기록이 없습니다.
                  </td>
                </tr>
                <tr v-for="(r, i) in attd.records" :key="r.attdId">
                  <td class="check-col">
                    <input type="checkbox" v-model="attdChecked[i]" />
                  </td>
                  <td>{{ r.workSeq }}</td>
                  <td>{{ fmtDateTime(r.checkInDate, r.checkInTime) }}</td>
                  <td>{{ fmtDateTime(r.checkOutDate, r.checkOutTime) }}</td>
                </tr>
              </tbody>
            </table>
          </template>

          <!-- 순회점검 (CHKPT) -->
          <template v-else-if="activeDomain === 'CHKPT'">
            <table class="data-grid lc-grid">
              <thead>
                <tr>
                  <th class="check-col"></th>
                  <th>점검대상</th>
                  <th>총항목</th>
                  <th>양호</th>
                  <th>불량</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-if="!patrol.summaryList || patrol.summaryList.length === 0"
                >
                  <td colspan="5" class="edu-grid-empty">
                    1주일 이내 순회점검 기록이 없습니다.
                  </td>
                </tr>
                <tr v-for="(r, i) in patrol.summaryList" :key="r.chkptCd">
                  <td class="check-col">
                    <input type="checkbox" v-model="patrolChecked[i]" />
                  </td>
                  <td>{{ r.chkptNm }}</td>
                  <td>{{ r.totalCnt }}</td>
                  <td>{{ r.goodCnt }}</td>
                  <td>
                    <span :class="{ 'lc-bad': r.badCnt > 0 }">{{
                      r.badCnt
                    }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div
              v-if="patrol.badItemList && patrol.badItemList.length"
              class="lc-bad-list"
            >
              <div class="lc-card-h">불량 항목</div>
              <div
                v-for="(b, i) in patrol.badItemList"
                :key="i"
                class="lc-bad-row"
              >
                <span class="lc-muted">{{ b.workDate }}</span>
                <span>{{ b.inspectItemSubj }}</span>
                <span class="lc-muted">{{ b.answerDesc }}</span>
              </div>
            </div>
          </template>

          <!-- 위험성평가 (RISK) -->
          <template v-else-if="activeDomain === 'RISK'">
            <table class="data-grid lc-grid">
              <thead>
                <tr>
                  <th class="check-col"></th>
                  <th>평가코드</th>
                  <th>공정/위험요인/유해요인</th>
                  <th>위험도(초기/재평가)</th>
                  <th>상태</th>
                  <th>평가일</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!risk.riskList || risk.riskList.length === 0">
                  <td colspan="6" class="edu-grid-empty">
                    3개월 이내 유효 위험성평가가 없습니다.
                  </td>
                </tr>
                <tr v-for="(r, i) in risk.riskList" :key="r.assessmentCd">
                  <td class="check-col">
                    <input type="checkbox" v-model="riskChecked[i]" />
                  </td>
                  <td>{{ r.assessmentCd }}</td>
                  <td>
                    {{
                      [r.processNm, r.riskTypeNm, r.hazardNm]
                        .filter(Boolean)
                        .join(" / ")
                    }}
                  </td>
                  <td>
                    {{ r.initRiskLv || "-" }} / {{ r.revalRiskLv || "-" }}
                  </td>
                  <td>{{ r.assessmentStatusNm || r.assessmentStatus }}</td>
                  <td>{{ r.initAssessDate }}</td>
                </tr>
              </tbody>
            </table>
          </template>

          <!-- TBM -->
          <template v-else-if="activeDomain === 'TBM'">
            <table class="data-grid lc-grid">
              <thead>
                <tr>
                  <th class="check-col"></th>
                  <th>세션</th>
                  <th>교육명</th>
                  <th>상태</th>
                  <th>재해자 이수</th>
                  <th>개시시각</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!tbm.tbmList || tbm.tbmList.length === 0">
                  <td colspan="6" class="edu-grid-empty">
                    당일 TBM 기록이 없습니다.
                  </td>
                </tr>
                <tr v-for="(r, i) in tbm.tbmList" :key="r.sessionCd">
                  <td class="check-col">
                    <input type="checkbox" v-model="tbmChecked[i]" />
                  </td>
                  <td>{{ r.sessionCd }}</td>
                  <td>{{ r.title }}</td>
                  <td>{{ r.statusNm || r.statusCd }}</td>
                  <td>
                    <span
                      class="lc-pill"
                      :class="
                        r.victimCompletionStatusCd === 'COMPLETED'
                          ? 'yes'
                          : 'no'
                      "
                    >
                      {{
                        r.victimCompletionStatusCd
                          ? r.victimCompletionStatusNm ||
                            r.victimCompletionStatusCd
                          : "시스템 기록 없음"
                      }}
                    </span>
                  </td>
                  <td>{{ r.openedAt }}</td>
                </tr>
              </tbody>
            </table>
          </template>

          <div class="lc-confirm-row">
            <button
              class="btn btn-second"
              :disabled="loading"
              @click="fnConfirmDomain"
            >
              현재 도메인 선택 확정
            </button>
            <span class="lc-muted">
              선택한 항목을 확정하면 ① 안전관리 현황 탭에 스냅샷으로 저장됩니다.
            </span>
          </div>

          <div class="lc-legend">
            ※ 모든 항목은 본 시스템 기록 기준이며, '기록 없음'은 행위 부재가
            아니라 입력 부재일 수 있습니다.
          </div>
        </div>

        <!-- 4. Footer -->
        <div class="modal-foot">
          <button class="btn btn-second" @click="$emit('close')">닫기</button>
          <button class="btn btn-primary" @click="fnFinish">확인</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot, formatHm } from "@/utils/dateFormat";

const props = defineProps({
  // 등록 직후 넘어온 조회조건 묶음
  acctId: { type: String, required: true },
  siteCd: { type: String, default: "" },
  chklstType: { type: String, default: "" },
  chkptCds: { type: Array, default: () => [] },
  processCd: { type: String, default: "" },
  riskTypeCd: { type: String, default: "" },
  hazardCds: { type: Array, default: () => [] }, // 유해요인 다건(위험성평가)
  onConfirmed: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 5,
});

const domains = [
  { code: "ATTD", label: "근태" },
  { code: "CHKPT", label: "순회점검" },
  { code: "RISK", label: "위험성평가" },
  { code: "TBM", label: "TBM" },
];

const activeDomain = ref("");
const loading = ref(false);

// 도메인별 조회 응답
const attd = ref({
  hasSchedule: false,
  scheduleNote: "",
  schedule: null,
  records: [],
  occurTime: "",
  notice: "",
});
const patrol = ref({ summaryList: [], badItemList: [], notice: "" });
const risk = ref({ riskList: [], notice: "" });
const tbm = ref({ tbmList: [], notice: "" });

// 체크 상태 (인덱스 기반)
const attdChecked = ref([]);
const patrolChecked = ref([]);
const riskChecked = ref([]);
const tbmChecked = ref([]);

// 도메인별 확정 건수
const confirmedCount = reactive({
  ATTD: 0,
  CHKPT: 0,
  RISK: 0,
  TBM: 0,
});
let anyConfirmed = false;

const currentNotice = computed(() => {
  switch (activeDomain.value) {
    case "ATTD":
      return attd.value.notice;
    case "CHKPT":
      return patrol.value.notice;
    case "RISK":
      return risk.value.notice;
    case "TBM":
      return tbm.value.notice;
    default:
      return "";
  }
});

onMounted(() => {
  fnSelectDomain("ATTD");
});

// 공통 조회 파라미터(식별자는 서버 JWT/사고헤더에서 도출)
const baseParams = () => ({
  siteCd: props.siteCd,
  acctId: props.acctId,
});

const fnSelectDomain = async (code) => {
  activeDomain.value = code;
  loading.value = true;
  try {
    if (code === "ATTD") {
      const res = await axios.get("/webApi/acct01/link/attendance", {
        params: baseParams(),
      });
      attd.value = res.data || attd.value;
      attdChecked.value = (attd.value.records || []).map(() => true);
    } else if (code === "CHKPT") {
      // 점검대상 다건 선택 시 chkptCd 별 반복 호출 후 병합
      const cds =
        props.chkptCds && props.chkptCds.length ? props.chkptCds : [""];
      const summary = [];
      const bad = [];
      let notice = "";
      for (const cd of cds) {
        const res = await axios.get("/webApi/acct01/link/patrol", {
          params: {
            ...baseParams(),
            chklstType: props.chklstType,
            chkptCd: cd,
          },
        });
        const d = res.data || {};
        notice = d.notice || notice;
        (d.summaryList || []).forEach((s) => summary.push(s));
        (d.badItemList || []).forEach((b) => bad.push(b));
      }
      patrol.value = { summaryList: summary, badItemList: bad, notice };
      patrolChecked.value = summary.map(() => true);
    } else if (code === "RISK") {
      // 유해요인 다건: 점검대상(patrol)과 동일하게 건별 조회 후 병합.
      //   각 위험성평가(assessmentCd)는 단일 HAZARD_CD 라 유해요인별 결과는 서로 겹치지 않는다.
      const hcds = (props.hazardCds || []).filter(Boolean);
      const calls = hcds.length > 0 ? hcds : [""]; // 미선택 시 1회 전체 조회(hazardCd="")
      const riskList = [];
      let riskNotice = "";
      for (const hcd of calls) {
        const res = await axios.get("/webApi/acct01/link/risk", {
          params: {
            ...baseParams(),
            processCd: props.processCd,
            riskTypeCd: props.riskTypeCd,
            hazardCd: hcd,
          },
        });
        const d = res.data || {};
        riskNotice = d.notice || riskNotice;
        (d.riskList || []).forEach((r) => riskList.push(r));
      }
      risk.value = { riskList, notice: riskNotice };
      riskChecked.value = (risk.value.riskList || []).map(() => true);
    } else if (code === "TBM") {
      const res = await axios.get("/webApi/acct01/link/tbm", {
        params: baseParams(),
      });
      tbm.value = res.data || tbm.value;
      tbmChecked.value = (tbm.value.tbmList || []).map(() => true);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "연계 데이터 조회 중 오류가 발생했습니다.")
    );
  } finally {
    loading.value = false;
  }
};

// 현재 도메인의 체크된 항목을 link/confirm 으로 스냅샷 저장
const fnConfirmDomain = async () => {
  const code = activeDomain.value;
  const items = buildConfirmItems(code);
  if (items.length === 0) {
    await proxy.$alert("확정할 항목을 선택하세요.");
    return;
  }
  try {
    const response = await axios.post("/webApi/acct01/link/confirm", {
      siteCd: props.siteCd,
      acctId: props.acctId,
      linkDomainCd: code,
      items,
    });
    if (response.status === 200) {
      confirmedCount[code] = items.length;
      anyConfirmed = true;
      await proxy.$alert(
        `${labelOf(code)} ${items.length}건이 확정되었습니다.`
      );
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "확정 저장 중 오류가 발생했습니다.")
    );
  }
};

// 체크된 항목 → LinkItem(linkKeyJson/snapshotJson) 변환
const buildConfirmItems = (code) => {
  const items = [];
  const push = (keyObj, snapObj) => {
    items.push({
      linkKeyJson: JSON.stringify(keyObj),
      snapshotJson: JSON.stringify(snapObj),
    });
  };
  if (code === "ATTD") {
    (attd.value.records || []).forEach((r, i) => {
      if (attdChecked.value[i]) push({ attdId: r.attdId }, r);
    });
  } else if (code === "CHKPT") {
    (patrol.value.summaryList || []).forEach((r, i) => {
      if (patrolChecked.value[i]) push({ chkptCd: r.chkptCd }, r);
    });
  } else if (code === "RISK") {
    (risk.value.riskList || []).forEach((r, i) => {
      if (riskChecked.value[i]) push({ assessmentCd: r.assessmentCd }, r);
    });
  } else if (code === "TBM") {
    (tbm.value.tbmList || []).forEach((r, i) => {
      if (tbmChecked.value[i]) push({ sessionCd: r.sessionCd }, r);
    });
  }
  return items;
};

const labelOf = (code) => domains.find((d) => d.code === code)?.label || code;

const fnFinish = () => {
  if (typeof props.onConfirmed === "function") {
    props.onConfirmed({ acctId: props.acctId, anyConfirmed });
  }
  emit("close");
};

// ── 포맷터 ──
// 시각 표시는 dateFormat 단일 출처에 위임(콜론 HH:mm). 빈값은 "-".
const fmtHm = (hhmm) => {
  if (!hhmm) return "-";
  return formatHm(hhmm);
};
// 날짜+시각 표시는 dateFormat 단일 출처에 위임(점/콜론). 둘 다 없으면 "-".
const fmtDateTime = (ymd, hhmm) => {
  if (!ymd && !hhmm) return "-";
  const d = ymd ? formatYmdDot(ymd) : "";
  return `${d} ${fmtHm(hhmm)}`.trim();
};
</script>

<style scoped>
.lc-timeline {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.5rem 2rem 0.75rem;
  background: var(--color-bg, #f9fafb);
}
.lc-line {
  position: absolute;
  top: calc(1.5rem + 14px);
  left: 3rem;
  right: 3rem;
  height: 2px;
  background: var(--color-border, #e5e7eb);
}
.lc-node {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: transparent;
  cursor: pointer;
  flex: 1;
}
.lc-circle {
  position: relative;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-surface, #fff);
  border: 2px solid var(--color-border-strong, #d1d5db);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.lc-node.active .lc-circle {
  border-color: var(--color-primary, #16a34a);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
.lc-node.done .lc-circle {
  background: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}
.lc-badge {
  font-size: 0.7rem;
  font-weight: 700;
  color: #fff;
}
.lc-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text, #374151);
}
.lc-node.active .lc-label {
  color: var(--color-primary-hover, #15803d);
}
.lc-body {
  padding: var(--card-padding, 20px);
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}
.lc-notice {
  background: var(--color-warning-bg, #fffbeb);
  border: 1px solid var(--color-warning-bg, #fde68a);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-warning-text, #92400e);
  margin-bottom: 0.75rem;
}
.lc-state {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-muted, #8b94a3);
}
.lc-sub {
  font-size: 0.8rem;
  color: var(--color-text, #374151);
  margin-bottom: 0.6rem;
}
.lc-muted {
  color: var(--color-text-muted, #8b94a3);
}
.lc-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.6rem 0.75rem;
  margin-bottom: 0.75rem;
}
.lc-card-h {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-text-muted, #4b5563);
  margin-bottom: 0.4rem;
}
.lc-kv {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  padding: 0.2rem 0;
}
.lc-grid {
  width: 100%;
}
.check-col {
  width: 36px;
  text-align: center;
}
.lc-bad {
  color: var(--color-danger, #ef4444);
  font-weight: 700;
}
.lc-bad-list {
  margin-top: 0.75rem;
}
.lc-bad-row {
  display: flex;
  gap: 0.75rem;
  font-size: 0.78rem;
  padding: 0.3rem 0;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.lc-pill {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-pill, 999px);
  font-weight: 600;
}
.lc-pill.yes {
  background: var(--color-primary-soft, #dcfce7);
  color: var(--color-primary-hover, #15803d);
}
.lc-pill.no {
  background: var(--danger-tint, #fef2f2);
  color: var(--color-danger, #ef4444);
}
.lc-confirm-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}
.lc-confirm-row .lc-muted {
  font-size: 0.72rem;
}
.lc-legend {
  font-size: 0.7rem;
  color: var(--color-text-muted, #8b94a3);
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  line-height: 1.6;
}
.modal-foot {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  padding: 0.875rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}
</style>
