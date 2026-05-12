---
description: 기존 모듈에 REST API 엔드포인트 추가 (GET/POST, 전체 레이어 파일 자동 생성)
argument-hint: <submodule> <method> <endpoint> [field1, field2, ...]  [--list] [--token-only] [--nested] [--flatten-list]
---

# ⚠️ 엄격 모드 (최우선 규칙)

1. **이 문서에 명시된 파일과 코드만 생성한다.** 템플릿에 없는 import, 주석, 메서드를 추가하지 않는다.
2. **dev-facing 문자열(에러 메시지, 변수명 등)은 반드시 영어로 작성한다.**
3. **기존 파일에 추가할 때는 해당 파일을 먼저 읽은 뒤 Edit 도구로만 수정한다.**
4. **실행 전 긴 설명을 출력하지 않는다.** 완료 후 생성/수정된 파일 목록만 출력한다.
5. **추가 제안을 하지 않는다.**

---

# 명령어 구문

```
/createApi {submodule} {method} {endpoint} [{field1, field2, ...}] [--list] [--token-only] [--nested] [--flatten-list]
```

| 인자 | 설명 | 예시 |
|------|------|------|
| `{submodule}` | 대상 서브모듈 | `attd07` |
| `{method}` | HTTP 메서드 | `get` \| `post` |
| `{endpoint}` | kebab-case 엔드포인트명 | `attd-lists`, `save-attd-info`, `delete-attd-info` |
| `[fields]` | 요청 파라미터 필드 목록 (생략 가능) | `[siteCd, nodeCd, userId]` |
| `--list` | POST: `List<Request>` + Model 레이어 사용 (attd05 saveUserWorkPlans 패턴) | |
| `--token-only` | GET: Request 없이 토큰만 사용 (attd05 getLeaveTypeList 패턴) | |
| `--nested` | POST: Request 내부에 복수의 중첩 객체(단건/리스트)가 있는 패턴 (attd01 updateShiftSchInfo 방식) | |
| `--flatten-list` | `--nested` 하위 primitive 리스트를 항목 단위로 평탄화하여 Command 생성 | |

## --nested sub-object 선언

`--nested` 사용 시 fields 자리에 중첩 객체를 아래 형식으로 선언한다. `|` 로 구분:
```
{SubName}:{f1,f2}           ← 단건 inner object
List<{SubName}>:{f1,f2}     ← 리스트 inner object
```
예시:
```
/createApi attd01 post update-shift-sch-infos --nested [ShiftType:shiftNo,siteCd | List<ShiftPattern>:siteCd,ptrnIdx | List<ShiftTeam>:siteCd,teamIdx | List<ShiftAssign>:siteCd,dayNo]
```

## --nested 깊은 중첩 (nested-in-nested)

sub-object 내부에 또 다른 리스트 필드가 있을 때 확장 형식을 사용한다:
```
{SubName}:{f1,f2}                                    ← 단건 inner object
List<{SubName}>:{f1,f2,List<String>:{fname}}         ← 리스트 inner object + primitive 리스트 필드
List<{SubName}>:{f1,f2,List<{InnerName}>:{g1,g2}}    ← 리스트 inner object + 객체 리스트 필드
```

규칙:
- 필드 목록 내부에 `List<Type>:{...}` 가 등장하면 해당 필드는 리스트 타입으로 해석한다.
- `List<String>` / `List<Integer>` / `List<Long>` 등 primitive wrapper 리스트는 **inner record 없이** `List<String>` 같은 필드 타입으로만 생성한다 (Command/Model/Param 공통).
- `List<{InnerName}>:{...}` 처럼 대문자로 시작하는 타입명이 오면 별도 nested record (`{InnerName}Param`) 를 sub-object Param 내부에 생성한다.

예시 (리스트 항목 내부에 primitive 리스트를 가지는 구조):
```
/createApi attd01 post update-shift-sch-infos --nested [ShiftMeta:{shiftTypeCd,siteCd,startDate,endDate} | List<Team>:{teamIdx,teamNm,leaderId,List<String>:{memberIds}}]
```

위 명령은 아래 JSON 구조를 그대로 매핑한다:
```
{
  shiftMeta: { shiftTypeCd, siteCd, startDate, endDate },
  teamList: [
    { teamIdx, teamNm, leaderId, memberIds: ["..."] },
    ...
  ]
}
```

