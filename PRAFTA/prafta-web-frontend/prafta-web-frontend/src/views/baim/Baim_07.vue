<template>
  <div class="viewComm">
    <!-- 헤더: [변경 이력] [조회] [저장] -->
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
    >
      <!-- ViewHeader 기본 버튼 외 [변경 이력] 액션은 헤더 우측에 별도 노출.
           ViewHeader가 slot을 지원하지 않으면 developer가 ViewHeader 확장 또는
           본문 상단 액션바로 이동 검토 (UI 스펙 결정 필요 D-3). -->
    </ViewHeader>

    <div class="viewBody leave-policy">
      <!-- 페이지 액션 영역: [변경 이력] (헤더 ViewHeader가 slot 미지원이므로 본문 상단에 배치) -->
      <div class="lp-page-actions">
        <button class="btn btn-second lp-history-btn" @click="fnOpenHistory">
          변경 이력
        </button>
      </div>

      <!-- 부여 시점 미리보기 안내 카드 -->
      <div class="lp-help-card">
        <div class="lp-help-left">
          <span class="lp-help-icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
          </span>
          <p class="lp-help-text">
            <strong>1번(연차 부여 기준)</strong>과
            <strong>2번(입사 첫해 처리 방식)</strong>의 조합에 따라 연차 발생
            시점이 결정됩니다. 시뮬레이션 결과를 미리 확인할 수 있습니다.
          </p>
        </div>
        <button class="btn btn-second lp-help-btn" @click="fnOpenPreview">
          부여 시점 미리보기
        </button>
      </div>

      <!-- ============ axis 1: 연차 부여 기준 ============ -->
      <section class="lp-card">
        <header class="lp-card__head">
          <span class="lp-num">1</span>
          <h3 class="lp-card__title">연차 부여 기준</h3>
        </header>
        <p class="lp-card__desc">
          연차 15일을 매년 어느 시점에 부여할지 결정합니다.
        </p>
        <div class="lp-options lp-options--2">
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis1GrantBase === 'HIRE_DATE' }"
            @click="fnSelectAxis1('HIRE_DATE')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">입사일 기준</span>
              <span class="lp-option__sub">개인별 입사일에 부여</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis1GrantBase === 'FISCAL_YEAR' }"
            @click="fnSelectAxis1('FISCAL_YEAR')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">회계연도 기준</span>
              <span class="lp-option__sub"
                >매년 정해진 회계연도 시작일에 일괄</span
              >
            </span>
          </button>
        </div>
      </section>

      <!-- ============ axis 2(UI) = axis3(백엔드): 입사 첫해 처리 방식 ============ -->
      <section class="lp-card">
        <header class="lp-card__head">
          <span class="lp-num">2</span>
          <h3 class="lp-card__title">입사 첫해 처리 방식</h3>
        </header>
        <p class="lp-card__desc">
          입사 첫 해(1년 미만)에 <strong>본연차</strong>를 어떻게 부여할지
          결정합니다. 1년 미만 월차는 정책과 관계없이
          <strong>매월 만근 시 1일씩 자동 부여</strong>됩니다 (법정 의무).
        </p>
        <div class="lp-options lp-options--3">
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis3FirstYearMethod === 'MONTHLY_ONLY',
              'is-disabled': axis2Disabled.MONTHLY_ONLY,
            }"
            :disabled="axis2Disabled.MONTHLY_ONLY"
            @click="fnSelectFirstYear('MONTHLY_ONLY')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">월차만 부여</span>
              <span class="lp-option__sub">입사일 기준 시만</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis3FirstYearMethod === 'PRORATE',
              'is-disabled': axis2Disabled.PRORATE,
            }"
            :disabled="axis2Disabled.PRORATE"
            @click="fnSelectFirstYear('PRORATE')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">비례 부여</span>
              <span class="lp-option__sub">회계연도 기준 시만</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis3FirstYearMethod === 'NEXT_YEAR_BULK',
              'is-disabled': axis2Disabled.NEXT_YEAR_BULK,
            }"
            :disabled="axis2Disabled.NEXT_YEAR_BULK"
            @click="fnSelectFirstYear('NEXT_YEAR_BULK')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">차년도 일괄 부여</span>
              <span class="lp-option__sub">회계연도 기준 시만</span>
            </span>
          </button>
        </div>
        <div class="lp-note lp-note--info">
          <svg
            viewBox="0 0 24 24"
            width="13"
            height="13"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            <strong>회계연도 기준</strong> 선택 시 [비례 부여]·[차년도 일괄
            부여]만 선택 가능 ([월차만 부여]는 입사일 기준 전용).
          </span>
        </div>
        <div class="lp-note lp-note--legal">
          <svg
            viewBox="0 0 24 24"
            width="13"
            height="13"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            <strong>법정 의무</strong>: 1년 미만 월차(최대 11일)는 입사일
            기준으로 매월 만근 시 1일씩 부여되며, 위 정책 선택과 관계없이 항상
            적용 (근로기준법 제60조 제2항).
          </span>
        </div>
      </section>

      <!-- ============ axis 3(UI) = axis4(백엔드): 비례 부여 시 반올림 (조건부) ============ -->
      <section class="lp-card" :class="{ 'is-conditional': !axis3Active }">
        <header class="lp-card__head">
          <span class="lp-num">3</span>
          <h3 class="lp-card__title">
            비례 부여 시 반올림
            <span v-if="!axis3Active" class="lp-badge lp-badge--cond"
              >조건부 활성</span
            >
          </h3>
        </header>
        <p class="lp-card__desc">
          입사 첫해 비례 부여 계산 시 발생하는 소수점 처리 방식입니다.
          <strong>2번에서 "비례 부여" 선택 시에만 활성화</strong>됩니다.
        </p>
        <div class="lp-options lp-options--4">
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis4ProrateRounding === 'CEIL',
              'is-disabled': !axis3Active,
            }"
            :disabled="!axis3Active"
            @click="fnSelectRounding('CEIL')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">올림</span>
              <span class="lp-option__sub">근로자 유리</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis4ProrateRounding === 'ROUND',
              'is-disabled': !axis3Active,
            }"
            :disabled="!axis3Active"
            @click="fnSelectRounding('ROUND')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">반올림</span>
              <span class="lp-option__sub">표준</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis4ProrateRounding === 'FLOOR',
              'is-disabled': !axis3Active,
            }"
            :disabled="!axis3Active"
            @click="fnSelectRounding('FLOOR')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">내림</span>
              <span class="lp-option__sub">엄격</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{
              'is-selected': axis4ProrateRounding === 'HALF_DAY',
              'is-disabled': !axis3Active,
            }"
            :disabled="!axis3Active"
            @click="fnSelectRounding('HALF_DAY')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">0.5일 단위 절사</span>
              <span class="lp-option__sub">반차 운영</span>
            </span>
          </button>
        </div>
        <p class="lp-strong-note">
          ※ <strong>0.5일 단위 절사</strong> 선택 시 아래 사용 단위가 [0.5일
          (반차)]로 자동 고정되며 변경 불가
        </p>
      </section>

      <!-- ============ axis 4(UI) = axis2(백엔드): 회계연도 시작일 (조건부) ============ -->
      <section class="lp-card" :class="{ 'is-conditional': !axis4Active }">
        <header class="lp-card__head">
          <span class="lp-num">4</span>
          <h3 class="lp-card__title">
            회계연도 시작일
            <span v-if="!axis4Active" class="lp-badge lp-badge--cond"
              >조건부 활성</span
            >
          </h3>
        </header>
        <p class="lp-card__desc">
          매년 본연차를 일괄 부여하는 기준일입니다.
          <strong>1번에서 "회계연도 기준" 선택 시에만 활성화</strong>됩니다.
        </p>
        <div class="lp-inline-row">
          <input
            type="number"
            class="lp-num-input"
            v-model="axis2FiscalStartMm"
            min="1"
            max="12"
            :disabled="!axis4Active"
          />
          <span class="lp-inline-label">월</span>
          <input
            type="number"
            class="lp-num-input"
            v-model="axis2FiscalStartDd"
            min="1"
            max="31"
            :disabled="!axis4Active"
          />
          <span class="lp-inline-label">일</span>
        </div>
      </section>

      <!-- ============ axis 5: 근속 가산 정책 ============ -->
      <section class="lp-card">
        <header class="lp-card__head">
          <span class="lp-num">5</span>
          <h3 class="lp-card__title">근속 가산 정책</h3>
        </header>
        <p class="lp-card__desc">
          근속 연수에 따라 추가로 발생하는 연차의 가산 규칙과 최대 한도를
          설정합니다.
        </p>
        <div class="lp-options lp-options--2">
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis5TenureMode === 'LEGAL' }"
            @click="fnSelectTenureMode('LEGAL')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">
                법정 기준 <span class="lp-badge lp-badge--legal">법정</span>
              </span>
              <span class="lp-option__sub">3년차부터 2년마다 +1일</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis5TenureMode === 'CUSTOM' }"
            @click="fnSelectTenureMode('CUSTOM')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">회사 정책 (직접 입력)</span>
              <span class="lp-option__sub"
                >시작 연도와 가산 주기를 직접 지정</span
              >
            </span>
          </button>
        </div>

        <div class="lp-subbox">
          <div class="lp-inline-row">
            <input
              type="number"
              class="lp-num-input"
              v-model="axis5StartYear"
              min="1"
              max="3"
              :disabled="!tenureCustom"
            />
            <span class="lp-inline-label">년차부터</span>
            <input
              type="number"
              class="lp-num-input"
              v-model="axis5Interval"
              min="1"
              max="2"
              :disabled="!tenureCustom"
            />
            <span class="lp-inline-label">년마다 +1일 가산</span>
          </div>
          <p class="lp-strong-note">
            ※ 법정 위반 방지를 위해 시작 연도 최대 3, 가산 주기 최대 2로 제한
            (법정보다 회사에 유리하게 설정 불가)
          </p>
        </div>

        <div class="lp-subbox lp-inline-row">
          <span class="lp-inline-label">최대 연차일수</span>
          <input
            type="number"
            class="lp-num-input"
            v-model="axis5MaxDays"
            min="25"
            max="40"
          />
          <span class="lp-inline-label">일</span>
          <span class="lp-badge lp-badge--legal"
            >법정 25일 (이하 설정 불가)</span
          >
        </div>

        <!-- 실시간 부여 미리보기 -->
        <div class="lp-preview">
          <p class="lp-preview__title">
            <svg
              viewBox="0 0 24 24"
              width="12"
              height="12"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
            {{ tenurePreviewTitle }}
          </p>
          <div class="lp-preview__grid">
            <span v-for="(item, idx) in tenurePreview" :key="idx">
              {{ item.label }}:
              <strong
                >{{ item.days }}일{{ item.isMax ? " (최대)" : "" }}</strong
              >
            </span>
          </div>
        </div>
      </section>

      <!-- ============ axis 6: 연차 유효기간 (12개월 법정 고정, prafta-028) ============ -->
      <section class="lp-card">
        <header class="lp-card__head">
          <span class="lp-num">6</span>
          <h3 class="lp-card__title">연차 유효기간</h3>
        </header>
        <p class="lp-card__desc">
          발생된 연차의 사용 기한입니다. (법정 12개월 고정)
        </p>
        <div class="lp-options">
          <button
            type="button"
            class="lp-option is-selected"
            disabled
            aria-disabled="true"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">
                12개월 <span class="lp-badge lp-badge--legal">법정 고정</span>
              </span>
              <span class="lp-option__sub">발생일로부터 1년 (변경 불가)</span>
            </span>
          </button>
        </div>
      </section>

      <!-- ============ axis 7: 사용촉진 제도 ============ -->
      <section class="lp-card">
        <header class="lp-card__head">
          <span class="lp-num">7</span>
          <h3 class="lp-card__title">연차 사용촉진 제도</h3>
        </header>
        <p class="lp-card__desc">
          미사용 연차에 대한 사용촉진 통지 자동 발송 여부를 결정합니다.
        </p>
        <div class="lp-options lp-options--2">
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis7UsePromotion === 'N' }"
            @click="fnSelectPromotion('N')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">사용 안 함</span>
              <span class="lp-option__sub">미사용 시 수당 지급 의무 발생</span>
            </span>
          </button>
          <button
            type="button"
            class="lp-option"
            :class="{ 'is-selected': axis7UsePromotion === 'Y' }"
            @click="fnSelectPromotion('Y')"
          >
            <span class="lp-radio" aria-hidden="true"></span>
            <span class="lp-option__text">
              <span class="lp-option__label">사용 (자동 통지)</span>
              <span class="lp-option__sub"
                >근속기간별 1·2차 사용촉진 자동 진행</span
              >
            </span>
          </button>
        </div>
        <div v-if="axis7UsePromotion === 'Y'" class="lp-note lp-note--info">
          <svg
            viewBox="0 0 24 24"
            width="13"
            height="13"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            <strong>1년차 이상</strong>: 법정휴가(월차/연차) 만료
            <strong>6개월 전 1차 촉진</strong>(연차 사용 계획서 요청) →
            <strong>3개월 전 2차 촉진</strong>(관리자 임의 연차일 지정).<br />
            <strong>1년차 미만</strong>: 만료
            <strong>3개월 전 1차 촉진</strong>(연차 사용 계획서 요청) →
            <strong>1개월 전 2차 촉진</strong>(관리자 임의 연차일 지정).
          </span>
        </div>
      </section>

      <!-- ============ 휴가 사용 단위 정책 ============ -->
      <div class="lp-divider">
        <span class="lp-divider__text">휴가 사용 단위 정책</span>
      </div>

      <section class="lp-card lp-usage">
        <div class="lp-usage__grid">
          <div class="lp-field">
            <label class="lp-field__label"
              >회사가 허용하는 사용 단위 (1개 선택)</label
            >
            <div class="lp-checks">
              <label class="lp-check">
                <input
                  type="radio"
                  value="FULL_DAY"
                  v-model="usageUnit"
                  :disabled="usageUnitLocked"
                />
                1일 (전일)
              </label>
              <label class="lp-check">
                <input
                  type="radio"
                  value="HALF_DAY"
                  v-model="usageUnit"
                  :disabled="usageUnitLocked"
                />
                0.5일 (반차)
              </label>
              <label class="lp-check">
                <input
                  type="radio"
                  value="HOUR_2"
                  v-model="usageUnit"
                  :disabled="usageUnitLocked"
                />
                시간차 2시간
              </label>
              <label class="lp-check">
                <input
                  type="radio"
                  value="HOUR_1"
                  v-model="usageUnit"
                  :disabled="usageUnitLocked"
                />
                시간차 1시간
              </label>
              <label class="lp-check">
                <input
                  type="radio"
                  value="MIN_30"
                  v-model="usageUnit"
                  :disabled="usageUnitLocked"
                />
                시간차 30분
              </label>
            </div>
          </div>
          <div class="lp-field">
            <label class="lp-field__label">법정연차 신청 결재</label>
            <label class="lp-check">
              <input
                type="checkbox"
                v-model="aprvUseYn"
                true-value="Y"
                false-value="N"
              />
              결재 필요 (해제 시 즉시 확정)
            </label>
          </div>
          <!-- LC-06/LC-08: 반반차(0.25일) 허용 토글 — 사용 단위(USAGE_UNIT) 계층과 독립인 회사 단위 토글 -->
          <div class="lp-field">
            <label class="lp-field__label">반반차(0.25일) 허용</label>
            <label class="lp-check">
              <input
                type="checkbox"
                v-model="allowQuarter"
                true-value="Y"
                false-value="N"
              />
              허용 (위 사용 단위와 별개로 반반차 신청 개방)
            </label>
          </div>
        </div>
        <p class="lp-strong-note">
          ※ 3번에서 <strong>"0.5일 단위 절사"</strong> 선택 시 사용 단위가
          <strong>[0.5일 (반차)]</strong>로 고정됩니다.
        </p>
        <p class="lp-strong-note">
          ※ 시간 단위 휴가는 근로자별 근무 스케줄 시간에 비례하여 자동 차감.
          휴게시간은 자동 제외되어 신청 불가.
        </p>
      </section>

      <!-- ============ 시간차 1일 환산시간 (LC-08, UI 명세 §5-A) ============ -->
      <div class="lp-divider">
        <span class="lp-divider__text">시간차 1일 환산시간</span>
      </div>

      <section class="lp-card lp-conv">
        <header class="lp-card__head">
          <h3 class="lp-card__title">1일 환산시간</h3>
        </header>
        <p class="lp-card__desc">
          시간차 연차 차감의 분모가 되는 <strong>1일 환산시간(분)</strong>을
          회사 단위로 설정합니다. 저장 즉시가 아니라 적용일 기준으로 반영됩니다.
        </p>

        <!-- loading -->
        <div v-if="convLoading" class="lp-conv-loading">조회 중…</div>

        <template v-else>
          <!-- 현재 적용값 -->
          <div class="lp-conv-current">
            <span class="lp-conv-current__label">현재 적용값</span>
            <strong class="lp-conv-current__value">
              {{ convCurrent.convMinutes }}분 ({{
                fnConvHourText(convCurrent.convMinutes)
              }})
            </strong>
            <span v-if="convCurrent.applyFromDate" class="lp-badge lp-badge--cond">
              적용일 {{ fnConvDateText(convCurrent.applyFromDate) }}
            </span>
            <span v-else class="lp-badge lp-badge--cond">기본 480분 적용 중</span>
          </div>

          <!-- 새 환산시간 + 적용일 입력 -->
          <div class="lp-inline-row lp-conv-input-row">
            <span class="lp-inline-label">새 환산시간</span>
            <input
              type="number"
              class="lp-num-input lp-conv-num"
              v-model="convNewMinutes"
              min="60"
              max="1440"
              step="30"
            />
            <span class="lp-inline-label">분</span>
            <span class="lp-inline-label lp-conv-date-label">적용일</span>
            <CalendarSrch v-model="convApplyDate" :min-date="convMinDate" />
            <button
              class="btn btn-primary"
              type="button"
              :disabled="convSaving"
              @click="fnSaveConversion"
            >
              저장
            </button>
          </div>

          <div class="lp-note lp-note--info">
            <svg
              viewBox="0 0 24 24"
              width="13"
              height="13"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span>
              <strong>적용일 이후 신청분부터 반영됩니다. 과거 신청분은
              재계산되지 않습니다.</strong>
            </span>
          </div>
          <p class="lp-strong-note">
            ※ <strong>30분 단위 신청이 유한소수가 되는 값만 허용됩니다
            (예: 240, 300, 480, 600, 750, 960)</strong>. 60~1440 사이 정수만
            입력할 수 있으며, 허용되지 않는 값(360/420/540 등)은 저장이
            거부됩니다.
          </p>

          <!-- 변경 이력 -->
          <p class="lp-conv-hist-title">변경 이력</p>
          <div v-if="convHistory.length > 0" class="lp-conv-table-wrap">
            <table class="lp-conv-table">
              <thead>
                <tr>
                  <th>적용일</th>
                  <th class="is-right">환산시간</th>
                  <th>등록자</th>
                  <th>등록일시</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="h in convHistory" :key="h.applyFromDate">
                  <td>{{ fnConvDateText(h.applyFromDate) }}</td>
                  <td class="is-right">
                    {{ h.dailyConvMinutes }}분 ({{
                      fnConvHourText(h.dailyConvMinutes)
                    }})
                  </td>
                  <td>{{ h.updateNo || h.insertNo || "-" }}</td>
                  <td>{{ h.updateDate || h.insertDate || "-" }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="lp-conv-empty">
            변경 이력이 없습니다. (기본 480분 적용 중)
          </p>
        </template>
      </section>

      <!-- ============ 고급 기능 ============ -->
      <div class="lp-divider">
        <span class="lp-divider__text">고급 기능</span>
      </div>

      <section class="lp-extra-card">
        <div class="lp-extra__left">
          <span class="lp-extra__icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
              />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </span>
          <div>
            <p class="lp-extra__title">정책 변경 영향 분석</p>
            <p class="lp-extra__sub">
              정책 변경 전에 영향받는 직원과 회사 추가 부담을 미리 분석
            </p>
          </div>
        </div>
        <button class="btn btn-second" @click="fnGoImpactAnalysis">
          분석 실행
        </button>
      </section>

      <!-- ============ 하단 버튼군 ============ -->
      <div class="lp-footer">
        <button class="btn btn-second btn-lg" @click="fnCancel">취소</button>
        <button
          class="btn btn-primary btn-lg"
          :disabled="isLoading"
          @click="fnSave"
        >
          저장
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  computed,
  watch,
  defineProps,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
import { formatLeaveMinutes } from "@/utils/leaveFormat";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";
import LeavePolicyPreviewPop from "./popup/LeavePolicyPreviewPop.vue";
import LeavePolicyHistoryPop from "./popup/LeavePolicyHistoryPop.vue";
import LeavePolicyImpactPop from "./popup/LeavePolicyImpactPop.vue";

// ================ Options ================
defineOptions({ name: "Baim_07" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

// ================ 상수 ================
const AXIS5_BASE_DAYS = 15; // 본연차 기본 일수 (§4.7.3)
const PREVIEW_HIRE_DATE = "2025-07-15"; // 부여 시점 미리보기 기준 입사일 (§4.8.2 고정값)

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });

// 활성 정책 식별자. null=신규(POST), 값 존재=변경(PUT /baim07/policy/{policySeq})
const policySeq = ref(null);

// --- 7개 axis (UI 번호 ↔ 백엔드 필드 매핑은 UI 스펙 §4 참조) ---
const axis1GrantBase = ref("HIRE_DATE"); // UI 1번
const axis3FirstYearMethod = ref("MONTHLY_ONLY"); // UI 2번 (백엔드 axis3)
const axis4ProrateRounding = ref("CEIL"); // UI 3번 (백엔드 axis4)
const axis2FiscalStartMm = ref("01"); // UI 4번 (백엔드 axis2)
const axis2FiscalStartDd = ref("01"); // UI 4번 (백엔드 axis2)
const axis5TenureMode = ref("LEGAL"); // UI 5번
const axis5StartYear = ref(3);
const axis5Interval = ref(2);
const axis5MaxDays = ref(25);
const axis6ValidityMonths = ref(12); // UI 6번
const axis7UsePromotion = ref("N"); // UI 7번

// --- 메타 ---
const applyFromDate = ref(""); // YYYYMMDD. 저장 직전 내일(오늘+1일)로 세팅 (§7.2 D-1)
const changeReason = ref("");

// --- 사용 단위 정책 (TB_LEAVE_USAGE_POLICY) ---
// 단일 선택(prafta-024): FULL_DAY / HALF_DAY / HOUR_2 / HOUR_1 / MIN_30
const usageUnit = ref("FULL_DAY");
// 법정연차 신청 결재 여부 (prafta-019-E 결정 #2). 'Y'=결재라인, 'N'=즉시확정
const aprvUseYn = ref("N");
// LC-06/LC-08: 반반차(0.25일) 허용 토글 (TB_LEAVE_USAGE_POLICY.ALLOW_QUARTER, 기본 'N')
const allowQuarter = ref("N");

// --- UI 상태 ---
const isLoading = ref(false);

// --- 시간차 1일 환산시간 (LC-08, GET/POST /baim07/conversion) ---
const convLoading = ref(false);
const convSaving = ref(false);
// 현재 적용값: convMinutes(미설정 회사는 서버가 480 폴백), applyFromDate(미설정이면 null)
const convCurrent = ref({ convMinutes: 480, applyFromDate: null });
// 변경 이력 (적용일 내림차순 — 미래 예약분 포함, LeaveConversionPolicyVO[])
const convHistory = ref([]);
// 입력 폼: 새 환산시간(분) + 적용일(CalendarSrch, YYYY-MM-DD)
const convNewMinutes = ref(480);
const convApplyDate = ref("");

// ================ Computed ================
// 1번 매트릭스(§4.3 / prafta-029): axis1=HIRE_DATE면 PRORATE/NEXT_YEAR_BULK 비활성,
//   axis1=FISCAL_YEAR면 MONTHLY_ONLY 비활성(회계연도는 PRORATE/NEXT_YEAR_BULK만 허용)
const axis2Disabled = computed(() => {
  const fiscal = axis1GrantBase.value === "FISCAL_YEAR";
  const hireDate = axis1GrantBase.value === "HIRE_DATE";
  return { MONTHLY_ONLY: fiscal, PRORATE: hireDate, NEXT_YEAR_BULK: hireDate };
});

// UI 3번(반올림) 활성 = UI 2번이 PRORATE일 때만 (§4.4)
const axis3Active = computed(() => axis3FirstYearMethod.value === "PRORATE");

// UI 4번(회계연도 시작일) 활성 = axis1=FISCAL_YEAR일 때만 (§4.4)
const axis4Active = computed(() => axis1GrantBase.value === "FISCAL_YEAR");

// UI 3번=HALF_DAY(0.5일 단위 절사) → 사용 단위 HALF_DAY 강제 + 잠금 (prafta-024 결정 2b)
const usageUnitLocked = computed(
  () => axis4ProrateRounding.value === "HALF_DAY"
);

// 5번 CUSTOM 여부
const tenureCustom = computed(() => axis5TenureMode.value === "CUSTOM");

// LC-08: 환산시간 적용일 선택 하한 = 오늘 (백엔드 검증: 오늘 이후만 — F4 소급 재계산 없음)
const convMinDate = computed(() => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
});

