#!/usr/bin/env python3
"""스킬 밸런스 및 도메인 정합성 검증 스크립트.

사용법:
  python3 verify_skill.py                     # skill.json 전체 8종 스킬 16키/단조성/규칙 검증
  python3 verify_skill.py --ref-crit 50       # SP 계산용 참조 크리(0.1%단위, 기본 50=5%)
  python3 verify_skill.py --json '{...}'       # 신규 후보 스킬 검증

지원 스킬 도메인 8종:
- NORMAL / HEAVY / DEBUFF (딜/디버프 스킬)
- DEFENSE (방어 스킬)
- RECOVERY (회복 스킬)
- ULTIMATE (결전 궁극기)
- BUFF (버프 스킬)
- CC (군중 제어 스킬)
- DOT (지속 피해 스킬)
- PASSIVE (영구 패시브 스킬)
"""

import argparse
import json
import sys

import balance_core as bc

CRITBONUS_CAP = 100          # +10%p (딜/궁극기 스킬 기본 상한)
DEFENSE_CRITBONUS_CAP = 200  # +20%p (카운터 어택 전용 상한)
COUNTER_CAP = 200            # 반격율 상한 %

VALID_STAT_TARGETS = {
    "STR", "DEX", "INT", "DEF", "HP", "MP", "STAMINA", "CRITICAL", "MP_REGEN"
}


def nominal_total(skill, rank):
    return skill["multiplierByRank"][rank] * skill.get("hitCount", 1)


def is_dealer(skill):
    return skill.get("type") in ("NORMAL", "HEAVY", "DEBUFF", "ULTIMATE")


def report_dealers(skills, ref_crit):
    dealers = [s for s in skills if is_dealer(s)]
    print(f"\n=== 공격 및 궁극기 스킬 (SP=기대딜지수, 참조크리 {ref_crit/10:.1f}%) ===")
    print(f"{'스킬':<16}{'재능':<9}{'type':<9}{'히트':>4}{'critB':>7}"
          f"{'총배율F':>8}{'총배율M':>8}{'SP(F)':>8}{'SP(M)':>8}")
    for s in sorted(dealers, key=lambda x: (x["talent"], x["type"], -bc.skill_sp(x, "MASTER", ref_crit))):
        nf = nominal_total(s, "F")
        nm = nominal_total(s, "MASTER")
        spf = bc.skill_sp(s, "F", ref_crit)
        spm = bc.skill_sp(s, "MASTER", ref_crit)
        print(f"{s['label']:<16}{s['talent']:<9}{s['type']:<9}{s.get('hitCount',1):>4}"
              f"{s.get('critBonus',0):>7}{nf:>8}{nm:>8}{bc.fmt(spf):>8}{bc.fmt(spm):>8}")


def report_defenses(skills):
    defs = [s for s in skills if s.get("type") == "DEFENSE"]
    if not defs:
        return
    print(f"\n=== 방어 스킬 (경감률 / 반격률) ===")
    print(f"{'스킬':<16}{'경감 F→M':>14}{'반격 F→M':>14}{'소모 F→M':>12}{'critB F→M':>14}")
    for s in defs:
        b = s["blockRateByRank"]
        c = s["counterMultiplierByRank"]
        block_s = f"{b['F']}%→{b['MASTER']}%"
        counter_s = f"{c['F']}%→{c['MASTER']}%"
        cost_s = f"{s['resourceCostByRank']['F']}→{s['resourceCostByRank']['MASTER']}" if "resourceCostByRank" in s else f"{s.get('resourceCost','?')}"
        crit_s = f"+{s['critBonusByRank']['F']/10:.1f}→+{s['critBonusByRank']['MASTER']/10:.1f}%p" if "critBonusByRank" in s else f"{s.get('critBonus',0)}"
        print(f"{s['label']:<16}{block_s:>14}{counter_s:>14}{cost_s:>12}{crit_s:>14}")


