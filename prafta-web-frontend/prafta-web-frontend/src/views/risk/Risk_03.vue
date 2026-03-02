<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
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
          v-model="siteNo"
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
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>진행상태</label>
        <select v-model="assessmentStatus" name="combo">
          <option
            v-for="opt in systCodeArr['SYS011'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>위험성구분</label>
        <select v-model="proccessCd" name="combo">
          <option
            v-for="opt in baseCodeArr['COM002'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>위험성분류</label>
        <select v-model="riskTypeCd" name="combo">
          <option
            v-for="opt in riskTypeArr.filter((o) => {
              if (proxy.$util.isEmpty(proccessCd)) {
                if (proxy.$util.isEmpty(o.processCd)) {
                  return o;
                }
              } else {
                if (
                  o.processCd == proccessCd ||
                  proxy.$util.isEmpty(o.processCd)
                ) {
                  return o;
                }
              }
            })"
            :key="opt.riskTypeCd"
            :value="opt.riskTypeCd"
          >
            {{ opt.riskTypeNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">평가대상 리스트</span>
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
                <!-- <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th> -->
                <th class="editableCell" style="width: 10%">위험구분</th>
                <th class="editableCell" style="width: 10%">위험분류</th>
                <th class="editableCell" style="width: 10%">평가요청일</th>
                <th class="editableCell" style="width: 10%">유해요인명</th>
                <th class="editableCell" style="width: 10%">평가요청자</th>
                <th class="editableCell" style="width: 10%">진행상태</th>
                <th class="editableCell" style="width: 10%">유해요인등급</th>
                <th class="editableCell">유해요인설명</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!riskAssessmentList || riskAssessmentList.length === 0"
              >
                <tr>
                  <td colspan="9" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(risk, idx) in riskAssessmentList"
                  :key="risk.assessmentCd"
                  @dblclick="fnOpenRiskAssessInfo(risk)"
                  style="cursor: pointer"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    {{ risk.processNm }}
                  </td>
                  <td>
                    {{ risk.riskTypeNm }}
                  </td>
                  <td>
                    {{ risk.initAssessDate }}
                  </td>
                  <td>
                    {{ risk.hazardNm }}
                  </td>
                  <td>
                    {{ risk.initAssessorNm }}
                  </td>
                  <td>
                    {{ risk.assessmentStatusNm }}
                  </td>
                  <td>
                    {{ risk.initRiskLv }}
                  </td>
                  <td>
                    {{ risk.initDesc }}
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
  computed,
  watch,
} from 'vue';
import { useModal } from '@/utils/useModal';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import search_icon from '@/assets/img/search_icon.png';
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue';
import RiskAssessInfo from './popup/RiskAssessInfo.vue';

defineOptions({ name: 'Risk_03' });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();

// const userStore = useUserStore();
const riskAssessmentList = ref([]);
const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const riskTypeArr = ref([]);
const SiteSearchPopOpen = ref(false);

/* 조회조건 변수 세팅 */
const assessmentStatus = ref();
const proccessCd = ref();
const riskTypeCd = ref('');
const revalDate = ref();

const sr_chkptNm = ref('');
const sr_useYn = ref('Y');
const siteCd = ref('');
const siteNo = ref('');
const siteNm = ref('');

/* UserInfoPop 파라미터 변수 */
const headChk = ref(false);

const siteDisabled = ref(false);

const { proxy } = getCurrentInstance();

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnGetriskTypeList();
  await fnSearch();
});

