# 운영 DB Workbench 접속 권한 부여 가이드

> 작성: 2026-07-20. 운영 RDS(MySQL)를 Workbench 직접 연결로 관리하기로 결정(Flyway 도입 보류)하면서,
> 다른 개발자에게 접속 권한을 주고/회수하는 절차를 역할별로 정리한 문서다.
> 관련: `배포_운영_매뉴얼.md` §4 (DB 운영), `작업지시서_Flyway-마이그레이션-자동적용.md` (보류 배너)

---

## 0. 구조 이해 — 관문이 2개다

운영 RDS는 퍼블릭 액세스가 없다. 접속하려면 반드시 두 관문을 모두 통과해야 한다:

```
개발자 PC ──(관문1: SSH 터널, 개인 SSH 키)──> 백엔드 EC2(3.38.237.103) ──(관문2: MySQL 계정)──> RDS
```

- **관문 1 — SSH**: EC2의 `authorized_keys`에 등록된 SSH 키가 있어야 터널이 열린다.
- **관문 2 — MySQL 계정**: 터널을 열어도 MySQL 계정/비밀번호가 없으면 DB에 못 들어간다.

권한 부여 = 두 관문을 각각 열어주는 것. 회수 = 두 관문을 각각 닫는 것.

**원칙**
- pem 파일(`prafta-key.pem`)을 복사해서 나눠주지 않는다 — 회수가 불가능해진다. 개발자마다 개인 SSH 키를 등록한다.
- 앱 계정(`prafta`)을 사람에게 주지 않는다 — 개인별 MySQL 계정을 만든다.
- 일반 개발자는 조회 전용(`_ro`), 데이터 수정이 필요한 소수만 DML 계정(`_dml`). DDL(스키마 변경)은 관리자만.

---

## 1. [개발자] SSH 키 쌍 생성 → 공개키 전달

개발자 본인 PC(Windows 10/11 기준, PowerShell 또는 cmd)에서:

```powershell
ssh-keygen -t rsa -b 4096 -m PEM -C "이름@prafta" -f "$env:USERPROFILE\.ssh\prafta-prod"
```

- 실행 중 passphrase를 물으면 설정 권장(생략 가능).
- `-m PEM` 은 Workbench 호환을 위한 것 (최신 OpenSSH 기본 포맷은 구버전 Workbench에서 인식 실패 사례 있음).
- 생성 결과 2개 파일:
  - `C:\Users\<사용자>\.ssh\prafta-prod` ← **개인키. 절대 전달·공유 금지**
  - `C:\Users\<사용자>\.ssh\prafta-prod.pub` ← **공개키. 이 파일 내용만 관리자에게 전달**

공개키 내용 확인(이걸 복사해서 관리자에게 메신저 등으로 전달):

```powershell
type $env:USERPROFILE\.ssh\prafta-prod.pub
```

`ssh-rsa AAAA... 이름@prafta` 형태의 한 줄이다. 공개키는 유출돼도 위험하지 않으므로 전달 수단은 자유.

---

## 2. [관리자] EC2 에 공개키 등록 (관문 1 열기)

전달받은 공개키 한 줄을 EC2의 `authorized_keys`에 추가한다. 관리자 PC에서:

```powershell
# received.pub = 전달받은 공개키를 저장한 파일 (혹은 한 줄 문자열)
type received.pub | ssh -i C:\Users\dudjs\.ssh\prafta-key.pem ec2-user@3.38.237.103 "cat >> ~/.ssh/authorized_keys"
```

또는 EC2에 직접 접속해서 편집해도 된다:

```bash
ssh -i C:/Users/dudjs/.ssh/prafta-key.pem ec2-user@3.38.237.103
echo 'ssh-rsa AAAA... 이름@prafta' >> ~/.ssh/authorized_keys
```

등록 확인(개발자에게 시켜본다):

```powershell
ssh -i $env:USERPROFILE\.ssh\prafta-prod ec2-user@3.38.237.103 "echo OK"
```

`OK`가 출력되면 관문 1 통과.

---

## 3. [관리자] MySQL 계정 생성 (관문 2 열기)

EC2 접속 후 `mysql prafta` (접속정보는 `~/.my.cnf`에 저장돼 있음) 또는 본인 Workbench 운영 연결에서 실행.
계정명은 `이름_역할` 규칙 권장 (예: `hong_ro`, `hong_dml`).