// 5번 미리보기 제목
const tenurePreviewTitle = computed(() => {
  if (axis5TenureMode.value === "LEGAL") {
    return "부여 미리보기 (법정 기준)";
  }
  return `부여 미리보기 (${axis5StartYear.value}년차부터 ${axis5Interval.value}년마다 +1일)`;
});

// 5번 실시간 부여 미리보기 (§4.7.3 / §4.7.4 계산식, HTML 시안 updatePreview 이식)
//   bonus = year>=start ? floor((year-start)/interval)+1 : 0
//   total = min(baseDays + bonus, maxDays)
//   milestone(1~2년차 + 가산 발생 연도)만 표시, total===maxDays 도달 시 break.
const tenurePreview = computed(() => {
  const start = toIntOr(axis5StartYear.value, 3);
  const interval = Math.max(1, toIntOr(axis5Interval.value, 2));
  const maxDays = toIntOr(axis5MaxDays.value, 25);

  // HTML 시안 updatePreview 와 동일: milestone(1~2년차 + 가산 발생 연도)마다 1행씩.
  const result = [];
  for (let year = 1; year <= 30; year++) {
    const bonus = year >= start ? Math.floor((year - start) / interval) + 1 : 0;
    const total = Math.min(AXIS5_BASE_DAYS + bonus, maxDays);
    const isMilestone =
      year <= 2 || (year >= start && (year - start) % interval === 0);
    if (!isMilestone) continue;

    const isMax = total === maxDays;
    result.push({ label: `${year}년차`, days: total, isMax });
    if (isMax) break;
  }
  return result;
});

