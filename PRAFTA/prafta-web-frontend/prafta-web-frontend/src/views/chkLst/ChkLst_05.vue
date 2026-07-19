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
        <button class="search-btn" :disabled="siteDisabled" @click="fnSiteSearchPopOpen">
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
            v-for="opt in (baseCodeArr['COM001'] || []).filter((o) => o.baimValDCd != null)"
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
        <button class="search-btn" :disabled="targetDisabled" @click="fnChkptTargetPopOpen">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
      </div>

      <!-- 점검일자 범위(일 단위 — 감사 정밀성) -->
      <div>
        <label>점검일자</label>
        <CalendarSrch :range="false" style="width: 130px" v-model="fromDate" />
        -
        <CalendarSrch :range="false" style="width: 130px" v-model="toDate" />
      </div>
    </div>

    <!-- 탭: 점검응답 이력 / 불량조치 이력 -->
    <div class="hist-tabs">
      <button
        class="hist-tab"
        :class="{ 'is-active': activeTab === 'answer' }"
        @click="fnChangeTab('answer')"
      >
        점검응답 이력
      </button>
      <button
        class="hist-tab"
        :class="{ 'is-active': activeTab === 'defect' }"
        @click="fnChangeTab('defect')"
      >
        불량조치 이력
      </button>
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
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="width: 14%">시각</th>
                <th style="width: 16%">수행자(ID)</th>
                <th style="width: 14%">회사</th>
                <th style="width: 10%">{{ activeTab === 'answer' ? '점검결과' : '조치내용' }}</th>
                <th style="width: 8%">사진</th>
                <th style="width: 12%">변경유형</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!groupedList || groupedList.length === 0">
                <td colspan="6" class="edu-grid-empty">조회된 이력이 없습니다.</td>
              </tr>
              <template
                v-for="(group, gi) in groupedList"
                v-else
                :key="group.key"
              >
                <!-- 좌표 그룹 캡션 -->
                <tr class="group-caption">
                  <td colspan="6">
                    <span class="group-site">{{ group.siteNm }}</span>
                    <span class="group-sep">·</span>
                    <span class="group-chkpt">{{ group.chkptNm }}</span>
                    <span class="group-sep">·</span>
                    <span class="group-item">{{ group.inspectItemSubj }}</span>
                    <span class="group-sep">·</span>
                    <span class="group-date">{{ formatYmdDot(group.workDate) }}</span>
                  </td>
                </tr>
                <!-- 타임라인 행(오름차순) -->
                <tr v-for="(row, ri) in group.rows" :key="gi + '_' + ri">
                  <td>{{ row.chgDtime }}</td>
                  <td>{{ row.performUserNm || '-' }}<span v-if="row.performUserCd" class="perform-id">({{ row.performUserCd }})</span></td>
                  <td>{{ row.performCmpnyNm || '-' }}</td>
                  <td style="text-align: center">
                    <template v-if="activeTab === 'answer'">
                      <span
                        class="answer-badge"
                        :class="row.inspectAnswerType === 'N' ? 'is-bad' : 'is-ok'"
                      >
                        {{ row.inspectAnswerType === 'N' ? '불량' : '양호' }}
                      </span>
                    </template>
                    <template v-else>
                      <button class="btn btn-custom" @click="fnDetailPopOpen(group, row)">상세</button>
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
const histList = ref([]); // 서버 원본(좌표별 오름차순)

// 화면 제어
const siteDisabled = ref(false);
const localButtons = ref({ ...props.buttons });

// 점검구분이 전체("")이면 점검대상 검색 비활성
const targetDisabled = computed(() => proxy.$util.isEmpty(chkLstType.value));

// 좌표(사업장·점검대상·문항·일자) 단위 그룹핑 결과
//   서버가 (siteNm, chkptNm, inspectItemSubj, workDate, actionDtime) 오름차순으로 정렬해 내려주므로
//   순차 스캔하며 좌표 키가 바뀔 때 새 그룹을 연다(타임라인 오름차순 보존).
const groupedList = computed(() => {
  const groups = [];
  const indexByKey = {};
  (histList.value || []).forEach((row) => {
    const key = [row.siteCd, row.chkptCd, row.inspectItemCd, row.workDate].join("|");
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
        rows: [],
      });
    }
    groups[gi].rows.push(row);
  });
  return groups;
});

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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

// ===== 조회 =====
// 사업장 필수 + 기간(YYYYMMDD 변환). activeTab 에 따라 응답/불량 엔드포인트 호출.
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  if (proxy.$util.isEmpty(fromDate.value) || proxy.$util.isEmpty(toDate.value)) {
    await proxy.$alert("점검일자 기간을 선택해주세요.");
    return;
  }

  histList.value = [];

  const url =
    activeTab.value === "answer"
      ? "/webApi/chkLst05/answer-hists"
      : "/webApi/chkLst05/defect-hists";
  const listKey = activeTab.value === "answer" ? "answerHistList" : "defectHistList";

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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
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
        onSiteSelected(siteList[0].siteCd, siteList[0].siteNo, siteList[0].siteNm);
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = "";
        sr_siteNo.value = "";
        sr_siteNm.value = "";
      }
    }
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
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
  if (proxy.$util.isEmpty(siteCd.value) || proxy.$util.isEmpty(chkLstType.value)) {
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

// ===== 상세/사진 열람(읽기전용, DefectDetailPop 재사용) =====
const fnDetailPopOpen = (group, row) => {
  openPop(DefectDetailPop, {
    inspectItemSubj_p: group.inspectItemSubj,
    workDate_p: formatYmdDot(group.workDate),
    answerDesc_p: row.actionDesc, // 불량조치 탭: 조치내용
    fileMgmtCd_p: row.fileMgmtCd,
    filePath_p: row.filePath,
  });
};
const fnPhotoPopOpen = (group, row) => {
  openPop(DefectDetailPop, {
    inspectItemSubj_p: group.inspectItemSubj,
    workDate_p: formatYmdDot(group.workDate),
    answerDesc_p: activeTab.value === "answer" ? row.answerDesc : row.actionDesc,
    fileMgmtCd_p: row.fileMgmtCd,
    filePath_p: row.filePath,
  });
};
</script>

<style scoped>
/* 탭 바 — 웹 표준(밑줄형) 승계 */
.hist-tabs {
  display: flex;
  gap: var(--header-gap);
  border-bottom: 1px solid var(--color-border);
  margin: 0.5rem 0;
}
.hist-tab {
  padding: 0.5rem 0.25rem;
  font-size: 14px;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}
.hist-tab.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

/* 좌표 그룹 캡션 행 */
.group-caption > td {
  background: var(--color-bg);
  color: var(--color-text-strong);
  font-weight: 600;
}
.group-sep {
  margin: 0 0.5rem;
  color: var(--color-text-muted);
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
