<!--
  LeaveApplyForm.vue — 연차 신청 폼 (prafta-app-018-C, 화면 명세 UI-018C-1)
  - 분해: .claude/requests/app_requests/prafta-app-018-C-tasks.md
  - 역할: 프레젠테이션 폼(입력/표시/검증). API 호출/라우팅은 부모(LeaveApplyView)가 담당.
  - 참조 패턴: views/req/components/OvertimeForm.vue · AttdCorrectionForm.vue
      (컨텍스트 박스 + 필드 + sticky 푸터 + DateStepperField/TimeStepperField + emit submit/cancel)
  - props:
      meta        : 018-A apply-meta 응답 ({ leaveTypes:[...] })
      presets     : 018-A approval-presets 응답의 presets 배열
      context     : 진입 컨텍스트 ({ workYmd?, nodeCd?, siteName?, scheduleSummary?, slots? }) — 없을 수 있음
      submitting  : 제출 진행 플래그(부모 소유)
      preview        : LC-10 예상 차감 preview 응답({ chargeDays, floorApplied, capApplied,
                       insufficientBalance, convMinutes, floorDays }) — 부모 소유. 실패/비대상이면 null(표시 생략)
      previewLoading : preview 호출 진행 플래그(부모 소유)
  - emits:
      submit({ leaveCd, leaveType, workYmd, useUnitType, halfPart, startTime, endTime, reason,
               approverUserCds, presetId })   ← 018-B POST /appApi/leaveflow/apply 요청 본문 키와 1:1
      cancel
      preview-request(payload|null)          ← LC-10: 시간차 입력 완성 시 디바운스 후 emit.
                                                null 이면 preview 표시 해제(입력 미완성/비대상 단위)
  - ⚠️ allowedUnits/balanceDays/aprvRequired 는 전부 서버(meta) 권위. 클라 추측 금지.
  - ⚠️ 종일(00)/반차(01)/시간차(02·03·04) 분기는 선택된 종류의 allowedUnits 안에서만.
       종일/반차 편의버튼은 시작/종료 시각을 자동입력(표시·BE 차감용)하되 제출 useUnitType 은 단위코드 그대로.
  - ⚠️ 결재자/프리셋 step 의 approverUserCd 는 식별자다. 위치 index 로 재인덱싱하지 않는다(서버가 STEP_NO=배열 순서로 INSERT).