def report_specials(skills):
    specs = [s for s in skills if s.get("type") in ("RECOVERY", "BUFF", "CC", "DOT", "PASSIVE")]
    if not specs:
        return
    print(f"\n=== 지원 / 특수 / 패시브 스킬 ===")
    print(f"{'스킬':<18}{'type':<10}{'주요 수치 (F → MASTER / 총량)':<40}")
    for s in sorted(specs, key=lambda x: x["type"]):
        stype = s.get("type")
        if stype == "RECOVERY":
            val = f"회복: {s['healAmountByRank']['F']}HP → {s['healAmountByRank']['MASTER']}HP (소모: {s['resourceCostByRank']['F']}→{s['resourceCostByRank']['MASTER']}MP)"
        elif stype == "BUFF":
            val = f"흡수: {s['absorbRateByRank']['F']}% → {s['absorbRateByRank']['MASTER']}% (지속: {s.get('durationTurns',5)}턴)"
        elif stype == "CC":
            val = f"성공률: {s['successRateByRank']['F']}% → {s['successRateByRank']['MASTER']}% (지속: 1턴)"
        elif stype == "DOT":
            val = f"초기: {s['initialMultiplierByRank']['F']}% / 지속: {s['dotTurnsByRank']['F']}→{s['dotTurnsByRank']['MASTER']}턴 / 틱당: {s['dotPerTurnByRank']['F']}→{s['dotPerTurnByRank']['MASTER']}%"
        elif stype == "PASSIVE":
            bonuses = ", ".join(f"{k} +{v}" for k, v in s.get("statBonusTotal", {}).items())
            val = f"최종 보너스: {bonuses}"
        else:
            val = "-"
        print(f"{s['label']:<18}{stype:<10}{val:<40}")


def validate_skill(s):
    """스킬 하나의 16키 및 도메인 규칙 위반 목록 반환."""
    warns = []
    stype = s.get("type")

    if stype in ("NORMAL", "HEAVY", "DEBUFF", "ULTIMATE"):
        for p in bc.check_rank_map(s.get("multiplierByRank", {})):
            warns.append(f"multiplierByRank: {p}")
        cb = s.get("critBonus", 0)
        if cb > CRITBONUS_CAP:
            warns.append(f"critBonus {cb} > 상한 {CRITBONUS_CAP}")
        if s.get("talent") == "MAGIC" and cb != 0 and stype != "ULTIMATE":
            warns.append(f"마법 공격 스킬 critBonus는 0이어야 함(현재 {cb})")
        if stype == "ULTIMATE":
            cool = s.get("coolWins") or s.get("coolWinsByRank")
            if not cool:
                warns.append("궁극기 쿨다운(coolWins) 정의 누락")

    elif stype == "DEFENSE":
        for p in bc.check_rank_map(s.get("blockRateByRank", {})):
            warns.append(f"blockRateByRank: {p}")
        for p in bc.check_rank_map(s.get("counterMultiplierByRank", {})):
            warns.append(f"counterMultiplierByRank: {p}")
        if "resourceCostByRank" in s:
            missing = [k for k in bc.RANK_KEYS if k not in s["resourceCostByRank"]]
            if missing:
                warns.append(f"resourceCostByRank 랭크키 누락: {missing}")
            prev = None
            for k in bc.RANK_KEYS:
                if k not in s["resourceCostByRank"]:
                    continue
                v = s["resourceCostByRank"][k]
                if prev is not None and v > prev:
                    warns.append(f"resourceCostByRank 단조 비증가 위반: {k}={v} > 직전 {prev}")
                prev = v
        if "critBonusByRank" in s:
            for p in bc.check_rank_map(s.get("critBonusByRank", {})):
                warns.append(f"critBonusByRank: {p}")
            cb_max = s.get("critBonusByRank", {}).get("MASTER")
            if cb_max is not None and cb_max > DEFENSE_CRITBONUS_CAP:
                warns.append(f"critBonusByRank MASTER {cb_max} > 상한 {DEFENSE_CRITBONUS_CAP} (+20%p)")
        cmax = s.get("counterMultiplierByRank", {}).get("MASTER")
        if cmax is not None and cmax > COUNTER_CAP:
            warns.append(f"반격률 MASTER {cmax} > 상한 {COUNTER_CAP}%")

    elif stype == "RECOVERY":
        for p in bc.check_rank_map(s.get("healAmountByRank", {})):
            warns.append(f"healAmountByRank: {p}")
        if "resourceCostByRank" in s:
            missing = [k for k in bc.RANK_KEYS if k not in s["resourceCostByRank"]]
            if missing:
                warns.append(f"resourceCostByRank 랭크키 누락: {missing}")
            prev = None
            for k in bc.RANK_KEYS:
                if k not in s["resourceCostByRank"]:
                    continue
                v = s["resourceCostByRank"][k]
                if prev is not None and v > prev:
                    warns.append(f"resourceCostByRank 단조 비증가 위반: {k}={v} > 직전 {prev}")
                prev = v

    elif stype == "BUFF":
        for p in bc.check_rank_map(s.get("absorbRateByRank", {})):
            warns.append(f"absorbRateByRank: {p}")
        if s.get("durationTurns", 0) <= 0:
            warns.append("durationTurns는 1 이상이어야 함")

    elif stype == "CC":
        for p in bc.check_rank_map(s.get("successRateByRank", {})):
            warns.append(f"successRateByRank: {p}")

    elif stype == "DOT":
        for p in bc.check_rank_map(s.get("dotTurnsByRank", {})):
            warns.append(f"dotTurnsByRank: {p}")
        turns = s.get("dotTurnsByRank", {})
        per_turn = s.get("dotPerTurnByRank", {})
        total_dot_map = {}
        for k in bc.RANK_KEYS:
            if k in turns and k in per_turn:
                total_dot_map[k] = turns[k] * per_turn[k]
        for p in bc.check_rank_map(total_dot_map):
            warns.append(f"총 도트 피해량(dotTurns*dotPerTurn): {p}")
        if "initialMultiplierByRank" not in s:
            warns.append("initialMultiplierByRank 정의 누락")

    elif stype == "PASSIVE":
        bonus = s.get("statBonusTotal")
        if not bonus or not isinstance(bonus, dict):
            warns.append("statBonusTotal 정의 누락 또는 딕셔너리아님")
        else:
            for k, v in bonus.items():
                if k not in VALID_STAT_TARGETS:
                    warns.append(f"statBonusTotal 알 수 없는 스탯 타겟: {k}")
                if v <= 0:
                    warns.append(f"statBonusTotal {k} 값이 0 이하: {v}")

    else:
        warns.append(f"알 수 없는 type: {stype}")

    return warns


