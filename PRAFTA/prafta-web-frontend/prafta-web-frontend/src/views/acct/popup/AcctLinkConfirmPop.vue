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

          <!-- prafta-065: 근태·TBM 은 재해자 축. 칩 = 보기 전환만. 확정은 도메인당 1회, 재해자 전원 체크 항목 병합 전송(REPLACE 회귀 차단) -->
          <div v-if="isVictimDomain && victimList.length" class="lc-victim-bar">
            <span class="lc-victim-bar__label">재해자</span>
            <button
              v-for="v in victimList"
              :key="v.victimSeq"
              type="button"
              class="lc-victim-chip"
              :class="{ active: selectedVictimSeq === v.victimSeq }"
              @click="fnSelectVictim(v.victimSeq)"
            >
              {{ v.userNm
              }}<span
                v-if="v.representativeYn === 'Y'"
                class="lc-victim-chip__rep"
                >대표</span
              >
            </button>
          </div>

          <!-- 로딩 -->
          <div v-if="loading" class="lc-state">조회 중...</div>

          <!-- 근태 (ATTD) — 선택 재해자(attdView)의 스케줄/실근태. 체크 상태는 attdId 키 -->
          <template v-else-if="activeDomain === 'ATTD'">
            <div class="lc-sub">
              발생 시각 마커: <b>{{ fmtHm(attdView.data.occurTime) }}</b>
              <span v-if="!attdView.data.hasSchedule" class="lc-muted">
                · {{ attdView.data.scheduleNote || "스케줄 없음" }}
              </span>
            </div>
            <div v-if="attdView.data.schedule" class="lc-card">
              <div class="lc-card-h">정규 당일 스케줄</div>
              <div class="lc-kv">
                <span>1구간</span>
                <span
                  >{{ fmtHm(attdView.data.schedule.fstSchStrTime) }} ~
                  {{ fmtHm(attdView.data.schedule.fstSchEndTime) }}</span
                >
              </div>
              <div
                class="lc-kv"
                v-if="
                  attdView.data.schedule.secSchStrTime ||
                  attdView.data.schedule.secSchEndTime
                "
              >
                <span>2구간</span>
                <span
                  >{{ fmtHm(attdView.data.schedule.secSchStrTime) }} ~
                  {{ fmtHm(attdView.data.schedule.secSchEndTime) }}</span
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
                <tr
                  v-if="
                    !attdView.data.records || attdView.data.records.length === 0
                  "
                >
                  <td colspan="4" class="edu-grid-empty">
                    당일 실근태 기록이 없습니다.
                  </td>
                </tr>
                <tr v-for="r in attdView.data.records" :key="r.attdId">
                  <td class="check-col">
                    <input
                      type="checkbox"
                      v-model="attdView.checked[r.attdId]"
                    />
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
                <tr
                  v-if="
                    !tbmView.data.tbmList || tbmView.data.tbmList.length === 0
                  "
                >
                  <td colspan="6" class="edu-grid-empty">
                    당일 TBM 기록이 없습니다.
                  </td>
                </tr>
                <tr v-for="r in tbmView.data.tbmList" :key="r.sessionCd">
                  <td class="check-col">
                    <input
                      type="checkbox"
                      v-model="tbmView.checked[r.sessionCd]"
                    />
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
              선택한 항목을 확정하면 ① 안전관리 현황 탭에 스냅샷으로
              저장됩니다.<template v-if="isVictimDomain">
                재해자 전원의 체크 항목이 함께 저장됩니다.</template
              >
            </span>
          </div>

          <div class="lc-legend">
            ※ 모든 항목은 본 시스템 기록 기준이며, '기록 없음'은 행위 부재가
            아니라 입력 부재일 수 있습니다.
          </div>
        </div>

        <!-- 4. Footer (F-10 규약: 왼쪽=진행/확정(확인), 오른쪽=이탈(닫기), 폭 균등) -->
        <div class="modal-foot">
          <button class="btn btn-primary" @click="fnFinish">확인</button>
          <button class="btn btn-second" @click="$emit('close')">닫기</button>
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