-->
<template>
  <form class="lvf" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 (특정 일자 진입 시) -->
    <section v-if="hasContext" class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div v-if="context.scheduleSummary" class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val">{{ context.scheduleSummary }}</span>
      </div>
    </section>

    <!-- 1) 연차 종류 (종류 선택 후 접힘 — 종류가 많을 때 화면 길이 절약) -->
    <section class="fs">
      <div class="fs__title-row">
        <p class="fs__title">연차 종류</p>
        <button
          v-if="selectedType"
          type="button"
          class="fs__toggle"
          @click="typeListCollapsed = !typeListCollapsed"
        >
          {{ typeListCollapsed ? `전체 보기 (${leaveTypes.length})` : '접기' }}
        </button>
      </div>
      <div class="type-list">
        <button
          v-for="lt in visibleLeaveTypes"
          :key="lt.leaveCd"
          type="button"
          class="type-item"
          :class="{
            'type-item--on': selectedLeaveCd === lt.leaveCd,
            'type-item--off': !lt.applicable && !lt.borrowable,
          }"
          :disabled="!lt.applicable && !lt.borrowable"
          @click="onSelectType(lt)"
        >
          <span class="type-item__name">{{ lt.leaveNm }}</span>
          <!-- 2026-08-09 규약: 날짜 선택 전 잔여는 일 단위 단독(E4 분모 환산 제거) -->
          <span class="type-item__bal">잔여 {{ formatLeaveDaysOnly(lt.balanceDays) }}</span>
        </button>

        <p v-if="leaveTypes.length === 0" class="fs__empty">신청 가능한 연차 종류가 없어요</p>
      </div>
    </section>

    <!-- 종류 선택 이후 노출되는 본문 -->
    <template v-if="selectedType">
      <!-- 잔여 요약 (2026-08-09 규약: 일 단위 단독 — 날짜 미정 문맥) -->
      <div class="balance-box">
        <span class="balance-box__lbl">선택한 연차 잔여</span>
        <span class="balance-box__val">{{ formatLeaveDaysOnly(selectedType.balanceDays) }}</span>
      </div>

      <!-- 2-E) 증빙 자료 안내 + 첨부 (Phase2: 강제 없음 — 미첨부 제출 허용) -->
      <section v-if="selectedType?.evidenceYn === 'Y'" class="fs">
        <p class="fs__title">증빙 자료</p>
        <p v-if="selectedType.evidenceGuideMsg" class="evid-guide">
          {{ selectedType.evidenceGuideMsg }}
        </p>

        <div class="evid-attach">
          <!-- 미첨부: 추가 버튼 -->
          <button
            v-if="!evidenceFile"
            type="button"
            class="evid-attach-btn"
            aria-label="증빙 자료 추가"
            :disabled="evidencePicking"
            @click="onPickEvidence"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>{{ evidencePicking ? '여는 중...' : '첨부' }}</span>
          </button>

          <!-- 첨부됨: 미리보기 + 제거 + 메타 -->
          <template v-else>
            <div class="evid-attach-prv">
              <img :src="evidenceFile.previewUrl" alt="첨부한 증빙 자료" />
              <button
                type="button"
                class="evid-attach-prv__rm"
                aria-label="증빙 자료 제거"
                @click="onRemoveEvidence"
              >
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div class="evid-attach-meta">
              <strong>{{ evidenceFile.name }}</strong>
              <span>{{ evidenceFile.sizeText }}</span>
            </div>
          </template>
        </div>
      </section>

      <!-- 2) 사용 단위 (allowedUnits 게이팅) -->
      <section class="fs">
        <p class="fs__title">사용 단위</p>
        <div class="unit-list">
          <button
            v-for="u in unitOptions"
            :key="u.code"
            type="button"
            class="unit-chip"
            :class="{ 'unit-chip--on': useUnitType === u.code }"
            :disabled="isScheduleRequiredUnit(u.code) && scheduleRequiredDisabledByDay"
            @click="onSelectUnit(u.code)"
          >
            {{ u.label }}
          </button>
        </div>
        <!-- E2·E5(당일분모 전환) + HB-11(F-4): 스케줄 필요 단위(반차·시간차) 가능 여부는 날짜 속성
             (당일 근무계획 배정 여부). day-schedule 응답 도착 전엔 칩 enable 유지(낙관 — 서버
             400_110/194 가 최종 판정), 도착 후 hasSchedule=false 면 반차·시간차 칩 disable + 안내.
             (구 hourlyBlocked 사용자 속성 안내는 E5 해제로 제거) -->
        <p v-if="scheduleRequiredDisabledByDay" class="unit-notice">
          이 날은 근무계획이 없어 종일 연차만 신청할 수 있어요.
        </p>
        <!-- F-5: 휴무/미배정일 종일 연차 안내 — 차단하지 않는다(사용자 확정 2026-08-05). -->
        <p v-if="showRestDayNotice" class="unit-notice unit-notice--rest">
          이 날은 근무계획이 없어요(휴무). 종일 연차를 써도 실제로 더 쉬게 되지는 않아요.
        </p>
      </section>

      <!-- 3) 날짜 -->
      <section class="fs">
        <p class="fs__title">신청 일자</p>
        <label class="field">
          <span class="field__label"><span class="req">*</span>날짜</span>
          <DateStepperField v-model="workDateInput" placeholder="날짜 선택" />
        </label>
      </section>

      <!-- 3-1) 반차 파트 (useUnitType==='01' 전용) — 경계 시각은 서버 day-schedule 권위값.
           시작기준 = 늦게 출근 / 종료기준 = 일찍 퇴근. 실제 시각을 함께 노출한다(스케줄마다 다름). -->
      <section v-if="isHalfUnit" class="fs">
        <p class="fs__title">반차 구분</p>

        <!-- 대상일 근무/휴게 시각 안내 — 반차 파트를 고를 때 참고할 수 있도록 원 스케줄을 보여준다. -->
        <div v-if="dayScheduleInfo" class="sch-info">
          <div class="sch-info__row">
            <span class="sch-info__lbl">근무</span>
            <span class="sch-info__val">{{ dayScheduleInfo.workText }}</span>
          </div>
          <div v-if="dayScheduleInfo.breakText" class="sch-info__row">
            <span class="sch-info__lbl">휴게</span>
            <span class="sch-info__val sch-info__val--brk">{{ dayScheduleInfo.breakText }}</span>
          </div>
        </div>

        <div class="half-list">
          <button
            type="button"
            class="half-card"
            :class="{ 'half-card--on': halfPart === 'START' }"
            :disabled="halfPartBlocked"
            @click="halfPart = 'START'"
          >
            <span class="half-card__name">늦게 출근</span>
            <span class="half-card__range">{{ halfStartRangeText || '--:-- ~ --:--' }}</span>
            <span class="half-card__hint">이 시간까지 쉬고 출근</span>
          </button>

          <button
            type="button"
            class="half-card"
            :class="{ 'half-card--on': halfPart === 'END' }"
            :disabled="halfPartBlocked"
            @click="halfPart = 'END'"
          >
            <span class="half-card__name">일찍 퇴근</span>
            <span class="half-card__range">{{ halfEndRangeText || '--:-- ~ --:--' }}</span>
            <span class="half-card__hint">이 시간부터 쉬고 퇴근</span>
          </button>
        </div>

        <!-- 경계 안내 — 휴게를 건너뛰고 근로를 절반으로 나눈 시각임을 명시 -->
        <p v-if="halfBoundaryText" class="half-note">
          <span class="half-note__dot" aria-hidden="true">·</span>
          <span class="half-note__text">
            이 날 근무를 절반으로 나누는 기준 시각은
            <strong>{{ halfBoundaryText }}</strong> 예요. (휴게시간은 근무로 세지 않아요)
          </span>
        </p>
        <!-- 차단이 확정된 경우에만 경고. daySchedule 미도착 구간에는 아무것도 띄우지 않는다
             (틀린 안내를 순간 노출하지 않기 위함 — halfPartBlocked 주석 참조) -->
        <p v-else-if="halfPartBlocked" class="half-note half-note--warn">
          <span class="half-note__dot" aria-hidden="true">·</span>
          <span class="half-note__text">이 날은 근무계획이 없어 반차를 신청할 수 없어요. 종일 연차로 신청해 주세요.</span>
        </p>
      </section>

      <!-- 4) 시간차 단위(02·03·04) — 시작~종료 시각 -->
      <section v-if="isTimeUnit" class="fs">
        <div class="time-head">
          <p class="fs__title">신청 시간</p>
          <!-- 종일/반차 편의버튼: allowedUnits 에 해당 단위가 있을 때만 노출.
               누르면 시각 자동입력(시작=스케줄시작, 종일=스케줄종료/반차=절반) — 계산은 developer. -->
          <div class="quick-btns">
            <button
              v-if="canQuickFullDay"
              type="button"
              class="quick-btn"
              @click="onQuickFill('00')"
            >
              종일
            </button>
            <button
              v-if="canQuickHalfDay"
              type="button"
              class="quick-btn"
              @click="onQuickFill('01')"
            >
              반차
            </button>
          </div>
        </div>

        <!-- 대상일 근무/휴게 시각 안내 — 시간차는 휴게시간을 가로지를 수 없으므로(서버 ATTD_400_055)
             시각 선택 전에 휴게 구간을 보여준다. 조회 실패/스케줄 없는 날은 표시 생략·안내만. -->
        <div v-if="dayScheduleInfo" class="sch-info">
          <div class="sch-info__row">
            <span class="sch-info__lbl">근무</span>
            <span class="sch-info__val">{{ dayScheduleInfo.workText }}</span>
          </div>
          <div v-if="dayScheduleInfo.breakText" class="sch-info__row">
            <span class="sch-info__lbl">휴게</span>
            <span class="sch-info__val sch-info__val--brk">{{ dayScheduleInfo.breakText }}</span>
          </div>
        </div>
        <!-- E2(당일분모 전환): 미배정일 안내 확장 — 시간차 자체가 불가함을 명시(종일 유도) -->
        <p v-else-if="daySchedule && !daySchedule.hasSchedule" class="sch-info-none">
          이 날은 근무계획이 없어 시간 단위 연차를 사용할 수 없어요. 종일 연차로 신청해 주세요.
        </p>

        <label class="field">
          <span class="field__label"><span class="req">*</span>시작</span>
          <TimeStepperField v-model="startTimeInput" :step="30" placeholder="시작 시각" />
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>종료</span>
          <div class="end-stepper">
            <button
              type="button"
              class="end-stepper__btn"
              aria-label="종료 시각 줄이기"
              :disabled="stepCount <= 1"
              @click="onStepDown"
            >
              −
            </button>
            <span class="end-stepper__val">{{ endTimeInput || '--:--' }}</span>
            <span class="end-stepper__n">{{ stepTotalText }}</span>
            <button
              type="button"
              class="end-stepper__btn"
              aria-label="종료 시각 늘리기"
              :disabled="!canStepUp"
              @click="onStepUp"
            >
              +
            </button>
          </div>
        </label>

        <p class="time-guide">
          <span class="time-guide__dot" aria-hidden="true">·</span>
          {{ unitGuideText }}
        </p>
      </section>

      <!-- 4-1) 가불(미래 연차 당겨쓰기) 동의 — 시스템 법정 연차 + 가불 가능 + 잔여 부족 시에만 노출 (prafta-com-011-4) -->
      <section v-if="showBorrowToggle" class="fs">
        <label class="borrow-toggle">
          <input
            v-model="borrowAgreed"
            type="checkbox"
            class="borrow-toggle__cb"
          />
          <span class="borrow-toggle__txt">미래 연차를 당겨 사용(가불)</span>
        </label>

        <!-- 토글 ON 시: 가불 한도/만료 안내 -->
        <div v-if="borrowAgreed" class="borrow-info">
          <div class="borrow-info__row">
            <span class="borrow-info__lbl">가불 가능 한도</span>
            <span class="borrow-info__val">{{ formatLeaveDaysOnly(borrowQuota) }}</span>
          </div>
          <div v-if="borrowExpiryDisplay" class="borrow-info__row">
            <span class="borrow-info__lbl">만료(소멸)</span>
            <span class="borrow-info__val">{{ borrowExpiryDisplay }}</span>
          </div>
          <p v-if="borrowDeficitText" class="borrow-info__deficit">{{ borrowDeficitText }}</p>
          <p class="borrow-info__guide">
            <span class="borrow-info__dot" aria-hidden="true">·</span>
            결재 승인 후 확정돼요. 미래에 발생할 연차에서 자동 차감됩니다.
          </p>
        </div>
      </section>

      <!-- 5) 사유 -->
      <section class="fs">
        <label class="field">
          <span class="field__label">
            신청 사유
            <span class="field__help">{{ reason.length }}/500</span>
          </span>
          <textarea
            v-model="reason"
            class="field__textarea"
            placeholder="사유를 입력해 주세요."
            maxlength="500"
            rows="4"
          ></textarea>
        </label>
      </section>

      <!-- 6) 결재선 (aprvRequired 종류만) -->
      <section v-if="aprvRequired" class="fs">
        <p class="fs__title">결재선</p>

        <!-- 프리셋 선택 -->
        <div v-if="presets.length > 0" class="preset-list">
          <button
            v-for="p in presets"
            :key="p.presetId"
            type="button"
            class="preset-chip"
            :class="{ 'preset-chip--on': selectedPresetId === p.presetId }"
            @click="onSelectPreset(p)"
          >
            {{ p.presetNm }}
            <span v-if="p.defaultYn" class="preset-chip__tag">기본</span>
          </button>
        </div>

        <!-- 결재자 순서 리스트 -->
        <ul v-if="approverList.length > 0" class="aprv-list">
          <li v-for="(ap, idx) in approverList" :key="ap.approverUserCd" class="aprv-row">
            <span class="aprv-row__step">{{ idx + 1 }}</span>
            <div class="aprv-row__info">
              <p class="aprv-row__name">{{ ap.userNm }}</p>
              <p class="aprv-row__meta">{{ approverMetaOf(ap) }}</p>
            </div>
            <button
              type="button"
              class="aprv-row__del"
              aria-label="결재자 제거"
              @click="onRemoveApprover(ap.approverUserCd)"
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
                aria-hidden="true"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </li>
        </ul>
        <p v-else class="aprv-empty">결재자를 추가해 주세요</p>

        <button type="button" class="btn-add" @click="onOpenApproverPicker">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          결재자 추가
        </button>
      </section>

      <!-- 잔여 초과 사전 안내 (서버 051 도 표면화) -->
      <p v-if="overBalanceWarning" class="warn-msg">
        신청 일수가 남은 연차보다 많아요. 신청이 거절될 수 있어요.
      </p>

      <!-- LC-10: 예상 차감 요약 카드 (시간차/반반차 preview — 신청 버튼 위, plan §5-D).
           preview 실패 시 카드 미노출(신청은 가능 — 서버가 최종 판정). -->
      <section v-if="showPreviewCard" class="preview-card" aria-live="polite">
        <p v-if="previewLoading" class="preview-card__loading">예상 차감 계산 중...</p>
        <template v-else-if="preview">
          <div class="preview-card__row">
            <span class="preview-card__lbl">예상 차감</span>
            <span class="preview-card__val">{{ previewChargeText }}</span>
          </div>
          <p v-if="preview.floorApplied" class="preview-card__floor">
            {{ floorNoticeText }}
          </p>
          <!-- 2026-08-17: 잔여 부족 원인 분기 — 부여 유효기간 밖 날짜(noGrantOnDate)는 잔여 문제가 아니라
               날짜 문제라서 별도 안내한다(화면 잔여는 오늘 기준이라 "잔여 있는데 왜 초과?" 혼란 실발생).
               구서버 응답(필드 부재)이면 falsy → 기존 문구 그대로(무회귀). -->
          <p v-if="preview.insufficientBalance && preview.noGrantOnDate" class="preview-card__warn">
            선택한 날짜에는 사용할 수 있는 연차가 없어요.
            <template v-if="grantAvailFromText">
              연차 부여 시작일({{ grantAvailFromText }}) 이후 날짜로 신청해 주세요.
            </template>
            <template v-else>
              연차 부여의 사용 가능 기간 안의 날짜로 신청해 주세요.
            </template>
          </p>
          <p v-else-if="preview.insufficientBalance" class="preview-card__warn">
            예상 차감이 남은 연차를 초과해요. 이대로 신청하면 거절될 수 있어요.
          </p>
          <!-- PC-11: 짜투리 발동 회사 부담 행 (웹 UI-C 미러 — D6). 발동 예상 시 서버가
               insufficientBalance=false 로 내리므로 위 부족 경고와 동시 노출되지 않는다. -->
          <div v-if="preview.remnantTriggered" class="preview-card__row">
            <span class="preview-card__lbl">회사 부담</span>
            <span class="preview-card__val preview-card__val--cover">{{ coverMinutesText }}</span>
          </div>
          <p v-if="preview.remnantTriggered" class="preview-card__remnant">
            잔여 전액이 차감되고 부족분은 회사 부담으로 처리됩니다.
          </p>
        </template>
      </section>
    </template>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      {{ helperText }}
    </p>

    <!-- F-10 규약: 왼쪽=진행/확정(신청하기), 오른쪽=이탈(취소), 폭 균등 -->
    <footer class="form-ft">
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '신청 중...' : '신청하기' }}
      </button>
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
    </footer>

    <!-- 결재자 추가 바텀시트 — HB-14(F-6) 통합 공용 시트.
         후보 검색은 018-A GET /appApi/leaveflow/approver-search?keyword=&page=&size= (source 기본값) -->
    <ApproverPickerSheet
      v-if="aprvRequired"
      v-model="approverPickerOpen"
      :excluded-user-cds="approverUserCds"
      @add="onAddApprovers"
    />
  </form>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, getCurrentInstance } from 'vue'
