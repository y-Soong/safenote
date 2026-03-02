<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
    />

    <!-- @Search="fnSearch"
    @save="fnSave"
    @create="fnCreate"
    @delete="fnDelete"
    @excel="fnExcel" -->

    <!-- 버튼 영역과 사업장 조회 영역 -->
    <div class="button-area-wrapper">
      <div class="button-group" ref="buttonGroupRef">
        <button
          v-for="(button, idx) in (baseInfoArr['COM002'] || []).filter(
            (o) => o.baimValDCd != null
          )"
          :key="idx"
          :ref="(el) => setButtonRef(el, idx)"
          :class="[
            'category-button',
            { active: selectedCategoryId === button.baimValDCd },
          ]"
          @click="fnCategoryClick(button, idx)"
        >
          <span class="button-text">{{ button.baimValDNm }}</span>
        </button>
        <div class="triangle-indicator" :style="triangleStyle"></div>
      </div>

      <!-- 사업장 조회 영역 -->
      <div class="site-search-area">
        <input
          type="checkbox"
          v-model="commonChk"
          @change="fnCommonChkChange"
        />
        <label
          style="
            margin-right: 10px;
            padding-right: 15px;
            border-right: 1px solid #ccc;
          "
          >공통관리</label
        >

        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="commonChk"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="commonChk"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="commonChk"
          @blur="focusKill"
        />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- FIRST TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 30%">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">위험분류</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnSearch_fst()">조회</button>
            <button class="btn btn-custom" @click="fnAddRow_fst()">생성</button>
            <button class="btn btn-custom" @click="fnSaveRow_fst()">
              저장
            </button>
            <button
              class="btn btn-custom"
              v-if="!commonChk"
              @click="fnDeleteRow_fst()"
            >
              삭제
            </button>
          </div>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: hidden"
        >
          <table class="data-grid w-full border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="width: 8%">No</th>
                <th style="width: 8%">
                  <input
                    id="headChk"
                    v-model="headChk_fst"
                    type="checkbox"
                    @click="fnHeadChk_fst()"
                  />
                </th>
                <th class="event_cell">
                  분류명 <br />
                  <input
                    v-model.trim="srchData_fst.riskTypeNm"
                    class="header-input"
                    type="text"
                    :maxlength="25"
                  />
                </th>
                <th class="event_cell" style="width: 30%">
                  사용여부 <br />
                  <select
                    v-model.trim="srchData_fst.useYn"
                    name="combo"
                    style="width: 100%"
                  >
                    <option
                      v-for="opt in systCodeArr['SYS003'] || []"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !filteredRiskTypeList || filteredRiskTypeList.length === 0
                "
              >
                <tr>
                  <td colspan="4" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(risk, idx) in filteredRiskTypeList"
                  :key="risk.id"
                  @dblclick="fnSelectRiskType(risk)"
                >
                  <!-- @dblclick="fnSearch_sec(risk)" -->
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="risk.chk"
                      v-if="commonChk || !proxy.$util.isEmpty(risk.siteCd)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="risk.riskTypeNm"
                      :disabled="!commonChk && proxy.$util.isEmpty(risk.siteCd)"
                    />
                  </td>
                  <td>
                    <BaseSelect
                      v-model="risk.useYn"
                      name="codeDetailSrc"
                      :disabled="!commonChk && proxy.$util.isEmpty(risk.siteCd)"
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
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- SECOND TABLE -->
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
            <span class="subtitle-text">위험발생상황</span>
          </div>

          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-text">[{{ targetValNm }}]</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnSearch_sec()">조회</button>
            <button class="btn btn-custom" @click="fnAddRow_sec()">생성</button>
            <button class="btn btn-custom" @click="fnSaveRow_sec()">
              저장
            </button>
            <button
              class="btn btn-custom"
              v-if="!commonChk"
              @click="fnDeleteRow_sec()"
            >
              삭제
            </button>
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
                    v-model="headChk_sec"
                    type="checkbox"
                    @click="fnHeadChk_sec()"
                  />
                </th>
                <th style="width: 20%">
                  상황명<br />
                  <input
                    v-model.trim="srchData_sec.hazardNm"
                    class="header-input"
                    type="text"
                    :maxlength="25"
                  />
                </th>
                <th>
                  비고<br />
                  <input
                    v-model.trim="srchData_sec.hazardDesc"
                    class="header-input"
                    type="text"
                  />
                </th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !filteredRiskHazardList || filteredRiskHazardList.length === 0
                "
              >
                <tr>
                  <td colspan="4" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(hazard, idx) in filteredRiskHazardList"
                  :key="hazard.id"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="hazard.chk"
                      v-if="commonChk || !proxy.$util.isEmpty(hazard.siteCd)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="hazard.hazardNm"
                      :disabled="
                        !commonChk && proxy.$util.isEmpty(hazard.siteCd)
                      "
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="hazard.hazardDesc"
                      :disabled="
                        !commonChk && proxy.$util.isEmpty(hazard.siteCd)
                      "
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
  computed,
  reactive,
} from 'vue';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';
import { useModal } from '@/utils/useModal';
import search_icon from '@/assets/img/search_icon.png';
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue';

