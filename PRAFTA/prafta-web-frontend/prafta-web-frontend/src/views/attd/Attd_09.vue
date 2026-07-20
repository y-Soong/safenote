<template>
  <div class="viewComm">
    <!-- 헤더: [조회] [엑셀] -->
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcel"
    />

    <!-- ============ 조회 영역 (Attd_08 viewSearch 스타일) ============ -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
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
          @blur="focusKill"
        />
      </div>
      <div>
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input
          v-model.trim="filter.userNm"
          type="text"
          placeholder="사용자명"
          @keyup.enter="fnSearch"
        />
      </div>
    </div>

    <div class="viewBody leave-dashboard">
      <!-- ============ 메트릭 4카드 (조회 조건 아래) ============ -->
      <div class="ld-metric-grid">
        <div class="ld-metric-card">
          <p class="ld-metric-label">전체 직원</p>
          <p class="ld-metric-value">
            {{ metrics.totalEmployees }}<span class="ld-metric-unit">명</span>
          </p>
        </div>
        <div class="ld-metric-card">
          <p class="ld-metric-label">평균 사용률</p>
          <p class="ld-metric-value">
            {{ metrics.avgUsageRate }}<span class="ld-metric-unit">%</span>
          </p>
        </div>
        <div class="ld-metric-card">
          <p class="ld-metric-label">소멸 임박 (30일)</p>
          <p class="ld-metric-value is-warning">
            {{ metrics.expiringSoon30 }}<span class="ld-metric-unit">명</span>
          </p>
        </div>
        <div class="ld-metric-card">
          <p class="ld-metric-label">이번달 신규부여</p>
          <p class="ld-metric-value">
            {{ metrics.newGrantThisMonth
            }}<span class="ld-metric-unit">명</span>
          </p>
        </div>
      </div>

      <!-- ============ 일괄 액션바 (항상 표시 · 선택 직원이 없으면 버튼 비활성) ============ -->
      <div
        class="ld-bulk-bar"
        :class="{ 'is-empty': selectedUserCds.length === 0 }"
      >
        <div class="ld-bulk-left">
          <template v-if="selectedUserCds.length > 0">
            <strong>{{ selectedUserCds.length }}명</strong> 선택됨
          </template>
          <template v-else>부여할 직원을 선택하세요</template>
        </div>
        <div class="ld-bulk-actions">
          <button
            class="btn btn-second"
            type="button"
            :disabled="selectedUserCds.length === 0"
            @click="fnOpenBulkGrant"
          >
            일괄 수동 부여
          </button>
          <div class="ld-policy-grant-wrap">
            <button
              class="btn btn-second"
              type="button"
              :disabled="selectedUserCds.length === 0"
              @click="fnPolicyGrant"
            >
              정책 기준 부여
            </button>
            <!-- 첫해 방식이 PRORATE면 차년도 일괄 폴백 안내 배지 (prafta-022 보완) -->
            <span
              v-if="prorateFallback"
              class="ld-policy-fallback-badge"
              :title="policyNoticeText"
            >
              비례부여 미적용 · 차년도 일괄
            </span>
          </div>
          <button
            class="btn btn-second"
            type="button"
            :disabled="selectedUserCds.length === 0"
            @click="fnViewLeavePlan"
          >
            연차사용계획서 조회
          </button>
        </div>
      </div>

      <!-- ============ 직원 테이블 (2단 헤더) ============ -->
      <div class="ld-table-wrap">
        <table class="ld-table">
          <colgroup>
            <col style="width: 36px" />
            <col style="width: 12%" />
            <col style="width: 7%" />
            <col style="width: 5%" />
            <col style="width: 6%" />
            <!-- 법정 휴가: 부여 / 사용 / 사용예정 / 잔여 -->
            <col style="width: 5%" />
            <col style="width: 5%" />
            <col style="width: 5.5%" />
            <col style="width: 5%" />
            <!-- 법정 휴가 외: 부여 / 사용 / 사용예정 / 잔여 -->
            <col style="width: 5%" />
            <col style="width: 5%" />
            <col style="width: 5.5%" />
            <col style="width: 5%" />
            <!-- 전체: 부여 / 사용 / 사용예정 / 잔여 -->
            <col style="width: 5%" />
            <col style="width: 5%" />
            <col style="width: 5.5%" />
            <col style="width: 5%" />
            <col style="width: 7%" />
          </colgroup>
          <thead>
            <tr>
              <th rowspan="2" class="is-middle">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="fnToggleSelectAll"
                />
              </th>
              <th rowspan="2" class="is-left is-middle">직원</th>
              <th rowspan="2" class="is-middle">입사일</th>
              <th rowspan="2" class="is-right is-middle">근속</th>
              <th rowspan="2" class="is-right is-middle">경력인정</th>
              <th colspan="4" class="ld-parent-header ld-grp-legal">
                법정 휴가
              </th>
              <th colspan="4" class="ld-parent-header ld-grp-nonlegal">
                법정 휴가 외
              </th>
              <th colspan="4" class="ld-parent-header ld-grp-total">전체</th>
              <th rowspan="2" class="is-middle">사용률</th>
            </tr>
            <tr>
              <th class="is-right ld-grp-legal">부여</th>
              <th class="is-right ld-grp-legal">사용</th>
              <th class="is-right ld-grp-legal">사용예정</th>
              <th class="is-right ld-grp-legal">잔여</th>
              <th class="is-right ld-grp-nonlegal">부여</th>
              <th class="is-right ld-grp-nonlegal">사용</th>
              <th class="is-right ld-grp-nonlegal">사용예정</th>
              <th class="is-right ld-grp-nonlegal">잔여</th>
              <th class="is-right ld-grp-total">부여</th>
              <th class="is-right ld-grp-total">사용</th>
              <th class="is-right ld-grp-total">사용예정</th>
              <th class="is-right ld-grp-total">잔여</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in list"
              :key="row.userCd"
              class="ld-row"
              :class="{ 'is-selected': isSelected(row.userCd) }"
              title="더블클릭하여 상세 보기"
              @dblclick="fnOpenDetail(row)"
            >
              <td class="is-center">
                <input
                  type="checkbox"
                  :checked="isSelected(row.userCd)"
                  @change="fnToggleSelect(row.userCd)"
                />
              </td>
              <td>
                <p class="ld-emp-name">
                  {{ row.userNm }}
                  <span
                    class="ld-emp-badge"
                    :class="{
                      'is-contract': row.employmentType === 'CONTRACT',
                    }"
                  >
                    {{ fnEmploymentLabel(row.employmentType) }}
                  </span>
                </p>
                <p class="ld-emp-info">{{ row.deptNm }}</p>
              </td>
              <td class="is-center is-secondary">
                {{ fnFormatDate(row.hireDate) }}
              </td>
              <td class="is-right">{{ row.tenureText }}</td>
              <td class="is-right is-secondary">
                {{ fnCreditText(row.creditMonths) }}
              </td>

              <td class="is-right ld-grp-legal">
                {{ fnDays(row.legal.granted) }}
              </td>
              <td class="is-right ld-grp-legal">{{ fnDays(row.legal.used) }}</td>
              <td class="is-right ld-grp-legal ld-scheduled">
                {{ fnDays(row.legal.scheduled) }}
              </td>
              <td class="is-right ld-grp-legal ld-cell-group-end ld-strong">
                {{ fnDays(row.legal.remaining) }}
              </td>

              <td class="is-right ld-grp-nonlegal">
                {{ fnDays(row.nonLegal.granted) }}
              </td>
              <td class="is-right ld-grp-nonlegal">
                {{ fnDays(row.nonLegal.used) }}
              </td>
              <td class="is-right ld-grp-nonlegal ld-scheduled">
                {{ fnDays(row.nonLegal.scheduled) }}
              </td>
              <td class="is-right ld-grp-nonlegal ld-cell-group-end">
                {{ fnDays(row.nonLegal.remaining) }}
              </td>

              <td class="is-right ld-grp-total">
                {{ fnDays(row.total.granted) }}
              </td>
              <td class="is-right ld-grp-total">{{ fnDays(row.total.used) }}</td>
              <td class="is-right ld-grp-total ld-scheduled">
                {{ fnDays(row.total.scheduled) }}
              </td>
              <td class="is-right ld-grp-total ld-cell-group-end ld-strong">
                {{ fnDays(row.total.remaining) }}
                <!-- 가불 사용분(prafta-com-011-7, 표시 전용) — 미발생 가불 USED 합이 있으면 잔여 아래 강조 표기 -->
                <span
                  v-if="fnBorrowedDays(row) > 0"
                  class="ld-borrowed-badge"
                  title="아직 발생하지 않은 미래 연차를 미리 당겨 사용한 분(가불)"
                >
                  가불 {{ fnDays(row.borrowedDays) }}
                </span>
              </td>

              <td class="is-right">{{ row.usageRate }}%</td>
            </tr>

            <!-- empty -->
            <tr v-if="list.length === 0">
              <td colspan="18" class="ld-table-empty">
                조회된 직원이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, defineProps, onMounted, getCurrentInstance } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
