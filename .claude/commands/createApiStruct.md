\---

description: 신규 REST API 구조 자동 생성 (Controller / Service / ServiceImpl / Mapper / Mapper XML)

argument-hint: <web|app> <module-name>

\---



\# ⚠️ 엄격 모드 (최우선 규칙)



이 명령어는 \*\*지정된 작업만\*\* 수행한다. 다음 규칙을 절대 위반하지 않는다.



1\. \*\*이 문서에 명시된 디렉토리와 파일만 생성한다.\*\* 그 외 어떤 파일도 만들지 않는다.

2\. \*\*이 문서에 명시된 템플릿 코드만 작성한다.\*\* 템플릿에 없는 내용은 한 줄도 추가하지 않는다.

&#x20;  - CRUD 메서드를 추가로 구현하지 않는다.

&#x20;  - DTO, Request, Response, Result 클래스를 미리 생성하지 않는다. (디렉토리만 만든다)

&#x20;  - 샘플 메서드, 예시 코드, TODO 주석을 넣지 않는다.

&#x20;  - Javadoc이나 설명 주석을 임의로 추가하지 않는다.

3\. \*\*기존 파일을 수정하지 않는다.\*\* `application.yml`, `pom.xml`, `build.gradle`, `CLAUDE.md` 등 어떤 기존 파일도 건드리지 않는다.

4\. \*\*실행 전 계획이나 설명을 길게 출력하지 않는다.\*\* 2단계의 `<submodule>` 값 출력과 4단계 완료 후 보고만 출력한다.

5\. \*\*추가 제안을 하지 않는다.\*\* "이것도 만들까요?", "CRUD를 추가할까요?" 같은 후속 제안을 하지 않는다.

6\. \*\*디렉토리가 비어있어도 `.gitkeep` 같은 더미 파일을 만들지 않는다.\*\*



위 규칙과 아래 본문이 충돌할 경우, 위 규칙이 우선한다.



\---



\# 명령어 목적



`src/main/java/com/prafta/$1/$2/` 경로 하위에 REST API 표준 개발 구조를 생성한다.

디렉토리 뼈대와 함께 Controller, Service, ServiceImpl, Mapper, Mapper XML \*\*5개 파일만\*\* 초기 템플릿으로 자동 생성한다.



\# 파라미터



\- `$1`: `web` 또는 `app` (대상 영역)

\- `$2`: 모듈명 (소문자, 예: `attd`, `member`)



\# 네이밍 규칙 (공통)



\- \*\*베이스 경로(Java)\*\*: `src/main/java/com/prafta/$1/$2`

\- \*\*베이스 경로(Resources)\*\*: `src/main/resources/com/prafta/$1/$2`

\- \*\*서브모듈명\*\*: `$2` 뒤에 2자리 순번이 붙은 형태 (예: `attd01`, `attd02`)

&#x20; - 이 문서에서는 이 값을 \*\*`<submodule>`\*\* 로 표기한다.

&#x20; - 순번은 `01`부터 시작하며 1씩 증가한다. (zero-padding 2자리 유지)

\- \*\*클래스명 접두어\*\*: `<submodule>`의 첫 글자를 대문자로 치환한 값

&#x20; - 이 문서에서는 이 값을 \*\*`<Submodule>`\*\* 로 표기한다. (예: `Attd01`)



\# 실행 순서



\## 1단계: 베이스 경로 확인 및 생성



\- `src/main/java/com/prafta/$1/$2/` 경로가 없으면 먼저 생성한다.



\## 2단계: 서브모듈 번호 결정



\- `src/main/java/com/prafta/$1/$2/` 하위를 조회하여 기존 `$2NN` 형태의 디렉토리를 확인한다.

\- 기존 디렉토리가 없으면: `<submodule>` = `$2` + `01`

\- 기존 디렉토리가 있으면: 기존 번호 중 최댓값 + 1 (2자리 zero-padding, 예: `07` → `08`)

\- 결정된 `<submodule>` 값을 한 줄로만 출력한다. (예: `Target submodule: attd07`)



\## 3단계: 디렉토리 구조 생성



\### 3-1. Java 소스 디렉토리



`src/main/java/com/prafta/$1/$2/<submodule>/` 하위에 \*\*아래 디렉토리만\*\* 생성한다. (파일은 생성하지 않는다)



