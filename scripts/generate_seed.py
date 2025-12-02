import random
from datetime import datetime, timedelta
from pathlib import Path

PROVINCE_CONFIG = [
    ("云南省", 73),
    ("贵州省", 66),
    ("四川省", 45),
    ("甘肃省", 58),
    ("陕西省", 50),
    ("河北省", 45),
    ("山西省", 36),
    ("内蒙古自治区", 31),
    ("辽宁省", 15),
    ("吉林省", 8),
    ("黑龙江省", 14),
    ("安徽省", 20),
    ("江西省", 24),
    ("河南省", 38),
    ("湖北省", 28),
    ("湖南省", 40),
    ("广西壮族自治区", 54),
    ("海南省", 5),
    ("重庆市", 14),
    ("青海省", 42),
    ("宁夏回族自治区", 8),
    ("新疆维吾尔自治区", 35),
    ("西藏自治区", 74),
]

YEARS = [2018, 2019, 2020, 2021, 2022, 2023]
MOUNTAIN_PROVINCES = {
    "云南省",
    "贵州省",
    "四川省",
    "甘肃省",
    "陕西省",
    "青海省",
    "宁夏回族自治区",
    "新疆维吾尔自治区",
    "西藏自治区",
}

random.seed(20240222)

province_rows = []
county_rows = []
indicator_rows = []
indicators_by_county = {}

province_id = 0
county_id = 0

for province_name, quota in PROVINCE_CONFIG:
    province_id += 1
    province_rows.append((province_id, province_name))
    for idx in range(quota):
        county_id += 1
        county_name = f"{province_name}示例县{idx + 1:03d}"
        delisting_year = random.choice([2018, 2019, 2020, 2021, None, None])
        county_rows.append((county_id, county_name, province_id, delisting_year))

        is_mountain = province_name in MOUNTAIN_PROVINCES
        base_gdp = random.uniform(12, 55) if is_mountain else random.uniform(30, 120)
        base_income = random.uniform(7000, 9500) if is_mountain else random.uniform(9000, 14000)
        base_percap = random.uniform(16000, 30000) if is_mountain else random.uniform(28000, 52000)
        base_fiscal = random.uniform(2.5, 8.0) if is_mountain else random.uniform(4.0, 18.0)
        poverty = random.uniform(7.0, 14.0) if is_mountain else random.uniform(3.0, 8.0)

        last_gdp = base_gdp
        last_income = base_income
        last_percap = base_percap
        last_fiscal = base_fiscal

        county_indicators = []

        for year in YEARS:
            if year == 2020:
                gdp_yoy = round(random.uniform(-0.03, 0.035), 3)
                income_yoy = round(random.uniform(-0.02, 0.05), 3)
                fiscal_yoy = round(random.uniform(-0.04, 0.04), 3)
            else:
                gdp_yoy = round(random.uniform(0.025, 0.095), 3)
                income_yoy = round(random.uniform(0.03, 0.11), 3)
                fiscal_yoy = round(random.uniform(0.02, 0.10), 3)

            gdp_value = round(last_gdp * (1 + gdp_yoy)) if year != YEARS[0] else round(last_gdp, 1)
            income_value = round(last_income * (1 + income_yoy)) if year != YEARS[0] else round(last_income)
            percap_value = round(last_percap * (1 + random.uniform(0.015, 0.05))) if year != YEARS[0] else round(last_percap)
            fiscal_value = round(last_fiscal * (1 + fiscal_yoy), 2) if year != YEARS[0] else round(last_fiscal, 2)

            last_gdp = gdp_value
            last_income = income_value
            last_percap = percap_value
            last_fiscal = fiscal_value
            poverty = max(poverty - random.uniform(0.4, 1.5), 0.5)

            indicator_rows.append(
                (
                    county_id,
                    year,
                    round(gdp_value, 1),
                    gdp_yoy,
                    int(percap_value),
                    int(income_value),
                    income_yoy,
                    fiscal_value,
                    fiscal_yoy,
                    round(poverty, 2),
                )
            )
            county_indicators.append(
                {
                    "year": year,
                    "gdp": round(gdp_value, 1),
                    "gdp_yoy": gdp_yoy,
                    "rural_income": int(income_value),
                    "fiscal_yoy": fiscal_yoy,
                }
            )

        indicators_by_county[county_id] = county_indicators

rules = [
    {
        "name": "GDP增速连续下滑预警",
        "metric": "gdp_yoy",
        "comparator": "lt",
        "threshold": 0.02,
        "duration": 2,
    },
    {
        "name": "农村居民收入波动监测",
        "metric": "rural_income",
        "comparator": "lt",
        "threshold": 10000.0,
        "duration": 1,
    },
    {
        "name": "财政收入连续为负",
        "metric": "fiscal_yoy",
        "comparator": "lt",
        "threshold": 0.0,
        "duration": 2,
    },
]

metric_map = {
    "gdp_yoy": "gdp_yoy",
    "rural_income": "rural_income",
    "fiscal_yoy": "fiscal_yoy",
}


def compare(value, comp, threshold):
    if value is None:
        return False
    if comp == "lt":
        return value < threshold
    if comp == "lte":
        return value <= threshold
    if comp == "gt":
        return value > threshold
    if comp == "gte":
        return value >= threshold
    return False


