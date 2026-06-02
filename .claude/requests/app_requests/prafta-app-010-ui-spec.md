# prafta-app-010 UI 화면 명세 (Notion "도메인 지식 베이스" 등록 초안)

> 영역: app / 모듈: mypage / 검증 상태: Claude 분석 (사용자 검토 후 YJ 확정)
> 참조 패턴: `MyLeaveSummaryView.vue`(풀스크린 헤더56+본문스크롤+푸터+인라인 SVG sprite+루트 토큰 1회 선언), `MainView.vue`/`HomeHeader.vue`(진입·아바타), `OffsiteReasonSheet.vue`/`BaseBottomSheet`(바텀시트), `PhoneAuthView.vue`(휴대폰 인증 인터랙션).
> 디자인 토큰: 하드코딩 금지. 각 화면 루트 컨테이너에 `--color-*`/`--radius-*`/`--space-*` 세트를 1회 선언(앱 기존 패턴). TypeScript 금지. scoped CSS.
> 진입 권한: 인증 필수(라우터 beforeEach 토큰 게이트). public 목록에 넣지 않는다.
>
> ✅ 사용자 확정(2026-05-30) 반영: **D1**(PII 마스킹 표시 + "개인정보 수정" 진입 시에만 복호화 프리필), **D5**(탈퇴 시 연차 자동취소 미구현), **D6**(성별 SYS004 100:남성/200:여성), **Q12**(알림 아이콘 미노출). 백엔드 호출은 전부 앱 전용 `/appApi/*`(프리셋/휴대폰검증도 앱 전용 신규, web `/user04/*`·comApi verify 호출 금지). 단일 진실은 `prafta-app-010-plan.md` §0.5.

---

## UI-A010 MyPageView (마이페이지 메인)
- 연결 작업: PRAFTA-APP-010-10
- 화면 위치: `src/views/mypage/MyPageView.vue`
- 진입: MainView 우측 상단 아바타(`onAvatarClick` → `router.push('/MyPage')`). 백 버튼 없음(탭/메인 복귀는 하단 또는 헤더 백).

