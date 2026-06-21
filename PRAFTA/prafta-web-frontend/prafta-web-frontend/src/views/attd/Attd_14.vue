<!--
  Attd_14.vue — 관리자 발신 연차 변경 요청 이력 조회 (읽기 전용) (prafta-com-016-H)
  유형: frontend-screen (웹 관리자)
  요청 방향: 관리자 → 사용자 (연차 이동/삭제 발신 이력). 사용자→관리자 상신은 제외.
  참조 패턴: views/attd/Attd_13.vue (검색바·역할 스코프·data-grid·사업장/부서 로드),
            views/tbm/Tbm_04.vue (기간 + 페이징 이력 화면 패턴)
  데이터 출처: tb_leave_change_request (INITIATOR_TYPE='ADMIN' 서버 고정, 전 상태)
  ※ 본 화면은 읽기 전용 — 발의/확인/반려 액션 버튼 없음(그 처리는 Attd_13).
  ※ 확정결정 Q3: 발의주체 토글 제거(서버 ADMIN 고정). Q5: 기간 기본 당월 + 페이징.
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색바(1행): 발의기간 / 사업장 / 소속부서 / 하위부서 포함 / 사용자명
         (2행): 요청유형 / 처리상태 -->
    <div class="viewSearch">
      <div>
        <label>발의기간</label>
        <CalendarSrch
          v-model="periodRange"
          range
          :style="{ width: '200px', textAlign: 'center' }"
        />
      </div>

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
          <input type="checkbox" v-model="includeSubNode" />
          하위부서 포함
        </label>
      </div>

      <div>
        <label>사용자명</label>
        <input
          id="adminReqUserKeyword"
          type="text"
          v-model="userKeyword"
          placeholder="사용자명"
          @keyup.enter="fnSearch"
        />
      </div>

      <!-- 1행에 다 담지 못하는 조건은 2행으로 줄바꿈 -->
      <div class="break-line"></div>

      <div>
        <label>요청유형</label>
        <select v-model="reqType" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in reqTypeOptions"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>처리상태</label>
        <select v-model="reqStatus" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in reqStatusOptions"
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
        <div class="subtitle">
          <span class="subtitle-text">관리자 요청 이력 목록</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 60vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="text-align: center; width: 3%">No</th>
                <th>발의일시</th>
                <th>대상 사용자</th>
                <th>요청유형</th>
                <th>대상 연차일</th>
                <th>이동 대상일</th>
                <th>발의자</th>
                <th>근로자 응답</th>
                <th>처리상태</th>
                <th>처리일시</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!historyList || historyList.length === 0">
                <tr>
                  <td colspan="10" class="grid-empty">
                    조회된 요청 이력이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in historyList"
                  :key="item.changeReqId"
                  style="cursor: pointer"
                  @click="fnOpenDetail(item)"
                >
                  <td style="text-align: center">{{ (page - 1) * pageSize + idx + 1 }}</td>
                  <td style="text-align: center">{{ item.insertDateText }}</td>
                  <td>{{ item.targetUserNm }}</td>
                  <td style="text-align: center">{{ item.reqTypeNm }}</td>
                  <td style="text-align: center">{{ item.targetStartDateText }}</td>
                  <td style="text-align: center">{{ item.moveTargetDateText || '-' }}</td>
                  <td>{{ item.initiatorUserNm }}</td>
                  <td style="text-align: center">{{ item.workerResponseNm || '-' }}</td>
                  <td style="text-align: center">
                    <span class="status-badge" :class="item.statusClass">
                      {{ item.reqStatusNm }}
                    </span>
                  </td>
                  <td style="text-align: center">{{ item.confirmDateText || '-' }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- 페이징 (Q5: 기간 기본 당월 + page/size) -->
        <div v-if="totalCnt > 0" class="pager">
          <button
            class="btn btn-second btn-sm"
            :disabled="page <= 1"
            @click="fnGoPage(page - 1)"
          >
            이전
          </button>
          <span class="pager-info">
            {{ page }} / {{ totalPages }} (총 {{ totalCnt }}건)
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

    <!-- 읽기 전용 상세 팝업 (액션 없음). -->
    <LeaveChangeHistoryDetailPop
      v-if="showDetailPop"
      :change-req-id="selectedChangeReqId"
      @close="showDetailPop = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import ViewHeader from '@/components/common/ViewHeader.vue'
import CalendarSrch from '@/components/common/CalendarSrch.vue'
import LeaveChangeHistoryDetailPop from './popup/LeaveChangeHistoryDetailPop.vue'
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue'
import SiteNodeSearchPop from '@/components/popup/SiteNodeSearchPop.vue'
import axios from '@/api/axios'
import { getMessage, MSG } from '@/messages'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { useModal } from '@/utils/useModal'
import search_icon from '@/assets/img/search_icon.png'
import { formatYmdDot, formatDateTimeDot } from '@/utils/dateFormat'

const props = defineProps({
  title: { type: String, default: '연차 변경 요청 이력' },
  buttons: Object,
})

const { proxy } = getCurrentInstance()
const { open: openPop } = useModal()

// ── 검색 조건 ────────────────────────────────────────────────────────────
const periodRange = ref([]) // CalendarSrch range → ['YYYY-MM-DD','YYYY-MM-DD']
const siteCd = ref('')
const siteNo = ref('')
const siteNm = ref('')
const siteDisabled = ref(false)
const nodeCd = ref('')
const nodeNm = ref('')
const nodeDisabled = ref(true)
const includeSubNode = ref(true)
const userKeyword = ref('')
const reqType = ref('')
const reqStatus = ref('')

// ── 코드/목록 ────────────────────────────────────────────────────────────
const historyList = ref([])
const totalCnt = ref(0)

// ── 페이징 ───────────────────────────────────────────────────────────────
const page = ref(1)
const pageSize = ref(20)
const totalPages = computed(() => {
  const pages = Math.ceil(totalCnt.value / pageSize.value)
  return pages < 1 ? 1 : pages
})

// ── 상세 팝업 토글 ───────────────────────────────────────────────────────
const showDetailPop = ref(false)
const selectedChangeReqId = ref('')

// 헤더 버튼 — 권한 메뉴(tb_syst_auth_menu BTN_*)에서 주입된 props.buttons 사용.
//   Attd_14 시드는 조회(BTN_SRCH)만 'Y', 나머지 'N'(읽기 전용). 다른 화면(Attd_12 등)과 동일 패턴.
const localButtons = ref({ ...props.buttons })

// 권한 스코프(D1+D3): master/hr 는 회사 전사, 그 외(노드 관리자)는 담당 부서 강제.
//   서버도 동일 정책으로 fail-closed 강제(canManageNodeExcludeSafe, safe 제외).
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem('gv_authCd')
  return a === 'master' || a === 'hr'
})

