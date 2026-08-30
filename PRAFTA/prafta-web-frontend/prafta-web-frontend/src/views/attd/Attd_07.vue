<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="incSubNodeYn" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
      <div>
        <label>고용형태</label>
        <select v-model="employmentType">
          <option value="">전체</option>
          <option value="REGULAR">정규직</option>
          <option value="DAILY">일일사용자</option>
        </select>
      </div>
    </div>

    <!-- 캘린더 툴바 (월 네비 / 뷰 토글 / 페이지 액션) -->
    <div class="a07-toolbar">
      <div class="a07-nav">
        <button class="a07-nav-arr" @click="fnPrevMonth">‹</button>
        <CalendarSrchMonth v-model="workYm" class="a07-nav-month-picker" />
        <button class="a07-nav-arr" @click="fnNextMonth">›</button>
        <div class="a07-view-toggle">
          <button
            type="button"
            :class="['a07-view-btn', { active: viewMode === 'calendar' }]"
            @click="viewMode = 'calendar'"
          >
            캘린더 뷰
          </button>
          <button
            type="button"
            :class="['a07-view-btn', { active: viewMode === 'list' }]"
            @click="viewMode = 'list'"
          >
            목록 뷰
          </button>
        </div>
      </div>
      <div class="a07-actions">
        <span v-if="isMonthClosed" class="a07-issue-count none">마감됨</span>
        <span
          v-else
          class="a07-issue-count"
          :class="{ none: blockCountDisplay === 0 }"
        >
          처리 필요 <b>{{ blockCountDisplay }}</b
          >건
        </span>
        <button
          class="a07-btn-line"
          :class="{ disabled: !monthCloseBtnEnabled }"
          @click="fnOpenMonthClosePop"
        >
          {{ monthCloseLabel }}
        </button>
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- 캘린더 뷰 -->
    <!-- ============================================================ -->
    <div v-show="viewMode === 'calendar'" class="viewBody a07-cal-body">
      <div class="a07-cal-wrap">
        <div class="a07-cal-scroll">
          <table class="a07-matrix">
            <thead>
              <tr>
                <th class="m-user-head sticky-top sticky-left">사용자 정보</th>
                <th
                  v-for="d in daysInMonth"
                  :key="d.day"
                  class="m-day-head sticky-top"
                  :class="{ 'head-sun': d.dow === 0, 'head-sat': d.dow === 6 }"
                >
                  {{ d.day }}<br /><span class="m-dow"
                    >({{ dowLabels[d.dow] }})</span
                  >
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in userList" :key="u.userId">
                <td class="m-user-cell sticky-left">
                  <div class="m-user-name">
                    {{ u.name }}
                    <span v-if="u.isDaily" class="badge-daily">일일사용자</span>
                    <span
                      v-if="u.currentSiteCd && u.currentSiteCd !== siteCd"
                      class="badge-other-site"
                    >
                      타 사업장 소속 · {{ u.currentSiteNm }}
                    </span>
                  </div>
                  <div class="m-user-meta">{{ u.userId }} · {{ u.dept }}</div>
                </td>
                <td
                  v-for="d in daysInMonth"
                  :key="d.day"
                  class="m-day-cell"
                  :class="getCellClass(u, d)"
                  @dblclick="fnOpenDayDetailPop(u, d)"
                >
                  <template v-if="getCell(u, d).type === 'off'">
                    <span class="m-off">−</span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'normal'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'normal2'">
                    <div class="m-time-2">
                      <div>{{ getCell(u, d).a1 }}</div>
                      <div>{{ getCell(u, d).a2 }}</div>
                      <div class="m-div"></div>
                      <div>{{ getCell(u, d).b1 }}</div>
                      <div>{{ getCell(u, d).b2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'issue-dash'">
                    <span class="m-off">−</span>
                    <span class="m-corner"></span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'issue-marker'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                    <span class="m-corner"></span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'pre-dash'">
                    <span class="m-off">−</span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'pre-time'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'code'">
                    <div class="m-code-label">{{ getCell(u, d).label }}</div>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="a07-cal-legend">
          <span class="lg-item"
            ><span class="sw sw-issue"></span>처리 필요 — 라벨형</span
          >
          <span class="lg-item"
            ><span class="sw sw-marker"></span>처리 필요 — 코너 마커</span
          >
          <span class="lg-item"><span class="sw sw-code"></span>근태 코드</span>
          <span class="lg-sat">토</span>
          <span class="lg-sun">일·공휴일</span>
          <span class="lg-summary">
            정상 <b>{{ summary.ok }}</b> · 처리필요
            <b class="warn">{{ summary.issue }}</b> · 마감률
            <b>{{ summary.rate }}%</b>
          </span>
        </div>
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- 목록 뷰 -->
    <!-- ============================================================ -->
    <div v-show="viewMode === 'list'" class="viewBody a07-list-body">
      <div class="a07-list-wrap">
        <!-- 좌측: 사용자 리스트 -->
        <div class="a07-user-pane">
          <div class="a07-user-head">
            <span
              >사용자 <b>{{ userList.length }}</b
              >명</span
            >
            <span class="a07-user-sort">정렬 ↓</span>
          </div>
          <div class="a07-user-list">
            <div
              v-for="u in userList"
              :key="u.userId"
              :class="[
                'a07-user-item',
                { active: selectedUserId === u.userId },
              ]"
              @click="selectedUserId = u.userId"
            >
              <div class="ui-top">
                <span class="ui-name">{{ u.name }}</span>
                <span v-if="u.isDaily" class="badge-daily">일일사용자</span>
                <span
                  v-if="u.currentSiteCd && u.currentSiteCd !== siteCd"
                  class="badge-other-site"
                >
                  타 사업장 소속 · {{ u.currentSiteNm }}
                </span>
                <span v-if="u.issues > 0" class="ui-badge-issue">{{
                  u.issues
                }}</span>
                <span v-else class="ui-badge-ok">정상</span>
              </div>
              <div class="ui-meta">{{ u.userId }}</div>
              <div class="ui-meta">{{ u.dept }}</div>
            </div>
          </div>
        </div>

        <!-- 우측: 사용자 상세 -->
        <div class="a07-detail">
          <div class="a07-detail-head" v-if="selectedUser">
            <div class="a07-uinfo">
              <div class="a07-avatar">{{ selectedUser.name.charAt(0) }}</div>
              <div>
                <div class="a07-uname">
                  {{ selectedUser.name }}
                  <span v-if="selectedUser.isDaily" class="badge-daily"
                    >일일사용자</span
                  >
                  <span
                    v-if="
                      selectedUser.currentSiteCd &&
                      selectedUser.currentSiteCd !== siteCd
                    "
                    class="badge-other-site"
                  >
                    타 사업장 소속 · {{ selectedUser.currentSiteNm }}
                  </span>
                </div>
                <div class="a07-umeta">
                  {{ selectedUser.userId }} · {{ selectedUser.dept }} ·
                  {{ selectedUser.role }}
                </div>
              </div>
            </div>
            <div class="a07-metrics">
              <div class="a07-m">
                <span class="a07-m-lbl">총 근무 기록</span>
                <span class="a07-m-val">
                  {{ detailSummary.recDays }}<small>일</small> /
                  {{ detailSummary.totalH }}<small>시간</small>
                  {{ detailSummary.totalM }}<small>분</small>
                </span>
              </div>
            </div>
          </div>

          <div class="a07-detail-table-wrap">
            <table class="a07-detail-table">
              <colgroup>
                <col class="c-note" />
                <col class="c-date" />
                <!-- 1구간 -->
                <col class="c-plan" />
                <col class="c-plan" />
                <col class="c-day" />
                <col class="c-act-time" />
                <col class="c-day" />
                <col class="c-act-time" />
                <!-- 2구간 -->
                <col class="c-plan" />
                <col class="c-plan" />
                <col class="c-day" />
                <col class="c-act-time" />
                <col class="c-day" />
                <col class="c-act-time" />
                <!-- PRAFTA-FIXEDOT-3: 고정연장 구간 / 실적(자동 계상) — 실근로시간 왼쪽 배치(2026-08-17 요청) -->
                <col class="c-fixedot" />
                <col class="c-fixedot" />
                <col class="c-total" />
                <col class="c-total" />
              </colgroup>
              <thead>
                <!-- Level 1: 비고 / 날짜 / 스케줄·근태(1·2구간) / 실근로시간 / 인정시간 -->
                <tr class="lvl1">
                  <th class="l1-rs col-note" rowspan="2">비고</th>
                  <th class="date-h bdr-section" rowspan="2">날짜</th>
                  <th class="l2-plan bdr-section" colspan="2">1구간 스케줄</th>
                  <th class="l2-actual bdr-sub" colspan="4">1구간 근태</th>
                  <th class="l2-plan bdr-section" colspan="2">2구간 스케줄</th>
                  <th class="l2-actual bdr-sub" colspan="4">2구간 근태</th>
                  <!-- PRAFTA-FIXEDOT-3: 고정연장근무(소정과 분리된 별도 축).
                       구간 = 근무타입에 설정된 전방/후방 고정연장, 실적 = 실근태가 그 구간을 커버한 분.
                       "연장 미이행" 배지는 조퇴 판정/통계와 완전히 분리된 별도 표식이다(정책 ②).
                       위치: 실근로시간 왼쪽(2026-08-17 요청). -->
                  <th class="l2-plan bdr-section" colspan="2">고정연장</th>
                  <th class="l1-rs bdr-section" rowspan="2">실근로시간</th>
                  <th class="l1-rs bdr-section" rowspan="2">인정시간</th>
                </tr>
                <!-- Level 2: 컬럼명 -->
                <tr class="lvl2">
                  <th class="l3-plan bdr-section">시작</th>
                  <th class="l3-plan">종료</th>
                  <th class="l3-actual bdr-sub">출근일자</th>
                  <th class="l3-actual">출근</th>
                  <th class="l3-actual">퇴근일자</th>
                  <th class="l3-actual">퇴근</th>
                  <th class="l3-plan bdr-section">시작</th>
                  <th class="l3-plan">종료</th>
                  <th class="l3-actual bdr-sub">출근일자</th>
                  <th class="l3-actual">출근</th>
                  <th class="l3-actual">퇴근일자</th>
                  <th class="l3-actual">퇴근</th>
                  <th class="l3-plan bdr-section">구간</th>
                  <th class="l3-plan">실적</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="r in detailRows" :key="r.rowKey">
                  <!-- 초과근무 행 (kind === 'ot') -->
                  <tr v-if="r.kind === 'ot'" class="ot-row">
                    <!-- 비고 -->
                    <td class="col-note">
                      <span class="badge-ot">초과근무</span>
                    </td>
                    <td class="date bdr-section"></td>
                    <!-- PRAFTA-COM-013-06-4(r34-3): OT 실적은 매칭 구간(r.seg)에 따라 실적1/실적2 칸에 표시 -->
                    <!-- 1구간 계획(OT 행은 항상 공란) -->
                    <td class="col-plan bdr-section">−</td>
                    <td class="col-plan">−</td>
                    <!-- 1구간 실적: seg === 1 일 때만 OT 시각 표시 -->
                    <td class="col-actual bdr-sub">
                      {{ r.seg === 1 ? valOrDash(r.otInDate) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 1 ? valOrDash(r.otIn) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 1 ? valOrDash(r.otOutDate) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 1 ? valOrDash(r.otOut) : "−" }}
                    </td>
                    <!-- 2구간 계획(OT 행은 항상 공란) -->
                    <td class="col-plan bdr-section">−</td>
                    <td class="col-plan">−</td>
                    <!-- 2구간 실적: seg === 2 일 때만 OT 시각 표시 -->
                    <td class="col-actual bdr-sub">
                      {{ r.seg === 2 ? valOrDash(r.otInDate) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 2 ? valOrDash(r.otIn) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 2 ? valOrDash(r.otOutDate) : "−" }}
                    </td>
                    <td class="col-actual">
                      {{ r.seg === 2 ? valOrDash(r.otOut) : "−" }}
                    </td>
                    <!-- PRAFTA-FIXEDOT-3: OT 행은 고정연장 개념이 없다(구간이 서로 배타) -->
                    <td class="col-plan bdr-section">−</td>
                    <td class="col-plan">−</td>
                    <td class="bdr-section right">{{ valOrDash(r.total) }}</td>
                    <td class="bdr-section right">
                      {{ valOrDash(r.recognized) }}
                    </td>
                  </tr>
                  <!-- 정규근무 행 (kind === 'work') -->
                  <!-- 행 더블클릭 → 일자 상세 팝업. 캘린더 뷰(m-day-cell @dblclick)와 동일 제스처·동일 조건
                       (종전엔 단일 클릭 + status==='alert' 행만 열려, 연차/연차변경 요청이 걸린 날은
                        같은 팝업을 목록 뷰에서 열 수 없었다). -->
                  <tr
                    v-else
                    :class="r.status"
                    @dblclick="fnOpenAttdAdjustPop(r)"
                  >
                    <!-- 비고 -->
                    <td class="col-note">
                      <template v-if="r.outsideList && r.outsideList.length">
                        <span
                          v-for="o in r.outsideList"
                          :key="o.gpsKey"
                          class="badge-outside"
                          @click.stop="fnOpenGpsPop(o)"
                        >
                          외근{{ o.label }}
                        </span>
                      </template>
                      <span
                        v-else-if="r.note"
                        :class="
                          r.status === 'alert' ? 'badge-warn' : 'badge-info'
                        "
                        >{{ r.note }}</span
                      >
                      <span v-else class="dash">−</span>
                    </td>
                    <td class="date bdr-section">
                      <span
                        v-if="r.status === 'alert'"
                        class="alert-dot"
                      ></span>
                      {{ r.day }}
                      <span :class="dowClass(r.dow)"
                        >({{ dowLabels[r.dow] }})</span
                      >
                    </td>
                    <!-- 1구간 계획 -->
                    <td class="col-plan bdr-section">
                      {{ valOrDash(r.p1Start) }}
                    </td>
                    <td class="col-plan">{{ valOrDash(r.p1End) }}</td>
                    <!-- 1구간 실적 -->
                    <td class="col-actual bdr-sub">
                      {{ valOrDash(r.a1InDate) }}
                    </td>
                    <td class="col-actual">{{ valOrDash(r.a1In) }}</td>
                    <td class="col-actual">{{ valOrDash(r.a1OutDate) }}</td>
                    <td class="col-actual">{{ valOrDash(r.a1Out) }}</td>
                    <!-- 2구간 계획 -->
                    <td class="col-plan bdr-section">
                      {{ valOrDash(r.p2Start) }}
                    </td>
                    <td class="col-plan">{{ valOrDash(r.p2End) }}</td>
                    <!-- 2구간 실적 -->
                    <td class="col-actual bdr-sub">
                      {{ valOrDash(r.a2InDate) }}
                    </td>
                    <td class="col-actual">{{ valOrDash(r.a2In) }}</td>
                    <td class="col-actual">{{ valOrDash(r.a2OutDate) }}</td>
                    <td class="col-actual">{{ valOrDash(r.a2Out) }}</td>
                    <!-- PRAFTA-FIXEDOT-3: 고정연장 구간 / 실적 + "연장 미이행" 배지.
                         배지는 서버 파생 판정(조퇴와 분리·연차 계열 사용일 미발화)을 그대로 표시만 한다. -->
                    <td class="col-plan bdr-section">
                      {{ valOrDash(r.fixedOtRange) }}
                    </td>
                    <td class="col-plan">
                      <span v-if="r.fixedOtUnmet" class="badge-fixedot-unmet"
                        >미이행</span
                      >
                      {{ valOrDash(r.fixedOtAct) }}
                    </td>
                    <!-- 실근로시간 -->
                    <td class="bdr-section right">
                      {{ valOrDash(r.total) }}
                    </td>
                    <!-- 인정시간 -->
                    <td class="bdr-section right">
                      {{ valOrDash(r.recognized) }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import AttdDayDetailPop from "@/views/attd/popup/AttdDayDetailPop.vue";
import AttdGpsTrailPop from "@/views/attd/popup/AttdGpsTrailPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "Attd_07" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// ── 조회 조건 ─────────────────────────────────────────────
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const searchUserNm = ref("");
// 고용형태 필터: "" 전체 / "REGULAR" 정규직 / "DAILY" 일일사용자
const employmentType = ref("");
const siteNoFcs = ref(null);

// ── 화면 상태 ─────────────────────────────────────────────
const viewMode = ref("calendar"); // 'calendar' | 'list'
const workYm = ref(currentYm()); // YYYY-MM

// PRAFTA-019-C 근태 마감 상태 (백엔드 authoritative). null = 미조회
const closeInfo = ref(null);
// PRAFTA-028 - 조회 완료 여부 ('조회 먼저' 가드). 스코프/월 변경 시 초기화.
const hasSearched = ref(false);
// PRAFTA-028 - master/hr 여부 (그 외 권한은 사업장+소속부서 필수)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];

function currentYm() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

const fnPrevMonth = () => shiftMonth(-1);
const fnNextMonth = () => shiftMonth(1);
const shiftMonth = (delta) => {
  const [y, m] = workYm.value.split("-").map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  workYm.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
};

// ── 사업장 / 부서 포커스 처리 (Attd_06_2 패턴 차용) ────────
//   값이 비어있으면 관련 필드 클리어
//   값이 있으면 짝 필드 비우고 즉시 자동조회 (단건 매칭이면 자동 세팅, 다건이면 팝업)
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
    }
  }
};

