export default function Dashboard() {
  const services = [
    { name: 'Config Server', port: 8888, type: 'Infra', desc: 'Centralized configuration source' },
    { name: 'Eureka Server', port: 8761, type: 'Infra', desc: 'Service discovery registry' },
    { name: 'API Gateway', port: 8000, type: 'Infra', desc: 'Unified entrypoint and routing' },
    { name: 'Auth Service', port: 8080, type: 'Core REST', desc: 'Registration, login, token lifecycle' },
    { name: 'User Service', port: 8081, type: 'Core REST', desc: 'User profile management' },
    { name: 'Order Service', port: 8082, type: 'Core REST', desc: 'Order creation and lifecycle' },
    { name: 'Product Service', port: 8083, type: 'Core REST', desc: 'Catalog, inventory, search' },
    { name: 'User gRPC Service', port: '8090/9090', type: 'REST + gRPC', desc: 'Enterprise user operations' },
    { name: 'Financial Service', port: 8084, type: 'Enterprise REST', desc: 'Accounts and transactions' },
    { name: 'Health Service', port: 8085, type: 'Enterprise REST', desc: 'Health records and vitals' },
    { name: 'Social Service', port: 8086, type: 'Enterprise REST', desc: 'Profiles, posts, connections' },
  ];

  return (
    <div>
      <h1>Userservice Platform Dashboard</h1>
      <p>Coverage for all microservices in the userservice project.</p>
      <div className="card-grid">
        {services.map((svc) => (
          <div key={svc.name} className="card">
            <h3>{svc.name}</h3>
            <p className="text-muted">Port: {svc.port} | {svc.type}</p>
            <p>{svc.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
