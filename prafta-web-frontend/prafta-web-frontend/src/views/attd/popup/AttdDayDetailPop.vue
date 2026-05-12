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
                <div class="time-lbl">스케줄 (계획)</div>
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
                        <span v-html="s.range"></span>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <span
                      v-html="cfg.timeCard.actual.value"
                      :class="{ 'val-empty': cfg.timeCard.actual.empty }"
                    ></span>
                  </template>
                </div>
              </div>
              <!-- 표준화 적용 -->
              <div class="time-row">
                <div class="time-lbl">표준화 적용</div>
                <div class="time-val">
                  <template v-if="cfg.timeCard.std.segments">
                    <div class="seg-multi">
                      <div
                        v-for="(s, i) in cfg.timeCard.std.segments"
                        :key="i"
                        class="seg-line"
                      >
                        <span class="seg-tag">{{ s.tag }}</span>
                        <span v-html="s.range"></span>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <span
                      v-html="cfg.timeCard.std.value"
                      :class="{ 'val-empty': cfg.timeCard.std.empty }"
                    ></span>
                  </template>
                </div>
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

            <!-- 근로자 요청 카드 리스트 -->
            <div v-if="reqCards.length" class="req-section">
              <div class="req-section-head">
                <h3>근로자 요청</h3>
                <span class="req-count">({{ reqCards.length }})</span>
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

                  <div class="req-diff">
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

                  <div class="req-card-actions">
                    <button
                      type="button"
                      class="req-btn req-btn-approve"
                      @click="fnApproveReq(card)"
                    >
                      승인
                    </button>
                    <button
                      type="button"
                      class="req-btn req-btn-edit"
                      @click="fnDirectEditReq(card)"
                    >
                      직접수정(승인)
                    </button>
                    <button
                      type="button"
                      class="req-btn req-btn-reject"
                      @click="fnRejectReq(card)"
                    >
                      반려
                    </button>
                  </div>
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
                <div class="panel-head-right">
                  <button
                    v-if="cfg.panel.kind === 'segments'"
                    type="button"
                    class="btn-del-all"
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
                <!-- 구간 입력 -->
                <div
                  v-for="(seg, i) in form.segments"
                  :key="i"
                  class="seg-section"
                >
                  <div class="seg-section-head">
                    <span class="seg-label">{{ i + 1 }}구간</span>
                    <div class="seg-section-head-actions">
                      <button
                        class="seg-del-btn"
                        type="button"
                        @click="openDeletePopup('segment', i)"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                  <div class="seg-inline-row">
                    <label class="seg-inline-label">출근</label>
                    <div class="datetime-wrap">
                      <CalendarSrch v-model="seg.inDate" class="seg-date" />
                      <input
                        type="text"
                        inputmode="numeric"
                        class="input seg-time"
                        :value="displayTime(seg.in, `${i}-in`)"
                        :maxlength="focusedTime === `${i}-in` ? 4 : 5"
                        placeholder="HHMM"
                        @focus="onTimeFocus(`${i}-in`)"
                        @blur="onTimeBlur"
                        @input="onTimeInput($event, seg, 'in')"
                      />
                    </div>
                    <label class="seg-inline-label">퇴근</label>
                    <div class="datetime-wrap">
                      <CalendarSrch v-model="seg.outDate" class="seg-date" />
                      <input
                        type="text"
                        inputmode="numeric"
                        class="input seg-time"
                        :value="displayTime(seg.out, `${i}-out`)"
                        :maxlength="focusedTime === `${i}-out` ? 4 : 5"
                        placeholder="HHMM"
                        @focus="onTimeFocus(`${i}-out`)"
                        @blur="onTimeBlur"
                        @input="onTimeInput($event, seg, 'out')"
                      />
                    </div>
                  </div>
                  <div v-if="isOverday(seg)" class="overday-hint">
                    출퇴근 날짜가 상이합니다. 익일 퇴근, Overnight 근무로
                    처리됩니다.
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
                  <button class="save-btn" :disabled="!canSave" @click="fnSave">
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

            <!-- 처리 이력 -->
            <div class="history-card" :class="{ 'is-open': !historyOpen }">
              <div class="history-head" @click="historyOpen = !historyOpen">
                <h3>
                  처리 이력
                  <span class="count">({{ cfg.history.length }})</span>
                </h3>
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
              <div class="history-body">
                <div v-if="cfg.history.length" class="hist-table-wrap">
                  <table class="hist-table">
                    <colgroup>
                      <col style="width: 110px" />
                      <col style="width: 70px" />
                      <col style="width: 130px" />
                      <col style="width: 130px" />
                      <col style="width: 130px" />
                      <col style="width: 130px" />
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
                        <td class="cell-time">{{ h.befCheckIn }}</td>
                        <td class="cell-time">{{ h.befCheckOut }}</td>
                        <td class="cell-time">{{ h.aftCheckIn }}</td>
                        <td class="cell-time">{{ h.aftCheckOut }}</td>
                        <td>
                          <button
                            class="hist-reason-btn"
                            type="button"
                            @click="openReasonPopup(h.reason)"
                          >
                            보기
                          </button>
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
            </div>
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
            <button class="btn-cancel" type="button" @click="closeDeletePopup">
              취소
            </button>
            <button
              class="btn-danger"
              type="button"
              :disabled="!deletePopup.reason.trim()"
              @click="fnDelete"
            >
              삭제
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
        class="del-pop-backdrop prafta-nested-modal-overlay"
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
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";

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
});

