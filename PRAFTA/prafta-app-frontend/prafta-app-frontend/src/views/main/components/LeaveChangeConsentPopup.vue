<!--
  LeaveChangeConsentPopup.vue — 관리자 발의 연차 변경/삭제 동의 팝업 (모바일 앱, 신규)
  - 작업 맥락: 관리자가 근무계획관리(웹 Attd_05)에서 본인 연차(연차/월차 무관)의 이동/삭제를
    요청(REQUESTED)하면, 앱 진입 시 자동으로 본 팝업을 띄워 즉시 동의/거부할 수 있게 한다.
    (앱 진입점은 메인화면 배너(A) + 진입 자동 팝업(B) 단일 동선 — 별도 전체화면 없음)
  - 참조 패턴: LeavePromotionLoginPopup.vue / NoticeLoginPopup.vue
      (딤 + 카드 + 본문만 스크롤 + 디자인토큰 루트 1회 재선언)
  - 역할: 목록/상세 표시 + 동의/거부(사유) UI. 실제 응답 API 호출/재조회는 부모(MainView)가 담당(emit).
-->
<template>
  <div v-if="open && items.length" class="lcc-popup" role="dialog" aria-modal="true">
    <div class="lcc-popup__dim" aria-hidden="true"></div>

    <div class="lcc-popup__card">
      <header class="lcc-popup__head">
        <p class="lcc-popup__heading">연차 변경 동의 요청</p>
        <button
          type="button"
          class="lcc-popup__close"
          aria-label="닫기"
          @click="onClose"
        >
          ✕
        </button>
      </header>

      <div class="lcc-popup__body">
        <p class="lcc-popup__lead">
          관리자가 요청한 연차 변경/삭제가
          <strong class="lcc-popup__num">{{ items.length }}</strong>
          건 있어요. 동의 또는 거부해 주세요.
        </p>

        <ul class="lcc-list">
          <li v-for="req in items" :key="req.changeReqId" class="lcc-card">
            <!-- 변경 전 → 후를 한 줄로 묶어 한눈에 비교. 삭제 요청은 "변경 후"가 없으므로 '삭제'로 표기. -->
            <div class="lcc-card__transition">
              <div class="lcc-trans__item">
                <span class="lcc-trans__cap">변경 전</span>
                <span class="lcc-trans__date">{{ fmtYmd(req.targetStartDate) }}</span>
              </div>
              <span class="lcc-trans__arrow" aria-hidden="true">→</span>
              <div class="lcc-trans__item">
                <span class="lcc-trans__cap">변경 후</span>
                <span
                  class="lcc-trans__date"
                  :class="{ 'is-delete': req.reqType !== 'MOVE' }"
                >
                  {{ req.reqType === 'MOVE' ? fmtYmd(req.moveTargetDate) : '삭제' }}
                </span>
              </div>
            </div>
            <!-- G1: 대상 연차 속성(종류/단위/구간/차감량) — 근로자가 "무슨 연차인지" 알고 동의하도록.
                 사용 구간은 시간차(02~04)만 값이 있다(반차·반반차는 시각 미기록). -->
            <div v-if="req.leaveNm" class="lcc-card__row">
              <span class="lcc-card__label">연차 종류</span>
              <span class="lcc-card__value">{{ req.leaveNm }}</span>
            </div>
            <div v-if="unitLabel(req)" class="lcc-card__row">
              <span class="lcc-card__label">사용 단위</span>
              <span class="lcc-card__value">{{ unitLabel(req) }}</span>
            </div>
            <div v-if="timeRange(req)" class="lcc-card__row">
              <span class="lcc-card__label">사용 구간</span>
              <span class="lcc-card__value">{{ timeRange(req) }}</span>
            </div>
            <div v-if="leaveDaysLabel(req)" class="lcc-card__row">
              <span class="lcc-card__label">차감 일수</span>
              <span class="lcc-card__value">{{ leaveDaysLabel(req) }}</span>
            </div>
            <div class="lcc-card__row">
              <span class="lcc-card__label">요청유형</span>
              <span class="lcc-card__value">{{ reqTypeNm(req.reqType) }}</span>
            </div>
            <div class="lcc-card__row">
              <span class="lcc-card__label">관리자 사유</span>
              <span class="lcc-card__value">{{ req.reqReason || '-' }}</span>
            </div>

            <!-- 거부 사유 입력(거부 선택 시 노출) -->
            <textarea
              v-if="rejectingId === req.changeReqId"
              v-model="rejectReason"
              class="lcc-reject-reason"
              rows="2"
              maxlength="500"
              placeholder="거부 사유를 입력하세요 (필수)"
            ></textarea>

            <div class="lcc-card__actions">
              <!-- F-10 규약: 왼쪽=진행/확정(파괴적 진행=danger), 오른쪽=이탈(취소) -->
              <template v-if="rejectingId === req.changeReqId">
                <button
                  type="button"
                  class="lcc-btn lcc-btn--danger"
                  :disabled="!rejectReason.trim() || submitting"
                  @click="onReject(req)"
                >
                  거부 확정
                </button>
                <button
                  type="button"
                  class="lcc-btn lcc-btn--ghost"
                  :disabled="submitting"
                  @click="cancelReject"
                >
                  취소
                </button>
              </template>
              <template v-else>
                <button
                  type="button"
                  class="lcc-btn lcc-btn--ghost"
                  :disabled="submitting"
                  @click="startReject(req)"
                >
                  거부
                </button>
                <button
                  type="button"
                  class="lcc-btn lcc-btn--primary"
                  :disabled="submitting"
                  @click="onAgree(req)"
                >
                  동의
                </button>
              </template>
            </div>
          </li>
        </ul>
      </div>

      <footer class="lcc-popup__footer">
        <button
          type="button"
          class="lcc-popup__btn lcc-popup__btn--ghost"
          :disabled="submitting"
          @click="onClose"
        >
          나중에
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'
import { formatMinutesToHm, trimRawDays } from '@/utils/leaveFormat'