## 타입 매핑 규칙 (primitive / 스칼라 타입)

sub-object 필드 선언 시 타입을 명시하지 않으면 `String` 으로 간주한다.
명시하려면 `{fieldName}:{type}` 형식을 사용한다:

| 선언 | Java 타입 |
|------|-----------|
| `field` | `String` (기본) |
| `field:String` | `String` |
| `field:Integer` | `Integer` |
| `field:Long` | `Long` |
| `field:Boolean` | `Boolean` |
| `field:LocalDate` | `LocalDate` (`java.time.LocalDate` import) |
| `List<String>:{field}` | `List<String> field` |
| `List<Integer>:{field}` | `List<Integer> field` |

필요한 import 는 해당 타입이 처음 등장하는 파일에 자동 추가한다.

---

# 패키지 경로 도출 규칙

`{submodule}` 에서 숫자 앞의 알파벳 접두어를 추출하여 경로를 결정한다.

- `attd07` → 접두어 `attd` → 패키지 `com.prafta.web.attd.attd07`
- `user02` → 접두어 `user` → 패키지 `com.prafta.web.user.user02`
- Java 소스: `prafta-backend/src/main/java/com/prafta/web/{prefix}/{submodule}/`
- Resources: `prafta-backend/src/main/resources/com/prafta/web/{prefix}/{submodule}/`

이 문서에서 패키지 루트를 `{pkg}` 로 표기한다. (예: `com.prafta.web.attd.attd07`)

---

# 네이밍 변환 규칙

endpoint `{endpoint}` (kebab-case) 로부터 다음을 도출한다.

| 형태 | 변환 규칙 | 예시 (`attd-lists`) | 예시 (`save-attd-info`) |
|------|----------|---------------------|-------------------------|
| `{Pascal}` | 각 단어 첫 글자 대문자 | `AttdLists` | `SaveAttdInfo` |
| `{camel}` | 첫 단어 소문자, 이후 대문자 | `attdLists` | `saveAttdInfo` |
| `{Submodule}` | submodule 첫 글자 대문자 | `Attd07` | `Attd07` |
| `{submodule}` | submodule 소문자 그대로 | `attd07` | `attd07` |

---

# 메서드명 규칙

| 위치 | GET | POST (일반) | POST (--list) |
|------|-----|-------------|---------------|
| Controller 메서드 | `get{Pascal}` | `{camel}` | `{camel}` |
| Service 메서드 | `get{Pascal}` | `{camel}` | `{camel}` |
| Mapper 메서드 | `select{Pascal}` | `{camel}` | `{camel}` |
| XML id | `select{Pascal}` | `{camel}` | `{camel}` |

endpoint 가 `delete-` 로 시작하면 XML 태그는 `<delete>` 사용, 나머지 POST 는 `<update>` 사용.

---

# TokenInfo 필드 규칙

Param 레코드에 TokenInfo 에서 가져오는 필드는 HTTP 메서드에 따라 고정한다.

| HTTP 메서드 | TokenInfo 에서 추출하는 필드 | Param 레코드 끝에 추가 |
|-------------|-----------------------------|-----------------------|
| GET | `gvCmpnyCd` 만 | `, String gvCmpnyCd` |
| POST | `gvCmpnyCd` + `gvUserCd` | `, String gvCmpnyCd` `, String gvUserCd` |

- GET Param.from() 마지막: `, tokenInfo.gv_cmpnyCd()`
- POST Param.from() 마지막: `, tokenInfo.gv_cmpnyCd()` `, tokenInfo.gv_userCd()`
- Command / Model 에는 항상 `gvCmpnyCd`, `gvUserCd` 둘 다 포함한다.
- Query 에는 `gvCmpnyCd` 만 포함한다.

---

# 생성/수정 파일 목록

## GET (기본 패턴 — fields 있음)

**신규 생성:**
```
dto/request/{Pascal}Request.java
dto/response/{Pascal}Response.java
result/{Pascal}Result.java
application/param/{Pascal}Param.java
application/query/{Pascal}Query.java
```

