<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup a07pop-backdrop"
      @click.self="onClose"
    >
      <div class="a07pop-modal" role="dialog" aria-modal="true">
        <!-- ── HEADER ─────────────────────────────────────── -->
        <div class="a07pop-header">
          <div class="a07pop-uinfo">
            <div class="a07pop-uline1">
              {{ headerUser.name }}
              <span class="a07pop-track">{{ headerUser.track }}</span>
              <span class="a07pop-meta">{{ headerUser.deptRole }}</span>
            </div>
            <div class="a07pop-uline2">{{ headerDate }}</div>
          </div>
          <button class="a07pop-close" @click="onClose" aria-label="닫기">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
            >
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- ── BODY ───────────────────────────────────────── -->
        <div v-if="loading" class="a07pop-loading">조회 중…</div>
        <div v-else class="a07pop-body">
          <!-- ── LEFT PANE ───────────────────────────────── -->
          <div class="a07pop-pane left">
            <!-- 시간 정보 카드 -->
            <div class="time-card">
              <!-- 스케줄 (계획) -->
              <div class="time-row">
                <div class="time-lbl">근무계획</div>
                <div class="time-val">
                  <template v-if="cfg.timeCard.plan.segments">
                    <div class="seg-multi">
                      <div
                        v-for="(s, i) in cfg.timeCard.plan.segments"
                        :key="i"
                        class="seg-line"
                      >
                        <span class="seg-tag">{{ s.tag }}</span>
                        <span v-html="s.range"></span>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <span v-html="cfg.timeCard.plan.value"></span>
                    <span v-if="cfg.timeCard.plan.meta" class="time-meta">{{
                      cfg.timeCard.plan.meta
                    }}</span>
                  </template>
                </div>
              </div>
              <!-- 2026-08-17: 휴게시간(스케줄 설정값) — 근무계획과 함께 참고 표시.
                   시각 미설정(분만 설정)이면 "N분"으로 폴백, 휴게 없는 타입은 행 미렌더. -->
              <div v-if="cfg.timeCard.breakInfo" class="time-row">
                <div class="time-lbl">휴게</div>
                <div class="time-val">
                  <span>{{ cfg.timeCard.breakInfo }}</span>
                </div>
              </div>
              <!-- PRAFTA-FIXEDOT-3: 고정연장근무(소정과 분리된 별도 축).
                   구간 = 근무타입 설정값, 실적 = 실근태가 그 구간을 커버한 분(정책 ① — 커버분만).
                   "연장 미이행" 배지는 조퇴 판정/통계와 완전히 분리된 별도 표식이며(정책 ②),
                   연차 계열 사용일에는 서버가 발화시키지 않는다(정책 ③ 의무 면제).
                   고정연장이 없는 근무타입은 이 행 자체가 렌더되지 않는다(기존 팝업과 동일). -->
              <div v-if="cfg.timeCard.fixedOt" class="time-row">
                <div class="time-lbl">고정연장</div>
                <div class="time-val">
                  <span>{{ cfg.timeCard.fixedOt.range }}</span>
                  <span v-if="cfg.timeCard.fixedOt.act" class="time-meta">
                    실적 {{ cfg.timeCard.fixedOt.act }}
                  </span>
                  <span
                    v-if="cfg.timeCard.fixedOt.unmet"
                    class="fixedot-unmet"
                  >
                    연장 미이행
                  </span>
                </div>
              </div>
              <!-- 실제 출퇴근 -->
              <div class="time-row">
                <div class="time-lbl">실제 출퇴근</div>
                <div class="time-val">
                  <template v-if="cfg.timeCard.actual.segments">
                    <div class="seg-multi">
                      <div
                        v-for="(s, i) in cfg.timeCard.actual.segments"
                        :key="i"
                        class="seg-line"
                      >
                        <span class="seg-tag">{{ s.tag }}</span>
                        <span class="seg-line-body">
                          <span v-html="s.range"></span>
                          <button
                            v-if="s.outside"
                            type="button"
                            class="seg-tag seg-tag-outside"
                            :class="{
                              'is-active': gpsPanel.segIdx === s.segIdx,
                            }"
                            @click="fnToggleGps(s.segIdx, s.attdId)"
                          >
                            외근
                          </button>
                        </span>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <span class="seg-line-body">
                      <span
                        v-html="cfg.timeCard.actual.value"
                        :class="{ 'val-empty': cfg.timeCard.actual.empty }"
                      ></span>
                      <button
                        v-if="cfg.timeCard.actual.outside"
                        type="button"
                        class="seg-tag seg-tag-outside"
                        :class="{ 'is-active': gpsPanel.segIdx === 0 }"
                        @click="fnToggleGps(0, cfg.timeCard.actual.attdId)"
                      >
                        외근
                      </button>
                    </span>
                  </template>
                </div>
              </div>

              <!-- 외근 GPS 동선 패널 (외근 버튼 클릭 시 토글 노출) -->
              <div v-if="gpsPanel.segIdx !== null" class="gps-panel-row">
                <AttdGpsCoordPanel
                  :trail="gpsPanel.trail"
                  :loading="gpsPanel.loading"
                />
              </div>
              <!-- 비고 -->
              <div class="time-row">
                <div class="time-lbl">비고</div>
                <div class="time-val">
                  <span
                    :class="cfg.timeCard.note.cls"
                    v-html="cfg.timeCard.note.value"
                  ></span>
                  <span v-if="cfg.timeCard.note.tag" class="note-pending">{{
                    cfg.timeCard.note.tag
                  }}</span>
                  <span v-if="cfg.timeCard.note.refNote" class="note-ref">{{
                    cfg.timeCard.note.refNote
                  }}</span>
                </div>
              </div>
            </div>

            <!-- 좌측 보조 블록 -->
            <div v-if="cfg.emptyHint" class="empty-hint">
              <div v-if="cfg.emptyHint.icon" class="hint-icon">
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              </div>
              <div v-html="cfg.emptyHint.text"></div>
            </div>

            <div v-if="cfg.workingNotice" class="work-notice">
              <span class="dot"></span>
              <span>{{ cfg.workingNotice }}</span>
            </div>

            <!-- 앞뒤 근무일(D-1 / D+1) 근태 구간 (겹침가드 개선 2026-08-06).
                 이웃 근무일의 미마감 근태가 이 날짜의 등록·승인을 막는 원인일 때 여기서 특정한다.
                 0건이면 섹션 자체를 렌더하지 않는다(팝업 세로 공간 절약). -->
            <AttdNeighborDaySegments
              v-if="neighborSegments.length"
              :segments="neighborSegments"
              :loading="loading"
            />

            <!-- 근로자 요청 카드 리스트 (근태/연차 결재 요청 + 연차 변경 요청) -->
            <div v-if="reqSectionCount" class="req-section">
              <div class="req-section-head">
                <h3>근로자 요청</h3>
                <span class="req-count">({{ reqSectionCount }})</span>
              </div>
              <div class="req-card-list">
                <div
                  v-for="card in reqCards"
                  :key="card.reqId"
                  class="req-card"
                >
                  <div class="req-card-head">
                    <span class="req-badge">
                      <span class="dot"></span>
                      {{ card.reqStatusNm || "요청" }}
                    </span>
                    <span class="req-title">{{ card.reqTypeNm }}</span>
                  </div>
                  <div class="req-card-sub">{{ card.insertDate }} 신청</div>

                  <div
                    class="req-diff"
                    :class="{
                      'req-diff--sched': card.mode === 'sched',
                      'req-diff--leave': card.mode === 'leave',
                    }"
                  >
                    <!-- 스케줄 수정(10): 근무시간(스케줄) 기준 BEFORE/AFTER (PRAFTA-APP-007-WEB-7)
                         com-013 #2: 2구간 스케줄 값이 좁은 칼럼에서 줄바꿈되지 않도록
                         req-diff--sched 에서 BEFORE→화살표→AFTER 를 세로 풀폭 스택으로 전환(CSS). -->
                    <template v-if="card.mode === 'sched'">
                      <div class="req-diff-col">
                        <div class="req-diff-head">BEFORE</div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">스케줄</span>
                          <span class="req-diff-val">{{ card.befSched }}</span>
                        </div>
                      </div>
                      <div class="req-diff-arrow">→</div>
                      <div class="req-diff-col">
                        <div class="req-diff-head">AFTER</div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">스케줄</span>
                          <span
                            class="req-diff-val"
                            :class="{ 'is-changed': card.schedChanged }"
                            >{{ card.aftSched }}</span
                          >
                        </div>
                      </div>
                    </template>
                    <!-- 연차(05/06): 사용단위·(시간차 범위)·차감일수 전용 1줄 표시 (PRAFTA-APP-018-D) -->
                    <template v-else-if="card.mode === 'leave'">
                      <div class="req-leave-line">
                        <span class="req-leave-seg">{{ card.reqTypeNm }}</span>
                        <span class="req-leave-seg">{{
                          card.leaveTypeLabel
                        }}</span>
                        <span v-if="card.timeRange" class="req-leave-seg">{{
                          card.timeRange
                        }}</span>
                        <span
                          v-if="card.leaveDaysLabel"
                          class="req-leave-seg"
                          >{{ card.leaveDaysLabel }}</span
                        >
                        <!-- 가불표시-06: 가불(미래 연차 당겨쓰기) 포함 요청 배지 — borrowDays > 0 일 때만 -->
                        <span
                          v-if="card.borrowDays > 0"
                          class="req-leave-seg req-leave-seg--borrow"
                          >가불 {{ card.borrowDays }}일</span
                        >
                      </div>
                    </template>
                    <!-- 그 외(01~04): 기존 출퇴근 시각 BEFORE/AFTER (현행 유지) -->
                    <template v-else>
                      <div class="req-diff-col">
                        <div class="req-diff-head">BEFORE</div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">출근</span>
                          <span class="req-diff-val">{{ card.befIn }}</span>
                        </div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">퇴근</span>
                          <span class="req-diff-val">{{ card.befOut }}</span>
                        </div>
                      </div>
                      <div class="req-diff-arrow">→</div>
                      <div class="req-diff-col">
                        <div class="req-diff-head">AFTER</div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">출근</span>
                          <span
                            class="req-diff-val"
                            :class="{
                              'is-changed': card.befIn !== card.aftIn,
                            }"
                            >{{ card.aftIn }}</span
                          >
                        </div>
                        <div class="req-diff-row">
                          <span class="req-diff-lbl">퇴근</span>
                          <span
                            class="req-diff-val"
                            :class="{
                              'is-changed': card.befOut !== card.aftOut,
                            }"
                            >{{ card.aftOut }}</span
                          >
                        </div>
                      </div>
                    </template>
                  </div>

                  <div v-if="card.reqReason" class="req-reason-row">
                    <span class="req-reason-lbl">사유</span>
                    <button
                      type="button"
                      class="hist-reason-btn"
                      @click="openReasonPopup(card.reqReason)"
                    >
                      보기
                    </button>
                  </div>

                  <!-- 결재자만 처리 가능(연차 05/06). 비결재자에게는 요청 내용만 보이고 버튼은 감춘다 —
                       마감을 막는 항목을 화면에서 추적할 수 있게 하되 권한은 넘기지 않는다. -->
                  <div v-if="card.canProcess" class="req-card-actions">
                    <button
                      type="button"
                      class="req-btn req-btn-approve"
                      :disabled="isMonthClosed"
                      @click="fnApproveReq(card)"
                    >
                      승인
                    </button>
                    <button
                      type="button"
                      class="req-btn req-btn-reject"
                      :disabled="isMonthClosed"
                      @click="fnRejectReq(card)"
                    >
                      반려
                    </button>
                  </div>
                  <div v-else class="lc-wait-hint">
                    내 결재 단계가 아닙니다. 결재 담당자만 승인/반려할 수
                    있습니다.
                  </div>
                </div>

                <!-- 연차 변경(이동/삭제) 요청 카드.
                     출처가 TB_LEAVE_CHANGE_REQUEST 라 결재 EP 가 다르다(attd13 confirm/reject).
                     BEFORE/AFTER 칸을 그대로 써서 좌=현재 연차일 / 우=이동 대상일(삭제면 "삭제")을 보여준다
                     (종전 연차 카드가 그리드 첫 트랙만 채워 반쪽으로 보였던 공간을 활용). -->
                <div
                  v-for="card in leaveChangeCards"
                  :key="card.key"
                  class="req-card"
                >
                  <div class="req-card-head">
                    <span
                      class="req-badge"
                      :class="{ 'req-badge--wait': !card.actionable }"
                    >
                      <span class="dot"></span>
                      {{ card.reqStatusNm }}
                    </span>
                    <span class="req-title">{{ card.reqTypeNm }}</span>
                  </div>
                  <div class="req-card-sub">
                    {{ card.initiatorNm }} · {{ card.insertDate }} 신청 ·
                    {{ card.sideHint }}
                  </div>

                  <div class="req-diff">
                    <div class="req-diff-col">
                      <div class="req-diff-head">현재 연차일</div>
                      <div class="req-diff-row">
                        <span class="req-diff-val">{{
                          card.fromDateLabel
                        }}</span>
                      </div>
                    </div>
                    <div class="req-diff-arrow">→</div>
                    <div class="req-diff-col">
                      <div class="req-diff-head">
                        {{ card.reqType === "MOVE" ? "이동 대상일" : "처리" }}
                      </div>
                      <div class="req-diff-row">
                        <span class="req-diff-val is-changed">{{
                          card.toDateLabel
                        }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="req-leave-line lc-detail-line">
                    <span class="req-leave-seg">{{ card.leaveNm }}</span>
                    <span class="req-leave-seg">{{ card.unitLabel }}</span>
                    <span v-if="card.timeRange" class="req-leave-seg">{{
                      card.timeRange
                    }}</span>
                    <span v-if="card.leaveDaysLabel" class="req-leave-seg">{{
                      card.leaveDaysLabel
                    }}</span>
                  </div>

                  <div v-if="card.reqReason" class="req-reason-row">
                    <span class="req-reason-lbl">사유</span>
                    <button
                      type="button"
                      class="hist-reason-btn"
                      @click="openReasonPopup(card.reqReason)"
                    >
                      보기
                    </button>
                  </div>

                  <!-- AGREED(관리자 확인 대기)만 처리 가능.
                       REQUESTED 는 관리자 발의 후 근로자 응답을 기다리는 단계라 버튼을 내지 않는다. -->
                  <div v-if="card.actionable" class="req-card-actions">
                    <button
                      type="button"
                      class="req-btn req-btn-approve"
                      :disabled="isMonthClosed || leaveChangeBusy"
                      @click="fnApproveLeaveChange(card)"
                    >
                      승인
                    </button>
                    <button
                      type="button"
                      class="req-btn req-btn-reject"
                      :disabled="isMonthClosed || leaveChangeBusy"
                      @click="fnRejectLeaveChange(card)"
                    >
                      반려
                    </button>
                  </div>
                  <div v-else class="lc-wait-hint">
                    근로자가 동의하면 확인할 수 있습니다.
                  </div>
                </div>
              </div>
            </div>

            <!-- PRAFTA-APP-018-F: 확정 연차 사용 섹션 (자동확정/직접 포함, 요청 카드와 별개 · 표시 전용) -->
            <div v-if="confirmedLeaveCards.length" class="leave-use-section">
              <div class="req-section-head">
                <h3>연차 사용</h3>
                <span class="req-count"
                  >({{ confirmedLeaveCards.length }})</span
                >
              </div>
              <div class="leave-use-list">
                <div
                  v-for="card in confirmedLeaveCards"
                  :key="card.key"
                  class="req-leave-line"
                >
                  <span class="req-leave-seg">{{ card.leaveNm }}</span>
                  <span class="req-leave-seg">{{ card.unitLabel }}</span>
                  <span v-if="card.timeRange" class="req-leave-seg">{{
                    card.timeRange
                  }}</span>
                  <span v-if="card.leaveDaysLabel" class="req-leave-seg">{{
                    card.leaveDaysLabel
                  }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- ── RIGHT PANE ──────────────────────────────── -->
          <div class="a07pop-pane right">
            <!-- 직접 수정 패널 -->
            <div class="panel-card" :class="{ 'is-open': panelOpen }">
              <div class="panel-head" @click="panelOpen = !panelOpen">
                <h3>관리자 직접 수정</h3>
                <div class="panel-head-right panel-actions">
                  <button
                    v-if="cfg.panel.kind === 'segments'"
                    type="button"
                    class="btn-reset-all"
                    @click.stop="fnResetForm"
                  >
                    초기화
                  </button>
                  <button
                    v-if="cfg.panel.kind === 'segments'"
                    type="button"
                    class="btn-clear-all"
                    @click.stop="openDeletePopup('all', null)"
                  >
                    전체삭제
                  </button>
                  <span v-if="cfg.panel.lockPill" class="lock-pill">
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    {{ cfg.panel.lockPill }}
                  </span>
                  <span v-else-if="cfg.panel.readonlyPill" class="lock-pill">
                    <svg
                      v-if="cfg.panel.readonlyPill === '미래 일자'"
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <circle cx="12" cy="12" r="10" />
                      <polyline points="12 6 12 12 16 14" />
                    </svg>
                    <svg
                      v-else
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    {{ cfg.panel.readonlyPill }}
                  </span>
                  <svg
                    class="chev"
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </div>
              </div>

              <!-- 일반 / 잠금 / 구간 입력 모드 -->
              <div v-if="cfg.panel.kind === 'segments'" class="panel-body">
                <!-- 구간 리스트 — 길어지면 자체 스크롤. 사유/저장 영역은 panel-body
                     안의 별도 블록으로 두어 항상 노출된다. -->
                <div class="seg-list-scroll">
                  <!-- 구간 입력 -->
                  <div
                    v-for="(seg, i) in form.segments"
                    :key="i"
                    class="seg-section"
                  >
                    <div class="seg-section-head">
                      <div class="seg-title-row">
                        <span class="seg-tag-lg">{{ i + 1 }}구간</span>
                        <span
                          v-if="segSummary(seg)"
                          class="seg-summary"
                          v-html="segSummary(seg)"
                        ></span>
                      </div>
                      <div class="seg-section-head-actions">
                        <button
                          class="seg-delete"
                          type="button"
                          aria-label="구간 삭제"
                          @click="openDeletePopup('segment', i)"
                        >
                          <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <polyline points="3 6 5 6 21 6" />
                            <path
                              d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"
                            />
                            <path d="M10 11v6" />
                            <path d="M14 11v6" />
                            <path d="M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2" />
                          </svg>
                        </button>
                      </div>
                    </div>

                    <!-- 정규근무 한 줄 (출근/퇴근). PRAFTA-003-7: 내부 변수명을 DB
                         컬럼명(START_*/END_*)과 정렬해 startDate/startTime/endDate/endTime 로 통일. -->
                    <div class="reg-row">
                      <div class="time-input-group">
                        <span class="lab">출근</span>
                        <CalendarSrch
                          v-model="seg.startDate"
                          class="seg-date"
                        />
                        <input
                          type="text"
                          inputmode="numeric"
                          class="input seg-time"
                          :value="displayTime(seg.startTime, `${i}-in`)"
                          :maxlength="focusedTime === `${i}-in` ? 4 : 5"
                          placeholder="HHMM"
                          @focus="onTimeFocus(`${i}-in`)"
                          @blur="onTimeBlur"
                          @input="onTimeInput($event, seg, 'startTime')"
                        />
                      </div>
                      <div class="time-input-group">
                        <span class="lab">퇴근</span>
                        <CalendarSrch v-model="seg.endDate" class="seg-date" />
                        <input
                          type="text"
                          inputmode="numeric"
                          class="input seg-time"
                          :value="displayTime(seg.endTime, `${i}-out`)"
                          :maxlength="focusedTime === `${i}-out` ? 4 : 5"
                          placeholder="HHMM"
                          @focus="onTimeFocus(`${i}-out`)"
                          @blur="onTimeBlur"
                          @input="onTimeInput($event, seg, 'endTime')"
                        />
                      </div>
                    </div>
                    <div v-if="isOverday(seg)" class="overday-hint">
                      출퇴근 날짜가 상이합니다. 익일 퇴근, Overnight 근무로
                      처리됩니다.
                    </div>

                    <!-- 초과근무 블록 -->
                    <div class="ot-block">
                      <div class="ot-block-head">
                        <span class="ot-block-title">초과근무</span>
                        <!-- 일용직은 초과근무를 등록할 수 없다(서버도 fail-closed 차단). -->
                        <div
                          v-if="isDailyWorker"
                          class="ot-allowed-hint is-empty"
                        >
                          일용직 근로자는 초과근무를 등록할 수 없습니다.
                        </div>
                        <template v-else-if="isSegmentFromDb(i)">
                          <div
                            v-if="otAllowedWindowsForSeg(i).length"
                            class="ot-allowed-hint"
                            aria-label="초과근무 등록 가능 범위"
                          >
                            <span class="ot-allowed-lbl">등록 가능</span>
                            <ul class="ot-allowed-list">
                              <li
                                v-for="(w, wi) in otAllowedWindowsForSeg(i)"
                                :key="wi"
                              >
                                <button
                                  type="button"
                                  class="ot-allowed-item"
                                  :title="`클릭하면 ${w.startLabel} ~ ${w.endLabel} 범위로 초과근무가 추가됩니다`"
                                  @click="addOtFromWindow(i, w)"
                                >
                                  {{ w.startLabel }} ~ {{ w.endLabel }}
                                </button>
                              </li>
                            </ul>
                          </div>
                          <div v-else class="ot-allowed-hint is-empty">
                            등록 가능한 초과근무 범위가 없습니다.
                          </div>
                        </template>
                        <div v-else class="ot-allowed-hint is-empty">
                          저장되지 않은 신규 구간은 초과근무를 등록할 수
                          없습니다.
                        </div>
                      </div>
                      <ul
                        v-if="
                          !isDailyWorker &&
                          isSegmentFromDb(i) &&
                          seg.otList &&
                          seg.otList.length
                        "
                        class="ot-list"
                      >
                        <li
                          v-for="(ot, oi) in seg.otList"
                          :key="oi"
                          class="ot-row"
                        >
                          <label class="ot-check" aria-label="선택">
                            <input type="checkbox" v-model="ot.checked" />
                          </label>
                          <div class="time-input-group">
                            <span class="lab">시작</span>
                            <CalendarSrch
                              v-model="ot.startDate"
                              class="ot-date"
                            />
                            <input
                              type="text"
                              inputmode="numeric"
                              class="input ot-time"
                              :value="
                                displayTime(ot.startTime, `${i}-ot${oi}-start`)
                              "
                              :maxlength="
                                focusedTime === `${i}-ot${oi}-start` ? 4 : 5
                              "
                              placeholder="HHMM"
                              @focus="onTimeFocus(`${i}-ot${oi}-start`)"
                              @blur="onTimeBlur"
                              @input="onTimeInput($event, ot, 'startTime')"
                            />
                          </div>
                          <div class="time-input-group">
                            <span class="lab">종료</span>
                            <CalendarSrch
                              v-model="ot.endDate"
                              class="ot-date"
                            />
                            <input
                              type="text"
                              inputmode="numeric"
                              class="input ot-time"
                              :value="
                                displayTime(ot.endTime, `${i}-ot${oi}-end`)
                              "
                              :maxlength="
                                focusedTime === `${i}-ot${oi}-end` ? 4 : 5
                              "
                              placeholder="HHMM"
                              @focus="onTimeFocus(`${i}-ot${oi}-end`)"
                              @blur="onTimeBlur"
                              @input="onTimeInput($event, ot, 'endTime')"
                            />
                          </div>
                        </li>
                      </ul>
                      <button
                        v-if="!isDailyWorker && isSegmentFromDb(i)"
                        class="add-ot-btn"
                        type="button"
                        @click="addOt(i)"
                      >
                        + {{ i + 1 }}구간 초과근무 추가
                      </button>
                      <!-- 소정-07: 단축근무자(육아기·가족돌봄) 연장근로 명시 청구 확인.
                           관리자 직접 등록(reqId 없음) 경로에서만 서버가 이 값을 요구한다.
                           2026-08-17: 일자상세 응답에 단축 기간 여부(reducedWorkYn)가 추가되어
                           단축 대상 근로자('Y')에게만 노출한다 — 일반 근로자 팝업에서는 숨김(사용자 요청).
                           실제 판정·차단은 종전대로 서버 게이트가 수행한다(미체크 저장 시 ATTD_400_201).
                           근로자 신청 승인(요청 카드) 경로는 신청 시점에 확인된 사실이라 이 값을 쓰지 않는다. -->
                      <div
                        v-if="
                          !isDailyWorker &&
                          isSegmentFromDb(i) &&
                          hasAnyOt(i) &&
                          reducedWorkYn === 'Y'
                        "
                        class="ot-claim-row"
                      >
                        <label class="ot-claim-label">
                          <input
                            type="checkbox"
                            v-model="otWorkerClaimConfirmed"
                          />
                          <span
                            >근로자가 연장근로를 명시적으로 청구했음을
                            확인</span
                          >
                        </label>
                        <p class="ot-claim-help">
                          육아기·가족돌봄 근로시간 단축 기간의 근로자는 본인이
                          청구한 경우에만 연장근로를 등록할 수 있습니다(임신기
                          단축은 등록 불가). 단축 기간이 아닌 근로자는 체크 여부와
                          무관합니다.
                        </p>
                      </div>
                      <div
                        v-if="
                          !isDailyWorker && isSegmentFromDb(i) && hasAnyOt(i)
                        "
                        class="ot-actions"
                      >
                        <button
                          type="button"
                          class="ot-save-btn"
                          :disabled="!isSegOtValid(i) || otSaving"
                          @click="fnApproveOvertime(i)"
                        >
                          <span v-if="!otSaving">초과근무 저장</span>
                          <span v-else>저장 중…</span>
                        </button>
                        <!-- com-016-E: "초과근무 삭제" — 체크된 행 삭제(기저장행=서버 soft-delete, 신규행=로컬 제거). -->
                        <button
                          type="button"
                          class="ot-delete-btn"
                          :disabled="
                            !hasCheckedDeletable(i) || otSaving || isMonthClosed
                          "
                          @click="fnDeleteOvertime(i)"
                        >
                          초과근무 삭제
                        </button>
                        <!-- com-013 #6b: OT '반려' 버튼 제거(관리자 직접수정 블록엔 결재 대상 요청이 흘러오지 않음).
                             요청 반려 인프라(onRejectConfirm overtime 분기 / reject-user-overtime EP)는 Attd_10 인박스 공용이라 보존. -->
                      </div>
                    </div>
                  </div>
                  <button
                    v-if="form.segments.length < MAX_SEGMENTS"
                    class="seg-add-btn"
                    type="button"
                    @click="addSegment"
                  >
                    + 구간 추가
                  </button>
                </div>
                <div class="reason-section">
                  <div class="form-row is-textarea">
                    <label class="required">사유</label>
                    <textarea
                      class="input"
                      v-model="form.reason"
                      placeholder="사유를 입력해 주세요. (최대 100자)"
                      maxlength="100"
                    ></textarea>
                  </div>
                  <button
                    class="save-btn"
                    :disabled="!canSave || isMonthClosed"
                    @click="fnSave"
                  >
                    저장
                  </button>
                </div>
              </div>

              <!-- 읽기 전용 (A-13 / A-14 / A-16) -->
              <div v-else class="panel-readonly-body">
                <div class="ro-icon">
                  <svg
                    v-if="cfg.panel.kind === 'readonly-leave'"
                    width="32"
                    height="32"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" />
                    <path d="M12 7v5l3 3" />
                  </svg>
                  <svg
                    v-else-if="cfg.panel.kind === 'readonly-future'"
                    width="32"
                    height="32"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                    <circle cx="12" cy="16" r="2" />
                  </svg>
                  <svg
                    v-else
                    width="32"
                    height="32"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                </div>
                <div v-html="cfg.panel.message"></div>
                <button
                  v-if="cfg.panel.extLink"
                  class="ext-link"
                  type="button"
                  @click="onExtLink"
                >
                  {{ cfg.panel.extLink }}
                  <svg
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.4"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M7 17L17 7M7 7h10v10" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- 처리 이력 — 버튼 클릭 시 별도 팝업으로 표시 -->
            <button
              type="button"
              class="history-toggle-btn"
              @click="openHistoryPopup"
            >
              <span class="history-toggle-label">
                처리 이력
                <span class="count">({{ cfg.history.length }})</span>
              </span>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M9 18l6-6-6-6" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- 삭제 사유 입력 팝업 (body로 Teleport 해서 부모 모달 stacking context 회피) -->
  <!-- prafta-nested-modal-overlay 클래스: body.prafta-modal-open의 pointer-events:none 상속을 우회 -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="deletePopup.open"
        class="del-pop-backdrop prafta-nested-modal-overlay"
        @click.self="closeDeletePopup"
      >
        <div class="del-pop" @click.stop>
          <div class="del-pop-head">
            <h3>
              {{ deletePopup.type === "all" ? "전체 삭제" : "구간 삭제" }}
            </h3>
            <button
              class="del-pop-close"
              type="button"
              @click="closeDeletePopup"
            >
              ×
            </button>
          </div>
          <div class="del-pop-body">
            <div class="del-pop-msg">
              {{
                deletePopup.type === "all"
                  ? "해당 일자의 근태 데이터를 모두 삭제합니다. 삭제 사유를 입력해 주세요."
                  : `${(deletePopup.segIdx ?? 0) + 1}구간 근태 데이터를 삭제합니다. 삭제 사유를 입력해 주세요.`
              }}
            </div>
            <label class="del-pop-label">사유 <span class="req">*</span></label>
            <textarea
              class="input del-pop-textarea"
              v-model="deletePopup.reason"
              placeholder="삭제 사유를 입력해 주세요. (최대 100자)"
              maxlength="100"
            ></textarea>
          </div>
          <div class="del-pop-foot">
            <!-- F-10 규약: 왼쪽=진행/확정(삭제 실행, danger), 오른쪽=이탈(취소) -->
            <button
              class="btn-danger"
              type="button"
              :disabled="!deletePopup.reason.trim()"
              @click="fnDelete"
            >
              삭제
            </button>
            <button class="btn-cancel" type="button" @click="closeDeletePopup">
              취소
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 처리 이력 사유 보기 팝업 (읽기 전용) -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="reasonPopup.open"
        class="del-pop-backdrop reason-pop-backdrop prafta-nested-modal-overlay"
        @click.self="closeReasonPopup"
      >
        <div class="del-pop" @click.stop>
          <div class="del-pop-head">
            <h3>처리 사유</h3>
            <button
              class="del-pop-close"
              type="button"
              @click="closeReasonPopup"
            >
              ×
            </button>
          </div>
          <div class="del-pop-body">
            <div class="reason-view">{{ reasonPopup.reason || "-" }}</div>
          </div>
          <div class="del-pop-foot">
            <button class="btn-cancel" type="button" @click="closeReasonPopup">
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 처리 이력 팝업 (별도 모달) -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="historyPopup.open"
        class="hist-pop-backdrop prafta-nested-modal-overlay"
        @click.self="closeHistoryPopup"
      >
        <div class="hist-pop" @click.stop>
          <div class="hist-pop-head">
            <h3>
              처리 이력
              <span class="count">({{ cfg.history.length }})</span>
            </h3>
            <button
              class="del-pop-close"
              type="button"
              @click="closeHistoryPopup"
            >
              ×
            </button>
          </div>
          <div class="hist-pop-body">
            <div v-if="cfg.history.length" class="hist-table-wrap">
              <table class="hist-table">
                <colgroup>
                  <col style="width: 160px" />
                  <col style="width: 70px" />
                  <col style="width: 130px" />
                  <col style="width: 130px" />
                  <col style="width: 130px" />
                  <col style="width: 130px" />
                  <col style="width: 90px" />
                  <col style="width: 70px" />
                  <col style="width: 90px" />
                  <col style="width: 160px" />
                </colgroup>
                <thead>
                  <tr>
                    <th rowspan="2">이력 유형</th>
                    <th rowspan="2">구간</th>
                    <th colspan="2">변경 전</th>
                    <th colspan="2">변경 후</th>
                    <!-- com-013 #4: 근로자 요청사유(관리자 사유와 별개). -->
                    <th rowspan="2">근로자 사유</th>
                    <th rowspan="2">사유</th>
                    <th rowspan="2">수정자</th>
                    <th rowspan="2">수정일시</th>
                  </tr>
                  <tr>
                    <th>출근</th>
                    <th>퇴근</th>
                    <th>출근</th>
                    <th>퇴근</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(h, i) in cfg.history" :key="i">
                    <td class="cell-type">{{ h.histTypeNm }}</td>
                    <td>{{ h.workSeq }}</td>
                    <!-- 스케줄 수정 승인 이력: 변경 전/후 스케줄 라벨을 각 컬럼에 colspan 으로 노출 (PRAFTA-APP-007-WEB-7) -->
                    <template v-if="h.isSchedApprove">
                      <td class="cell-time" colspan="2">
                        {{ h.befSchedLabel }}
                      </td>
                      <td class="cell-time" colspan="2">
                        {{ h.aftSchedLabel }}
                      </td>
                    </template>
                    <!-- 그 외(근태/OT/연차/스케줄 반려): 기존 출퇴근 시각 -->
                    <template v-else>
                      <td class="cell-time">{{ h.befCheckIn }}</td>
                      <td class="cell-time">{{ h.befCheckOut }}</td>
                      <td class="cell-time">{{ h.aftCheckIn }}</td>
                      <td class="cell-time">{{ h.aftCheckOut }}</td>
                    </template>
                    <!-- com-013 #4: 근로자 요청사유 — 값 있으면 보기 버튼, 없으면 "-". -->
                    <td>
                      <button
                        v-if="h.reqReason"
                        class="hist-reason-btn"
                        type="button"
                        @click="openReasonPopup(h.reqReason)"
                      >
                        보기
                      </button>
                      <span v-else>-</span>
                    </td>
                    <td>
                      <button
                        v-if="h.reason"
                        class="hist-reason-btn"
                        type="button"
                        @click="openReasonPopup(h.reason)"
                      >
                        보기
                      </button>
                      <span v-else>-</span>
                    </td>
                    <td>{{ h.insertNm }}</td>
                    <td class="cell-time">{{ h.insertDate }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="hist-empty">
              {{ cfg.historyEmpty || "등록된 처리 이력이 없습니다." }}
            </div>
          </div>
          <div class="del-pop-foot">
            <button class="btn-cancel" type="button" @click="closeHistoryPopup">
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 반려 사유 입력 모달 (근태 / 초과근무 / 연차 요청 반려 공용) -->
  <ReasonInputModal
    v-if="rejectModal.open"
    title="반려 사유 입력"
    message="반려 사유를 입력해 주세요. 반려 시 해당 요청은 처리할 수 없습니다."
    placeholder="반려 사유를 입력해 주세요. (최대 500자)"
    :max-length="500"
    :required="true"
    @confirm="onRejectConfirm"
    @cancel="closeRejectModal"
    @close="closeRejectModal"
  />
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  defineProps,
  defineEmits,
  defineOptions,
  getCurrentInstance,
} from "vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";
import AttdGpsCoordPanel from "@/views/attd/popup/AttdGpsCoordPanel.vue";
import AttdNeighborDaySegments from "@/views/attd/popup/AttdNeighborDaySegments.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatLeaveDays, formatLeaveMinutes } from "@/utils/leaveFormat";
import {
  formatYmdDot,
  formatMdDot,
  formatHm,
  formatDateTimeDot,
  formatDateTimeDotWithSec,
} from "@/utils/dateFormat";

defineOptions({ name: "AttdDayDetailPop" });

// 팝업은 식별자(siteCd/userCd/workYmd/nodeCd/attdId)만 받고,
// 나머지 상세/이력 데이터는 onMounted에서 API로 직접 조회한다.
// fallback_p는 응답에 스케줄(계획) 값이 비어있을 때 보충용으로만 사용된다.
const props = defineProps({
  attdId_p: { type: String, default: "" },
  siteCd_p: { type: String, default: "" },
  userCd_p: { type: String, default: "" },
  nodeCd_p: { type: String, default: "" },
  date_p: { type: String, default: "" }, // YYYY-MM-DD (UI/캘린더용)
  dow_p: { type: Number, default: -1 },
  // 응답의 plan* 값이 없을 때 보충용으로 사용되는 스케줄 값.
  // { plan1Start, plan1End, plan2Start, plan2End }
  fallback_p: { type: Object, default: () => ({}) },
  // PRAFTA-028 - 해당 월 마감 여부 (true 면 모든 쓰기 차단, 조회는 허용)
  isMonthClosed_p: { type: Boolean, default: false },
});

const emit = defineEmits(["close", "saved"]);
const { proxy } = getCurrentInstance();

// PRAFTA-028 - 마감된 월에는 근태/OT 생성·수정·삭제·승인·반려 등 모든 쓰기를 차단한다(조회는 허용).
const isMonthClosed = computed(() => props.isMonthClosed_p);
const guardClosed = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 마감 해제 후 수정할 수 있습니다.");
    return true;
  }
  return false;
};

