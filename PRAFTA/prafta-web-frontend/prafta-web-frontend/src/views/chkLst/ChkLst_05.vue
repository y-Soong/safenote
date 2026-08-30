<template>
  <div class="viewComm">
    <!-- 탭: 점검응답 이력 / 불량조치 이력 (페이지 레벨 탭 — Attd_01 표준에 따라 화면명 위 배치) -->
    <div class="hist-tabs">
      <button
        type="button"
        class="hist-tab"
        :class="{ 'is-active': activeTab === 'answer' }"
        @click="fnChangeTab('answer')"
      >
        점검응답 이력
      </button>
      <button
        type="button"
        class="hist-tab"
        :class="{ 'is-active': activeTab === 'defect' }"
        @click="fnChangeTab('defect')"
      >
        불량조치 이력
      </button>
    </div>

    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
    <div class="viewSearch">
      <!-- 사업장(필수): 코드 - 검색버튼 - 명칭 (ChkLst_04 동일 구조) -->
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

      <!-- 점검대상명칭 (선택) -->
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

      <!-- 점검일자 범위(일 단위 — 감사 정밀성) -->
      <div>
        <label>점검일자</label>
        <CalendarSrch :range="false" style="width: 130px" v-model="fromDate" />
        <span class="date-range-sep">~</span>
        <CalendarSrch :range="false" style="width: 130px" v-model="toDate" />
      </div>
    </div>

    <!-- 결과 그리드: 좌표별 그룹 캡션 + 타임라인 행 -->
    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">덮어쓰기 타임라인 (좌표별)</span>

          <!-- 조회 규모 요약 + 덮어쓰기 필터. 이 화면의 목적이 '덮어쓰기 추적'이라
               덮어쓴 적 없는 단건 좌표는 기본으로 접어 둔다(체크 해제 시 전체 노출). -->
          <span class="hist-toolbar">
            <span class="hist-summary">
              좌표 <b>{{ histSummary.groupCnt }}</b
              >개 · 덮어쓰기
              <b class="is-warn">{{ histSummary.overwriteCnt }}</b
              >건
            </span>
            <label class="hist-toggle">
              <input type="checkbox" v-model="overwriteOnly" />
              덮어쓰기만 보기
              <span
                v-if="overwriteOnly && hiddenGroupCnt > 0"
                class="hist-hint"
              >
                (단건 {{ hiddenGroupCnt }}개 숨김)
              </span>
            </label>
          </span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <!-- 좌표(점검대상·문항·일자)는 캡션 행이 아니라 좌측 컬럼으로 두고 동일 좌표의 연속 행을
               rowspan 으로 묶는다. 종전 '캡션행 + 데이터행' 구조는 이력이 1건인 좌표도 2행을 차지해
               대부분의 화면이 2행 페어로 채워졌다(1건 = 1행이 되도록 전환).
               사업장은 조회조건상 단일 지정이라 컬럼에서 제외(요약 영역에 표기). -->
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="width: 13%">점검대상</th>
                <th style="width: 19%">점검문항</th>
                <th style="width: 8%">점검일자</th>
                <th style="width: 10%">시각</th>
                <th style="width: 14%">
                  {{ activeTab === "answer" ? "수행자(ID)" : "조치자(ID)" }}
                </th>
                <th style="width: 11%">회사</th>
                <th style="width: 15%">
                  {{ activeTab === "answer" ? "점검결과" : "조치내용" }}
                </th>
                <th style="width: 5%">사진</th>
                <th style="width: 10%">변경유형</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!visibleGroups || visibleGroups.length === 0">
                <td colspan="9" class="edu-grid-empty">
                  {{
                    overwriteOnly && hiddenGroupCnt > 0
                      ? "덮어쓴 이력이 없습니다. (단건 " +
                        hiddenGroupCnt +
                        "개는 '덮어쓰기만 보기' 해제 시 표시)"
                      : "조회된 이력이 없습니다."
                  }}
                </td>
              </tr>
              <template
                v-for="(group, gi) in visibleGroups"
                v-else
                :key="group.key"
              >
                <!-- 타임라인 행(오름차순). 좌표 3칸은 그룹 첫 행에서만 rowspan 으로 출력. -->
                <tr
                  v-for="(row, ri) in group.rows"
                  :key="gi + '_' + ri"
                  :class="{
                    'is-group-start': ri === 0,
                    'is-overwrite-row': row.chgType === '02',
                  }"
                >
                  <td
                    v-if="ri === 0"
                    :rowspan="group.rows.length"
                    class="coord-cell"
                  >
                    {{ group.chkptNm || "-" }}
                  </td>
                  <td
                    v-if="ri === 0"
                    :rowspan="group.rows.length"
                    class="coord-cell"
                  >
                    {{ group.inspectItemSubj || "-" }}
                    <span v-if="group.rows.length > 1" class="coord-count">
                      {{ group.rows.length }}회
                    </span>
                  </td>
                  <td
                    v-if="ri === 0"
                    :rowspan="group.rows.length"
                    class="coord-cell"
                  >
                    {{ formatYmdDot(group.workDate) }}
                  </td>

                  <td>{{ row.chgDtime }}</td>
                  <td :class="{ 'is-changed': row.performerChanged }">
                    {{ row.performUserNm || "-"
                    }}<span v-if="row.performUserCd" class="perform-id"
                      >({{ row.performUserCd }})</span
                    >
                  </td>
                  <td>{{ row.performCmpnyNm || "-" }}</td>
                  <td :class="{ 'is-changed': row.valueChanged }">
                    <template v-if="activeTab === 'answer'">
                      <span
                        class="answer-badge"
                        :class="
                          row.inspectAnswerType === 'N' ? 'is-bad' : 'is-ok'
                        "
                      >
                        {{ row.inspectAnswerType === "N" ? "불량" : "양호" }}
                      </span>
                      <!-- 불량 → 양호 로 뒤집힌 덮어쓰기는 감사상 핵심 케이스라 별도 표식. -->
                      <span
                        v-if="row.isRevert"
                        class="revert-flag"
                        title="이전 '불량' 판정을 '양호'로 덮어썼습니다"
                      >
                        불량 해제
                      </span>
                    </template>
                    <template v-else>
                      <span class="desc-preview" :title="row.actionDesc || ''">
                        {{ row.actionDesc || "-" }}
                      </span>
                    </template>
                  </td>
                  <td style="text-align: center">
                    <button
                      v-if="row.photoYn === 'Y'"
                      class="btn btn-custom"
                      @click="fnPhotoPopOpen(group, row)"
                    >
                      사진
                    </button>
                    <span v-else>-</span>
                  </td>
                  <td style="text-align: center">
                    <span
                      class="chg-badge"
                      :class="row.chgType === '02' ? 'is-overwrite' : 'is-new'"
                    >
                      {{ row.chgTypeNm }}
                    </span>
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
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import DefectDetailPop from "@/views/chkLst/popup/DefectDetailPop.vue";