**기존 파일에 추가 (없으면 생성):**
```
controller/{Submodule}Controller.java   ← @GetMapping 메서드 추가
service/{Submodule}Service.java         ← 메서드 시그니처 추가
service/impl/{Submodule}ServiceImpl.java ← @Override 구현 추가
mapper/{Submodule}Mapper.java           ← select 메서드 추가
resources/.../mapper/{Submodule}Mapper.xml ← <select> 블록 추가
```

## GET (--token-only — fields 없음)

Request 클래스 생략. Param.from(tokenInfo) 만 사용.

**신규 생성:**
```
dto/response/{Pascal}Response.java
result/{Pascal}Result.java
application/param/{Pascal}Param.java    ← from(TokenInfo) 만 존재
application/query/{Pascal}Query.java
```

나머지 기존 파일 수정은 동일.

## POST (기본 단건 패턴)

**신규 생성:**
```
dto/request/{Pascal}Request.java
application/param/{Pascal}Param.java
application/command/{Pascal}Command.java
```

**기존 파일에 추가:**
```
controller/{Submodule}Controller.java   ← @PostMapping 메서드 추가
service/{Submodule}Service.java         ← void 메서드 시그니처 추가
service/impl/{Submodule}ServiceImpl.java ← @Override + @Transactional 구현 추가
mapper/{Submodule}Mapper.java           ← save/delete 메서드 추가
resources/.../mapper/{Submodule}Mapper.xml ← <update> 또는 <delete> 블록 추가
```

## POST (--list 패턴 — attd05 saveUserWorkPlans 방식)

**신규 생성:**
```
dto/request/{Pascal}Request.java
application/model/{Pascal}Model.java
application/param/{Pascal}Param.java    ← List<{Pascal}Model>, stream().map() 변환
application/command/{Pascal}Command.java ← from({Pascal}Model)
```

ServiceImpl 에서 `for({Pascal}Model model : param.{camel}ModelList())` 루프 사용.

## POST (--nested 패턴 — attd01 updateShiftSchInfo 방식)

**신규 생성:**
```
dto/request/{Pascal}Request.java              ← static inner class 다수 포함
application/param/{Pascal}Param.java          ← nested record 다수 포함 (from + fromList)
application/command/{SubObj}Command.java      ← sub-object 마다 하나씩 생성
```

단건 sub-object Command: `from({Pascal}Param param)` — parent Param 전체를 받아 내부 nested record 접근
리스트 sub-object Command: `from({Pascal}Param.{SubObj}Param param, String gvCmpnyCd, String gvUserCd)` — nested Param + token 필드 개별 수신
ServiceImpl 의 비즈니스 로직은 `// TODO` 로 남기고 Mapper 호출 패턴만 주석으로 표시한다.

## POST (--nested + 깊은 중첩 / primitive 리스트)

sub-object 내부에 primitive 리스트 필드(`List<String>` 등)가 포함될 경우:

- Request inner class 에 `List<String> {listField}` 필드 추가
- Param nested record 에 동일한 `List<String> {listField}` 필드 추가 (별도 record 생성 안 함)
- Command 는 **리스트 보존형** 이 기본이며, `--flatten-list` 플래그가 있으면 **평탄화형** 추가 생성

---

# 파일 템플릿

## Request (GET / POST 공통)

```java
package {pkg}.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class {Pascal}Request {
    private String {field1};
    private String {field2};
    // ... fields 목록 그대로
}
```

POST 에서 필수 필드가 있으면 `@FieldLabel("field name") @NotBlank` 추가.

## Response (GET)

```java
package {pkg}.dto.response;

import java.util.List;
import {pkg}.result.{Pascal}Result;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class {Pascal}Response {
    List<{Pascal}Result> {camel}ResultList;
}
```

## Result (GET)

```java
package {pkg}.result;

public record {Pascal}Result(
    // TODO: DB 컬럼에 맞게 필드 추가
) {
}
```

## Param (GET — fields 있음)

```java
package {pkg}.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.dto.request.{Pascal}Request;

public record {Pascal}Param(
    String {field1}
    , String {field2}
    // ... fields
    , String gvCmpnyCd
) {
    public static {Pascal}Param from({Pascal}Request request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Request");

        return new {Pascal}Param(
            request.get{Field1}()
            , request.get{Field2}()
            // ...
            , tokenInfo.gv_cmpnyCd()
        );
    }
}
```

## Param (GET — --token-only)

