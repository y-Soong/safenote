<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
    <div class="viewSearch">
      <!-- 사업장: 코드 - 검색버튼 - 명칭 (ChkLst_01 동일 구조) -->
      <div>
        <label>사업장</label>
        <input
          id="sr_siteNo"
          type="text"
          v-model="sr_siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="sr_siteNm"
          type="text"
          v-model="sr_siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <!-- 점검구분 (COM001) -->
      <div>
        <label>점검구분</label>
        <select v-model="chkLstType" name="combo" @change="fnChkLstTypeChange">
          <option value="">전체</option>
          <option
            v-for="opt in (baseCodeArr['COM001'] || []).filter(
              (o) => o.baimValDCd != null
            )"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <!-- 점검대상명칭: input(편집가능) + 검색버튼 → ChkptTargetSearchPop -->
      <!-- 점검구분이 전체("")면 비활성 (targetDisabled) -->
      <div>
        <label>점검대상명칭</label>
        <input
          type="text"
          v-model="chkptNm"
          :disabled="targetDisabled"
          placeholder="검색"
          @input="onChkptNmInput"
          @blur="onChkptNmBlur"
        />
        <button
          class="search-btn"
          :disabled="targetDisabled"
          @click="fnChkptTargetPopOpen"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
      </div>

      <!-- 점검문항: input(편집가능) + 검색버튼 → InspectItemSearchPop -->
      <!-- 점검구분이 전체("")면 비활성 (targetDisabled) -->
      <div>
        <label>점검문항</label>
        <input
          type="text"
          v-model="inspectItemSubj"
          :disabled="targetDisabled"
          placeholder="검색"
          @input="onInspectItemInput"
          @blur="onInspectItemBlur"
        />
        <button
          class="search-btn"
          :disabled="targetDisabled"
          @click="fnInspectItemPopOpen"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
      </div>

      <!-- 조치여부: 전체 / 조치완료 / 미조치 -->
      <div>
        <label>조치여부</label>
        <select v-model="actionStatus" name="combo">
          <option value="">전체</option>
          <option value="Y">조치완료</option>
          <option value="N">미조치</option>
        </select>
      </div>
    </div>

    <!-- 결과 그리드 -->
    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">점검 불량 리스트</span>
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
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th style="width: 9%">조치여부</th>
                <th style="width: 12%">사업장</th>
                <th style="width: 10%">점검구분</th>
                <th style="width: 15%">점검대상명칭</th>
                <th style="width: 15%">점검항목명</th>
                <th style="width: 8%">불량내용</th>
                <th style="width: 10%">점검자</th>
                <th style="width: 10%">점검일자</th>
                <th style="width: 7%">조치</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!defectList || defectList.length === 0">
                <td colspan="10" class="edu-grid-empty">
                  조회된 불량 항목이 없습니다.
                </td>
              </tr>
              <tr
                v-for="(defect, idx) in defectList"
                v-else
                :key="
                  defect.siteCd +
                  '_' +
                  defect.chkptCd +
                  '_' +
                  defect.inspectItemCd +
                  '_' +
                  defect.workDate
                "
              >
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td style="text-align: center">
                  <!-- 조치완료 = 조치상세(actionDesc) 존재 (Q3 파생) -->
                  <span
                    class="action-status"
                    :class="defect.actionDesc ? 'is-done' : 'is-todo'"
                  >
                    {{ defect.actionDesc ? "조치완료" : "미조치" }}
                  </span>
                </td>
                <td>{{ defect.siteNm }}</td>
                <td>{{ defect.chkLstTypeNm }}</td>
                <td>{{ defect.chkptNm }}</td>
                <td>{{ defect.inspectItemSubj }}</td>
                <td style="text-align: center">
                  <button
                    class="btn btn-custom"
                    @click="fnDefectDetailPopOpen(defect)"
                  >
                    상세
                  </button>
                </td>
                <td>{{ defect.inspectorNm }}</td>
                <td style="text-align: center">
                  {{ formatYmdDot(defect.workDate) }}
                </td>
                <td style="text-align: center">
                  <button
                    class="btn btn-custom"
                    @click="fnDefectActionPopOpen(defect)"
                  >
                    입력
                  </button>
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
// ================ Imports ================
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import ChkptTargetSearchPop from "@/views/chkLst/popup/ChkptTargetSearchPop.vue";
import InspectItemSearchPop from "@/views/chkLst/popup/InspectItemSearchPop.vue";
import DefectDetailPop from "@/views/chkLst/popup/DefectDetailPop.vue";
import DefectActionInputPop from "@/views/chkLst/popup/DefectActionInputPop.vue";

