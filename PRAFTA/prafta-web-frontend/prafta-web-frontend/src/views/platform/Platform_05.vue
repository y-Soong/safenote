<!--
  Platform_05.vue — SMS 발송 관리 (플랫폼 운영자 전용 콘솔)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_05', MENU_VIEW='platform/Platform_05.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 탭1(발송 현황): GET /platformApi/sms/console 로 현황·상태·임계값을 한 번에 조회 →
          POST /platformApi/sms/send-policy(임계값 저장) / POST /platformApi/sms/kill-switch-release(킬스위치 해제).
  - 탭2(발송 이력): POST /platformApi/sms/send-histories/search 로 TB_SMS_AUTH_CODE 를
          기간 필터 + 서버 페이징 조회.
    ★조회인데 POST 인 이유 — 검색 조건에 휴대폰 평문이 들어간다. 쿼리스트링으로 보내면 서버가 로그를
      남기지 않아도 nginx/ALB access log · CloudFront 로그 · 브라우저 히스토리에 평문 휴대폰이 복제된다.
      바디로 보내면 그 경로가 사라진다. 휴대폰 조건을 쿼리스트링으로 되돌리지 말 것.
  - ★발송 게이트(prafta.sms.enabled)는 서버 secrets(PPURIO_ENABLED) 소관이라 화면에서 바꾸지 않는다(읽기전용 표시).
    킬스위치만 DB 상태이며 화면에서 수동 해제한다.
  - ★★[방침 변경] 개별 발송 이력은 이 플랫폼 운영자 콘솔에 한해 노출한다(휴대폰은 서버에서 마스킹).
    MBL_NO_HMAC·SEND_IP_HASH 는 여전히 어떤 응답에도 담지 않는다.
  - ★[방침 변경 / 2026-08-30 사용자 확정] 인증번호(authCd)는 미발송(SKIPPED = 게이트 OFF) 행에
    한해 서버가 내려주고 화면에 표시한다 — 게이트 OFF 환경에서 운영자가 이 화면에서 코드를 읽어
    검증 흐름을 테스트하기 위함. 실발송(SENT)·실패·대기 행은 종전대로 표시하지 않는다(응답에 null) —
    만료 전 인증번호는 그 자체로 계정 탈취(비밀번호 재설정 통과)에 쓰이는 유효 자격증명이다.
  - ★TB_SMS_AUTH_CODE 에는 CMPNY_CD 가 없다. 이 조회는 플랫폼 운영자 전용에서만 성립한다.
  - 골격: planner 작성(template + scoped style), script 로직: developer 작성(SMS2-C2 / P05H-4).
