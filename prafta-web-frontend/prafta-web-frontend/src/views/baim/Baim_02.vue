<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!--
    @search="fnSearch"
    @create="fnCreate"
    @save="fnSave"
    -->

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>코드명</label>
        <input v-model.trim="codeNm" type="text" />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- LEFT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 20%">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">기초 코드 리스트</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th class="w-30">코드명</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(code, idx) in cmmCodeList" :key="code.baimValCd">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td @dblclick="fnSubSearch(code)">{{ code.baimValNm }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">상세 코드 리스트</span>
          </div>

          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-text">[{{ targetValNm }}]</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnAddRow()">생성</button>
            <button class="btn btn-custom" @click="fnSaveRow()">저장</button>
            <button class="btn btn-custom" @click="fnDeleteRow()">삭제</button>
          </div>
        </div>

        <div
          class="table-box"
          style="
            --box-h: 65vh;
            --box-sticky-top: 1px;
            --box-ox: auto;
            width: 100%;
          "
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChk"
                    type="checkbox"
                    :checked="headChk"
                    @change="fnHeadChk"
                  />
                </th>
                <th style="width: 15%">상세코드</th>
                <th class="editableCell" style="width: 15%">상세코드명</th>
                <th class="editableCell" style="width: 8%">
                  {{ targetValCd === "COM005" ? "권한등급" : "코드순번" }}
                </th>
                <th class="editableCell">비고</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!cmmCodeDetailList || cmmCodeDetailList.length === 0"
              >
                <tr>
                  <td colspan="6" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(codeDetail, idx) in cmmCodeDetailList"
                  :key="codeDetail.baimValDCd"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="codeDetail.chk"
                      :disabled="codeDetail.valDInfo1 === 'system'"
                    />
                  </td>
                  <td>{{ codeDetail.baimValDCd }}</td>
                  <td>
                    <input
                      v-model="codeDetail.baimValDNm"
                      :disabled="
                        codeDetail.valDInfo1 === 'system' ||
                        targetValCd === 'COM006'
                      "
                    />
                  </td>
                  <td>
                    <input
                      id="sortIdx"
                      type="text"
                      inputmode="numeric"
                      autocomplete="off"
                      :value="codeDetail.sortIdx"
                      :disabled="codeDetail.valDInfo1 === 'system'"
                      @input="sanitizeSortIdxInput($event, codeDetail)"
                      @blur="focusKill(codeDetail, idx)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="codeDetail.valDDesc"
                      :disabled="codeDetail.valDInfo1 === 'system'"
                    />
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
/* eslint-disable */

// ================ Imports ================
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import BaseSelect from "@/components/common/BaseSelect.vue";

// ================ Options ================
defineOptions({ name: "Baim_02" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });
const cmmCodeList = ref([]);
const cmmCodeDetailList = ref([]);
const systCodeArr = ref({});

// 조회조건
const codeNm = ref("");

// 좌측 선택 코드(상세 그리드 기준)
const targetValCd = ref("");
const targetValNm = ref("");

const headChk = ref(false);

// ================ Field watcher ================
useFieldWatcher(
  cmmCodeDetailList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ COM005 권한등급(코드순번) 유틸 ================

const isCom005SortIdxValid = (n) => {
  return Number.isInteger(n) && n >= 3 && n <= 99998;
};

// ================ Life Cycle Functions ================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;
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
  cmmCodeList.value = [];
  cmmCodeDetailList.value = [];
  headChk.value = false;

  try {
    const response = await axios.get("/webApi/baim02/comp-cmm-code-m-list", {
      params: {
        codeNm: codeNm.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      cmmCodeList.value = response.data.compCmmCodeMList;
      console.log(cmmCodeList.value);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (code) => {
  if (proxy.$util.isNotEmpty(code)) {
    targetValCd.value = code.baimValCd;
    targetValNm.value = code.baimValNm;
  }

  cmmCodeDetailList.value = [];
  headChk.value = false;

  try {
    const response = await axios.get("/webApi/baim02/comp-cmm-code-d-list", {
      params: {
        codeCd: targetValCd.value,
      },
    });

    if (response.status === 200) {
      cmmCodeDetailList.value = response.data.compCmmCodeDList || [];
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSave = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim02/update-cmm-code-detail-info",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSubSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnDelete = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim02/deleteCmmCodeDetailInfo",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSubSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
function fnButtonControll() {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

function fnHeadChk(e) {
  const checked = e.target.checked;
  headChk.value = checked;
  cmmCodeDetailList.value.forEach((item) => {
    if (item.valDInfo1 !== "system") {
      item.chk = checked;
    }
  });
}

/** 코드순번·권한등급: 숫자만 입력 */
function sanitizeSortIdxInput(e, codeDetail) {
  if (codeDetail.valDInfo1 === "system") return;
  const raw = e.target.value ?? "";
  const digitsOnly = String(raw).replace(/\D/g, "");
  codeDetail.sortIdx = digitsOnly === "" ? "" : digitsOnly;
}

/** 상세 그리드 코드순번(권한등급) blur */
const focusKill = async (codeDetail, idx) => {
  const raw = codeDetail.sortIdx;
  const strVal = raw == null ? "" : String(raw).trim();

  if (targetValCd.value === "COM005" && codeDetail.valDInfo1 !== "system") {
    const n = parseInt(strVal, 10);
    if (strVal === "" || Number.isNaN(n) || !isCom005SortIdxValid(n)) {
      await proxy.$alert(getMessage(MSG.COM005_SORT_IDX_RANGE));
      cmmCodeDetailList.value[idx].sortIdx = 3;
      return;
    }
    cmmCodeDetailList.value[idx].sortIdx = n;
    return;
  }

  if (proxy.$util.isNotEmpty(strVal) && !proxy.$util.isInteger(strVal)) {
    cmmCodeDetailList.value[idx].sortIdx = idx + 1;
  }
};

function fnAddRow() {
  const nextIdx = cmmCodeDetailList.value.length + 1;
  const newRow = {
    chk: true,
    useYn: "Y",
    baimValCd: targetValCd.value,
  };
  if (targetValCd.value === "COM006") {
    newRow.baimValDNm = `${nextIdx}일`;
  }
  cmmCodeDetailList.value.push(newRow);
}

async function fnSaveRow() {
  const filteredData = cmmCodeDetailList.value.filter(
    (cmmCode) => cmmCode.chk && cmmCode.baimValDNm
  );
  //  const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  if (targetValCd.value === "COM005") {
    const invalid = filteredData.find(
      (row) =>
        row.valDInfo1 !== "system" &&
        !isCom005SortIdxValid(
          parseInt(String(row.sortIdx ?? row.SORT_IDX ?? "").trim(), 10)
        )
    );
    if (invalid) {
      await proxy.$alert(getMessage(MSG.COM005_SORT_IDX_RANGE));
      return;
    }
  }

  fnSave(filteredData);
}

function fnDeleteRow() {
  const filteredData = cmmCodeDetailList.value.filter(
    (cmmCode) => cmmCode.chk && cmmCode.baimValDNm
  );
  // const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  fnDelete(filteredData);
}
</script>

<style scoped></style>