```sql
-- (A) 조회 전용 — 일반 개발자 기본
CREATE USER 'hong_ro'@'%' IDENTIFIED BY '<강한 비밀번호>';
GRANT SELECT ON prafta.* TO 'hong_ro'@'%';

-- (B) 데이터 수정 가능 — 코드값(TB_SYST_VAL_M/D 등)·데이터 보정 담당 소수만
--     DROP/ALTER/CREATE/TRUNCATE 가 없어 실수로 테이블을 파괴할 수 없다
CREATE USER 'hong_dml'@'%' IDENTIFIED BY '<강한 비밀번호>';
GRANT SELECT, INSERT, UPDATE, DELETE ON prafta.* TO 'hong_dml'@'%';
```

- 비밀번호는 개인별로 다르게, 관리자가 1회 전달 후 본인이 보관.
- DDL(스키마 변경)은 계정을 만들어주지 않는다 — `배포_운영_매뉴얼.md` §4 절차(사전 스냅샷 → scp → 적용)로 관리자가 수행.

생성 확인:

```sql
SELECT USER, HOST FROM mysql.user WHERE USER LIKE 'hong%';
SHOW GRANTS FOR 'hong_dml'@'%';
```

---

## 4. [개발자] Workbench 연결 생성

Workbench → Database → Manage Connections → New 로 아래처럼 입력:

| 항목 | 값 |
|---|---|
| Connection Name | `🔴 PRAFTA [PROD]` (로컬 연결과 확실히 구분되게) |
| Connection Method | **Standard TCP/IP over SSH** |
| SSH Hostname | `3.38.237.103:22` |
| SSH Username | `ec2-user` |
| SSH Key File | **본인 개인키** 경로 (예: `C:\Users\<사용자>\.ssh\prafta-prod`) |
| MySQL Hostname | `prafta-db.cj2yeu0mepo8.ap-northeast-2.rds.amazonaws.com` |
| MySQL Server Port | `3306` |
| Username | **본인 MySQL 계정** (예: `hong_dml`) |
| Default Schema | `prafta` |

- **Test Connection** 성공 확인 → **Close** 누르면 저장 (별도 저장 버튼 없음). 홈 화면 타일 더블클릭으로 접속.
- 첫 접속 시 MySQL 비밀번호 입력 → "Save password in vault" 체크하면 이후 생략.

**개발자 필수 설정 2가지** (연결 만들고 바로):

1. Edit → Preferences → SQL Editor → 하단 **"Safe Updates" 체크** — WHERE 없는 UPDATE/DELETE 차단. (의도적 해제는 세션에서 `SET SQL_SAFE_UPDATES=0;`)
2. 운영 연결은 로컬 작업 창과 **별도 Workbench 창**으로 띄우는 습관 — 탭 착각이 사고의 전형적 경로.

---

## 5. [관리자] 회수 절차

두 관문을 모두 닫는다:

```sql
-- 관문 2: MySQL 계정 삭제
DROP USER 'hong_dml'@'%';   -- 만든 계정 전부 (hong_ro 등 포함)
```

```bash
# 관문 1: EC2 공개키 제거 — authorized_keys 에서 해당 개발자 줄 삭제
ssh -i C:/Users/dudjs/.ssh/prafta-key.pem ec2-user@3.38.237.103
nano ~/.ssh/authorized_keys   # '이름@prafta' 주석이 붙은 줄을 찾아 삭제
```

키 등록 시 `-C "이름@prafta"` 주석을 넣어두었기 때문에 줄 끝 주석으로 누구 키인지 식별할 수 있다.

---

## 6. 운영 수칙 (전원 공통)

- **대량 수정 전 수동 스냅샷**: RDS 자동 백업 보존이 1일뿐(무료 플랜). 여러 행을 건드리기 전 RDS 콘솔 → `prafta-db` → 작업 → 스냅샷 생성.
- **수동 DML 기록**: 운영에 실행한 데이터 수정 SQL은 실행 후 기록을 남긴다 (실행일·실행자·SQL·사유 — 본 문서 하단 이력표 또는 별도 로그).
- 로컬 DB와 운영 DB에 같은 회사코드(001 등) 데이터가 양쪽에 존재한다 — **쿼리 실행 전 창 제목의 연결 이름을 확인**하는 습관.
- 색상 구분이 꼭 필요하면 운영 접속만 DBeaver 사용 고려 (Connection type=Production → 빨간 테두리 + 실행 전 확인 + auto-commit 해제).

---

## 부록: 접근자 현황 (수기 관리)

| 이름 | MySQL 계정 | 권한 | SSH 키 등록일 | 회수일 |
|---|---|---|---|---|
| (예) 홍길동 | hong_dml | DML | 2026-07-XX | |
