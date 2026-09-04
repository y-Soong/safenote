<!--
  AdminApprovalDetailView.vue — 승인 상세 (공통 6섹션 + 유형별 본문 + 관리자 결정)
  - 작업 ID: 001-P2-F4 (분해: 001-phase2-admin-approval-plan.md §2-2 / §3-C A-2, §3-D A-3 / §5 정책 4종)
  - web 단일 출처: Attd_10.vue 상세 패널 / 재기획서 05-screen-structure §5.7(공통)·§5.8(유형별) / 07-interactions §7.2/§7.3/§7.4/§7.6
  - 진입: 대기·이력 카드 클릭 → /AdminApprovalDetail?reqId&group(&mode=history)
  - 백엔드: GET /appApi/admin/approval/detail?reqId&group  /  POST /appApi/admin/approval/process
      선점: 진입 시 lock, 이탈/처리완료 시 unlock (§7.2, A2)
  - 정책 4종 반영(서버 gate 산출값 렌더만, C1):
      ① 조정 후 승인(초과/근태보정/스케줄, 연차 제외) → AdminApprovalAdjustSheet
      ② 본인 결재 차단 + 상위 위임 → gate.selfBlockedYn
      ③ 선점(처리 잠금) 배너 → gate.lockedYn / lockedByNm
      ④ 마감 차단 → gate.closedYn
  - 디자인 토큰: 본 화면은 독립 라우트(셸 외부 가능)이므로 루트에 토큰 1회 선언(자식 시트는 자급).
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer) 골격만.
  - developer 라운드 스코프(TODO):
      (1) onMounted: detail 조회 + lock. onBeforeUnmount: unlock.
      (2) gate 에 따라 결정 영역 활성/비활성 렌더(서버 판정값, 역할 분기 금지)
      (3) 결정 process(요청대로/조정후/반려) → POST process → 성공 시 목록 복귀
      (4) 조정후승인 시 AdminApprovalAdjustSheet 결과를 adjusted 로 전송
