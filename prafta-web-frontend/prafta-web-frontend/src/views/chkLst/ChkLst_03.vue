<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
      @excel="fnExcel" -->

    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="formData.siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen('searchForm')"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="formData.siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>점검대상명칭</label>
        <input v-model.trim="formData.chkptNm" type="text" />
      </div>

      <div>
        <label>순회점검구분</label>
        <select v-model="formData.chkLstType" name="combo">
          <option
            v-for="opt in baseCodeArr['COM001'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>점검시행월</label>
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="formData.fromDate"
        />
        -
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="formData.toDate"
        />
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">점검결과 리스트</span>
          </div>
          <div class="custom-btn-area">
            <button
              class="btn btn-custom"
              @click="fnSelectedChkLstRstPopOpen()"
            >
              점검일지 조회
            </button>
          </div>
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
                <th class="editableCell" style="width: 13%">사업장</th>
                <th class="editableCell" style="width: 15%">점검대상명칭</th>
                <th class="editableCell" style="width: 10%">점검구분</th>
                <th class="editableCell" style="width: 10%">점검시행행월</th>
                <th class="editableCell" style="width: 10%">관리자</th>
                <th class="editableCell" style="width: 10%">점검시행일수</th>
                <th class="editableCell" style="width: 10%">불량항목 수</th>
                <th class="editableCell">점검결과확인</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(chkptResult, idx) in chkptResultList"
                :key="chkptResult.siteCd"
              >
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td>
                  <input type="checkbox" v-model="chkptResult.chk" />
                </td>
                <td>{{ chkptResult.siteNm }}</td>
                <td>{{ chkptResult.chkptNm }}</td>
                <td>
                  <BaseSelect
                    v-model="chkptResult.chkLstType"
                    :readonly="true"
                    name="codeDetailSrc"
                  >
                    <option
                      v-for="opt in (baseCodeArr['COM001'] || []).filter(
                        (o) => o.baimValDCd != null
                      )"
                      :key="opt.baimValDCd"
                      :value="opt.baimValDCd"
                    >
                      {{ opt.baimValDNm }}
                    </option>
                  </BaseSelect>
                </td>
                <!-- <td>
                  {{ chkptResult.chkLstType }}
                </td> -->
                <td>{{ chkptResult.workDate }}</td>
                <td>{{ chkptResult.siteAdminNm }}</td>
                <td>{{ chkptResult.inspectDayCnt }}</td>
                <td>{{ chkptResult.defectiveResultCnt }}</td>
                <td>
                  <div class="flex items-center gap-2 w-full">
                    <button
                      class="border rounded"
                      style="
                        background-color: #30796a;
                        border: none;
                        padding: 0.2rem 0.5rem;
                        color: #fff; /* ← 글자색 흰색 */
                      "
                      @click="fnChkLstRstPopOpen(chkptResult)"
                    >
                      점검일지
                    </button>
                  </div>
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
  reactive,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
  nextTick,
  watch,
} from 'vue';
import { useModal } from '@/utils/useModal';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import search_icon from '@/assets/img/search_icon.png';
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';
import CalendarSrchMonth from '@/components/common/CalendarSrchMonth.vue';
import ChkLstRstPop from '@/views/chkLst/popup/ChkLstRstPop.vue';

// =========================== Define ===========================
defineOptions({ name: 'ChkLst_03' });
const props = defineProps({
  buttons: Object,
  title: Object,
});

// =========================== Reactive ===========================
const formData = reactive({
  fromDate: '',
  toDate: '',
  siteCd: '',
  siteNo: '',
  siteNm: '',
  chkLstType: '',
  chkptNm: '',
});

// =========================== Ref ===========================
const chkptResultList = ref([]);
const baseCodeArr = ref([]);
const SiteSearchPopOpen = ref(false);
const headChk = ref(false);
const siteDisabled = ref(false);