import {
  formatLeaveDays,
  formatLeaveDaysOnly,
  formatMinutesToHm,
  trimRawDays,
} from '@/utils/leaveFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'
// HB-14(F-6): 화면마다 3벌이던 결재자 시트를 공용 1벌로 통합(LeaveApproverPickerSheet 대체).
import ApproverPickerSheet from '@/components/common/ApproverPickerSheet.vue'
// 연차 신청 증빙 필수화(2026-08-29): 증빙 자료 첨부 — 기존 사진 첨부 유틸 재사용(SafetyInspectBadForm.vue 패턴 미러).
import { selectImage, revokePreview } from '@/utils/imagePicker'

const props = defineProps({
  // 018-A apply-meta 응답: { leaveTypes: [{ leaveCd, leaveNm, systemYn, aprvRequired, allowedUnits[], balanceDays, applicable }],
  //   convMinutes(오늘 기준 1일 환산시간(분) — E4 참고치. 2026-08-09 표기 규약 변경으로 잔여 표기가
  //     일 단위 단독으로 전환되어 FE 미사용 — 서버 additive 잔존 필드),
  //   hourlyBlocked(E5 교대 차단 해제로 서버가 항상 false 반환 — 하위호환 잔존 필드, FE 미사용.
  //     시간차 가능 여부는 날짜 속성(day-schedule hasSchedule)으로 게이팅) }
  meta: { type: Object, default: () => ({ leaveTypes: [] }) },
  // 018-A approval-presets 응답의 presets 배열: [{ presetId, presetNm, defaultYn, steps:[{ stepNo, approverUserCd, userNm, userId, rankNm, nodeNm }] }]
  presets: { type: Array, default: () => [] },
  // 진입 컨텍스트(특정 일자 진입 시): { workYmd?, nodeCd?, siteName?, scheduleSummary?, slots? }
  context: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false },
  // LC-10: 예상 차감 preview 응답(부모 소유). { chargeDays, floorApplied, capApplied, insufficientBalance,
  //   convMinutes, floorDays(발동 마일스톤 요금 0.25/0.5/1 — 구응답이면 부재),
  //   remnantTriggered(PC-05 D6: 짜투리 보전 발동 예상 — 발동 시 insufficientBalance=false 로 옴),
  //   remnantDays(발동 시 차감될 잔여 전액(일) — 미발동 null),
  //   companyCoverMinutes(회사 부담분(분) — 미발동 null) }
  //   preview 실패/비대상이면 null — 표시 생략하고 신청은 가능(서버가 최종 판정).
  preview: { type: Object, default: null },
  // LC-10: preview 호출 진행 플래그(부모 소유) — 요약 카드 로딩 표시용.
  previewLoading: { type: Boolean, default: false },
  // 대상일 근무/휴게 시각(부모 소유, GET day-schedule 응답) — 시간차 휴게 가로지름 사전 안내용.
  //   { hasSchedule, fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
  //     fstBrkStrTime, fstBrkEndTime, secBrkStrTime, secBrkEndTime,
  //     halfDayBoundaryTime, halfStartPartRange, halfEndPartRange } | null(미조회/실패 — 표시 생략)
  //   HB-03(반차 시간대 도입): 뒤 3필드는 반차 경계 미리보기(서버 산출 권위값 — FE 재계산 금지).
  //     halfDayBoundaryTime='HHMM' / halfStartPartRange·halfEndPartRange='HHMM~HHMM'.
  //     스케줄 없음/산출 불가면 전부 null(구 응답도 부재 → null 취급).
  daySchedule: { type: Object, default: null },
})
const emit = defineEmits(['submit', 'cancel', 'preview-request', 'day-schedule-request'])

const { proxy } = getCurrentInstance() || { proxy: null }
// 공통: alert 폴백(앱 전역 $alert 우선, 없으면 window.alert) — LeaveApplyView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 사용 단위 라벨(SYS025) — 표시 전용 상수 ──────────────────────────────
// 00 종일 / 01 반차 / 02 2시간 / 03 1시간 / 04 30분
//   HB-04(2026-08-07): 반반차('05') 폐지 — 서버 allowedUnits 가 '05' 를 반환하지 않는다.
const UNIT_LABELS = {
  '00': '종일',
  '01': '반차',
  '02': '2시간',
  '03': '1시간',
  '04': '30분',
}

// 시간차 단위(02·03·04)별 1스텝 분량(분). 종료 = 시작 + N×단위분 계산에 사용.
const UNIT_MINUTES = {
  '02': 120,
  '03': 60,
  '04': 30,
}

// ── 반응형 상태 (developer: 초기값/리셋/시각 자동계산 로직 보완) ──────────
const selectedLeaveCd = ref('')
const useUnitType = ref('') // SYS025 코드
// ── 반차 파트(시작기준/종료기준) — HB-10 ──────────────────────────────
// halfPart: 'START'(늦게 출근) | 'END'(일찍 퇴근). 제출 payload 키와 1:1.
//   반차('01') 신청 시 필수 — 미선택 제출은 서버가 fail-closed 거부(ATTD_400_195)하므로 FE 도 차단한다.
const halfPart = ref('')
const workDateInput = ref('') // 'YYYY-MM-DD' (DateStepperField v-model)
const startTimeInput = ref('') // 'HH:MM' (TimeStepperField v-model, 30분 단위)
// 종료 시각 = 시작 + stepCount × 단위분. [+]/[−] 로 stepCount 조정(최소 1).
const stepCount = ref(1)
const reason = ref('')

// 가불(미래 연차 당겨쓰기) 동의 상태 (prafta-com-011-4). 종류/날짜 변경 시 리셋.
const borrowAgreed = ref(false)

// 연차 신청 증빙 필수화(2026-08-29): 증빙 자료. { file, previewUrl, name, sizeText } | null
const evidenceFile = ref(null)
const evidencePicking = ref(false)

const formatEvidenceSize = (bytes) => {
  if (!bytes && bytes !== 0) return ''
  const kb = bytes / 1024
  return kb < 1024 ? `${kb.toFixed(0)}KB` : `${(kb / 1024).toFixed(1)}MB`
}

const onPickEvidence = async () => {
  if (evidencePicking.value) return
  evidencePicking.value = true
  try {
    // TODO(developer): 카메라/갤러리 선택 UX 확정(현재는 갤러리 우선 — 증빙 서류는 사진보다 문서 촬영 빈도가
    //   높을 수 있어 SafetyInspectBadForm.vue와 달리 'camera' 고정이 아닌 선택지가 필요할 수 있음).
    const { file, previewUrl } = await selectImage('gallery')
    if (evidenceFile.value?.previewUrl) revokePreview(evidenceFile.value.previewUrl)
    evidenceFile.value = {
      file,
      previewUrl,
      name: file.name || 'evidence.jpg',
      sizeText: formatEvidenceSize(file.size),
    }
  } catch (e) {
    // 선택 취소는 정상 흐름 — 별도 알림 없음(SafetyInspectBadForm.vue 관례 동일)
    console.log(`[LeaveApplyForm] 증빙 첨부 취소/실패: ${e && e.message}`)
  } finally {
    evidencePicking.value = false
  }
}

const onRemoveEvidence = () => {
  if (evidenceFile.value?.previewUrl) revokePreview(evidenceFile.value.previewUrl)
  evidenceFile.value = null
}