// ================ Options ================
defineOptions({ name: "ChkLst_04" });

// ================ Props ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs ================
const defectList = ref([]);
const baseCodeArr = ref({});
const localButtons = ref({ ...props.buttons });

// 조회조건
const siteCd = ref("");
const sr_siteNo = ref("");
const sr_siteNm = ref("");
const chkLstType = ref();
// 점검대상 선택값 (팝업 return)
const chkptCd = ref("");
const chkptNm = ref("");
// 점검문항 선택값 (팝업 return)
const inspectItemCd = ref("");
const inspectItemSubj = ref("");
// 조치여부 필터: '' 전체 / 'Y' 조치완료 / 'N' 미조치
const actionStatus = ref("");

// 화면 제어
const siteDisabled = ref(false);

// 점검구분이 전체("")이면 점검대상명칭/점검문항 입력·검색을 비활성
const targetDisabled = computed(() => proxy.$util.isEmpty(chkLstType.value));

// ================ Life Cycle ================
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  sr_siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  sr_siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  // COM001 점검구분 코드 로드(첫 코드를 기본값으로 세팅) 후 초기 조회
  await fnGetBaseinfoList();
  await fnSearch();
});

// ================ API Functions (developer 구현) ================
// COM001 점검구분 코드 조회 (ChkLst_01 동일 패턴)
const fnGetBaseinfoList = async () => {
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

      // 첫 점검구분 코드를 기본값으로 세팅
      const com001 = (baseCodeArr.value.COM001 || []).filter(
        (o) => o.baimValDCd != null
      );
      if (com001.length > 0) {
        chkLstType.value = com001[0].baimValDCd;
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 불량 목록 조회 (정렬은 서버에서 점검일자 최신순)
const fnSearch = async () => {
  defectList.value = [];

  try {
    const response = await axios.get("/webApi/chkLst04/defect-lists", {
      params: {
        siteCd: siteCd.value,
        chkLstType: chkLstType.value,
        chkptCd: chkptCd.value,
        inspectItemCd: inspectItemCd.value,
        actionStatus: actionStatus.value,
      },
    });

    if (response.status === 200) {
      defectList.value = response.data?.defectResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 사업장 단건 조회 (코드/명 직접 입력 후 blur 시)
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: sr_siteNo.value,
        siteNm: sr_siteNm.value,
      },
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// ================ Methods ================
// 사업장 포커스 처리 (ChkLst_01 동일 — 코드/명 단방향 클리어 + 단건 조회)
const focusKill = (e) => {
  if (e.target.id == "sr_siteNo") {
    if (proxy.$util.isEmpty(sr_siteNo.value)) {
      siteCd.value = "";
      sr_siteNm.value = "";
    } else {
      sr_siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "sr_siteNm") {
    if (proxy.$util.isEmpty(sr_siteNm.value)) {
      siteCd.value = "";
      sr_siteNo.value = "";
    } else {
      sr_siteNo.value = "";
      siteFocusKill();
    }
  }
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

// 사업장 단건 조회 결과 처리 (ChkLst_01 동일)
const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();
    if (apiId == "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        onSiteSelected(
          siteList[0].siteCd,
          siteList[0].siteNo,
          siteList[0].siteNm
        );
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = "";
        sr_siteNo.value = "";
        sr_siteNm.value = "";
      }
    }
  }
};

// PRAFTA_COM_001-T5-12.1: 점검구분 변경 시 점검대상명칭/점검문항 선택값 초기화
//   (두 검색 팝업 모두 chkLstType 종속이므로 구분이 바뀌면 하위 선택값을 비운다)
const fnChkLstTypeChange = () => {
  chkptCd.value = "";
  chkptNm.value = "";
  inspectItemCd.value = "";
  inspectItemSubj.value = "";
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  sr_siteNo.value = siteNoVal;
  sr_siteNm.value = siteNmVal;
  // 사업장 변경 시 점검구분을 전체("")로 자동 전환하고 점검대상/점검문항 선택값 초기화
  chkLstType.value = "";
  chkptCd.value = "";
  chkptNm.value = "";
  inspectItemCd.value = "";
  inspectItemSubj.value = "";
};

// 점검대상명칭 사용자 입력: 팝업 선택값을 직접 수정하면 표시값과 코드가 어긋나므로(stale)
//   타이핑 시점에 선택 코드(chkptCd)를 해제해 재선택을 강제한다.
//   (팝업 onSelect 는 ref 직접 대입이라 input 이벤트가 발생하지 않아 정상 선택값은 보존된다)
const onChkptNmInput = () => {
  chkptCd.value = "";
};

// 점검대상명칭 blur: 값이 비면 선택 코드(chkptCd)를 해제 (사업장 코드/명 해제 UX 동형)
const onChkptNmBlur = () => {
  if (proxy.$util.isEmpty(chkptNm.value)) {
    chkptCd.value = "";
  }
};

// 점검문항 사용자 입력: 위와 동일 — 타이핑 시 선택 코드(inspectItemCd)를 해제(stale 차단)
const onInspectItemInput = () => {
  inspectItemCd.value = "";
};

// 점검문항 blur: 값이 비면 선택 코드(inspectItemCd)를 해제
const onInspectItemBlur = () => {
  if (proxy.$util.isEmpty(inspectItemSubj.value)) {
    inspectItemCd.value = "";
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

// 점검대상 검색: 사업장 + 점검구분 선택 후에만 오픈
const fnChkptTargetPopOpen = () => {
  if (
    proxy.$util.isEmpty(siteCd.value) ||
    proxy.$util.isEmpty(chkLstType.value)
  ) {
    proxy.$alert("사업장과 점검구분을 먼저 선택해주세요.");
    return;
  }
  openPop(ChkptTargetSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    chkLstType_p: chkLstType.value,
    onSelect: (chkptCdVal, chkptNmVal) => {
      chkptCd.value = chkptCdVal;
      chkptNm.value = chkptNmVal;
    },
  });
};

// 점검문항 검색: 사업장 + 점검구분 선택 후에만 오픈
const fnInspectItemPopOpen = () => {
  if (
    proxy.$util.isEmpty(siteCd.value) ||
    proxy.$util.isEmpty(chkLstType.value)
  ) {
    proxy.$alert("사업장과 점검구분을 먼저 선택해주세요.");
    return;
  }
  openPop(InspectItemSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    chkLstType_p: chkLstType.value,
    onSelect: (itemCdVal, itemSubjVal) => {
      inspectItemCd.value = itemCdVal;
      inspectItemSubj.value = itemSubjVal;
    },
  });
};

// 불량내용 상세 (비고 + 첨부사진)
const fnDefectDetailPopOpen = (defect) => {
  openPop(DefectDetailPop, {
    inspectItemSubj_p: defect.inspectItemSubj,
    workDate_p: defect.workDate,
    answerDesc_p: defect.answerDesc,
    fileMgmtCd_p: defect.fileMgmtCd,
    filePath_p: defect.filePath,
  });
};

// 조치 입력 (upsert) — 저장 후 목록 갱신
const fnDefectActionPopOpen = (defect) => {
  openPop(DefectActionInputPop, {
    siteCd_p: defect.siteCd,
    chkptCd_p: defect.chkptCd,
    inspectItemCd_p: defect.inspectItemCd,
    workDate_p: defect.workDate,
    actionDesc_p: defect.actionDesc,
    onSaved: () => {
      fnSearch();
    },
  });
};
</script>

<style scoped>
/* table.css 의 .data-grid button 전역 border 가 .btn-custom 색을 덮으므로
   테이블 내부에서도 동일한 border 가 유지되도록 specificity 보강 (ChkLst_01 동일) */
.data-grid .btn.btn-custom {
  border-color: var(--color-primary);
}

.action-status {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: var(--btn-radius);
  font-size: 0.8rem;
  font-weight: 600;
}

.action-status.is-done {
  color: var(--color-primary);
}

.action-status.is-todo {
  color: var(--color-danger);
}
</style>