const emit = defineEmits(["close", "saved"]);
const { proxy } = getCurrentInstance();

const onClose = () => emit("close");
const historyOpen = ref(true);
// 관리자 직접 수정 패널 접기/펼치기 — 기본 열림
const panelOpen = ref(true);

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

// API 결과 보관용 — 응답 전에는 비어 있음
const loading = ref(true);
const record = ref({}); // 일자 근태 상세 (plan/act/leave …)
const userInfo = ref({}); // 사용자 정보 (헤더용)
const historyList = ref([]); // 처리 이력
const reqList = ref([]); // 근로자 요청 (monthlyAttdReqResultList)

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

// ── 헤더 ──────────────────────────────────────────────────
// 본 팝업은 정규직 전용이라 트랙 라벨은 고정값("정규") 사용
const headerUser = computed(() => {
  const u = userInfo.value || {};
  const role = u.authNm || u.authCd || u.role || "";
  return {
    name: u.userNm || u.name || "—",
    track: "정규",
    deptRole: role ? `· ${role}` : "",
  };
});

const headerDate = computed(() => {
  if (!props.date_p) return "";
  const [y, m, d] = props.date_p.split("-").map(Number);
  const dow = props.dow_p >= 0 ? props.dow_p : new Date(y, m - 1, d).getDay();
  return `${y}.${String(m).padStart(2, "0")}.${String(d).padStart(2, "0")} (${dowLabels[dow]})`;
});

// "0000"은 백엔드에서 빈 plan을 의미하는 sentinel — 의미 있는 시간으로 취급하지 않음
const isMeaningfulTime = (t) => !!t && t !== "0000";

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
// "20260502" → "05/02"
const fmtMd = (ymd) => {
  if (!ymd) return "";
  const s = String(ymd);
  if (s.length < 8) return "";
  return `${s.slice(4, 6)}/${s.slice(6, 8)}`;
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
  return `${left}<span class="dt-arrow">→</span>${right}`;
};

const planRange = (s, e) => {
  if (!s && !e) return "−";
  return `${fmtTime(s)} → ${fmtTime(e)}`;
};

