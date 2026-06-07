# PRAFTA-049 작업지시서 — 공지 관리 화면 보완 (Notice_01 / NoticeInfoPop)

> 출처 요청서: `.claude/requests/web_requests/prafta-049.md`
> 작업 영역: 웹/백엔드 (`PRAFTA/prafta-backend`, `PRAFTA/prafta-web-frontend/prafta-web-frontend`)
> 도메인: 공지사항(Notice) — 기존 PRAFTA-047 도메인 보완
> 정책서 출처: 공지 도메인은 PRAFTA-047 greenfield 설계물로, 별도 정책서 섹션(common/10-notifications)에는 발송/알림 위주 기술만 있고 공지 관리 화면 동작은 정의가 없다. 본 작업은 비즈니스 룰 신설이 아니라 기존 화면/계약의 UI 보완 + 목록 필터 추가이므로 정책 충돌 없음. (스코프 ALL/SITE/NODE·하위포함·일용직 노출 규칙은 기존 구현 = 단일 출처.)

---

## ★ 확정 결정 (사용자, 2026-06-06) — 아래가 최우선
- **D1 (§8-1 제거)**: "팝업 게시기간 중 수정 차단" 정책을 **완전히 제거**한다. 게시기간 중에도 제목·내용·대상·첨부 전부 수정 가능.
  - 프론트: `NoticeInfoPop` 의 `isContentLocked` 잠금 제거(제목/내용 readonly = `!isEditMode` 만), 노란 안내문(`lock-notice`) 제거.
  - 백엔드: `Notice01ServiceImpl.updateNotice` 의 `if (isInActivePopupWindow(current)) throw NOTICE_422_001;` 블록 제거. (미사용이 되면 `isInActivePopupWindow` 헬퍼·`NOTICE_422_001` 는 그대로 둬도 무방하나, 호출부 제거가 핵심.)
- **D2 (삭제 버튼 제거)**: NoticeInfoPop 수정모드 footer 에서 **삭제 버튼 제거**. footer 는 **저장 버튼 1개만**(닫기·삭제 모두 없음). → 049-05 갱신. 미사용이 되는 `fnDelete` 및 관련 핸들러도 정리.
- **D3 (049-01 스코프)**: 사업장/노드 조회 시 **ALL 포함 + 노드 하위포함** 넓은 기준 확정(planner 가정 A/B 채택).
- **D4 (049-02 0건)**: 첨부 0건은 `-` 표시 확정.

---

## 0. 사전 사실 확인 (코드 정독 결과 · developer 가 반드시 인지)

### 0-1. 이미 구현되어 있는 것 (요청서와의 차이 — 헛작업 방지)
- **fileCnt 는 백엔드/프론트 모두 이미 존재**한다.
  - 백엔드: `NoticeResult.fileCnt` (Integer), `Notice01Mapper.xml selectNoticeList` 에 `(SELECT COUNT(1) FROM TB_NOTICE_FILE ...) AS fileCnt` 서브쿼리 존재.
  - 프론트: `Notice_01.vue` 154행 `<span v-if="item.fileCnt > 0">📎</span><span v-else>-</span>`.
  - 즉 **요청서 2번(첨부 개수 표시)은 백엔드 신규작업이 아니라 프론트 표시 방식만 변경**한다(아이콘 📎 → 숫자).
- NoticeInfoPop 수정모드의 **대상/노출/고정/일용직 필드 편집 UI 와 사업장·노드 조회 팝업 연동은 이미 구현**되어 있다(112~248행). 누락은 (a) 제목/내용/첨부 편집 잠금, (b) 첨부 신규추가 업로드, (c) 상단고정+고정순번 레이아웃, (d) 닫기 버튼/버튼 너비뿐이다.

### 0-2. TB_NOTICE_TARGET 스키마 (Notice01Mapper.xml 정독 기준 · 추측 아님)
| 컬럼 | 용도 |
|------|------|
| CMPNY_CD | 회사 |
| NOTICE_ID | 공지 |
| TARGET_SEQ | 대상 순번 (INSERT 시 index+1) |
| SITE_CD | 대상 사업장 |
| NODE_CD | 대상 노드 (SITE 스코프면 NULL) |
| INCLUDE_DESCENDANTS_YN | 하위 노드 포함 Y/N |