// 사업장 자동조회 (코드/명 입력 후 blur)
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 부서 자동조회 (코드/명 입력 후 blur)
const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      fnCallback({ ...response, config: { url: "/dummy/site-node-lists" } });
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 자동조회 응답 처리 — 0건/1건/다건 분기
const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
      nodeDisabled.value = false;
      // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 팝업 선택 경로와 정합)
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } else if (apiId === "site-node-lists") {
    const list = res.data?.siteNodeInfoList || [];
    if (list.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? "";
      nodeNm.value = list[0].nodeNm ?? "";
    } else {
      fnSiteNodeSearchPopOpen();
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// ── 사용자/근태 데이터 (조회 결과로 채움) ─────────────────
const userList = ref([]);
// key: `${userId}_${day}` → 응답 레코드 1건
const recordMap = ref({});
// 처리 필요 목록 (response.data.monthlyAttdReqSummaryResultList)
//   { reqId, workYmd, userCd } — "처리 필요 n 건" 카운트 + 캘린더 셀 라벨형 표시
const reqIdList = ref([]);
// 연차 변경(이동/삭제) 활성 요청 요약 (response.data.monthlyLeaveChangeSummaryResultList)
//   { reqId(=CHANGE_REQ_ID), workYmd, userCd } — 캘린더 셀 강조 전용.
//   이동요청은 출발일·이동대상일 두 행이 내려와 양쪽 셀이 모두 강조된다(근태 요청 UX 정합).
//   "처리 필요 n건" 카운트에는 섞지 않는다 — 그 값은 백엔드 blockTotalCnt(연차 변경 포함)가 권위.
const leaveChangeSummaryList = ref([]);
// userCd_workYmd 키 Set — 캘린더 셀별 매칭 O(1)
const reqCellSet = computed(() => {
  const s = new Set();
  for (const r of reqIdList.value) {
    if (r?.userCd && r?.workYmd) s.add(`${r.userCd}_${r.workYmd}`);
  }
  for (const r of leaveChangeSummaryList.value) {
    if (r?.userCd && r?.workYmd) s.add(`${r.userCd}_${r.workYmd}`);
  }
  return s;
});

// 월간 초과근무 목록 (response.data.monthlyOvertimeResultList)
//   PRAFTA-017 신규 응답. 목록 뷰에서 일자별 정규근무 행 아래 OT 행으로 펼침.
const monthlyOvertimeList = ref([]);

// A안(2026-08-17): 확정 "시각 보유" 연차(반차/시간차) 구간 맵 — `${userCd}_${workYmd}` → [[s,e],...]
//   (근무일 00:00 anchor 분, END<=START 는 익일 wrap). 연차 사용 시간은 근로시간이 아니므로
//   실근로시간/인정시간 표시에서 실근태와의 겹침만큼 차감한다(구서버 응답 미수신이면 빈 맵 = 종전 값).
const timeLeaveWindowsMap = ref({});

const selectedUserId = ref("");
const selectedUser = computed(
  () => userList.value.find((u) => u.userId === selectedUserId.value) ?? null
);

// ── 월의 일자 목록 ────────────────────────────────────────
const daysInMonth = computed(() => {
  const [y, m] = workYm.value.split("-").map(Number);
  const last = new Date(y, m, 0).getDate();
  const arr = [];
  for (let d = 1; d <= last; d++) {
    arr.push({ day: d, dow: new Date(y, m - 1, d).getDay() });
  }
  return arr;
});

// ── 응답 변환 헬퍼 ────────────────────────────────────────
// "0930" → "09:30"
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};
// "20260502" → "05.02" (일자 컬럼 표시용)
const fmtMmdd = (ymd) => {
  if (!ymd) return "";
  const v = String(ymd);
  if (v.length < 8) return v;
  return `${v.slice(4, 6)}.${v.slice(6, 8)}`;
};
// 응답 record에서 day(1~31) 추출 — workYmd(YYYYMMDD) 우선, fallback cmpnyCd
const pickDay = (r) => {
  const ymd = String(r.workYmd ?? "");
  if (/^\d{8}$/.test(ymd)) return Number(ymd.slice(6, 8));
  const alt = String(r.cmpnyCd ?? "");
  if (/^\d{8}$/.test(alt)) return Number(alt.slice(6, 8));
  return 0;
};
// 오늘 YYYYMMDD
const todayYmd = () => {
  const n = new Date();
  return `${n.getFullYear()}${String(n.getMonth() + 1).padStart(2, "0")}${String(n.getDate()).padStart(2, "0")}`;
};
// 현재 HHMM
const nowHhmm = () => {
  const n = new Date();
  return `${String(n.getHours()).padStart(2, "0")}${String(n.getMinutes()).padStart(2, "0")}`;
};
// 시간값을 4자리 HHMM로 정규화
const normHhmm = (v) =>
  String(v ?? "")
    .replace(/\D/g, "")
    .slice(0, 4);

// A안(2026-08-17): 'HHMM' → 분(0~1439). 형식 위반이면 null.
const hhmmToMin = (hhmm) => {
  const v = String(hhmm ?? "");
  if (!/^\d{4}$/.test(v)) return null;
  const h = parseInt(v.slice(0, 2), 10);
  const m = parseInt(v.slice(2, 4), 10);
  if (h > 24 || m > 59) return null;
  return h * 60 + m;
};

// A안: 확정 시각 연차 구간과 그날 실근태(1·2구간)의 겹침(분).
//   실근태 스탬프를 근무일 00:00 anchor 분으로 환산해(전일 음수/익일 1440+) 구간 교집합을 합산한다.
//   시간차는 스케줄 안에서만 신청되므로(등록 가드) 이 값은 인정시간(실제∩스케줄) 차감에도 그대로 쓴다.
const timeLeaveOverlapMin = (r, workYmd) => {
  const wins = timeLeaveWindowsMap.value[`${r.userCd}_${workYmd}`];
  if (!wins || !wins.length) return 0;
  let total = 0;
  for (const seg of [1, 2]) {
    const inT = normHhmm(seg === 2 ? r.act2InTime : r.act1InTime);
    const outT = normHhmm(seg === 2 ? r.act2OutTime : r.act1OutTime);
    if (inT.length !== 4 || outT.length !== 4) continue;
    const inD = String((seg === 2 ? r.act2InDate : r.act1InDate) || workYmd);
    const outD = String((seg === 2 ? r.act2OutDate : r.act1OutDate) || workYmd);
    const im = hhmmToMin(inT);
    const om = hhmmToMin(outT);
    if (im == null || om == null) continue;
    const s = ymdDayDiff(workYmd, inD) * 1440 + im;
    const e = ymdDayDiff(workYmd, outD) * 1440 + om;
    if (e <= s) continue;
    for (const w of wins) {
      const os = Math.max(s, w[0]);
      const oe = Math.min(e, w[1]);
      if (oe > os) total += oe - os;
    }
  }
  return total;
};

// 출퇴근 상태 판정 (오늘/미래는 누락이 아니라 "출근전"/"퇴근전" 으로 분류)
// 반환: { status: 'alert' | 'pre' | '', note }
//  - 미래 날짜: 미입력은 "출근전" / "퇴근전"
//  - 오늘 날짜: 스케줄 시간 전이면 "전", 시간이 지났는데도 없으면 "누락"
//  - 과거 날짜: 기존 누락 규칙 그대로
const detectAttdState = (r, workYmd) => {
  if (!r) return { status: "", note: "" };
  if (r.leaveNm) return { status: "pre", note: r.leaveNm };
  if (!r.plan1Start && !r.plan2Start) return { status: "", note: "" };

  const ymd = String(workYmd ?? "");
  const today = todayYmd();
  const isFuture = ymd > today;
  const isToday = ymd === today;
  const now = nowHhmm();

  const beforePlan = (planTime) => isToday && now < normHhmm(planTime);

  if (r.plan1Start && !r.act1InTime) {
    if (isFuture || beforePlan(r.plan1Start))
      return { status: "pre", note: "출근전" };
    return { status: "alert", note: "출근누락" };
  }
  if (r.plan1End && r.act1InTime && !r.act1OutTime) {
    if (isFuture || beforePlan(r.plan1End))
      return { status: "pre", note: "퇴근전" };
    return { status: "alert", note: "퇴근누락" };
  }
  if (r.plan2Start && !r.act2InTime) {
    if (isFuture || beforePlan(r.plan2Start))
      return { status: "pre", note: "출근전" };
    return { status: "alert", note: "출근누락" };
  }
  if (r.plan2End && r.act2InTime && !r.act2OutTime) {
    if (isFuture || beforePlan(r.plan2End))
      return { status: "pre", note: "퇴근전" };
    return { status: "alert", note: "퇴근누락" };
  }
  return { status: "", note: "" };
};

// 출퇴근 누락 등 처리필요 여부 (사용자 issue 카운트용)
const isIssueRecord = (r) => detectAttdState(r, r?.workYmd).status === "alert";
// YYYYMMDD 두 일자 간 일수 차(out - in). 잘못된 값이면 0.
//   PRAFTA-COM-013-06-5(r34-4): 오버나이트(자정 넘김) 보정을 위해 출/퇴근 일자 차이를 분 계산에 반영한다.
const ymdDayDiff = (inDate, outDate) => {
  const i = String(inDate || "");
  const o = String(outDate || "");
  if (!/^\d{8}$/.test(i) || !/^\d{8}$/.test(o)) return 0;
  const di = new Date(
    parseInt(i.slice(0, 4), 10),
    parseInt(i.slice(4, 6), 10) - 1,
    parseInt(i.slice(6, 8), 10)
  );
  const dout = new Date(
    parseInt(o.slice(0, 4), 10),
    parseInt(o.slice(4, 6), 10) - 1,
    parseInt(o.slice(6, 8), 10)
  );
  return Math.round((dout.getTime() - di.getTime()) / (1000 * 60 * 60 * 24));
};

// 분 단위 근무시간 계산
//   PRAFTA-COM-013-06-5(r34-4): 출/퇴근 일자(inDate/outDate)를 함께 받아 자정 넘김(오버나이트)을
//   보정한다. 일자 정보가 없으면(레거시 호출) 0일 차로 간주해 기존 동작과 동일.
const calcMin = (inT, outT, breakMin, inDate, outDate) => {
  if (!inT || !outT) return 0;
  const i = String(inT),
    o = String(outT);
  if (i.length < 4 || o.length < 4) return 0;
  const im = parseInt(i.slice(0, 2), 10) * 60 + parseInt(i.slice(2, 4), 10);
  const om = parseInt(o.slice(0, 2), 10) * 60 + parseInt(o.slice(2, 4), 10);
  // 자정 넘김 보정: 퇴근 일자가 출근 일자보다 뒤이면 일수 차 × 1440분을 더한다.
  const dayDiff = ymdDayDiff(inDate, outDate);
  return Math.max(0, om - im + dayDiff * 1440 - (parseInt(breakMin, 10) || 0));
};
// A안(2026-08-17): minusMin = 확정 시각 연차와 실근태의 겹침(분) — 연차 시간은 근로시간 미산입.
const calcTotal = (r, minusMin = 0) => {
  const t =
    calcMin(
      r.act1InTime,
      r.act1OutTime,
      r.plan1BreakMin,
      r.act1InDate,
      r.act1OutDate
    ) +
    calcMin(
      r.act2InTime,
      r.act2OutTime,
      r.plan2BreakMin,
      r.act2InDate,
      r.act2OutDate
    ) -
    (minusMin || 0);
  if (t <= 0) return "";
  return `${Math.floor(t / 60)}시간 ${String(t % 60).padStart(2, "0")}분`;
};

// ── 인정시간(분단위) 산정 — Attd_08 인정시간 로직 포팅 ─────
// 'yyyyMMdd'+'HHmm' → 분 절대값. 일자 미기재 시 baseYmd 보정. (Attd_08 dtMinutes 포팅)
const dtAbsMin = (ymd, hhmm, baseYmd) => {
  if (!hhmm) return null;
  const t = String(hhmm).padStart(4, "0");
  const h = parseInt(t.slice(0, 2), 10);
  const mi = parseInt(t.slice(2, 4), 10);
  if (isNaN(h) || isNaN(mi)) return null;
  let s = String(ymd ?? "");
  if (s.length !== 8) s = String(baseYmd ?? "");
  if (s.length !== 8) return null;
  const base = Date.UTC(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8));
  return Math.round(base / 60000) + h * 60 + mi;
};
// 구간(seg=1|2)의 인정시간(분) = (실제근무 ∩ 스케줄) − 휴게. 산정 불가면 0.
const recognizedSegMin = (r, seg) => {
  const inT = seg === 2 ? r.act2InTime : r.act1InTime;
  const outT = seg === 2 ? r.act2OutTime : r.act1OutTime;
  const inD = seg === 2 ? r.act2InDate : r.act1InDate;
  const outD = seg === 2 ? r.act2OutDate : r.act1OutDate;
  const schStart = seg === 2 ? r.plan2Start : r.plan1Start;
  const schEnd = seg === 2 ? r.plan2End : r.plan1End;
  const brk = parseInt(seg === 2 ? r.plan2BreakMin : r.plan1BreakMin, 10) || 0;
  if (!inT || !outT || !schStart || !schEnd) return 0;
  const inM = dtAbsMin(inD, inT, r.workYmd);
  let outM = dtAbsMin(outD, outT, r.workYmd);
  if (inM == null || outM == null) return 0;
  if (outM < inM) outM += 1440;
  const schStartM = dtAbsMin(r.workYmd, schStart, r.workYmd);
  let schEndM = dtAbsMin(r.workYmd, schEnd, r.workYmd);
  if (schStartM == null || schEndM == null) return 0;
  if (schEndM < schStartM) schEndM += 1440;
  const overlap = Math.max(
    0,
    Math.min(outM, schEndM) - Math.max(inM, schStartM)
  );
  return Math.max(0, overlap - brk);
};
// 정규근무 행 인정시간(분) = 1구간 + 2구간.
const calcRecognized = (r) => recognizedSegMin(r, 1) + recognizedSegMin(r, 2);
// 분 → 표기. Attd_08 fmtDuration 정합("N시간 M분"/"N시간"/"M분", 0/음수 빈문자).
const fmtRecognized = (min) => {
  const m = Math.max(0, Math.round(min || 0));
  if (m <= 0) return "";
  const h = Math.floor(m / 60);
  const mm = m % 60;
  if (h && mm) return `${h}시간 ${mm}분`;
  if (h) return `${h}시간`;
  return `${mm}분`;
};