def validate_all(skills):
    print("\n=== 규칙 검증(현재 전체 31종 스킬) ===")
    ok = True
    for s in skills:
        w = validate_skill(s)
        if w:
            ok = False
            print(f"  [{s.get('label', s.get('id'))}] ⚠️")
            for x in w:
                print(f"    - {x}")
    if ok:
        print("  ✅ 전체 31종 스킬 규칙 위반 없음 (100% 통과)")


def validate_candidate(s, skills, ref_crit):
    print(f"\n=== 신규 후보 검증: {s.get('label', s.get('id','(이름없음)'))} ===")
    if is_dealer(s):
        spf = bc.skill_sp(s, "F", ref_crit)
        spm = bc.skill_sp(s, "MASTER", ref_crit)
        print(f"  {s['talent']} {s['type']} / 히트 {s.get('hitCount',1)} / critBonus {s.get('critBonus',0)}")
        print(f"  명목총배율 F {nominal_total(s,'F')} → M {nominal_total(s,'MASTER')}")
        print(f"  SP F {bc.fmt(spf)} → MASTER {bc.fmt(spm)}")
    elif s.get("type") == "DEFENSE":
        b = s.get("blockRateByRank", {})
        c = s.get("counterMultiplierByRank", {})
        print(f"  경감 {b.get('F','?')}→{b.get('MASTER','?')} / 반격 {c.get('F','?')}→{c.get('MASTER','?')}")

    warns = validate_skill(s)
    if warns:
        print("  ⚠️ 경고:")
        for w in warns:
            print(f"    - {w}")
    else:
        print("  ✅ 규칙 위반 없음")


def main():
    ap = argparse.ArgumentParser(description="스킬 밸런스 검증")
    ap.add_argument("--ref-crit", type=int, default=50, help="SP 계산용 참조 크리(0.1%단위, 기본 50)")
    ap.add_argument("--json", type=str, default=None, help="신규 후보 스킬 JSON")
    args = ap.parse_args()

    skills = bc.load_skills()
    report_dealers(skills, args.ref_crit)
    report_defenses(skills)
    report_specials(skills)
    validate_all(skills)

    if args.json:
        try:
            candidate = json.loads(args.json)
        except json.JSONDecodeError as e:
            print(f"\n[에러] --json 파싱 실패: {e}", file=sys.stderr)
            sys.exit(1)
        validate_candidate(candidate, skills, args.ref_crit)


if __name__ == "__main__":
    main()