const buildTimeCard = () => {
  const r = record.value;
  const { isOff, isLeave, hasSeg2, hasPlan2 } = recordState.value;

  // 휴무
  if (isOff) {
    return {
      plan: { value: "휴무", meta: "정기 휴일", cls: "value-off" },
      actual: { value: "−", cls: "val-empty" },
      std: { value: "−", cls: "val-empty" },
      note: { value: "휴무", cls: "value-off" },
    };
  }

  // 계획 — plan2가 의미 있는 시간일 때만 2구간 표시
  const plan = hasPlan2
    ? {
        segments: [
          { tag: "1구간", range: planRange(r.plan1Start, r.plan1End) },
          { tag: "2구간", range: planRange(r.plan2Start, r.plan2End) },
        ],
      }
    : { value: planRange(r.plan1Start, r.plan1End), meta: "1구간" };

  // 실적 / 표준화
  let actual;
  let std;
  if (isLeave) {
    actual = { value: "출근 차단됨", cls: "val-empty" };
    std = { value: "−", cls: "val-empty" };
  } else if (hasSeg2) {
    actual = {
      segments: [
        {
          tag: "1구간",
          range: actualRange(
            r.act1InDate,
            r.act1InTime,
            r.act1OutDate,
            r.act1OutTime
          ),
        },
        {
          tag: "2구간",
          range: actualRange(
            r.act2InDate,
            r.act2InTime,
            r.act2OutDate,
            r.act2OutTime
          ),
        },
      ],
    };
    std = {
      segments: [
        {
          tag: "1구간",
          range: actualRange(
            r.act1InDate,
            r.act1InStdTime,
            r.act1OutDate,
            r.act1OutStdTime
          ),
        },
        {
          tag: "2구간",
          range: actualRange(
            r.act2InDate,
            r.act2InStdTime,
            r.act2OutDate,
            r.act2OutStdTime
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
    };
    std = {
      value: actualRange(
        r.act1InDate,
        r.act1InStdTime,
        r.act1OutDate,
        r.act1OutStdTime
      ),
    };
  }

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
  } else {
    note = { value: "−", cls: "val-empty" };
  }

  return { plan, actual, std, note };
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
      inDate: ymdNumToDash(r.act1InDate) || props.date_p,
      in: r.act1InTime || "",
      outDate: ymdNumToDash(r.act1OutDate) || props.date_p,
      out: r.act1OutTime || "",
    });
  }
  if (hasAct2) {
    segments.push({
      inDate: ymdNumToDash(r.act2InDate) || props.date_p,
      in: r.act2InTime || "",
      outDate: ymdNumToDash(r.act2OutDate) || props.date_p,
      out: r.act2OutTime || "",
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
const fmtYmdToDot = (ymd) => {
  const v = String(ymd || "");
  if (v.length < 8) return "";
  return `${v.slice(0, 4)}.${v.slice(4, 6)}.${v.slice(6, 8)}`;
};
const fmtHmsToColon = (hms) => {
  const v = String(hms || "");
  if (v.length < 4) return "";
  const hh = v.slice(0, 2);
  const mm = v.slice(2, 4);
  return v.length >= 6 ? `${hh}:${mm}:${v.slice(4, 6)}` : `${hh}:${mm}`;
};
// 날짜+시간 합쳐서 한 셀로 표시. 둘 다 없으면 "-"
const fmtDateTime = (ymd, hms) => {
  const d = fmtYmdToDot(ymd);
  const t = fmtHmsToColon(hms);
  if (!d && !t) return "-";
  return `${d} ${t}`.trim();
};
// INSERT_DATE: "YYYYMMDDHHMMSS" 또는 이미 포맷된 문자열 둘 다 대응
const fmtInsertDate = (v) => {
  const s = String(v || "");
  if (!s) return "-";
  if (/^\d{14}$/.test(s)) {
    return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)} ${s.slice(8, 10)}:${s.slice(10, 12)}:${s.slice(12, 14)}`;
  }
  if (/^\d{12}$/.test(s)) {
    return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)} ${s.slice(8, 10)}:${s.slice(10, 12)}`;
  }
  return s;
};
const historyView = computed(() =>
  (historyList.value || []).map((h) => ({
    histTypeNm: h.histTypeNm || "-",
    workSeq: h.workSeq != null && h.workSeq !== "" ? `${h.workSeq}구간` : "-",
    befCheckIn: fmtDateTime(h.befCheckInDate, h.befCheckInTime),
    befCheckOut: fmtDateTime(h.befCheckOutDate, h.befCheckOutTime),
    aftCheckIn: fmtDateTime(h.aftCheckInDate, h.aftCheckInTime),
    aftCheckOut: fmtDateTime(h.aftCheckOutDate, h.aftCheckOutTime),
    reason: h.processReason ?? "",
    insertNm: h.insertNm || "-",
    insertDate: fmtInsertDate(h.insertDate),
  }))
);

// ── 근로자 요청 (monthlyAttdReqResultList) ──────────────────
//   각 카드는 BEFORE(현재 act{workSeq}*) vs AFTER(요청된 checkIn/Out*) 를 비교한다.
const reqCards = computed(() => {
  const r = record.value ?? {};
  return (reqList.value || []).map((req) => {
    const n = parseInt(req.workSeq, 10) || 1;
    return {
      raw: req,
      reqId: req.reqId,
      reqType: req.reqType,
      reqTypeNm: req.reqTypeNm || "-",
      reqStatus: req.reqStatus,
      reqStatusNm: req.reqStatusNm || "",
      insertDate: fmtInsertDate(req.insertDate),
      workSeq: n,
      befIn: fmtTime(r[`act${n}InTime`]) || "-",
      befOut: fmtTime(r[`act${n}OutTime`]) || "-",
      aftIn: fmtTime(req.checkInTime) || "-",
      aftOut: fmtTime(req.checkOutTime) || "-",
      reqReason: req.reqReason || "",
    };
  });
});

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
const form = ref({ segments: [], reason: "" });
function initForm() {
  const panel = cfg.value.panel;
  if (panel.kind === "segments") {
    form.value = {
      segments: panel.segments.map((s) => ({ ...s })),
      reason: "",
    };
  } else {
    form.value = { reason: "" };
  }
}

const canSave = computed(() => form.value.reason.trim().length > 0);

// ── 구간 입력 관리 ────────────────────────────────────────
const MAX_SEGMENTS = 2;

const addSegment = () => {
  if (form.value.segments.length >= MAX_SEGMENTS) return;
  form.value.segments.push({
    inDate: props.date_p,
    in: "",
    outDate: props.date_p,
    out: "",
  });
};

// 출/퇴근 일자가 다르면 익일 처리 (날짜 문자열은 사전순 비교 가능)
const isOverday = (seg) =>
  !!seg.outDate && !!seg.inDate && seg.outDate > seg.inDate;

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
    if (!seg.inDate || !seg.in) {
      await proxy.$alert(getMessage(MSG.SEG_CHECKIN_REQUIRED, { idx }));
      return false;
    }
    if (!seg.outDate || !seg.out) {
      await proxy.$alert(getMessage(MSG.SEG_CHECKOUT_REQUIRED, { idx }));
      return false;
    }
    if (seg.in.length < 4 || seg.out.length < 4) {
      await proxy.$alert(getMessage(MSG.SEG_TIME_FORMAT, { idx }));
      return false;
    }
    if (seg.outDate < seg.inDate) {
      await proxy.$alert(getMessage(MSG.SEG_OUT_DATE_BEFORE_IN, { idx }));
      return false;
    }
    if (seg.outDate === seg.inDate) {
      const inN = parseInt(seg.in, 10);
      const outN = parseInt(seg.out, 10);
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
    const seg1OutStamp = toMinuteStamp(seg1.outDate, seg1.out);
    const seg2InStamp = toMinuteStamp(seg2.inDate, seg2.in);
    if (seg2InStamp <= seg1OutStamp) {
      await proxy.$alert(getMessage(MSG.SEG2_IN_AFTER_SEG1_OUT));
      return false;
    }
  }

  return true;
};