// ── prafta-065: 재해자 축(ATTD/TBM) ──
// victimList = GET /victims (진입 시 1회). selectedVictimSeq = 보기 전환용(기본 = 대표).
// attdByVictim / tbmByVictim = { [victimKey]: { data: 응답, checked: { [attdId|sessionCd]: true } } }
//   ★체크 상태는 식별자 키 객체(인덱스 배열 금지) — 재해자 전환·재조회 후에도 보존.
//   ★확정은 도메인당 1회 POST 에 재해자 전원의 체크 항목을 병합(서버 REPLACE → 개별 확정 시 앞 인원이 지워지는 회귀 차단).
const victimList = ref([]);
const selectedVictimSeq = ref(null);
const attdByVictim = reactive({});
const tbmByVictim = reactive({});
const attdLoaded = ref(false);
const tbmLoaded = ref(false);

const emptyAttd = () => ({
  hasSchedule: false,
  scheduleNote: "",
  schedule: null,
  records: [],
  occurTime: "",
  notice: "",
});
const emptyTbm = () => ({ tbmList: [], notice: "" });

// 재해자 0명(백필 누락) 방어: 대표 기준 종전 경로(victimSeq 미전송) 1축으로 동작
const FALLBACK_VICTIM = {
  victimSeq: null,
  userNm: "",
  userTypeCd: "",
  userCd: "",
  representativeYn: "Y",
  _fallback: true,
};
const victimAxis = computed(() =>
  victimList.value.length ? victimList.value : [FALLBACK_VICTIM]
);
const victimKeyOf = (v) => (v.victimSeq == null ? "REP" : String(v.victimSeq));
const selectedVictimKey = computed(() => {
  const found = victimAxis.value.find(
    (v) => v.victimSeq === selectedVictimSeq.value
  );
  return victimKeyOf(found || victimAxis.value[0]);
});
const isVictimDomain = computed(
  () => activeDomain.value === "ATTD" || activeDomain.value === "TBM"
);
// 선택 재해자의 표시용 항목(없으면 빈 구조 — 기록 0건이라 체크 쓰기 대상도 없음)
const attdView = computed(
  () =>
    attdByVictim[selectedVictimKey.value] || { data: emptyAttd(), checked: {} }
);
const tbmView = computed(
  () =>
    tbmByVictim[selectedVictimKey.value] || { data: emptyTbm(), checked: {} }
);

// 사업장 축 도메인(CHKPT/RISK) — 종전 구조 무변경
const patrol = ref({ summaryList: [], badItemList: [], notice: "" });
const risk = ref({ riskList: [], notice: "" });

// 체크 상태 (CHKPT/RISK 는 종전 인덱스 기반 유지 — 재해자 전환이 없어 붕괴 요인 없음)
const patrolChecked = ref([]);
const riskChecked = ref([]);

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
      return attdView.value.data.notice;
    case "CHKPT":
      return patrol.value.notice;
    case "RISK":
      return risk.value.notice;
    case "TBM":
      return tbmView.value.data.notice;
    default:
      return "";
  }
});

onMounted(async () => {
  await fnLoadVictims();
  await fnSelectDomain("ATTD");
});

// 공통 조회 파라미터(식별자는 서버 JWT/사고헤더에서 도출)
const baseParams = () => ({
  siteCd: props.siteCd,
  acctId: props.acctId,
});

// 재해자 목록 로드 + 기본 선택(대표). 0건이면 안내 후 대표 기준 종전 경로로 동작.
const fnLoadVictims = async () => {
  try {
    const res = await axios.get("/webApi/acct01/victims", {
      params: baseParams(),
    });
    victimList.value = res.data?.victimList || [];
  } catch (err) {
    victimList.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "재해자 조회 중 오류가 발생했습니다.")
    );
  }
  if (victimList.value.length === 0) {
    await proxy.$alert(
      "재해자 정보를 찾을 수 없어 대표 재해자 기준으로 근태·TBM 을 조회합니다."
    );
    selectedVictimSeq.value = null;
    return;
  }
  const rep =
    victimList.value.find((v) => v.representativeYn === "Y") ||
    victimList.value[0];
  selectedVictimSeq.value = rep.victimSeq;
};

// 재해자 칩 클릭 = 보기 전환만(조회 없음, 체크 보존)
const fnSelectVictim = (seq) => {
  selectedVictimSeq.value = seq;
};

// 재해자 1명 기준 조회 파라미터(victimSeq 없으면 서버가 대표로 처리 — 종전 경로)
const victimParams = (v) =>
  v.victimSeq == null
    ? baseParams()
    : { ...baseParams(), victimSeq: v.victimSeq };

// 식별자 키 → true 맵(전원 기본 체크: 현행 관례)
const allChecked = (list, keyName) => {
  const m = {};
  (list || []).forEach((r) => {
    m[r[keyName]] = true;
  });
  return m;
};