// ================ Watch (조건부 활성/매트릭스/HALF_DAY 잠금 자동 보정) ================
// R1: axis1 변경 시 UI2의 비활성 옵션 선택값을 자동 보정 (§4.3 / prafta-029)
//   HIRE_DATE면 PRORATE/NEXT_YEAR_BULK → MONTHLY_ONLY로,
//   FISCAL_YEAR면 MONTHLY_ONLY → PRORATE로 전환(대칭 보정).
watch(axis1GrantBase, (val) => {
  if (val === "HIRE_DATE") {
    if (
      axis3FirstYearMethod.value === "PRORATE" ||
      axis3FirstYearMethod.value === "NEXT_YEAR_BULK"
    ) {
      axis3FirstYearMethod.value = "MONTHLY_ONLY";
    }
  } else if (val === "FISCAL_YEAR") {
    if (axis3FirstYearMethod.value === "MONTHLY_ONLY") {
      axis3FirstYearMethod.value = "PRORATE";
    }
  }
});

// R5: UI3=HALF_DAY(0.5일 단위 절사) 선택 시 사용 단위를 HALF_DAY로 강제 고정
//     (잠금은 usageUnitLocked computed가 radio disabled로 처리) (prafta-024 결정 2b)
watch(usageUnitLocked, (locked) => {
  if (locked) {
    usageUnit.value = "HALF_DAY";
  }
});