-->
<template>
  <div class="viewComm">
    <!-- 탭바 — Attd_01/User_09 표준(밑줄형 0.875rem). ★기본 탭은 기존 화면(발송 현황)이다. -->
    <div class="p05-tabs">
      <button
        type="button"
        class="p05-tab"
        :class="{ active: activeTab === 'console' }"
        @click="fnSelectTab('console')"
      >
        발송 현황
      </button>
      <button
        type="button"
        class="p05-tab"
        :class="{ active: activeTab === 'history' }"
        @click="fnSelectTab('history')"
      >
        발송 이력
      </button>
    </div>

    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnHeaderSearch"
    />

    <!-- 이력 탭 전용 조회조건. 기간을 비워도 서버가 최근 7일로 채운다(기간 상한은 없다). -->
    <div v-if="activeTab === 'history'" class="viewSearch">
      <div>
        <label>발송기간</label>
        <CalendarSrch v-model="histStartDate" />
        <span class="p05-date-sep">~</span>
        <CalendarSrch v-model="histEndDate" />
      </div>

      <div>
        <label>목적</label>
        <div class="p05-select">
          <BaseSelect v-model="histPurposeCd">
            <option value="">전체</option>
            <option value="SELF_JOIN">셀프가입</option>
            <option value="PLATFORM_LOCATION">위치정보 열람</option>
            <option value="MOBILE_CHANGE">휴대폰 변경</option>
          </BaseSelect>
        </div>
      </div>

      <div>
        <label>발송상태</label>
        <div class="p05-select">
          <BaseSelect v-model="histSendStatus">
            <option value="">전체</option>
            <option value="PENDING">대기</option>
            <option value="SENT">성공</option>
            <option value="FAILED">실패</option>
            <option value="SKIPPED">미발송</option>
          </BaseSelect>
        </div>
      </div>

      <!--
        휴대폰 검색 — 평문 입력이지만 요청 바디로만 나간다(위 상단 주석의 POST 사유).
        서버가 HMAC 으로 변환해 정확 일치로만 조회한다(부분일치 없음). 하이픈은 서버가 제거한다.
      -->
      <div>
        <label>휴대폰</label>
        <input
          v-model.trim="histMblNo"
          type="text"
          class="p05-mbl-input"
          placeholder="휴대폰 번호(정확히 일치)"
          @keyup.enter="fnHeaderSearch"
        />
      </div>
    </div>

    <div class="viewBody">
      <!-- 킬스위치 발동 배너 — 차단/제한 상황은 배너로 명시(공통 정책서 §13.3).
           ★탭 밖에 둔다: 시스템 전역 차단 상태라 어느 탭에서도 보여야 한다. -->
      <div v-if="killSwitchOn" class="p05-killswitch-banner">
        <span class="p05-killswitch-banner__text">
          발송이 중지되었습니다. {{ killSwitchAtLabel }} 발동 · {{ killSwitchReason }}
        </span>
        <button
          class="btn btn-primary p05-killswitch-banner__btn"
          :disabled="releasing"
          @click="fnReleaseKillSwitch"
        >
          해제
        </button>
      </div>

      <!-- ==================== 탭1: 발송 현황(기존 화면 그대로) ==================== -->
      <div v-show="activeTab === 'console'">
        <LoadingSpinner v-if="loading" />

        <template v-else>
          <!-- ===================== 발송 현황 ===================== -->
          <div class="table-wrapper subtitle-pane">
            <div class="subtitle">
              <span class="subtitle-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path d="M4 4h16v4H4zM4 10h10v10H4z" />
                </svg>
              </span>
              <span class="subtitle-text">발송 현황</span>
            </div>

            <div class="p05-kpi-row">
              <div class="p05-kpi">
                <span class="p05-kpi__label">최근 1시간</span>
                <span class="p05-kpi__value">{{ stat1h.total }}</span>
                <span class="p05-kpi__unit">건</span>
              </div>
              <div class="p05-kpi">
                <span class="p05-kpi__label">최근 24시간</span>
                <span class="p05-kpi__value">{{ stat24h.total }}</span>
                <span class="p05-kpi__unit">건</span>
              </div>
              <div class="p05-kpi">
                <span class="p05-kpi__label">성공 / 실패 (24h)</span>
                <span class="p05-kpi__value">{{ stat24h.sent }}</span>
                <span class="p05-kpi__sep">/</span>
                <span class="p05-kpi__value p05-kpi__value--danger">{{ stat24h.failed }}</span>
              </div>
              <div class="p05-kpi">
                <span class="p05-kpi__label">스킵 / 대기 (24h)</span>
                <span class="p05-kpi__value">{{ stat24h.skipped }}</span>
                <span class="p05-kpi__sep">/</span>
                <span class="p05-kpi__value">{{ stat24h.pending }}</span>
              </div>
            </div>

            <div class="p05-gauge-row">
              <span class="p05-gauge-label">전역 상한 소진율</span>
              <div class="p05-gauge">
                <div class="p05-gauge__fill" :class="gaugeClass" :style="gaugeStyle"></div>
              </div>
              <span class="p05-gauge-text">{{ gaugeText }}</span>
            </div>
          </div>

          <!-- ===================== 발송 상태 ===================== -->
          <div class="table-wrapper subtitle-pane">
            <div class="subtitle">
              <span class="subtitle-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path d="M12 2l9 5v10l-9 5-9-5V7z" />
                </svg>
              </span>
              <span class="subtitle-text">발송 상태</span>
            </div>

            <div class="p05-status">
              <!--
                ★게이트 토글과 "실제 발송 가능" 을 분리해 보여준다.
                  키 미주입 / base URL 이 http 면 토글은 ON 인데 전 흐름이 조용히 SKIPPED 로 흐른다.
                  토글만 보여 주면 "켜져 있으니 나가고 있다" 고 오판하게 된다.
              -->
              <div class="p05-status__row">
                <span class="p05-status__label">발송 가능</span>
                <span class="p05-badge" :class="sendableBadgeClass">{{ sendableLabel }}</span>
                <span class="p05-status__desc">{{ sendableDesc }}</span>
              </div>
              <div class="p05-status__row">
                <span class="p05-status__label">발송 게이트(설정)</span>
                <span class="p05-badge" :class="gateBadgeClass">{{ gateLabel }}</span>
                <span class="p05-status__desc">
                  읽기전용 — 변경은 서버 secrets 의 PPURIO_ENABLED
                </span>
              </div>
              <div class="p05-status__row">
                <span class="p05-status__label">킬스위치</span>
                <span class="p05-badge" :class="killSwitchBadgeClass">{{ killSwitchLabel }}</span>
                <button
                  v-if="killSwitchOn"
                  class="btn btn-primary p05-status__btn"
                  :disabled="releasing"
                  @click="fnReleaseKillSwitch"
                >
                  해제
                </button>
              </div>
              <div class="p05-status__row">
                <span class="p05-status__label">최근 발동</span>
                <span class="p05-status__value">{{ killSwitchAtLabel }}</span>
              </div>
              <div class="p05-status__row">
                <span class="p05-status__label">발동 사유</span>
                <span class="p05-status__value">{{ killSwitchReason }}</span>
              </div>
              <div class="p05-status__row">
                <span class="p05-status__label">최근 해제</span>
                <span class="p05-status__value">{{ killSwitchReleaseLabel }}</span>
              </div>
            </div>
          </div>

          <!-- ===================== 발송 임계값 ===================== -->
          <div class="table-wrapper subtitle-pane">
            <div class="subtitle">
              <span class="subtitle-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path d="M4 6h16M4 12h16M4 18h10" />
                </svg>
              </span>
              <span class="subtitle-text">발송 임계값</span>
              <button
                class="btn btn-primary p05-save-btn"
                :disabled="saving || !policyLoaded"
                @click="fnSavePolicy"
              >
                저장
              </button>
            </div>

            <div class="p05-form">
              <span v-if="!policyLoaded" class="p05-form__msg">
                정책 설정을 불러오지 못했습니다. 관리자에게 문의해 주세요.
              </span>

              <div class="p05-form__row">
                <span class="p05-form__label">번호별 연속 발송 간격</span>
                <input
                  v-model.number="policy.phoneWindowSec"
                  type="number"
                  min="1"
                  max="59"
                  step="1"
                  class="p05-input"
                />
                <span class="p05-form__unit">초</span>
                <span class="p05-form__desc">
                  1~59. 프론트 재발송 타이머(60초)보다 반드시 짧아야 합니다.
                </span>
              </div>

              <div class="p05-form__row">
                <span class="p05-form__label">번호별 상한</span>
                <input v-model.number="policy.phoneHourLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/시간</span>
                <input v-model.number="policy.phoneDayLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/일</span>
              </div>

              <div class="p05-form__row">
                <span class="p05-form__label">IP별 상한</span>
                <input v-model.number="policy.ipHourLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/시간</span>
                <input v-model.number="policy.ipDayLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/일</span>
                <span class="p05-form__desc">IP 를 신뢰 수준으로 확정하지 못하면 이 축은 적용되지 않습니다.</span>
              </div>

              <!-- SMS2-B2 2단계 전환용 토글 — 운영 XFF 구조를 계측해 홉 수를 확정한 뒤에만 '차단'으로 바꾼다. -->
              <div class="p05-form__row">
                <span class="p05-form__label">IP축 동작</span>
                <div class="p05-select">
                  <BaseSelect v-model="policy.ipAxisEnabledYn">
                    <option value="N">관측만(차단 안 함)</option>
                    <option value="Y">차단</option>
                  </BaseSelect>
                </div>
                <span class="p05-form__desc">
                  서버 로그의 [SMS상한:IP축진단] 으로 X-Forwarded-For 홉 수를 확정한 뒤 '차단'으로 바꾸세요.
                </span>
              </div>

              <div class="p05-form__row">
                <span class="p05-form__label">사용자별 상한</span>
                <input v-model.number="policy.userHourLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/시간</span>
                <input v-model.number="policy.userDayLimit" type="number" min="0" step="1" class="p05-input" />
                <span class="p05-form__unit">건/일</span>
                <span class="p05-form__desc">로그인 흐름(앱 휴대폰 변경 · 플랫폼 위치열람)에만 적용됩니다.</span>
              </div>

              <!-- ★전역 상한만 하한이 1 이다(다른 축과 달리 0=무제한을 허용하지 않는다).
                   0 이면 킬스위치가 영구 무력화되어 최후 방어선이 사라진다. -->
              <div class="p05-form__row">
                <span class="p05-form__label">전역 상한</span>
                <input v-model.number="policy.globalHourLimit" type="number" min="1" step="1" class="p05-input" />
                <span class="p05-form__unit">건/시간</span>
                <span class="p05-form__desc">
                  1 이상만 입력할 수 있습니다(0=무제한은 킬스위치를 무력화). 초과하면 킬스위치가 자동 발동하며, 해제는 이 화면에서만 가능합니다.
                </span>
              </div>

              <span v-show="policyMsg" class="p05-form__msg">{{ policyMsg }}</span>

              <p class="p05-guide">
                0 을 입력하면 해당 축은 무제한입니다. 저장 즉시 다음 발송부터 적용되며,
                변경 이력(변경 전·후 값)은 감사 로그에 기록됩니다.
              </p>
            </div>
          </div>
        </template>
      </div>

      <!-- ==================== 탭2: 발송 이력 ==================== -->
      <div v-show="activeTab === 'history'" class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">발송 이력</span>
        </div>

        <p class="p05-guide">
          ⓘ 인증번호는 미발송(발송 게이트 OFF) 건에 한해 표시됩니다. 실제
          발송된 건의 인증번호는 표시하지 않습니다. 휴대폰 번호는 마스킹 처리되며, 발송
          요청자는 로그인 상태에서 발송한 경우에만 표시됩니다(셀프가입 등
          비로그인 흐름은 -). 실패 횟수는 인증번호 검증 실패 누적으로, 5회
          이상이면 코드가 무효화되며 무차별 대입 시도를 의심할 수 있습니다.
          <span class="p05-guide__note">
            ※ 발송 상태 추적은 2026년 8월 문자 발송사 연동부터 기록됩니다. 그
            이전 요청은 상태가 갱신된 적이 없어 실제 발송 여부와 무관하게
            '대기(발송전)'로 남아 있으며, 발송 실패를 뜻하지 않습니다.
          </span>
        </p>

        <div
          class="table-box"
          style="--box-h: 60vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 3%">
                  No
                </th>
                <th style="width: 11%">요청일시</th>
                <th style="width: 10%">휴대폰</th>
                <th style="width: 10%">목적</th>
                <th style="width: 7%; text-align: center">상태</th>
                <th style="width: 11%">결과일시</th>
                <th style="width: 6%; text-align: center">인증번호</th>
                <th style="width: 5%; text-align: center">인증</th>
                <th style="width: 5%; text-align: center">실패</th>
                <th style="width: 10%">오류코드</th>
                <th style="width: 11%">오류메시지</th>
                <th style="width: 11%">요청자</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="histLoading">
                <tr>
                  <td colspan="12" class="edu-grid-empty">조회 중입니다...</td>
                </tr>
              </template>
              <template v-else-if="!histList || histList.length === 0">
                <tr>
                  <td colspan="12" class="edu-grid-empty">
                    조회된 발송 이력이 없습니다. 기간을 넓혀 다시 조회해 주세요.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in histList" :key="row.smsId">
                  <td style="text-align: center">
                    {{ (histPage - 1) * histPageSize + idx + 1 }}
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td>{{ row.mblNo || "-" }}</td>
                  <td>{{ fnPurposeLabel(row.purposeCd) }}</td>
                  <td style="text-align: center">
                    <span
                      class="p05-badge"
                      :class="fnStatusClass(row.sendStatus)"
                    >
                      {{ fnStatusLabel(row.sendStatus) }}
                    </span>
                  </td>
                  <td>{{ row.sendDate || "-" }}</td>
                  <!-- 인증번호: 미발송(SKIPPED) 행만 서버가 내려준다(그 외 null → -) -->
                  <td style="text-align: center">{{ row.authCd || "-" }}</td>
                  <td style="text-align: center">
                    {{ fnVerifiedLabel(row.verifiedYn) }}
                  </td>
                  <td style="text-align: center">
                    <span v-if="row.failCnt >= 5" class="p05-fail-cnt">{{
                      row.failCnt
                    }}</span>
                    <span v-else-if="row.failCnt > 0">{{ row.failCnt }}</span>
                    <span v-else>-</span>
                  </td>
                  <td>{{ row.sendErrCd || "-" }}</td>
                  <td class="p05-errmsg-cell" :title="row.sendErrMsg || ''">
                    {{ row.sendErrMsg || "-" }}
                  </td>
                  <td>{{ row.sendUserCd || "-" }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- 페이징 (User_09 / Tbm_04 패턴) -->
        <div v-if="histTotalCount > 0" class="p05-pager">
          <button
            class="btn btn-second p05-pager__btn"
            :disabled="histPage <= 1"
            @click="fnGoHistPage(histPage - 1)"
          >
            이전
          </button>
          <span class="p05-pager__info">
            {{ histPage }} / {{ histTotalPages }} (총 {{ histTotalCount }}건)
          </span>
          <button
            class="btn btn-second p05-pager__btn"
            :disabled="histPage >= histTotalPages"
            @click="fnGoHistPage(histPage + 1)"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  getCurrentInstance,
  defineOptions,
  defineProps,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// keep-alive 매칭용 컴포넌트 이름 = 라우트 이름(MENU_D_ID)
defineOptions({ name: "Platform_05" });

// MainLayout 이 주입하는 공통 props(탭 제목/버튼 권한)
const props = defineProps({
  title: { type: String, default: "SMS 발송 관리" },
  buttons: { type: Object, default: () => ({}) },
});

const { proxy } = getCurrentInstance();

/* 화면 상태 */
const loading = ref(false);
const saving = ref(false);
const releasing = ref(false);

/* ── 탭 상태 — ★기본 탭은 기존 화면(발송 현황)이다 ── */
const activeTab = ref("console");

/* 현황 — GET /platformApi/sms/console 응답으로 채운다 */
const stat1h = ref({ sent: 0, failed: 0, skipped: 0, pending: 0, total: 0 });
const stat24h = ref({ sent: 0, failed: 0, skipped: 0, pending: 0, total: 0 });

/* 상태 */
const gateEnabled = ref(false);
/*
 * 실제 발송 가능 여부(게이트 ON + 계정/인증키/발신번호 주입 + base URL https).
 * ★gateEnabled 와 분리해서 받는다 — 토글은 켜져 있는데 실제로는 안 나가는 상태를 화면이 숨기면 안 된다.
 */
const sendable = ref(false);
/*
 * 런타임 강제 OFF 여부/사유(4차 / sec T-7).
 * ★gateEnabled(설정값)와 분리해서 받는다 — "운영자가 OFF 로 둔 것" 과 "시스템이 사고를 감지해 내린 것" 은
 *   대응이 완전히 다르다. 화면이 둘을 뭉뚱그리면 원인을 잘못 짚는다(PUSH_WORKER_ENABLED 오판 계열).
 */
const gateForcedOff = ref(false);
const gateForcedOffReason = ref("");
const killSwitchOn = ref(false);
const killSwitchAtLabel = ref("-");
const killSwitchReason = ref("-");
const killSwitchReleaseLabel = ref("-");

/* 전역 상한 소진율 */
const globalUsedCnt = ref(0);
const globalHourLimit = ref(0);

/*
 * 임계값 폼 — 서버가 유일 출처다. 초기값은 비워 두고 조회 응답으로만 채운다.
 * policyLoaded=false 면 정책행 부재(마이그레이션 시드 누락)이므로 저장 버튼을 막는다.
 */
const policyLoaded = ref(false);
const policy = ref({
  phoneWindowSec: null,
  phoneHourLimit: null,
  phoneDayLimit: null,
  ipAxisEnabledYn: "N",
  ipHourLimit: null,
  ipDayLimit: null,
  userHourLimit: null,
  userDayLimit: null,
  globalHourLimit: null,
});
const policyMsg = ref("");

/* ── 발송 이력 탭 상태 ──
 * 서버 페이징이라 페이저는 응답 totalCount + 로컬 page/pageSize 로만 계산한다
 * (서버는 page/pageSize 를 되돌려주지 않는다). 클라이언트 정렬은 두지 않는다 —
 * 현재 페이지 안에서만 정렬돼 "최신순" 이라는 계약을 오해하게 만든다(정렬은 서버 고정).
 */
const histLoading = ref(false);
const histList = ref([]);
const histStartDate = ref(""); // YYYY-MM-DD (비우면 서버가 최근 7일로 채운다)
const histEndDate = ref("");
const histPurposeCd = ref("");
const histSendStatus = ref("");
const histMblNo = ref(""); // 평문 입력. 하이픈 허용 — 서버가 정규화 후 HMAC 정확 일치로 조회한다.
const histPage = ref(1);
const histPageSize = ref(20); // 서버 기본 20 / 상한 100
const histTotalCount = ref(0);
const histLoadedOnce = ref(false); // 이력 탭 최초 진입 시 1회 자동 조회 여부

/* 목적/상태 코드 → 한글 라벨. 서버는 코드값만 내려준다(기존 Platform_05 방식과 동일). */
const PURPOSE_LABEL = {
  SELF_JOIN: "셀프가입",
  PLATFORM_LOCATION: "위치정보 열람",
  MOBILE_CHANGE: "휴대폰 변경",
};
const STATUS_LABEL = {
  PENDING: "대기",
  SENT: "성공",
  FAILED: "실패",
  SKIPPED: "미발송",
};
const STATUS_CLASS = {
  PENDING: "is-wait",
  SENT: "is-on",
  FAILED: "is-fired",
  SKIPPED: "is-off",
};

/* 0건이면 1페이지로 본다(페이저가 "1 / 0" 을 그리지 않게) */
const histTotalPages = computed(() => {
  const pages = Math.ceil(histTotalCount.value / histPageSize.value);
  return pages < 1 ? 1 : pages;
});

/* 조회 외 헤더 버튼 숨김 — 저장은 임계값 섹션 내부 버튼이 담당(Platform_03 fnButtonControll 전례).
   ★엑셀은 계속 'N' 이다 — 다운로드를 켜는 순간 공통 정책서 §11.3 감사 대상이 된다. */
const localButtons = ref({ ...props.buttons });
function fnButtonControll() {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

/* ── 표시 라벨(computed) ── */
/*
 * 게이트(설정) — 4차부터 "설정은 ON 인데 시스템이 강제로 내린" 상태를 라벨에서 구분한다.
 * ★강제 OFF 는 정책행 부재/조회 실패를 만났을 때만 걸리며, 되돌리는 방법은 원인 해소 후 재기동뿐이다.
 */
const gateLabel = computed(() => {
  if (gateForcedOff.value) return gateEnabled.value ? "강제 OFF" : "OFF(강제)";
  return gateEnabled.value ? "ON" : "OFF";
});
const gateBadgeClass = computed(() => {
  if (gateForcedOff.value) return "is-fired";
  return gateEnabled.value ? "is-on" : "is-off";
});

/* 발송 가능 — "설정상 ON 인데 실제로는 안 나가는" 상태를 별도 문구로 드러낸다 */
const sendableLabel = computed(() => {
  if (sendable.value) return "가능";
  if (gateForcedOff.value) return "불가(강제 OFF)";
  return gateEnabled.value ? "불가(설정 미완)" : "불가(게이트 OFF)";
});
const sendableBadgeClass = computed(() => {
  if (sendable.value) return "is-on";
  // 게이트는 켰는데 발송이 안 되는 상태는 운영자가 오판하기 쉬우므로 경고색으로 구분한다.
  if (gateForcedOff.value) return "is-fired";
  return gateEnabled.value ? "is-fired" : "is-off";
});
const sendableDesc = computed(() => {
  if (sendable.value) return "인증번호가 실제로 발송됩니다.";
  if (gateForcedOff.value) {
    const reason = gateForcedOffReason.value || "원인 미상";
    return `시스템이 발송을 강제로 중단했습니다(${reason}). 원인을 해소한 뒤 서버를 재기동해야 복구됩니다. 전 흐름이 SKIPPED 로 기록됩니다.`;
  }
  if (gateEnabled.value) {
    return "게이트는 ON 이지만 계정·인증키·발신번호 또는 PPURIO_BASE_URL(https) 설정이 미완입니다. 전 흐름이 SKIPPED 로 기록됩니다.";
  }
  return "발송 게이트가 OFF 입니다. 전 흐름이 SKIPPED 로 기록됩니다.";
});
const killSwitchLabel = computed(() => (killSwitchOn.value ? "발동중" : "정상"));
const killSwitchBadgeClass = computed(() =>
  killSwitchOn.value ? "is-fired" : "is-on"
);

/* 소진율(%) — 상한이 0 이하면 무제한이라 게이지를 채우지 않는다 */
const gaugeRate = computed(() => {
  if (!(globalHourLimit.value > 0)) return -1;
  return (globalUsedCnt.value / globalHourLimit.value) * 100;
});

const gaugeStyle = computed(() => {
  const rate = gaugeRate.value;
  if (rate < 0) return { width: "0%" };
  // 100% 초과 시에도 막대가 컨테이너를 넘지 않게 상한을 둔다(수치는 텍스트로 정확히 표기).
  return { width: `${Math.min(100, Math.max(0, rate))}%` };
});

/* 강조 임계는 Platform_03 사용률과 동일 규칙(>=100 danger / >=90 warning) — 클래스명도 재사용 */
const gaugeClass = computed(() => {
  const rate = gaugeRate.value;
  if (rate < 0) return "";
  if (rate >= 100) return "is-expired";
  if (rate >= 90) return "is-expiring";
  return "";
});

const gaugeText = computed(() => {
  if (!(globalHourLimit.value > 0)) {
    return `${globalUsedCnt.value} 건 (무제한)`;
  }
  const rate = (Math.round(gaugeRate.value * 10) / 10)
    .toFixed(1)
    .replace(/\.0$/, "");
  return `${globalUsedCnt.value} / ${globalHourLimit.value} (${rate}%)`;
});

onMounted(async () => {
  fnButtonControll();
  fnInitHistPeriod();
  await fnSearch();
});

/* ── 탭 전환 ── */
/*
 * 탭은 v-show 로 전환하므로 조회조건/목록 상태가 유지된다.
 * 이력 탭은 최초 진입 시에만 1회 자동 조회한다(재진입마다 재조회하면 페이지 위치가 리셋된다).
 */
async function fnSelectTab(key) {
  if (activeTab.value === key) return;
  activeTab.value = key;

  if (key === "history" && !histLoadedOnce.value) {
    histPage.value = 1;
    await fnSearchHistory();
  }
}

/*
 * ViewHeader [조회] 진입점 — 활성 탭에 따라 분기한다.
 * ★fnSearch(발송 현황 조회)는 이름/동작을 그대로 둔다 — 임계값 저장·킬스위치 해제 후 재조회가
 *   이 함수를 직접 부르고 있어, 여기서 분기시키면 저장 직후 엉뚱한 탭을 조회하게 된다.
 */
async function fnHeaderSearch() {
  if (activeTab.value === "history") {
    // 조회조건이 바뀐 새 조회이므로 첫 페이지부터 본다(페이지 이동은 fnGoHistPage 가 담당).
    histPage.value = 1;
    await fnSearchHistory();
    return;
  }
  await fnSearch();
}

/* ── 조회 ── */
async function fnSearch() {
  if (loading.value) return;
  loading.value = true;
  policyMsg.value = "";

  try {
    const response = await axios.get("/platformApi/sms/console");

    if (response.status === 200) {
      const data = response.data || {};

      gateEnabled.value = data.gateEnabled === true;
      sendable.value = data.sendable === true;
      gateForcedOff.value = data.gateForcedOff === true;
      gateForcedOffReason.value = data.gateForcedOffReason || "";
      stat1h.value = fnNormalizeStat(data.stat1h);
      stat24h.value = fnNormalizeStat(data.stat24h);
      globalUsedCnt.value = data.globalUsedCnt ?? 0;
      globalHourLimit.value = data.globalHourLimit ?? 0;

      const p = data.policy;
      policyLoaded.value = !!p;

      if (p) {
        policy.value = {
          phoneWindowSec: p.phoneWindowSec,
          phoneHourLimit: p.phoneHourLimit,
          phoneDayLimit: p.phoneDayLimit,
          ipAxisEnabledYn: p.ipAxisEnabledYn === "Y" ? "Y" : "N",
          ipHourLimit: p.ipHourLimit,
          ipDayLimit: p.ipDayLimit,
          userHourLimit: p.userHourLimit,
          userDayLimit: p.userDayLimit,
          globalHourLimit: p.globalHourLimit,
        };

        killSwitchOn.value = p.killSwitchYn === "Y";
        // [2026-08-30] 서버가 epoch 초로 내린다(운영 DB 시계=UTC 라 문자열 그대로는 KST-9h 표시) → KST 포맷
        killSwitchAtLabel.value = fnFormatEpochKst(p.killSwitchAtEpoch) || "-";
        killSwitchReason.value = p.killSwitchReason || "-";
        // 해제 시각 + 해제자를 한 줄로(둘 중 하나라도 없으면 "-")
        killSwitchReleaseLabel.value = p.killSwitchReleaseAtEpoch
          ? `${fnFormatEpochKst(p.killSwitchReleaseAtEpoch)}${
              p.killSwitchReleaseNo ? ` (${p.killSwitchReleaseNo})` : ""
            }`
          : "-";
      }
    }
  } catch (err) {
    // 화면은 직전 값을 유지한다(부분 갱신으로 인한 혼동 방지).
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    loading.value = false;
  }
}

/* epoch 초 → "YYYY-MM-DD HH:mm:ss" (Asia/Seoul 명시 — 열람자 기기 타임존과 무관하게 KST 고정) */
function fnFormatEpochKst(epochSec) {
  if (epochSec == null) return "";
  // sv-SE 로케일은 "YYYY-MM-DD HH:mm:ss" 형식을 그대로 돌려준다
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date(epochSec * 1000));
}

/* 서버 응답 누락 대비 기본값 폴백 */
function fnNormalizeStat(stat) {
  return {
    sent: stat?.sent ?? 0,
    failed: stat?.failed ?? 0,
    skipped: stat?.skipped ?? 0,
    pending: stat?.pending ?? 0,
    total: stat?.total ?? 0,
  };
}

/* ── 발송 이력 조회 ── */
/*
 * POST /platformApi/sms/send-histories/search (조회지만 POST — 검색 조건에 휴대폰 평문이 들어간다.
 * 쿼리스트링이면 access log·CDN 로그·브라우저 히스토리에 평문이 복제되므로 반드시 바디로 보낸다).
 * 응답은 historyList + totalCount 뿐이며 page/pageSize 는 서버가 되돌려주지 않는다.
 * ★휴대폰은 서버가 이미 마스킹해서 내려준다 — 프론트에서 재가공하지 않는다.
 */
async function fnSearchHistory() {
  if (histLoading.value) return;
  histLoading.value = true;

  try {
    const response = await axios.post(
      "/platformApi/sms/send-histories/search",
      {
        startDate: histStartDate.value,
        endDate: histEndDate.value,
        purposeCd: histPurposeCd.value,
        sendStatus: histSendStatus.value,
        mblNo: histMblNo.value,
        page: histPage.value,
        pageSize: histPageSize.value,
      }
    );

    if (response.status === 200) {
      const data = response.data || {};
      histList.value = data.historyList || [];
      histTotalCount.value = data.totalCount || 0;
      histLoadedOnce.value = true;
    }
  } catch (err) {
    // 실패 시 목록과 건수를 함께 비운다(건수만 남으면 페이저가 유령 페이지를 가리킨다).
    histList.value = [];
    histTotalCount.value = 0;
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    histLoading.value = false;
  }
}

/* 페이지 이동
   ★조회 중이면 page 를 올리지 않고 즉시 빠진다(qa D1).
     fnSearchHistory 에 재진입 가드가 있어서, 로딩 중 [다음]을 연타하면
     histPage 만 3으로 올라가고 실제 요청은 나가지 않는다. 그러면 먼저 보낸
     2페이지 응답이 렌더되는데 페이저는 "3 / 5", No. 컬럼도 41~ 로 어긋난다.
     (User_09 는 fnSearchHistory 에 재진입 가드가 없어 이 조합이 성립하지 않는다
      — "동일 규약"이라는 이유로 이 가드를 빼지 말 것.) */
function fnGoHistPage(target) {
  if (histLoading.value) return;
  if (target < 1 || target > histTotalPages.value) return;
  histPage.value = target;
  fnSearchHistory();
}

/* 이력 탭 기본 기간 — 오늘 포함 최근 7일(서버 기본값과 동일하게 화면에도 보이게 채운다)
   ★기준 시각대는 KST 고정이다(qa D3). 서버 기본값이 Asia/Seoul 이라, 브라우저 로컬 TZ 로
     계산하면 운영자 PC 가 UTC 인 경우 endDate 가 KST 오늘보다 하루 이르게 채워진다.
     프론트는 항상 명시 날짜를 보내므로, 그 상태로 조회하면 당일 발송분이 조용히 누락된다. */
function fnInitHistPeriod() {
  const today = fnKstToday();
  const from = new Date(today.getTime());
  from.setUTCDate(today.getUTCDate() - 6);
  histStartDate.value = fnFormatDate(from);
  histEndDate.value = fnFormatDate(today);
}

/* 현재 시각의 KST 달력일. UTC 기준으로 +9시간 민 뒤 getUTC* 로 읽으면
   브라우저 TZ 와 무관하게 한국 날짜가 나온다. */
function fnKstToday() {
  return new Date(Date.now() + 9 * 60 * 60 * 1000);
}

function fnFormatDate(date) {
  const yyyy = date.getUTCFullYear();
  const mm = String(date.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(date.getUTCDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/* 코드 → 라벨(미매핑 코드는 원문 그대로 — 신규 목적/상태가 추가돼도 화면이 값을 숨기지 않게) */
function fnPurposeLabel(purposeCd) {
  return PURPOSE_LABEL[purposeCd] || purposeCd || "-";
}

function fnStatusLabel(sendStatus) {
  return STATUS_LABEL[sendStatus] || sendStatus || "-";
}

function fnStatusClass(sendStatus) {
  return STATUS_CLASS[sendStatus] || "";
}

/*
 * 인증 여부 — ★'Y'(검증완료) 와 'C'(소비완료) 를 모두 "완료" 로 표기한다.
 * 'C' 는 검증을 통과한 뒤 코드가 소비된 상태라 'N'(미검증)과 의미가 정반대다.
 * 'Y' 만 완료로 보면 실제로 인증에 성공한 건이 미인증으로 뒤집혀 보인다.
 */
function fnVerifiedLabel(verifiedYn) {
  return verifiedYn === "Y" || verifiedYn === "C" ? "완료" : "-";
}

/* ── 임계값 저장 ── */
async function fnSavePolicy() {
  policyMsg.value = "";

  if (!policyLoaded.value) return;

  // 클라 1차 검증 — 서버 PLATFORM_400_016 과 동일 기준(서버가 최종 관문).
  const p = policy.value;

  if (!fnIsIntInRange(p.phoneWindowSec, 1, 59)) {
    policyMsg.value =
      "연속 발송 간격은 1~59 사이의 정수여야 합니다. (프론트 재발송 타이머 60초보다 짧아야 합니다)";
    return;
  }

  const axisFields = [
    ["번호별 시간당 상한", p.phoneHourLimit],
    ["번호별 일별 상한", p.phoneDayLimit],
    ["IP별 시간당 상한", p.ipHourLimit],
    ["IP별 일별 상한", p.ipDayLimit],
    ["사용자별 시간당 상한", p.userHourLimit],
    ["사용자별 일별 상한", p.userDayLimit],
  ];
  for (const [label, value] of axisFields) {
    if (!fnIsIntInRange(value, 0, 10000)) {
      policyMsg.value = `${label}은 0~10,000 사이의 정수여야 합니다. (0 = 무제한)`;
      return;
    }
  }

  // ★전역 상한은 하한 1(서버 PLATFORM_400_016 과 동일 기준).
  //   0(무제한)을 허용하면 킬스위치가 영구 무력화된다 — 다른 축과 규칙이 다른 유일한 항목이다.
  if (!fnIsIntInRange(p.globalHourLimit, 1, 1000000)) {
    policyMsg.value =
      "전역 시간당 상한은 1~1,000,000 사이의 정수여야 합니다. (0 = 무제한은 킬스위치를 무력화하므로 허용하지 않습니다)";
    return;
  }

  // 교차 검증: 시간당 > 일별이면 일별 상한이 의미를 잃는다. 0(무제한)은 비교에서 제외.
  const crossPairs = [
    ["번호별", p.phoneHourLimit, p.phoneDayLimit],
    ["IP별", p.ipHourLimit, p.ipDayLimit],
    ["사용자별", p.userHourLimit, p.userDayLimit],
  ];
  for (const [label, hour, day] of crossPairs) {
    if (hour > 0 && day > 0 && hour > day) {
      policyMsg.value = `${label} 시간당 상한은 일별 상한보다 클 수 없습니다.`;
      return;
    }
  }

  // IP축을 '차단'으로 켜는 것은 파급이 크다(홉 수를 잘못 잡으면 전 사용자 오차단) → 확인 절차.
  if (p.ipAxisEnabledYn === "Y") {
    const ok = await proxy.$confirm(
      "IP축을 차단으로 전환합니다.\n서버 로그에서 X-Forwarded-For 홉 수를 확인하셨습니까?\n" +
        "홉 수가 틀리면 정상 사용자가 대량으로 차단될 수 있습니다."
    );
    if (!ok) return;
  }

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/platformApi/sms/send-policy", {
      phoneWindowSec: p.phoneWindowSec,
      phoneHourLimit: p.phoneHourLimit,
      phoneDayLimit: p.phoneDayLimit,
      ipAxisEnabledYn: p.ipAxisEnabledYn,
      ipHourLimit: p.ipHourLimit,
      ipDayLimit: p.ipDayLimit,
      userHourLimit: p.userHourLimit,
      userDayLimit: p.userDayLimit,
      globalHourLimit: p.globalHourLimit,
    });

    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
}

/* 정수 + 범위 검증(빈 입력은 null 이라 Number.isInteger 에서 걸린다) */
function fnIsIntInRange(value, min, max) {
  return Number.isInteger(value) && value >= min && value <= max;
}

/* ── 킬스위치 수동 해제 ── */
async function fnReleaseKillSwitch() {
  // 원인을 확인하지 않은 재개는 과금 재폭발이므로 반드시 확인을 받는다.
  const ok = await proxy.$confirm(
    "발송이 즉시 재개됩니다.\n원인을 확인했습니까?"
  );
  if (!ok) return;

  if (releasing.value) return;
  releasing.value = true;

  try {
    const response = await axios.post("/platformApi/sms/kill-switch-release");

    if (response.status === 200) {
      await proxy.$alert("킬스위치를 해제했습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "해제 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    releasing.value = false;
  }
}
</script>

<style scoped>
/* 탭바 — Attd_01 .attd01-tab-bar / User_09 .u09-tabs 스펙 준수(밑줄형 0.875rem) */
.p05-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}
.p05-tab {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  cursor: pointer;
}
.p05-tab:hover {
  color: var(--color-text);
}
.p05-tab.active {
  font-weight: 600;
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 이력 탭 기간 구분자 (User_09 .u09-date-sep 동일) */
.p05-date-sep {
  margin: 0 0.25rem;
}

/* 휴대폰 검색 입력 — 정확 일치 안내가 잘리지 않을 폭 */
.p05-mbl-input {
  width: 12rem;
}

/* 킬스위치 발동 배너 — Platform_03 .p03-truncated-banner 전례 */
.p05-killswitch-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0 0 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.p05-killswitch-banner__text {
  flex: 1 1 auto;
}
.p05-killswitch-banner__btn {
  flex: 0 0 auto;
  padding: 0.15rem 0.6rem;
  font-size: var(--btn-font-sm);
}

/* ── 발송 현황 KPI ── */
.p05-kpi-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.75rem;
}
.p05-kpi {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  flex: 1 1 12rem;
  padding: 0.6rem 0.9rem;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--input-radius);
}
.p05-kpi__label {
  flex: 1 1 auto;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.p05-kpi__value {
  font-size: var(--btn-font-lg);
  font-weight: 700;
  color: var(--color-text-strong);
}
.p05-kpi__value--danger {
  color: var(--color-danger);
}
.p05-kpi__sep,
.p05-kpi__unit {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

/* ── 전역 상한 소진율 게이지 ── */
.p05-gauge-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0 0.75rem 0.9rem;
}
.p05-gauge-label {
  flex: 0 0 8rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.p05-gauge {
  flex: 1 1 auto;
  height: 10px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  overflow: hidden;
}
.p05-gauge__fill {
  height: 100%;
  width: 0;
  background: var(--color-primary);
}
.p05-gauge__fill.is-expiring {
  background: var(--color-warning-text);
}
.p05-gauge__fill.is-expired {
  background: var(--color-danger);
}
.p05-gauge-text {
  flex: 0 0 auto;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text);
}

/* ── 발송 상태 ── */
.p05-status {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.75rem;
}
.p05-status__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}
.p05-status__label {
  flex: 0 0 8rem;
  color: var(--color-text-muted);
}
.p05-status__value {
  color: var(--color-text);
  font-weight: 600;
}
.p05-status__desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.p05-status__btn {
  padding: 0.15rem 0.6rem;
  font-size: var(--btn-font-sm);
}

/* 상태 배지 */
.p05-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
}
.p05-badge.is-on {
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.p05-badge.is-off {
  color: var(--color-text-muted);
}
.p05-badge.is-fired {
  color: var(--color-danger);
  border-color: var(--color-danger);
}
/* 대기(PENDING) — 실패가 아니라 "아직 결과가 확정되지 않음" 이므로 경고색 계열로 구분한다 */
.p05-badge.is-wait {
  color: var(--color-warning-text);
  border-color: var(--color-warning-text);
}

/* 검증 실패 누적 — 5회 이상이면 코드가 무효화된다(무차별 대입 단서라 눈에 띄게) */
.p05-fail-cnt {
  font-weight: 700;
  color: var(--color-danger);
}

/* 오류메시지 셀 — 벤더 원문. 1줄 말줄임 + title 툴팁(v-html 사용 금지) */
.p05-errmsg-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 페이징 (User_09 .pager / Tbm_04 동일 규격) */
.p05-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}
.p05-pager__info {
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}
.p05-pager__btn {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}

/* ── 임계값 폼 ── */
.p05-save-btn {
  margin-left: auto;
  padding: 0.15rem 0.6rem;
  font-size: var(--btn-font-sm);
}
.p05-form {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.75rem;
}
.p05-form__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text);
}
.p05-form__label {
  flex: 0 0 12rem;
  color: var(--color-text-muted);
}
.p05-form__unit {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.p05-form__desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.p05-form__msg {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-danger);
}

/* 숫자 입력 — AiTokenQuotaPop .quota-limit-input 전례 */
.p05-input {
  width: 6rem;
  padding: 0.35rem 0.6rem;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  font: inherit;
  color: var(--color-text);
  text-align: right;
}
.p05-input:focus {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}

/* BaseSelect 는 내부 select 가 width:100% 라 폭을 감싸는 래퍼로 제한한다 */
.p05-select {
  flex: 0 0 12rem;
}

/* 안내문 — AiTokenQuotaPop .quota-guide 전례 */
.p05-guide {
  margin: 0.25rem 0 0;
  padding: 0.5rem 0.75rem;
  font-size: var(--btn-font-sm);
  line-height: 1.5;
  color: var(--color-text-muted);
  background: var(--color-warning-bg);
  border-radius: var(--btn-radius);
}

/* 안내문 보조 줄 — 과거 데이터의 상태 미기록을 별도 줄로 분리(실패로 오해하지 않게) */
.p05-guide__note {
  display: block;
  margin-top: 0.25rem;
  font-weight: 600;
  color: var(--color-warning-text);
}
</style>