-->
<template>
  <div class="ap-detail-view">
    <!-- 헤더 -->
    <header class="ap-detail-hd">
      <button type="button" class="ap-detail-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-ap-detail-chev-left" />
        </svg>
      </button>
      <h1 class="ap-detail-hd__title">승인 상세</h1>
      <span class="ap-detail-hd__spacer" aria-hidden="true" />
    </header>

    <main
      class="ap-detail-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출(공통 컴포저블) -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 로딩 -->
      <p v-if="isLoading" class="ap-detail-state" aria-live="polite">불러오는 중...</p>

      <template v-else-if="detail">
        <!-- ① 메타 -->
        <section class="ap-sec">
          <div class="ap-sec__head">
            <span class="ap-type-chip">{{ detail.meta.reqTypeNm }}</span>
            <span v-if="detail.meta.reqStatusNm" class="ap-status-chip">
              {{ detail.meta.reqStatusNm }}
            </span>
          </div>
          <dl class="ap-meta">
            <dt>요청번호</dt>
            <dd>{{ detail.meta.reqId }}</dd>
            <dt>요청자</dt>
            <dd>{{ detail.meta.requesterUserNm }} ({{ detail.meta.requesterUserCd }})</dd>
            <dt>소속</dt>
            <dd>{{ detail.meta.nodeNm || '-' }}</dd>
            <dt>근무일자</dt>
            <dd>{{ detail.meta.targetYmdDisplay || '-' }}</dd>
            <dt>요청일시</dt>
            <dd>{{ detail.meta.reqDateDisplay || '-' }}</dd>
            <dt v-if="detail.meta.deadlineText">마감</dt>
            <dd v-if="detail.meta.deadlineText">{{ detail.meta.deadlineText }}</dd>
            <!-- PRAFTA-001: 근태결재선통합 P4 — LEAVE 는 ③ 섹션에서 이미 표시(HR 최종 포함), 중복 방지로 제외.
                 근태보정/초과/스케줄은 P1(커밋 0edaefed)부터 meta.approvalStep 이 서버에서 채워진다. -->
            <dt v-if="detail.meta.approvalStep != null && detail.group !== 'LEAVE'">결재 단계</dt>
            <dd v-if="detail.meta.approvalStep != null && detail.group !== 'LEAVE'">
              결재 {{ detail.meta.approvalStep }}단계
            </dd>
          </dl>
        </section>

        <!-- ② 검증·제약 배너 (서버 gate 산출 — C1) -->
        <section v-if="gateBanner" class="ap-sec">
          <div class="ap-banner" :class="gateBanner.cls">
            <p class="ap-banner__text">{{ gateBanner.text }}</p>
          </div>
        </section>

        <!-- ③ 요청 내용 — 유형별 본문 -->
        <section class="ap-sec">
          <h2 class="ap-sec__title">요청 내용</h2>

          <!-- 근태보정: Before/After 비교 -->
          <div v-if="detail.group === 'CORRECTION'" class="ap-compare">
            <span v-if="detail.body.correctionTypeNm" class="ap-corr-chip">
              {{ detail.body.correctionTypeNm }}
            </span>
            <div class="ap-compare__row">
              <div class="ap-compare__col ap-compare__col--before">
                <span class="ap-compare__label">원본(보존)</span>
                <p class="ap-compare__val">{{ detail.body.beforeDisplay || '-' }}</p>
              </div>
              <div class="ap-compare__col ap-compare__col--after">
                <span class="ap-compare__label">보정</span>
                <p class="ap-compare__val">{{ detail.body.afterDisplay || '-' }}</p>
              </div>
            </div>
          </div>

          <!-- 초과근무: 당일 컨텍스트(스케줄/실근태/고정연장) + 시스템계산/상신/(조정)승인 3단 -->
          <div v-else-if="detail.group === 'OVERTIME'" class="ap-ot">
            <span v-if="detail.body.claimModeNm" class="ap-corr-chip">
              {{ detail.body.claimModeNm }}
            </span>
            <dl class="ap-meta">
              <!-- 2026-08-17: 승인 판단 컨텍스트 — 서버 완성 문자열(dayContext) 표시 전용.
                   값이 없어도 행을 유지해 '-' 로 명시한다(스케줄 미배정·근태 미기록도 판단 정보).
                   구서버(dayContext 미수신)면 세 행 모두 미노출(회귀 없음). -->
              <template v-if="detail.body.dayContext">
                <dt>당일 스케줄</dt>
                <dd>{{ dayScheduleDisplay || '-' }}</dd>
                <dt>실제 근무</dt>
                <dd class="ap-meta__dd--break">{{ detail.body.dayContext.actualText || '-' }}</dd>
                <dt>고정연장</dt>
                <dd>{{ detail.body.dayContext.fixedOtText || '-' }}</dd>
              </template>
              <dt>시스템 계산</dt>
              <dd>{{ detail.body.systemCalcDisplay || '-' }}</dd>
              <dt>근로자 상신</dt>
              <dd class="ap-meta__dd--break">
                {{ breakAfterTilde(detail.body.claimedDisplay) || '-' }}
              </dd>
              <dt v-if="detail.body.approvedDisplay">승인값</dt>
              <dd v-if="detail.body.approvedDisplay">{{ detail.body.approvedDisplay }}</dd>
            </dl>
            <p class="ap-ot__note">휴게 면제 30분이 반영된 값입니다.</p>
          </div>

          <!-- 연차: 타입 + 구간 + 잔여 + (단계) -->
          <div v-else-if="detail.group === 'LEAVE'" class="ap-leave">
            <dl class="ap-meta">
              <dt>연차 타입</dt>
              <dd>{{ detail.body.leaveNm || '-' }} {{ detail.body.paidYnNm }}</dd>
              <dt>사용 단위</dt>
              <dd>{{ detail.body.unitNm || '-' }}</dd>
              <dt>사용 구간</dt>
              <dd class="ap-meta__dd--range">{{ detail.body.appliedRangeDisplay || '-' }}</dd>
              <!-- 가불표시-04: 가불 포함 신청 표기 — borrowDays > 0 일 때만(일 단위 표기는 formatLeaveDaysOnly 단일 출처) -->
              <dt v-if="Number(detail.body.borrowDays) > 0">가불</dt>
              <dd v-if="Number(detail.body.borrowDays) > 0">
                가불 {{ formatLeaveDaysOnly(detail.body.borrowDays) }} 포함
              </dd>
              <!-- BW-08: 휴게 미이용 요청(근기법 제54조① 단서) — 서버 body.brkWaiveYn / brkWaiveReqDtime.
                   필드 부재(구서버)면 미노출. 승인/반려 게이트 무변경(차단 없음). -->
              <dt v-if="detail.body.brkWaiveYn === 'Y'">휴게 미이용</dt>
              <dd v-if="detail.body.brkWaiveYn === 'Y'" class="ap-meta__dd--brk-waive">
                근로자 요청 · {{ detail.body.brkWaiveReqDtime || '-' }}
              </dd>
              <!-- 법정 휴게 하한 경고(차단 없음) — 문구 서버 제공 -->
              <dt v-if="detail.body.brkLegalWarnYn === 'Y'">법정 휴게</dt>
              <dd
                v-if="detail.body.brkLegalWarnYn === 'Y'"
                class="ap-meta__dd--legal-warn"
                role="status"
              >
                {{ detail.body.brkLegalWarnMsg }}
              </dd>
              <dt>잔여 현황</dt>
              <dd>
                부여 {{ fnBalanceDays(detail.body.balance?.granted) }} · 사용
                {{ fnBalanceDays(detail.body.balance?.used) }} · 잔여
                {{ fnBalanceDays(detail.body.balance?.remain) }}
              </dd>
              <dt v-if="detail.body.stepDisplay">결재 단계</dt>
              <dd v-if="detail.body.stepDisplay">{{ detail.body.stepDisplay }}</dd>
            </dl>
          </div>

          <!-- 스케줄수정 / 기본근무타입변경(PRAFTA-003): 현재 → 요청 스케줄 비교(근태보정 ap-compare 패턴 재사용) -->
          <div
            v-else-if="detail.group === 'SCHEDULE' || detail.group === 'DEFAULT_SCH_CHANGE'"
            class="ap-compare"
          >
            <div class="ap-compare__row">
              <div class="ap-compare__col ap-compare__col--before">
                <span class="ap-compare__label">{{
                  detail.group === 'DEFAULT_SCH_CHANGE' ? '현재 기본 근무타입' : '현재 스케줄'
                }}</span>
                <p class="ap-compare__val">{{ detail.body.beforeDisplay || '없음' }}</p>
              </div>
              <div class="ap-compare__col ap-compare__col--after">
                <span class="ap-compare__label">{{
                  detail.group === 'DEFAULT_SCH_CHANGE' ? '요청 기본 근무타입' : '요청 스케줄'
                }}</span>
                <p class="ap-compare__val">{{ detail.body.afterDisplay || '-' }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 앞뒤 근무일(D-1 / D+1) 근태 — 근태보정 한정(겹침가드 개선 2026-08-06).
             이웃 근무일의 미마감 근태가 이 승인을 막는 원인일 때 화면에서 특정한다.
             0건이면 섹션 자체를 렌더하지 않는다. -->
        <AdminApprovalNeighborSegments
          v-if="detail.group === 'CORRECTION' && (detail.body.neighborSegments || []).length"
          :segments="detail.body.neighborSegments"
        />

        <!-- ④ 사유·증빙 -->
        <section class="ap-sec">
          <h2 class="ap-sec__title">사유 · 증빙</h2>
          <p class="ap-reason">{{ detail.reason || '-' }}</p>
          <ul v-if="(detail.attachments || []).length" class="ap-files">
            <li v-for="f in detail.attachments" :key="f.fileId" class="ap-files__item">
              <button
                type="button"
                class="ap-files__btn"
                :disabled="evidenceLoadingId === f.fileId"
                @click="onViewAttachment(f.fileId)"
              >
                {{ evidenceLoadingId === f.fileId ? '불러오는 중...' : f.fileNm }}
              </button>
            </li>
          </ul>

          <!-- 증빙 뷰어 오버레이 (연차 신청 증빙 필수화 2026-08-29) -->
          <div v-if="evidenceViewerSrc" class="ap-evid-viewer" @click="onCloseEvidenceViewer">
            <img :src="evidenceViewerSrc" alt="첨부 원본" />
          </div>
        </section>

        <!-- ⑤ 관리자 결정 (이력 모드/차단 시 숨김·비활성) -->
        <section v-if="!isHistory" class="ap-sec ap-decide">
          <h2 class="ap-sec__title">관리자 결정</h2>

          <fieldset class="ap-radios" :disabled="!gate.canProcess">
            <label class="ap-radio">
              <input type="radio" v-model="decision" value="APPROVE_ASIS" />
              <span>요청대로 승인</span>
            </label>
            <!-- ① 조정 후 승인 — 연차(LEAVE) 제외, 스케줄은 A5 전 비활성 -->
            <label v-if="adjustable" class="ap-radio">
              <input type="radio" v-model="decision" value="APPROVE_ADJUST" />
              <span>조정 후 승인</span>
            </label>
            <label class="ap-radio">
              <input type="radio" v-model="decision" value="REJECT" />
              <span>반려</span>
            </label>
          </fieldset>

          <!-- 조정 후 승인 선택 시 요약(편집은 시트에서) -->
          <button
            v-if="decision === 'APPROVE_ADJUST' && gate.canProcess"
            type="button"
            class="ap-decide__adjust-open"
            @click="adjustSheetOpen = true"
          >
            조정 값 입력 / 수정
          </button>

          <div class="ap-decide__actions">
            <button
              type="button"
              class="ap-btn ap-btn--primary"
              :class="{ 'ap-btn--danger': decision === 'REJECT' }"
              :disabled="!gate.canProcess || processing"
              @click="onProcess"
            >
              처리하기
            </button>
          </div>
        </section>
      </template>
    </main>

    <!-- 반려 사유 시트(②⑤) -->
    <AdminApprovalRejectSheet
      :open="rejectSheetOpen"
      :submitting="processing"
      @close="rejectSheetOpen = false"
      @confirm="onConfirmReject"
    />

    <!-- 조정 후 승인 입력 시트(①) -->
    <AdminApprovalAdjustSheet
      :open="adjustSheetOpen"
      :group="detail ? detail.group : ''"
      :body="detail ? detail.body : null"
      :submitting="processing"
      @close="adjustSheetOpen = false"
      @confirm="onConfirmAdjust"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="ap-detail-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-ap-detail-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  formatYmdDisplay,
  formatDateTimeDisplay,
  formatTimeWithDateIfDiff,
  formatHhmmDisplay,
} from '@/utils/approvalFormat'
// 가불표시-04: 일 단위 수량 표기 단일 출처(2026-08-09 규약)
import { formatLeaveDaysOnly } from '@/utils/leaveFormat'

