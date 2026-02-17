import { NavLink } from 'react-router-dom';

export default function Navbar() {
  const links = [
    { to: '/', label: 'Dashboard' },
    { to: '/users', label: 'Users' },
    { to: '/accounts', label: 'Accounts' },
    { to: '/transactions', label: 'Transactions' },
    { to: '/health-records', label: 'Health Records' },
    { to: '/vitals', label: 'Vitals' },
    { to: '/profiles', label: 'Profiles' },
    { to: '/posts', label: 'Posts' },
    { to: '/connections', label: 'Connections' },
  ];

  return (
    <nav className="navbar">
      <div className="navbar-brand">Enterprise Platform</div>
      <ul className="navbar-links">
        {links.map((link) => (
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
    </nav>
  );
}
