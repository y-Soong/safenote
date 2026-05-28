<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnCreate"
    />

    <div class="viewSearch">
      <div>
        <label>연차코드</label>
        <input v-model.trim="leaveNo" type="text" />
      </div>
      <div>
        <label>연차명</label>
        <input v-model.trim="leaveNm" type="text" />
      </div>
      <div>
        <label>연차타입</label>
        <select v-model.trim="leaveType" name="combo">
          <option
            v-for="opt in systCodeArr['SYS021'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>
      <div>
        <label>사용여부</label>
        <select v-model.trim="useYn" name="combo">
          <option
            v-for="opt in systCodeArr['SYS003'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">연차 타입 리스트</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 3%">
                  No
                </th>
                <ThSortable
                  label="연차코드"
                  col-key="leaveNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="연차명"
                  col-key="leaveNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="연차타입"
                  col-key="leaveTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="수동여부"
                  col-key="grantType"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.grantType"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="유급구분"
                  col-key="paidTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.paidTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="휴가성격"
                  col-key="leaveNatureTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveNatureTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="기본일수/부여일수"
                  col-key="leaveDays"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveDays"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용단위"
                  col-key="useUnitTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useUnitTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용 가능기간"
                  col-key="availTermTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.availTermTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용여부"
                  col-key="useYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="자동부여 기준일"
                  col-key="grantBaseTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.grantBaseTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="실행시점"
                  col-key="grantOffsetMonth"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.grantOffsetMonth"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="결재여부"
                  col-key="aprvUseYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.aprvUseYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="증빙여부"
                  col-key="evidenceYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.evidenceYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="비고"
                  col-key="leaveDesc"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.leaveDesc"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template v-if="!leaveList || leaveList.length === 0">
                <tr>
                  <td colspan="16" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in sortedData"
                  :key="row.leaveNo || idx"
                  class="row-clickable"
                  @dblclick="fnOpenEdit(row)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.leaveNo }}</td>
                  <td>{{ row.leaveNm }}</td>
                  <td>{{ row.leaveTypeNm }}</td>
                  <td>
                    {{
                      row.grantType === "02"
                        ? "수동"
                        : row.grantType === "01"
                        ? "자동"
                        : "-"
                    }}
                  </td>
                  <td>{{ row.paidTypeNm }}</td>
                  <td>{{ row.leaveNatureTypeNm }}</td>
                  <td>{{ row.leaveDays || "-" }}</td>
                  <td>{{ row.useUnitTypeNm || "-" }}</td>
                  <td>{{ row.availTermTypeNm || "-" }}</td>
                  <td>{{ row.useYn === "Y" ? "사용" : "미사용" }}</td>
                  <td>{{ row.grantBaseTypeNm || row.grantBaseType || "-" }}</td>
                  <td>
                    {{ formatExecTime(row) }}
                  </td>
                  <td>{{ row.aprvUseYn === "Y" ? "사용" : "미사용" }}</td>
                  <td>{{ row.evidenceYn === "Y" ? "사용" : "미사용" }}</td>
                  <td>{{ row.leaveDesc || "-" }}</td>
                </tr>
              </template>
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
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import { resolveApiErrorMessage } from "@/utils/apiError";
import LeaveTypeCreatePop from "@/views/attd/popup/LeaveTypeCreatePop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

defineOptions({ name: "Attd_03" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
const leaveList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(leaveList);
const { colWidths, onResize } = useColumnResize({
  leaveNo: 110,
  leaveNm: 100,
  leaveTypeNm: 110,
  grantType: 90,
  paidTypeNm: 90,
  leaveNatureTypeNm: 90,
  leaveDays: 110,
  useUnitTypeNm: 90,
  availTermTypeNm: 110,
  useYn: 80,
  grantBaseTypeNm: 110,
  grantOffsetMonth: 80,
  aprvUseYn: 80,
  evidenceYn: 80,
  leaveDesc: 120,
});
const systCodeArr = ref([]);
const leaveNo = ref("");
const leaveNm = ref("");
const leaveType = ref("");
const useYn = ref("");

const fnButtonControll = () => {
  localButtons.value.search = "Y";
  localButtons.value.create = "Y";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
  localButtons.value.save = "N";
};

// 실행시점 컬럼 포맷
// grantBaseType='03'(부여일지정) → MM-DD, 그 외 → n개월
const formatExecTime = (row) => {
  if (row?.grantBaseType === "03") {
    const v = String(row?.grantAssignMmdd || "").replace(/\D/g, "");
    if (v.length >= 4) return `${v.slice(0, 2)}-${v.slice(2, 4)}`;
    return "-";
  }
  return row?.grantOffsetMonth != null ? row.grantOffsetMonth + "개월" : "-";
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS003", "SYS021"] },
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
      useYn.value = systCodeArr.value.SYS003?.[0]?.systValDCd;
      leaveType.value = systCodeArr.value.SYS021?.[0]?.systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "시스템코드 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    // TODO: API 연동
    const response = await axios.get("/webApi/attd03/leave-type-lists", {
      params: {
        leaveNo: leaveNo.value,
        leaveNm: leaveNm.value,
        leaveType: leaveType.value,
        useYn: useYn.value,
      },
    });
    if (response.status === 200) {
      leaveList.value = response.data?.leaveTypeResultList ?? [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnCreate = () => {
  openPop(LeaveTypeCreatePop, {
    onSearch: fnSearch,
  });
};

const fnOpenEdit = (row) => {
  openPop(LeaveTypeCreatePop, {
    onSearch: fnSearch,
    editRow: row,
  });
};

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
});
</script>

<style scoped>
.row-clickable {
  cursor: default;
}

.btn-history-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem;
  background: transparent;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}

.btn-history-icon:hover {
  color: #30796a;
  background: rgba(48, 121, 106, 0.08);
  border-color: #30796a;
}

.btn-history-icon svg {
  width: 18px;
  height: 18px;
}
</style>