TB_NOTICE: TARGET_SCOPE('ALL'/'SITE'/'NODE'), INCLUDE_DAILY_YN, POPUP_YN, POPUP_FROM_YMD, POPUP_TO_YMD, PIN_YN, PIN_ORDER, INSERT_NO, INSERT_DATE, UPDATE_DATE, DEL_YN.

### 0-3. 노드 하위포함 재귀 CTE 가 이미 매퍼에 존재
`countNodeInSelfSubtree` (XML 26~53행) 가 `WITH RECURSIVE node_tree` 로 노드+자손을 구한다. 049-01 목록 필터의 "노드 하위포함" 판정 시 이 패턴을 재사용한다(신규 재귀 로직 작성 금지).

### 0-4. 업로드 플로우 (NoticeCreatePop 검증된 패턴 — NoticeInfoPop 으로 이식)
1. native `<input type="file" multiple accept="...">` 숨김 + 버튼 트리거.
2. 저장 시 각 파일을 `POST /webApi/notice01/upload-file` (multipart, key=`file`) → 응답 `{ fileMgmtCd, fileNm }`.
3. `fileList` 페이로드 = `[{ fileMgmtCd, sortIdx }]` 형태로 save/update-notice 에 동봉.
- accept 화이트리스트(NoticeCreatePop 215행): `image/*,video/*,audio/*,text/*,.csv,.tsv,.log,.md,.json,.xml,.rtf`. 서버가 최종 검증.

---

## 작업 분해 결과

### PRAFTA-049-01 — 공지 목록 사업장/노드 조회조건 (프론트 + 백엔드)
- **유형**: mixed → 아래 049-01-BE / 049-01-FE 로 분할
- **영역**: web
- **모듈**: notice/notice01
- **작업 유형**: 보완
- **요구사항 요약**: Notice_01 검색바에 사업장+소속부서(노드) 조건 추가, 백엔드 목록 필터에 siteCd/nodeCd 반영.

#### ★ 핵심 가정 — "사업장/노드로 조회" 의 정의 (근거와 함께 명시)
공지는 `TARGET_SCOPE` (ALL/SITE/NODE) + `TB_NOTICE_TARGET` 구조다. "특정 사업장/노드로 조회" 란 그 site/node 를 **대상으로 하는 공지**를 거르는 것으로 정의한다. 결정사항:

- **가정 A (ALL 포함 여부)**: 사업장/노드 필터가 걸리면 **`TARGET_SCOPE='ALL'`(전사) 공지도 포함**한다.
  - 근거: 전사 공지는 모든 사업장/노드 구성원에게 노출되므로, "이 사업장에 노출되는 공지"를 보려는 관리자 의도에 부합. ALL 을 빼면 "해당 사업장 사람이 보는 공지 전체"를 못 보게 된다.
  - 구현: `WHERE (A.TARGET_SCOPE='ALL' OR EXISTS(...해당 site/node 매칭 타겟...))`.
- **가정 B (노드 하위 포함)**: nodeCd 로 조회 시, **해당 노드를 직접 대상으로 한 공지** + **상위 노드가 `INCLUDE_DESCENDANTS_YN='Y'` 로 지정되어 조회 노드를 포섭하는 공지** 둘 다 포함한다.
  - 근거: 하위포함('Y')으로 발행된 공지는 실제로 하위 노드에 노출되므로, 그 노드로 조회하면 보여야 일관적이다.
  - 구현: 타겟의 NODE_CD = 조회 nodeCd **OR** (타겟의 INCLUDE_DESCENDANTS_YN='Y' AND 조회 nodeCd 가 타겟 NODE_CD 의 자손) — 자손 판정은 `countNodeInSelfSubtree` 와 동일한 RECURSIVE CTE 를 EXISTS 안에서 사용. 단 방향에 주의: "타겟 노드의 서브트리에 조회 노드가 포함되는가" 이므로 CTE 의 root = 타겟 NODE_CD, 검사 = 조회 nodeCd.
