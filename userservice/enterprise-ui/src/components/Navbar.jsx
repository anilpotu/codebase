import { NavLink } from 'react-router-dom';

export default function Navbar() {
  const sections = [
    {
      title: 'Overview',
      links: [
        { to: '/', label: 'Dashboard' },
        { to: '/service-health', label: 'Service Health' },
      ],
    },
    {
      title: 'Core Services',
      links: [
        { to: '/auth', label: 'Auth' },
        { to: '/grpc-users', label: 'gRPC Users' },
        { to: '/users', label: 'User Profiles' },
        { to: '/products', label: 'Products' },
        { to: '/orders', label: 'Orders' },
      ],
    },
    {
      title: 'Enterprise Services',
      links: [
        { to: '/accounts', label: 'Accounts' },
        { to: '/transactions', label: 'Transactions' },
        { to: '/health-records', label: 'Health Records' },
        { to: '/vitals', label: 'Vitals' },
        { to: '/profiles', label: 'Profiles' },
        { to: '/posts', label: 'Posts' },
        { to: '/connections', label: 'Connections' },
      ],
    },
  ];

  return (
    <nav className="navbar">
      <div className="navbar-brand-wrap">
        <div className="navbar-brand">Enterprise Platform</div>
        <div className="navbar-subtitle">Operations Console</div>
      </div>

      {sections.map((section) => (
        <div className="nav-section" key={section.title}>
          <div className="nav-section-title">{section.title}</div>
          <ul className="navbar-links">
            {section.links.map((link) => (
              <li key={link.to}>
                <NavLink
                  to={link.to}
                  className={({ isActive }) => (isActive ? 'active' : '')}
                >
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
