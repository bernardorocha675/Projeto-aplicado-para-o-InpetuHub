"""Consulta OSV para as dependências efetivamente resolvidas pelo Maven; falha se não consultar."""
import json
import pathlib
import sys
import urllib.request

source = pathlib.Path('target/dependencies.txt')
if not source.is_file():
    raise SystemExit('Execute primeiro: mvn dependency:list -DincludeScope=runtime -DoutputFile=target/dependencies.txt')
packages = set()
for line in source.read_text().splitlines():
    coordinate = line.strip().split(' ')[0].split(':')
    if len(coordinate) in (5, 6) and coordinate[-1] in ('compile', 'runtime'):
        packages.add((coordinate[0] + ':' + coordinate[1], coordinate[-2]))
if not packages:
    raise SystemExit('Nenhuma dependência identificada; a análise não foi concluída.')
findings = []
items = sorted(packages)
for start in range(0, len(items), 100):
    batch = items[start:start + 100]
    body = {'queries': [{'package': {'ecosystem': 'Maven', 'name': name}, 'version': version} for name, version in batch]}
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch', json.dumps(body).encode(), {'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            data = json.load(response)
    except Exception:
        raise SystemExit('Não foi possível consultar OSV; não interpretar como ausência de vulnerabilidades.')
    results = data.get('results', [])
    if len(results) != len(batch):
        raise SystemExit('Resposta OSV incompleta; análise recusada.')
    for (name, version), result in zip(batch, results):
        for issue in result.get('vulns', []):
            findings.append({'package': name, 'version': version, 'id': issue['id']})
report = {'packages': len(items), 'findings': findings, 'source': 'https://api.osv.dev/v1/querybatch'}
pathlib.Path('target/dependency-security.json').write_text(json.dumps(report, indent=2))
print(f'Dependências consultadas: {len(items)}; alertas: {len(findings)}.')
for finding in findings:
    print(f"{finding['package']} {finding['version']}: {finding['id']}")
sys.exit(1 if findings else 0)
