<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
      @delete="fnDelete"
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
          ref="nodeCdFcs"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpenForCondition()"
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
        <label>조회월</label>
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="workYm"
        />
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 적용 툴바 -->
    <div class="attd05-toolbar">
      <!-- ── 근무 타입 적용 섹션 ─────────────────────────── -->
      <span class="toolbar-label">근무 타입</span>
      <select v-model="selectedSchType" class="toolbar-sch-select">
        <option value="">스케줄 타입 선택</option>
        <option v-for="sch in schTypeList" :key="sch.schCd" :value="sch.schCd">
          {{ sch.schNm }}
        </option>
      </select>
      <div
        class="toolbar-selection-box"
        :class="{ 'has-value': !!selectionLabel }"
      >
        {{ selectionLabel || "" }}
      </div>
      <div class="toolbar-radio-wrap">
        <label class="radio-item">
          <input type="radio" v-model="schHolidayMode" value="exclude" />
          <span>휴일 제외</span>
        </label>
        <label class="radio-item">
          <input type="radio" v-model="schHolidayMode" value="include" />
          <span>휴일 포함</span>
        </label>
      </div>
      <button
        class="btn-toolbar-apply"
        :disabled="isMonthClosed"
        @click="fnApplySchType"
      >
        적용
      </button>
      <span class="toolbar-count-label" :class="{ invisible: !selectionLabel }">
        선택: {{ selectionLabel || "–" }} &middot; {{ selectionCount }}건
      </span>

      <!-- ── 구분선 ─────────────────────────────────────── -->
      <div class="toolbar-divider"></div>

      <!-- ── 법정 연차 적용 섹션 — 타입 선택 없이 본연차(SYS_ANNUAL) 고정 ── -->
      <span class="toolbar-label toolbar-label-leave">법정 연차</span>
      <div
        class="toolbar-fixed-leave"
        :class="{ 'is-missing': !annualLeaveType }"
        :title="
          annualLeaveType
            ? '법정 본연차로 고정 적용됩니다.'
            : '법정 연차(SYS_ANNUAL)가 설정되어 있지 않습니다.'
        "
      >
        {{ annualLeaveType ? annualLeaveType.leaveNm : "연차 미설정" }}
      </div>
      <div
        class="toolbar-selection-box toolbar-selection-box-leave"
        :class="{ 'has-value': !!selectionLabel }"
      >
        {{ selectionLabel || "" }}
      </div>
      <div class="toolbar-radio-wrap">
        <label class="radio-item radio-item-leave">
          <input type="radio" v-model="leaveHolidayMode" value="exclude" />
          <span>휴일 제외</span>
        </label>
        <label class="radio-item radio-item-leave">
          <input type="radio" v-model="leaveHolidayMode" value="include" />
          <span>휴일 포함</span>
        </label>
      </div>
      <button
        class="btn-toolbar-apply btn-toolbar-apply-leave"
        @click="fnApplyLeaveType"
      >
        적용
      </button>
      <span class="toolbar-count-label" :class="{ invisible: !selectionLabel }">
        선택: {{ selectionLabel || "–" }} &middot; {{ selectionCount }}건
      </span>
      <!-- ── 구분선 ─────────────────────────────────────── -->
      <div class="toolbar-divider"></div>

      <!-- ── 셀 비우기 (지우기) ─────────────────────────────── -->
      <button
        class="btn-toolbar-clear"
        :disabled="isMonthClosed"
        @click="fnClearCells"
      >
        지우기
      </button>

      <div class="toolbar-spacer"></div>
      <button class="btn-toolbar-upload" @click="fnUploadExcel">
        엑셀 업로드
      </button>
    </div>

    <!-- 테이블 영역 -->
    <div class="viewBody attd05-body">
      <div class="attd05-table-outer" @mouseleave="onTableMouseLeave">
        <table class="attd05-table" @selectstart.prevent>
          <thead>
            <tr>
              <th class="th-seq sticky-col-seq sticky-top">No</th>
              <th class="th-chk sticky-col-chk sticky-top">
                <input type="checkbox" v-model="allChecked" />
              </th>
              <ThSortable
                label="사용자 정보"
                col-key="userNm"
                :sort-key="sortKey"
                :sort-order="sortOrder"
                :width="colWidths.userInfo"
                class="th-user-info sticky-col-info sticky-top"
                @sort="onSort"
                @update:width="onResize"
              />
              <th
                v-for="d in daysInMonth"
                :key="d.workYmd"
                class="th-day sticky-top"
                :class="{
                  'head-sun': d.dow === '일',
                  'head-sat': d.dow === '토',
                  'head-holiday':
                    d.holidayYn !== 'N' && d.dow !== '일' && d.dow !== '토',
                }"
              >
                {{ parseInt(d.workYmd.slice(6)) }}({{ d.dow }})
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(user, rowIdx) in sortedUserList" :key="user.userCd">
              <td class="td-seq sticky-col-seq">{{ rowIdx + 1 }}</td>
              <td class="td-chk sticky-col-chk">
                <input
                  type="checkbox"
                  v-model="checkedRows"
                  :value="user.userCd"
                />
              </td>
              <td class="td-user-info sticky-col-info">
                <div class="user-cell-inner">
                  <span class="row-badge">{{ getRowLabel(rowIdx) }}</span>
                  <div class="user-text">
                    <div class="u-name">
                      {{ user.userNm }}({{ user.userId }})
                    </div>
                    <div class="u-dept">
                      {{ user.nodeNm }} / {{ user.shiftNm || "-" }}
                    </div>
                    <div class="u-phone">
                      {{ proxy.$util.formatPhoneNumber(user.mblNo) }}
                    </div>
                  </div>
                </div>
              </td>
              <td
                v-for="d in daysInMonth"
                :key="d.workYmd"
                class="td-day"
                :class="[
                  d.dow === '일' ? 'td-sun' : '',
                  d.dow === '토' ? 'td-sat' : '',
                  d.holidayYn !== 'N' && d.dow !== '일' && d.dow !== '토'
                    ? 'td-holiday'
                    : '',
                  isCellSelected(rowIdx, d.workYmd) ? 'td-selected' : '',
                  isLeaveCell(user.userCd, d.workYmd) ? 'td-leave' : '',
                  isShiftLockedCell(user.userCd, d.workYmd) ? 'is-shift-locked' : '',
                  ...getSelEdgeClasses(rowIdx, d.workYmd),
                ]"
                :title="
                  isShiftLockedCell(user.userCd, d.workYmd)
                    ? '교대근무팀 소속 기간입니다. 교대패턴 자동 생성으로만 설정되며, 연차는 사용할 수 있습니다.'
                    : isLeaveCell(user.userCd, d.workYmd)
                    ? '연차 등록일입니다. 더블클릭하면 연차 변경/삭제 요청을 할 수 있습니다.'
                    : null
                "
                @mousedown.prevent="onCellDown(rowIdx, d.workYmd)"
                @mousemove="onCellMove(rowIdx, d.workYmd)"
                @mouseup="onCellUp"
                @dblclick="fnOpenLeaveChangeRequest(rowIdx, d.workYmd)"
              >
                <span
                  class="td-val"
                  :class="{
                    'val-muted':
                      (d.weekendYn || d.holidayYn) &&
                      !getCellNmValue(user.userCd, d.workYmd),
                  }"
                >
                  {{
                    getCellNmValue(user.userCd, d.workYmd) ||
                    (d.weekendYn || d.holidayYn ? "-" : "")
                  }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  onUnmounted,
  defineProps,
  getCurrentInstance,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import ExcelUploadPop from "@/views/attd/popup/ExcelUploadPop.vue";
import LeaveChangeRequestPop from "@/views/attd/popup/LeaveChangeRequestPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";
defineOptions({ name: "Attd_05" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

const localButtons = ref({ ...props.buttons });

// ── 조회 조건 ─────────────────────────────────────────────
const now = new Date();
const workYm = ref(
  `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`
);
const searchUserNm = ref("");

// ── 사업장 / 소속부서 ──────────────────────────────────────
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
// PRAFTA-028 - 조회한 월(스코프)이 마감되었는지 (마감 시 근무타입 변경/저장/삭제 차단)
const isMonthClosed = ref(false);

// ── 포커싱 변수 목록 ───────────────────────────────────────
const siteNoFcs = ref(null);
const nodeCdFcs = ref(null);

// ── 스케줄 타입 목록 ───────────────────────────────────────
const schTypeList = ref([]);

// ── 근무타입(SCH_CD)별 검증 메타 ────────────────────────────
// key = schCd, value = { createDt, versionList:[{ applyDate, useYn, histIdx }] }
const schTypeValidMetaMap = ref({});

// ── 연차 타입 목록 ───────────────────────────────────────
const leaveTypeList = ref([]);

// ── 사용자 목록 ────────────────────────────────────────────
const userList = ref([]);
const {
  sortKey,
  sortOrder,
  sortedData: sortedUserList,
  onSort,
} = useTableSort(userList);
const { colWidths, onResize } = useColumnResize({ userInfo: 210 });

// ── 셀 데이터: key = `${userCd}_${day}`, value = 표시문자열 ─
const scheduleData = ref({});

// prafta-com-008-E-6: 연차 오버레이. key = `${userCd}_${workYmd}`, value = { leaveCd, leaveId }(종일 CONFIRMED).
//   work_plan(SCH_CD) 위에 "연차" 표시를 덮는 단일 출처(모델 전환). 조회 응답으로만 갱신(저장 미관여).
//   prafta-com-008-B-7(D-E8): 연차 셀 개별 동의요청(attd13 DELETE) 진입을 위해 leaveId 를 함께 보관한다.
const leaveOverlay = ref({});

// prafta-com-008-D-5: 교대 잠금 오버레이. key = `${userCd}_${workYmd}` 가 true 면 교대팀 소속 구간(SCH 잠금).
//   교대 소속 구간의 일반 근무(SCH) 셀은 비활성/자물쇠 표시하고, 적용/비우기 대상에서 제외한다.
//   연차 셀은 잠금 무관 활성(D-3 — 연차만 허용). 저장 차단의 최종 강제는 BE 가드(D-3, ATTD_400_160).
const shiftLockOverlay = ref({});

// ── baseline 스냅샷 (조회 직후 깊은 복사 — dirty 비교 기준) (PRAFTA-041) ──
// 저장 시 scheduleData 와 비교해 실제 변경된 셀만 업서트/삭제로 분리 전송한다.
const scheduleBaseline = ref({});

// ── 적용 옵션 (근무 타입) ──────────────────────────────────
const selectedSchType = ref("");
const schHolidayMode = ref("exclude");

// ── 적용 옵션 (법정 연차) ──────────────────────────────────
// 연차 타입 선택 없이 법정 본연차(SYS_ANNUAL)만 고정 적용한다.
// 그 외 연차(월차/근속가산/약정 등)는 사용자 직접 신청·사후 요청 경로로 처리하며 이 화면 대상이 아니다.
const ANNUAL_LEAVE_CD = "SYS_ANNUAL";
const annualLeaveType = computed(
  () => leaveTypeList.value.find((t) => t.leaveCd === ANNUAL_LEAVE_CD) ?? null
);
const leaveHolidayMode = ref("exclude");

// ── 행 체크박스 ───────────────────────────────────────────
const checkedRows = ref([]);
const allChecked = computed({
  get: () =>
    userList.value.length > 0 &&
    checkedRows.value.length === userList.value.length,
  set: (val) => {
    checkedRows.value = val ? userList.value.map((u) => u.userCd) : [];
  },
});

// ── 드래그 선택 상태 ──────────────────────────────────────
const isDragging = ref(false);
const dragStart = ref(null); // { rowIdx, workYmd }
const dragEnd = ref(null);

// ── 버튼 컨트롤 ────────────────────────────────────────────
const fnButtonControll = () => {
  // localButtons.value.search = "Y";
  localButtons.value.create = "N";
  // localButtons.value.save = "N";
  // localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// ── 해당 월 날짜 목록 (fnSearch 호출 후 서버 응답으로 세팅) ─
const daysInMonth = ref([]);

// ── 행 레이블 (A, B, C … Z, AA, AB …) ─────────────────────
const getRowLabel = (idx) => {
  if (idx < 26) return String.fromCharCode(65 + idx);
  return (
    String.fromCharCode(64 + Math.floor(idx / 26)) +
    String.fromCharCode(65 + (idx % 26))
  );
};

// ── 셀 값 조회 (표시명 변환) ──────────────────────────
// prafta-com-008-E-6: 연차-스케줄 모델 전환 — work_plan 에는 SCH_CD 가 유지되므로,
//   "연차" 표시는 leave_use 오버레이(leaveOverlay)를 work_plan 표시보다 우선한다(단일 출처).
const getCellNmValue = (userCd, workYmd) => {
  // 1) 연차 오버레이 우선(종일 CONFIRMED). work_plan 의 SCH_CD 와 무관하게 "연차"로 렌더.
  const overlay = leaveOverlay.value[`${userCd}_${workYmd}`];
  const leaveCd = overlay?.leaveCd;
  if (leaveCd) {
    const leave = leaveTypeList.value.find((l) => l.leaveCd === leaveCd);
    return leave ? leave.leaveNm : leaveCd;
  }

  // 2) 오버레이 없으면 work_plan 코드(SCH_CD, 레거시 LEAVE_CD 모두 대응).
  const code = scheduleData.value[`${userCd}_${workYmd}`];
  if (!code) return "";

  const sch = schTypeList.value.find((s) => s.schCd === code);
  if (sch) return sch.schNm;

  const leave = leaveTypeList.value.find((l) => l.leaveCd === code);
  if (leave) return leave.leaveNm;

  return code;
};

// ── 근무타입 표시명 조회 ───────────────────────────────────
const getSchTypeNm = (schCd) => {
  const sch = schTypeList.value.find((s) => s.schCd === schCd);
  return sch ? sch.schNm : schCd;
};

// ── 연차 셀 여부(오버레이 보유) ────────────────────────────
// prafta-com-008-B-7(D-E8): 셀에 종일 연차(leave_use CONFIRMED)가 덮여 있는지.
//   true 인 셀은 "비우기" 대상이 아니라 동의요청(이동/삭제) 진입 대상이다.
const isLeaveCell = (userCd, workYmd) =>
  !!leaveOverlay.value[`${userCd}_${workYmd}`]?.leaveId;

// ── 교대 잠금 셀 여부 (prafta-com-008-D-5) ────────────────────
//   교대팀 소속 구간(BE shiftLockOverlay)인 셀. true 면 SCH 변경/비우기 비활성(자물쇠).
//   단 연차 셀(isLeaveCell)은 잠금과 무관하게 활성 — 연차만 허용(D-3). 따라서 "연차가 아닌데 교대 잠금"일 때만 잠금 표시.
const isShiftLockedCell = (userCd, workYmd) =>
  !!shiftLockOverlay.value[`${userCd}_${workYmd}`] && !isLeaveCell(userCd, workYmd);

// ── 연차 셀 더블클릭 → 연차 변경/삭제 동의요청 팝업(기존 LeaveChangeRequestPop 재사용) ──
// prafta-com-008-B-7(D-E8): 연차 등록일은 "비우기"로 삭제할 수 없고, 셀 개별 진입으로
//   attd13 동의요청(POST /webApi/attd13/change-requests)을 발의한다(이동/삭제는 팝업 내 선택).
//   - 신규 팝업/동의 로직 작성 금지: 기존 LeaveChangeRequestPop(DELETE 지원) 을 그대로 연다.
//   - TARGET_LEAVE_ID = 해당 셀 오버레이의 leaveId(B-5 응답으로 동반된 LEAVE_ID).
//   - 드래그 선택(mousedown/up)과 충돌하지 않도록 더블클릭으로 진입한다.
const fnOpenLeaveChangeRequest = (rowIdx, workYmd) => {
  const user = sortedUserList.value[rowIdx];
  if (!user) return;
  const overlay = leaveOverlay.value[`${user.userCd}_${workYmd}`];
  // 연차 오버레이(leaveId) 없는 셀은 동의요청 대상이 아님(일반 스케줄 셀).
  if (!overlay?.leaveId) return;

  const leave = leaveTypeList.value.find((l) => l.leaveCd === overlay.leaveCd);
  // YYYYMMDD → YYYY-MM-DD (팝업 표시용).
  const startDateDisplay =
    workYmd && workYmd.length === 8
      ? `${workYmd.slice(0, 4)}-${workYmd.slice(4, 6)}-${workYmd.slice(6, 8)}`
      : workYmd;

  openPop(LeaveChangeRequestPop, {
    target: {
      leaveId: overlay.leaveId,
      userCd: user.userCd,
      userNm: user.userNm,
      startDate: startDateDisplay,
      leaveNm: leave ? leave.leaveNm : overlay.leaveCd,
      // 촉진단계명은 오버레이에 없음 → 팝업이 null 을 "비촉진"으로 표기(서버가 단계/권한 재검증).
      promotionStageNm: null,
    },
    // 동의요청 등록 성공 시 팝업을 닫고 그리드 재조회(오버레이/스케줄 최신화).
    //   LeaveChangeRequestPop 은 submitted 만 emit(자체 close 없음) → 부모가 close 후 재조회.
    onSubmitted: () => {
      closePop();
      fnSearch();
    },
  });
};

// ── 근무타입(SCH_CD)×날짜 지정 가능 여부 검증 ───────────────
// 반환: 위반 시 { reasonCode, reason }, 정상이면 null.
// 검증 메타가 없는 코드(휴가코드 등)는 검증 대상이 아니므로 null 반환.
const validateSchCell = (schCd, workYmd) => {
  const meta = schTypeValidMetaMap.value[schCd];
  if (!meta) return null;

  const versionList = meta.versionList || [];
  if (versionList.length === 0) return null;

  // 검증1) 생성일(MIN APPLY_DATE) 이전이면 차단
  if (!workYmd || workYmd < meta.createDt) {
    return {
      reasonCode: "BEFORE_CREATE",
      reason: "근무타입 생성일 이전 날짜입니다.",
    };
  }

  // 검증2) effective USE_YN : APPLY_DATE <= workYmd 인 최신 버전의 USE_YN
  let effectiveUseYn = null;
  for (const v of versionList) {
    if (v.applyDate <= workYmd) {
      effectiveUseYn = v.useYn;
    } else {
      break;
    }
  }
  if (effectiveUseYn === "N") {
    return {
      reasonCode: "USE_YN_N",
      reason: "해당 날짜는 근무타입 미사용 기간입니다.",
    };
  }

  return null;
};

// ── 선택 범위 ──────────────────────────────────────────────
const selectionRange = computed(() => {
  if (!dragStart.value || !dragEnd.value) return null;
  const ymds = [dragStart.value.workYmd, dragEnd.value.workYmd].sort();
  return {
    minRow: Math.min(dragStart.value.rowIdx, dragEnd.value.rowIdx),
    maxRow: Math.max(dragStart.value.rowIdx, dragEnd.value.rowIdx),
    minDay: ymds[0],
    maxDay: ymds[1],
  };
});

const isCellSelected = (rowIdx, workYmd) => {
  if (!selectionRange.value) return false;
  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  return (
    rowIdx >= minRow &&
    rowIdx <= maxRow &&
    workYmd >= minDay &&
    workYmd <= maxDay
  );
};

const getSelEdgeClasses = (rowIdx, workYmd) => {
  if (!isCellSelected(rowIdx, workYmd)) return [];
  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const cls = [];
  if (rowIdx === minRow) cls.push("sel-top");
  if (rowIdx === maxRow) cls.push("sel-bottom");
  if (workYmd === minDay) cls.push("sel-left");
  if (workYmd === maxDay) cls.push("sel-right");
  return cls;
};

const selectionLabel = computed(() => {
  if (!selectionRange.value) return "";
  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const minDayNum = parseInt(minDay.slice(6));
  const maxDayNum = parseInt(maxDay.slice(6));
  return `${getRowLabel(minRow)}${minDayNum}:${getRowLabel(maxRow)}${maxDayNum}`;
});

const selectionCount = computed(() => {
  if (!selectionRange.value) return 0;
  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const dayCount = daysInMonth.value.filter(
    (d) => d.workYmd >= minDay && d.workYmd <= maxDay
  ).length;
  return (maxRow - minRow + 1) * dayCount;
});

// ── 드래그 이벤트 ──────────────────────────────────────────
const onCellDown = (rowIdx, workYmd) => {
  isDragging.value = true;
  dragStart.value = { rowIdx, workYmd };
  dragEnd.value = { rowIdx, workYmd };
};

const onCellMove = (rowIdx, workYmd) => {
  if (!isDragging.value) return;
  dragEnd.value = { rowIdx, workYmd };
};

const onCellUp = () => {
  isDragging.value = false;
};

const onTableMouseLeave = () => {
  // 드래그 중 테이블 밖 이동 시 선택 유지, mouseup으로 종료
};

const onDocMouseUp = () => {
  isDragging.value = false;
};

// ── 근무 타입 적용 ─────────────────────────────────────────
const fnApplySchType = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 근무타입을 변경할 수 없습니다.");
    return;
  }
  if (!selectedSchType.value) {
    await proxy.$alert("스케줄 타입을 선택해주세요.");
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert("적용할 영역을 선택해주세요.");
    return;
  }

  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const updatedUserCds = new Set();
  // 위반으로 스킵된 날짜를 사유별로 수집 (고지 문구용, 중복 제거)
  const skippedReasons = new Map();
  // prafta-com-008-D-5: 교대 잠금으로 스킵된 셀 수(별도 안내).
  let shiftLockedSkipCount = 0;
  for (let r = minRow; r <= maxRow; r++) {
    const user = userList.value[r];
    if (!user) continue;
    const daysInRange = daysInMonth.value.filter(
      (d) => d.workYmd >= minDay && d.workYmd <= maxDay
    );
    for (const d of daysInRange) {
      if (
        schHolidayMode.value === "exclude" &&
        (d.weekendYn === "Y" || d.holidayYn === "Y")
      ) {
        continue;
      }
      // prafta-com-008-D-5: 교대팀 소속 구간 셀은 SCH 적용 불가(연차만 허용 — D-3). 스킵.
      if (isShiftLockedCell(user.userCd, d.workYmd)) {
        shiftLockedSkipCount++;
        continue;
      }
      // 근무타입 생성일·미사용 기간 검증 — 위반 시 해당 셀 스킵
      const violation = validateSchCell(selectedSchType.value, d.workYmd);
      if (violation) {
        if (!skippedReasons.has(violation.reasonCode)) {
          skippedReasons.set(violation.reasonCode, violation.reason);
        }
        continue;
      }
      scheduleData.value[`${user.userCd}_${d.workYmd}`] = selectedSchType.value;
      updatedUserCds.add(user.userCd);
    }
  }
  if (updatedUserCds.size > 0) {
    const merged = new Set([...checkedRows.value, ...updatedUserCds]);
    checkedRows.value = [...merged];
  }
  // 스킵된 셀이 있으면 사용자에게 고지
  if (skippedReasons.size > 0 || shiftLockedSkipCount > 0) {
    const schNm = getSchTypeNm(selectedSchType.value);
    const lines = [...skippedReasons.values()];
    if (shiftLockedSkipCount > 0) {
      lines.push(
        `교대근무팀 소속 기간 ${shiftLockedSkipCount}건은 근무계획을 변경할 수 없습니다(연차만 사용 가능).`
      );
    }
    await proxy.$alert(
      `'${schNm}' 근무타입을 지정할 수 없는 날짜가 있어 일부 셀은 제외되었습니다.\n${lines.join("\n")}`
    );
  }
};

// ── 법정 연차 적용 ─────────────────────────────────────────
// 타입 선택 없이 법정 본연차(SYS_ANNUAL)만 적용한다. 저장 시 백엔드가 잔여 차감/유효성 검증을 수행한다.
const fnApplyLeaveType = async () => {
  const leaveCd = annualLeaveType.value?.leaveCd;
  if (!leaveCd) {
    await proxy.$alert(
      "적용 가능한 법정 연차(SYS_ANNUAL)가 없습니다. 연차 정책 설정을 확인해주세요."
    );
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert("적용할 영역을 선택해주세요.");
    return;
  }

  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const updatedUserCds = new Set();
  // 근무 스케줄(SCH)이 배정되지 않은 빈 셀로 인해 연차 적용에서 제외된 셀 수(안내용).
  let noScheduleSkipCount = 0;
  for (let r = minRow; r <= maxRow; r++) {
    const user = userList.value[r];
    if (!user) continue;
    const daysInRange = daysInMonth.value.filter(
      (d) => d.workYmd >= minDay && d.workYmd <= maxDay
    );
    for (const d of daysInRange) {
      if (
        leaveHolidayMode.value === "exclude" &&
        (d.weekendYn === "Y" || d.holidayYn === "Y")
      ) {
        continue;
      }
      const cellKey = `${user.userCd}_${d.workYmd}`;
      // 근무 스케줄(SCH 배정)이 없는 빈 셀에는 연차를 적용할 수 없다(사용자 요청).
      //   "스케줄 있음" = 셀에 이미 연차가 적용되어 있거나(멱등 재적용 허용),
      //   셀 값이 근무타입 코드(schTypeList 의 schCd)인 경우.
      //   빈 값이거나 schCd/연차코드가 아닌 셀은 "스케줄 없음" → 스킵.
      if (!hasWorkScheduleForLeave(user.userCd, d.workYmd)) {
        noScheduleSkipCount++;
        continue;
      }
      scheduleData.value[cellKey] = leaveCd;
      updatedUserCds.add(user.userCd);
    }
  }
  if (updatedUserCds.size > 0) {
    const merged = new Set([...checkedRows.value, ...updatedUserCds]);
    checkedRows.value = [...merged];
  }
  // 스케줄 없는 셀이 있어 일부(또는 전부) 제외된 경우 안내.
  if (noScheduleSkipCount > 0) {
    if (updatedUserCds.size === 0) {
      await proxy.$alert(
        "연차를 적용할 수 있는(근무 스케줄이 있는) 셀이 없습니다."
      );
    } else {
      await proxy.$alert(
        `근무 스케줄이 없는 ${noScheduleSkipCount}개 셀은 연차 적용에서 제외되었습니다.`
      );
    }
  }
};

// ── 연차 적용 가능 여부(근무 스케줄 배정 셀) 판정 ─────────────
// 빈 셀(근무 스케줄 미배정)에는 연차를 적용할 수 없다. true 이면 적용 허용.
//   - 이미 연차 셀(leaveOverlay 보유): 멱등 재적용 무해 → 허용.
//   - 현재 셀 값이 근무타입 코드(schTypeList 의 schCd): 근무 스케줄 있음 → 허용.
//   - 빈 값 / schCd 도 연차코드도 아님: 근무 스케줄 없음 → 불허.
const hasWorkScheduleForLeave = (userCd, workYmd) => {
  if (isLeaveCell(userCd, workYmd)) return true;
  const code = scheduleData.value[`${userCd}_${workYmd}`];
  if (!code) return false;
  return schTypeList.value.some((s) => s.schCd === code);
};

// ── 셀 비우기 (지우기) (PRAFTA-041) ──────────────────────────
// 드래그 선택영역의 셀을 빈값으로 만들고 해당 row 를 자동 체크한다.
// 저장 시 baseline 에 값이 있던 셀이 빈값이면 삭제(/delete-user-work-plan-cells) 대상이 된다.
const fnClearCells = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 셀을 비울 수 없습니다.");
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert(getMessage(MSG.SCHEDULE_CLEAR_SELECT_REQUIRED));
    return;
  }

  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const updatedUserCds = new Set();
  // prafta-com-008-D-5: 교대 잠금으로 비우기 스킵된 셀 수(별도 안내).
  let shiftLockedSkipCount = 0;
  for (let r = minRow; r <= maxRow; r++) {
    const user = userList.value[r];
    if (!user) continue;
    const daysInRange = daysInMonth.value.filter(
      (d) => d.workYmd >= minDay && d.workYmd <= maxDay
    );
    for (const d of daysInRange) {
      // prafta-com-008-D-5: 교대팀 소속 구간 셀은 비우기 불가(연차만 허용 — D-3). 스킵.
      //   (연차 셀은 BE B-5 가 동의요청 경유로 보호하므로 여기선 교대 잠금만 사전 차단.)
      if (isShiftLockedCell(user.userCd, d.workYmd)) {
        shiftLockedSkipCount++;
        continue;
      }
      // 셀을 빈값으로 만든다(키는 유지 — baseline 대비 "값있던→빈값" 삭제 판정용).
      scheduleData.value[`${user.userCd}_${d.workYmd}`] = "";
      updatedUserCds.add(user.userCd);
    }
  }
  if (updatedUserCds.size > 0) {
    const merged = new Set([...checkedRows.value, ...updatedUserCds]);
    checkedRows.value = [...merged];
  }
  if (shiftLockedSkipCount > 0) {
    await proxy.$alert(
      `교대근무팀 소속 기간 ${shiftLockedSkipCount}건은 비우기할 수 없습니다(연차만 사용 가능).`
    );
  }
};