### 레이아웃
```
┌─────────────────────────────────┐
│  마이페이지                       │  ← 헤더(title). 알림 아이콘 미노출(Q12 확정)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ [김여]  김여진               │ │  ← 프로필 카드(아바타 2글자+이름)
│ │         중곡사업장 · 2본부   │ │     siteNm · nodeNm (탭 불가)
│ └─────────────────────────────┘ │
│                                 │
│  계정                            │  ← 그룹 라벨
│ ┌─────────────────────────────┐ │
│ │ 개인정보 수정            ›   │ │
│ │ 비밀번호 변경            ›   │ │
│ └─────────────────────────────┘ │
│  결재                            │
│ ┌─────────────────────────────┐ │
│ │ 연차 결재선 관리   3개   ›   │ │  ← 우측 메타 presetCount
│ └─────────────────────────────┘ │
│                                 │
│ [  로그아웃  ]                   │  ← 풀폭 secondary 버튼
│      회원 탈퇴                   │  ← 텍스트 링크(tertiary, 밑줄)
│ PRAFTA SAFETY NOTE v1.0.0        │  ← caption
└─────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 사용 |
|---|---|
| 프로필 카드 | 인라인(MyPageView 내부). 아바타=userNm 앞 2자 |
| 메뉴 행 | 인라인 버튼 행(앱 카드 톤). router.push |
| 로그아웃 | 인라인 secondary 버튼 → LogoutConfirmDialog(010-23) → `/comApi/login/logout`(D3 재사용) |
| 회원 탈퇴 | 인라인 텍스트 링크 → WithdrawalConfirmDialog(010-24) |

### 상태별 동작
- loading: 본문 "불러오는 중..."(프로필/presetCount 조회 대기, MyLeave 패턴).
- empty: 해당 없음(본인 프로필은 항상 존재).
- error: $alert "정보를 불러오지 못했어요" + 프로필 영역 폴백(세션 gv_userNm/gv_siteNm로 최소 표시).
- success: 카드+메뉴 렌더.

### 사용자 플로우
진입(아바타) → 프로필 조회(010-01, **마스킹 응답**) → 메뉴 탭(개인정보/비번/프리셋 라우팅) | 로그아웃(모달→comApi logout→로그인) | 탈퇴(모달 게이트→010-07→로그인).

### 백엔드 의존
- GET /appApi/mypage/profile (010-01, 마스킹) — userNm/siteNm/nodeNm/presetCount.
- POST /comApi/login/logout (010-06, D3 재사용), POST /appApi/auth/withdraw (010-07).

---

## UI-A011 ProfileEditView (개인정보 수정)
- 연결 작업: PRAFTA-APP-010-11
- 화면 위치: `src/views/mypage/ProfileEditView.vue`
- ⚠️ **D1**: 진입 시 **복호화 전용 엔드포인트 `GET /appApi/mypage/profile/edit`(010-01b)** 로 휴대폰/이메일/생년월일 전체값을 프리필한다(메인 마스킹 응답이 아님). 복호화 응답은 캐시 no-store, 폼 로컬 ref만 유지(store 영속화 금지).

### 레이아웃
```
┌─────────────────────────────────┐
│ ‹  개인정보 수정                 │  ← 헤더 백
├─────────────────────────────────┤
│  소속 정보         수정은 관리자 │  ← 읽기전용(회색 bg)
│ ┌ 아이디  yjkim ───────────────┐ │  ← USER_ID
│ │ 사업장  중곡사업장            │ │  ← siteNm
│ │ 소속부서 2본부                │ │  ← nodeNm
│ └ 입사일  2025-07-17 ──────────┘ │  ← HIRE_DATE
│  기본 정보                       │  ← 수정 가능
│ │ 이름   [김여진          ]     │ │  USER_NM
│ │ 성별   [남성 / 여성 / 선택안함]│ │  GENDER (SYS004: 100/200/null)
│ │ 생년월일 [1993-09-16]         │ │  BIRTH_DT (복호화 프리필, Q7)
│  연락처                          │
│ │ 휴대폰 [010-...][인증요청60s] │ │  MobileVerificationField(010-20)
│ │ 인증번호 [______][확인] ✓     │ │
│ │ 이메일 [test@test.com    ]    │ │  EMAIL (인증 불요, 복호화 프리필)
│                                 │
│  마지막 로그인 · 2026-05-25 20:13│  ← 캡션
├─────────────────────────────────┤
│ [ 취소 ]            [ 저장 ]      │  ← 푸터
└─────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 사용 |
|---|---|
| 소속 정보 | 인라인 읽기전용 행(회색 bg, --color-bg) |
| 이름/이메일 | 인라인 text/email input |
| 성별 | 인라인 select(옵션 SYS004 systCode: 100 남성/200 여성/선택안함=null). JoinUser systCode 조회 재사용 |
| 생년월일 | 인라인 date input |
| 휴대폰+인증 | MobileVerificationField(010-20), **앱 전용** /appApi/mypage/mobile/* |

### 상태별 동작
- loading: 폼 스켈레톤/불러오는 중. **PII 프리필은 010-01b(복호화 전체)** 로 채운다(D1).
- success(저장): 토스트 후 마이페이지 메인 복귀 + 프로필 store 갱신(§8.6).
- error(422): 해당 필드 아래 인라인 helper danger.
- 저장 가능: 변경 1개+ AND (휴대폰 변경 시 인증완료) AND 검증 통과.

### 사용자 플로우
진입 → **복호화 프리필 조회(010-01b)** → 필드 수정 → (휴대폰 변경 시) 인증요청·확인(010-03, 앱전용) → 저장(010-02) → 복귀.

### 백엔드 의존
- GET /appApi/mypage/profile/edit(010-01b, 복호화 프리필), PUT /appApi/mypage/profile(010-02), 휴대폰 인증(010-03 2종, 앱 전용).

---

## UI-A012 PasswordChangeView (비밀번호 변경)
- 연결 작업: PRAFTA-APP-010-12
- 화면 위치: `src/views/mypage/PasswordChangeView.vue`

### 레이아웃
```
┌─────────────────────────────────┐
│ ‹  비밀번호 변경                 │
├─────────────────────────────────┤
│ (i) 안전을 위해 3개월마다 변경 권장│  ← info 노트
│  현재 비밀번호 [••••••] [👁]      │
│  새 비밀번호   [••••••] [👁]      │
│  새 비밀번호 확인 [••••••] [👁]   │
│ ┌ 비밀번호 규칙 ────────────────┐ │  ← PasswordRuleGuide(010-21)
│ │ ✓ 8자 이상  ○ 영문 대/소문자  │ │
│ │ ✓ 숫자      ○ 특수문자         │ │
│ └──────────────────────────────┘ │
├─────────────────────────────────┤
│ [        변경하기        ]        │  ← 전 검증 통과 시 활성
└─────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 사용 |
|---|---|
| 3 입력 | 인라인 password input + 토글(👁, UI 토글만) |
| 규칙 가이드 | PasswordRuleGuide(010-21) — 충족여부 props |

### 상태별 동작
- 실시간: 새 비번 입력 시 4규칙 충족 표시(010-21). 모두 충족+확인 일치+현재≠새 → [변경하기] 활성.
- success: 토스트 "비밀번호가 변경되었습니다" + 메인 복귀(세션 유지, Q10).
- error 422: INVALID_CURRENT_PASSWORD(현재 필드)/PASSWORD_RULE_VIOLATION(가이드)/SAME_AS_CURRENT.

### 사용자 플로우
진입 → 3필드 입력(실시간 검증) → 변경하기(010-04) → 복귀.

### 백엔드 의존
- PUT /appApi/mypage/password(010-04). 잠금/실패 카운트는 기존 로그인 인프라 공유(Q9).

---

## UI-A013 ApprovalPresetListView (연차 결재선 관리)
- 연결 작업: PRAFTA-APP-010-13
- 화면 위치: `src/views/mypage/ApprovalPresetListView.vue`

### 레이아웃
```
┌─────────────────────────────────┐
│ ‹  연차 결재선 관리              │
├─────────────────────────────────┤
│ (i) 연차 신청 시 기본 프리셋이    │  ← info 노트
│     자동 적용됩니다.             │
│ ┌─────────────────────────────┐ │
│ │ [기본] 팀 결재선        ✎    │ │  ← defaultYn='Y' 배지
│ │ 박지훈 → 김영수              │ │  ← steps→summary 합성
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ 본부장 포함             ✎    │ │
│ │ 박지훈 → 김영수 → 이상철      │ │
│ └─────────────────────────────┘ │
│ ┌ + 새 프리셋 추가 ───(dashed)─┐ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```
- 빈 상태: "아직 결재선이 없어요" + 안내 + [+ 새 프리셋 추가] 유지.
- 정렬: DEFAULT_YN='Y' 우선(그 외 앱 매퍼 기본 정렬: PRESET_NM, PRESET_ID).
- ⚠️ 비활성 결재자 "확인 필요" 배지는 앱 프리셋 응답에 활성여부 필드가 없어 **1차 미노출**(questions ③ 보류).

### 컴포넌트 매핑
| 영역 | 사용 |
|---|---|
| 프리셋 카드 | 인라인(카드 전체 탭 → 편집 라우팅). summary는 steps.map(userNm).join(' → ') |
| 추가 | dashed 카드 → 편집(신규 모드) 라우팅 |

### 상태별 동작
- loading/empty/error(목록 조회 010-05). success: 카드 리스트.

### 백엔드 의존
- GET /appApi/mypage/approval-presets(010-05, **앱 전용 신규**).

---

## UI-A014 ApprovalPresetEditView (프리셋 편집/신규)
- 연결 작업: PRAFTA-APP-010-14
- 화면 위치: `src/views/mypage/ApprovalPresetEditView.vue`
- 진입: 카드 탭(수정, presetId 쿼리) / 추가 카드(신규, presetId 없음). 헤더 우측 🗑 삭제는 **수정 모드만**.

### 레이아웃
```
┌─────────────────────────────────┐
│ ‹  프리셋 편집            🗑     │  ← 신규 시 🗑 미노출
├─────────────────────────────────┤
│  프리셋 이름 [팀 결재선        ]  │
│  기본 프리셋으로 사용   [● ON]    │  ← role=switch
│   └ 연차 신청 시 자동 적용됩니다.  │
│  결재자  · 위→아래 순서대로 결재  │
│ ┌① 박지훈  2본부·팀장 [⬆][⬇][✕]┐ │  ← 인라인 결재자 행
│ ┌② 김영수  2본부·본부장 [⬆][⬇][✕]│ │
│ ┌ + 결재자 추가 ───────(dashed)─┐ │  → PresetApproverPickerSheet(010-22)
├─────────────────────────────────┤
│ [ 취소 ]            [ 저장 ]      │
└─────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 사용 |
|---|---|
| 이름 | 인라인 text input |
| 기본 토글 | 인라인 switch(role=switch) |
| 결재자 행 | 인라인(순번 배지+이름+rankNm/nodeNm+⬆⬇✕). 1행 ⬆/마지막 ⬇ 비활성 |
| 결재자 추가 | PresetApproverPickerSheet(010-22), **앱 전용** /appApi/mypage/approval-candidates |
| 삭제 | 헤더 🗑 → $confirm |

### 상태별 동작
- 신규 첫 프리셋: 기본 토글 ON 고정·OFF 불가("첫 프리셋은 자동으로 기본").
- 유일 기본 프리셋 OFF 시도: 토스트 차단.
- 결재자 0명: 빈 메시지+[저장] disabled.
- 저장 가능: 이름 1자+ AND 결재자 1명+ AND 본인 미포함(근태 §9.5) AND 중복 없음. (검증은 앱 service에 신규 구현, D2)
- success: 목록 복귀. 중복 이름 422 → 인라인 에러.

### 사용자 플로우
진입(상세 010-05 또는 빈 폼) → 이름/토글/결재자 편집(추가 시트 010-22) → 저장(010-05 save) / 삭제(010-05 delete).

### 백엔드 의존
- GET /appApi/mypage/approval-presets/{presetId}, POST(저장), POST(set-default), POST(delete), GET /appApi/mypage/approval-candidates (모두 010-05, **앱 전용 신규**).

---

## 컴포넌트 명세 (요약)

### 010-20 MobileVerificationField (`components/MobileVerificationField.vue`)
- 휴대폰 input + [인증요청](60초 카운트다운) + 인증번호 input + [확인] + 성공 표시. v-model로 mblNo·verificationToken 부모(ProfileEdit)에 emit. 발송/검증 호출은 **앱 전용 `/appApi/mypage/mobile/request-verification`·`/appApi/mypage/mobile/verify`(010-03, D4)** 를 developer가 script에 연결(comApi verify-phone-auth 호출 금지). PhoneAuthView의 resendTimer/포맷/검증 패턴 재사용.

### 010-21 PasswordRuleGuide (`components/PasswordRuleGuide.vue`)
- props: `{ rules: [{ key,label,met:Boolean }] }`. 충족=primary 체크, 미충족=tertiary 원. 비즈니스 판정은 부모/props(컴포넌트는 표시만).

### 010-22 PresetApproverPickerSheet (`components/PresetApproverPickerSheet.vue`)
- 바텀시트(BaseBottomSheet/OffsiteReasonSheet 패턴: v-model+transition+dimmer). 검색 input + 후보 리스트(다중 체크) + [N명 추가]. 이미 추가된 사용자 disabled, 본인 제외(BE). 선택 결과 emit. 후보는 **앱 전용 `/appApi/mypage/approval-candidates`(010-05)**.

### 010-23 LogoutConfirmDialog (`components/LogoutConfirmDialog.vue`)
- 타이틀 "로그아웃할까요?"(본문 없음) + [취소]/[로그아웃]. 확정 시 부모가 `/comApi/login/logout`(D3) 호출. 전역 $confirm 대체 가능성은 1차 전용 다이얼로그로 톤 통일.

### 010-24 WithdrawalConfirmDialog (`components/WithdrawalConfirmDialog.vue`)
- Danger tint 아이콘 + "{userNm}님, 탈퇴 전 꼭 확인해주세요" + 본문 + 회색 콜아웃 + 체크박스("유의사항 확인") + [취소]/[탈퇴하기 Danger]. **체크 시에만 [탈퇴하기] 활성**(추가 게이트). 확정 emit → 부모가 010-07 호출.
- ⚠️ **D5 반영**: 콜아웃 문구는 "출퇴근/근태/안전 기록은 법령에 따라 3년간 보존됩니다. 탈퇴 후 동일 계정 재가입은 별도 절차가 필요합니다." 수준으로 유지. **"신청 중 연차가 자동 취소됩니다" 류의 문구는 넣지 않는다**(탈퇴 시 연차를 건드리지 않음). 퇴사 연차 정산 안내도 본 모달에 넣지 않음(별도 페이지).