// R6: 5번 LEGAL이면 n=3, m=2 고정 리셋 (§4.7.2)
watch(axis5TenureMode, (val) => {
  if (val === "LEGAL") {
    axis5StartYear.value = 3;
    axis5Interval.value = 2;
  }
});

// ================ Life Cycle Functions ================
onMounted(() => {
  fnButtonControll();
  fnSearch();
  fnLoadConversion();
});

// ================ API Functions ================
const fnSearch = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/baim07/policy/active");
    const policy = response.data?.policy ?? null;

    if (policy) {
      fnApplyPolicyToState(policy);
    } else {
      fnResetToDefault();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

const fnSave = async () => {
  // 1) 1차 검증
  if (!fnValidate()) return;

  // 2) 변경 사유 입력 (ReasonInputModal) → changeReason → 본 저장 진행
  openPop(ReasonInputModal, {
    title: "정책 변경 사유 입력",
    message: "연차 부여 정책 변경 사유를 입력해 주세요.",
    placeholder: "변경 사유를 입력해 주세요.",
    required: true,
    onConfirm: async (reason) => {
      closePop();
      changeReason.value = reason;
      await fnSubmit();
    },
    onCancel: () => {
      closePop();
    },
  });
};

// 실제 저장 호출 (변경 사유 확정 이후)
const fnSubmit = async () => {
  // 3) 적용 시작일 = 내일(오늘+1일) (§7.2 D-1, 백엔드 과거 소급 금지 회피)
  applyFromDate.value = fnTomorrowYyyymmdd();

  // 4) body 구성
  const body = fnBuildSaveRequest();

  isLoading.value = true;
  try {
    // 5) policySeq 유무로 분기 (§7.1)
    if (policySeq.value != null) {
      await axios.put(`/webApi/baim07/policy/${policySeq.value}`, body);
    } else {
      await axios.post("/webApi/baim07/policy", body);
    }
    await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    await fnSearch();
  } catch (err) {
    // 백엔드 검증 거부(400/403)는 resolveApiErrorMessage 로 표시
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// 저장 요청 body 구성 (axis↔백엔드 필드 매핑 + 정규화). UI 스펙 §7.2 참조.
const fnBuildSaveRequest = () => {
  const body = {
    // 프리셋 제거 → 항상 CUSTOM 고정
    policyPreset: "CUSTOM",
    // 통합 스펙 2번 옵션에 PREGRANT 없음 → 항상 N 고정
    axis3PregrantYn: "N",

    axis1GrantBase: axis1GrantBase.value,
    axis3FirstYearMethod: axis3FirstYearMethod.value,
    axis5TenureMode: axis5TenureMode.value,
    axis5StartYear: toIntOr(axis5StartYear.value, 3),
    axis5Interval: toIntOr(axis5Interval.value, 2),
    axis5MaxDays: toIntOr(axis5MaxDays.value, 25),
    axis6ValidityMonths: toIntOr(axis6ValidityMonths.value, 12),
    axis7UsePromotion: axis7UsePromotion.value,

    // UI3(반올림): axis3Active(=UI2 PRORATE) 아니면 'CEIL' 정규화 (백엔드도 강제)
    axis4ProrateRounding: axis3Active.value
      ? axis4ProrateRounding.value
      : "CEIL",

    // 사용 단위(단일): HALF_DAY 잠금(3번=0.5일 절사) 시 HALF_DAY 강제 (prafta-024 결정 2b)
    usageUnit: usageUnitLocked.value ? "HALF_DAY" : usageUnit.value,
    aprvUseYn: aprvUseYn.value,
    // LC-06: 반반차(0.25일) 허용 토글 — USAGE_UNIT 계층과 독립 (Y/N 외 값은 서버가 'N' 정규화)
    allowQuarter: allowQuarter.value,

    applyFromDate: applyFromDate.value,
    changeReason: changeReason.value,
  };

  // UI4(회계연도 시작일): axis4Active(=axis1 FISCAL_YEAR)일 때만 전송. 그 외 미전송(백엔드 NULL 정규화)
  if (axis4Active.value) {
    body.axis2FiscalStartMm = padMmDd(axis2FiscalStartMm.value);
    body.axis2FiscalStartDd = padMmDd(axis2FiscalStartDd.value);
  }

  return body;
};

// --- 변경 이력 (D-2): GET /baim07/policy/history → 모달 팝업 ---
const fnOpenHistory = () => {
  openPop(LeavePolicyHistoryPop, {});
};

// --- LC-08: 시간차 1일 환산시간 조회 (현재 적용값 + 변경 이력) ---
const fnLoadConversion = async () => {
  convLoading.value = true;
  try {
    const res = await axios.get("/webApi/baim07/conversion");
    const d = res.data || {};
    convCurrent.value = {
      convMinutes: d.currentConvMinutes ?? 480,
      applyFromDate: d.currentApplyFromDate ?? null,
    };
    convHistory.value = Array.isArray(d.history) ? d.history : [];
    // 입력 기본값 = 현재 적용값 (사용자가 값만 바꿔 저장하도록)
    convNewMinutes.value = convCurrent.value.convMinutes;
  } catch (err) {
    // error 상태: 공통 alert (UI 명세 §5-A)
    const msg = resolveApiErrorMessage(
      err,
      "환산시간 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    convLoading.value = false;
  }
};

// --- LC-08: 시간차 1일 환산시간 저장 (신규 적용일 INSERT / 같은 적용일 UPDATE) ---
const fnSaveConversion = async () => {
  // 1차 검증(UX 게이트) — 최종 권위는 백엔드(유한소수 방어 포함)
  const minutesRaw = convNewMinutes.value;
  const minutes = Number(minutesRaw);
  if (!Number.isInteger(minutes) || minutes < 60 || minutes > 1440) {
    await proxy.$alert("환산시간은 60~1440분 범위의 정수로 입력해 주세요.");
    return;
  }
  if (!convApplyDate.value) {
    await proxy.$alert("적용일을 선택해 주세요.");
    return;
  }
  const ymd = String(convApplyDate.value).replace(/-/g, "");
  // CalendarSrch minDate 로 차단되지만 방어적으로 재검증 (백엔드도 fail-closed)
  if (!/^\d{8}$/.test(ymd) || ymd < convMinDate.value.replace(/-/g, "")) {
    await proxy.$alert("적용일은 오늘 이후 날짜만 선택할 수 있습니다.");
    return;
  }

  convSaving.value = true;
  try {
    const res = await axios.post("/webApi/baim07/conversion", {
      applyFromDate: ymd,
      dailyConvMinutes: minutes,
    });
    // 성공: 서버 안내 문구(적용일 이후 신청분부터 반영) 우선 표시 + 이력 갱신
    await proxy.$alert(res.data?.message || getMessage(MSG.SAVE_SUCCESS));
    convApplyDate.value = "";
    await fnLoadConversion();
  } catch (err) {
    // 허용되지 않는 값(360/420/540 등)은 서버 400 메시지를 그대로 표시
    const msg = resolveApiErrorMessage(
      err,
      "환산시간 저장 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    convSaving.value = false;
  }
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  // 헤더 버튼: 조회만 노출. 저장은 하단 [취소][저장]만 동작 (D-3, 헤더 저장 중복 금지)
  localButtons.value.search = "Y";
  localButtons.value.save = "N";
  localButtons.value.create = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// 활성 정책(LeavePolicyVO)을 화면 상태에 매핑
const fnApplyPolicyToState = (p) => {
  policySeq.value = p.policySeq ?? null;
  axis1GrantBase.value = p.axis1GrantBase ?? "HIRE_DATE";
  axis3FirstYearMethod.value = p.axis3FirstYearMethod ?? "MONTHLY_ONLY";
  // prafta-029: 잔존 비표준 조합(FISCAL_YEAR+MONTHLY_ONLY)이 로드되면 PRORATE로 명시 보정(watch에 의존하지 않음).
  if (
    axis1GrantBase.value === "FISCAL_YEAR" &&
    axis3FirstYearMethod.value === "MONTHLY_ONLY"
  ) {
    axis3FirstYearMethod.value = "PRORATE";
  }
  axis4ProrateRounding.value = p.axis4ProrateRounding ?? "CEIL";
  axis2FiscalStartMm.value = p.axis2FiscalStartMm ?? "01";
  axis2FiscalStartDd.value = p.axis2FiscalStartDd ?? "01";
  axis5TenureMode.value = p.axis5TenureMode ?? "LEGAL";
  axis5StartYear.value = p.axis5StartYear ?? 3;
  axis5Interval.value = p.axis5Interval ?? 2;
  axis5MaxDays.value = p.axis5MaxDays ?? 25;
  axis6ValidityMonths.value = 12; // prafta-028: 연차 유효기간 12개월 법정 고정 (구버전 24 데이터도 12로 정규화)
  axis7UsePromotion.value = p.axis7UsePromotion ?? "N";

  // 사용 단위(단일): 미지정/구버전 데이터는 FULL_DAY로 폴백
  usageUnit.value = p.usageUnit || "FULL_DAY";
  aprvUseYn.value = p.aprvUseYn ?? "N";
  // LC-06: 반반차 허용 토글 (미지정/구버전 데이터는 'N' — fail-closed)
  allowQuarter.value = p.allowQuarter ?? "N";
};

// 신규 작성 모드(활성 정책 없음) — 기본값으로 초기화
const fnResetToDefault = () => {
  policySeq.value = null;
  axis1GrantBase.value = "HIRE_DATE";
  axis3FirstYearMethod.value = "MONTHLY_ONLY";
  axis4ProrateRounding.value = "CEIL";
  axis2FiscalStartMm.value = "01";
  axis2FiscalStartDd.value = "01";
  axis5TenureMode.value = "LEGAL";
  axis5StartYear.value = 3;
  axis5Interval.value = 2;
  axis5MaxDays.value = 25;
  axis6ValidityMonths.value = 12;
  axis7UsePromotion.value = "N";
  usageUnit.value = "FULL_DAY";
  aprvUseYn.value = "N";
  allowQuarter.value = "N";
};

// --- axis 선택 핸들러 (UI 토글; 매트릭스 보정은 watch 가 담당) ---
const fnSelectAxis1 = (val) => {
  axis1GrantBase.value = val; // R1 보정은 watch(axis1GrantBase)
};

const fnSelectFirstYear = (val) => {
  // 비활성 옵션 클릭은 button:disabled 로 차단되지만, 방어적으로 무시 처리.
  if (val === "MONTHLY_ONLY" && axis2Disabled.value.MONTHLY_ONLY) return;
  if (val === "PRORATE" && axis2Disabled.value.PRORATE) return;
  if (val === "NEXT_YEAR_BULK" && axis2Disabled.value.NEXT_YEAR_BULK) return;
  axis3FirstYearMethod.value = val;
};

const fnSelectRounding = (val) => {
  if (!axis3Active.value) return; // 조건부 비활성 시 무시
  axis4ProrateRounding.value = val; // HALF_DAY 시 사용 단위 강제는 watch(usageUnitLocked)
};

const fnSelectTenureMode = (val) => {
  axis5TenureMode.value = val; // LEGAL 강제 리셋은 watch(axis5TenureMode)
};

// (prafta-028) fnSelectValidity 제거 — 연차 유효기간은 12개월 법정 고정(선택 불가).

const fnSelectPromotion = (val) => {
  axis7UsePromotion.value = val;
};

// --- 1차 검증 (UX 게이트; 최종 권위는 백엔드 validateAxisMatrix). UI 스펙 §6 ---
const fnValidate = () => {
  // 매트릭스: axis1=HIRE_DATE면 axis3는 MONTHLY_ONLY만 (UI에서 비활성으로 강제되지만 방어 검증)
  if (
    axis1GrantBase.value === "HIRE_DATE" &&
    axis3FirstYearMethod.value !== "MONTHLY_ONLY"
  ) {
    proxy.$alert(
      "입사일 기준에서는 입사 첫해 처리 방식으로 [월차만 부여]만 선택할 수 있습니다."
    );
    return false;
  }

  // 매트릭스(prafta-029): axis1=FISCAL_YEAR면 axis3는 PRORATE/NEXT_YEAR_BULK만 (MONTHLY_ONLY 금지)
  if (
    axis1GrantBase.value === "FISCAL_YEAR" &&
    axis3FirstYearMethod.value === "MONTHLY_ONLY"
  ) {
    proxy.$alert(
      "회계연도 기준에서는 입사 첫해 처리 방식으로 [비례 부여] 또는 [차년도 일괄 부여]만 선택할 수 있습니다."
    );
    return false;
  }

  // max_days >= 25
  const maxDays = toIntOr(axis5MaxDays.value, 0);
  if (maxDays < 25) {
    proxy.$alert("최대 연차일수는 법정 기준 25일 이상이어야 합니다.");
    return false;
  }

  // CUSTOM 범위: 1<=startYear<=3, 1<=interval<=2
  if (axis5TenureMode.value === "CUSTOM") {
    const sy = toIntOr(axis5StartYear.value, 0);
    const iv = toIntOr(axis5Interval.value, 0);
    if (sy < 1 || sy > 3) {
      proxy.$alert("가산 시작 연차는 1~3 범위로 입력해 주세요.");
      return false;
    }
    if (iv < 1 || iv > 2) {
      proxy.$alert("가산 주기는 1~2 범위로 입력해 주세요.");
      return false;
    }
  }

  // 회계연도 시작일: axis1=FISCAL_YEAR면 mm 1~12, dd 1~31
  if (axis4Active.value) {
    const mm = toIntOr(axis2FiscalStartMm.value, 0);
    const dd = toIntOr(axis2FiscalStartDd.value, 0);
    if (mm < 1 || mm > 12) {
      proxy.$alert("회계연도 시작월은 1~12 범위로 입력해 주세요.");
      return false;
    }
    if (dd < 1 || dd > 31) {
      proxy.$alert("회계연도 시작일은 1~31 범위로 입력해 주세요.");
      return false;
    }
  }

  // applyFromDate: 저장 직전 fnSubmit 에서 내일로 세팅하므로 항상 8자리 보장됨.
  //   (별도 입력 UI 가 없는 통합 화면 정책. §7.2 D-1)
  return true;
};

// --- 부여 시점 미리보기 팝업 (클라이언트 계산만, 신규 엔드포인트 호출 금지) ---
const fnOpenPreview = () => {
  openPop(LeavePolicyPreviewPop, {
    axis1GrantBase: axis1GrantBase.value,
    fiscalStartMm: padMmDd(axis2FiscalStartMm.value),
    fiscalStartDd: padMmDd(axis2FiscalStartDd.value),
    prorateRounding: axis4ProrateRounding.value,
    hireDate: PREVIEW_HIRE_DATE,
  });
};

// --- 정책 변경 영향 분석 (화면 8) ---
//   메인 세션 결정 D-1: 라우팅(DB 메뉴 기반) 대신 풀스크린 모달로 띄운다.
//   타깃 axis 조합 + 현재 활성 policySeq 를 props 로 직접 전달(store/route/TB_MENU 미사용).
//   닫힐 때(정책 변경 진행 성공 포함) 폼을 재조회하여 최신 활성 정책으로 동기화한다.
const fnGoImpactAnalysis = () => {
  // 1차 검증: 잘못된 axis 조합으로 분석 화면을 띄우지 않도록 저장 검증과 동일하게 게이트
  if (!fnValidate()) return;

  //   useModal은 onClose를 자체 닫기 콜백으로 강제 덮어쓰므로(부모 onClose는 무시됨),
  //   정책 변경 성공 시 재조회는 모달이 별도로 emit하는 'saved' 이벤트(→ onSaved)로 받는다.
  openPop(LeavePolicyImpactPop, {
    targetPolicy: fnBuildTargetForImpact(),
    policySeq: policySeq.value,
    onSaved: () => {
      // 모달 내 [정책 변경 진행] 성공 → 최신 활성 정책으로 폼 재조회
      fnSearch();
    },
  });
};

// 영향 분석/정책 변경 진행에 넘길 타깃 axis 조합 구성.
//   fnBuildSaveRequest 와 동일한 정규화 규칙을 적용하되, applyFromDate/changeReason 은
//   모달에서 사용자 입력으로 채우므로 제외한다(중복 정의 방지를 위해 동일 규칙을 그대로 사용).
const fnBuildTargetForImpact = () => {
  const target = {
    policyPreset: "CUSTOM",
    axis3PregrantYn: "N",
    axis1GrantBase: axis1GrantBase.value,
    axis3FirstYearMethod: axis3FirstYearMethod.value,
    axis5TenureMode: axis5TenureMode.value,
    axis5StartYear: toIntOr(axis5StartYear.value, 3),
    axis5Interval: toIntOr(axis5Interval.value, 2),
    axis5MaxDays: toIntOr(axis5MaxDays.value, 25),
    axis6ValidityMonths: toIntOr(axis6ValidityMonths.value, 12),
    axis7UsePromotion: axis7UsePromotion.value,
    axis4ProrateRounding: axis3Active.value
      ? axis4ProrateRounding.value
      : "CEIL",
    usageUnit: usageUnitLocked.value ? "HALF_DAY" : usageUnit.value,
    aprvUseYn: aprvUseYn.value,
    // LC-06: 반반차 토글 — 저장 body 와 동일 규칙으로 전달
    allowQuarter: allowQuarter.value,
  };

  if (axis4Active.value) {
    target.axis2FiscalStartMm = padMmDd(axis2FiscalStartMm.value);
    target.axis2FiscalStartDd = padMmDd(axis2FiscalStartDd.value);
  }

  return target;
};

// --- 취소: 변경 사항 폐기 후 재조회 ---
const fnCancel = async () => {
  const ok = await proxy.$confirm(
    "변경 사항을 취소하고 마지막 저장 상태로 되돌리시겠습니까?"
  );
  if (!ok) return;
  await fnSearch();
};

// ================ 내부 유틸 ================
// LC-08: 환산시간(분) → "8시간"/"7시간 30분" 표시 (leaveFormat 단일 출처)
const fnConvHourText = (minutes) => formatLeaveMinutes(minutes);

// LC-08: 적용일(YYYYMMDD) → "YYYY.MM.DD" 표시
const fnConvDateText = (yyyymmdd) => {
  const s = String(yyyymmdd || "");
  if (s.length !== 8) return s || "-";
  return formatYmdDot(s);
};

const toIntOr = (v, def) => {
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? def : n;
};
// 1자리 월/일을 2자리(zero-pad)로 정규화 ("1" → "01")
const padMmDd = (v) => String(toIntOr(v, 0)).padStart(2, "0");
// 오늘+1일을 YYYYMMDD 문자열로
const fnTomorrowYyyymmdd = () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
};
</script>

<style scoped>
.leave-policy {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
}

/* ===== 페이지 액션 영역 (변경 이력) ===== */
.lp-page-actions {
  display: flex;
  justify-content: flex-end;
}

/* ===== 미리보기 안내 카드 ===== */
.lp-help-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  background: rgba(22, 163, 74, 0.06);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.75rem 1rem;
}

.lp-help-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  color: var(--color-primary-pressed);
}

.lp-help-icon {
  display: inline-flex;
  flex-shrink: 0;
}

.lp-help-text {
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--color-text);
  margin: 0;
}

.lp-help-text strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

.lp-help-btn {
  flex-shrink: 0;
}

/* ===== axis 카드 ===== */
.lp-card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
  padding: 1.125rem 1.25rem;
}

.lp-card.is-conditional {
  opacity: 0.75;
}

.lp-card__head {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 0.5rem;
}

.lp-num {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 0.75rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lp-card__title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin: 0;
}

.lp-card__desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0 0 0.75rem;
}

