import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import './index.css'
import App from './App.jsx'
import './styles/Mobile.css' // after App so its phone fixes override page styles
import { startResponsiveTables } from './utils/responsiveTables'
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

startResponsiveTables()

// After a redeploy, a page that was already open asks for page code that no longer exists. Reload once to
// get the new version instead of showing an error (at most once a minute, so it can't loop).
window.addEventListener('vite:preloadError', (event) => {
  try {
    const last = Number(sessionStorage.getItem('chunk-reload-at') || 0)
    if (Date.now() - last > 60_000) {
      sessionStorage.setItem('chunk-reload-at', String(Date.now()))
      event.preventDefault()
      window.location.reload()
    }
  } catch {
    /* storage blocked: let the error boundary show the error */
  }
})
