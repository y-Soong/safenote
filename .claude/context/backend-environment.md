# PRAFTA 백엔드 환경변수 / 외부화 정리

prafta.com 배포를 앞두고 코드/설정에 하드코딩돼 있던 경로·시크릿을 환경변수로 외부화한 내역과,
현재 백엔드가 요구하는 전체 환경변수 목록·사용법을 정리한다.

- 설정 파일 위치: `PRAFTA/prafta-backend/src/main/resources/`
- 활성 프로파일: `application.properties` 의 `spring.profiles.active=local` → `application-local.properties` 로딩
- 값 주입 문법: Spring placeholder `${ENV_NAME:기본값}` (기본값 없으면 `${ENV_NAME}`)
- `.env.example` 템플릿은 관례상 `PRAFTA/prafta-backend/.env.example` (프로젝트 루트)에 둔다.
  (`.gitignore` 의 `!.env.example` 허용 규칙, 개발자가 `.env` 와 같은 위치에서 찾기 때문)

---

## 1. 환경변수 전체 목록

### 필수 (기본값 없음 → 미설정 시 앱 기동 실패)

| 환경변수 | 용도 | 사용 위치 |
|---|---|---|
| `JWT_SECRET` | JWT 서명 시크릿 | `application.properties` → `jwt.secret` → `JwtUtil` |
| `SECURITY_PASSWORD_PEPPER` | 비밀번호 해싱 pepper | `security.password.pepper` → `PasswordHasher` |
| `PRAFTA_REFRESH_TOKEN_PEPPER` | RefreshToken HMAC pepper | `crypto.hmacPepper` |
| `PRAFTA_AES_DATA_KEY` | PII AES-GCM 암호화 키(Base64) | `crypto.aesKey` |
| `DB_PASSWORD` | DB 비밀번호 | `application-local.properties` → `spring.datasource.password` → `DBConfig` |

> 앞 4개는 이전부터 환경변수였고, `DB_PASSWORD` 가 이번에 추가됐다.
> 즉 **로컬 실행 시 이제 `DB_PASSWORD` 도 함께 설정**해야 한다(기존엔 평문 박혀 있었음).

### 선택 (기본값 있음 → 미설정 시 기본값 사용)

| 환경변수 | 기본값 | 용도 |
|---|---|---|
| `DB_URL` | 로컬 MySQL + p6spy URL | `spring.datasource.url` |
| `DB_USERNAME` | `dev_prafta` | `spring.datasource.username` |
| `FILE_UPLOAD_BASE_DIR` | `${user.dir}/uploads` | 업로드 파일 저장/서빙 루트 |
| `HOLIDAY_API_BASE_URL` | 공공데이터포털 URL | 공휴일 API |
| `HOLIDAY_API_SERVICE_KEY` | (빈 값) | 공휴일 API 키 |
| `HOLIDAY_API_NUM_OF_ROWS` | `100` | 공휴일 API |
| `HOLIDAY_API_TYPE` | `json` | 공휴일 API |

### 빌드 환경 (Spring 프로퍼티 아님)

| 환경변수 | 용도 |
|---|---|
| `JAVA_HOME` | Gradle 실행 JVM / JDK21 toolchain 탐지. `gradle.properties` 의 JDK 절대경로 하드코딩 제거 후 이 값에 의존. |

---

## 2. 이번 작업으로 바뀐 것

### (A) 업로드 파일 경로 — 하드코딩 절대경로 제거
- 기존: 서빙은 `file:///C:/PRAFTA/PRAFTA/prafta-backend/uploads/` 절대경로 하드코딩(`ApiPrefixConfig`),
  저장은 `System.getProperty("user.dir")+"/uploads"`(`FileServiceImpl`) → **두 경로가 실행 위치에 따라 어긋날 수 있었음**.
- 변경: 공통 프로퍼티 `file.upload.base-dir=${FILE_UPLOAD_BASE_DIR:${user.dir}/uploads}` 신설,
  서빙·저장이 동일 값을 사용. DB 저장 상대경로의 `/uploads` 는 공개 URL 마운트 경로라 유지.
- 기본값이 기존 동작과 같아 로컬 영향 없음. 운영에선 영속 볼륨 경로를 `FILE_UPLOAD_BASE_DIR` 로 지정.

### (B) Gradle JDK 절대경로 — 제거
- 기존: `gradle.properties` 에 `org.gradle.java.home=C:\Java\jdk-21.0.2` 등 JDK 절대경로 하드코딩
  → 다른 PC/서버에 해당 경로 없으면 **Gradle 기동 자체 실패**.
- 변경: 두 줄 제거. `build.gradle` 의 toolchain(JDK 21) + `JAVA_HOME` + auto-detect 로 위임.
  머신별 경로가 필요하면 커밋되지 않는 `~/.gradle/gradle.properties` 에 둔다.

### (C) DB 자격증명 — 평문 제거 + 죽은 파일 삭제
- `application-local.properties` 의 `spring.datasource.password=prafta12345!` 평문 제거 → `${DB_PASSWORD}` 로 전환.
  url/username 은 비-시크릿이라 기본값을 유지하되 `DB_URL`/`DB_USERNAME` 으로 덮어쓰기 가능.