// =========================== Data ===========================
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();
// 날짜 자동 조정 플래그 (무한 루프 방지)
let isAdjustingDate = false;

// =========================== Life Cycle ===========================
onMounted(async () => {
  initializeFormData();
  // await fnGetSystinfoList();
  await fnGetBaseinfoList();
  // await fnSearch();
  // formData.workDate
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  chkptResultList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// fromDate 변경 감시 - toDate보다 클 경우 toDate를 fromDate의 한달 후로 세팅
watch(
  () => formData.fromDate,
  (newFromDate) => {
    if (isAdjustingDate || !newFromDate || !formData.toDate) return;

    const fromDateParsed = parseDate(newFromDate);
    const toDateParsed = parseDate(formData.toDate);

    if (!fromDateParsed || !toDateParsed) return;

    // fromDate가 toDate보다 클 경우, toDate를 fromDate의 한달 후로 세팅
    if (fromDateParsed > toDateParsed) {
      isAdjustingDate = true;
      const adjustedDate = new Date(
        fromDateParsed.getFullYear(),
        fromDateParsed.getMonth() + 1,
        1
      );
      formData.toDate = formatDate(adjustedDate);
      nextTick(() => {
        isAdjustingDate = false;
      });
    }
  }
);

// toDate 변경 감시 - fromDate보다 작을 경우 fromDate를 toDate의 한달 전으로 세팅
watch(
  () => formData.toDate,
  (newToDate) => {
    if (isAdjustingDate || !newToDate || !formData.fromDate) return;

    const fromDateParsed = parseDate(formData.fromDate);
    const toDateParsed = parseDate(newToDate);

    if (!fromDateParsed || !toDateParsed) return;

    // toDate가 fromDate보다 작을 경우, fromDate를 toDate의 한달 전으로 세팅
    if (toDateParsed < fromDateParsed) {
      isAdjustingDate = true;
      const adjustedDate = new Date(
        toDateParsed.getFullYear(),
        toDateParsed.getMonth() - 1,
        1
      );
      formData.fromDate = formatDate(adjustedDate);
      nextTick(() => {
        isAdjustingDate = false;
      });
    }
  }
);

// =========================== Methods ===========================
// 날짜 validation 관련 함수들
// yyyy-mm 형식의 날짜를 Date 객체로 변환
const parseDate = (dateStr) => {
  if (!dateStr) return null;
  const [year, month] = dateStr.split('-').map(Number);
  return new Date(year, month - 1, 1);
};

// Date 객체를 yyyy-mm 형식으로 변환
const formatDate = (date) => {
  if (!date) return '';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    '0'
  )}`;
};

// 날짜 validation 체크 (조회 전에 호출)
const validateDateRange = () => {
  if (!formData.fromDate || !formData.toDate) {
    proxy.$alert('시작월과 종료월을 모두 입력해주세요.');
    return false;
  }

  const fromDateParsed = parseDate(formData.fromDate);
  const toDateParsed = parseDate(formData.toDate);

  if (!fromDateParsed || !toDateParsed) {
    proxy.$alert('날짜 형식이 올바르지 않습니다.');
    return false;
  }

  // fromDate가 toDate보다 크면, fromDate를 toDate보다 1개월 전으로 설정
  if (fromDateParsed > toDateParsed) {
    const adjustedDate = new Date(
      toDateParsed.getFullYear(),
      toDateParsed.getMonth() - 1,
      1
    );
    formData.fromDate = formatDate(adjustedDate);
    proxy.$alert(
      '시작월이 종료월보다 클 수 없습니다. 시작월이 자동으로 조정되었습니다.'
    );
    return false; // 조정 후 다시 확인하도록 false 반환
  }

  return true;
};

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == 'siteNo') {
    if (proxy.$util.isEmpty(formData.sr_siteNo)) {
      formData.siteCd = '';
      formData.siteNm = '';
    } else {
      formData.siteNm = '';
      siteFocusKill();
    }
  } else if (e.target.id == 'siteNm') {
    if (proxy.$util.isEmpty(formData.siteNm)) {
      formData.siteCd = '';
      formData.siteNo = '';
    } else {
      formData.siteNo = '';
      siteFocusKill();
    }
  }
}

const initializeFormData = () => {
  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${String(
    now.getMonth() + 1
  ).padStart(2, '0')}`;

  // 1개월 뒤 계산
  const nextMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const nextMonthStr = `${nextMonth.getFullYear()}-${String(
    nextMonth.getMonth() + 1
  ).padStart(2, '0')}`;

  formData.toDate = currentMonth;
  formData.fromDate = nextMonthStr;
};

