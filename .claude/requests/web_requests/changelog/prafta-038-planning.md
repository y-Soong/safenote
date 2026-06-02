# prafta-038 — Flutter 셸(safenote) APK 배포 빌드 가능 상태 정비 (인프라/배포 1차 분해)

> **작업 요청서**: `.claude/requests/prafta-038.md` (채팅 지시 — 짧은 stub)
> **대상 프로젝트**: `PRAFTA_FLUTTER/safenote/` (Flutter 3.x + flutter_inappwebview 6.x 기반 Android 셸)
> **본 라운드 스코프**: 인프라/배포 정비 7개 작업(applicationId·라벨·아이콘·signing·release localhost 분기·release network config·Vite 동기화 스크립트). 비즈니스 로직 / dead code 정리 / 권한 정비 / 위생 항목은 deferred.
> **분해일자**: 2026-05-28
> **작성자**: planner (prafta-038-1 분해)
> **정책서 매핑**: 본 라운드 대상 작업은 배포 인프라 영역으로 비즈니스 정책서와 무관 (사용자 지시 명시).

---

## 1. 분해 원칙

1. 본 라운드는 **분해만**. 실제 파일 수정/생성은 developer 후속 라운드에서 수행. planner 는 본 markdown 2개(`prafta-038.md` stub + 본 planning 문서) 외 어떤 코드 파일도 작성하지 않는다.
2. 사용자가 확정한 결정(applicationId / 앱 라벨 / 아이콘 원본 / release 번들 로딩 방식 / keystore 정책 / 백엔드 URL hooks / sync 스크립트 위치) 7건을 그대로 따른다.
3. 메인 세션 직전 보고의 22개 리뷰 발견 사항 중 Phase A 7개(1·2·3·4·5·6·7) 만 본 라운드에서 다룬다. 나머지(8~13)는 §5 deferred.
4. 신규/교체 파일은 작업 단위로 1:1 매핑되도록 분해. 한 작업이 여러 파일에 걸치면 "선행 관계"를 명시.
5. 각 작업은 (a) 대상 파일 (b) 변경 요지 (c) 결정 사유 (d) developer 후속 절차 까지 기록.

---

## 2. 작업 ID 분해표 (PLNprafta-038NNN)

> ID 채번 규칙: `PLNprafta-038` + 3자리 순번. 본 라운드 7개 작업.

| 작업 ID | 유형 | 제목 | 대상 파일 | 우선순위 | 선행 |
|---|---|---|---|---|---|
| PLNprafta-038001 | infra/android | applicationId / namespace / Kotlin 패키지 디렉토리 이동 | `android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`, Kotlin 패키지 디렉토리 이동 | P0 (다른 모든 빌드 작업의 선행) | 없음 |
| PLNprafta-038002 | infra/android | 앱 라벨 `세이프노트` 적용 (strings.xml 신설) | `android/app/src/main/res/values/strings.xml` (신규), `AndroidManifest.xml` | P0 | PLNprafta-038001 |
| PLNprafta-038003 | infra/android | release signing 분기 + `.gitignore` 보강 + `key.properties.example` 생성 | `android/app/build.gradle.kts`, `android/key.properties.example` (신규), `.gitignore` | P1 | PLNprafta-038001 |
| PLNprafta-038004 | infra/android | release용 network_security_config 분리 + `usesCleartextTraffic` 제거 | `android/app/src/release/res/xml/network_security_config.xml` (신규), `android/app/src/main/res/xml/network_security_config.xml` (주석 보강), `AndroidManifest.xml` | P1 | PLNprafta-038001 |
| PLNprafta-038005 | infra/flutter | `web_app.dart` release/debug 분기 (InAppLocalhostServer 도입 + `--dart-define` hooks) | `lib/web_app.dart` | P1 | 없음 (PLNprafta-038007 산출물이 release 시 필요하지만 코드 변경은 독립) |
| PLNprafta-038006 | infra/flutter | flutter_launcher_icons 도입 + assets/icons/ 신규 + pubspec.yaml 정비 | `pubspec.yaml`, `assets/icons/app_icon.png` (신규 — 원본 복사), `assets/icons/app_icon_foreground.png` (신규 — 원본 복사) | P2 | 없음 |
| PLNprafta-038007 | infra/build | Vite → assets/vue_app 동기화 PowerShell 스크립트 | `scripts/sync-vue-app.ps1` (신규) | P1 | 없음 (PLNprafta-038005 의 release 분기와 함께 동작해야 의미 있음) |

### 2.1 각 작업 상세

---

#### PLNprafta-038001 — applicationId / namespace / Kotlin 패키지 디렉토리 이동

- **유형**: infra/android (Gradle 설정 + Kotlin 패키지 이동 + manifest 정비)
- **변경 요지**:
  1. `android/app/build.gradle.kts`:
     - `namespace = "com.example.safenote"` → `namespace = "com.prafta.safenote"`
     - `defaultConfig.applicationId = "com.example.safenote"` → `applicationId = "com.prafta.safenote"`
  2. `android/app/src/main/AndroidManifest.xml`:
     - 루트 `<manifest>` 의 `package="com.example.safenote"` 속성 **제거** (AGP 8.x 에서 namespace 가 권위, package 속성은 deprecated)
  3. Kotlin 패키지 디렉토리 이동:
     - 기존: `android/app/src/main/kotlin/com/example/safenote/MainActivity.kt`
     - 신규: `android/app/src/main/kotlin/com/prafta/safenote/MainActivity.kt`
     - 파일 내부 `package com.example.safenote` → `package com.prafta.safenote`
     - 기존 `com/example/safenote/` 디렉토리 + 빈 `com/example/` 디렉토리 삭제
     - `java/io/...` 폴더는 비어 있으면 정리, 비어있지 않으면 손대지 않음 (현재 검증: planner 가 정독 시 `java/` 폴더는 확인하지 않았으므로 developer 가 ls 로 확인 후 비어 있을 때만 삭제)