import AdminApprovalRejectSheet from './components/AdminApprovalRejectSheet.vue'
import AdminApprovalAdjustSheet from './components/AdminApprovalAdjustSheet.vue'
import AdminApprovalNeighborSegments from './components/AdminApprovalNeighborSegments.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

// 잔여 현황 수량 표기 — 백엔드 원시 소수(예: 9.68749)를 일 단위 규약(2026-08-09,
// formatLeaveDaysOnly 2자리 반올림)으로 정리한다. 값 부재 시에만 '-' 유지.
const fnBalanceDays = (v) => (v == null ? '-' : formatLeaveDaysOnly(v))

// 공통: alert / confirm 폴백(앱 전역 우선) — AdminTbmSessionDetailView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// 진입 식별자 — reqId/group 만 사용(나머지 식별자는 토큰 클레임, IDOR 차단 plan §3/§4)
const reqId = computed(() => route.query.reqId || '')
const queryGroup = computed(() => route.query.group || '')
// 이력 모드(읽기전용) — route query.mode==='history' (대기/이력 상세 공용, 이력은 후속 골격)
const isHistory = computed(() => route.query.mode === 'history')

// ── 상태(서버 응답으로 채움) ──────────────────────────────────────────────
const isLoading = ref(false)
const detail = ref(null) // { meta, gate, body, reason, attachments, group }
// gate 기본값(서버 산출 전 보수적으로 비활성) — ②③④ 단일 출처
const gate = computed(
  () =>
    detail.value?.gate || {
      canProcess: false,
      selfBlockedYn: false,
      closedYn: false,
      lockedYn: false,
      lockedByNm: '',
      conflictYn: false,
      conflictMsg: '',
    },
)

