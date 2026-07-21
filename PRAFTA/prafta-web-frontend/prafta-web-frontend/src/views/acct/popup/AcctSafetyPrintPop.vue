<template>
  <!-- 통합 안전관리 현황 출력 팝업: 3섹션(순회점검 / 위험성평가 / 근태+TBM 합본)을
       prev/next 로 넘겨보며, 각 섹션마다 "인쇄" 버튼이 해당 섹션을 출력한다.
       전역 모달 클래스(modal.css)와 useCenteredDraggable 패턴은 ChkLstRstPop 을 따른다. -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide acc-print-modal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span
            >안전관리 현황 출력 — {{ props.victimUserNm }} /
            {{ props.acctId }}</span
          >
          <button
            type="button"
            class="icon-button"
            @mousedown.stop
            @click.stop="$emit('close')"
          >
            ✕
          </button>
        </div>

        <!-- 섹션 네비게이션 -->
        <div class="acc-print-nav">
          <button
            type="button"
            class="acc-print-nav__btn"
            :disabled="sectionIdx === 0"
            @click="fnPrevSection"
          >
            ◀
          </button>
          <div class="acc-print-nav__center">
            <div class="acc-print-nav__idx">
              {{ sectionIdx + 1 }} / {{ sections.length }}
            </div>
            <div class="acc-print-nav__title">{{ currentSection.label }}</div>
          </div>
          <button
            type="button"
            class="acc-print-nav__btn"
            :disabled="sectionIdx === sections.length - 1"
            @click="fnNextSection"
          >
            ▶
          </button>
        </div>

        <!-- 본문 (섹션별 스위치) -->
        <div class="modal-body acc-print-body">
          <!-- 로딩 -->
          <div v-if="isLoading" class="acc-print-state">불러오는 중...</div>

          <template v-else>
            <!-- ① 순회점검 ─────────────────────────────────────────── -->
            <section
              v-show="currentSection.key === 'chkpt'"
              class="acc-print-section"
            >
              <div class="acc-print-section__head">
                <h3>순회점검 점검결과 확인서</h3>
                <button
                  type="button"
                  class="btn btn-primary"
                  :disabled="chkptInfoList.length === 0"
                  @click="fnOpenChkLstResult"
                >
                  점검결과 확인서 인쇄
                </button>
              </div>
              <div v-if="chkptInfoList.length === 0" class="acc-print-state">
                연계된 순회점검 점검대상이 없습니다.
              </div>
              <ul v-else class="acc-print-list">
                <li v-for="(c, i) in chkptInfoList" :key="i">
                  <span class="acc-print-list__lead">{{ c.chkptNm }}</span>
                  <span class="acc-print-list__sub">
                    {{ c.chkLstTypeNm }} · {{ c.workMonth }}
                  </span>
                </li>
              </ul>
              <p class="acc-print-note">
                ※ 점검결과 확인서는 사고일이 속한 월(月) 기준으로 라이브
                조회됩니다.
              </p>
            </section>

            <!-- ② 위험성평가 ────────────────────────────────────────── -->
            <section
              v-show="currentSection.key === 'risk'"
              class="acc-print-section"
            >
              <div class="acc-print-section__head">
                <h3>위험성평가 (개선실행계획서 / 개선완료보고서)</h3>
              </div>
              <div v-if="riskList.length === 0" class="acc-print-state">
                연계된 위험성평가가 없습니다.
              </div>
              <ul v-else class="acc-print-list acc-print-list--risk">
                <li v-for="(r, i) in riskList" :key="i">
                  <div class="acc-print-list__main">
                    <span class="acc-print-list__lead">{{
                      r.assessmentCd
                    }}</span>
                    <span class="acc-print-list__sub">
                      {{
                        [r.processNm, r.riskTypeNm, r.hazardNm]
                          .filter(Boolean)
                          .join(" / ")
                      }}
                    </span>
                  </div>
                  <div class="acc-print-list__actions">
                    <button
                      type="button"
                      class="btn btn-outline"
                      @click="fnOpenRiskImprovementPlan(r)"
                    >
                      개선실행계획서
                    </button>
                    <button
                      type="button"
                      class="btn btn-primary"
                      @click="fnOpenRiskCompleteReport(r)"
                    >
                      개선완료보고서
                    </button>
                  </div>
                </li>
              </ul>
              <p class="acc-print-note">
                ※ 식별자(assessmentCd)는 사고 스냅샷에서, 본문은 사고일/사업장
                스코프로 라이브 조회됩니다.
              </p>
            </section>

            <!-- ③ 근태 + TBM 합본 (신규 출력물) ───────────────────────── -->
            <section
              v-show="currentSection.key === 'attdTbm'"
              class="acc-print-section"
            >
              <div class="acc-print-section__head">
                <h3>근태(스케줄 + 실근태) + TBM 교육 합본</h3>
                <button
                  type="button"
                  class="btn btn-primary"
                  @click="fnPrintAttdTbm"
                >
                  인쇄
                </button>
              </div>

              <!-- 인쇄 영역(패턴 A: 아래 DOM 을 그대로 자체 인쇄창으로 출력) -->
              <div ref="attdTbmPrintArea" class="acc-report">
                <!-- 사고 헤더 -->
                <h2 class="acc-report__title">근태 · TBM 안전관리 현황</h2>
                <table class="acc-report__meta">
                  <tbody>
                    <tr>
                      <th>사고 ID</th>
                      <td>{{ props.acctId }}</td>
                      <th>발생일시</th>
                      <td>
                        {{ fmtYmd(props.occurYmd) }}
                        {{ fmtHm(props.occurTime) }}
                      </td>
                    </tr>
                    <tr>
                      <th>피해자</th>
                      <td>{{ props.victimUserNm }} ({{ victimTypeLabel }})</td>
                      <th>재해등급</th>
                      <td>{{ props.acctGradeNm }}</td>
                    </tr>
                    <tr>
                      <th>발생장소</th>
                      <td>{{ props.occurPlace || "-" }}</td>
                      <th>사업장</th>
                      <td>{{ props.siteNm }}</td>
                    </tr>
                  </tbody>
                </table>

                <!-- 근무 스케줄 구간표 -->
                <h3 class="acc-report__sub">근무 스케줄 (사고일 기준)</h3>
                <div v-if="attdData.scheduleNote" class="acc-report__empty">
                  {{ attdData.scheduleNote }}
                </div>
                <table v-else-if="attdData.schedule" class="acc-report__tbl">
                  <thead>
                    <tr>
                      <th>구분</th>
                      <th>시작</th>
                      <th>종료</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>1차</td>
                      <td>{{ fmtHm(attdData.schedule.fstSchStrTime) }}</td>
                      <td>{{ fmtHm(attdData.schedule.fstSchEndTime) }}</td>
                    </tr>
                    <tr v-if="attdData.schedule.secSchStrTime">
                      <td>2차</td>
                      <td>{{ fmtHm(attdData.schedule.secSchStrTime) }}</td>
                      <td>{{ fmtHm(attdData.schedule.secSchEndTime) }}</td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="acc-report__empty">
                  스케줄 기록 없음 (연차/휴무 가능)
                </div>

                <!-- 실근태 차수표 -->
                <h3 class="acc-report__sub">실근태 (차수별)</h3>
                <table class="acc-report__tbl">
                  <thead>
                    <tr>
                      <th>차수</th>
                      <th>출근</th>
                      <th>퇴근</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="attdData.records.length === 0">
                      <td colspan="3" class="acc-report__empty-cell">
                        실근태 기록 없음
                      </td>
                    </tr>
                    <tr v-for="(rec, i) in attdData.records" :key="i">
                      <td>{{ rec.workSeq }}</td>
                      <td>
                        {{ fmtYmd(rec.checkInDate) }}
                        {{ fmtHm(rec.checkInTime) }}
                      </td>
                      <td>
                        <template v-if="rec.checkOutTime">
                          {{ fmtYmd(rec.checkOutDate) }}
                          {{ fmtHm(rec.checkOutTime) }}
                        </template>
                        <template v-else>미퇴근</template>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <!-- 사고일 TBM 세션·이수표 -->
                <h3 class="acc-report__sub">TBM 교육 (사고 당일)</h3>
                <table class="acc-report__tbl">
                  <thead>
                    <tr>
                      <th>세션</th>
                      <th>상태</th>
                      <th>관리자</th>
                      <th>개설일시</th>
                      <th>재해자 이수</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="attdData.tbmList.length === 0">
                      <td colspan="5" class="acc-report__empty-cell">
                        당일 TBM 기록 없음
                      </td>
                    </tr>
                    <tr v-for="(t, i) in attdData.tbmList" :key="i">
                      <td>{{ t.title }}</td>
                      <td>{{ t.statusNm }}</td>
                      <td>{{ t.managerUserNm }}</td>
                      <td>{{ t.openedAt }}</td>
                      <td>{{ t.victimCompletionStatusNm || "기록없음" }}</td>
                    </tr>
                  </tbody>
                </table>

                <p class="acc-report__foot">
                  ※ 본 출력물은 본 시스템 기록 기준이며, '기록 없음'은 행위
                  부재가 아니라 입력 부재일 수 있습니다.
                </p>
              </div>
            </section>
          </template>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              type="button"
              class="btn btn-primary"
              @click="$emit('close')"
            >
              닫기
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
  onMounted,
  getCurrentInstance,
  defineProps,
  defineEmits,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot, formatHm } from "@/utils/dateFormat";
