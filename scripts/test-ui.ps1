$ErrorActionPreference = 'Stop'
$workspacePath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
docker run --rm --mount "type=bind,source=$workspacePath,target=/workspace,readonly" -e HMS_ROOT=/workspace node:22-alpine sh -c 'mkdir /tmp/ui-checks && cp /workspace/src/test/ui/* /tmp/ui-checks/ && cd /tmp/ui-checks && npm ci --ignore-scripts --no-audit --no-fund && npm test'
if ($LASTEXITCODE -ne 0) { throw 'UI interaction tests failed.' }