// 캘린더 셀 데이터
const getCell = (user, dayInfo) => {
  const r = recordMap.value[`${user.userId}_${dayInfo.day}`];
  if (!r) return { type: "off" };
  // 실적이 하나라도 있으면 스케줄 유무와 관계없이 데이터 표시
  const hasAct = !!(
    r.act1InTime ||
    r.act1OutTime ||
    r.act2InTime ||
    r.act2OutTime
  );
  // 계획도 없고 실적도 없으면 휴무
  if (!r.plan1Start && !r.plan2Start && !hasAct) return { type: "off" };
  // 휴가
  if (r.leaveNm) return { type: "code", label: r.leaveNm };

  // 오늘/미래/과거 판정용 YYYYMMDD
  const ymd =
    String(r.workYmd ?? "") ||
    `${workYm.value.replace("-", "")}${String(dayInfo.day).padStart(2, "0")}`;
  // 1구간(plan1) 기준으로만 셀 누락/예정 판정 — 정상이면 아래에서 시간 표시로 떨어짐
  // (2구간 누락 여부는 isIssueRecord/목록 뷰에서 별도 처리)
  const today = todayYmd();
  const isFuture = ymd > today;
  const isToday = ymd === today;
  const now = nowHhmm();
  const beforePlan = (planTime) => isToday && now < normHhmm(planTime);

  // 1구간 출근 자체가 없음
  if (r.plan1Start && !r.act1InTime) {
    if (isFuture || beforePlan(r.plan1Start)) return { type: "pre-dash" };
    return { type: "issue-dash" };
  }
  // 1구간 출근은 있고 퇴근만 없음
  if (r.plan1End && r.act1InTime && !r.act1OutTime) {
    if (isFuture || beforePlan(r.plan1End)) {
      return { type: "pre-time", t1: fmtTime(r.act1InTime), t2: "-" };
    }
    return { type: "issue-marker", t1: fmtTime(r.act1InTime), t2: "-" };
  }

  // 분할 근무 — act2 실적이 있을 때만 4줄(1·2구간) 표시
  if (r.act2InTime || r.act2OutTime) {
    return {
      type: "normal2",
      a1: fmtTime(r.act1InTime),
      a2: fmtTime(r.act1OutTime),
      b1: fmtTime(r.act2InTime),
      b2: fmtTime(r.act2OutTime),
    };
  }
  // 단일 근무 또는 분할근무지만 act2 미입력 — act1만 2줄로 표시
  return {
    type: "normal",
    t1: fmtTime(r.act1InTime),
    t2: fmtTime(r.act1OutTime),
  };
};