import { printImprovementPlan } from "@/utils/print/riskImprovementPlanPrint";
import ChkLstRstPop from "@/views/chkLst/popup/ChkLstRstPop.vue";
import { printImprovementCompleteReport } from "@/utils/print/riskImprovementCompleteReportPrint";
import { buildFileServingUrl } from "@/utils/fileUrl";

// ── props / emits ──────────────────────────────────────────────
// 식별자(siteCd, acctId)는 신뢰 원천. 헤더 표시값은 Acct_01.current 에서 그대로 받음.
// 상세 본문(근태/TBM/위험성평가)은 BE 가 acctId 로 victim/occurYmd 를 서버 도출하여 라이브 조회한다.
const props = defineProps({
  siteCd: { type: String, required: true },
  acctId: { type: String, required: true },
  victimUserNm: { type: String, default: "" },
  victimUserTypeCd: { type: String, default: "" }, // REGULAR / DAILY
  occurYmd: { type: String, default: "" },
  occurTime: { type: String, default: "" },
  occurPlace: { type: String, default: "" },
  siteNm: { type: String, default: "" },
  acctGradeNm: { type: String, default: "" },
});

defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const attdTbmPrintArea = ref(null);

// ── 섹션 정의 / 네비게이션 ──────────────────────────────────────
const sections = [
  { key: "chkpt", label: "① 순회점검 점검결과 확인서" },
  { key: "risk", label: "② 위험성평가 개선실행계획서 / 개선완료보고서" },
  { key: "attdTbm", label: "③ 근태 + TBM 합본" },
];
const sectionIdx = ref(0);
const currentSection = computed(() => sections[sectionIdx.value]);

