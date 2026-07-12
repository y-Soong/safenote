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
        신규 고객사를 등록합니다. 회사코드는 자동 발급되며, 입력한 관리자 정보로 master 계정 1개가 생성됩니다.
      </p>

      <!-- 입력 폼 -->
      <table class="form-table">
        <colgroup>
          <col style="width: 160px" />
          <col />
        </colgroup>
        <tbody>
          <tr>
            <th>회사명 <span class="req">*</span></th>
            <td>
              <input v-model.trim="form.cmpnyNm" type="text" maxlength="50" placeholder="회사명" :disabled="saving" />
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
          <tr>
            <th>관리자명 <span class="req">*</span></th>
            <td>
              <input v-model.trim="form.adminNm" type="text" maxlength="50" placeholder="관리자 이름" :disabled="saving" />
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
              <p class="hint">최초 master 계정의 로그인 ID로 사용됩니다. 회사 내에서 유일하게 입력하세요.</p>
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
              <p class="hint">초기 비밀번호 = 이 휴대폰번호(숫자). 첫 로그인 시 SMS 본인인증 후 변경 가능합니다.</p>
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
          위 정보를 고객사 관리자에게 안전하게 전달하세요. 회사코드는 추측 불가한 식별자이며, 분실 시 재확인이 어렵습니다.
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
const showAlert = (msg) => (proxy?.$alert ? proxy.$alert(msg) : Promise.resolve(window.alert(msg)));
const showConfirm = (msg) =>
  proxy?.$confirm ? proxy.$confirm(msg) : Promise.resolve(window.confirm(msg));

const form = reactive({
  cmpnyNm: "",
  bsnsLcnNo: "",
  contractEndDate: "",
  adminNm: "",
  adminId: "",
  adminMbl: "",
});

const saving = ref(false);
const result = ref(null);

// 숫자만 남기기(하이픈/공백 제거)
const digits = (v) => String(v || "").replace(/[^0-9]/g, "");

// 클라 1차 검증(서버가 최종 검증). 형식은 서버와 동일 기준.
function validate() {
  if (!form.cmpnyNm || !form.bsnsLcnNo || !form.adminNm || !form.adminId || !form.adminMbl) {
    return "필수 입력값(회사명/사업자번호/관리자명/관리자ID/관리자 휴대폰)을 모두 입력해 주세요.";
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
  return null;
}

async function fnSave() {
  if (saving.value) return;

  const err = validate();
  if (err) {
    await showAlert(err);
    return;
  }

  const ok = await showConfirm(`'${form.cmpnyNm}' 고객사를 등록할까요? 회사코드와 master 계정이 생성됩니다.`);
  if (!ok) return;

  saving.value = true;
  result.value = null;
  try {
    const payload = {
      cmpnyNm: form.cmpnyNm,
      bsnsLcnNo: digits(form.bsnsLcnNo),
      contractEndDate: form.contractEndDate ? digits(form.contractEndDate) : "",
      adminNm: form.adminNm,
      adminId: form.adminId,
      adminMbl: digits(form.adminMbl),
    };
    const { data } = await api.post("/platformApi/company", payload);
    result.value = {
      cmpnyCd: data?.cmpnyCd || "",
      masterUserId: data?.masterUserId || "",
      initialPasswordGuide: data?.initialPasswordGuide || "초기 비밀번호 = 관리자 휴대폰번호",
    };
    // 입력 폼 초기화(중복 등록 방지)
    form.cmpnyNm = "";
    form.bsnsLcnNo = "";
    form.contractEndDate = "";
    form.adminNm = "";
    form.adminId = "";
    form.adminMbl = "";
    await showAlert("고객사 등록이 완료되었습니다.");
  } catch (e) {
    await showAlert(resolveApiErrorMessage(e, "고객사 등록 중 오류가 발생했습니다."));
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
.hint {
  margin: 0.35rem 0 0;
  font-size: 0.75rem;
  color: #9ca3af;
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
</style>