```

<submodule>/

├── application/

│   ├── command/

│   ├── model/

│   ├── param/

│   └── query/

├── controller/

│   └── dto/

│       ├── request/

│       └── response/

├── mapper/

│   └── result/

└── service/

&#x20;   └── impl/

```



\### 3-2. Resources 디렉토리



`src/main/resources/com/prafta/$1/$2/<submodule>/mapper/` 디렉토리를 생성한다.



\## 4단계: 파일 생성 (정확히 5개)



아래 5개 파일을 템플릿 \*\*그대로\*\* 생성한다.

\*\*템플릿에 없는 코드, 주석, import, 어노테이션을 추가하지 않는다.\*\*



> \*\*치환 규칙\*\*

> - `$1`, `$2` → 명령어 인자 그대로

> - `<submodule>` → 2단계에서 결정된 값 (예: `attd01`)

> - `<Submodule>` → `<submodule>`의 첫 글자를 대문자로 (예: `Attd01`)



\### 4-1. Controller



\- \*\*경로\*\*: `src/main/java/com/prafta/$1/$2/<submodule>/controller/<Submodule>Controller.java`



```java

package com.prafta.$1.$2.<submodule>.controller;



import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



import com.prafta.common.security.JwtUtil;

import com.prafta.$1.$2.<submodule>.service.<Submodule>Service;



import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;



@Slf4j

@RestController

@RequestMapping("/<submodule>")

@RequiredArgsConstructor

public class <Submodule>Controller {



&#x20;   private final <Submodule>Service <submodule>Service;

&#x20;   private final JwtUtil jwtUtil;

}

```



\### 4-2. Service (Interface)



\- \*\*경로\*\*: `src/main/java/com/prafta/$1/$2/<submodule>/service/<Submodule>Service.java`



```java

package com.prafta.$1.$2.<submodule>.service;



public interface <Submodule>Service {



}

```



\### 4-3. ServiceImpl



\- \*\*경로\*\*: `src/main/java/com/prafta/$1/$2/<submodule>/service/impl/<Submodule>ServiceImpl.java`



```java

package com.prafta.$1.$2.<submodule>.service.impl;



import org.springframework.stereotype.Service;



import com.prafta.$1.$2.<submodule>.mapper.<Submodule>Mapper;

import com.prafta.$1.$2.<submodule>.service.<Submodule>Service;



import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;



@Slf4j

@Service

@RequiredArgsConstructor

public class <Submodule>ServiceImpl implements <Submodule>Service {



&#x20;   private final <Submodule>Mapper <submodule>Mapper;

}

```



\### 4-4. Mapper (Interface)



\- \*\*경로\*\*: `src/main/java/com/prafta/$1/$2/<submodule>/mapper/<Submodule>Mapper.java`



```java

package com.prafta.$1.$2.<submodule>.mapper;



import org.apache.ibatis.annotations.Mapper;



@Mapper

public interface <Submodule>Mapper {



}

```



\### 4-5. Mapper XML



\- \*\*경로\*\*: `src/main/resources/com/prafta/$1/$2/<submodule>/mapper/<Submodule>Mapper.xml`



```xml

<?xml version="1.0" encoding="UTF-8" ?>

<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"

&#x20;   "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.prafta.$1.$2.<submodule>.mapper.<Submodule>Mapper">



</mapper>

```



\# 완료 후 보고



아래 형식으로 \*\*간단하게만\*\* 출력한다. 추가 설명이나 제안을 덧붙이지 않는다.



```

✅ Done: <submodule>



Created files:

\- <Controller 경로>

\- <Service 경로>

\- <ServiceImpl 경로>

\- <Mapper 경로>

\- <Mapper XML 경로>

```



\# ⚠️ 최종 확인 (응답 전 반드시 점검)



응답을 출력하기 직전에 다음을 확인한다.



\- \[ ] 생성한 Java 파일이 정확히 4개인가? (Controller, Service, ServiceImpl, Mapper)

\- \[ ] 생성한 XML 파일이 정확히 1개인가? (Mapper XML)

\- \[ ] 각 파일의 내용이 템플릿과 \*\*한 글자도 다르지 않은가?\*\* (import, 어노테이션, 주석 포함)

\- \[ ] 기존 파일을 수정하지 않았는가?

\- \[ ] 후속 제안이나 추가 설명을 덧붙이지 않았는가?



하나라도 아니오이면 수정한다.

