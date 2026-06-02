# -*- coding: utf-8 -*-
import zipfile, re
from xml.etree import ElementTree as ET

p = '.claude/requests/ref/prafta-030/연차_재할당_18개_케이스_정리.xlsx'
z = zipfile.ZipFile(p)
ns = '{http://schemas.openxmlformats.org/spreadsheetml/2006/main}'
wb = ET.fromstring(z.read('xl/workbook.xml').decode('utf-8'))
sheets = [s.get('name') for s in wb.iter(ns + 'sheet')]

def col_to_idx(ref):
    m = re.match(r'([A-Z]+)(\d+)', ref)
    letters = m.group(1)
    idx = 0
    for c in letters:
        idx = idx * 26 + (ord(c) - ord('A') + 1)
    return idx - 1, int(m.group(2))

out = []
for i, name in enumerate(sheets, start=1):
    path = 'xl/worksheets/sheet%d.xml' % i
    out.append('\n' + '=' * 70)
    out.append('SHEET %d: %s' % (i, name))
    out.append('=' * 70)
    root = ET.fromstring(z.read(path).decode('utf-8'))
    for row in root.iter(ns + 'row'):
        cells = []
        maxc = 0
        rowmap = {}
        for c in row.findall(ns + 'c'):
            ref = c.get('r')
            t = c.get('t')
            val = ''
            if t == 'inlineStr':
                is_el = c.find(ns + 'is')
                if is_el is not None:
                    val = ''.join(node.text or '' for node in is_el.iter(ns + 't'))
            else:
                v = c.find(ns + 'v')
                if v is not None:
                    val = v.text or ''
            if ref:
                ci, ri = col_to_idx(ref)
                rowmap[ci] = val
                if ci > maxc:
                    maxc = ci
        if rowmap:
            line = ' | '.join((rowmap.get(j, '') or '').strip() for j in range(maxc + 1))
            if line.strip(' |'):
                out.append(line)

with open('.claude/requests/ref/prafta-030/_xlsx_dump.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))
print('done', len(out), 'lines')