// 코드값 → 라벨 매핑은 하드코딩하지 않고 TB_SYST_VAL_D 에서 단일 출처로 로드한다.
//   SYS071 요청유형 / SYS072 요청상태 / SYS073 근로자 응답 (/comApi/baseinfo/syst-info-lists).
//   systCodeArr = { SYS071: [...], SYS072: [...], SYS073: [...] } (systValCd 로 그룹핑).
const systCodeArr = ref({})

// 그룹(SYSxxx) + 상세코드(systValDCd) → 라벨(systValDNm). 미로딩/미일치 시 코드값 폴백.
const codeNm = (group, cd) => {
  if (cd == null || cd === '') return cd
  const hit = (systCodeArr.value[group] || []).find((o) => o.systValDCd === cd)
  return hit ? hit.systValDNm : cd
}

// 검색바 드롭다운 옵션('전체'는 템플릿에서 별도). 정렬/사용여부는 서버 조회 결과 순서를 따른다.
const reqTypeOptions = computed(() =>
  (systCodeArr.value.SYS071 || []).filter((o) => o.systValDCd != null)
)
const reqStatusOptions = computed(() =>
  (systCodeArr.value.SYS072 || []).filter((o) => o.systValDCd != null)
)

// 처리상태 → 배지 색 분기(확정=primary, 거부=danger, 종료=closed, 그 외 대기성=pending)
const STATUS_CLASS = {
  CONFIRMED: 'is-confirmed',
  REJECTED: 'is-rejected',
  CLOSED: 'is-closed',
  REQUESTED: 'is-pending',
  AGREED: 'is-pending',
}

// 날짜/시각 표시는 dateFormat 단일 출처에 위임(점 표기).
const fmtYmd = (ymd) => formatYmdDot(ymd)
const fmtDateTime = (v) => formatDateTimeDot(v)

// 서버 row → 그리드 표시 객체(라벨/포맷 보강)
const toRow = (r) => ({
  changeReqId: r.changeReqId,
  reqStatus: r.reqStatus,
  reqType: r.reqType,
  targetUserNm: r.targetUserNm,
  initiatorUserNm: r.initiatorUserNm,
  insertDateText: fmtDateTime(r.insertDate),
  confirmDateText: fmtDateTime(r.confirmDate),
  targetStartDateText: fmtYmd(r.targetStartDate),
  moveTargetDateText: fmtYmd(r.moveTargetDate),
  reqTypeNm: codeNm('SYS071', r.reqType),
  reqStatusNm: codeNm('SYS072', r.reqStatus),
  workerResponseNm: codeNm('SYS073', r.workerResponse),
  statusClass: STATUS_CLASS[r.reqStatus] || 'is-pending',
})