const fnGetBaseinfoList = async () => {
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

      formData.chkLstType = baseCodeArr.value.COM001[0].baimValDCd;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
    // proxy.$alert(err.response.data.message);
  }
};

const fnSearch = async () => {
  // 날짜 validation 체크
  if (!validateDateRange()) {
    return; // validation 실패 시 조회 중단
  }

  const param = {
    cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
    userId: sessionStorage.getItem('gv_userId'),
    fromDate: formData.fromDate.split('T')[0].replaceAll('-', ''),
    toDate: formData.toDate.split('T')[0].replaceAll('-', ''),
    siteCd: formData.siteCd,
    chkptNm: formData.chkptNm,
    chkLstType: formData.chkLstType,
  };

  try {
    const response = await axios.get('/webApi/chkLst03/inspect-results', {
      params: param,
    });

    if (response.status === 200) {
      chkptResultList.value = response.data?.inspectResult;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
    // proxy.$alert(err.response.data.message);
  }
};

function fnDataValidationChk(filteredData) {
  let alertMsg = '';
  let retVal = true;

  for (var i = 0; i < filteredData.length; i++) {
    if (proxy.$util.isEmpty(filteredData[i].siteCd)) {
      alertMsg = '사업장은 필수 입력 값 입니다.';

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkLstType)) {
      alertMsg = '점검구분은 필수 입력 값 입니다.';

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkptNm)) {
      alertMsg = '점검대상명칭은 필수 입력 값 입니다.';

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].mgmtUserId)) {
      alertMsg = '관리자는 필수 입력 값 입니다.';

      fnAlertMsg(alertMsg);
      retVal = false;
    }

    if (!retVal) {
      return retVal;
    }
  }

  return retVal;
}

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getSiteInfoList', {
      cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      siteNo: formData.siteNo,
      siteNm: formData.siteNm,
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
    // alert(err.response.data.message);
  }
};

/* fnCallback */
const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split('/').pop();
    if (apiId == 'getSiteInfoList') {
      if (res.data.length == 1) {
        formData.siteCd = res.data[0].SITE_CD;
        formData.siteNo = res.data[0].SITE_NO;
        formData.siteNm = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen('searchForm');
        SiteSearchPopOpen.value = true;
      } else {
        formData.siteCd = '';
        formData.siteNo = '';
        formData.siteNm = '';
      }
    }
  }
};

/* user function */
const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  formData.siteCd = siteCdVal;
  formData.siteNo = siteNoVal;
  formData.siteNm = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  chkptResultList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint == 'searchForm') {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
      siteNo_p: formData.sr_siteNo,
      siteNm_p: formData.sr_siteNm,
      onSelect: onSiteSelected,
    });
  } else {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
      siteNo_p: formData.sr_siteNo,
      siteNm_p: formData.sr_siteNm,
      onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
        chkptList.value[callPoint].siteCd = siteCdVal;
        chkptList.value[callPoint].siteNm = siteNmVal;
      },
    });
  }
};