// 연차 종류 리스트 접힘 상태 — 종류 선택 후 자동 접힘(종류가 많을 때 화면 길이 절약).
const typeListCollapsed = ref(false)

// 결재선 상태
const selectedPresetId = ref('')
// approverList: [{ approverUserCd, userNm, userId, rankNm, nodeNm }] (순서 = 결재 단계)
const approverList = ref([])
const approverPickerOpen = ref(false)

// ── 파생값 (단순 표시/필터 — 비즈니스 로직 아님) ─────────────────────────
const leaveTypes = computed(() => props.meta?.leaveTypes || [])

// 2026-08-09 규약: 잔여 표기가 일 단위 단독으로 전환되어 apply-meta convMinutes 소비처 소멸
//   (구 metaConvMinutes computed 제거 — preview 계열의 convMinutes(E1, 신청일 기준)는 그대로 유지).

// E2·E5(당일분모 전환) + HB-11(F-4): "스케줄이 배정된 날에만 신청 가능한 단위" 게이팅은 날짜 속성 —
//   당일 근무계획 미배정이면 칩 disable + 안내. day-schedule 미도착/조회 실패 시엔 enable 유지(낙관)
//   — 서버 400_110/194 fail-closed 가 최종 판정.
//   ★ 반차('01')도 경계 시각을 당일 스케줄에서 역산하므로 미배정일 신청 불가(서버 ATTD_400_110)다.
//   (구 hourlyUnavailableNotice: apply-meta hourlyBlocked 기반 사용자 속성 안내 — E5 교대 차단 해제로 제거)
const SCHEDULE_REQUIRED_UNIT_CODES = ['01', '02', '03', '04']
const isScheduleRequiredUnit = (code) => SCHEDULE_REQUIRED_UNIT_CODES.includes(code)
//   ★ NEW-3 부수: 조회 게이트에 종일('00')이 포함되면서, 종일만 허용하는 종류(USAGE_UNIT='FULL_DAY')
//     에도 daySchedule 이 도착하게 됐다. 그때 이 값이 true 가 되면 "종일 연차만 신청할 수 있어요"
//     안내(:106)가 F-5 휴무 안내(:110)와 겹쳐 두 줄이 쌓인다. 그 문구는 "다른 단위가 있는데 막혔다"는
//     뜻이므로, 애초에 스케줄 필요 단위가 하나도 없는 종류에서는 성립하지 않는다 → 조건에 포함한다.
//     (칩 :disabled 는 isScheduleRequiredUnit(u.code) 와 AND 라 그런 칩이 없는 종류에선 영향 0.)
const scheduleRequiredDisabledByDay = computed(
  () =>
    hasScheduleRequiredUnits.value &&
    Boolean(props.daySchedule) &&
    props.daySchedule.hasSchedule !== true,
)

// 시간차 단위 집합(02/03/04) — 스텝퍼·시각입력 분기 전용. 위 스케줄 필요 집합과 의미가 다르므로 합치지 않는다.
const HOURLY_UNIT_CODES = ['02', '03', '04']
const isHourlyUnitCode = (code) => HOURLY_UNIT_CODES.includes(code)

const selectedType = computed(
  () => leaveTypes.value.find((t) => t.leaveCd === selectedLeaveCd.value) || null,
)

// 결재 필요 여부(선택 종류의 서버 플래그). 가불(borrowAgreed) ON 이면 체크박스 설정 무관하게 결재 강제(결정 §4).
const aprvRequired = computed(() => Boolean(selectedType.value?.aprvRequired) || borrowAgreed.value)

// 선택 종류 allowedUnits(서버 권위) → 표시용 옵션
const unitOptions = computed(() => {
  const allowed = selectedType.value?.allowedUnits || []
  return allowed.map((code) => ({ code, label: UNIT_LABELS[code] || code }))
})

// 접힘 시 선택 종류 1건만 노출(펼침/미선택은 전체). 접힌 항목 클릭은 onSelectType 이 펼침으로 처리.
const visibleLeaveTypes = computed(() =>
  typeListCollapsed.value && selectedType.value ? [selectedType.value] : leaveTypes.value,
)

// 시간차 단위 여부(02·03·04)
const isTimeUnit = computed(() => isHourlyUnitCode(useUnitType.value))

// 반차 단위 여부(표시 분기 전용)
const isHalfUnit = computed(() => useUnitType.value === '01')

// 종료 스텝퍼 옆 신청 총 시간 표시 — 예: 7×30분 = "3시간 30분".
//   (구현 주의: 스텝수+단위라벨 문자열 연결("730분")은 시간처럼 오독되므로 총 분량으로 환산 표기)
const stepTotalText = computed(() => {
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return ''
  return formatMinutesToHm(stepCount.value * unitMin)
})

// 종료 시각(읽기전용 파생): 시작 미입력/비시간차면 ''. 아니면 시작 + stepCount×단위분(24h wrap).
//   minutesToInput() 재사용. 시작은 30분 단위 TimeStepperField 값.
const endTimeInput = computed(() => {
  if (!isTimeUnit.value) return ''
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return ''
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return ''
  return minutesToInput(startM + stepCount.value * unitMin)
})

// 종료 분이 자정(1440) 이상으로 넘어가는지(=익일 wrap) 판정. 시간차+시작입력+단위분 유효 전제.
//   1440(=24:00)은 minutesToInput 이 '00:00' 으로만 표현해 BE 가 받지 못하므로 1440 도 차단(유효 종료 ≤ 1439분).
const endOverflowsDay = computed(() => {
  if (!isTimeUnit.value) return false
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return false
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return false
  return startM + stepCount.value * unitMin >= 1440
})

// 한 단계 더 늘렸을 때 자정을 넘지 않는지(=종료 [+] 활성 가능). 시작 미입력/비시간차/단위분 무효면 false.
const canStepUp = computed(() => {
  if (!isTimeUnit.value) return false
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return false
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return false
  return startM + (stepCount.value + 1) * unitMin < 1440
})

// 종일/반차 편의버튼 노출 — allowedUnits 에 해당 단위가 있을 때만
const canQuickFullDay = computed(() => (selectedType.value?.allowedUnits || []).includes('00'))
const canQuickHalfDay = computed(() => (selectedType.value?.allowedUnits || []).includes('01'))

// 컨텍스트(특정 일자 진입) 유무
const hasContext = computed(() => Boolean(props.context?.workYmd))

const ctxDateDisplay = computed(() => {
  const ymd = props.context?.workYmd
  if (!ymd || ymd.length !== 8) return '-'
  return `${ymd.slice(0, 4)}년 ${Number(ymd.slice(4, 6))}월 ${Number(ymd.slice(6, 8))}일`
})
const ctxSiteDisplay = computed(() => props.context?.siteName || '')

// 단위별 안내 문구(시간차 단위 입력 영역)
const unitGuideText = computed(() => {
  const label = UNIT_LABELS[useUnitType.value] || ''
  // developer: 휴게시간 가로지름 불가 등 정책 문구 확정(attd §8.5). 골격은 기본 안내만.
  return `${label} 단위로 신청해 주세요. 휴게시간을 가로지를 수 없어요.`
})

// ── 대상일 근무/휴게 시각 안내 (day-schedule) ─────────────────────────────
// 'HHMM' → 'HH:MM' 표시. 형식 위반이면 ''.
const fmtHHMM = (hhmm) => {
  if (!hhmm || !/^\d{4}$/.test(hhmm)) return ''
  return `${hhmm.slice(0, 2)}:${hhmm.slice(2)}`
}

// 'HHMM' 시각쌍 → "HH:MM~HH:MM". 한쪽이라도 무효면 null.
const fmtRange = (str, end) => {
  const s = fmtHHMM(str)
  const e = fmtHHMM(end)
  return s && e ? `${s}~${e}` : null
}

// 표시용 근무/휴게 텍스트. 스케줄 없음/미조회면 null(블록 미노출).
//   2구간 스케줄은 " / " 로 병기. 휴게 미설정이면 breakText='' (근무만 노출).
const dayScheduleInfo = computed(() => {
  const ds = props.daySchedule
  if (!ds || ds.hasSchedule !== true) return null
  const workRanges = [
    fmtRange(ds.fstSchStrTime, ds.fstSchEndTime),
    fmtRange(ds.secSchStrTime, ds.secSchEndTime),
  ].filter(Boolean)
  if (workRanges.length === 0) return null
  const breakRanges = [
    fmtRange(ds.fstBrkStrTime, ds.fstBrkEndTime),
    fmtRange(ds.secBrkStrTime, ds.secBrkEndTime),
  ].filter(Boolean)
  return {
    workText: workRanges.join(' / '),
    breakText: breakRanges.join(' / '),
  }
})

// ── HB-10: 반차 경계 미리보기 (서버 day-schedule 권위값) ────────────────────
// ★ 클라이언트에서 경계를 재계산하지 않는다 — 서버 산식(ScheduleWorkMinutesUtils)이 단일 출처다.
//   서버 표기 'HHMM~HHMM'(자정 경계는 '2400') → 표시용 'HH:MM~HH:MM'. 형식 위반/부재면 ''.
const fmtServerRange = (range) => {
  if (typeof range !== 'string') return ''
  const parts = range.split('~')
  if (parts.length !== 2) return ''
  return fmtRange(parts[0], parts[1]) || ''
}

