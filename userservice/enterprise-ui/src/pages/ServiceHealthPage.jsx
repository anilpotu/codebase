import { useMemo, useState } from 'react';

const CHECK_TIMEOUT_MS = 5000;

const serviceChecks = [
  { id: 'api-gateway', name: 'API Gateway', method: 'GET', path: '/api/does-not-exist' },
  { id: 'auth-service', name: 'Auth Service', method: 'POST', path: '/api/auth/login', body: { username: 'health-check', password: 'health-check' } },
  { id: 'user-service', name: 'User Service', method: 'GET', path: '/api/users/me' },
  { id: 'order-service', name: 'Order Service', method: 'GET', path: '/api/orders' },
  { id: 'product-service', name: 'Product Service', method: 'GET', path: '/api/products' },
  { id: 'user-grpc-service', name: 'User gRPC Service', method: 'GET', path: '/api/grpc-users/1' },
  { id: 'financial-service', name: 'Financial Service', method: 'GET', path: '/api/accounts/user/1' },
  { id: 'health-service', name: 'Health Service', method: 'GET', path: '/api/health-records/user/1' },
  { id: 'social-service', name: 'Social Service', method: 'GET', path: '/api/posts/user/1' },
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

    const isServerError = response.status >= 500;
    const isAuthOrNotFound = [401, 403, 404].includes(response.status);

    return {
      ...check,
      status: isServerError ? 'DOWN' : 'UP',
      httpStatus: response.status,
      durationMs,
      detail: response.ok
        ? 'Healthy response'
        : isAuthOrNotFound
          ? 'Endpoint reachable (auth/not-found response)'
          : isServerError
            ? 'Server error from endpoint'
            : 'Endpoint reachable (non-2xx response)',
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
