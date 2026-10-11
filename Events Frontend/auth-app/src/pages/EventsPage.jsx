import { useCallback } from "react";
import Navbar from "../components/Navbar";
import SidebarFilters from "../components/SidebarFilters";
import EventCard from "../components/EventCard";
import Pagination from "../components/Pagination";
import useEvents from "../hooks/useEvents";
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

  const handleApplyFilters = useCallback(
    (filters) => {
      applyFilters(filters);
    },
    [applyFilters],
  );

  const handleClearFilters = useCallback(() => {
    clearFilters();
  }, [clearFilters]);

  const handleRegister = useCallback((eventId) => {
    console.log("Registering for event:", eventId);
    // Navigation or modal logic would go here
  }, []);

  // Determine which events are "hot" or "trending" based on some criteria
  const getEventBadge = (event, index) => {
    // Mock logic - in real app, this would come from backend
    if (index === 0) return { isHot: true, isTrending: false };
    if (event.maxPeople && event.maxPeople > 500)
      return { isHot: false, isTrending: true };
    return { isHot: false, isTrending: false };
  };

  return (
    <div className="events-page-wrapper">
      <Navbar />
      <div className="events-page">
        <SidebarFilters
          onApplyFilters={handleApplyFilters}
          onClearAll={handleClearFilters}
          options={filterOptions}
        />

        <main className="events-main">
          <div className="events-header">
            <h1 className="events-page-title">Discover Events</h1>
            <p className="events-page-subtitle">
              Find and register for exciting campus events
            </p>
          </div>

          {error ? (
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
              {events.map((event, index) => {
                const { isHot, isTrending } = getEventBadge(event, index);
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

          <div className="pagination-bar">
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={goToPage}
              hasNextPage={hasNextPage}
              hasPrevPage={hasPrevPage}
            />
          </div>
        </main>
      </div>
    </div>
  );
};

export default EventsPage;