// 결정 라디오: APPROVE_ASIS | APPROVE_ADJUST | REJECT
const decision = ref('APPROVE_ASIS')
const processing = ref(false)

// 시트 토글(UI 토글 — 허용 범위)
const rejectSheetOpen = ref(false)
const adjustSheetOpen = ref(false)

// ── 첨부(연차 증빙 등) 열람 상태 — fileId=fileMgmtCd (연차 신청 증빙 필수화 2026-08-29) ──
const evidenceLoadingId = ref('')
const evidenceViewerSrc = ref('')

// blob objectURL 해제 — 오버레이 닫기/화면 이탈 시 즉시 정리(메모리 누수 방지)
const revokeEvidenceUrl = () => {
  if (evidenceViewerSrc.value) {
    try {
      URL.revokeObjectURL(evidenceViewerSrc.value)
    } catch (e) {
      console.warn('[AdminApprovalDetail] objectURL 해제 실패:', e?.message)
    }
    evidenceViewerSrc.value = ''
  }
}

const onCloseEvidenceViewer = () => {
  revokeEvidenceUrl()
}

// 첨부 blob 로드 → 오버레이 표시. 실패는 showAlert 고정 메시지(MyContractView.vue catch 패턴).
const onViewAttachment = async (fileId) => {
  if (evidenceLoadingId.value || !fileId) return
  evidenceLoadingId.value = fileId
  try {
    const res = await api.get(`/appApi/leaveflow/evidence-file/${fileId}`, {
      responseType: 'blob',
    })
    revokeEvidenceUrl()
    evidenceViewerSrc.value = URL.createObjectURL(res.data)
  } catch (e) {
    console.warn('[AdminApprovalDetail] 첨부 조회 실패:', e?.message)
    await showAlert('첨부 파일을 불러오지 못했어요.')
  } finally {
    evidenceLoadingId.value = ''
  }
}

onUnmounted(() => {
  revokeEvidenceUrl()
})