- **가정 C (일용직/SITE 매칭)**: siteCd 만 입력(nodeCd 없음)이면 SITE 스코프 타겟(NODE_CD IS NULL) + 해당 사업장의 모든 NODE 스코프 타겟을 포함한다. 즉 `T.SITE_CD = 조회 siteCd` 매칭이면 노드 무관 모두 포함.
- **가정 D (선택 단위)**: 사업장만 단독 조회 가능. 노드는 User_01 처럼 사업장 선택 후에만 활성화(노드는 사업장 종속).
- **식별자 원칙**: cmpnyCd 는 절대 클라가 보내지 않음(서버 JWT 도출 유지). siteCd/nodeCd 만 검색 파라미터로 전송.

> 결정 확인 필요(낮음): 가정 A(ALL 포함)와 가정 B(하위포함 역방향)는 관리자 조회 편의 기준으로 planner 가 합리적으로 확정했다. 만약 "정확히 그 site/node 만 명시 대상인 공지"만 보고 싶다면 가정 A/B 를 끄는 옵션이 필요 — 현재는 끄지 않는 단순안으로 진행.

---

#### 049-01-BE — 목록 필터 백엔드
- **대상 파일**:
  - `dto/request/NoticeListRequest.java` — 필드 `siteCd`, `nodeCd` 추가.
  - `application/param/NoticeListParam.java` — record 인자 `siteCd`, `nodeCd` 추가 + `from()` 매핑.
  - `application/query/NoticeListQuery.java` — record 인자 `siteCd`, `nodeCd` 추가 + `from()` 매핑.
  - `mapper/Notice01Mapper.xml` `selectNoticeList` — WHERE 절에 site/node 조건 추가.
- **컨트롤러/서비스**: `getNoticeLists`, `selectNoticeList` 시그니처 무변경(파라미터 클래스만 확장). 수정 불필요.

- **계약 (요청 파라미터)**:
  | 파라미터 | 타입 | 비고 |
  |----------|------|------|
  | siteCd | String | 빈문자/누락 시 필터 미적용 |
  | nodeCd | String | siteCd 동반 시에만 의미. 빈문자/누락 시 노드 무관 |

- **XML 변경 (selectNoticeList WHERE 절에 추가)** — leading comma 규칙·`#{}` 바인딩 준수:

```xml
<!-- 사업장/노드 대상 필터 (가정 A: ALL 포함 / 가정 B: 노드 하위포함 역방향 / 가정 C: site 매칭) -->
<if test="siteCd != null and siteCd != ''">
  AND (
    A.TARGET_SCOPE = 'ALL'
    OR EXISTS (
      SELECT 1
      FROM TB_NOTICE_TARGET T
      WHERE T.CMPNY_CD = A.CMPNY_CD
        AND T.NOTICE_ID = A.NOTICE_ID
        AND T.SITE_CD = #{siteCd}
        <if test="nodeCd != null and nodeCd != ''">
          AND (
            T.NODE_CD = #{nodeCd}
            OR T.NODE_CD IS NULL  <!-- SITE 스코프 타겟은 사업장 전체 노출 → 노드조회에도 포함 -->
            OR (
              T.INCLUDE_DESCENDANTS_YN = 'Y'
              AND EXISTS (
                WITH RECURSIVE node_tree AS (
                    SELECT N.CMPNY_CD, N.SITE_CD, N.NODE_CD
                    FROM TB_SITE_NODE N
                    WHERE N.CMPNY_CD = T.CMPNY_CD
                      AND N.SITE_CD  = T.SITE_CD
                      AND N.NODE_CD  = T.NODE_CD
                    UNION ALL
                    SELECT C.CMPNY_CD, C.SITE_CD, C.NODE_CD
                    FROM TB_SITE_NODE C
                    INNER JOIN node_tree P
                      ON  C.CMPNY_CD       = P.CMPNY_CD
                      AND C.SITE_CD        = P.SITE_CD
                      AND C.PARENT_NODE_CD = P.NODE_CD
                )
                SELECT 1 FROM node_tree TT WHERE TT.NODE_CD = #{nodeCd}
              )
            )
          )
        </if>
    )
  )
</if>
```

> ⚠️ developer 검증 포인트: MySQL 8.0.42 에서 `EXISTS (WITH RECURSIVE ... )` 서브쿼리 중첩이 허용되는지 확인. 만약 파서가 거부하면, 자손 판정용 별도 매퍼 메서드(`selectNoticeList` 호출 전에 nodeCd 의 조상 노드 집합을 미리 구해 IN 절로 전달)로 대안 구현. 우선 인라인 시도 후 실패 시 분리.