// workYm.value(YYYY-MM) + day → YYYYMMDD
const toCellYmd = (day) =>
  `${workYm.value.replace("-", "")}${String(day).padStart(2, "0")}`;

const getCellClass = (user, dayInfo) => {
  const c = getCell(user, dayInfo);
  // 백엔드 처리필요 목록(monthlyAttdReqSummaryResultList)과 매칭되면 라벨형 강조
  const isReq = reqCellSet.value.has(
    `${user.userCd}_${toCellYmd(dayInfo.day)}`
  );
  return {
    off: c.type === "off",
    "issue-marker": c.type === "issue-marker" || c.type === "issue-dash",
    "issue-label": isReq,
    "code-cell": c.type === "code",
    // PRAFTA-028 - 마감된 월이면 셀을 회색 계열로 표시(관리자 인지용)
    closed: isMonthClosed.value,
  };
};

// ── 요약 (샘플) ────────────────────────────────────────────
// "처리 필요 n 건" 카운트 — fnSearch 응답의 reqIdResult 리스트 길이를 그대로 사용
const issueCount = computed(() => reqIdList.value.length);
const canMonthClose = computed(() => issueCount.value === 0);
const summary = computed(() => {
  const total = userList.value.length * daysInMonth.value.length;
  const issue = issueCount.value;
  const ok = total - issue;
  return { ok, issue, rate: total ? Math.round((ok / total) * 100) : 0 };
});

// ── 목록 뷰: 선택 사용자 OT 일자별 그룹핑 ─────────────────
// 선택 사용자(userCd)의 월간 OT 를 WORK_YMD 의 일(day)별로 묶는다.
const overtimeByDay = computed(() => {
  const map = {};
  if (!selectedUser.value) return map;
  const userCd = selectedUser.value.userCd;
  for (const ot of monthlyOvertimeList.value) {
    if (ot?.userCd !== userCd) continue;
    const ymd = String(ot.workYmd ?? "");
    if (!/^\d{8}$/.test(ymd)) continue;
    const day = Number(ymd.slice(6, 8));
    if (!map[day]) map[day] = [];
    map[day].push(ot);
  }
  return map;
});

// ── 목록 뷰: 선택 사용자 상세 행 빌드 ─────────────────────
// 정규근무 행 + (해당 일자에 OT 가 있으면) OT 행을 바로 아래에 끼워넣는다.
const detailRows = computed(() => {
  if (!selectedUser.value) return [];
  const u = selectedUser.value;
  const rows = [];
  daysInMonth.value.forEach((d) => {
    rows.push(buildDetailRow(u, d));
    const otList = overtimeByDay.value[d.day] ?? [];
    // PRAFTA-COM-013-06-4(r34-3): OT 를 어느 실적(1/2)에 매칭할지 판정하기 위해 그날 근태 record 를 함께 전달한다.
    const dayRecord = recordMap.value[`${u.userId}_${d.day}`];
    otList.forEach((ot, idx) => {
      rows.push(buildOvertimeRow(d, ot, idx, dayRecord));
    });
  });
  return rows;
});