// ① 조정 후 승인 가능 유형: 근태보정/초과(스케줄은 A5 전 제외). 연차 제외(§5.8.4).
// TODO(developer): v1 보류 — 백엔드 APPROVE_ADJUST 조정값 입력은 R3 라운드에서 구현 예정.
//   백엔드 미지원 상태에서 라디오를 노출하면 ATTD_400_006 이 발생하므로 노출 자체를 차단한다.
//   R3 구현 후 원복: ['CORRECTION', 'OVERTIME'].includes(detail.value?.group)
const adjustable = computed(() => false)

// 초과근무 당일 스케줄 표시: "{스케줄번호} · {소정 구간}" (2026-08-17 — 서버 dayContext 결합만).
const dayScheduleDisplay = computed(() => {
  const c = detail.value?.body?.dayContext
  if (!c) return ''
  return [c.schNm, c.scheduleText].filter(Boolean).join(' · ')
})

// ② 검증·제약 배너 텍스트/색상(서버 gate 산출값 렌더만)
const gateBanner = computed(() => {
  const g = gate.value
  if (g.selfBlockedYn) {
    return {
      cls: 'ap-banner--warning',
      text: '이 요청은 본인의 결재 권한 범위가 아닙니다. 상위 관리자에게 처리가 위임됩니다.',
    }
  }
  if (g.closedYn) {
    return { cls: 'ap-banner--danger', text: '근태 마감 후 — 처리할 수 없습니다.' }
  }
  if (g.lockedYn) {
    return {
      cls: 'ap-banner--neutral',
      text: `${g.lockedByNm || '다른 관리자'}이(가) 처리 중입니다.`,
    }
  }
  if (g.conflictYn) {
    return { cls: 'ap-banner--warning', text: g.conflictMsg || '충돌이 감지되었습니다.' }
  }
  return null
})

// ── 응답 정규화(서버 원본 → template 표시 필드) ───────────────────────────
//   서버가 *Display 문자열을 내려주면 그 값을 우선 사용하고, 원본만 오면 본 함수가 파생.
//   비즈니스 판정(gate)은 서버 산출값을 그대로 전달(C1, 가공 금지).
const SYS033_STATUS_NM = { '01': '대기', '02': '승인', '03': '반려', '04': '취소' }

// "출근 .. · 퇴근 .." 비교행 문자열(근태보정 before/after).
//   근무일자(targetYmd)와 같은 날이면 시각만(HH:mm), 다른 날(오버나이트)이면 "MM.DD HH:mm"으로 날짜를 덧붙인다.
//   상단 메타에 근무일자가 이미 표시되므로 기본은 시각만으로 간결하게 노출한다.
const buildCheckDisplay = (o, targetYmd) => {
  if (!o || typeof o !== 'object') return ''
  const parts = []
  if (o.checkIn) parts.push(`출근 ${formatTimeWithDateIfDiff(o.checkIn, targetYmd)}`)
  if (o.checkOut) parts.push(`퇴근 ${formatTimeWithDateIfDiff(o.checkOut, targetYmd)}`)
  // 출근/퇴근을 각 줄로 분리(개행). 표시 영역(.ap-compare__val)에 white-space: pre-line 적용.
  return parts.join('\n')
}

// "{start} ~ {end} ({minutes}분)" 구간 문자열(초과근무 systemCalc/claimed/approved)
const buildRangeDisplay = (o) => {
  if (!o || typeof o !== 'object') return ''
  if (!o.startAt && !o.endAt) return ''
  const range = `${o.startAt || '-'} ~ ${o.endAt || '-'}`
  return o.minutes != null ? `${range} (${o.minutes}분)` : range
}

// 시작 ~ 종료 구간 문자열을 '~' 뒤에서 개행(종료 시각을 다음 줄로)
const breakAfterTilde = (v) => {
  if (!v || typeof v !== 'string') return v
  return v.replace(/\s*~\s*/, ' ~\n')
}

// 연차 사용 구간(문자열 또는 {from,to})
/**
 * 연차 사용 구간 표시 문자열.
 *
 * <p>서버(body.appliedRange)는 startDate/startTime/endDate/endTime 을 원본 포맷으로 준다
 * (날짜 YYYYMMDD, 시각 HHmm, 종일 신청이면 시각 null).
 * ★종전 구현은 from/to/startYmd/endYmd 를 찾았는데 서버가 보내는 키와 하나도 맞지 않아
 *   항상 빈 문자열이 됐고, 화면에 "사용 구간 -" 만 떴다. 시각은 읽지도 않았다.
 *   "언제부터 언제까지 쉬는지"가 승인 판단의 핵심 정보다 — 키 이름을 임의로 바꾸지 말 것.
 *
 * <p>표기(웹 Attd_10 과 동일 정보를 한 줄로 합친다):
 *   같은 날 + 시간대 → "2026.08.11 14:00 ~ 15:00"  (날짜를 반복하지 않는다)
 *   같은 날 + 종일   → "2026.08.11"
 *   기간 + 종일      → "2026.08.11 ~ 2026.08.13"
 *   기간 + 시간대    → "2026.08.11 14:00 ~ 2026.08.13 15:00"
 *
 * <p>★날짜와 시각 사이는 <b>줄바꿈 없는 공백(U+00A0)</b>으로 묶는다. 일반 공백이면 좁은 화면에서
 *   "2026.08.11" 과 "14:00" 사이가 갈라져 어색하게 개행된다. 이렇게 두면 끊길 수 있는 자리가
 *   " ~ " 양옆뿐이라, 넘칠 때 "시작구간 ~ / 종료구간" 으로만 깔끔하게 접힌다.
 */