const fnSave = async () => {
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
      ymdToYmdNum(seg.inDate) !== String(oriInDate ?? "") ||
      (seg.in || "") !== String(oriInTime ?? "") ||
      ymdToYmdNum(seg.outDate) !== String(oriOutDate ?? "") ||
      (seg.out || "") !== String(oriOutTime ?? "")
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
      checkInDate: ymdToYmdNum(seg.inDate),
      checkInTime: seg.in,
      checkInMethod: "02",
      checkOutDate: ymdToYmdNum(seg.outDate),
      checkOutTime: seg.out,
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
      emit("saved");
      emit("close");
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      getMessage(MSG.SAVE_ERROR);
    await proxy.$alert(msg);
  }
};

// ── 근로자 요청 카드 액션 ─────────────────────────────────
//  - 직접수정(승인): 요청 값을 form 의 해당 workSeq 구간에 채워 넣음 (UI 적용)
//    실제 승인 처리는 form 저장 시점에 함께 처리 (추후 API 연동 예정)
//  - 승인 / 반려: 별도 API — 현재는 스텁 (메시지 코드 REQ_ACTION_PREPARING)
// 요청 카드의 출퇴근 값을 form 의 해당 workSeq 구간에 채워 넣는다.
// 반환: 채워 넣은 segment 객체 (실패 시 null)
const fillSegmentFromReq = (card) => {
  const i = (card.workSeq || 1) - 1;
  // form 의 해당 구간이 없으면 생성 (1구간 → index 0, 2구간 → index 1)
  while (
    form.value.segments.length <= i &&
    form.value.segments.length < MAX_SEGMENTS
  ) {
    form.value.segments.push({
      inDate: props.date_p,
      in: "",
      outDate: props.date_p,
      out: "",
    });
  }
  const seg = form.value.segments[i];
  if (!seg) return null;
  const req = card.raw ?? {};
  seg.inDate = ymdNumToDash(req.checkInDate) || props.date_p;
  seg.in = req.checkInTime || "";
  seg.outDate = ymdNumToDash(req.checkOutDate) || props.date_p;
  seg.out = req.checkOutTime || "";
  return seg;
};

