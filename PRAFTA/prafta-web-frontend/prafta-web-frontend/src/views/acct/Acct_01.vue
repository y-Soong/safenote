<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenCreate"
    />

    <!-- 검색바 -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          @blur="focusKill"
        />
        <button class="search-btn" @click="fnSiteSearchPopOpen">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>재해등급</label>
        <select v-model="acctGradeCd" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in systCodeArr['SYS065'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>처리상태</label>
        <select v-model="processStatusCd" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in systCodeArr['SYS066'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>발생기간</label>
        <CalendarSrch v-model="startDate" />
        <span class="date-range-sep">~</span>
        <CalendarSrch v-model="endDate" />
      </div>

      <div>
        <label>재해자명</label>
        <input v-model.trim="searchKeyword" type="text" @keyup.enter="fnSearch" />
      </div>
    </div>

    <!-- 본문: 좌 목록 + 우 상세 -->
    <div class="viewBody acc-split">
      <!-- 좌측: 사고 목록 -->
      <div class="acc-list-pane">
        <div class="acc-list-head">사고 목록 ({{ acctList.length }})</div>
        <div class="acc-list-scroll">
          <div
            v-if="acctList.length === 0"
            class="acc-empty"
          >
            등록된 사고가 없습니다.
          </div>
          <div
            v-for="a in acctList"
            :key="a.acctId"
            class="acc-item"
            :class="{ active: current && current.acctId === a.acctId }"
            @click="fnSelect(a)"
          >
            <div class="acc-item-r1">
              <span class="acc-name">{{ a.victimUserNm }}</span>
              <span class="grade" :class="gradeClass(a.acctGradeCd)">
                {{ a.acctGradeNm || a.acctGradeCd }}
              </span>
              <span class="st" :class="statusClass(a.processStatusCd)">
                {{ a.processStatusNm || a.processStatusCd }}
              </span>
            </div>
            <div class="acc-meta">
              {{ fmtYmd(a.occurYmd) }} {{ fmtHm(a.occurTime) }} · {{ a.acctId }}
            </div>
            <div class="acc-site">{{ a.siteNm }}</div>
          </div>
        </div>
        <div class="acc-legend">
          <b class="g-crit">중대재해</b> 사망1↑·3개월요양 2↑·10명↑<br />
          <b class="g-norm">일반산재</b> 사망 또는 3일↑ 휴업<br />
          <b class="g-exem">신고제외</b> 3일 미만 휴업 (기록·보존만)
        </div>
      </div>

      <!-- 우측: 상세 -->
      <div class="acc-detail-pane">
        <template v-if="!current">
          <div class="acc-empty acc-detail-empty">
            좌측에서 사고를 선택하세요.
          </div>
        </template>
        <template v-else>
          <div class="acc-det-head">
            <h1>{{ current.victimUserNm }}</h1>
            <span class="grade" :class="gradeClass(current.acctGradeCd)">
              {{ current.acctGradeNm || current.acctGradeCd }}
            </span>
            <span class="st" :class="statusClass(current.processStatusCd)">
              {{ current.processStatusNm || current.processStatusCd }}
            </span>
          </div>
          <div class="acc-det-sub">
            {{ current.acctId }} · 사고일시
            <b>{{ fmtYmd(current.occurYmd) }} {{ fmtHm(current.occurTime) }}</b>
            <template v-if="current.occurPlace"> · {{ current.occurPlace }}</template>
            · {{ current.siteNm }}
          </div>

          <!-- 탭 -->
          <div class="acc-tabs">
            <button
              v-for="t in tabs"
              :key="t.key"
              class="acc-tab"
              :class="{ active: activeTab === t.key }"
              @click="fnSelectTab(t.key)"
            >
              {{ t.label }}
            </button>
          </div>

          <!-- ① 안전관리 현황 -->
          <section v-show="activeTab === 'context'">
            <div v-if="snapshotList.length === 0" class="acc-empty">
              확정된 연계 데이터가 없습니다. 사고 등록 후 연계 데이터를 확정하세요.
            </div>
            <div v-else class="acc-snap-grid">
              <div
                v-for="(group, domain) in snapshotByDomain"
                :key="domain"
                class="acc-card"
              >
                <h3>
                  <span class="dot"></span>{{ domainLabel(domain) }}
                  <span class="acc-card-sub">· {{ group.length }}건 확정</span>
                </h3>
                <div
                  v-for="(s, i) in group"
                  :key="i"
                  class="acc-snap-row"
                >
                  <span class="acc-snap-lead">{{ s.linkSeq }}</span>
                  <span class="acc-snap-body">{{ snapshotSummary(domain, s) }}</span>
                </div>
              </div>
            </div>
            <div class="acc-legend2">
              ※ 모든 항목은 본 시스템 기록 기준이며, 사고일 시점의 확정 스냅샷을
              표시합니다. '기록 없음'은 행위 부재가 아니라 입력 부재일 수
              있습니다.
            </div>
          </section>

          <!-- ② 법정 처리 / 기한 -->
          <section v-show="activeTab === 'actions'">
            <div class="acc-banner" v-if="isCriticalOpen">
              ⚠ <b>중대재해</b> — 발생보고는 "지체없이" 이행해야 합니다. 시스템은
              기한을 계산하지 않으니 즉시 보고 후 완료 처리하세요.
            </div>
            <div class="acc-notice" v-if="legalNotice">ⓘ {{ legalNotice }}</div>

            <div v-if="legalStepList.length === 0" class="acc-empty">
              해당 등급의 법정 절차 정의가 없습니다.
            </div>
            <div v-else class="acc-action-grid">
              <div
                v-for="(step, i) in legalStepList"
                :key="step.stepCd"
                class="acc-action"
                :class="actionClass(step)"
              >
                <div class="acc-action-num" :class="{ done: step.isDoneYn === 'Y' }">
                  {{ step.isDoneYn === "Y" ? "✓" : i + 1 }}
                </div>
                <div class="acc-action-info">
                  <div class="acc-action-t">{{ step.stepNm }}</div>
                  <div class="acc-action-d">{{ step.actionGuide }}</div>
                  <div class="acc-action-law" v-if="step.legalBasis">
                    {{ step.legalBasis }}
                  </div>
                  <div class="acc-action-note" v-if="step.stepNote">
                    {{ step.stepNote }}
                  </div>
                  <div class="acc-action-remark">
                    <input
                      type="text"
                      v-model.trim="step._remark"
                      placeholder="비고 입력"
                    />
                  </div>
                </div>
                <div class="acc-action-deadline">
                  <div class="dl-label">{{ deadlineInfo(step).label }}</div>
                  <div class="dl-val" :class="deadlineInfo(step).cls">
                    {{ deadlineInfo(step).text }}
                  </div>
                  <label class="acc-done-check">
                    <input
                      type="checkbox"
                      :checked="step.isDoneYn === 'Y'"
                      @change="fnToggleDone(step, $event)"
                    />
                    조치완료
                  </label>
                </div>
              </div>
            </div>

            <div class="acc-save-row" v-if="legalStepList.length">
              <button class="btn btn-primary" @click="fnSaveLegalSteps">
                저장
              </button>
            </div>

            <div class="acc-legend2">
              ※ 의무·기한은 재해등급에 따라 분기됩니다. 당국 신고와 근로복지공단
              요양급여 신청은 독립된 별도 트랙입니다. 기한·등급 기준은 노무사
              최종 확인 대상이며, 본 화면은 실무 보조용입니다.
            </div>
          </section>

          <!-- ③ 처리 이력 -->
          <section v-show="activeTab === 'log'">
            <div v-if="historyList.length === 0" class="acc-empty">
              완료 처리된 절차 이력이 없습니다.
            </div>
            <div v-else class="acc-timeline">
              <div
                v-for="(h, i) in historyList"
                :key="i"
                class="acc-tl-item"
              >
                <div class="acc-tl-head">
                  <span class="acc-tl-cat">{{ h.stepNm }}</span>
                  <span class="acc-tl-date">{{ h.doneDtime }}</span>
                </div>
                <div class="acc-tl-body" v-if="h.remark">{{ h.remark }}</div>
                <div class="acc-tl-author" v-if="h.doneUserNm">
                  처리 · {{ h.doneUserNm }}
                </div>
              </div>
            </div>
            <div class="acc-legend2">
              ※ 처리 이력은 ② 법정 처리 단계에서 완료한 절차를 완료일시순으로
              롤업한 읽기전용 뷰입니다.
            </div>
          </section>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import AcctCreatePop from "./popup/AcctCreatePop.vue";
import AcctLinkConfirmPop from "./popup/AcctLinkConfirmPop.vue";

defineOptions({ name: "Acct_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();

// 목록/조회조건
const acctList = ref([]);
const current = ref(null);
const systCodeArr = ref({});
const acctGradeCd = ref("");
const processStatusCd = ref("");
const startDate = ref();
const endDate = ref();
const searchKeyword = ref("");

// 사업장
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");

// 탭
const tabs = [
  { key: "context", label: "① 안전관리 현황 (사고일 기준)" },
  { key: "actions", label: "② 법정 처리 / 기한" },
  { key: "log", label: "③ 처리 이력" },
];
const activeTab = ref("context");

// 탭 데이터
const snapshotList = ref([]);
const legalStepList = ref([]);
const legalOccurYmd = ref("");
const legalNotice = ref("");
const historyList = ref([]);

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

// 조회 + 생성만 노출 (저장/삭제/엑셀은 상세 탭/별도)
const fnButtonControll = () => {
  localButtons.value.search = "Y";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// SYS065(재해등급) / SYS066(처리상태) / SYS067(연계도메인)
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS065", "SYS066", "SYS067"] },
    });
    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "코드 조회 중 오류가 발생했습니다.")
    );
  }
};