.lp-card__desc strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

/* ===== 배지 ===== */
.lp-badge {
  font-size: 0.625rem;
  padding: 0.0625rem 0.375rem;
  border-radius: var(--btn-radius);
  font-weight: 500;
}

.lp-badge--legal {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.lp-badge--cond {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

/* ===== 옵션 그리드 ===== */
.lp-options {
  display: grid;
  gap: 0.5rem;
}

.lp-options--2 {
  grid-template-columns: repeat(2, 1fr);
}

.lp-options--3 {
  grid-template-columns: repeat(3, 1fr);
}

.lp-options--4 {
  grid-template-columns: repeat(4, 1fr);
}

.lp-option {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
  cursor: pointer;
  text-align: left;
  font-family: "Pretendard", sans-serif;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.lp-option:hover:not(.is-disabled) {
  border-color: var(--color-border-strong);
}

.lp-option.is-selected {
  border: 2px solid var(--color-primary);
  background: rgba(22, 163, 74, 0.04);
  padding: calc(0.625rem - 1px) calc(0.75rem - 1px);
}

.lp-option.is-disabled {
  background: var(--color-bg);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.lp-radio {
  width: 0.875rem;
  height: 0.875rem;
  border-radius: 50%;
  border: 1.5px solid var(--color-border-strong);
  flex-shrink: 0;
  margin-top: 0.125rem;
  position: relative;
}

.lp-option.is-selected .lp-radio {
  border-color: var(--color-primary);
}

.lp-option.is-selected .lp-radio::after {
  content: "";
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--color-primary);
}

.lp-option__text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.0625rem;
}

.lp-option__label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.lp-option.is-disabled .lp-option__label {
  color: var(--color-text-muted);
}

.lp-option__sub {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

/* ===== 안내 박스 ===== */
.lp-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  border-radius: var(--btn-radius);
  padding: 0.5rem 0.75rem;
  margin-top: 0.625rem;
  font-size: 0.6875rem;
  line-height: 1.5;
}

.lp-note svg {
  flex-shrink: 0;
  margin-top: 0.0625rem;
}

.lp-note--info {
  background: rgba(22, 163, 74, 0.06);
  color: var(--color-primary-pressed);
}

.lp-note--legal {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.lp-note strong {
  font-weight: 600;
}

.lp-strong-note {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0.5rem 0 0;
}

.lp-strong-note strong {
  color: var(--color-warning-text);
  font-weight: 600;
}

/* ===== 인라인 입력 행 ===== */
.lp-inline-row {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.lp-inline-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.lp-num-input {
  width: 3.75rem;
  height: 2rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0 0.5rem;
  font-size: 0.8125rem;
  text-align: center;
  font-family: "Pretendard", sans-serif;
  background: var(--color-surface);
  color: var(--color-text-strong);
}

.lp-num-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

.lp-num-input:disabled {
  background: var(--color-bg);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

/* ===== 근속 가산 보조 박스 ===== */
.lp-subbox {
  margin-top: 0.625rem;
  margin-left: 1.75rem;
  padding: 0.75rem 0.875rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

/* ===== 5번 실시간 미리보기 ===== */
.lp-preview {
  margin-top: 0.625rem;
  margin-left: 1.75rem;
  padding: 0.625rem 0.75rem;
  background: rgba(22, 163, 74, 0.06);
  border-radius: var(--btn-radius);
}

.lp-preview__title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-primary-pressed);
  margin: 0 0 0.375rem;
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.lp-preview__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.25rem 0.75rem;
  font-size: 0.6875rem;
  color: var(--color-primary-pressed);
}

.lp-preview__grid strong {
  font-weight: 600;
}

/* ===== 섹션 구분선 ===== */
.lp-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0.75rem 0 0.25rem;
}

.lp-divider::before,
.lp-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.lp-divider__text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.3px;
}

/* ===== 사용 단위 정책 ===== */
.lp-usage__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.lp-field {
  display: flex;
  flex-direction: column;
}

.lp-field__label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}

.lp-checks {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.lp-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text);
  cursor: pointer;
}