// ── 사업장 조회 ────────────────────────────────────────────
const fnSrchSiteInfo = async () => {
  userList.value = [];
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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
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

      fnGetSchTypeList();
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
      fnSiteNodeSearchPopOpenForCondition();
    }
  }
};

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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
  userList.value = [];
  fnGetSchTypeList();
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpenForCondition = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
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

// ── 조회 ───────────────────────────────────────────────────
// PRAFTA-028 - 조회 스코프(사업장+부서+하위포함)의 해당 월 마감 여부 조회.
//   마감이면 근무타입 적용/저장/삭제를 막는다(백엔드도 동일 가드). 조회 실패 시엔 막지 않음.
const fnLoadCloseStatus = async () => {
  try {
    const res = await axios.get("/webApi/attd07/attd-close-status", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        closeYm: workYm.value.replace("-", ""),
      },
    });
    isMonthClosed.value = res.status === 200 ? !!res.data?.closed : false;
  } catch (e) {
    isMonthClosed.value = false;
  }
};

const fnSearch = async () => {
  userList.value = [];
  checkedRows.value = [];

  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, {
        fieldLabel: "사업장",
      })
    );
    siteNoFcs.value.focus();
    return false;
  }

  if (proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, {
        fieldLabel: "소속부서",
      })
    );
    nodeCdFcs.value.focus();
    return false;
  }

  try {
    const response = await axios.get("/webApi/attd05/user-work-plans", {
      params: {
        workYm: workYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);

      userList.value = response.data.userListResultList;
      daysInMonth.value = response.data.dayResultList;
      scheduleData.value = {};
      response.data.schedResultList.forEach((item) => {
        scheduleData.value[`${item.userCd}_${item.workYmd}`] = item.workPlanCd;
      });
      // prafta-com-008-E-6: 연차 오버레이 적재(셀 "연차" 표시는 work_plan 코드가 아닌 leave_use 기준).
      //   prafta-com-008-B-7: leaveId 동반 적재(셀 개별 동의요청 진입 시 TARGET_LEAVE_ID 로 사용).
      leaveOverlay.value = {};
      (response.data.leaveOverlayResultList ?? []).forEach((item) => {
        leaveOverlay.value[`${item.userCd}_${item.workYmd}`] = {
          leaveCd: item.leaveCd,
          leaveId: item.leaveId,
        };
      });
      // prafta-com-008-D-5: 교대 잠금 오버레이 적재(교대팀 소속 구간 SCH 셀 비활성/자물쇠 표시 단일출처).
      shiftLockOverlay.value = {};
      (response.data.shiftLockOverlayResultList ?? []).forEach((item) => {
        shiftLockOverlay.value[`${item.userCd}_${item.workYmd}`] = true;
      });
      // PRAFTA-041 - 조회 직후 baseline 깊은 복사(plain object). 저장 시 dirty 비교 기준이며,
      //   저장 성공 후 재조회로 자연 갱신되어 직후 재저장 시 0건이 된다.
      scheduleBaseline.value = { ...scheduleData.value };
      dragStart.value = null;
      await fnLoadCloseStatus(); // PRAFTA-028 - 조회 후 마감 여부 갱신
      dragEnd.value = null;

      await fnGetLeaveTypeList();
      await fnGetSchTypeList();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ── 스케줄 타입 목록 조회 ──────────────────────────────────
const fnGetSchTypeList = async () => {
  try {
    const response = await axios.get("/webApi/attd05/sch-type-lists", {
      params: {
        siteCd: siteCd.value,
      },
    });
    schTypeList.value = response.data?.schTypeResultList ?? [];

    // 근무타입(SCH_CD)별 검증 메타를 schCd 키 맵으로 가공해 보관
    const metaList = response.data?.schTypeValidMetaList ?? [];
    const metaMap = {};
    metaList.forEach((m) => {
      metaMap[m.schCd] = {
        createDt: m.createDt,
        versionList: m.versionList ?? [],
      };
    });
    schTypeValidMetaMap.value = metaMap;
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "스케줄 목록 조회 오류.");
    await proxy.$alert(msg);
  }
};

// ── 연차 타입 목록 조회 (법정 본연차 식별 + 셀 표시명 변환용) ──────────
// 관리자가 직접 적용하는 화면이며 법정 본연차(SYS_ANNUAL)만 고정 적용한다.
// 법정 휴가(LEAVE_NATURE_TYPE='01') 목록을 받아 SYS_ANNUAL 식별(annualLeaveType) 및
// 기존 셀의 표시명 변환(getCellNmValue)에 사용한다. (prafta-021 → 타입 선택 제거 후속)
const fnGetLeaveTypeList = async () => {
  try {
    const response = await axios.get("/webApi/attd05/leave-type-lists", {});
    leaveTypeList.value = (response.data?.leaveTypeResultList ?? []).filter(
      (t) => t.leaveNatureType === "01"
    );
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "스케줄 목록 조회 오류.");
    await proxy.$alert(msg);
  }
};

// ── 저장 ───────────────────────────────────────────────────
const fnSave = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 근무계획을 저장할 수 없습니다.");
    return;
  }
  if (checkedRows.value.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }

  const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
  // 조회월(workYm) 접두어 — 체크된 사용자라도 "조회 중인 달"의 셀만 저장한다.
  // (과거/타 월 근무계획이 scheduleData 에 섞여 들어와도 무관한 셀까지 재저장되지 않도록 방어)
  const ymPrefix = workYm.value.replace("-", "");

  // PRAFTA-041 - baseline 대비 dirty 판정.
  //   대상 셀 = (체크된 row) AND (조회월) 교집합.
  //   - 신규/값변경(현재값 있고 baseline 과 다름) → 업서트(save-user-work-plans)
  //   - 값있던 셀이 현재 빈값(비우기) → 삭제(delete-user-work-plan-cells)
  //   - 동일값(no-op) → 제외
  // baseline + 현재 양쪽 키의 합집합을 순회해 "사라진(비워진) 셀"도 빠짐없이 잡는다.
  const upsertList = [];
  const deleteList = [];
  const allKeys = new Set([
    ...Object.keys(scheduleData.value),
    ...Object.keys(scheduleBaseline.value),
  ]);
  for (const key of allKeys) {
    const workYmd = key.slice(-8);
    const userCd = key.substring(0, key.length - 9);
    if (!checkedRows.value.includes(userCd)) continue;
    if (!workYmd || !workYmd.startsWith(ymPrefix)) continue;

    const current = scheduleData.value[key] || "";
    const baseline = scheduleBaseline.value[key] || "";
    if (current === baseline) continue; // 동일값 = no-op

    if (current) {
      // 신규 또는 값 변경 → 업서트
      upsertList.push({
        cmpnyCd,
        siteCd: siteCd.value,
        userCd,
        workYmd,
        workPlanCd: current,
      });
    } else if (baseline) {
      // 값있던 셀 → 빈값 = 비우기(삭제). 빈값 신규(baseline 도 없음)는 전송 대상 아님.
      deleteList.push({
        cmpnyCd,
        siteCd: siteCd.value,
        userCd,
        workYmd,
      });
    }
  }

  // 업서트·삭제 모두 0건이면 API 미호출 + 안내(AC 3·4·6).
  if (upsertList.length === 0 && deleteList.length === 0) {
    await proxy.$alert(getMessage(MSG.SCHEDULE_NO_CHANGE));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    let savedCount = 0;
    let deletedCount = 0;
    let skippedList = [];

    // 1) 삭제(셀 비우기) 먼저 — 직접 연차 셀이면 서버가 차감 복원까지 수행.
    //    prafta-com-008-E (M2): 승인기반 연차 셀은 서버가 skip 하고 skippedList 로 사유를 내려준다.
    if (deleteList.length > 0) {
      const delRes = await axios.post(
        "/webApi/attd05/delete-user-work-plan-cells",
        deleteList
      );
      if (delRes.status === 200) {
        // 실제 삭제 건수는 서버 응답 기준(승인기반 연차 skip 분 제외). 구버전 응답(본문 없음) 폴백.
        deletedCount = delRes.data?.deletedCount ?? deleteList.length;
        if (Array.isArray(delRes.data?.skippedList)) {
          skippedList = [...skippedList, ...delRes.data.skippedList];
        }
      }
    }

    // 2) 업서트(신규/변경) — 0건이면 호출 생략.
    if (upsertList.length > 0) {
      const saveRes = await axios.post(
        "/webApi/attd05/save-user-work-plans",
        upsertList
      );
      if (saveRes.status === 200) {
        savedCount = saveRes.data?.savedCount ?? 0;
        skippedList = saveRes.data?.skippedList ?? [];
      }
    }

    // 결과 합산 안내
    let resultMsg = `${savedCount}건 저장`;
    if (deletedCount > 0) {
      resultMsg += `, ${deletedCount}건 비우기(삭제)`;
    }
    resultMsg += "되었습니다.";

    // prafta-com-008-B-7(D-E8): skip 사유를 "연차 셀 비우기 불가(동의요청 유도)"와
    //   기존 "근무타입 지정 불가"로 분리 안내. 연차 셀은 비우기로 삭제되지 않고 보호된다.
    const leaveConsentSkips = skippedList.filter(
      (s) => s.reasonCode === "LEAVE_CELL_CONSENT_REQUIRED"
    );
    const otherSkips = skippedList.filter(
      (s) => s.reasonCode !== "LEAVE_CELL_CONSENT_REQUIRED"
    );

    if (leaveConsentSkips.length > 0) {
      const detail = leaveConsentSkips
        .map((s) => `· ${s.workYmd} : ${getCellNmValue(s.userCd, s.workYmd) || "연차"}`)
        .join("\n");
      resultMsg +=
        `\n\n아래 ${leaveConsentSkips.length}건은 연차 등록일이라 비우기로 삭제할 수 없습니다.` +
        `\n해당 셀을 더블클릭하여 연차 변경/삭제 요청(동의요청)을 이용해 주세요.\n${detail}`;
    }
    if (otherSkips.length > 0) {
      const detail = otherSkips
        .map((s) => {
          const schNm = getSchTypeNm(s.workPlanCd);
          return `· ${s.workYmd} / ${schNm} : ${s.reason}`;
        })
        .join("\n");
      resultMsg += `\n\n아래 ${otherSkips.length}건은 근무타입 지정이 불가하여 제외되었습니다.\n${detail}`;
    }
    await proxy.$alert(resultMsg);

    // 저장 성공 → 재조회로 baseline 자연 갱신(직후 재저장 0건).
    fnSearch();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ── 삭제 ───────────────────────────────────────────────────
const fnDelete = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 근무계획을 삭제할 수 없습니다.");
    return;
  }
  if (checkedRows.value.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }

  const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
  const deleteList = checkedRows.value.map((userCd) => ({
    cmpnyCd,
    userCd,
    siteCd: siteCd.value,
    workYm: workYm.value,
  }));

  console.log(deleteList);

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/attd05/delete-user-work-plans",
      deleteList
    );
    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.DELETE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ── 엑셀 업로드 팝업 오픈 ────────────────────────────────────
