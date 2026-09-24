# Черновая модель экономики CashPet (docs/02-экономика.md), версия 2.
# Проверяет неравенства раздела 19 рамок и прогоняет 5 недель для трёх сценариев.
# Временный инструмент: после появления симулятора в модуле core источником правды станет он.
# Запуск: python3 tools/econ_sim.py   (Windows: python tools\econ_sim.py)
import math

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
DECAY = {"сытость": 0.75, "уход": 0.8, "настроение": 0.7}   # в конце недели
N_FOOD, N_CARE = 40, 25   # «нужное в порядке»: за неделю еды на +40 сытости и ухода на +25

# --- Рост ---
TOL = 10                  # допуск «попроще» = 20
W_N, W_M, W_S = 40, 30, 30
STAGE2, STAGE3 = 150, 350

f5 = lambda x: int(x // 5 * 5)

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
        ("8 нет тупика: карманные ≥ O", POCKET >= O, f"{POCKET} ≥ {O}"),
    ]
    print(f"I={I} O={O} S={S} заработок={E} ΣP(желаемое)={SP}")
    for n, ok, d in rows:
        print(("OK   " if ok else "FAIL ") + n + "   " + d)
    return all(ok for _, ok, _ in rows)

def growth(food, care, fact_need, fact_want, plan, saved, goal_done):
    n = 0.5 * (food >= N_FOOD) + 0.5 * (care >= N_CARE)
    d = max(0, fact_need - (plan[0] + TOL)) + max(0, fact_want - (plan[1] + TOL))
    m = max(0.0, 1 - d / (2 * TOL))
    target = max(plan[2], 10)
    s = 1.0 if goal_done else min(saved / target, 1.0)
    return n, m, s, round(W_N * n + W_M * m + W_S * s)

def mood_label(st):
    v = 0.4 * st["настроение"] + 0.3 * st["сытость"] + 0.3 * st["уход"]
    return "радуется" if v >= 75 else "спокоен" if v >= 50 else "грустит"

def run(kind):
    bal = START; sav = 0; goals = list(GOALS); goal = goals.pop(0); total = 0
    st = {k: START_STAT for k in DECAY}; rows = []
    for w in range(5):
        if w > 0: bal += POCKET
        avail = bal
        # План: нужное / желаемое / копилка
        if kind == "разумная":
            plan = (50, 0, 0); save = f5((avail - 50) / 2); plan = (50, avail - 50 - save, save)
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
        # Копилка
        dep = {"разумная": plan[2], "транжира": 0, "скопидом": bal}[kind]
        dep = min(dep, bal, goal - sav)
        bal -= dep; sav += dep
        assert bal >= 0, (kind, w, bal)
        done = sav >= goal
        if done: sav -= goal; goal = goals.pop(0) if goals else 10 ** 6
        n, m, s, gp = growth(food, care, fact_need, fact_want, plan, dep, done)
        total += gp
        stage = 3 if total >= STAGE3 else 2 if total >= STAGE2 else 1
        label = mood_label(st)
        rows.append((w + 1, avail, "/".join(map(str, plan)), earned, fact_need, fact_want, dep,
                     f"{sav}" + (" ✓цель" if done else ""), bal, f"{n:g}", f"{m:g}", f"{s:.2g}", gp, total, stage, label))
        for k in st: st[k] = max(FLOOR, round(st[k] * DECAY[k]))
    return rows

if __name__ == "__main__":
    checks()
    head = ("| Неделя | Доступно | План нужн./жел./копилка | Заработал | Нужное | Желаемое | В копилку "
            "| Копилка | Баланс | N | M | S | GP | ΣGP | Стадия | Кот |")
    for kind in ["разумная", "транжира", "скопидом"]:
        print(f"\n### {kind}\n{head}\n" + "|---" * 16 + "|")
        for r in run(kind):
            print("| " + " | ".join(map(str, r)) + " |")
