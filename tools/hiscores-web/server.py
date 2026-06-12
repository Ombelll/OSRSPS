from __future__ import annotations

import argparse
import html
import json
import sqlite3
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlparse


SKILLS = [
    "Attack",
    "Defence",
    "Strength",
    "Hitpoints",
    "Ranged",
    "Prayer",
    "Magic",
    "Cooking",
    "Woodcutting",
    "Fletching",
    "Fishing",
    "Firemaking",
    "Crafting",
    "Smithing",
    "Mining",
    "Herblore",
    "Agility",
    "Thieving",
    "Slayer",
    "Farming",
    "Runecrafting",
    "Hunter",
    "Construction",
]


@dataclass(frozen=True)
class WorldDb:
    label: str
    path: Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Serve local OSRSPS hiscores from game.db.")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8088)
    parser.add_argument("--repo", default=Path.cwd(), type=Path)
    args = parser.parse_args()

    repo = args.repo.resolve()
    worlds = {
        "1": WorldDb("World 1", repo / ".data" / "saves" / "game.db"),
        "2": WorldDb("World 2", repo / ".data" / "saves" / "game_w2.db"),
    }

    class Handler(HiscoresHandler):
        world_dbs = worlds

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"Hiscores web running at http://{args.host}:{args.port}")
    print("Reading databases in SQLite read-only mode.")
    server.serve_forever()


class HiscoresHandler(BaseHTTPRequestHandler):
    world_dbs: dict[str, WorldDb]

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/":
            self.write_html(render_page())
            return
        if parsed.path == "/api/hiscores":
            self.write_json(self.hiscores(parse_qs(parsed.query)))
            return
        if parsed.path == "/health":
            self.write_json({"ok": True, "worlds": self.world_status()})
            return
        self.send_error(HTTPStatus.NOT_FOUND, "Not found")

    def log_message(self, format: str, *args: Any) -> None:
        return

    def hiscores(self, query: dict[str, list[str]]) -> dict[str, Any]:
        world_key = first(query, "world", "1")
        skill_arg = first(query, "skill", "overall").lower()
        search = first(query, "search", "").strip().lower()
        limit = clamp_int(first(query, "limit", "50"), 1, 200)
        world = self.world_dbs.get(world_key, self.world_dbs["1"])
        if not world.path.exists():
            return {
                "ok": False,
                "error": f"Database not found: {world.path}",
                "world": world.label,
                "rows": [],
            }

        if skill_arg == "overall":
            rows = load_overall_hiscores(world.path)
        else:
            rows = [row for row in load_skill_hiscores(world.path) if row["skill"].lower() == skill_arg]
        if search:
            rows = [row for row in rows if search in row["displayName"].lower()]
        rows.sort(key=lambda row: (-row["xp"], -row["level"], row["displayName"].lower(), row["skillId"]))
        for index, row in enumerate(rows, start=1):
            row["rank"] = index
        return {
            "ok": True,
            "world": world.label,
            "updatedFrom": str(world.path),
            "skills": ["Overall", *SKILLS],
            "rows": rows[:limit],
            "count": len(rows),
        }

    def world_status(self) -> list[dict[str, Any]]:
        return [
            {"id": key, "label": world.label, "path": str(world.path), "exists": world.path.exists()}
            for key, world in self.world_dbs.items()
        ]

    def write_json(self, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, ensure_ascii=True).encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def write_html(self, body: str) -> None:
        payload = body.encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