// 3섹션 데이터는 onMounted 에서 일괄 로드하므로 네비게이션은 인덱스 이동만 담당한다.
const fnPrevSection = () => {
  if (sectionIdx.value > 0) sectionIdx.value -= 1;
};
const fnNextSection = () => {
  if (sectionIdx.value < sections.length - 1) sectionIdx.value += 1;
};

// ── 상태 ───────────────────────────────────────────────────────
const isLoading = ref(false);

// ① 순회점검: ChkLstRstPop 에 넘길 chkptInfo 배열(스냅샷 식별자 + 라이브 보강)
const chkptInfoList = ref([]);
// ② 위험성평가: 스냅샷 assessmentCd + 라이브 보강(processCd/명칭/점수 등)
const riskList = ref([]);
// ③ 근태+TBM 합본: 신규 집계 EP 응답
const attdData = ref({
  schedule: null,
  scheduleNote: "",
  records: [],
  tbmList: [],
});

// ── 표시 헬퍼 ──────────────────────────────────────────────────
const fmtYmd = (ymd) => formatYmdDot(ymd);
const fmtHm = (hhmm) => formatHm(hhmm);
const victimTypeLabel = computed(() =>
  props.victimUserTypeCd === "DAILY" ? "일용" : "정규"
);

// 파일 경로 + 파일관리코드 → 표시 URL 조합.
//   공용 유틸(buildFileServingUrl)로 일원화 — 동일 출처 상대경로 조립이라 터널/도메인 경유에도 동작.
//   (기존 VITE_API_BASE_URL 절대주소 조립은 https 도메인에서 혼합콘텐츠 차단으로 이미지가 깨짐)
const buildFileUrl = (filePath, fileMgmtCd) =>
  buildFileServingUrl(filePath, fileMgmtCd) || null;