- **빌드 검증**: `gradlew.bat compileJava --no-daemon` (타임아웃 300초). XML 은 컴파일 비대상이므로 앱 기동/단건 호출로 별도 확인 권장.

---

#### 049-01-FE — Notice_01 검색바 사업장/노드 입력
- **대상 파일**: `src/views/notice/Notice_01.vue`
- **변경 요지**: 검색바(viewSearch)에 사업장(코드/명/돋보기) + 소속부서(코드/명/돋보기) 추가. User_01 의 "코드 input + 돋보기 버튼 + 명칭 input" 3분할 구조를 차용. SiteSearchPop/SiteNodeSearchPop 연동. fnSearch params 에 siteCd/nodeCd 추가.

- **template 변경 (viewSearch `<div class="viewSearch">` 안, 등록기간 div 다음에 추가)**:

```vue
<div>
  <label>사업장</label>
  <input
    id="noticeSiteCd"
    type="text"
    v-model="siteCd"
    placeholder="사업장코드"
    readonly
    @click="fnOpenSiteSearch"
  />
  <button class="search-btn" @click="fnOpenSiteSearch">
    <img class="search_icon" :src="search_icon" alt="사업장 조회" />
  </button>
  <input
    id="noticeSiteNm"
    type="text"
    v-model="siteNm"
    placeholder="사업장명"
    readonly
    @click="fnOpenSiteSearch"
  />
</div>

<div>
  <label>소속부서</label>
  <input
    id="noticeNodeCd"
    type="text"
    v-model="nodeCd"
    placeholder="부서코드"
    :disabled="!siteCd"
    readonly
    @click="fnOpenNodeSearch"
  />
  <button class="search-btn" :disabled="!siteCd" @click="fnOpenNodeSearch">
    <img class="search_icon" :src="search_icon" alt="부서 조회" />
  </button>
  <input
    id="noticeNodeNm"
    type="text"
    v-model="nodeNm"
    placeholder="부서명"
    :disabled="!siteCd"
    readonly
    @click="fnOpenNodeSearch"
  />
</div>
```

> 비고: User_01 은 코드 직접입력+focusKill 까지 지원하나, 공지 조회는 조회팝업 선택만으로 충분하므로 input 을 `readonly @click` 방식(NoticeCreatePop 의 search-input 패턴과 동일 철학)으로 단순화한다. 직접입력 검증 로직 불필요 → 버그 표면 축소.

- **script 변경 (developer 가 채울 로직 — planner 는 골격/계약만 제시)**:
  - import: `import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";` `import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";` `import search_icon from "@/assets/img/search_icon.png";`
  - 반응형 변수 추가: `const siteCd = ref(""); const siteNo = ref(""); const siteNm = ref(""); const nodeCd = ref(""); const nodeNm = ref("");`
  - `fnOpenSiteSearch` / `fnOpenNodeSearch` : NoticeInfoPop 의 동명 함수와 동일 구조(아래 참조). 사업장 선택 시 노드 초기화. nodeCd 활성화는 `siteCd` 존재로 게이팅.
  - `fnSearch` params 에 추가: `siteCd: siteCd.value, nodeCd: nodeCd.value`.
  - TODO 주석: `// TODO(developer): SiteSearchPop onSelect 콜백 → siteCd/siteNo/siteNm 세팅 + nodeCd/nodeNm 초기화`

- **style**: 검색바는 전역 `.viewSearch` / `.search-btn` / `.search_icon` 클래스를 그대로 사용(User_01 과 동일). **신규 scoped 스타일 불필요**. 하드코딩 금지 규칙상 추가 CSS 없음.

---

### PRAFTA-049-02 — 목록 "첨부" 컬럼: 아이콘 → 개수 표시 (프론트)
- **유형**: frontend-screen
- **영역**: web / **모듈**: notice/notice01 / **작업 유형**: 보완
- **대상 파일**: `src/views/notice/Notice_01.vue`
- **스키마 근거**: `fileCnt` 는 이미 응답에 포함(0-1 참조). 백엔드 무변경.
- **변경 요지**: 첨부 셀을 📎 아이콘 대신 **첨부 개수**로 표시. 0건일 때 표시 방식 정의.

