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
      <button class="btn-toolbar-apply" @click="fnApplySchType">적용</button>
      <span class="toolbar-count-label" :class="{ invisible: !selectionLabel }">
        선택: {{ selectionLabel || "–" }} &middot; {{ selectionCount }}건
      </span>

      <!-- ── 구분선 ─────────────────────────────────────── -->
      <div class="toolbar-divider"></div>

      <!-- ── 연차 타입 적용 섹션 ─────────────────────────── -->
      <span class="toolbar-label toolbar-label-leave">연차 타입</span>
      <select
        v-model="selectedLeaveType"
        class="toolbar-sch-select toolbar-sch-select-leave"
      >
        <option value="">연차 타입 선택</option>
        <option
          v-for="leave in leaveTypeList"
          :key="leave.leaveCd"
          :value="leave.leaveCd"
        >
          {{ leave.leaveNm }}
        </option>
      </select>
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
                  ...getSelEdgeClasses(rowIdx, d.workYmd),
                ]"
                @mousedown.prevent="onCellDown(rowIdx, d.workYmd)"
                @mousemove="onCellMove(rowIdx, d.workYmd)"
                @mouseup="onCellUp"
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
const { open: openPop } = useModal();

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

// ── 적용 옵션 (근무 타입) ──────────────────────────────────
const selectedSchType = ref("");
const schHolidayMode = ref("exclude");

// ── 적용 옵션 (연차 타입) ──────────────────────────────────
const selectedLeaveType = ref("");
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
const getCellNmValue = (userCd, workYmd) => {
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
  if (skippedReasons.size > 0) {
    const schNm = getSchTypeNm(selectedSchType.value);
    const reasonText = [...skippedReasons.values()].join("\n");
    await proxy.$alert(
      `'${schNm}' 근무타입을 지정할 수 없는 날짜가 있어 일부 셀은 제외되었습니다.\n${reasonText}`
    );
  }
};

// ── 연차 타입 적용 ─────────────────────────────────────────
const fnApplyLeaveType = async () => {
  if (!selectedLeaveType.value) {
    await proxy.$alert("연차 타입을 선택해주세요.");
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert("적용할 영역을 선택해주세요.");
    return;
  }

  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  const updatedUserCds = new Set();
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
      scheduleData.value[`${user.userCd}_${d.workYmd}`] =
        selectedLeaveType.value;
      updatedUserCds.add(user.userCd);
    }
  }
  if (updatedUserCds.size > 0) {
    const merged = new Set([...checkedRows.value, ...updatedUserCds]);
    checkedRows.value = [...merged];
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
      dragStart.value = null;
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

// ── 연차 타입 목록 조회 ──────────────────────────────────
const fnGetLeaveTypeList = async () => {
  try {
    // TODO: API 연동
    const response = await axios.get("/webApi/attd05/leave-type-lists", {});
    leaveTypeList.value = response.data?.leaveTypeResultList ?? [];
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "스케줄 목록 조회 오류.");
    await proxy.$alert(msg);
  }
};

// ── 저장 ───────────────────────────────────────────────────
const fnSave = async () => {
  if (checkedRows.value.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }

  const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
  const saveList = Object.entries(scheduleData.value)
    .filter(([key, workPlanCd]) => {
      if (!workPlanCd) return false;
      const workYmd = key.slice(-8);
      const userCd = key.substring(0, key.length - 9);
      return checkedRows.value.includes(userCd) && !!workYmd;
    })
    .map(([key, workPlanCd]) => {
      const workYmd = key.slice(-8);
      const userCd = key.substring(0, key.length - 9);
      return { cmpnyCd, siteCd: siteCd.value, userCd, workYmd, workPlanCd };
    });

  if (saveList.length === 0) {
    await proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/attd05/save-user-work-plans",
      saveList
    );
    if (response.status === 200) {
      const skippedList = response.data?.skippedList ?? [];
      if (skippedList.length > 0) {
        // 서버 검증으로 스킵된 셀 목록을 사용자에게 표시
        const detail = skippedList
          .map((s) => {
            const schNm = getSchTypeNm(s.workPlanCd);
            return `· ${s.workYmd} / ${schNm} : ${s.reason}`;
          })
          .join("\n");
        await proxy.$alert(
          `${response.data.savedCount}건 저장되었습니다.\n` +
            `아래 ${skippedList.length}건은 근무타입 지정이 불가하여 제외되었습니다.\n${detail}`
        );
      } else {
        await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      }
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ── 삭제 ───────────────────────────────────────────────────
const fnDelete = async () => {
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

/* ── 연차 타입 섹션 pink 스타일 ─────────────────────────── */
.toolbar-label-leave {
  color: #be185d;
}

.toolbar-sch-select-leave:focus {
  border-color: #db2777 !important;
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
