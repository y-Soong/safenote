<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewSearch">
      <div>
        <label>교대타입코드</label>
        <input v-model.trim="shiftTypeCd" type="text" placeholder="검색" />
      </div>
      <div>
        <label>교대일수</label>
        <select v-model="shiftDays" name="combo">
          <option value="">전체</option>
          <option v-for="n in 7" :key="n" :value="String(n)">{{ n }}일</option>
        </select>
      </div>
      <div>
        <label>사용여부</label>
        <select v-model="useYn" name="combo">
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

    <div class="attd01-2-info-banner">
      <span class="info-icon">ℹ</span>
      <span
        >교대근무는 지정한 교대일수 동안 스케줄 타입을 순환 적용합니다.</span
      >
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">교대근무 타입 리스트</span>
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
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @change="fnHeadChk"
                  />
                </th>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th class="editableCell" style="width: 10%">타입코드</th>
                <th class="editableCell" style="width: 8%">교대일수</th>
                <th class="editableCell" style="width: 35%">패턴요약</th>
                <th class="editableCell" style="width: 10%">등록사용자수</th>
                <th style="width: 12%">사용여부</th>
                <th class="editableCell" style="width: 8%">변경이력</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!shiftList || shiftList.length === 0">
                <tr>
                  <td colspan="8" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in shiftList"
                  :key="row.shiftTypeCd || idx"
                  class="row-clickable"
                >
                  <td style="text-align: center" @click.stop>
                    <input
                      v-model="row.chk"
                      type="checkbox"
                      @change="fnRowChk"
                    />
                  </td>
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <span
                      class="type-code-link"
                      @click="fnShiftTypeDetail(row)"
                    >
                      {{ row.shiftTypeCd }}
                    </span>
                  </td>
                  <td>{{ row.shiftDays }}일</td>
                  <td class="pattern-cell" :title="row.patternSummary">
                    {{ row.patternSummary }}
                  </td>
                  <td>{{ row.regUserCnt }}명</td>
                  <td>
                    <select
                      v-model="row.useYn"
                      class="use-yn-select"
                      @change="fnUseYnChange(row)"
                    >
                      <option
                        v-for="opt in systCodeArr['SYS003'] || []"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </select>
                  </td>
                  <td style="text-align: center" @click.stop>
                    <button
                      type="button"
                      class="btn-history-icon"
                      title="변경이력"
                      @click="fnChangeHistOpen(row)"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path
                          d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                        />
                        <polyline points="14,2 14,8 20,8" />
                        <path d="M8 12h8" />
                        <path d="M8 16h8" />
                        <path d="M8 10h4" />
                      </svg>
                    </button>
                  </td>
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

defineOptions({ name: "Attd_01_2" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();

const localButtons = ref({ ...props.buttons });
const shiftList = ref([]);
const systCodeArr = ref([]);
const headChk = ref(false);

const shiftTypeCd = ref("");
const shiftDays = ref("");
const useYn = ref("");

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  fnSearch();
});

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: { systCodeList: ["SYS003"] },
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
      useYn.value = systCodeArr.value.SYS003?.[0]?.systValDCd ?? "";
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    // TODO: API 연동 시 주석 해제
    // const response = await axios.get("/webApi/attd01/shift-type-list", {
    //   params: {
    //     shiftTypeCd: shiftTypeCd.value,
    //     shiftDays: shiftDays.value,
    //     useYn: useYn.value,
    //   },
    // });
    // shiftList.value = response.data?.list ?? [];
    shiftList.value = getMockShiftList();
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const getMockShiftList = () => [
  {
    shiftTypeCd: "SH001",
    shiftDays: 2,
    patternSummary: "ST001 (09:00~18:00) / ST002(22:00~06:00)",
    regUserCnt: 45,
    useYn: "Y",
    chk: false,
  },
  {
    shiftTypeCd: "SH002",
    shiftDays: 3,
    patternSummary: "ST024(20:00~05:00) / OFF / OFF",
    regUserCnt: 28,
    useYn: "Y",
    chk: false,
  },
  {
    shiftTypeCd: "SH003",
    shiftDays: 3,
    patternSummary: "ST001 (09:00~18:00) / ST002(22:00~06:00) / OFF",
    regUserCnt: 0,
    useYn: "Y",
    chk: false,
  },
  {
    shiftTypeCd: "SH004",
    shiftDays: 4,
    patternSummary:
      "ST001 (09:00~18:00) / ST002(22:00~06:00) / ST024(20:00~05:00) / OFF",
    regUserCnt: 63,
    useYn: "Y",
    chk: false,
  },
  {
    shiftTypeCd: "SH005",
    shiftDays: 7,
    patternSummary: "ST001 (09:00~18:00)×5 / OFF×2",
    regUserCnt: 12,
    useYn: "Y",
    chk: false,
  },
  {
    shiftTypeCd: "SH006",
    shiftDays: 5,
    patternSummary:
      "ST001 (09:00~18:00) / ST004(10:00~19:00) / ST001 (09:00~18:00) / ST002(22:00~06:00) / OFF",
    regUserCnt: 0,
    useYn: "N",
    chk: false,
  },
];

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnHeadChk = () => {
  shiftList.value.forEach((row) => (row.chk = headChk.value));
};

const fnRowChk = () => {
  headChk.value =
    shiftList.value.length > 0 && shiftList.value.every((row) => row.chk);
};

const fnShiftTypeDetail = (row) => {
  // TODO: 교대타입 상세 팝업/화면
  proxy.$alert(`타입코드 ${row.shiftTypeCd} 상세 기능은 준비 중입니다.`);
};

const fnUseYnChange = async (row) => {
  const ok = await proxy.$confirm(
    `사용여부를 변경하시겠습니까? (${row.shiftTypeCd})`
  );
  if (!ok) {
    row.useYn = row.useYn === "Y" ? "N" : "Y";
    return;
  }
  try {
    // TODO: API 연동
    // await axios.post("/webApi/attd01/update-shift-use-yn", {
    //   shiftTypeCd: row.shiftTypeCd,
    //   useYn: row.useYn,
    // });
    proxy.$alert("변경되었습니다.");
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "변경 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
    row.useYn = row.useYn === "Y" ? "N" : "Y";
  }
};

const fnChangeHistOpen = (row) => {
  // TODO: 교대근무 타입 변경이력 팝업
  proxy.$alert(`타입코드 ${row.shiftTypeCd} 변경이력 팝업은 준비 중입니다.`);
};
</script>

<style scoped>
.attd01-2-info-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: #d1fae5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  color: #065f46;
  font-size: 0.875rem;
}
.attd01-2-info-banner .info-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  background: #10b981;
  color: #fff;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}
.type-code-link {
  color: #16a34a;
  font-weight: 500;
  cursor: pointer;
}
.type-code-link:hover {
  text-decoration: underline;
}
.pattern-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.use-yn-select {
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  font-size: 0.8125rem;
  min-width: 6rem;
}
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
  transition: color 0.2s, background 0.2s, border-color 0.2s;
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
