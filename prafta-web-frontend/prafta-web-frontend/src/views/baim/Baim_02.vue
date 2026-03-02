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
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <th style="width: 15%">상세코드</th>
                <th class="editableCell" style="width: 15%">상세코드명</th>
                <th class="editableCell" style="width: 8%">코드순번</th>
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
                      v-model="codeDetail.sortIdx"
                      :disabled="codeDetail.valDInfo1 === 'system'"
                      @blur="focusKill(codeDetail.SORT_IDX, idx)"
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
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from 'vue';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';

defineOptions({ name: 'Baim_02' });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const cmmCodeList = ref([]);
const cmmCodeDetailList = ref([]);
const systCodeArr = ref({});
const localButtons = ref({ ...props.buttons });

/* 조회조건 변수 세팅 */
const codeNm = ref('');

const targetValCd = ref('');
const targetValNm = ref('');

const headChk = ref(false);

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

useFieldWatcher(
  cmmCodeDetailList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// focusKill 이벤트
function focusKill(value, idx) {
  // if (e.target.id == "sortIdx") {
  if (proxy.$util.isNotEmpty(value) && !proxy.$util.isInteger(value)) {
    cmmCodeDetailList.value[idx].sortIdx = idx + 1;
  }
  // }
}

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/syst-info-list', {
      params: {
        systCodeList: ['SYS003'],
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
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  cmmCodeList.value = [];
  cmmCodeDetailList.value = [];

  try {
    const response = await axios.get('/webApi/baim02/comp-cmm-code-m-list', {
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
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (code) => {
  if (proxy.$util.isNotEmpty(code)) {
    targetValCd.value = code.baimValCd;
    targetValNm.value = code.baimValNm;
  }

  cmmCodeDetailList.value = [];

  try {
    const response = await axios.get('/webApi/baim02/comp-cmm-code-d-list', {
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
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnSave = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert('저장할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('저장하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/baim02/update-cmm-code-detail-info',
      dataList
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSubSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '저장 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnDelete = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert('삭제할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('삭제하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/baim02/deleteCmmCodeDetailInfo',
      dataList
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSubSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '삭제 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

/* user function */
function fnButtonControll() {
  // localButtons.value.search = "N";
  localButtons.value.create = 'N';
  localButtons.value.save = 'N';
  localButtons.value.delete = 'N';
  localButtons.value.excel = 'N';
}

function fnHeadChk() {
  headChk.value = !headChk.value;
  cmmCodeDetailList.value.forEach((item) => {
    item.chk = headChk.value;
  });
}

function fnAddRow() {
  const nextIdx = cmmCodeDetailList.value.length + 1;
  const newRow = {
    chk: true,
    useYn: 'Y',
    baimValCd: targetValCd.value,
  };
  if (targetValCd.value === 'COM006') {
    newRow.baimValDNm = `${nextIdx}일`;
  }
  cmmCodeDetailList.value.push(newRow);
}

function fnSaveRow() {
  const filteredData = cmmCodeDetailList.value.filter(
    (cmmCode) => cmmCode.chk && cmmCode.baimValDNm
  );
  //  const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert('저장할 데이터가 없습니다.');
    return;
  }

  fnSave(filteredData);
}

function fnDeleteRow() {
  const filteredData = cmmCodeDetailList.value.filter(
    (cmmCode) => cmmCode.chk && cmmCode.baimValDNm
  );
  // const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert('삭제할 데이터가 없습니다.');
    return;
  }

  fnDelete(filteredData);
}
</script>

<style scoped></style>