import { formatLeaveDays } from "@/utils/leaveFormat";
import { getMessage, MSG } from "@/messages";
import search_icon from "@/assets/img/search_icon.png";
import ViewHeader from "@/components/common/ViewHeader.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import LeaveDetailPop from "./popup/LeaveDetailPop.vue";
import ManualGrantPop from "./popup/ManualGrantPop.vue";
import PolicyGrantPreviewPop from "@/views/attd/popup/PolicyGrantPreviewPop.vue";

// ================ Options ================
defineOptions({ name: "Attd_09" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });

// 검색 조건 (사용자명)
const filter = ref({
  userNm: "",
});

// 사업장/소속부서 조회 조건 (Attd_08 패턴 차용)
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const siteNoFcs = ref(null);

// 메트릭 카드
const metrics = ref({
  totalEmployees: 0,
  avgUsageRate: 0,
  expiringSoon30: 0,
  newGrantThisMonth: 0,
});

// 직원 목록 (각 행: { userCd, userNm, deptNm, hireDate, employmentType,
//   tenureText, creditMonths, legal:{granted,used,remaining},
//   nonLegal:{granted,used,remaining}, usageRate })
const list = ref([]);

// 선택된 직원 코드 목록
const selectedUserCds = ref([]);

// LC-09(§5-B): 1일 환산시간(분) — 목록 응답(convMinutes)에서 채움. 미수신 시 480 폴백.
//   "N일 H시간 M분" 표기 조립에만 사용(정렬/내부 계산은 원 수치 유지).
const convMinutes = ref(480);