- **template 변경 (153~156행 교체)**:

```vue
<td style="text-align: center">
  <span v-if="item.fileCnt > 0" class="file-cnt" :aria-label="`첨부 ${item.fileCnt}건`">
    📎 {{ item.fileCnt }}
  </span>
  <span v-else class="file-cnt file-cnt--none">-</span>
</td>
```

> 0건 표시: 요청서가 "개수가 나오도록" 이라 했으나 0 을 그대로 찍으면 의미가 약하므로 **0건은 `-`** 로 유지(기존과 동일), 1건 이상은 `📎 N` 으로 클립 아이콘+숫자 병기. 아이콘 완전 제거를 원하면 `📎 ` 프리픽스를 빼고 숫자만 표기(아래 style 의 .file-cnt 는 동일).

- **style 추가 (scoped, CSS 변수만)**:

```css
.file-cnt {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: nowrap;
}
.file-cnt--none {
  color: var(--color-text-muted, #4b5563);
}
```

---

### PRAFTA-049-03 — NoticeInfoPop 수정모드 제목/내용/첨부 편집 활성화 (프론트)
- **유형**: frontend-screen + 백엔드 계약 확인
- **영역**: web / **모듈**: notice/notice01 / **작업 유형**: 보완
- **대상 파일**: `src/views/notice/popup/NoticeInfoPop.vue` (+ 백엔드 update-notice 가 신규 fileMgmtCd 를 수용하는지 확인)
- **변경 요지**:
  1. **[D1 확정] 제목/내용 잠금 완전 제거** — `:readonly="!isEditMode || isContentLocked"` → `:readonly="!isEditMode"` 로 변경. `isContentLocked` computed 와 노란 안내문(`lock-notice` form-row, 64~70행) **제거**. 또한 **백엔드 `updateNotice` 의 `if (isInActivePopupWindow(current)) throw NOTICE_422_001;`(190~192행) 블록 제거** (이걸 안 지우면 프론트만 풀려도 저장 시 422 로 거부됨). isContentLocked 가 묶고 있던 다른 필드(대상/노출/고정 등)도 함께 자유 편집 — 게시기간 무관 전체 수정 허용.
  2. **첨부 편집(추가/삭제) UI 이식** — NoticeCreatePop 의 파일선택+업로드 플로우를 수정모드 첨부 섹션에 추가. 기존(서버 보유) 첨부는 삭제 가능, 신규 파일은 추가 가능.
  3. **update-notice 페이로드의 fileList 에 신규 fileMgmtCd 포함** — 현재(586~590행)는 기존 첨부만 유지. 신규 업로드분을 합산.

- **백엔드 계약 확인 (developer 선행)**:
  - `update-notice` 는 `NoticeSaveRequest` 를 받아 `NoticeSaveParam.from()` → `updateNotice()`. save-notice 와 동일 DTO 이므로 **fileList 재설정(전체 교체) 의미를 updateNotice 가 이미 지원하는지** 서비스 구현 정독 필요. save 와 동일하게 "전달된 fileList 로 TB_NOTICE_FILE 재구성(delete+insert)" 이면 추가 작업 0. 만약 update 가 fileList 를 무시/추가만 한다면 서비스 보완 필요.
  - **계약 (변경 없음, 기존 구조 그대로)**: `fileList: [{ fileMgmtCd, sortIdx }]`. 신규 업로드도 `upload-file` 로 fileMgmtCd 발급 후 동일 구조로 동봉.
  - upload-file EP 는 이미 존재(컨트롤러 129행). 추가 백엔드 작업 없음(서비스 fileList 처리 방식만 검증).

- **template 변경**: 기존 "첨부파일(조회 다운로드)" 행(73~93행)을 **수정모드일 때는 편집 가능 버전**으로 분기. 비수정모드는 기존 다운로드 리스트 유지.

