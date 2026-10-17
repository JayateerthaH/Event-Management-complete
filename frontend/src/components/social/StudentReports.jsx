import { useCallback, useEffect, useState } from "react";
import { apiErrorMessage } from "../../api/apiClient";
import { getStudentReports, resolveStudentReport } from "../../api/socialService";

/** Admin dashboard: reports students made about each other from Event buddies and Friends. */
export default function StudentReports() {
  const [reports, setReports] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    getStudentReports().then(setReports).catch((e) => setError(apiErrorMessage(e)));
  }, []);
  useEffect(load, [load]);

  const resolve = (id) => resolveStudentReport(id).then(load).catch((e) => setError(apiErrorMessage(e)));

  return (
    <section className="admin-card">
      <h2 className="admin-card__title">Reports / Flags</h2>
      <p className="admin-empty" style={{ textAlign: "left", padding: "0 0 0.75rem" }}>
        Students' reports about other students. The reporter has already blocked the person they reported.
      </p>
      {error && <p className="admin-empty" role="alert">{error}</p>}
      {!reports ? (
        !error && <p className="admin-empty">Loading…</p>
      ) : reports.length === 0 ? (
        <p className="admin-empty">No reports.</p>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Reported student</th>
                <th>Reported by</th>
                <th>What happened</th>
                <th>Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id}>
                  <td>{r.reportedName} <small>(#{r.reportedId})</small></td>
                  <td>{r.reporterName} <small>(#{r.reporterId})</small></td>
                  <td style={{ whiteSpace: "pre-wrap", maxWidth: 360 }}>{r.reason}</td>
                  <td>{r.createdAt ? new Date(r.createdAt).toLocaleString() : "--"}</td>
                  <td>
                    {r.status === "OPEN" ? (
                      <button type="button" className="admin-accept-btn" onClick={() => resolve(r.id)}>
                        Mark resolved
                      </button>
                    ) : (
                      <span className="admin-badge admin-badge--resolved">Resolved</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