const props = defineProps({
  // 팝업 표시 여부 (v-model:open)
  open: {
    type: Boolean,
    default: false,
  },
  // 미응답(REQUESTED) 관리자 발의 요청 목록 — 부모(MainView)가 조회해 전달.
  //   각 항목: { changeReqId, reqType('MOVE'|'DELETE'), targetStartDate(YYYYMMDD), moveTargetDate, reqReason,
  //             leaveNm, useUnitType(SYS025), unitNm, startTime(HHmm), endTime(HHmm), leaveDays, leaveMinutes }
  items: {
    type: Array,
    default: () => [],
  },
  // 응답 처리 중(중복 제출 방지) — 부모가 제어.
  submitting: {
    type: Boolean,
    default: false,
  },
})

// open(v-model) + 응답 결과를 부모로 전달
const emit = defineEmits(['update:open', 'agree', 'reject', 'closed'])

// 거부 입력 상태(어느 카드의 거부 사유 입력을 펼쳤는지)
const rejectingId = ref('')
const rejectReason = ref('')

// 코드 → 라벨 (서버 row 는 코드값만 반환)
const REQ_TYPE_NM = { MOVE: '이동', DELETE: '삭제' }
const reqTypeNm = (t) => REQ_TYPE_NM[t] || t

// YYYYMMDD → "YYYY.MM.DD" (표시 단일 출처 위임, D1)
const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return formatYmdDisplay(ymd)
}

// ── G1: 대상 연차 속성 표기 ────────────────────────────────────────────────
// 시간차 단위(SYS025 02:2시간 / 03:1시간 / 04:30분)만 시각 구간을 보유한다.
//   반차(01)·반반차(05)는 신청 시 시각을 기록하지 않아 startTime/endTime 이 NULL — 구간 행을 숨긴다.
const HOURLY_UNITS = ['02', '03', '04']
const isHourlyUnit = (unitCode) => HOURLY_UNITS.includes(unitCode)

// "1230" → "12:30"
const fmtHm = (hhmm) => {
  const s = String(hhmm ?? '')
  return s.length >= 4 ? `${s.slice(0, 2)}:${s.slice(2, 4)}` : ''
}
const hhmmToMin = (hhmm) => {
  const s = String(hhmm ?? '')
  if (s.length !== 4) return null
  const h = parseInt(s.slice(0, 2), 10)
  const m = parseInt(s.slice(2, 4), 10)
  if (Number.isNaN(h) || Number.isNaN(m)) return null
  if (h < 0 || h > 23 || m < 0 || m > 59) return null
  return h * 60 + m
}

// 사용 단위 라벨 — 시간차면 '시간차 ' 접두(웹 AttdDayDetailPop 표기 관례와 동형).
const unitLabel = (req) => {
  if (!req?.unitNm) return ''
  return isHourlyUnit(req.useUnitType) ? `시간차 ${req.unitNm}` : req.unitNm
}

