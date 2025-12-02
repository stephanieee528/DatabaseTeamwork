import json
from pathlib import Path
text = Path('county_sample.json').read_text(encoding='utf-8')
data = json.loads(text)
print(data[0]['countyName'])
