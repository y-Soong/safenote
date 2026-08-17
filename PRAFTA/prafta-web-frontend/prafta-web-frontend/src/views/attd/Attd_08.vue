<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcel"
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
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 기간 + 뷰 전환 툴바 (Attd_07 툴바 패턴 차용): 기간 픽커(< >)·전체/요약 토글·안내문구 -->
    <div class="a08-toolbar">
      <!-- From 캘린더 + 전일/익일 이동 -->
      <div class="a08-date-nav">
        <button
          type="button"
          class="a08-date-arr"
          @click="fnFromPrev"
          aria-label="시작일 전일"
        >
          ‹
        </button>
        <CalendarSrch :range="false" v-model="fromDate" />
        <button
          type="button"
          class="a08-date-arr"
          @click="fnFromNext"
          aria-label="시작일 익일"
        >
          ›
        </button>
      </div>
      <span class="period-sep">~</span>
      <!-- To 캘린더 + 전일/익일 이동 -->
      <div class="a08-date-nav">
        <button
          type="button"
          class="a08-date-arr"
          @click="fnToPrev"
          aria-label="종료일 전일"
        >
          ‹
        </button>
        <CalendarSrch :range="false" v-model="toDate" />
        <button
          type="button"
          class="a08-date-arr"
          @click="fnToNext"
          aria-label="종료일 익일"
        >
          ›
        </button>
      </div>
      <!-- 뷰 전환: 전체 / 요약 -->
      <div class="a08-view-toggle">
        <button
          type="button"
          :class="['a08-view-btn', { active: viewMode === 'full' }]"
          @click="viewMode = 'full'"
        >
          전체
        </button>
        <button
          type="button"
          :class="['a08-view-btn', { active: viewMode === 'summary' }]"
          @click="viewMode = 'summary'"
        >
          요약
        </button>
      </div>
      <!-- 휴게시간 자동 차감 안내 (전체/요약 버튼 우측) -->
      <p class="a08-note">
        ※ <b>실근로시간</b>과 <b>인정시간(정상근무)</b>은 스케줄에 등록된
        휴게시간을 자동 차감하여 표시합니다. 초과근무 인정시간은 정해진
        휴게시간이 없어 관리자가 승인한 근로시간 전체를 표시합니다.
        <b>고정연장</b>은 실제 근무가 고정연장 구간과 겹친 시간만 별도로
        계상합니다(인정시간과 합산하지 않음).
      </p>
    </div>

    <!-- 본문(전체/요약)을 subtitle-pane 래퍼로 감싼다 (테이블 높이/스크롤 관리) -->
    <div class="table-wrapper subtitle-pane">
      <!-- 본문(전체): 좌측 결과 테이블 / 우측 상세 패널 -->
      <div
        v-show="viewMode === 'full'"
        class="viewBody a08-body"
        :class="{ 'detail-open': !!selected }"
      >
        <div class="a08-table-wrap">
          <table class="a08-table" :style="theadStyleVars">
            <thead>
              <tr ref="theadRow1El">
                <th rowspan="2">사용자명</th>
                <th rowspan="2">부서</th>
                <th rowspan="2">근무구분</th>
                <th rowspan="2">근무일</th>
                <th rowspan="2">요일</th>
                <th rowspan="2">차수</th>
                <th rowspan="2">스케줄</th>
                <th colspan="4">실제근무</th>
                <th rowspan="2">실근로시간</th>
                <th rowspan="2">인정시간</th>
                <!-- PRAFTA-FIXEDOT-3(정책 ①): 고정연장 실적 별도 축 — 인정시간(소정)과 분리 표기 -->
                <th rowspan="2">고정연장</th>
                <th rowspan="2">상태</th>
                <th rowspan="2">상세</th>
              </tr>
              <tr>
                <th>출근일</th>
                <th>출근시각</th>
                <th>퇴근일</th>
                <th>퇴근시각</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="displayRows.length === 0">
                <td colspan="16" class="a08-empty">조회 결과가 없습니다.</td>
              </tr>
              <tr
                v-for="r in displayRows"
                :key="r._rowKey"
                :class="rowClass(r)"
                @click="fnSelectRow(r)"
              >
                <td>{{ r.userNm }}</td>
                <td>{{ r.nodeNm }}</td>
                <!-- 근무구분: 정상근무 / 초과근무 (prafta-043: 유형 파기) -->
                <td>
                  <span
                    :class="['a08-badge', r._isOt ? 'b-ot' : 'b-work-normal']"
                  >
                    {{ workTypeLabel(r) }}
                  </span>
                </td>
                <td>{{ fmtYmd(r.workYmd) }}</td>
                <td>{{ fmtDow(r.workYmd) }}</td>
                <!-- 차수: 초과근무 행은 '-' -->
                <td>{{ r._isOt ? "-" : r.workSeq }}</td>
                <!-- 스케줄(통합): 차수에 해당하는 구간. 초과근무 행은 '-'
                     PRAFTA-FIXEDOT-3: 고정연장 보유 타입은 경계 슬롯에 구간을 구분 표기(additive) -->
                <td>
                  {{ scheduleCell(r) }}
                  <div v-if="fixedOtCellLabel(r)" class="a08-fixedot-label">
                    {{ fixedOtCellLabel(r) }}
                  </div>
                </td>
                <!-- 실제근무 (차수에 해당하는 구간 / 초과근무 실제 출퇴근) -->
                <td>{{ dCell(r._inDate) }}</td>
                <td>{{ tCell(r._inTime) }}</td>
                <td>{{ dCell(r._outDate) }}</td>
                <td>{{ tCell(r._outTime) }}</td>
                <!-- 실근로시간: 실제 구간 − 휴게 -->
                <td>{{ fmtDuration(workedNetMin(r)) }}</td>
                <!-- 인정시간: 정상=(실제∩스케줄)−휴게 / 초과=관리자 승인 시간 -->
                <td>{{ fmtDuration(recognizedMin(r)) }}</td>
                <!-- PRAFTA-FIXEDOT-3(정책 ①): 고정연장 실적(실근태∩고정연장, 서버 파생 fixedOtActMinutes).
                     일 단위 값이라 그날 마지막 스케줄 슬롯 행에만 실림(그 외 행·초과근무 행은 '-'). -->
                <td>{{ r.fixedOtActMinutes != null ? fmtDuration(r.fixedOtActMinutes) : "-" }}</td>
                <!-- 상태: 초과근무 행은 배지 없이 '-' 텍스트만 -->
                <td>
                  <template v-if="r._isOt">-</template>
                  <span
                    v-else
                    :class="['a08-badge', statusBadgeClass(r._status)]"
                  >
                    {{ statusLabel(r._status) }}
                  </span>
                  <!-- PRAFTA-FIXEDOT-3(정책 ②): "연장 미이행" 배지 — 조퇴 판정/통계와 완전 분리된
                       별도 표식(서버 파생 판정, 연차 계열 사용일에는 서버가 발화시키지 않음) -->
                  <span
                    v-if="r.fixedOtUnfulfilledYn === 'Y'"
                    class="a08-badge b-fixedot-unmet"
                  >
                    연장 미이행
                  </span>
                </td>
                <td>
                  <!-- 상세(GPS 동선)는 GPS 기록이 있는 외근 행만 노출 -->
                  <button
                    v-if="r.isOutsideYn === 'Y'"
                    class="a08-btn-detail"
                    @click.stop="fnSelectRow(r)"
                  >
                    상세
                  </button>
                  <span v-else class="a08-no-detail">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 상세 패널 (행 클릭 시 표시) -->
        <div v-if="selected" class="a08-detail-panel">
          <div class="a08-detail-head">
            <div>
              <div class="a08-detail-title">
                {{ selected.userNm }} ({{ selected.userId }})
              </div>
              <div class="a08-detail-sub">
                {{ selected.nodeNm }} · {{ fmtYmd(selected.workYmd) }}
                <template v-if="!selected._isOt">
                  · 차수 {{ selected.workSeq }}
                </template>
              </div>
            </div>
            <button class="a08-detail-close" @click="fnCloseDetail">×</button>
          </div>

          <div class="a08-detail-meta">
            <!-- 근무구분 (PRAFTA-015): 정상근무 / 초과근무 (prafta-043: 유형 파기) -->
            <div class="meta-row">
              <span class="meta-label">근무구분</span>
              <span class="meta-value">
                <span
                  :class="[
                    'a08-badge',
                    selected._isOt ? 'b-ot' : 'b-work-normal',
                  ]"
                >
                  {{ workTypeLabel(selected) }}
                </span>
              </span>
            </div>
            <div class="meta-row">
              <span class="meta-label">스케줄</span>
              <span class="meta-value">
                {{ scheduleCell(selected) }}
                <span v-if="fixedOtCellLabel(selected)" class="a08-fixedot-label">
                  {{ fixedOtCellLabel(selected) }}
                </span>
              </span>
            </div>
            <!-- PRAFTA-FIXEDOT-3: 고정연장 실적(실근태 ∩ 고정연장 파생 계상) — 마지막 슬롯 행에만 수신 -->
            <div v-if="selected.fixedOtActMinutes != null" class="meta-row">
              <span class="meta-label">고정연장 실적</span>
              <span class="meta-value">
                {{ fmtDuration(selected.fixedOtActMinutes) }}
                <span
                  v-if="selected.fixedOtUnfulfilledYn === 'Y'"
                  class="a08-badge b-fixedot-unmet"
                >
                  연장 미이행
                </span>
              </span>
            </div>
            <div class="meta-row">
              <span class="meta-label">실제근무</span>
              <span class="meta-value">{{
                dtRange(
                  selected._inDate,
                  selected._inTime,
                  selected._outDate,
                  selected._outTime
                )
              }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">실근로시간</span>
              <span class="meta-value">{{
                fmtDuration(workedNetMin(selected))
              }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">인정시간</span>
              <span class="meta-value">{{
                fmtDuration(recognizedMin(selected))
              }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">상태</span>
              <!-- meta-value 는 flex 셀 역할만, 배지는 안쪽 inline span 으로 분리해
                 텍스트 크기만큼만 배경이 채워지도록 한다(PRAFTA-015) -->
              <span class="meta-value">
                <template v-if="selected._isOt">-</template>
                <span
                  v-else
                  :class="['a08-badge', statusBadgeClass(selected._status)]"
                >
                  {{ statusLabel(selected._status) }}
                </span>
              </span>
            </div>
            <div class="meta-row">
              <span class="meta-label">외근여부</span>
              <!-- 배지를 안쪽 inline span 으로 분리 (PRAFTA-015) -->
              <span class="meta-value">
                <span
                  :class="[
                    'a08-badge',
                    selected.isOutsideYn === 'Y' ? 'b-out' : 'b-in',
                  ]"
                >
                  {{ selected.isOutsideYn === "Y" ? "외근" : "내근" }}
                </span>
              </span>
            </div>
          </div>

          <!-- 지도 영역: 외근일 때만 GPS 호출 -->
          <div class="a08-map-section">
            <div class="a08-map-title">GPS 동선</div>

            <!-- 출근/퇴근/전체 필터 — 출근·퇴근 좌표가 겹쳐 가려질 때 개별 확인용 -->
            <div
              v-if="
                selected.isOutsideYn === 'Y' &&
                !gpsLoading &&
                validGpsList.length > 0
              "
              class="a08-gps-filter"
            >
              <button
                type="button"
                class="a08-gps-filter-btn"
                :class="{ 'is-active': gpsViewMode === 'all' }"
                @click="setGpsViewMode('all')"
              >
                전체
              </button>
              <button
                type="button"
                class="a08-gps-filter-btn"
                :class="{ 'is-active': gpsViewMode === '01' }"
                :disabled="gpsStartCount === 0"
                @click="setGpsViewMode('01')"
              >
                출근
              </button>
              <button
                type="button"
                class="a08-gps-filter-btn"
                :class="{ 'is-active': gpsViewMode === '02' }"
                :disabled="gpsEndCount === 0"
                @click="setGpsViewMode('02')"
              >
                퇴근
              </button>
            </div>

            <div v-if="selected.isOutsideYn !== 'Y'" class="a08-map-empty">
              내근 근태로 GPS 기록이 없습니다.
            </div>
            <div v-else-if="gpsLoading" class="a08-map-empty">
              GPS 정보를 불러오는 중...
            </div>
            <div v-else-if="validGpsList.length === 0" class="a08-map-empty">
              수집된 GPS 좌표가 없습니다.
            </div>
            <div
              v-else
              id="a08-kakao-map"
              ref="mapContainer"
              class="a08-map-canvas"
            ></div>

            <div v-if="validGpsList.length > 0" class="a08-gps-summary">
              총 <b>{{ validGpsList.length }}</b
              >건
              <span v-if="mockedCount > 0" class="mocked-warn">
                (Mock 좌표 {{ mockedCount }}건 포함)
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 본문(요약): 날짜 기준 인원별 근태 요약 (차수/초과 합산) -->
      <div v-show="viewMode === 'summary'" class="viewBody a08-summary-body">
        <div class="a08-table-wrap">
          <table class="a08-table a08-summary-table">
            <thead>
              <tr>
                <th>사용자명</th>
                <th>부서</th>
                <th>근무일</th>
                <th>요일</th>
                <th>실근로시간(분)</th>
                <th>인정시간(분)</th>
                <th>고정연장(분)</th>
                <th>지각(분)</th>
                <th>조기퇴근(분)</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="summaryRows.length === 0">
                <td colspan="10" class="a08-empty">조회 결과가 없습니다.</td>
              </tr>
              <tr
                v-for="s in summaryRows"
                :key="s._key"
                :class="summaryRowClass(s)"
              >
                <td>{{ s.userNm }}</td>
                <td>{{ s.nodeNm }}</td>
                <td>{{ fmtYmd(s.workYmd) }}</td>
                <td>{{ fmtDow(s.workYmd) }}</td>
                <td>{{ fmtMinutes(s.workedMin) }}</td>
                <td>{{ fmtMinutes(s.recognizedMin) }}</td>
                <td>{{ fmtMinutes(s.fixedOtMin) }}</td>
                <td>{{ fmtMinutes(s.lateMin) }}</td>
                <td>{{ fmtMinutes(s.earlyMin) }}</td>
                <td>
                  <span
                    v-if="s._status"
                    :class="['a08-badge', statusBadgeClass(s._status)]"
                  >
                    {{ statusLabel(s._status) }}
                  </span>
                  <template v-else>-</template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  getCurrentInstance,
  defineProps,
  defineOptions,
  onBeforeUnmount,
  onMounted,
  onActivated,
  nextTick,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import { useDashboardNavStore } from "@/stores/dashboardNavStore";
import { ymToDateRange } from "@/utils/common";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { exportStyledExcel } from "@/utils/excelExport";
import { formatYmdDot } from "@/utils/dateFormat";

defineOptions({ name: "Attd_08" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const dashNav = useDashboardNavStore();

const localButtons = ref({ ...props.buttons });

// hide non-search buttons
(() => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "Y";
})();

// 조회 조건
const fromDate = ref(defaultFrom());
const toDate = ref(defaultTo());
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const searchUserNm = ref("");
const siteNoFcs = ref(null);

// security H-1: 전사 권한(master/hr) 여부 — 그 외 권한은 사업장 + 소속부서 필수(서버 canManageNode 게이트).
//   Attd_11(:243~247) 동일 패턴.
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

function defaultFrom() {
  const d = new Date();
  d.setMonth(d.getMonth() - 1);
  return toIsoDate(d);
}
function defaultTo() {
  return toIsoDate(new Date());
}
function toIsoDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// 기간 픽커 하루 단위 이동 (< / > 화살표). 값이 없으면 오늘 기준으로 시작.
function shiftIsoDate(iso, deltaDays) {
  const base = iso ? iso.split("-").map(Number) : null;
  const dt = base ? new Date(base[0], base[1] - 1, base[2]) : new Date();
  dt.setDate(dt.getDate() + deltaDays);
  return toIsoDate(dt);
}
function fnFromPrev() {
  fromDate.value = shiftIsoDate(fromDate.value, -1);
}
function fnFromNext() {
  fromDate.value = shiftIsoDate(fromDate.value, 1);
}
function fnToPrev() {
  toDate.value = shiftIsoDate(toDate.value, -1);
}
function fnToNext() {
  toDate.value = shiftIsoDate(toDate.value, 1);
}

// 사업장/부서 자동조회 처리 (Attd_07 패턴 차용)
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

// 결과
const rows = ref([]);
// A안(2026-08-17): 확정 "시각 보유" 연차(반차/시간차) 구간 맵 — `${userCd}_${workYmd}` → [[startHHMM,endHHMM],...]
//   실근로/인정시간 표시에서 실근태와의 겹침 차감용(연차 시간은 근로시간 미산입).
const timeLeaveWinMap = ref({});
const selected = ref(null);

// 뷰 전환 모드 — 'full'(전체) | 'summary'(요약) (Attd_07 토글 패턴 차용)
const viewMode = ref("full");

// 2단 헤더 sticky 오프셋 — 1행(rowspan 셀) 실제 렌더 높이를 측정해 2행(출근일 등)의
// top 값으로 주입한다. rem 값을 고정 추정하면 폰트/브라우저 렌더링 차이로 헤더 경계
// 틈이 생겨 스크롤된 본문 행이 비치는 문제가 있어, ResizeObserver 로 항상 실측한다.
const theadRow1El = ref(null);
const thead1H = ref(37);
const theadStyleVars = computed(() => ({
  "--a08-thead1-h": `${thead1H.value}px`,
}));
let thead1RO = null;
const measureThead1 = () => {
  const h = theadRow1El.value?.getBoundingClientRect().height;
  if (h) thead1H.value = h;
};
onMounted(() => {
  measureThead1();
  if (window.ResizeObserver && theadRow1El.value) {
    thead1RO = new ResizeObserver(measureThead1);
    thead1RO.observe(theadRow1El.value);
  } else {
    window.addEventListener("resize", measureThead1);
  }
});
onActivated(measureThead1);
onBeforeUnmount(() => {
  if (thead1RO) thead1RO.disconnect();
  else window.removeEventListener("resize", measureThead1);
});

// 클라이언트측 기간 검증 (≤3개월)
function isWithinThreeMonths(fromIso, toIso) {
  if (!fromIso || !toIso) return false;
  const f = new Date(fromIso);
  const t = new Date(toIso);
  if (isNaN(f.getTime()) || isNaN(t.getTime())) return false;
  if (f > t) return false;
  const limit = new Date(f);
  limit.setMonth(limit.getMonth() + 3);
  return t <= limit;
}

// 조회 실행
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert("사업장을 선택해 주세요.");
    return;
  }
  // security H-1: 서버 부서 관리 권한 게이트(canManageNode)와 동일 조건을 화면에서도 안내한다
  //   (Attd_11 :434~438 미러). 전사 권한이 아니면 부서 미지정 = 사업장 전체 조회라 서버가 403 을 준다.
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }
  if (
    proxy.$util.isEmpty(fromDate.value) ||
    proxy.$util.isEmpty(toDate.value)
  ) {
    await proxy.$alert("조회 기간을 입력해 주세요.");
    return;
  }
  if (!isWithinThreeMonths(fromDate.value, toDate.value)) {
    await proxy.$alert("조회 기간은 최대 3개월까지만 가능합니다.");
    return;
  }

  try {
    const response = await axios.get("/webApi/attd08/attd-lists", {
      params: {
        fromDate: fromDate.value,
        toDate: toDate.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value || "",
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value || "",
      },
    });
    if (response.status === 200) {
      console.log(response.data);
      rows.value = response.data?.attdListsResultList ?? [];
      // A안(2026-08-17): 확정 시각 연차 구간 맵 — `${userCd}_${workYmd}` → [[startHHMM,endHHMM],...]
      //   구서버 응답(필드 부재)이면 빈 맵 = 종전 값 그대로(무회귀).
      const tlMap = {};
      for (const w of response.data?.timeLeaveWindowList ?? []) {
        const key = `${w.userCd}_${w.workYmd}`;
        if (!tlMap[key]) tlMap[key] = [];
        tlMap[key].push([w.startTime, w.endTime]);
      }
      timeLeaveWinMap.value = tlMap;
      // 상세 닫기 (조회 결과가 갱신됨)
      fnCloseDetail();
    }
  } catch (err) {
    console.error("[Attd_08] search failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 엑셀 다운로드 — 화면 테이블과 동일 컬럼/포맷으로 내보낸다.
//   - displayRows 의 파생필드(_isOt, _status, _inDate/_inTime/...)를 그대로 사용.
//   - 초과근무 행은 차수/스케줄/정규화/상태 컬럼이 '-' 로 표시되는 규칙도 동일하게 적용.
const fnExcel = async () => {
  // 요약 뷰: 날짜 기준 인원별 요약 시트로 내보낸다.
  if (viewMode.value === "summary") {
    if (summaryRows.value.length === 0) {
      await proxy.$alert("내보낼 데이터가 없습니다.");
      return;
    }
    const columns = [
      { header: "사용자명", fixed: false, width: 12 },
      { header: "부서", fixed: false, width: 16 },
      { header: "근무일", fixed: false, width: 12 },
      { header: "요일", fixed: false, width: 6 },
      { header: "실근로시간(분)", fixed: false, width: 14 },
      { header: "인정시간(분)", fixed: false, width: 14 },
      { header: "고정연장(분)", fixed: false, width: 14 },
      { header: "지각(분)", fixed: false, width: 10 },
      { header: "조기퇴근(분)", fixed: false, width: 12 },
      { header: "상태", fixed: false, width: 10 },
    ];
    const data = summaryRows.value.map((s) => [
      s.userNm ?? "",
      s.nodeNm ?? "",
      fmtYmd(s.workYmd),
      fmtDow(s.workYmd),
      fmtMinutes(s.workedMin),
      fmtMinutes(s.recognizedMin),
      fmtMinutes(s.fixedOtMin),
      fmtMinutes(s.lateMin),
      fmtMinutes(s.earlyMin),
      s._status ? statusLabel(s._status) : "-",
    ]);
    try {
      await exportStyledExcel({
        fileName: `근태요약_${(fromDate.value || "").replaceAll("-", "")}_${(toDate.value || "").replaceAll("-", "")}.xlsx`,
        sheets: [{ name: "근태요약", columns, data }],
      });
    } catch (err) {
      console.error("[Attd_08] excel export failed", err);
      await proxy.$alert("엑셀 다운로드 중 오류가 발생했습니다.");
    }
    return;
  }

  if (displayRows.value.length === 0) {
    await proxy.$alert("내보낼 데이터가 없습니다.");
    return;
  }
  const columns = [
    { header: "사용자명", fixed: false, width: 12 },
    { header: "부서", fixed: false, width: 16 },
    { header: "근무구분", fixed: false, width: 14 },
    { header: "근무일", fixed: false, width: 12 },
    { header: "요일", fixed: false, width: 6 },
    { header: "차수", fixed: false, width: 6 },
    { header: "스케줄", fixed: false, width: 18 },
    { header: "실제 출근일", fixed: false, width: 10 },
    { header: "실제 출근시각", fixed: false, width: 10 },
    { header: "실제 퇴근일", fixed: false, width: 10 },
    { header: "실제 퇴근시각", fixed: false, width: 10 },
    { header: "실근로시간", fixed: false, width: 12 },
    { header: "인정시간", fixed: false, width: 12 },
    { header: "고정연장", fixed: false, width: 12 },
    { header: "상태", fixed: false, width: 10 },
    { header: "외근여부", fixed: false, width: 8 },
  ];
  const data = displayRows.value.map((r) => [
    r.userNm ?? "",
    r.nodeNm ?? "",
    workTypeLabel(r),
    fmtYmd(r.workYmd),
    fmtDow(r.workYmd),
    r._isOt ? "-" : (r.workSeq ?? ""),
    scheduleCell(r),
    dCell(r._inDate),
    tCell(r._inTime),
    dCell(r._outDate),
    tCell(r._outTime),
    fmtDuration(workedNetMin(r)),
    fmtDuration(recognizedMin(r)),
    r.fixedOtActMinutes != null ? fmtDuration(r.fixedOtActMinutes) : "-",
    r._isOt ? "-" : statusLabel(r._status),
    r.isOutsideYn === "Y" ? "외근" : "내근",
  ]);
  try {
    await exportStyledExcel({
      fileName: `근태조회_${(fromDate.value || "").replaceAll("-", "")}_${(toDate.value || "").replaceAll("-", "")}.xlsx`,
      sheets: [{ name: "근태조회", columns, data }],
    });
  } catch (err) {
    console.error("[Attd_08] excel export failed", err);
    await proxy.$alert("엑셀 다운로드 중 오류가 발생했습니다.");
  }
};

// 표시 헬퍼
// 표시용 날짜 포맷은 dateFormat 단일 출처에 위임(점 구분).
const fmtYmd = (ymd) => formatYmdDot(ymd);
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};
const planRange = (a, b) => {
  if (!a && !b) return "-";
  return `${fmtTime(a) || "-"} ~ ${fmtTime(b) || "-"}`;
};
// yyyyMMdd -> MM-DD
const fmtMd = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "";
  return `${s.slice(4, 6)}-${s.slice(6, 8)}`;
};
// yyyyMMdd -> 요일 라벨 (일~토)
const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];
const fmtDow = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "";
  const day = new Date(
    Number(s.slice(0, 4)),
    Number(s.slice(4, 6)) - 1,
    Number(s.slice(6, 8))
  ).getDay();
  return dowLabels[day] ?? "";
};
// 날짜+시각을 묶어 "MM-DD HH:mm ~ MM-DD HH:mm" 형태로 표시 (상세 패널용)
const dtRange = (inDate, inTime, outDate, outTime) => {
  const fmtOne = (date, time) => {
    if (!time) return "";
    const md = fmtMd(date);
    const hm = fmtTime(time);
    return md ? `${md} ${hm}` : hm;
  };
  const inPart = fmtOne(inDate, inTime);
  const outPart = fmtOne(outDate, outTime);
  if (!inPart && !outPart) return "-";
  return `${inPart || "-"} ~ ${outPart || "-"}`;
};
// 테이블 일자 셀 (MM-DD)
const dCell = (ymd) => fmtMd(ymd) || "-";
// 테이블 시각 셀 (HH:mm)
const tCell = (hhmm) => fmtTime(hhmm) || "-";

