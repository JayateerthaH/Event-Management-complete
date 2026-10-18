import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import ScrollToTop from "./components/ScrollToTop";
import PageLoader from "./components/PageLoader";
import Home from "./pages/Home";

// Every page except the landing page is downloaded when first opened, so a phone loading the home page
// doesn't fetch the dashboards, charts and QR scanner too (one 1.3 MB bundle before)
const RoleBasedRoute = lazy(() => import("./components/dashboard/RoleBasedRoute"));
const Login = lazy(() => import("./pages/Login"));
const Signup = lazy(() => import("./pages/Signup"));
const Profile = lazy(() => import("./pages/Profile"));
const Friends = lazy(() => import("./pages/Friends"));
const Welcome = lazy(() => import("./pages/Welcome"));
const Settings = lazy(() => import("./pages/Settings"));
const Tickets = lazy(() => import("./pages/Tickets"));
const Portfolio = lazy(() => import("./pages/Portfolio"));
const Wrapped = lazy(() => import("./pages/Wrapped"));
const EventsPage = lazy(() => import("./pages/EventsPage"));
const EventDetailsPage = lazy(() => import("./pages/EventDetailsPage"));
const EventManagePage = lazy(() => import("./pages/dashboard/organizer/EventManagePage"));
const QRScanner = lazy(() => import("./components/QRScanner"));

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <AuthProvider>
        <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />
          <Route path="/home" element={<Home />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/events/:eventId" element={<EventDetailsPage />} />
          <Route
            path="/dashboard/manage-event/:eventId"
            element={
              <ProtectedRoute>
                <EventManagePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/scan"
            element={
              <ProtectedRoute>
                <QRScanner />
              </ProtectedRoute>
            }
          />
          <Route
            path="/scan/:eventId"
            element={
              <ProtectedRoute>
                <QRScanner />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <RoleBasedRoute />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            }
          />
          {/* Not behind the login redirect: it must open offline from the saved copy */}
          <Route path="/tickets" element={<Tickets />} />
          <Route path="/p/:slug" element={<Portfolio />} />
          <Route
            path="/wrapped"
            element={
              <ProtectedRoute>
                <Wrapped />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <Settings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/welcome"
            element={
              <ProtectedRoute>
                <Welcome />
              </ProtectedRoute>
            }
          />
          <Route
            path="/friends"
            element={
              <ProtectedRoute>
                <Friends />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  );
}
