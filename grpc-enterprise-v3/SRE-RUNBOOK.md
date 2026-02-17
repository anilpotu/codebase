
# SRE Runbook v3

## SLOs
- 99.95% Availability
- <150ms P95 latency
- Error rate <0.5%

## Rollback Strategy
- Shift traffic in Istio VirtualService
- Or revert Git commit (ArgoCD auto-sync)

## Monitoring Stack
- Prometheus
- Grafana
- Jaeger
- ELK