// ───────────────────────────────────────────────────────────
// 근로시간 산정 (실근로시간 / 인정시간).
//   - 실근로시간   : 실제 출근~퇴근 구간 길이 − 스케줄 휴게시간(차수별).
//   - 인정시간(정상): (실제근무 ∩ 스케줄) 교집합 길이 − 스케줄 휴게시간(차수별).
//   - 인정시간(초과): 관리자 승인 실근무 분(otWorkMinutes, 휴게 제외)을 전체 표시.
//   휴게 차감 결과가 음수면 0 으로 클램프한다. 자정 넘김은 일자(yyyyMMdd) 기준으로 보정.
// ───────────────────────────────────────────────────────────
// 'yyyyMMdd' + 'HHmm' → 분 단위 절대값. 일자 미기재 시 baseYmd 로 보정.
const dtMinutes = (ymd, hhmm, baseYmd) => {
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
// 차수별 스케줄 휴게시간(분). 초과근무는 정해진 휴게 없음(0).
const schedBreakMin = (r) => {
  if (r._isOt) return 0;
  const isSeq2 = String(r.workSeq) === "2";
  const n = parseInt(isSeq2 ? r.plan2BreakMin : r.plan1BreakMin, 10);
  return isNaN(n) ? 0 : n;
};
// 실제 출근~퇴근 총 구간(분, 휴게 차감 전).
const actualGrossMin = (r) => {
  const inM = dtMinutes(r._inDate, r._inTime, r.workYmd);
  let outM = dtMinutes(r._outDate, r._outTime, r.workYmd);
  if (inM == null || outM == null) return null;
  // 퇴근 일자 미기재로 퇴근<출근이면 자정 넘김으로 보고 +1일
  if (outM < inM) outM += 1440;
  if (outM < inM) return null;
  return outM - inM;
};
// A안(2026-08-17): 확정 "시각 보유" 연차(반차/시간차) 구간과 [sM,eM] 구간의 겹침(분).
//   연차 사용 시간은 유급이되 근로시간이 아니므로 실근로/인정시간에서 차감한다.
//   구간 절대분 프레임은 dtMinutes(epoch 분)와 동일 축, END<=START 는 익일 wrap(저장 규약).
const leaveOverlapForRange = (r, sM, eM) => {
  if (r._isOt || sM == null || eM == null || eM <= sM) return 0;
  const wins = timeLeaveWinMap.value[`${r.userCd}_${r.workYmd}`];
  if (!wins || !wins.length) return 0;
  let total = 0;
  for (const w of wins) {
    const ws = dtMinutes(r.workYmd, w[0], r.workYmd);
    let we = dtMinutes(r.workYmd, w[1], r.workYmd);
    if (ws == null || we == null) continue;
    if (we <= ws) we += 1440;
    const os = Math.max(sM, ws);
    const oe = Math.min(eM, we);
    if (oe > os) total += oe - os;
  }
  return total;
};

// 실근로시간(분) = 실제 구간 − 휴게 − 확정 시각 연차 겹침(A안).
const workedNetMin = (r) => {
  const gross = actualGrossMin(r);
  if (gross == null) return null;
  const inM = dtMinutes(r._inDate, r._inTime, r.workYmd);
  let outM = dtMinutes(r._outDate, r._outTime, r.workYmd);
  let leaveOverlap = 0;
  if (inM != null && outM != null) {
    if (outM < inM) outM += 1440;
    leaveOverlap = leaveOverlapForRange(r, inM, outM);
  }
  return Math.max(0, gross - schedBreakMin(r) - leaveOverlap);
};
// 인정시간(분).
const recognizedMin = (r) => {
  // 초과근무: 관리자 승인 실근무 분(휴게 제외) 전체.
  if (r._isOt) {
    const n = parseInt(r.otWorkMinutes, 10);
    return isNaN(n) ? null : Math.max(0, n);
  }
  // 정상근무: (실제근무 ∩ 스케줄) − 휴게.
  const isSeq2 = String(r.workSeq) === "2";
  const schStart = isSeq2 ? r.plan2Start : r.plan1Start;
  const schEnd = isSeq2 ? r.plan2End : r.plan1End;
  if (!schStart || !schEnd) return null;
  const inM = dtMinutes(r._inDate, r._inTime, r.workYmd);
  let outM = dtMinutes(r._outDate, r._outTime, r.workYmd);
  if (inM == null || outM == null) return null;
  if (outM < inM) outM += 1440;
  const schStartM = dtMinutes(r.workYmd, schStart, r.workYmd);
  let schEndM = dtMinutes(r.workYmd, schEnd, r.workYmd);
  if (schStartM == null || schEndM == null) return null;
  // 종료<시작이면 익일 종료(야간 스케줄)
  if (schEndM < schStartM) schEndM += 1440;
  const overlap = Math.max(
    0,
    Math.min(outM, schEndM) - Math.max(inM, schStartM)
  );
  // A안: 확정 시각 연차 겹침 차감 — (실제∩스케줄) 구간과의 겹침만 뺀다(과차감 방지).
  const leaveOverlap = leaveOverlapForRange(
    r,
    Math.max(inM, schStartM),
    Math.min(outM, schEndM)
  );
  return Math.max(0, overlap - schedBreakMin(r) - leaveOverlap);
};
// 분 → "N시간 M분" 표기. null/0 처리.
const fmtDuration = (min) => {
  if (min == null) return "-";
  const m = Math.max(0, Math.round(min));
  const h = Math.floor(m / 60);
  const mm = m % 60;
  if (h > 0 && mm > 0) return `${h}시간 ${mm}분`;
  if (h > 0) return `${h}시간`;
  return `${mm}분`;
};
// 통합 스케줄 셀: 차수에 해당하는 구간(1/2). 초과근무는 '-'.
const scheduleCell = (r) => {
  if (r._isOt) return "-";
  const isSeq2 = String(r.workSeq) === "2";
  return isSeq2
    ? planRange(r.plan2Start, r.plan2End)
    : planRange(r.plan1Start, r.plan1End);
};

// PRAFTA-FIXEDOT-3(표기): 그 행 차수의 경계에 붙는 고정연장 구간 라벨.
//   전방은 1구간(seq1) 행에, 후방은 마지막 스케줄 슬롯(2구간 스케줄이면 seq2, 아니면 seq1) 행에 표시.
//   고정연장 없는 타입/구서버 응답이면 빈 문자열(기존 셀 표기 불변).
const fixedOtCellLabel = (r) => {
  if (r._isOt) return "";
  const isSeq2 = String(r.workSeq) === "2";
  const lastSlotIsSeq2 = !!r.plan2Start;
  const parts = [];
  if (!isSeq2 && r.preFixedOtStrTime && r.preFixedOtEndTime) {
    parts.push(`${fmtHhmm(r.preFixedOtStrTime)}~${fmtHhmm(r.preFixedOtEndTime)}`);
  }
  if (isSeq2 === lastSlotIsSeq2 && r.fixedOtStrTime && r.fixedOtEndTime) {
    parts.push(`${fmtHhmm(r.fixedOtStrTime)}~${fmtHhmm(r.fixedOtEndTime)}`);
  }
  return parts.length ? `+ 고정연장 ${parts.join(" · ")}` : "";
};
// HHMM → "HH:MM" (fixedOtCellLabel 전용 — planRange 내부 포맷과 동일 톤)
const fmtHhmm = (hm) => {
  const s = String(hm ?? "");
  if (s.length !== 4) return "-";
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`;
};

// 행 클래스: 주말 배경 + 선택 행 강조
const rowClass = (r) => {
  const cls = [];
  const s = String(r.workYmd ?? "");
  if (s.length === 8) {
    const day = new Date(
      Number(s.slice(0, 4)),
      Number(s.slice(4, 6)) - 1,
      Number(s.slice(6, 8))
    ).getDay();
    if (day === 0) cls.push("row-sun");
    else if (day === 6) cls.push("row-sat");
  }
  if (selected.value && selected.value._rowKey === r._rowKey) {
    cls.push("row-active");
  }
  // 외근(GPS 존재)이 아니면 상세를 열 수 없으므로 클릭 가능 표시를 끈다.
  if (r.isOutsideYn !== "Y") {
    cls.push("row-no-detail");
  }
  return cls;
};
const statusLabel = (cd) => {
  switch (cd) {
    case "LATE":
      return "지각";
    case "EARLY_LEAVE":
      return "조퇴";
    case "ABSENT":
      return "결근";
    case "NORMAL":
    default:
      return "정상";
  }
};
const statusBadgeClass = (cd) => {
  switch (cd) {
    case "LATE":
      return "b-late";
    case "EARLY_LEAVE":
      return "b-early";
    case "ABSENT":
      return "b-absent";
    case "NORMAL":
    default:
      return "b-normal";
  }
};

// 근무구분 라벨: 정상근무 / 초과근무
// prafta-043: 초과근무 유형(연장/야간/휴일) 전면 파기 — 유형 표기 없이 '초과근무' 단일 표기.
const workTypeLabel = (r) => {
  if (!r || !r._isOt) return "정상근무";
  return "초과근무";
};

// yyyyMMdd 에 일수를 더해 yyyyMMdd 로 반환
const ymdPlusDays = (ymd, days) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return s;
  const d = new Date(
    Number(s.slice(0, 4)),
    Number(s.slice(4, 6)) - 1,
    Number(s.slice(6, 8))
  );
  d.setDate(d.getDate() + days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
};

/**
 * HB-05(D1): 판정용 유효 소정 시각.
 * 서버(Attd08ServiceImpl)가 그날 확정 반차를 반영해 산출한 값(effPlanStart/effPlanEnd)을 쓴다.
 * - 반차가 없으면 그 차수 구간의 원 스케줄 시각과 동일하다.
 * - 구간 전체가 면제(반차 2건으로 종일 면제 등)면 null → 지각·조퇴 판정 제외.
 * - 구버전 응답(필드 없음) 방어: 없으면 원 스케줄 시각으로 폴백한다.
 */
// ⚠️ null(= 구간 전체 면제)과 "필드 자체가 없음"(구버전 응답)을 구분해야 한다.
//    null 을 미전달로 오인해 원 스케줄로 폴백하면 종일 쉰 사람에게 지각·조퇴가 다시 뜬다.
const effPlanStartOf = (r) => {
  if (r && "effPlanStart" in r) return r.effPlanStart || null;
  return (String(r?.workSeq) === "2" ? r?.plan2Start : r?.plan1Start) || null;
};
const effPlanEndOf = (r) => {
  if (r && "effPlanEnd" in r) return r.effPlanEnd || null;
  return (String(r?.workSeq) === "2" ? r?.plan2End : r?.plan1End) || null;
};
// 그 행(차수)의 원 스케줄 시각 — 판정용 시각의 일자 프레임(당일/익일) 판정에만 쓴다.
const rawPlanStartOf = (r) =>
  (String(r?.workSeq) === "2" ? r?.plan2Start : r?.plan1Start) || null;
const rawPlanEndOf = (r) =>
  (String(r?.workSeq) === "2" ? r?.plan2End : r?.plan1End) || null;
/**
 * 판정용 시각이 근무일 당일(0)인지 익일(1)인지 — 원 스케줄 프레임 기준.
 * 야간 스케줄(원 종료 < 원 시작)에서는 스케줄 시작보다 이른 시각이 전부 익일이다.
 * 반차 경계가 자정을 넘기면 유효 소정 시각도 익일 값이 되므로(예: 시작기준 반차 → 판정용 시작 01:15)
 * 유효 시각끼리 비교하는 종전 규칙으로는 "01:15 출근"이 지각으로 오판정된다.
 * 서버 PartialLeaveWindowUtils.dayOffsetOf 와 동일 규칙.
 */
const dayOffsetOf = (rawStart, rawEnd, hhmm) => {
  if (!rawStart || !rawEnd || !hhmm) return 0;
  const s = String(rawStart);
  const e = String(rawEnd);
  if (e >= s) return 0; // 야간 아님(자정종료 "2400" 포함)
  return String(hhmm) < s ? 1 : 0;
};

/**
 * 상태 판정 (지각/조퇴/결근/정상).
 * 백엔드도 동일 규칙으로 판정하지만(attdStatusCd), 화면은 표시 시점 데이터로 일관되게
 * 재판정한다 — 판정 기준 시각만 서버 권위값(effPlan*)을 쓴다.
 */
const computeStatus = (r) => {
  const seq = String(r.workSeq);
  const isSeq2 = seq === "2";
  const inDate = isSeq2 ? r.act2InDate : r.act1InDate;
  const inTime = isSeq2 ? r.act2InTime : r.act1InTime;
  const outDate = isSeq2 ? r.act2OutDate : r.act1OutDate;
  const outTime = isSeq2 ? r.act2OutTime : r.act1OutTime;
  const planStart = effPlanStartOf(r);
  const planEnd = effPlanEndOf(r);
  const rawStart = rawPlanStartOf(r);
  const rawEnd = rawPlanEndOf(r);
  const workYmd = String(r.workYmd ?? "");

  // 출근 기록이 없으면 결근
  if (!inTime) return "ABSENT";

  // 지각: 실제 출근 일시 > 유효 소정 시작 일시(일자 프레임 = 원 스케줄 기준)
  if (planStart) {
    const startYmd =
      dayOffsetOf(rawStart, rawEnd, planStart) === 1
        ? ymdPlusDays(workYmd, 1)
        : workYmd;
    const schStart = startYmd + String(planStart);
    const actIn = (inDate ? String(inDate) : workYmd) + String(inTime);
    if (actIn > schStart) return "LATE";
  }

  // 조퇴: 실제 퇴근 일시 < 유효 소정 종료 일시
  if (planEnd && outTime) {
    const endYmd =
      dayOffsetOf(rawStart, rawEnd, planEnd) === 1
        ? ymdPlusDays(workYmd, 1)
        : workYmd;
    const schEnd = endYmd + String(planEnd);
    const actOut = (outDate ? String(outDate) : workYmd) + String(outTime);
    if (actOut < schEnd) return "EARLY_LEAVE";
  }

  return "NORMAL";
};

// 행 목록에 화면 재판정 상태(_status) 및 표시용 파생필드를 부여.
//   - 정상근무(NORMAL): 차수에 해당하는 구간(1/2)의 실제·정규화 출퇴근을 단일 필드로 정리.
//   - 초과근무(OT): act1In*/act1Out* 를 그대로 사용하고 정규화·상태는 없음.
//   - _rowKey: OT 행은 attdId 가 NULL/중복 가능하므로 otId 기반 고유키를 별도 부여.
const displayRows = computed(() =>
  rows.value.map((r) => {
    if (r.rowType === "OT") {
      // 초과근무 행 — OT 전용 매핑
      return {
        ...r,
        _isOt: true,
        _status: null,
        _inDate: r.act1InDate,
        _inTime: r.act1InTime,
        _outDate: r.act1OutDate,
        _outTime: r.act1OutTime,
        _rowKey: "ot-" + r.otId,
      };
    }
    // 정상근무 행 — 기존 로직 유지
    const isSeq2 = String(r.workSeq) === "2";
    const inDate = isSeq2 ? r.act2InDate : r.act1InDate;
    const outDate = isSeq2 ? r.act2OutDate : r.act1OutDate;
    return {
      ...r,
      _isOt: false,
      _status: computeStatus(r),
      _inDate: inDate,
      _inTime: isSeq2 ? r.act2InTime : r.act1InTime,
      _outDate: outDate,
      _outTime: isSeq2 ? r.act2OutTime : r.act1OutTime,
      _rowKey: "a-" + r.attdId,
    };
  })
);

// ───────────────────────────────────────────────────────────
// 요약 뷰: 날짜 기준 인원별로 차수(1/2)·초과근무를 단일 행으로 합산.
//   - 실근로시간/인정시간: 정규 구간 + 초과근무 모두 합산(분).
//   - 지각/조기퇴근: 정규 구간별 분을 합산(결근/초과 행은 0분).
//   - 상태: 정규 구간 상태 중 우선순위 최상위(결근>지각>조퇴>정상).
// ───────────────────────────────────────────────────────────
// 분 → "전체 N분 (a시간 b분)" 표기 (요약 탭 전용 단위). null/음수는 0 처리.
//   - 60분 이하: 괄호 없이 "전체 N분".
//   - 60분 초과: "전체 N분 (a시간 b분)". 분이 0이면 "(a시간)"만.
const fmtMinutes = (min) => {
  const m = Math.max(0, Math.round(min ?? 0));
  if (m <= 60) return `전체 ${m}분`;
  const h = Math.floor(m / 60);
  const mm = m % 60;
  return `전체 ${m}분 (${h}시간${mm > 0 ? ` ${mm}분` : ""})`;
};
// 지각(분): 실제 출근 − 유효 소정 시작 (양수일 때만). 정규근무 행만.
//   ★ HB-05(D1): 기준 시각은 서버가 내려준 effPlanStart(반차 반영 유효 소정)를 쓴다.
//     반차가 없으면 원 스케줄 시각과 같고, 구간 전체가 면제되면 null 이라 0 분이 된다.
//     클라이언트에서 반차 규칙을 재계산하지 않는다(서버 단일 출처).
const lateMin = (r) => {
  if (r._isOt) return 0;
  const planStart = effPlanStartOf(r);
  if (!planStart || !r._inTime) return 0;
  const baseStartM = dtMinutes(r.workYmd, planStart, r.workYmd);
  const actInM = dtMinutes(r._inDate, r._inTime, r.workYmd);
  if (baseStartM == null || actInM == null) return 0;
  const schStartM =
    baseStartM + dayOffsetOf(rawPlanStartOf(r), rawPlanEndOf(r), planStart) * 1440;
  return Math.max(0, actInM - schStartM);
};
// 조기퇴근(분): 유효 소정 종료 − 실제 퇴근 (양수일 때만). 정규근무 행만.
const earlyLeaveMin = (r) => {
  if (r._isOt) return 0;
  const planEnd = effPlanEndOf(r);
  if (!planEnd || !r._outTime) return 0;
  // 야간 스케줄이면 원 스케줄 프레임으로 익일 여부를 판정한다(유효 시각 간 비교 금지).
  const baseEndM = dtMinutes(r.workYmd, planEnd, r.workYmd);
  const actOutM = dtMinutes(r._outDate, r._outTime, r.workYmd);
  if (baseEndM == null || actOutM == null) return 0;
  const schEndM =
    baseEndM + dayOffsetOf(rawPlanStartOf(r), rawPlanEndOf(r), planEnd) * 1440;
  return Math.max(0, schEndM - actOutM);
};
// 상태 우선순위 — 한 사람 하루에 차수가 여럿이면 단일 상태로 산출.
const STATUS_PRIORITY = { ABSENT: 3, LATE: 2, EARLY_LEAVE: 1, NORMAL: 0 };

const summaryRows = computed(() => {
  const map = new Map();
  for (const r of displayRows.value) {
    const key = (r.userId ?? r.userCd ?? r.userNm) + "_" + r.workYmd;
    let g = map.get(key);
    if (!g) {
      g = {
        _key: key,
        userNm: r.userNm,
        nodeNm: r.nodeNm,
        workYmd: r.workYmd,
        workedMin: 0,
        recognizedMin: 0,
        fixedOtMin: 0,
        lateMin: 0,
        earlyMin: 0,
        _statusPriority: -1,
        _status: null,
      };
      map.set(key, g);
    }
    const wn = workedNetMin(r);
    if (wn != null) g.workedMin += wn;
    const rn = recognizedMin(r);
    if (rn != null) g.recognizedMin += rn;
    // PRAFTA-FIXEDOT-3: 고정연장 실적은 일 단위 값이 마지막 슬롯 행에만 실리므로 그대로 합산해도 중복 없음.
    if (r.fixedOtActMinutes != null) g.fixedOtMin += r.fixedOtActMinutes;
    g.lateMin += lateMin(r);
    g.earlyMin += earlyLeaveMin(r);
    if (!r._isOt && r._status) {
      const p = STATUS_PRIORITY[r._status] ?? -1;
      if (p > g._statusPriority) {
        g._statusPriority = p;
        g._status = r._status;
      }
    }
  }
  // 근무일 오름차순 → 사용자명 가나다순
  return Array.from(map.values()).sort((a, b) => {
    const d = String(a.workYmd).localeCompare(String(b.workYmd));
    if (d !== 0) return d;
    return String(a.userNm).localeCompare(String(b.userNm), "ko");
  });
});

// 요약 행 주말 배경 클래스 (전체 뷰 rowClass 의 주말 로직과 동일)
const summaryRowClass = (s) => {
  const cls = [];
  const ss = String(s.workYmd ?? "");
  if (ss.length === 8) {
    const day = new Date(
      Number(ss.slice(0, 4)),
      Number(ss.slice(4, 6)) - 1,
      Number(ss.slice(6, 8))
    ).getDay();
    if (day === 0) cls.push("row-sun");
    else if (day === 6) cls.push("row-sat");
  }
  return cls;
};

// 상세 패널 + 지도
const mapContainer = ref(null);
const gpsList = ref([]);
const gpsLoading = ref(false);
// gpsInfoType 이 '01'(출근)/'02'(퇴근) 인 유효 좌표만 추린 목록.
//   지도 표시 대상이며, 빈-상태 안내·요약 건수의 기준이 된다.
//   gpsInfoType 이 NULL 등으로 유효하지 않은 좌표는 지도에 찍지 않으므로
//   여기서도 제외해, 그런 좌표만 있을 때 빈 캔버스 대신 안내 문구가 노출되게 한다.
const validGpsList = computed(() =>
  gpsList.value.filter((g) => g.gpsInfoType === "01" || g.gpsInfoType === "02")
);
const mockedCount = computed(
  () => validGpsList.value.filter((g) => g.isMocked === "Y").length
);

// 출근('01')/퇴근('02') 좌표 건수 — 필터 버튼 비활성화 판단용
const gpsStartCount = computed(
  () => validGpsList.value.filter((g) => g.gpsInfoType === "01").length
);
const gpsEndCount = computed(
  () => validGpsList.value.filter((g) => g.gpsInfoType === "02").length
);
// 지도 표시 필터 — 'all'=전체 / '01'=출근만 / '02'=퇴근만
const gpsViewMode = ref("all");

let kakaoMap = null;
let kakaoMarkers = [];
let kakaoPolyline = null;

const fnSelectRow = async (r) => {
  // GPS 동선을 표시할 수 있는 외근 행(GPS 기록 존재)만 상세 패널을 연다.
  if (r.isOutsideYn !== "Y") return;

  selected.value = r;
  // 지도/GPS 초기화
  cleanupMap();
  gpsList.value = [];
  gpsViewMode.value = "all";

  gpsLoading.value = true;
  try {
    const response = await axios.get("/webApi/attd08/attd-gps-trail", {
      params: { attdId: r.attdId },
    });
    if (response.status === 200) {
      gpsList.value = response.data?.attdGpsTrailResultList ?? [];
    }
  } catch (err) {
    console.error("[Attd_08] gps trail load failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
    gpsList.value = [];
  } finally {
    gpsLoading.value = false;
  }

  if (gpsList.value.length === 0) return;

  await nextTick();
  await renderMap();
};

const fnCloseDetail = () => {
  selected.value = null;
  cleanupMap();
  gpsList.value = [];
};

// Kakao Map loader (SiteInfoPop 패턴 차용)
const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }
    const existingScript = document.querySelector(
      'script[src*="dapi.kakao.com"]'
    );
    if (existingScript) {
      const checkInterval = setInterval(() => {
        if (window.kakao && window.kakao.maps) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);
      setTimeout(() => {
        clearInterval(checkInterval);
        if (!window.kakao || !window.kakao.maps) {
          reject(new Error("카카오 지도 API 로드 타임아웃"));
        }
      }, 10000);
      return;
    }
    const kakaoKey = import.meta.env.VITE_PUBLIC_KAKAO_APP_JS_KEY;
    if (!kakaoKey) {
      reject(new Error("카카오 지도 API 키가 없습니다."));
      return;
    }
    const scriptUrl = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&libraries=services&autoload=false`;
    const script = document.createElement("script");
    script.src = scriptUrl;
    script.async = true;
    script.onload = () => {
      if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(() => resolve());
      } else {
        reject(new Error("카카오 지도 API 객체를 찾을 수 없습니다."));
      }
    };
    script.onerror = () => {
      reject(new Error("카카오 지도 API 로드 실패."));
    };
    document.head.appendChild(script);
  });
};

