# -*- coding: utf-8 -*-
"""
PRAFTA 로컬 AI 스택 미니 E2E 스모크 테스트.

관통 흐름(검증 3):
  1) TEI(BGE-m3)로 SIF 샘플 문장들 -> 1024차원 벡터 임베딩
  2) 차원(1024) 검증
  3) pgvector chunk 테이블에 INSERT (docker exec psql 경유, 드라이버 불필요)
  4) 질의 문장("탱크 내부 청소 작업")을 임베딩 -> 코사인 유사도 검색
  5) 가장 가까운 청크가 상식적으로 맞게 잡히는지 출력

전제: `docker compose up -d` 로 prafta-pgvector, prafta-tei 가 이미 떠 있고
      TEI 모델 로딩(Ready)이 끝났을 것.
실행: py smoke_test.py    (또는 python smoke_test.py)
"""

import json
import subprocess
import sys
import urllib.request
import urllib.error

TEI_URL = "http://localhost:8090/embed"
PG_CONTAINER = "prafta-pgvector"
EXPECTED_DIM = 1024
SOURCE_ID = "SMOKE-TEST"

# SIF 유형 위험 문장 샘플(코퍼스 역할). 질의와의 유사도 순위가 상식적으로 나오는지 보려는 용도.
CORPUS = [
    ("SMOKE-C1", "밀폐공간 작업 중 산소결핍으로 인한 질식 위험", "밀폐공간"),
    ("SMOKE-C2", "탱크 내부 청소 작업 시 유해가스 중독 위험", "밀폐공간"),
    ("SMOKE-C3", "고소작업 중 안전대 미착용으로 인한 추락 위험", "고소작업"),
    ("SMOKE-C4", "전기 패널 점검 중 감전 위험", "전기"),
    ("SMOKE-C5", "지게차 운행 중 근로자 협착 위험", "운반하역"),
]
QUERY_TEXT = "탱크 내부 청소 작업"


def embed(texts):
    """TEI /embed 호출. texts(list[str]) -> list[list[float]]"""
    body = json.dumps({"inputs": texts}).encode("utf-8")
    req = urllib.request.Request(
        TEI_URL, data=body, headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.URLError as e:
        sys.exit(
            f"[실패] TEI 호출 오류: {e}\n"
            f"  - prafta-tei 컨테이너가 떠 있고 모델 로딩(Ready)이 끝났는지 확인하세요.\n"
            f"  - docker compose logs -f tei"
        )


def vec_literal(vec):
    """float 리스트 -> pgvector 입력 리터럴 '[0.1,0.2,...]'"""
    return "[" + ",".join(f"{x:.8f}" for x in vec) + "]"


def run_psql(sql):
    """docker exec -i prafta-pgvector psql 로 SQL 실행(드라이버 불필요)."""
    cmd = [
        "docker", "exec", "-i", PG_CONTAINER,
        "psql", "-U", "prafta", "-d", "prafta_ai",
        "-v", "ON_ERROR_STOP=1", "-q",
    ]
    try:
        proc = subprocess.run(
            cmd, input=sql, text=True, encoding="utf-8",
            capture_output=True,
        )
    except FileNotFoundError:
        sys.exit("[실패] docker 명령을 찾을 수 없습니다. Docker Desktop 설치/실행을 확인하세요.")
    if proc.returncode != 0:
        sys.exit(f"[실패] psql 오류:\n{proc.stderr}")
    return proc.stdout


def main():
    print("== 1) 코퍼스 문장 임베딩 (TEI/BGE-m3) ==")
    corpus_vecs = embed([c[1] for c in CORPUS])
    for (cid, text, _), v in zip(CORPUS, corpus_vecs):
        if len(v) != EXPECTED_DIM:
            sys.exit(f"[실패] {cid} 임베딩 차원이 {len(v)} (기대: {EXPECTED_DIM})")
    print(f"   OK - {len(corpus_vecs)}개 문장, 각 {EXPECTED_DIM}차원 확인")

    print("== 2) pgvector 적재 (기존 스모크 데이터 정리 후 INSERT) ==")
    lines = []
    # 재실행 멱등: 이전 스모크 데이터 제거(자식 chunk -> 부모 source 순)
    lines.append(f"DELETE FROM tb_ai_corpus_chunk  WHERE SOURCE_ID = '{SOURCE_ID}';")
    lines.append(f"DELETE FROM tb_ai_corpus_source WHERE SOURCE_ID = '{SOURCE_ID}';")
    lines.append(
        "INSERT INTO tb_ai_corpus_source "
        "(SOURCE_ID, SOURCE_ORG, SOURCE_NAME, LICENSE_TYPE, ADOPTED_DATE, FEED_TYPE) "
        f"VALUES ('{SOURCE_ID}', 'PRAFTA', '스모크 테스트 코퍼스', 'INTERNAL', '20260702', 'MANUAL');"
    )
    for (cid, text, tag), v in zip(CORPUS, corpus_vecs):
        content = text.replace("'", "''")
        lines.append(
            "INSERT INTO tb_ai_corpus_chunk "
            "(CHUNK_ID, SOURCE_ID, CONTENT, EMBEDDING, HAZARD_TEXT, DOMAIN_TAG) "
            f"VALUES ('{cid}', '{SOURCE_ID}', '{content}', "
            f"'{vec_literal(v)}'::vector, '{content}', '{tag}');"
        )
    run_psql("\n".join(lines))
    print(f"   OK - source 1행 + chunk {len(CORPUS)}행 적재")

    print(f"== 3) 유사도 검색 (질의: '{QUERY_TEXT}') ==")
    query_vec = embed([QUERY_TEXT])[0]
    if len(query_vec) != EXPECTED_DIM:
        sys.exit(f"[실패] 질의 임베딩 차원 {len(query_vec)} (기대: {EXPECTED_DIM})")
    search_sql = (
        "SELECT CHUNK_ID, DOMAIN_TAG, "
        f"ROUND((EMBEDDING <=> '{vec_literal(query_vec)}'::vector)::numeric, 4) AS cos_dist, "
        "CONTENT "
        "FROM tb_ai_corpus_chunk "
        f"WHERE SOURCE_ID = '{SOURCE_ID}' "
        f"ORDER BY EMBEDDING <=> '{vec_literal(query_vec)}'::vector "
        "LIMIT 5;"
    )
    out = run_psql(search_sql)
    print(out)
    print("== 완료 ==")
    print("cos_dist 가 작을수록 유사. 상단에 '탱크 내부 청소/밀폐공간' 계열 청크가 잡히면")
    print("텍스트->벡터->유사도검색 관통이 로컬에서 실제로 도는 것입니다.")


if __name__ == "__main__":
    main()
