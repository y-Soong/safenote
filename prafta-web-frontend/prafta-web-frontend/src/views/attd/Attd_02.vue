<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody attd02-body">
      <div class="attd02-main">
        <!-- 좌측: 휴일 캘린더 -->
        <div class="attd02-calendar">
          <div class="calendar-header">
            <div class="calendar-title-row">
              <div class="calendar-title-left">
                <h2 class="calendar-title">휴일 캘린더</h2>
                <div class="calendar-legend">
                  <span class="legend-item legend-public">
                    <span class="legend-dot"></span>
                    공휴일
                  </span>
                  <span class="legend-item legend-manual">
                    <span class="legend-dot"></span>
                    지정휴무
                  </span>
                  <span class="legend-item legend-recurring">
                    <span class="legend-dot"></span>
                    반복휴무
                  </span>
                </div>
              </div>
              <button
                type="button"
                class="btn-holiday-register"
                @click="fnHolidayRegisterOpen"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                휴일 등록
              </button>
            </div>
            <div class="calendar-nav-row">
              <div class="calendar-nav">
                <button type="button" class="btn-nav" @click="prevMonth">
                  &lt;
                </button>
                <span
                  class="calendar-month calendar-month-clickable"
                  @click="fnOpenMonthPicker"
                >
                  {{ displayYear }}년 {{ displayMonth }}월
                </span>
                <button type="button" class="btn-nav" @click="nextMonth">
                  &gt;
                </button>
              </div>
              <div class="calendar-options">
                <select v-model="displayFilter" class="select-display">
                  <option value="all">표시: 전체</option>
                  <option value="public">공휴일만</option>
                  <option value="manual">지정휴무만</option>
                  <option value="recurring">반복휴무만</option>
                </select>
                <!-- <div class="btn-group-toggle">
                  <button
                    type="button"
                    :class="['btn-toggle', { active: viewMode === 'month' }]"
                    @click="viewMode = 'month'"
                  >
                    월
                  </button>
                  <button
                    type="button"
                    :class="['btn-toggle', { active: viewMode === 'week' }]"
                    @click="viewMode = 'week'"
                  >
                    주
                  </button>
                </div> -->
              </div>
            </div>
          </div>
          <div class="calendar-body">
            <table class="calendar-table">
              <thead>
                <tr>
                  <th
                    v-for="d in dayLabels"
                    :key="d"
                    :class="dayHeaderClass(d)"
                  >
                    {{ d }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(week, wi) in calendarWeeks" :key="wi">
                  <td
                    v-for="(cell, ci) in week"
                    :key="ci"
                    :class="[
                      cellClass(cell),
                      {
                        'cell-selected': cell && selectedDate === cell.dateStr,
                      },
                    ]"
                    @click="cell && fnSelectDate(cell)"
                  >
                    <div v-if="cell" class="cell-inner">
                      <span class="cell-num">{{ cell.date }}</span>
                      <div class="cell-events">
                        <span
                          v-for="(ev, evIdx) in cell.events"
                          :key="ev.dateKey || `${cell.dateStr}-${evIdx}`"
                          :class="[
                            'ev-tag',
                            (ev.type ?? ev.holidayType) === '01'
                              ? 'ev-public'
                              : (ev.type ?? ev.holidayType) === '03'
                              ? 'ev-recurring'
                              : 'ev-manual',
                          ]"
                          :title="ev.name ?? ev.holidayNm ?? ''"
                          @click.stop="fnOpenHolidayEdit(ev)"
                        >
                          {{
                            ev.name ??
                            ev.holidayNm ??
                            ((ev.type ?? ev.holidayType) === "01"
                              ? "공휴일"
                              : (ev.type ?? ev.holidayType) === "03"
                              ? "반복휴무"
                              : ev.typeNm ?? "지정휴무")
                          }}
                        </span>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 우측: 목록/상세 패널 -->
        <div class="attd02-panel">
          <div class="panel-tabs">
            <button
              type="button"
              :class="['panel-tab', { active: activeTab === 'list' }]"
              @click="activeTab = 'list'"
            >
              목록
            </button>
            <button
              type="button"
              :class="['panel-tab', { active: activeTab === 'detail' }]"
              @click="activeTab = 'detail'"
            >
              상세
            </button>
          </div>

          <div class="panel-body">
            <!-- 목록 탭 -->
            <div v-show="activeTab === 'list'" class="panel-list">
              <div class="list-summary">
                총 {{ filteredList.length }}일 (공휴일 {{ publicCount }},
                지정휴무 {{ manualCount }}, 반복휴무 {{ recurringCount }})
              </div>
              <div class="list-filters">
                <button
                  type="button"
                  :class="['filter-btn', { active: listFilter === 'all' }]"
                  @click="listFilter = 'all'"
                >
                  전체
                </button>
                <button
                  type="button"
                  :class="['filter-btn', { active: listFilter === '공휴일' }]"
                  @click="listFilter = '공휴일'"
                >
                  공휴일
                </button>
                <button
                  type="button"
                  :class="['filter-btn', { active: listFilter === '지정휴무' }]"
                  @click="listFilter = '지정휴무'"
                >
                  지정휴무
                </button>
                <button
                  type="button"
                  :class="['filter-btn', { active: listFilter === '반복휴무' }]"
                  @click="listFilter = '반복휴무'"
                >
                  반복휴무
                </button>
              </div>
              <div class="list-scroll">
                <div
                  v-for="item in filteredList"
                  :key="item.dateKey"
                  class="list-row"
                >
                  <div class="list-left">
                    <span class="list-date"
                      >{{ item.dateFmt }} {{ item.dayOfWeek }}</span
                    >
                    <span class="list-name">- {{ item.name }}</span>
                  </div>
                  <div class="list-right">
                    <span class="list-type">{{ item.typeNm }}</span>
                    <button
                      v-if="item.type === '02'"
                      type="button"
                      class="btn-del"
                      @click="fnDeleteHoliday(item)"
                    >
                      삭제
                    </button>
                    <span v-else>-</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 상세 탭 -->
            <div v-show="activeTab === 'detail'" class="panel-detail">
              <template v-if="selectedDate">
                <div class="detail-date">{{ selectedDateLabel }}</div>
                <div v-if="selectedDateEvents.length > 0" class="detail-cards">
                  <div
                    v-for="(ev, evIdx) in selectedDateEvents"
                    :key="ev.dateKey || `detail-${evIdx}`"
                    class="detail-card"
                  >
                    <div class="card-header">
                      <span
                        :class="[
                          'card-tag',
                          (ev.type ?? ev.holidayType) === '01'
                            ? 'tag-public'
                            : (ev.type ?? ev.holidayType) === '03'
                            ? 'tag-recurring'
                            : 'tag-manual',
                        ]"
                      >
                        {{
                          ev.typeNm ??
                          ((ev.type ?? ev.holidayType) === "01"
                            ? "공휴일"
                            : (ev.type ?? ev.holidayType) === "03"
                            ? "반복휴무"
                            : "지정휴무")
                        }}
                      </span>
                      <button
                        v-if="(ev.type ?? ev.holidayType) === '02'"
                        type="button"
                        class="btn-del-card"
                        @click="fnDeleteHoliday(ev)"
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.5"
                        >
                          <path d="M3 6h18" />
                          <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
                          <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
                          <line x1="10" y1="11" x2="10" y2="17" />
                          <line x1="14" y1="11" x2="14" y2="17" />
                        </svg>
                        삭제
                      </button>
                    </div>
                    <h3 class="card-title">
                      {{ ev.name ?? ev.holidayNm ?? "-" }}
                    </h3>
                    <ul class="card-info">
                      <li>
                        등록 주체: {{ ev.insertNm ?? ev.regBy ?? "시스템" }}
                      </li>
                      <li>등록일시: {{ ev.insertDate ?? ev.regDt ?? "-" }}</li>
                    </ul>
                    <p
                      v-if="(ev.type ?? ev.holidayType) === '01'"
                      class="card-note"
                    >
                      국가 지정 공휴일은 삭제할 수 없습니다.
                    </p>
                  </div>
                </div>
                <div v-else class="detail-empty">
                  해당 날짜에 등록된 스케줄이 없습니다.
                </div>
              </template>
              <div v-else class="detail-empty">
                좌측 캘린더에서 날짜를 선택해주세요.
              </div>
            </div>
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
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import HolidayRegisterPop from "@/views/attd/popup/HolidayRegisterPop.vue";
import CalendarMonthPickerPop from "@/views/attd/popup/CalendarMonthPickerPop.vue";

