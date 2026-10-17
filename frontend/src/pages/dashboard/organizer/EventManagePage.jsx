import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import Navbar from "../../../components/Navbar";
import {
  fetchAboutEvent,
  fetchEventById,
  fetchEventDetails,
  fetchSchedule,
  fetchFaqs,
  fetchHighlights,
  fetchSpeakers,
  fetchWhatToBring,
  updateAboutEvent,
  addEventDetails,
  addSchedule,
  addFaq,
  addHighlight,
  addSpeaker,
  addWhatToBring,
} from "../../../api/eventDetailsService";
import {
  uploadEventImage,
  getEventImages,
  deleteEventImage,
  getImageUrl,
} from "../../../api/imageService";
import "../../../styles/dashboard/EventManage.css";

/* ═══════════════════════════════════════════════════════════════════════════
   TABS
   ═══════════════════════════════════════════════════════════════════════════ */
const TABS = [
  { key: "overview", label: "Overview" },
  { key: "schedule", label: "Schedule" },
  { key: "speakers", label: "Speakers" },
  { key: "gallery", label: "Gallery" },
  { key: "faqs", label: "FAQs" },
];

/* ═══════════════════════════════════════════════════════════════════════════
   HELPERS
   ═══════════════════════════════════════════════════════════════════════════ */
