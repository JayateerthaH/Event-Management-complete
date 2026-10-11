import { useState, useEffect, useCallback, useMemo } from "react";
import { useAuth } from "../context/AuthContext";
import Navbar from "../components/Navbar";
import {
  fetchProfile,
  updateProfile,
  fetchVolunteerPreferences,
  updateVolunteerPreferences,
  fetchAllPreferences,
  fetchBadges,
  fetchAllBadges,
  updateStudentBadges,
  fetchSocialLinks,
  updateSocialLinks,
  fetchPlatforms,
  fetchAllInterests,
  fetchStudentInterests,
  updateStudentInterests,
  fetchNotificationPrefs,
  updateNotificationPrefs,
  fetchAllNotifications,
} from "../api/profileService";
import "../styles/Profile.css";

// ─── Constants ─────────────────────────────────────────────────────────────────

const DEPARTMENTS = [
  "Computer Science",
  "Information Technology",
  "Electronics",
  "Mechanical",
  "Civil",
  "Electrical",
  "Chemical",
  "Biotechnology",
  "Mathematics",
  "Physics",
];

const YEARS = ["1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"];

const getInitials = (first, last) => {
  const f = first?.[0] || "";
  const l = last?.[0] || "";
  return (f + l).toUpperCase() || "U";
};

// ─── Profile Page ──────────────────────────────────────────────────────────────