defineOptions({ name: "Attd_02" });

const props = defineProps({ title: String, buttons: Object });
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

const now = new Date();
const displayYear = ref(now.getFullYear());
const displayMonth = ref(now.getMonth() + 1);
const displayFilter = ref("all");
/* eslint-disable-next-line no-unused-vars -- template에서 사용 */
const viewMode = ref("month");
const activeTab = ref("detail");
const listFilter = ref("all");
const selectedDate = ref("");
const holidayList = ref([]);

const dayLabels = ["일", "월", "화", "수", "목", "금", "토"];

const calendarWeeks = computed(() => {
  const y = displayYear.value;
  const m = displayMonth.value;
  const first = new Date(y, m - 1, 1);
  const last = new Date(y, m, 0);
  const startPad = first.getDay();
  const totalDays = last.getDate();
  const cells = [];
  for (let i = 0; i < startPad; i++) cells.push(null);
  for (let d = 1; d <= totalDays; d++) {
    const dt = new Date(y, m - 1, d);
    const dateStr = `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(
      2,
      "0"
    )}`;
    const dow = dt.getDay();
    const events = getEventsForDate(dateStr);
    cells.push({ date: d, dateStr, dayOfWeek: dayLabels[dow], dow, events });
  }
  const weeks = [];
  for (let i = 0; i < cells.length; i += 7) {
    const week = cells.slice(i, i + 7);
    while (week.length < 7) week.push(null);
    weeks.push(week);
  }
  return weeks;
});