useFieldWatcher(
  riskAssessmentList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

watch(proccessCd, (newVal) => {
  riskTypeCd.value = '';
});

// focusKill 이벤트
const focusKill = (e) => {
  if (e.target.id == 'siteNo') {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = '';
      siteNm.value = '';
    } else {
      siteNm.value = '';
      siteFocusKill();
    }
  } else if (e.target.id == 'siteNm') {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = '';
      siteNo.value = '';
    } else {
      siteNo.value = '';
      siteFocusKill();
    }
  }
};

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/syst-info-list', {
      params: {
        systCodeList: ['SYS011'],
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

      assessmentStatus.value = systCodeArr.value.SYS011[0].systValDCd;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/base-info-list', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        baseCodeList: ['COM002'],
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

      proccessCd.value = baseCodeArr.value.COM002[0].baimValDCd;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnGetriskTypeList = async () => {
  try {
    const response = await axios.get('/webApi/risk03/risk-type-info-lists', {});

    if (response.status === 200) {
      riskTypeArr.value = response.data.riskTypeList;
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
  riskAssessmentList.value = [];

  try {
    const response = await axios.get('/webApi/risk03/risk-assessment-lists', {
      params: {
        siteCd: siteCd.value,
        assessmentStatus: assessmentStatus.value,
        processCd: proccessCd.value,
        riskTypeCd: riskTypeCd.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      riskAssessmentList.value = response.data.riskAssessmentList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnDataValidationChk = (filteredData) => {
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
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getSiteInfoList', {
      cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      siteNo: siteNo.value,
      siteNm: siteNm.value,
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
  }
};

/* fnCallback */
const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split('/').pop();
    if (apiId == 'getSiteInfoList') {
      if (res.data.length == 1) {
        siteCd.value = res.data[0].SITE_CD;
        siteNo.value = res.data[0].SITE_NO;
        siteNm.value = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen('searchForm');
        SiteSearchPopOpen.value = true;
      } else {
        siteCd.value = '';
        siteNo.value = '';
        siteNm.value = '';
      }
    }
  }
};

/* user function */
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  localButtons.value.create = 'N';
  localButtons.value.save = 'N';
  localButtons.value.delete = 'N';
  localButtons.value.excel = 'N';
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  riskAssessmentList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint == 'searchForm') {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
      siteNo_p: siteNo.value,
      siteNm_p: siteNm.value,
      onSelect: onSiteSelected,
    });
  }
};

const fnOpenRiskAssessInfo = (risk) => {
  console.log(risk);

  openPop(RiskAssessInfo, {
    riskAssessmentData: {
      cmpnyCd: risk.cmpnyCd || '',
      siteCd: risk.siteCd || '',
      processCd: risk.processCd || '',
      processNm: risk.processNm || '',
      riskTypeCd: risk.riskTypeCd || '',
      riskTypeNm: risk.riskTypeNm || '',
      hazardCd: risk.hazardCd || '',
      hazardNm: risk.hazardNm || '',
      assessmentCd: risk.assessmentCd || '',
      assessmentStatus: risk.assessmentStatus || '',
      assessmentStatusNm: risk.assessmentStatusNm || '',
      initLikelihoodScore: risk.initLikelihoodScore || '',
      initSeverityScore: risk.initSeverityScore || '',
      initRiskLv: risk.initRiskLv || '',
      initDesc: risk.initDesc || '',
      initAssessorId: risk.initAssessorId || '',
      initAssessorNm: risk.initAssessorNm || '',
      initAssessDate: risk.initAssessDate || '',
      initFileMgmtCd: risk.initFileMgmtCd || '',
      initFilePath: risk.initFilePath || '',
      revalDate: risk.revalDate || '',
      revalBeforeDesc: risk.revalBeforeDesc || '',
      revalLikelihoodScore: proxy.$util.isEmpty(risk.revalLikelihoodScore)
        ? risk.initLikelihoodScore
        : risk.revalLikelihoodScore || '',
      revalSeverityScore: proxy.$util.isEmpty(risk.revalSeverityScore)
        ? risk.initSeverityScore
        : risk.revalSeverityScore || '',
      revalRiskLv: risk.revalRiskLv || '',
      revalDesc: risk.revalDesc || '',
      revalAssessorId: risk.revalAssessorId || '',
      revalAssessorNm: risk.revalAssessorNm || '',
      revalAssessDate: risk.revalAssessDate || '',
      revalFileMgmtCd: risk.revalFileMgmtCd || '',
      revalFilePath: risk.revalFilePath || '',
      revalAssessDate: risk.revalAssessDate || '',
    },
    onSave: (data) => {
      // 저장 후 처리 로직
      console.log('Saved data:', data);
      fnSearch(); // 목록 새로고침
    },
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