const NBSP = ' '

const buildAppliedRange = (v) => {
  if (!v) return ''
  if (typeof v === 'string') return v
  if (typeof v !== 'object') return ''

  const startDate = formatYmdDisplay(v.startDate)
  const endDate = formatYmdDisplay(v.endDate)
  const startTime = formatHhmmDisplay(v.startTime)
  const endTime = formatHhmmDisplay(v.endTime)

  // 날짜 + 시각을 한 덩어리로(중간에서 개행되지 않게)
  const glue = (date, time) => (date && time ? `${date}${NBSP}${time}` : date || time || '')

  const sameDay = !endDate || startDate === endDate
  const head = glue(startDate, startTime)

  // 같은 날이면 종료는 시각만 — 날짜를 두 번 쓰지 않는다.
  const tail = sameDay ? endTime : glue(endDate, endTime)

  if (head && tail) return `${head} ~ ${tail}`
  return head || tail || ''
}

// 스케줄 표시 결합(현재/요청): { schNm, rangeText } → "{schNm} · {rangeText}".
//   rangeText 는 서버가 HH:MM 콜론 포맷으로 내려줌(D10) — 프론트는 결합만, 신규 포맷 없음.
const buildSchedDisplay = (s) => {
  if (!s || typeof s !== 'object') return ''
  return [s.schNm, s.rangeText].filter(Boolean).join(' · ')
}

const deadlineTextOf = (meta) => {
  const d = meta?.deadlineDday
  if (d === null || d === undefined) return ''
  return Number(d) <= 0 ? '마감 도래' : `마감 D-${d}`
}

const normalizeDetail = (data) => {
  if (!data || !data.meta) return null
  const meta = data.meta
  const group = meta.group || queryGroup.value
  const rawBody = data.body || {}

  let body = { ...rawBody }
  if (group === 'CORRECTION') {
    body.beforeDisplay = rawBody.beforeDisplay || buildCheckDisplay(rawBody.before, meta.targetYmd)
    body.afterDisplay = rawBody.afterDisplay || buildCheckDisplay(rawBody.after, meta.targetYmd)
    // 겹침가드 개선(2026-08-06): 앞뒤 근무일(D-1 / D+1) 근태 구간. 표시 문자열·status 는 서버 완성값이라
    //   배열 정규화만 한다(미수신·구서버면 빈 배열 → 섹션 미노출, 회귀 없음).
    body.neighborSegments = Array.isArray(rawBody.neighborSegments) ? rawBody.neighborSegments : []
  } else if (group === 'OVERTIME') {
    body.claimModeNm =
      rawBody.claimModeNm ||
      (rawBody.claimMode === 'post' ? '사후 상신' : rawBody.claimMode === 'pre' ? '사전 상신' : '')
    body.systemCalcDisplay = rawBody.systemCalcDisplay || buildRangeDisplay(rawBody.systemCalc)
    body.claimedDisplay = rawBody.claimedDisplay || buildRangeDisplay(rawBody.claimed)
    body.approvedDisplay = rawBody.approvedDisplay || buildRangeDisplay(rawBody.approved)
  } else if (group === 'LEAVE') {
    body.paidYnNm =
      rawBody.paidYnNm ||
      (rawBody.paidYn === 'Y' || rawBody.paidYn === true
        ? '(유급)'
        : rawBody.paidYn === 'N' || rawBody.paidYn === false
          ? '(무급)'
          : '')
    body.appliedRangeDisplay =
      rawBody.appliedRangeDisplay || buildAppliedRange(rawBody.appliedRange)
    if (!body.stepDisplay) {
      if (rawBody.hrFinalYn === 'Y' || rawBody.hrFinalYn === true)
        body.stepDisplay = 'HR 최종 승인 단계'
      else if (meta.approvalStep != null) body.stepDisplay = `결재 ${meta.approvalStep}단계`
    }
  } else if (group === 'SCHEDULE' || group === 'DEFAULT_SCH_CHANGE') {
    // 현재(승인 전 work_plan / TB_USER.DEFAULT_SCH_CD) → 요청(REQ.SCH_CD) 스케줄.
    //   서버 before/after = { schCd, schNm, rangeText } — PRAFTA-003 도 동일 shape 재사용.
    body.beforeDisplay = rawBody.beforeDisplay || buildSchedDisplay(rawBody.before)
    body.afterDisplay = rawBody.afterDisplay || buildSchedDisplay(rawBody.after)
  }

  return {
    group,
    meta: {
      ...meta,
      reqStatusNm: meta.reqStatusNm || SYS033_STATUS_NM[meta.reqStatus] || '',
      targetYmdDisplay: meta.targetYmdDisplay || formatYmdDisplay(meta.targetYmd),
      reqDateDisplay: meta.reqDateDisplay || formatDateTimeDisplay(meta.reqDate),
      deadlineText: meta.deadlineText || deadlineTextOf(meta),
    },
    gate: data.gate || null,
    body,
    reason: data.reason || '',
    attachments: Array.isArray(data.attachments) ? data.attachments : [],
  }
}

