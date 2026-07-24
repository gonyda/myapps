import random

RL, RH, CRIT = 0.9, 1.1, 1.5
BASE = dict(hp=100, mp=50, atk=10, deff=5, spd=5, crit=0)
GAIN = dict(hp=20, mp=10, atk=3, deff=2, spd=1, crit=1)
HP_POTION_HEAL, HP_POTION_COST = 50, 30

def exp_to_next(lv): return round(100 * (lv ** 1.5))
def cum_exp(lv): return sum(exp_to_next(n) for n in range(1, lv))

class Player:
    def __init__(self):
        self.level = 1; self.exp = 0
        self._recalc(); self.hp = self.max_hp; self.mp = self.max_mp
        self.w_atk, self.w_spd, self.w_crit, self.w_rand_atk = 12, 2, 2, 1
        self.arm_def = 0; self.arm_hp = 0
    def _recalc(self):
        L = self.level - 1
        self.max_hp = BASE['hp'] + GAIN['hp']*L
        self.max_mp = BASE['mp'] + GAIN['mp']*L
        self.c_atk = BASE['atk'] + GAIN['atk']*L
        self.c_def = BASE['deff'] + GAIN['deff']*L
        self.c_spd = BASE['spd'] + GAIN['spd']*L
        self.c_crit = BASE['crit'] + GAIN['crit']*L
    def eff_atk(self):   return self.c_atk + self.w_atk + self.w_rand_atk
    def eff_spd(self):   return self.c_spd + self.w_spd
    def eff_crit(self):  return self.c_crit + self.w_crit
    def eff_def(self):   return self.c_def + self.arm_def
    def eff_maxhp(self): return self.max_hp + self.arm_hp
    def gain_exp(self, a):
        self.exp += a; up = 0
        while self.exp >= exp_to_next(self.level):
            self.exp -= exp_to_next(self.level); self.level += 1; up += 1
        if up:
            self._recalc(); self.hp = self.eff_maxhp(); self.mp = self.max_mp
        return up

MON = {
 'goblin': dict(hp=50, atk=12, deff=5, spd=8, dt='P', exp=20, gold=10),
 'wolf':   dict(hp=65, atk=15, deff=4, spd=14, dt='P', exp=28, gold=12),
 'trent':  dict(hp=150, atk=20, deff=18, spd=6, dt='P', exp=150, gold=80),
}

def dmg(atk, mult, deff, dt, crit_pct_v):
    coeff = 0.5 if dt == 'P' else 0.2
    d = (atk*mult - deff*coeff) * random.uniform(RL, RH)
    if random.random()*100 < crit_pct_v: d *= CRIT
    return max(1, round(d))

def crit_pct(p): return 5 + p.eff_spd()*0.2 + p.eff_crit()

def battle(p, mk, potions=0, use_potions=False):
    m = MON[mk]; p.mp = p.max_mp
    mhp = m['hp']; php = p.hp; mx = p.eff_maxhp()
    pf = p.eff_spd() > m['spd'] or (p.eff_spd() == m['spd'] and random.random() < 0.5)
    pc = crit_pct(p); turn = pf
    while True:
        if turn:
            if use_potions and php < mx*0.35 and potions > 0:
                php = min(mx, php + HP_POTION_HEAL); potions -= 1
            else:
                mhp -= dmg(p.eff_atk(), 1.0, m['deff'], 'P', pc)
                if mhp <= 0: p.hp = php; return True, potions
        else:
            php -= dmg(m['atk'], 1.0, p.eff_def(), m['dt'], 5)
            if php <= 0: p.hp = 0; return False, potions
        turn = not turn

EVENTS = [('combat',0.75),('rest',0.08),('merchant',0.07),('trap',0.05),('treasure',0.05)]
def roll_event():
    r = random.random(); a = 0
    for n,pr in EVENTS:
        a += pr
        if r < a: return n
    return 'combat'
def roll_mon(): return 'goblin' if random.random() < 5/8 else 'wolf'

def farming_run_v1(p):
    combats = 0; e0 = cum_exp(p.level)+p.exp
    for _ in range(4):
        ev = roll_event()
        if ev == 'combat':
            combats += 1; mk = roll_mon()
            win, _ = battle(p, mk)
            if not win: return combats, (cum_exp(p.level)+p.exp)-e0, True
            p.gain_exp(MON[mk]['exp'])
        elif ev == 'rest': p.hp = min(p.eff_maxhp(), p.hp+round(p.eff_maxhp()*0.10))
        elif ev == 'trap': p.hp = max(1, p.hp-round(p.hp*0.10))
    return combats, (cum_exp(p.level)+p.exp)-e0, False