- **결정 사유**:
  - applicationId 와 namespace 를 동일하게 맞추는 것이 Flutter+AGP 8.x 의 표준 패턴. 분리하면 R 클래스 경로 혼선 발생.
  - manifest 의 `package` 속성은 AGP 8.0+ 에서 deprecated. 빌드 시 경고 발생하므로 제거.
  - 사용자가 별도 dev/prod flavor 를 요구하지 않았으므로 단일 applicationId 사용. flavor 도입은 향후 검토.
- **developer 후속 절차**:
  1. `git mv` 또는 수동 디렉토리 이동(Robocopy `/MOVE` 또는 IDE refactor)으로 Kotlin 파일 이동.
  2. `flutter clean && flutter pub get` 후 빌드 확인.
  3. APK 의 applicationId 가 `com.prafta.safenote` 인지 `aapt dump badging` 또는 `./gradlew :app:dependencies` 로 확인 권장.

---

#### PLNprafta-038002 — 앱 라벨 `세이프노트` 적용 (strings.xml 신설)

- **유형**: infra/android (리소스 신설 + manifest 참조 변경)
- **변경 요지**:
  1. `android/app/src/main/res/values/strings.xml` **신규 생성** (현재 미존재 — Glob 검증 완료):
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <resources>
         <string name="app_name">세이프노트</string>
     </resources>
     ```
  2. `android/app/src/main/AndroidManifest.xml`:
     - `<application ... android:label="safenote" ...>` → `android:label="@string/app_name"`
- **결정 사유**:
  - 한글 라벨을 manifest 에 직접 하드코딩하지 않고 strings.xml 로 분리하는 것이 Android 표준 i18n 패턴. 향후 다국어 추가 시 `values-en/strings.xml` 만 추가하면 됨.
  - `values/strings.xml` 이 현재 존재하지 않음을 Glob 으로 확인 (`PRAFTA_FLUTTER/safenote/android/app/src/main/res/values/` 에는 `styles.xml` 만 존재).
- **developer 후속 절차**:
  1. strings.xml 인코딩은 UTF-8 (BOM 없음). 한글 깨짐 주의.
  2. 빌드 후 런처에 "세이프노트" 가 표시되는지 시뮬레이터/실기기에서 확인.

---

#### PLNprafta-038003 — release signing 분기 + `.gitignore` 보강 + `key.properties.example` 생성

- **유형**: infra/android (Gradle signing 분기 패턴 + 비밀파일 예시)
- **변경 요지**:
  1. `android/app/build.gradle.kts` 상단 import 추가:
     ```kotlin
     import java.util.Properties
     import java.io.FileInputStream
     ```
  2. `android` 블록 상단(`namespace` 위)에 keystoreProperties 로딩 로직 추가:
     ```kotlin
     val keystorePropertiesFile = rootProject.file("key.properties")
     val keystoreProperties = Properties().apply {
         if (keystorePropertiesFile.exists()) {
             load(FileInputStream(keystorePropertiesFile))
         }
     }
     ```
  3. `android` 블록 내 `signingConfigs { ... }` 신규(또는 `buildTypes` 위에 삽입):
     ```kotlin
     signingConfigs {
         if (keystorePropertiesFile.exists()) {
             create("release") {
                 storeFile = file(keystoreProperties["storeFile"] as String)
                 storePassword = keystoreProperties["storePassword"] as String
                 keyAlias = keystoreProperties["keyAlias"] as String
                 keyPassword = keystoreProperties["keyPassword"] as String
             }
         }
     }
     ```
  4. `buildTypes.release` 블록의 `signingConfig` 변경:
     ```kotlin
     signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
     ```
     - `findByName` 으로 release 가 없으면 debug 키 fallback (현재 동작 유지).
     - 빌드 로그에 명확한 경고를 출력하려면 `if (signingConfigs.findByName("release") == null) { logger.warn("[safenote] key.properties 가 없어 release 빌드가 debug 키로 서명됩니다. 스토어 업로드 불가.") }` 추가 권장.
  5. `android/key.properties.example` **신규 생성**:
     ```properties
     # 실제 keystore 생성 후 본 파일을 key.properties 로 복사·작성. 절대 커밋 금지.
     # storeFile 은 절대경로 권장 (상대경로 사용 시 rootProject.file 기준).
     storePassword=<keystore 비밀번호>
     keyPassword=<key 비밀번호>
     keyAlias=<alias, 예: safenote>
     storeFile=<keystore 절대경로, 예: C:/PRAFTA/PRAFTA_FLUTTER/safenote/android/safenote-upload-key.jks>
     ```
  6. `PRAFTA_FLUTTER/safenote/.gitignore` 하단에 추가:
     ```
     # Android signing (절대 커밋 금지)
     android/key.properties
     android/*.jks
     android/*.keystore
     android/app/*.jks
     android/app/*.keystore
     android/app/upload-keystore.jks
     android/app/release.keystore
     ```
- **결정 사유**:
  - Gradle Kotlin DSL 에서 `java.util.Properties` 를 사용해 key.properties 를 읽는 패턴은 Flutter 공식 문서(`https://docs.flutter.dev/deployment/android#signing-the-app`) 의 표준 예시. file.exists() 가드로 미존재 시 fallback 처리.
  - **사용자 결정**: planner/developer 는 keystore 파일 자체를 생성하지 않음. 사용자가 `keytool -genkey` 로 직접 생성하고 `key.properties` 를 작성한다.
  - `.gitignore` 에 `android/app/*.jks` 도 포함하는 이유: 일부 가이드는 keystore 를 `android/app/` 에 두는 경우가 있음. 양쪽 모두 차단.
- **developer 후속 절차**:
  1. 위 build.gradle.kts 수정 적용 후 `./gradlew.bat :app:assembleDebug --no-daemon` 으로 syntax 검증 (timeout 300s).
  2. key.properties 가 없을 때 build 가 debug 키로 release 빌드를 만들어 내는지 확인. 의도된 fallback 이지만 경고 로그가 나오는지 확인.
  3. 사용자에게 keystore 수동 생성 명령 안내 (§4 사용자 수동 단계 참조).

---

#### PLNprafta-038004 — release용 network_security_config 분리 + `usesCleartextTraffic` 제거

- **유형**: infra/android (리소스 분기 + manifest 보안 속성 제거)
- **변경 요지**:
  1. `android/app/src/release/res/xml/network_security_config.xml` **신규 생성** (release sourceSet — 디렉토리 신설):
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <network-security-config>
         <!-- release: cleartext 차단 + 시스템 인증서만 신뢰 -->
         <base-config cleartextTrafficPermitted="false">
             <trust-anchors>
                 <certificates src="system"/>
             </trust-anchors>
         </base-config>
         <!-- 운영 도메인 결정 후 domain-config 추가 -->
     </network-security-config>
     ```
  2. `android/app/src/main/res/xml/network_security_config.xml` 기존 파일 상단에 주석 보강 (변경 없이 의도 명시):
     ```xml
     <!-- ⚠️ 이 파일은 debug 빌드 전용. release 빌드는 src/release/res/xml/network_security_config.xml 가 우선 적용됨.
          개발 단계: 사용자(단말) 인증서 신뢰 + 172.30.1.4 cleartext 허용. -->
     ```
  3. `android/app/src/main/AndroidManifest.xml`:
     - `<application ... android:usesCleartextTraffic="true" ...>` 속성 **제거**.
     - `android:networkSecurityConfig="@xml/network_security_config"` 는 그대로 유지 (release 빌드 시 sourceSet 자동 병합으로 release/ 의 동일명 파일이 우선).
- **결정 사유 (옵션 B 채택)**:
  - 옵션 A(manifestPlaceholders 로 `${cleartextTraffic}` 분기)는 복잡도 증가. 또한 manifest 의 `usesCleartextTraffic` 속성은 networkSecurityConfig 가 있을 때 무시되는 것이 원칙이지만 API 레벨에 따라 우선순위가 달라질 수 있음.
  - 옵션 B(manifest 속성 제거 + sourceSet 분기): release/ 의 config 가 자동으로 main/ 을 덮어쓰며, base-config 의 `cleartextTrafficPermitted="false"` 가 모든 cleartext 트래픽을 차단. 가장 단순하고 표준적임.
  - 사용자 작업 지시서 §A-5 "권장: 옵션 B" 명시 준수.
- **developer 후속 절차**:
  1. `android/app/src/release/res/xml/` 디렉토리 신설 후 파일 생성 (Robocopy `/CREATE` 또는 단순 Write).
  2. `./gradlew.bat :app:assembleRelease --no-daemon` 빌드 후 (release 키 없으면 debug 폴백) 생성된 APK 를 `apkanalyzer` 로 열어 network_security_config 가 release 버전인지 확인 — 권장.
  3. release 에서 cleartext HTTP 호출이 차단되는지 실기기 테스트 (관련 비즈니스 로직 작업은 본 라운드 outside scope).

---

#### PLNprafta-038005 — `web_app.dart` release/debug 분기 (InAppLocalhostServer 도입 + `--dart-define` hooks)

- **유형**: infra/flutter (Dart 코드 변경 — release/debug 분기)
- **변경 요지**:
  1. import 추가:
     ```dart
     import 'package:flutter/foundation.dart' show kReleaseMode;
     ```
     (`flutter/services.dart` 의 SystemChrome 등은 기존 import 유지)
  2. 기존 `const String DEV_URL = "https://172.30.1.4:8082";` 제거. 다음으로 교체:
     ```dart
     // 빌드 시 --dart-define=APP_DEV_URL=... 으로 주입 가능. 기본값은 LAN IP.
     const String _APP_DEV_URL = String.fromEnvironment(
       'APP_DEV_URL',
       defaultValue: 'https://172.30.1.4:8082',
     );
     // 운영 백엔드 baseURL hooks. Vue 측 axios 가 이 값을 읽도록 후속 라운드에서 처리.
     // 본 라운드는 hook 만 작성. 기본값은 빈 문자열.
     const String _APP_BASE_URL = String.fromEnvironment(
       'APP_BASE_URL',
       defaultValue: '',
     );
     // release localhost 서버 포트. 충돌 시 변경.
     const int _LOCALHOST_PORT = 8080;
     ```
  3. `_WebAppState` 에 InAppLocalhostServer 필드 추가:
     ```dart
     InAppLocalhostServer? _localhostServer;
     ```
  4. `initState()` 에서 release 모드일 때 localhost 서버 시작:
     ```dart
     if (kReleaseMode) {
       _localhostServer = InAppLocalhostServer(
         documentRoot: 'assets/vue_app',
         port: _LOCALHOST_PORT,
       );
       // start 는 async 라서 별도 메서드로 분리 후 await
       _startLocalhostThenLoad();
     }
     ```
     - 별도 메서드 `Future<void> _startLocalhostThenLoad() async { await _localhostServer?.start(); /* controller 가 준비된 후 _openTarget 호출 */ }` 패턴. 또는 `onWebViewCreated` 콜백 내에서 release 분기로 처리.
  5. `_openDevServer` 메서드를 `_openTarget` 으로 일반화. 시그니처:
     ```dart
     Future<void> _openTarget(InAppWebViewController ctl) async {
       final String url = kReleaseMode
           ? 'http://localhost:$_LOCALHOST_PORT/index.html'
           : _APP_DEV_URL;
       await ctl.loadUrl(urlRequest: URLRequest(url: WebUri(url)));
       debugPrint('🌐 Load target -> $url (release=$kReleaseMode)');
     }
     ```
     기존 호출처(`onWebViewCreated` 의 `await _openDevServer(controller)`)를 `_openTarget` 으로 교체.
  6. `dispose()` 에 localhost 서버 종료 추가:
     ```dart
     @override
     void dispose() {
       _localhostServer?.close();
       WidgetsBinding.instance.removeObserver(this);
       super.dispose();
     }
     ```
- **API 검증 사항**:
  - **flutter_inappwebview 6.x 의 `InAppLocalhostServer` 공개 API 시그니처**:
    - 생성자: `InAppLocalhostServer({int port = 8080, String directoryIndex = 'index.html', String documentRoot = './', bool shared = false})`
    - 메서드: `Future<void> start()`, `Future<void> close()`, `bool isRunning()`
  - planner 의 로컬 검증 시도: `PRAFTA_FLUTTER/safenote/.dart_tool/` 및 pub-cache 글로브 시도했으나 시간초과 / 미발견. **developer 가 패키지 import 후 정확한 named parameter 와 documentRoot 가 `assets/vue_app` 으로 동작하는지 확인 필요**. Flutter assets 시스템 특성상 documentRoot 가 절대경로가 아닌 assets 매핑명일 수 있음 — 패키지 README/pub.dev 문서 참조 필수.
- **결정 사유**:
  - `kReleaseMode` 사용: profile/debug 모두에서 LAN IP 를 쓰고 싶을 가능성이 있어 `kDebugMode` 가 아닌 `kReleaseMode` 분기 채택. profile 빌드는 LAN IP 사용 (현재 동작 유지).
  - `--dart-define` hook 추가: 사용자 결정 "운영 백엔드 URL 미정 → hooks 만 작성" 준수. `_APP_BASE_URL` 은 본 라운드에서 사용하지 않으며 Vue 측 axios baseURL 처리가 outside scope. Dart 측에 상수 hook 만 둠.
  - InAppLocalhostServer 포트 8080: 기본값. 백엔드와 충돌하면 8081/8082 로 변경. 단 release 에서는 localhost 만 사용하므로 백엔드는 다른 호스트(공인 URL) 일 것이고 포트 충돌은 없을 것으로 예상.
- **developer 후속 절차**:
  1. 위 코드 적용 후 `flutter analyze` 로 lint 검증 (timeout 60s).
  2. release 빌드 명령: `flutter build apk --release --dart-define=APP_BASE_URL=https://<운영백엔드>` — 단 본 라운드는 baseURL 미정이라 `--dart-define` 생략해도 빌드 가능.
  3. release APK 실기기 설치 후 화면이 `http://localhost:8080/index.html` 을 로드하는지 logcat 으로 확인.

