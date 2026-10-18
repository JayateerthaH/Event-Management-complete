import { useCallback, useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import api from "../api/apiClient";
import SidebarFilters from "../components/SidebarFilters";
import EventCard from "../components/EventCard";
import Pagination from "../components/Pagination";
import useEvents from "../hooks/useEvents";
import SmartSearch from "../components/ai/SmartSearch";
import "../styles/EventsPage.css";

const EventCardSkeleton = () => (
  <div className="event-card-skeleton">
    <div className="skeleton-image">
      <div className="skeleton-shimmer"></div>
    </div>
    <div className="skeleton-content">
      <div className="skeleton-header">
        <div className="skeleton-tag"></div>
        <div className="skeleton-date"></div>
      </div>
      <div className="skeleton-title"></div>
      <div className="skeleton-description"></div>
      <div className="skeleton-description short"></div>
      <div className="skeleton-info"></div>
      <div className="skeleton-footer">
        <div className="skeleton-price"></div>
        <div className="skeleton-button"></div>
      </div>
    </div>
  </div>
);

const EmptyState = () => (
  <div className="empty-state">
    <svg
      className="empty-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
    >
      <circle cx="12" cy="12" r="10" strokeWidth="2" />
      <path d="M8 15s1.5 2 4 2 4-2 4-2" strokeWidth="2" />
      <line
        x1="9"
        y1="9"
        x2="9.01"
        y2="9"
        strokeWidth="3"
        strokeLinecap="round"
      />
      <line
        x1="15"
        y1="9"
        x2="15.01"
        y2="9"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
    <h3>No Events Found</h3>
    <p>Try adjusting your filters or check back later for new events.</p>
  </div>
);

const ErrorState = ({ message, onRetry }) => (
  <div className="error-state">
    <svg
      className="error-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
    >
      <circle cx="12" cy="12" r="10" strokeWidth="2" />
      <line x1="12" y1="8" x2="12" y2="12" strokeWidth="2" />
      <line x1="12" y1="16" x2="12.01" y2="16" strokeWidth="2" />
    </svg>
    <h3>Something went wrong</h3>
    <p>{message}</p>
    <button className="retry-btn" onClick={onRetry}>
      Try Again
    </button>
  </div>
);

const EventsPage = () => {
  const {
    events,
    loading,
    error,
    currentPage,
    totalPages,
    goToPage,
    hasNextPage,
    hasPrevPage,
    refresh,
    applyFilters,
    clearFilters,
    filterOptions,
  } = useEvents(0, 6);
  // While AI search results are shown, the regular filtered grid is hidden
  const [aiActive, setAiActive] = useState(false);
  // Phones: the filters open as a full-screen panel from the "Filters" button
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [activeFilterCount, setActiveFilterCount] = useState(0);
  const [quick, setQuick] = useState(null); // the active quick filter chip, if any

  useEffect(() => {
    if (!filtersOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden"; // keep the page behind the panel from scrolling
    return () => {
      document.body.style.overflow = previous;
    };
  }, [filtersOpen]);

  const handleApplyFilters = useCallback(
    (filters) => {
      applyFilters(filters);
      setQuick(null);
      setActiveFilterCount(
        (filters.search?.trim() ? 1 : 0) +
          (filters.categories?.length || 0) +
          (filters.date ? 1 : 0) +
          (filters.department && filters.department !== "all" ? 1 : 0) +
          (filters.venues?.length || 0) +
          (filters.fee ? 1 : 0),
      );
    },
    [applyFilters],
  );

  const handleClearFilters = useCallback(() => {
    setQuick(null);
    clearFilters();
  }, [clearFilters]);

  const handleRegister = useCallback((eventId) => {
    console.log("Registering for event:", eventId);
    // Navigation or modal logic would go here
  }, []);

  // Badges from real activity: the two most saved/registered events this week are "hot", the rest of the
  // trending list "trending"
  const [trendingRank, setTrendingRank] = useState({});
  useEffect(() => {
    api.get("/events/trending", { params: { limit: 12 } })
      .then(({ data }) => setTrendingRank(Object.fromEntries((Array.isArray(data) ? data : []).map((e, i) => [e.id, i]))))
      .catch(() => {});
  }, []);
  const getEventBadge = (event) => {
    const rank = trendingRank[event.id];
    return { isHot: rank != null && rank < 2, isTrending: rank != null && rank >= 2 };
  };

  // Quick filters for the most common questions: what's on today, this week, and what's free
  const QUICK = [
    { id: "today", label: "Today", filters: { date: "today" } },
    { id: "week", label: "This week", filters: { date: "this-week" } },
    { id: "free", label: "Free", filters: { fee: "free" } },
  ];
  const applyQuick = (q) => {
    if (quick === q.id) {
      setQuick(null);
      clearFilters();
      setActiveFilterCount(0);
    } else {
      setQuick(q.id);
      applyFilters(q.filters);
      setActiveFilterCount(1);
    }
  };

  return (
    <div className="events-page-wrapper">
      <Navbar />
      <div className="events-page">
        <SidebarFilters
          onApplyFilters={handleApplyFilters}
          onClearAll={handleClearFilters}
          options={filterOptions}
          isOpen={filtersOpen}
          onClose={() => setFiltersOpen(false)}
        />

        <main className="events-main">
          <div className="events-header">
            <h1 className="events-page-title">Discover Events</h1>
            <p className="events-page-subtitle">
              Find and register for exciting campus events
            </p>
            {/* Shown on phones only (Mobile.css); the sidebar is hidden there */}
            <button
              type="button"
              className="mobile-filters-btn"
              onClick={() => setFiltersOpen(true)}
              aria-haspopup="dialog"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <polygon points="22,3 2,3 10,12.46 10,19 14,21 14,12.46" />
              </svg>
              Filters
              {activeFilterCount > 0 && <span className="mobile-filters-btn__count">{activeFilterCount}</span>}
            </button>
          </div>

          <SmartSearch onActiveChange={setAiActive} />

          {!aiActive && (
            <div className="quick-filters" role="group" aria-label="Quick filters">
              {QUICK.map((q) => (
                <button key={q.id} type="button" className={`quick-filter${quick === q.id ? " is-on" : ""}`}
                  aria-pressed={quick === q.id} onClick={() => applyQuick(q)}>
                  {q.label}
                </button>
              ))}
            </div>
          )}

          {aiActive ? null : error ? (
            <ErrorState message={error} onRetry={refresh} />
          ) : loading ? (
            <div className="events-grid">
              {[...Array(6)].map((_, i) => (
                <EventCardSkeleton key={i} />
              ))}
            </div>
          ) : events.length === 0 ? (
            <EmptyState />
          ) : (
            <div className="events-grid">
              {events.map((event) => {
                const { isHot, isTrending } = getEventBadge(event);
                return (
                  <EventCard
                    key={event.id}
                    id={event.id}
                    name={event.name}
                    description={event.description}
                    category={event.category}
                    field={event.field}
                    eventDate={event.eventDate}
                    venue={event.venue}
                    maxPeople={event.maxPeople}
                    costOfRegistration={event.costOfRegistration}
                    imageUrl={event.imageUrl}
                    isHot={isHot}
                    isTrending={isTrending}
                    onRegister={handleRegister}
                  />
                );
              })}
            </div>
          )}

          {!aiActive && (
            <div className="pagination-bar">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={goToPage}
                hasNextPage={hasNextPage}
                hasPrevPage={hasPrevPage}
              />
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default EventsPage;