```vue
<!-- 첨부: 조회모드 = 다운로드 / 수정모드 = 편집(기존 삭제 + 신규 추가) -->
<div class="form-row">
  <label>첨부파일</label>

  <!-- 조회모드: 기존 다운로드 리스트 (현행 유지) -->
  <ul class="file-list" v-if="!isEditMode">
    <li v-for="(f, i) in formData.fileList" :key="i" class="file-item">
      <div
        class="file-item__link"
        role="button" tabindex="0"
        @click="fnDownloadFile(f)"
        @keydown.enter.space.prevent="fnDownloadFile(f)"
      >
        <span class="file-item__name">{{ f.fileNm }}</span>
        <span class="file-item__dl" aria-hidden="true">⬇</span>
      </div>
    </li>
    <li v-if="!formData.fileList || formData.fileList.length === 0" class="file-empty">
      첨부 없음
    </li>
  </ul>

  <!-- 수정모드: 편집 영역 (기존첨부 삭제칩 + 신규파일 추가) -->
  <div class="file-area" v-else>
    <input
      ref="fileInputRef"
      type="file"
      multiple
      accept="image/*,video/*,audio/*,text/*,.csv,.tsv,.log,.md,.json,.xml,.rtf"
      class="file-input-hidden"
      @change="fnOnFileChange"
    />
    <button type="button" class="btn-secondary file-select-btn" @click="fnTriggerFileSelect">
      파일 추가
    </button>

    <!-- 기존 첨부(서버 보유) : 삭제 가능 칩 -->
    <ul class="file-list" v-if="formData.fileList.length > 0 || newFiles.length > 0">
      <li v-for="(f, i) in formData.fileList" :key="'old-' + i" class="file-item">
        <span class="file-item__name">{{ f.fileNm }}</span>
        <button class="file-item__del" @click="fnRemoveExistingFile(i)">×</button>
      </li>
      <!-- 신규 추가 파일(미업로드) -->
      <li v-for="(nf, i) in newFiles" :key="'new-' + i" class="file-item file-item--new">
        <span class="file-item__name">{{ nf.name }}</span>
        <button class="file-item__del" @click="fnRemoveNewFile(i)">×</button>
      </li>
    </ul>
    <p class="hint">
      텍스트/이미지/동영상/음성 파일만 첨부할 수 있습니다(실행/스크립트 형식 제외). 저장 시 함께 업로드됩니다.
    </p>
  </div>
</div>
```

- **script 변경 (developer 영역 · planner 는 변수/계약만 제시)**:
  - 신규 ref: `const fileInputRef = ref(null); const newFiles = ref([]); const saving = ref(false);`
  - `fnTriggerFileSelect`, `fnOnFileChange`, `fnRemoveNewFile`(newFiles), `fnRemoveExistingFile`(formData.fileList) — NoticeCreatePop 패턴 그대로.
  - `fnUploadFile(file)` — NoticeCreatePop 431~438행 그대로 복사(POST /webApi/notice01/upload-file multipart).
  - `fnUpdate` 보완: 신규 파일 먼저 업로드 → fileMgmtCd 수집 → 기존 유지분 + 신규분 합산하여 fileList 페이로드 구성. sortIdx 는 (기존 length + i) 순서로 부여.
    - ```js
      // TODO(developer): 신규 newFiles 를 fnUploadFile 로 업로드 후 fileMgmtCd 수집
      // 페이로드: [...formData.fileList.map(f=>({fileMgmtCd:f.fileMgmtCd, sortIdx:f.sortIdx})),
      //            ...uploadedNew.map((u,idx)=>({fileMgmtCd:u.fileMgmtCd, sortIdx: base+idx+1}))]
      ```
  - 중복 제출 가드(`saving`) 추가.

- **style 추가 (scoped, CSS 변수만)** — NoticeCreatePop 의 file-area/file-input-hidden/file-select-btn/file-item__del 스타일을 NoticeInfoPop 으로 이식(현 NoticeInfoPop 에는 file-item__del/file-input-hidden/file-select-btn 미존재):

```css
.file-area { display: flex; flex-direction: column; align-items: flex-start; gap: var(--space-xs, 0.375rem); }
.file-input-hidden { display: none; }
.file-area .file-select-btn {
  align-self: flex-start; flex: 0 0 auto; width: fit-content; min-width: 0;
  height: var(--btn-height-sm, 26px); padding: 0 var(--space-md, 0.75rem);
  margin-left: 0; font-size: var(--btn-font, 11px);
}
.file-item--new { border-style: dashed; }
.file-item__del {
  flex: 0 0 auto; margin-left: 0; border: none; background: transparent;
  color: var(--color-text-muted, #4b5563); cursor: pointer;
  font-size: var(--font-size-sm, 0.875rem); line-height: 1;
}
```