// ── 조회 ──────────────────────────────────────────────────────────────────
const loadDetail = async () => {
  if (!reqId.value || !queryGroup.value) {
    await showAlert('잘못된 접근입니다.')
    router.back()
    return
  }
  isLoading.value = true
  try {
    const res = await api.get('/appApi/admin/approval/detail', {
      params: { reqId: reqId.value, group: queryGroup.value },
    })
    detail.value = normalizeDetail(res?.data)
    if (!detail.value) {
      await showAlert('요청 정보를 불러오지 못했어요.')
      router.back()
    }
  } catch (e) {
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림 후 복귀.
    console.error('[AdminApprovalDetailView] 상세 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '요청 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
    await showAlert(msg)
    router.back()
  } finally {
    isLoading.value = false
  }
}

// ── 처리(요청대로/조정후/반려) ────────────────────────────────────────────
//   처리 전 서버가 ②③④ 재검증 + 멱등(409). 409 = 다른 관리자가 이미 처리 → 안내 후 목록 복귀.
const submitProcess = async (extra) => {
  if (processing.value) return
  processing.value = true
  try {
    const payload = {
      reqId: reqId.value,
      group: detail.value?.group || queryGroup.value,
      ...extra,
    }
    // 연차 등 다단 결재는 approvalStep 필수(§3-D) — 서버 산출값을 그대로 회신.
    if (detail.value?.meta?.approvalStep != null) {
      payload.approvalStep = detail.value.meta.approvalStep
    }
    await api.post('/appApi/admin/approval/process', payload)
    rejectSheetOpen.value = false
    adjustSheetOpen.value = false
    await showAlert('처리되었습니다.')
    router.back()
  } catch (e) {
    if (e?.response?.status === 409) {
      rejectSheetOpen.value = false
      adjustSheetOpen.value = false
      await showAlert('다른 관리자가 이미 처리했습니다.')
      router.back()
      return
    }
    const msg = resolveApiErrorMessage(e, '처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
    await showAlert(msg)
  } finally {
    processing.value = false
  }
}

// ── 액션 ────────────────────────────────────────────────────────────────
const onBack = () => {
  // 선점 잠금(§7.2, A2)은 v1 미구현(사용자 확정) → unlock 호출 없이 복귀.
  router.back()
}

// 처리하기 — 결정 분기. 조정/반려는 시트로 위임, 요청대로는 confirm 후 즉시 처리.
const onProcess = async () => {
  if (!gate.value.canProcess || processing.value) return
  if (decision.value === 'REJECT') {
    rejectSheetOpen.value = true
    return
  }
  if (decision.value === 'APPROVE_ADJUST') {
    adjustSheetOpen.value = true
    return
  }
  const ok = await askConfirm('요청 내용 그대로 승인할까요?')
  if (!ok) return
  await submitProcess({ decision: 'APPROVE_ASIS' })
}

// 반려 확정(시트에서 사유 수신, 10자↑은 시트가 검증 — 서버도 재검증)
const onConfirmReject = (reason) => {
  submitProcess({ decision: 'REJECT', comment: reason })
}

// 조정 후 승인 확정(시트에서 조정 페이로드 수신)
const onConfirmAdjust = (adjusted) => {
  submitProcess({ decision: 'APPROVE_ADJUST', adjusted })
}

// ── 당겨서 새로고침 (공통 컴포저블) — 상세(gate/내용)를 재조회. 부작용 없는 조회만. ──
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(
  scrollRef,
  async () => {
    await loadDetail()
  },
)

onMounted(loadDetail)
</script>

<style scoped>
.ap-detail-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-disabled-bg: #f3f4f6;
  --color-disabled-text: #9ca3af;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.ap-detail-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.ap-detail-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.ap-detail-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.ap-detail-hd__spacer {
  width: 36px;
}

/* 본문 */
.ap-detail-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.ap-detail-state {
  margin: 0;
  padding: 48px 16px;
  text-align: center;
  color: var(--color-text-secondary);
}

/* 섹션 카드 */
.ap-sec {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
}
.ap-sec__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.ap-sec__title {
  margin: 0 0 var(--space-sm);
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ap-type-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
}
.ap-status-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

/* 메타 dl */
.ap-meta {
  display: grid;
  grid-template-columns: 6.5rem 1fr;
  gap: 6px 10px;
  margin: 0;
  font-size: 13px;
}
.ap-meta dt {
  color: var(--color-text-tertiary);
  /* 라벨이 컬럼 폭(6.5rem)을 넘어도 글자 중간이 아닌 어절 단위로만 개행 */
  word-break: keep-all;
}
.ap-meta dd {
  margin: 0;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.ap-meta__dd--break {
  white-space: pre-line;
}
/* 사용 구간 — 좁은 화면에서 날짜·시각이 토막나지 않게 한다.
   값 안의 날짜~시각 사이는 U+00A0 로 묶여 있어 끊길 수 있는 자리가 " ~ " 뿐이고,
   keep-all 로 어절 중간 분절까지 막아 "시작구간 ~ / 종료구간" 으로만 접힌다. */
.ap-meta__dd--range {
  word-break: keep-all;
  overflow-wrap: normal;
}
/* BW-08: 휴게 미이용 요청 — primary 톤(요청 사실) */
.ap-meta__dd--brk-waive {
  color: var(--color-primary);
  font-weight: 600;
}
/* BW-08: 법정 휴게 하한 경고 — warning 텍스트 토큰(차단 없음, 표시 전용) */
.ap-meta__dd--legal-warn {
  color: var(--color-warning-text);
  font-weight: 600;
  word-break: keep-all;
}

/* 배너 */
.ap-banner {
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
.ap-banner__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}
.ap-banner--warning {
  background: var(--color-warning-tint);
}
.ap-banner--warning .ap-banner__text {
  color: var(--color-warning-text);
}
.ap-banner--danger {
  background: var(--color-danger-tint);
}
.ap-banner--danger .ap-banner__text {
  color: var(--color-danger-text);
}
.ap-banner--neutral {
  background: var(--color-border-light);
}
.ap-banner--neutral .ap-banner__text {
  color: var(--color-text-secondary);
}

/* 비교(근태보정) */
.ap-corr-chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: var(--radius-sm);
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 600;
  margin-bottom: var(--space-sm);
}
.ap-compare__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}
.ap-compare__col {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px;
}
.ap-compare__col--after {
  border-color: var(--color-primary-tint-border);
  background: var(--color-primary-tint);
}
.ap-compare__label {
  display: block;
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}
.ap-compare__val {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  /* 근태보정 출근/퇴근을 개행(\n)으로 줄바꿈 표시. 스케줄(개행 없음)엔 영향 없음. */
  white-space: pre-line;
}