// 페이지를 넘나들며 로드된 직원 정보 누적(userCd → {hireDate, userNm}).
// 입사일 기준 부여 시 선택 직원의 입사일 사전 검증에 사용(현재 페이지 밖 선택분도 커버).
const userInfoMap = ref({});

// 로딩
const isLoading = ref(false);

// 정책 기준 부여 안내 (prafta-022 보완)
//   활성 정책의 첫해 방식(AXIS3)이 PRORATE면 차년도 일괄(NEXT_YEAR_BULK)로 폴백되므로,
//   그 사실을 버튼 근처 캡션 + 프리뷰 모달 배너로 안내한다(부여 로직 변경 아님, 노출만).
const prorateFallback = ref(false);
const policyNoticeText = ref("");

// ================ Computed ================
// 전체 선택 여부 (현재 페이지 기준)
const isAllSelected = computed(
  () =>
    list.value.length > 0 &&
    list.value.every((r) => selectedUserCds.value.includes(r.userCd))
);

// ================ Life Cycle Functions ================
onMounted(() => {
  fnButtonControll();
  fnInitSite();
  fnLoadPolicyInfo();
  fnSearch();
});

// ================ API Functions ================
// 대시보드 목록 조회 ([조회]/검색/필터 변경 진입점)
//   com-013-08-1: 재조회 시 이전 선택 체크박스를 초기화한다(선택 유지 버그 수정).
const fnSearch = () => {
  fnClearSelection();
  fnLoad();
};

// 실제 목록/메트릭 조회.
//   com-013-08-4: 페이징 UI 제거에 따라 전체 직원을 한 화면에 표시한다.
//   백엔드는 1회 요청당 최대 PAGE_SIZE(=100)건으로 상한이 걸려 있으므로,
//   totalCount 를 모두 채울 때까지 페이지를 순회하며 누적 로드한다(백엔드 계약/스코프/상한 불변).
const PAGE_SIZE = 100; // 백엔드 MAX_PAGE_SIZE 와 동일(한 번에 받을 최대 건수)
const MAX_FETCH_PAGES = 200; // 무한 루프 방어(이론상 최대 2만 명)