const fnDirectEditReq = (card) => {
  const seg = fillSegmentFromReq(card);
  if (!seg) return;
  proxy.$alert(getMessage(MSG.REQ_DIRECT_EDIT_FILLED));
};

// 승인: confirm → 해당 구간을 요청 값으로 갱신 → 사유 자동 입력 → 저장
const fnApproveReq = async (card) => {
  const ok = await proxy.$confirm(getMessage(MSG.REQ_APPROVE_CONFIRM));
  if (!ok) return;

  // 수정 가능한 일자(segments 패널)에서만 진행
  if (cfg.value.panel.kind !== "segments") {
    await proxy.$alert(getMessage(MSG.REQ_APPROVE_NOT_EDITABLE));
    return;
  }

  // 1) 해당 workSeq 구간을 요청 값으로 채움
  fillSegmentFromReq(card);
  // 2) 사유 자동 입력
  form.value.reason = getMessage(MSG.REQ_APPROVED_REASON);
  // 3) 저장 실행 (fnSave 내부에서 유효성/변경여부 검사 + SAVE_CONFIRM)
  await fnSave();
};

const fnRejectReq = async (card) => {
  const ok = await proxy.$confirm(getMessage(MSG.REQ_REJECT_CONFIRM));
  if (!ok) return;
  // TODO: 반려 API 연동 — reqId, userCd, siteCd 전달
  console.log("[reject] reqId=", card?.reqId);
  await proxy.$alert(getMessage(MSG.REQ_ACTION_PREPARING));
};