.lp-check input[type="checkbox"],
.lp-check input[type="radio"] {
  width: 0.875rem;
  height: 0.875rem;
  accent-color: var(--color-primary);
}

/* 사용 단위 라디오 잠금(3번=0.5일 단위 절사 시 HALF_DAY 고정) */
.lp-check input:disabled {
  cursor: not-allowed;
}

.lp-check:has(input:disabled) {
  color: var(--color-text-muted);
  cursor: not-allowed;
}

/* ===== 시간차 1일 환산시간 (LC-08) ===== */
.lp-conv-loading {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  padding: 0.75rem 0;
}

.lp-conv-current {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  background: rgba(22, 163, 74, 0.06);
  border-radius: var(--btn-radius);
  margin-bottom: 0.625rem;
}

.lp-conv-current__label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.lp-conv-current__value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.lp-conv-input-row {
  margin-bottom: 0.25rem;
}

.lp-conv-num {
  width: 5rem;
}

.lp-conv-date-label {
  margin-left: 0.5rem;
}

.lp-conv-hist-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin: 0.75rem 0 0.375rem;
  padding-top: 0.625rem;
  border-top: 1px solid var(--color-border);
}

.lp-conv-table-wrap {
  overflow-x: auto;
}

.lp-conv-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
}

.lp-conv-table th {
  text-align: left;
  padding: 0.375rem 0.5rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}

.lp-conv-table td {
  padding: 0.375rem 0.5rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.lp-conv-table th.is-right,
.lp-conv-table td.is-right {
  text-align: right;
}

.lp-conv-empty {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0;
  padding: 0.5rem 0;
}

/* ===== 고급 기능 카드 ===== */
.lp-extra-card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.lp-extra__left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.lp-extra__icon {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lp-extra__title {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-strong);
  margin: 0;
}

.lp-extra__sub {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0;
}

/* ===== 하단 버튼군 ===== */
.lp-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .lp-usage__grid,
  .lp-options--2,
  .lp-options--3,
  .lp-options--4,
  .lp-preview__grid {
    grid-template-columns: 1fr;
  }

  .lp-help-card,
  .lp-extra-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
