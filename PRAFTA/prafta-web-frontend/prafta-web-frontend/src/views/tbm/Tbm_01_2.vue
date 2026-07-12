<template>
  <div class="tbm-ai-manage">
    <!-- 탭 자식(Attd_01_1 표준): 자식이 자기 ViewHeader 를 소유한다.
         이 탭에는 생성/저장/삭제/엑셀 대상이 없어 조회만 노출한다. -->
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="headerButtons"
      @search="fnSearch"
    />

    <!-- 검색/필터 (Tbm_01 .viewSearch 패턴) -->
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
        <label>교육자료명</label>
        <input
          v-model.trim="mtrlTitle"
          type="text"
          @input="fnDebouncedSearch"
        />
      </div>
      <div>
        <label>첨부파일명</label>
        <input v-model.trim="fileNm" type="text" @input="fnDebouncedSearch" />
      </div>
      <div>
        <label>AI 상태</label>
        <select v-model.trim="statusFilter" name="combo" @change="fnSearch">
          <option value="">전체</option>
          <option
            v-for="opt in statusOptions"
            :key="opt.code"
            :value="opt.code"
          >
            {{ opt.label }}
          </option>
        </select>
      </div>
      <div>
        <label>항목타입</label>
        <select v-model.trim="itemTypeFilter" name="combo" @change="fnSearch">
          <option value="">전체</option>
          <option
            v-for="opt in (systCodeArr['SYS018'] || []).filter(
              (o) => o.systValDCd != null
            )"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <!-- 단일 평면 테이블. 스코프(회사공통/사업장)와 자료명은 행 컬럼으로 표기한다
           (기존 자료별 그룹 박스는 폐기 — 자료 수가 늘면 세로로 길어지고 정렬/검색이 어렵다). -->
      <div class="table-wrapper subtitle-pane">
        <!-- ⬇️ 소제목 바 (Tbm_01_1 동일 마크업) -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">첨부파일 목록</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="text-align: center; width: 4%">No</th>
                <th style="text-align: center; width: 9%">스코프</th>
                <th style="width: 16%">교육자료명</th>
                <th style="width: 10%">항목 타입</th>
                <th>자료 설명</th>
                <th style="width: 16%">첨부파일명</th>
                <th style="width: 14%; text-align: center">AI 상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!worklist.length">
                <td colspan="7" class="tbm-ai-empty">
                  관리 대상 AI 분석 항목이 없습니다.
                </td>
              </tr>
              <tr
                v-for="(item, idx) in worklist"
                :key="item.mtrlItemCd"
                class="tbm-ai-row"
                @dblclick="fnOpenItem(item)"
              >
                <td style="text-align: center">{{ rowNo(idx) }}</td>
                <td style="text-align: center">
                  <span
                    class="tbm-ai-scope"
                    :class="
                      isCommonRow(item)
                        ? 'tbm-ai-scope--common'
                        : 'tbm-ai-scope--site'
                    "
                  >
                    {{
                      isCommonRow(item)
                        ? "회사공통"
                        : resolveSiteNm(item.siteCd)
                    }}
                  </span>
                </td>
                <td class="tbm-ai-desc">{{ item.title }}</td>
                <td>{{ itemTypeLabel(item.mtrlItemType) }}</td>
                <td class="tbm-ai-desc">{{ item.mtrlDesc }}</td>
                <!-- 첨부파일명(TB_FILE_INFO.FILE_NM). 이미지/PDF 등 첨부가 없는 항목은 '-' -->
                <td class="tbm-ai-desc">{{ item.fileNm || "-" }}</td>
                <!-- AI 분석 지정(AI_ANALYZE_YN='Y') 항목만 조회되므로 지정 여부 컬럼은 두지 않는다.
                     01·04 는 VLM 자동분석, 02·03 은 관리자 입력을 AI 분석내용으로 취급(플로우 동일). -->
                <td style="text-align: center">
                  <button
                    type="button"
                    class="tbm-ai-badge"
                    :class="badgeClass(item)"
                    @click.stop="fnOpenItem(item)"
                  >
                    {{ statusLabel(item) }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 페이징 (총 건수가 1페이지를 넘을 때만 노출 — Tbm_03 .pager 패턴) -->
      <div v-if="totalPages > 1" class="pager">
        <button
          class="btn btn-second btn-sm"
          :disabled="page <= 1"
          @click="fnGoPage(page - 1)"
        >
          이전
        </button>
        <span class="pager-info">
          {{ page }} / {{ totalPages }} (총 {{ totalCount }}건)
        </span>
        <button
          class="btn btn-second btn-sm"
          :disabled="page >= totalPages"
          @click="fnGoPage(page + 1)"
        >
          다음
        </button>
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
  onMounted,
  defineOptions,
  defineProps,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import ViewHeader from "@/components/common/ViewHeader.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import TbmItemAiPanel from "./popup/TbmItemAiPanel.vue";
import TbmItemTextConfirmPop from "./popup/TbmItemTextConfirmPop.vue";

// ================ Options ================
defineOptions({ name: "Tbm_01_2" });

// ================ Props & Emits ================
// 셸(Tbm_01)이 라우터에서 받은 title/buttons 를 그대로 내려준다(Attd_01 표준).
const props = defineProps({
  title: String,
  buttons: Object,
});

// 이 탭에는 생성/저장/삭제/엑셀 대상이 없다 — 조회만 노출.
const headerButtons = computed(() => ({
  ...(props.buttons || {}),
  search: "Y",
  create: "N",
  save: "N",
  delete: "N",
  excel: "N",
}));

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
// worklist: analysis-worklist 응답의 세부항목 행 배열(자료명 포함). RA 계약(items[]) 기준.
const worklist = ref([]);
const siteList = ref([]);
const systCodeArr = ref({}); // SYS018(항목타입) / SYS056(AI상태)

// 조회조건
// 사업장: siteCd 는 숨김 실제키(서버 전달용), siteNo/siteNm 은 화면 표시값(Attd_01_1 표준).
//   빈 값이면 전체(회사공통 + 전 사업장) 조회.
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const mtrlTitle = ref("");
const fileNm = ref(""); // 첨부파일명(TB_FILE_INFO.FILE_NM) LIKE 검색
const statusFilter = ref("");
const itemTypeFilter = ref("");

// 자료명/첨부파일명 입력 디바운스
let searchDebounceTimer = null;

// 페이징(EP page/size 지원). 서버 상한(100) 초과분 침묵 누락 방지 — totalCount 실사용.
//   페이지 크기는 서버 상한과 동일하게 두어 <100 건은 단일 페이지로 기존 동작 유지,
//   101건 이상일 때만 페이저가 노출된다.
const page = ref(1);
const pageSize = ref(100);
const totalCount = ref(0);

// SYS056 폴백 라벨(systCode 로드 실패 시).
// ★NONE 은 statusLabel 파생 규칙(AI_ANALYZE_YN·항목타입 조합)이 전담하므로 폴백맵에서 제외했다
//   (도달 불가). SYS056 코드값 자체는 신규 추가하지 않고 FE 파생만 수행한다.
const AI_STATUS_LABEL = {
  ANALYZING: "분석중",
  DRAFT: "분석완료-미확정",
  FAILED: "분석실패",
  CONFIRMED: "분석완료-확정",
};

// AI 상태 필터의 의사코드(SYS056 미등록). 저장값은 둘 다 AI_STATUS='NONE' 이고 항목타입으로만 갈리므로
// (statusLabel 파생 규칙과 동일), 서버 WHERE 가 이 두 코드를 NONE + 타입조합으로 해석한다.
const PSEUDO_STATUS_OPTIONS = [
  { code: "PENDING_ANALYZE", label: "분석대기" },
  { code: "PENDING_CONFIRM", label: "관리자 확정 대기" },
];

// ================ Computed ================
// AI 상태 필터 옵션.
//  - syst-info-lists 는 코드마스터마다 systValDCd=null 인 '전체' 행을 UNION 으로 덧붙인다.
//    템플릿이 이미 '전체'를 고정 노출하므로 null 행은 제외한다(중복 노출 방지).
//  - NONE('분석대상아님')은 이 화면 조회 대상(AI_ANALYZE_YN='Y')에 없는 라벨이라 제외하고,
//    그리드 배지와 동일한 대기 라벨 2종으로 대체한다.
const statusOptions = computed(() => [
  ...PSEUDO_STATUS_OPTIONS,
  ...(systCodeArr.value["SYS056"] || [])
    .filter((o) => o.systValDCd != null && o.systValDCd !== "NONE")
    .map((o) => ({ code: o.systValDCd, label: o.systValDNm })),
]);

// 총 페이지 수(totalCount 기준)
const totalPages = computed(() => {
  const pages = Math.ceil((totalCount.value || 0) / pageSize.value);
  return pages < 1 ? 1 : pages;
});

// ================ Life Cycle Functions ================
onMounted(async () => {
  await fnGetSystinfoList(); // SYS018, SYS056
  await fnGetSiteList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS018", "SYS056"],
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

// 검색 트리거(필터 변경/디바운스/확정 후): 조건 변경 시 1페이지로 복귀 후 조회.
//   페이지 이동(fnGoPage)은 선택 페이지를 유지해야 하므로 조회 본체를 fnFetchWorklist 로 분리.
const fnSearch = async () => {
  page.value = 1;
  await fnFetchWorklist();
};

// 워크리스트 조회 본체(현재 page 유지). totalCount 를 실사용하여 페이저 산출.
const fnFetchWorklist = async () => {
  worklist.value = [];

  try {
    const response = await axios.get("/webApi/tbmai01/analysis-worklist", {
      params: {
        // 사업장 스코프는 서버측(RA)에서 강제 — FE 는 필터값만 전달.
        siteCd: siteCd.value || "",
        keyword: mtrlTitle.value || "",
        fileNm: fileNm.value || "",
        aiStatus: statusFilter.value || "",
        mtrlItemType: itemTypeFilter.value || "",
        page: page.value,
        size: pageSize.value,
      },
    });

    if (response.status === 200) {
      worklist.value = response.data?.items || [];
      totalCount.value = response.data?.totalCount || 0;
    }
  } catch (err) {
    worklist.value = [];
    totalCount.value = 0;
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 페이지 이동(현재 조건 유지)
const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
  fnFetchWorklist();
};

// 자료명 디바운스(500ms)
const fnDebouncedSearch = () => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    fnSearch();
  }, 500);
};

// 02/03 관리자 의견 프리필: 워크리스트는 확정서술 유무(hasConfirmDesc)만 주므로,
// 기존 확정건 재편집 시 analysis-status 로 실제 확정서술(aiConfirmDesc)을 불러온다.
const fnLoadConfirmDesc = async (mtrlCd, mtrlItemCd) => {
  try {
    const response = await axios.get("/webApi/tbmai01/analysis-status", {
      params: { mtrlCd },
    });
    const items = response.data?.items || [];
    const found = items.find((it) => it && it.mtrlItemCd === mtrlItemCd);
    return found ? found.aiConfirmDesc || "" : "";
  } catch (err) {
    // 프리필 실패는 치명적이지 않음 — 빈 값으로 진행(관리자 재입력 가능)
    return "";
  }
};

// ================ User Functions ================
// ── 사업장 3요소 입력(코드/찾기/명) — Attd_01_1 표준 ──────────
// 코드나 명을 직접 입력하고 blur 하면 site-lists 로 조회한다.
//   1건이면 자동 채움, 다건이면 팝업, 0건이면 초기화(= 전체 조회).
// 선택/초기화가 확정되는 시점에만 fnSearch 를 태운다(기존 select @change 트리거 대체).
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      fnSearch();
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      fnSearch();
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
    if (response.status === 200) await fnSiteCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSiteCallback = async (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId !== "site-lists") return;

  const list = res.data?.siteInfoResultList ?? [];
  if (list.length === 1) {
    siteCd.value = list[0].siteCd;
    siteNo.value = list[0].siteNo;
    siteNm.value = list[0].siteNm;
    await fnSearch();
  } else if (list.length > 1) {
    fnSiteSearchPopOpen();
  } else {
    siteCd.value = "";
    siteNo.value = "";
    siteNm.value = "";
    await fnSearch();
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  fnSearch();
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

// 평면 테이블 행번호 — 현재 페이지 오프셋을 더해 전체 기준 번호를 매긴다.
const rowNo = (idx) => (page.value - 1) * pageSize.value + idx + 1;

// 회사공통 행 판정 — 자료(MTRL)의 SITE_CD 가 비어 있으면 회사공통.
const isCommonRow = (item) => proxy.$util.isEmpty(item?.siteCd);

// 사업장코드 → 사업장명(siteList 기준)
const resolveSiteNm = (code) => {
  if (proxy.$util.isEmpty(code)) return "사업장";
  const found = (siteList.value || []).find((s) => s.siteCd === code);
  return found ? found.siteNm : "사업장";
};

// SYS018 라벨(항목타입 코드 → 한글)
const itemTypeLabel = (type) => {
  const list = systCodeArr.value["SYS018"] || [];
  const found = list.find((o) => o.systValDCd === type);
  return found ? found.systValDNm : type;
};

// AI 상태 라벨 파생: 서버가 AI_ANALYZE_YN='Y' 행만 내려주므로 항목타입과 상태만으로 결정한다.
//  - 진행/완료 상태(ANALYZING/DRAFT/FAILED/CONFIRMED)면 SYS056 라벨(systCode 우선, 폴백맵).
//  - 상태가 비었거나 'NONE'(미분석)이면 타입별 대기 라벨로 파생:
//      01(이미지)/04(PDF) → "분석대기"(VLM 자동분석 대상),
//      02(동영상)/03(유튜브) → "관리자 확정 대기"(자동분석 없이 manual-confirm 으로 확정).
//  ★SYS056 에 신규 코드를 추가하지 않는다 — 상태값 자체는 여전히 NONE 이라 백엔드 재큐잉 WHERE 와 정합.
const statusLabel = (item) => {
  const code = item?.aiStatus || "NONE";
  if (code === "NONE") {
    return isAnalyzable(item?.mtrlItemType) ? "분석대기" : "관리자 확정 대기";
  }
  const list = systCodeArr.value["SYS056"] || [];
  const found = list.find((o) => o.systValDCd === code);
  return found ? found.systValDNm : AI_STATUS_LABEL[code] || code;
};

// 상태별 배지 색상.
const badgeClass = (item) => {
  const code = item?.aiStatus || "NONE";
  return {
    "tbm-ai-badge--pending": code === "NONE",
    "tbm-ai-badge--analyzing": code === "ANALYZING",
    "tbm-ai-badge--draft": code === "DRAFT",
    "tbm-ai-badge--failed": code === "FAILED",
    "tbm-ai-badge--confirmed": code === "CONFIRMED",
  };
};

// 분석 대상(이미지 01 / PDF 04)
const isAnalyzable = (type) => type === "01" || type === "04";

// 항목 진입: 01/04 → 대화형 확정 패널, 02/03 → 관리자 의견 팝업.
//  상태별 진입 규칙은 각 팝업이 내부적으로 강제(ANALYZING=열람만/blur, CONFIRMED=재대화 가능).
//  팝업 확정(onConfirmed) 후 워크리스트 재조회로 배지/지정값 갱신.
// 행 @dblclick 과 배지 @click.stop 이 겹쳐 배지 더블클릭 시 모달이 중복 오픈되는 것을
// 방지하기 위한 짧은 재진입 가드(Tbm_01 detailPopOpening 패턴 이식).
let itemPopOpening = false;
const fnOpenItem = async (item) => {
  if (!item || proxy.$util.isEmpty(item.mtrlItemCd)) return;
  if (itemPopOpening) return;
  itemPopOpening = true;
  setTimeout(() => {
    itemPopOpening = false;
  }, 300);

  const type = item.mtrlItemType;
  // 확정 후에는 현재 페이지를 유지한 채 갱신(fnFetchWorklist).
  if (isAnalyzable(type)) {
    openPop(TbmItemAiPanel, {
      mtrlCd_p: item.mtrlCd,
      mtrlItemCd_p: item.mtrlItemCd,
      itemType_p: type,
      fileUrl_p: item.fileUrl || "",
      fileNm_p: item.fileNm || "",
      onConfirmed: fnFetchWorklist,
    });
  } else {
    // 동영상(02)·유튜브(03): 확정서술이 이미 있으면 재편집 프리필.
    let prefill = item.mtrlDesc || "";
    if (item.hasConfirmDesc === "Y") {
      const desc = await fnLoadConfirmDesc(item.mtrlCd, item.mtrlItemCd);
      if (desc) prefill = desc;
    }
    openPop(TbmItemTextConfirmPop, {
      mtrlCd_p: item.mtrlCd,
      mtrlItemCd_p: item.mtrlItemCd,
      itemType_p: type,
      desc_p: prefill,
      onConfirmed: fnFetchWorklist,
    });
  }
};
</script>

<style scoped>
.tbm-ai-manage {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* empty */
.tbm-ai-empty {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

/* 스코프 배지 (Tbm_01 .scope-badge 계열) */
.tbm-ai-scope {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  line-height: 1.6;
  white-space: nowrap;
}

.tbm-ai-scope--common {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.tbm-ai-scope--site {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

/* 행 */
.tbm-ai-row {
  cursor: pointer;
}

.tbm-ai-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tbm-ai-na {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

/* AI 상태 배지 */
.tbm-ai-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  line-height: 1.6;
  white-space: nowrap;
  cursor: pointer;
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.tbm-ai-badge:hover {
  border-color: var(--color-primary);
}

/* 대기(분석대기/관리자 확정 대기): 기본(muted)·확정(primary)과 구분되는 중립 강조. */
.tbm-ai-badge--pending {
  background: var(--color-surface);
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
}

.tbm-ai-badge--analyzing,
.tbm-ai-badge--draft {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-bg);
}

.tbm-ai-badge--failed {
  background: var(--color-surface);
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.tbm-ai-badge--confirmed {
  background: var(--color-surface);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

/* 페이징 (Tbm_03 .pager 패턴 이식, 토큰만 사용) */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.pager-info {
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