export default function Profile() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState("personal");

  // ── Data states ──
  const [profile, setProfile] = useState(null);
  const [, setVolunteerPrefs] = useState([]); // [{ preferenceId, preferenceType }]
  const [selectedPrefIds, setSelectedPrefIds] = useState(new Set());
  const [badges, setBadges] = useState([]); // [{ id, badgeName, badgeDescription }]
  const [allBadges, setAllBadges] = useState([]); // all available badge definitions (master list)
  const [selectedBadgeIds, setSelectedBadgeIds] = useState(new Set()); // student's selected badge IDs
  const [socials, setSocials] = useState([]); // [{ id:{ platformId, urlLink }, userId }]
  const [platformMap, setPlatformMap] = useState({}); // { platformId → platformName }
  const [allInterests, setAllInterests] = useState([]); // [{ interestId, interestName }]
  const [selectedInterestIds, setSelectedInterestIds] = useState(new Set());
  const [allPreferences, setAllPreferences] = useState([]); // [{ preferenceId, preferenceType }]
  const [allNotifications, setAllNotifications] = useState([]); // [{ id, notificationType }]
  const [selectedNotifIds, setSelectedNotifIds] = useState(new Set());

  // ── Loading / saving / editing states ──
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState("");
  const [editing, setEditing] = useState(false);

  // ── Form states (editable copies — init from user context as fallback) ──
  const [form, setForm] = useState({
    name: user?.name || "",
    email: user?.email || "",
    phoneNumber: "", // Long on the backend — send as number
    department: "",
    yearOfStudy: "",
    bio: "",
    privacy: false,
  });
  // Per-platform URL draft inputs: { [platformId]: url }
  const [newSocialUrls, setNewSocialUrls] = useState({});

  // ── Fetch all data on mount ──
  useEffect(() => {
    let cancelled = false;

    const loadAll = async () => {
      setLoading(true);
      try {
        const [
          prof,
          volPrefs,
          bdg,
          allBdg,
          soc,
          platf,
          allInt,
          studInt,
          allPref,
          allNotif,
          studNotif,
        ] = await Promise.allSettled([
          fetchProfile(), // GET /management/students
          fetchVolunteerPreferences(), // GET /management/preferences
          fetchBadges(), // GET /management/badges  (student's earned/selected badges)
          fetchAllBadges(), // GET /management/all-badges  (master list for selection)
          fetchSocialLinks(), // GET /management/profile
          fetchPlatforms(), // GET /admin/social (optional, may 403)
          fetchAllInterests(), // GET /management/all-interests
          fetchStudentInterests(), // GET /management/interests
          fetchAllPreferences(), // GET /management/all-preferences
          fetchAllNotifications(), // GET /management/all-notifications
          fetchNotificationPrefs(), // GET /management/notifications
        ]);

        if (cancelled) return;

        // Personal info
        if (prof.status === "fulfilled" && prof.value) {
          setProfile(prof.value);
          setForm((prev) => ({
            ...prev,
            name: prof.value.name || prev.name,
            email: prof.value.email || prev.email,
            phoneNumber:
              prof.value.phoneNumber != null
                ? prof.value.phoneNumber
                : prev.phoneNumber,
            department: prof.value.department || "",
            yearOfStudy: prof.value.yearOfStudy || "",
            // Read both keys for compatibility, but write only `bio`.
            bio: prof.value.bio ?? prof.value.Bio ?? "",
            privacy: prof.value.privacy ?? false,
          }));
        }

        // Volunteer Preferences
        if (volPrefs.status === "fulfilled" && Array.isArray(volPrefs.value)) {
          setVolunteerPrefs(volPrefs.value);
          setSelectedPrefIds(
            new Set(volPrefs.value.map((p) => p.preferenceId)),
          );
        }

        // Badges (student's earned/selected badges — used to init selection)
        if (bdg.status === "fulfilled") {
          const earned = Array.isArray(bdg.value) ? bdg.value : [];
          setBadges(earned);
          setSelectedBadgeIds(new Set(earned.map((b) => b.id)));
        }

        // All available badge definitions (master list for the selection UI)
        if (allBdg.status === "fulfilled" && Array.isArray(allBdg.value)) {
          setAllBadges(allBdg.value);
        }

        // Social profiles (user's linked profiles)
        if (soc.status === "fulfilled") {
          setSocials(Array.isArray(soc.value) ? soc.value : []);
        }

        // Platform master list (optional — builds { id → name } lookup)
        if (platf.status === "fulfilled" && Array.isArray(platf.value)) {
          const map = {};
          platf.value.forEach((p) => {
            map[p.id] = p.platform;
          });
          setPlatformMap(map);
        }

        // All available interests (master list)
        if (allInt.status === "fulfilled" && Array.isArray(allInt.value)) {
          setAllInterests(allInt.value);
        }

        // Student's current interests (selected)
        if (studInt.status === "fulfilled" && Array.isArray(studInt.value)) {
          setSelectedInterestIds(
            new Set(studInt.value.map((i) => i.interestId)),
          );
        }

        // All available preferences (master list)
        if (allPref.status === "fulfilled" && Array.isArray(allPref.value)) {
          setAllPreferences(allPref.value);
        }

        // All available notification types (master list)
        if (allNotif.status === "fulfilled" && Array.isArray(allNotif.value)) {
          setAllNotifications(allNotif.value);
        }

        // Student's current notification preferences (selected)
        if (
          studNotif.status === "fulfilled" &&
          Array.isArray(studNotif.value)
        ) {
          setSelectedNotifIds(new Set(studNotif.value.map((n) => n.id)));
        }
      } catch {
        // Silently continue
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadAll();
    return () => {
      cancelled = true;
    };
  }, []);

  // ── Flash save message helper ──
  const flash = useCallback((msg) => {
    setSaveMsg(msg);
    setTimeout(() => setSaveMsg(""), 3000);
  }, []);

  // ── Profile completeness — based on form so it works even without API ──
  const completeness = useMemo(() => {
    const src = profile || form;
    const fields = [
      "name",
      "email",
      "phoneNumber",
      "department",
      "yearOfStudy",
      "bio",
    ];
    const filled = fields.filter((f) => !!src[f]).length;
    return Math.round((filled / fields.length) * 100);
  }, [profile, form]);

  // ────────────────────────────────────────────────────────────────────────────
  // HANDLERS
  // ────────────────────────────────────────────────────────────────────────────

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const saveProfile = async () => {
    setSaving(true);
    try {
      // phoneNumber must be a Long (number) — coerce before sending
      const payload = {
        ...form,
        phoneNumber: form.phoneNumber !== "" ? Number(form.phoneNumber) : null,
      };
      const updated = await updateProfile(payload);
      setProfile(updated);
      setEditing(false);
      flash("Profile saved successfully!");
    } catch {
      flash("Failed to save profile.");
    } finally {
      setSaving(false);
    }
  };

  const cancelEditing = () => {
    // Reset form from saved profile data
    if (profile) {
      setForm({
        name: profile.name || user?.name || "",
        email: profile.email || user?.email || "",
        phoneNumber: profile.phoneNumber != null ? profile.phoneNumber : "",
        department: profile.department || "",
        yearOfStudy: profile.yearOfStudy || "",
        bio: profile.bio ?? profile.Bio ?? "",
        privacy: profile.privacy ?? false,
      });
    }
    setEditing(false);
  };

  // Toggle a volunteer preference by its preferenceId
  const togglePrefId = (prefId) => {
    setSelectedPrefIds((prev) => {
      const next = new Set(prev);
      next.has(prefId) ? next.delete(prefId) : next.add(prefId);
      return next;
    });
  };

  // POST /management/preferences — body: Set<Long>
  const saveVolunteerPreferences = async () => {
    setSaving(true);
    try {
      const updated = await updateVolunteerPreferences([...selectedPrefIds]);
      setVolunteerPrefs(updated);
      setSelectedPrefIds(new Set(updated.map((p) => p.preferenceId)));
      flash("Volunteer preferences updated!");
    } catch {
      flash("Failed to update volunteer preferences.");
    } finally {
      setSaving(false);
    }
  };

  // Toggle a notification preference by its id
  const toggleNotifId = (nId) => {
    setSelectedNotifIds((prev) => {
      const next = new Set(prev);
      next.has(nId) ? next.delete(nId) : next.add(nId);
      return next;
    });
  };

  // POST /management/notifications — body: Set<Long>
  const saveNotificationPrefs = async () => {
    setSaving(true);
    try {
      const updated = await updateNotificationPrefs([...selectedNotifIds]);
      setSelectedNotifIds(new Set(updated.map((n) => n.id)));
      flash("Notification preferences updated!");
    } catch {
      flash("Failed to update notification preferences.");
    } finally {
      setSaving(false);
    }
  };

  // Toggle an interest by its interestId
  const toggleInterestId = (intId) => {
    setSelectedInterestIds((prev) => {
      const next = new Set(prev);
      next.has(intId) ? next.delete(intId) : next.add(intId);
      return next;
    });
  };

  // POST /management/interests — body: Set<Long>
  const saveInterests = async () => {
    setSaving(true);
    try {
      const updated = await updateStudentInterests([...selectedInterestIds]);
      setSelectedInterestIds(new Set(updated.map((i) => i.interestId)));
      flash("Interests updated!");
    } catch {
      flash("Failed to update interests.");
    } finally {
      setSaving(false);
    }
  };

  // Toggle a badge by its id
  const toggleBadgeId = (badgeId) => {
    setSelectedBadgeIds((prev) => {
      const next = new Set(prev);
      next.has(badgeId) ? next.delete(badgeId) : next.add(badgeId);
      return next;
    });
  };

  // POST /management/badges — body: Set<Long>
  const saveBadges = async () => {
    setSaving(true);
    try {
      const updated = await updateStudentBadges([...selectedBadgeIds]);
      const list = Array.isArray(updated) ? updated : [];
      setBadges(list);
      setSelectedBadgeIds(new Set(list.map((b) => b.id)));
      flash("Badges updated!");
    } catch {
      flash("Failed to update badges.");
    } finally {
      setSaving(false);
    }
  };

  // Build Map<Long,String> from current socials array
  const buildSocialsMap = (socialsArray) => {
    const map = {};
    socialsArray.forEach((s) => {
      const pid = s.id?.platformId ?? s.platformId;
      const url = s.id?.urlLink ?? s.urlLink ?? "";
      if (pid !== undefined && pid !== null) map[pid] = url;
    });
    return map;
  };

  const normaliseExternalUrl = (rawUrl) => {
    const trimmed = rawUrl?.trim() ?? "";
    if (!trimmed) return "";
    return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  };

  // POST /management/profile with updated Map<Long,String>
  const handleDisconnectSocial = async (platformId) => {
    setSaving(true);
    try {
      const currentMap = buildSocialsMap(socials);
      delete currentMap[platformId];
      const updated = await updateSocialLinks(currentMap);
      setSocials(updated);
      flash("Social profile removed.");
    } catch {
      flash("Failed to remove social profile.");
    } finally {
      setSaving(false);
    }
  };

  const handleAddSocialForPlatform = async (platformId, url) => {
    if (!url?.trim()) return;
    const socialUrl = normaliseExternalUrl(url);

    setSaving(true);
    try {
      const currentMap = buildSocialsMap(socials);
      currentMap[platformId] = socialUrl;
      const updated = await updateSocialLinks(currentMap);
      setSocials(updated);
      setNewSocialUrls((prev) => ({ ...prev, [platformId]: "" }));
      flash("Social profile linked!");
    } catch {
      flash("Failed to add social profile.");
    } finally {
      setSaving(false);
    }
  };

  // ────────────────────────────────────────────────────────────────────────────
  // RENDER HELPERS
  // ────────────────────────────────────────────────────────────────────────────

  const renderPersonalInfo = () => (
    <section className="pf-section">
      <div className="pf-section__header">
        <h2 className="pf-section__title">Personal Information</h2>
        {!editing && (
          <button
            className="pf-btn pf-btn--outline pf-btn--sm"
            onClick={() => setEditing(true)}
          >
            ✏️ Edit
          </button>
        )}
      </div>

      {editing ? (
        /* ── Edit Mode ── */
        <>
          <div className="pf-field">
            <label className="pf-label">Full Name</label>
            <input
              className="pf-input"
              name="name"
              value={form.name || ""}
              onChange={handleFormChange}
            />
          </div>

          <div className="pf-field">
            <label className="pf-label">Email Address</label>
            <input
              className="pf-input"
              name="email"
              value={form.email || ""}
              onChange={handleFormChange}
              type="email"
            />
          </div>

          <div className="pf-field">
            <label className="pf-label">Phone Number</label>
            <input
              className="pf-input"
              name="phoneNumber"
              value={form.phoneNumber || ""}
              onChange={handleFormChange}
              type="number"
            />
          </div>

          <div className="pf-form-grid">
            <div className="pf-field">
              <label className="pf-label">Department</label>
              <select
                className="pf-select"
                name="department"
                value={form.department || ""}
                onChange={handleFormChange}
              >
                <option value="">Select Department</option>
                {DEPARTMENTS.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>
            <div className="pf-field">
              <label className="pf-label">Year of Study</label>
              <select
                className="pf-select"
                name="yearOfStudy"
                value={form.yearOfStudy || ""}
                onChange={handleFormChange}
              >
                <option value="">Select Year</option>
                {YEARS.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="pf-field">
            <label className="pf-label">Bio</label>
            <textarea
              className="pf-textarea"
              name="bio"
              rows={4}
              value={form.bio || ""}
              onChange={handleFormChange}
              placeholder="Tell us about yourself..."
            />
          </div>

          <div className="pf-field pf-field--inline">
            <label className="pf-label">Private Profile</label>
            <input
              type="checkbox"
              name="privacy"
              checked={!!form.privacy}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, privacy: e.target.checked }))
              }
            />
            <span className="pf-hint">
              Hide your profile from other students
            </span>
          </div>

          <div className="pf-btn-group">
            <button
              className="pf-btn pf-btn--primary"
              onClick={saveProfile}
              disabled={saving}
            >
              {saving ? "Saving…" : "Save Changes"}
            </button>
            <button
              className="pf-btn pf-btn--outline"
              onClick={cancelEditing}
              disabled={saving}
            >
              Cancel
            </button>
          </div>
        </>
      ) : (
        /* ── View Mode ── */
        <div className="pf-view">
          <div className="pf-view-grid">
            <div className="pf-view-item">
              <span className="pf-view-item__label">Full Name</span>
              <span className="pf-view-item__value">{form.name || "—"}</span>
            </div>
            <div className="pf-view-item">
              <span className="pf-view-item__label">Email Address</span>
              <span className="pf-view-item__value">{form.email || "—"}</span>
            </div>
            <div className="pf-view-item">
              <span className="pf-view-item__label">Phone Number</span>
              <span className="pf-view-item__value">
                {form.phoneNumber || "—"}
              </span>
            </div>
            <div className="pf-view-item">
              <span className="pf-view-item__label">Department</span>
              <span className="pf-view-item__value">
                {form.department || "—"}
              </span>
            </div>
            <div className="pf-view-item">
              <span className="pf-view-item__label">Year of Study</span>
              <span className="pf-view-item__value">
                {form.yearOfStudy || "—"}
              </span>
            </div>
            <div className="pf-view-item pf-view-item--full">
              <span className="pf-view-item__label">Bio</span>
              <span className="pf-view-item__value">{form.bio || "—"}</span>
            </div>
          </div>
          <div className="pf-view-item pf-view-item--privacy">
            <span className="pf-view-item__label">Profile Visibility</span>
            <span
              className={`pf-privacy-badge ${form.privacy ? "pf-privacy-badge--private" : "pf-privacy-badge--public"}`}
            >
              {form.privacy ? "🔒 Private" : "🌐 Public"}
            </span>
          </div>
        </div>
      )}
    </section>
  );

  const renderVolunteerPreferences = () => (
    <section className="pf-section">
      <h2 className="pf-section__title">Volunteer Preferences</h2>
      <p className="pf-section__subtitle">
        Select your preferred volunteer roles for events
      </p>

      {allPreferences.length === 0 ? (
        <p className="pf-muted">No volunteer preferences available yet.</p>
      ) : (
        <div className="pf-interests-grid">
          {allPreferences.map((pref) => {
            const isSelected = selectedPrefIds.has(pref.preferenceId);
            return (
              <button
                key={pref.preferenceId}
                className={`pf-interest-chip ${isSelected ? "pf-interest-chip--selected" : ""}`}
                onClick={() => togglePrefId(pref.preferenceId)}
                type="button"
              >
                {pref.preferenceType}
                {isSelected && (
                  <span className="pf-interest-chip__check">✓</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <button
        className="pf-btn pf-btn--primary pf-btn--sm"
        onClick={saveVolunteerPreferences}
        disabled={saving || allPreferences.length === 0}
        style={{ marginTop: "1rem" }}
      >
        {saving ? "Saving…" : "Save Preferences"}
      </button>
    </section>
  );

  const renderAchievements = () => (
    <section className="pf-section">
      <h2 className="pf-section__title">Achievement Badges</h2>
      <p className="pf-section__subtitle">
        Select the badges that represent your achievements
      </p>

      {allBadges.length === 0 ? (
        <p className="pf-muted">No badges available yet.</p>
      ) : (
        <div className="pf-interests-grid">
          {allBadges.map((b) => {
            const isSelected = selectedBadgeIds.has(b.id);
            return (
              <button
                key={b.id}
                className={`pf-interest-chip ${isSelected ? "pf-interest-chip--selected" : ""}`}
                onClick={() => toggleBadgeId(b.id)}
                type="button"
                title={b.badgeDescription}
              >
                🏅 {b.badgeName}
                {isSelected && (
                  <span className="pf-interest-chip__check">✓</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <button
        className="pf-btn pf-btn--primary pf-btn--sm"
        onClick={saveBadges}
        disabled={saving || allBadges.length === 0}
        style={{ marginTop: "1rem" }}
      >
        {saving ? "Saving…" : "Save Badges"}
      </button>
    </section>
  );

  // Map platform name → icon symbol (SVG-free, uses unicode/emoji)
  const platformIcon = (name) => {
    const n = (name || "").toLowerCase();
    if (n.includes("github")) return "🐙";
    if (n.includes("linkedin")) return "💼";
    if (n.includes("twitter") || n.includes("x")) return "🐦";
    if (n.includes("instagram")) return "📷";
    if (n.includes("facebook")) return "📘";
    if (n.includes("youtube")) return "🎬";
    if (n.includes("discord")) return "💬";
    if (n.includes("reddit")) return "🔴";
    if (n.includes("portfolio") || n.includes("website")) return "🌐";
    if (n.includes("medium") || n.includes("blog")) return "📝";
    if (n.includes("stack")) return "📚";
    if (n.includes("behance") || n.includes("dribbble")) return "🎨";
    return "🔗";
  };

  const renderSocialProfiles = () => {
    // Build connected map: platformId → url
    const connectedMap = {};
    socials.forEach((s) => {
      const pid = s.id?.platformId ?? s.platformId;
      const url = s.id?.urlLink ?? s.urlLink ?? "";
      if (pid !== undefined && pid !== null) connectedMap[pid] = url;
    });

    const platformEntries = Object.entries(platformMap);

    return (
      <section className="pf-section">
        <h2 className="pf-section__title">Linked Social Profiles</h2>

        {platformEntries.length === 0 ? (
          <p className="pf-muted">No platforms configured yet.</p>
        ) : (
          <div className="pf-socials-list">
            {platformEntries.map(([id, name]) => {
              const numId = Number(id);
              const isConnected = connectedMap[numId] !== undefined;
              const url = connectedMap[numId] || "";
              const icon = platformIcon(name);
              const href = normaliseExternalUrl(url);

              return (
                <div
                  key={id}
                  className={`pf-social-row ${isConnected ? "pf-social-row--connected" : ""}`}
                >
                  <div className="pf-social-row__left">
                    <span className="pf-social-row__icon">{icon}</span>
                    <div className="pf-social-row__details">
                      <span className="pf-social-row__platform">{name}</span>
                      {isConnected ? (
                        <a
                          className="pf-social-row__url"
                          href={href}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{
                            cursor: "pointer",
                            color: "#2563eb",
                            textDecoration: "underline",
                          }}
                        >
                          {name} Profile
                        </a>
                      ) : (
                        <span
                          className="pf-social-row__url"
                          style={{ color: "#64748b" }}
                        >
                          Not connected
                        </span>
                      )}
                    </div>
                  </div>

                  {isConnected ? (
                    <button
                      className="pf-btn pf-btn--sm pf-btn--danger-outline"
                      disabled={saving}
                      onClick={() => handleDisconnectSocial(numId)}
                    >
                      Disconnect
                    </button>
                  ) : (
                    <div
                      style={{
                        display: "flex",
                        gap: "0.5rem",
                        alignItems: "center",
                      }}
                    >
                      <input
                        className="pf-input"
                        placeholder={`${name} profile URL`}
                        value={newSocialUrls[id] || ""}
                        onChange={(e) =>
                          setNewSocialUrls((prev) => ({
                            ...prev,
                            [id]: e.target.value,
                          }))
                        }
                        style={{ width: "220px" }}
                      />
                      <button
                        className="pf-btn pf-btn--primary pf-btn--sm"
                        disabled={saving || !newSocialUrls[id]?.trim()}
                        onClick={() =>
                          handleAddSocialForPlatform(numId, newSocialUrls[id])
                        }
                      >
                        {saving ? "Saving…" : "Add"}
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>
    );
  };

  const renderInterests = () => (
    <section className="pf-section">
      <h2 className="pf-section__title">Your Interests</h2>
      <p className="pf-section__subtitle">
        Select your interests to get personalized event recommendations
      </p>

      {allInterests.length === 0 ? (
        <p className="pf-muted">No interests available yet.</p>
      ) : (
        <div className="pf-interests-grid">
          {allInterests.map((interest) => {
            const isSelected = selectedInterestIds.has(interest.interestId);
            return (
              <button
                key={interest.interestId}
                className={`pf-interest-chip ${isSelected ? "pf-interest-chip--selected" : ""}`}
                onClick={() => toggleInterestId(interest.interestId)}
                type="button"
              >
                {interest.interestName}
                {isSelected && (
                  <span className="pf-interest-chip__check">✓</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <button
        className="pf-btn pf-btn--primary pf-btn--sm"
        onClick={saveInterests}
        disabled={saving || allInterests.length === 0}
        style={{ marginTop: "1rem" }}
      >
        {saving ? "Saving…" : "Save Interests"}
      </button>
    </section>
  );

  const renderNotifications = () => (
    <section className="pf-section">
      <h2 className="pf-section__title">Notification Preferences</h2>
      <p className="pf-section__subtitle">
        Choose how you'd like to be notified about events
      </p>

      {allNotifications.length === 0 ? (
        <p className="pf-muted">No notification types available yet.</p>
      ) : (
        <div className="pf-interests-grid">
          {allNotifications.map((notif) => {
            const isSelected = selectedNotifIds.has(notif.id);
            return (
              <button
                key={notif.id}
                className={`pf-interest-chip ${isSelected ? "pf-interest-chip--selected" : ""}`}
                onClick={() => toggleNotifId(notif.id)}
                type="button"
              >
                {notif.notificationType}
                {isSelected && (
                  <span className="pf-interest-chip__check">✓</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <button
        className="pf-btn pf-btn--primary pf-btn--sm"
        onClick={saveNotificationPrefs}
        disabled={saving || allNotifications.length === 0}
        style={{ marginTop: "1rem" }}
      >
        {saving ? "Saving…" : "Save Notification Preferences"}
      </button>
    </section>
  );

  const renderTab = () => {
    switch (activeTab) {
      case "personal":
        return renderPersonalInfo();
      case "interests":
        return renderInterests();
      case "preferences":
        return renderVolunteerPreferences();
      case "notifications":
        return renderNotifications();
      case "social":
        return renderSocialProfiles();
      case "achievements":
        return renderAchievements();
      default:
        return null;
    }
  };

  // ── Quick Stats (sidebar) ──
  const statsItems = useMemo(() => {
    const eventsCount = profile?.eventsAttended ?? 0;
    const certsCount = profile?.certificates ?? 0;
    const achCount = badges.length;
    return [
      { icon: "📅", label: "Events", value: eventsCount },
      { icon: "🏅", label: "Certificates", value: certsCount },
      { icon: "🏆", label: "Achievements", value: achCount },
    ];
  }, [badges, profile]);

  // ────────────────────────────────────────────────────────────────────────────
  // MAIN RENDER
  // ────────────────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="pf-page">
        <Navbar />
        <div className="pf-loading">
          <div className="pf-loading__spinner" />
          <p>Loading profile…</p>
        </div>
      </div>
    );
  }

  // No blocking error page — always render the full UI,
  // even if all APIs failed (form fields stay empty / defaults).

  const displayName = form.name || user?.name || user?.email || "User";
  const departmentLabel = form.department
    ? `${form.department} Engineering`
    : "";

  return (
    <div className="pf-page">
      <Navbar />

      {/* ── Toast ── */}
      {saveMsg && <div className="pf-toast">{saveMsg}</div>}

      {/* ── Hero Banner ── */}
      <header className="pf-hero">
        <div className="pf-hero__avatar">
          <span className="pf-hero__initials">
            {getInitials(
              form.name?.split(" ")[0],
              form.name?.split(" ").slice(1).join(" "),
            )}
          </span>
          <button className="pf-hero__camera" title="Change avatar">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" />
              <circle cx="12" cy="13" r="4" />
            </svg>
          </button>
        </div>
        <h1 className="pf-hero__name">{displayName}</h1>
        {departmentLabel && <p className="pf-hero__dept">{departmentLabel}</p>}
      </header>

      {/* ── Tabs ── */}
      <nav className="pf-tabs">
        {[
          { key: "personal", label: "Personal Info" },
          { key: "interests", label: "Interests" },
          { key: "preferences", label: "Preferences" },
          { key: "notifications", label: "Notifications" },
          { key: "social", label: "Social Profiles" },
          { key: "achievements", label: "Badges" },
        ].map((t) => (
          <button
            key={t.key}
            className={`pf-tabs__btn ${activeTab === t.key ? "pf-tabs__btn--active" : ""}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </nav>

      {/* ── Layout: main + sidebar ── */}
      <div className="pf-layout">
        <main className="pf-main">{renderTab()}</main>

        <aside className="pf-sidebar">
          {/* Quick Stats */}
          <div className="pf-card">
            <h3 className="pf-card__title">Quick Stats</h3>
            {statsItems.map((s) => (
              <div key={s.label} className="pf-stat-row">
                <span className="pf-stat-row__icon">{s.icon}</span>
                <span className="pf-stat-row__value">
                  {s.value} {s.label}
                </span>
              </div>
            ))}
          </div>

          {/* Profile Completeness */}
          <div className="pf-card pf-card--center">
            <h3 className="pf-card__title">Profile Completeness</h3>
            <div
              className="pf-progress-ring"
              style={{ "--pf-progress": completeness }}
            >
              <svg viewBox="0 0 120 120">
                <circle
                  className="pf-progress-ring__bg"
                  cx="60"
                  cy="60"
                  r="50"
                />
                <circle
                  className="pf-progress-ring__fg"
                  cx="60"
                  cy="60"
                  r="50"
                />
              </svg>
              <span className="pf-progress-ring__label">{completeness}%</span>
            </div>
            <p className="pf-muted">
              Complete your profile to get better recommendations!
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