// CalendarSrch range 모델(['YYYY-MM-DD','YYYY-MM-DD']) → 백엔드 YYYYMMDD
const ymdParam = (v) => (v ? String(v).replace(/[^0-9]/g, '') : '')
const fromDateParam = computed(() => ymdParam(periodRange.value?.[0]))
const toDateParam = computed(() =>
  ymdParam(periodRange.value?.[1] ?? periodRange.value?.[0])
)

// 당월 1일 ~ 오늘 기본값 세팅(YYYY-MM-DD)
const setDefaultPeriod = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  periodRange.value = [`${y}-${m}-01`, `${y}-${m}-${d}`]
}

// ── 사업장 / 부서 입력 처리 (Attd_07 패턴 차용) ───────────────────────────
//   코드/명 입력 후 blur 시 짝 필드 비우고 자동조회(단건 자동세팅 / 다건 팝업).
const focusKill = (e) => {
  if (e.target.id === 'siteNo') {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = ''
      siteNm.value = ''
      nodeDisabled.value = true
      nodeCd.value = ''
      nodeNm.value = ''
    } else {
      siteNm.value = ''
      fnSrchSiteInfo()
    }
  } else if (e.target.id === 'siteNm') {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = ''
      siteNo.value = ''
      nodeDisabled.value = true
      nodeCd.value = ''
      nodeNm.value = ''
    } else {
      siteNo.value = ''
      fnSrchSiteInfo()
    }
  } else if (e.target.id === 'nodeCd') {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = ''
    } else {
      nodeNm.value = ''
      fnSrchNodeInfo()
    }
  } else if (e.target.id === 'nodeNm') {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = ''
    } else {
      nodeCd.value = ''
      fnSrchNodeInfo()
    }
  }
}

// 사업장 자동조회 (코드/명 입력 후 blur)
const fnSrchSiteInfo = async () => {
  try {
    const res = await axios.get('/comApi/baseinfo/site-lists', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    })
    if (res.status === 200) fnCallback(res)
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT)))
  }
}

// 부서 자동조회 (코드/명 입력 후 blur)
const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return
  try {
    const res = await axios.get('/comApi/baseinfo/site-node-lists', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    })
    if (res.status === 200) {
      fnCallback({ ...res, config: { url: '/dummy/site-node-lists' } })
    }
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT)))
  }
}

// 자동조회 응답 처리 — 0건/1건/다건 분기
const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return
  const apiId = res.config.url.split('/').pop()
  if (apiId === 'site-lists') {
    const list = res.data?.siteInfoResultList ?? []
    if (list.length === 1) {
      siteCd.value = list[0].siteCd
      siteNo.value = list[0].siteNo
      siteNm.value = list[0].siteNm
      nodeDisabled.value = false
    } else if (list.length > 1) {
      fnSiteSearchPopOpen()
    } else {
      siteCd.value = ''
      siteNo.value = ''
      siteNm.value = ''
      nodeDisabled.value = true
      nodeCd.value = ''
      nodeNm.value = ''
    }
  } else if (apiId === 'site-node-lists') {
    const list = res.data?.siteNodeInfoList || []
    if (list.length === 0) {
      nodeCd.value = ''
      nodeNm.value = ''
    } else if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? ''
      nodeNm.value = list[0].nodeNm ?? ''
    } else {
      fnSiteNodeSearchPopOpen()
    }
  }
}

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal
  siteNo.value = siteNoVal
  siteNm.value = siteNmVal
  nodeDisabled.value = false
  nodeCd.value = ''
  nodeNm.value = ''
}

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
    siteNo_p: '',
    siteNm_p: '',
    onSelect: onSiteSelected,
  })
}

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST))
    return
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
    siteCd_p: siteCd.value,
    nodeCd_p: '',
    userCd_p: '',
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? ''
      nodeNm.value = nodeNmVal ?? ''
    },
  })
}

