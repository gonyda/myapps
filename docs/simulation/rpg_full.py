"""전체 진행 시뮬레이션 (장비 드랍/스킬 포함).
가정: 메인 무기 = SWORD(밸런스), 마을 복귀 시 HP 전량 회복, 포션 사용, Lv 무관 각 던전 보스는 stage5에서 항상 도전.
장비: 보스=무기15%/스킬북15%, 일반몹=방어구5%. 등급/수치는 던전 gradeChance·itemLevel로 롤.
세대1은 무기타입별 스킬 1종뿐(중복 장착 불가) → 무기는 사실상 기본공격+해당 타입 스킬 1개."""
import random

RL, RH, CRIT = 0.9, 1.1, 1.5
BASE = dict(hp=100, mp=50, atk=10, deff=5, spd=5, crit=0)
GAIN = dict(hp=20, mp=10, atk=3, deff=2, spd=1, crit=1)
HP_HEAL, HP_COST = 50, 30

GRADE_BONUS = dict(C=0, U=2, R=5, E=8, L=10)
GRADE_SLOTS = dict(C=1, U=2, R=3, E=4, L=5)
# statcount 분포: (개수, 확률)
STAT_DIST = {
 'C': [(1,1.0)],
 'U': [(1,0.6),(2,0.4)],
 'R': [(2,0.6),(3,0.4)],
 'E': [(3,0.6),(4,0.4)],
 'L': [(4,0.5),(5,0.5)],
}
STAT_TYPES = ['DEF','HP','SPD','ATK','CRIT']
SWORD_BASEATK, SWORD_SPD, SWORD_CRIT = 10, 2, 2
STRIKE_MULT, STRIKE_MP = 1.5, 15  # 강타

def exp_to_next(lv): return round(100 * (lv ** 1.5))

def roll_grade(gc):
    r = random.random(); a = 0
    for g in ['C','U','R','E','L']:
        a += gc[g]
        if r < a: return g
    return 'L'

def roll_statcount(g):
    r = random.random(); a = 0
    for cnt, pr in STAT_DIST[g]:
        a += pr
        if r < a: return cnt
    return STAT_DIST[g][-1][0]

def roll_stats(g, P):
    cnt = roll_statcount(g)
    lo = max(1, round(P*0.4)); hi = round(P*0.8)
    if hi < lo: hi = lo
    types = random.sample(STAT_TYPES, cnt)
    return {t: random.randint(lo, hi) for t in types}

class Weapon:  # SWORD only
    def __init__(self, grade, item_level):
        self.grade = grade; self.ilvl = item_level
        P = item_level + GRADE_BONUS[grade]
        self.base_atk = round(SWORD_BASEATK * (1 + 0.15*P))
        st = roll_stats(grade, P)
        self.r_atk = st.get('ATK',0); self.r_crit = st.get('CRIT',0); self.r_spd = st.get('SPD',0)
        self.slots = GRADE_SLOTS[grade]
        self.has_strike = False
    def power(self): return self.base_atk + self.r_atk  # 교체 판단 기준

DUNGEONS = {
 'forest': dict(ilvl=1, gc=dict(C=.700,U=.220,R=.060,E=.018,L=.002),
                mobs=[('goblin',5),('wolf',3)], boss='trent'),
 'mine':   dict(ilvl=5, gc=dict(C=.550,U=.280,R=.120,E=.040,L=.010),
                mobs=[('bat',5),('golem',2)], boss='minelord'),
 'tower':  dict(ilvl=10, gc=dict(C=.400,U=.300,R=.180,E=.090,L=.030),
                mobs=[('mage',5),('gargoyle',2)], boss='lich'),
}
MON = {
 'goblin': dict(hp=50, atk=12, deff=5, spd=8, dt='P', exp=20, gold=10),
 'wolf':   dict(hp=65, atk=15, deff=4, spd=14, dt='P', exp=28, gold=12),
 'trent':  dict(hp=150, atk=20, deff=18, spd=6, dt='P', exp=150, gold=80),
 'bat':    dict(hp=80, atk=20, deff=6, spd=18, dt='P', exp=48, gold=22),
 'golem':  dict(hp=160, atk=26, deff=22, spd=4, dt='P', exp=95, gold=52),
 'minelord':dict(hp=340, atk=36, deff=26, spd=10, dt='P', exp=300, gold=170),
 'mage':   dict(hp=130, atk=30, deff=10, spd=13, dt='M', exp=120, gold=48),
 'gargoyle':dict(hp=210, atk=36, deff=25, spd=11, dt='P', exp=150, gold=65),
 'lich':   dict(hp=500, atk=44, deff=30, spd=16, dt='M', exp=480, gold=320),
}

