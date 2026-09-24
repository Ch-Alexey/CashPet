# Черновая модель экономики CashPet (docs/02-экономика.md), версия 3.
# Проверяет неравенства раздела 19 рамок и прогоняет 5 недель для трёх сценариев.
# Временный инструмент: после появления симулятора в модуле core источником правды станет он.
# Запуск: python3 tools/econ_sim.py   (Windows: python tools\econ_sim.py)
import math
import sys
from fractions import Fraction

# Без этого вывод в файл или в конвейер на Windows падает на «Σ» и кириллице
sys.stdout.reconfigure(encoding="utf-8")

# --- Доходы ---
START = 100            # стартовый бюджет, 1-я неделя
POCKET = 60            # карманные от мамы-кошки, со 2-й недели
JOB_R, JOBS_PER = 10, 2  # подработка: оплата и лимит в неделю
# Награды учебных заданий MVP в порядке открытия (по 2 в неделю): 0, 1.1 | 4.2, 2.3 | 3.1, 3.3 | 6.2
TASKS_BY_WEEK = [[20, 15], [20, 20], [15, 25], [15], []]

# --- Магазин: цена, сытость, уход, настроение ---
NEED = {"Корм": (30, 40, 0, 0), "Рыбка": (45, 50, 0, 5), "Витамины": (15, 20, 0, 0),
        "Консервы": (60, 65, 0, 10), "Шампунь": (20, 0, 40, 0), "Расчёска": (15, 0, 25, 0),
        "Полотенце": (20, 0, 30, 0), "Пластырь": (25, 0, 30, 0)}
WANT = {"Погремушка": (10, 10), "Мячик": (15, 15), "Мышка": (20, 18), "Бантик": (25, 20),
        "Кепка": (35, 25), "Лежанка": (40, 22), "Куртка": (55, 30), "Самокат": (80, 40)}
GOALS = [100, 150]     # Домик, затем Велосипед (Когтеточка 60 — короткая)

# --- Кот ---
START_STAT = 70; FLOOR = 25; CAP = 100
DECAY_PCT = {"сытость": 75, "уход": 80, "настроение": 70}   # в конце недели, в процентах
N_FOOD, N_CARE = 40, 25   # «нужное в порядке»: за неделю еды на +40 сытости и ухода на +25

# --- Рост ---
TOL = 10                  # допуск «попроще» = 20
W_N, W_M, W_S = 40, 30, 30
STAGE2, STAGE3 = 150, 350