---

### PRAFTA-049-04 — NoticeInfoPop "상단 고정" + "고정 순번" 한 행 배치 (프론트)
- **유형**: frontend-screen / **영역**: web / **모듈**: notice/notice01 / **작업 유형**: 보완
- **대상 파일**: `src/views/notice/popup/NoticeInfoPop.vue`
- **변경 요지**: 현재 "상단 고정"(234~240행)과 "고정 순번"(241~247행)이 별도 form-row 2줄. 한 행에 배치(체크박스 + 순번 input 인라인).
- **template 변경 (234~247행 교체)**:

```vue
<div class="form-row">
  <label>상단 고정</label>
  <div class="pin-row">
    <label class="checkbox-item">
      <input type="checkbox" v-model="formData.pinYn" true-value="Y" false-value="N" />
      목록 상단에 고정
    </label>
    <div class="pin-order-inline" v-if="formData.pinYn === 'Y'">
      <span class="pin-order-label">고정 순번</span>
      <input v-model.number="formData.pinOrder" type="number" min="1" class="pin-order-input" />
    </div>
  </div>
</div>
<div class="form-row" v-if="formData.pinYn === 'Y'">
  <span></span>
  <p class="hint">순번은 저장 시 서버에서 1..N 연속으로 보정됩니다.</p>
</div>
```

- **style 추가 (scoped, CSS 변수만)**:

```css
.pin-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md, 1rem);
}
.pin-order-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.5rem);
}
.pin-order-label {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: nowrap;
}
/* .pin-order-input(width:80px) 는 기존 정의 재사용 */
```

> 비고: 기존 `.pin-order-input { width: 80px; }` 가 이미 있으므로 그대로 적용된다. hint 는 별도 행으로 두어 한 행 배치를 깔끔히 유지.

---

### PRAFTA-049-05 — NoticeInfoPop "닫기" 제거 + 수정모드 버튼 너비/개행 방지 (프론트)
- **유형**: frontend-screen / **영역**: web / **모듈**: notice/notice01 / **작업 유형**: 보완
- **대상 파일**: `src/views/notice/popup/NoticeInfoPop.vue`
- **변경 요지 [D2 확정]**:
  1. footer 의 "닫기" 버튼 제거. (헤더 X 아이콘으로 닫음.)
  2. **"삭제" 버튼도 제거** (사용자: 삭제 기능 불필요). footer 는 **"저장" 1개만**.
  3. 미사용이 되는 `fnDelete` 핸들러 및 삭제 전용 import/로직 정리(제거). 백엔드 delete EP 자체는 건드리지 않음(다른 곳에서 쓸 수 있으니 화면 버튼만 제거).
  4. 남은 "저장" 버튼 텍스트 개행 방지 — 버튼 공통 규격(min-width, inline-flex, white-space:nowrap, line-height:1) 적용.
- **template 변경 (기존 footer 251~258행 교체)**:

```vue
<!-- 액션 (수정모드: 저장만. 닫기/삭제 제거 — 헤더 X 로 닫음) -->
<div class="modal-footer" v-if="isEditMode">
  <button class="btn-primary" @click="fnUpdate">저장</button>
</div>
```

> 조회모드(isEditMode=false)에서는 footer 가 비므로 `v-if="isEditMode"` 로 footer 자체를 숨긴다. (조회모드 액션은 본문 비밀번호 영역의 "확인/수정 모드" 버튼이 담당.)

- **style 변경 (scoped)** — 기존 `.btn-primary`(footer) 에 개행 방지 규격 추가:

```css
/* 수정모드 저장 버튼: 텍스트 개행 방지 + 최소 너비 */
.modal-footer .btn-primary {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  white-space: nowrap;
  line-height: 1;
}
```

> 근본 원인: 현 `.btn-primary` 는 height 만 고정하고 폭/줄바꿈 규격이 없어 좁은 폭에서 개행될 수 있다. min-width+nowrap+inline-flex 로 통일.

