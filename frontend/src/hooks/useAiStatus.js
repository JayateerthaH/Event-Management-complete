import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { fetchAiStatus } from "../api/aiService";

/** Whether LLM-backed features can work for the current user ({ llmEnabled, model, loaded }). */
export default function useAiStatus() {
  const { token } = useAuth();
  const [status, setStatus] = useState({ llmEnabled: false, model: "none", loaded: false });

  useEffect(() => {
    let cancelled = false;
    fetchAiStatus().then((s) => {
      if (!cancelled) setStatus({ ...s, loaded: true });
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  return status;
}