class Char:
    def __init__(self):
        self.level = 1; self.exp = 0; self._recalc()
        self.weapon = Weapon('C', 1); self.weapon.r_atk = 1  # 시작 낡은 검
        self.armor = {s: None for s in ['HELMET','CHEST','GLOVES','BOOTS']}
        self.strike_books = 0
        self.hp = self.eff_maxhp(); self.mp = self.max_mp
    def _recalc(self):
        L = self.level-1
        self.max_hp = BASE['hp']+GAIN['hp']*L; self.max_mp = BASE['mp']+GAIN['mp']*L
        self.c_atk = BASE['atk']+GAIN['atk']*L; self.c_def = BASE['deff']+GAIN['deff']*L
        self.c_spd = BASE['spd']+GAIN['spd']*L; self.c_crit = BASE['crit']+GAIN['crit']*L
    def _armor_sum(self, key):
        tot = 0
        for inst in self.armor.values():
            if inst: tot += inst.get(key, 0)
        return tot
    def eff_atk(self):   return self.c_atk + self.weapon.base_atk + self.weapon.r_atk + self._armor_sum('ATK')
    def eff_spd(self):   return self.c_spd + SWORD_SPD + self.weapon.r_spd + self._armor_sum('SPD')
    def eff_crit(self):  return self.c_crit + SWORD_CRIT + self.weapon.r_crit + self._armor_sum('CRIT')
    def eff_def(self):   return self.c_def + self._armor_sum('DEF')
    def eff_maxhp(self): return self.max_hp + self._armor_sum('HP')
    def gain_exp(self, a):
        self.exp += a
        while self.exp >= exp_to_next(self.level):
            self.exp -= exp_to_next(self.level); self.level += 1
        self._recalc()
    def try_slot_strike(self):
        if not self.weapon.has_strike and self.weapon.slots >= 1 and self.strike_books > 0:
            self.weapon.has_strike = True; self.strike_books -= 1

def crit_pct(c): return 5 + c.eff_spd()*0.2 + c.eff_crit()

def dmg(atk, mult, deff, dt, cp):
    coeff = 0.5 if dt=='P' else 0.2
    d = (atk*mult - deff*coeff)*random.uniform(RL,RH)
    if random.random()*100 < cp: d *= CRIT
    return max(1, round(d))

def battle(c, mk, potions):
    m = MON[mk]; mx = c.eff_maxhp()
    mhp = m['hp']; php = c.hp; mp = c.max_mp
    pf = c.eff_spd() > m['spd'] or (c.eff_spd()==m['spd'] and random.random()<0.5)
    cp = crit_pct(c); turn = pf
    ea = c.eff_atk(); ed = c.eff_def()
    while True:
        if turn:
            if php < mx*0.35 and potions > 0:
                php = min(mx, php+HP_HEAL); potions -= 1
            elif c.weapon.has_strike and mp >= STRIKE_MP:
                mp -= STRIKE_MP; mhp -= dmg(ea, STRIKE_MULT, m['deff'],'P',cp)
                if mhp<=0: c.hp=php; return True, potions
            else:
                mhp -= dmg(ea, 1.0, m['deff'],'P',cp)
                if mhp<=0: c.hp=php; return True, potions
        else:
            php -= dmg(m['atk'],1.0, ed, m['dt'],5)
            if php<=0: c.hp=0; return False, potions
        turn = not turn

EVENTS = [('combat',0.75),('rest',0.08),('merchant',0.07),('trap',0.05),('treasure',0.05)]
def roll_event():
    r=random.random(); a=0
    for n,pr in EVENTS:
        a+=pr
        if r<a: return n
    return 'combat'
def pick_mob(mobs):
    tot=sum(w for _,w in mobs); r=random.random()*tot; a=0
    for name,w in mobs:
        a+=w
        if r<a: return name
    return mobs[0][0]