// 근무를 절반으로 나누는 기준 시각('HH:MM'). 스케줄 없음/산출 불가면 ''(반차 카드 disable 판정에도 사용).
const halfBoundaryText = computed(() => fmtHHMM(props.daySchedule?.halfDayBoundaryTime))

// 반차 파트 선택 차단 판정 — ★ scheduleRequiredDisabledByDay 와 같은 "낙관 enable" 규약을 따른다.
//   daySchedule 미도착(조회 in-flight) 구간에는 차단하지 않는다. `!halfBoundaryText` 로 판정하면
//   로딩 수백 ms 동안 카드가 잠기고 "근무계획이 없어…" 라는 사실과 다른 문구가 노출된다.
//   차단은 "스케줄이 없음을 확인했을 때" 또는 "스케줄은 있는데 서버가 경계를 못 냈을 때"만.
const halfPartBlocked = computed(() => {
  if (!props.daySchedule) return false // 미도착 → 낙관 enable
  return props.daySchedule.hasSchedule !== true || !halfBoundaryText.value
})
// 시작기준(늦게 출근)이 쉬는 구간 = [근무 시작, 경계)
const halfStartRangeText = computed(() => fmtServerRange(props.daySchedule?.halfStartPartRange))
// 종료기준(일찍 퇴근)이 쉬는 구간 = [경계, 근무 종료)
const halfEndRangeText = computed(() => fmtServerRange(props.daySchedule?.halfEndPartRange))

// F-5(HB-12): 휴무·미배정일에 종일('00') 연차를 고를 때만 안내. 차단하지 않는다(사용자 확정 2026-08-05).
//   신규 조회 없이 기존 day-schedule 응답만 재사용(미조회/실패 시엔 안내 생략 — 오안내 방지).
const showRestDayNotice = computed(
  () =>
    useUnitType.value === '00' &&
    Boolean(props.daySchedule) &&
    props.daySchedule.hasSchedule !== true,
)

// 선택 종류가 스케줄 필요 단위(반차·시간차)를 허용하는지 — E2 날짜 게이팅은 단위 선택 "전"(날짜 선택
//   시점)에 칩 disable 판정이 필요하므로, 조회 조건을 구 isTimeUnit(단위 선택 후)에서 종류 허용 기준으로 확장.
//   HB-10: 반차 경계 미리보기도 같은 day-schedule 응답을 쓰므로 '01' 허용 종류도 조회 대상이다.
const hasScheduleRequiredUnits = computed(() =>
  (selectedType.value?.allowedUnits || []).some((c) => isScheduleRequiredUnit(c)),
)

// ★ NEW-3(F-5/HB-12 미발동 수정): day-schedule 조회 대상에 종일('00')도 포함한다.
//   회사 정책이 USAGE_UNIT='FULL_DAY'(Baim_07 기본값)면 allowedUnits = ['00'] 뿐이라
//   구 게이트(스케줄 필요 단위 보유)로는 조회 자체가 나가지 않았고 → daySchedule 이 영원히 null →
//   showRestDayNotice 의 Boolean(props.daySchedule) 이 false → 휴무 안내가 영구 미노출이었다.
//   그런데 F-5 의 주 대상("휴무·미배정일에 종일 연차가 경고 없이 차감")이 정확히 그 구성이다.
//   ※ 종일 경로에서 이 응답은 '안내 표시 전용'이다 — 차단·검증·차감 로직에는 쓰지 않는다(사용자 확정).
const needsDaySchedule = computed(() => {
  const allowed = selectedType.value?.allowedUnits || []
  return allowed.some((c) => isScheduleRequiredUnit(c)) || allowed.includes('00')
})

// 조회 대상 종류 + 날짜 완성 시에만 조회 대상(그 외 null → 표시 해제).
//   ※ 중복 호출 방지: 조회는 이 computed 의 "값 변화"에만 반응한다(watch). 종류를 바꿔도 같은 날짜면
//     값이 그대로라 재조회가 없고, 게이트가 넓어져 종류 전환 시 null↔ymd 왕복도 오히려 줄어든다.
const dayScheduleYmd = computed(() => {
  if (!needsDaySchedule.value) return null
  const ymd = toYmd(workDateInput.value)
  return ymd && ymd.length === 8 ? ymd : null
})

// 대상 변경 → 부모에 조회 요청 emit(API 호출은 부모 소유 — preview 패턴 동일). 즉시 emit(디바운스
//   불요 — 날짜/단위 변경은 이산적). immediate 로 컨텍스트 프리필 진입도 커버.
watch(
  dayScheduleYmd,
  (ymd) => {
    emit('day-schedule-request', ymd)
  },
  { immediate: true },
)

// E2 + HB-11: 미배정일 판명 시 선택 중이던 스케줄 필요 단위(반차·시간차)를 종일 우선으로 폴백 —
//   disable 된 칩이 선택 상태로 잔존하는 것을 방지. 시각 입력·반차 파트도 함께 리셋.
watch(scheduleRequiredDisabledByDay, (blocked) => {
  if (!blocked || !isScheduleRequiredUnit(useUnitType.value)) return
  const allowed = selectedType.value?.allowedUnits || []
  useUnitType.value = allowed.includes('00')
    ? '00'
    : allowed.find((c) => !isScheduleRequiredUnit(c)) || ''
  startTimeInput.value = ''
  stepCount.value = 1
  halfPart.value = ''
})

// HB-10: 대상일이 바뀌면(=경계 재산출) 이전 날짜 기준으로 고른 반차 파트를 무효화한다.
//   경계 시각이 스케줄마다 다르므로, 사용자가 새 경계를 보고 다시 고르게 하는 것이 안전하다.
watch(
  () => props.daySchedule,
  () => {
    halfPart.value = ''
  },
)

// 결재자 emit 용 userCd 배열(순서 보존 — 위치 재인덱싱 아님, 표시 순서 그대로)
const approverUserCds = computed(() => approverList.value.map((a) => a.approverUserCd))

const helperText = computed(() =>
  aprvRequired.value
    ? '신청 후 결재선의 승인을 거쳐 연차로 반영돼요.'
    : '신청 시 바로 연차로 반영돼요.',
)