```java
package {pkg}.application.param;

import com.prafta.common.dto.TokenInfo;

public record {Pascal}Param(
    String gvCmpnyCd
) {
    public static {Pascal}Param from(TokenInfo tokenInfo) {
        return new {Pascal}Param(tokenInfo.gv_cmpnyCd());
    }
}
```

## Param (POST — 단건)

```java
package {pkg}.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.dto.request.{Pascal}Request;

public record {Pascal}Param(
    String {field1}
    , String {field2}
    // ...
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {Pascal}Param from({Pascal}Request request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Request");

        return new {Pascal}Param(
            request.get{Field1}()
            , request.get{Field2}()
            // ...
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
```

## Param (POST — --list)

```java
package {pkg}.application.param;

import java.util.List;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.model.{Pascal}Model;
import {pkg}.dto.request.{Pascal}Request;

public record {Pascal}Param(
    List<{Pascal}Model> {camel}ModelList
) {
    public static {Pascal}Param from(List<{Pascal}Request> requests, TokenInfo tokenInfo) {

        if (requests == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Request");

        List<{Pascal}Model> models = requests.stream()
            .map(req -> new {Pascal}Model(
                req.get{Field1}()
                , req.get{Field2}()
                // ...
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_userCd()
            ))
            .toList();

        return new {Pascal}Param(models);
    }
}
```

## Model (POST — --list)

```java
package {pkg}.application.model;

public record {Pascal}Model(
    String {field1}
    , String {field2}
    // ...
    , String gvCmpnyCd
    , String gvUserCd
) {
}
```

## Query (GET)

```java
package {pkg}.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param;

public record {Pascal}Query(
    String {field1}
    , String {field2}
    // ...
    , String gvCmpnyCd
) {
    public static {Pascal}Query from({Pascal}Param param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Param");

        return new {Pascal}Query(
            param.{field1}()
            , param.{field2}()
            // ...
            , param.gvCmpnyCd()
        );
    }
}
```

## Command (POST — 단건)

```java
package {pkg}.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param;

public record {Pascal}Command(
    String {field1}
    , String {field2}
    // ...
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {Pascal}Command from({Pascal}Param param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Param");

        return new {Pascal}Command(
            param.{field1}()
            , param.{field2}()
            // ...
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
```

## Command (POST — --list)

```java
package {pkg}.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.model.{Pascal}Model;

public record {Pascal}Command(
    String {field1}
    , String {field2}
    // ...
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {Pascal}Command from({Pascal}Model model) {

        if (model == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Model");

        return new {Pascal}Command(
            model.{field1}()
            , model.{field2}()
            // ...
            , model.gvCmpnyCd()
            , model.gvUserCd()
        );
    }
}
```

## Request (POST — --nested)

```java
package {pkg}.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class {Pascal}Request {
    private {SubObj1} {subObj1};                   // 단건 inner object
    private List<{SubObj2}> {subObj2}List;          // 리스트 inner object

    @Getter
    @Setter
    @NoArgsConstructor
    public static class {SubObj1} {
        private String {field1};
        private String {field2};
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class {SubObj2} {
        private String {field1};
        private String {field2};
    }
}
```

## Request (POST — --nested + primitive 리스트)

sub-object 내부에 primitive 리스트 필드가 있을 때:

```java
package {pkg}.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class {Pascal}Request {
    private {SubObj1} {subObj1};
    private List<{SubObj2}> {subObj2}List;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class {SubObj1} {
        private String {field1};
        private String {field2};
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class {SubObj2} {
        private String {field1};
        private String {field2};
        private List<String> {listField};   // primitive 리스트는 List<String> 그대로
    }
}
```

## Param (POST — --nested)

```java
package {pkg}.application.param;

import java.util.List;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.dto.request.{Pascal}Request;

public record {Pascal}Param(
    {SubObj1}Param {subObj1}
    , List<{SubObj2}Param> {subObj2}List
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {Pascal}Param from({Pascal}Request request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Request");

        return new {Pascal}Param(
            {SubObj1}Param.from(request.get{SubObj1}())
            , {SubObj2}Param.fromList(request.get{SubObj2}List())
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }

    public record {SubObj1}Param(
        String {field1}
        , String {field2}
    ) {
        public static {SubObj1}Param from({Pascal}Request.{SubObj1} obj) {
            if (obj == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj1}");
            return new {SubObj1}Param(
                obj.get{Field1}()
                , obj.get{Field2}()
            );
        }
    }

    public record {SubObj2}Param(
        String {field1}
        , String {field2}
    ) {
        public static {SubObj2}Param from({Pascal}Request.{SubObj2} obj) {
            if (obj == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj2}");
            return new {SubObj2}Param(
                obj.get{Field1}()
                , obj.get{Field2}()
            );
        }

        public static List<{SubObj2}Param> fromList(List<{Pascal}Request.{SubObj2}> list) {
            if (list == null) return null;
            return list.stream().map({SubObj2}Param::from).toList();
        }
    }
}
```