- **`db-local.properties` / `db-prod.properties` 삭제**: `db.*` 접두로 `root` / `praftadkagh1!` 를 담고 있었으나,
  코드 어디에서도 참조되지 않는 **죽은 파일**이었다(실제 datasource 는 `spring.datasource.*`). 보안상 제거.

---

## 3. `.env` 사용법 (중요 — 자세히)

### 3.1 핵심 개념: `.env` 파일은 "그 자체로는" 아무것도 안 한다

Spring Boot 는 `.env` 파일을 **자동으로 읽지 않는다**. `${DB_PASSWORD}` 같은 placeholder 는
**JVM 프로세스의 OS 환경변수(또는 JVM `-D` 시스템 프로퍼티)** 에서 값을 찾는다.

따라서 `.env` 는 어디까지나 "메모/템플릿"일 뿐이고, **그 값을 OS 환경변수로 실제로 올려주는 주체**가
따로 있어야 한다 (IDE 실행구성 / 플러그인 / 쉘 export / 도커). 아래 방법 중 **하나**를 택한다.

### 3.2 먼저 `.env` 파일 만들기

`PRAFTA/prafta-backend/.env.example` 을 복사해서 같은 위치에 `.env` 를 만들고 실제 값을 채운다.
(`.env` 는 `.gitignore` 로 커밋되지 않는다.)

```dotenv
# PRAFTA/prafta-backend/.env  (커밋 금지)
JWT_SECRET=로컬용_충분히_긴_랜덤문자열
SECURITY_PASSWORD_PEPPER=로컬_pepper값
PRAFTA_REFRESH_TOKEN_PEPPER=로컬_refresh_pepper값
PRAFTA_AES_DATA_KEY=Base64로_인코딩된_AES키
DB_PASSWORD=prafta12345!      # 로컬 dev_prafta 계정의 실제 비밀번호

# 아래는 선택(기본값으로 충분하면 생략)
# DB_URL=jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
# DB_USERNAME=dev_prafta
# FILE_UPLOAD_BASE_DIR=C:/PRAFTA/PRAFTA/prafta-backend/uploads
```

> 참고: 기존에 로컬에서 `dev_prafta` 계정으로 잘 돌아갔다면 그 비밀번호가 `prafta12345!` 였다.
> (이번에 평문을 properties 에서 뺐을 뿐, 비밀번호 자체가 바뀐 건 아니다.)

### 3.3 그 값을 실제로 올리는 방법 (택 1)

#### 방법 A — IntelliJ 실행구성 (가장 흔함)
1. 상단 Run/Debug Configurations → 해당 Spring Boot 애플리케이션 선택 → Edit Configurations.
2. **Environment variables** 칸에 직접 입력:
   `JWT_SECRET=...;SECURITY_PASSWORD_PEPPER=...;PRAFTA_REFRESH_TOKEN_PEPPER=...;PRAFTA_AES_DATA_KEY=...;DB_PASSWORD=prafta12345!`
   (세미콜론 `;` 로 구분)
3. 또는 **EnvFile 플러그인** 설치 후 "Enable EnvFile" 체크 → 위 `.env` 파일을 지정하면
   매 실행 시 자동으로 읽어준다(파일 하나만 관리하면 되므로 편함).

#### 방법 B — 쉘에서 export 후 실행 (PowerShell / Git Bash)
- PowerShell:
  ```powershell
  $env:DB_PASSWORD="prafta12345!"
  $env:JWT_SECRET="..."   # 필수 4개도 동일하게
  ./gradlew.bat bootRun
  ```
- Git Bash:
  ```bash
  export DB_PASSWORD='prafta12345!'
  export JWT_SECRET='...'
  ./gradlew.bat bootRun
  ```
  (이 방식은 해당 터미널 세션에서만 유효하다.)

#### 방법 C — 도커 / 운영 서버
- docker compose: `env_file: .env` 또는 `environment:` 블록에 나열.
- 운영 서버: 시스템 환경변수 또는 시크릿 매니저(AWS SSM/Secrets Manager 등)로 주입.

### 3.4 적용 확인
앱 기동 로그에 DB 연결 오류(`Access denied` / `password ... is null`)가 없으면 정상.
`DB_PASSWORD` 누락 시 placeholder 미해석으로 빈 비밀번호 → `Access denied for user 'dev_prafta'` 류 에러가 난다.

---

## 4. 남은 권장 작업 (이번 범위 밖)

1. **`root` DB 비밀번호 로테이션**: 삭제했어도 git 히스토리에는 `praftadkagh1!` 가 남아 있다. 운영 전 비밀번호 변경 권장.
2. **운영 프로파일 datasource**: `application-prod.properties` 는 현재 비어 있음. 배포 시 prod 프로파일에도
   동일한 `${DB_URL}/${DB_USERNAME}/${DB_PASSWORD}` 패턴으로 datasource 를 구성해야 한다(p6spy 드라이버는 운영에서 제외 검토).
3. **프론트/모바일 관련 env(참고)**: 웹 프론트는 `VITE_FILE_API_BASE`/`VITE_API_CONTEXT`, Flutter 셸은
   빌드 옵션 `--dart-define=APP_BASE_URL=https://prafta.com` 으로 백엔드 주소를 주입한다(백엔드 .env 와는 별개).
