# 공통 정책서 INDEX (v1.1)

PRAFTA와 SAFETY NOTE 두 모듈이 공통으로 참조하는 기반 정책. 권한·조직·사업장·계정·인증·보안 등 공통 주제는 본 정책서에서만 정의한다.

## 빠른 검색 가이드

작업 키워드별로 정독해야 할 파일을 정리한다. 키워드가 두 개 이상 해당하면 모두 정독한다.

### 인증 / 세션 / 토큰
- 회원가입 필수/선택 입력, 약관 동의 → `03-account-auth.md` §3.1
- 로그인 / 아이디 저장 → `03-account-auth.md` §3.2
- 계정 찾기 / 비밀번호 재설정 → `03-account-auth.md` §3.3
- **액세스 토큰 1시간 / 리프레쉬 토큰 48시간** → `03-account-auth.md` §3.4
- 자동 토큰 갱신 → `03-account-auth.md` §3.4
- 계정 상태(활성화/미사용/탈퇴) → `03-account-auth.md` §3.5

### 사용자 트랙
- 정규 사용자 vs 일일계정 사용자 → `04-user-tracks.md`

### 일일계정 / 슬롯
- 슬롯 개념 / 사업장별 사전 할당 → `05-slot-management.md` §5.2
- 슬롯 총량 = 요금제 기반 → `05-slot-management.md` §5.3
- 슬롯 구성 요소(상태값 empty/on, hold) → `05-slot-management.md` §5.4
- 발급 채널(직접가입 self / 관리자 QR qr) → `05-slot-management.md` §5.5
- 슬롯 수명 / 자정 만료 배치 / 점유 유지 → `05-slot-management.md` §5.6
- 슬롯점유 이력 조회 → `05-slot-management.md` §5.6.4

### 사업장 / 조직
- 사업장 구성 요소 → `06-site-management.md` §6.1
- GPS 허용범위 정책 → `06-site-management.md` §6.2
- 조직도(트리) 구조 / 노드 → `07-organization.md` §7.1
- 담당 정 / 담당 부 → `07-organization.md` §7.2
- 자체근태승인여부 → `07-organization.md` §7.3
- 타사업장 조직부서 가져오기 → `07-organization.md` §7.4

### 권한
- 권한 결정 모델 — 3축 AND(권한 × 사업장권한 × 조직스코프) → `08-permissions.md` §8.1
- 화면 권한(Role) 관리 → `08-permissions.md` §8.2
- 사업장 권한 / 사이트 스위처 → `08-permissions.md` §8.3
- 조직 스코프 규칙 → `08-permissions.md` §8.4
- 마스터관리자 예외 → `08-permissions.md` §8.5

### 동시성 / 잠금
- 선점(처리 잠금) 동작 / 적용 범위 / 제외 범위 → `09-locking.md`

### 알림 / 공지
- 알림 채널 / 트리거 원칙 → `10-notifications.md`
- 오발송 방지 → `10-notifications.md` §10.3

### 보안 / 개인정보 / 위치정보
- 기본 원칙 (PII, 암호화) → `11-security-privacy.md` §11.1
- 위치정보 처리 → `11-security-privacy.md` §11.2
- **감사 로그 대상** → `11-security-privacy.md` §11.3

### 이용약관
- 약관 종류 / 관리 방식 → `12-terms.md`

### UI/UX
- 디자인 가이드 참조 / 핵심 원칙 → `13-ui-ux.md`
- 공통 인터랙션 원칙 → `13-ui-ux.md` §13.3

## 파일 목록

| 파일 | 영역 |
| --- | --- |
| `01-overview.md` | 1장 개요(목적, 범위, 정책서 관계, 용어) |
| `02-system-structure.md` | 2장 시스템 구조 |
| `03-account-auth.md` | 3장 계정 및 인증 |
| `04-user-tracks.md` | 4장 사용자 트랙 구분 |
| `05-slot-management.md` | 5장 일일계정 및 슬롯 관리 |
| `06-site-management.md` | 6장 사업장 관리 |
| `07-organization.md` | 7장 조직 관리 |
| `08-permissions.md` | 8장 권한 체계 |
| `09-locking.md` | 9장 선점(처리 잠금) 정책 |
| `10-notifications.md` | 10장 알림 / 공지 정책 |
| `11-security-privacy.md` | 11장 보안 · 개인정보 · 위치정보 |
| `12-terms.md` | 12장 이용약관 관리 |
| `13-ui-ux.md` | 13장 공통 UI/UX 원칙 |