// 사고 목록 조회
const fnSearch = async () => {
  acctList.value = [];
  try {
    const response = await axios.get("/webApi/acct01/list", {
      params: {
        siteCd: siteCd.value,
        acctGradeCd: acctGradeCd.value,
        processStatusCd: processStatusCd.value,
        startDate: startDate.value,
        endDate: endDate.value,
        searchKeyword: searchKeyword.value,
      },
    });
    if (response.status === 200) {
      acctList.value = response.data?.acctList || [];
      // 현재 선택 유지 또는 첫 행 선택
      if (current.value) {
        const found = acctList.value.find(
          (a) => a.acctId === current.value.acctId
        );
        if (found) {
          await fnSelect(found);
          return;
        }
      }
      current.value = null;
      snapshotList.value = [];
      legalStepList.value = [];
      historyList.value = [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// 사고 선택 → 활성 탭 데이터 로드
const fnSelect = async (a) => {
  current.value = a;
  await fnLoadTab(activeTab.value);
};

const fnSelectTab = async (key) => {
  activeTab.value = key;
  await fnLoadTab(key);
};

const fnLoadTab = async (key) => {
  if (!current.value) return;
  if (key === "context") await fnLoadSnapshot();
  else if (key === "actions") await fnLoadLegalSteps();
  else if (key === "log") await fnLoadHistory();
};

// ① 스냅샷
const fnLoadSnapshot = async () => {
  snapshotList.value = [];
  try {
    const response = await axios.get("/webApi/acct01/link/snapshot", {
      params: {
        siteCd: current.value.siteCd,
        acctId: current.value.acctId,
        linkDomainCd: "",
      },
    });
    if (response.status === 200) {
      snapshotList.value = response.data?.snapshotList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "안전관리 현황 조회 중 오류가 발생했습니다.")
    );
  }
};

// ② 법정 절차
const fnLoadLegalSteps = async () => {
  legalStepList.value = [];
  try {
    const response = await axios.get("/webApi/acct01/legal-step/list", {
      params: {
        siteCd: current.value.siteCd,
        acctId: current.value.acctId,
      },
    });
    if (response.status === 200) {
      const d = response.data || {};
      legalOccurYmd.value = d.occurYmd || current.value.occurYmd || "";
      legalNotice.value = d.notice || "";
      legalStepList.value = (d.legalStepList || []).map((s) => ({
        ...s,
        _remark: s.remark || "",
        // 변경 행만 전송하기 위한 로드 시점 원본 스냅샷
        _origDoneYn: s.isDoneYn || "N",
        _origRemark: s.remark || "",
      }));
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "법정 절차 조회 중 오류가 발생했습니다.")
    );
  }
};

// ③ 처리 이력
const fnLoadHistory = async () => {
  historyList.value = [];
  try {
    const response = await axios.get("/webApi/acct01/legal-step/history", {
      params: {
        siteCd: current.value.siteCd,
        acctId: current.value.acctId,
      },
    });
    if (response.status === 200) {
      historyList.value = response.data?.historyList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 이력 조회 중 오류가 발생했습니다.")
    );
  }
};

// 조치완료 체크 토글 (로컬 상태만; 저장은 일괄 fnSaveLegalSteps)
const fnToggleDone = (step, e) => {
  step.isDoneYn = e.target.checked ? "Y" : "N";
};

// 법정 절차 저장 (변경 행만 UPSERT)
const fnSaveLegalSteps = async () => {
  try {
    // 로드 시점 대비 완료여부/비고가 바뀐 행만 전송한다(불필요한 재기록 방지).
    const changed = legalStepList.value.filter(
      (step) =>
        (step.isDoneYn || "N") !== (step._origDoneYn || "N") ||
        (step._remark || "") !== (step._origRemark || "")
    );
    if (!changed.length) {
      await proxy.$alert("변경된 내용이 없습니다.");
      return;
    }
    for (const step of changed) {
      await axios.post("/webApi/acct01/legal-step/save", {
        siteCd: current.value.siteCd,
        acctId: current.value.acctId,
        stepCd: step.stepCd,
        isDoneYn: step.isDoneYn || "N",
        remark: step._remark || "",
      });
    }
    await proxy.$alert("저장되었습니다.");
    await fnLoadLegalSteps();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  }
};

// 사업장 검색 팝업
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: (cd, no, nm) => {
      siteCd.value = cd;
      siteNo.value = no;
      siteNm.value = nm;
    },
  });
};

