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
// Leaving a page also cancels its pending page-code downloads (Safari reports those as preload errors);
// that must not reload the page being left, so ignore errors while leaving and let a navigation win.
let leaving = false
window.addEventListener('beforeunload', () => { leaving = true })
window.addEventListener('pagehide', () => { leaving = true })
window.addEventListener('pageshow', () => { leaving = false })
window.addEventListener('vite:preloadError', (event) => {
  if (leaving) {
    event.preventDefault()
    return
  }
  try {
    const last = Number(sessionStorage.getItem('chunk-reload-at') || 0)
    if (Date.now() - last > 60_000) {
      event.preventDefault()
      setTimeout(() => {
        if (leaving) return
        sessionStorage.setItem('chunk-reload-at', String(Date.now()))
        window.location.reload()
      }, 400)
    }
  } catch {
    /* storage blocked: let the error boundary show the error */
  }
})

// Installable app: the service worker keeps the app shell and the tickets page available offline.
// Once it controls the page, load the tickets page code so it's cached before the student needs it.
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  // Registered a few seconds after load, when the browser is idle, so installing it never competes with
  // the first page load or an immediate navigation (WebKit can abort a navigation that overlaps it).
  const register = () => navigator.serviceWorker
      .register('/sw.js')
      .then(() => {
        const warm = () => {
          const loaded = performance.getEntriesByType('resource').map((r) => r.name).filter((u) => u.includes('/assets/'))
          navigator.serviceWorker.controller?.postMessage({ type: 'cache-assets', urls: loaded })
          import('./pages/Tickets.jsx').catch(() => {})
        }
        if (navigator.serviceWorker.controller) warm()
        else navigator.serviceWorker.addEventListener('controllerchange', warm, { once: true })
      })
      .catch(() => {})
  window.addEventListener('load', () => {
    const idle = window.requestIdleCallback || ((fn) => setTimeout(fn, 1))
    setTimeout(() => idle(register), 4000)
  })
}