// ── 형식 유틸 (input 값 ↔ emit/스케줄 값) — OvertimeForm 패턴 차용 ─────────
// 'YYYY-MM-DD' → 'YYYYMMDD'
const toYmd = (s) => (s ? s.replace(/-/g, '') : '')
// 'HH:MM' → 'HHMM' (앞 4자리)
const toHHMM = (s) => (s ? s.replace(':', '').slice(0, 4) : '')
// 'HH:MM' → 분(minute). 형식 위반 시 -1.
const toMinutes = (s) => {
  if (!/^\d{2}:\d{2}/.test(s || '')) return -1
  const h = Number(s.slice(0, 2))
  const m = Number(s.slice(3, 5))
  if (Number.isNaN(h) || Number.isNaN(m) || h > 23 || m > 59) return -1
  return h * 60 + m
}
// 분 → 'HH:MM' (24시간 wrap). 자동입력(반차 절반)용.
const minutesToInput = (mins) => {
  let m = mins
  if (m < 0) m += 24 * 60
  m = m % (24 * 60)
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

// 컨텍스트 스케줄(첫 구간)의 시작/종료 시각(HHMM). 편의버튼 자동입력 출처. 없으면 null.
const contextSchedule = computed(() => {
  const slots = props.context?.slots || []
  if (!Array.isArray(slots) || slots.length === 0) return null
  const sch = slots[0]?.schedule
  if (!sch || (!sch.startTime && !sch.endTime)) return null
  return { startTime: sch.startTime || '', endTime: sch.endTime || '' }
})

// 신청 일수 추정(종일 1.0 / 반차 0.5 / 시간차 (종료-시작)분÷소정근로분). 계산 불가/미선택 시 null.
//   표시 전용 근사(서버가 최종 판정). 잔여초과 경고와 가불 토글 노출 판정의 단일출처.
//   HB-04: 반반차('05') 분기 폐지.
const estimatedDays = computed(() => {
  if (!selectedType.value) return null
  if (useUnitType.value === '00') return 1.0
  if (useUnitType.value === '01') return 0.5
  if (isTimeUnit.value) {
    // 시간차: (종료-시작)분 ÷ 소정근로분. 소정근로 출처는 컨텍스트 스케줄(시작~종료), 휴게 미반영 근사.
    const startM = toMinutes(startTimeInput.value)
    const endM = toMinutes(endTimeInput.value)
    if (startM < 0 || endM < 0) return null
    const reqMin = endM - startM
    if (reqMin <= 0) return null
    const sch = contextSchedule.value
    const schStart = sch ? toMin4(sch.startTime) : -1
    const schEnd = sch ? toMin4(sch.endTime) : -1
    const workMin = schStart >= 0 && schEnd >= 0 ? schEnd - schStart : -1
    if (workMin <= 0) return null // 소정근로 산출 불가 → 추정 보류
    return reqMin / workMin
  }
  return null
})

// 잔여 초과 사전 경고 — 신청 일수 추정 > 선택 종류 balanceDays 면 true. 계산 불가 시 false.
//   PC-11: 짜투리 보전 발동 예상(preview.remnantTriggered — 서버 권위)이면 신청이 성공하므로 억제
//   (발동 안내 카드가 대신 노출 — "거절될 수 있어요" 와의 모순 방지, dev2 설계 확정 6 미러).
const overBalanceWarning = computed(() => {
  if (props.preview?.remnantTriggered) return false
  const type = selectedType.value
  if (!type) return false
  const bal = Number(type.balanceDays)
  if (Number.isNaN(bal)) return false
  const est = estimatedDays.value
  if (est === null) return false
  return est > bal
})

// ── 가불(미래 연차 당겨쓰기) 파생값 (prafta-com-011-4) ─────────────────────
// 가불 한도(서버 권위, apply-meta borrowQuota). 비대상이면 0.
const borrowQuota = computed(() => Number(selectedType.value?.borrowQuota) || 0)

// 가불분 만료(소멸)일 YYYYMMDD(서버 산출). 없으면 ''.
const borrowExpiryYmd = computed(() => String(selectedType.value?.borrowExpiryYmd || ''))

// 만료일 표시(YYYY-MM-DD). 미산정이면 ''.
const borrowExpiryDisplay = computed(() => {
  const ymd = borrowExpiryYmd.value
  if (!ymd || ymd.length !== 8 || !/^\d{8}$/.test(ymd)) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
})

// 가불 토글 노출: 시스템 법정 연차(systemYn='Y') + 가불 가능(borrowable) + 잔여(balanceDays) 부족(추정 신청일수 초과).
//   잔여 충분이거나 추정 불가/비대상이면 미노출(결정 §6-1: 부족할 때만).
const showBorrowToggle = computed(() => {
  const type = selectedType.value
  if (!type) return false
  if (String(type.systemYn) !== 'Y') return false
  if (!type.borrowable) return false
  const bal = Number(type.balanceDays)
  const est = estimatedDays.value
  if (Number.isNaN(bal) || est === null) return false
  return est > bal
})

// 가불 충당(부족) 안내 텍스트 — 예: "남은 0일 + 가불 3일". 추정 불가/충분이면 ''.
const borrowDeficitText = computed(() => {
  const type = selectedType.value
  if (!type) return ''
  const bal = Number(type.balanceDays)
  const est = estimatedDays.value
  if (Number.isNaN(bal) || est === null) return ''
  const deficit = est - Math.max(0, bal)
  if (deficit <= 0) return ''
  return `남은 ${formatLeaveDaysOnly(Math.max(0, bal))} + 가불 ${formatLeaveDaysOnly(deficit)}`
})

// 선택 일자가 가불 만료(소멸)일을 지났는지(가불 토글 ON 한정 가드). 만료 미산정이면 false.
const borrowDateExpired = computed(() => {
  if (!borrowAgreed.value) return false
  const exp = borrowExpiryYmd.value
  const ymd = toYmd(workDateInput.value)
  if (!exp || !ymd || ymd.length !== 8) return false
  return ymd > exp
})

// 가불 토글 노출 조건이 깨지면(잔여 충분/비대상 전환 등) 동의 자동 해제 — 잔존 동의 누수 방지.
watch(showBorrowToggle, (visible) => {
  if (!visible && borrowAgreed.value) borrowAgreed.value = false
})

// 가불 토글 ON + 만료 경과 일자 선택 → alert 안내 후 차단(결정 §3, 서버도 fail-closed). 날짜 초기화.
watch(borrowDateExpired, (expired) => {
  if (expired) {
    showAlert('가불 만료일이 지난 날짜에는 사용할 수 없어요.')
    workDateInput.value = ''
  }
})

// ── LC-10: 예상 차감 preview 요청 (시간차 — POST /appApi/leaveflow/preview-deduction) ──
// 입력 완성 시 디바운스 후 부모에 emit(API 호출은 부모 소유 — 컨테이너/폼 역할 분담 유지).
const PREVIEW_DEBOUNCE_MS = 400
let previewTimer = null

// preview 대상 payload(요청 본문 키 1:1). 비대상(종일/반차)·입력 미완성이면 null.
//   시간차(02/03/04) = 날짜 + 시작/종료 완성 + 자정 미초과일 때. (HB-04: 반반차 분기 폐지)
const previewPayload = computed(() => {
  if (!selectedType.value) return null
  const ymd = toYmd(workDateInput.value)
  if (!ymd || ymd.length !== 8) return null
  const unit = useUnitType.value
  if (isHourlyUnitCode(unit)) {
    if (!startTimeInput.value || !endTimeInput.value || endOverflowsDay.value) return null
    return {
      leaveCd: selectedLeaveCd.value,
      workYmd: ymd,
      useUnitType: unit,
      startTime: toHHMM(startTimeInput.value),
      endTime: toHHMM(endTimeInput.value),
    }
  }
  return null
})

// payload 변경 → 디바운스 후 preview 요청 emit. null 전환은 즉시(잔존 카드 누수 방지).
watch(previewPayload, (payload) => {
  if (previewTimer) {
    clearTimeout(previewTimer)
    previewTimer = null
  }
  if (!payload) {
    emit('preview-request', null)
    return
  }
  previewTimer = setTimeout(() => {
    previewTimer = null
    emit('preview-request', payload)
  }, PREVIEW_DEBOUNCE_MS)
})

// 폼 해제 시 잔여 타이머 정리(unmount 후 emit 방지).
onUnmounted(() => {
  if (previewTimer) clearTimeout(previewTimer)
})

// 예상 차감 카드 노출: preview 대상 단위(시간차) + (로딩 중 또는 응답 보유).
//   preview 실패(null)면 미노출 — 신청은 가능(서버 최종 판정). HB-04: 반반차 대상 제거.
const showPreviewCard = computed(
  () => isTimeUnit.value && (props.previewLoading || !!props.preview),
)

// E4(당일분모 전환): 시간차는 "이 날 기준 {신청 시간} = {X}일 차감" — 분모가 당일 배정 스케줄임을
//   날짜 기준으로 표기(신청 시간 = stepCount×단위분, X = 서버 chargeDays 그대로).
//   단위분 산출 불가(방어) 시에만 기존 일반 표기("N일 H시간 (X일)")로 폴백한다.
const previewChargeText = computed(() => {
  const p = props.preview
  if (!p) return ''
  if (isTimeUnit.value) {
    const unitMin = UNIT_MINUTES[useUnitType.value]
    if (unitMin) {
      const reqText = formatMinutesToHm(stepCount.value * unitMin)
      return `이 날 기준 ${reqText} = ${trimRawDays(p.chargeDays)}일 차감`
    }
  }
  return `${formatLeaveDays(p.chargeDays, p.convMinutes)} (${trimRawDays(p.chargeDays)}일)`
})

// 하한 발동 마일스톤 요금(floorDays) → 단위 라벨. 0.5=반차 / 1=종일.
//   HB-04: 반반차 폐지로 0.25 라벨 제거 — 서버 하한 마일스톤(R3)은 0.25 를 계속 산출할 수 있으므로
//   그 경우엔 아래 폴백 문구(단위명 미언급)로 안내한다.
const FLOOR_UNIT_LABELS = { 0.5: '반차', 1: '종일' }

// 하한 발동 안내 문구 — floorDays 기반 단위 분기. 라벨 없는 값(0.25)·구응답이면 일반 문구 폴백.
const floorNoticeText = computed(() => {
  const p = props.preview
  if (!p || !p.floorApplied) return ''
  const label = FLOOR_UNIT_LABELS[Number(p.floorDays)]
  if (!label) {
    const rawFloor = Number(p.floorDays)
    if (p.floorDays != null && Number.isFinite(rawFloor) && rawFloor > 0) {
      return `같은 날 누적 신청이 하한 기준 시간에 도달하여 ${trimRawDays(rawFloor)}일이 차감됩니다.`
    }
    // 구응답(floorDays 부재) 폴백 — 일반화 문구.
    return '같은 날 누적 신청이 고정 단위 기준 시간에 도달하여 고정 단위 요금이 적용됩니다.'
  }
  return `같은 날 누적 신청이 ${label} 시간에 도달하여 ${label} 요금(${trimRawDays(p.floorDays)}일)이 적용됩니다.`
})

// PC-11: 짜투리 발동 회사 부담분 텍스트 — 예: "1시간 30분". 발동 시에만 노출(companyCoverMinutes 서버 산출).
const coverMinutesText = computed(() => {
  const p = props.preview
  if (!p || !p.remnantTriggered) return ''
  return formatMinutesToHm(p.companyCoverMinutes)
})

// 2026-08-17: 부여 유효기간 밖 날짜 안내 — 대상일 이후 가장 이른 부여 시작일(YYYYMMDD → 'YYYY.MM.DD').
//   서버 grantAvailFromDate 부재/형식 위반이면 '' (일반화 문구로 폴백).
const grantAvailFromText = computed(() => {
  const v = props.preview?.grantAvailFromDate
  if (typeof v !== 'string' || !/^\d{8}$/.test(v)) return ''
  return `${v.slice(0, 4)}.${v.slice(4, 6)}.${v.slice(6, 8)}`
})

// 'HHMM' → 분. 형식 위반 시 -1. (스케줄 HHMM 용)
function toMin4(hhmm) {
  if (!hhmm || hhmm.length !== 4 || !/^\d{4}$/.test(hhmm)) return -1
  const h = Number(hhmm.slice(0, 2))
  const m = Number(hhmm.slice(2))
  if (h > 23 || m > 59) return -1
  return h * 60 + m
}

// ── 검증 (단순 필수입력 — 그 외 분기/계산은 developer) ────────────────────
const isValid = computed(() => {
  if (!selectedType.value) return false
  if (!useUnitType.value) return false
  if (!workDateInput.value) return false
  if (isTimeUnit.value && (!startTimeInput.value || !endTimeInput.value)) return false
  // 종료가 자정을 넘어가면(익일 wrap) 제출 차단 — BE(eMin<=sMin) 가 ATTD_400_052 로 거부하므로 사전 방어.
  if (isTimeUnit.value && endOverflowsDay.value) return false
  // HB-10: 반차는 파트(늦게 출근/일찍 퇴근) 필수 — 서버 fail-closed(ATTD_400_195) 사전 방어.
  if (isHalfUnit.value && !halfPart.value) return false
  if (aprvRequired.value && approverList.value.length === 0) return false
  // 가불 토글 ON + 만료 경과 일자면 제출 차단(결정 §3, 서버 fail-closed 사전 방어).
  if (borrowDateExpired.value) return false
  return true
})

// ── 표시 헬퍼 (UI — 허용) ────────────────────────────────────────────────
/*
 * ★잔여/한도 일수 표기용 formatDays(소수 1자리) 인라인 포맷은 제거했다.
 *   leaveFormat.js 가 "날짜 미정 문맥(잔여/부여/사용예정/한도)의 유일한 표기 함수,
 *   인라인 포맷 금지" 를 규정하는데도 이 파일은 formatLeaveDaysOnly 를 이미 import 해 쓰면서
 *   그 옆에 1자리 복사본을 따로 두고 있었다. 그 결과 같은 잔여값이 이 화면에서는 "13.4",
 *   연차 현황 화면에서는 "13.44" 로 보여 서로 다른 값처럼 읽혔다.
 *   자릿수를 여기서 다시 정하지 말 것 — 규칙이 바뀌면 leaveFormat.js 만 고친다.
 */
const approverMetaOf = (ap) => [ap?.nodeNm, ap?.rankNm].filter(Boolean).join(' · ')

// ── UI 토글/선택 (developer: 종류 변경 시 단위/시각/결재선 재구성 로직 보완) ─
// 선택 종류의 허용 단위(allowedUnits, 서버 권위) 안에서 기본 사용 단위 결정.
//   종일('00') 우선, 없으면 첫 허용 단위, 허용 단위가 없으면 빈 값 유지.
const resolveDefaultUnit = (type) => {
  const allowed = type?.allowedUnits || []
  if (allowed.includes('00')) return '00'
  return allowed[0] || ''
}

// 종류 변경 시 단위/시각/결재선 재초기화. 단위는 종류별 허용 단위 안에서 종일을 기본 선택한다.
//   선택 후에는 리스트를 접는다(화면 길이 절약). 이미 선택된 종류 재탭은 입력 리셋 없이
//   접힘/펼침 토글만 한다(접힘 상태의 단일 항목 탭 = 펼치기).
const onSelectType = (lt) => {
  // 잔여 0(applicable=false)이어도 가불 가능(borrowable)하면 선택 허용 — 가불은 잔여 부족/0일 때가
  //   본래 대상이라(showBorrowToggle), 여기서 막으면 그 케이스에 영영 도달할 수 없었다.
  if (!lt?.applicable && !lt?.borrowable) return
  if (selectedLeaveCd.value === lt.leaveCd) {
    typeListCollapsed.value = !typeListCollapsed.value
    return
  }
  selectedLeaveCd.value = lt.leaveCd
  typeListCollapsed.value = true
  useUnitType.value = resolveDefaultUnit(lt)
  startTimeInput.value = ''
  stepCount.value = 1
  halfPart.value = '' // HB-10: 종류 변경 시 반차 파트 초기화
  selectedPresetId.value = ''
  approverList.value = []
  borrowAgreed.value = false // 가불 동의는 종류별 — 종류 변경 시 해제
  applyDefaultPreset() // 종류 변경으로 비운 결재선에 기본 프리셋 재전개(2026-08-15)
}

// 단위 전환. 시간차가 아닌 단위(종일/반차)로 전환 시 잔존 시작값을 비워 누수 방지.
//   시간차로 전환/변경 시 stepCount 를 1 로 리셋(종료는 computed 라 자동 재계산).
//   HB-10: 반차 외 단위로 전환하면 반차 파트를 비운다(잔존 파트가 제출되는 것을 방지).
const onSelectUnit = (code) => {
  useUnitType.value = code
  if (isHourlyUnitCode(code)) {
    stepCount.value = 1
  } else {
    startTimeInput.value = ''
    stepCount.value = 1
  }
  if (code !== '01') halfPart.value = ''
}

// 종료 스텝 증감 — 최소 N=1. 증가 시 자정 초과(익일 wrap)면 무시.
const onStepUp = () => {
  // 시작 입력된 시간차에서 다음 증가가 자정을 넘으면(>=1440) 상한 도달 — 증가 무시.
  if (!canStepUp.value) return
  stepCount.value += 1
}
const onStepDown = () => {
  stepCount.value = Math.max(1, stepCount.value - 1)
}

// 편의버튼('00'=종일 / '01'=반차): 단위만 세팅(비시간차).
//   종일/반차는 시각입력 UI 없이 단위코드만 제출(submit 에서 startTime/endTime 은 null).
//   시간차 진입 시 잔존 시작값을 비워 누수 방지하고 stepCount 리셋.
const onQuickFill = (unitCode) => {
  useUnitType.value = unitCode
  startTimeInput.value = ''
  stepCount.value = 1
  if (unitCode !== '01') halfPart.value = '' // HB-10: 종일 전환 시 반차 파트 초기화
}

// 프리셋 선택 → steps 를 approverList 로 전개(STEP_NO=배열 순서 보존). 같은 프리셋 재선택 시 토글 해제.
const onSelectPreset = (preset) => {
  if (!preset) return
  if (selectedPresetId.value === preset.presetId) {
    // 재선택 토글: 해제 + 전개 결재선 비움.
    selectedPresetId.value = ''
    approverList.value = []
    return
  }
  selectedPresetId.value = preset.presetId
  approverList.value = (preset.steps || []).map((s) => ({
    approverUserCd: s.approverUserCd,
    userNm: s.userNm,
    userId: s.userId,
    rankNm: s.rankNm,
    nodeNm: s.nodeNm,
  }))
}

// 기본 프리셋 자동 전개 (2026-08-15 사용자 지시 — 화면 간 동작 통일).
//   기간 연차 신청(LeaveApplyMultiView)이 이미 자동 전개라 단건 화면을 그쪽에 맞춘다.
//   ⚠️ 결재선이 비어 있을 때만 적용 — 사용자가 직접 편집한 상태를 덮어쓰지 않는다.
//   ⚠️ 칩 재선택(토글 해제)으로 비운 뒤에는 재적용하지 않는다(watch 의존값이 안 바뀌므로 성립).
//   ★ defaultYn 은 엔드포인트마다 형이 다르다 — leaveflow=boolean / mypage=('Y'|'N') 문자열. 둘 다 수용.
const isDefaultPreset = (p) => p?.defaultYn === true || p?.defaultYn === 'Y'

const applyDefaultPreset = () => {
  if (!aprvRequired.value) return // 무결재 종류는 섹션 자체가 없다
  if (selectedPresetId.value) return
  if (approverList.value.length > 0) return
  const def = (props.presets || []).find(isDefaultPreset)
  if (def) onSelectPreset(def)
}

// 프리셋 도착(비동기) · 결재 필요 여부 전환(종류 선택 / 가불 동의) 시점에 전개.
//   같은 aprvRequired=true 종류끼리의 변경은 값이 안 바뀌어 여기서 안 잡히므로
//   onSelectType 이 초기화 직후 직접 호출한다(아래).
watch([() => props.presets, aprvRequired], applyDefaultPreset, { immediate: true })

const onOpenApproverPicker = () => {
  approverPickerOpen.value = true
}

// 시트 add(picked[]) 수신 → approverList 에 순서 append. userCd 식별자 dedup.
//   직접 추가 시 프리셋 이탈(selectedPresetId 해제) — 폼은 approverUserCds 를 SSOT 로 제출하므로 정합.
const onAddApprovers = (picked) => {
  const existing = new Set(approverList.value.map((a) => a.approverUserCd))
  const additions = (picked || [])
    .filter((p) => p && p.userCd && !existing.has(p.userCd))
    .map((p) => ({
      approverUserCd: p.userCd,
      userNm: p.userNm,
      userId: p.userId,
      rankNm: p.rankNm,
      nodeNm: p.nodeNm,
    }))
  if (additions.length > 0) {
    approverList.value = [...approverList.value, ...additions]
    selectedPresetId.value = ''
  }
  approverPickerOpen.value = false
}

// 결재자 제거 — userCd 식별자 필터(위치 index 재인덱싱 금지).
const onRemoveApprover = (approverUserCd) => {
  approverList.value = approverList.value.filter((a) => a.approverUserCd !== approverUserCd)
  // 프리셋 전개에서 일부 제거 시 더 이상 프리셋과 동일하지 않으므로 이탈 표시.
  selectedPresetId.value = ''
}

// ── 제출 (018-B LeaveApplyRequest 키 1:1) ─────────────────────────────────
const onSubmit = () => {
  if (!isValid.value) return
  const timeUnit = isTimeUnit.value
  const reasonText = reason.value.trim()
  emit('submit', {
    leaveCd: selectedLeaveCd.value,
    // ⚠️ leaveType(성격코드)은 018-A apply-meta 응답에 없음 → 보낼 값 없음(추측 금지). 미전송(서버 null 저장).
    workYmd: toYmd(workDateInput.value),
    useUnitType: useUnitType.value,
    // HB-10: 반차 파트('START'=늦게 출근 / 'END'=일찍 퇴근). 반차 외 단위는 null(서버 무시).
    //   서버가 이 값으로 경계 시각을 역산해 START_TIME/END_TIME 을 확정한다(FE 시각 산출 금지).
    halfPart: isHalfUnit.value ? halfPart.value : null,
    startTime: timeUnit ? toHHMM(startTimeInput.value) : null,
    endTime: timeUnit ? toHHMM(endTimeInput.value) : null,
    reason: reasonText || null,
    // 결재 불필요 종류면 미전송. 결재 필요면 전개된 최종 순서의 userCd 배열(SSOT).
    approverUserCds: aprvRequired.value ? approverUserCds.value : undefined,
    // presetId 는 생략 — 폼이 approverUserCds(전개)를 SSOT 로 보냄(018-B 결정 2 정합).
    presetId: undefined,
    // 가불 동의(prafta-com-011-4): 토글 ON 시 true. 미선택이면 false(서버 미전송 시 false 취급).
    isBorrow: borrowAgreed.value,
    // 연차 신청 증빙 필수화(2026-08-29): Phase2 는 미첨부여도 제출 허용(강제는 서버 Phase3).
    //   부모 뷰(LeaveApplyView.vue)가 업로드 후 fileMgmtCd 로 치환한다.
    evidenceFile: evidenceFile.value?.file || null,
  })
}

// ── 진입 컨텍스트(특정 일자)면 날짜 프리필. 폼 날짜는 이후 사용자 선택이 SSOT. ──
onMounted(() => {
  const ymd = props.context?.workYmd
  if (ymd && ymd.length === 8 && /^\d{8}$/.test(ymd)) {
    workDateInput.value = `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
  }
})
</script>

<style scoped>
.lvf {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 컨텍스트 박스 (OvertimeForm .ctx 패턴 동일) */
.ctx {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ctx__date {
  margin: 0 0 var(--space-xs);
  display: flex;
  flex-direction: column;
}
.ctx__date strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ctx__date small {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: baseline;
}
.ctx__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__val {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

/* 섹션 공통 */
.fs {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.fs__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.fs__empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
/* 섹션 제목 + 접기/펼치기 토글 행 (연차 종류) */
.fs__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fs__toggle {
  height: 28px;
  padding: 0 var(--space-sm);
  background: transparent;
  border: 0;
  font-size: 12px;
  color: var(--color-primary-text-deep);
  cursor: pointer;
  font-family: inherit;
}

/* 연차 종류 리스트 */
.type-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.type-item--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
}
.type-item--off {
  opacity: 0.5;
  cursor: not-allowed;
}
.type-item__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.type-item__bal {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

/* 잔여 요약 박스 */
.balance-box {
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.balance-box__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.balance-box__val {
  font-size: 14px;
  color: var(--color-primary-text-darkest);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 사용 단위 칩 */
.unit-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.unit-chip {
  min-height: 40px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.unit-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
/* E2(당일분모 전환): 미배정일 시간차 칩 disable 시각 상태 — .type-item--off 패턴 미러 */
.unit-chip:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* 미배정일 시간차 불가 안내 (time-guide 톤 미러 — 앱 토큰으로 치환) */
.unit-notice {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.unit-notice--rest {
  color: var(--color-warning-text);
}

/* 반차 파트 선택 — unit-chip / sch-info 토큰 계열 승계 */
.half-list {
  display: flex;
  gap: var(--space-sm);
}
.half-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  text-align: left;
  cursor: pointer;
  font-family: inherit;
}
.half-card--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
}
.half-card:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.half-card__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.half-card__range {
  font-size: 13px;
  color: var(--color-primary-text-deep);
}
.half-card__hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.half-note {
  display: flex;
  gap: var(--space-xs);
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}
.half-note--warn {
  color: var(--color-warning-text);
}
.half-note__dot {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}
.half-note__text {
  flex: 1;
  min-width: 0;
}

/* 시간차 입력 영역 헤더(제목 + 편의버튼) */
.time-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.quick-btns {
  display: flex;
  gap: var(--space-xs);
}
.quick-btn {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.quick-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
/* 대상일 근무/휴게 시각 안내 (시간차 — 휴게 가로지름 사전 안내) */
.sch-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
}
.sch-info__row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--space-sm);
}
.sch-info__lbl {
  font-size: 12px;
  color: var(--color-warning-text);
}
.sch-info__val {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.sch-info__val--brk {
  color: var(--color-warning-text);
}
.sch-info-none {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.time-guide {
  margin: 0;
  display: flex;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.time-guide__dot {
  color: var(--color-text-tertiary);
}

/* 종료 시각 [−]/[+] 스텝퍼 (시작 + N×단위분 자동계산) */
.end-stepper {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  height: 44px;
  padding: 0 var(--space-sm);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.end-stepper__btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  font-family: inherit;
}
.end-stepper__btn:disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
.end-stepper__val {
  flex: 1;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.end-stepper__n {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 필드 공통(OvertimeForm .field 패턴) */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__label .req {
  color: var(--color-danger);
}
.field__help {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.field__textarea {
  width: 100%;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
  resize: vertical;
  min-height: 96px;
}
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 결재선 — 프리셋 칩 */
.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.preset-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.preset-chip__tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
}

/* 결재자 리스트 */
.aprv-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.aprv-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.aprv-row__step {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-size: 12px;
  font-weight: 600;
}
.aprv-row__info {
  flex: 1;
  min-width: 0;
}
.aprv-row__name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.aprv-row__meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aprv-row__del {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.aprv-empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.warn-msg {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-danger-tint);
  border: 0.5px solid var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-danger);
}

/* LC-10: 예상 차감 요약 카드 — balance-box 톤 재사용(CSS 변수만) */
.preview-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
}
.preview-card__loading {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.preview-card__row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--space-sm);
}
.preview-card__lbl {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary-text-deep);
}
.preview-card__val {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary-text-darkest);
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.preview-card__floor {
  margin: 0;
  font-size: 12px;
  color: var(--color-warning-text);
}
.preview-card__warn {
  margin: 0;
  font-size: 12px;
  color: var(--color-danger);
}
/* PC-11: 짜투리 발동 회사 부담 행/안내 (웹 UI-C 미러 — 앱 토큰으로 치환) */
.preview-card__val--cover {
  color: var(--color-primary);
}
.preview-card__remnant {
  margin: 0;
  font-size: 12px;
  color: var(--color-primary-text-deep);
}

/* 가불 동의 토글 + 안내 (prafta-com-011-4) — 기존 토큰/패턴 재사용 */
.borrow-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 44px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}
.borrow-toggle__cb {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  accent-color: var(--color-primary);
  cursor: pointer;
}
.borrow-toggle__txt {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.borrow-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
}
.borrow-info__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.borrow-info__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
}
.borrow-info__val {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary-text-darkest);
  font-variant-numeric: tabular-nums;
}
.borrow-info__deficit {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary-text-deep);
}
.borrow-info__guide {
  margin: 0;
  display: flex;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-primary-text-deep);
}
.borrow-info__dot {
  color: var(--color-primary-text-deep);
}

.helper {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-warning-text);
  display: flex;
  gap: var(--space-xs);
}
.helper__dot {
  color: var(--color-warning);
}

/* sticky 푸터(OvertimeForm .form-ft 패턴 동일) */
.form-ft {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom));
  background: var(--color-bg);
  border-top: 0.5px solid var(--color-border);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}
.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn--x {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--p {
  background: var(--color-primary);
  border: 0;
  color: var(--color-surface);
}
.btn--p:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.evid-guide {
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  color: var(--color-warning-text);
  font-size: 12px;
  line-height: 1.5;
}
.evid-attach {
  display: flex;
  align-items: center;
  gap: 8px;
}
.evid-attach-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 72px;
  height: 72px;
  border: 1.5px dashed var(--color-border);
  border-radius: 10px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
  flex-shrink: 0;
}
.evid-attach-btn:disabled {
  opacity: 0.6;
  cursor: progress;
}
.evid-attach-prv {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
}
.evid-attach-prv img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.evid-attach-prv__rm {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: var(--color-surface);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}
.evid-attach-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
  flex: 1;
  min-width: 0;
}
.evid-attach-meta strong {
  color: var(--color-text-primary);
  font-weight: 600;
  display: block;
}
</style>
