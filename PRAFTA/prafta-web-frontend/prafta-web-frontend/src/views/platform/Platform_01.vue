<!--
  Platform_01.vue — 신규 고객사 등록 (플랫폼 운영자 전용 콘솔)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_01', MENU_VIEW='platform/Platform_01.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 동작: 회사명/사업자번호/계약종료일/관리자명/관리자ID/관리자휴대폰 입력 → POST /platformApi/company
          → 응답(회사코드/최초 master ID/초기비번 안내) 표시.
  - 초기 비밀번호: 관리자 휴대폰번호로 BCrypt 해시되어 저장됨 (평문 미응답).
-->
<template>
  <div class="viewComm">
    <ViewHeader :title="props.title" :buttons="props.buttons" @save="fnSave" />

    <div class="viewBody platform-create">
      <p class="desc">
        신규 고객사를 등록합니다. 회사코드를 직접 지정하며, 입력한 관리자 정보로
        master 계정 1개가 생성됩니다.
      </p>

      <!-- 입력 폼 -->
      <table class="form-table">
        <colgroup>
          <col style="width: 160px" />
          <col />
        </colgroup>
        <tbody>
          <!-- 회사코드 — 등록 후 변경 불가(22개 테이블 복합 PK 선두 컬럼)라
               입력 즉시 확인 + 저장 시 서버 재검사(2중)로 막는다. -->
          <tr>
            <th>회사코드 <span class="req">*</span></th>
            <td>
              <div class="cmpny-cd-row">
                <input
                  v-model.trim="form.cmpnyCd"
                  type="text"
                  maxlength="20"
                  placeholder="영문·숫자 2~20자 (예: PARMA)"
                  :disabled="saving"
                  @input="onCmpnyCdInput"
                />
                <button
                  type="button"
                  class="btn btn-sm btn-custom"
                  :disabled="saving || checkingCd || !form.cmpnyCd"
                  @click="fnCheckCmpnyCd"
                >
                  {{ checkingCd ? "확인 중…" : "중복확인" }}
                </button>
              </div>
              <p
                v-if="cmpnyCdCheck.message"
                class="cmpny-cd-msg"
                :class="cmpnyCdCheck.available ? 'is-ok' : 'is-err'"
              >
                {{ cmpnyCdCheck.message }}
              </p>
              <p class="cmpny-cd-hint">
                입력한 값이 그대로 회사코드가 됩니다.
                <b>등록 후에는 변경할 수 없습니다.</b> 소문자는 대문자로 자동 변환됩니다.
              </p>
            </td>
          </tr>
          <tr>
            <th>회사명 <span class="req">*</span></th>
            <td>
              <input
                v-model.trim="form.cmpnyNm"
                type="text"
                maxlength="50"
                placeholder="회사명"
                :disabled="saving"
              />
            </td>
          </tr>
          <tr>
            <th>사업자등록번호 <span class="req">*</span></th>
            <td>
              <input
                v-model.trim="form.bsnsLcnNo"
                type="text"
                inputmode="numeric"
                maxlength="12"
                placeholder="숫자 10자리(하이픈 제외)"
                :disabled="saving"
              />
            </td>
          </tr>
          <tr>
            <th>계약 종료일</th>
            <td>
              <input
                v-model.trim="form.contractEndDate"
                type="text"
                inputmode="numeric"
                maxlength="8"
                placeholder="YYYYMMDD (선택, 무기한이면 비움)"
                :disabled="saving"
              />
            </td>
          </tr>
          <!-- 통상근로시간(회사 기본값) — 미입력이면 행을 만들지 않고 주 40시간 폴백을 쓴다. -->
          <tr>
            <th>통상근로시간</th>
            <td>
              <div class="std-work-field">
                <span>주</span>
                <input
                  v-model="form.stdWorkHours"
                  type="text"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="40"
                  :disabled="saving"
                />
                <span>시간</span>
                <input
                  v-model="form.stdWorkMinutes"
                  type="text"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="0"
                  :disabled="saving"
                />
                <span>분</span>
              </div>
              <p class="hint">
                통상근로자(풀타임)의 주 소정근로시간입니다. 단시간근로자 판정과
                연차 비례부여의 기준이 됩니다. 미입력 시 주 40시간으로 적용되며,
                사업장별로 다르면 사업장 관리에서 따로 지정할 수 있습니다.
                (법정 상한 주 40시간 — 초과 근무는 연장근로로 근무타입에서 관리)
              </p>
            </td>
          </tr>
          <!-- 기본 근무시간 — 프로비저닝이 시드하는 기본 근무타입(ST001)의 근무/휴게 시각.
               서버가 형식·구간을 최종 검증하고 휴게 분(FST_SCH_BRK_MIN)은 서버가 산출한다. -->
          <tr>
            <th>기본 근무시간 <span class="req">*</span></th>
            <td>
              <div class="std-work-field">
                <input v-model="form.schStrTime" type="time" :disabled="saving" />
                <span>~</span>
                <input v-model="form.schEndTime" type="time" :disabled="saving" />
                <span>· 휴게</span>
                <input v-model="form.brkStrTime" type="time" :disabled="saving" />
                <span>~</span>
                <input v-model="form.brkEndTime" type="time" :disabled="saving" />
              </div>
              <p class="hint">
                등록 시 생성되는 기본 근무타입의 근무시간입니다. 휴게시간이 없으면
                휴게 시작·종료를 모두 비워 두세요. 야간·2교대 등 다른 형태는 등록
                후 근무타입 관리에서 추가할 수 있습니다.
              </p>
              <p class="hint hint-warn">
                ⚠ 휴게시간을 비워서 소정근로가 8시간을 초과하면(예: 09:00~18:00,
                휴게 없음 → 9시간), 연차 시간차 계산은 근로기준법 §50 법정근로시간
                (1일 8시간) 기준 480분 상한으로 적용됩니다 — 실제 근무시간(9시간)이
                아니라 480분을 분모로 차감되므로, 실제 휴게시간이 있다면 반드시
                입력해 주세요.
              </p>
            </td>
          </tr>
          <tr>
            <th>관리자명 <span class="req">*</span></th>
            <td>
              <input
                v-model.trim="form.adminNm"
                type="text"
                maxlength="50"
                placeholder="관리자 이름"
                :disabled="saving"
              />
            </td>
          </tr>
          <tr>
            <th>관리자 ID <span class="req">*</span></th>
            <td>
              <input
                v-model.trim="form.adminId"
                type="text"
                maxlength="50"
                placeholder="로그인에 사용할 ID (영문, 숫자, 특수문자 조합)"
                :disabled="saving"
              />
              <p class="hint">
                최초 master 계정의 로그인 ID로 사용됩니다. 회사 내에서 유일하게
                입력하세요.
              </p>
            </td>
          </tr>
          <tr>
            <th>관리자 휴대폰번호 <span class="req">*</span></th>
            <td>
              <input
                v-model.trim="form.adminMbl"
                type="text"
                inputmode="numeric"
                maxlength="13"
                placeholder="숫자 10~11자리(하이픈 제외)"
                :disabled="saving"
              />
              <p class="hint">
                초기 비밀번호 = 이 휴대폰번호(숫자). 첫 로그인 시 SMS 본인인증
                후 변경 가능합니다.
              </p>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="form-actions">
        <button class="btn btn-primary" :disabled="saving" @click="fnSave">
          {{ saving ? "등록 중…" : "고객사 등록" }}
        </button>
      </div>

      <!-- 등록 결과 -->
      <div v-if="result" class="result-panel">
        <h3>등록 완료</h3>
        <table class="result-table">
          <tbody>
            <tr>
              <th>회사코드(CMPNY_CD)</th>
              <td class="mono">{{ result.cmpnyCd }}</td>
            </tr>
            <tr>
              <th>master 로그인 ID</th>
              <td class="mono">{{ result.masterUserId }}</td>
            </tr>
            <tr>
              <th>초기 비밀번호</th>
              <td>{{ result.initialPasswordGuide }}</td>
            </tr>
          </tbody>
        </table>
        <p class="result-note">
          위 정보를 고객사 관리자에게 안전하게 전달하세요. 초기 비밀번호는 첫 로그인
          시 변경하도록 안내해 주세요.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import api from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// keep-alive 매칭용 컴포넌트 이름 = 라우트 이름(MENU_D_ID)
