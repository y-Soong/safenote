# PRAFTA 로컬 AI 스택 (pgvector + TEI/BGE-m3)

한글 텍스트 → 벡터(1024차원) → pgvector 유사도 검색까지 **로컬에서** 관통 검증하기 위한 최소 인프라.
백엔드(MySQL/Spring)와 분리된 실험용이며, 여기서 검색이 서야 적재 파이프라인·Spring 연동을 그 위에 올린다.

| 컨테이너 | 역할 | 포트 |
|---|---|---|
| `prafta-pgvector` | 코퍼스 DB(source/chunk) + 벡터검색 | 5432 |
| `prafta-tei` | 임베딩 서버(BGE-m3, 텍스트→벡터) | 8090 → 컨테이너 80 |

## 사전 준비 (Windows)

- **Docker Desktop 설치 필요** (현재 미설치 상태). WSL2 백엔드 권장.
- TEI를 CPU로 돌리므로 Docker Desktop에 **메모리 최소 4GB 이상** 할당 권장
  (Settings → Resources). 모자라면 TEI 컨테이너가 OOM으로 죽는다.
- 설치/기동은 사용자가 직접: 프롬프트에 `! <명령>` 형태로 실행하면 출력이 이 세션에 남는다.

## 기동

```bash
cd C:/PRAFTA/prafta-ai-stack
docker compose up -d
docker compose logs -f tei      # 모델 로딩 완료(Ready)까지 관찰
```

> ⚠️ 첫 기동: TEI가 BGE-m3(약 2GB+)를 내려받는다. 회선에 따라 수 분~수십 분.
> 로그에 `Ready` 계열 메시지가 뜨기 전엔 임베딩 요청이 안 받아진다.
> `tei_model_cache` 볼륨 덕에 두 번째 기동부터는 빠르다.

## 검증

**검증 1 — 임베딩이 나오는가 (길이 1024 확인)**
```bash
curl -X POST http://localhost:8090/embed \
  -H "Content-Type: application/json" \
  -d '{"inputs": "밀폐공간 작업 중 산소결핍 질식 위험"}'
```

**검증 2 — pgvector 확장/테이블 확인**
```bash
docker exec -it prafta-pgvector psql -U prafta -d prafta_ai -c "\dx"
docker exec -it prafta-pgvector psql -U prafta -d prafta_ai -c "\dt"
```
`vector` 확장과 `tb_ai_corpus_source` / `tb_ai_corpus_chunk` 두 테이블이 보이면 성공.

**검증 3 — 끝까지 관통(미니 E2E, 자동 스크립트)**
`smoke_test.py` 가 전 과정을 한 방에 수행한다:
SIF 샘플 5문장 임베딩(차원 1024 검증) → chunk 적재 → 질의("탱크 내부 청소 작업")
임베딩 → 코사인 유사도 검색 결과 출력.

```bash
py smoke_test.py        # 또는 python smoke_test.py
```

- 전제: 컨테이너 2개가 떠 있고 TEI 모델 로딩(Ready)이 끝난 상태.
- 별도 파이썬 패키지 불필요(표준 라이브러리 + `docker exec psql` 사용).
- 재실행해도 안전(멱등: SOURCE_ID='SMOKE-TEST' 데이터를 지우고 다시 적재).
- 결과 상단에 밀폐공간/청소 계열 청크(`cos_dist` 작은 순)가 잡히면 관통 성공.

## 정지 / 초기화

```bash
docker compose down            # 컨테이너만 정지(데이터·모델 캐시 볼륨 유지)
docker compose down -v         # 볼륨까지 삭제(스키마·모델 캐시 전부 초기화 → init.sql 재실행)
```

## 알아둘 함정

1. **소문자 접힘**: Postgres는 따옴표 없는 식별자를 소문자로 접는다. `SOURCE_ID`는 실제 `source_id`.
   MyBatis `mapUnderscoreToCamelCase=true`면 `sourceId`로 자연 매핑되니 소문자 그대로 수용 권장.
2. **HNSW 인덱스 + 대량 적재**: SIF 수천 건 일괄 적재 시엔 인덱스 DROP → 적재 → 재생성이 빠르다.
   (적재 파이프라인에서 반영)
3. **CPU 임베딩 속도**: BGE-m3 CPU는 건당 수백 ms. 6천 건 일괄은 수십 분. 로컬 검증엔 무방,
   대량 적재는 배치(야간). 정말 느리면 그때 GPU/외부 API 검토.

## 주의

- `POSTGRES_PASSWORD`는 로컬 전용 throwaway 값이다. 운영 자격증명은 별도 시크릿으로 관리하고 커밋하지 않는다.
- `init.sql`은 **데이터 볼륨이 비어 있는 최초 기동에만** 실행된다. 스키마를 바꿨는데 반영이 안 되면
  `docker compose down -v`로 볼륨을 초기화한 뒤 다시 `up`.