const getEventsForDate = (dateStr) => {
  const list = holidayList.value;
  if (!Array.isArray(list)) return [];
  return list.filter((h) => {
    const hDate = h.date ?? h.holidayYmd ?? h.holidayDate;
    const d = typeof hDate === "string" ? hDate.slice(0, 10) : "";
    if (d !== dateStr) return false;
    const t = h.type ?? h.holidayType;
    if (displayFilter.value === "public" && t !== "01") return false;
    if (displayFilter.value === "manual" && t !== "02") return false;
    if (displayFilter.value === "recurring" && t !== "03") return false;
    return true;
  });
};

const filteredList = computed(() => {
  const list = holidayList.value;
  if (!Array.isArray(list)) return [];
  const arr = list.map((h) => {
    const type = h.type ?? h.holidayType ?? "02";
    const typeNm = h.typeNm ?? getTypeNm(type);
    const date = h.date ?? h.holidayYmd ?? h.holidayDate;
    const name = h.name ?? h.holidayNm ?? "-";
    return {
      ...h,
      dateFmt: formatDateShort(date),
      dayOfWeek: getDayOfWeek(date),
      name,
      type,
      typeNm,
      dateKey: h.dateKey ?? `${String(date ?? "").slice(0, 10)}-${name}`,
    };
  });
  return listFilter.value === "all"
    ? arr
    : arr.filter((x) => x.typeNm === listFilter.value);
});

const publicCount = computed(
  () =>
    holidayList.value.filter((h) => (h.type ?? h.holidayType) === "01").length
);
const manualCount = computed(
  () =>
    holidayList.value.filter((h) => (h.type ?? h.holidayType) === "02").length
);
const recurringCount = computed(
  () =>
    holidayList.value.filter((h) => (h.type ?? h.holidayType) === "03").length
);

const selectedDateEvents = computed(() =>
  selectedDate.value ? getEventsForDate(selectedDate.value) : []
);

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return "";
  const d = selectedDate.value;
  const parts = d.split("-");
  if (parts.length < 3) return d;
  const [y, m, day] = parts;
  const dt = new Date(parseInt(y), parseInt(m) - 1, parseInt(day));
  return isNaN(dt.getTime())
    ? d
    : `${y}-${m}-${day} (${dayLabels[dt.getDay()]})`;
});

const formatDateShort = (d) => {
  if (!d) return "-";
  const s = String(d).slice(0, 10);
  return s.length < 10 ? s : s.slice(5, 7) + "/" + s.slice(8, 10);
};

const getDayOfWeek = (d) => {
  if (!d) return "";
  const dt = new Date(String(d).slice(0, 10));
  return isNaN(dt.getTime()) ? "" : dayLabels[dt.getDay()];
};

const dayHeaderClass = (d) => {
  if (d === "일") return "hd-sun";
  if (d === "토") return "hd-sat";
  return "";
};

