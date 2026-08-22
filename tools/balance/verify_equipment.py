#!/usr/bin/env python3
"""장비(무기/방어구) 밸런스 검증 스크립트.

사용법:
  python3 verify_equipment.py                 # 현재 item.json 장비 전체를 CP/판매가로 리포트
  python3 verify_equipment.py --level 1       # 기준 캐릭 레벨 지정(기본 1)
  python3 verify_equipment.py --json '{"id":"long_sword","name":"롱소드","type":"weapon",
       "kind":"one_handed_sword","bonuses":[{"target":"STR","amount":12}],
       "maxDurability":15,"buyPrice":700}'    # 신규 후보를 기존과 비교·검증

CP는 "그 재능 기본 캐릭(레벨별 StatProgression) + 해당 장비만 착용" 기준.
- 무기: 공격력(주스탯)을 올려 CP↑ → ΔCP가 곧 무기의 딜 기여.
- 방어구: DEF/HP를 올려 생존(EHP)↑ → ΔCP + EHP로 본다.
판매가/수리비는 확정 규칙(대상별 가중 CRITICAL=1, buyPrice×0.5 배타) 그대로.
"""

import argparse
import json
import sys

import balance_core as bc


def weapon_metrics(item, level):
    kind = item.get("kind")
    if kind not in bc.KIND_TO_TALENT:
        return None
    talent, main_stat = bc.KIND_TO_TALENT[kind]
    coeff = bc.TALENT_COEFF[talent]
    base = bc.player_base_stats(level)

    main_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == main_stat)
    crit_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == "CRITICAL")
    def_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == "DEF")

    main_total = base[main_stat] + main_bonus
    crit_total = base["CRITICAL"] + crit_bonus
    atk = main_total * coeff
    deff = base["DEF"] + def_bonus
    hp = base["HP"]

    cp_val = bc.cp(atk, crit_total, hp, deff)
    bare_atk = base[main_stat] * coeff
    bare_cp = bc.cp(bare_atk, base["CRITICAL"], hp, base["DEF"])
    return {
        "talent": talent, "main_stat": main_stat, "coeff": coeff,
        "main_total": main_total, "crit_total": crit_total, "atk": atk,
        "cp": cp_val, "dcp": cp_val - bare_cp,
    }


def armor_metrics(item, level):
    # 방어구는 재능 무관 → 중립 근접 기본 캐릭에 얹어 생존 기여를 본다.
    base = bc.player_base_stats(level)
    def_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == "DEF")
    hp_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == "HP")

    deff = base["DEF"] + def_bonus
    hp = base["HP"] + hp_bonus
    atk = base["STR"] * bc.TALENT_COEFF["MELEE"]

    cp_val = bc.cp(atk, base["CRITICAL"], hp, deff)
    bare_cp = bc.cp(atk, base["CRITICAL"], base["HP"], base["DEF"])
    return {
        "def_bonus": def_bonus, "def_total": deff, "hp_total": hp,
        "ehp": bc.survival(hp, deff), "cp": cp_val, "dcp": cp_val - bare_cp,
    }


def bonus_str(item):
    parts = [f"{b['target']}+{b['amount']}" for b in item.get("bonuses", [])]
    return ", ".join(parts) if parts else "-"


def price_str(item):
    buy = item.get("buyPrice")
    sv = bc.sell_value(item)
    origin = "상점" if buy is not None else "드랍전용"
    buy_s = str(buy) if buy is not None else "-"
    return f"구매 {buy_s:>5} / 판매·수리 {sv:>4} ({origin})"


def report_weapons(items, level):
    weapons = [i for i in items if i.get("type") == "weapon"]
    print(f"\n=== 무기 (기준 캐릭 Lv{level}, 무기만 착용) ===")
    print(f"{'이름':<16}{'kind':<18}{'재능':<8}{'보너스':<22}{'공격력':>7}{'CP':>7}{'ΔCP':>7}  {'내구':>4}  가격")
    # kind 그룹별 CP 오름차순 정렬
    for kind in sorted({w.get('kind') for w in weapons}):
        group = [w for w in weapons if w.get("kind") == kind]
        rows = []
        for w in group:
            m = weapon_metrics(w, level)
            rows.append((m["cp"], w, m))
        rows.sort(key=lambda r: r[0])
        for cp_val, w, m in rows:
            print(f"{w['name']:<16}{w.get('kind',''):<18}{m['talent']:<8}{bonus_str(w):<22}"
                  f"{bc.fmt(m['atk']):>7}{bc.fmt(m['cp']):>7}{('+'+bc.fmt(m['dcp'])):>7}"
                  f"  {str(w.get('maxDurability','-')):>4}  {price_str(w)}")


def report_armors(items, level):
    armors = [i for i in items if i.get("type") == "armor"]
    print(f"\n=== 방어구 (기준 캐릭 Lv{level}, 방어구만 착용) ===")
    print(f"{'이름':<16}{'kind':<14}{'보너스':<12}{'DEF합':>6}{'EHP':>8}{'CP':>7}{'ΔCP':>7}  {'내구':>4}  가격")
    for a in sorted(armors, key=lambda x: armor_metrics(x, level)["cp"]):
        m = armor_metrics(a, level)
        print(f"{a['name']:<16}{a.get('kind',''):<14}{bonus_str(a):<12}"
              f"{str(m['def_total']):>6}{bc.fmt(m['ehp']):>8}{bc.fmt(m['cp']):>7}{('+'+bc.fmt(m['dcp'])):>7}"
              f"  {str(a.get('maxDurability','-')):>4}  {price_str(a)}")