---

## SiteSearch/SiteNodeSearch 연동 콜백 계약 (049-01-FE / 049-03 공통)

NoticeInfoPop/NoticeCreatePop 에 이미 검증된 형태. Notice_01 검색바도 동일 콜백 시그니처 사용:

```js
// 사업장 조회
const fnOpenSiteSearch = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
      siteCd.value = siteCdVal ?? "";
      siteNo.value = siteNoVal ?? "";
      siteNm.value = siteNmVal ?? "";
      nodeCd.value = ""; nodeNm.value = "";   // 사업장 변경 시 노드 초기화
    },
  });
};
// 노드 조회 (사업장 선택 후)
const fnOpenNodeSearch = () => {
  if (!siteCd.value) return proxy.$alert("사업장을 먼저 조회하여 선택해 주세요.");
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};
```

> SiteSearchPop onSelect 인자 순서 = (siteCd, siteNo, siteNm) / SiteNodeSearchPop onSelect = (nodeCd, nodeNm). User_01·NoticeCreatePop 정독으로 확정.

---

## developer 착수 순서
1. **049-01-BE** (목록 필터 백엔드) — DTO/Param/Query record 인자 추가 + XML WHERE 절. record 위치 매핑은 NoticeResult 가 아니라 Query/Param 이므로 컬럼순서 함정 무관. EXISTS+RECURSIVE 중첩 가능 여부 먼저 검증(불가 시 조상노드 IN 절 대안). API 없으면 FE 가 빈 결과만 받으므로 BE 선행.
2. **049-01-FE** (Notice_01 검색바) — BE 파라미터 확정 후 연결.
3. **049-02** (첨부 개수 표시) — 독립, 즉시 가능.
4. **049-03** (수정모드 첨부 편집) — update-notice 의 fileList 재구성 동작을 서비스에서 먼저 확인(추가 BE 필요 여부 판단) 후 FE 이식.
5. **049-04 / 049-05** (레이아웃) — 독립, 즉시 가능.

권장 그룹핑: BE 1건(049-01-BE) → FE 일괄(049-01-FE / 02 / 03 / 04 / 05). 02/04/05 는 순수 표시·레이아웃이라 리스크 낮음.

## 확인 필요한 결정사항
1. **(049-03 핵심)** 사용자가 본 "수정모드인데 제목/내용 수정 불가" 가 **팝업 게시기간 중 잠금(§8-1, isContentLocked)** 때문인가? 그렇다면 제목/내용 잠금은 의도된 정책이며 코드 변경 없이 정상이다(노란 안내문 노출 중). 이 경우 049-03 은 **첨부 편집 추가**만 수행. 만약 "팝업기간이 아닌데도 못 고친다"면 별도 버그이므로 재현 조건 필요. → developer/QA 는 먼저 isContentLocked 발동 여부로 분기 판단.
2. **(049-01 가정 A/B)** ALL 공지 포함·노드 하위포함 역방향 포함을 planner 가 관리자 조회 편의 기준으로 확정했다. "정확히 그 site/node 명시 대상만" 으로 좁히려면 알려달라(현재는 넓게 포함).
3. **(049-02)** 첨부 0건 표시를 `-` 로 둘지 `0` 으로 둘지. planner 는 `-` 권장(기존 일관성). 0 표기를 원하면 알려달라.

## 보안/PII 참고 (security 에이전트 인계용)
- 목록 필터의 siteCd/nodeCd 는 검색 편의 파라미터일 뿐, **cmpnyCd 스코프(WHERE A.CMPNY_CD=#{gvCmpnyCd})는 유지**되므로 cross-company 누수 없음. 단 siteCd/nodeCd 로 **자기 회사 외 사업장 코드를 넣어도** cmpnyCd 스코프가 걸려 빈 결과만 나옴(IDOR 무영향). 추가 사업장 접근권한(tb_user_site_auth) 게이팅은 기존 목록이 안 하므로 본 작업도 동일(표시 범위 = 자사 전체 공지, 047 설계 유지).
- 첨부 업로드/다운로드 보안(확장자 화이트리스트·다운로드 토큰)은 047 에서 구현됨. 049-03 은 기존 EP 재사용이라 신규 표면 없음.