// ── 코드 로드 ────────────────────────────────────────────────────────────
//   요청유형(SYS071)/요청상태(SYS072)/응답(SYS073) 라벨·드롭다운 옵션 단일 출처.
//   다른 화면(Tbm_01/User_01 등)과 동일하게 /comApi/baseinfo/syst-info-lists 사용.
const fnGetSystinfoList = async () => {
  try {
    const res = await axios.get('/comApi/baseinfo/syst-info-lists', {
      params: { systCodeList: ['SYS071', 'SYS072', 'SYS073'] },
    })
    if (res.status === 200) {
      const list = res.data?.systInfoList ?? []
      const grouped = {}
      list.forEach((item) => {
        const key = item.systValCd
        if (!grouped[key]) grouped[key] = []
        grouped[key].push(item)
      })
      systCodeArr.value = grouped
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    )
  }
}

// ── 조회 ─────────────────────────────────────────────────────────────────
//   GET /webApi/attd14/admin-requests
//   서버가 권한/IDOR(canManageNodeExcludeSafe + INC_SUB) fail-closed 강제(safe 제외, ADMIN 발신 고정).
const fnLoadList = async () => {
  try {
    const res = await axios.get('/webApi/attd14/admin-requests', {
      params: {
        SITE_CD: siteCd.value,
        NODE_CD: nodeCd.value,
        INC_SUB_NODE_YN: includeSubNode.value ? 'Y' : 'N',
        USER_NM: userKeyword.value,
        REQ_TYPE: reqType.value,
        REQ_STATUS: reqStatus.value,
        FROM_DATE: fromDateParam.value,
        TO_DATE: toDateParam.value,
        PAGE: page.value,
        PAGE_SIZE: pageSize.value,
      },
    })
    if (res.status === 200) {
      const list = res.data?.list ?? []
      historyList.value = list.map(toRow)
      totalCnt.value = res.data?.totalCnt ?? 0
    }
  } catch (err) {
    historyList.value = []
    totalCnt.value = 0
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR)))
  }
}

// 조회(검색조건 변경 → 1페이지부터). 노드 관리자는 담당 부서 선택 필수.
const fnSearch = async () => {
  if (!isMasterOrHr.value && !nodeCd.value) {
    historyList.value = []
    totalCnt.value = 0
    await proxy.$alert('조회할 부서를 선택해 주세요.')
    return
  }
  page.value = 1
  await fnLoadList()
}

const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return
  page.value = target
  fnLoadList()
}

const fnOpenDetail = (item) => {
  selectedChangeReqId.value = item.changeReqId
  showDetailPop.value = true
}

onMounted(async () => {
  // 코드(라벨/드롭다운)를 먼저 로드해야 최초 조회 결과의 라벨 매핑이 정확하다.
  await fnGetSystinfoList()
  setDefaultPeriod()
  if (isMasterOrHr.value) {
    // master/hr: 회사 전사 기본(사업장 미지정). 진입 즉시 전사 자동조회.
    siteDisabled.value = false
    nodeDisabled.value = true
    await fnSearch()
  } else {
    // 노드 관리자: 세션 사업장 + 본인 담당 부서 프리셋. 프리셋 있으면 자동조회.
    siteCd.value = sessionStorage.getItem('gv_siteCd') ?? ''
    siteNo.value = sessionStorage.getItem('gv_siteNo') ?? ''
    siteNm.value = sessionStorage.getItem('gv_siteNm') ?? ''
    if (siteCd.value) {
      nodeDisabled.value = false
      nodeCd.value = sessionStorage.getItem('gv_nodeCd') ?? ''
      nodeNm.value = sessionStorage.getItem('gv_nodeNm') ?? ''
    }
    if (nodeCd.value) {
      await fnSearch()
    }
  }
})
</script>

<style scoped>
/* 조회조건이 여러 행으로 줄바꿈될 때 각 행의 왼쪽 끝선을 발의기간과 맞춘다.
   (전역 form.css는 첫 항목에만 margin-left를 줘서 두 번째 행이 좌측으로 밀린다.) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  /* 행 간 간격 축소(열 간격 2rem은 유지) */
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

/* 하위부서 포함 체크박스 (Attd_07 checkbox-label 패턴 차용) */
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

.grid-empty {
  text-align: center;
  padding: var(--card-padding);
  color: var(--color-text-muted);
}

/* 처리상태 배지 (읽기 전용 색 분기) */
.status-badge {
  display: inline-block;
  min-width: 48px;
  padding: 2px var(--btn-padding-sm);
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  line-height: 1.6;
}

.status-badge.is-confirmed {
  background: var(--color-primary);
  color: var(--color-surface);
}

.status-badge.is-rejected {
  background: var(--color-danger);
  color: var(--color-surface);
}

.status-badge.is-pending {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.status-badge.is-closed {
  background: var(--color-border);
  color: var(--color-text);
}

/* 페이징 (Tbm_04 패턴 미러) */
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