const focusKill = (e) => {
  if (e.target.id === "siteNo" && proxy.$util.isEmpty(siteNo.value)) {
    siteCd.value = "";
    siteNm.value = "";
  } else if (e.target.id === "siteNm" && proxy.$util.isEmpty(siteNm.value)) {
    siteCd.value = "";
    siteNo.value = "";
  }
};

// 사고 등록 팝업 → 등록 후 수평선 확정 팝업
const fnOpenCreate = () => {
  openPop(AcctCreatePop, {
    onCreated: (cond) => {
      fnOpenLinkConfirm(cond);
    },
  });
};

const fnOpenLinkConfirm = (cond) => {
  openPop(AcctLinkConfirmPop, {
    acctId: cond.acctId,
    siteCd: cond.siteCd,
    chklstType: cond.chklstType,
    chkptCds: cond.chkptCds,
    processCd: cond.processCd,
    riskTypeCd: cond.riskTypeCd,
    hazardCd: cond.hazardCd,
    incidentTypeCd: cond.incidentTypeCd,
    potentialSeverityCd: cond.potentialSeverityCd,
    onConfirmed: async () => {
      await fnSearch();
    },
  });
};

// ── 스냅샷 도메인 그룹핑 ──
const snapshotByDomain = computed(() => {
  const map = {};
  snapshotList.value.forEach((s) => {
    if (!map[s.linkDomainCd]) map[s.linkDomainCd] = [];
    map[s.linkDomainCd].push(s);
  });
  return map;
});

