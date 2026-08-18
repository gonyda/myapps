#!/usr/bin/env python3
"""몬스터 밸런스 검증 스크립트.

사용법:
  python3 verify_monster.py                          # monster.json 전체 CP·난이도 리포트
  python3 verify_monster.py --level 1 --difficulty 1.0   # 그 레벨 목표 CP(=플레이어CP×계수) 제시
  python3 verify_monster.py --json '{"id":"wolf","name":"늑대","type":"normal","level":3,
       "maxHp":80,"attackPower":60,"defense":5,"critical":40,"experience":40,
       "goldDrop":{"min":10,"max":25}}'              # 신규 후보 검증

몬스터 CP는 장비/캐릭터와 같은 공식(O×S)으로 산출 → 같은 스케일에서 비교.
'적정 난이도' = 그 레벨 플레이어 baseline CP(풀 초보장비) 대비 비율.
"""

import argparse
import json
import sys

import balance_core as bc

# 난이도 계수 → 라벨 (몬스터CP / 그 레벨 플레이어CP)
DIFFICULTY_BANDS = [
    (0.6, "매우 쉬움(잡몹)"),
    (0.9, "쉬움"),
    (1.2, "대등(정면 접전)"),
    (2.0, "강함(주의)"),
    (float("inf"), "보스급"),
]


def difficulty_label(ratio):
    for hi, label in DIFFICULTY_BANDS:
        if ratio < hi:
            return label
    return "보스급"


def player_baseline(level):
    """그 레벨 플레이어 baseline: 풀 초보장비(근접) 착용 추정."""
    base = bc.player_base_stats(level)
    main = base["STR"] + bc.STARTER_WEAPON_MAIN          # 한손검 STR+5
    atk = main * bc.TALENT_COEFF["MELEE"]
    deff = base["DEF"] + bc.STARTER_ARMOR_DEF            # 방어구 풀세트 +17
    hp = base["HP"]
    crit = base["CRITICAL"]
    return {
        "atk": atk, "deff": deff, "hp": hp, "crit": crit,
        "cp": bc.cp(atk, crit, hp, deff),
        "hit_basic": max(1.0, atk - 0),                  # 기본타(배율100%) 원피해(방어 전)
    }


def monster_metrics(m):
    cpb = bc.cp_breakdown(m["attackPower"], m.get("critical", 0), m["maxHp"], m.get("defense", 0))
    pl = player_baseline(m.get("level", 1))
    ratio = cpb["CP"] / pl["cp"] if pl["cp"] else 0.0
    # 기본타 기준 처치 턴(플레이어가 이 몬스터를 몇 턴에): 방어 차감 반영
    per_hit = max(1.0, pl["atk"] - m.get("defense", 0)) * bc.crit_factor(pl["crit"])
    ttk = m["maxHp"] / per_hit if per_hit else 0.0
    # 몬스터가 플레이어를 때리는 실피해(기본 100%)
    dmg_to_player = max(1.0, m["attackPower"] - pl["deff"])
    return {"cpb": cpb, "pl": pl, "ratio": ratio, "ttk": ttk, "dmg_to_player": dmg_to_player}


def print_monster(m):
    mm = monster_metrics(m)
    cpb, pl = mm["cpb"], mm["pl"]
    print(f"\n[{m['name']}] Lv{m.get('level')} ({m.get('type')})")
    print(f"  스탯: HP {m['maxHp']} / ATK {m['attackPower']} / DEF {m.get('defense',0)} "
          f"/ crit {m.get('critical',0)/10:.1f}% / exp {m.get('experience','-')} "
          f"/ gold {m.get('goldDrop',{}).get('min','?')}~{m.get('goldDrop',{}).get('max','?')}")
    print(f"  CP {bc.fmt(cpb['CP'])}  (O공격 {bc.fmt(cpb['O'])} / S생존 {bc.fmt(cpb['S'])})")
    print(f"  Lv{m.get('level')} 플레이어 baseline CP {bc.fmt(pl['cp'])} "
          f"(ATK {bc.fmt(pl['atk'])}, 유효DEF {pl['deff']}, HP {pl['hp']})")
    print(f"  난이도비 {bc.fmt(mm['ratio'],2)} → {difficulty_label(mm['ratio'])}")
    print(f"  플레이어 기본타 처치 예상 ≈ {bc.fmt(mm['ttk'])}턴 / "
          f"몬스터 일반공격 실피해 ≈ {bc.fmt(mm['dmg_to_player'])} (강공격 ×1.5)")