def give_armor_drop(c, dg):
    g = roll_grade(dg['gc']); P = dg['ilvl']+GRADE_BONUS[g]
    st = roll_stats(g, P); slot = random.choice(['HELMET','CHEST','GLOVES','BOOTS'])
    cur = c.armor[slot]
    if cur is None or sum(st.values()) > sum(cur.values()):
        c.armor[slot] = st

def give_boss_drop(c, dg):
    # 무기 15% / 스킬북 15% / 무획득 70% (상호배타)
    r = random.random()
    if r < 0.15:  # 무기
        wtype = random.randint(0,5)  # 6종 균등
        if wtype == 0:  # SWORD (메인)만 채택
            g = roll_grade(dg['gc'])
            nw = Weapon(g, dg['ilvl'])
            if nw.power() > c.weapon.power():
                keep_strike = c.weapon.has_strike
                c.weapon = nw
                if keep_strike: c.strike_books += 1  # 슬롯 재장착용으로 환원(단순화)
                c.try_slot_strike()
    elif r < 0.30:  # 스킬북
        stype = random.randint(0,5)
        if stype == 0:  # 강타(SWORD)만 유효
            c.strike_books += 1; c.try_slot_strike()

def run(c, dgkey, potions):
    dg = DUNGEONS[dgkey]
    for _ in range(4):
        ev = roll_event()
        if ev=='combat':
            mk = pick_mob(dg['mobs'])
            win, potions = battle(c, mk, potions)
            if not win: return potions, False, True
            c.gain_exp(MON[mk]['exp'])
            if random.random() < 0.05: give_armor_drop(c, dg)
        elif ev=='rest': c.hp = min(c.eff_maxhp(), c.hp+round(c.eff_maxhp()*0.10))
        elif ev=='trap': c.hp = max(1, c.hp-round(c.hp*0.10))
        elif ev=='treasure':
            rr=random.random()
            if rr<0.9 and rr>=0.5: potions+=1
    # 보스
    win, potions = battle(c, dg['boss'], potions)
    if not win: return potions, False, True
    c.gain_exp(MON[dg['boss']]['exp']); give_boss_drop(c, dg)
    return potions, True, False

def town(c, potions, gold_income, mx=6):
    c.hp = c.eff_maxhp(); c.mp = c.max_mp
    # 골드는 단순화: 런마다 충분히 벌어 포션 최대치 유지
    while potions < mx:
        potions += 1
    return potions

def playthrough():
    c = Char(); potions = 0
    order = ['forest','mine','tower']
    first_kill_level = {}
    for dgkey in order:
        runs = 0
        while runs < 2000:
            potions = town(c, potions, 0)
            potions, boss, died = run(c, dgkey, potions)
            runs += 1
            if boss:
                first_kill_level[dgkey] = c.level_at_kill if False else c.level
                break
    return first_kill_level, c

if __name__ == "__main__":
    import statistics as st
    N = 4000
    data = {'forest':[], 'mine':[], 'tower':[]}
    for _ in range(N):
        fk, c = playthrough()
        for k in data:
            if k in fk: data[k].append(fk[k])
    labels = {'forest':'숲/트렌트(권장1)','mine':'광산/광산왕(권장5)','tower':'탑/흑마도사(권장10)'}
    print("="*70)
    print("각 보스 '첫 격파 시점 레벨' 분포 (메인=SWORD, 장비/스킬 성장 포함)")
    print("="*70)
    for k in ['forest','mine','tower']:
        d = data[k]
        d.sort()
        mean = sum(d)/len(d)
        p25 = d[len(d)//4]; p50 = d[len(d)//2]; p75 = d[3*len(d)//4]
        mn, mx = d[0], d[-1]
        print("%s" % labels[k])
        print("   평균 Lv %.2f | 중앙 Lv %d | 25~75%%: Lv%d~%d | 범위 Lv%d~%d"
              % (mean, p50, p25, p75, mn, mx))
    print()
    # 목표 창 적중률
    def within(d, lo, hi): return 100.0*sum(1 for x in d if lo<=x<=hi)/len(d)
    print("목표창 적중률:")
    print("  광산왕 Lv4~6 이내 격파: %.1f%%" % within(data['mine'],4,6))
    print("  흑마도사 Lv9~11 이내 격파: %.1f%%" % within(data['tower'],9,11))