// 안전 JSON 파싱(스냅샷 필드). 실패 시 빈 객체.
const safeParse = (json) => {
  try {
    return json ? JSON.parse(json) : {};
  } catch (e) {
    return {};
  }
};

// ── 데이터 로딩 (developer 구현) ────────────────────────────────
onMounted(async () => {
  // 초기 진입 시 3섹션 데이터를 일괄 로드(섹션 네비게이션은 인덱스 이동만).
  isLoading.value = true;
  try {
    await Promise.all([
      fnLoadChkptSection(),
      fnLoadRiskSection(),
      fnLoadAttdTbmSection(),
    ]);
  } finally {
    isLoading.value = false;
  }
});

// ① 순회점검: 스냅샷(CHKPT) 식별자 + 점검대상 옵션(명칭/타입/관리자) 보강 → chkptInfoList 채움
const fnLoadChkptSection = async () => {
  chkptInfoList.value = [];
  try {
    // 1) 확정 스냅샷(CHKPT) 조회 — 식별자(chkptCd) + 스냅샷 본문(chkptNm/chklstType)
    const snapRes = await axios.get("/webApi/acct01/link/snapshot", {
      params: {
        siteCd: props.siteCd,
        acctId: props.acctId,
        linkDomainCd: "CHKPT",
      },
    });
    const snapshots = snapRes.data?.snapshotList || [];
    if (snapshots.length === 0) return;

    // 2) 점검대상 옵션으로 chkLstTypeNm/관리자명(siteAdminNm) 보강(실패해도 섹션은 표시).
    const optByChkpt = {};
    try {
      const optRes = await axios.get("/webApi/acct01/patrol/chkpt-options", {
        params: { siteCd: props.siteCd },
      });
      (optRes.data?.chkptOptionList || []).forEach((o) => {
        optByChkpt[o.chkptCd] = o;
      });
    } catch (e) {
      // 보강 실패는 비치명적(명칭만 일부 비게 됨)
    }

    // 3) workMonth = 사고일(occurYmd)이 속한 월(YYYYMM)
    const workMonth = (props.occurYmd || "").slice(0, 6);

    chkptInfoList.value = snapshots.map((s) => {
      const snap = safeParse(s.snapshotJson);
      const key = safeParse(s.linkKeyJson);
      const chkptCd = snap.chkptCd || key.chkptCd || "";
      const opt = optByChkpt[chkptCd] || {};
      return {
        siteCd: props.siteCd,
        siteNm: props.siteNm,
        workMonth,
        chkptCd,
        chkptNm: snap.chkptNm || opt.chkptNm || "",
        // ChkLstRstPop 계약: chkLstType(대문자 L). 스냅샷/옵션은 chklstType(소문자 l).
        chkLstType: snap.chklstType || opt.chklstType || "",
        chkLstTypeNm: opt.chklstTypeNm || "",
        siteAdminNm: opt.mgmtUserNm || "",
        chkptDesc: "",
      };
    });
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "순회점검 출력 데이터 조회 중 오류가 발생했습니다.")
    );
  }
};

// ② 위험성평가: 스냅샷(RISK) assessmentCd → 보강 EP 로 buildRiskAssessmentData 풀상세 라이브 조회
const fnLoadRiskSection = async () => {
  riskList.value = [];
  try {
    const snapRes = await axios.get("/webApi/acct01/link/snapshot", {
      params: {
        siteCd: props.siteCd,
        acctId: props.acctId,
        linkDomainCd: "RISK",
      },
    });
    const snapshots = snapRes.data?.snapshotList || [];
    if (snapshots.length === 0) return;

    const list = [];
    for (const s of snapshots) {
      const snap = safeParse(s.snapshotJson);
      const key = safeParse(s.linkKeyJson);
      const assessmentCd = key.assessmentCd || snap.assessmentCd || "";
      if (!assessmentCd) continue;
      try {
        // 식별자는 스냅샷, 본문은 사고일/사업장 스코프 라이브 재조회(하이브리드).
        const detailRes = await axios.get(
          "/webApi/acct01/print/risk-assessment",
          {
            params: {
              siteCd: props.siteCd,
              acctId: props.acctId,
              assessmentCd,
            },
          }
        );
        list.push(detailRes.data || snap);
      } catch (e) {
        // 단건 보강 실패 시 스냅샷 본문으로 폴백(목록 표시 유지, 상세 버튼은 제한적).
        list.push(snap);
      }
    }
    riskList.value = list;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "위험성평가 출력 데이터 조회 중 오류가 발생했습니다.")
    );
  }
};