const renderMap = async () => {
  if (!mapContainer.value) return;
  try {
    await loadKakaoMapScript();
  } catch (e) {
    console.error("[Attd_08] kakao load fail:", e);
    return;
  }

  // gpsInfoType 이 '01'(출근)/'02'(퇴근) 인 좌표만 표시한다.
  //   그 외/NULL 좌표는 지도에 표시하지 않는다.
  //   '01' 이 '02' 보다 앞에 오도록 정렬한다 (백엔드 ORDER BY 와 동일 보장).
  const points = gpsList.value
    .map((g) => {
      const lat = Number(g.lat);
      const lon = Number(g.lon);
      if (isNaN(lat) || isNaN(lon)) return null;
      const gpsInfoType = g.gpsInfoType;
      if (gpsInfoType !== "01" && gpsInfoType !== "02") return null;
      return { lat, lon, gpsInfoType, isMocked: g.isMocked === "Y", raw: g };
    })
    .filter(Boolean)
    .sort((a, b) => a.gpsInfoType.localeCompare(b.gpsInfoType));

  if (points.length === 0) return;

  // gpsViewMode 필터 — '01'(출근)/'02'(퇴근) 단독 보기 시 해당 좌표만 표시한다.
  // (출근·퇴근 좌표가 동일해 마커가 겹쳐 가려질 때 개별 확인용)
  const visible =
    gpsViewMode.value === "all"
      ? points
      : points.filter((p) => p.gpsInfoType === gpsViewMode.value);
  if (visible.length === 0) return;

  // 지도 생성 (첫 좌표 중심)
  const center = new window.kakao.maps.LatLng(visible[0].lat, visible[0].lon);
  kakaoMap = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 4,
  });

  // 마커: 출근('01')=초록, 퇴근('02')=빨강. (AttdGpsCoordPanel 과 색/SVG 패턴 일치)
  const buildPinImage = (color) =>
    new window.kakao.maps.MarkerImage(
      "data:image/svg+xml;base64," +
        btoa(
          `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" fill="${color}"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>`
        ),
      new window.kakao.maps.Size(24, 32),
      { offset: new window.kakao.maps.Point(12, 32) }
    );
  const startImage = buildPinImage("#16a34a"); // 출근
  const endImage = buildPinImage("#ef4444"); // 퇴근

  const bounds = new window.kakao.maps.LatLngBounds();
  const path = [];
  for (const p of visible) {
    const pos = new window.kakao.maps.LatLng(p.lat, p.lon);

    // gpsInfoType 으로 출근('01')/퇴근('02') 구분
    const isStart = p.gpsInfoType === "01";

    const marker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position: pos,
      image: isStart ? startImage : endImage,
    });
    kakaoMarkers.push(marker);

    // 출근/퇴근 라벨 (CustomOverlay)
    const label = new window.kakao.maps.CustomOverlay({
      map: kakaoMap,
      position: pos,
      yAnchor: 2.2,
      content: `<div class="gps-pin-label ${
        isStart ? "is-start" : "is-end"
      }">${isStart ? "출근" : "퇴근"}</div>`,
    });
    kakaoMarkers.push(label);

    path.push(pos);
    bounds.extend(pos);
  }

  // 출근·퇴근 2점 연결 폴리라인
  if (path.length >= 2) {
    kakaoPolyline = new window.kakao.maps.Polyline({
      path,
      strokeWeight: 3,
      strokeColor: "#16a34a",
      strokeOpacity: 0.8,
      strokeStyle: "solid",
    });
    kakaoPolyline.setMap(kakaoMap);
  }

  if (path.length === 1) {
    kakaoMap.setCenter(path[0]);
    kakaoMap.setLevel(4);
  } else {
    kakaoMap.setBounds(bounds);
  }
};