const cellClass = (cell) => {
  if (!cell) return "cell-empty";
  if (cell.dow === 0) return "cell-sun";
  if (cell.dow === 6) return "cell-sat";
  return "";
};

const fnSelectDate = (cell) => {
  selectedDate.value = cell.dateStr;
  activeTab.value = "detail";
};

const fnHolidayRegisterOpen = () => {
  openPop(HolidayRegisterPop, {
    siteCd_p: "",
    onSearch: fnSearch,
  });
};

const fnOpenMonthPicker = () => {
  openPop(CalendarMonthPickerPop, {
    year_p: displayYear.value,
    month_p: displayMonth.value,
    onConfirm: (y, m) => {
      displayYear.value = y;
      displayMonth.value = m;
      fnSearch();
    },
  });
};

const fnOpenHolidayEdit = (ev) => {
  const date = ev.date ?? ev.holidayYmd ?? ev.holidayDate ?? "";
  const dateStr = String(date).slice(0, 10);

  openPop(HolidayRegisterPop, {
    siteCd_p: "",
    onSearch: fnSearch,
    editData_p: {
      holidayId: ev.holidayId ?? "",
      holidayNm: ev.name ?? ev.holidayNm ?? "",
      holidayYmd: dateStr,
      holidayType: ev.type ?? ev.holidayType ?? "02",
      dateKey: ev.dateKey,
    },
  });
};

const prevMonth = () => {
  if (displayMonth.value <= 1) {
    displayMonth.value = 12;
    displayYear.value--;
  } else displayMonth.value--;
  fnSearch();
};

const nextMonth = () => {
  if (displayMonth.value >= 12) {
    displayMonth.value = 1;
    displayYear.value++;
  } else displayMonth.value++;
  fnSearch();
};

/** type → typeNm 매핑: 01=공휴일, 02=지정휴무 */
const getTypeNm = (type) =>
  type === "01"
    ? "공휴일"
    : type === "02"
    ? "지정휴무"
    : type === "03"
    ? "반복휴무"
    : type || "";

/** insertDate "20260223" → "2026-02-23" 포맷 */
const formatInsertDate = (v) => {
  if (!v) return "-";
  const s = String(v).replace(/\D/g, "");
  if (s.length >= 8) {
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  }
  return v;
};

/** API 응답 정규화: holidayYmd→date, holidayType→type, holidayNm→name */
const normalizeHolidayItem = (item) => {
  const type = item.holidayType ?? item.type ?? "02";
  const date = item.holidayYmd ?? item.date ?? item.holidayDate ?? "";
  const name = item.holidayNm ?? item.name ?? "-";
  const dateStr = String(date).slice(0, 10);
  return {
    ...item,
    holidayId: item.holidayId,
    date: dateStr,
    name,
    type,
    typeNm: item.typeNm ?? getTypeNm(type),
    insertNm: item.insertNm ?? "시스템",
    insertDate: formatInsertDate(item.insertDate ?? item.regDt),
    dateKey: dateStr ? `${dateStr}-${name}` : `${Date.now()}-${Math.random()}`,
  };
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/attd02/holiday-info-lists", {
      params: {
        year: displayYear.value,
        month: displayMonth.value,
      },
    });

    if (response.status === 200) {
      const data = response?.data?.holidayList;
      holidayList.value = Array.isArray(data)
        ? data.map(normalizeHolidayItem)
        : [];
    } else {
      holidayList.value = [];
    }
  } catch (err) {
    console.warn("[Attd02] holiday-list API error:", err);
    holidayList.value = [];
  }
};

