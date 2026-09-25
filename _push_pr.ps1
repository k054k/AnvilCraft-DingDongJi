# DDJ 0.0.9 commit + push helper (run once, then this file and _commit_msg.txt can be deleted)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath 'D:\桌面\ddj090-pr'

Write-Host '== git status before commit ==' -ForegroundColor Cyan
git status --short

git add build.gradle src/
git commit -F _commit_msg.txt

Write-Host '== pushing feat/glow-band-config ==' -ForegroundColor Cyan
git push origin feat/glow-band-config

Write-Host ''
Write-Host '== DONE. Open this URL to create the PR (main <- feat/glow-band-config) ==' -ForegroundColor Green
Write-Host 'https://github.com/k054k/AnvilCraft-DingDongJi/compare/main...feat/glow-band-config?expand=1'