const cleanupMap = () => {
  if (kakaoPolyline) {
    try {
      kakaoPolyline.setMap(null);
    } catch (_e) {
      void 0;
    }
    kakaoPolyline = null;
  }
  for (const m of kakaoMarkers) {
    try {
      m.setMap(null);
    } catch (_e) {
      void 0;
    }
  }
  kakaoMarkers = [];
  kakaoMap = null;
};

// 출근/퇴근/전체 필터 버튼 — 선택한 모드로 지도를 다시 그린다.
const setGpsViewMode = async (mode) => {
  if (gpsViewMode.value === mode) return;
  gpsViewMode.value = mode;
  cleanupMap();
  if (gpsLoading.value || validGpsList.value.length === 0) return;
  await nextTick();
  await renderMap();
};

// 진입 시 sessionStorage 의 사업장 정보로 초기화 (Attd_05 패턴 차용)
//   ★ security H-1(2026-08-07): 서버에 부서 관리 권한 게이트(canManageNode)가 추가되어,
//     master/hr 이 아닌 사용자는 부서(nodeCd)를 지정하지 않으면 403 을 맞는다.
//     Attd_11(fnInit :507~516)·Attd_13 과 동일하게 세션 소속부서를 프리필해 진입 즉시 403 을 막는다.
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

