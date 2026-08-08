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
      <!-- PRAFTA-SUBCON-T0-05: 사업장 검색(ChkLst_03 규약) — 문항 사업장 단위 분리 -->
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="fnSiteBlur"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="fnSiteBlur"
        />
      </div>
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
            <!-- PRAFTA-SUBCON-T0-05: 타 사업장 문항 가져오기(Baim_06 UX 준거) -->
            <button class="btn btn-custom" @click="fnImportPopOpen()">
              타 사업장 문항 가져오기
            </button>
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
                <th class="editableCell" style="width: 8%">시행일</th>
                <th style="width: 5%">이력</th>
              </tr>
            </thead>
            <tbody>
              <!-- PRAFTA-SUBCON-T0-05: 사업장 미선택 시 빈 안내 -->
              <template v-if="!siteCd">
                <tr>
                  <td colspan="7" class="edu-grid-empty">
                    사업장을 먼저 선택해주세요.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in chkptInspectItemList"
                  :key="item.inspectItemCd"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="item.chk"
                      :disabled="isMirror(item)"
                    />
                  </td>
                  <td>
                    <input
                      id="sortIdx"
                      v-model="item.sortIdx"
                      :disabled="isMirror(item)"
                      @blur="focusKill(item.sortIdx, idx)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="item.inspectItemSubj"
                      :disabled="isMirror(item)"
                    />
                    <span v-if="isMirror(item)" class="link-badge">연동</span>
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
                    <!-- 시행월→시행일 전환: 일 단위 캘린더 (YYYYMMDD 압축값도 CalendarSrch 가 정규화) -->
                    <CalendarSrch
                      :range="false"
                      style="width: 150px"
                      v-model="item.strDate"
                      :disabled="isMirror(item)"
                    />
                  </td>
                  <td style="text-align: center">
                    <!-- 저장된 문항만 변경이력 조회 가능(신규 미저장 행은 코드 없음) -->
                    <button
                      v-if="item.inspectItemCd"
                      class="btn btn-custom"
                      style="padding: 2px 10px"
                      @click="fnHistPopOpen(item)"
                    >
                      이력
                    </button>
                    <span v-else>-</span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <!-- 문항 변경이력 팝업 -->
  <InspectItemHistPop
    v-if="showHistPop"
    :siteCd_p="histTarget.siteCd"
    :chkLstType_p="histTarget.chkLstType"
    :inspectItemCd_p="histTarget.inspectItemCd"
    :inspectItemSubj_p="histTarget.inspectItemSubj"
    @close="showHistPop = false"
  />

  <!-- PRAFTA-SUBCON-T0-05: 타 사업장 점검문항 가져오기 팝업 -->
  <InspectItemImportPop
    v-if="showImportPop"
    :dstSiteCd_p="siteCd"
    :dstSiteNo_p="siteNo"
    :dstSiteNm_p="siteNm"
    :chkLstType_p="targetValCd"
    :chkLstTypeNm_p="targetValNm"
    @imported="fnSubSearch"
    @close="showImportPop = false"
  />
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import InspectItemHistPop from "@/views/chkLst/popup/InspectItemHistPop.vue";
import InspectItemImportPop from "@/views/chkLst/popup/InspectItemImportPop.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

