import { useState, useMemo, useEffect, useCallback } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import useEventDetails from "../hooks/useEventDetails";
import Navbar from "../components/Navbar";
import HeroBanner from "../components/event-details/HeroBanner";
import TabsNavigation from "../components/event-details/TabsNavigation";
import OverviewTab from "../components/event-details/OverviewTab";
import ScheduleTab from "../components/event-details/ScheduleTab";
import FaqTab from "../components/event-details/FaqTab";
import SidebarCard from "../components/event-details/SidebarCard";
import RegisterModal from "../components/event-details/RegisterModal";
import EventAssistant from "../components/ai/EventAssistant";
import SimilarEvents from "../components/ai/SimilarEvents";
import { getEventImages, getImageUrl } from "../api/imageService";
import { fetchEventAnnouncements } from "../api/announcementService";
import "../styles/EventDetails.css";

export default function EventDetailsPage() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState("overview");
  const [showRegister, setShowRegister] = useState(false);

  // Gallery state
  const [galleryImages, setGalleryImages] = useState([]);
  const [galleryLoading, setGalleryLoading] = useState(false);
  const [lightboxImg, setLightboxImg] = useState(null);

  // Announcements state
  const [announcements, setAnnouncements] = useState([]);
  const [, setAnnouncementsLoading] = useState(false);

  // Event summary passed from EventCard via navigation state.
  // On a hard refresh location.state will be null - the hook falls back
  // to fetching /events/{id} from the backend.
  const navEvent = location.state?.event ?? null;

  const {
    event,
    about,
    details,
    schedule,
    faqs,
    highlights,
    speakers,
    whatToBring,
    loading,
    scheduleLoading,
    faqsLoading,
    error,
    refetchSchedule,
    refetchFaqs,
    refetchAbout,
    refetchEvent,
  } = useEventDetails(eventId, navEvent);

  // Determine if user is organizer / admin
  const isOrganizer = useMemo(() => {
    if (!user) return false;
    const roles = user.roles || [];
    return roles.some((r) =>
      ["ORGANIZER", "ADMIN", "ROLE_ORGANIZER", "ROLE_ADMIN"].includes(
        r?.toUpperCase?.(),
      ),
    );
  }, [user]);

  // Load gallery images when gallery tab becomes active
  const loadGallery = useCallback(async () => {
    if (!eventId) return;
    setGalleryLoading(true);
    try {
      const imgs = await getEventImages(eventId);
      setGalleryImages(Array.isArray(imgs) ? imgs : []);
    } catch (err) {
      console.error("[EventDetails] gallery load error:", err);
      setGalleryImages([]);
    } finally {
      setGalleryLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    if (activeTab === "gallery") loadGallery();
  }, [activeTab, loadGallery]);

  // Load announcements when the event is loaded and user is registered
  const isRegistered = event?.isRegistered === true;
  useEffect(() => {
    if (!eventId || !isRegistered) return;
    let cancelled = false;
    setAnnouncementsLoading(true);
    fetchEventAnnouncements(eventId)
      .then((data) => {
        if (!cancelled) setAnnouncements(data);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setAnnouncementsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [eventId, isRegistered]);

  // ── Loading state ──
  if (loading) {
    return (
      <div className="ed-page">
        <Navbar />
        <div className="ed-loading">
          <div className="ed-loading__spinner" />
          <p>Loading event details...</p>
        </div>
      </div>
    );
  }

  // ── Error state ──
  if (error || !event) {
    return (
      <div className="ed-page">
        <Navbar />
        <div className="ed-error">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
          <h2>Event not found</h2>
          <p>
            {error ||
              "The event you're looking for doesn't exist or has been removed."}
          </p>
          <button
            className="ed-btn ed-btn--primary"
            onClick={() => navigate("/events")}
          >
            Back to Events
          </button>
        </div>
      </div>
    );
  }

  // ── Tab content ──
  const renderTab = () => {
    switch (activeTab) {
      case "overview":
        return (
          <OverviewTab
            event={event}
            about={about}
            details={details}
            highlights={highlights}
            whatToBring={whatToBring}
            isOrganizer={isOrganizer}
            onAboutUpdated={refetchAbout}
          />
        );
      case "schedule":
        return (
          <ScheduleTab
            eventId={eventId}
            schedule={schedule}
            loading={scheduleLoading}
            isOrganizer={isOrganizer}
            onSessionAdded={refetchSchedule}
          />
        );
      case "faqs":
        return (
          <FaqTab
            eventId={eventId}
            faqs={faqs}
            loading={faqsLoading}
            isOrganizer={isOrganizer}
            onFaqAdded={refetchFaqs}
          />
        );
      case "speakers":
        return (
          <div className="ed-speakers">
            <section className="ed-section">
              <h2 className="ed-section__title">Event Speakers</h2>
              {speakers && speakers.length > 0 ? (
                <div className="ed-speakers-grid">
                  {speakers.map((sp, i) => (
                    <div key={sp.id || i} className="ed-speaker-card">
                      <div className="ed-speaker-card__avatar">
                        {(sp.name || "S").charAt(0).toUpperCase()}
                      </div>
                      <div className="ed-speaker-card__info">
                        <h3 className="ed-speaker-card__name">{sp.name}</h3>
                        {sp.profession && (
                          <p className="ed-speaker-card__role">
                            {sp.profession}
                          </p>
                        )}
                        {sp.shortExperience && (
                          <p className="ed-speaker-card__bio">
                            {sp.shortExperience}
                          </p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="ed-section__text ed-section__text--muted">
                  No speakers added yet.
                </p>
              )}
            </section>
          </div>
        );
      case "gallery":
        return (
          <div className="ed-gallery-tab">
            <section className="ed-section">
              <h2 className="ed-section__title">Event Gallery</h2>
              {galleryLoading && (
                <div className="ed-gallery__loading">
                  <div className="ed-loading__spinner" />
                  <span>Loading gallery...</span>
                </div>
              )}
              {!galleryLoading && galleryImages.length > 0 ? (
                <div className="ed-gallery">
                  {galleryImages.map((img) => (
                    <div
                      key={img.id}
                      className="ed-gallery__item"
                      onClick={() => setLightboxImg(getImageUrl(img.fileName))}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) =>
                        e.key === "Enter" &&
                        setLightboxImg(getImageUrl(img.fileName))
                      }
                      aria-label={img.originalName || "View image"}
                    >
                      <img
                        src={getImageUrl(img.fileName)}
                        alt={img.originalName || "Event image"}
                        className="ed-gallery__img"
                        loading="lazy"
                      />
                    </div>
                  ))}
                </div>
              ) : (
                !galleryLoading && (
                  <div className="ed-placeholder">
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.5"
                    >
                      <rect x="3" y="3" width="18" height="18" rx="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <polyline points="21 15 16 10 5 21" />
                    </svg>
                    <h3>Gallery</h3>
                    <p>No gallery images have been added to this event yet.</p>
                  </div>
                )
              )}
            </section>

            {/* Lightbox */}
            {lightboxImg && (
              <div className="ed-lightbox" onClick={() => setLightboxImg(null)}>
                <button
                  className="ed-lightbox__close"
                  onClick={() => setLightboxImg(null)}
                  aria-label="Close"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    width="22"
                    height="22"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
                <img
                  src={lightboxImg}
                  alt="Gallery preview"
                  className="ed-lightbox__img"
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
            )}
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="ed-page">
      <Navbar />
      <HeroBanner event={event} />

      {/* ── Latest Announcement Banner (visible to registered users) ── */}
      {isRegistered &&
        announcements.length > 0 &&
        (() => {
          const latest = announcements[announcements.length - 1];
          return (
            <div className="ed-announcements">
              <div className="ed-announcements__header">
                <div className="ed-announcements__icon">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 01-3.46 0" />
                  </svg>
                </div>
                <h3 className="ed-announcements__title">Latest Announcement</h3>
              </div>
              <div className="ed-announcements__list">
                <div className="ed-announcement-card">
                  <div className="ed-announcement-card__dot" />
                  <div className="ed-announcement-card__content">
                    <h4 className="ed-announcement-card__title">
                      {latest.title}
                    </h4>
                    <p className="ed-announcement-card__message">
                      {latest.announcement}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          );
        })()}

      <TabsNavigation activeTab={activeTab} onTabChange={setActiveTab} />
      <div className="ed-layout">
        <main className="ed-main">
          {renderTab()}
          <EventAssistant key={eventId} eventId={eventId} eventName={event.name} />
        </main>
        <SidebarCard
          event={event}
          details={details}
          onRegister={() => setShowRegister(true)}
          refetchEvent={refetchEvent}
        />
      </div>
      <div className="ed-similar">
        <SimilarEvents eventId={eventId} />
      </div>
      {showRegister && (
        <RegisterModal
          event={event}
          onClose={() => setShowRegister(false)}
          onSuccess={() => {
            setShowRegister(false);
            refetchEvent();
          }}
        />
      )}
    </div>
  );
}