// ── 대시보드 조회조건 주입 (PRAFTA-DASHBOARD-T1) ──────────────
// 대시보드(Dashboard_01)에서 넘어온 조회조건이 있으면 반영한다 (없으면 no-op).
// 기준월(ym)은 본 화면의 일자 기간(fromDate/toDate)으로 변환한다. 반영 여부를 반환한다.
const applyDashboardParams = () => {
  const p = dashNav.consumeParams("Attd_08");
  if (!p) return false;
  siteCd.value = p.siteCd ?? "";
  siteNo.value = p.siteNo ?? "";
  siteNm.value = p.siteNm ?? "";
  nodeDisabled.value = proxy.$util.isEmpty(siteCd.value);
  nodeCd.value = p.nodeCd ?? "";
  nodeNm.value = p.nodeNm ?? "";
  incSubNodeYn.value = !!p.incSubNodeYn;
  const range = ymToDateRange(p.ym);
  if (range) {
    fromDate.value = range.fromDate;
    toDate.value = range.toDate;
  }
  return true;
};

// 본 화면 fnSearch 는 사업장 필수 — 사업장이 있을 때만 자동 재조회한다.
//   security H-1: 비 master/hr 은 부서까지 있어야 서버 게이트를 통과하므로, 프리필 대상이 없으면
//   자동조회를 건너뛴다(불필요한 403 alert 방지 — 사용자가 부서를 고르고 직접 조회).
const fnSearchByDashboard = () => {
  if (!applyDashboardParams() || !siteCd.value) return;
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) return;
  fnSearch();
};