const fnSelectDomain = async (code) => {
  activeDomain.value = code;
  loading.value = true;
  try {
    if (code === "ATTD") {
      // 재해자 전원 일괄 조회(D-D). 이미 로드된 도메인은 재조회하지 않음(체크 보존).
      if (!attdLoaded.value) {
        const results = await Promise.all(
          victimAxis.value.map((v) =>
            axios.get("/webApi/acct01/link/attendance", {
              params: victimParams(v),
            })
          )
        );
        victimAxis.value.forEach((v, idx) => {
          const data = results[idx].data || emptyAttd();
          attdByVictim[victimKeyOf(v)] = {
            data,
            checked: allChecked(data.records, "attdId"),
          };
        });
        attdLoaded.value = true;
      }
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
      if (!tbmLoaded.value) {
        const results = await Promise.all(
          victimAxis.value.map((v) =>
            axios.get("/webApi/acct01/link/tbm", { params: victimParams(v) })
          )
        );
        victimAxis.value.forEach((v, idx) => {
          const data = results[idx].data || emptyTbm();
          tbmByVictim[victimKeyOf(v)] = {
            data,
            checked: allChecked(data.tbmList, "sessionCd"),
          };
        });
        tbmLoaded.value = true;
      }
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
      const victimNote =
        isVictimDomain.value && victimList.value.length
          ? `(재해자 ${victimList.value.length}명)`
          : "";
      await proxy.$alert(
        `${labelOf(code)} ${items.length}건${victimNote}이 확정되었습니다.`
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
  // 재해자 축: 전원 순회 × 식별자 키 체크. 순서 = 재해자 순번 → 원본 순서(LINK_SEQ 는 서버 부여).
  //   LINK_KEY_JSON 에 victimSeq + userTypeCd/userCd 를 함께 넣어 순번 재사용(D-I)에도 인물을 특정한다.
  //   SNAPSHOT_JSON 에 victimSeq/victimUserNm(마스킹 응답값 그대로) 을 넣어 ① 탭 요약 접두에 쓴다.
  //   폴백(재해자 0명) 축은 종전 키/스냅샷 그대로(구 스냅샷 호환).
  const pushVictimItems = (v, entry, keyName) => {
    if (!entry) return;
    (entry.data[keyName === "attdId" ? "records" : "tbmList"] || []).forEach(
      (r) => {
        if (!entry.checked[r[keyName]]) return;
        if (v._fallback) {
          push({ [keyName]: r[keyName] }, r);
          return;
        }
        push(
          {
            [keyName]: r[keyName],
            victimSeq: v.victimSeq,
            userTypeCd: v.userTypeCd,
            userCd: v.userCd,
          },
          { ...r, victimSeq: v.victimSeq, victimUserNm: v.userNm }
        );
      }
    );
  };
  if (code === "ATTD") {
    victimAxis.value.forEach((v) =>
      pushVictimItems(v, attdByVictim[victimKeyOf(v)], "attdId")
    );
  } else if (code === "CHKPT") {
    (patrol.value.summaryList || []).forEach((r, i) => {
      if (patrolChecked.value[i]) push({ chkptCd: r.chkptCd }, r);
    });
  } else if (code === "RISK") {
    (risk.value.riskList || []).forEach((r, i) => {
      if (riskChecked.value[i]) push({ assessmentCd: r.assessmentCd }, r);
    });
  } else if (code === "TBM") {
    victimAxis.value.forEach((v) =>
      pushVictimItems(v, tbmByVictim[victimKeyOf(v)], "sessionCd")
    );
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
/* prafta-065 재해자 칩 바(ATTD/TBM) */
.lc-victim-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.7rem;
}
.lc-victim-bar__label {
  font-size: 0.74rem;
  font-weight: 700;
  color: var(--color-text-muted, #4b5563);
  margin-right: 0.2rem;
}
.lc-victim-chip {
  border: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-surface, #fff);
  color: var(--color-text, #374151);
  font-size: 0.76rem;
  padding: 0.25rem 0.7rem;
  border-radius: var(--radius-pill, 999px);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}
.lc-victim-chip.active {
  border-color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.1));
  color: var(--color-primary-hover, #15803d);
  font-weight: 700;
}
.lc-victim-chip__rep {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.02rem 0.3rem;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-primary, #16a34a);
  color: var(--color-surface, #fff);
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
/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-foot .btn {
  flex: 1;
}
</style>