defineOptions({ name: "ChkLst_05" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// 조회조건
const siteCd = ref("");
const sr_siteNo = ref("");
const sr_siteNm = ref("");
const chkLstType = ref("");
const chkptCd = ref("");
const chkptNm = ref("");
const fromDate = ref("");
const toDate = ref("");

// 점검구분 코드(COM001)
const baseCodeArr = ref({});

// 탭 / 결과
const activeTab = ref("answer"); // answer | defect
const histList = ref([]); // 서버 원본(점검일자 내림차순 · 좌표 내 타임라인 오름차순)

// 화면 제어
const siteDisabled = ref(false);
const localButtons = ref({ ...props.buttons });

// 점검구분이 전체("")이면 점검대상 검색 비활성
const targetDisabled = computed(() => proxy.$util.isEmpty(chkLstType.value));

// 덮어쓰기 필터 — 이 화면의 목적이 '덮어쓰기 추적'이므로 기본 ON.
//   OFF 로 바꾸면 덮어쓴 적 없는 단건 좌표까지 전부 노출한다.
const overwriteOnly = ref(true);

// 좌표(사업장·점검대상·문항·일자) 단위 그룹핑 결과
//   서버가 (workDate 내림차순, siteNm, chkptNm, inspectItemSubj, actionDtime 오름차순)으로 정렬해
//   내려주므로 순차 스캔하며 좌표 키가 바뀔 때 새 그룹을 연다.
//   → 화면 정렬축 = 점검일자 최신순(위가 최신), 좌표 내부는 타임라인 오름차순 보존.
//   그룹을 닫을 때 직전 행 대비 변경점(valueChanged/performerChanged/isRevert)을 함께 표시해 둔다 —
//   스냅샷만 나열하면 "무엇이 바뀌었는지"를 사용자가 위아래로 눈 비교해야 하기 때문.
const groupedList = computed(() => {
  const groups = [];
  const indexByKey = {};
  const isAnswerTab = activeTab.value === "answer";
  (histList.value || []).forEach((row) => {
    const key = [row.siteCd, row.chkptCd, row.inspectItemCd, row.workDate].join(
      "|"
    );
    let gi = indexByKey[key];
    if (gi === undefined) {
      gi = groups.length;
      indexByKey[key] = gi;
      groups.push({
        key,
        siteNm: row.siteNm,
        chkptNm: row.chkptNm,
        inspectItemSubj: row.inspectItemSubj,
        workDate: row.workDate,
        hasOverwrite: false,
        rows: [],
      });
    }
    const g = groups[gi];
    const prev = g.rows.length ? g.rows[g.rows.length - 1] : null;
    // 탭별 '값' 정의: 응답 = 양호/불량 판정, 조치 = 조치내용 텍스트.
    const curVal = isAnswerTab ? row.inspectAnswerType : row.actionDesc;
    const prevVal = prev
      ? isAnswerTab
        ? prev.inspectAnswerType
        : prev.actionDesc
      : null;
    g.rows.push({
      ...row,
      // 첫 행은 비교 대상이 없으므로 변경 표시하지 않는다.
      valueChanged: !!prev && curVal !== prevVal,
      performerChanged: !!prev && row.performUserCd !== prev.performUserCd,
      // 불량('N') → 양호 로 뒤집힌 덮어쓰기 (응답 탭 전용, 감사상 핵심 케이스)
      isRevert:
        isAnswerTab &&
        !!prev &&
        prev.inspectAnswerType === "N" &&
        row.inspectAnswerType !== "N",
    });
    if (row.chgType === "02") g.hasOverwrite = true;
  });
  return groups;
});

// 실제 표에 그릴 그룹 — 필터 ON 이면 덮어쓰기 있는 좌표만.
const visibleGroups = computed(() =>
  overwriteOnly.value
    ? groupedList.value.filter((g) => g.hasOverwrite)
    : groupedList.value
);

// 필터로 감춰진 단건 좌표 수(안내 문구용).
const hiddenGroupCnt = computed(
  () => groupedList.value.length - visibleGroups.value.length
);

// 조회 규모 요약 — 좌표 수 / 덮어쓰기 건수(행 기준).
const histSummary = computed(() => ({
  groupCnt: groupedList.value.length,
  overwriteCnt: (histList.value || []).filter((r) => r.chgType === "02").length,
}));

// ===== 기본값 세팅 =====
// CalendarSrch 는 YYYY-MM-DD 문자열 v-model 이다. 기본값 = 최근 1개월(from) ~ 오늘(to).
const toIsoDate = (d) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  sr_siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  sr_siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";

  const to = new Date();
  const from = new Date();
  from.setMonth(from.getMonth() - 1);
  fromDate.value = toIsoDate(from);
  toDate.value = toIsoDate(to);

  // 읽기전용 감사화면 — 조회 버튼만 노출(생성/저장/삭제/엑셀 숨김).
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

onMounted(async () => {
  fnInit();
  await fnGetBaseinfoList();
  await fnSearch();
});

// ===== COM001 점검구분 코드 조회(ChkLst_04 동일 패턴) =====
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
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      baseCodeArr.value = grouped;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// ===== 조회 =====
// 사업장 필수 + 기간(YYYYMMDD 변환). activeTab 에 따라 응답/불량 엔드포인트 호출.
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  if (
    proxy.$util.isEmpty(fromDate.value) ||
    proxy.$util.isEmpty(toDate.value)
  ) {
    await proxy.$alert("점검일자 기간을 선택해주세요.");
    return;
  }

  histList.value = [];

  const url =
    activeTab.value === "answer"
      ? "/webApi/chkLst05/answer-hists"
      : "/webApi/chkLst05/defect-hists";
  const listKey =
    activeTab.value === "answer" ? "answerHistList" : "defectHistList";

  try {
    const response = await axios.get(url, {
      params: {
        siteCd: siteCd.value,
        fromWorkDate: String(fromDate.value).replaceAll("-", ""),
        toWorkDate: String(toDate.value).replaceAll("-", ""),
        chkptCd: chkptCd.value,
        inspectItemCd: "",
      },
    });

    if (response.status === 200) {
      histList.value = response.data?.[listKey] || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnChangeTab = (tab) => {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  // 탭 전환 시 해당 탭 데이터로 재조회.
  fnSearch();
};

// ===== 사업장 검색(ChkLst_04 패턴 승계) =====
const focusKill = (e) => {
  if (e.target.id == "sr_siteNo") {
    if (proxy.$util.isEmpty(sr_siteNo.value)) {
      siteCd.value = "";
      sr_siteNm.value = "";
    } else {
      sr_siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id == "sr_siteNm") {
    if (proxy.$util.isEmpty(sr_siteNm.value)) {
      siteCd.value = "";
      sr_siteNo.value = "";
    } else {
      sr_siteNo.value = "";
      fnSrchSiteInfo();
    }
  }
};

// 사업장 코드/명 직접 입력 후 단건 조회(ChkLst_04 동일).
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
        siteCd.value = "";
        sr_siteNo.value = "";
        sr_siteNm.value = "";
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  sr_siteNo.value = siteNoVal;
  sr_siteNm.value = siteNmVal;
  // 사업장 변경 시 점검구분/점검대상 선택값 초기화.
  chkLstType.value = "";
  chkptCd.value = "";
  chkptNm.value = "";
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

// ===== 점검구분/점검대상 =====
const fnChkLstTypeChange = () => {
  chkptCd.value = "";
  chkptNm.value = "";
};
const onChkptNmInput = () => {
  chkptCd.value = "";
};
const onChkptNmBlur = () => {
  if (proxy.$util.isEmpty(chkptNm.value)) chkptCd.value = "";
};
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

// ===== 상세 열람(읽기전용, DefectDetailPop 재사용) =====
const fnPhotoPopOpen = (group, row) => {
  openPop(DefectDetailPop, {
    inspectItemSubj_p: group.inspectItemSubj,
    workDate_p: formatYmdDot(group.workDate),
    answerDesc_p:
      activeTab.value === "answer" ? row.answerDesc : row.actionDesc,
    fileMgmtCd_p: row.fileMgmtCd,
    filePath_p: row.filePath,
  });
};
</script>

<style scoped>
/* 탭 바 — 웹 표준(Attd_01 .attd01-tab-bar/.attd01-tab-btn 스펙, 밑줄형 14px) 준수 */
.hist-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.hist-tab {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.hist-tab:hover {
  color: var(--color-text, #374151);
}
.hist-tab.is-active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}

/* 점검일자 from~to 구분자 (User_05 .date-range-sep 표준) */
.date-range-sep {
  margin: 0 0.4rem;
  color: var(--color-text-muted, #6b7280);
}

/* 소제목 바를 풀폭 flex 로 전환 — 우측에 요약/필터 툴바를 붙이기 위함
   (table.css 전역은 inline-flex 라 폭이 내용에 맞춰져 margin-left:auto 가 먹지 않는다). */
.subtitle-pane .subtitle {
  display: flex;
  width: 100%;
}

/* 서브타이틀 우측 요약/필터 툴바 */
.hist-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
  margin-left: auto;
  font-size: 0.8125rem;
  font-weight: 400;
  color: var(--color-text-muted, #6b7280);
}
.hist-summary b {
  color: var(--color-text-strong, #111827);
}
.hist-summary b.is-warn {
  color: var(--color-warning-text, #92400e);
}
.hist-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  user-select: none;
}
.hist-hint {
  color: var(--color-text-muted, #9ca3af);
}

/* 좌표 컬럼(rowspan 병합 셀) — 타임라인 칸과 시각적으로 구분 */
.coord-cell {
  background: var(--color-bg);
  color: var(--color-text-strong);
  font-weight: 600;
  vertical-align: top;
}
/* 같은 좌표가 여러 번 기록된 경우 회수를 함께 표기(덮어쓰기 존재 신호) */
.coord-count {
  display: inline-block;
  margin-left: 0.35rem;
  padding: 0.05rem 0.35rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  font-size: 0.75rem;
  font-weight: 600;
}
/* 그룹 경계선 — 좌표가 바뀌는 지점을 굵게 */
.is-group-start > td {
  border-top: 2px solid var(--color-border, #e5e7eb);
}
/* 직전 행 대비 값이 바뀐 셀 강조 */
.is-changed {
  color: var(--color-warning-text, #92400e);
  font-weight: 600;
}
/* 불량 → 양호 뒤집힘 표식 */
.revert-flag {
  display: inline-block;
  margin-left: 0.35rem;
  padding: 0.05rem 0.35rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-danger);
  font-size: 0.75rem;
  font-weight: 600;
}
/* 불량조치 탭 — 조치내용 인라인 미리보기(말줄임, hover 시 title 로 전문 노출) */
.desc-preview {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

/* 수행자 ID 보조 표기 */
.perform-id {
  margin-left: 0.25rem;
  color: var(--color-text-muted);
  font-size: 0.85em;
}

/* 점검결과 배지 */
.answer-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: var(--btn-radius);
  font-size: 0.8rem;
  font-weight: 600;
}
.answer-badge.is-ok {
  color: var(--color-primary);
}
.answer-badge.is-bad {
  color: var(--color-danger);
}

/* 변경유형 배지 — 덮어쓰기 강조 */
.chg-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: var(--btn-radius);
  font-size: 0.8rem;
  font-weight: 600;
}
.chg-badge.is-new {
  color: var(--color-text-muted);
}
.chg-badge.is-overwrite {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

/* table.css 전역 border 보강(ChkLst_04 동일) */
.data-grid .btn.btn-custom {
  border-color: var(--color-primary);
}
</style>