const domainLabel = (code) => {
  const found = (systCodeArr.value["SYS067"] || []).find(
    (c) => c.systValDCd === code
  );
  if (found) return found.systValDNm;
  const fb = {
    ATTD: "근태",
    CHKPT: "순회점검",
    RISK: "위험성평가",
    TBM: "TBM",
    NEAR_MISS: "아차사고",
  };
  return fb[code] || code;
};

// 스냅샷 1건 요약(snapshotJson 파싱; 도메인별 핵심 필드)
const snapshotSummary = (domain, s) => {
  let snap = {};
  try {
    snap = s.snapshotJson ? JSON.parse(s.snapshotJson) : {};
  } catch (e) {
    snap = {};
  }
  if (domain === "ATTD") {
    return `구간 ${snap.workSeq ?? "-"} · 출근 ${snap.checkInTime || "-"} / 퇴근 ${snap.checkOutTime || "-"}`;
  }
  if (domain === "CHKPT") {
    return `${snap.chkptNm || "-"} · 양호 ${snap.goodCnt ?? 0} / 불량 ${snap.badCnt ?? 0}`;
  }
  if (domain === "RISK") {
    return `${snap.assessmentCd || "-"} · ${[snap.processNm, snap.riskTypeNm, snap.hazardNm].filter(Boolean).join("/")} · 위험도 ${snap.initRiskLv || "-"}`;
  }
  if (domain === "TBM") {
    return `${snap.title || "-"} · 재해자 이수 ${snap.victimCompletionStatusNm || (snap.victimCompletionStatusCd ? snap.victimCompletionStatusCd : "기록없음")}`;
  }
  if (domain === "NEAR_MISS") {
    return `${snap.nearMissId || "-"} · ${snap.incidentTypeNm || ""} · ${snap.occurDtime || ""}`;
  }
  return s.linkKeyJson || "";
};