---

#### PLNprafta-038006 — flutter_launcher_icons 도입 + assets/icons/ 신규 + pubspec.yaml 정비

- **유형**: infra/flutter (dev_dependencies 추가 + 아이콘 원본 복사 + pubspec assets 정비)
- **변경 요지**:
  1. `pubspec.yaml` `dev_dependencies` 섹션:
     ```yaml
     dev_dependencies:
       flutter_test:
         sdk: flutter
       flutter_lints: ^5.0.0
       flutter_launcher_icons: ^0.13.1
     ```
  2. `pubspec.yaml` 하단에 launcher_icons 설정 섹션 추가:
     ```yaml
     flutter_launcher_icons:
       android: true
       ios: false
       image_path: "assets/icons/app_icon.png"
       min_sdk_android: 21
       adaptive_icon_background: "#FFFFFF"
       adaptive_icon_foreground: "assets/icons/app_icon_foreground.png"
       remove_alpha_ios: true
     ```
  3. 아이콘 원본 파일 복사 (디렉토리 신설):
     - 원본: `C:\PRAFTA\PRAFTA\prafta-app-frontend\prafta-app-frontend\src\assets\img\safenote_sign.png` (Glob 검증 완료, 존재)
     - 신규 1: `PRAFTA_FLUTTER/safenote/assets/icons/app_icon.png` (원본 그대로 복사)
     - 신규 2: `PRAFTA_FLUTTER/safenote/assets/icons/app_icon_foreground.png` (원본 그대로 복사 — adaptive icon foreground 용)
  4. `pubspec.yaml` `flutter.assets` 섹션 정비:
     - 기존(stale):
       ```yaml
       assets:
         - assets/vue_app/
         - assets/vue_app/js/
         - assets/vue_app/css/
         - assets/vue_app/img/
       ```
     - 신규(정비):
       ```yaml
       assets:
         - assets/vue_app/
         - assets/icons/
       ```
     - Vite 산출물(hashed filename) 은 `assets/vue_app/` 하위에 `assets/`, `js/`, `css/`, `img/`, 폰트 파일 등이 자유롭게 들어가므로 **단일 라인** `- assets/vue_app/` 으로 통합. Flutter 의 assets 선언은 디렉토리 단일 라인 시 직속 자식만 포함하므로, Vite 의 hashed 출력 구조(`assets/vue_app/index.html` + `assets/vue_app/assets/*.js` 등)의 하위 디렉토리도 포함되려면 각 하위 디렉토리도 라인 추가 필요할 수 있음. **결정**: 일단 `- assets/vue_app/` 단일 라인으로 두고, developer 가 sync 스크립트 실행 후 `flutter pub get && flutter run` 으로 검증. Vite 산출물의 실제 구조는 `dist/assets/index-*.js`, `dist/assets/Pretendard-*.woff2` 등 1-depth 하위 디렉토리만 사용하므로 `- assets/vue_app/` + `- assets/vue_app/assets/` 두 라인 정도면 충분할 것으로 예상. 정확한 라인 수는 developer 가 sync 후 결정.
