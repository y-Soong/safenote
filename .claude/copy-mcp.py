"""prafta-mysql MCP 설정을 C:/Users/dudjs 프로젝트 컨텍스트에서 C:/PRAFTA로 복사."""
import json
import shutil
import sys
from datetime import datetime
from pathlib import Path

CONFIG_PATH = Path(r"C:\Users\dudjs\.claude.json")
SRC_PROJECT = "C:/Users/dudjs"
DST_PROJECT = "C:/PRAFTA"
TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")
BACKUP_PATH = CONFIG_PATH.with_suffix(f".json.bak.{TIMESTAMP}")


def main() -> int:
    if not CONFIG_PATH.exists():
        print(f"ERROR: {CONFIG_PATH} not found")
        return 1

    shutil.copy2(CONFIG_PATH, BACKUP_PATH)
    print(f"[OK] Backup created: {BACKUP_PATH}")

    with CONFIG_PATH.open("r", encoding="utf-8") as f:
        data = json.load(f)

    projects = data.get("projects", {})

    src_cfg = projects.get(SRC_PROJECT, {})
    src_mcp = src_cfg.get("mcpServers", {})
    if "prafta-mysql" not in src_mcp:
        print(f"ERROR: 'prafta-mysql' not in {SRC_PROJECT} mcpServers")
        return 1
    src_entry = src_mcp["prafta-mysql"]
    print(f"[OK] Source entry found in {SRC_PROJECT}")

    if DST_PROJECT not in projects:
        print(f"ERROR: project '{DST_PROJECT}' context missing")
        return 1

    dst_cfg = projects[DST_PROJECT]
    dst_mcp = dst_cfg.setdefault("mcpServers", {})
    if "prafta-mysql" in dst_mcp:
        print(f"[NOTE] 'prafta-mysql' already exists in {DST_PROJECT}, will overwrite")

    dst_mcp["prafta-mysql"] = src_entry
    print(f"[OK] Copied 'prafta-mysql' to {DST_PROJECT} mcpServers")

    with CONFIG_PATH.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # 검증
    with CONFIG_PATH.open("r", encoding="utf-8") as f:
        verify = json.load(f)
    verify_mcp = verify.get("projects", {}).get(DST_PROJECT, {}).get("mcpServers", {})
    if "prafta-mysql" not in verify_mcp:
        print("ERROR: verify failed, restoring backup")
        shutil.copy2(BACKUP_PATH, CONFIG_PATH)
        return 1

    print(f"[OK] Verified: {DST_PROJECT} mcpServers now has keys = {list(verify_mcp.keys())}")
    print(f"[OK] Restart Claude Code (/exit then re-enter from C:\\PRAFTA) to activate MCP")
    return 0


if __name__ == "__main__":
    sys.exit(main())
