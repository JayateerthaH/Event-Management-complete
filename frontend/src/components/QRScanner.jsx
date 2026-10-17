import React, { useState, useEffect, useRef } from "react";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import { Html5Qrcode } from "html5-qrcode";
import api from "../api/apiClient";
import Navbar from "./Navbar";
import "../styles/QRScanner.css";

const toNumericIfPossible = (value) => {
  const n = Number(value);
  return Number.isNaN(n) ? value : n;
};

const extractEventIdFromScannedData = (scannedData) => {
  if (typeof scannedData !== "string") return "";

  const text = scannedData.trim();
  if (!text) return "";

  const directMatch = text.match(/(?:eventId|event_id|event)[:=](\d+)/i);
  if (directMatch?.[1]) return directMatch[1];

  try {
    const parsed = JSON.parse(text);
    const raw = parsed?.eventId ?? parsed?.event_id ?? parsed?.event;
    if (raw !== undefined && raw !== null && String(raw).trim() !== "") {
      return String(raw).trim();
    }
  } catch {
    // Not JSON; try URL parse below.
  }

  try {
    const url = new URL(text);
    const fromQuery =
      url.searchParams.get("eventId") ||
      url.searchParams.get("event_id") ||
      url.searchParams.get("event") || 
      url.searchParams.get("id");
    if (fromQuery) return fromQuery;
  } catch {
    // Not URL; no-op.
  }

  return "";
};

export default function QRScanner() {
  const { eventId: pathEventId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  // Priority: 1. URL path param, 2. URL query param
  const eventId = pathEventId || searchParams.get("eventId") || "";

  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const html5QrRef = useRef(null);
  const processingRef = useRef(false);

  const markAttendance = async (scannedData) => {
    if (processingRef.current) return;
    processingRef.current = true;
    
    // Clear previous results
    setResult(null);
    setError(null);

    try {
      // 1. Determine Event ID
      let resolvedEventId = eventId;
      if (!resolvedEventId) {
        resolvedEventId = extractEventIdFromScannedData(scannedData);
      }

      const payload = { scannedData };
      if (resolvedEventId) {
        payload.eventId = toNumericIfPossible(resolvedEventId);
      }

      console.log("Marking attendance payload:", payload);

      const { data } = await api.post("/attendance/mark", payload);
      setResult({
        success: true,
        message:
          typeof data === "string" ? data : data?.message || "Attendance marked successfully!",
      });
      
      // Pause (not stop) on success to prevent a double scan; "Scan Next" resumes the same camera
      if (html5QrRef.current) {
        html5QrRef.current.pause(true);
      }
    } catch (err) {
      console.error("Attendance error:", err);
      // Backend errors arrive as ApiError { error, statusCode }
      const rawMsg =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        (typeof err?.response?.data === "string" ? err.response.data : null) ||
        err?.message ||
        "Failed to mark attendance";

      let userMsg = rawMsg;
      // Handle "Multiple registrations" error specially
      if (
        typeof rawMsg === "string" &&
        rawMsg.toLowerCase().includes("did not return a unique result")
      ) {
        userMsg = "Multiple registrations matched this QR. Please open the scanner from the specific Event Dashboard to ensure the correct event is targeted.";
      }

      setResult({
        success: false,
        message: typeof userMsg === "string" ? userMsg : "Failed to mark attendance",
      });
      
      // Pause briefly on error so user can read it, then resume if user wants
      if (html5QrRef.current) {
        html5QrRef.current.pause(true);
        setTimeout(() => {
           // Auto-resume after 3s if still scanning? Better to let user click "Scan Next"
           // but for errors, maybe auto-resume. 
           // For now, let's keep it paused until user acts.
        }, 3000);
      }
    } finally {
      processingRef.current = false;
    }
  };

  const startScanner = async () => {
    try {
      setResult(null);
      setError(null);
      setScanning(true);

      const codes = await Html5Qrcode.getCameras();
      if (codes && codes.length) {
        const cameraId = codes[0].id;
        const html5QrCode = new Html5Qrcode("reader");
        html5QrRef.current = html5QrCode;

        await html5QrCode.start(
          cameraId,
          {
            fps: 10,
            qrbox: { width: 250, height: 250 },
          },
          (decodedText) => {
            if (!processingRef.current) {
              markAttendance(decodedText);
            }
          },
          () => {
            // per-frame "no QR found" callbacks — ignore
          }
        );
      } else {
        setError("No cameras found.");
        setScanning(false);
      }
    } catch (err) {
      setError("Failed to start scanner: " + err.message);
      setScanning(false);
    }
  };

  const stopScanner = async () => {
    if (html5QrRef.current) {
      try {
        await html5QrRef.current.stop();
        html5QrRef.current.clear();
        html5QrRef.current = null;
      } catch (err) {
        console.error("Failed to stop scanner", err);
      }
    }
    setScanning(false);
  };

  useEffect(() => {
    return () => {
      if (html5QrRef.current) {
         html5QrRef.current.stop().then(() => html5QrRef.current.clear()).catch(console.error);
      }
    };
  }, []);

  return (
    <div className="qr-scanner-page">
      <Navbar />
      <div className="qr-container">
         <div className="qr-card">
           <div className="qr-header">
             <h2>Scan QR Code</h2>
             {eventId && <div className="qr-context-badge">Event ID: {eventId}</div>}
             {!eventId && <div className="qr-warning">No specific event selected. Ensure attendee is registered for only one event.</div>}
           </div>

           <div id="reader" className="qr-reader-box"></div>

           <div className="qr-controls">
             {!scanning ? (
               <button className="qr-btn qr-btn--primary" onClick={startScanner}>Start Scanner</button>
             ) : (
               <button className="qr-btn qr-btn--danger" onClick={stopScanner}>Stop Scanner</button>
             )}
             <button className="qr-btn qr-btn--secondary" onClick={() => navigate(-1)}>Back</button>
           </div>

           {result && (
             <div className={`qr-result ${result.success ? "success" : "error"}`}>
               <p>{result.message}</p>
               <button className="qr-btn-small" onClick={() => {
                  setResult(null);
                  // The camera is only paused after a scan; resume it instead of starting a second instance
                  if (html5QrRef.current) {
                    html5QrRef.current.resume();
                    setScanning(true);
                  } else {
                    startScanner();
                  }
               }}>Scan Next</button>
             </div>
           )}
           
           {error && <div className="qr-result error"><p>{error}</p></div>}
         </div>
      </div>
    </div>
  );
}