def warnings_for(m):
    warns = []
    mm = monster_metrics(m)
    pl = mm["pl"]
    if m["attackPower"] <= pl["deff"]:
        warns.append(f"attackPower({m['attackPower']}) ≤ 플레이어 유효DEF({pl['deff']}) "
                     f"→ 거의 1뎀만 줌(무의미). 유효DEF보다 위로.")
    if m["attackPower"] < pl["deff"] * 2:
        warns.append(f"attackPower가 유효DEF의 2배({pl['deff']*2}) 미만 → 실피해가 약함(권장 2~2.5배).")
    if mm["ttk"] < 2 and m.get("type") != "normal":
        warns.append(f"처치 예상 {bc.fmt(mm['ttk'])}턴 — 보스치고 너무 빨리 죽음(maxHp↑ 검토).")
    crit = m.get("critical", 0)
    if crit > 100 and m.get("type") == "normal":
        warns.append(f"critical {crit/10:.1f}% — 일반 몬스터치고 높음(보통 1~5%).")
    return warns


def suggest_targets(level, difficulty):
    pl = player_baseline(level)
    target_cp = pl["cp"] * difficulty
    print(f"\n=== Lv{level} 목표 제시 (난이도계수 {difficulty}) ===")
    print(f"  플레이어 baseline CP {bc.fmt(pl['cp'])} × {difficulty} = 목표 몬스터 CP ≈ {bc.fmt(target_cp)}")
    print(f"  권장 attackPower ≈ 유효DEF({pl['deff']})의 2~2.5배 = {pl['deff']*2}~{int(pl['deff']*2.5)}")
    print(f"  권장 maxHp ≈ 플레이어 1타({bc.fmt(pl['hit_basic'])})의 4~6배 = "
          f"{int(pl['hit_basic']*4)}~{int(pl['hit_basic']*6)} (일반 3~5턴 목표)")
    print(f"  → CP 공식으로 확인: 이 attackPower/maxHp 조합의 CP가 목표 CP({bc.fmt(target_cp)}) 근처인지 --json으로 검증.")


def validate_candidate(m):
    print(f"\n=== 신규 후보 검증 ===")
    print_monster(m)
    warns = warnings_for(m)
    if warns:
        print("  ⚠️ 경고:")
        for w in warns:
            print(f"    - {w}")
    else:
        print("  ✅ 규칙 위반 없음")


def main():
    ap = argparse.ArgumentParser(description="몬스터 밸런스 검증")
    ap.add_argument("--level", type=int, default=None, help="목표 제시용 레벨")
    ap.add_argument("--difficulty", type=float, default=1.0, help="난이도계수 (기본 1.0=대등)")
    ap.add_argument("--json", type=str, default=None, help="신규 후보 몬스터 JSON")
    args = ap.parse_args()

    monsters = bc.load_monsters()
    print("=== 현재 몬스터 ===")
    for m in sorted(monsters, key=lambda x: x.get("level", 0)):
        print_monster(m)

    if args.level is not None:
        suggest_targets(args.level, args.difficulty)

    if args.json:
        try:
            candidate = json.loads(args.json)
        except json.JSONDecodeError as e:
            print(f"\n[에러] --json 파싱 실패: {e}", file=sys.stderr)
            sys.exit(1)
        validate_candidate(candidate)


if __name__ == "__main__":
    main()