// ── 법정 기한/D-day 계산 (목업 renderActions 로직) ──
const today = new Date();
today.setHours(0, 0, 0, 0);

const parseYmd = (ymd) => {
  if (!ymd || ymd.length < 8) return null;
  return new Date(
    Number(ymd.slice(0, 4)),
    Number(ymd.slice(4, 6)) - 1,
    Number(ymd.slice(6, 8))
  );
};

const fmtDl = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

const daysBtw = (a, b) => Math.round((b - a) / 864e5);

// 발생일 + 1개월 기한 (계산 기준은 현행 유지: 발생일 기준 1개월 후).
// JS setMonth 의 말일 롤오버 보정: 1/31 + 1개월이 3/3 으로 튀지 않도록
// 목표 월의 말일로 클램프한다(예: 1/31 -> 2/28 또는 2/29).
const monthPlus1Deadline = () => {
  const occur = parseYmd(legalOccurYmd.value);
  if (!occur) return null;
  const day = occur.getDate();
  const d = new Date(occur);
  d.setDate(1);
  d.setMonth(d.getMonth() + 1);
  // 목표 월의 말일 = 다음 달 0일
  const lastDay = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, lastDay));
  return d;
};

const deadlineInfo = (step) => {
  const rule = step.deadlineRuleCd;
  const done = step.isDoneYn === "Y";
  if (rule === "MONTH_PLUS_1") {
    const dl = monthPlus1Deadline();
    if (done) return { label: "제출", text: "완료", cls: "ok" };
    if (!dl) return { label: "제출기한", text: "-", cls: "" };
    const left = daysBtw(today, dl);
    if (left < 0)
      return {
        label: "제출기한",
        text: `${fmtDl(dl)} · ${Math.abs(left)}일 경과`,
        cls: "over",
      };
    if (left <= 7)
      return { label: "제출기한", text: `${fmtDl(dl)} · D-${left}`, cls: "soon" };
    return { label: "제출기한", text: `${fmtDl(dl)} · D-${left}`, cls: "ok" };
  }
  if (rule === "IMMEDIATE") {
    if (done) return { label: "보고", text: "완료", cls: "ok" };
    return { label: "기한", text: "지체없이", cls: "imm" };
  }
  if (rule === "TRACK") {
    return done
      ? { label: "진행", text: "완료", cls: "ok" }
      : { label: "진행", text: "별도 트랙", cls: "" };
  }
  // NONE 등
  return done
    ? { label: "상태", text: "완료", cls: "ok" }
    : { label: "상태", text: "미완료", cls: "soon" };
};

const actionClass = (step) => {
  if (step.isDoneYn === "Y") return "complete";
  const info = deadlineInfo(step);
  if (info.cls === "over" || info.cls === "imm") return "alert";
  if (info.cls === "soon" && step.deadlineRuleCd === "MONTH_PLUS_1")
    return "warnbox";
  return "";
};

const isCriticalOpen = computed(() => {
  if (!current.value) return false;
  return (
    current.value.acctGradeCd === "100" &&
    current.value.processStatusCd !== "300"
  );
});

// ── 칩/포맷 ──
const gradeClass = (code) => {
  if (code === "100") return "critical";
  if (code === "200") return "normal";
  if (code === "300") return "exempt";
  return "";
};
const statusClass = (code) => {
  if (code === "100") return "open";
  if (code === "200") return "proc";
  if (code === "300") return "done";
  return "";
};
const fmtYmd = (ymd) =>
  ymd && ymd.length >= 8
    ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
    : ymd || "";