// 사용 구간 — "10:00~12:00 (2시간)". 차감 분(원본)이 있으면 그 값을, 없으면 구간에서 산출.
const timeRange = (req) => {
  if (!isHourlyUnit(req?.useUnitType) || !req?.startTime || !req?.endTime) return ''
  const range = `${fmtHm(req.startTime)}~${fmtHm(req.endTime)}`
  const raw = Number(req.leaveMinutes)
  if (Number.isFinite(raw) && raw > 0) return `${range} (${formatMinutesToHm(raw)})`
  const s = hhmmToMin(req.startTime)
  const e = hhmmToMin(req.endTime)
  if (s == null || e == null || e <= s) return range
  return `${range} (${formatMinutesToHm(e - s)})`
}

// 차감 일수 — 개인 분모(convMinutes)가 응답에 없으므로 "N일 H시간" 조립을 하지 않는다
//   (480 폴백 오표기 방지). 단순 일수 표기 + 시간차는 원본 분을 병기.
const leaveDaysLabel = (req) => {
  const v = req?.leaveDays
  if (v === null || v === undefined || v === '') return ''
  const n = Number(v)
  if (!Number.isFinite(n)) return ''
  const base = `${trimRawDays(n)}일`
  const raw = Number(req.leaveMinutes)
  if (isHourlyUnit(req.useUnitType) && Number.isFinite(raw) && raw > 0) {
    return `${base} (${formatMinutesToHm(raw)})`
  }
  return base
}

const startReject = (req) => {
  rejectingId.value = req.changeReqId
  rejectReason.value = ''
}
const cancelReject = () => {
  rejectingId.value = ''
  rejectReason.value = ''
}

// 동의 → 부모가 respond(AGREE) 호출 후 목록 재조회.
const onAgree = (req) => {
  if (props.submitting) return
  emit('agree', req.changeReqId)
}

// 거부 → 사유 필수. 부모가 respond(REJECT) 호출 후 목록 재조회.
const onReject = (req) => {
  if (props.submitting) return
  const reason = rejectReason.value.trim()
  if (!reason) return
  emit('reject', { changeReqId: req.changeReqId, reason })
  cancelReject()
}

// 나중에/닫기 → 팝업만 닫음(요청은 그대로 보존, 배너로 재진입 가능).
const onClose = () => {
  cancelReject()
  emit('update:open', false)
  emit('closed')
}
</script>

<style scoped>
/*
 * 디자인 토큰 — 팝업은 오버레이로 렌더되어 상속이 끊기므로 루트에 1회 재선언
 * (LeavePromotionLoginPopup / NoticeLoginPopup 과 동일 관례).
 */
.lcc-popup {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
  --radius-lg: 14px;

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

.lcc-popup__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.lcc-popup__card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 360px;
  max-height: 76vh;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  color: var(--color-text-primary);
}

.lcc-popup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 8px;
  flex-shrink: 0;
}
.lcc-popup__heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.lcc-popup__close {
  width: 28px;
  height: 28px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 16px;
  cursor: pointer;
}

.lcc-popup__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 16px 12px;
  border-top: 0.5px solid var(--color-border-light);
}
.lcc-popup__lead {
  margin: 4px 0 12px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-primary);
}
.lcc-popup__num {
  color: var(--color-primary);
  font-weight: 700;
}

.lcc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.lcc-card {
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
/* 변경 전 → 후 비교 블록 — 두 날짜를 화살표로 잇고 강조해 한눈에 들어오게 한다. */
.lcc-card__transition {
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 10px 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.lcc-trans__item {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 0;
}
.lcc-trans__cap {
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.lcc-trans__date {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  word-break: keep-all;
}
.lcc-trans__date.is-delete {
  color: var(--color-danger);
}
.lcc-trans__arrow {
  align-self: center;
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.lcc-card__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}
.lcc-card__label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}
.lcc-card__value {
  color: var(--color-text-primary);
  text-align: right;
  word-break: break-all;
}

.lcc-reject-reason {
  width: 100%;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 8px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}

.lcc-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
}
.lcc-btn {
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  border: 0.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-primary);
  white-space: nowrap;
}
.lcc-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.lcc-btn--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.lcc-btn--danger {
  background: var(--color-danger);
  border-color: var(--color-danger);
  color: #fff;
}
.lcc-btn--ghost {
  background: var(--color-surface);
}

.lcc-popup__footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 0.5px solid var(--color-border-light);
}
.lcc-popup__btn {
  flex: 0 0 auto;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.lcc-popup__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.lcc-popup__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 0.5px solid var(--color-border);
}
</style>