onMounted(() => {
  fnInit();
  fnSearchByDashboard();
});

// keep-alive 로 이미 열린 탭에 재진입하는 경우 대응
onActivated(() => {
  fnSearchByDashboard();
});

onBeforeUnmount(() => {
  cleanupMap();
});
</script>

<style scoped>
/* viewBody 위에 덮어쓰는 레이아웃 (Attd_05 패턴 차용).
   - flex 컨테이너 + min-height:0 으로 내부 wrap 이 100% 높이를 갖도록 함
   - height: calc 제거 → 하단 빈 공간/끊김 현상 해소 */
.a08-body {
  display: flex;
  flex-direction: row;
  gap: 1rem;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a08-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: #fff;
}

/* 휴게 자동차감 안내 문구 — 툴바 내 전체/요약 토글 우측에 배치. 남은 폭을 채우며
   길면 줄바꿈된다(min-width:0 으로 flex 축소 허용). */
.a08-note {
  flex: 1 1 auto;
  min-width: 0;
  margin: 0;
  margin-inline-start: 0.6rem;
  padding: 0;
  font-size: 0.75rem;
  line-height: 1.35;
  color: var(--color-text-muted, #6b7280);
}
.a08-note b {
  color: var(--color-text, #374151);
  font-weight: 600;
}
.a08-body.detail-open .a08-table-wrap {
  flex: 1 1 60%;
}

/* 소제목 + 본문(전체/요약)을 감싸는 subtitle-pane 래퍼: flex 컬럼 레이아웃에서
   남은 높이만 차지하고 내부 테이블만 스크롤되도록 한다. 이 제약이 없으면 조회 후
   행이 늘어날 때 래퍼가 테이블 전체 높이로 커져 viewComm 을 밀어내고,
   전체/요약 토글·소제목이 위로 밀려 잘린다 (Attd_11 패턴 차용). */
.table-wrapper.subtitle-pane {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* 기간 + 뷰 전환 툴바 (Attd_07 .a07-toolbar 패턴 차용): From/To 픽커(< >)·전체/요약 토글·안내문구.
   조회영역(.viewSearch) 과 동일한 바(배경+하단 구분선) 형태로 만들어, 조회영역이 아닌
   테이블 영역의 헤더로 명확히 읽히도록 한다. */
.a08-toolbar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
  flex-wrap: wrap;
  font-family: "Pretendard", sans-serif;
}
/* 캘린더 + 전일/익일(< >) 이동 버튼 묶음 */
.a08-date-nav {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
/* 캘린더 입력 형태를 Attd_07 월 픽커(.a07-nav-month-picker)와 동일하게.
   포맷은 컴포넌트가 Y-m-d 로 유지하므로 날짜는 yyyy-mm-dd 로 표시된다. */
.a08-date-nav :deep(.calendar-input) {
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
  min-width: 120px;
  font-family: "Pretendard", sans-serif;
}
.a08-date-nav :deep(.calendar-input:hover) {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
}
.a08-date-arr {
  width: 24px;
  height: 24px;
  border: 1px solid var(--color-border, #d1d5db);
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.875rem;
  line-height: 1;
  flex: 0 0 auto;
}
.a08-date-arr:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}

.a08-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a08-table thead th {
  background: var(--thead-bg, #f3f4f6);
  /* 헤더/본문 모두 사방 1px 테두리로 통일 (.data-grid 표준 패턴) */
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  box-sizing: border-box;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text, #374151);
  font-weight: 600;
}
/* 2단 헤더 sticky: 1행은 상단, 2행은 1행 높이만큼 아래에 고정.
   top 오프셋을 rem 값으로 고정 추정하면 폰트/브라우저 렌더링 차이로 1행의
   실제 높이와 어긋나 헤더 경계 틈으로 스크롤된 본문 행이 비치는 문제가 있었다.
   대신 JS(ResizeObserver)로 1행의 실제 렌더 높이를 측정해 --a08-thead1-h 로
   주입하고, 2행은 그 값을 top 으로 그대로 사용해 항상 정확히 맞물리게 한다. */
.a08-table thead tr:first-child th {
  position: sticky;
  top: 0;
}
.a08-table thead tr:last-child th {
  position: sticky;
  top: var(--a08-thead1-h, 2.3rem);
}
.a08-table tbody td {
  /* 컬럼 사이 세로선이 보이도록 사방 1px 테두리 (.data-grid 표준 패턴) */
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text, #374151);
}
.a08-table tbody tr {
  cursor: pointer;
}
/* 외근이 아니어서 상세를 열 수 없는 행 — 클릭 커서 제거 */
.a08-table tbody tr.row-no-detail {
  cursor: default;
}
/* 상세 버튼이 없는 행의 자리 표시 */
.a08-no-detail {
  color: #9ca3af;
}
/* 주말 행 배경 (요일 컬럼 대체) */
.a08-table tbody tr.row-sun {
  background: #fef2f2;
}
.a08-table tbody tr.row-sat {
  background: #eff6ff;
}
.a08-table tbody tr:hover {
  background: #f9fafb;
}
.a08-table tbody tr.row-active {
  background: #eef2ff;
}
.a08-empty {
  padding: 2rem !important;
  color: #9ca3af;
  text-align: center;
}

.a08-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.b-normal {
  background: #d1fae5;
  color: #065f46;
}
.b-late {
  background: #fef3c7;
  color: #92400e;
}
.b-early {
  background: #fde68a;
  color: #92400e;
}
.b-absent {
  background: #fee2e2;
  color: #991b1b;
}
.b-out {
  background: #dbeafe;
  color: #1e40af;
}
.b-in {
  background: #f3f4f6;
  color: #374151;
}
/* PRAFTA-FIXEDOT-3: "연장 미이행" 배지 — 조퇴(b-early)와 시각적으로도 구분(경고 토큰, 하드코딩 금지) */
.b-fixedot-unmet {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  margin-left: 0.25rem;
}
/* PRAFTA-FIXEDOT-3: 스케줄 셀 고정연장 구분 표기(보조 라벨) */
.a08-fixedot-label {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}
/* 근무구분 배지 (PRAFTA-015) — 정상근무/초과근무 구분 */
.b-work-normal {
  background: #e0e7ff;
  color: #3730a3;
}
.b-ot {
  background: #ffedd5;
  color: #9a3412;
}

.a08-btn-detail {
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
}
.a08-btn-detail:hover {
  background: #f3f4f6;
}

.a08-detail-panel {
  flex: 0 0 36rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.a08-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
}
.a08-detail-title {
  font-weight: 600;
  font-size: 1rem;
}
.a08-detail-sub {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 0.2rem;
}
.a08-detail-close {
  background: transparent;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  color: #6b7280;
  line-height: 1;
}
.a08-detail-meta {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}
.meta-row {
  display: flex;
  align-items: center;
  font-size: 0.85rem;
  padding: 0.2rem 0;
}
.meta-label {
  flex: 0 0 8rem;
  color: #6b7280;
}
.meta-value {
  flex: 1;
}

.a08-map-section {
  padding: 0.75rem 1rem;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 18rem;
}
.a08-map-title {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}
.a08-map-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 16rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}
.a08-map-empty {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 0.85rem;
  background: #f9fafb;
  border: 1px dashed #e5e7eb;
  border-radius: 4px;
  min-height: 16rem;
}
.a08-gps-summary {
  margin-top: 0.4rem;
  font-size: 0.8rem;
  color: #4b5563;
}
.mocked-warn {
  color: #b91c1c;
  font-weight: 600;
  margin-left: 0.4rem;
}

/* 출근/퇴근/전체 필터 버튼 */
.a08-gps-filter {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}
.a08-gps-filter-btn {
  flex: 0 0 auto;
  padding: 0.25rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  cursor: pointer;
}
.a08-gps-filter-btn:hover:not(:disabled):not(.is-active) {
  border-color: var(--color-border-strong);
}
.a08-gps-filter-btn.is-active {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.a08-gps-filter-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 출근/퇴근 핀 라벨 — CustomOverlay content 는 .a08-map-canvas(scoped 데이터
   속성 보유) 하위에 삽입되므로 :deep() 로 자식 셀렉터를 관통시켜 스타일을 적용한다.
   (AttdGpsCoordPanel 의 gps-pin-label 패턴과 동일) */
:deep(.gps-pin-label) {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-surface);
  white-space: nowrap;
}
:deep(.gps-pin-label.is-start) {
  background: var(--color-primary);
}
:deep(.gps-pin-label.is-end) {
  background: var(--color-danger);
}

/* Attd_07 패턴: viewSearch 의 div 간 gap(2rem)을 일부 상쇄해
   소속부서 입력 뭉치와 가깝게 붙여 보이게 한다. */
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

/* 뷰 전환 토글 (전체/요약) — Attd_07 세그먼트 컨트롤과 동일 형태 */
.a08-view-toggle {
  display: inline-flex;
  /* 툴바 flex 에서 압축되어 버튼이 세로로 잘리지 않도록 자연 높이 고정 */
  flex: 0 0 auto;
  align-self: center;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  overflow: hidden;
  /* 기간 픽커 그룹과 살짝 띄운다 */
  margin-inline-start: 0.6rem;
}
.a08-view-btn {
  background: none;
  border: none;
  padding: 0.3rem 0.85rem;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}
.a08-view-btn.active {
  background: var(--color-primary, #16a34a);
  color: #fff;
  font-weight: 600;
}

/* 요약 본문 — 단일 테이블 (상세 패널 없음) */
.a08-summary-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
/* 요약 테이블은 단일 헤더 행 → 2단 헤더용 sticky 오프셋(2.3rem)을 0으로 되돌린다 */
.a08-summary-table thead tr:last-child th {
  top: 0;
}
</style>