// 날짜 범위 생성 함수 (fromDate ~ toDate 사이의 모든 월)
const generateDateRange = (fromDate, toDate) => {
  const dates = [];

  if (!fromDate || !toDate) return dates;

  // yyyy-mm 형식을 파싱
  const [fromYear, fromMonth] = fromDate.split('-').map(Number);
  const [toYear, toMonth] = toDate.split('-').map(Number);

  let currentYear = fromYear;
  let currentMonth = fromMonth;

  // 시작 월부터 종료 월까지 반복
  while (
    currentYear < toYear ||
    (currentYear === toYear && currentMonth <= toMonth)
  ) {
    // yyyyMM 형식으로 변환 (예: 202411)
    const workDate = `${currentYear}${String(currentMonth).padStart(2, '0')}`;
    dates.push(workDate);

    // 다음 월로 이동
    currentMonth++;
    if (currentMonth > 12) {
      currentMonth = 1;
      currentYear++;
    }
  }

  return dates;
};

const fnChkLstRstPopOpen = (chkptResult) => {
  // fromDate와 toDate 사이의 모든 월에 대해 workDate 생성
  const workMonths = generateDateRange(formData.fromDate, formData.toDate);
  const baseCode = baseCodeArr.value['COM001'].filter(
    (item) => item.baimValDCd == chkptResult.chkLstType
  );
  let chkLstTypeNm = '';

  if (proxy.$util.isNotEmpty(baseCode) && baseCode.length == 1) {
    chkLstTypeNm = baseCode[0]?.baimValDNm || '';
  }

  // 각 workDate마다 chkptInfo 항목 생성
  const chkptInfo = workMonths.map((workMonth) => ({
    siteCd: chkptResult.siteCd || '',
    siteNm: chkptResult.siteNm || '',
    workMonth: workMonth,
    chkptCd: chkptResult.chkptCd || '',
    chkptNm: chkptResult.chkptNm || '',
    chkLstType: chkptResult.chkLstType || '',
    chkLstTypeNm: chkLstTypeNm || '',
    siteAdminNm: chkptResult.siteAdminNm || '',
    chkptDesc: chkptResult.chkptDesc || '',
  }));

  openPop(ChkLstRstPop, {
    cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
    chkptInfo: chkptInfo,
  });
};

// 점검일지 조회 버튼 클릭 시 - 체크된 항목들만 모아서 팝업 열기
const fnSelectedChkLstRstPopOpen = () => {
  // chk가 true인 항목들만 필터링
  const selectedItems = chkptResultList.value.filter(
    (item) => item.chk === true
  );

  if (selectedItems.length === 0) {
    proxy.$alert('조회할 항목을 선택해주세요.');
    return;
  }

  // fromDate와 toDate 사이의 모든 월에 대해 workDate 생성
  const workMonths = generateDateRange(formData.fromDate, formData.toDate);

  // chkptInfo 배열 생성 (각 체크된 항목 × 각 월의 조합)
  const chkptInfo = [];

  selectedItems.forEach((chkptResult) => {
    // chkLstTypeNm 찾기
    const baseCode =
      baseCodeArr.value['COM001']?.filter(
        (item) => item.baimValDCd == chkptResult.chkLstType
      ) || [];
    let chkLstTypeNm = '';

    if (proxy.$util.isNotEmpty(baseCode) && baseCode.length == 1) {
      chkLstTypeNm = baseCode[0]?.baimValDNm || '';
    }

    // 각 workMonth마다 chkptInfo 항목 생성
    workMonths.forEach((workMonth) => {
      chkptInfo.push({
        siteCd: chkptResult.siteCd || '',
        siteNm: chkptResult.siteNm || '',
        workMonth: workMonth,
        chkptCd: chkptResult.chkptCd || '',
        chkptNm: chkptResult.chkptNm || '',
        chkLstType: chkptResult.chkLstType || '',
        chkLstTypeNm: chkLstTypeNm || '',
        siteAdminNm: chkptResult.siteAdminNm || '',
        chkptDesc: chkptResult.chkptDesc || '',
      });
    });
  });

  openPop(ChkLstRstPop, {
    cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
    chkptInfo: chkptInfo,
  });
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};
</script>

<style scoped></style>