f5 = lambda x: int(x // 5 * 5)

def round_half_up(x):
    # Правило округления из docs/02-экономика.md: до целого, половина вверх.
    # Встроенный round() в Python округляет половину к чётному (92,5 → 92), а Kotlin — вверх (93)
    return math.floor(Fraction(x) + Fraction(1, 2))

def checks():
    O = NEED["Корм"][0] + NEED["Шампунь"][0]
    E = sum(TASKS_BY_WEEK[0]) + JOBS_PER * JOB_R
    I = POCKET + E; S = I - O
    prices = [p[0] for p in WANT.values()]
    SP = sum(prices)
    rows = [
        ("1 O ≈ 0,5·I", 0.4 <= O / I <= 0.6, f"{O}/{I} = {O/I:.2f}"),
        ("2 P_min < S", min(prices) < S, f"{min(prices)} < {S}"),
        ("3 ΣP ≥ 3·S", SP >= 3 * S, f"{SP} ≥ {3*S}"),
        ("4 P_max > 0,5·S", max(prices) > 0.5 * S, f"{max(prices)} > {0.5*S:g}"),
        ("5 C_средняя ≈ 3,5·0,5·S", abs(GOALS[0] - 1.75 * S) <= 25, f"{GOALS[0]} ≈ {1.75*S:g}"),
        ("6 U ≤ 0,8·S", NEED["Пластырь"][0] <= 0.8 * S, f"{NEED['Пластырь'][0]} ≤ {0.8*S:g}"),
        ("7 заработок ≈ 0,4–0,6·I", 0.4 <= E / I <= 0.6, f"{E}/{I} = {E/I:.2f}"),
        ("8а нет тупика: карманные ≥ O", POCKET >= O, f"{POCKET} ≥ {O}"),
    ]
    print(f"I={I} O={O} S={S} заработок={E} ΣP(желаемое)={SP}")
    for n, ok, d in rows:
        print(("OK   " if ok else "FAIL ") + n + "   " + d)
    return all(ok for _, ok, _ in rows)

def check_carryover(rows):
    # Неравенство 8б: с переносом остатка свободных монет недели больше, чем S первой недели,
    # поэтому проверяем по разумной игре: даже в самую «богатую» неделю всё желаемое не купить
    SP = sum(p[0] for p in WANT.values())
    s_max = max(r[1] + r[3] - r[4] for r in rows)   # доступно + заработал − нужное
    ok = SP >= 2 * s_max
    print(("OK   " if ok else "FAIL ") + f"8б перенос: ΣP ≥ 2·S_max   {SP} ≥ {2*s_max}")
    return ok

def growth(food, care, fact_need, fact_want, plan, saved, goal_done):
    n = Fraction(1, 2) * (food >= N_FOOD) + Fraction(1, 2) * (care >= N_CARE)
    d = max(0, fact_need - (plan[0] + TOL)) + max(0, fact_want - (plan[1] + TOL))
    m = max(Fraction(0), 1 - Fraction(d, 2 * TOL))
    target = max(plan[2], 10)
    s = Fraction(1) if goal_done else min(Fraction(saved, target), Fraction(1))
    return n, m, s, round_half_up(W_N * n + W_M * m + W_S * s)

def mood_label(st):
    v = 0.4 * st["настроение"] + 0.3 * st["сытость"] + 0.3 * st["уход"]
    return "радуется" if v >= 75 else "спокоен" if v >= 50 else "грустит"

def run(kind):
    bal = START; sav = 0; goals = list(GOALS); goal = goals.pop(0); total = 0
    st = {k: START_STAT for k in DECAY_PCT}; rows = []
    for w in range(5):
        if w > 0: bal += POCKET
        avail = bal
        # План: нужное / желаемое / копилка
        if kind == "разумная":
            save = f5((avail - 50) / 2); plan = (50, avail - 50 - save, save)
        elif kind == "транжира":
            plan = (50, avail - 60, 10)
        else:
            plan = (30, 0, avail - 30)
        tasks = TASKS_BY_WEEK[w] if kind != "транжира" else TASKS_BY_WEEK[w][:1]
        jobs = JOBS_PER if kind != "транжира" else 1
        earned = sum(tasks) + jobs * JOB_R
        bal += earned
        # Нужное
        if kind == "скопидом":
            buy = ["Корм"] if w % 2 == 0 else ["Корм", "Расчёска"]
        else:
            buy = ["Корм", "Шампунь"]
        fact_need = food = care = 0
        for it in buy:
            p, sa, ca, mo = NEED[it]
            bal -= p; fact_need += p; food += sa; care += ca
            st["сытость"] = min(CAP, st["сытость"] + sa); st["уход"] = min(CAP, st["уход"] + ca)
        # Желаемое
        limit = plan[1] if kind == "разумная" else (bal if kind == "транжира" else 0)
        fact_want = 0
        for it, (p, mo) in sorted(WANT.items(), key=lambda x: -x[1][0]):
            if fact_want + p <= limit and p <= bal:
                bal -= p; fact_want += p; st["настроение"] = min(CAP, st["настроение"] + mo)
        # Копилка: взнос не ограничен стоимостью цели, лишнее остаётся в копилке
        dep = {"разумная": plan[2], "транжира": 0, "скопидом": bal}[kind]
        dep = min(dep, bal)
        bal -= dep; sav += dep
        assert bal >= 0, (kind, w, bal)
        # «Купить мечту»: стоимость списывается из копилки, это не снятие и не факт «Нужно»/«Хочу»
        done = sav >= goal
        if done: sav -= goal; goal = goals.pop(0) if goals else 10 ** 6
        n, m, s, gp = growth(food, care, fact_need, fact_want, plan, dep, done)
        total += gp
        stage = 3 if total >= STAGE3 else 2 if total >= STAGE2 else 1
        label = mood_label(st)
        rows.append((w + 1, avail, "/".join(map(str, plan)), earned, fact_need, fact_want, dep,
                     f"{sav}" + (" ✓цель" if done else ""), bal,
                     f"{float(n):g}", f"{float(m):g}", f"{float(s):.2g}", gp, total, stage, label))
        for k in st: st[k] = max(FLOOR, round_half_up(Fraction(st[k] * DECAY_PCT[k], 100)))
    return rows

if __name__ == "__main__":
    checks()
    results = {kind: run(kind) for kind in ["разумная", "транжира", "скопидом"]}
    check_carryover(results["разумная"])
    head = ("| Неделя | Доступно | План нужн./жел./копилка | Заработал | Нужное | Желаемое | В копилку "
            "| Копилка | Баланс | N | M | S | GP | ΣGP | Стадия | Кот |")
    for kind, rows in results.items():
        print(f"\n### {kind}\n{head}\n" + "|---" * 16 + "|")
        for r in rows:
            print("| " + " | ".join(map(str, r)) + " |")