def try_boss(level, trials=3000):
    w = 0
    for _ in range(trials):
        c = Player(); c.level = level; c._recalc(); c.hp = c.eff_maxhp()
        win, _ = battle(c, 'trent')
        if win: w += 1
    return w/trials

def run_dungeon_v2(p, gold, potions):
    combats = 0; fight_boss = p.level >= 3
    for _ in range(4):
        ev = roll_event()
        if ev == 'combat':
            combats += 1; mk = roll_mon()
            win, potions = battle(p, mk, potions, True)
            if not win: return combats, gold, potions, True, False
            p.gain_exp(MON[mk]['exp']); gold += MON[mk]['gold']
        elif ev == 'rest': p.hp = min(p.eff_maxhp(), p.hp+round(p.eff_maxhp()*0.10))
        elif ev == 'trap': p.hp = max(1, p.hp-round(p.hp*0.10))
        elif ev == 'treasure':
            r = random.random()
            if r < 0.5: gold += round(30*1.05)
            elif r < 0.9: potions += 1
    if fight_boss:
        combats += 1
        win, potions = battle(p, 'trent', potions, True)
        if not win: return combats, gold, potions, True, False
        p.gain_exp(MON['trent']['exp']); gold += MON['trent']['gold']
        return combats, gold, potions, False, True
    return combats, gold, potions, False, False

def town(p, gold, potions, mx_pot=6):
    p.hp = p.eff_maxhp(); p.mp = p.max_mp
    while potions < mx_pot and gold >= HP_POTION_COST:
        gold -= HP_POTION_COST; potions += 1
    return gold, potions

def playthrough_v2(target, mx_runs=500):
    p = Player(); gold = 0; potions = 0; runs = 0; combats = 0; deaths = 0; bkills = 0
    fbr = None
    while p.level < target and runs < mx_runs:
        c, gold, potions, died, boss = run_dungeon_v2(p, gold, potions)
        runs += 1; combats += c
        if died:
            deaths += 1; p.exp = max(0, p.exp-round(p.exp*0.10))
        if boss:
            bkills += 1
            if fbr is None: fbr = runs
        gold, potions = town(p, gold, potions)
    return runs, combats, deaths, bkills, fbr

if __name__ == "__main__":
    print("="*66)
    print("1) LEVEL CURVE")
    print("="*66)
    print("Lv  next  cum")
    for lv in range(1, 11):
        print(lv, exp_to_next(lv), cum_exp(lv+1))

    print("="*66)
    print("2) TRENT(HP150/atk20/def18) win% by level - basic attack only")
    print("="*66)
    for lv in range(1, 7):
        p = Player(); p.level = lv; p._recalc(); p.hp = p.eff_maxhp()
        wr = try_boss(lv)
        print("Lv%2d  win %5.1f%%  eatk %d  edef %d  maxhp %d" % (lv, wr*100, p.eff_atk(), p.eff_def(), p.eff_maxhp()))

    print("="*66)
    print("3) [v1 harsh] no potion/no town-heal, abandon before boss")
    print("="*66)
    N = 3000
    for target in [2, 3, 4, 5]:
        R = C = D = 0
        for _ in range(N):
            p = Player(); runs = 0; comb = 0
            while p.level < target and runs <= 500:
                c, _, died = farming_run_v1(p)
                runs += 1; comb += c
                if died:
                    D += 1; p.exp = max(0, p.exp-round(p.exp*0.10)); p.hp = p.eff_maxhp()
            R += runs; C += comb
        print("Lv%d: avg %5.1f runs  %6.1f combats  deaths %4.2f/char" % (target, R/N, C/N, D/N))

    print("="*66)
    print("4) [v2 realistic] potions + town full-heal + boss from Lv3")
    print("="*66)
    for target in [3, 5, 7]:
        aR=aC=aD=aB=0; fbrs=[]
        for _ in range(N):
            runs, comb, d, b, fbr = playthrough_v2(target)
            aR+=runs; aC+=comb; aD+=d; aB+=b
            if fbr: fbrs.append(fbr)
        fbrm = sum(fbrs)/len(fbrs) if fbrs else 0
        print("Lv%d: avg %5.1f runs  %6.1f combats  deaths %4.2f/char  bosskills %4.1f  firstboss %4.1f" % (target, aR/N, aC/N, aD/N, aB/N, fbrm))

    print("="*66)
    print("5) v1 per-run averages")
    print("="*66)
    tc = te = 0; K = 5000
    for _ in range(K):
        p = Player(); c, e, _ = farming_run_v1(p); tc += c; te += e
    print("per-run combats %.2f  exp %.1f" % (tc/K, te/K))