// 팝업 종료 시 Attd_07 테이블을 최신 데이터로 reload 한다.
// (saved 이벤트 → Attd_07.vue 의 onSaved 핸들러(fnSearch) 호출)
const onClose = () => {
  emit("saved");
  emit("close");
};
// 관리자 직접 수정 패널 접기/펼치기 — 기본 열림
const panelOpen = ref(true);

// 처리 이력 팝업 상태 (별도 모달로 분리)
const historyPopup = ref({ open: false });
const openHistoryPopup = () => {
  historyPopup.value.open = true;
};
const closeHistoryPopup = () => {
  historyPopup.value.open = false;
};

// 삭제 사유 입력 팝업 상태
//   type:   'all'      → 일자 전체 삭제
//           'segment'  → 특정 구간(workSeq) 삭제
//   segIdx: type='segment'일 때 구간 인덱스(0-based)
const deletePopup = ref({
  open: false,
  type: null,
  segIdx: null,
  reason: "",
});

// 처리 이력 — 사유(processReason) 보기 팝업 상태 (읽기 전용)
const reasonPopup = ref({ open: false, reason: "" });
const openReasonPopup = (reason) => {
  reasonPopup.value = { open: true, reason: reason || "" };
};
const closeReasonPopup = () => {
  reasonPopup.value.open = false;
};

// 반려 사유 입력 모달 상태 (PRAFTA-009 part1).
//   kind: 'attd'     → 근태 요청 반려 (POST /attd07/reject-user-attd-requests)
//         'overtime' → 초과근무 요청 반려 (POST /attd07/reject-user-overtime-requests)
//   context: 반려 API 호출에 필요한 식별자 묶음 (kind 별로 키 구성 상이)
const rejectModal = ref({
  open: false,
  kind: null,
  context: null,
  busy: false,
});
const closeRejectModal = () => {
  if (rejectModal.value.busy) return;
  rejectModal.value = { open: false, kind: null, context: null, busy: false };
};

// API 결과 보관용 — 응답 전에는 비어 있음
const loading = ref(true);
const record = ref({}); // 일자 근태 상세 (plan/act/leave …)
const userInfo = ref({}); // 사용자 정보 (헤더용)
const historyList = ref([]); // 처리 이력
const reqList = ref([]); // 근로자 요청 (monthlyAttdReqResultList)
// PRAFTA-APP-018-F: 그날 확정 연차 사용내역 (confirmedLeaveResultList). 표시 전용.
const confirmedLeaves = ref([]);
// 연차 변경(이동/삭제) 활성 요청 (leaveChangeReqResultList).
//   TB_LEAVE_CHANGE_REQUEST 출처 — 근태 요청(TB_USER_ATTD_REQ)과 별 테이블이라 종전 이 팝업에서
//   누락돼 있었다. 출발일·이동대상일 양쪽 셀에서 동일 요청이 보인다.
const leaveChangeReqs = ref([]);
// PRAFTA-003-7: getDailyAttdDetails 응답의 dailyOvertimeResultList 원본 보관용.
//   initForm()에서 segments[*].otList 에 분배해서 프리필하는 데 사용한다.
const dailyOvertimeList = ref([]);
// PC-09(N8): 대상 사용자·대상일 기준 1일 환산시간(분) — daily-attd-details 응답 convMinutes.
//   개인 분모(480 캡, 백엔드가 미산출 시 480 폴백 보장). 연차 차감 "N일 H시간 M분" 조립 분모.
const convMinutes = ref(480);
// 겹침가드 개선(2026-08-06): 앞뒤 근무일(D-1 / D+1) 근태 구간 (neighborAttdSegmentList).
//   표시 문자열·상태(status)는 서버 완성값 — 프론트 재가공·재판정 금지.
//   미수신(구서버)이면 빈 배열 → 섹션 미노출(회귀 없음).
const neighborSegments = ref([]);

// OT 칩 정합(2026-08-08): 그날 확정 부분연차(반차/시간차) 면제 구간 (otLeaveExemptWindowList).
//   서버가 OT 저장 검증(ATTD_400_012)과 동일 산식으로 내려준다 — FE 는 재계산 없이 stamp 를 그대로
//   차집합에 쓴다(startStamp/endStamp 축 = buildActualSegments 와 동일, workYmd-1 00:00 원점).
//   미수신(구서버)이면 빈 배열 → 종전 계산(실근태−스케줄) 그대로(회귀 없음).
const otLeaveExemptWindows = ref([]);
// PRAFTA-FIXEDOT-2: 그날 스케줄의 고정연장(전방·후방) 점유 구간(서버 산출, stamp 동일 축).
//   OT 칩 피감수에 연차 면제와 동일하게 합친다 — 서버 검증(실근태 − (소정 ∪ 고정연장 ∪ 연차면제)) 정합.
const otFixedOtWindows = ref([]);

// "YYYY-MM-DD" → "YYYYMMDD"
const ymdDashToNum = (s) => (s || "").replace(/-/g, "");

const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];

// ── 시간 포맷 헬퍼 ────────────────────────────────────────
// "0930" → "09:30"
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};

// PRAFTA-APP-007-WEB-7: 스케줄 원시 시각(HHmm) → 표시 라벨.
//   1구간/2구간 자동 판별(2구간 시작·종료가 모두 있으면 2구간) + 구간 수 suffix.
//   입력은 tb_sch_mgmt 의 HHmm 4자리 문자열. 1구간 데이터(fst)도 없으면 "-".
//   앱 SchedModifyForm 라벨 포맷과 동형(웹/앱 프론트 분리 — 각자 보유, D13).
const schedLabel = (fstStr, fstEnd, secStr, secEnd) => {
  if (!fstStr && !fstEnd) return "-"; // 스케줄 데이터 부재(예: 삭제된 SCH_CD)
  const fst = `${fmtTime(fstStr)}~${fmtTime(fstEnd)}`;
  if (secStr && secEnd) {
    return `${fst} / ${fmtTime(secStr)}~${fmtTime(secEnd)} (2구간)`;
  }
  return `${fst} (1구간)`;
};

// PRAFTA-FIXEDOT-2(표기): 고정연장(전방·후방) 라벨 suffix — 없으면 빈 문자열(기존 라벨 불변).
//   스케줄 비교/이력 라벨 뒤에 붙여 소정과 시각적으로 구분한다(명칭 "고정연장" 통일 — 정책 ⑤).
const fixedOtLabel = (preStr, preEnd, rearStr, rearEnd) => {
  const parts = [];
  if (preStr && preEnd) parts.push(`${fmtTime(preStr)}~${fmtTime(preEnd)}`);
  if (rearStr && rearEnd) parts.push(`${fmtTime(rearStr)}~${fmtTime(rearEnd)}`);
  return parts.length ? ` + 고정연장 ${parts.join(" · ")}` : "";
};

// LC-09(§5-B): 연차 차감일수 표기 — 소수점 노출 금지, "N일 H시간 M분 차감" 조립.
//   PC-09(N8): 분모 = daily-attd-details 응답 convMinutes(대상 사용자·대상일 개인 분모).
//   (기존 480 고정 폴백 결함 D2 해소 — 미수신 시에만 ref 초기값 480 사용)
//   NaN/null → '' (카드에서 라벨 숨김 — 기존 normalizeDays 동작 유지).
const chargeDaysLabel = (v) => {
  if (v === null || v === undefined || v === "") return "";
  const n = Number(v);
  if (Number.isNaN(n)) return "";
  return `${formatLeaveDays(n, convMinutes.value)} 차감`;
};