def load_overall_hiscores(path: Path) -> list[dict[str, Any]]:
    uri = f"file:{path.as_posix()}?mode=ro"
    query = """
        SELECT
            COALESCE(a.display_name, a.login_username) AS display_name,
            SUM(s.base_level) AS total_level,
            SUM(s.fine_xp) / 10 AS xp,
            c.last_logout
        FROM stats s
        JOIN characters c ON c.id = s.character_id
        JOIN accounts a ON a.id = c.account_id
        WHERE s.stat_id BETWEEN 0 AND 22
        GROUP BY c.id, display_name, c.last_logout
        ORDER BY xp DESC
    """
    with sqlite3.connect(uri, uri=True) as connection:
        connection.row_factory = sqlite3.Row
        return [
            {
                "displayName": html.escape(str(row["display_name"] or "Unknown")),
                "skillId": -1,
                "skill": "Overall",
                "level": int(row["total_level"] or 0),
                "xp": int(row["xp"] or 0),
                "lastLogout": row["last_logout"],
            }
            for row in connection.execute(query)
        ]


def load_skill_hiscores(path: Path) -> list[dict[str, Any]]:
    uri = f"file:{path.as_posix()}?mode=ro"
    query = """
        SELECT
            COALESCE(a.display_name, a.login_username) AS display_name,
            s.stat_id,
            s.base_level,
            s.fine_xp / 10 AS xp,
            c.last_logout
        FROM stats s
        JOIN characters c ON c.id = s.character_id
        JOIN accounts a ON a.id = c.account_id
        WHERE s.stat_id BETWEEN 0 AND 22
        ORDER BY xp DESC
    """
    with sqlite3.connect(uri, uri=True) as connection:
        connection.row_factory = sqlite3.Row
        return [format_row(row) for row in connection.execute(query)]


def format_row(row: sqlite3.Row) -> dict[str, Any]:
    stat_id = int(row["stat_id"])
    return {
        "displayName": html.escape(str(row["display_name"] or "Unknown")),
        "skillId": stat_id,
        "skill": SKILLS[stat_id] if stat_id < len(SKILLS) else f"Skill {stat_id}",
        "level": int(row["base_level"] or 1),
        "xp": int(row["xp"] or 0),
        "lastLogout": row["last_logout"],
    }


def first(query: dict[str, list[str]], key: str, default: str) -> str:
    values = query.get(key)
    return values[0] if values else default


def clamp_int(value: str, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except ValueError:
        parsed = minimum
    return max(minimum, min(maximum, parsed))


def render_page() -> str:
    skills = "".join(f"<option value=\"{skill.lower()}\">{skill}</option>" for skill in ["Overall", *SKILLS])
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>OSRSPS Hiscores</title>
  <style>
    :root {{
      color-scheme: dark;
      --bg: #121418;
      --panel: #1b2027;
      --panel-2: #222933;
      --line: #343e4c;
      --text: #eef3f8;
      --muted: #a9b5c3;
      --accent: #f1b84b;
      --accent-2: #64c6a7;
      --danger: #ff7b7b;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      min-height: 100vh;
      background: var(--bg);
      color: var(--text);
      font-family: Arial, Helvetica, sans-serif;
    }}
    header {{
      border-bottom: 1px solid var(--line);
      background: #171b21;
    }}
    .wrap {{
      width: min(1120px, calc(100vw - 32px));
      margin: 0 auto;
    }}
    .top {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      min-height: 76px;
    }}
    h1 {{
      margin: 0;
      font-size: 26px;
      letter-spacing: 0;
    }}
    .status {{
      color: var(--muted);
      font-size: 14px;
      text-align: right;
    }}
    main {{ padding: 24px 0 32px; }}
    .toolbar {{
      display: grid;
      grid-template-columns: 150px 180px minmax(180px, 1fr) 100px;
      gap: 12px;
      margin-bottom: 18px;
    }}
    select, input {{
      width: 100%;
      min-height: 42px;
      border: 1px solid var(--line);
      border-radius: 6px;
      background: var(--panel);
      color: var(--text);
      padding: 0 12px;
      font: inherit;
    }}
    .table-wrap {{
      overflow-x: auto;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--panel);
    }}
    table {{
      width: 100%;
      border-collapse: collapse;
      min-width: 760px;
    }}
    th, td {{
      padding: 13px 14px;
      border-bottom: 1px solid var(--line);
      text-align: left;
      white-space: nowrap;
    }}
    th {{
      background: var(--panel-2);
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
    }}
    td.rank {{
      color: var(--accent);
      font-weight: 700;
      width: 72px;
    }}
    td.num {{ text-align: right; font-variant-numeric: tabular-nums; }}
    .player {{ font-weight: 700; }}
    .empty {{
      padding: 30px;
      color: var(--muted);
      text-align: center;
    }}
    .error {{ color: var(--danger); }}
    .pill {{
      display: inline-block;
      padding: 4px 9px;
      border-radius: 999px;
      background: rgba(100, 198, 167, 0.14);
      color: var(--accent-2);
      font-size: 12px;
      font-weight: 700;
    }}
    @media (max-width: 760px) {{
      .top {{
        align-items: flex-start;
        flex-direction: column;
        padding: 18px 0;
      }}
      .status {{ text-align: left; }}
      .toolbar {{ grid-template-columns: 1fr 1fr; }}
    }}
  </style>