const fnLoad = async () => {
  isLoading.value = true;
  try {
    const baseParams = {
      siteCd: siteCd.value || "",
      nodeCd: nodeCd.value || "",
      incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
      userNm: filter.value.userNm || "",
      size: PAGE_SIZE,
    };

    const accumulated = [];
    let total = 0;
    let metricsData = null;

    for (let page = 1; page <= MAX_FETCH_PAGES; page++) {
      const response = await axios.get("/webApi/attd09/leave-dashboard/list", {
        params: { ...baseParams, page },
      });
      const data = response.data || {};

      // 메트릭/총건수는 회사 공통 값이라 첫 페이지 응답만 채택한다.
      if (page === 1) {
        metricsData = data.metrics || {};
        total = data.paging?.totalCount ?? 0;
        // LC-09: 환산시간(회사 공통) — 표기 조립용
        convMinutes.value = data.convMinutes ?? 480;
      }

      const pageList = Array.isArray(data.list) ? data.list : [];
      accumulated.push(...pageList);

      // 더 받을 게 없으면 종료(누적 건수가 총건수 도달 or 빈 페이지).
      if (accumulated.length >= total || pageList.length === 0) {
        break;
      }
    }

    metrics.value = {
      totalEmployees: metricsData?.totalEmployees ?? 0,
      avgUsageRate: metricsData?.avgUsageRate ?? 0,
      expiringSoon30: metricsData?.expiringSoon30 ?? 0,
      newGrantThisMonth: metricsData?.newGrantThisMonth ?? 0,
    };
    list.value = accumulated;

    // 입사일 사전 검증용 누적 맵 갱신
    list.value.forEach((r) => {
      userInfoMap.value[r.userCd] = { hireDate: r.hireDate, userNm: r.userNm };
    });
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// 활성 연차정책 안내 정보 조회 (prafta-022 보완)
//   prorateFallback/noticeText만 보관 → 첫해 방식이 PRORATE일 때 버튼 근처 캡션/모달 배너 노출.
//   조회 실패 시 안내는 미표시로 두고(사용자 흐름 비차단) 콘솔에만 남긴다.
const fnLoadPolicyInfo = async () => {
  try {
    const res = await axios.get("/webApi/attd09/leave-grant/policy-info");
    const d = res.data || {};
    prorateFallback.value = d.prorateFallback === true;
    policyNoticeText.value = d.noticeText || "";
  } catch (err) {
    prorateFallback.value = false;
    policyNoticeText.value = "";
    console.warn("연차정책 안내 정보 조회 실패", err);
  }
};

// 엑셀(전체 직원 데이터 CSV) — LeavePolicyImpactPop fnDownloadReport 패턴
const fnExcel = () => {
  if (list.value.length === 0) {
    proxy.$alert("내보낼 데이터가 없습니다.");
    return;
  }
  const header = [
    "사용자코드",
    "직원",
    "부서",
    "입사일",
    "근속",
    "고용형태",
    "경력인정개월",
    "법정부여",
    "법정사용",
    "법정사용예정",
    "법정잔여",
    "법정외부여",
    "법정외사용",
    "법정외사용예정",
    "법정외잔여",
    "사용률(%)",
  ];
  // LC-09(§5-B): 일수 컬럼은 화면과 동일하게 "N일 H시간 M분" 표기(소수점 노출 금지)
  const rows = list.value.map((r) => [
    r.userCd,
    r.userNm,
    r.deptNm,
    fnFormatDate(r.hireDate),
    r.tenureText,
    fnEmploymentLabel(r.employmentType),
    r.creditMonths,
    fnDays(r.legal?.granted),
    fnDays(r.legal?.used),
    fnDays(r.legal?.scheduled),
    fnDays(r.legal?.remaining),
    fnDays(r.nonLegal?.granted),
    fnDays(r.nonLegal?.used),
    fnDays(r.nonLegal?.scheduled),
    fnDays(r.nonLegal?.remaining),
    r.usageRate,
  ]);
  const csvBody = [header, ...rows]
    .map((cols) => cols.map(fnCsvCell).join(","))
    .join("\r\n");
  // Excel 한글 깨짐 방지용 BOM
  const blob = new Blob(["﻿" + csvBody], {
    type: "text/csv;charset=utf-8;",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `연차현황_${fnTodayYyyymmdd()}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  localButtons.value.search = "Y";
  localButtons.value.excel = "Y";
  localButtons.value.save = "N";
  localButtons.value.create = "N";
  localButtons.value.delete = "N";
};

// ================ 사업장/소속부서 조회 (Attd_08 패턴 차용) ================
// 입력칸 blur 시 자동조회 처리 — 코드/명 단일 일치면 자동 채움, 복수면 팝업.
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
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
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      fnSiteCallback({
        ...response,
        config: { url: "/dummy/site-node-lists" },
      });
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnSiteCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const siteList = res.data?.siteInfoResultList ?? [];
    if (siteList.length === 1) {
      siteCd.value = siteList[0].siteCd;
      siteNo.value = siteList[0].siteNo;
      siteNm.value = siteList[0].siteNm;
      nodeDisabled.value = false;
    } else if (siteList.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } else if (apiId === "site-node-lists") {
    const nodeList = res.data?.siteNodeInfoList || [];
    if (nodeList.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (nodeList.length === 1) {
      nodeCd.value = nodeList[0].nodeCd ?? "";
      nodeNm.value = nodeList[0].nodeNm ?? "";
    } else {
      fnSiteNodeSearchPopOpen();
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// 진입 시 sessionStorage 의 사업장 정보로 초기화 (Attd_08 fnInit 패턴 차용)
const fnInitSite = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
  }
};

// --- 선택 토글 ---
const isSelected = (userCd) => selectedUserCds.value.includes(userCd);

const fnToggleSelect = (userCd) => {
  const idx = selectedUserCds.value.indexOf(userCd);
  if (idx >= 0) {
    selectedUserCds.value.splice(idx, 1);
  } else {
    selectedUserCds.value.push(userCd);
  }
};

const fnToggleSelectAll = () => {
  if (isAllSelected.value) {
    const pageCds = list.value.map((r) => r.userCd);
    selectedUserCds.value = selectedUserCds.value.filter(
      (cd) => !pageCds.includes(cd)
    );
  } else {
    list.value.forEach((r) => {
      if (!selectedUserCds.value.includes(r.userCd)) {
        selectedUserCds.value.push(r.userCd);
      }
    });
  }
};

const fnClearSelection = () => {
  selectedUserCds.value = [];
};

// --- 상세 모달 ---
const fnOpenDetail = (row) => {
  openPop(LeaveDetailPop, {
    userCd: row.userCd,
    // 상세 모달 내부 [수동 부여] 성공 시 대시보드 재조회 신호
    onGranted: () => {
      fnSearch();
    },
  });
};

// --- 일괄 수동 부여 모달 ---
const fnOpenBulkGrant = () => {
  if (selectedUserCds.value.length === 0) return;
  // 대상 직원 표시용 정보 추출 (현재 페이지 기준)
  const targetUsers = list.value
    .filter((r) => selectedUserCds.value.includes(r.userCd))
    .map((r) => ({ userCd: r.userCd, userNm: r.userNm, deptNm: r.deptNm }));

  openPop(ManualGrantPop, {
    targetUsers,
    onGranted: () => {
      fnClearSelection();
      fnSearch();
    },
  });
};

// --- 정책 기준 부여 (프리뷰 모달 → 확인 → 적용) ---
// 흐름: ① 입사일 미입력 1차 가드 → ② 프리뷰 조회 → ③ 프리뷰 모달 표시
//       → ④ 모달 [적용] 확인 시 적용 POST → 결과 alert + 목록 갱신
const fnPolicyGrant = async () => {
  if (selectedUserCds.value.length === 0) return;

  // 1) 선택 직원 중 입사일 미입력자 검증 → 있으면 API 호출하지 않고 Alert 후 중단
  const missing = selectedUserCds.value.filter((cd) => {
    const info = userInfoMap.value[cd];
    return !info || !info.hireDate || String(info.hireDate).trim() === "";
  });
  if (missing.length > 0) {
    const names = missing
      .map((cd) => userInfoMap.value[cd]?.userNm || cd)
      .join(", ");
    await proxy.$alert(
      `입사일이 입력되지 않은 직원이 있습니다.\n(${names})\n입사일 입력 후 다시 시도해주세요.`
    );
    return;
  }

  // 2) 프리뷰 조회 (부여 미수행 dry-run)
  let preview;
  try {
    const res = await axios.post(
      "/webApi/attd09/leave-grant/policy-grant/preview",
      { userCds: selectedUserCds.value }
    );
    preview = res.data || {};
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "부여 미리보기 중 오류가 발생했습니다.")
    );
    return;
  }

  // 3) 프리뷰 모달 표시 → [적용] 확인 시 onConfirm 콜백에서 실제 적용 수행
  openPop(PolicyGrantPreviewPop, {
    // prafta-032 D6: 입사일 변경 처리방식 폐기로 재발급(reissueCount) 전달 제거
    selectedCount: preview.selectedCount ?? 0,
    newGrantCount: preview.newGrantCount ?? 0,
    noChangeCount: preview.noChangeCount ?? 0,
    rows: Array.isArray(preview.rows) ? preview.rows : [],
    userInfoMap: userInfoMap.value,
    // PRORATE 폴백 안내(있을 때만 모달 상단 배너로 표시)
    prorateFallback: prorateFallback.value,
    noticeText: policyNoticeText.value,
    onConfirm: () => {
      // 모달은 UI만 담당 — 실제 적용 API 호출은 부모(Attd_09)가 수행
      fnApplyPolicyGrant();
    },
  });
};

// --- 정책 기준 부여 적용 (프리뷰 모달 [적용] 확인 후 실행) ---
const fnApplyPolicyGrant = async () => {
  try {
    const res = await axios.post("/webApi/attd09/leave-grant/policy-grant", {
      userCds: selectedUserCds.value,
    });
    const d = res.data || {};
    // prafta-032 D6: 입사일 변경 처리방식 폐기로 취소(재발급) 문구 제거
    const lines = [
      `부여 ${d.grantedCount ?? 0}명 (총 ${d.grantedDays ?? 0}일)`,
    ];
    if ((d.skippedCount ?? 0) > 0) {
      lines.push(`건너뜀 ${d.skippedCount}명 (이미 부여됨/부여 대상 아님)`);
    }
    await proxy.$alert(lines.join("\n"));
    fnClearSelection();
    fnSearch();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "연차 부여 중 오류가 발생했습니다.")
    );
  }
};

// --- 연차사용계획서 조회 (D-1: 세부 미확정, 안내만) ---
const fnViewLeavePlan = () => {
  proxy.$alert("준비 중인 기능입니다.");
};

// --- 가불 사용분(표시 전용, prafta-com-011-7) ---
//   borrowedDays = 미발생 가불 GRANT 의 USED 합(BE 산정). 숫자로 정규화해 0 이하면 0 반환.
//   LC-09(§5-B): 표기는 fnDays(소수점 노출 금지)로 조립하므로 본 함수는 배지 노출 판정에만 쓴다.
const fnBorrowedDays = (row) => {
  const n = Number(row?.borrowedDays ?? 0);
  if (!Number.isFinite(n) || n <= 0) return 0;
  return n;
};

// --- 고용형태 라벨 ---
const fnEmploymentLabel = (type) => {
  const map = {
    REGULAR: "정규직",
    CONTRACT: "계약직",
    DAILY: "일용직",
    EXECUTIVE: "임원",
  };
  return map[type] || type || "-";
};

// ================ 내부 유틸 ================
// LC-09(§5-B): 일수 표기 — 소수점 노출 금지, "N일 H시간 M분"(leaveFormat 단일 출처)
const fnDays = (v) => formatLeaveDays(v, convMinutes.value);

// YYYYMMDD → "YYYY.MM.DD" 표기. 빈값/형식불충분은 "-".
const fnFormatDate = (yyyymmdd) => {
  const s = String(yyyymmdd || "");
  if (s.length !== 8) return s || "-";
  return formatYmdDot(s);
};

// 경력 인정 개월 → "N년 M개월" (0/null이면 "-")
const fnCreditText = (months) => {
  const m = Number(months) || 0;
  if (m <= 0) return "-";
  const y = Math.floor(m / 12);
  const rm = m % 12;
  if (y > 0 && rm > 0) return `${y}년 ${rm}개월`;
  if (y > 0) return `${y}년`;
  return `${rm}개월`;
};

// 오늘 YYYYMMDD (파일명용)
const fnTodayYyyymmdd = () => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
};

// CSV 셀 escape (LeavePolicyImpactPop 동일 패턴)
//   1) CSV Injection 방어: 수식 트리거 문자(= + - @, 탭/CR)로 시작하면 앞에 작은따옴표를 붙여 중화.
//   2) 콤마/따옴표/개행 포함 시 따옴표로 감싸고 내부 따옴표는 두 번으로 escape.
const fnCsvCell = (v) => {
  let s = v == null ? "" : String(v);
  if (/^[=+\-@\t\r]/.test(s)) {
    s = `'${s}`;
  }
  if (/[",\r\n]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
};
</script>

<style scoped>
.leave-dashboard {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
  /* viewBody(flex:1)의 높이를 채우고, 내부 테이블 wrap 이 남은 세로 공간을
     차지하도록 함 (Attd_08 a08-body 패턴) */
  min-height: 0;
  overflow: hidden;
}

/* ===== 메트릭 카드 ===== */
.ld-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.625rem;
}

.ld-metric-card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
  padding: 0.875rem 1rem;
}

.ld-metric-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0 0 0.25rem;
}

.ld-metric-value {
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0;
}

.ld-metric-value.is-warning {
  color: var(--color-warning-text);
}

.ld-metric-unit {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  font-weight: 500;
  margin-left: 0.125rem;
}

/* ===== 일괄 액션바 ===== */
.ld-bulk-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  background: rgba(22, 163, 74, 0.06);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.625rem 0.875rem;
}

.ld-bulk-left {
  font-size: 0.8125rem;
  color: var(--color-primary-pressed);
  font-weight: 500;
}

/* 선택 직원이 없을 때: 강조(초록 틴트) 대신 중립톤으로 — 평소 노출 상태가 알림처럼 보이지 않게 */
.ld-bulk-bar.is-empty {
  background: var(--card-bg);
}

.ld-bulk-bar.is-empty .ld-bulk-left {
  color: var(--color-text-muted);
  font-weight: 400;
}

.ld-bulk-actions {
  display: flex;
  gap: 0.375rem;
  align-items: center;
}

/* ===== 정책 기준 부여 버튼 + PRORATE 폴백 안내 배지 (prafta-022 보완) ===== */
.ld-policy-grant-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.ld-policy-fallback-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.6875rem;
  font-weight: 500;
  line-height: 1.2;
  padding: 0.125rem 0.5rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-text);
  white-space: nowrap;
  cursor: help;
}