- **결정 사유**:
  - flutter_launcher_icons 0.13.x 는 Flutter 3.x + AGP 8.x 와 호환되는 안정 버전.
  - adaptive icon: 사용자가 fg/bg 를 분리 디자인하지 않았으므로 원본을 fg/bg 둘 다에 사용. 사용자가 추후 디자인 시 `app_icon_foreground.png` 만 교체.
  - 원본 PNG 사이즈는 1024x1024 권장이지만 미확인. flutter_launcher_icons 가 리사이즈 처리하므로 작동 자체는 가능. 단 결과 품질은 원본 해상도에 의존 — developer 가 빌드 후 런처 아이콘 시각 확인 필요.
  - **실제 아이콘 생성 명령은 사용자가 수동 실행**: `cd PRAFTA_FLUTTER/safenote && flutter pub get && flutter pub run flutter_launcher_icons`. 결과로 `mipmap-*/` 의 ic_launcher 가 교체되고, `mipmap-anydpi-v26/` 의 adaptive XML 이 생성됨.
  - mipmap 산출물은 `.gitignore` 대상이 아님 (커밋되어야 빌드 가능).
- **developer 후속 절차**:
  1. `assets/icons/` 디렉토리 신설 + 원본 PNG 2개 복사 (Copy-Item 또는 cp).
  2. pubspec.yaml 수정 후 `flutter pub get` (사용자 수동 실행 권장 — Bash 도구로 호출 시 timeout 60s).
  3. **launcher icon 생성은 사용자 수동 단계** (§4 참조).
  4. assets 라인 수가 부족해 vue_app 일부가 빠지면 sync 스크립트 실행 후 `flutter run` 시 404 발생. 그 시점에 라인 추가.

