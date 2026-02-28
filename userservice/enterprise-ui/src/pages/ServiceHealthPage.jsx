import { useMemo, useState } from 'react';

const CHECK_TIMEOUT_MS = 5000;

const serviceChecks = [
  { id: 'api-gateway', name: 'API Gateway', method: 'GET', path: '/actuator/health' },
  { id: 'auth-service', name: 'Auth Service', method: 'GET', path: '/health/auth-service' },
  { id: 'user-service', name: 'User Service', method: 'GET', path: '/health/user-service' },
  { id: 'order-service', name: 'Order Service', method: 'GET', path: '/health/order-service' },
  { id: 'product-service', name: 'Product Service', method: 'GET', path: '/health/product-service' },
  { id: 'user-grpc-service', name: 'User gRPC Service', method: 'GET', path: '/health/user-grpc-service' },
  { id: 'financial-service', name: 'Financial Service', method: 'GET', path: '/health/financial-service' },
  { id: 'health-service', name: 'Health Service', method: 'GET', path: '/health/health-service' },
  { id: 'social-service', name: 'Social Service', method: 'GET', path: '/health/social-service' },
];

function statusClass(status) {
  if (status === 'UP') return 'badge badge-accepted';
  if (status === 'DOWN') return 'badge badge-pending';
  return 'badge';
}

async function runCheck(check) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), CHECK_TIMEOUT_MS);
  const startedAt = performance.now();

  try {
    const options = {
      method: check.method,
      headers: { 'Content-Type': 'application/json' },
      signal: controller.signal,
    };

    if (check.body) {
      options.body = JSON.stringify(check.body);
    }

    const response = await fetch(check.path, options);
    const durationMs = Math.round(performance.now() - startedAt);

    const isHealthy = response.ok;

    return {
      ...check,
      status: isHealthy ? 'UP' : 'DOWN',
      httpStatus: response.status,
      durationMs,
      detail: isHealthy
        ? 'Healthy response'
        : 'Health endpoint returned non-2xx',
      checkedAt: new Date().toISOString(),
    };
  } catch (error) {
    const durationMs = Math.round(performance.now() - startedAt);
    const isAbort = error?.name === 'AbortError';
    return {
      ...check,
      status: 'DOWN',
      httpStatus: null,
      durationMs,
      detail: isAbort ? `Timed out after ${CHECK_TIMEOUT_MS}ms` : (error?.message || 'Network error'),
      checkedAt: new Date().toISOString(),
    };
  } finally {
    clearTimeout(timeout);
  }
}

export default function ServiceHealthPage() {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);

  const summary = useMemo(() => {
    const up = results.filter((r) => r.status === 'UP').length;
    const down = results.filter((r) => r.status === 'DOWN').length;
    return { up, down, total: results.length };
  }, [results]);

  const runAllChecks = async () => {
    setLoading(true);
    const checkResults = await Promise.all(serviceChecks.map(runCheck));
    setResults(checkResults);
    setLoading(false);
  };

  return (
    <div>
      <h1>Service Health</h1>
      <p>Live connectivity checks across the userservice platform.</p>

      <div className="section">
        <button className="btn" type="button" onClick={runAllChecks} disabled={loading}>
          {loading ? 'Running checks...' : 'Run Health Checks'}
        </button>
        {results.length > 0 && (
          <p className="text-muted" style={{ marginTop: 10 }}>
            Summary: {summary.up} UP / {summary.down} DOWN / {summary.total} total
          </p>
        )}
      </div>

      {results.length > 0 && (
        <div className="section">
          <h2>Results</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Service</th>
                <th>Status</th>
                <th>HTTP</th>
                <th>Latency</th>
                <th>Endpoint</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {results.map((result) => (
                <tr key={result.id}>
                  <td>{result.name}</td>
                  <td><span className={statusClass(result.status)}>{result.status}</span></td>
                  <td>{result.httpStatus ?? 'N/A'}</td>
                  <td>{result.durationMs} ms</td>
                  <td>{result.method} {result.path}</td>
                  <td>{result.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
