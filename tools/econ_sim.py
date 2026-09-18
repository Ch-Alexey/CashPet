# Черновая модель стартовой экономики CashPet (docs/02-экономика.md).
# Проверяет неравенства раздела 19 рамок и прогоняет 5 периодов для трёх сценариев.
# Временный инструмент: после появления симулятора в модуле core источником правды станет он.
# Запуск: python3 tools/econ_sim.py
# Стартовая экономика CashPet: проверка неравенств и 5 периодов на 3 сценария
POCKET=60; START=100
TASK_R=15; TASKS_PER=2; JOB_R=10; JOBS_PER=2
NEED={"Корм":30,"Рыбка":45,"Шампунь":20,"Расчёска":15}
FOOD={"Корм","Рыбка"}; CARE={"Шампунь","Расчёска"}
WANT={"Мячик":15,"Бантик":25,"Кепка":35,"Куртка":55,"Самокат":80}
GOALS={"Когтеточка":60,"Домик":100,"Велосипед":150}
TOL=10; ST2=5; ST3=12
O=NEED["Корм"]+NEED["Шампунь"]; E=TASKS_PER*TASK_R+JOBS_PER*JOB_R; I=POCKET+E; S=I-O
SP=sum(WANT.values()); Pmin=min(WANT.values()); Pmax=max(WANT.values()); U=25
print(f"I={I} O={O} S={S} E={E} ΣP={SP}")
chk=[("1 O≈0.5I",0.4<=O/I<=0.6,f"{O}/{I}={O/I:.2f}"),("2 Pmin<S",Pmin<S,f"{Pmin}<{S}"),
("3 ΣP≥3S",SP>=3*S,f"{SP}≥{3*S}"),("4 Pmax>0.5S",Pmax>0.5*S,f"{Pmax}>{0.5*S}"),
("5 C_mid≈3.5·0.5·S",abs(GOALS['Домик']-1.75*S)<=15,f"{GOALS['Домик']}≈{1.75*S}"),
("6 U≤0.8S",U<=0.8*S,f"{U}≤{0.8*S}"),("7 E≈0.4–0.6 I",0.4<=E/I<=0.6,f"{E}/{I}={E/I:.2f}"),
("8 перенос: POCKET≥O (нет тупика)",POCKET>=O,f"{POCKET}≥{O}")]
for n,ok,d in chk: print(("OK " if ok else "FAIL ")+n+"  "+d)
f5=lambda x:int(x//5*5)
def run(kind):
    bal=START; sav=0; pts=0; rows=[]; goals=[GOALS["Домик"],GOALS["Велосипед"]]; goal=goals.pop(0); done=[]
    for p in range(1,6):
        if p>1: bal+=POCKET
        A=bal
        if kind=="разумная": pn=50; ps=f5((A-50)*0.5); pw=A-50-ps
        elif kind=="транжира": pn=50; ps=10; pw=A-60
        else: pn=30; ps=A-30; pw=0
        earned=E if kind!="транжира" else TASK_R+JOB_R
        bal+=earned
        # траты
        need=0; bought=set()
        items=["Корм","Шампунь"] if kind!="скопидом" else (["Корм"] if p%2 else ["Корм","Расчёска"])
        for it in items: bal-=NEED[it]; need+=NEED[it]; bought.add(it)
        want=0
        budget = pw if kind=="разумная" else (bal if kind=="транжира" else 0)
        for it,c in sorted(WANT.items(),key=lambda x:-x[1]):
            if want+c<=budget and c<=bal: bal-=c; want+=c
        if kind=="разумная": dep=min(bal, ps+f5(earned/2))
        elif kind=="транжира": dep=0
        else: dep=bal
        dep=min(dep, goal-sav)
        bal-=dep; sav+=dep
        assert bal>=0, (kind,p,bal)
        if sav>=goal: done.append(p); sav-=goal; goal=goals.pop(0) if goals else 10**6
        c1=bool(bought&FOOD) and bool(bought&CARE)
        c2=need<=pn+TOL and want<=pw+TOL
        c3=dep>=ps and dep>=10
        g=c1+c2+c3; pts+=g
        st=3 if pts>=ST3 else 2 if pts>=ST2 else 1
        rows.append((p,A,f"{pn}/{pw}/{ps}",earned,need,want,dep,f"{sav}"+(' ✓цель' if done and done[-1]==p else ''),bal,g,pts,st))
    return rows
for k in ["разумная","транжира","скопидом"]:
    print(f"\n### {k}")
    print("| Период | Доступно в начале | План нужн./жел./копилка | Заработал | Нужное | Желаемое | В копилку | Копилка всего | Баланс в конце | Очки | Сумма очков | Стадия |")
    print("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for r in run(k): print("| "+" | ".join(map(str,r))+" |")
