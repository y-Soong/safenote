<template>
  <div class="viewComm">
    <!-- Tbm_01 탭 자식 1: 교육자료 관리 (Attd_01_1 표준 — 자식이 자기 ViewHeader 를 소유) -->
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
    />
    <!--
      @excel="fnExcel" -->

    <div>
      <div class="viewSearch">
        <div>
          <label>사업장</label>
          <input
            id="siteNo"
            type="text"
            v-model="siteNo"
            placeholder="사업장코드"
            @blur="focusKill"
          />
          <button class="search-btn" @click="fnSiteSearchPopOpen()">
            <img class="search_icon" :src="search_icon" alt="검색" />
          </button>
          <input
            id="siteNm"
            type="text"
            v-model="siteNm"
            placeholder="사업장명"
            @blur="focusKill"
          />
        </div>
        <div>
          <label>자료유형</label>
          <select v-model.trim="mtrlType" name="combo">
            <option
              v-for="opt in baseCodeArr['COM003'] || []"
              :key="opt.baimValDCd"
              :value="opt.baimValDCd"
            >
              {{ opt.baimValDNm }}
            </option>
          </select>
        </div>
        <div>
          <label>자료명</label>
          <input
            v-model.trim="mtrlTitle"
            type="text"
            @input="fnDebouncedSearch"
          />
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
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">교육자료 리스트</span>
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
                  <th class="event_cell" style="width: 8%">스코프</th>
                  <ThSortable
                    label="교육자료명"
                    col-key="title"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.title"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <th class="event_cell" style="width: 10%">교육자료 타입</th>
                  <th style="width: 15%">사용여부</th>
                  <ThSortable
                    label="교육자료 설명"
                    col-key="contents"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.contents"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="등록된 교육자료 수"
                    col-key="mtrlCnt"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.mtrlCnt"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="등록자"
                    col-key="insertNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.insertNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="등록일자"
                    col-key="insertDate"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.insertDate"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                </tr>
              </thead>
              <tbody>
                <template
                  v-if="
                    !tbmEduInfoResultList || tbmEduInfoResultList.length === 0
                  "
                >
                  <tr>
                    <td colspan="10" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(info, idx) in sortedData" :key="info.id">
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td>
                      <input
                        type="checkbox"
                        v-model="info.chk"
                        :disabled="info.lockedYn === 'Y'"
                      />
                    </td>
                    <td style="text-align: center">
                      <span
                        class="scope-badge"
                        :class="
                          info.isCommonContent === 'Y'
                            ? 'scope-badge-common'
                            : 'scope-badge-site'
                        "
                      >
                        {{
                          info.isCommonContent === "Y"
                            ? "회사공통"
                            : fnSiteNm(info.siteCd)
                        }}
                      </span>
                    </td>
                    <!-- 제목 셀: 단일 클릭=상세 팝업. 다른 셀처럼 td dblclick=수정 핸들러를
                       달면 더블클릭 시 click 2회+dblclick 1회가 동시에 발생하여 팝업이
                       겹쳐 unmount 도중 호출 에러가 나므로, 이 셀에서는 dblclick 미연결. -->
                    <td>
                      <button
                        type="button"
                        class="title-link"
                        @click.stop="fnTbmEduDetailPopOpen(info)"
                      >
                        {{ info.title }}
                      </button>
                    </td>
                    <td>
                      <BaseSelect
                        v-model="info.mtrlType"
                        :disabled="info.lockedYn === 'Y'"
                      >
                        <option
                          v-for="opt in (baseCodeArr['COM003'] || []).filter(
                            (o) => o.baimValDCd != null
                          )"
                          :key="opt.baimValDCd"
                          :value="opt.baimValDCd"
                        >
                          {{ opt.baimValDNm }}
                        </option>
                      </BaseSelect>
                    </td>
                    <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                      <BaseSelect
                        v-model="info.useYn"
                        :disabled="info.lockedYn === 'Y'"
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
                    <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                      {{ info.contents }}
                    </td>
                    <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                      {{ info.mtrlCnt }}
                    </td>
                    <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                      {{ info.insertNm }}
                    </td>
                    <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                      {{ info.insertDate }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import TbmEduMtrlInfo from "./popup/TbmEduMtrlInfo.vue";
import TbmEduDetailPop from "./popup/TbmEduDetailPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// ================ Options ================
defineOptions({ name: "Tbm_01_1" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const tbmEduInfoResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(tbmEduInfoResultList);
const { colWidths, onResize } = useColumnResize({
  title: 130,
  contents: 180,
  mtrlCnt: 130,
  insertNm: 110,
  insertDate: 110,
});
const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const siteList = ref([]);

// 조회조건 변수
const mtrlType = ref();
const mtrlTitle = ref();
const useYn = ref();
// prafta-033-A: 스코프 필터. siteCd 는 숨김 실제키, siteNo/siteNm 은 화면 표시값(Attd_01_1 표준).
//   빈 값이면 전체(회사공통 + 전 사업장) 조회.
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");

// 화면 제어 변수
const headChk = ref(false);

// prafta-033-A: 검색 디바운싱(500ms)
let searchDebounceTimer = null;

// ================ Watchers ================
useFieldWatcher(
  tbmEduInfoResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnGetSiteList();
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

      useYn.value = systCodeArr.value.SYS003[0].systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM003"],
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

      mtrlType.value = baseCodeArr.value.COM003[0].baimValDCd;

      // mtrlType.value = baseCodeArr.value.COM003.filter(
      //   (o) => o.baimValDCd != null
      // )[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetSiteList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: "",
        siteNm: "",
      },
    });

    if (response.status === 200) {
      siteList.value = response.data?.siteInfoResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  tbmEduInfoResultList.value = [];

  try {
    const response = await axios.get("/webApi/tbm01/tbm-edu-infos", {
      params: {
        mtrlType: mtrlType.value,
        title: mtrlTitle.value,
        useYn: useYn.value,
        siteCd: siteCd.value,
      },
    });

    if (response.status === 200) {
      tbmEduInfoResultList.value = response.data?.tbmEduInfoResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// prafta-033-A: 자료명 입력 디바운싱(500ms) - 불필요한 조회 호출 방지
const fnDebouncedSearch = () => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    fnSearch();
  }, 500);
};

// prafta-033-A: 사업장코드 -> 사업장명 표기(스코프 배지)
const fnSiteNm = (code) => {
  if (!code) return "사업장";
  const found = (siteList.value || []).find((s) => s.siteCd === code);
  return found ? found.siteNm : "사업장";
};

// ── 사업장 3요소 입력(코드/찾기/명) — Attd_01_1 표준 ──────────
// 코드나 명을 직접 입력하고 blur 하면 site-lists 로 조회한다.
//   1건이면 자동 채움, 다건이면 팝업, 0건이면 초기화(= 전체 조회).
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnSiteCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSiteCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId !== "site-lists") return;

  const list = res.data?.siteInfoResultList ?? [];
  if (list.length === 1) {
    siteCd.value = list[0].siteCd;
    siteNo.value = list[0].siteNo;
    siteNm.value = list[0].siteNm;
  } else if (list.length > 1) {
    fnSiteSearchPopOpen();
  } else {
    siteCd.value = "";
    siteNo.value = "";
    siteNm.value = "";
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSave = async () => {
  const filteredUsers = tbmEduInfoResultList.value.filter((user) => user.chk);

  if (filteredUsers.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm01/save-tbm-edus",
      filteredUsers
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDelete = async () => {
  const filteredUsers = tbmEduInfoResultList.value.filter((user) => user.chk);

  if (filteredUsers.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM), {
    variant: "danger",
  });
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm01/delete-tbm-edus",
      filteredUsers
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnCreate = () => {
  openPop(TbmEduMtrlInfo, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    onSearch: fnSearch,
  });
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  tbmEduInfoResultList.value.forEach((item) => {
    // T5-2: 사용 중(잠금) 자료는 전체 선택에서도 제외(수정/삭제 대상 불가)
    item.chk = item.lockedYn === "Y" ? false : headChk.value;
  });
};

const fnTbmEduMtrlInfoPopOpen = async (info) => {
  // T5-2: 사용 중(취소 외 세션 참조) 교육자료는 내용 수정이 잠기지만 AI 분석 지정은 변경 가능.
  // 잠금/편집 가능 여부와 안내는 팝업(TbmEduMtrlInfo) 내부에서 처리하므로 여기서 별도 안내하지 않는다.
  openPop(TbmEduMtrlInfo, {
    mtrlCd_p: info.mtrlCd,
    onSearch: fnSearch,
  });
};

// prafta-033-A: W-03 상세 보기(미디어 미리보기 + 사용 TBM 이력)
// 빠른 더블클릭 시 같은 팝업이 mount-도중-unmount-재mount 되며 비동기 콜백이
// 무효 인스턴스를 참조해 에러가 나는 것을 방지하기 위해 짧은 가드를 둔다.
let detailPopOpening = false;
const fnTbmEduDetailPopOpen = (info) => {
  if (detailPopOpening) return;
  detailPopOpening = true;
  setTimeout(() => {
    detailPopOpening = false;
  }, 300);
  openPop(TbmEduDetailPop, {
    mtrlCd_p: info.mtrlCd,
    onEdit: () => fnTbmEduMtrlInfoPopOpen(info),
  });
};
</script>

<style scoped>
/* prafta-033-A: 스코프 배지 */
.scope-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  line-height: 1.6;
  white-space: nowrap;
}

.scope-badge-common {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.scope-badge-site {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

/* prafta-033-A: 제목 링크(상세 보기 진입) */
.title-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
  text-decoration: underline;
  font: inherit;
}

.title-link:hover {
  color: var(--color-primary-hover);
}
</style>