// =========================== Define ===========================
defineOptions({ name: 'Risk_01' });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Reactive ===========================
/* 위험요인구분 조회 데이터 */
const srchData_fst = reactive({
  useYn: null,
  riskTypeNm: '',
});

/* 유해요인 조회 데이터 */
const srchData_sec = reactive({
  hazardNm: '',
  hazardDesc: '',
});

// =========================== Ref ===========================
/* 버튼 관련 */
const localButtons = ref({ ...props.buttons });
const buttonGroupRef = ref('');
const buttonRefs = ref([]);
const selectedButtonIndex = ref(0);

/* 카테고리/선택 관련 */
const selectedCategoryId = ref('');
const selectedRiskTypeCd = ref('');
const selectedRiskTypeNm = ref('');
const targetValNm = ref('');

/* 사업장 관련 */
const siteCd = ref('');
const siteNo = ref('');
const siteNm = ref('');
const commonChk = ref(true);

/* 체크박스 */
const headChk_fst = ref(false);
const headChk_sec = ref(false);

/* 데이터 리스트 */
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const riskTypeList = ref([]);
const riskHazardList = ref([]);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Computed ===========================
// 필터링된 위험요인구분 리스트
const filteredRiskTypeList = computed(() => {
  let filtered = [...riskTypeList.value];

  // 위험요인구분명 필터링
  if (srchData_fst.riskTypeNm && srchData_fst.riskTypeNm.trim() !== '') {
    filtered = filtered.filter((item) =>
      item.riskTypeNm
        ?.toLowerCase()
        .includes(srchData_fst.riskTypeNm.toLowerCase())
    );
  }

  // 사용여부 필터링
  if (srchData_fst.useYn !== null && srchData_fst.useYn !== '') {
    filtered = filtered.filter((item) => item.useYn === srchData_fst.useYn);
  }

  return filtered;
});

// 필터링된 유해요인 리스트
const filteredRiskHazardList = computed(() => {
  let filtered = [...riskHazardList.value];

  // 유해요인명 필터링
  if (srchData_sec.hazardNm && srchData_sec.hazardNm.trim() !== '') {
    filtered = filtered.filter((item) =>
      item.hazardNm?.toLowerCase().includes(srchData_sec.hazardNm.toLowerCase())
    );
  }

  // 유해요인 비고 필터링
  if (srchData_sec.hazardDesc && srchData_sec.hazardDesc.trim() !== '') {
    filtered = filtered.filter((item) =>
      item.hazardDesc
        ?.toLowerCase()
        .includes(srchData_sec.hazardDesc.toLowerCase())
    );
  }

  return filtered;
});

