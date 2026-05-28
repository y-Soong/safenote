DB 스키마 참조 규칙

prafta는 MySQL 8.0.42를 사용한다. 스키마 정보는 다음 두 가지 방식으로 참조 가능하다.

1순위: MCP MySQL 서버 (실시간)

MCP 서버 이름: `prafta-mysql`

접속 계정: read-only 권한 (SELECT, SHOW, DESCRIBE, EXPLAIN만 가능)

사용 예: 테이블 구조 확인은 `DESCRIBE TB\_USER`, `SHOW CREATE TABLE TB\_DAILY\_USER\_SLOT` 등 실제 쿼리로 조회

장점: 항상 최신 스키마 반영, 트리거/뷰/프로시저 본문까지 직접 조회 가능

사용 가능 작업: SELECT, DESCRIBE, SHOW, EXPLAIN

2순위: schema-full.sql (스냅샷)

경로: `.claude/context/schema-full.sql`

MySQL 전체 DDL dump (mysqldump 결과)

MCP 서버가 불가용일 때만 사용

정기 갱신 필요 (스키마 변경 후 mysqldump 재실행)

언제 스키마를 읽어야 하는가

다음 작업을 시작하기 전에 반드시 스키마를 확인한다 (MCP 우선, 안 되면 schema-full.sql):

SQL을 작성하거나 수정하는 모든 작업

MyBatis mapper.xml을 작성하거나 수정하는 작업

DTO/Entity/VO 클래스를 작성하거나 수정하는 작업

신규 테이블 설계나 컬럼 추가를 검토하는 작업

DB 컬럼명/타입이 등장하는 모든 작업

어떻게 사용하는가

작업 시작 시 MCP 서버로 관련 테이블의 구조를 먼저 조회한다.

예: 회원 관련 작업이면 `DESCRIBE TB\_USER` 또는 `SHOW CREATE TABLE TB\_USER`

MCP 불가 시 schema-full.sql에서 `CREATE TABLE.\*TB\_USER`로 grep

컬럼명/타입/NULL 여부/기본값/PK/FK를 정확히 확인한다.

스키마에 명시된 정보만 사용한다.

절대 금지 사항

스키마에 없는 컬럼명을 추측해서 SQL/DTO/Mapper에 사용하지 않는다.

컬럼 타입을 추측하지 않는다 (예: USER\_ID가 varchar인지 bigint인지 추측 금지).

"보통 이런 컬럼이 있을 것 같다"는 가정으로 코드를 작성하지 않는다.

스키마와 코드의 컬럼이 불일치하면 즉시 사용자에게 보고한다.

스키마에서 찾지 못한 테이블/컬럼이 있으면 추측하지 말고 사용자에게 질문한다.

MCP MySQL 사용 시 추가 금지 사항

INSERT / UPDATE / DELETE / DROP / TRUNCATE / ALTER 시도 (read-only 계정으로 차단되어 있음).

실제 사용자 PII 데이터(평문 이름/휴대폰/이메일)를 화면에 출력하지 않는다. AES-GCM 암호화된 값은 그대로 출력해도 무방.

`SELECT \*` 사용 금지 (필요 컬럼만 명시).

대량 SELECT 시 반드시 `LIMIT` 부착 (페이징 없이 전수 조회 금지).

운영 DB에 직접 연결 금지 (로컬/개발 환경 DB만 연결).

DTO 매핑 규칙

DB 컬럼명: 대문자 + 언더스코어 (예: USER\_ID, CREATE\_DT)

Java DTO 필드명: 대문자 그대로 유지 (예: `private String USER\_ID;`)

MyBatis 결과 매핑: column → property 매핑 명시

SQL 작성 규칙 (재확인)

컬럼 콤마: leading (앞에 붙임)

파라미터 바인딩: `#{...}` 사용 (`${...}`는 정렬 컬럼명 등 특수 케이스만)

`SELECT \*` 금지, 명시적 컬럼 나열

모든 SQL은 실제 DB 스키마와 100% 일치해야 함

프론트엔드 환경

prafta는 별도의 web-frontend 프로젝트를 가진다.

프레임워크: Vue 3 + Vite

언어: JavaScript (TypeScript 미사용)

스타일: scoped CSS + CSS 변수 기반 자체 디자인 시스템

위치: `prafta-web-frontend/`

`src/views/{module}/` — 화면

`src/components/common/` — 공용 폼/UI

`src/components/popup/` — 전역 팝업