## Param (POST — --nested + primitive 리스트)

sub-object Param 안에 primitive 리스트 필드를 그대로 포함한다. 별도 inner record 를 만들지 않는다.

```java
package {pkg}.application.param;

import java.util.List;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.dto.request.{Pascal}Request;

public record {Pascal}Param(
    {SubObj1}Param {subObj1}
    , List<{SubObj2}Param> {subObj2}List
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {Pascal}Param from({Pascal}Request request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Request");

        return new {Pascal}Param(
            {SubObj1}Param.from(request.get{SubObj1}())
            , {SubObj2}Param.fromList(request.get{SubObj2}List())
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }

    public record {SubObj1}Param(
        String {field1}
        , String {field2}
    ) {
        public static {SubObj1}Param from({Pascal}Request.{SubObj1} obj) {
            if (obj == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj1}");
            return new {SubObj1}Param(
                obj.get{Field1}()
                , obj.get{Field2}()
            );
        }
    }

    public record {SubObj2}Param(
        String {field1}
        , String {field2}
        , List<String> {listField}
    ) {
        public static {SubObj2}Param from({Pascal}Request.{SubObj2} obj) {
            if (obj == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj2}");
            return new {SubObj2}Param(
                obj.get{Field1}()
                , obj.get{Field2}()
                , obj.get{ListField}()
            );
        }

        public static List<{SubObj2}Param> fromList(List<{Pascal}Request.{SubObj2}> list) {
            if (list == null) return null;
            return list.stream().map({SubObj2}Param::from).toList();
        }
    }
}
```

## Command (POST — --nested, 단건 sub-object)

`from()` 은 parent Param 전체를 받아 내부 nested record 를 직접 접근한다.

```java
package {pkg}.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param;

public record {SubObj1}Command(
    String {field1}
    , String {field2}
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {SubObj1}Command from({Pascal}Param param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {Pascal}Param");

        return new {SubObj1}Command(
            param.{subObj1}().{field1}()
            , param.{subObj1}().{field2}()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
```

## Command (POST — --nested, 리스트 sub-object)

`from()` 은 nested Param + `gvCmpnyCd` + `gvUserCd` 를 개별 인자로 받는다.

```java
package {pkg}.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param.{SubObj2}Param;

public record {SubObj2}Command(
    String {field1}
    , String {field2}
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {SubObj2}Command from({SubObj2}Param param, String gvCmpnyCd, String gvUserCd) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj2}Param");

        return new {SubObj2}Command(
            param.{field1}()
            , param.{field2}()
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
```

## Command (POST — --nested + primitive 리스트 / 리스트 보존형 — 기본)

리스트 sub-object 가 primitive 리스트 필드를 가질 때 기본 생성 형태.
Command 필드에 `List<String>` 을 그대로 보존하며, XML 에서 `<foreach>` 로 처리한다.

```java
package {pkg}.application.command;

import java.util.List;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param.{SubObj2}Param;

public record {SubObj2}Command(
    String {field1}
    , String {field2}
    , List<String> {listField}
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {SubObj2}Command from({SubObj2}Param param, String gvCmpnyCd, String gvUserCd) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj2}Param");

        return new {SubObj2}Command(
            param.{field1}()
            , param.{field2}()
            , param.{listField}()
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
```

## Command (POST — --nested + primitive 리스트 / 평탄화형 — --flatten-list)

ServiceImpl 에서 `for (String item : param.{listField}())` 루프로 건건이 INSERT 할 때 사용.
Command 필드에는 primitive 리스트 대신 단일 값 필드를 둔다.