---

#### PLNprafta-038007 — Vite → assets/vue_app 동기화 PowerShell 스크립트

- **유형**: infra/build (Windows PowerShell 스크립트 신규)
- **변경 요지**:
  1. `PRAFTA_FLUTTER/safenote/scripts/sync-vue-app.ps1` **신규 생성** (디렉토리 신설):
     - 기능:
       1. `$ErrorActionPreference = 'Stop'` 설정
       2. 경로 변수 선언:
          - `$VueSrc = 'C:\PRAFTA\PRAFTA\prafta-app-frontend\prafta-app-frontend'`
          - `$AppAssets = (Resolve-Path "$PSScriptRoot\..\assets\vue_app").Path` (또는 미존재 시 New-Item)
       3. cert 파일 존재 확인 (vite.config.js 가 module load 시 `readFileSync` 하므로 빌드도 동일하게 cert 필요):
          - 체크 대상: `$VueSrc\cert\172.30.1.4-key.pem`, `$VueSrc\cert\172.30.1.4.pem`
          - 없으면 친절한 한국어 에러 메시지 출력 후 `exit 1`
       4. `Push-Location $VueSrc`
       5. `node_modules` 존재 확인 → 없으면 `npm install --no-audit --no-fund` (timeout 가이드: 5분)
       6. `npm run build` 실행 (Vite build → `dist/`)
       7. `Pop-Location`
       8. `$AppAssets` 비우기 (Remove-Item -Recurse -Force, 단 디렉토리 자체는 유지)
       9. `Copy-Item -Path "$VueSrc\dist\*" -Destination $AppAssets -Recurse -Force` 로 mirror 복사
       10. 단계별 한국어 echo 출력 (`Write-Host` + 색상)
  2. 스크립트 내용 (참고 골격 — developer 가 그대로 사용 가능):
     ```powershell
     # PRAFTA_FLUTTER/safenote/scripts/sync-vue-app.ps1
     # Vue (Vite) 빌드 산출물을 Flutter assets/vue_app/ 로 동기화한다.
     # 실행: powershell -ExecutionPolicy Bypass -File .\scripts\sync-vue-app.ps1

     $ErrorActionPreference = 'Stop'

     # === 경로 설정 ===
     $VueSrc = 'C:\PRAFTA\PRAFTA\prafta-app-frontend\prafta-app-frontend'
     $AppAssetsRoot = Join-Path $PSScriptRoot '..\assets\vue_app'

     Write-Host '[1/6] 환경 확인...' -ForegroundColor Cyan
     if (-not (Test-Path $VueSrc)) {
         Write-Host "Vue 소스 경로를 찾을 수 없습니다: $VueSrc" -ForegroundColor Red
         exit 1
     }

     # === cert 파일 존재 확인 ===
     # vite.config.js 가 module 평가 단계에서 cert 를 readFileSync 하므로
     # cert 파일이 없으면 build 명령도 실패한다.
     $certKey = Join-Path $VueSrc 'cert\172.30.1.4-key.pem'
     $certPem = Join-Path $VueSrc 'cert\172.30.1.4.pem'
     if (-not ((Test-Path $certKey) -and (Test-Path $certPem))) {
         Write-Host '' -ForegroundColor Red
         Write-Host 'cert 파일이 없어 Vite 빌드를 실행할 수 없습니다.' -ForegroundColor Red
         Write-Host "필요 파일:" -ForegroundColor Red
         Write-Host "  $certKey" -ForegroundColor Red
         Write-Host "  $certPem" -ForegroundColor Red
         Write-Host '' -ForegroundColor Red
         Write-Host 'mkcert 로 LAN IP 인증서를 발급하거나, vite.config.js 의 https 옵션을' -ForegroundColor Yellow
         Write-Host 'build 시점에 우회하도록 별도 수정이 필요합니다.' -ForegroundColor Yellow
         exit 1
     }

     # === assets/vue_app 디렉토리 보장 ===
     if (-not (Test-Path $AppAssetsRoot)) {
         New-Item -ItemType Directory -Path $AppAssetsRoot | Out-Null
     }

     Write-Host '[2/6] Vue 프로젝트 디렉토리로 이동...' -ForegroundColor Cyan
     Push-Location $VueSrc
     try {
         if (-not (Test-Path 'node_modules')) {
             Write-Host '[3/6] node_modules 미존재 -> npm install --no-audit --no-fund' -ForegroundColor Cyan
             npm install --no-audit --no-fund
         } else {
             Write-Host '[3/6] node_modules 존재 -> install 스킵' -ForegroundColor DarkGray
         }

         Write-Host '[4/6] vite build 실행...' -ForegroundColor Cyan
         npm run build
         if ($LASTEXITCODE -ne 0) {
             Write-Host "vite build 실패 (exit=$LASTEXITCODE)" -ForegroundColor Red
             exit $LASTEXITCODE
         }
     } finally {
         Pop-Location
     }

     Write-Host '[5/6] 기존 assets/vue_app 정리...' -ForegroundColor Cyan
     Get-ChildItem -Path $AppAssetsRoot -Force | ForEach-Object {
         Remove-Item -Path $_.FullName -Recurse -Force
     }

     Write-Host '[6/6] dist -> assets/vue_app 동기화...' -ForegroundColor Cyan
     Copy-Item -Path (Join-Path $VueSrc 'dist\*') -Destination $AppAssetsRoot -Recurse -Force

     Write-Host '' -ForegroundColor Green
     Write-Host '동기화 완료. flutter build apk --release 로 빌드를 실행하세요.' -ForegroundColor Green
     ```