`src/components/modal/` — Alert / Confirm

`src/components/layout/` — 앱 셸

화면 작업 흐름

planner가 화면 명세를 Notion "도메인 지식 베이스"에 UI-{순번}으로 등록

planner가 Vue 컴포넌트 골격(template + style) 작성

developer가 골격의 script 영역(API 호출, store, router)을 채움

화면 작업 시 절대 규칙

색상/폰트/간격은 CSS 변수만 사용 (하드코딩 금지)

공통 컴포넌트가 있으면 우선 사용 (native HTML 직접 사용 지양)

TypeScript 문법 사용 금지

`<style>`은 반드시 scoped

비즈니스 정책서 참조 규칙

prafta의 비즈니스/도메인 정책은 `.claude/context/policies/` 하위에 분할 저장되어 있다. 모든 에이전트는 작업 시작 전 관련 정책서 섹션을 정독한다.

참조 파일

`.claude/context/policies/README.md` — 마스터 INDEX (정책서 우선순위 포함)

`.claude/context/policies/common/INDEX.md` — 공통 정책서 (v1.1) 키워드 매핑

`.claude/context/policies/attd/INDEX.md` — 근태관리 정책서 (v1.0) 키워드 매핑

`.claude/context/policies/request-approval/INDEX.md` — 요청승인관리 재기획서 (v0.1) 키워드 매핑

언제 이 파일들을 읽어야 하는가

비즈니스 룰이 등장하는 모든 작업에서, 관련 정책서 섹션을 읽는다 (전체 정독 아님):

출퇴근 판정, 슬롯 만료, 사후 상신 기한 등 도메인 규칙이 등장하는 작업

요구사항이 모호하거나 정책서 출처를 확인해야 하는 작업

화면 명세 / UI 동작 정의 작업

권한·인증·감사 로그 관련 작업

어떻게 사용하는가

planner: 작업 요청서를 분해할 때 정책서 출처를 작업 로그 "상세 설명"에 명시 (planner.md "비즈니스 정책서 참조 규칙" 참조).

developer/security/qa: planner가 명시한 정책서 출처를 정독.

출처가 없거나 모호하면 planner에게 재분해 요청.

정책서 우선순위 (충돌 시)

상세는 `.claude/context/policies/README.md` 참조. 핵심만:

요청승인관리 재기획서 (단일 출처 선언 영역)

공통 정책서

근태관리 정책서

기술 정책서 (`CLAUDE.md`, `developer.md`, `security.md`, `qa.md`, `planner.md`)

비즈니스 정책서와 기술 정책서가 충돌하면 비즈니스 정책서가 우선한다.

절대 금지 사항

정책서 전체를 통째로 읽지 않는다 (INDEX 경유 필수).

정책서에 명시되지 않은 비즈니스 룰을 추측해서 코드/명세에 반영하지 않는다.

docx 원본을 직접 읽지 않는다 (분할된 .md만 참조).

정책서 간 충돌이 의심되는데 우선순위로도 풀리지 않으면 즉시 사용자에게 보고.

작업 요청서

사용자가 직접 작성한 작업 요청서는 `.claude/requests/` 하위에 보관된다 (`prafta-001.txt`, `prafta-002.txt` 등). planner가 이를 정독하여 작업으로 분해한다.

`.claude/requests/` — 사용자 작성 작업 요청서

`.claude/context/policies/` — PRAFTA 비즈니스 정책서

두 디렉토리는 역할이 다르므로 혼동하지 않는다.

주석 / 식별자 명명 규칙

코드 주석은 한국어로 작성한다 (프로젝트 전반의 일관성 유지).

다만 변수명/메서드명/클래스명 등 코드 식별자는 영어로 작성한다 (Java/JavaScript 관례).

TODO 주석은 `// TODO(developer): ...` 형식을 따르며, 본문은 한국어.

로그 메시지도 한국어 (developer.md §6 로깅 컨벤션 참조).

실행 환경

Windows 10/11 + Git Bash 환경

python3 명령 없음. python 또는 py 사용

Bash heredoc (<<'EOF') 사용 금지 - stdin EOF 대기로 hang 발생

여러 줄 스크립트가 필요하면 임시 파일로 저장 후 실행

파일 편집은 Python 스크립트 우회 대신 Edit 도구로 직접 처리

tab/space mismatch는 view로 정확한 바이트 확인 후 그대로 복사

Bash 명령 실행 규칙 (전 에이전트 공통, 엄수)