const fmtHm = (hhmm) => {
  if (!hhmm) return "";
  const s = String(hhmm).padStart(4, "0");
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`;
};
</script>

<style scoped>
.date-range-sep {
  margin: 0 var(--space-xs, 0.25rem);
}
.acc-split {
  display: flex;
  gap: 1rem;
  align-items: stretch;
}
.acc-list-pane {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--card-radius, 16px);
  background: var(--color-surface, #fff);
  overflow: hidden;
}
.acc-list-head {
  padding: 0.75rem 1rem;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-muted, #8b94a3);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.acc-list-scroll {
  flex: 1;
  overflow-y: auto;
  max-height: 64vh;
}
.acc-item {
  padding: 0.8rem 1rem;
  border-bottom: 1px solid var(--color-border, #eef0f3);
  cursor: pointer;
  border-left: 3px solid transparent;
}
.acc-item:hover {
  background: var(--color-bg, #f9fafb);
}
.acc-item.active {
  background: var(--color-primary-soft, #f0fdf4);
  border-left-color: var(--color-primary, #16a34a);
}
.acc-item-r1 {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.3rem;
  flex-wrap: wrap;
}
.acc-name {
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--color-text-strong, #111827);
}
.acc-meta {
  font-size: 0.72rem;
  color: var(--color-text-muted, #8b94a3);
}
.acc-site {
  font-size: 0.75rem;
  color: var(--color-text-muted, #5b6472);
  margin-top: 0.2rem;
}
.acc-legend {
  font-size: 0.68rem;
  color: var(--color-text-muted, #8b94a3);
  line-height: 1.65;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
}
.acc-legend .g-crit {
  color: var(--color-danger, #ef4444);
}
.acc-legend .g-norm {
  color: var(--color-warning-text, #b45309);
}
.acc-legend .g-exem {
  color: var(--color-text-muted, #475569);
}
.acc-detail-pane {
  flex: 1;
  min-width: 0;
  padding: 0.5rem 0.25rem;
  overflow-y: auto;
  max-height: 72vh;
}
.acc-detail-empty {
  padding: 4rem 1rem;
  text-align: center;
}
.acc-empty {
  padding: 1.5rem;
  text-align: center;
  color: var(--color-text-muted, #8b94a3);
  font-size: 0.85rem;
}
/* 등급/상태 칩 */
.grade {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 5px;
  white-space: nowrap;
}
.grade.critical {
  background: var(--danger-tint, #fef2f2);
  color: var(--color-danger, #ef4444);
}
.grade.normal {
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
}
.grade.exempt {
  background: var(--color-bg, #f1f5f9);
  color: var(--color-text-muted, #475569);
}
.st {
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.55rem;
  border-radius: 20px;
  white-space: nowrap;
}
.st.open {
  background: var(--color-primary-soft, #dcfce7);
  color: var(--color-primary-hover, #15803d);
}
.st.proc {
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
}
.st.done {
  background: var(--color-bg, #f1f5f9);
  color: var(--color-text-muted, #64748b);
}
/* 상세 헤더 */
.acc-det-head {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.3rem;
  flex-wrap: wrap;
}
.acc-det-head h1 {
  font-size: 1.3rem;
  font-weight: 800;
  color: var(--color-text-strong, #111827);
}
.acc-det-sub {
  color: var(--color-text-muted, #5b6472);
  font-size: 0.78rem;
  margin-bottom: 1rem;
}
.acc-det-sub b {
  color: var(--color-text-strong, #111827);
}
/* 탭 */
.acc-tabs {
  display: flex;
  gap: 0.25rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  margin-bottom: 1.1rem;
}
.acc-tab {
  padding: 0.55rem 0.9rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted, #8b94a3);
  cursor: pointer;
  border: none;
  background: transparent;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.acc-tab:hover {
  color: var(--color-text, #374151);
}
.acc-tab.active {
  color: var(--color-primary-hover, #15803d);
  border-bottom-color: var(--color-primary, #16a34a);
}
/* ① 스냅샷 카드 */
.acc-snap-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.9rem;
}
.acc-card {
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 1rem 1.1rem;
}
.acc-card h3 {
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-text-muted, #8b94a3);
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
}
.acc-card h3 .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary, #16a34a);
}
.acc-card-sub {
  color: var(--color-text-muted, #8b94a3);
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
  font-size: 0.68rem;
}
.acc-snap-row {
  display: flex;
  gap: 0.6rem;
  padding: 0.45rem 0;
  border-bottom: 1px solid var(--color-border, #eef0f3);
  font-size: 0.8rem;
}
.acc-snap-row:last-child {
  border-bottom: none;
}
.acc-snap-lead {
  color: var(--color-text-muted, #8b94a3);
  min-width: 1.5rem;
}
.acc-snap-body {
  flex: 1;
  color: var(--color-text, #374151);
}
.acc-legend2 {
  font-size: 0.7rem;
  color: var(--color-text-muted, #8b94a3);
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  line-height: 1.6;
}
/* ② 법정 절차 */
.acc-banner {
  padding: 0.7rem 0.95rem;
  border-radius: 9px;
  font-size: 0.8rem;
  margin-bottom: 1rem;
  background: var(--danger-tint, #fef2f2);
  border: 1px solid var(--color-danger, #fca5a5);
  color: var(--color-danger, #b91c1c);
  font-weight: 500;
}
.acc-notice {
  background: var(--color-warning-bg, #fffbeb);
  border: 1px solid var(--color-warning-bg, #fde68a);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-warning-text, #92400e);
  margin-bottom: 0.75rem;
}
.acc-action-grid {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.acc-action {
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.9rem 1rem;
  display: flex;
  align-items: flex-start;
  gap: 0.95rem;
}
.acc-action.alert {
  border-color: var(--color-danger, #fca5a5);
  background: var(--danger-tint, #fef2f2);
}
.acc-action.warnbox {
  border-color: var(--color-warning-text, #fcd34d);
  background: var(--color-warning-bg, #fffbeb);
}
.acc-action.complete {
  border-color: var(--color-primary-soft, #dcfce7);
}
.acc-action-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.8rem;
  background: var(--color-bg, #f1f5f9);
  color: var(--color-text-muted, #8b94a3);
  flex-shrink: 0;
}
.acc-action-num.done {
  background: var(--color-primary, #16a34a);
  color: #fff;
}
.acc-action-info {
  flex: 1;
  min-width: 0;
}
.acc-action-t {
  font-weight: 700;
  font-size: 0.88rem;
  margin-bottom: 0.15rem;
  color: var(--color-text-strong, #111827);
}
.acc-action-d {
  font-size: 0.78rem;
  color: var(--color-text, #5b6472);
}
.acc-action-law {
  font-size: 0.7rem;
  color: var(--color-text-muted, #8b94a3);
  margin-top: 0.2rem;
}
.acc-action-note {
  font-size: 0.72rem;
  color: var(--color-text-muted, #8b94a3);
  margin-top: 0.35rem;
  line-height: 1.5;
}
.acc-action-remark {
  margin-top: 0.5rem;
}
.acc-action-remark input {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0.4rem 0.6rem;
  font-size: 0.78rem;
  font-family: "Pretendard", sans-serif;
  background: var(--color-surface, #fff);
}
.acc-action-remark input:focus {
  outline: none;
  border-color: var(--color-primary, #16a34a);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
.acc-action-deadline {
  text-align: right;
  min-width: 130px;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.dl-label {
  font-size: 0.66rem;
  color: var(--color-text-muted, #8b94a3);
}
.dl-val {
  font-weight: 700;
  font-size: 0.78rem;
}
.dl-val.over,
.dl-val.imm {
  color: var(--color-danger, #ef4444);
}
.dl-val.soon {
  color: var(--color-warning-text, #b45309);
}
.dl-val.ok {
  color: var(--color-primary-hover, #15803d);
}
.acc-done-check {
  font-size: 0.72rem;
  color: var(--color-text, #374151);
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  justify-content: flex-end;
  margin-top: 0.3rem;
  cursor: pointer;
}
.acc-save-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}
/* ③ 처리 이력 타임라인 */
.acc-timeline {
  position: relative;
  padding-left: 1.5rem;
}
.acc-timeline::before {
  content: "";
  position: absolute;
  left: 7px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: var(--color-border, #e5e7eb);
}
.acc-tl-item {
  position: relative;
  padding: 0 0 1.1rem 0;
}
.acc-tl-item::before {
  content: "";
  position: absolute;
  left: -1.31rem;
  top: 3px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-surface, #fff);
  border: 2px solid var(--color-primary, #16a34a);
}
.acc-tl-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.2rem;
}
.acc-tl-cat {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 5px;
  background: var(--color-primary-soft, #dcfce7);
  color: var(--color-primary-hover, #15803d);
}
.acc-tl-date {
  font-size: 0.72rem;
  color: var(--color-text-muted, #8b94a3);
}
.acc-tl-body {
  font-size: 0.82rem;
  color: var(--color-text, #374151);
  margin-bottom: 0.15rem;
}
.acc-tl-author {
  font-size: 0.72rem;
  color: var(--color-text-muted, #8b94a3);
}
</style>