const pick = (obj, ...keys) => {
  for (const k of keys) {
    const v = obj?.[k];
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return null;
};

const formatDate = (d) => {
  if (!d || d === "--") return "--";
  try {
    return new Date(d).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return d;
  }
};

/* ── Generic modal shell ─────────────────────────────────────────────────── */
const Modal = ({ title, saving, onClose, onSubmit, children }) => (
  <div className="em-modal-overlay" onClick={() => !saving && onClose()}>
    <div className="em-modal" onClick={(e) => e.stopPropagation()}>
      <div className="em-modal__header">
        <h3 className="em-modal__title">{title}</h3>
        <button className="em-modal__close" onClick={onClose} disabled={saving}>
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
      <form onSubmit={onSubmit}>{children}</form>
    </div>
  </div>
);

/* ═══════════════════════════════════════════════════════════════════════════
   COMPONENT
   ═══════════════════════════════════════════════════════════════════════════ */
export default function EventManagePage() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  // eslint-disable-next-line no-unused-vars
  const { user } = useAuth();

  const navEvent = location.state?.event ?? null;
  const [event, setEvent] = useState(navEvent);
  const eventFetched = useRef(!!navEvent);
  const [activeTab, setActiveTab] = useState("overview");

  /* ── Data state ─────────────────────────────────────────────────────────── */
  const [about, setAbout] = useState(null);
  const [details, setDetails] = useState(null);
  const [schedule, setSchedule] = useState(null); // { id, time[], schedule[], smallDescription[] }
  const [faqs, setFaqs] = useState([]); // [{ id, questions, answers }]
  const [highlights, setHighlights] = useState([]); // [{ id, name, smallDescription }]
  const [speakers, setSpeakers] = useState([]); // [{ id, name, profession, shortExperience }]
  const [whatToBring, setWhatToBring] = useState([]); // [{ id, item }]
  const [loading, setLoading] = useState(true);

  /* ── Gallery state ──────────────────────────────────────────────────────── */
  const [galleryImages, setGalleryImages] = useState([]);
  const [galleryLoading, setGalleryLoading] = useState(false);
  const [galleryUploading, setGalleryUploading] = useState(false);
  const [galleryError, setGalleryError] = useState(null);
  const [lightboxImg, setLightboxImg] = useState(null); // URL string when lightbox open
  const galleryInputRef = useRef(null);

  /* ── Modal open flags ───────────────────────────────────────────────────── */
  const [modal, setModal] = useState(null); // 'about'|'details'|'schedule'|'faq'|'highlight'|'speaker'|'whatToBring'

  /* ── Modal form data ────────────────────────────────────────────────────── */
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);

  // About
  const [aboutInput, setAboutInput] = useState("");

  // Details
  const EMPTY_DETAILS = {
    organizedBy: "",
    durationInHours: "",
    language: "",
    level: "",
    contactEmail: "",
    contactPhone: "",
  };
  const [detailsInput, setDetailsInput] = useState(EMPTY_DETAILS);

  // Schedule (add one session)
  const [scheduleInput, setScheduleInput] = useState({
    time: "",
    schedule: "",
    smallDescription: "",
  });

  // FAQ
  const [faqInput, setFaqInput] = useState({ questions: "", answers: "" });

  // Highlight
  const [highlightInput, setHighlightInput] = useState({
    highlights: "",
    name: "",
    smallDescription: "",
  });

  // Speaker
  const [speakerInput, setSpeakerInput] = useState({
    name: "",
    profession: "",
    shortExperience: "",
  });

  // What to Bring
  const [wtbInput, setWtbInput] = useState({ item: "" });

  /* ── Fetch all data on mount ────────────────────────────────────────────── */
  const loadData = useCallback(async () => {
    if (!eventId) return;
    setLoading(true);
    try {
      const needsFetch = !eventFetched.current;
      const [
        eventR,
        aboutR,
        detailsR,
        scheduleR,
        faqsR,
        highlightsR,
        speakersR,
        wtbR,
      ] = await Promise.allSettled([
        needsFetch ? fetchEventById(eventId) : Promise.resolve(null),
        fetchAboutEvent(eventId),
        fetchEventDetails(eventId),
        fetchSchedule(eventId),
        fetchFaqs(eventId),
        fetchHighlights(eventId),
        fetchSpeakers(eventId),
        fetchWhatToBring(eventId),
      ]);
      if (needsFetch && eventR.status === "fulfilled" && eventR.value) {
        setEvent(eventR.value);
        eventFetched.current = true;
      }
      if (aboutR.status === "fulfilled") setAbout(aboutR.value);
      if (detailsR.status === "fulfilled") setDetails(detailsR.value);
      if (scheduleR.status === "fulfilled") setSchedule(scheduleR.value);
      if (faqsR.status === "fulfilled")
        setFaqs(Array.isArray(faqsR.value) ? faqsR.value : []);
      if (highlightsR.status === "fulfilled")
        setHighlights(
          Array.isArray(highlightsR.value) ? highlightsR.value : [],
        );
      if (speakersR.status === "fulfilled")
        setSpeakers(Array.isArray(speakersR.value) ? speakersR.value : []);
      if (wtbR.status === "fulfilled")
        setWhatToBring(Array.isArray(wtbR.value) ? wtbR.value : []);
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  /* ── Load gallery images (runs when gallery tab becomes active) ───────────*/
  const loadGallery = useCallback(async () => {
    if (!eventId) return;
    setGalleryLoading(true);
    setGalleryError(null);
    try {
      const imgs = await getEventImages(eventId);
      setGalleryImages(imgs);
    } catch (err) {
      console.error("[Gallery] load error:", err);
      setGalleryError(`Couldn't load gallery images. ${err?.message || ""}`.trim());
    } finally {
      setGalleryLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    if (activeTab === "gallery") loadGallery();
  }, [activeTab, loadGallery]);

  /* ── Gallery upload handler ──────────────────────────────────────────────*/
  const handleGalleryUpload = async (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    // reset input so the same file can be re-selected
    if (galleryInputRef.current) galleryInputRef.current.value = "";
    setGalleryUploading(true);
    setGalleryError(null);
    try {
      // Upload all selected files sequentially to avoid overwhelming the server
      const uploaded = [];
      for (const file of files) {
        const img = await uploadEventImage(eventId, file);
        uploaded.push(img);
      }
      // Prepend new images to the top of the list
      setGalleryImages((prev) => [...uploaded.reverse(), ...prev]);
    } catch (err) {
      console.error("[Gallery] upload error:", err);
      setGalleryError(err?.message || "Upload failed. Please try again.");
    } finally {
      setGalleryUploading(false);
    }
  };

  /* ── Gallery delete handler ──────────────────────────────────────────────*/
  const handleDeleteImage = async (imageId) => {
    if (!window.confirm("Delete this image? This cannot be undone.")) return;
    setGalleryError(null);
    try {
      await deleteEventImage(imageId);
      setGalleryImages((prev) => prev.filter((img) => img.id !== imageId));
    } catch (err) {
      console.error("[Gallery] delete error:", err);
      setGalleryError(`Couldn't delete the image. ${err?.message || ""}`.trim());
    }
  };

  /* ── Derived display data ───────────────────────────────────────────────── */
  const eventTitle =
    pick(event, "name", "title", "eventName") ?? "Untitled Event";
  const eventDate = pick(event, "eventDate", "date") ?? "--";
  const eventVenue = pick(event, "venue", "location") ?? "--";
  const eventImage = pick(event, "imageUrl", "image");

  const aboutText = about?.about || (typeof about === "string" ? about : null);

  // Build details list from the /details endpoint data
  const detailsList = [
    { label: "Organized By", value: details?.organizedBy },
    {
      label: "Departments",
      value: Array.isArray(details?.departments)
        ? details.departments.join(", ")
        : details?.departments,
    },
    {
      label: "Duration",
      value: details?.durationInHours
        ? `${details.durationInHours} Hours`
        : null,
    },
    { label: "Language", value: details?.language },
    { label: "Level", value: details?.level },
    { label: "Contact Email", value: details?.contactEmail },
    { label: "Contact Phone", value: details?.contactPhone },
    // Also show static event fields (from EventsDto)
    { label: "Category", value: pick(event, "category") },
    { label: "Venue", value: eventVenue !== "--" ? eventVenue : null },
    { label: "Max People", value: pick(event, "maxPeople") },
    { label: "Cost", value: pick(event, "costOfRegistration") },
  ].filter((d) => d.value);

  // Flatten schedule parallel-arrays into renderable list
  const scheduleSessions = (() => {
    if (!schedule) return [];
    const times = schedule.time || [];
    const titles = schedule.schedule || [];
    const descs = schedule.smallDescription || [];
    const len = Math.max(times.length, titles.length, descs.length);
    const arr = [];
    for (let i = 0; i < len; i++) {
      arr.push({
        time: times[i] || "--",
        title: titles[i] || `Session ${i + 1}`,
        desc: descs[i] || "",
      });
    }
    return arr;
  })();

  /* ═══════════════════════════════════════════════════════════════════════════
     MODAL OPEN HELPERS
     ═══════════════════════════════════════════════════════════════════════════ */
  const openModal = (name) => {
    setSaveError(null);
    switch (name) {
      case "about":
        setAboutInput(aboutText || "");
        break;
      case "details":
        setDetailsInput({
          organizedBy: details?.organizedBy || "",
          durationInHours: details?.durationInHours ?? "",
          language: details?.language || "",
          level: details?.level || "",
          contactEmail: details?.contactEmail || "",
          contactPhone: details?.contactPhone || "",
        });
        break;
      case "schedule":
        setScheduleInput({ time: "", schedule: "", smallDescription: "" });
        break;
      case "faq":
        setFaqInput({ questions: "", answers: "" });
        break;
      case "highlight":
        setHighlightInput({ highlights: "", name: "", smallDescription: "" });
        break;
      case "speaker":
        setSpeakerInput({ name: "", profession: "", shortExperience: "" });
        break;
      case "whatToBring":
        setWtbInput({ item: "" });
        break;
      default:
        break;
    }
    setModal(name);
  };
  const closeModal = () => {
    if (!saving) setModal(null);
  };

  /* ═══════════════════════════════════════════════════════════════════════════
     SAVE HANDLERS
     ═══════════════════════════════════════════════════════════════════════════ */
  const wrapSave = async (fn) => {
    setSaving(true);
    setSaveError(null);
    try {
      await fn();
      setModal(null);
    } catch (err) {
      console.error(
        "[EventManage] Save error:",
        err?.response?.status,
        err?.response?.data,
        err,
      );
      const data = err?.response?.data;
      // Ensure saveError is ALWAYS a string - objects crash React rendering
      const msg =
        typeof data === "string"
          ? data
          : data?.message || data?.error || err?.message || "Failed to save.";
      setSaveError(typeof msg === "string" ? msg : JSON.stringify(msg));
    } finally {
      setSaving(false);
    }
  };

  // About
  const handleAboutSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const saved = await updateAboutEvent(eventId, { about: aboutInput });
      setAbout(saved?.about ? saved : { about: aboutInput });
    });
  };

  // Details
  const handleDetailsSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const payload = {
        organizedBy: detailsInput.organizedBy.trim() || undefined,
        durationInHours: detailsInput.durationInHours
          ? Number(detailsInput.durationInHours)
          : undefined,
        language: detailsInput.language.trim() || undefined,
        level: detailsInput.level.trim() || undefined,
        contactEmail: detailsInput.contactEmail.trim() || undefined,
        contactPhone: detailsInput.contactPhone.trim() || undefined,
      };
      const saved = await addEventDetails(eventId, payload);
      setDetails(saved ?? payload);
    });
  };

  // Schedule - sends full updated EventSchedule (existing + new session)
  const handleScheduleSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const existingTimes = schedule?.time || [];
      const existingTitles = schedule?.schedule || [];
      const existingDescs = schedule?.smallDescription || [];
      const payload = {
        time: [...existingTimes, scheduleInput.time.trim()],
        schedule: [...existingTitles, scheduleInput.schedule.trim()],
        smallDescription: [
          ...existingDescs,
          scheduleInput.smallDescription.trim(),
        ],
      };
      const saved = await addSchedule(eventId, payload);
      setSchedule(saved ?? payload);
    });
  };

  // FAQ
  const handleFaqSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const saved = await addFaq(eventId, {
        questions: faqInput.questions.trim(),
        answers: faqInput.answers.trim(),
      });
      setFaqs((prev) => [
        ...prev,
        saved ?? { questions: faqInput.questions, answers: faqInput.answers },
      ]);
    });
  };

  // Highlight
  const handleHighlightSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const payload = {
        highlights:
          highlightInput.highlights.trim() || highlightInput.name.trim(),
        name: highlightInput.name.trim(),
        smallDescription: highlightInput.smallDescription.trim(),
      };
      console.log(
        "[Highlights] Sending payload:",
        JSON.stringify(payload),
        "to eventId:",
        eventId,
      );
      const saved = await addHighlight(eventId, payload);
      console.log("[Highlights] Backend response:", saved);
      setHighlights((prev) => [...prev, saved ?? payload]);
    });
  };

  // Speaker
  const handleSpeakerSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const saved = await addSpeaker(eventId, {
        name: speakerInput.name.trim(),
        profession: speakerInput.profession.trim(),
        shortExperience: speakerInput.shortExperience.trim(),
      });
      setSpeakers((prev) => [...prev, saved ?? speakerInput]);
    });
  };

  // What To Bring
  const handleWtbSave = (e) => {
    e.preventDefault();
    wrapSave(async () => {
      const saved = await addWhatToBring(eventId, {
        item: wtbInput.item.trim(),
      });
      setWhatToBring((prev) => [...prev, saved ?? { item: wtbInput.item }]);
    });
  };

  /* ═══════════════════════════════════════════════════════════════════════════
     BUTTONS
     ═══════════════════════════════════════════════════════════════════════════ */
  const EditBtn = ({ label, onClick }) => (
    <button className="em-edit-btn" onClick={onClick} title={`Edit ${label}`}>
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
        <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
      </svg>
      Edit {label}
    </button>
  );

  const AddBtn = ({ label, onClick }) => (
    <button className="em-add-btn" onClick={onClick} title={`Add ${label}`}>
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="16" />
        <line x1="8" y1="12" x2="16" y2="12" />
      </svg>
      Add {label}
    </button>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     MODAL FOOTER (shared)
     ═══════════════════════════════════════════════════════════════════════════ */
  const ModalFooter = () => (
    <div className="em-modal__footer">
      <button
        type="button"
        className="em-modal__btn em-modal__btn--cancel"
        onClick={closeModal}
        disabled={saving}
      >
        Cancel
      </button>
      <button
        type="submit"
        className="em-modal__btn em-modal__btn--save"
        disabled={saving}
      >
        {saving ? "Saving..." : "Save"}
      </button>
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     TAB: OVERVIEW
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderOverview = () => (
    <div className="em-tab-content">
      {/* ── About ── */}
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">About This Event</h3>
          <EditBtn label="About" onClick={() => openModal("about")} />
        </div>
        {aboutText ? (
          <p className="em-section__text">{aboutText}</p>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No about info yet.
          </p>
        )}
      </section>

      {/* ── Key Highlights ── */}
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Key Highlights</h3>
          <AddBtn label="Highlight" onClick={() => openModal("highlight")} />
        </div>
        {highlights.length > 0 ? (
          <div className="em-highlights">
            {highlights.map((h, i) => (
              <div key={h.id || i} className="em-highlight-card">
                <svg
                  className="em-highlight-card__icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.5"
                >
                  <circle cx="12" cy="12" r="10" />
                  <path d="M8 12l2.5 2.5L16 9" />
                </svg>
                <div>
                  <strong>{h.name}</strong>
                  {h.smallDescription && (
                    <p className="em-highlight-card__desc">
                      {h.smallDescription}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No highlights added yet.
          </p>
        )}
      </section>

      {/* ── Event Details ── */}
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Event Details</h3>
          <EditBtn label="Details" onClick={() => openModal("details")} />
        </div>
        {detailsList.length > 0 ? (
          <div className="em-details-grid">
            {detailsList.map((d, i) => (
              <div key={i} className="em-detail-item">
                <span className="em-detail-item__label">{d.label}</span>
                <span className="em-detail-item__value">{d.value}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No event details added yet.
          </p>
        )}
      </section>

      {/* ── What to Bring ── */}
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">What to Bring</h3>
          <AddBtn label="Item" onClick={() => openModal("whatToBring")} />
        </div>
        {whatToBring.length > 0 ? (
          <ul className="em-checklist">
            {whatToBring.map((w, i) => (
              <li key={w.id || i} className="em-checklist__item">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  className="em-checklist__icon"
                >
                  <circle
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="#22c55e"
                    strokeWidth="2"
                  />
                  <path
                    d="M8 12l2.5 2.5L16 9"
                    stroke="#22c55e"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <span>{w.item || w}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No items listed yet.
          </p>
        )}
      </section>
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     TAB: SCHEDULE
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderSchedule = () => (
    <div className="em-tab-content">
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Event Schedule</h3>
          <AddBtn label="Session" onClick={() => openModal("schedule")} />
        </div>
        {scheduleSessions.length > 0 ? (
          <div className="em-schedule-list">
            {scheduleSessions.map((s, i) => (
              <div key={i} className="em-schedule-item">
                <div className="em-schedule-item__time">{s.time}</div>
                <div className="em-schedule-item__content">
                  <strong>{s.title}</strong>
                  {s.desc && <p>{s.desc}</p>}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No schedule added yet. Click &ldquo;Add Session&rdquo; to create
            one.
          </p>
        )}
      </section>
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     TAB: SPEAKERS
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderSpeakers = () => (
    <div className="em-tab-content">
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Event Speakers</h3>
          <AddBtn label="Speaker" onClick={() => openModal("speaker")} />
        </div>
        {speakers.length > 0 ? (
          <div className="em-speakers-grid">
            {speakers.map((sp, i) => (
              <div key={sp.id || i} className="em-speaker-card">
                <div className="em-speaker-card__avatar">
                  {(sp.name || "S").charAt(0).toUpperCase()}
                </div>
                <div className="em-speaker-card__info">
                  <strong>{sp.name}</strong>
                  {sp.profession && (
                    <p className="em-speaker-card__role">{sp.profession}</p>
                  )}
                  {sp.shortExperience && (
                    <p className="em-speaker-card__bio">{sp.shortExperience}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No speakers added yet.
          </p>
        )}
      </section>
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     TAB: GALLERY
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderGallery = () => (
    <div className="em-tab-content">
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Event Gallery</h3>
          {/* Hidden file input, triggered by the label-button below */}
          <input
            ref={galleryInputRef}
            id="em-gallery-input"
            type="file"
            accept="image/*"
            multiple
            style={{ display: "none" }}
            onChange={handleGalleryUpload}
          />
          <label
            htmlFor="em-gallery-input"
            className={`em-add-btn em-gallery__upload-label${galleryUploading ? " em-gallery__upload-label--busy" : ""}`}
          >
            {galleryUploading ? (
              <>
                <span className="em-spinner" /> Uploading...
              </>
            ) : (
              <>
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  width="16"
                  height="16"
                >
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
                Upload Images
              </>
            )}
          </label>
        </div>

        {/* Error banner */}
        {galleryError && (
          <div className="em-gallery__error">{galleryError}</div>
        )}

        {/* Loading skeleton */}
        {galleryLoading && (
          <div className="em-gallery__loading">
            <span className="em-spinner" /> Loading gallery...
          </div>
        )}

        {/* Images grid */}
        {!galleryLoading && galleryImages.length > 0 && (
          <div className="em-gallery">
            {galleryImages.map((img) => (
              <div key={img.id} className="em-gallery__item">
                <img
                  src={getImageUrl(img.fileName)}
                  alt={img.originalName || "Event image"}
                  className="em-gallery__img"
                  loading="lazy"
                  onClick={() => setLightboxImg(getImageUrl(img.fileName))}
                />
                <button
                  className="em-gallery__delete"
                  onClick={() => handleDeleteImage(img.id)}
                  title="Delete image"
                  aria-label="Delete image"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    width="14"
                    height="14"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
                {img.originalName && (
                  <div className="em-gallery__caption">{img.originalName}</div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Empty state */}
        {!galleryLoading && galleryImages.length === 0 && !galleryError && (
          <div className="em-gallery__empty">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              width="48"
              height="48"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <p>No images yet.</p>
            <p className="em-gallery__empty-hint">
              Click &ldquo;Upload Images&rdquo; to add photos to this
              event&apos;s gallery.
            </p>
          </div>
        )}
      </section>

      {/* ── Lightbox ── */}
      {lightboxImg && (
        <div className="em-lightbox" onClick={() => setLightboxImg(null)}>
          <button
            className="em-lightbox__close"
            onClick={() => setLightboxImg(null)}
            aria-label="Close"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              width="20"
              height="20"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
          <img
            src={lightboxImg}
            alt="Preview"
            className="em-lightbox__img"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     TAB: FAQS
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderFaqs = () => (
    <div className="em-tab-content">
      <section className="em-section">
        <div className="em-section__header">
          <h3 className="em-section__title">Frequently Asked Questions</h3>
          <AddBtn label="FAQ" onClick={() => openModal("faq")} />
        </div>
        {faqs.length > 0 ? (
          <div className="em-faq-list">
            {faqs.map((faq, i) => (
              <div key={faq.id || i} className="em-faq-item">
                <div className="em-faq-item__q">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className="em-faq-item__icon"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                  <strong>{faq.questions}</strong>
                </div>
                <p className="em-faq-item__a">{faq.answers || "—"}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="em-section__text em-section__text--muted">
            No FAQs added yet.
          </p>
        )}
      </section>
    </div>
  );

  /* ═══════════════════════════════════════════════════════════════════════════
     RENDER TAB
     ═══════════════════════════════════════════════════════════════════════════ */
  const renderTab = () => {
    switch (activeTab) {
      case "overview":
        return renderOverview();
      case "schedule":
        return renderSchedule();
      case "speakers":
        return renderSpeakers();
      case "gallery":
        return renderGallery();
      case "faqs":
        return renderFaqs();
      default:
        return null;
    }
  };

  /* ═══════════════════════════════════════════════════════════════════════════
     MAIN RENDER
     ═══════════════════════════════════════════════════════════════════════════ */
  if (!event && !loading) {
    return (
      <div className="em-page">
        <Navbar />
        <div className="em-error">
          <h2>Event not found</h2>
          <p>No event data available. Go back and select an event.</p>
          <button
            className="em-btn em-btn--primary"
            onClick={() => navigate("/dashboard")}
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="em-page">
        <Navbar />
        <div className="em-error">
          <p>Loading event data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="em-page">
      <Navbar />

      {/* ═══════════════════════════════════════════════════════════════════════
         MODALS
         ═══════════════════════════════════════════════════════════════════════ */}

      {/* ── About ── */}
      {modal === "about" && (
        <Modal
          title="Edit About This Event"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleAboutSave}
        >
          <div className="em-modal__body">
            <label className="em-modal__label">About (max 300 chars)</label>
            <textarea
              className="em-modal__textarea"
              value={aboutInput}
              onChange={(e) => setAboutInput(e.target.value)}
              maxLength={300}
              rows={6}
              placeholder="Describe your event..."
              required
            />
            <span className="em-modal__charcount">{aboutInput.length}/300</span>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── Details ── */}
      {modal === "details" && (
        <Modal
          title="Edit Event Details"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleDetailsSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__row">
              <div className="em-modal__field">
                <label className="em-modal__label">Organized By</label>
                <input
                  className="em-modal__input"
                  value={detailsInput.organizedBy}
                  onChange={(e) =>
                    setDetailsInput({
                      ...detailsInput,
                      organizedBy: e.target.value,
                    })
                  }
                  placeholder="e.g. Computer Science Department"
                />
              </div>
              <div className="em-modal__field">
                <label className="em-modal__label">Duration (Hours)</label>
                <input
                  className="em-modal__input"
                  type="number"
                  min={1}
                  value={detailsInput.durationInHours}
                  onChange={(e) =>
                    setDetailsInput({
                      ...detailsInput,
                      durationInHours: e.target.value,
                    })
                  }
                  placeholder="e.g. 9"
                />
              </div>
            </div>
            <div className="em-modal__row">
              <div className="em-modal__field">
                <label className="em-modal__label">Language</label>
                <input
                  className="em-modal__input"
                  value={detailsInput.language}
                  onChange={(e) =>
                    setDetailsInput({
                      ...detailsInput,
                      language: e.target.value,
                    })
                  }
                  placeholder="e.g. English"
                />
              </div>
              <div className="em-modal__field">
                <label className="em-modal__label">Level</label>
                <input
                  className="em-modal__input"
                  value={detailsInput.level}
                  onChange={(e) =>
                    setDetailsInput({ ...detailsInput, level: e.target.value })
                  }
                  placeholder="e.g. All Levels Welcome"
                />
              </div>
            </div>
            <div className="em-modal__row">
              <div className="em-modal__field">
                <label className="em-modal__label">Contact Email</label>
                <input
                  className="em-modal__input"
                  type="email"
                  value={detailsInput.contactEmail}
                  onChange={(e) =>
                    setDetailsInput({
                      ...detailsInput,
                      contactEmail: e.target.value,
                    })
                  }
                  placeholder="e.g. techfest@college.edu"
                />
              </div>
              <div className="em-modal__field">
                <label className="em-modal__label">Contact Phone</label>
                <input
                  className="em-modal__input"
                  value={detailsInput.contactPhone}
                  onChange={(e) =>
                    setDetailsInput({
                      ...detailsInput,
                      contactPhone: e.target.value,
                    })
                  }
                  placeholder="e.g. +91-9876543210"
                />
              </div>
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── Schedule ── */}
      {modal === "schedule" && (
        <Modal
          title="Add Schedule Session"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleScheduleSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__field">
              <label className="em-modal__label">Time *</label>
              <input
                className="em-modal__input"
                value={scheduleInput.time}
                onChange={(e) =>
                  setScheduleInput({ ...scheduleInput, time: e.target.value })
                }
                placeholder="e.g. 10:00 AM - 11:00 AM"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Session Title *</label>
              <input
                className="em-modal__input"
                value={scheduleInput.schedule}
                onChange={(e) =>
                  setScheduleInput({
                    ...scheduleInput,
                    schedule: e.target.value,
                  })
                }
                placeholder="e.g. Opening Keynote"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Description</label>
              <textarea
                className="em-modal__textarea"
                value={scheduleInput.smallDescription}
                rows={3}
                onChange={(e) =>
                  setScheduleInput({
                    ...scheduleInput,
                    smallDescription: e.target.value,
                  })
                }
                placeholder="Brief description of this session..."
              />
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── FAQ ── */}
      {modal === "faq" && (
        <Modal
          title="Add FAQ"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleFaqSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__field">
              <label className="em-modal__label">Question *</label>
              <input
                className="em-modal__input"
                value={faqInput.questions}
                onChange={(e) =>
                  setFaqInput({ ...faqInput, questions: e.target.value })
                }
                placeholder="e.g. Is there a registration fee?"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Answer *</label>
              <textarea
                className="em-modal__textarea"
                value={faqInput.answers}
                rows={4}
                onChange={(e) =>
                  setFaqInput({ ...faqInput, answers: e.target.value })
                }
                placeholder="Provide a detailed answer..."
                required
              />
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── Highlight ── */}
      {modal === "highlight" && (
        <Modal
          title="Add Key Highlight"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleHighlightSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__field">
              <label className="em-modal__label">Highlight Category *</label>
              <input
                className="em-modal__input"
                value={highlightInput.highlights}
                onChange={(e) =>
                  setHighlightInput({
                    ...highlightInput,
                    highlights: e.target.value,
                  })
                }
                placeholder="e.g. PRIZE, CERTIFICATE, NETWORKING"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Highlight Name *</label>
              <input
                className="em-modal__input"
                value={highlightInput.name}
                onChange={(e) =>
                  setHighlightInput({ ...highlightInput, name: e.target.value })
                }
                placeholder="e.g. Cash Prizes Worth ₹50,000"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Description</label>
              <textarea
                className="em-modal__textarea"
                value={highlightInput.smallDescription}
                rows={3}
                onChange={(e) =>
                  setHighlightInput({
                    ...highlightInput,
                    smallDescription: e.target.value,
                  })
                }
                placeholder="Brief description of this highlight..."
              />
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── Speaker ── */}
      {modal === "speaker" && (
        <Modal
          title="Add Speaker"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleSpeakerSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__field">
              <label className="em-modal__label">Speaker Name *</label>
              <input
                className="em-modal__input"
                value={speakerInput.name}
                onChange={(e) =>
                  setSpeakerInput({ ...speakerInput, name: e.target.value })
                }
                placeholder="e.g. Dr. Jane Doe"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Profession *</label>
              <input
                className="em-modal__input"
                value={speakerInput.profession}
                onChange={(e) =>
                  setSpeakerInput({
                    ...speakerInput,
                    profession: e.target.value,
                  })
                }
                placeholder="e.g. AI Researcher at Google"
                required
              />
            </div>
            <div className="em-modal__field">
              <label className="em-modal__label">Short Experience</label>
              <textarea
                className="em-modal__textarea"
                value={speakerInput.shortExperience}
                rows={3}
                onChange={(e) =>
                  setSpeakerInput({
                    ...speakerInput,
                    shortExperience: e.target.value,
                  })
                }
                placeholder="Brief bio or experience summary..."
              />
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── What to Bring ── */}
      {modal === "whatToBring" && (
        <Modal
          title="Add Item to Bring"
          saving={saving}
          onClose={closeModal}
          onSubmit={handleWtbSave}
        >
          <div className="em-modal__body">
            <div className="em-modal__field">
              <label className="em-modal__label">Item *</label>
              <input
                className="em-modal__input"
                value={wtbInput.item}
                onChange={(e) =>
                  setWtbInput({ ...wtbInput, item: e.target.value })
                }
                placeholder="e.g. Laptop with charger"
                required
              />
            </div>
            {saveError && <p className="em-modal__error">{saveError}</p>}
          </div>
          <ModalFooter />
        </Modal>
      )}

      {/* ── Header ── */}
      <header className="em-header">
        <button className="em-back-btn" onClick={() => navigate("/dashboard")}>
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          Back to Dashboard
        </button>
        <div className="em-header__info">
          {eventImage && (
            <img src={eventImage} alt="" className="em-header__thumb" />
          )}
          <div>
            <h1 className="em-header__title">{eventTitle}</h1>
            <p className="em-header__meta">
              <span>{formatDate(eventDate)}</span>
              <span className="em-header__dot">·</span>
              <span>{eventVenue}</span>
            </p>
          </div>
        </div>
      </header>

      {/* ── Tab navigation ── */}
      <nav className="em-tabs">
        <div className="em-tabs__inner">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              className={`em-tabs__btn ${activeTab === tab.key ? "em-tabs__btn--active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </nav>

      {/* ── Content ── */}
      <main className="em-content">
        {loading ? (
          <div className="em-loading">
            <span className="em-spinner" />
            Loading event data...
          </div>
        ) : (
          renderTab()
        )}
      </main>
    </div>
  );
}
