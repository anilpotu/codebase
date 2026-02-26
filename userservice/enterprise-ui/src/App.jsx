import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import UsersPage from './pages/users/UsersPage';
import AccountsPage from './pages/financial/AccountsPage';
import TransactionsPage from './pages/financial/TransactionsPage';
import HealthRecordsPage from './pages/health/HealthRecordsPage';
import VitalsPage from './pages/health/VitalsPage';
import ProfilesPage from './pages/social/ProfilesPage';
import PostsPage from './pages/social/PostsPage';
import ConnectionsPage from './pages/social/ConnectionsPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="health-records" element={<HealthRecordsPage />} />
        <Route path="vitals" element={<VitalsPage />} />
        <Route path="profiles" element={<ProfilesPage />} />
        <Route path="posts" element={<PostsPage />} />
        <Route path="connections" element={<ConnectionsPage />} />
      </Route>
    </Routes>
  );
}
