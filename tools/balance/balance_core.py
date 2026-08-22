"""MyRPG 밸런스 검증 공통 모듈.

장비/몬스터/스킬 검증 스크립트가 공유하는 계산식·상수·데이터 로더를 모은다.

핵심 개념(자세한 배경은 skills/myrpg-data-balance/SKILL.md):
- CP(전투력) = sqrt(O * S)  — 스탯 기반 전투 잠재력 (무기/방어구/몬스터/캐릭터 공통 스케일)
    O(공격) = 공격력 * 크리계수
    S(생존) = HP * A_REF / max(1, A_REF - DEF)
  고정 상수(D_REF=0, A_REF=100, K=1)로 레벨/스킬과 무관하게 결정론적으로 산출.
- SP(딜지수) = 명목총배율 * 크리계수 * 캐스팅성공률 * 재능계수  — 스킬(딜) 예산 잣대.
- 스킬은 CP에 넣지 않고 SP(딜)/방어가치(방어)로 따로 관리한다.
  실전 파워 ≈ CP(스탯) * (SP/100)(스킬). 두 축은 곱.
"""

import json
import math
import os

# ─── 고정 상수 ──────────────────────────────────────────────────────────────
A_REF = 100          # 생존 지표(EHP)용 기준 공격력
D_REF = 0            # 공격 지표용 기준 방어 (CP에는 방어 절벽 미반영 — 매치업 검증은 별도)
K = 1.0              # CP 스케일 계수

# 재능계수 (공격력에 곱)
TALENT_COEFF = {"MELEE": 1.0, "ARCHERY": 0.85, "MAGIC": 1.2}

# 무기 kind → (talent, 주스탯)
KIND_TO_TALENT = {
    "one_handed_sword": ("MELEE", "STR"),
    "two_handed_sword": ("MELEE", "STR"),
    "bow": ("ARCHERY", "DEX"),
    "wand": ("MAGIC", "INT"),
    "staff": ("MAGIC", "INT"),
}
TWO_HANDED_KINDS = {"two_handed_sword", "bow", "staff"}

# 판매가/수리비 대상별 가중치 (CRITICAL은 0.1%단위라 1/10 보정)
WEIGHT_DEFAULT = 10
WEIGHT_BY_TARGET = {"CRITICAL": 1}
SELL_RATIO = 0.5     # 상점 판매 아이템(buyPrice 有) 판매가 = buyPrice * 0.5

# 스킬 랭크 16키 (F → MASTER, 단조 비감소여야 함)
RANK_KEYS = ["F", "E", "D", "C", "B", "A", "R9", "R8",
             "R7", "R6", "R5", "R4", "R3", "R2", "R1", "MASTER"]

# 몬스터 방어 상수 기본값
DEFAULT_BLOCK_RATE = 100
DEFAULT_COUNTER_RATE = 0

# 초보 풀장비 DEF 합(방패5+갑옷5+투구3+장갑2+부츠2) — 플레이어 baseline 유효 DEF 추정용
STARTER_ARMOR_DEF = 17
# 초보 무기 주스탯(한손검 STR+5) — baseline 공격력 추정용
STARTER_WEAPON_MAIN = 5


# ─── 데이터 로더 ────────────────────────────────────────────────────────────
def _data_dir():
    here = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.dirname(os.path.dirname(here))   # tools/balance → repo
    return os.path.join(repo_root, "myrpg", "src", "main", "resources", "data")


def load_json(filename):
    with open(os.path.join(_data_dir(), filename), encoding="utf-8") as f:
        return json.load(f)


def load_items():
    return load_json("item.json")


def load_monsters():
    return load_json("monster.json")


def load_skills():
    return load_json("skill.json")


# ─── 플레이어 성장 기준선 (StatProgression 공식, 재능 보너스 제외 하한) ──────
def player_base_stats(level):
    """레벨별 기본 스탯(장비·재능 보너스 제외). crit은 0.1% 단위 정수."""
    d = level - 1
    return {
        "STR": 10 + 3 * d, "DEX": 10 + 3 * d, "INT": 10 + 3 * d,
        "DEF": 5 + 1 * d, "HP": 100 + 10 * d, "CRITICAL": 50 + 3 * d,
    }


# ─── CP 계산 ────────────────────────────────────────────────────────────────
def crit_factor(crit_permille):
    """크리 기대 배수 = 1 + 크리확률 * 0.5 (크리 시 ×1.5)."""
    return 1.0 + (crit_permille / 1000.0) * 0.5


def offense(attack_power, crit_permille):
    """O(공격) = 공격력 * 크리계수. (기준 방어 D_REF=0)"""
    return max(0.0, attack_power - D_REF) * crit_factor(crit_permille)


def survival(hp, deff):
    """S(생존) = HP * A_REF / max(1, A_REF - DEF)."""
    return hp * A_REF / max(1.0, A_REF - deff)


def cp(attack_power, crit_permille, hp, deff):
    """전투력 CP = K * sqrt(O * S)."""
    o = offense(attack_power, crit_permille)
    s = survival(hp, deff)
    return K * math.sqrt(o * s)


def cp_breakdown(attack_power, crit_permille, hp, deff):
    o = offense(attack_power, crit_permille)
    s = survival(hp, deff)
    return {"O": o, "S": s, "CP": K * math.sqrt(o * s)}


# ─── 판매가·수리비 (data-balance-guide §C-3) ────────────────────────────────
def weight_of(target):
    return WEIGHT_BY_TARGET.get(target, WEIGHT_DEFAULT)


def base_value(item):
    """기본가(인챈트 전 고유 판매가). buyPrice 있으면 buyPrice*0.5, 없으면 카탈로그 보너스*가중치."""
    buy = item.get("buyPrice")
    if buy is not None:
        return round(buy * SELL_RATIO)
    total = 0
    for b in item.get("bonuses", []):
        total += b["amount"] * weight_of(b["target"])
    return total


def sell_value(item):
    """판매가 = 기본가 + 인스턴스보너스(인챈트, 현재 0)."""
    return base_value(item)


def repair_cost_per_point(item):
    """1포인트 수리비 = 판매가."""
    return sell_value(item)


# ─── 스킬 딜지수 SP ─────────────────────────────────────────────────────────
def cast_success(talent):
    return 0.9 if talent == "MAGIC" else 1.0


def skill_sp(skill, rank, ref_crit=50):
    """딜 스킬의 기대 딜지수(맨손 1.0타 대비 배). ref_crit=참조 캐릭 크리(0.1%단위)."""
    mult = skill["multiplierByRank"][rank]
    nominal = mult * skill.get("hitCount", 1)          # 명목 총배율
    cf = crit_factor(ref_crit + skill.get("critBonus", 0))
    coeff = TALENT_COEFF[skill["talent"]]
    return nominal * cf * cast_success(skill["talent"]) * coeff


# ─── 검증 유틸 ──────────────────────────────────────────────────────────────
def check_rank_map(rank_map):
    """16키 완비 + 단조 비감소 검사. (문제 목록 반환, 없으면 빈 리스트)"""
    problems = []
    missing = [k for k in RANK_KEYS if k not in rank_map]
    if missing:
        problems.append(f"랭크키 누락: {missing}")
    prev = None
    for k in RANK_KEYS:
        if k not in rank_map:
            continue
        v = rank_map[k]
        if prev is not None and v < prev:
            problems.append(f"단조 비감소 위반: {k}={v} < 직전 {prev}")
        prev = v
    return problems


def fmt(x, nd=1):
    return f"{x:.{nd}f}"