defineOptions({ name: "Platform_01" });

// MainLayout 이 주입하는 공통 props(탭 제목/버튼 권한)
const props = defineProps({
  title: { type: String, default: "신규 고객사 등록" },
  buttons: { type: Object, default: () => ({}) },
});

const { proxy } = getCurrentInstance() || { proxy: null };
const showAlert = (msg) =>
  proxy?.$alert ? proxy.$alert(msg) : Promise.resolve(window.alert(msg));
const showConfirm = (msg) =>
  proxy?.$confirm ? proxy.$confirm(msg) : Promise.resolve(window.confirm(msg));

const form = reactive({
  cmpnyCd: "",
  cmpnyNm: "",
  bsnsLcnNo: "",
  contractEndDate: "",
  // 통상근로자 주 소정근로시간(선택). 시간/분 분리 입력 → 서버에는 분으로 환산해 보낸다.
  stdWorkHours: "",
  stdWorkMinutes: "",
  // 기본 근무타입(ST001) 근무/휴게 시각 — 표준 기본값 프리필(수정 가능). 휴게 없으면 둘 다 비움.
  schStrTime: "09:00",
  schEndTime: "18:00",
  brkStrTime: "12:00",
  brkEndTime: "13:00",
  adminNm: "",
  adminId: "",
  adminMbl: "",
});

