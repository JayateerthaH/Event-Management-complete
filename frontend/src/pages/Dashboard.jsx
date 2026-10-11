import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/Dashboard.css';

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="dashboard-page">
      <nav className="dashboard-nav">
        <div className="nav-brand">MyApp</div>
        <div className="nav-user">
          <img
            src={user?.picture}
            alt={user?.name}
            className="nav-avatar"
            referrerPolicy="no-referrer"
          />
          <span className="nav-username">{user?.name}</span>
          <button className="logout-btn" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </nav>

      <main className="dashboard-main">
        <div className="welcome-card">
          <img
            src={user?.picture}
            alt={user?.name}
            className="profile-avatar"
            referrerPolicy="no-referrer"
          />
          <h1 className="welcome-title">Welcome, {user?.name?.split(' ')[0]}!</h1>
          <p className="welcome-email">{user?.email}</p>
          <div className="welcome-badge">
            <span className="badge-dot"></span>
            Authenticated via Google
          </div>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon stat-icon--blue">
              <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" fill="currentColor"/></svg>
            </div>
            <div className="stat-info">
              <p className="stat-label">Account Status</p>
              <p className="stat-value">Active</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon stat-icon--green">
              <svg viewBox="0 0 24 24" fill="none"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 4l6 2.67V11c0 3.67-2.45 7.13-6 8.29-3.55-1.16-6-4.62-6-8.29V7.67L12 5z" fill="currentColor"/></svg>
            </div>
            <div className="stat-info">
              <p className="stat-label">Security</p>
              <p className="stat-value">Protected</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon stat-icon--purple">
              <svg viewBox="0 0 24 24" fill="none"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" fill="currentColor"/></svg>
            </div>
            <div className="stat-info">
              <p className="stat-label">Auth Provider</p>
              <p className="stat-value">Google</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