events = []
rule_id = 0
for rule in rules:
    rule_id += 1
    matches = 0
    for cid, entries in indicators_by_county.items():
        duration = rule["duration"]
        if len(entries) < duration:
            continue
        window = entries[-duration:]
        if all(compare(entry[metric_map[rule["metric"]]], rule["comparator"], rule["threshold"]) for entry in window):
            metric_value = window[-1][metric_map[rule["metric"]]]
            events.append(
                {
                    "rule_id": rule_id,
                    "county_id": cid,
                    "year": window[-1]["year"],
                    "metric_value": metric_value,
                }
            )
            matches += 1
            if matches >= 10:
                break

base_dt = datetime(2024, 9, 1, 9, 0)
for idx, event in enumerate(events):
    triggered = base_dt + timedelta(days=idx * 2, hours=random.randint(0, 6))
    event["triggered_at"] = triggered.strftime("%Y-%m-%d %H:%M:%S")
    if idx % 4 == 0:
        ack = triggered + timedelta(days=3, hours=2)
        event["ack_by"] = 2
        event["ack_at"] = ack.strftime("%Y-%m-%d %H:%M:%S")
    else:
        event["ack_by"] = None
        event["ack_at"] = None


lines = []
lines.append(
    "DELETE FROM alert_event;\n"
    "DELETE FROM alert_rule;\n"
    "DELETE FROM economic_indicator;\n"
    "DELETE FROM poverty_county;\n"
    "DELETE FROM province;\n"
    "DELETE FROM sys_user;\n"
    "DELETE FROM role;\n"
    "ALTER TABLE role ALTER COLUMN role_id RESTART WITH 1;\n"
    "ALTER TABLE province ALTER COLUMN province_id RESTART WITH 1;\n"
    "ALTER TABLE poverty_county ALTER COLUMN county_id RESTART WITH 1;\n"
    "ALTER TABLE economic_indicator ALTER COLUMN id RESTART WITH 1;\n"
    "ALTER TABLE alert_rule ALTER COLUMN rule_id RESTART WITH 1;\n"
    "ALTER TABLE alert_event ALTER COLUMN event_id RESTART WITH 1;\n"
    "ALTER TABLE sys_user ALTER COLUMN user_id RESTART WITH 1;\n"
)

lines.append("-- 省份\n")
province_values = ",\n".join([f"('{name}')" for _, name in province_rows])
lines.append(f"INSERT INTO province (province_name) VALUES\n{province_values};\n\n")

lines.append("-- 贫困县\n")
county_values = []
for cid, name, pid, delist in county_rows:
    year_val = "NULL" if delist is None else delist
    county_values.append(f"('{name}', {pid}, {year_val})")
lines.append(
    "INSERT INTO poverty_county (county_name, province_id, delisting_year) VALUES\n"
    + ",\n".join(county_values)
    + ";\n\n"
)

lines.append("-- 经济指标\n")
chunk = 200
for i in range(0, len(indicator_rows), chunk):
    batch = indicator_rows[i : i + chunk]
    rows = []
    for row in batch:
        rows.append(
            f"({row[1]}, {row[0]}, {row[2]}, {row[3]}, {row[4]}, {row[5]}, {row[6]}, {row[7]}, {row[8]}, {row[9]})"
        )
    lines.append(
        "INSERT INTO economic_indicator (year, county_id, gdp, gdp_yoy, gdp_per_capita, rural_disposable_income, rural_income_yoy, fiscal_revenue, fiscal_revenue_yoy, poverty_rate) VALUES\n"
        + ",\n".join(rows)
        + ";\n\n"
    )

lines.append("-- 预警规则\n")
rule_values = []
for rule in rules:
    rule_values.append(
        f"('{rule['name']}', '{rule['metric']}', '{rule['comparator']}', {rule['threshold']}, {rule['duration']}, true)"
    )
lines.append(
    "INSERT INTO alert_rule (rule_name, metric_key, comparator, threshold, duration_years, enabled) VALUES\n"
    + ",\n".join(rule_values)
    + ";\n\n"
)

if events:
    event_rows = []
    for e in events:
        metric_val = f"{e['metric_value']:.3f}" if isinstance(e["metric_value"], float) else f"{e['metric_value']}"
        ack_by = "NULL" if e["ack_by"] is None else str(e["ack_by"])
        ack_at = "NULL" if e["ack_at"] is None else f"'{e['ack_at']}'"
        event_rows.append(
            f"({e['rule_id']}, {e['county_id']}, {e['year']}, {metric_val}, '{e['triggered_at']}', {ack_by}, {ack_at})"
        )
    lines.append("-- 告警事件\n")
    lines.append(
        "INSERT INTO alert_event (rule_id, county_id, year, metric_value, triggered_at, acknowledged_by, acknowledged_at) VALUES\n"
        + ",\n".join(event_rows)
        + ";\n\n"
    )

content = "\n".join(lines)
output = Path("../backend/src/main/resources/data.sql")
output.write_text(content, encoding="utf-8")
print(f"生成省份 {len(province_rows)} 条，县 {len(county_rows)} 条，指标 {len(indicator_rows)} 条，写入 {output}")
