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
        <input v-model.trim="sr_codeNm" type="text" />
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
          <span class="subtitle-text">순회점검 구분</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
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
              <tr
                v-for="(code, idx) in (baseCodeArr['COM001'] || []).filter(
                  (o) => o.baimValDCd != null
                )"
                :key="code.baimValDCd"
                :value="code.baimValDCd"
              >
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td @dblclick="fnSubSearch(code)">{{ code.baimValDNm }}</td>
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
            <span class="subtitle-text">점검문항</span>
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
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid">
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
                <th style="width: 15%">정렬순서</th>
                <th class="editableCell">점검항목명</th>
                <th style="width: 8%">사용여부</th>
                <th class="editableCell" style="width: 8%">시행월</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, idx) in chkptInspectItemList"
                :key="item.inspectItemCd"
              >
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td>
                  <input type="checkbox" v-model="item.chk" />
                </td>
                <td>
                  <input
                    id="sortIdx"
                    v-model="item.sortIdx"
                    @blur="focusKill(item.sortIdx, idx)"
                  />
                </td>
                <td>
                  <input style="width: 100%" v-model="item.inspectItemSubj" />
                </td>
                <td>
                  <BaseSelect
                    v-model="item.useYn"
                    :readonly="true"
                    name="codeDetailSrc"
                  >
                    <option
                      v-for="opt in (systCodeArr['SYS003'] || []).filter(
                        (o) => o.systValDCd != null
                      )"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </BaseSelect>
                </td>
                <td>
                  <CalendarSrchMonth
                    :range="false"
                    style="width: 150px"
                    v-model="item.strDate"
                  />
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
import CalendarSrchMonth from '@/components/common/CalendarSrchMonth';

defineOptions({ name: 'ChkLst_02' });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const chkptInspectItemList = ref([]);
const systCodeArr = ref({});
const baseCodeArr = ref({});
const localButtons = ref({ ...props.buttons });

/* 조회조건 변수 세팅 */
const sr_codeNm = ref('');

const targetValCd = ref('');
const targetValNm = ref('');

const headChk = ref(false);

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

useFieldWatcher(
  chkptInspectItemList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// focusKill 이벤트
function focusKill(value, idx) {
  // if (e.target.id == "sortIdx") {
  if (proxy.$util.isNotEmpty(value) && !proxy.$util.isInteger(value)) {
    chkptInspectItemList.value[idx].sortIdx = idx + 1;
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
  chkptInspectItemList.value = [];

  try {
    const response = await axios.get('/comApi/baseinfo/base-info-list', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        baseCodeList: ['COM001'],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseCodeArr.value = grouped;
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
    targetValCd.value = code.baimValDCd;
    targetValNm.value = code.baimValDNm;
  }

  chkptInspectItemList.value = [];

  try {
    const response = await axios.post(
      '/webApi/chkLst02/getChkptInspectItemList',
      {
        sr_codeCd: targetValCd.value,
      }
    );

    if (response.status === 200) {
      chkptInspectItemList.value = response.data;
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
      '/webApi/chkLst02/updateChkptInspectItemList',
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
      '/webApi/chkLst02/deleteChkptInspectItemList',
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
  chkptInspectItemList.value.forEach((item) => {
    item.chk = headChk.value;
  });
}

function fnAddRow() {
  if (proxy.$util.isNotEmpty(targetValCd.value)) {
    chkptInspectItemList.value.push({
      chk: true,
      cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      chkLstType: targetValCd.value,
      sortIdx: chkptInspectItemList.value.length + 1,
      useYn: 'Y',
      strDate: proxy.$util.getToday(),
    });
  }
}

function fnSaveRow() {
  const filteredData = chkptInspectItemList.value.filter((chkpt) => chkpt.chk);
  //  const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert('저장할 데이터가 없습니다.');
    return;
  }

  fnSave(filteredData);
}

function fnDeleteRow() {
  const filteredData = chkptInspectItemList.value.filter(
    (chkpt) => chkpt.chk && chkpt.inspectItemCd
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
