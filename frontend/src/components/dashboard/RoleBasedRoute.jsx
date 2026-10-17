import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { resolveRole } from '../../utils/roleResolver';
import { lazy } from 'react';

// Each role downloads only its own dashboard (App.jsx's Suspense shows the loader meanwhile)
const StudentDashboard = lazy(() => import('../../pages/dashboard/student/StudentDashboard'));
const OrganizerDashboard = lazy(() => import('../../pages/dashboard/organizer/OrganizerDashboard'));
const AdminDashboard = lazy(() => import('../../pages/dashboard/admin/AdminDashboard'));

/**
 * Reads the user from AuthContext, resolves the highest-priority role,
 * and renders the corresponding dashboard.
 *
 * - ADMIN     → AdminDashboard
 * - ORGANIZER → OrganizerDashboard
 * - STUDENT   → StudentDashboard
 * - null      → redirect to /login
 */
export default function RoleBasedRoute() {
  const { user } = useAuth();

  // Not logged in
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const role = resolveRole(user);

  switch (role) {
    case 'ADMIN':
      return <AdminDashboard />;
    case 'ORGANIZER':
      return <OrganizerDashboard />;
    case 'STUDENT':
      return <StudentDashboard />;
    default:
      // Unknown / missing role — default to student if authenticated,
      // otherwise redirect.
      return <StudentDashboard />;
  }
}