// 세모 위치 계산
const triangleStyle = computed(() => {
  if (!buttonRefs.value[selectedButtonIndex.value]) {
    return { left: '0px', opacity: 0 };
  }

  const button = buttonRefs.value[selectedButtonIndex.value];
  const buttonGroup = buttonGroupRef.value;

  if (!buttonGroup) {
    return { left: '0px', opacity: 0 };
  }

  const buttonRect = button.getBoundingClientRect();
  const groupRect = buttonGroup.getBoundingClientRect();
  const left = buttonRect.left - groupRect.left + buttonRect.width / 2;

  return {
    left: `${left}px`,
    opacity: 1,
  };
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  riskTypeList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// =========================== Methods ===========================
// API 호출
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
        if (item.baimValDCd == null) return;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseInfoArr.value = grouped;

      fnCategoryClick(baseInfoArr.value['COM002'][0], 0);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

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

// 조회
const fnSearch_fst = async () => {
  if (!commonChk.value) {
    if (proxy.$util.isEmpty(siteCd.value)) {
      proxy.$alert('사업장을 선택해주세요.');
      return;
    }
  }

  riskTypeList.value = [];

  try {
    const response = await axios.get('/webApi/risk01/risk-type-lists', {
      params: {
        processCd: selectedCategoryId.value,
        siteCd: siteCd.value,
        riskTypeNm: srchData_fst.riskTypeNm,
        useYn: srchData_fst.useYn,
      },
    });

    if (response.status === 200) {
      const resData = response.data?.riskTypeList || [];

      riskTypeList.value = resData;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnSearch_sec = async () => {
  targetValNm.value =
    baseInfoArr.value.COM002.filter(
      (o) => o.baimValDCd == selectedCategoryId.value
    )[0].baimValDNm +
    ' - ' +
    selectedRiskTypeNm.value;

  riskHazardList.value = [];

  try {
    const response = await axios.get('/webApi/risk01/risk-hazard-lists', {
      params: {
        riskTypeCd: selectedRiskTypeCd.value,
        processCd: selectedCategoryId.value,
        siteCd: siteCd.value,
        hazardNm: srchData_sec.hazardNm,
        hazardDesc: srchData_sec.hazardDesc,
      },
    });
    if (response.status === 200) {
      const resData = response.data?.riskHazardList || [];

      riskHazardList.value = resData;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

// 저장/삭제
const fnSaveRow_fst = async () => {
  const filteredDetail = riskTypeList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert('저장할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('저장하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/risk01/update-risk-types',
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSearch_fst();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '저장 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnSaveRow_sec = async () => {
  const filteredDetail = riskHazardList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert('저장할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('저장하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/risk01/update-risk-hazards',
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSearch_fst();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '저장 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnDeleteRow_fst = async () => {
  const filteredDetail = riskTypeList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert('삭제할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('삭제하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/risk01/delete-risk-types',
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSearch_fst();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '삭제 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnDeleteRow_sec = async () => {
  const filteredDetail = riskHazardList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert('삭제할 데이터가 없습니다.');
    return;
  }

  const ok = await proxy.$confirm('삭제하시겠습니까 ?');
  if (!ok) return;

  try {
    const response = await axios.post(
      '/webApi/risk01/delete-risk-hazards',
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert('처리되었습니다.');
      fnSearch_sec();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '삭제제 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

// 행 추가
const fnAddRow_fst = () => {
  if (!commonChk.value && proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert('사업장을 선택해주세요.');
    return;
  }

  riskTypeList.value.push({
    chk: true,
    cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
    siteCd: siteCd.value,
    processCd: selectedCategoryId.value,
    useYn: 'Y',
  });
};

const fnAddRow_sec = () => {
  if (proxy.$util.isEmpty(selectedRiskTypeCd.value)) {
    proxy.$alert('위험요인구분을 선택해주세요.');
    return;
  }

  if (!commonChk.value && proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert('사업장을 선택해주세요.');
    return;
  }

  riskHazardList.value.push({
    chk: true,
    cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
    riskTypeCd: selectedRiskTypeCd.value,
    processCd: selectedCategoryId.value,
    siteCd: siteCd.value,
    useYn: 'Y',
  });
};

// 체크박스
const fnHeadChk_fst = () => {
  headChk_fst.value = !headChk_fst.value;
  filteredRiskTypeList.value.forEach((item) => {
    // 체크박스가 표시되지 않는 경우는 무조건 false
    const isVisible =
      commonChk.value ||
      (item.siteCd !== null && item.siteCd !== undefined && item.siteCd !== '');
    if (!isVisible) {
      item.chk = false;
    } else {
      item.chk = headChk_fst.value;
    }
  });
};

const fnHeadChk_sec = () => {
  headChk_sec.value = !headChk_sec.value;
  filteredRiskHazardList.value.forEach((item) => {
    // 체크박스가 표시되지 않는 경우는 무조건 false
    const isVisible =
      commonChk.value ||
      (item.siteCd !== null && item.siteCd !== undefined && item.siteCd !== '');
    if (!isVisible) {
      item.chk = false;
    } else {
      item.chk = headChk_sec.value;
    }
  });
};

// 카테고리/버튼 관련
const fnCategoryClick = (button, idx) => {
  selectedCategoryId.value = button.baimValDCd;
  selectedButtonIndex.value = idx;

  selectedRiskTypeCd.value = '';
  srchData_fst.riskTypeNm = '';
  srchData_fst.useYn = null;

  riskTypeList.value = [];
  riskHazardList.value = [];

  fnSearch_fst();
};

const fnSelectRiskType = (risk) => {
  selectedRiskTypeCd.value = risk.riskTypeCd;
  selectedRiskTypeNm.value = risk.riskTypeNm;

  srchData_sec.hazardNm = '';
  srchData_sec.hazardDesc = '';

  fnSearch_sec(risk);
};

const setButtonRef = (el, idx) => {
  if (el) {
    buttonRefs.value[idx] = el;
  }
};

const fnButtonControll = () => {
  localButtons.value.search = 'N';
  localButtons.value.save = 'N';
  localButtons.value.create = 'N';
  localButtons.value.delete = 'N';
  localButtons.value.excel = 'N';
};

const fnCommonChkChange = () => {
  if (commonChk.value) {
    siteCd.value = '';
    siteNo.value = '';
    siteNm.value = '';
    fnSearch_fst();
  }

  selectedRiskTypeCd.value = '';
  srchData_fst.riskTypeNm = '';
  srchData_fst.useYn = null;

  srchData_sec.hazardNm = '';
  srchData_sec.hazardDesc = '';

  targetValNm.value = '';

  riskTypeList.value = [];
  riskHazardList.value = [];
};

// 사업장 관련
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post('/comApi/baseinfo/getSiteInfoList', {
      cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      userId: sessionStorage.getItem('gv_userId'),
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

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split('/').pop();

    if (apiId == 'getSiteInfoList') {
      if (res.data.length == 1) {
        siteCd.value = res.data[0].SITE_CD;
        siteNo.value = res.data[0].SITE_NO;
        siteNm.value = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = '';
        siteNo.value = '';
        siteNm.value = '';
      }
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

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

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
    siteNo_p: siteNo.value,
    siteNm_p: siteNm.value,
    onSelect: onSiteSelected,
  });
};
</script>

<style scoped>
.button-area-wrapper {
  background-color: #d5eee5;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 0;
}

.button-group {
  display: flex;
  gap: 0;
  padding: 0;
  background-color: #d5eee5;
  position: relative;
  overflow: visible;
  margin-bottom: 0;
  flex: 1;
}

.site-search-area {
  display: flex;
  align-items: center;
  gap: 8px;
  /* padding: 10px; */
  background-color: #d5eee5;
  border-radius: 4px;
  margin-right: 10px;
}

.site-search-area label {
  white-space: nowrap;
  margin-right: 4px;
  font-family: "Pretendard", sans-serif;
  font-size: 0.9rem;
  font-weight: 500;
}

.site-search-area input {
  padding: 6px 10px;
  border: 1px solid #d5eee5;
  border-radius: 4px;
  font-size: 14px;
}

.site-search-area input[type='text'] {
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0.05rem 0.3rem;
  width: 120px;
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search_icon {
  width: 16px;
  height: 16px;
}

.category-button {
  padding: 10px 40px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: clamp(14px, 1.2vw, 15px);
  letter-spacing: 0.2px;
  color: #1f1e1e60;
  position: relative;
  transition: all 10.3s ease;
  z-index: 1;
}

.category-button:hover {
  opacity: 0.9;
}

.category-button.active {
  color: #30796a;
  z-index: 2;
}

.triangle-indicator {
  position: absolute;
  bottom: -5px;
  width: 0;
  height: 0;
  border-left: 15px solid transparent;
  border-right: 15px solid transparent;
  border-bottom: 15px solid #fff;
  transform: translateX(-50%);
  transition: left 0.3s ease;
  z-index: 3;
  pointer-events: none;
}

.button-text {
  position: relative;
  z-index: 4;
}

/* 헤더 input 스타일 */
.header-input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.2rem 0.3rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 0.7rem;
  background: #fff;
  margin-top: 0.2rem;
}

/* 헤더 th에 input이 있을 때 padding 조정 */
.data-grid th:has(.header-input) {
  padding: 0.2rem 0.3rem;
  vertical-align: top;
}

/* 테이블 가로 스크롤 방지 */
.table-wrapper:first-child .table-box {
  overflow-x: hidden !important;
}

.table-wrapper:first-child .table-box .data-grid {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
  table-layout: fixed;
}
</style>