function buildDetailRow(user, d) {
  const r = recordMap.value[`${user.userId}_${d.day}`];
  if (!r) return emptyRow(d, "off");
  // 실적이 하나라도 있으면 스케줄이 없어도 행을 채워서 표시
  const hasAct = !!(
    r.act1InTime ||
    r.act1OutTime ||
    r.act2InTime ||
    r.act2OutTime
  );
  if (!r.plan1Start && !r.plan2Start && !hasAct) {
    return emptyRow(d, "off");
  }

  // 선택 월 + 일자로 YYYYMMDD 구성 (오늘/미래/과거 판정용)
  const ymd =
    String(r.workYmd ?? "") ||
    `${workYm.value.replace("-", "")}${String(d.day).padStart(2, "0")}`;
  const { status, note } = detectAttdState(r, ymd);

  // A안: 확정 시각 연차(반차/시간차)와 실근태의 겹침 — 실근로/인정시간에서 공통 차감.
  //   (시간차는 스케줄 안에서만 신청되므로 인정시간(실제∩스케줄) 차감량도 동일하다.)
  const leaveOverlapMin = timeLeaveOverlapMin(r, ymd);

  return {
    kind: "work",
    rowKey: `work_${d.day}`,
    day: d.day,
    dow: d.dow,
    p1Start: fmtTime(r.plan1Start),
    p1End: fmtTime(r.plan1End),
    a1InDate: fmtMmdd(r.act1InDate),
    a1In: fmtTime(r.act1InTime),
    a1OutDate: fmtMmdd(r.act1OutDate),
    a1Out: fmtTime(r.act1OutTime),
    p2Start: fmtTime(r.plan2Start),
    p2End: fmtTime(r.plan2End),
    a2InDate: fmtMmdd(r.act2InDate),
    a2In: fmtTime(r.act2InTime),
    a2OutDate: fmtMmdd(r.act2OutDate),
    a2Out: fmtTime(r.act2OutTime),
    total: calcTotal(r, leaveOverlapMin),
    recognized: fmtRecognized(calcRecognized(r) - leaveOverlapMin),
    note,
    status,
    // 외근 배지 — 구간별 외근 플래그(attd{1,2}OutsideYn)가 'Y'인 구간만 노출
    outsideList: buildOutsideList(d, r),
    // PRAFTA-FIXEDOT-3: 고정연장 구간/실적/미이행 배지(서버 파생값 표시만 — 판정 로직 없음)
    fixedOtRange: fixedOtRangeLabel(r),
    // 0분도 "0분"으로 명시한다(미이행 배지와 짝이 되는 정보라 '−' 로 숨기면 오독)
    fixedOtAct:
      r.fixedOtActMinutes == null
        ? ""
        : Number(r.fixedOtActMinutes) > 0
          ? fmtMinutes(r.fixedOtActMinutes)
          : "0분",
    fixedOtUnmet: r.fixedOtUnfulfilledYn === "Y",
  };
}

// PRAFTA-FIXEDOT-3: 근무타입에 설정된 고정연장 구간 라벨(전방 · 후방 순).
//   고정연장 없는 타입/구서버 응답이면 빈 문자열 → 셀은 기존과 동일하게 '−'.
function fixedOtRangeLabel(r) {
  const parts = [];
  if (r.preFixedOtStrTime && r.preFixedOtEndTime) {
    parts.push(`${fmtTime(r.preFixedOtStrTime)}~${fmtTime(r.preFixedOtEndTime)}`);
  }
  if (r.fixedOtStrTime && r.fixedOtEndTime) {
    parts.push(`${fmtTime(r.fixedOtStrTime)}~${fmtTime(r.fixedOtEndTime)}`);
  }
  return parts.join(" · ");
}

// 정규근무 행의 외근 배지 목록 빌드.
//   각 항목: { seg, label, attdId, gpsKey }
//   gpsKey = `${day}_${seg}` (외근 배지 v-for 의 고유 key)
function buildOutsideList(d, r) {
  const list = [];
  if (r.attd1OutsideYn === "Y" && r.attd1Id) {
    list.push({
      seg: 1,
      label: "(1)",
      attdId: r.attd1Id,
      gpsKey: `${d.day}_1`,
    });
  }
  if (r.attd2OutsideYn === "Y" && r.attd2Id) {
    list.push({
      seg: 2,
      label: "(2)",
      attdId: r.attd2Id,
      gpsKey: `${d.day}_2`,
    });
  }
  return list;
}

// OT 가 어느 실적 구간(1/2)에 속하는지 판정한다.
//   PRAFTA-COM-013-06-4(r34-3): OT 는 ATTD_ID 로 근태 구간(WORK_SEQ)에 연결된다.
//   그날 근태 record 의 attd1Id/attd2Id 와 OT 의 attdId 를 대조해 1 또는 2 를 반환한다.
//   매칭 실패(레거시/데이터 불일치) 시 기존 동작과 동일하게 1구간(실적1)으로 fallback.
function resolveOvertimeSeg(ot, dayRecord) {
  const r = dayRecord ?? {};
  const otAttdId = ot?.attdId ?? "";
  if (otAttdId && r.attd2Id && otAttdId === r.attd2Id) return 2;
  if (otAttdId && r.attd1Id && otAttdId === r.attd1Id) return 1;
  return 1;
}

// OT 행 빌드 — 비고 칸 "초과근무" 배지, 매칭 실적 구간(1/2) 칸에 OT 시작/종료 표시.
function buildOvertimeRow(d, ot, idx, dayRecord) {
  return {
    kind: "ot",
    rowKey: `ot_${d.day}_${ot.otId ?? idx}`,
    day: d.day,
    dow: d.dow,
    // PRAFTA-COM-013-06-4(r34-3): 구간별(실적1/실적2) 매칭 표시용.
    seg: resolveOvertimeSeg(ot, dayRecord),
    otInDate: fmtMmdd(ot.actualStartDate),
    otIn: fmtTime(ot.actualStartTime),
    otOutDate: fmtMmdd(ot.actualEndDate),
    otOut: fmtTime(ot.actualEndTime),
    total: fmtMinutes(ot.workMinutes),
    recognized: fmtMinutes(ot.workMinutes),
    status: "",
  };
}

// 분(min) → "N시간 M분" 표기. OT 행 근무시간 칸 표시용.
function fmtMinutes(min) {
  const m = parseInt(min, 10);
  if (isNaN(m) || m <= 0) return "";
  const h = Math.floor(m / 60);
  const rm = m % 60;
  if (h && rm) return `${h}시간 ${rm}분`;
  if (h) return `${h}시간`;
  return `${rm}분`;
}

function emptyRow(d, status) {
  return {
    kind: "work",
    rowKey: `work_${d.day}`,
    day: d.day,
    dow: d.dow,
    p1Start: "",
    p1End: "",
    a1InDate: "",
    a1In: "",
    a1OutDate: "",
    a1Out: "",
    p2Start: "",
    p2End: "",
    a2InDate: "",
    a2In: "",
    a2OutDate: "",
    a2Out: "",
    total: "",
    recognized: "",
    note: "",
    status,
    outsideList: [],
    // PRAFTA-FIXEDOT-3: 휴무/무배정 행 — 고정연장 표기 없음
    fixedOtRange: "",
    fixedOtAct: "",
    fixedOtUnmet: false,
  };
}

const detailSummary = computed(() => {
  const rows = detailRows.value.filter(
    (r) => r.kind === "work" && r.status !== "off"
  );
  const alerts = rows.filter((r) => r.status === "alert").length;
  let recDays = 0,
    totalH = 0,
    totalM = 0;
  rows.forEach((r) => {
    const m = (r.total || "").match(/^(\d+)시간(?:\s*(\d+)분)?/);
    if (m) {
      recDays++;
      totalH += parseInt(m[1], 10);
      if (m[2]) totalM += parseInt(m[2], 10);
    }
  });
  totalH += Math.floor(totalM / 60);
  totalM = totalM % 60;
  return { alerts, recDays, totalH, totalM };
});

const valOrDash = (v) => (v ? v : "−");
const dowClass = (dow) => (dow === 0 ? "dow-sun" : dow === 6 ? "dow-sat" : "");

// ── 외근 GPS 동선 팝업 ────────────────────────────────────
// 외근 배지 클릭 시 GPS 동선 팝업(AttdGpsTrailPop)을 연다.
// 동선 조회는 팝업 내부에서 attdId 로 직접 수행한다.
const fnOpenGpsPop = (outside) => {
  if (!outside.attdId) return;
  openPop(AttdGpsTrailPop, {
    attdId_p: outside.attdId,
    label_p: `${outside.seg}구간`,
  });
};

// ── 팝업 핸들러 ───────────────────────────────────────────
// 일자 상세 팝업(AttdDayDetailPop)은 record 데이터를 그대로 전달
// PRAFTA-019-C 근태 마감 상태 조회 (사업장 + 월 기준)
const fnLoadCloseStatus = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    closeInfo.value = null;
    return;
  }
  try {
    const response = await axios.get("/webApi/attd07/attd-close-status", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        closeYm: workYm.value.replace("-", ""),
      },
    });
    if (response.status === 200) {
      closeInfo.value = response.data;
    }
  } catch (err) {
    // 마감 상태 조회 실패는 화면 핵심 기능을 막지 않는다 (배지/버튼만 미표시 fallback)
    closeInfo.value = null;
  }
};

// 마감 여부 / 버튼 라벨 / 버튼 활성 / 차단 카운트 (백엔드 우선, 미조회 시 프론트 파생값 fallback)
const isMonthClosed = computed(() => !!closeInfo.value?.closed);
const monthCloseLabel = computed(() =>
  isMonthClosed.value ? "마감 해제" : "근태 마감"
);
const monthCloseBtnEnabled = computed(() =>
  closeInfo.value
    ? closeInfo.value.closable || closeInfo.value.closed
    : canMonthClose.value
);
const blockCountDisplay = computed(() =>
  closeInfo.value ? closeInfo.value.blockTotalCnt : issueCount.value
);

