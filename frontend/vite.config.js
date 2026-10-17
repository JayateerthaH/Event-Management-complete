import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    // node_modules may be a symlink to node_modules.nosync (kept out of iCloud); don't watch it
    watch: { ignored: ["**/node_modules.nosync/**"] },
    // Only proxy paths that are EXCLUSIVELY backend API endpoints.
    // React Router handles /home, /events, /dashboard as SPA page routes.
    proxy: {
      "/auth": { target: "http://localhost:8080", changeOrigin: true },

      // Image files and image APIs must pass via gateway so auth filters
      // can inject internal headers required by downstream services.
      "/images": { target: "http://localhost:8080", changeOrigin: true },
      "/event/images": { target: "http://localhost:8080", changeOrigin: true },

      // Other upload routes go through the gateway.
      "/event/organizer/create": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/certificate/organizer/upload-certificate-template": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