def report_potions(items):
    potions = [i for i in items if i.get("type") == "potion"]
    if not potions:
        return
    print(f"\n=== 포션 ===")
    for p in potions:
        effects = []
        if p.get("healHp"):
            effects.append(f"healHp={p['healHp']}")
        if p.get("healMp"):
            effects.append(f"healMp={p['healMp']}")
        if p.get("healStamina"):
            effects.append(f"healStamina={p['healStamina']}")
        effects_str = ", ".join(effects) if effects else "-"
        print(f"{p['name']:<16} {effects_str:<24} {price_str(p)}")


def validate_candidate(item, items, level):
    print(f"\n=== 신규 후보 검증: {item.get('name','(이름없음)')} ===")
    warnings = []
    itype = item.get("type")

    if item.get("maxDurability") is None and itype in ("weapon", "armor"):
        warnings.append("maxDurability 누락 (장비 필수 필드)")

    if itype == "weapon":
        kind = item.get("kind")
        if kind not in bc.KIND_TO_TALENT:
            warnings.append(f"알 수 없는 무기 kind: {kind}")
        else:
            talent, main_stat = bc.KIND_TO_TALENT[kind]
            m = weapon_metrics(item, level)
            print(f"  재능 {talent} / 주스탯 {main_stat} / 공격력 {bc.fmt(m['atk'])} "
                  f"→ CP {bc.fmt(m['cp'])} (ΔCP +{bc.fmt(m['dcp'])})")
            # 주스탯 일치 검사
            main_bonus = sum(b["amount"] for b in item.get("bonuses", []) if b["target"] == main_stat)
            if main_bonus == 0:
                warnings.append(f"주스탯({main_stat}) 보너스가 없음 — 이 재능 무기는 {main_stat}를 올려야 공격력 기여")
            other_main = [b["target"] for b in item.get("bonuses", [])
                          if b["target"] in ("STR", "DEX", "INT") and b["target"] != main_stat]
            if other_main:
                warnings.append(f"주스탯 외 스탯 보너스({other_main})는 이 재능 공격력에 기여 안 함")
            has_crit = any(b["target"] == "CRITICAL" for b in item.get("bonuses", []))
            if has_crit and kind != "bow":
                warnings.append("CRITICAL 보너스는 활(bow) 전용 특성 — 다른 무기에 부여 주의")
            if kind == "bow" and not has_crit:
                warnings.append("활은 보통 CRITICAL 보너스로 '활=크리↑' 특성을 부여 (검토)")
            # 같은 kind 랭킹 위치
            same = [w for w in items if w.get("type") == "weapon" and w.get("kind") == kind]
            ranked = sorted(same + [item], key=lambda w: weapon_metrics(w, level)["cp"])
            names = [f"{w['name']}({bc.fmt(weapon_metrics(w, level)['cp'])})" for w in ranked]
            print("  같은 kind CP 순위: " + " < ".join(names))

    elif itype == "armor":
        m = armor_metrics(item, level)
        print(f"  DEF합 {m['def_total']} / EHP {bc.fmt(m['ehp'])} → CP {bc.fmt(m['cp'])} (ΔCP +{bc.fmt(m['dcp'])})")
        same = [a for a in items if a.get("type") == "armor" and a.get("kind") == item.get("kind")]
        ranked = sorted(same + [item], key=lambda a: armor_metrics(a, level)["cp"])
        names = [f"{a['name']}({bc.fmt(armor_metrics(a, level)['cp'])})" for a in ranked]
        print("  같은 슬롯 CP 순위: " + " < ".join(names))

    elif itype == "potion":
        effects = []
        if item.get("healHp"):
            effects.append(f"healHp={item['healHp']}")
        if item.get("healMp"):
            effects.append(f"healMp={item['healMp']}")
        if item.get("healStamina"):
            effects.append(f"healStamina={item['healStamina']}")
        print(f"  {', '.join(effects)} / {price_str(item)}")
    else:
        warnings.append(f"알 수 없는 type: {itype}")

    print(f"  가격: {price_str(item)}")
    if warnings:
        print("  ⚠️ 경고:")
        for w in warnings:
            print(f"    - {w}")
    else:
        print("  ✅ 규칙 위반 없음")


def main():
    ap = argparse.ArgumentParser(description="장비 밸런스 검증")
    ap.add_argument("--level", type=int, default=1, help="기준 캐릭 레벨 (기본 1)")
    ap.add_argument("--json", type=str, default=None, help="신규 후보 아이템 JSON")
    args = ap.parse_args()

    items = bc.load_items()
    report_weapons(items, args.level)
    report_armors(items, args.level)
    report_potions(items)

    if args.json:
        try:
            candidate = json.loads(args.json)
        except json.JSONDecodeError as e:
            print(f"\n[에러] --json 파싱 실패: {e}", file=sys.stderr)
            sys.exit(1)
        validate_candidate(candidate, items, args.level)


if __name__ == "__main__":
    main()
