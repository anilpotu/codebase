export default function Dashboard() {
  const services = [
    { name: 'User Service', port: 8080, type: 'gRPC + REST', desc: 'User management (create, get, delete)' },
    { name: 'Financial Service', port: 8081, type: 'REST', desc: 'Accounts and transactions' },
    { name: 'Health Service', port: 8082, type: 'REST', desc: 'Health records and vitals' },
    { name: 'Social Service', port: 8083, type: 'REST', desc: 'Profiles, posts, and connections' },
  ];

  return (
    <div>
      <h1>Enterprise Platform Dashboard</h1>
      <p>Manage all microservices from a single interface.</p>
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