const saving = ref(false);
const result = ref(null);

// ── 회사코드 중복확인 ────────────────────────────────────────────────────
//   ★1차 확인일 뿐이다. 확인과 저장 사이에 다른 운영자가 선점할 수 있어 서버가 저장
//     트랜잭션에서 다시 검사한다. 여기 통과했다고 저장이 보장되지 않는다.
const checkingCd = ref(false);
const cmpnyCdCheck = reactive({ checked: "", available: false, message: "" });

// 대문자로 즉시 변환해 보여준다 — 서버도 대문자로 정규화하므로 "보이는 값 = 저장될 값"이 된다.
//   (DB 콜레이션이 대소문자를 무시해 parma 와 PARMA 는 어차피 같은 코드다)
const onCmpnyCdInput = () => {
  form.cmpnyCd = String(form.cmpnyCd || "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
  // 값이 바뀌면 이전 확인 결과는 무효다.
  if (cmpnyCdCheck.checked !== form.cmpnyCd) {
    cmpnyCdCheck.checked = "";
    cmpnyCdCheck.available = false;
    cmpnyCdCheck.message = "";
  }
};

const fnCheckCmpnyCd = async () => {
  if (!form.cmpnyCd) return;
  checkingCd.value = true;
  try {
    const { data } = await api.get("/platformApi/company/cmpny-cd-available", {
      params: { cmpnyCd: form.cmpnyCd },
    });
    form.cmpnyCd = data?.normalized || form.cmpnyCd;
    cmpnyCdCheck.checked = data?.available ? form.cmpnyCd : "";
    cmpnyCdCheck.available = Boolean(data?.available);
    cmpnyCdCheck.message = data?.message || "";
  } catch (e) {
    cmpnyCdCheck.checked = "";
    cmpnyCdCheck.available = false;
    cmpnyCdCheck.message = resolveApiErrorMessage(
      e,
      "회사코드 확인 중 오류가 발생했습니다."
    );
  } finally {
    checkingCd.value = false;
  }
};

// 숫자만 남기기(하이픈/공백 제거)
const digits = (v) => String(v || "").replace(/[^0-9]/g, "");

// 법정 상한(근로기준법 제50조 1주 40시간). 서버가 최종 검증하며 여기서는 1차 안내만 한다.
const LEGAL_MAX_WEEK_MINUTES = 2400;

// 통상근로시간 입력 여부(시간·분 중 하나라도 입력하면 지정으로 본다).
const isStdWorkEntered = () =>
  digits(form.stdWorkHours) !== "" || digits(form.stdWorkMinutes) !== "";

// 입력값 → 분. 미입력이면 null(= 기준값 행을 만들지 않음).
const stdWorkWeekMinutes = () => {
  if (!isStdWorkEntered()) return null;
  const h = Number(digits(form.stdWorkHours) || 0);
  const m = Number(digits(form.stdWorkMinutes) || 0);
  return h * 60 + m;
};

// 클라 1차 검증(서버가 최종 검증). 형식은 서버와 동일 기준.
function validate() {
  if (
    !form.cmpnyCd ||
    !form.cmpnyNm ||
    !form.bsnsLcnNo ||
    !form.adminNm ||
    !form.adminId ||
    !form.adminMbl
  ) {
    return "필수 입력값(회사코드/회사명/사업자번호/관리자명/관리자ID/관리자 휴대폰)을 모두 입력해 주세요.";
  }
  if (!/^[A-Z0-9]{2,20}$/.test(form.cmpnyCd)) {
    return "회사코드는 영문·숫자 2~20자로 입력해 주세요.";
  }
  // 등록 후 변경이 불가능하므로 중복확인을 반드시 거치게 한다(서버도 저장 시 재검사).
  if (cmpnyCdCheck.checked !== form.cmpnyCd) {
    return "회사코드 중복확인을 해 주세요.";
  }
  if (digits(form.bsnsLcnNo).length !== 10) {
    return "사업자등록번호는 숫자 10자리여야 합니다.";
  }
  if (form.adminId.length < 3 || form.adminId.length > 50) {
    return "관리자 ID는 3~50자여야 합니다.";
  }
  // 영문/숫자/특수문자 조합 검증 (단순 alphanumeric + _ - . 허용)
  if (!/^[a-zA-Z0-9_\-\.]+$/.test(form.adminId)) {
    return "관리자 ID는 영문, 숫자, 언더스코어(_), 하이픈(-), 점(.)만 사용 가능합니다.";
  }
  const mbl = digits(form.adminMbl);
  if (mbl.length < 10 || mbl.length > 11) {
    return "관리자 휴대폰번호는 숫자 10~11자리여야 합니다.";
  }
  if (form.contractEndDate && digits(form.contractEndDate).length !== 8) {
    return "계약 종료일은 YYYYMMDD 8자리여야 합니다(미입력 시 무기한).";
  }
  // 기본 근무시간: 시작·종료 필수, 종료 > 시작(당일 주간만). 휴게는 쌍 입력 + 근무구간 내부.
  //   서버(PLATFORM_400_019)와 동일 기준의 1차 검증이다.
  const toMin = (t) => {
    if (!t || !/^\d{2}:\d{2}$/.test(t)) return null;
    const [h, m] = t.split(":").map(Number);
    return h * 60 + m;
  };
  const schStr = toMin(form.schStrTime);
  const schEnd = toMin(form.schEndTime);
  if (schStr == null || schEnd == null) {
    return "기본 근무시간(시작·종료)을 입력해 주세요.";
  }
  if (schEnd <= schStr) {
    return "기본 근무시간 종료는 시작 이후여야 합니다.\n야간 등 다른 형태는 등록 후 근무타입 관리에서 추가해 주세요.";
  }
  if (form.brkStrTime || form.brkEndTime) {
    const brkStr = toMin(form.brkStrTime);
    const brkEnd = toMin(form.brkEndTime);
    if (brkStr == null || brkEnd == null) {
      return "휴게시간은 시작·종료를 함께 입력해 주세요(휴게가 없으면 모두 비움).";
    }
    if (brkEnd <= brkStr || brkStr < schStr || brkEnd > schEnd) {
      return "휴게시간은 근무시간 안에 있어야 하고, 종료가 시작보다 늦어야 합니다.";
    }
  }
  // 통상근로시간: 입력했다면 0 초과 ~ 주 40시간(2400분) 이하.
  const stdMinutes = stdWorkWeekMinutes();
  if (stdMinutes !== null) {
    if (stdMinutes <= 0) {
      return "통상근로시간을 0보다 크게 입력해 주세요(비워두면 주 40시간이 적용됩니다).";
    }
    if (stdMinutes > LEGAL_MAX_WEEK_MINUTES) {
      return "통상근로시간은 주 40시간을 초과할 수 없습니다.\n근로기준법 제50조상 1주 소정근로시간의 법정 상한입니다.\n40시간을 넘는 근무는 연장근로이므로 고정연장근무 근무타입으로 등록해 주세요.";
    }
  }
  return null;
}

async function fnSave() {
  if (saving.value) return;

  const err = validate();
  if (err) {
    await showAlert(err);
    return;
  }

  const ok = await showConfirm(
    `'${form.cmpnyNm}' 고객사를 등록할까요?\n\n` +
      `회사코드: ${form.cmpnyCd}\n` +
      `회사코드는 등록 후 변경할 수 없습니다.`
  );
  if (!ok) return;

  saving.value = true;
  result.value = null;
  try {
    const payload = {
      cmpnyCd: form.cmpnyCd,
      cmpnyNm: form.cmpnyNm,
      bsnsLcnNo: digits(form.bsnsLcnNo),
      contractEndDate: form.contractEndDate ? digits(form.contractEndDate) : "",
      // null 이면 서버가 기준값 행을 만들지 않는다(= 주 40시간 폴백 유지).
      weekStdMinutes: stdWorkWeekMinutes(),
      // 기본 근무타입 시각(HHMM). 휴게 미입력이면 빈 문자열(서버가 휴게 없음으로 처리).
      schStrTime: form.schStrTime.replace(":", ""),
      schEndTime: form.schEndTime.replace(":", ""),
      brkStrTime: form.brkStrTime ? form.brkStrTime.replace(":", "") : "",
      brkEndTime: form.brkEndTime ? form.brkEndTime.replace(":", "") : "",
      adminNm: form.adminNm,
      adminId: form.adminId,
      adminMbl: digits(form.adminMbl),
    };
    const { data } = await api.post("/platformApi/company", payload);
    result.value = {
      cmpnyCd: data?.cmpnyCd || "",
      masterUserId: data?.masterUserId || "",
      initialPasswordGuide:
        data?.initialPasswordGuide || "초기 비밀번호 = 관리자 휴대폰번호",
    };
    // 입력 폼 초기화(중복 등록 방지)
    form.cmpnyCd = "";
    cmpnyCdCheck.checked = "";
    cmpnyCdCheck.available = false;
    cmpnyCdCheck.message = "";
    form.cmpnyNm = "";
    form.bsnsLcnNo = "";
    form.contractEndDate = "";
    form.stdWorkHours = "";
    form.stdWorkMinutes = "";
    form.schStrTime = "09:00";
    form.schEndTime = "18:00";
    form.brkStrTime = "12:00";
    form.brkEndTime = "13:00";
    form.adminNm = "";
    form.adminId = "";
    form.adminMbl = "";
    await showAlert("고객사 등록이 완료되었습니다.");
  } catch (e) {
    await showAlert(
      resolveApiErrorMessage(e, "고객사 등록 중 오류가 발생했습니다.")
    );
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.platform-create {
  padding: 1rem 1.25rem;
  overflow-y: auto;
  font-family: "Pretendard", sans-serif;
}
.desc {
  margin: 0 0 1rem;
  font-size: 0.85rem;
  color: #6b7280;
}
.form-table {
  width: 100%;
  max-width: 720px;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid #e5e7eb;
}
.form-table th,
.form-table td {
  border: 1px solid #e5e7eb;
  padding: 0.6rem 0.75rem;
  font-size: 0.85rem;
  text-align: left;
  vertical-align: middle;
}
.form-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
}
.form-table input {
  width: 100%;
  max-width: 360px;
  box-sizing: border-box;
  height: 34px;
  padding: 0 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.85rem;
}
.form-table input:focus {
  outline: none;
  border-color: #16a34a;
}
.req {
  color: #ef4444;
}
/* 통상근로시간 — 시간/분 분리 입력(폼 테이블 입력 폭 규칙과 별개로 좁게) */
.std-work-field {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: #374151;
}
.std-work-field input {
  width: 64px;
  max-width: 64px;
  text-align: right;
}
.hint {
  margin: 0.35rem 0 0;
  font-size: 0.75rem;
  color: #9ca3af;
}
.hint-warn {
  color: var(--color-danger, #dc2626);
}
.form-actions {
  max-width: 720px;
  margin: 1rem 0 0;
  display: flex;
  justify-content: flex-end;
}
.btn {
  height: 36px;
  padding: 0 1.2rem;
  border: 0;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-primary {
  background: #16a34a;
  color: #fff;
}
.btn-primary:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}
.result-panel {
  max-width: 720px;
  margin: 1.5rem 0 0;
  padding: 1rem 1.25rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
}
.result-panel h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: #15803d;
}
.result-table {
  width: 100%;
  border-collapse: collapse;
}
.result-table th,
.result-table td {
  padding: 0.45rem 0.6rem;
  font-size: 0.85rem;
  text-align: left;
  border-bottom: 1px solid #dcfce7;
}
.result-table th {
  width: 180px;
  color: #374151;
  font-weight: 600;
  white-space: nowrap;
}
.mono {
  font-family: "D2Coding", Consolas, monospace;
  word-break: break-all;
}
.result-note {
  margin: 0.75rem 0 0;
  font-size: 0.78rem;
  color: #166534;
}

/* ── 회사코드 입력(직접 지정 전환, 2026-08-16) ───────────────────────────── */
.cmpny-cd-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.cmpny-cd-row input {
  /* 코드값이라 등폭 글꼴이 오독(0/O, 1/I)을 줄인다. */
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}
.cmpny-cd-msg {
  margin: 0.25rem 0 0;
  font-size: 12px;
  line-height: 1.5;
}
.cmpny-cd-msg.is-ok {
  color: var(--color-success-text, #15803d);
}
.cmpny-cd-msg.is-err {
  color: var(--color-danger, #dc2626);
}
.cmpny-cd-hint {
  margin: 0.2rem 0 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
}
.cmpny-cd-hint b {
  font-weight: 600;
  color: var(--color-danger, #dc2626);
}
</style>