```java
package {pkg}.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import {pkg}.application.param.{Pascal}Param.{SubObj2}Param;

public record {SubObj2}ItemCommand(
    String {field1}
    , String {field2}
    , String {itemField}          // 원래 List<String> 이었던 항목 1개
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static {SubObj2}ItemCommand from({SubObj2}Param param, String itemValue, String gvCmpnyCd, String gvUserCd) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - {SubObj2}Param");

        return new {SubObj2}ItemCommand(
            param.{field1}()
            , param.{field2}()
            , itemValue
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
```

**리스트 보존형 vs 평탄화형 선택 기준:**
- MyBatis `<foreach collection="{listField}">` 로 한 번에 처리 → **리스트 보존형** 만 생성 (기본)
- ServiceImpl 에서 Java 루프로 건건이 Mapper 호출 → **평탄화형** 만 생성 (`--flatten-list` 플래그)

---

# Controller 추가 패턴

## GET (fields 있음)

```java
@GetMapping("/{endpoint}")
public ResponseEntity<?> get{Pascal}(
        @ModelAttribute {Pascal}Request request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    {Pascal}Response response = {submodule}Service.get{Pascal}(
            {Pascal}Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    return ResponseEntity.status(HttpStatus.OK).body(response);
}
```

## GET (--token-only)

```java
@GetMapping("/{endpoint}")
public ResponseEntity<?> get{Pascal}(
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    {Pascal}Response response = {submodule}Service.get{Pascal}(
            {Pascal}Param.from(jwtUtil.getAllClaimsAsMap(authorization)));

    return ResponseEntity.status(HttpStatus.OK).body(response);
}
```

## POST (단건)

```java
@PostMapping("/{endpoint}")
public ResponseEntity<?> {camel}(
        @Valid @RequestBody {Pascal}Request request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    {submodule}Service.{camel}(
            {Pascal}Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    return ResponseEntity.status(HttpStatus.OK).build();
}
```

## POST (--list)

```java
@PostMapping("/{endpoint}")
public ResponseEntity<?> {camel}(
        @RequestBody List<{Pascal}Request> request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    {submodule}Service.{camel}(
            {Pascal}Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    return ResponseEntity.status(HttpStatus.OK).build();
}
```

## POST (--nested)

단건 POST와 동일. `@Valid` 없이 `@RequestBody` 사용.

```java
@PostMapping("/{endpoint}")
public ResponseEntity<?> {camel}(
        @RequestBody {Pascal}Request request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    {submodule}Service.{camel}(
            {Pascal}Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    return ResponseEntity.status(HttpStatus.OK).build();
}
```

---

# Service 추가 패턴

## GET

```java
// Service interface
{Pascal}Response get{Pascal}({Pascal}Param param);

// ServiceImpl
@Override
public {Pascal}Response get{Pascal}({Pascal}Param param) {

    List<{Pascal}Result> {camel}ResultList = {submodule}Mapper.select{Pascal}({Pascal}Query.from(param));

    if ({camel}ResultList == null || {camel}ResultList.isEmpty()) {
        return null;
    }

    return {Pascal}Response.builder()
            .{camel}ResultList({camel}ResultList)
            .build();
}
```

## POST (단건)

```java
// Service interface
void {camel}({Pascal}Param param);

// ServiceImpl
@Override
@Transactional
public void {camel}({Pascal}Param param) {
    {submodule}Mapper.{camel}({Pascal}Command.from(param));
}
```

## POST (--list)

```java
// Service interface
void {camel}({Pascal}Param param);

// ServiceImpl
@Override
@Transactional
public void {camel}({Pascal}Param param) {
    for ({Pascal}Model model : param.{camel}ModelList()) {
        {submodule}Mapper.{camel}({Pascal}Command.from(model));
    }
}
```

## POST (--nested)

ServiceImpl 의 비즈니스 로직은 `// TODO` 로 남긴다. Mapper 호출 패턴을 주석으로 표시.

```java
// Service interface
void {camel}({Pascal}Param param);

// ServiceImpl
@Override
@Transactional
public void {camel}({Pascal}Param param) {
    // TODO: 비즈니스 로직 구현
    // 단건: {submodule}Mapper.{mapper1}({SubObj1}Command.from(param));
    // 리스트: for ({SubObj2}Param obj : param.{subObj2}List()) { {submodule}Mapper.{mapper2}({SubObj2}Command.from(obj, param.gvCmpnyCd(), param.gvUserCd())); }
}
```