- **결정 사유 (옵션 B 채택)**:
  - 사용자 작업 지시서 §A-2 의 옵션 분석 그대로 옵션 B 채택: vite.config.js 의 `readFileSync` 가 module 평가 시점에 실행되므로 build 명령에도 cert 파일이 필수. 스크립트가 cert 부재 시 친절한 에러를 출력하고 중단. 사용자가 mkcert 로 발급하거나 vite.config.js 를 별도 수정해야 함 — 본 라운드 outside scope.
  - **옵션 A 거부 사유**: vite.config.js 를 임시 수정하는 방식은 원본 손상 위험. CI/CD 에서 동작 불안정.
  - **대안 메모**: 향후 vite.config.js 를 `if (mode === 'production') { https = undefined } else { https = { ... } }` 패턴으로 리팩터링하면 cert 없이도 build 가능. 본 라운드는 그대로 두고 별도 작업으로 분리.
  - Robocopy 가 아닌 `Copy-Item -Recurse -Force` 사용: PowerShell 표준 명령이라 외부 도구 의존 없음. Robocopy 는 종료 코드가 0이 아닐 수 있어 PowerShell `$ErrorActionPreference = 'Stop'` 과 상충 가능성.
  - `npm install --no-audit --no-fund` 사용: CLAUDE.md 의 Bash 명령 규칙 준수 (prompt 회피).
  - `npx --yes` 는 사용 안 함 (vite 는 dev dependency 로 설치되어 `npm run build` 가 로컬 vite 호출).
- **developer 후속 절차**:
  1. `PRAFTA_FLUTTER/safenote/scripts/` 디렉토리 신설.
  2. 위 PowerShell 스크립트를 파일로 저장.
  3. 실행 가이드:
     ```powershell
     cd PRAFTA_FLUTTER\safenote
     powershell -ExecutionPolicy Bypass -File .\scripts\sync-vue-app.ps1
     ```
  4. 스크립트 첫 실행은 사용자가 수동으로 실행하여 cert 파일/Node 환경 확인 권장.

---

## 3. 본 라운드 outside scope (deferred — 메인 세션 22개 리뷰 발견 사항 중 8~13)

