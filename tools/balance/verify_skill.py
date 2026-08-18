#!/usr/bin/env python3
"""스킬 밸런스 검증 스크립트.

사용법:
  python3 verify_skill.py                     # skill.json 전체 SP밴드/방어밴드 리포트 + 규칙검증
  python3 verify_skill.py --ref-crit 50       # SP 계산용 참조 크리(0.1%단위, 기본 50=5%)
  python3 verify_skill.py --json '{...}'       # 신규 후보 스킬 검증

딜 스킬은 SP(기대딜지수)=명목총배율×크리계수×캐스팅성공×재능계수 로 예산 비교.
방어 스킬은 경감률↔반격률 밴드로 검증(반격 상한 50).
모든 스킬: 랭크 16키 완비 + 단조 비감소 필수.
"""

import argparse
import json
import sys

import balance_core as bc

# 명목 총배율(=히트당배율×hitCount) 소프트 밴드 (F, MASTER) — 벗어나면 경고
NOMINAL_BAND = {
    "NORMAL": {"F": (90, 110), "MASTER": (170, 205)},
    "HEAVY": {"F": (130, 145), "MASTER": (250, 265)},
}
CRITBONUS_CAP = 100      # +10%p
COUNTER_CAP = 50         # 반격율 상한 %


def nominal_total(skill, rank):
    return skill["multiplierByRank"][rank] * skill.get("hitCount", 1)


def is_dealer(skill):
    return skill.get("type") in ("NORMAL", "HEAVY")


def report_dealers(skills, ref_crit):
    dealers = [s for s in skills if is_dealer(s)]
    print(f"\n=== 딜 스킬 (SP=기대딜지수, 참조크리 {ref_crit/10:.1f}%) ===")
    print(f"{'스킬':<16}{'재능':<9}{'type':<8}{'히트':>4}{'critB':>7}"
          f"{'총배율F':>8}{'총배율M':>8}{'SP(F)':>8}{'SP(M)':>8}")
    # 재능→type 순 정렬, SP(M) 표시
    for s in sorted(dealers, key=lambda x: (x["talent"], x["type"] != "NORMAL", -bc.skill_sp(x, "MASTER", ref_crit))):
        nf = nominal_total(s, "F")
        nm = nominal_total(s, "MASTER")
        spf = bc.skill_sp(s, "F", ref_crit)
        spm = bc.skill_sp(s, "MASTER", ref_crit)
        print(f"{s['label']:<16}{s['talent']:<9}{s['type']:<8}{s.get('hitCount',1):>4}"
              f"{s.get('critBonus',0):>7}{nf:>8}{nm:>8}{bc.fmt(spf):>8}{bc.fmt(spm):>8}")


def report_defenses(skills):
    defs = [s for s in skills if s.get("type") == "DEFENSE"]
    if not defs:
        return
    print(f"\n=== 방어 스킬 (경감률 / 반격률, 상한 반격 {COUNTER_CAP}%) ===")
    print(f"{'스킬':<16}{'경감 F→M':>14}{'반격 F→M':>14}")
    for s in defs:
        b = s["blockRateByRank"]
        c = s["counterMultiplierByRank"]
        block_s = f"{b['F']}→{b['MASTER']}"
        counter_s = f"{c['F']}→{c['MASTER']}"
        print(f"{s['label']:<16}{block_s:>14}{counter_s:>14}")


def validate_skill(s):
    """스킬 하나의 규칙 위반 목록 반환."""
    warns = []
    stype = s.get("type")

    if stype in ("NORMAL", "HEAVY"):
        # 랭크맵 16키/단조
        for p in bc.check_rank_map(s.get("multiplierByRank", {})):
            warns.append(f"multiplierByRank: {p}")
        # 명목 총배율 밴드
        band = NOMINAL_BAND.get(stype)
        if band:
            for rk in ("F", "MASTER"):
                if rk in s.get("multiplierByRank", {}):
                    val = nominal_total(s, rk)
                    lo, hi = band[rk]
                    if not (lo <= val <= hi):
                        warns.append(f"명목 총배율 {rk}={val} 이 {stype} 밴드 {lo}~{hi} 밖")
        # critBonus 상한 / 마법 0
        cb = s.get("critBonus", 0)
        if cb > CRITBONUS_CAP:
            warns.append(f"critBonus {cb} > 상한 {CRITBONUS_CAP}")
        if s.get("talent") == "MAGIC" and cb != 0:
            warns.append(f"마법 스킬 critBonus는 0이어야 함(현재 {cb})")
        # 2축 차별화 힌트: 단일+critBonus0+표준배율이면 기본형
    elif stype == "DEFENSE":
        for p in bc.check_rank_map(s.get("blockRateByRank", {})):
            warns.append(f"blockRateByRank: {p}")
        for p in bc.check_rank_map(s.get("counterMultiplierByRank", {})):
            warns.append(f"counterMultiplierByRank: {p}")
        cmax = s.get("counterMultiplierByRank", {}).get("MASTER")
        if cmax is not None and cmax > COUNTER_CAP:
            warns.append(f"반격률 MASTER {cmax} > 상한 {COUNTER_CAP}%")
        if s.get("talent") != "COMMON":
            warns.append("방어 스킬 talent은 COMMON 권장")
    else:
        warns.append(f"알 수 없는 type: {stype}")

    return warns


def validate_all(skills):
    print("\n=== 규칙 검증(현재 스킬) ===")
    ok = True
    for s in skills:
        w = validate_skill(s)
        if w:
            ok = False
            print(f"  [{s.get('label', s.get('id'))}] ⚠️")
            for x in w:
                print(f"    - {x}")
    if ok:
        print("  ✅ 전 스킬 규칙 위반 없음")


def validate_candidate(s, skills, ref_crit):
    print(f"\n=== 신규 후보 검증: {s.get('label', s.get('id','(이름없음)'))} ===")
    if is_dealer(s):
        spf = bc.skill_sp(s, "F", ref_crit)
        spm = bc.skill_sp(s, "MASTER", ref_crit)
        print(f"  {s['talent']} {s['type']} / 히트 {s.get('hitCount',1)} / critBonus {s.get('critBonus',0)}")
        print(f"  명목총배율 F {nominal_total(s,'F')} → M {nominal_total(s,'MASTER')}")
        print(f"  SP F {bc.fmt(spf)} → MASTER {bc.fmt(spm)}")
        # 같은 재능·type SP 순위
        peers = [x for x in skills if x["talent"] == s["talent"] and x["type"] == s["type"] and is_dealer(x)]
        ranked = sorted(peers + [s], key=lambda x: bc.skill_sp(x, "MASTER", ref_crit))
        names = [f"{x.get('label','?')}({bc.fmt(bc.skill_sp(x,'MASTER',ref_crit))})" for x in ranked]
        print(f"  같은 재능·{s['type']} SP(M) 순위: " + " < ".join(names))
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