const fnUploadExcel = () => {
  openPop(ExcelUploadPop, {
    siteCd_p: siteCd.value,
    siteNo_p: siteNo.value,
    siteNm_p: siteNm.value,
    nodeCd_p: nodeCd.value,
    nodeNm_p: nodeNm.value,
    incSubNodeYn_p: incSubNodeYn.value,
    workYm_p: workYm.value,
    onSaved: fnSearch,
  });
};

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;

    if (proxy.$util.isEmpty(sessionStorage.getItem("gv_nodeCd"))) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      nodeCd.value = sessionStorage.getItem("gv_nodeCd");
      nodeNm.value = sessionStorage.getItem("gv_nodeNm");

      fnSearch();
    }
  }
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  document.addEventListener("mouseup", onDocMouseUp);
});

onUnmounted(() => {
  document.removeEventListener("mouseup", onDocMouseUp);
});
</script>

<style scoped>
/* ── viewSearch 추가 스타일 ──────────────────────────────── */
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

.input-readonly {
  background: var(--color-bg, #f3f4f6) !important;
  cursor: default;
  color: var(--color-text-muted, #6b7280);
}

.ym-clickable {
  display: inline-block;
  padding: 0.3rem 0.8rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: var(--input-radius, 6px);
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text-strong, #111827);
  font-weight: 500;
  transition:
    border-color 0.15s,
    color 0.15s;
  user-select: none;
}
.ym-clickable:hover {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
}

/* ── 적용 툴바 ───────────────────────────────────────────── */
.attd05-toolbar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.45rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
  flex-wrap: wrap;
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}

.toolbar-label {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  padding-right: 0.2rem;
}

.toolbar-sch-select {
  padding: 0.3rem 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: var(--input-radius, 6px);
  background: #fff;
  font-size: 0.875rem;
  color: var(--color-text-strong, #111827);
  cursor: pointer;
  min-width: 160px;
  font-family: "Pretendard", sans-serif;
}
.toolbar-sch-select:focus {
  outline: none;
  border-color: var(--color-primary, #16a34a);
}

.toolbar-selection-box {
  min-width: 90px;
  padding: 0.3rem 0.6rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: var(--input-radius, 6px);
  background: #fff;
  font-size: 0.875rem;
  color: var(--color-text-muted, #9ca3af);
  text-align: center;
  font-family: monospace;
  letter-spacing: 0.03em;
}
.toolbar-selection-box.has-value {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
  font-weight: 700;
}

.toolbar-radio-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0 0.2rem;
}
.radio-item {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text, #374151);
  user-select: none;
}
.radio-item input[type="radio"] {
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
}

.btn-toolbar-apply {
  padding: 0.35rem 1rem;
  background: var(--color-primary, #16a34a);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  transition: background 0.15s;
}
.btn-toolbar-apply:hover {
  background: #15803d;
}

/* ── 법정 연차 섹션 pink 스타일 ─────────────────────────── */
.toolbar-label-leave {
  color: #be185d;
}

/* 연차 타입 선택 제거 — 법정 본연차 고정 표시 칩 */
.toolbar-fixed-leave {
  display: inline-flex;
  align-items: center;
  min-width: 160px;
  padding: 0.3rem 0.6rem;
  border: 1px solid #db2777;
  border-radius: var(--input-radius, 6px);
  background: rgba(219, 39, 119, 0.06);
  font-size: 0.875rem;
  font-weight: 600;
  color: #db2777;
  font-family: "Pretendard", sans-serif;
  white-space: nowrap;
}
.toolbar-fixed-leave.is-missing {
  border-color: var(--color-border, #d1d5db);
  background: var(--color-bg, #f3f4f6);
  color: var(--color-text-muted, #9ca3af);
}

.toolbar-selection-box-leave.has-value {
  border-color: #db2777 !important;
  color: #db2777 !important;
}

.radio-item-leave input[type="radio"] {
  accent-color: #db2777;
}

.btn-toolbar-apply-leave {
  background: #db2777;
}
.btn-toolbar-apply-leave:hover {
  background: #be185d;
}

/* ── 툴바 구분선 ─────────────────────────────────────────── */
.toolbar-divider {
  width: 1px;
  height: 28px;
  background: var(--color-border, #d1d5db);
  margin: 0 0.75rem;
  flex-shrink: 0;
}

.toolbar-count-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  margin-left: 0.25rem;
  margin-right: 0.5rem;
}
.toolbar-count-label.invisible {
  visibility: hidden;
}

/* ── 툴바 스페이서 / 업로드 버튼 ────────────────────────── */
.toolbar-spacer {
  flex: 1;
}

.btn-toolbar-upload {
  padding: 0.35rem 1rem;
  background: #fff;
  border: 1px solid var(--color-border, #d1d5db);
  color: var(--color-text, #374151);
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-toolbar-upload:hover {
  background: var(--color-bg, #f3f4f6);
}

/* ── 셀 비우기(지우기) 버튼 (PRAFTA-041) ─────────────────── */
.btn-toolbar-clear {
  padding: 0.35rem 1rem;
  background: #fff;
  border: 1px solid var(--color-border, #d1d5db);
  color: var(--color-text, #374151);
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-toolbar-clear:hover {
  background: var(--color-bg, #f3f4f6);
}
.btn-toolbar-clear:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── 테이블 바디 영역 ─────────────────────────────────────── */
.attd05-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
}

.attd05-table-outer {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
}

/* ── 테이블 ───────────────────────────────────────────────── */
.attd05-table {
  border-collapse: collapse;
  table-layout: auto;
  width: max-content;
  min-width: 100%;
  font-family: "Pretendard", sans-serif;
  font-size: 0.8125rem;
}

/* 틀고정: 가로(left) - 순번/체크/사용자정보 순서로 left 누적 */
.sticky-left {
  position: sticky;
  left: 0;
  z-index: 2;
  background: var(--color-surface, #fff);
}
.sticky-col-seq {
  position: sticky;
  left: 0;
  z-index: 2;
  background: var(--color-surface, #fff);
}
.sticky-col-chk {
  position: sticky;
  left: 40px;
  z-index: 2;
  background: var(--color-surface, #fff);
}
.sticky-col-info {
  position: sticky;
  left: 80px;
  z-index: 2;
  background: var(--color-surface, #fff);
}

/* 틀고정: 세로(top) */
.sticky-top {
  position: sticky;
  top: 0;
  z-index: 1;
}

/* 좌상단 교차 셀은 z-index 최상위 */
.th-seq,
.th-chk,
.th-user-info {
  z-index: 4 !important;
}

/* ── 순번 / 체크박스 헤더 셀 ───────────────────────────────── */
.th-seq {
  min-width: 40px;
  width: 40px;
  background: var(--color-bg, #f3f4f6);
  border-bottom: 2px solid var(--color-border, #d1d5db);
  border-right: 1px solid var(--color-border, #e5e7eb);
  padding: 0.45rem 0.2rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  text-align: center;
  white-space: nowrap;
}

.th-chk {
  min-width: 40px;
  width: 40px;
  background: var(--color-bg, #f3f4f6);
  border-bottom: 2px solid var(--color-border, #d1d5db);
  border-right: 1px solid var(--color-border, #e5e7eb);
  padding: 0.45rem 0.2rem;
  text-align: center;
}

/* ── 순번 / 체크박스 데이터 셀 ─────────────────────────────── */
.td-seq {
  min-width: 40px;
  width: 40px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  border-right: 1px solid var(--color-border, #e5e7eb);
  padding: 0.35rem 0.2rem;
  text-align: center;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  background: var(--color-surface, #fff);
}

.td-chk {
  min-width: 40px;
  width: 40px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  border-right: 1px solid var(--color-border, #e5e7eb);
  padding: 0.35rem 0.2rem;
  text-align: center;
  background: var(--color-surface, #fff);
}

.td-chk input[type="checkbox"],
.th-chk input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
}

/* ── 헤더 ─────────────────────────────────────────────────── */
.th-user-info,
.th-day {
  background: var(--color-bg, #f3f4f6);
  border-bottom: 2px solid var(--color-border, #d1d5db);
  border-right: 1px solid var(--color-border, #e5e7eb);
  padding: 0.45rem 0.4rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  white-space: nowrap;
  text-align: center;
}

.th-user-info {
  min-width: 210px;
  width: 210px;
  text-align: left;
  padding-left: 0.6rem;
  border-right: 2px solid var(--color-border, #c7cdd6);
}

.th-day {
  min-width: 96px;
}

.head-sun {
  color: #ef4444;
}
.head-sat {
  color: #3b82f6;
}
.head-holiday {
  color: #ef4444;
}

/* ── 사용자 정보 셀 ───────────────────────────────────────── */
.td-user-info {
  min-width: 210px;
  width: 210px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  border-right: 2px solid var(--color-border, #c7cdd6);
  padding: 0.45rem 0.5rem;
  background: var(--color-surface, #fff);
  vertical-align: middle;
}

.user-cell-inner {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
}

.row-badge {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-primary, #16a34a);
  font-size: 0.7rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
}

.user-text {
  flex: 1;
  min-width: 0;
}

.u-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  white-space: nowrap;
}
.u-dept {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 0.1rem;
  white-space: nowrap;
}
.u-phone {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 0.05rem;
  white-space: nowrap;
}

/* ── 날짜 셀 ──────────────────────────────────────────────── */
.td-day {
  min-width: 96px;
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.35rem 0.3rem;
  text-align: center;
  cursor: pointer;
  white-space: pre-line;
  vertical-align: middle;
  font-size: 0.78rem;
  color: var(--color-text, #374151);
  line-height: 1.4;
  transition: background 0.05s;
}

.td-sun {
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
}
.td-sat {
  background: rgba(59, 130, 246, 0.04);
  color: #3b82f6;
}
.td-holiday {
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
}

.td-val {
  display: block;
  white-space: pre-line;
}
.val-muted {
  color: var(--color-text-muted, #9ca3af);
}

/* prafta-com-008-B-7(D-E8): 연차 등록 셀 — 더블클릭 시 연차 변경/삭제 동의요청 진입 가능 표시.
   비우기로 삭제 불가(서버 skip). 점선 밑줄로 클릭 가능 affordance 만 부여(색상/간격은 디자인 토큰). */
.td-leave .td-val {
  text-decoration: underline dotted;
  text-underline-offset: 2px;
  color: var(--color-primary, #16a34a);
  font-weight: 600;
}

/* prafta-com-008-D-5: 교대팀 소속 구간 SCH 셀 — 비활성(자물쇠) 표시.
   교대패턴 자동생성으로만 설정 가능하며 직접 변경/비우기 불가(연차 셀은 활성 — D-3).
   색상은 디자인 토큰(muted) 사용. 셀 우상단 자물쇠 표시는 ::after 유니코드(별도 아이콘 의존 없음). */
.td-day.is-shift-locked {
  position: relative;
  background: var(--color-bg, #f3f4f6);
  cursor: not-allowed;
}
.td-day.is-shift-locked .td-val {
  color: var(--color-text-muted, #9ca3af);
}
.td-day.is-shift-locked::after {
  content: "\1F512"; /* 🔒 자물쇠 */
  position: absolute;
  top: 1px;
  right: 2px;
  font-size: 0.62rem;
  line-height: 1;
  opacity: 0.55;
}

/* ── 드래그 선택 스타일 ──────────────────────────────────── */
.td-selected {
  background: rgba(22, 163, 74, 0.1) !important;
}

.sel-top {
  border-top: 2px solid #16a34a !important;
}
.sel-bottom {
  border-bottom: 2px solid #16a34a !important;
}
.sel-left {
  border-left: 2px solid #16a34a !important;
}
.sel-right {
  border-right: 2px solid #16a34a !important;
}
</style>
