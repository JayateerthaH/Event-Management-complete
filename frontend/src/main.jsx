import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import './index.css'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import { GOOGLE_CLIENT_ID, GOOGLE_ENABLED } from './config/google'


createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
      {/* Google's script is only loaded when sign-in with Google is configured (VITE_GOOGLE_CLIENT_ID) */}
      {GOOGLE_ENABLED ? (
        <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
          <App />
        </GoogleOAuthProvider>
      ) : (
        <App />
      )}
    </ErrorBoundary>
  </StrictMode>,
)