defineOptions({ name: "ChkLst_02" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const chkptInspectItemList = ref([]);
const systCodeArr = ref({});

// PRAFTA-SUBCON-T0-05: 사업장 조회 상태(ChkLst_03 규약)
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

// 타 사업장 문항 가져오기 팝업
const showImportPop = ref(false);

// 문항 변경이력 팝업
const showHistPop = ref(false);
const histTarget = ref({});

const fnHistPopOpen = (item) => {
  histTarget.value = {
    siteCd: item.siteCd, // PRAFTA-SUBCON-T0-05: 사업장 키 관통
    chkLstType: item.chkLstType,
    inspectItemCd: item.inspectItemCd,
    inspectItemSubj: item.inspectItemSubj,
  };
  showHistPop.value = true;
};
const baseCodeArr = ref({});
const localButtons = ref({ ...props.buttons });

/* 조회조건 변수 세팅 */
const sr_codeNm = ref("");

const targetValCd = ref("");
const targetValNm = ref("");

const headChk = ref(false);

// PRAFTA-SUBCON-T6-10: 미러(연동 수신) 문항 판정 — 서버 응답의 linkSrcCmpnyCd 기준.
//   미러 문항은 제공 회사에서만 관리한다(수정/삭제 시 서버가 403). 자체 문항 신규 추가는 그대로 허용.
const isMirror = (item) => !!item.linkSrcCmpnyCd;

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// PRAFTA-SUBCON-T0-05: 로그인 소속 사업장 기본값(ChkLst_03 규약)
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

useFieldWatcher(
  chkptInspectItemList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  chkptInspectItemList.value = [];

  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM001"],
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (code) => {
  if (proxy.$util.isNotEmpty(code)) {
    targetValCd.value = code.baimValDCd;
    targetValNm.value = code.baimValDNm;
  }

  // PRAFTA-SUBCON-T0-05: 사업장 미선택 시 조회 차단
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 먼저 선택해주세요.");
    return;
  }

  if (proxy.$util.isEmpty(targetValCd.value)) {
    return;
  }

  chkptInspectItemList.value = [];

  try {
    const response = await axios.get(
      "/webApi/chkLst02/chkpt-inspect-item-lists",
      {
        params: {
          siteCd: siteCd.value, // PRAFTA-SUBCON-T0-05: 사업장 키 관통
          codeCd: targetValCd.value,
        },
      }
    );

    if (response.status === 200) {
      chkptInspectItemList.value =
        response.data?.chkptInspectItemResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const invalidIdx = dataList.findIndex((item) =>
    proxy.$util.isEmpty(item.inspectItemSubj)
  );

  if (invalidIdx !== -1) {
    proxy.$alert(`선택된 데이터 중 ${invalidIdx + 1}번째 행의 점검항목명을\n입력해주세요.`);
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst02/update-chkpt-inspect-items",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSubSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDelete = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM), {
    variant: "danger",
  });
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst02/delete-chkpt-inspect-items",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSubSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/* user function */
function fnButtonControll() {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

function fnHeadChk() {
  headChk.value = !headChk.value;
  chkptInspectItemList.value.forEach((item) => {
    // PRAFTA-SUBCON-T6-10: 미러 문항은 저장/삭제 대상이 아니므로 전체선택에서 제외(서버도 403 으로 차단)
    if (isMirror(item)) return;
    item.chk = headChk.value;
  });
}

function fnAddRow() {
  // PRAFTA-SUBCON-T0-05: 행 생성 시 현재 사업장 주입(미선택 시 차단)
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 먼저 선택해주세요.");
    return;
  }

  if (proxy.$util.isNotEmpty(targetValCd.value)) {
    chkptInspectItemList.value.push({
      chk: true,
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteCd: siteCd.value,
      chkLstType: targetValCd.value,
      sortIdx: chkptInspectItemList.value.length + 1,
      useYn: "Y",
      strDate: proxy.$util.getToday(),
    });
  }
}

function fnSaveRow() {
  // PRAFTA-SUBCON-T6-10: 미러 문항은 저장 대상에서 제외(서버 잠금 가드와 동일 규칙)
  const filteredData = chkptInspectItemList.value.filter(
    (chkpt) => chkpt.chk && !isMirror(chkpt)
  );
  //  const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  fnSave(filteredData);
}

function fnDeleteRow() {
  // PRAFTA-SUBCON-T6-10: 미러 문항은 삭제(사용중지)도 제공 회사에서 관리한다
  const filteredData = chkptInspectItemList.value.filter(
    (chkpt) => chkpt.chk && chkpt.inspectItemCd && !isMirror(chkpt)
  );
  // const dataList = proxy.$util.toCamelCaseKeys(filteredData);

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  fnDelete(filteredData);
}

// ================ PRAFTA-SUBCON-T0-05: 사업장 검색(ChkLst_03 규약) ================

// 사업장 선택 반영 — 사업장 변경 시 문항 목록 초기화 후 재조회
const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;

  chkptInspectItemList.value = [];
  headChk.value = false;

  // 점검구분이 이미 선택돼 있으면 새 사업장 기준으로 재조회
  if (proxy.$util.isNotEmpty(siteCd.value) && proxy.$util.isNotEmpty(targetValCd.value)) {
    fnSubSearch();
  }
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

// 사업장 입력 blur — 비우면 초기화, 입력값 있으면 단건 자동 매칭(ChkLst_03 siteFocusKill 규약)
// (기존 focusKill 은 그리드 정렬순서 검증용 시그니처라 별도 핸들러로 분리)
function fnSiteBlur(e) {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      onSiteSelected("", "", "");
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      onSiteSelected("", "", "");
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  }
}

// 사업장 단건 자동 매칭: 1건이면 확정, 다건이면 검색팝업, 0건이면 초기화
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });

    if (response.status === 200) {
      const siteList = response.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        onSiteSelected(
          siteList[0].siteCd,
          siteList[0].siteNo,
          siteList[0].siteNm
        );
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        onSiteSelected("", "", "");
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ================ PRAFTA-SUBCON-T0-05: 타 사업장 문항 가져오기 ================

// 가져오기 팝업 오픈 — 사업장·점검구분 미선택 시 차단(Baim_06 규약)
const fnImportPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 먼저 선택해주세요.");
    return;
  }
  if (proxy.$util.isEmpty(targetValCd.value)) {
    proxy.$alert("점검구분을 먼저 선택해주세요.");
    return;
  }
  showImportPop.value = true;
};
</script>

<style scoped>
/* PRAFTA-SUBCON-T6-10: 연동(읽기전용) 배지 — ChkLst_01 / Subcon_02 배지 톤 정합 */
.link-badge {
  display: inline-block;
  margin-left: 0.35rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
</style>