## POST (--nested + primitive 리스트 / 리스트 보존형 — 기본)

```java
@Override
@Transactional
public void {camel}({Pascal}Param param) {
    // TODO: 비즈니스 로직 구현
    // 단건: {submodule}Mapper.{mapper1}({SubObj1}Command.from(param));
    // 리스트(보존형 — XML <foreach> 로 처리):
    // for ({SubObj2}Param obj : param.{subObj2}List()) {
    //     {submodule}Mapper.{mapper2}({SubObj2}Command.from(obj, param.gvCmpnyCd(), param.gvUserCd()));
    // }
}
```

## POST (--nested + primitive 리스트 / 평탄화형 — --flatten-list)

```java
@Override
@Transactional
public void {camel}({Pascal}Param param) {
    // TODO: 비즈니스 로직 구현
    // 단건: {submodule}Mapper.{mapper1}({SubObj1}Command.from(param));
    // 리스트(평탄화):
    // for ({SubObj2}Param obj : param.{subObj2}List()) {
    //     for (String item : obj.{listField}()) {
    //         {submodule}Mapper.{mapper2Item}({SubObj2}ItemCommand.from(obj, item, param.gvCmpnyCd(), param.gvUserCd()));
    //     }
    // }
}
```

---

# Mapper 추가 패턴

```java
// GET
List<{Pascal}Result> select{Pascal}({Pascal}Query query);

// POST (단건 / --list 동일)
void {camel}({Pascal}Command command);

// POST (--nested) — sub-object 마다 메서드 추가
void {mapper1}({SubObj1}Command command);
void {mapper2}({SubObj2}Command command);

// POST (--nested + 평탄화형) — primitive 리스트 sub-object 는 Item Command 사용
void {mapper2Item}({SubObj2}ItemCommand command);
```

---

# Mapper XML 추가 패턴

## GET

```xml
<select id="select{Pascal}" parameterType="{pkg}.application.query.{Pascal}Query" resultType="{pkg}.result.{Pascal}Result">
/* {Submodule}Mapper.select{Pascal} */
</select>
```

## POST (save)

```xml
<update id="{camel}" parameterType="{pkg}.application.command.{Pascal}Command">
/* {Submodule}Mapper.{camel} */
</update>
```

## POST (delete — endpoint가 `delete-`로 시작할 때)

```xml
<delete id="{camel}" parameterType="{pkg}.application.command.{Pascal}Command">
/* {Submodule}Mapper.{camel} */
</delete>
```

## POST (--nested) — sub-object 마다 블록 추가

```xml
<update id="{mapper1}" parameterType="{pkg}.application.command.{SubObj1}Command">
/* {Submodule}Mapper.{mapper1} */
</update>

<update id="{mapper2}" parameterType="{pkg}.application.command.{SubObj2}Command">
/* {Submodule}Mapper.{mapper2} */
</update>
```

## POST (--nested + primitive 리스트 / 리스트 보존형 — 기본)

XML 내부에서 `<foreach>` 로 `{listField}` 순회:

```xml
<update id="{mapper2}" parameterType="{pkg}.application.command.{SubObj2}Command">
/* {Submodule}Mapper.{mapper2} */
/*
<foreach collection="{listField}" item="item" separator=",">
    ...
</foreach>
*/
</update>
```

## POST (--nested + primitive 리스트 / 평탄화형 — --flatten-list)

항목 단위 Command 로 단건 INSERT:

```xml
<update id="{mapper2Item}" parameterType="{pkg}.application.command.{SubObj2}ItemCommand">
/* {Submodule}Mapper.{mapper2Item} */
</update>
```

---

# 기존 파일 수정 규칙

1. Controller/Service/Mapper 파일이 이미 존재하면 → 해당 파일을 Read 후 **기존 내용 끝의 `}` 앞에 삽입**한다.
2. Mapper XML 이 이미 존재하면 → `</mapper>` 바로 앞에 새 블록을 삽입한다.
3. 신규 import 가 필요하면 기존 import 블록 마지막 줄 뒤에 추가한다.

---

# 완료 후 출력 형식

```
✅ Done: {submodule} {method} /{endpoint}

Created:
- {신규 생성 파일 경로 목록}

Modified:
- {수정된 파일 경로 목록}
```