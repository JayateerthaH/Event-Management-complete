import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import PersonCard from "../components/social/PersonCard";
import { apiErrorMessage } from "../api/apiClient";
import {
  getFriends, removeFriend, getRequests, ignoreRequest, cancelRequest,
  searchPeople, getAllInterests, getSocialSettings, updateSocialSettings, getBlocked, unblockUser,
  getFollowing, unfollowOrganizer,
} from "../api/socialService";
import "../styles/Social.css";

const TABS = [
  { key: "friends", label: "Friends" },
  { key: "requests", label: "Requests" },
  { key: "find", label: "Find people" },
  { key: "following", label: "Following" },
  { key: "settings", label: "Settings" },
];

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString(undefined, { day: "numeric", month: "short" }) : "";

export default function Friends() {
  const [params, setParams] = useSearchParams();
  const tab = TABS.some((t) => t.key === params.get("tab")) ? params.get("tab") : "friends";
  const [settings, setSettings] = useState(null);
  const [incomingCount, setIncomingCount] = useState(0);

  const loadSettings = useCallback(() => getSocialSettings().then(setSettings).catch(() => {}), []);
  useEffect(() => { loadSettings(); }, [loadSettings]);
  useEffect(() => {
    getRequests().then((r) => setIncomingCount(r.incoming.length)).catch(() => {});
  }, [tab]);

  return (
    <div className="social-page">
      <Navbar />
      <main className="social-wrap">
        <header className="social-head">
          <h1>Friends</h1>
          <p>Meet students who go to the same events and share your interests. You choose what others see.</p>
        </header>

        <nav className="social-tabs" role="tablist" aria-label="Friends sections">
          {TABS.map((t) => (
            <button key={t.key} type="button" role="tab" aria-selected={tab === t.key}
              className={tab === t.key ? "is-active" : ""} onClick={() => setParams({ tab: t.key })}>
              {t.label}
              {t.key === "requests" && incomingCount > 0 && <span className="social-tabs__count">{incomingCount}</span>}
            </button>
          ))}
        </nav>

        {tab === "friends" && <FriendsTab />}
        {tab === "requests" && <RequestsTab onCount={setIncomingCount} />}
        {tab === "find" && <FindTab settings={settings} onSettings={setSettings} />}
        {tab === "following" && <FollowingTab />}
        {tab === "settings" && <SettingsTab settings={settings} onSaved={setSettings} />}
      </main>
    </div>
  );
}