// LC-09(§5-B): 시간차 행의 LEAVE_MINUTES 원본 병기 — "10:00~11:30 (1시간 30분)".
//   HHmm 범위에서 분을 산출(시간차 use 행은 항상 시각 보유. 기존 hhmmToMin 헬퍼 재사용 —
//   computed 내부에서 호출되므로 선언 순서 무관). 산출 불가 시 범위만 표시.
const hourlyRangeLabel = (startTime, endTime) => {
  const range = `${fmtTime(startTime)}~${fmtTime(endTime)}`;
  const s = hhmmToMin(startTime);
  const e = hhmmToMin(endTime);
  if (s == null || e == null || e <= s) return range;
  return `${range} (${formatLeaveMinutes(e - s)})`;
};

// "1230" → "12:30" (포커스 잃을 때 표시)
const fmtRaw = (raw) => {
  if (!raw) return "";
  if (raw.length === 4) return `${raw.slice(0, 2)}:${raw.slice(2)}`;
  return raw;
};

// 입력값 정규화 — 숫자만, 4자리 제한, 시/분 상한 (HH≤23, MM≤59 → 최댓값 2359)
const sanitizeTime = (v) => {
  let s = String(v || "")
    .replace(/\D/g, "")
    .slice(0, 4);
  if (s.length >= 2) {
    const hh = parseInt(s.slice(0, 2), 10);
    if (hh > 23) s = "23" + s.slice(2);
  }
  if (s.length === 4) {
    const mm = parseInt(s.slice(2, 4), 10);
    if (mm > 59) s = s.slice(0, 2) + "59";
  }
  return s;
};

// 날짜 비교 (오늘/미래/과거)
const dateInfo = computed(() => {
  if (!props.date_p) {
    return { isFuture: false, isToday: false, isPast: false };
  }
  const [y, m, d] = props.date_p.split("-").map(Number);
  const target = new Date(y, m - 1, d);
  target.setHours(0, 0, 0, 0);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return {
    isFuture: target > today,
    isToday: target.getTime() === today.getTime(),
    isPast: target < today,
  };
});

// 대상 근로자가 일용직(EMPLOYMENT_TYPE='DAILY')인지 여부.
//   일용직은 초과근무를 등록할 수 없으므로 OT 등록 UI 를 노출하지 않는다(서버도 fail-closed 차단).
const isDailyWorker = computed(
  () => (userInfo.value?.employmentType || "") === "DAILY"
);

// ── 헤더 ──────────────────────────────────────────────────
// 트랙 라벨은 고용형태에 따라 "일용"/"정규" 로 표시한다.
const headerUser = computed(() => {
  const u = userInfo.value || {};
  const role = u.authNm || u.authCd || u.role || "";
  return {
    name: u.userNm || u.name || "—",
    track: isDailyWorker.value ? "일용" : "정규",
    deptRole: role ? `· ${role}` : "",
  };
});

const headerDate = computed(() => {
  if (!props.date_p) return "";
  const [y, m, d] = props.date_p.split("-").map(Number);
  const dow = props.dow_p >= 0 ? props.dow_p : new Date(y, m - 1, d).getDay();
  return `${y}.${String(m).padStart(2, "0")}.${String(d).padStart(2, "0")} (${dowLabels[dow]})`;
});

// 빈 2구간 스케줄은 백엔드에서 NULL(→ 프론트에서 "")로 내려온다.
// "0000"은 자정(00:00)을 뜻하는 유효한 시간이므로 빈 값으로 취급하지 않는다.
// (예: 1구간 00:00~07:00 + 2구간 00:00~18:00 같은 심야 분할근무 스케줄)
const isMeaningfulTime = (t) => !!t;

// ── 셀 상태 분석 ──────────────────────────────────────────
const recordState = computed(() => {
  const r = record.value;
  const isOff = !r || (!r.plan1Start && !r.plan2Start);
  const isLeave = !!r?.leaveNm;
  // plan2가 의미 있는 시간일 때만 계획상 분할근무로 인정
  const hasPlan2 =
    isMeaningfulTime(r?.plan2Start) && isMeaningfulTime(r?.plan2End);
  // act2 실적이 하나라도 있으면 실제 분할근무
  const hasAct2 = !!(r?.act2InTime || r?.act2OutTime);
  // 둘 중 하나라도 있으면 2구간 영역 표시
  const hasSeg2 = hasPlan2 || hasAct2;

  // 누락 종류 — 계획이 있는 경우에만 누락 판정
  let missing = null;
  if (r && !isLeave && !isOff) {
    if (r.plan1Start && !r.act1InTime) missing = "in1";
    else if (r.plan1End && r.act1InTime && !r.act1OutTime) missing = "out1";
    else if (hasPlan2 && !r.act2InTime) missing = "in2";
    else if (hasPlan2 && r.act2InTime && !r.act2OutTime) missing = "out2";
  }

  // 진행중 (오늘이고 출근만 있고 퇴근 없음)
  const inProgress =
    dateInfo.value.isToday && r && r.act1InTime && !r.act1OutTime;

  return { isOff, isLeave, hasSeg2, hasPlan2, hasAct2, missing, inProgress };
});

// ── 시간 카드 빌더 ────────────────────────────────────────
// "20260502" → "05.02" (월-일 표시). dateFormat 단일 출처에 위임.
const fmtMd = (ymd) => {
  if (!ymd) return "";
  const s = String(ymd);
  if (s.length < 8) return "";
  return formatMdDot(s);
};

// 실제 출퇴근 — 날짜(상단 보조) / 시간(하단 강조)으로 분리 표시
const dtBlock = (ymd, hhmm) => {
  const d = fmtMd(ymd);
  const t = fmtTime(hhmm);
  return `<span class="dt-block"><span class="dt-date">${d}</span><span class="dt-time">${t}</span></span>`;
};
const actualRange = (inDate, inTime, outDate, outTime) => {
  if (!inTime && !outTime) return "기록 없음";
  const left = inTime
    ? dtBlock(inDate, inTime)
    : '<span class="val-missing">−</span>';
  const right = outTime
    ? dtBlock(outDate, outTime)
    : '<span class="val-missing">(미등록)</span>';
  // 실제 출퇴근은 구간 분리기호로 '~' 를 사용한다 (스케줄 계획은 '→' 유지)
  return `${left}<span class="dt-arrow">~</span>${right}`;
};

const planRange = (s, e) => {
  if (!s && !e) return "−";
  return `${fmtTime(s)} → ${fmtTime(e)}`;
};

const buildTimeCard = () => {
  const r = record.value;
  const { isOff, isLeave, hasSeg2, hasPlan2 } = recordState.value;

  // 레코드 자체가 없으면 표시할 데이터가 없으므로 휴무 placeholder 반환
  if (!r) {
    return {
      plan: { value: "휴무", meta: "정기 휴일", cls: "value-off" },
      actual: { value: "−", cls: "val-empty" },
      note: { value: "휴무", cls: "value-off" },
    };
  }

  // 계획 — 휴무(계획 없음)면 "휴무", plan2가 의미 있는 시간일 때만 2구간 표시
  // 정책 §7.5: 휴무일에도 출퇴근 등록은 허용되며 실적/비고는 그대로 표시한다
  // (해당 근무는 전량 초과근무로 취급).
  let plan;
  if (isOff) {
    plan = { value: "휴무", meta: "정기 휴일", cls: "value-off" };
  } else if (hasPlan2) {
    plan = {
      segments: [
        { tag: "1구간", range: planRange(r.plan1Start, r.plan1End) },
        { tag: "2구간", range: planRange(r.plan2Start, r.plan2End) },
      ],
    };
  } else {
    plan = { value: planRange(r.plan1Start, r.plan1End), meta: "1구간" };
  }

  // 실적
  let actual;
  if (isLeave) {
    actual = { value: "출근 차단됨", cls: "val-empty" };
  } else if (hasSeg2) {
    actual = {
      segments: [
        {
          tag: "1구간",
          segIdx: 0,
          // PRAFTA-009 part2: 구간별 외근 여부/ATTD_ID (외근 버튼 노출·GPS 조회용).
          outside: r.attd1OutsideYn === "Y",
          attdId: r.attd1Id || "",
          range: actualRange(
            r.act1InDate,
            r.act1InTime,
            r.act1OutDate,
            r.act1OutTime
          ),
        },
        {
          tag: "2구간",
          segIdx: 1,
          outside: r.attd2OutsideYn === "Y",
          attdId: r.attd2Id || "",
          range: actualRange(
            r.act2InDate,
            r.act2InTime,
            r.act2OutDate,
            r.act2OutTime
          ),
        },
      ],
    };
  } else {
    actual = {
      value: actualRange(
        r.act1InDate,
        r.act1InTime,
        r.act1OutDate,
        r.act1OutTime
      ),
      // PRAFTA-009 part2: 단일 구간(1구간만) 케이스의 외근 여부/ATTD_ID.
      outside: r.attd1OutsideYn === "Y",
      attdId: r.attd1Id || "",
    };
  }

  // 초과근무 집계 — 우측 패널의 form.segments[].otList 기반.
  //   비고 영역의 "초과근무 N건 인정 (Xh Ym)" 표시에만 사용한다.
  const segs = form.value.segments || [];
  const otCount = segs.reduce(
    (sum, seg) =>
      sum +
      (seg.otList || []).filter(
        (o) =>
          minutesOfRange(o.startDate, o.startTime, o.endDate, o.endTime) > 0
      ).length,
    0
  );
  const otTotal = segs.reduce(
    (sum, seg) =>
      sum +
      (seg.otList || []).reduce(
        (s, o) =>
          s + minutesOfRange(o.startDate, o.startTime, o.endDate, o.endTime),
        0
      ),
    0
  );

  // 비고
  let note;
  if (isLeave) {
    note = { value: `휴가 (${r.leaveNm})`, cls: "value-leave" };
  } else if (
    recordState.value.missing === "in1" ||
    recordState.value.missing === "in2"
  ) {
    note = { value: "출근 누락" };
  } else if (
    recordState.value.missing === "out1" ||
    recordState.value.missing === "out2"
  ) {
    note = { value: "퇴근 누락" };
  } else if (otCount > 0) {
    note = { value: `초과근무 ${otCount}건 인정 (${fmtHm(otTotal)})` };
  } else {
    note = { value: "−", cls: "val-empty" };
  }

  // PRAFTA-FIXEDOT-3: 고정연장근무(소정과 분리된 별도 축) — 구간 표기 + 실적(자동 계상) + 미이행 배지.
  //   전부 서버 파생값을 그대로 표시만 한다(정책 ①·②·③ 판정은 서버 단일 출처).
  //   고정연장 없는 근무타입/구서버 응답이면 null → 행 자체를 렌더하지 않는다(기존 팝업과 동일).
  const fixedOt = buildFixedOtRow(r);

  // 2026-08-17: 휴게시간 표시(스케줄 설정값 — 참고 표기 전용, 판정 로직 없음).
  const breakInfo = buildBreakInfo(r);

  return { plan, actual, note, fixedOt, breakInfo };
};

// 2026-08-17: 휴게 표시 문자열 — 시각 설정이면 "11:00 → 12:00", 분만 설정이면 "60분".
//   2구간 스케줄은 " · " 로 병기, 휴게 없는 타입/구서버 응답이면 null(행 미렌더).
const buildBreakInfo = (r) => {
  if (!r) return null;
  const parts = [];
  if (r.fstBrkStrTime && r.fstBrkEndTime) {
    parts.push(`${fmtTime(r.fstBrkStrTime)} → ${fmtTime(r.fstBrkEndTime)}`);
  } else if (Number(r.plan1BreakMin) > 0) {
    parts.push(`${Number(r.plan1BreakMin)}분`);
  }
  if (r.secBrkStrTime && r.secBrkEndTime) {
    parts.push(`${fmtTime(r.secBrkStrTime)} → ${fmtTime(r.secBrkEndTime)}`);
  } else if (Number(r.plan2BreakMin) > 0) {
    parts.push(`${Number(r.plan2BreakMin)}분`);
  }
  return parts.length ? parts.join(" · ") : null;
};

// PRAFTA-FIXEDOT-3: 고정연장 표시 모델. 구간이 없으면 null.
const buildFixedOtRow = (r) => {
  if (!r) return null;
  const ranges = [];
  if (r.preFixedOtStrTime && r.preFixedOtEndTime) {
    ranges.push(`${fmtTime(r.preFixedOtStrTime)} → ${fmtTime(r.preFixedOtEndTime)}`);
  }
  if (r.fixedOtStrTime && r.fixedOtEndTime) {
    ranges.push(`${fmtTime(r.fixedOtStrTime)} → ${fmtTime(r.fixedOtEndTime)}`);
  }
  if (!ranges.length) return null;
  const act = Number(r.fixedOtActMinutes);
  return {
    range: ranges.join(" · "),
    // 실적 = 실근태가 고정연장 구간을 커버한 분(정책 ① — 커버분만 계상).
    //   0분도 "0분"으로 명시한다(미이행 배지와 짝이 되는 정보라 숨기면 오독).
    act: !Number.isFinite(act) ? "" : act > 0 ? fmtHm(act) : "0분",
    unmet: r.fixedOtUnfulfilledYn === "Y",
  };
};

// "20260503" → "2026-05-03" (CalendarSrch 입력 포맷)
const ymdNumToDash = (s) => {
  const v = String(s || "");
  if (v.length < 8) return "";
  return `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`;
};

// ── 패널 빌더 ─────────────────────────────────────────────
const buildPanel = () => {
  const { isLeave, hasAct2 } = recordState.value;
  const { isFuture } = dateInfo.value;

  if (isFuture) {
    return {
      kind: "readonly-future",
      readonlyPill: "미래 일자",
      message:
        "<strong>아직 도래하지 않은 일자</strong>입니다.<br>근태 데이터는 출퇴근 등록 시점부터 생성됩니다.",
    };
  }
  if (isLeave) {
    return {
      kind: "readonly-leave",
      readonlyPill: "읽기 전용",
      message:
        "승인된 <strong>휴가</strong> 일자는 출근 등록이 차단되며<br>본 화면에서 수정할 수 없습니다.<br>휴가 변경은 별도 휴가 관리 화면에서 처리하세요.",
    };
  }
  // 편집 가능 케이스: 실제 출퇴근(act) 데이터가 있는 구간만 프리필.
  //  - act1 데이터가 있으면 1구간 추가, 없으면 비움 (기본값 09:00→18:00 자동 생성하지 않음)
  //  - act2 데이터가 있으면 2구간 추가
  // 비어 있어도 사용자가 "+ 구간 추가" 버튼으로 직접 추가 가능
  const r = record.value ?? {};
  const segments = [];
  const hasAct1 = !!(
    r.act1InTime ||
    r.act1OutTime ||
    r.act1InDate ||
    r.act1OutDate
  );
  if (hasAct1) {
    segments.push({
      startDate: ymdNumToDash(r.act1InDate) || props.date_p,
      startTime: r.act1InTime || "",
      endDate: ymdNumToDash(r.act1OutDate) || props.date_p,
      endTime: r.act1OutTime || "",
    });
  }
  if (hasAct2) {
    segments.push({
      startDate: ymdNumToDash(r.act2InDate) || props.date_p,
      startTime: r.act2InTime || "",
      endDate: ymdNumToDash(r.act2OutDate) || props.date_p,
      endTime: r.act2OutTime || "",
    });
  }
  return { kind: "segments", segments };
};

// ── 보조 안내 ─────────────────────────────────────────────
const buildEmptyHint = () => {
  const m = recordState.value.missing;
  if (m === "in1" || m === "in2") {
    return {
      text: "출근 기록이 없습니다.<br/>실제 근무 사실이 확인되면 우측에서 출퇴근 시간을 직접 등록해 보정하세요.",
    };
  }
  if (m === "out1" || m === "out2") {
    return {
      text: "출근만 등록되고 퇴근 기록이 없습니다.<br/>실제 퇴근 시간을 우측에서 직접 등록해 보정하세요.<br/>미보정 시 <strong>근태 마감 차단 사유</strong>가 됩니다.",
    };
  }
  return null;
};

// 처리 이력 응답 → 화면 모델 변환
// attdHistResultList row 예:
// { histTypeNm, befCheckInDate, befCheckInTime, befCheckOutDate, befCheckOutTime,
//   aftCheckInDate, aftCheckInTime, aftCheckOutDate, aftCheckOutTime,
//   insertNm, insertDate }
// 날짜+시간 합쳐서 한 셀로 표시. 둘 다 없으면 "-"
//   표시 포맷은 dateFormat 단일 출처에 위임(점/콜론). 초가 실재하면 함께 노출.
const fmtDateTime = (ymd, hms) => {
  const d = formatYmdDot(ymd);
  // 출퇴근 시각 컬럼(CHECK_IN_TIME 등)은 varchar(4)=HHMM(초 미존재) → 분까지만 표기.
  const t = formatHm(hms);
  if (!d && !t) return "-";
  return `${d} ${t}`.trim();
};
// INSERT_DATE: "YYYYMMDDHHMMSS"(초 실재) 또는 이미 포맷된 문자열 둘 다 대응
const fmtInsertDate = (v) => {
  const s = String(v || "");
  if (!s) return "-";
  // 14자리(초 실재)는 초까지, 그 외(12자리 등)는 분까지 표시
  if (/^\d{14}$/.test(s)) return formatDateTimeDotWithSec(s);
  if (/^\d{12}$/.test(s)) return formatDateTimeDot(s);
  return s;
};
const historyView = computed(() =>
  (historyList.value || []).map((h) => {
    // PRAFTA-APP-007-WEB-7 + D15 + com-013 #3: 스케줄 수정(10) 이력은 출퇴근 시각 대신
    //   "변경 전→후 스케줄 라벨"을 표시한다. 식별 기준을 reqType==='10' 으로 일반화해
    //   승인('02')뿐 아니라 반려('03')도 스케줄 라벨 분기를 타게 한다(백엔드가 반려에도 bef/aft 채움).
    //   (근태/OT/연차 이력은 reqType NULL → 기존 출퇴근 시각 렌더링.)
    const isSchedApprove = h.reqType === "10";
    const hasBefSched = !!(h.befSchedFstStrTime || h.befSchedFstEndTime);
    const hasAftSched = !!(h.aftSchedFstStrTime || h.aftSchedFstEndTime);
    return {
      histTypeNm: h.histTypeNm || "-",
      workSeq: h.workSeq != null && h.workSeq !== "" ? `${h.workSeq}구간` : "-",
      // 스케줄 이력 행: 변경 전/후 라벨(각각 도출 불가 시 "없음")을 단일 라벨로 노출.
      isSchedApprove,
      befSchedLabel: isSchedApprove
        ? hasBefSched
          ? schedLabel(
              h.befSchedFstStrTime,
              h.befSchedFstEndTime,
              h.befSchedSecStrTime,
              h.befSchedSecEndTime
            ) +
            // PRAFTA-FIXEDOT-2: 변경 전 고정연장 구분 표기(없으면 빈 문자열 — 기존 라벨 불변).
            fixedOtLabel(
              h.befPreFixedOtStrTime,
              h.befPreFixedOtEndTime,
              h.befFixedOtStrTime,
              h.befFixedOtEndTime
            )
          : "없음"
        : "",
      aftSchedLabel: isSchedApprove
        ? hasAftSched
          ? schedLabel(
              h.aftSchedFstStrTime,
              h.aftSchedFstEndTime,
              h.aftSchedSecStrTime,
              h.aftSchedSecEndTime
            ) +
            // PRAFTA-FIXEDOT-2: 변경 후 고정연장 구분 표기.
            fixedOtLabel(
              h.aftPreFixedOtStrTime,
              h.aftPreFixedOtEndTime,
              h.aftFixedOtStrTime,
              h.aftFixedOtEndTime
            )
          : "없음"
        : "",
      befCheckIn: fmtDateTime(h.befCheckInDate, h.befCheckInTime),
      befCheckOut: fmtDateTime(h.befCheckOutDate, h.befCheckOutTime),
      aftCheckIn: fmtDateTime(h.aftCheckInDate, h.aftCheckInTime),
      aftCheckOut: fmtDateTime(h.aftCheckOutDate, h.aftCheckOutTime),
      reason: h.processReason ?? "",
      // com-013 #4: 근로자 요청사유(processReason 관리자사유와 별개 컬럼).
      reqReason: h.reqReason ?? "",
      insertNm: h.insertNm || "-",
      insertDate: fmtInsertDate(h.insertDate),
    };
  })
);