| # | 발견 사항 | 사유 / 처리 방식 |
|---|---|---|
| 8 | `MainActivity.onResume` 의 dead code WebView 인스턴스 (`WebView(this).webChromeClient = ...`) | Flutter 임베딩에서는 무의미한 코드(WebView 인스턴스가 어디에도 attach 안 됨). Phase B 의 dead code 정리에 포함. 본 라운드 변경 시 동작 회귀 위험은 없으나 다른 위생 항목과 묶음 처리 권장. |
| 9 | `READ_EXTERNAL_STORAGE` / `RECORD_AUDIO` / `org.apache.http.legacy` 권한·라이브러리 정리 | RECORD_AUDIO 는 mobile_scanner 에서 불필요(QR 스캐너는 카메라만 사용). READ_EXTERNAL_STORAGE 는 Android 13+ READ_MEDIA_IMAGES 로 대체됨. `org.apache.http.legacy` 는 flutter_inappwebview 6.x 에서 미사용. 모두 Phase B 위생 정리. Play Store 정책상 불필요 권한은 리뷰 차단 사유가 될 수 있어 Phase B 우선순위 P0 격상 필요. |
| 10 | pubspec.yaml `fonts/` 누락 | PLNprafta-038006 에서 `- assets/vue_app/` 단일 라인으로 통합 시도. Vite dist 의 폰트 파일이 `assets/Pretendard-*.woff2` 위치에 hashed name 으로 존재하므로 sync 후 검증 필요. 본 라운드에서 해결되거나, 안 되면 §5 deferred 로 라인 추가 작업. |
| 11 | `qr_scan_page.dart` dead code 정리 / JS-bridge 설계 | 본 라운드 변경 없음. mobile_scanner 와 flutter_inappwebview 의 역할 분리는 Phase B. `_scanPickersJS` 의 처리 위치 재검토는 별도 작업. |
| 12 | `enableJetifier=true` / `tools:targetApi="33"` / manifest `package` attr | manifest `package` attr 은 PLNprafta-038001 에서 제거됨. 나머지 enableJetifier·tools:targetApi 는 Phase C 위생 정리. Jetifier 는 AndroidX 마이그레이션 끝난 프로젝트에서 false 로 변경 가능 (빌드 속도 개선). |
| 13 | 운영 백엔드 도메인 결정 | 사용자 결정 "미정 → `--dart-define=APP_BASE_URL=...` hooks". PLNprafta-038005 에서 hook 만 작성. 실제 Vue 측 axios baseURL 적용은 prafta-app-frontend 별도 작업. |

추가 deferred:

- **profile build 검증**: 본 라운드는 release/debug 만 다룸. `flutter build apk --profile` 의 network config / localhost server 동작은 별도 검증.
- **iOS 빌드**: 사용자가 Android APK 만 명시. iOS 는 본 라운드 outside scope.
- **CI/CD 자동화**: GitHub Actions 등에서 keystore + sync 스크립트 + flutter build apk 자동화는 별도 라운드.
- **App Bundle (.aab)**: Play Store 권장 포맷은 .aab 이지만 사용자가 "APK 배포 빌드" 명시. .aab 추가는 별도 작업 (signing 분기 동일 적용 가능).

---

## 4. 사용자 수동 단계 (메인 세션이 사용자에게 안내해야 할 명령)

본 라운드 분해 작업이 완료된 후, developer 가 코드 변경을 적용하고 나면 사용자는 다음 단계를 수동 실행해야 한다:

### 4.1 release keystore 생성 (한 번만)

```powershell
# JDK 의 keytool 사용 (Java 11+ 동봉)
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote\android
keytool -genkey -v -keystore safenote-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias safenote
# 비밀번호와 정보 입력 (잊지 말 것 — 분실 시 앱 업데이트 불가)
```

생성 후 `android/key.properties.example` 을 복사해 `android/key.properties` 로 만들고 storePassword/keyPassword/keyAlias/storeFile 4개 값을 채운다. `.gitignore` 에 의해 자동으로 커밋 제외됨.

### 4.2 launcher icon 생성 (아이콘 원본 변경 시마다)

```powershell
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
flutter pub get
flutter pub run flutter_launcher_icons
```

생성 결과: `android/app/src/main/res/mipmap-*/ic_launcher.png` + `mipmap-anydpi-v26/ic_launcher.xml` (adaptive). 커밋 대상.

### 4.3 Vue 산출물 동기화 (release 빌드 직전마다)

```powershell
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
powershell -ExecutionPolicy Bypass -File .\scripts\sync-vue-app.ps1
```

cert 파일이 `PRAFTA/prafta-app-frontend/prafta-app-frontend/cert/` 에 있어야 빌드 성공.

### 4.4 release APK 빌드

```powershell
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
flutter clean
flutter pub get
flutter build apk --release
# 산출물: build/app/outputs/flutter-apk/app-release.apk
```

`--dart-define=APP_BASE_URL=https://...` 옵션은 운영 백엔드 URL 결정 후 추가.

### 4.5 release APK 검증

```powershell
# applicationId 확인
$env:Path += ";C:\Users\<USER>\AppData\Local\Android\Sdk\build-tools\<VERSION>"
aapt dump badging build\app\outputs\flutter-apk\app-release.apk | Select-String 'package|application-label'
# 결과:
#   package: name='com.prafta.safenote' ...
#   application-label-ko:'세이프노트'
```

실기기 설치 후 런처 아이콘/라벨이 "세이프노트" 로 표시되고, 앱 진입 시 `http://localhost:8080/index.html` 로딩이 동작하는지 확인.

---

## 5. 결정 사유 로그 (요약)