function FriendsTab() {
  const [friends, setFriends] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    getFriends().then(setFriends).catch((e) => setError(apiErrorMessage(e)));
  }, []);
  useEffect(load, [load]);

  if (error) return <p className="social-error" role="alert">{error}</p>;
  if (!friends) return <p className="social-muted">Loading…</p>;
  if (friends.length === 0) {
    return (
      <div className="social-empty">
        <h2>No friends yet</h2>
        <p>Register for an event and choose <b>I'm open to meeting people</b>, or find students with your interests.</p>
        <Link className="pc-btn pc-btn--primary" to="/friends?tab=find">Find people</Link>
      </div>
    );
  }
  return (
    <div className="pc-grid">
      {friends.map(({ person, upcomingEvents }) => (
        <div key={person.userId} className="friend">
          <PersonCard
            person={person}
            onChange={load}
            extraActions={
              <button type="button" className="pc-btn pc-btn--ghost"
                onClick={() => removeFriend(person.userId).then(load).catch(() => {})}>
                Remove friend
              </button>
            }
          />
          {upcomingEvents.length > 0 && (
            <div className="friend__events">
              <span className="eb__label">Going to</span>
              <ul>
                {upcomingEvents.map((e) => (
                  <li key={e.id}>
                    <Link to={`/events/${e.id}`}>{e.name}</Link>
                    <span>{formatDate(e.eventDate)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function RequestsTab({ onCount }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    getRequests()
      .then((r) => { setData(r); onCount(r.incoming.length); })
      .catch((e) => setError(apiErrorMessage(e)));
  }, [onCount]);
  useEffect(load, [load]);

  const act = (fn) => fn().then(load).catch((e) => setError(apiErrorMessage(e)));

  if (!data) return error ? <p className="social-error" role="alert">{error}</p> : <p className="social-muted">Loading…</p>;
  return (
    <div className="social-stack">
      {error && <p className="social-error" role="alert">{error}</p>}
      <section>
        <h2 className="social-h2">Received</h2>
        {data.incoming.length === 0 ? <p className="social-muted">No requests right now.</p> : (
          <div className="pc-grid">
            {data.incoming.map((r) => (
              <div key={r.id} className="friend">
                {r.eventName && <p className="req-where">Met at <Link to={`/events/${r.eventId}`}>{r.eventName}</Link></p>}
                <PersonCard
                  person={{ ...r.person, relation: "INCOMING", requestId: r.id }}
                  onChange={load}
                  extraActions={
                    <button type="button" className="pc-btn pc-btn--ghost" onClick={() => act(() => ignoreRequest(r.id))}>Ignore</button>
                  }
                />
              </div>
            ))}
          </div>
        )}
        <p className="social-note">If you ignore a request, the sender isn't told.</p>
      </section>
      <section>
        <h2 className="social-h2">Sent</h2>
        {data.outgoing.length === 0 ? <p className="social-muted">You haven't sent any requests that are waiting.</p> : (
          <ul className="sent-list">
            {data.outgoing.map((r) => (
              <li key={r.id}>
                <span><b>{r.person.name}</b>{r.eventName && <> · {r.eventName}</>}</span>
                <button type="button" className="pc-btn pc-btn--ghost" onClick={() => act(() => cancelRequest(r.id))}>Cancel</button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function FindTab({ settings, onSettings }) {
  const [interests, setInterests] = useState([]);
  const [selected, setSelected] = useState([]);
  const [q, setQ] = useState("");
  const [results, setResults] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => { getAllInterests().then(setInterests).catch(() => {}); }, []);
  // Start from the student's own interests so the first results are relevant.
  useEffect(() => {
    if (settings?.myInterests?.length && selected.length === 0) setSelected(settings.myInterests);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settings]);

  const run = useCallback(() => {
    setBusy(true);
    setError("");
    searchPeople(q.trim(), selected)
      .then(setResults)
      .catch((e) => { setResults(null); setError(apiErrorMessage(e)); })
      .finally(() => setBusy(false));
  }, [q, selected]);

  const discoverable = settings?.discoverable;
  useEffect(() => {
    if (discoverable) run();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [discoverable, selected]);

  if (!settings) return <p className="social-muted">Loading…</p>;
  if (!discoverable) {
    return <DiscoverPrompt settings={settings} onSettings={onSettings} />;
  }

  const toggle = (i) => setSelected((s) => (s.includes(i) ? s.filter((x) => x !== i) : [...s, i]));

  return (
    <div className="social-stack">
      <form className="find-bar" onSubmit={(e) => { e.preventDefault(); run(); }}>
        <label htmlFor="find-q" className="sr-only">Search by name, department or interest</label>
        <input id="find-q" type="search" value={q} onChange={(e) => setQ(e.target.value)}
          placeholder="Search by name, department or interest" />
        <button type="submit" className="pc-btn pc-btn--primary" disabled={busy}>Search</button>
      </form>
      {interests.length > 0 && (
        <div className="find-interests" role="group" aria-label="Filter by interests">
          {interests.map((i) => (
            <button key={i} type="button" aria-pressed={selected.includes(i)}
              className={selected.includes(i) ? "is-on" : ""} onClick={() => toggle(i)}>
              {i}
            </button>
          ))}
          {selected.length > 0 && (
            <button type="button" className="find-clear" onClick={() => setSelected([])}>Clear</button>
          )}
        </div>
      )}
      {error && <p className="social-error" role="alert">{error}</p>}
      {results && (results.length === 0 ? (
        <p className="social-muted">
          No one matches yet. Only students who turned on <b>Let others find me</b> appear here.
        </p>
      ) : (
        <>
          <p className="social-muted">{results.length} student{results.length === 1 ? "" : "s"}, most shared interests first</p>
          <div className="pc-grid">
            {results.map((p) => <PersonCard key={p.userId} person={p} onChange={run} />)}
          </div>
        </>
      ))}
    </div>
  );
}

function DiscoverPrompt({ settings, onSettings }) {
  const [name, setName] = useState(settings.displayName || "");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await updateSocialSettings({ displayName: name });
      onSettings(await updateSocialSettings({ discoverable: true }));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };
  return (
    <form className="social-empty" onSubmit={submit}>
      <h2>Find students with your interests</h2>
      <p>
        To search, others need to be able to find you too. They'll see your name, department, year and
        interests. Never your email or phone. You can turn this off at any time.
      </p>
      <label htmlFor="discover-name" className="social-label">Name other students will see</label>
      <input id="discover-name" className="social-input" value={name} maxLength={60} required
        onChange={(e) => setName(e.target.value)} placeholder="e.g. Priya S" />
      {settings.myInterests?.length === 0 && (
        <p className="social-note">Tip: add interests on your <Link to="/profile">profile</Link> to get better matches.</p>
      )}
      {error && <p className="social-error" role="alert">{error}</p>}
      <button type="submit" className="pc-btn pc-btn--primary" disabled={busy || !name.trim()}>Let others find me</button>
    </form>
  );
}

function FollowingTab() {
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    getFollowing().then(setList).catch((e) => setError(apiErrorMessage(e)));
  }, []);
  useEffect(load, [load]);

  if (error) return <p className="social-error" role="alert">{error}</p>;
  if (!list) return <p className="social-muted">Loading…</p>;
  if (list.length === 0) {
    return (
      <div className="social-empty">
        <h2>Not following anyone yet</h2>
        <p>Open any event and choose <b>Follow</b> next to its organizer. We'll tell you when they publish a new event.</p>
        <Link className="pc-btn pc-btn--primary" to="/events">Browse events</Link>
      </div>
    );
  }
  return (
    <ul className="following-list">
      {list.map((o) => (
        <li key={o.organizerId}>
          <div className="following-list__head">
            <div>
              {o.name && <><b>{o.name}</b><br /></>}
              <span>{o.followers} follower{o.followers === 1 ? "" : "s"} · {o.upcomingCount} upcoming</span>
            </div>
            <button type="button" className="pc-btn pc-btn--ghost"
              onClick={() => unfollowOrganizer(o.organizerId).then(load).catch((e) => setError(apiErrorMessage(e)))}>
              Unfollow
            </button>
          </div>
          {o.upcoming.length > 0 && (
            <div className="friend__events">
              <span className="eb__label">Coming up</span>
              <ul>
                {o.upcoming.map((e) => (
                  <li key={e.id}><Link to={`/events/${e.id}`}>{e.name}</Link><span>{formatDate(e.eventDate)}</span></li>
                ))}
              </ul>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}

function SettingsTab({ settings, onSaved }) {
  const [form, setForm] = useState(null);
  const [blocked, setBlocked] = useState([]);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (settings) setForm({
      displayName: settings.displayName || "", contact: settings.contact || "",
      discoverable: settings.discoverable, shareAttendance: settings.shareAttendance,
    });
  }, [settings]);
  const loadBlocked = useCallback(() => getBlocked().then(setBlocked).catch(() => {}), []);
  useEffect(() => { loadBlocked(); }, [loadBlocked]);

  if (!form) return <p className="social-muted">Loading…</p>;
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const save = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    setStatus("");
    try {
      await updateSocialSettings({ displayName: form.displayName, contact: form.contact });
      onSaved(await updateSocialSettings({ discoverable: form.discoverable, shareAttendance: form.shareAttendance }));
      setStatus("Saved");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="social-stack">
      <form className="social-form" onSubmit={save}>
        <div>
          <label htmlFor="set-name" className="social-label">Name other students will see</label>
          <input id="set-name" className="social-input" value={form.displayName} maxLength={60} required
            onChange={(e) => set("displayName", e.target.value)} />
        </div>
        <div>
          <label htmlFor="set-contact" className="social-label">Contact for friends (optional)</label>
          <input id="set-contact" className="social-input" value={form.contact} maxLength={120}
            onChange={(e) => set("contact", e.target.value)} placeholder="e.g. Instagram @priya.s or a LinkedIn link" />
          <p className="social-note">Only shown to people after you've both accepted. Avoid sharing your phone number.</p>
        </div>
        <label className="social-check" htmlFor="set-discoverable">
          <input id="set-discoverable" type="checkbox" checked={form.discoverable}
            onChange={(e) => set("discoverable", e.target.checked)} />
          <span><b>Let others find me</b> by name and interests (needed to search)</span>
        </label>
        <label className="social-check" htmlFor="set-share">
          <input id="set-share" type="checkbox" checked={form.shareAttendance}
            onChange={(e) => set("shareAttendance", e.target.checked)} />
          <span><b>Tell my friends</b> when I register for an event</span>
        </label>
        {error && <p className="social-error" role="alert">{error}</p>}
        <div className="pc__actions">
          <button type="submit" className="pc-btn pc-btn--primary" disabled={busy}>Save settings</button>
          {status && <span className="social-saved" role="status">{status}</span>}
        </div>
      </form>

      <section>
        <h2 className="social-h2">Blocked</h2>
        {blocked.length === 0 ? <p className="social-muted">You haven't blocked anyone.</p> : (
          <ul className="sent-list">
            {blocked.map((b) => (
              <li key={b.userId}>
                <span>{b.name}</span>
                <button type="button" className="pc-btn pc-btn--ghost"
                  onClick={() => unblockUser(b.userId).then(loadBlocked).catch(() => {})}>Unblock</button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