// ── 근로자 요청 (monthlyAttdReqResultList) ──────────────────
//   각 카드는 BEFORE(현재 act{workSeq}*) vs AFTER(요청된 checkIn/Out*) 를 비교한다.
const reqCards = computed(() => {
  const r = record.value ?? {};
  return (reqList.value || []).map((req) => {
    const n = parseInt(req.workSeq, 10) || 1;
    const base = {
      raw: req,
      reqId: req.reqId,
      reqType: req.reqType,
      reqTypeNm: req.reqTypeNm || "-",
      reqStatus: req.reqStatus,
      reqStatusNm: req.reqStatusNm || "",
      insertDate: fmtInsertDate(req.insertDate),
      workSeq: n,
      reqReason: req.reqReason || "",
      // 연차(05/06) 결재 라우팅용 — 백엔드가 현재 로그인 사용자의 처리 단계를 내려줌(비결재자/그 외 타입은 null)
      approvalStep: req.approvalStep ?? null,
      // 승인/반려 버튼 노출 여부(표시/처리 분리).
      //   연차(05/06)는 다단계 결재라 '현재 단계 결재자' 만 처리할 수 있다(서버가 approvalStep 요구 →
      //   비결재자는 NULL 이라 호출 자체가 실패). 종전엔 백엔드가 비결재자에게 카드를 아예 안 내렸는데,
      //   그러면 마감을 막는 요청이 화면에서 사라져 추적이 불가해 표시는 허용하도록 바꿨다.
      //   그 대신 여기서 버튼을 감춘다. 근태/OT/스케줄(01~04/10)은 매니저 모델이라 종전대로 항상 처리 가능.
      canProcess:
        req.reqType === "05" || req.reqType === "06"
          ? !!req.approvalStep
          : true,
    };
    // PRAFTA-APP-007-WEB-7: 스케줄 수정(10) 은 출퇴근 시각이 아니라 "현재→목표 스케줄"을 비교한다.
    //   현재(cur*)는 tb_user_work_plan→tb_sch_mgmt, 목표(tgt*)는 REQ.SCH_CD→tb_sch_mgmt (WEB-5 응답).
    //   현재 스케줄이 없거나(WORK_PLAN_CD 없음) 연차코드면 cur* 전부 NULL → BEFORE "없음".
    if (req.reqType === "10") {
      const hasCur = !!(req.curFstStrTime || req.curFstEndTime);
      const befSched = hasCur
        ? schedLabel(
            req.curFstStrTime,
            req.curFstEndTime,
            req.curSecStrTime,
            req.curSecEndTime
          ) +
          // PRAFTA-FIXEDOT-2: 현재 스케줄 고정연장 구분 표기(없으면 빈 문자열).
          //   고정연장만 다른 변경도 befSched !== aftSched 로 변경점이 감지된다.
          fixedOtLabel(
            req.curPreFixedOtStrTime,
            req.curPreFixedOtEndTime,
            req.curFixedOtStrTime,
            req.curFixedOtEndTime
          )
        : "없음";
      const aftSched =
        schedLabel(
          req.tgtFstStrTime,
          req.tgtFstEndTime,
          req.tgtSecStrTime,
          req.tgtSecEndTime
        ) +
        // PRAFTA-FIXEDOT-2: 목표 스케줄 고정연장 구분 표기.
        fixedOtLabel(
          req.tgtPreFixedOtStrTime,
          req.tgtPreFixedOtEndTime,
          req.tgtFixedOtStrTime,
          req.tgtFixedOtEndTime
        );
      return {
        ...base,
        mode: "sched",
        befSched,
        aftSched,
        schedChanged: befSched !== aftSched,
      };
    }
    // PRAFTA-APP-018-D: 연차(05 연차사용 / 06 연차수정)는 출퇴근 BEFORE/AFTER 가 아니라
    //   사용단위·(시간차 범위)·차감일수를 보여주는 전용 카드(mode='leave')로 분리한다.
    //   정규근태(act{n}*)를 끌어오지 않는다(BEFORE 혼입 금지). 단위 출처는 백엔드 useUnitType/unitNm(SYS025).
    if (req.reqType === "05" || req.reqType === "06") {
      const unitCode = req.useUnitType ?? null; // '00'~'04' 또는 null(U 미매칭)
      // 시간차(02 2시간 / 03 1시간 / 04 30분)일 때만 시작~종료 범위 표시
      const isTimed = ["02", "03", "04"].includes(unitCode);
      // 라벨: 시간차면 '시간차 ' 접두(목표 포맷), 그 외엔 unitNm 그대로. unitNm 없으면 '연차' fallback.
      const leaveTypeLabel = req.unitNm
        ? isTimed && !req.unitNm.startsWith("시간차")
          ? `시간차 ${req.unitNm}`
          : req.unitNm
        : "연차";
      // LC-09(§5-B): 차감액은 "N일 H시간 M분 차감", 시간차는 원본 사용 분 병기
      return {
        ...base,
        mode: "leave",
        unitCode,
        leaveTypeLabel,
        timeRange: isTimed
          ? hourlyRangeLabel(req.startTime, req.endTime)
          : null,
        leaveDaysLabel: chargeDaysLabel(req.leaveDays),
        // 가불표시-06: 가불 충당 일수(서버 산출, 0 이상). 0/구서버(undefined)는 0 정규화 → 배지 미표시.
        borrowDays: Number(req.borrowDays) || 0,
      };
    }
    // 그 외(01~04): 출퇴근 시각 BEFORE/AFTER 모델.
    //   [prafta-app-017 이슈③] OT '생성'(03, TARGET_ID null)은 "변경 전"이 없으므로
    //   BEFORE 를 정규근태(act{n}*)에서 끌어오면 안 된다 → 공란("-").
    //   OT '수정'(04, TARGET_ID=기존 OT)·근태보정(01/02)은 현행 유지(회귀 금지).
    //   TODO(developer): OT 수정(04) BEFORE 를 기존 OT 행 값으로 정밀화 — 별도 작업(prafta-app-017 follow-up).
    const isOtCreate = req.reqType === "03";
    return {
      ...base,
      mode: "time",
      befIn: isOtCreate ? "-" : fmtTime(r[`act${n}InTime`]) || "-",
      befOut: isOtCreate ? "-" : fmtTime(r[`act${n}OutTime`]) || "-",
      aftIn: fmtTime(req.startTime) || "-",
      aftOut: fmtTime(req.endTime) || "-",
    };
  });
});

// ── 확정 연차 사용 (confirmedLeaveResultList) ────────────────
// PRAFTA-APP-018-F: 그날 확정 연차(자동확정/직접 포함) 표시 카드.
//   D 의 요청 카드(미처리 결재대기)와 상호배타(백엔드가 미처리01 제외) → 이중표시 없음.
//   포맷: {leaveNm} · {단위(시간차면 '시간차 ' 접두)} · (시간차면 시각) · {정규화}일 차감.
//   chargeDaysLabel/hourlyRangeLabel 헬퍼 재사용(LC-09 표기 규칙 §5-B).
const confirmedLeaveCards = computed(() =>
  (confirmedLeaves.value || []).map((lv, i) => {
    const unitCode = lv.useUnitType ?? null; // '00'~'04' 또는 null
    const isTimed = ["02", "03", "04"].includes(unitCode);
    const unitLabel = lv.unitNm
      ? isTimed && !lv.unitNm.startsWith("시간차")
        ? `시간차 ${lv.unitNm}`
        : lv.unitNm
      : "연차";
    // LC-09(§5-B): 차감액 표기 + 시간차 원본 사용 분 병기 (요청 카드와 동일 규칙)
    return {
      key: `cl-${i}`,
      leaveNm: lv.leaveNm || "연차사용",
      unitLabel,
      timeRange: isTimed ? hourlyRangeLabel(lv.startTime, lv.endTime) : null,
      leaveDaysLabel: chargeDaysLabel(lv.leaveDays),
    };
  })
);

// ── 연차 변경(이동/삭제) 요청 카드 (leaveChangeReqResultList) ──
//   Attd_13 의 라벨 맵과 동일 문구를 쓴다(화면 간 용어 일치).
const LC_TYPE_NM = { MOVE: "연차 일자 이동 요청", DELETE: "연차 삭제 요청" };
const LC_STATUS_NM = {
  REQUESTED: "근로자 동의 대기",
  AGREED: "확인 대기",
};
const LC_INITIATOR_NM = { ADMIN: "관리자 발의", WORKER: "근로자 요청" };

// ── 위치선택 확장(2026-08-18): 이동 대상 위치(반차 파트/시간차 지정 시각) 병기 ──
//   구서버 응답에 필드가 없으면 빈 값 → 카드 표시 종전 그대로(회귀 없음 — leaveChangeReqs 방어 관례 미러).
//   반차 파트는 대상일 경계 조회 없이 "시작 기준(늦게 출근)/종료 기준(일찍 퇴근)" 고정 표기
//   (오전/오후 환산 금지 — plan §4 사용자 확정).
//   attd07 read-model 은 leaveMinutes 미보유 — 시간차 종료는 원 구간 길이(END−START)로 파생
//   (시간차 use 행은 항상 시각 보유). hhmmToMin/fmtTime 은 computed 내부 호출이라 선언 순서 무관.
const LC_MOVE_HALF_PART_NM = {
  START: "시작 기준(늦게 출근)",
  END: "종료 기준(일찍 퇴근)",
};
const lcMoveTargetPosLabel = (r) => {
  if (r?.moveTargetHalfPart)
    return LC_MOVE_HALF_PART_NM[r.moveTargetHalfPart] || "";
  const s = hhmmToMin(r?.moveTargetStartTime);
  if (s == null) return "";
  const os = hhmmToMin(r?.startTime);
  const oe = hhmmToMin(r?.endTime);
  const dur = os != null && oe != null && oe > os ? oe - os : null;
  if (dur == null) return fmtTime(r.moveTargetStartTime);
  // 자정 넘김(END<START)은 익일 저장 규약 — 시각만 모듈러 표기
  const e = (s + dur) % 1440;
  const pad = (n) => String(n).padStart(2, "0");
  return `${fmtTime(r.moveTargetStartTime)}~${pad(Math.floor(e / 60))}:${pad(e % 60)}`;
};

// "20260805" → "08.05(수)". 값이 없으면 "-".
const fmtLeaveChangeDate = (ymd) => {
  const s = String(ymd ?? "");
  if (!/^\d{8}$/.test(s)) return "-";
  const d = new Date(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8));
  return `${s.slice(4, 6)}.${s.slice(6, 8)}(${dowLabels[d.getDay()]})`;
};

const leaveChangeCards = computed(() => {
  const viewYmd = ymdDashToNum(props.date_p);
  return (leaveChangeReqs.value || []).map((r, i) => {
    const isMove = r.reqType === "MOVE";
    const unitCode = r.useUnitType ?? null;
    // 시간차(02 2시간 / 03 1시간 / 04 30분)일 때만 시각 범위 병기 — 연차 카드와 동일 규칙
    const isTimed = ["02", "03", "04"].includes(unitCode);
    const unitLabel = r.unitNm
      ? isTimed && !r.unitNm.startsWith("시간차")
        ? `시간차 ${r.unitNm}`
        : r.unitNm
      : "연차";
    // 지금 열려 있는 셀이 출발일인지 이동 대상일인지 — 카드 상단에 방향 안내를 띄운다.
    //   (근태 요청처럼 관련된 두 날짜 모두에서 같은 요청이 보이므로 혼동 방지용)
    const isTargetSide = isMove && viewYmd && viewYmd === r.moveTargetDate;
    // 위치선택 확장: 이동 대상 위치(파트/지정 시각) — 미지정/구서버면 빈 값(종전 표시 그대로)
    const movePos = isMove ? lcMoveTargetPosLabel(r) : "";
    return {
      key: `lc-${r.changeReqId ?? i}`,
      changeReqId: r.changeReqId,
      reqType: r.reqType,
      reqTypeNm: LC_TYPE_NM[r.reqType] ?? "연차 변경 요청",
      reqStatus: r.reqStatus,
      reqStatusNm: LC_STATUS_NM[r.reqStatus] ?? r.reqStatus ?? "",
      initiatorNm: LC_INITIATOR_NM[r.initiatorType] ?? "",
      insertDate: fmtInsertDate(r.insertDate),
      reqReason: r.reqReason || "",
      // BEFORE/AFTER 칸: 좌=현재 연차일(출발일), 우=이동 대상일(삭제면 "삭제")
      fromDateLabel: fmtLeaveChangeDate(r.targetStartDate),
      // 위치선택 확장: 지정 파트/시각이 있으면 접미 병기(미지정이면 종전 문자열 바이트 그대로)
      toDateLabel: isMove
        ? fmtLeaveChangeDate(r.moveTargetDate) + (movePos ? ` · ${movePos}` : "")
        : "삭제",
      // 연차 상세 1줄(종류·단위·시간차 범위·차감일수) — 연차 카드 표기 규칙 재사용
      leaveNm: r.leaveNm || "연차",
      unitLabel,
      timeRange: isTimed ? hourlyRangeLabel(r.startTime, r.endTime) : null,
      leaveDaysLabel: chargeDaysLabel(r.leaveDays),
      // 지금 보고 있는 셀 기준 방향 안내
      sideHint: !isMove
        ? "이 날짜의 연차를 삭제"
        : isTargetSide
          ? "이 날짜로 이동"
          : "이 날짜에서 이동",
      // AGREED(관리자 확인 대기)만 처리 가능. REQUESTED 는 근로자 응답을 기다리는 단계.
      actionable: r.reqStatus === "AGREED",
    };
  });
});

// 근로자 요청 섹션 헤더 카운트 — 근태/연차 요청 + 연차 변경 요청 합산.
const reqSectionCount = computed(
  () => reqCards.value.length + leaveChangeCards.value.length
);

// ── 최종 cfg ──────────────────────────────────────────────
const cfg = computed(() => ({
  timeCard: buildTimeCard(),
  panel: buildPanel(),
  emptyHint: buildEmptyHint(),
  workingNotice: recordState.value.inProgress
    ? "아직 퇴근하지 않았습니다. 퇴근 등록 후 정산값이 표시됩니다."
    : null,
  history: historyView.value,
  historyEmpty: "등록된 처리 이력이 없습니다.",
}));

// ── 폼 상태 ───────────────────────────────────────────────
// API 응답 전엔 비어 있고, fnSearch 완료 후 initForm()으로 채워진다.
//   각 segment 는 정규근무 입력(startTime/endTime) + 초과근무 리스트(otList) 를 가진다.
//   otList: [{ startDate, startTime, endDate, endTime, otId?, reqId? }]
//   prafta-043: 초과근무 유형(type/otType) 전면 파기 — otList 항목에서 type 제거.
// PRAFTA-003-7: 백엔드 dailyOvertimeResultList 응답을 segment 별로 분배해 프리필한다.
const form = ref({ segments: [], reason: "" });

// "초기화" 버튼 복원 기준점 — initForm() 으로 form 이 채워질 때마다 그 시점 값을 깊은 복사로 보관.
const formSnapshot = ref(null);
const cloneForm = (v) => JSON.parse(JSON.stringify(v));

// "20260514" + "0900" 형식의 (날짜, 시각) → 분 stamp.
//   [QA 재작업 D1] 분 stamp 기준을 buildActualSegments 와 동일하게 workYmd-1 00:00 으로 통일.
//   (dayDiff + 1) * 1440 + m 으로 산출해 workYmd 00:00 = 1440 이 되도록 한다.
//   잘못된 값이면 null.
const otStampFromYmdHm = (ymd, hhmm) => {
  const baseYmd = ymdDashToNum(props.date_p);
  const d = String(ymd || "");
  if (baseYmd.length !== 8 || d.length !== 8) return null;
  const m = hhmmToMin(hhmm);
  if (m == null) return null;
  const by = parseInt(baseYmd.slice(0, 4), 10);
  const bm = parseInt(baseYmd.slice(4, 6), 10) - 1;
  const bd = parseInt(baseYmd.slice(6, 8), 10);
  const ty = parseInt(d.slice(0, 4), 10);
  const tm = parseInt(d.slice(4, 6), 10) - 1;
  const td = parseInt(d.slice(6, 8), 10);
  const dayDiff = Math.round(
    (new Date(ty, tm, td).getTime() - new Date(by, bm, bd).getTime()) /
      (1000 * 60 * 60 * 24)
  );
  return (dayDiff + 1) * 1440 + m;
};

// OT row 1건을 form.segments 의 어느 구간에 넣을지 판정한다.
//   분 stamp 기준으로 실근태 구간(buildActualSegments) 중 어느 구간에 OT 시작이
//   포함되는지 검사. 어디에도 포함되지 않으면 가장 가까운(시작이 더 작은 구간 끝
//   이후에 있는) 구간으로 fallback. segment 개수가 부족하면 0(1구간)으로 fallback.
const assignOtToSegment = (otRow, stdSegs, segCount) => {
  if (!segCount) return -1;
  if (segCount === 1) return 0;
  // segCount === 2 — 실근태 구간이 인덱스별로 1:1 대응한다는 가정.
  // buildActualSegments 는 act1, act2 순서로 push 하므로 인덱스 의미가 일치.
  const start = otStampFromYmdHm(otRow.actualStartDate, otRow.actualStartTime);
  const end = otStampFromYmdHm(otRow.actualEndDate, otRow.actualEndTime);
  if (start == null) return 0;
  // stdSegs[0] / stdSegs[1] 와 비교.
  // [QA 재작업 D3] stdSegs 는 인덱스 보존 배열이므로 각 자리에 null 이 올 수 있다.
  //   양쪽 구간이 모두 유효할 때만 구간 판정을 수행하고, 한쪽이 null 이면
  //   유효한 구간(또는 1구간)으로 fallback 한다.
  // OT 시작이 std[1] 구간 내부 또는 std[1] 끝 이후라면 2구간에 귀속.
  if (stdSegs[0] && stdSegs[1]) {
    const [s1, e1] = stdSegs[0];
    const [s2] = stdSegs[1];
    // 1구간 actual 내부 또는 1구간 종료 직후 ~ 2구간 시작 전까지면 1구간 OT.
    if (start >= s1 && (end == null || end <= s2)) return 0;
    // 2구간 actual 내부 또는 2구간 종료 이후면 2구간 OT.
    if (start >= s2 || (end != null && end > e1 && start >= e1)) return 1;
    // 기본: 1구간.
    return 0;
  }
  // 한쪽 구간만 유효한(또는 둘 다 null 인) 비정상 케이스 → 유효 구간이 2구간뿐이면 1, 그 외 1구간.
  if (!stdSegs[0] && stdSegs[1]) return 1;
  return 0;
};

function initForm() {
  const panel = cfg.value.panel;
  if (panel.kind === "segments") {
    // 1) 정규근무(startDate/Time/endDate/Time) 부분을 panel.segments 로 채운다.
    const segments = panel.segments.map((s) => ({ ...s, otList: [] }));

    // 2) PRAFTA-003-7: dailyOvertimeResultList 를 segment 인덱스별로 분배.
    const actSegs = buildActualSegments();
    const otRows = dailyOvertimeList.value || [];
    for (const ot of otRows) {
      const idx = assignOtToSegment(ot, actSegs, segments.length);
      if (idx < 0 || !segments[idx]) continue;
      const otStartDate = ymdNumToDash(ot.actualStartDate) || props.date_p;
      const otStartTime = ot.actualStartTime || "";
      const otEndDate = ymdNumToDash(ot.actualEndDate) || props.date_p;
      const otEndTime = ot.actualEndTime || "";
      segments[idx].otList.push({
        otId: ot.otId,
        reqId: ot.reqId || null,
        // com-016-E: 기저장(otId 보유) 행은 기본 미체크. 삭제는 체크된 기저장행을,
        //   저장은 체크된 "신규행 또는 편집된 기저장행(in-place 수정)"을 대상으로 한다(좌측 체크박스 모델).
        checked: false,
        startDate: otStartDate,
        startTime: otStartTime,
        endDate: otEndDate,
        endTime: otEndTime,
        // com-013-06 A: in-place 수정 dirty 판정용 원본 스냅샷(기저장행 한정).
        //   네 값 중 하나라도 바뀌면 저장 대상이 되어 서버로 UPDATE 전송된다.
        _origStartDate: otStartDate,
        _origStartTime: otStartTime,
        _origEndDate: otEndDate,
        _origEndTime: otEndTime,
      });
    }

    form.value = {
      segments,
      reason: "",
    };
  } else {
    form.value = { reason: "" };
  }
  // 방금 채워진 form 상태를 "초기화" 복원 기준점으로 저장 (저장/삭제 후 reload 시 갱신됨)
  formSnapshot.value = cloneForm(form.value);
}

// "초기화" — 직접 수정 폼을 마지막으로 데이터를 불러온 시점(initForm) 값으로 되돌린다.
const fnResetForm = async () => {
  if (!formSnapshot.value) return;
  const ok = await proxy.$confirm(getMessage(MSG.FORM_RESET_CONFIRM));
  if (!ok) return;
  form.value = cloneForm(formSnapshot.value);
};

const canSave = computed(() => form.value.reason.trim().length > 0);

// ── 구간 입력 관리 ────────────────────────────────────────
const MAX_SEGMENTS = 2;

const addSegment = () => {
  if (isMonthClosed.value) {
    proxy.$alert("마감된 월입니다. 마감 해제 후 수정할 수 있습니다.");
    return;
  }
  if (form.value.segments.length >= MAX_SEGMENTS) return;
  form.value.segments.push({
    startDate: props.date_p,
    startTime: "",
    endDate: props.date_p,
    endTime: "",
    otList: [],
  });
};

// 출/퇴근 일자가 다르면 익일 처리 (날짜 문자열은 사전순 비교 가능)
const isOverday = (seg) =>
  !!seg.endDate && !!seg.startDate && seg.endDate > seg.startDate;

// ── 초과근무 관리 (UI 골격) ─────────────────────────────────
// 추가/삭제만 화면 상태로 처리. 저장 payload 매핑은 추후 백엔드 합의 후 연동.

// DB에서 읽어온 구간인지 여부 (record.attd{n}Id 존재 ⇒ DB 적재 구간).
//   화면에서 "+ 구간 추가"로 새로 추가된 구간은 false 가 되므로 초과근무 입력을 막는다.
const isSegmentFromDb = (segIdx) => {
  const r = record.value ?? {};
  return !!r[`attd${segIdx + 1}Id`];
};

const addOt = (segIdx) => {
  // 일용직은 초과근무 등록 불가(UI 가드와 동일 규칙·서버도 차단).
  if (isDailyWorker.value) return;
  const seg = form.value.segments[segIdx];
  if (!seg) return;
  // DB 적재 구간이 아니면 초과근무 등록 차단 (UI 가드와 동일 규칙).
  if (!isSegmentFromDb(segIdx)) return;
  if (!Array.isArray(seg.otList)) seg.otList = [];
  // 기본값: 해당 구간 퇴근 직후 시작, 동일 일자
  // prafta-043: 초과근무 유형(type) 전면 파기 — 기본 타입 미부여.
  // com-016-E: 새로 추가한 신규행은 저장 의도가 명확하므로 기본 체크.
  seg.otList.push({
    checked: true,
    startDate: seg.endDate || seg.startDate || props.date_p,
    startTime: "",
    endDate: seg.endDate || seg.startDate || props.date_p,
    endTime: "",
  });
};

// 분 stamp → CalendarSrch 입력용 일자 "YYYY-MM-DD".
//   stampToDateTime 과 동일하게 stamp origin 은 workYmd-1 00:00 기준이다(dayOffset = floor/1440 - 1).
const stampToDateDash = (mins) => {
  if (!props.date_p) return props.date_p || "";
  const dayOffset = Math.floor(mins / 1440) - 1;
  const [y, mo, d] = props.date_p.split("-").map(Number);
  const dt = new Date(y, mo - 1, d + dayOffset);
  const yy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, "0");
  const dd = String(dt.getDate()).padStart(2, "0");
  return `${yy}-${mm}-${dd}`;
};

// 분 stamp → 시각 입력용 "HHMM"(콜론 없는 4자리). stampToHHmm 의 콜론 제거 버전.
const stampToHHMMRaw = (mins) => {
  const m = ((mins % 1440) + 1440) % 1440;
  const h = Math.floor(m / 60);
  const mm = m % 60;
  return `${String(h).padStart(2, "0")}${String(mm).padStart(2, "0")}`;
};