const fnDeleteHoliday = async (item) => {
  const ok = await proxy.$confirm("해당 휴무를 삭제하시겠습니까?");
  if (!ok) return;

  try {
    const payload = {
      ...item,
      useYn: "N",
    };

    console.log("payload :: " + JSON.stringify(payload));

    const response = await axios.post(
      "/webApi/attd02/update-holiday-infos",
      payload
    );
    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 처리 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

onMounted(() => {
  fnButtonControll();
  fnSearch();
});
</script>

<style scoped>
.attd02-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.attd02-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 1rem;
  overflow: hidden;
}

/* 캘린더 */
.attd02-calendar {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 1rem;
  background: #fff;
  overflow: hidden;
}
.calendar-header {
  flex-shrink: 0;
}
.calendar-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}
.calendar-title-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.calendar-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-strong, #111827);
}
.calendar-legend {
  display: flex;
  gap: 1rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.legend-public .legend-dot {
  background: #9ca3af;
}
.legend-manual .legend-dot {
  background: #16a34a;
}
.legend-recurring .legend-dot {
  background: #2563eb;
}
.btn-holiday-register {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--color-primary, #16a34a);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}
.btn-holiday-register:hover {
  background: #15803d;
}
.btn-holiday-register:active {
  opacity: 0.9;
}
.btn-holiday-register svg {
  width: 18px;
  height: 18px;
}
.calendar-nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.calendar-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-nav {
  padding: 0.35rem 0.6rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
}
.calendar-month {
  font-weight: 600;
  font-size: 1rem;
  min-width: 7rem;
  text-align: center;
}
.calendar-month-clickable {
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: background 0.2s;
}
.calendar-month-clickable:hover {
  background: rgba(22, 163, 74, 0.08);
}
.calendar-options {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.select-display {
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 8px;
  font-size: 0.875rem;
}
.btn-group-toggle {
  display: flex;
  gap: 0.25rem;
}
/* .btn-toggle {
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.8125rem;
}
.btn-toggle.active {
  border-color: var(--color-primary, #16a34a);
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary);
} */
.calendar-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
}
.calendar-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
  table-layout: fixed;
}
.calendar-table th,
.calendar-table td {
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.35rem;
  vertical-align: top;
}
.calendar-table thead th {
  background: var(--color-bg, #f9fafb);
  font-weight: 600;
}
.hd-sun {
  color: #dc2626;
}
.hd-sat {
  color: #2563eb;
}
.cell-empty {
  background: #fafafa;
}
.cell-sun {
  color: #dc2626;
}
.cell-sat {
  color: #2563eb;
}
.cell-inner {
  min-height: 90px;
  height: 90px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
}
.cell-selected .cell-inner {
  outline: 2px solid var(--color-primary, #16a34a);
  outline-offset: 5px;
}
.cell-num {
  font-weight: 500;
  margin-bottom: 0.25rem;
  flex-shrink: 0;
}
.cell-events {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}
.ev-tag {
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.ev-public {
  background: #e5e7eb;
  color: #4b5563;
}
.ev-manual {
  background: #d1fae5;
  color: #065f46;
}
.ev-recurring {
  background: #dbeafe;
  color: #1e40af;
}

/* 우측 패널 */
.attd02-panel {
  width: 340px;
  flex-shrink: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}
.panel-tabs {
  display: flex;
  /* flex: 1; */
  width: 100%;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  flex-shrink: 0;
}
.panel-tab {
  flex: 1;
  padding: 0.6rem 1rem;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}
.panel-tab.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom: 2px solid var(--color-primary);
  margin-bottom: -1px;
}
.panel-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 1rem;
}
.panel-list,
.panel-detail {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.list-summary {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  margin-bottom: 0.5rem;
  flex-shrink: 0;
}
.list-filters {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
  flex-shrink: 0;
}
.filter-btn {
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.8125rem;
}
.filter-btn.active {
  background: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
  color: #fff;
}
.list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.list-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.list-left {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.list-date {
  font-size: 0.8125rem;
  font-weight: 500;
}
.list-name {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}
.list-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}
.list-type {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
}
.btn-del {
  font-size: 0.75rem;
  color: #dc2626;
  background: none;
  border: none;
  cursor: pointer;
}

/* 상세 */
.detail-date {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  margin-bottom: 0.75rem;
  flex-shrink: 0;
}
.detail-cards {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.detail-card {
  padding: 1rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.card-tag {
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
}
.tag-public {
  background: #e5e7eb;
  color: #4b5563;
}
.tag-manual {
  background: #d1fae5;
  color: #065f46;
}
.tag-recurring {
  background: #dbeafe;
  color: #1e40af;
}
.btn-del-card {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background: none;
  border: none;
  font-size: 0.8125rem;
  color: #dc2626;
  cursor: pointer;
}
.btn-del-card:hover {
  color: #b91c1c;
  text-decoration: underline;
}
.btn-del-card svg {
  width: 16px;
  height: 16px;
}
.card-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.5rem;
  color: var(--color-text-strong, #111827);
}
.card-info {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  line-height: 1.6;
}
.card-note {
  margin: 0.5rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
}
.detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}
</style>