// ③ 근태+TBM 합본: 신규 집계 EP 1회 호출
const fnLoadAttdTbmSection = async () => {
  try {
    const res = await axios.get("/webApi/acct01/print/attd-tbm", {
      params: {
        siteCd: props.siteCd,
        acctId: props.acctId,
      },
    });
    const d = res.data || {};
    attdData.value = {
      schedule: d.schedule || null,
      scheduleNote: d.scheduleNote || "",
      records: d.records || [],
      tbmList: d.tbmList || [],
    };
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "근태·TBM 출력 데이터 조회 중 오류가 발생했습니다.")
    );
  }
};

// ── 순회점검 인쇄: 기존 ChkLstRstPop 재사용(자체 prev/next + 패턴 A 인쇄) ──
const fnOpenChkLstResult = () => {
  openPop(ChkLstRstPop, {
    cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
    chkptInfo: chkptInfoList.value,
  });
};

// ── 위험성평가 개선완료보고서: Risk_03(RiskAssessInfo)과 동일한 추출 공용 유틸 재사용 ──
//    (기존에는 RiskImprovementCompleteReport 모달을 띄워 Risk_03 인쇄물과 양식이 달랐음 → 단일 출처 통일)
const fnOpenRiskCompleteReport = (risk) => {
  const r = risk || {};
  const opened = printImprovementCompleteReport({
    processNm: r.processNm,
    riskTypeNm: r.riskTypeNm,
    initAssessDate: r.initAssessDate,
    initAssessorNm: r.initAssessorNm,
    hazardNm: r.hazardNm,
    initDesc: r.initDesc,
    initLikelihoodScore: r.initLikelihoodScore,
    initSeverityScore: r.initSeverityScore,
    initRiskLv: r.initRiskLv,
    assessmentStatusName: r.assessmentStatusNm,
    revalDate: r.revalDate,
    revalBeforeDesc: r.revalBeforeDesc,
    revalAssessorNm: r.revalAssessorNm,
    revalAssessDate: r.revalAssessDate,
    revalDesc: r.revalDesc,
    revalLikelihoodScore: r.revalLikelihoodScore,
    revalSeverityScore: r.revalSeverityScore,
    revalRiskLv: r.revalRiskLv,
    beforePhotoUrl: buildFileUrl(r.initFilePath, r.initFileMgmtCd),
    afterPhotoUrl: buildFileUrl(r.revalFilePath, r.revalFileMgmtCd),
  });
  if (!opened) {
    proxy.$alert("팝업이 차단되어 인쇄 창을 열 수 없습니다. 팝업 차단을 해제해주세요.");
  }
};

// ── 위험성평가 개선실행계획서: 추출 공용 유틸(riskImprovementPlanPrint) 재사용 ──
const fnOpenRiskImprovementPlan = (risk) => {
  const r = risk || {};
  // 진행상태명(assessmentStatusNm)/개선 전 사진 URL 만 화면에서 산출해 전달. 나머지는 빌더가 처리.
  const opened = printImprovementPlan({
    processNm: r.processNm,
    riskTypeNm: r.riskTypeNm,
    initAssessDate: r.initAssessDate,
    initAssessorNm: r.initAssessorNm,
    hazardNm: r.hazardNm,
    initDesc: r.initDesc,
    initLikelihoodScore: r.initLikelihoodScore,
    initSeverityScore: r.initSeverityScore,
    initRiskLv: r.initRiskLv,
    assessmentStatus: r.assessmentStatus,
    assessmentStatusName: r.assessmentStatusNm,
    revalDate: r.revalDate,
    revalBeforeDesc: r.revalBeforeDesc,
    beforePhotoUrl: buildFileUrl(r.initFilePath, r.initFileMgmtCd),
  });
  if (!opened) {
    proxy.$alert("팝업이 차단되어 인쇄 창을 열 수 없습니다. 팝업 차단을 해제해주세요.");
  }
};