// ── 삭제 ──────────────────────────────────────────────────
// 삭제 진입.
//  - DB에서 읽어온 구간(record.attd{n}Id 존재) → 사유 입력 팝업 → API 호출
//  - 화면상 신규 추가한 구간(attd{n}Id 없음) → 팝업/API 없이 form에서 즉시 제거
const openDeletePopup = (type, segIdx) => {
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
    emit("saved");
    emit("close");
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      getMessage(MSG.DELETE_ERROR);
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
  };
  record.value = {
    plan1Start: f.plan1Start ?? "",
    plan1End: f.plan1End ?? "",
    plan2Start: f.plan2Start ?? "",
    plan2End: f.plan2End ?? "",
  };
  historyList.value = [];
  reqList.value = [];

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
      reqList.value =
        response.data?.monthlyAttdReqResultList ??
        response.data?.MonthlyAttdReqResultList ??
        [];
    }
  } catch (err) {
    // 조회 실패해도 fallback 값으로 화면은 정상 렌더되도록 알림만 띄움
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      getMessage(MSG.SEARCH_ERROR);
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
/* 좌측 pane은 기존 480px 폭을 유지, 추가 공간은 모두 우측(관리자 직접 수정/처리 이력)에 배분 */
.a07pop-body {
  flex: 1;
  display: grid;
  grid-template-columns: 480px minmax(0, 1fr);
  overflow: hidden;
}
.a07pop-pane {
  overflow-y: auto;
  padding: 18px 22px 24px;
}
.a07pop-pane.left {
  border-right: 1px solid #e5e7eb;
  background: #f9fafb;
}
.a07pop-pane.right {
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
  font-weight: 500;
  font-size: 14px;
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
.req-btn-edit {
  background: #fff;
  border-color: #d1d5db;
  color: #374151;
}
.req-btn-edit:hover {
  background: #f9fafb;
  border-color: #9ca3af;
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
.btn-del-all {
  background: #fff;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-del-all:hover {
  background: #fef2f2;
  border-color: #f87171;
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
  margin-top: 8px;
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

/* 구간 입력 (A-9) */
.seg-section {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}
.seg-section:last-of-type {
  border-bottom: none;
  padding-bottom: 0;
}
.seg-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.seg-section-head .seg-label {
  font-size: 12px;
  font-weight: 800;
  color: #374151;
  letter-spacing: 0.04em;
}
.seg-section-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.seg-section-head .seg-action {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}
.seg-section-head .seg-action:hover {
  color: #111827;
}
.seg-section-head .seg-del-btn {
  background: #fff;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.seg-section-head .seg-del-btn:hover {
  background: #fef2f2;
  border-color: #f87171;
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
/* 한 행에 출근/퇴근 묶음을 가로로 배치 */
.seg-inline-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.seg-inline-label {
  font-size: 14px;
  font-weight: 700;
  color: #374151;
  white-space: nowrap;
  min-width: 36px;
  text-align: center;
}
/* 출근 그룹과 퇴근 그룹 사이 여백 확대 */
.seg-inline-row .datetime-wrap + .seg-inline-label {
  margin-left: 28px;
}
.datetime-wrap {
  display: flex;
  gap: 8px;
  align-items: center;
}
.datetime-wrap .seg-date {
  flex: 0 0 150px;
  min-width: 0;
}
.datetime-wrap :deep(.calendar-input) {
  width: 100%;
  height: 38px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  color: #111827;
  background: #fff;
}
.datetime-wrap :deep(.calendar-input:focus) {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 3px #dcfce7;
}
.datetime-wrap .seg-time {
  width: 150px;
  flex-shrink: 0;
  text-align: center;
}
.overday-hint {
  margin-top: -4px;
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 11.5px;
  font-weight: 500;
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
.history-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  user-select: none;
}
.history-head:hover {
  background: #f9fafb;
}
.history-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
}
.history-head .count {
  color: #6b7280;
  font-weight: 600;
  margin-left: 4px;
}
.history-head .chev {
  transition: transform 0.2s;
  color: #6b7280;
}
.history-card.is-open .chev {
  transform: rotate(180deg);
}
.history-body {
  padding: 0 16px 16px;
  display: none;
}
.history-card.is-open .history-body {
  display: block;
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
.del-pop-backdrop {
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
</style>