// 근태 마감 / 마감 해제 실행 (PRAFTA-019-C — 자동/강제 마감 금지)
const fnOpenMonthClosePop = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    return;
  }

  // PRAFTA-028 - 조회 스코프 기준 마감 → 먼저 조회해야 함
  if (!hasSearched.value) {
    await proxy.$alert("먼저 조회한 뒤 마감해 주세요.");
    return;
  }
  // PRAFTA-028 - master/hr 이 아니면 사업장+소속부서 필수 (전체 마감 불가)
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }

  // 이미 마감된 기간 → 마감 해제 플로우
  if (isMonthClosed.value) {
    const ok = await proxy.$confirm("해당 월의 근태 마감을 해제하시겠습니까?");
    if (!ok) return;
    try {
      await axios.post("/webApi/attd07/attd-unclose", {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        closeYm: workYm.value.replace("-", ""),
      });
      await proxy.$alert("마감이 해제되었습니다.");
      await fnLoadCloseStatus();
    } catch (err) {
      await proxy.$alert(
        resolveApiErrorMessage(err, "마감 해제 중 오류가 발생했습니다.")
      );
    }
    return;
  }

  // 차단 사유 잔존 시 마감 불가
  if (!monthCloseBtnEnabled.value) {
    await proxy.$alert(getMessage(MSG.MONTH_CLOSE_BLOCKED));
    return;
  }

  const ok = await proxy.$confirm(
    "해당 월의 근태를 마감하시겠습니까? 마감 후에는 사후 신청이 차단됩니다."
  );
  if (!ok) return;
  try {
    await axios.post("/webApi/attd07/attd-close", {
      siteCd: siteCd.value,
      nodeCd: nodeCd.value,
      incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
      closeYm: workYm.value.replace("-", ""),
    });
    await proxy.$alert("근태가 마감되었습니다.");
    await fnLoadCloseStatus();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "근태 마감 중 오류가 발생했습니다.")
    );
  }
};
// 팝업은 식별자만 전달하고 상세/이력은 팝업 내부에서 API로 조회한다.
// (attd1Id 우선, 없으면 attd2Id, 둘 다 없으면 신규 케이스로 빈문자 전달)
// attdId가 없는 케이스는 팝업이 API를 안 부르므로 사용자/스케줄을 fallback_p로 같이 넘긴다.
const buildFallback = (user, record) => ({
  userId: user.userId ?? "",
  userCd: user.userCd ?? "",
  userNm: user.name ?? "",
  authCd: user.role ?? "",
  authNm: user.authNm ?? "",
  plan1Start: record.plan1Start ?? "",
  plan1End: record.plan1End ?? "",
  plan2Start: record.plan2Start ?? "",
  plan2End: record.plan2End ?? "",
  // [소속이동이력가시성-03] 팝업 자체 조회 완료 전에도 배지가 바로 보이도록 선세팅
  currentSiteCd: user.currentSiteCd ?? "",
  currentSiteNm: user.currentSiteNm ?? "",
});

const fnOpenDayDetailPop = (user, d) => {
  // 스케줄 데이터가 없는(off) 셀도 클릭 시 팝업이 열리도록 허용
  const [y, m] = workYm.value.split("-").map(Number);
  const ymd = `${y}-${String(m).padStart(2, "0")}-${String(d.day).padStart(2, "0")}`;
  const record = recordMap.value[`${user.userId}_${d.day}`] ?? {};
  openPop(AttdDayDetailPop, {
    attdId_p: record.attd1Id ?? record.attd2Id ?? "",
    siteCd_p: siteCd.value,
    userCd_p: user.userCd ?? "",
    nodeCd_p: record.nodeCd ?? "",
    date_p: ymd,
    dow_p: d.dow,
    fallback_p: buildFallback(user, record),
    isMonthClosed_p: isMonthClosed.value, // PRAFTA-028 - 마감 시 팝업 쓰기 차단
    onSaved: fnSearch,
  });
};

const fnOpenAttdAdjustPop = (row) => {
  // 캘린더 뷰(fnOpenDayDetailPop)와 동일하게 상태 무관하게 연다.
  //   종전 `status !== 'alert'` 게이트는 연차(status='pre')·정상 근무일에 걸린 연차 변경 요청을
  //   목록 뷰에서 열 수 없게 만들었다. 팝업 내용은 workYmd 기준 조회라 어느 경로로 열어도 동일하다.
  if (!selectedUser.value) return;
  const [y, m] = workYm.value.split("-").map(Number);
  const ymd = `${y}-${String(m).padStart(2, "0")}-${String(row.day).padStart(2, "0")}`;
  const record =
    recordMap.value[`${selectedUser.value.userId}_${row.day}`] ?? {};
  openPop(AttdDayDetailPop, {
    attdId_p: record.attd1Id ?? record.attd2Id ?? "",
    siteCd_p: siteCd.value,
    userCd_p: selectedUser.value.userCd ?? "",
    nodeCd_p: record.nodeCd ?? "",
    date_p: ymd,
    dow_p: row.dow,
    fallback_p: buildFallback(selectedUser.value, record),
    isMonthClosed_p: isMonthClosed.value, // PRAFTA-028 - 마감 시 팝업 쓰기 차단
    onSaved: fnSearch,
  });
};

// ── 응답 → 화면 모델 매핑 ─────────────────────────────────
const fnBindResponse = (data) => {
  reqIdList.value = data?.monthlyAttdReqSummaryResultList ?? [];
  leaveChangeSummaryList.value =
    data?.monthlyLeaveChangeSummaryResultList ?? [];
  monthlyOvertimeList.value = data?.monthlyOvertimeResultList ?? [];

  // A안: 확정 시각 연차 구간 맵 빌드 — 근무일 00:00 anchor 분, END<=START 는 익일 wrap(저장 규약).
  const tlMap = {};
  for (const w of data?.timeLeaveWindowList ?? []) {
    const s = hhmmToMin(w.startTime);
    let e = hhmmToMin(w.endTime);
    if (s == null || e == null) continue;
    if (e <= s) e += 1440;
    const key = `${w.userCd}_${w.workYmd}`;
    if (!tlMap[key]) tlMap[key] = [];
    tlMap[key].push([s, e]);
  }
  timeLeaveWindowsMap.value = tlMap;

  const recs = data?.attdRecordResultList ?? [];

  // 1. 사용자 목록 (userId 기준 unique, 등장 순서 유지)
  const seen = new Set();
  const list = [];
  const issuesByUser = {};
  const map = {};

  for (const r of recs) {
    if (!seen.has(r.userId)) {
      seen.add(r.userId);
      list.push({
        userId: r.userId,
        userCd: r.userCd,
        name: r.userNm,
        dept: r.deptNm,
        role: r.authNm ?? "",
        shift: r.plan2Start ? 2 : 1,
        // 고용형태가 DAILY 인 경우 일일사용자(일용직)로 표시 구분
        isDaily: r.employmentType === "DAILY",
        issues: 0,
        // [소속이동이력가시성-03] 현재 소속(이동자면 조회 사업장과 다름) — "타 사업장 소속" 배지 판정용
        currentSiteCd: r.currentSiteCd,
        currentSiteNm: r.currentSiteNm,
      });
    }

    const day = pickDay(r);
    if (day) map[`${r.userId}_${day}`] = r;

    if (isIssueRecord(r)) {
      issuesByUser[r.userId] = (issuesByUser[r.userId] || 0) + 1;
    }
  }

  // 분할근무 여부는 사용자 전체 record를 보고 한 번이라도 plan2가 있으면 2로
  list.forEach((u) => {
    u.issues = issuesByUser[u.userId] || 0;
  });

  userList.value = list;
  recordMap.value = map;

  // 선택 사용자 유지 또는 첫 사용자로 전환
  if (list.length && !list.find((u) => u.userId === selectedUserId.value)) {
    selectedUserId.value = list[0].userId;
  }
};

// ── 조회 ──────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }

  // 소속부서는 역할 무관 필수 — 백엔드(monthly-attd-lists)가 부서 기준으로 조회하므로
  //   미선택 시 서버 generic 400("요청 필수 파라미터 누락") 대신 어떤 조건이 비었는지 안내한다.
  if (proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }

  try {
    const response = await axios.get("/webApi/attd07/monthly-attd-lists", {
      params: {
        workYm: workYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value,
        employmentType: employmentType.value,
      },
    });

    if (response.status === 200) {
      fnBindResponse(response.data);
      hasSearched.value = true; // PRAFTA-028 - 조회 완료 → 마감 가능
    }
    await fnLoadCloseStatus();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR));
    await proxy.$alert(msg);
  }
};

// ── 초기화 ────────────────────────────────────────────────
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
    nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
    nodeNm.value = sessionStorage.getItem("gv_nodeNm") ?? "";
  }
};

// PRAFTA-028 - 스코프/월이 바뀌면 재조회 전까지 마감 불가 ('조회 먼저' 가드)
watch([siteCd, nodeCd, incSubNodeYn, workYm], () => {
  hasSearched.value = false;
});

// 사업장 조회조건이 바뀌면, 재조회 전까지 이전 사업장의 캘린더/목록 데이터가
//   화면에 남아있지 않도록 즉시 초기화한다(다른 사업장 데이터를 보고 있다고 오인하는 것 방지).
watch(siteCd, () => {
  userList.value = [];
  recordMap.value = {};
  reqIdList.value = [];
  leaveChangeSummaryList.value = [];
  monthlyOvertimeList.value = [];
  timeLeaveWindowsMap.value = {};
  selectedUserId.value = "";
  closeInfo.value = null;
});