// ── 근태+TBM 합본 인쇄 (패턴 A: window.open + document.write) ──
const fnPrintAttdTbm = () => {
  if (!attdTbmPrintArea.value) return;
  const w = window.open("", "_blank", "width=1000,height=800");
  if (!w) {
    proxy.$alert("팝업이 차단되어 인쇄 창을 열 수 없습니다. 팝업 차단을 해제해주세요.");
    return;
  }
  // scoped CSS 는 인쇄창에 전파되지 않으므로 표/헤더 인쇄 스타일을 인라인으로 주입한다.
  const printContent = attdTbmPrintArea.value.innerHTML;
  w.document.write(`
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>근태·TBM 안전관리 현황</title>
        <style>
          @media print { @page { size: A4 portrait; margin: 8mm; } }
          body { font-family: "Pretendard", sans-serif; font-size: 12px; color: #333; padding: 16px; }
          .acc-report__title { text-align: center; font-size: 18px; font-weight: 800; border-bottom: 2px solid #333; padding-bottom: 8px; margin: 0 0 16px; }
          .acc-report__sub { font-size: 13px; font-weight: 700; margin: 16px 0 6px; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 4px; }
          th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; }
          thead th, .acc-report__meta th { background: #f5f5f5; font-weight: 600; white-space: nowrap; }
          .acc-report__meta th { width: 90px; }
          .acc-report__empty, .acc-report__empty-cell { color: #888; }
          .acc-report__empty-cell { text-align: center; }
          .acc-report__foot { margin-top: 12px; font-size: 11px; color: #888; line-height: 1.6; }
        </style>
      </head>
      <body>${printContent}</body>
    </html>
  `);
  w.document.close();
  w.print();
};
</script>

<style scoped>
/* 전역 모달 클래스(modal.css)는 그대로 사용하고, 본 팝업 전용 레이아웃만 정의한다. */
.acc-print-modal {
  width: 960px;
  max-width: 95vw;
  max-height: 90vh;
  height: 90vh;
  position: fixed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 섹션 네비게이션 바 */
.acc-print-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md, 1rem);
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.acc-print-nav__btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: var(--btn-radius);
  cursor: pointer;
  font-size: 1rem;
  color: var(--color-text);
}
.acc-print-nav__btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.acc-print-nav__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.acc-print-nav__center {
  text-align: center;
  min-width: 320px;
}
.acc-print-nav__idx {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}
.acc-print-nav__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

/* 본문 */
.acc-print-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1.25rem;
}
.acc-print-state {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
.acc-print-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
.acc-print-section__head h3 {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

/* 섹션 내 연계 목록 */
.acc-print-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.acc-print-list > li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: var(--color-surface);
}
.acc-print-list--risk > li {
  justify-content: space-between;
}
.acc-print-list__main {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}
.acc-print-list__lead {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--color-text-strong);
}
.acc-print-list__sub {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.acc-print-list__actions {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
}
.acc-print-note {
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.72rem;
  color: var(--color-text-muted);
  line-height: 1.6;
}

/* ③ 근태+TBM 합본 인쇄 리포트 */
.acc-report {
  background: var(--color-surface);
}
.acc-report__title {
  text-align: center;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-text-strong);
  border-bottom: 2px solid var(--color-border-strong);
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}
.acc-report__sub {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text-strong);
  margin: 1.1rem 0 0.5rem;
}
.acc-report__meta,
.acc-report__tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.82rem;
}
.acc-report__meta th,
.acc-report__meta td,
.acc-report__tbl th,
.acc-report__tbl td {
  border: 1px solid var(--color-border);
  padding: 0.45rem 0.6rem;
  text-align: left;
}
.acc-report__meta th,
.acc-report__tbl thead th {
  background: var(--color-bg);
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
}
.acc-report__meta th {
  width: 90px;
}
.acc-report__empty,
.acc-report__empty-cell {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}
.acc-report__empty {
  padding: 0.5rem 0;
}
.acc-report__empty-cell {
  text-align: center;
  padding: 0.6rem 0;
}
.acc-report__foot {
  margin-top: 1rem;
  font-size: 0.72rem;
  color: var(--color-text-muted);
  line-height: 1.6;
}

/* 버튼: 전역 .btn / .btn-primary 보강(아웃라인 변형만 로컬) */
.btn-outline {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.btn-outline:hover {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.06));
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