/* ===== 테이블 (Attd_08 a08-table 스타일 정렬) ===== */
.ld-table-wrap {
  /* 남은 세로 공간을 채우고 내부 스크롤 (Attd_08 a08-table-wrap 패턴) */
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: #fff;
}

.ld-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  table-layout: fixed;
  /* 그룹별 4컬럼(부여/사용/사용예정/잔여) × 3 = 12 + 고정 6컬럼 → 가로 스크롤 기준폭 확대 */
  min-width: 1320px;
}

.ld-table thead th {
  text-align: center;
  padding: 0.5rem 0.4rem;
  font-weight: 600;
  line-height: 1.2;
  color: var(--color-text-strong);
  white-space: nowrap;
  background: var(--thead-bg, #f3f4f6);
  /* 컬럼마다 경계가 보이도록 사방 테두리 (Attd_03 .data-grid 패턴) */
  border: 1px solid var(--color-border, #e5e7eb);
  z-index: 1;
}

/* 2단 헤더 sticky: 1행은 상단, 2행은 1행 높이만큼 아래에 고정 (Attd_08 패턴) */
.ld-table thead tr:first-child th {
  position: sticky;
  top: 0;
}

.ld-table thead tr:last-child th {
  position: sticky;
  top: 2.1rem;
}

.ld-table th.is-left {
  text-align: left;
}

.ld-table th.is-right {
  text-align: right;
}

.ld-table th.is-middle {
  vertical-align: middle;
}

/* 2단 헤더의 그룹 헤더(법정 휴가 / 법정 휴가 외 / 전체) — 배경색은 그룹 톤(.ld-grp-*)에서 지정 */
.ld-parent-header {
  color: var(--color-text-strong);
}

/* ===== 항목 그룹 배경 (연한 계통, 법정/법정외/전체 시각 구분) =====
   디자인 토큰에 파랑/주황 틴트가 없어 이 화면 한정으로 옅은 rgba 리터럴을 사용한다
   (.ld-bulk-bar / .ld-emp-badge 등 기존 셀의 rgba 사용 패턴과 동일).
   본문 셀은 매우 옅게, 헤더 셀은 약간 진하게 한다.
   hover/선택 행 배경(.ld-table tr:hover td, .ld-table tr.is-selected td)이 우선 적용되도록
   본문은 단일 클래스(.ld-grp-*) 선택자로만 지정한다. */
.ld-grp-legal {
  background: rgba(37, 99, 235, 0.05); /* 연한 파랑 — 법정 휴가 */
}
.ld-grp-nonlegal {
  background: rgba(217, 119, 6, 0.06); /* 연한 주황 — 법정 휴가 외 */
}
.ld-grp-total {
  background: rgba(22, 163, 74, 0.06); /* 연한 초록 — 전체 */
}
/* 헤더 셀은 sticky 고정되므로 반투명(rgba)이면 스크롤되는 본문이 비쳐 보인다.
   불투명 thead 배경 위에 그룹 틴트를 겹쳐 같은 색감을 유지하면서 불투명화한다. */
.ld-table thead th.ld-grp-legal {
  background-color: var(--thead-bg, #f3f4f6);
  background-image: linear-gradient(
    rgba(37, 99, 235, 0.12),
    rgba(37, 99, 235, 0.12)
  );
}
.ld-table thead th.ld-grp-nonlegal {
  background-color: var(--thead-bg, #f3f4f6);
  background-image: linear-gradient(
    rgba(217, 119, 6, 0.13),
    rgba(217, 119, 6, 0.13)
  );
}
.ld-table thead th.ld-grp-total {
  background-color: var(--thead-bg, #f3f4f6);
  background-image: linear-gradient(
    rgba(22, 163, 74, 0.13),
    rgba(22, 163, 74, 0.13)
  );
}

/* 사용예정 셀: 보조 정보이므로 약하게(이탤릭 + muted) */
.ld-table td.ld-scheduled {
  color: var(--color-text-muted);
  font-style: italic;
}

.ld-table td {
  padding: 0.4rem;
  /* 컬럼마다 경계가 보이도록 사방 테두리 (Attd_03 .data-grid 패턴) */
  border: 1px solid var(--color-border, #e5e7eb);
  vertical-align: middle;
  color: var(--color-text);
}

.ld-table tr:hover td {
  background: #f9fafb;
}

/* 행 더블클릭 시 상세 팝업 (관리 컬럼 대체) */
.ld-row {
  cursor: pointer;
}

.ld-table tr.is-selected td {
  background: #eef2ff;
}

.ld-table td.is-right {
  text-align: right;
}

.ld-table td.is-center {
  text-align: center;
}

.ld-table td.is-secondary {
  color: var(--color-text-muted);
}

.ld-cell-group-end {
  border-right: 2px solid var(--color-border);
}

.ld-strong {
  font-weight: 600;
  color: var(--color-text-strong);
}

/* ===== 가불 사용분 배지 (prafta-com-011-7, 표시 전용) =====
   잔여 셀 내부에 줄바꿈하여 노출. 디자인 토큰 경고색(주의 환기)만 사용. */
.ld-borrowed-badge {
  display: block;
  margin-top: 0.125rem;
  font-size: 0.625rem;
  font-weight: 600;
  line-height: 1.2;
  color: var(--color-warning-text);
  white-space: nowrap;
}

.ld-emp-name {
  margin: 0 0 0.0625rem;
  font-weight: 500;
  color: var(--color-text-strong);
}

.ld-emp-info {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0;
}

.ld-emp-badge {
  font-size: 0.625rem;
  padding: 0.0625rem 0.375rem;
  border-radius: var(--btn-radius);
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary-pressed);
  margin-left: 0.25rem;
}

.ld-emp-badge.is-contract {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

/* ===== progress ===== */
.ld-progress-wrap {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.ld-progress-bar {
  flex: 1;
  height: 0.375rem;
  background: var(--color-bg);
  border-radius: 0.1875rem;
  overflow: hidden;
}

.ld-progress-fill {
  height: 100%;
}

.ld-progress-fill.is-primary {
  background: var(--color-primary);
}

.ld-progress-fill.is-warning {
  background: var(--color-warning-text);
}

.ld-progress-fill.is-danger {
  background: var(--color-danger);
}

.ld-progress-text {
  font-size: 0.625rem;
  color: var(--color-text-muted);
  min-width: 1.625rem;
  text-align: right;
}

/* ===== chevron 버튼 ===== */
.ld-chevron-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  padding: 0.125rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ld-chevron-btn:hover {
  color: var(--color-primary);
}

.ld-table-empty {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2.5rem 0.75rem;
}

/* ===== 체크박스 토큰 ===== */
.ld-table input[type="checkbox"] {
  width: 0.875rem;
  height: 0.875rem;
  accent-color: var(--color-primary);
  cursor: pointer;
}

/* ===== 반응형 ===== */
@media (max-width: 1024px) {
  .ld-metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .ld-bulk-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}

/* ===== 하위부서 조회 체크박스 (Attd_08 checkbox-label 패턴 차용) =====
   viewSearch 의 div 간 gap 을 일부 상쇄해 소속부서 입력 뭉치와 가깝게 붙여 보이게 한다. */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}
</style>