onMounted(() => {
  fnInit();
  fnButtonControll();
});
</script>

<style scoped>
/* ── 공통 ─────────────────────────────────────────────────── */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

/* ── 툴바 ─────────────────────────────────────────────────── */
.a07-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
  flex-wrap: wrap;
  font-family: "Pretendard", sans-serif;
}
.a07-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}
.a07-nav-arr {
  width: 24px;
  height: 24px;
  border: 1px solid var(--color-border, #d1d5db);
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.875rem;
  line-height: 1;
}
.a07-nav-arr:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}
.a07-nav-month-picker {
  display: inline-flex;
  align-items: center;
}
.a07-nav-month-picker :deep(.calendar-input) {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  background: #fff;
  cursor: pointer;
  text-align: center;
  min-width: 110px;
  font-family: "Pretendard", sans-serif;
}
.a07-nav-month-picker :deep(.calendar-input:hover) {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
}
.a07-view-toggle {
  display: inline-flex;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  overflow: hidden;
  margin-left: 0.5rem;
}
.a07-view-btn {
  background: none;
  border: none;
  padding: 0.3rem 0.85rem;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}
.a07-view-btn.active {
  background: var(--color-primary, #16a34a);
  color: #fff;
  font-weight: 600;
}
.a07-actions {
  display: flex;
  gap: 0.4rem;
  align-items: center;
  font-size: 0.8125rem;
}
/* 처리 필요 건수 표시 (클릭 불가 — 단순 상태 표시용) */
.a07-issue-count {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: 1.5px solid #f59e0b;
  background: #fef3c7;
  color: #92400e;
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  font-size: 0.8125rem;
  font-family: "Pretendard", sans-serif;
}
.a07-issue-count b {
  font-size: 0.9rem;
}
.a07-issue-count.none {
  background: #fff;
  border-color: var(--color-border, #d1d5db);
  color: var(--color-text-muted, #9ca3af);
}
.a07-btn-line {
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  border: 1px solid var(--color-border, #d1d5db);
  background: #fff;
  cursor: pointer;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  font-family: "Pretendard", sans-serif;
}
.a07-btn-line:hover {
  background: var(--color-bg, #f3f4f6);
}
.a07-btn-line.disabled {
  color: var(--color-text-muted, #9ca3af);
  cursor: not-allowed;
  background: #fff;
}

/* ============================================================ */
/* 캘린더 뷰 */
/* ============================================================ */
.a07-cal-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a07-cal-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}
.a07-cal-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

table.a07-matrix {
  border-collapse: collapse;
  font-size: 0.6875rem;
  min-width: max-content;
  font-family: "Pretendard", sans-serif;
}
table.a07-matrix th,
table.a07-matrix td {
  border-right: 0.5px solid var(--color-border, #e5e7eb);
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
}
table.a07-matrix th {
  font-weight: 500;
  color: var(--color-text-muted, #6b7280);
  background: #fff;
  font-size: 0.6875rem;
  white-space: nowrap;
}
table.a07-matrix th.sticky-top {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--color-bg, #f9fafb);
}
table.a07-matrix th.sticky-left,
table.a07-matrix td.sticky-left {
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 1;
}
table.a07-matrix th.sticky-left.sticky-top {
  z-index: 3;
}

.m-user-head {
  padding: 8px 12px;
  width: 200px;
  min-width: 200px;
  text-align: left;
}
.m-day-head {
  width: 52px;
  min-width: 52px;
  text-align: center;
  line-height: 1.3;
  padding: 6px 2px;
}
.m-day-head .m-dow {
  font-size: 0.625rem;
  opacity: 0.85;
}
.m-day-head.head-sat {
  color: #3b82f6;
}
.m-day-head.head-sun {
  color: #ef4444;
}

.m-user-cell {
  padding: 9px 12px;
  vertical-align: middle;
  border-right: 1px solid var(--color-border, #d1d5db);
}
.m-user-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  line-height: 1.2;
}
.m-user-meta {
  font-size: 0.625rem;
  color: var(--color-text-muted, #9ca3af);
  margin-top: 2px;
  line-height: 1.3;
}

/* 일일사용자(일용직) 구분 배지 — 사용자명 옆 텍스트 + 배경색 구분 */
.badge-daily {
  display: inline-block;
  margin-left: 4px;
  padding: 1px 5px;
  font-size: 0.625rem;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
  color: var(--color-text-muted, #6b7280);
  background: var(--color-bg-muted, #f3f4f6);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  vertical-align: middle;
}

/* [소속이동이력가시성-03] "현재 타 사업장 소속" 배지 — CSS 변수만 사용(.badge-daily 패턴 재사용).
   조회 사업장과 근로자의 "현재" 소속이 다를 때만(이동 전 이력을 보고 있음을 알림). */
.badge-other-site {
  display: inline-block;
  margin-left: 4px;
  padding: 1px 5px;
  font-size: 0.625rem;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
  color: var(--color-text-muted, #6b7280);
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: 4px;
  vertical-align: middle;
}

table.a07-matrix td.m-day-cell {
  padding: 6px 3px;
  text-align: center;
  vertical-align: middle;
  color: var(--color-text-muted, #6b7280);
  min-width: 52px;
  height: 62px;
  position: relative;
  background: #fff;
  cursor: pointer;
}
table.a07-matrix td.m-day-cell:hover {
  background: rgba(22, 163, 74, 0.04);
}
table.a07-matrix td.m-day-cell.off {
  background: #fafaf7;
  color: #9a9a95;
}
table.a07-matrix td.m-day-cell.off:hover {
  background: rgba(22, 163, 74, 0.04);
}
.m-time-1 {
  line-height: 1.25;
}
.m-time-2 {
  line-height: 1.15;
  font-size: 0.625rem;
}
.m-time-2 .m-div {
  height: 0.5px;
  background: var(--color-border, #e5e7eb);
  margin: 2px 6px;
}
table.a07-matrix td.m-day-cell.code-cell {
  background: #f1efe8;
  color: var(--color-text-muted, #6b7280);
}
/* PRAFTA-028 - 마감된 월 셀 (회색 처리, 관리자 인지용) */
table.a07-matrix td.m-day-cell.closed {
  background: var(--color-bg-muted, #f1f3f5);
  color: var(--color-text-muted, #9ca3af);
}
table.a07-matrix td.m-day-cell.closed:hover {
  background: var(--color-bg-muted, #e9ecef);
}

/* 처리필요 — 라벨형 (monthlyAttdReqSummaryResultList 매칭 셀) */
table.a07-matrix td.m-day-cell.issue-label {
  background: #fef3c7;
  box-shadow: inset 0 0 0 1px #f59e0b;
  color: #92400e;
}
table.a07-matrix td.m-day-cell.issue-label:hover {
  background: #fde68a;
}
.m-code-label {
  line-height: 1.2;
}
.m-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 10px 10px 0;
  border-color: transparent #f59e0b transparent transparent;
}

.a07-cal-legend {
  display: flex;
  gap: 1rem;
  padding: 9px 14px;
  border-top: 0.5px solid var(--color-border, #e5e7eb);
  font-size: 0.6875rem;
  color: var(--color-text-muted, #6b7280);
  align-items: center;
  flex-wrap: wrap;
  background: #fff;
}
.a07-cal-legend .lg-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.sw {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  display: inline-block;
}
.sw-issue {
  background: #fef3c7;
  border: 0.5px solid #f59e0b;
}
.sw-marker {
  background: #fff;
  border: 0.5px solid var(--color-border, #d1d5db);
  position: relative;
}
.sw-marker::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 5px 5px 0;
  border-color: transparent #f59e0b transparent transparent;
}
.sw-code {
  background: #f1efe8;
}
.lg-sat {
  color: #3b82f6;
}
.lg-sun {
  color: #ef4444;
}
.lg-summary {
  margin-left: auto;
  font-size: 0.6875rem;
}
.lg-summary b {
  color: var(--color-text-strong, #111827);
}
.lg-summary .warn {
  color: #92400e;
}
.m-off {
  color: var(--color-text-muted, #9ca3af);
}

/* ============================================================ */
/* 목록 뷰 */
/* ============================================================ */
.a07-list-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a07-list-wrap {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}

/* 좌측 사용자 리스트 */
.a07-user-pane {
  border-right: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}
.a07-user-head {
  padding: 9px 12px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg, #f9fafb);
}
.a07-user-head b {
  color: var(--color-text-strong, #111827);
}
.a07-user-sort {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.6875rem;
  cursor: pointer;
}
.a07-user-list {
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}
.a07-user-item {
  padding: 9px 12px 9px 14px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  cursor: pointer;
  position: relative;
}
.a07-user-item:hover {
  background: #fafaf7;
}
.a07-user-item.active {
  background: rgba(22, 163, 74, 0.08);
}
.a07-user-item.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--color-primary, #16a34a);
}
.ui-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.4rem;
}
.ui-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  line-height: 1.2;
}
.a07-user-item.active .ui-name {
  color: #15803d;
}
.ui-meta {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #9ca3af);
  margin-top: 3px;
  line-height: 1.4;
}
.ui-badge-issue {
  font-size: 0.625rem;
  background: #fef3c7;
  color: #92400e;
  border: 0.5px solid #f59e0b;
  padding: 1px 6px;
  border-radius: 9px;
  font-weight: 600;
}
.ui-badge-ok {
  font-size: 0.625rem;
  color: var(--color-text-muted, #9ca3af);
}

/* 우측 상세 영역 */
.a07-detail {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.a07-detail-head {
  padding: 10px 18px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  background: #fff;
}
.a07-uinfo {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.a07-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.12);
  color: #15803d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.8125rem;
  flex-shrink: 0;
}
.a07-uname {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.2;
  color: var(--color-text-strong, #111827);
}
.a07-umeta {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 2px;
  line-height: 1.3;
}
.a07-metrics {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-left: auto;
}
.a07-m {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
  font-size: 0.75rem;
  white-space: nowrap;
}
.a07-m .a07-m-lbl {
  color: var(--color-text-muted, #6b7280);
}
.a07-m .a07-m-val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}
.a07-m .a07-m-val small {
  font-weight: 400;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.6875rem;
  margin-left: 2px;
}

.a07-detail-table-wrap {
  overflow: auto;
  flex: 1;
  min-height: 0;
}

/* ── 상세 테이블 (3단 헤더) ─────────────────────────────── */
table.a07-detail-table {
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  /* col 너비 합계와 일치해야 한다(표준화 컬럼 제거: 비고120+날짜80+구간1 392+구간2 392+근무시간110+인정시간110). */
  width: 1204px;
  font-size: 0.75rem;
  font-family: "Pretendard", sans-serif;
}
table.a07-detail-table col.c-date {
  width: 80px;
}
table.a07-detail-table col.c-plan {
  width: 60px;
}
table.a07-detail-table col.c-act-time {
  width: 80px;
}
table.a07-detail-table col.c-day {
  width: 56px;
}
table.a07-detail-table col.c-total {
  /* 근무시간 값(예: "12시간 00분")이 잘리지 않도록 90px → 110px 로 확대 */
  width: 110px;
}
table.a07-detail-table col.c-note {
  width: 120px;
}
/* PRAFTA-FIXEDOT-3: 고정연장 구간/실적 컬럼 */
table.a07-detail-table col.c-fixedot {
  width: 120px;
}

table.a07-detail-table th {
  padding: 7px 8px;
  /* height 가 패딩/보더 포함 실제 높이가 되도록 border-box 로 둔다.
     (sticky top 오프셋과 헤더 행 높이를 정확히 일치시켜 헤더 어긋남 방지) */
  box-sizing: border-box;
  text-align: center;
  font-weight: 500;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.6875rem;
  background: var(--color-bg, #f9fafb);
  white-space: nowrap;
  position: sticky;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 2단 헤더 sticky — top 오프셋은 윗 행 높이와 정확히 일치해야 한다.
   (border-box 기준: lvl1 그룹행 44 / lvl2 컬럼명행 40) */
table.a07-detail-table thead tr.lvl1 th {
  top: 0;
  z-index: 5;
  height: 44px;
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
}
/* sticky-left 코너 헤더(비고/날짜)는 그룹 헤더보다 확실히 위에 그려져야 한다.
   `tr.lvl1 th` 규칙(specificity 0,0,2,4)보다 높은 셀렉터(0,0,3,4)로 z-index 를
   덮어써, 횡스크롤 시 그룹 헤더(계획1 등)가 좌측 고정 컬럼을 침범하지 않게 한다. */
table.a07-detail-table thead tr.lvl1 th.col-note,
table.a07-detail-table thead tr.lvl1 th.date-h {
  z-index: 7;
}
table.a07-detail-table thead tr.lvl2 th {
  top: 44px;
  z-index: 4;
  height: 40px;
  font-size: 0.6875rem;
  font-weight: 500;
  color: var(--color-text-muted, #9a9a95);
  border-bottom: 1px solid #cfcfc8;
}
/* rowspan 헤더 셀(비고/날짜/근무시간)은 2개 행 전체 높이(44+40)를
   차지해야 sticky 상태에서 헤더가 끊기거나 컬럼이 어긋나 보이지 않는다. */
table.a07-detail-table thead th[rowspan="2"] {
  height: 84px;
}

/* 그룹 컬러 */
table.a07-detail-table th.l1-rs {
  background: var(--color-bg, #f9fafb);
}
table.a07-detail-table th.l2-plan,
table.a07-detail-table th.l3-plan {
  background: #eef4f9;
  color: #3b5a75;
}
table.a07-detail-table th.l2-actual,
table.a07-detail-table th.l3-actual {
  background: #f7f7f2;
}

/* 셀 */
table.a07-detail-table td {
  padding: 8px 8px;
  /* 모든 행 높이를 고정해 sticky 셀(비고/날짜)과 일반 셀의 끝단을 일치시킨다.
     (sticky <td> 는 행 높이에 맞춰 늘어나지 않으므로 명시 높이가 필요하다)
     height 는 한 줄 콘텐츠(line-height 1.5 → 18px)보다 넉넉히 잡아 글자가
     overflow:hidden 으로 잘리지 않게 한다.
     box-sizing: border-box 로 두어 th(line 1886)와 동일한 높이 기준을 쓰게 하고,
     vertical-align: middle 로 배지(비고)·텍스트(근무시간) 콘텐츠가 baseline 차이
     없이 세로 중앙 정렬되어 컬럼 간 행 높이가 어긋나 보이지 않게 한다. */
  box-sizing: border-box;
  height: 36px;
  line-height: 1.5;
  vertical-align: middle;
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
  font-size: 0.75rem;
  color: var(--color-text, #374151);
  white-space: nowrap;
  background: #fff;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
}
table.a07-detail-table td.right {
  text-align: right;
}
table.a07-detail-table td.col-plan {
  background: #f8fafc;
}
table.a07-detail-table td.col-actual {
  background: #fbfbf7;
}

/* 그룹 경계선 */
table.a07-detail-table th.bdr-section,
table.a07-detail-table td.bdr-section {
  border-left: 1.5px solid #b8b8b0;
}
table.a07-detail-table th.bdr-sub,
table.a07-detail-table td.bdr-sub {
  border-left: 0.5px solid #cfcfc8;
}

/* 비고 sticky (목록 뷰 최좌측 고정) */
table.a07-detail-table th.col-note {
  left: 0;
  z-index: 6;
}
table.a07-detail-table td.col-note {
  position: sticky;
  left: 0;
  z-index: 2;
  background: #fff;
}

/* 날짜 sticky (비고 우측에 이어서 고정) */
table.a07-detail-table th.date-h {
  position: sticky;
  left: 120px;
  top: 0;
  z-index: 6;
  background: var(--color-bg, #f9fafb);
  box-shadow: 1px 0 0 var(--color-border, #e5e7eb);
  min-width: 78px;
  border-bottom: 1px solid #cfcfc8;
}
table.a07-detail-table td.date {
  position: sticky;
  left: 120px;
  z-index: 2;
  background: #fff;
  box-shadow: 1px 0 0 var(--color-border, #e5e7eb);
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  text-align: left;
  min-width: 78px;
}

/* 행 상태 */
table.a07-detail-table tr.off td {
  background: #fafaf7;
  color: var(--color-text-muted, #9a9a95);
}
table.a07-detail-table tr.off td.col-plan,
table.a07-detail-table tr.off td.col-actual {
  background: #f4f4f0;
}
table.a07-detail-table tr.alert td {
  background: #fffbeb;
  cursor: pointer;
}
table.a07-detail-table tr.alert td.col-plan,
table.a07-detail-table tr.alert td.col-actual {
  background: #fbf7e5;
}
table.a07-detail-table tr.alert td.date {
  box-shadow:
    inset 3px 0 0 #f59e0b,
    1px 0 0 var(--color-border, #e5e7eb);
  background: #fffbeb;
}
table.a07-detail-table tr.alert:hover td {
  background: #fdf6c4;
}
table.a07-detail-table tr.pre td {
  color: var(--color-text-muted, #6b7280);
}

.alert-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  margin-right: 6px;
  vertical-align: middle;
}
.dow-sat {
  color: #3b82f6;
}
.dow-sun {
  color: #ef4444;
}

.badge-warn {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #fef3c7;
  color: #92400e;
  border: 0.5px solid #f59e0b;
}
.badge-info {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #eff6ff;
  color: #1d4ed8;
  border: 0.5px solid #93c5fd;
}
/* PRAFTA-FIXEDOT-3: "연장 미이행" 배지 — 조퇴(badge-warn)와 다른 별도 축임을 색으로도 구분.
   서버 파생 판정 결과를 표시만 한다(연차 계열 사용일에는 서버가 발화시키지 않음). */
.badge-fixedot-unmet {
  font-size: 0.625rem;
  padding: 2px 6px;
  border-radius: 9px;
  font-weight: 600;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  margin-right: 2px;
}
.dash {
  color: var(--color-text-muted, #9ca3af);
}

/* ── 외근 배지 (비고 칸) ──────────────────────────────────── */
.badge-outside {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 0.625rem;
  /* 줄 높이/세로 마진을 제거해 일반 인라인 배지와 동일하게 행 높이에 들어가도록 함 */
  line-height: 1;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #fff7ed;
  color: #92400e;
  border: 0.5px solid #fdba74;
  cursor: pointer;
  margin: 0 2px;
  vertical-align: middle;
}
.badge-outside:hover {
  background: #ffedd5;
}

/* ── 초과근무 배지 / 행 ───────────────────────────────────── */
.badge-ot {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #f3e8ff;
  color: #6b21a8;
  border: 0.5px solid #c084fc;
}
table.a07-detail-table tr.ot-row td {
  background: #faf8fd;
  color: var(--color-text, #374151);
}
table.a07-detail-table tr.ot-row td.col-plan {
  background: #f6f2fb;
}
table.a07-detail-table tr.ot-row td.col-actual {
  background: #f6f2fb;
}
table.a07-detail-table tr.ot-row td.date {
  background: #faf8fd;
}
</style>