1. **applicationId·namespace 단일화**: AGP 8.x 표준 패턴. flavor 미도입.
2. **앱 라벨 strings.xml 분리**: 한글 i18n 표준 + manifest 하드코딩 회피.
3. **signing fallback to debug**: key.properties 미존재 시 release 빌드 가능하게 (개발 편의). 단 경고 로그 출력.
4. **network config sourceSet 분기 (옵션 B)**: manifestPlaceholders 보다 단순. 표준적.
5. **InAppLocalhostServer + kReleaseMode 분기**: profile 빌드는 debug 처럼 동작 (LAN IP). release 만 localhost.
6. **--dart-define hooks 만 작성, baseURL 미적용**: 사용자 결정 준수. Vue 측 axios 처리는 outside scope.
7. **flutter_launcher_icons 의 fg/bg 동일 원본**: 사용자가 디자인 분리 안 함. 추후 fg 만 교체 가능한 구조 유지.
8. **PowerShell 스크립트 옵션 B (cert 부재 시 중단)**: vite.config.js 원본 손상 회피 + CI 안정성.
9. **assets 라인 단일 통합 시도**: pubspec.yaml 의 vue_app 하위 디렉토리 라인을 1개로. 부족하면 sync 후 추가.
10. **사용자 수동 단계 명시**: keystore / launcher icon / sync 는 사용자가 직접 실행. planner/developer 가 자동화하지 않음.

---

## 6. 산출 파일 목록 (본 라운드 — developer 후속 처리 대상)

| # | 경로 | 신규/변경/삭제 |
|---|---|---|
| 1 | `PRAFTA_FLUTTER/safenote/android/app/build.gradle.kts` | 변경 (namespace, applicationId, Properties import, signingConfigs 블록, buildTypes.release.signingConfig) |
| 2 | `PRAFTA_FLUTTER/safenote/android/app/src/main/AndroidManifest.xml` | 변경 (package attr 제거, android:label 참조 변경, usesCleartextTraffic 제거) |
| 3 | `PRAFTA_FLUTTER/safenote/android/app/src/main/kotlin/com/example/safenote/MainActivity.kt` | 삭제 (이동) |
| 4 | `PRAFTA_FLUTTER/safenote/android/app/src/main/kotlin/com/prafta/safenote/MainActivity.kt` | 신규 (이동 결과, package 라인 변경) |
| 5 | `PRAFTA_FLUTTER/safenote/android/app/src/main/res/values/strings.xml` | 신규 |
| 6 | `PRAFTA_FLUTTER/safenote/android/app/src/release/res/xml/network_security_config.xml` | 신규 (디렉토리 신설) |
| 7 | `PRAFTA_FLUTTER/safenote/android/app/src/main/res/xml/network_security_config.xml` | 변경 (주석 보강만, 동작 변경 없음) |
| 8 | `PRAFTA_FLUTTER/safenote/android/key.properties.example` | 신규 |
| 9 | `PRAFTA_FLUTTER/safenote/.gitignore` | 변경 (Android signing 섹션 추가) |
| 10 | `PRAFTA_FLUTTER/safenote/lib/web_app.dart` | 변경 (kReleaseMode 분기, InAppLocalhostServer, --dart-define hooks) |
| 11 | `PRAFTA_FLUTTER/safenote/pubspec.yaml` | 변경 (flutter_launcher_icons dev dep, launcher 설정, assets 정비) |
| 12 | `PRAFTA_FLUTTER/safenote/assets/icons/app_icon.png` | 신규 (원본 복사) |
| 13 | `PRAFTA_FLUTTER/safenote/assets/icons/app_icon_foreground.png` | 신규 (원본 복사) |
| 14 | `PRAFTA_FLUTTER/safenote/scripts/sync-vue-app.ps1` | 신규 (디렉토리 신설) |

> **planner 가 직접 만들지 않음을 다시 강조**: 본 라운드의 산출물은 위 markdown 1개(`prafta-038-planning.md`) + 작업 stub 1개(`prafta-038.md`) 뿐. 14개 코드 파일 변경은 developer 후속 라운드 담당.

---

## 7. 우선순위 근거 (요약)

- **PLNprafta-038001 (applicationId/namespace/Kotlin 이동)** P0: 다른 모든 Android 작업의 선행. 패키지명이 정해져야 manifest / strings 등 후속 작업 의미 있음.
- **PLNprafta-038002 (앱 라벨)** P0: 사용자 식별 가능한 외관 변경. 빌드 결과의 첫 검증 포인트.
- **PLNprafta-038003 (signing)** P1: 실제 스토어 업로드 직전까지는 debug fallback 으로 대체 가능. 단 사용자가 keystore 를 미리 생성하면 동시 진행 가능.
- **PLNprafta-038004 (release network config)** P1: release 빌드의 보안 기본기. 운영 도메인 결정 전이라 cleartext 차단 + system 인증서만 신뢰의 최소 구성.
- **PLNprafta-038005 (web_app.dart release 분기)** P1: release 빌드가 의미를 가지려면 localhost 분기가 동작해야 함. PLNprafta-038007 의 sync 스크립트와 함께 동작해야 의미 있음.
- **PLNprafta-038006 (launcher icon)** P2: 빌드 가능 여부와 무관하나 마켓 노출 품질의 기본기. 빌드 통과 후 아이콘만 별도 적용 가능.
- **PLNprafta-038007 (sync 스크립트)** P1: release 빌드 직전 매번 실행. assets/vue_app 의 stale 상태 해결.

---

## 8. 본 라운드 종료 후 메인 세션이 할 일 (Notion)

- 메인 세션이 `PLNprafta-038001~007` 7건을 Notion "작업 로그" DB 에 일괄 등록.
- 본 markdown 의 "각 작업 상세" 섹션을 그대로 "상세 설명" 컬럼에 복사.
- 산출 파일 경로(§6)를 "산출물" 컬럼에 기록.
- 상태=분해완료, 담당 에이전트=planner. 영역=app (Flutter 셸).
- §3 deferred 항목은 본 라운드에서 작업 ID 채번하지 않음 (Phase B/C 분해 시 별도 채번).
- §4 사용자 수동 단계를 사용자에게 그대로 안내.
