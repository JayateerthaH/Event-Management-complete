import { lazy, Suspense, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import ScrollToTop from "./components/ScrollToTop";
import PageLoader from "./components/PageLoader";
import Home from "./pages/Home";

// Every page except the landing page is downloaded when first opened, so a phone loading the home page
// doesn't fetch the dashboards, charts and QR scanner too (one 1.3 MB bundle before)
const load = {
  RoleBasedRoute: () => import("./components/dashboard/RoleBasedRoute"),
  Login: () => import("./pages/Login"),
  Signup: () => import("./pages/Signup"),
  Profile: () => import("./pages/Profile"),
  Friends: () => import("./pages/Friends"),
  Welcome: () => import("./pages/Welcome"),
  Settings: () => import("./pages/Settings"),
  Tickets: () => import("./pages/Tickets"),
  Portfolio: () => import("./pages/Portfolio"),
  Wrapped: () => import("./pages/Wrapped"),
  EventsPage: () => import("./pages/EventsPage"),
  EventDetailsPage: () => import("./pages/EventDetailsPage"),
  EventManagePage: () => import("./pages/dashboard/organizer/EventManagePage"),
  QRScanner: () => import("./components/QRScanner"),
};
const RoleBasedRoute = lazy(load.RoleBasedRoute);
const Login = lazy(load.Login);
const Signup = lazy(load.Signup);
const Profile = lazy(load.Profile);
const Friends = lazy(load.Friends);
const Welcome = lazy(load.Welcome);
const Settings = lazy(load.Settings);
const Tickets = lazy(load.Tickets);
const Portfolio = lazy(load.Portfolio);
const Wrapped = lazy(load.Wrapped);
const EventsPage = lazy(load.EventsPage);
const EventDetailsPage = lazy(load.EventDetailsPage);
const EventManagePage = lazy(load.EventManagePage);
const QRScanner = lazy(load.QRScanner);

// Once the first page is up and the browser is idle, fetch the pages people open next, so tapping a link
// switches at once instead of showing "Loading…". Skipped on data saver; the QR scanner and manage page stay on demand.
const PREFETCH = ["EventsPage", "EventDetailsPage", "RoleBasedRoute", "Login", "Signup", "Settings", "Friends", "Profile", "Tickets", "Wrapped"];
function usePrefetchPages() {
  useEffect(() => {
    if (navigator.connection?.saveData) return;
    const idle = window.requestIdleCallback || ((fn) => setTimeout(fn, 1500));
    const cancel = window.cancelIdleCallback || clearTimeout;
    let i = 0;
    let handle;
    const next = () => {
      if (i >= PREFETCH.length) return;
      load[PREFETCH[i++]]().catch(() => {}).finally(() => { handle = idle(next); });
    };
    const start = setTimeout(() => { handle = idle(next); }, 2000);
    return () => { clearTimeout(start); if (handle) cancel(handle); };
  }, []);
}

export default function App() {
  usePrefetchPages();
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