Claude Code가 Bash 도구로 명령을 실행할 때 무한 대기(hang)에 빠지는 사고를 방지하기 위한 공통 규칙이다. 모든 에이전트(planner / developer / security / qa)에 동일하게 적용된다.

1\. stdin 대기 유발 명령 금지

다음 패턴은 절대 사용 금지. stdin EOF를 기다리며 hang한다:

Bash heredoc (`<<'EOF'`)

`read`, `cat`(인자 없이), `python`(스크립트 없이 대화형 진입)

입력 prompt가 발생할 수 있는 모든 명령

2\. 대화형 prompt 유발 명령은 반드시 비대화형 옵션으로

다음 명령들은 첫 실행 시 또는 특정 조건에서 사용자 확인 prompt를 띄운다. Git Bash에서 TTY가 없으면 그대로 무한 대기.

명령	위험	안전한 호출

`npx <pkg>`	"Ok to proceed? (y)" prompt	`npx --yes <pkg>` 또는 로컬 설치 후 `./node\_modules/.bin/<bin>` 직접 호출

`npm install`	peer dependency 충돌 시 prompt	`npm install --no-audit --no-fund --yes` 또는 `npm ci`

`git commit` (인자 없음)	editor 진입	`git commit -m "메시지"` 강제

`git rebase -i`	editor 진입	Claude Code에서 직접 사용 금지 (사용자 위임)

`gradlew` (Windows)	가끔 hang	반드시 `./gradlew.bat` 또는 `gradlew.bat`, 데몬 끄기 `--no-daemon` 권장

`mysql` 클라이언트	password prompt	환경변수 또는 `--password=...` (단, 로그 마스킹 주의)

3\. 장시간 실행 명령에 타임아웃 강제

Bash 도구 호출 시 명시적 타임아웃을 부여한다. 가이드라인:

명령 유형	권장 타임아웃

lint, syntax check, 단순 grep	60초

빌드(`gradlew build`, `vite build`)	300초 (5분)

테스트 실행	600초 (10분)

그 외 분석성 명령	120초

Bash 도구의 `timeout` 파라미터 또는 명령 내부 `timeout` 유틸리티 사용. 타임아웃을 명시하지 않은 채 외부 도구를 호출하지 않는다.

4\. hang 감지 및 보고 절차

다음 신호 중 하나라도 발생하면 즉시 작업을 중단하고 사용자에게 보고한다:

Bash 명령이 30초 이상 출력 없이 진행

"Waiting..." 표시가 1분 이상 지속

토큰 카운터가 증가하지 않고 단일 작업에 머무름

보고 형식:

```

\[Bash hang 감지] 명령: <실행 명령 원문>

경과 시간: <N분 N초>

추정 원인: (예: npx 첫 설치 prompt, peer dep 충돌, password 입력 대기)

다음 행동 후보:

&#x20; 1) <비대화형 옵션 추가하여 재시도>

&#x20; 2) <해당 검증 단계 건너뛰기>

&#x20; 3) <사용자 수동 처리 위임>

어느 쪽으로 진행할지 결정해 주세요.

```

5\. 도구 가용성 사전 확인

외부 CLI를 처음 호출하기 전, 가용성을 짧은 명령으로 먼저 확인한다 (타임아웃 10초):

`which <명령>` 또는 `command -v <명령>`

`<명령> --version` (단, npx는 이것도 prompt 가능하므로 `npx --yes` 또는 직접 경로 사용)

설치되어 있지 않으면 설치를 시도하지 말고 사용자에게 보고.

6\. 작업 중 디스크 부담이 큰 명령

다음 명령은 작업 시간을 크게 늘릴 수 있다. 필요 시에만 명시적 의도와 함께 실행:

`find / ...` 전체 파일시스템 스캔 → 작업 디렉토리로 범위 제한

`npm install`을 매번 호출 → `node\_modules` 존재 확인 후 skip

`gradlew clean build` → 정말 필요한 경우만 (clean 없이 build 우선)

7\. 절대 금지 사항 (Bash 관련 요약)

타임아웃 없이 외부 도구(npm/npx/gradle/mysql 등) 호출

비대화형 옵션 없이 `npx <pkg>` 호출

heredoc 사용

editor 진입이 가능한 git 명령(인자 없는 `git commit`, `git rebase -i` 등) 호출

hang이 의심되는데 보고 없이 추가 명령으로 덮어쓰기