</head>
<body>
  <header>
    <div class="wrap top">
      <h1>OSRSPS Hiscores</h1>
      <div class="status" id="status">Loading read-only hiscores...</div>
    </div>
  </header>
  <main class="wrap">
    <div class="toolbar">
      <select id="world" aria-label="World">
        <option value="1">World 1</option>
        <option value="2">World 2</option>
      </select>
      <select id="skill" aria-label="Skill">{skills}</select>
      <input id="search" placeholder="Search player" aria-label="Search player">
      <select id="limit" aria-label="Limit">
        <option value="25">Top 25</option>
        <option value="50" selected>Top 50</option>
        <option value="100">Top 100</option>
        <option value="200">Top 200</option>
      </select>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Rank</th>
            <th>Player</th>
            <th>Skill</th>
            <th class="num">Level</th>
            <th class="num">XP</th>
            <th>Last logout</th>
          </tr>
        </thead>
        <tbody id="rows"><tr><td colspan="6" class="empty">Loading...</td></tr></tbody>
      </table>
    </div>
  </main>
  <script>
    const controls = ["world", "skill", "search", "limit"].map((id) => document.getElementById(id));
    const rows = document.getElementById("rows");
    const status = document.getElementById("status");
    let timer;

    for (const control of controls) {{
      control.addEventListener("input", () => {{
        clearTimeout(timer);
        timer = setTimeout(load, 120);
      }});
    }}

    async function load() {{
      const params = new URLSearchParams({{
        world: document.getElementById("world").value,
        skill: document.getElementById("skill").value,
        search: document.getElementById("search").value,
        limit: document.getElementById("limit").value,
      }});
      const response = await fetch(`/api/hiscores?${{params}}`);
      const payload = await response.json();
      if (!payload.ok) {{
        rows.innerHTML = `<tr><td colspan="6" class="empty error">${{escapeHtml(payload.error)}}</td></tr>`;
        status.textContent = "Database unavailable";
        return;
      }}
      status.innerHTML = `<span class="pill">${{payload.world}}</span> ${{payload.count}} ranked rows`;
      if (payload.rows.length === 0) {{
        rows.innerHTML = '<tr><td colspan="6" class="empty">No hiscore rows found.</td></tr>';
        return;
      }}
      rows.innerHTML = payload.rows.map((row) => `
        <tr>
          <td class="rank">#${{row.rank}}</td>
          <td class="player">${{row.displayName}}</td>
          <td>${{row.skill}}</td>
          <td class="num">${{row.level.toLocaleString()}}</td>
          <td class="num">${{row.xp.toLocaleString()}}</td>
          <td>${{row.lastLogout || "Never"}}</td>
        </tr>
      `).join("");
    }}

    function escapeHtml(value) {{
      const div = document.createElement("div");
      div.textContent = value;
      return div.innerHTML;
    }}

    load();
  </script>
</body>
</html>"""


if __name__ == "__main__":
    main()