/* 초과근무 */
.ap-ot__note {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 사유·증빙 */
.ap-reason {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
}
.ap-files {
  list-style: none;
  margin: var(--space-sm) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ap-files__item {
  font-size: 13px;
  color: var(--color-primary);
}
.ap-files__btn {
  background: none;
  border: 0;
  padding: 0;
  color: var(--color-primary);
  font-size: inherit;
  text-decoration: underline;
  cursor: pointer;
  font-family: inherit;
}
.ap-files__btn:disabled {
  opacity: 0.6;
  cursor: progress;
  text-decoration: none;
}
.ap-evid-viewer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.ap-evid-viewer img {
  max-width: 92vw;
  max-height: 92vh;
  object-fit: contain;
}

/* 관리자 결정 */
.ap-radios {
  border: 0;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.ap-radios:disabled {
  opacity: 0.5;
}
.ap-radio {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  /* label 박스가 행 전체(가로 100%)라 웹뷰 기본 탭 하이라이트가 행 전체를 덮는다.
     선택 피드백은 라디오 자체의 체크 상태로 충분하므로 넓은 배경은 제거한다.
     (전역 base 규칙과 중복이지만, 이 화면의 의도를 명시적으로 남긴다) */
  -webkit-tap-highlight-color: transparent;
}
.ap-decide__adjust-open {
  margin-top: var(--space-md);
  width: 100%;
  height: 44px;
  border: 1px dashed var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.ap-decide__actions {
  margin-top: var(--space-md);
}
.ap-btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ap-btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.ap-btn--danger {
  background: var(--color-danger);
  color: var(--color-surface);
}
.ap-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 스프라이트 */
.ap-detail-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