// "등록 가능" 칩 클릭 → 해당 범위(w)에 맞춰 초과근무 row 를 자동 추가한다.
//   w.startMin/endMin(분 stamp)을 OT row 필드(startDate/startTime/endDate/endTime)로 변환한다.
//   동일 범위가 이미 등록돼 있으면 중복 추가하지 않는다.
const addOtFromWindow = (segIdx, w) => {
  // 일용직은 초과근무 등록 불가(UI 가드와 동일 규칙·서버도 차단).
  if (isDailyWorker.value) return;
  const seg = form.value.segments[segIdx];
  if (!seg) return;
  // DB 적재 구간이 아니면 초과근무 등록 차단 (addOt 와 동일 규칙).
  if (!isSegmentFromDb(segIdx)) return;
  if (w == null || w.startMin == null || w.endMin == null) return;
  if (!Array.isArray(seg.otList)) seg.otList = [];

  const row = {
    // com-016-E: 칩 클릭으로 추가한 신규행도 저장 의도가 명확하므로 기본 체크.
    checked: true,
    startDate: stampToDateDash(w.startMin),
    startTime: stampToHHMMRaw(w.startMin),
    endDate: stampToDateDash(w.endMin),
    endTime: stampToHHMMRaw(w.endMin),
  };
  // 동일 범위 중복 추가 방지.
  const dup = seg.otList.some(
    (o) =>
      o.startDate === row.startDate &&
      o.startTime === row.startTime &&
      o.endDate === row.endDate &&
      o.endTime === row.endTime
  );
  if (dup) return;
  seg.otList.push(row);
};

// com-016-E: 휴지통 제거 → 좌측 체크박스 + "초과근무 삭제" 버튼 모델로 전환.
//   해당 구간에 "삭제 가능한 체크 행"(체크된 기저장행 otId 보유) 또는 "체크된 신규행"이 있는지.
//   버튼 활성 조건: 서버 삭제 대상(기저장) 또는 로컬 제거 대상(신규)이 1건이라도 있으면 활성.
const hasCheckedDeletable = (segIdx) => {
  const seg = form.value.segments?.[segIdx];
  if (!seg || !Array.isArray(seg.otList)) return false;
  return seg.otList.some((o) => o.checked);
};

// com-016-E: "초과근무 삭제" — 체크된 행을 삭제한다.
//   기저장행(otId 보유) → 다건 삭제 API 1회 호출(서버 soft-delete).
//   신규행(otId 없음) → 화면에서만 splice(서버 무관).
const fnDeleteOvertime = async (segIdx) => {
  if (await guardClosed()) return;
  if (otSaving.value) return;
  const seg = form.value.segments[segIdx];
  if (!seg || !Array.isArray(seg.otList)) return;

  const checkedSaved = seg.otList.filter((o) => o.checked && o.otId);
  const checkedNew = seg.otList.filter((o) => o.checked && !o.otId);
  if (!checkedSaved.length && !checkedNew.length) return;

  const ok = await proxy.$confirm(getMessage(MSG.OT_DELETE_CONFIRM), {
    variant: "danger",
  });
  if (!ok) return;

  // 1) 기저장행이 없으면 서버 호출 없이 신규 체크행만 로컬 제거.
  if (!checkedSaved.length) {
    seg.otList = seg.otList.filter((o) => !(o.checked && !o.otId));
    return;
  }

  // 2) 기저장행이 있으면 다건 삭제 API 1회 호출.
  const r = record.value ?? {};
  const u = userInfo.value ?? {};
  const payload = {
    otIds: checkedSaved.map((o) => o.otId),
    siteCd: props.siteCd_p || r.siteCd || "",
    userCd: props.userCd_p || u.userCd || r.userCd || "",
    workYmd: ymdToYmdNum(props.date_p),
    nodeCd: props.nodeCd_p || r.nodeCd || "",
    reqReason: form.value.reason || "",
  };

  otSaving.value = true;
  try {
    const response = await axios.post(
      "/webApi/attd07/delete-user-overtime",
      payload
    );
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.DELETE_SUCCESS));
      // 삭제 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해 데이터를 reload 한다.
      //   (reload 시 신규 체크행은 어차피 폼이 재구성되며 사라진다.)
      await fnSearch();
    }
  } catch (err) {
    console.error("[AttdDayDetailPop] overtime delete failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.OT_DELETE_ERROR))
    );
  } finally {
    otSaving.value = false;
  }
};

