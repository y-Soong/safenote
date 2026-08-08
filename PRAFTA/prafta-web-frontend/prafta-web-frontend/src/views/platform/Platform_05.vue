<!--
  Platform_05.vue — SMS 발송 관리 (플랫폼 운영자 전용 콘솔)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_05', MENU_VIEW='platform/Platform_05.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 동작: GET /platformApi/sms/console 로 현황·상태·임계값을 한 번에 조회 →
          POST /platformApi/sms/send-policy(임계값 저장) / POST /platformApi/sms/kill-switch-release(킬스위치 해제).
  - ★발송 게이트(prafta.sms.enabled)는 서버 secrets(PPURIO_ENABLED) 소관이라 화면에서 바꾸지 않는다(읽기전용 표시).
    킬스위치만 DB 상태이며 화면에서 수동 해제한다.
  - ★개별 발송 이력(휴대폰/인증번호)은 표시하지 않는다 — 집계만(PII 최소 처리).
  - 골격: planner 작성(template + scoped style), script 로직: developer 작성(SMS2-C2).
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody">
      <!-- 킬스위치 발동 배너 — 차단/제한 상황은 배너로 명시(공통 정책서 §13.3) -->
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

/* 조회 외 헤더 버튼 숨김 — 저장은 임계값 섹션 내부 버튼이 담당(Platform_03 fnButtonControll 전례) */
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
  await fnSearch();
});

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
        killSwitchAtLabel.value = p.killSwitchAt || "-";
        killSwitchReason.value = p.killSwitchReason || "-";
        // 해제 시각 + 해제자를 한 줄로(둘 중 하나라도 없으면 "-")
        killSwitchReleaseLabel.value = p.killSwitchReleaseAt
          ? `${p.killSwitchReleaseAt}${
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
</style>
