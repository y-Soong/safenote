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
                <th style="width: 10%">연차코드</th>
                <th style="width: 8%">연차명</th>
                <th style="width: 10%">연차타입</th>
                <th style="width: 8%">유급구분</th>
                <th style="width: 8%">휴가성격</th>
                <th style="width: 10%">기본일수/부여일수</th>
                <th style="width: 7%">사용단위</th>
                <th style="width: 11%">사용 가능기간</th>
                <th style="width: 8%">사용여부</th>
                <th style="width: 8%">자동부여 기준일</th>
                <th style="width: 6%">실행시점</th>
                <th style="width: 6%">결재여부</th>
                <th style="width: 6%">결재단계</th>
                <th style="width: 8%">인사팀승인</th>
                <th style="width: 6%">증빙여부</th>
                <th>비고</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!leaveList || leaveList.length === 0">
                <tr>
                  <td colspan="17" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in leaveList"
                  :key="row.leaveNo || idx"
                  class="row-clickable"
                  @dblclick="fnOpenEdit(row)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.leaveNo }}</td>
                  <td>{{ row.leaveNm }}</td>
                  <td>{{ row.leaveTypeNm }}</td>
                  <td>{{ row.paidTypeNm }}</td>
                  <td>{{ row.leaveNatureTypeNm }}</td>
                  <td>{{ row.leaveDays || "-" }}</td>
                  <td>{{ row.useUnitTypeNm || "-" }}</td>
                  <td>{{ row.availTermTypeNm || "-" }}</td>
                  <td>{{ row.useYn === "Y" ? "사용" : "미사용" }}</td>
                  <td>{{ row.grantBaseTypeNm || row.grantBaseType || "-" }}</td>
                  <td>
                    {{
                      row.grantOffsetMonth != null
                        ? row.grantOffsetMonth + "개월"
                        : "-"
                    }}
                  </td>
                  <td>{{ row.aprvUseYn === "Y" ? "사용" : "미사용" }}</td>
                  <td>{{ row.aprvStepCnt ?? "-" }}</td>
                  <td>{{ row.hrFinalAprvYn === "Y" ? "예" : "아니오" }}</td>
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
import LeaveTypeCreatePop from "@/views/attd/popup/LeaveTypeCreatePop.vue";

defineOptions({ name: "Attd_03" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
const leaveList = ref([]);
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "시스템코드 조회 중 오류가 발생했습니다.";
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
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