// 구간 헤더의 요약 표시 (정규/초과 합계). 데이터가 없거나 계산 불가 시 빈 문자열.
//   - 정규근무: in~out 시간 차 (분 단위)
//   - 초과근무: otList 의 각 row 시간 합 (분 단위)
const minutesOfRange = (sDate, sTime, eDate, eTime) => {
  if (!sTime || !eTime || sTime.length < 4 || eTime.length < 4) return 0;
  const sd = (sDate || "").replace(/-/g, "");
  const ed = (eDate || "").replace(/-/g, "");
  if (!sd || !ed) return 0;
  const sStamp =
    parseInt(sd, 10) * 1440 +
    parseInt(sTime.slice(0, 2), 10) * 60 +
    parseInt(sTime.slice(2, 4), 10);
  const eStamp =
    parseInt(ed, 10) * 1440 +
    parseInt(eTime.slice(0, 2), 10) * 60 +
    parseInt(eTime.slice(2, 4), 10);
  return Math.max(0, eStamp - sStamp);
};
const fmtHm = (mins) => {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${h}시간 ${m}분`;
};
const segSummary = (seg) => {
  const reg = minutesOfRange(
    seg.startDate,
    seg.startTime,
    seg.endDate,
    seg.endTime
  );
  const ot = (seg.otList || []).reduce(
    (sum, o) =>
      sum + minutesOfRange(o.startDate, o.startTime, o.endDate, o.endTime),
    0
  );
  if (!reg && !ot) return "";
  const parts = [];
  if (reg) parts.push(`정규<strong>${fmtHm(reg)}</strong>`);
  if (ot) parts.push(`초과<strong>${fmtHm(ot)}</strong>`);
  return parts.join('<span class="dot">·</span>');
};

// ── 초과근무(OT) 신규 API 연동 (PRAFTA-003) ────────────────
// 정책서 2,3번 — 등록 가능 OT 범위 = (실근태 근무시간) - (스케줄 시간).
// 백엔드 /attd07/update-user-overtime-requests 에서도 동일 검증을 수행한다.
// 여기서는 사용자 가이드 + 사전 차단을 위해 클라이언트 측에서도 한 번 계산한다.

// 저장 진행 중 플래그 (모든 segment 의 ot-actions 가 공유)
const otSaving = ref(false);

// 소정-07: 단축근무자(육아기·가족돌봄) 연장근로 "근로자 명시 청구 확인" 체크.
//   otSaving 과 동일하게 모든 segment 의 OT 블록이 공유한다(한 일자 = 한 근로자 = 하나의 청구 사실).
//   서버에는 'Y'/'N' 문자열로 전송하며, 단축 기간이 아닌 근로자에게는 서버가 값을 보지 않는다.
const otWorkerClaimConfirmed = ref(false);

// 소정-07 후속(2026-08-17): 대상 근로자의 근무일 기준 단축 기간 여부(서버 파생 'Y'/'N').
//   'Y' 일 때만 위 체크박스·안내 문구를 노출한다(대다수 일반 근로자 팝업에서는 숨김).
//   판정·차단의 단일 출처는 여전히 서버 게이트(ATTD_400_200/201/202) — 이 값은 표시 조건 전용이라
//   구서버(필드 미수신)면 'Y' 로 폴백해 종전처럼 항상 노출한다(fail-visible: 숨겨서 등록이 막히는 것 방지).
const reducedWorkYn = ref("Y");

// ── 외근 GPS 동선 (PRAFTA-009 part2) ───────────────────────
//   외근 버튼 클릭 시 해당 구간의 ATTD_ID 로 GET /attd08/attd-gps-trail 호출,
//   응답 trail 을 AttdGpsCoordPanel 에 전달한다. 같은 구간 버튼 재클릭 시 패널 닫힘.
//   segIdx: 현재 열려 있는 구간 인덱스(0/1), null 이면 패널 닫힘.
const gpsPanel = ref({ segIdx: null, attdId: "", trail: [], loading: false });

const fnToggleGps = async (segIdx, attdId) => {
  console.log("[AttdDayDetailPop] fnToggleGps", { segIdx, attdId });

  // 같은 구간 버튼 재클릭 → 패널 토글(닫기)
  if (gpsPanel.value.segIdx === segIdx) {
    gpsPanel.value = { segIdx: null, attdId: "", trail: [], loading: false };
    return;
  }
  if (!attdId) {
    await proxy.$alert(getMessage(MSG.SEARCH_ERROR));
    return;
  }
  // 다른 구간으로 전환: 패널을 즉시 열고 로딩 표시 후 trail 조회.
  gpsPanel.value = { segIdx, attdId, trail: [], loading: true };
  try {
    const response = await axios.get("/webApi/attd08/attd-gps-trail", {
      params: { attdId },
    });
    if (response.status === 200) {
      // 조회 도중 사용자가 다른 구간으로 전환했다면 결과를 버린다.
      if (gpsPanel.value.segIdx !== segIdx) return;
      gpsPanel.value.trail = response.data?.attdGpsTrailResultList ?? [];
    }
  } catch (err) {
    console.error("[AttdDayDetailPop] gps trail load failed", err);
    if (gpsPanel.value.segIdx === segIdx) gpsPanel.value.trail = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR))
    );
  } finally {
    if (gpsPanel.value.segIdx === segIdx) gpsPanel.value.loading = false;
  }
};

// HH:mm 라벨 (분 stamp → "HH:mm")
const stampToHHmm = (mins) => {
  const m = ((mins % 1440) + 1440) % 1440;
  const h = Math.floor(m / 60);
  const mm = m % 60;
  return `${String(h).padStart(2, "0")}:${String(mm).padStart(2, "0")}`;
};

// 날짜+시각 라벨 (분 stamp → "YYYY-MM-DD HH:mm")
// [QA 재작업 D1] stamp origin 이 workYmd-1 00:00 기준(workYmd 00:00 = 1440)으로 통일되었으므로
//   props.date_p(workYmd) 기준 일자 오프셋은 Math.floor(mins / 1440) - 1 이다.
const stampToDateTime = (mins) => {
  const hhmm = stampToHHmm(mins);
  if (!props.date_p) return hhmm;
  const dayOffset = Math.floor(mins / 1440) - 1;
  const [y, mo, d] = props.date_p.split("-").map(Number);
  const dt = new Date(y, mo - 1, d + dayOffset);
  // 표시용 날짜는 점(YYYY.MM.DD). 시각은 HH:mm 그대로 결합.
  return `${formatYmdDot(dt)} ${hhmm}`;
};

// "HHmm" → 분 (00:00 기준). 잘못된 값이면 null.
const hhmmToMin = (hhmm) => {
  if (!hhmm || String(hhmm).length !== 4) return null;
  const h = parseInt(String(hhmm).slice(0, 2), 10);
  const m = parseInt(String(hhmm).slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};

// 스케줄 구간 (record.plan{n}Start/End) → 분 stamp.
// [QA 재작업 D1] stamp origin 을 workYmd-1 00:00 기준으로 통일(백엔드와 동일).
//   plan 은 workYmd anchor 이므로 sMin/eMin 모두 +1440. end <= start 이면 자정 넘김으로 추가 +1440.
// [QA 재작업 D3] 구간 인덱스(idx0=1구간, idx1=2구간)를 보존한다.
//   값이 null/invalid 여도 push 를 건너뛰지 않고 null 을 채워 자리를 유지한다(compaction 금지).
const buildSchSegments = () => {
  const r = record.value ?? {};
  const segs = [];
  const build = (s, e) => {
    const sRaw = hhmmToMin(s);
    let eRaw = hhmmToMin(e);
    if (sRaw == null || eRaw == null) return null;
    const sMin = 1440 + sRaw;
    let eMin = 1440 + eRaw;
    if (eMin <= sMin) eMin += 1440;
    return [sMin, eMin];
  };
  segs.push(build(r.plan1Start, r.plan1End));
  segs.push(build(r.plan2Start, r.plan2End));
  return segs;
};

// raw 실제 근무 구간 (act{n}InTime, act{n}OutTime) → 분 stamp.
// 출퇴근 일자 (act{n}InDate, act{n}OutDate) 차이로 자정 넘김 보정.
// [QA 재작업 D1] stamp origin 을 workYmd-1 00:00 기준으로 통일(백엔드와 동일).
//   (dayDiff + 1) * 1440 + hhmm 으로 산출해 workYmd 00:00 = 1440 이 되도록 한다.
// [QA 재작업 D3] 구간 인덱스(idx0=1구간, idx1=2구간)를 보존한다.
//   값이 null/invalid 여도 push 를 건너뛰지 않고 null 을 채워 자리를 유지한다(compaction 금지).
// 초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산한다(raw 실근태 시각 기준).
//   (백엔드 AttdScheduleUtils.buildActualSegmentsBySeq 와 동일 규칙.)
const buildActualSegments = () => {
  const r = record.value ?? {};
  const baseYmd = ymdDashToNum(props.date_p);
  if (baseYmd.length !== 8) return [null, null];

  const ymdDiffDays = (ymd) => {
    if (!ymd || ymd.length !== 8) return 0;
    const y = parseInt(ymd.slice(0, 4), 10);
    const m = parseInt(ymd.slice(4, 6), 10);
    const d = parseInt(ymd.slice(6, 8), 10);
    const b = new Date(
      parseInt(baseYmd.slice(0, 4), 10),
      parseInt(baseYmd.slice(4, 6), 10) - 1,
      parseInt(baseYmd.slice(6, 8), 10)
    );
    const t = new Date(y, m - 1, d);
    return Math.round((t.getTime() - b.getTime()) / (1000 * 60 * 60 * 24));
  };

  const segs = [];
  const build = (inDate, inTime, outDate, outTime) => {
    const sMin = hhmmToMin(inTime);
    const eMin = hhmmToMin(outTime);
    if (sMin == null || eMin == null) return null;
    const sStamp = (ymdDiffDays(inDate) + 1) * 1440 + sMin;
    const eStamp = (ymdDiffDays(outDate) + 1) * 1440 + eMin;
    if (eStamp <= sStamp) return null;
    return [sStamp, eStamp];
  };

  segs.push(build(r.act1InDate, r.act1InTime, r.act1OutDate, r.act1OutTime));
  segs.push(build(r.act2InDate, r.act2InTime, r.act2OutDate, r.act2OutTime));
  return segs;
};

// 정렬 + 인접/겹침 병합. 입력은 변경하지 않는다.
const mergeIntervals = (segs) => {
  const sorted = [...segs].sort((a, b) => a[0] - b[0]);
  const out = [];
  for (const [s, e] of sorted) {
    if (!out.length || out[out.length - 1][1] < s) {
      out.push([s, e]);
    } else if (e > out[out.length - 1][1]) {
      out[out.length - 1][1] = e;
    }
  }
  return out;
};

// 차집합 (a - b). 두 입력 모두 mergeIntervals 결과(정렬·분리)여야 한다.
const subtractIntervals = (a, b) => {
  const out = [];
  for (const [start, end] of a) {
    let cursor = start;
    for (const [bs, be] of b) {
      if (be <= cursor) continue;
      if (bs >= end) break;
      if (bs > cursor) out.push([cursor, bs]);
      cursor = Math.max(cursor, be);
      if (cursor >= end) break;
    }
    if (cursor < end) out.push([cursor, end]);
  }
  return out;
};

// 화면 노출용 — 등록 가능 OT 구간 리스트 (구간별 분리 계산).
//   PRAFTA-011 백엔드 규칙과 동일하게 1구간/2구간 각각 따로 계산한다.
//   - 매칭 스케줄이 있는 구간: (해당 구간 실근태) - (해당 구간 스케줄)
//   - 매칭 스케줄이 없는 구간: 해당 구간 실근태 전체가 등록 가능
//   초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산한다(raw 실근태 시각 기준).
//   [QA 재작업 D3] buildActualSegments / buildSchSegments 는 구간 인덱스를 보존하며
//   해당 구간 값이 없으면 null 을 둔다. idx0=1구간, idx1=2구간 1:1 매핑이 정상 동작한다.
//   - act 가 null 인 구간: 등록 가능 OT 없음 → 빈 배열.
//   - sch 가 null 인 구간: 매칭 스케줄 없음 → 실근태 전체가 등록 가능.
const otAllowedWindowsBySeg = computed(() => {
  const actSegs = buildActualSegments();
  const schSegs = buildSchSegments();
  // OT 칩 정합(2026-08-08): 서버가 내려준 연차 면제 구간을 피감수에 합친다.
  //   서버 검증 산식(등록 가능 = 실근태 − (스케줄 ∪ 연차 면제), ATTD_400_012)과 동일해져
  //   반차일 2차 재출근 구간에서 칩(전량)과 저장 결과(거부)가 어긋나던 불일치가 사라진다.
  //   stamp 는 서버 값 그대로(동일 축 — FE 재계산 금지: 야간 wrap 재현 위험). 미수신이면 빈 배열이라 종전과 동일.
  const exemptSegs = (otLeaveExemptWindows.value || [])
    .map((w) => [w.startStamp, w.endStamp])
    .filter(([s, e]) => Number.isFinite(s) && Number.isFinite(e) && e > s);
  // PRAFTA-FIXEDOT-2: 고정연장 점유 구간(서버 산출)도 피감수에 합친다 — 고정연장 구간이
  //   "등록 가능"으로 오표시되지 않게(서버 검증과 동일 산식). 미수신/빈 배열이면 종전과 동일.
  const fixedOtSegs = (otFixedOtWindows.value || [])
    .map((w) => [w.startStamp, w.endStamp])
    .filter(([s, e]) => Number.isFinite(s) && Number.isFinite(e) && e > s);
  return actSegs.map((act, i) => {
    if (!act) return [];
    const sch = schSegs[i];
    // 피감수 = 매칭 스케줄(있으면) ∪ 그날 고정연장 구간 ∪ 그날 연차 면제 구간 (서버 4-B/4-C 와 동일 형태).
    const subtrahend = [...(sch ? [sch] : []), ...fixedOtSegs, ...exemptSegs];
    const allowed = subtrahend.length
      ? subtractIntervals(mergeIntervals([act]), mergeIntervals(subtrahend))
      : [act];
    return allowed.map(([s, e]) => ({
      startMin: s,
      endMin: e,
      startLabel: stampToDateTime(s),
      endLabel: stampToDateTime(e),
    }));
  });
});

// 특정 구간(0-based)의 등록 가능 OT 구간 리스트. 없으면 빈 배열.
const otAllowedWindowsForSeg = (segIdx) =>
  otAllowedWindowsBySeg.value[segIdx] || [];

// otList 의 한 row → 분 stamp 구간. 잘못된 값이면 null.
const otRowToStamp = (ot) => {
  const sd = ymdDashToNum(ot.startDate || "");
  const ed = ymdDashToNum(ot.endDate || "");
  const baseYmd = ymdDashToNum(props.date_p);
  if (sd.length !== 8 || ed.length !== 8 || baseYmd.length !== 8) return null;
  const sMin = hhmmToMin(ot.startTime);
  const eMin = hhmmToMin(ot.endTime);
  if (sMin == null || eMin == null) return null;
  const dayDiff = (ymd) => {
    const y = parseInt(ymd.slice(0, 4), 10);
    const m = parseInt(ymd.slice(4, 6), 10);
    const d = parseInt(ymd.slice(6, 8), 10);
    const b = new Date(
      parseInt(baseYmd.slice(0, 4), 10),
      parseInt(baseYmd.slice(4, 6), 10) - 1,
      parseInt(baseYmd.slice(6, 8), 10)
    );
    const t = new Date(y, m - 1, d);
    return Math.round((t.getTime() - b.getTime()) / (1000 * 60 * 60 * 24));
  };
  // [QA 재작업 D1] stamp origin 을 workYmd-1 00:00 기준으로 통일 (+1 day offset).
  const sStamp = (dayDiff(sd) + 1) * 1440 + sMin;
  const eStamp = (dayDiff(ed) + 1) * 1440 + eMin;
  if (eStamp <= sStamp) return null;
  return [sStamp, eStamp];
};

// com-013-06 A: 저장(전송) 대상 OT row 판정.
//   - 신규행(otId 없음): 항상 저장 대상(INSERT).
//   - 기저장행(otId 보유): 원본 스냅샷 대비 시간이 바뀐 경우(in-place 수정)만 저장 대상(UPDATE).
//     변경되지 않은 기저장행은 (삭제 의도로) 체크돼 있어도 저장 payload 에서 제외해 불필요한 UPDATE 를 막는다.
const isOtRowDirty = (o) =>
  !o.otId ||
  o.startDate !== o._origStartDate ||
  o.startTime !== o._origStartTime ||
  o.endDate !== o._origEndDate ||
  o.endTime !== o._origEndTime;

// 저장 전송 대상 = 체크되었고(선택) 또한 신규/편집됨(dirty)인 행.
const isOtSaveTarget = (o) => o.checked && isOtRowDirty(o);

// 특정 구간의 OT row 들이 (1) 완전 입력 (2) 그 구간의 허용 범위 포함
// (3) 서로 겹치지 않을 때 true. 그 외 false.
//   PRAFTA-009 part4: 등록 가능 범위가 구간별로 분리되었으므로, OT 검증도
//   각 구간 자신의 허용 범위(otAllowedWindowsForSeg)와 대조한다.
const isSegOtValid = (segIdx) => {
  const seg = form.value.segments?.[segIdx];
  if (!seg || !Array.isArray(seg.otList) || !seg.otList.length) return false;
  // com-013-06 A: 저장 검증 대상은 "저장 전송 대상"(체크된 신규행 + 체크·편집된 기저장행)이다.
  //   미체크/미편집 기저장행은 저장 payload 에서 제외되므로 유효성 판정에서도 제외한다
  //   (그 행이 범위 밖이어도 신규/편집 1건 저장이 막히지 않게).
  const targets = seg.otList.filter(isOtSaveTarget);
  if (!targets.length) return false;
  const allowed = otAllowedWindowsForSeg(segIdx).map((w) => [
    w.startMin,
    w.endMin,
  ]);
  if (!allowed.length) return false;

  const stamps = [];
  for (const ot of targets) {
    if (!ot.startDate || !ot.startTime || !ot.endDate || !ot.endTime) {
      return false;
    }
    const stamp = otRowToStamp(ot);
    if (!stamp) return false;
    const ok = allowed.some(([as, ae]) => as <= stamp[0] && ae >= stamp[1]);
    if (!ok) return false;
    stamps.push(stamp);
  }
  // overlap check (구간 내부)
  const sorted = [...stamps].sort((a, b) => a[0] - b[0]);
  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i][0] < sorted[i - 1][1]) return false;
  }
  return true;
};

// PRAFTA-COM-013-06-3(r34-2): 초과근무 저장 버튼 활성/비활성 판정을 구간별로 분리한다.
//   기존 canSaveOt 는 "OT 행이 있는 모든 구간이 각자 유효" 라는 전역 조건이라,
//   2구간 OT 가 유효하지 않으면 1구간 저장 버튼까지 비활성화되는 버그가 있었다.
//   템플릿의 구간별 저장 버튼은 이제 isSegOtValid(i)(해당 구간 자신만 검증)로 판정한다.
//   (전역 canSaveOt 는 제거 — 더 이상 사용처가 없다.)

// 해당 segment 에 OT 행이 하나라도 있는지
const hasAnyOt = (segIdx) => {
  const seg = form.value.segments?.[segIdx];
  return !!(seg && Array.isArray(seg.otList) && seg.otList.length);
};

// com-013 #6b: OT '반려' 버튼/핸들러(otHasReqId, fnRejectOvertime) 제거.
//   관리자 직접수정 OT 블록엔 결재 대상 요청이 흘러오지 않아 반려 개념이 무의미했다.
//   요청 반려 인프라(onRejectConfirm 의 kind==="overtime" 분기 + reject-user-overtime-requests EP)는
//   Attd_10 인박스 공용이므로 그대로 보존한다.

// 클라이언트 측 검증 — 통과하면 true, 실패 시 alert 노출.
const validateOtBeforeSave = async (segIdx) => {
  const seg = form.value.segments?.[segIdx];
  // com-013-06 A: 저장 검증 대상은 저장 전송 대상(체크된 신규행 + 체크·편집된 기저장행).
  const targets = (seg?.otList || []).filter(isOtSaveTarget);
  if (!seg || !Array.isArray(seg.otList) || !targets.length) {
    await proxy.$alert(getMessage(MSG.OT_LIST_EMPTY));
    return false;
  }
  // isSegOtValid 가 false 인 정확한 원인을 사용자에게 알린다.
  for (const ot of targets) {
    if (!ot.startDate || !ot.startTime || !ot.endDate || !ot.endTime) {
      await proxy.$alert(getMessage(MSG.OT_RANGE_INVALID));
      return false;
    }
    const stamp = otRowToStamp(ot);
    if (!stamp) {
      await proxy.$alert(getMessage(MSG.OT_RANGE_INVALID));
      return false;
    }
  }
  // 해당 구간 자신의 등록 가능 범위와 대조 (구간별 분리 검증).
  if (!isSegOtValid(segIdx)) {
    // 범위 외 또는 겹침
    await proxy.$alert(getMessage(MSG.OT_OUTSIDE_ALLOWED));
    return false;
  }
  return true;
};

// prafta-043: 초과근무 유형(OT_TYPE) 전면 파기 — mapOtType 매핑 제거.

// 저장: POST /attd07/update-user-overtime-requests
const fnApproveOvertime = async (segIdx) => {
  // 일용직은 초과근무를 등록할 수 없다(UI 미노출이나 우회 방어·서버도 fail-closed 차단).
  if (isDailyWorker.value) {
    await proxy.$alert("일용직 근로자는 초과근무를 등록할 수 없습니다.");
    return;
  }
  if (await guardClosed()) return;
  if (otSaving.value) return;
  if (!(await validateOtBeforeSave(segIdx))) return;

  const ok = await proxy.$confirm(getMessage(MSG.OT_SAVE_CONFIRM));
  if (!ok) return;

  const r = record.value ?? {};
  const u = userInfo.value ?? {};
  const seg = form.value.segments[segIdx];
  const segNo = segIdx + 1;
  const workYmd = ymdToYmdNum(props.date_p);

  // com-013-06 A: 저장 대상 = 저장 전송 대상(체크된 신규행 + 체크·편집된 기저장행).
  //   - 신규행(otId 없음) → 서버 INSERT.
  //   - 편집된 기저장행(otId 보유) → 서버 in-place UPDATE(otId 동반 전송).
  //   미편집 기저장행은 (삭제 의도 등으로) 체크돼 있어도 제외한다. 삭제는 "초과근무 삭제" 버튼이 별도 EP 로 처리.
  const saveRows = (seg.otList || []).filter(isOtSaveTarget);
  if (!saveRows.length) {
    await proxy.$alert(getMessage(MSG.OT_LIST_EMPTY));
    return;
  }
  const overtimes = saveRows.map((o) => ({
    // prafta-043: 초과근무 유형(otType) 전면 파기 — payload 에서 제거.
    // com-013-06 A: 기저장행은 otId 를 함께 보내 서버가 in-place UPDATE 하도록 한다(신규행은 null/미전달).
    otId: o.otId || null,
    startDate: ymdToYmdNum(o.startDate),
    startTime: o.startTime,
    endDate: ymdToYmdNum(o.endDate),
    endTime: o.endTime,
  }));

  const payload = {
    userCd: props.userCd_p || u.userCd || r.userCd || "",
    siteCd: props.siteCd_p || r.siteCd || "",
    nodeCd: props.nodeCd_p || r.nodeCd || "",
    workYmd,
    attdId: r[`attd${segNo}Id`] || props.attdId_p || null,
    reqId: null,
    overtimes,
    reqReason: form.value.reason || "",
    // 소정-07: 근로자 명시 청구 확인 값. 관리자 직접 등록(reqId=null) 경로에서만 서버가 요구한다.
    //   단축 기간(육아기·가족돌봄) 대상이 아니면 서버가 이 값을 보지 않는다.
    reducedWorkOtClaimYn: otWorkerClaimConfirmed.value ? "Y" : "N",
  };

  otSaving.value = true;
  try {
    const response = await axios.post(
      "/webApi/attd07/update-user-overtime-requests",
      payload
    );
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
      // 저장 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해 데이터를 reload 한다.
      await fnSearch();
    }
  } catch (err) {
    console.error("[AttdDayDetailPop] overtime save failed", err);
    // prafta-008: 백엔드 ApiException 메시지를 그대로 노출(공통 유틸 경유).
    // 백엔드 enum 이 민감 코드에 일반 메시지를 쓰므로 정보 누설 없음.
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.OT_SAVE_ERROR))
    );
  } finally {
    otSaving.value = false;
  }
};

// com-013 #6b: fnRejectOvertime(관리자 직접수정 OT 블록 '반려') 제거.
//   onRejectConfirm 의 kind==="overtime" 분기(Attd_10 인박스 요청 반려)는 보존한다.

// ── 시간 입력 (포커스에 따라 raw "1230" / 표시 "12:30" 전환) ───
const focusedTime = ref(null); // 예: "0-in", "1-out"

const displayTime = (raw, key) =>
  focusedTime.value === key ? raw : fmtRaw(raw);

const onTimeFocus = (key) => {
  focusedTime.value = key;
};
const onTimeBlur = () => {
  focusedTime.value = null;
};
const onTimeInput = (e, seg, field) => {
  const v = sanitizeTime(e.target.value);
  seg[field] = v;
  if (e.target.value !== v) e.target.value = v;
};

// ── 저장 ──────────────────────────────────────────────────
const ymdToYmdNum = (ymd) => (ymd || "").replace(/-/g, "");

// 일자(YYYY-MM-DD) + 시간(HHMM) → 비교 가능한 숫자 (YYYYMMDDHHMM)
const toMinuteStamp = (ymd, hhmm) =>
  parseInt(ymdToYmdNum(ymd) + (hhmm || ""), 10);

// 저장 전 폼 유효성 검사 — 통과 시 true, 실패 시 alert 노출 후 false 반환
const validateForm = async () => {
  // 1. 구간별 출/퇴근 유효성 검사
  for (let i = 0; i < form.value.segments.length; i++) {
    const seg = form.value.segments[i];
    const idx = i + 1;
    if (!seg.startDate || !seg.startTime) {
      await proxy.$alert(getMessage(MSG.SEG_CHECKIN_REQUIRED, { idx }));
      return false;
    }
    if (!seg.endDate || !seg.endTime) {
      await proxy.$alert(getMessage(MSG.SEG_CHECKOUT_REQUIRED, { idx }));
      return false;
    }
    if (seg.startTime.length < 4 || seg.endTime.length < 4) {
      await proxy.$alert(getMessage(MSG.SEG_TIME_FORMAT, { idx }));
      return false;
    }
    if (seg.endDate < seg.startDate) {
      await proxy.$alert(getMessage(MSG.SEG_OUT_DATE_BEFORE_IN, { idx }));
      return false;
    }
    if (seg.endDate === seg.startDate) {
      const inN = parseInt(seg.startTime, 10);
      const outN = parseInt(seg.endTime, 10);
      if (outN <= inN) {
        await proxy.$alert(getMessage(MSG.SEG_OUT_TIME_BEFORE_IN, { idx }));
        return false;
      }
    }
  }

  // 2. 구간 간 검증 — 2구간 출근은 1구간 퇴근 이후여야 함
  // (1구간 범위 내에 들어오거나 1구간보다 빠른 경우 차단)
  if (form.value.segments.length >= 2) {
    const seg1 = form.value.segments[0];
    const seg2 = form.value.segments[1];
    const seg1OutStamp = toMinuteStamp(seg1.endDate, seg1.endTime);
    const seg2InStamp = toMinuteStamp(seg2.startDate, seg2.startTime);
    if (seg2InStamp <= seg1OutStamp) {
      await proxy.$alert(getMessage(MSG.SEG2_IN_AFTER_SEG1_OUT));
      return false;
    }
  }

  return true;
};

const fnSave = async () => {
  if (await guardClosed()) return;
  if (!(await validateForm())) return;

  // payload — 구간(workSeq)별 1건씩 (관리자 등록: method '02' 고정)
  const r = record.value ?? {};
  const u = userInfo.value ?? {};
  const workYmd = ymdToYmdNum(props.date_p);

  // 구간 변경 여부 — oriAct{n}* (서버가 보내준 원본 실제 출퇴근 값) 4개 컬럼 중
  // 하나라도 form 값과 다르면 변경된 것으로 본다.
  // 신규 추가 구간은 oriAct* 가 비어 있으므로 자동으로 "변경"으로 판정됨.
  const isSegmentChanged = (seg, segNo) => {
    const oriInDate = r[`oriAct${segNo}InDate`] ?? "";
    const oriInTime = r[`oriAct${segNo}InTime`] ?? "";
    const oriOutDate = r[`oriAct${segNo}OutDate`] ?? "";
    const oriOutTime = r[`oriAct${segNo}OutTime`] ?? "";
    return (
      ymdToYmdNum(seg.startDate) !== String(oriInDate ?? "") ||
      (seg.startTime || "") !== String(oriInTime ?? "") ||
      ymdToYmdNum(seg.endDate) !== String(oriOutDate ?? "") ||
      (seg.endTime || "") !== String(oriOutTime ?? "")
    );
  };

  const records = form.value.segments
    .map((seg, i) => ({ seg, segNo: i + 1 }))
    .filter(({ seg, segNo }) => isSegmentChanged(seg, segNo))
    .map(({ seg, segNo }) => ({
      siteCd: props.siteCd_p,
      attdId: r[`attd${segNo}Id`] ?? null,
      workYmd,
      nodeCd: props.nodeCd_p || r.nodeCd || "",
      userId: u.userId || "",
      userCd: props.userCd_p || u.userCd || "",
      workSeq: segNo,
      // PRAFTA-003-7: 프론트 내부 변수명은 startDate/startTime/endDate/endTime 으로
      // 정렬되었으나, 백엔드 request DTO 키(checkInDate/Time, checkOutDate/Time)는 그대로 유지.
      checkInDate: ymdToYmdNum(seg.startDate),
      checkInTime: seg.startTime,
      checkInMethod: "02",
      checkOutDate: ymdToYmdNum(seg.endDate),
      checkOutTime: seg.endTime,
      checkOutMethod: "02",
      // 수정 전 실제 출퇴근 값 (구간별 oriAct{n}* — 서버 원본)
      oriCheckInDate: r[`oriAct${segNo}InDate`] ?? "",
      oriCheckInTime: r[`oriAct${segNo}InTime`] ?? "",
      oriCheckOutDate: r[`oriAct${segNo}OutDate`] ?? "",
      oriCheckOutTime: r[`oriAct${segNo}OutTime`] ?? "",
      reason: form.value.reason,
    }));

  // 변경된 구간이 하나도 없으면 저장하지 않음
  if (records.length === 0) {
    await proxy.$alert(getMessage(MSG.DAY_NO_CHANGES));
    return;
  }

  // 저장 확인
  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  // API 호출
  try {
    const response = await axios.post(
      "/webApi/attd07/update-user-attd-infos",
      records
    );
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
      // PRAFTA-COM-013-06-2(r34-1): 저장 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해
      //   신규 데이터를 재조회한다(승인/반려/OT 저장/삭제 핸들러와 동일 패턴 정합).
      await fnSearch();
    }
  } catch (err) {
    console.log("[AttdDayDetailPop] save failed", err);
    const msg = resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR));
    await proxy.$alert(msg);
  }
};

// ── 근로자 요청 카드 액션 ─────────────────────────────────
//  - 승인: POST /attd07/update-user-attd-requests
//  - 반려: POST /attd07/reject-user-attd-requests (반려 사유 입력 모달 경유)
// 승인: confirm → 단일 API 호출로 TB_USER_ATTD_MGMT 갱신 + TB_USER_ATTD_REQ 상태 변경 + HIST 기록
const fnApproveReq = async (card) => {
  if (await guardClosed()) return;
  const ok = await proxy.$confirm(getMessage(MSG.REQ_APPROVE_CONFIRM));
  if (!ok) return;

  const raw = card?.raw ?? {};
  const r = record.value ?? {};
  const n = parseInt(raw.workSeq, 10) || card?.workSeq || 1;

  // [PRAFTA-027] 초과근무 요청(03 생성 / 04 수정)은 OT 승인 엔드포인트로 라우팅한다.
  //   근태 승인 엔드포인트(update-user-attd-requests)는 SEC-018 REQ_TYPE 가드가 있어
  //   OT 요청을 "요청을 처리할 수 없습니다"로 거부한다. (fnRejectReq 의 OT 분기와 동일 취지,
  //   Attd_10 의 OT 승인 payload 형식과 동일.)
  const isOtReq = card?.reqType === "03" || card?.reqType === "04";
  if (isOtReq) {
    const otPayload = {
      reqId: raw.reqId,
      userCd: raw.userCd || props.userCd_p,
      siteCd: raw.siteCd || props.siteCd_p,
      nodeCd: raw.nodeCd || props.nodeCd_p || "",
      workYmd: raw.workYmd || ymdDashToNum(props.date_p),
      reqReason: raw.reqReason || "",
      overtimes: [
        {
          // prafta-043: 초과근무 유형(otType) 전면 파기 — payload 에서 제거.
          startDate: raw.startDate || raw.workYmd || ymdDashToNum(props.date_p),
          startTime: raw.startTime,
          endDate: raw.endDate || raw.workYmd || ymdDashToNum(props.date_p),
          endTime: raw.endTime,
        },
      ],
    };
    try {
      const response = await axios.post(
        "/webApi/attd07/update-user-overtime-requests",
        otPayload
      );
      if (response.status === 200) {
        await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
        await fnSearch();
      }
    } catch (err) {
      console.error("[AttdDayDetailPop] approve OT request failed", err);
      await proxy.$alert(
        resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR))
      );
    }
    return;
  }

  // [PRAFTA-APP-007] 스케줄 수정 요청(10)은 전용 승인 엔드포인트로 라우팅한다.
  //   근태 승인 엔드포인트(update-user-attd-requests)는 SEC-018 가드(01/02)에 막힌다.
  //   목표 스케줄 코드(SCH_CD)는 서버가 REQ row 에서 권위 조회하므로 전송하지 않는다(IDOR/변조 방지).
  const isSchedModifyReq = card?.reqType === "10";
  if (isSchedModifyReq) {
    const payload = {
      reqId: raw.reqId,
      userCd: raw.userCd || props.userCd_p,
      siteCd: raw.siteCd || props.siteCd_p,
      nodeCd: raw.nodeCd || props.nodeCd_p || "",
      workYmd: raw.workYmd || ymdDashToNum(props.date_p),
      workSeq: String(n),
    };
    try {
      const response = await axios.post(
        "/webApi/attd07/approve-sched-modify-requests",
        payload
      );
      if (response.status === 200) {
        await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
        await fnSearch();
      }
    } catch (err) {
      console.error(
        "[AttdDayDetailPop] approve sched-modify request failed",
        err
      );
      await proxy.$alert(
        resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR))
      );
    }
    return;
  }

  // [A] 연차(05 사용 / 06 수정) 요청은 LeaveFlow 결재 엔드포인트로 라우팅한다.
  //   근태 승인 엔드포인트는 SEC-018 가드(01/02)에 막히고, 연차는 다단계 결재 모델이라
  //   현재 로그인 사용자가 처리할 결재 단계(approvalStep)를 함께 전송한다(Attd_10 연차 탭과 동일 payload).
  const isLeaveReq = card?.reqType === "05" || card?.reqType === "06";
  if (isLeaveReq) {
    try {
      const response = await axios.post("/webApi/leaveflow/approve", {
        reqId: raw.reqId,
        approvalStep: card.approvalStep,
        comment: "",
      });
      if (response.status === 200) {
        await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
        await fnSearch();
      }
    } catch (err) {
      console.error("[AttdDayDetailPop] approve leave request failed", err);
      await proxy.$alert(
        resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR))
      );
    }
    return;
  }

  const payload = {
    reqId: raw.reqId,
    attdId: r[`attd${n}Id`] || props.attdId_p || "",
    siteCd: raw.siteCd || props.siteCd_p,
    userCd: raw.userCd || props.userCd_p,
    workYmd: raw.workYmd || ymdDashToNum(props.date_p),
    workSeq: String(n),
    nodeCd: raw.nodeCd || props.nodeCd_p,

    // PRAFTA-003 QA-008: TB_USER_ATTD_REQ가 START_*/END_* 구조로 마이그됐기 때문에
    // raw(req 객체)에는 더 이상 checkInDate/Time 등이 존재하지 않는다. 응답 객체의
    // 신규 키(startDate/startTime/endDate/endTime)에서 값을 가져오되 백엔드 request
    // DTO(UpdateUserAttdRequestRequest) 키는 그대로 유지하여 변환 매핑한다.
    checkInDate: raw.startDate || "",
    checkInTime: raw.startTime || "",
    checkInMethod: r[`act${n}InMethod`] || "02",
    checkOutDate: raw.endDate || "",
    checkOutTime: raw.endTime || "",
    checkOutMethod: r[`act${n}OutMethod`] || "02",

    oriCheckInDate: r[`oriAct${n}InDate`] || "",
    oriCheckInTime: r[`oriAct${n}InTime`] || "",
    oriCheckOutDate: r[`oriAct${n}OutDate`] || "",
    oriCheckOutTime: r[`oriAct${n}OutTime`] || "",

    processComment: getMessage(MSG.REQ_APPROVED_REASON),
  };

  try {
    const response = await axios.post(
      "/webApi/attd07/update-user-attd-requests",
      payload
    );
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
      // 저장 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해 데이터를 reload 한다.
      await fnSearch();
    }
  } catch (err) {
    console.error("[AttdDayDetailPop] approve request failed", err);
    // prafta-008: 백엔드 ApiException 메시지를 그대로 노출(공통 유틸 경유).
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  }
};

// 근로자 요청 반려 — 반려 사유 입력 모달을 띄운다.
//   [QA 재작업 D4] OT 요청(reqType==='03', 초과근무생성)은 근태 반려 endpoint
//   (reject-user-attd-requests)의 SEC-018 REQ_TYPE 가드에 막히므로, OT 요청은
//   kind='overtime' 으로 분기해 onRejectConfirm 에서 reject-user-overtime-requests 로
//   라우팅한다(요청 body 는 OT 반려 DTO 형식 { reqId, siteCd, userCd, rejectReason }).
//   그 외 근태 수정/생성 요청은 기존 kind='attd' 경로를 그대로 사용한다.
//   reqType 은 SYS032 코드값으로 관리된다(PRAFTA-010-2).
const fnRejectReq = (card) => {
  if (isMonthClosed.value) {
    proxy.$alert("마감된 월입니다. 마감 해제 후 수정할 수 있습니다.");
    return;
  }
  const raw = card?.raw ?? {};
  const n = parseInt(raw.workSeq, 10) || card?.workSeq || 1;
  // [8번 보정] OT는 생성(03)·수정(04) 모두 OT 반려 엔드포인트로 라우팅한다.
  //   (기존엔 03만 봐서 04 반려가 근태 반려 엔드포인트의 SEC-018 가드에 막혔다 — 승인은 03·04를 보는데 반려만 03이라 비대칭이었음.)
  const isOtReq = card?.reqType === "03" || card?.reqType === "04";
  // [A] 연차(05 사용 / 06 수정)는 LeaveFlow 반려 엔드포인트로 라우팅(다단계 결재 → approvalStep 동반).
  const isLeaveReq = card?.reqType === "05" || card?.reqType === "06";
  // [PRAFTA-APP-007] 스케줄 수정 요청(10)은 전용 반려 엔드포인트로 라우팅한다.
  const isSchedModifyReq = card?.reqType === "10";
  rejectModal.value = {
    open: true,
    kind: isLeaveReq
      ? "leave"
      : isOtReq
        ? "overtime"
        : isSchedModifyReq
          ? "schedModify"
          : "attd",
    busy: false,
    context: {
      reqId: raw.reqId,
      siteCd: raw.siteCd || props.siteCd_p,
      userCd: raw.userCd || props.userCd_p,
      workYmd: raw.workYmd || ymdDashToNum(props.date_p),
      workSeq: String(n),
      nodeCd: raw.nodeCd || props.nodeCd_p,
      approvalStep: card?.approvalStep,
    },
  };
};

// ── 연차 변경(이동/삭제) 요청 처리 ─────────────────────────
//   신규 EP 없이 attd13 의 기존 엔드포인트를 재사용한다. 대상자 관리 권한·마감 가드(출발일+이동일
//   양쪽)·만료/충돌 재검증·AGREED 상태 가드가 모두 서버(Attd13ServiceImpl)에 있으므로 프론트는
//   호출과 갱신만 담당한다. 실패 메시지는 서버 ApiException 문구를 그대로 노출한다.
const leaveChangeBusy = ref(false);

const fnApproveLeaveChange = async (card) => {
  if (await guardClosed()) return;
  if (leaveChangeBusy.value) return;
  const ok = await proxy.$confirm(
    card.reqType === "MOVE"
      ? `연차를 ${card.toDateLabel} 로 이동하시겠습니까?`
      : "해당 연차를 삭제하시겠습니까? 해당 일자는 근무일로 복귀합니다."
  );
  if (!ok) return;

  leaveChangeBusy.value = true;
  try {
    const response = await axios.post(
      `/webApi/attd13/change-requests/${card.changeReqId}/confirm`
    );
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
      await fnSearch();
    }
  } catch (err) {
    console.error(
      "[AttdDayDetailPop] confirm leave-change request failed",
      err
    );
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  } finally {
    leaveChangeBusy.value = false;
  }
};

// 연차 변경 요청 반려 — 공용 반려 사유 모달(kind='leaveChange')로 사유를 받는다.
const fnRejectLeaveChange = (card) => {
  if (isMonthClosed.value) {
    proxy.$alert("마감된 월입니다. 마감 해제 후 수정할 수 있습니다.");
    return;
  }
  rejectModal.value = {
    open: true,
    kind: "leaveChange",
    busy: false,
    context: { changeReqId: card.changeReqId },
  };
};

// 반려 사유 입력 모달 "확인" 핸들러 — 입력된 사유로 반려 API 호출.
//   기존 승인(fnApproveReq/fnApproveOvertime)의 호출/에러처리 패턴을 따른다.
const onRejectConfirm = async (reason) => {
  if (await guardClosed()) return;
  if (rejectModal.value.busy) return;
  const { kind, context } = rejectModal.value;
  if (!kind || !context) return;

  rejectModal.value.busy = true;
  try {
    if (kind === "attd") {
      await axios.post("/webApi/attd07/reject-user-attd-requests", {
        reqId: context.reqId,
        siteCd: context.siteCd,
        userCd: context.userCd,
        workYmd: context.workYmd,
        workSeq: context.workSeq,
        nodeCd: context.nodeCd,
        rejectReason: reason,
      });
    } else if (kind === "leave") {
      // [A] 연차(05/06) 반려 — LeaveFlow 엔드포인트(다단계 결재). 반려 사유(comment) 서버 필수.
      await axios.post("/webApi/leaveflow/reject", {
        reqId: context.reqId,
        approvalStep: context.approvalStep,
        comment: reason,
      });
    } else if (kind === "leaveChange") {
      // 연차 변경(이동/삭제) 반려 — attd13 기존 EP. 반려 사유(rejectReason) 서버 필수.
      //   원 연차는 불변이고 요청만 REJECTED 로 전이된다.
      //   요청 body 키는 대문자 REJECT_REASON (백엔드 DTO 가 @JsonProperty 로 고정 — LeaveChangeConfirmPop 정합).
      await axios.post(
        `/webApi/attd13/change-requests/${context.changeReqId}/reject`,
        { REJECT_REASON: reason }
      );
    } else if (kind === "schedModify") {
      // [PRAFTA-APP-007] 스케줄 수정 요청(10) 반려 — 전용 엔드포인트. 반려 사유(rejectReason) 서버 필수.
      //   body 의 키 필드는 서버 REQ row 와 일치 검증되며, 스케줄(tb_user_work_plan)은 미반영.
      await axios.post("/webApi/attd07/reject-sched-modify-requests", {
        reqId: context.reqId,
        siteCd: context.siteCd,
        userCd: context.userCd,
        workYmd: context.workYmd,
        workSeq: context.workSeq,
        nodeCd: context.nodeCd,
        rejectReason: reason,
      });
    } else {
      await axios.post("/webApi/attd07/reject-user-overtime-requests", {
        reqId: context.reqId,
        siteCd: context.siteCd,
        userCd: context.userCd,
        rejectReason: reason,
      });
    }
    rejectModal.value = { open: false, kind: null, context: null, busy: false };
    await proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
    // 반려 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해 데이터를 reload 한다.
    await fnSearch();
  } catch (err) {
    console.error("[AttdDayDetailPop] reject request failed", err);
    rejectModal.value.busy = false;
    // 백엔드 ApiException 메시지를 그대로 노출(공통 유틸 경유).
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)));
  }
};

// ── 삭제 ──────────────────────────────────────────────────
// 삭제 진입.
//  - DB에서 읽어온 구간(record.attd{n}Id 존재) → 사유 입력 팝업 → API 호출
//  - 화면상 신규 추가한 구간(attd{n}Id 없음) → 팝업/API 없이 form에서 즉시 제거
const openDeletePopup = (type, segIdx) => {
  if (isMonthClosed.value) {
    proxy.$alert("마감된 월입니다. 마감 해제 후 수정할 수 있습니다.");
    return;
  }
  const r = record.value ?? {};

  if (type === "segment") {
    const segNo = (segIdx ?? 0) + 1;
    const attdId = r[`attd${segNo}Id`];
    if (!attdId) {
      // DB에 없는 신규 구간 — 화면에서만 제거
      form.value.segments.splice(segIdx, 1);
      return;
    }
    deletePopup.value = { open: true, type, segIdx, reason: "" };
    return;
  }

  if (type === "all") {
    // 전체 삭제: attdId 없는 구간들은 화면에서 즉시 제거 (역순 splice로 인덱스 안정화)
    // → 사용자가 사유 팝업에서 취소/X를 눌러도 신규 구간들은 그대로 사라진 상태로 남음
    const segments = form.value.segments || [];
    for (let i = segments.length - 1; i >= 0; i--) {
      if (!r[`attd${i + 1}Id`]) segments.splice(i, 1);
    }
    // record에 attdId가 하나도 없으면 API 대상 자체가 없으므로 팝업 없이 종료
    const hasApiTarget = !!(r.attd1Id || r.attd2Id);
    if (!hasApiTarget) return;
    deletePopup.value = {
      open: true,
      type: "all",
      segIdx: null,
      reason: "",
    };
  }
};

const closeDeletePopup = () => {
  deletePopup.value.open = false;
};

// 삭제 실행 (사유 검증 + API 호출)
//  사유 입력 + "삭제" 버튼 클릭 자체가 명시적 확정 행위이므로 별도 $confirm 없음
const fnDelete = async () => {
  const reason = (deletePopup.value.reason || "").trim();
  if (!reason) {
    await proxy.$alert(getMessage(MSG.REASON_REQUIRED));
    return;
  }

  try {
    if (deletePopup.value.type === "all") {
      await fnDeleteAll(reason);
    } else {
      await fnDeleteSegment(deletePopup.value.segIdx, reason);
    }
    closeDeletePopup();
    await proxy.$alert(getMessage(MSG.DELETE_SUCCESS));
    // 삭제 후 팝업을 닫지 않고 상세 조회 API를 다시 호출해 데이터를 reload 한다.
    await fnSearch();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, getMessage(MSG.DELETE_ERROR));
    await proxy.$alert(msg);
  }
};

// 삭제 API 단일 호출 — payload: { siteCd, attdId, userCd, reason }
const DELETE_URL = "/webApi/attd07/daily-attd-detail-delete";
const callDeleteApi = (attdId, reason) =>
  axios.post(DELETE_URL, {
    siteCd: props.siteCd_p,
    attdId,
    userCd: props.userCd_p,
    reason,
  });

// 전체 삭제 — record에 존재하는 모든 attdId(1구간/2구간) 각각 호출
const fnDeleteAll = async (reason) => {
  const r = record.value ?? {};
  const ids = [r.attd1Id, r.attd2Id].filter(Boolean);
  if (!ids.length) return;

  await Promise.all(ids.map((id) => callDeleteApi(id, reason)));
};

// 구간 삭제 — 해당 구간의 attdId 1건만 호출
const fnDeleteSegment = async (idx, reason) => {
  const r = record.value ?? {};
  const segNo = (idx ?? 0) + 1;
  const attdId = r[`attd${segNo}Id`];
  if (!attdId) return;

  await callDeleteApi(attdId, reason);
};

// ── 일자 상세 / 처리 이력 조회 ──────────────────────────────
// 팝업 오픈 시 1회 호출.
// 1) 우선 Attd_07에서 넘겨준 fallback_p로 사용자/스케줄을 채워둔다.
//    → 응답이 비거나 실패해도 화면이 빈 상태가 되지 않도록.
// 2) 응답이 오면 해당 데이터로 덮어씀.
// 3) finally에서 initForm()을 호출 → 스케줄이 없는 off 케이스에서도
//    우측 패널(관리자 직접 수정)이 기본 09:00→18:00 1구간으로 입력 가능 상태가 됨.
const fnSearch = async () => {
  loading.value = true;
  const f = props.fallback_p ?? {};

  // 응답 전 fallback으로 선세팅
  userInfo.value = {
    userId: f.userId ?? "",
    userCd: f.userCd ?? "",
    userNm: f.userNm ?? "",
    authCd: f.authCd ?? "",
    authNm: f.authNm ?? "",
    employmentType: f.employmentType ?? "",
  };
  record.value = {
    plan1Start: f.plan1Start ?? "",
    plan1End: f.plan1End ?? "",
    plan2Start: f.plan2Start ?? "",
    plan2End: f.plan2End ?? "",
  };
  historyList.value = [];
  reqList.value = [];
  leaveChangeReqs.value = [];
  // PRAFTA-003-7: OT 리스트도 응답 전엔 비워두고, 응답 후 덮어쓴다.
  dailyOvertimeList.value = [];
  // OT 칩 정합: 면제 구간도 응답 전엔 비운다(이전 사용자/일자 값 잔류 방지).
  otLeaveExemptWindows.value = [];
  // PRAFTA-FIXEDOT-2: 고정연장 점유 구간도 동일하게 초기화.
  otFixedOtWindows.value = [];
  // PRAFTA-009 part2: reload 시 외근 GPS 패널을 닫는다(ATTD_ID 가 갱신될 수 있음).
  gpsPanel.value = { segIdx: null, attdId: "", trail: [], loading: false };

  try {
    const response = await axios.get("/webApi/attd07/daily-attd-details", {
      params: {
        attdId: props.attdId_p,
        siteCd: props.siteCd_p,
        userCd: props.userCd_p,
        workYmd: ymdDashToNum(props.date_p),
        nodeCd: props.nodeCd_p,
      },
    });
    if (response.status === 200) {
      console.log(response.data);

      const d = response.data?.dailyAttdDetailsResult ?? {};
      // 응답에 의미 있는 데이터가 있을 때만 덮어씀
      if (d && Object.keys(d).length) {
        if (d.userId || d.userCd || d.userNm) {
          userInfo.value = {
            userId: d.userId,
            userCd: d.userCd,
            userNm: d.userNm ?? userInfo.value.userNm,
            authCd: d.authCd ?? userInfo.value.authCd,
            authNm: d.authNm ?? userInfo.value.authNm,
            employmentType:
              d.employmentType ?? userInfo.value.employmentType ?? "",
          };
        }
        record.value = {
          ...d,
          plan1Start: d.plan1Start || f.plan1Start || "",
          plan1End: d.plan1End || f.plan1End || "",
          plan2Start: d.plan2Start || f.plan2Start || "",
          plan2End: d.plan2End || f.plan2End || "",
        };
      }
      historyList.value = response.data?.dailyAttdDetailHistoryResultList ?? [];
      // PRAFTA-003-7: 백엔드 응답 키를 lowerCamel(`monthlyAttdReqResultList`)로 정규화 완료.
      reqList.value = response.data?.monthlyAttdReqResultList ?? [];
      // PRAFTA-003-7: OT 응답 키도 lowerCamel(`dailyOvertimeResultList`)로 정규화 완료.
      dailyOvertimeList.value = response.data?.dailyOvertimeResultList ?? [];
      // PRAFTA-APP-018-F: 그날 확정 연차 사용내역(자동확정/직접 포함, 미처리 결재대기 제외).
      confirmedLeaves.value = response.data?.confirmedLeaveResultList ?? [];
      // 연차 변경(이동/삭제) 활성 요청. 미수신(구서버)이면 빈 배열 → 카드 미노출(회귀 없음).
      leaveChangeReqs.value = response.data?.leaveChangeReqResultList ?? [];
      // PC-09(N8): 대상 사용자·대상일 기준 개인 분모(분). 미수신(구서버)이면 480 유지.
      convMinutes.value = response.data?.convMinutes ?? 480;
      // 겹침가드 개선: 앞뒤 근무일 근태 구간(당일 구간은 서버가 제외). 미수신이면 빈 배열.
      neighborSegments.value = response.data?.neighborAttdSegmentList ?? [];
      // OT 칩 정합: 연차 면제 구간(서버 검증과 동일 산식). 미수신이면 빈 배열(종전 칩 계산 유지).
      otLeaveExemptWindows.value = response.data?.otLeaveExemptWindowList ?? [];
      // PRAFTA-FIXEDOT-2: 고정연장 점유 구간(서버 산출). 미수신/고정연장 없는 타입이면 빈 배열.
      otFixedOtWindows.value = response.data?.otFixedOtWindowList ?? [];
      // 소정-07 후속: 단축 기간 여부. 미수신(구서버)이면 'Y' 폴백 — 체크박스를 종전처럼 항상 노출.
      reducedWorkYn.value = response.data?.reducedWorkYn ?? "Y";
    }
  } catch (err) {
    // 조회 실패해도 fallback 값으로 화면은 정상 렌더되도록 알림만 띄움
    const msg = resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR));
    await proxy.$alert(msg);
  } finally {
    initForm();
    loading.value = false;
  }
};

onMounted(() => {
  // attdId 유무와 관계없이 항상 상세 조회 API 호출.
  // 스케줄(계획) 값이 응답에 없을 경우 fnSearch 내부에서 fallback_p로 보충한다.
  fnSearch();
});
</script>

<style scoped>
/* ── 백드롭 / 모달 셸 ────────────────────────────────────── */
.a07pop-backdrop {
  background: rgba(17, 24, 39, 0.45);
  padding: 24px;
  overflow-y: auto;
  font-family: "Pretendard", "Apple SD Gothic Neo", "맑은 고딕", sans-serif;
  font-feature-settings: "tnum";
  font-variant-numeric: tabular-nums;
}
.a07pop-modal {
  background: #fff;
  border-radius: 14px;
  width: 1320px;
  max-width: 100%;
  /* 우측 패널(1·2구간 모두 활성) + 처리이력 3행을 담는 고정 높이.
     좌측 근로자 요청 카드 수와 무관하게 팝업 크기 고정. */
  height: 880px;
  max-height: calc(100vh - 48px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.24);
  color: #111827;
  font-size: 14px;
  line-height: 1.5;
}

/* ── 헤더 ─────────────────────────────────────────────────── */
.a07pop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.a07pop-uinfo {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.a07pop-uline1 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.a07pop-uline1 .a07pop-meta {
  color: #6b7280;
  font-weight: 400;
  font-size: 13.5px;
}
.a07pop-uline2 {
  font-size: 12.5px;
  color: #6b7280;
  font-weight: 500;
}
.a07pop-track {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: #dcfce7;
  color: #15803d;
}
.a07pop-close {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.15s;
}
.a07pop-close:hover {
  background: #f9fafb;
  color: #111827;
}

/* ── 로딩 ─────────────────────────────────────────────────── */
.a07pop-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

/* ── 바디 (2-pane) ───────────────────────────────────────── */
/* 좌 480px 고정 + 우 나머지.
   flex/grid stretch 만으로는 자식 콘텐츠가 부모 height 를 초과할 때 자식 height 가
   부모 안에 가둬지지 않는 환경이 있어(콘텐츠 따라 늘어남 → 자식 overflow-y 가 동작
   하지 않고 상위 overflow:hidden 으로 잘려나감), 자식을 position:absolute 로 부모
   안에 강제 inset 시켜 height 를 부모와 동일하게 고정한다. body 는 relative + flex
   grow 로 modal 안의 남은 height 를 정확히 점유한다. */
.a07pop-body {
  flex: 1 1 0;
  min-height: 0;
  position: relative;
  overflow: hidden;
}
.a07pop-pane {
  position: absolute;
  top: 0;
  bottom: 0;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 18px 22px 0px;
}
.a07pop-pane.left {
  left: 0;
  width: 480px;
  border-right: 1px solid #e5e7eb;
  background: #f9fafb;
}
.a07pop-pane.right {
  left: 480px;
  right: 0;
  background: #fff;
}
.a07pop-pane::-webkit-scrollbar {
  width: 8px;
}
.a07pop-pane::-webkit-scrollbar-track {
  background: transparent;
}
.a07pop-pane::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 4px;
}

/* ── 시간 정보 카드 ───────────────────────────────────────── */
.time-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 14px;
}
.time-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: flex-start;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13.5px;
  min-height: 44px;
}
.time-row:last-child {
  border-bottom: none;
  align-items: center;
}
.time-lbl {
  color: #6b7280;
  font-size: 12.5px;
  font-weight: 500;
  padding-top: 2px;
}
.time-val {
  color: #111827;
  font-weight: 700;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.time-val .time-meta {
  margin-left: 4px;
  color: #6b7280;
  font-weight: 400;
  font-size: 12.5px;
}
/* v-html로 삽입되는 요소는 scoped data 속성이 없어 :deep()으로 감싸야 함 */
:deep(.dt-block) {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 5px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  line-height: 1.15;
  vertical-align: middle;
}
:deep(.dt-date) {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  letter-spacing: 0.04em;
}
:deep(.dt-time) {
  font-size: 15px;
  color: #111827;
  font-weight: 800;
  letter-spacing: 0.02em;
}
:deep(.dt-arrow) {
  display: inline-flex;
  align-items: center;
  margin: 0 10px;
  color: #9ca3af;
  font-weight: 600;
  font-size: 15px;
  /* '~' 분리기호가 시간 블록 가운데에 자연스럽게 정렬되도록 함 */
  vertical-align: middle;
}
.val-empty {
  color: #9ca3af;
  font-weight: 400;
}
.val-missing {
  color: #9ca3af;
  font-weight: 600;
}
.value-leave {
  color: #374151;
  font-weight: 700;
}
.value-off {
  color: #374151;
  font-weight: 700;
}
.biko-out {
  color: #b91c1c;
  font-weight: 700;
}
/* PRAFTA-FIXEDOT-3: "연장 미이행" 배지 — 조퇴 표기와 다른 별도 축(경고 토큰만 사용, 하드코딩 금지) */
.fixedot-unmet {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.note-pending {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}
.note-ref {
  color: #6b7280;
  font-weight: 500;
  font-size: 12px;
  margin-left: 8px;
}
.val-meta-warn {
  color: #6b7280;
  font-weight: 500;
  font-size: 12px;
  margin-left: 8px;
}
.out-pill {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  background: #fee2e2;
  color: #b91c1c;
  line-height: 1.4;
}

/* 구간 표시 (A-9) */
.seg-multi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.seg-line {
  display: grid;
  grid-template-columns: 50px 1fr;
  align-items: center;
  gap: 10px;
}
.seg-tag {
  font-size: 11px;
  font-weight: 800;
  color: #6b7280;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 2px 6px;
  border-radius: 4px;
  text-align: center;
}
/* 실제 출퇴근 줄 — 시간 범위 + 외근 버튼을 한 줄에 배치 (PRAFTA-009 part2) */
.seg-line-body {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
/* 외근 버튼 — 1구간/2구간 태그(seg-tag)와 동일 룩. 클릭 가능하도록 보강. */
.seg-tag-outside {
  cursor: pointer;
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fecaca;
  transition:
    background 0.15s,
    color 0.15s;
}
.seg-tag-outside:hover {
  background: #fecaca;
}
.seg-tag-outside.is-active {
  color: #fff;
  background: #b91c1c;
  border-color: #b91c1c;
}
/* 외근 GPS 동선 패널 행 — 시간 카드 내부 별도 행 */
.gps-panel-row {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}
.nextday-mark {
  color: #6b7280;
  font-weight: 600;
  font-size: 12.5px;
}

/* ── 보조 블록 ────────────────────────────────────────────── */
.empty-hint {
  margin-bottom: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  text-align: center;
  color: #6b7280;
  font-size: 12.5px;
  line-height: 1.55;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.empty-hint strong {
  color: #374151;
}
.hint-icon {
  color: #9ca3af;
}
.work-notice {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #fff;
  color: #6b7280;
  font-size: 12.5px;
  line-height: 1.5;
  display: flex;
  align-items: center;
  gap: 8px;
}
.work-notice .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #16a34a;
  flex-shrink: 0;
  animation: a07-pulse 1.6s ease-in-out infinite;
}
@keyframes a07-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

/* ── 근로자 요청 카드 ──────────────────────────────────────── */
.req-section {
  margin-top: 14px;
}
.req-section-head {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 0 2px 8px;
}
.req-section-head h3 {
  font-size: 13.5px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.req-section-head .req-count {
  font-size: 12px;
  color: #6b7280;
}
.req-card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.req-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}
.req-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.req-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 11.5px;
  font-weight: 600;
}
.req-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4f46e5;
}
/* 연차 변경 요청 중 아직 처리할 수 없는 단계(REQUESTED — 근로자 동의 대기) 배지.
   처리 가능(AGREED)한 카드와 색으로 구분해 오조작을 줄인다. */
.req-badge--wait {
  background: #f3f4f6;
  color: #6b7280;
}
.req-badge--wait .dot {
  background: #9ca3af;
}
.req-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #111827;
}
.req-card-sub {
  font-size: 11.5px;
  color: #9ca3af;
  margin-bottom: 10px;
}
.req-diff {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f1f3f5;
}
.req-diff-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.req-diff-head {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #9ca3af;
  margin-bottom: 2px;
}
.req-diff-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12.5px;
}
.req-diff-lbl {
  color: #6b7280;
}
.req-diff-val {
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
}
.req-diff-val.is-changed {
  color: #2563eb;
}
.req-diff-arrow {
  color: #9ca3af;
  font-size: 14px;
}
/* com-013 #2: 스케줄 수정(10) 카드 전용 — BEFORE→화살표→AFTER 세로 풀폭 스택.
   2구간 스케줄 문자열("09:00~18:00 / 19:00~22:00 (2구간)")이 좁은 칼럼에서
   줄바꿈/말줄임되지 않도록 단일 컬럼 풀폭으로 전환한다(정보 손실 금지). */
.req-diff--sched {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}
.req-diff--sched .req-diff-col {
  width: 100%;
}
.req-diff--sched .req-diff-row {
  /* 라벨(스케줄)과 값을 양끝 배치하되, 값이 길면 전체 폭을 쓰며 한 줄 유지. */
  gap: 10px;
}
.req-diff--sched .req-diff-val {
  /* 스케줄 한 줄 문자열은 개행시키지 않는다(말줄임 없이 전체 표시). */
  white-space: nowrap;
  text-align: right;
}
/* 세로 스택에서는 화살표를 아래 방향으로 회전해 흐름을 명확히 한다. */
.req-diff--sched .req-diff-arrow {
  align-self: center;
  transform: rotate(90deg);
}
/* PRAFTA-APP-018-D: 연차(05/06) 전용 1줄 표시 (사용단위·범위·차감일수, 가운데점 구분) */
.req-leave-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--bg-subtle, var(--color-surface, #f9fafb));
  border: 1px solid var(--color-border, #f1f3f5);
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-text-strong, var(--color-text, #111827));
  font-variant-numeric: tabular-nums;
}
/* 연차(05/06) 카드 전용 — 자식이 req-leave-line 하나뿐이라 3트랙 그리드(1fr auto 1fr)의
   첫 칸만 채워 카드가 반쪽으로 보였다. 풀폭 단일 컬럼으로 전환한다(정보 손실 없음).
   겉 컨테이너의 배경/보더는 안쪽 req-leave-line 과 이중이 되므로 제거한다. */
.req-diff--leave {
  display: block;
  padding: 0;
  background: none;
  border: none;
}
.req-diff--leave .req-leave-line {
  width: 100%;
}
.req-leave-seg + .req-leave-seg::before {
  content: "·";
  margin-right: 6px;
  color: var(--color-text-muted, #9ca3af);
  font-weight: 400;
}
/* 가불표시-06: 가불 배지 세그먼트 — 경고(warning) 시맨틱 토큰(tokens.css) 재사용.
   칩 형태라 세그먼트 구분점(·)은 배경 안에 들어가지 않도록 제거한다. */
.req-leave-seg--borrow {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  border-radius: 4px;
  padding: 1px 6px;
}
.req-leave-seg + .req-leave-seg--borrow::before {
  content: none;
}
/* PRAFTA-APP-018-F: 확정 연차 사용 섹션 (요청 카드와 구분, 표시 전용) */
.leave-use-section {
  margin-top: 14px;
}
.leave-use-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}
/* 연차 변경 요청 카드의 연차 상세 1줄 — BEFORE/AFTER 블록 아래에 붙인다. */
.lc-detail-line {
  margin-top: 8px;
}
/* 근로자 동의 대기(REQUESTED) 카드의 액션 자리 안내문. */
.lc-wait-hint {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--bg-subtle, #f9fafb);
  border: 1px dashed var(--color-border, #e5e7eb);
  font-size: 11.5px;
  color: var(--color-text-muted, #9ca3af);
  text-align: center;
}
.req-reason-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
}
.req-reason-lbl {
  color: #6b7280;
  font-weight: 600;
  flex-shrink: 0;
}
.req-card-actions {
  display: flex;
  gap: 6px;
  margin-top: 12px;
}
.req-btn {
  flex: 1;
  height: 34px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.req-btn-approve {
  background: #16a34a;
  color: #fff;
}
.req-btn-approve:hover {
  background: #15803d;
}
.req-btn-reject {
  background: #fff;
  border-color: #fecaca;
  color: #dc2626;
}
.req-btn-reject:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 2px 10px;
}
.section-title h4 {
  margin: 0;
  font-size: 12px;
  font-weight: 800;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.section-title .count {
  color: #9ca3af;
  font-weight: 700;
  margin-left: 4px;
}

.info-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 14px;
}

/* 위치 카드 (A-7a) */
.map-area {
  background: linear-gradient(180deg, #f0f9ff 0%, #ecfdf5 100%);
  height: 160px;
  border-bottom: 1px solid #e5e7eb;
  overflow: hidden;
}
.map-svg {
  width: 100%;
  height: 100%;
  display: block;
}
.loc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.loc-row {
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: grid;
  grid-template-columns: 38px 1fr auto;
  align-items: center;
  gap: 10px;
}
.loc-row:last-child {
  border-bottom: none;
}
.loc-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 24px;
  border-radius: 6px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 11px;
  font-weight: 800;
}
.loc-coord {
  font-size: 12.5px;
  color: #374151;
  font-weight: 600;
}
.loc-coord .sub {
  color: #6b7280;
  font-weight: 400;
}
.loc-distance {
  font-size: 13px;
  color: #b91c1c;
  font-weight: 800;
}
.reason-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.reason-row {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}
.reason-row:last-child {
  border-bottom: none;
}
.reason-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.reason-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: #f9fafb;
  color: #374151;
}
.reason-time {
  font-size: 12px;
  color: #6b7280;
}
.reason-body {
  font-size: 13px;
  line-height: 1.55;
  color: #374151;
}

/* 휴가 정보 (A-13) */
.kv-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.kv-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  min-height: 42px;
}
.kv-row:last-child {
  border-bottom: none;
}
.kv-k {
  color: #6b7280;
  font-size: 12.5px;
  font-weight: 500;
}
.kv-v {
  color: #111827;
  font-weight: 700;
}
.kv-v.kv-reason {
  font-weight: 500;
  line-height: 1.55;
  color: #374151;
}

/* 로그 리스트 (A-12) */
.log-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.log-row {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
}
.log-row:last-child {
  border-bottom: none;
}
.log-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: #f9fafb;
  color: #374151;
  border: 1px solid #d1d5db;
}
.log-row.is-dup .log-tag {
  color: #6b7280;
}
.log-time {
  font-weight: 700;
  color: #111827;
}
.log-meta {
  font-size: 12px;
  color: #6b7280;
}
.log-row.is-dup .log-time {
  color: #6b7280;
  text-decoration: line-through;
}
.dup-pill {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  margin-left: 8px;
}

/* 근로자 요청 카드 */
.request-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 14px;
}
.req-head {
  padding: 12px 16px 8px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}
.req-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  margin-right: 8px;
  line-height: 1.4;
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}
.req-tag .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.req-title {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.req-title-text {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
}
.req-meta {
  font-size: 12px;
  color: #6b7280;
}
.req-body {
  padding: 12px 16px 16px;
}
.ba-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: center;
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.ba-col-label {
  font-size: 10.5px;
  font-weight: 800;
  color: #9ca3af;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}
.ba-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 0;
  font-size: 13px;
}
.ba-key {
  color: #6b7280;
  font-size: 12px;
}
.ba-val {
  font-weight: 700;
}
.ba-col.after .ba-val {
  color: #111827;
  font-weight: 800;
}
.ba-arrow {
  color: #9ca3af;
  width: 24px;
  display: flex;
  justify-content: center;
}
.ot-summary {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 14px;
  background: #f9fafb;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.ot-summary .ot-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: 0.04em;
}
.ot-summary .ot-range {
  font-size: 14px;
  color: #374151;
  font-weight: 700;
}
.ot-summary .ot-value {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
  line-height: 1;
}
.req-reason {
  background: #f9fafb;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 14px;
  font-size: 13px;
  line-height: 1.55;
  color: #374151;
}
.req-reason .lab {
  font-weight: 700;
  color: #6b7280;
  margin-right: 6px;
}
.req-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

/* 공통 버튼 */
.btn {
  height: 40px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #111827;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn:hover {
  background: #f9fafb;
}
.btn.btn-primary {
  background: #16a34a;
  color: #fff;
  border-color: #16a34a;
}
.btn.btn-primary:hover {
  background: #15803d;
}
.btn.btn-danger {
  color: #ef4444;
  border-color: #d1d5db;
}
.btn.btn-danger:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

/* ── 액션 카드 (A-7a) ────────────────────────────────────── */
.action-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-left: 3px solid #ef4444;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 14px;
}
.action-head {
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fef2f2;
}
.action-head h3 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #b91c1c;
}
.action-desc {
  font-size: 12.5px;
  color: #374151;
  line-height: 1.5;
}
.action-body {
  padding: 14px 16px;
}
.action-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

/* ── 직접 수정 패널 ───────────────────────────────────────── */
.panel-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 14px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  cursor: pointer;
  user-select: none;
}
.panel-head:hover {
  background: #f9fafb;
}
.panel-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
}
.panel-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-head .chev {
  transition: transform 0.2s;
  color: #6b7280;
}
.panel-card.is-open .chev {
  transform: rotate(180deg);
}
.btn-del-all,
.btn-clear-all {
  background: #fff;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 700;
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-del-all:hover,
.btn-clear-all:hover {
  background: #fef2f2;
  border-color: #f87171;
}
/* 초기화 — 파괴적 동작이 아니므로 중립(회색) 톤 */
.btn-reset-all {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #4b5563;
  font-size: 12.5px;
  font-weight: 700;
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-reset-all:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}
.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  font-size: 11.5px;
  color: #6b7280;
  font-weight: 600;
}
.panel-body {
  padding: 16px;
  position: relative;
  display: none;
}
.panel-card.is-open .panel-body {
  display: block;
}
/* 구간/초과근무가 많아질 때 이 영역만 자체 스크롤. 사유/저장 영역은 panel-body
   안의 별도 블록이라 스크롤 위치와 관계없이 항상 노출된다. */
.seg-list-scroll {
  max-height: calc(100vh - 460px);
  min-height: 200px;
  overflow-y: auto;
  padding-right: 4px;
  margin-right: -4px;
}
.panel-card .panel-readonly-body {
  display: none;
}
.panel-card.is-open .panel-readonly-body {
  display: block;
}
.panel-body.is-locked {
  opacity: 0.5;
  pointer-events: none;
}
.form-row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.form-row.is-textarea {
  align-items: flex-start;
}
.form-row label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.form-row label.required::after {
  content: "*";
  color: #ef4444;
  margin-left: 2px;
}
.input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  font-size: 13.5px;
  color: #111827;
  font-family: inherit;
  font-feature-settings: "tnum";
  font-variant-numeric: tabular-nums;
}
.input::placeholder {
  color: #9ca3af;
}
.input:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 3px #dcfce7;
}
.input:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}
.input.input-needs {
  border-color: #9ca3af;
  background: #f9fafb;
}
textarea.input {
  height: auto;
  padding: 10px 12px;
  line-height: 1.5;
  resize: vertical;
  min-height: 76px;
  font-feature-settings: normal;
  font-variant-numeric: normal;
}
.input-wrap {
  position: relative;
}
.input-hint {
  margin-top: 4px;
  font-size: 11.5px;
  color: #6b7280;
  font-weight: 500;
}
.form-hint {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  font-size: 12px;
  line-height: 1.55;
  color: #374151;
}
.save-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 8px;
  background: #dcfce7;
  color: #15803d;
  font-weight: 800;
  font-size: 14px;
  cursor: not-allowed;
  opacity: 0.7;
  font-family: inherit;
}
.save-btn:not(:disabled) {
  background: #16a34a;
  color: #fff;
  cursor: pointer;
  opacity: 1;
}
.save-btn:not(:disabled):hover {
  background: #15803d;
}

/* ── 구간 입력 (박스형) ─────────────────────────────────── */
.seg-section {
  margin-bottom: 18px;
  padding: 14px 16px 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}
.seg-section:last-of-type {
  margin-bottom: 18px;
}
.seg-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.seg-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.seg-tag-lg {
  font-size: 11px;
  font-weight: 800;
  color: #15803d;
  background: #dcfce7;
  padding: 3px 8px;
  border-radius: 4px;
  letter-spacing: 0.02em;
}
.seg-summary {
  font-size: 12.5px;
  color: #6b7280;
  font-weight: 600;
}
:deep(.seg-summary strong) {
  color: #111827;
  font-weight: 800;
  margin-left: 2px;
}
:deep(.seg-summary .dot) {
  color: #d1d5db;
  margin: 0 4px;
}
.seg-section-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
/* 구간 삭제 — 휴지통 아이콘 버튼 (초과근무 ot-delete와 동일한 룩) */
.seg-delete {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid transparent;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
}
.seg-delete:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* 정규근무 한 줄 (출근/퇴근) */
.reg-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  flex-wrap: wrap;
}
.time-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.time-input-group .lab {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-left: 30px;
  margin-right: 10px;
}
.reg-row .seg-date {
  flex: 0 0 130px;
  min-width: 0;
}
.reg-row :deep(.calendar-input) {
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  color: #111827;
  background: #fff;
}
.reg-row :deep(.calendar-input:focus) {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 3px #dcfce7;
}
.reg-row .seg-time {
  width: 90px;
  height: 36px;
  flex-shrink: 0;
  text-align: center;
  letter-spacing: 0.06em;
  font-size: 13px;
}

.overday-hint {
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 11.5px;
  font-weight: 500;
}

/* ── 초과근무 블록 ──────────────────────────────────────── */
.ot-block {
  margin-top: 12px;
}
.ot-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 2px 8px;
}
.ot-block-title {
  font-size: 12px;
  font-weight: 800;
  color: #374151;
  letter-spacing: 0.02em;
}
.ot-list {
  list-style: none;
  margin: 0 0 8px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
/* 초과근무 행 — 정규근무 reg-row 와 동일한 입력 컴포넌트/텍스트 크기 적용. */
.ot-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 14px;
  flex-wrap: wrap;
}
.ot-row .ot-date {
  flex: 0 0 130px;
  min-width: 0;
}
.ot-row :deep(.calendar-input) {
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  color: #111827;
  background: #fff;
}
.ot-row :deep(.calendar-input:focus) {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 3px #dcfce7;
}
.ot-row .ot-time {
  width: 90px;
  height: 36px;
  flex-shrink: 0;
  text-align: center;
  letter-spacing: 0.06em;
  font-size: 13px;
  padding: 0 10px;
}
/* com-016-E: 행 좌측 선택 체크박스(우측 휴지통 ot-delete 대체). */
.ot-row .ot-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  cursor: pointer;
}
.ot-row .ot-check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.add-ot-btn {
  width: 100%;
  height: 34px;
  border: 1px dashed #d1d5db;
  background: #fff;
  color: #6b7280;
  font-size: 12.5px;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.add-ot-btn:hover {
  background: #f0fdf4;
  border-color: #16a34a;
  color: #15803d;
}

.seg-add-btn {
  width: 100%;
  padding: 9px 12px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  color: #6b7280;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.seg-add-btn:hover {
  background: #f9fafb;
  border-color: #16a34a;
  color: #15803d;
}

.reason-section {
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

/* 읽기 전용 패널 */
.panel-readonly-body {
  padding: 24px 16px;
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}
.panel-readonly-body strong {
  color: #374151;
}
.ro-icon {
  color: #9ca3af;
  margin-bottom: 8px;
}
.ext-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  padding: 8px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ext-link:hover {
  background: #f9fafb;
}

/* ── 처리 이력 ────────────────────────────────────────────── */
/* 우측 pane 안의 토글 버튼 — 클릭 시 별도 팝업으로 처리 이력 표시 */
.history-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 16px;
  margin-top: 5px;
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  font-family: inherit;
  color: #111827;
  transition: background 0.15s;
}
.history-toggle-btn:hover {
  background: #f9fafb;
}
.history-toggle-label {
  font-size: 14px;
  font-weight: 800;
}
.history-toggle-label .count {
  color: #6b7280;
  font-weight: 600;
  margin-left: 4px;
}
.history-toggle-btn svg {
  color: #6b7280;
}
.hist-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: auto;
}
.hist-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
  table-layout: fixed;
}
.hist-table th,
.hist-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #eef0f3;
  border-right: 1px solid #eef0f3;
  text-align: center;
  vertical-align: middle;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #111827;
}
.hist-table th:last-child,
.hist-table td:last-child {
  border-right: none;
}
.hist-table thead th {
  background: #f9fafb;
  font-weight: 700;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
}
.hist-table tbody tr:last-child td {
  border-bottom: none;
}
.hist-table tbody tr:hover td {
  background: #fafbfc;
}
.hist-table .cell-type {
  font-weight: 600;
}
.hist-table .cell-time {
  font-variant-numeric: tabular-nums;
  color: #374151;
}
.hist-empty {
  padding: 18px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 12.5px;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
}
.hist-reason-btn {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}
.hist-reason-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}
.reason-view {
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 90px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafafa;
  color: #111827;
  font-size: 13.5px;
  line-height: 1.6;
}

/* ── 삭제 사유 입력 팝업 ────────────────────────────────── */
.del-pop-backdrop,
.hist-pop-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  /* 프레임워크 nested overlay와 동일 레이어 (flatpickr/month-day-picker와 동급) */
  z-index: 10001;
  font-family: "Pretendard", "Apple SD Gothic Neo", "맑은 고딕", sans-serif;
}
/* 사유 보기 팝업은 처리 이력 팝업(hist-pop-backdrop) 내부의 "보기" 버튼에서
   열리므로, 처리 이력 팝업보다 한 단계 위 레이어에 떠야 한다. */
.reason-pop-backdrop {
  z-index: 10002;
}
.hist-pop {
  background: #fff;
  border-radius: 12px;
  width: 1130px;
  max-width: calc(100% - 32px);
  max-height: calc(100vh - 64px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
}
.hist-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e5e7eb;
}
.hist-pop-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #111827;
}
.hist-pop-head .count {
  color: #6b7280;
  font-weight: 600;
  margin-left: 4px;
  font-size: 13px;
}
.hist-pop-body {
  padding: 18px;
  overflow-y: auto;
  flex: 1 1 auto;
  min-height: 0;
}
.del-pop {
  background: #fff;
  border-radius: 12px;
  width: 420px;
  max-width: calc(100% - 32px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
}
.del-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e5e7eb;
}
.del-pop-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #111827;
}
.del-pop-close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: #6b7280;
  cursor: pointer;
  border-radius: 6px;
}
.del-pop-close:hover {
  background: #f3f4f6;
  color: #111827;
}
.del-pop-body {
  padding: 18px;
}
.del-pop-msg {
  font-size: 13px;
  color: #374151;
  margin-bottom: 12px;
  line-height: 1.5;
}
.del-pop-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  margin-bottom: 6px;
}
.del-pop-label .req {
  color: #ef4444;
  margin-left: 2px;
}
.del-pop-textarea {
  width: 100%;
  min-height: 90px;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}
.del-pop-textarea:focus {
  outline: none;
  border-color: #ef4444;
  box-shadow: 0 0 0 3px #fee2e2;
}
.del-pop-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 18px;
  border-top: 1px solid #e5e7eb;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
}
.del-pop-foot .btn-cancel {
  height: 36px;
  padding: 0 16px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.del-pop-foot .btn-cancel:hover {
  background: #f9fafb;
}
.del-pop-foot .btn-danger {
  height: 36px;
  padding: 0 18px;
  border-radius: 6px;
  border: 1px solid #dc2626;
  background: #dc2626;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.del-pop-foot .btn-danger:hover:not(:disabled) {
  background: #b91c1c;
  border-color: #b91c1c;
}
.del-pop-foot .btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── transition ─────────────────────────────────────────── */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* PRAFTA-003 F1 — 초과근무 허용 범위 안내 */
.ot-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--header-gap);
}

.ot-allowed-hint {
  display: flex;
  align-items: center;
  gap: var(--header-right-gap);
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.ot-allowed-hint.is-empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.ot-allowed-lbl {
  color: var(--color-text);
  font-weight: 600;
}

.ot-allowed-list {
  display: flex;
  align-items: center;
  gap: var(--header-right-gap);
  margin: 0;
  padding: 0;
  list-style: none;
}

.ot-allowed-item {
  padding: 0 var(--header-right-gap);
  border: var(--card-border);
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  color: var(--color-text);
  line-height: var(--btn-height-sm);
  font-size: var(--btn-font-sm);
  font-family: inherit;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;
}

.ot-allowed-item:hover {
  background: var(--color-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.ot-allowed-item:active {
  transform: translateY(1px);
}

/* 소정-07 — 단축근무자 연장근로 명시 청구 확인 체크 영역 */
.ot-claim-row {
  display: flex;
  flex-direction: column;
  gap: var(--header-right-gap);
  margin-top: var(--header-right-gap);
}

.ot-claim-label {
  display: flex;
  align-items: center;
  gap: var(--header-right-gap);
  font-size: var(--btn-font-sm);
  color: var(--color-text);
  cursor: pointer;
}

.ot-claim-help {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* PRAFTA-003 F1 — 초과근무 저장 액션 영역 (com-013 #6b: 반려 버튼/스타일 제거) */
.ot-actions {
  display: flex;
  gap: var(--header-right-gap);
  justify-content: flex-end;
  margin-top: var(--header-right-gap);
}

.ot-save-btn {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  border-radius: var(--btn-radius);
  border: var(--card-border);
  font-size: var(--btn-font);
  cursor: pointer;
}

.ot-save-btn {
  background: var(--color-primary);
  color: var(--color-surface);
  border-color: var(--color-primary);
}

.ot-save-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.ot-save-btn:active:not(:disabled) {
  background: var(--color-primary-pressed);
  border-color: var(--color-primary-pressed);
}

.ot-save-btn:disabled {
  background: var(--color-border-strong);
  border-color: var(--color-border-strong);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

/* com-016-E: "초과근무 삭제" — outline danger 보조 버튼. */
.ot-delete-btn {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  border-radius: var(--btn-radius);
  border: 1px solid var(--color-danger);
  background: var(--color-surface);
  color: var(--color-danger);
  font-size: var(--btn-font);
  cursor: pointer;
}

.ot-delete-btn:hover:not(:disabled) {
  background: var(--color-danger);
  color: var(--color-surface);
}

.ot-delete-btn:disabled {
  border-color: var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: not-allowed;
}
</style>
