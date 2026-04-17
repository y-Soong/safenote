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
          v-model="searchYm"
        />
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 적용 툴바 -->
    <div class="attd05-toolbar">
      <span class="toolbar-label">적용</span>
      <select v-model="selectedSchType" class="toolbar-sch-select">
        <option value="">스케줄 타입 선택</option>
        <option v-for="sch in schTypeList" :key="sch.schNo" :value="sch.schNo">
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
          <input type="radio" v-model="holidayMode" value="exclude" />
          <span>휴일 제외</span>
        </label>
        <label class="radio-item">
          <input type="radio" v-model="holidayMode" value="include" />
          <span>휴일 포함</span>
        </label>
      </div>
      <button class="btn-toolbar-apply" @click="fnApply">적용</button>
      <span v-if="selectionLabel" class="toolbar-count-label">
        선택: {{ selectionLabel }} &middot; {{ selectionCount }}건
      </span>
    </div>

    <!-- 테이블 영역 -->
    <div class="viewBody attd05-body">
      <div class="attd05-table-outer" @mouseleave="onTableMouseLeave">
        <table class="attd05-table" @selectstart.prevent>
          <thead>
            <tr>
              <th class="th-user-info sticky-left sticky-top">사용자 정보</th>
              <th
                v-for="d in daysInMonth"
                :key="d.workYmd"
                class="th-day sticky-top"
                :class="{
                  'head-sun': d.dow === '일',
                  'head-sat': d.dow === '토',
                }"
              >
                {{ parseInt(d.workYmd.slice(6)) }}({{ d.dow }})
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(user, rowIdx) in userList" :key="user.userCd">
              <td class="td-user-info sticky-left">
                <div class="user-cell-inner">
                  <span class="row-badge">{{ getRowLabel(rowIdx) }}</span>
                  <div class="user-text">
                    <div class="u-name">
                      {{ user.userNm }}({{ user.userId }})
                    </div>
                    <div class="u-dept">
                      {{ user.nodeNm }} / {{ user.shiftNm || "-" }}
                    </div>
                    <div class="u-phone">{{ proxy.$util.formatPhoneNumber(user.mblNo) }}</div>
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
                      d.weekendYn && !getCellValue(user.userCd, d.workYmd),
                  }"
                >
                  {{
                    getCellValue(user.userCd, d.workYmd) ||
                    (d.weekendYn ? "-" : "")
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
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";

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
const searchYm = ref(
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

// ── 사용자 목록 ────────────────────────────────────────────
const userList = ref([]);

// ── 셀 데이터: key = `${userCd}_${day}`, value = 표시문자열 ─
const scheduleData = ref({});

// ── 적용 옵션 ─────────────────────────────────────────────
const selectedSchType = ref("");
const holidayMode = ref("exclude");

// ── 드래그 선택 상태 ──────────────────────────────────────
const isDragging = ref(false);
const dragStart = ref(null); // { rowIdx, workYmd }
const dragEnd = ref(null);

// ── 버튼 컨트롤 ────────────────────────────────────────────
const fnButtonControll = () => {
  localButtons.value.search = "Y";
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// ── 해당 월 날짜 목록 ──────────────────────────────────────
const daysInMonth = computed(() => {
  if (!searchYm.value) return [];
  const [year, month] = searchYm.value.split("-").map(Number);
  const lastDay = new Date(year, month, 0).getDate();
  const DOW = ["일", "월", "화", "수", "목", "금", "토"];
  const result = [];
  for (let d = 1; d <= lastDay; d++) {
    const date = new Date(year, month - 1, d);
    const dayOfWeek = date.getDay();
    const workYmd = `${year}${String(month).padStart(2, "0")}${String(d).padStart(2, "0")}`;
    result.push({
      workYmd,
      dow: DOW[dayOfWeek],
      weekendYn: dayOfWeek === 0 || dayOfWeek === 6,
      holidayYn: false, // API 응답으로 채워짐
    });
  }
  return result;
});

// ── 행 레이블 (A, B, C … Z, AA, AB …) ─────────────────────
const getRowLabel = (idx) => {
  if (idx < 26) return String.fromCharCode(65 + idx);
  return (
    String.fromCharCode(64 + Math.floor(idx / 26)) +
    String.fromCharCode(65 + (idx % 26))
  );
};

// ── 셀 값 조회 ─────────────────────────────────────────────
const getCellValue = (userCd, workYmd) => {
  return scheduleData.value[`${userCd}_${workYmd}`] || "";
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

// ── 적용 ───────────────────────────────────────────────────
const fnApply = async () => {
  if (!selectedSchType.value) {
    await proxy.$alert("스케줄 타입을 선택해주세요.");
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert("적용할 영역을 선택해주세요.");
    return;
  }

  const sch = schTypeList.value.find((s) => s.schNo === selectedSchType.value);
  const displayVal = sch ? sch.schNm : selectedSchType.value;

  const { minRow, maxRow, minDay, maxDay } = selectionRange.value;
  for (let r = minRow; r <= maxRow; r++) {
    const user = userList.value[r];
    if (!user) continue;
    const daysInRange = daysInMonth.value.filter(
      (d) => d.workYmd >= minDay && d.workYmd <= maxDay
    );
    for (const d of daysInRange) {
      if (holidayMode.value === "exclude" && (d.weekendYn || d.holidayYn))
        continue;
      scheduleData.value[`${user.userCd}_${d.workYmd}`] = displayVal;
    }
  }
};

// ── 사업장 조회 ────────────────────────────────────────────
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
      err?.response?.data?.message || err?.message || "조회 오류"
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
    await proxy.$alert(
      err?.response?.data?.message || err?.message || "조회 오류"
    );
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
  if(proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, {
        fieldLabel: "사업장",
      })
    );
    siteNoFcs.value.focus();
    return false;
  }

  if(proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, {
        fieldLabel: "사업장",
      })
    );
    nodeCdFcs.value.focus();
    return false;
  }

  try {
    const response = await axios.get("/webApi/attd05/user-work-plans", {
      params: {
        searchYm: searchYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      console.log(response.data.userListResultList[0].mblNo);
      // TODO: 서버 응답 세팅
      userList.value = response.data.userListResultList;
      // daysInMonth 서버 응답 사용 시: daysInMonth.value = response.data.dayResultList;
      scheduleData.value = {};
      response.data.schedResultList.forEach((item) => {
        const sch = schTypeList.value.find((s) => s.schNo === item.dayPlanCd);
        scheduleData.value[`${item.userCd}_${item.workYmd}`] = sch?.schNm ?? item.dayPlanCd;
      });
    }

    scheduleData.value = {};
    dragStart.value = null;
    dragEnd.value = null;
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

// ── 스케줄 타입 목록 조회 ──────────────────────────────────
const fnGetSchTypeList = async () => {
  try {
    // TODO: API 연동
    // const response = await axios.get('/webApi/attd01/sch-lists', {});
    // schTypeList.value = response.data?.schList ?? [];
    schTypeList.value = [
      { schNo: "SCH001", schNm: "09:00~18:00" },
      { schNo: "SCH002", schNm: "09:00~12:00\n14:00~18:00" },
      { schNo: "SCH003", schNm: "10:00~19:00" },
      { schNo: "SCH004", schNm: "08:00~17:00" },
      { schNo: "SCH005", schNm: "휴무" },
    ];
  } catch (err) {
    const msg =
      err?.response?.data?.message || err?.message || "스케줄 목록 조회 오류.";
    await proxy.$alert(msg);
  }
};

// ── Mock 데이터 초기화 ─────────────────────────────────────
const initMockData = () => {
  const mockUsers = [
    {
      userCd: "pjiyoung",
      userNm: "박지영",
      nodeNm: "디자인팀",
      shiftNm: null,
      mblNo: "010-3456-7890",
    },
    {
      userCd: "HONGgd",
      userNm: "홍길동",
      nodeNm: "인프라팀",
      shiftNm: null,
      mblNo: "010-3455-3333",
    },
    {
      userCd: "cdonghun",
      userNm: "최동훈",
      nodeNm: "DT팀",
      shiftNm: "C조",
      mblNo: "010-5678-9012",
    },
    {
      userCd: "jhaneul",
      userNm: "정하늘",
      nodeNm: "개발팀",
      shiftNm: null,
      mblNo: "010-6789-0123",
    },
    {
      userCd: "kseoyeon",
      userNm: "강서연",
      nodeNm: "마케팅팀",
      shiftNm: null,
      mblNo: "010-1234-5678",
    },
    {
      userCd: "lminsoo",
      userNm: "이민수",
      nodeNm: "IT팀",
      shiftNm: "A조",
      mblNo: "010-2345-6789",
    },
    {
      userCd: "cwonbin",
      userNm: "최원빈",
      nodeNm: "영업팀",
      shiftNm: null,
      mblNo: "010-9876-5432",
    },
    {
      userCd: "khyunjin",
      userNm: "김현진",
      nodeNm: "기획팀",
      shiftNm: null,
      mblNo: "010-4567-8901",
    },
  ];
  userList.value = mockUsers;

  const days = daysInMonth.value;
  mockUsers.forEach((user) => {
    days.forEach((d) => {
      if (d.weekendYn) return;
      scheduleData.value[`${user.userCd}_${d.workYmd}`] =
        d.dow === "목" ? "09:00~12:00\n14:00~18:00" : "09:00~18:00";
    });
  });
  // 홍길동은 다른 스케줄
  days.forEach((d) => {
    if (!d.weekendYn) {
      scheduleData.value[`HONGgd_${d.workYmd}`] = "10:00~19:00";
    }
  });
};

onMounted(async () => {
  fnButtonControll();
  await fnGetSchTypeList();
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

.toolbar-count-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  margin-left: 0.25rem;
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

/* 틀고정: 가로(left) */
.sticky-left {
  position: sticky;
  left: 0;
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
.th-user-info {
  z-index: 4 !important;
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
