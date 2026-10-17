import { useState, useEffect, memo, useCallback } from "react";
import "../styles/SidebarFilters.css";

const DATE_FILTERS = [
  { id: "today", label: "Today" },
  { id: "tomorrow", label: "Tomorrow" },
  { id: "this-week", label: "This Week" },
  { id: "this-month", label: "This Month" },
];

const FEE_FILTERS = [
  { id: "free", label: "Free Events" },
  { id: "paid", label: "Paid Events" },
];

const SidebarFilters = memo(({ onApplyFilters, onClearAll, options = {}, isOpen = false, onClose }) => {
  const categories = Array.isArray(options.categories)
    ? options.categories
    : [];
  const venues = Array.isArray(options.venues) ? options.venues : [];
  const departments =
    Array.isArray(options.departments) && options.departments.length
      ? options.departments
      : [{ id: "all", label: "All Departments" }];

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedDepartment, setSelectedDepartment] = useState("all");
  const [selectedVenue, setSelectedVenue] = useState("all");
  const [selectedFeeFilter, setSelectedFeeFilter] = useState("");
  const [isDepartmentOpen, setIsDepartmentOpen] = useState(false);
  const [isVenueOpen, setIsVenueOpen] = useState(false);

  const handleCategoryChange = useCallback((categoryId) => {
    setSelectedCategories((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId],
    );
  }, []);

  const handleDateChange = useCallback((dateId) => {
    setSelectedDate((prev) => (prev === dateId ? "" : dateId));
  }, []);

  const handleFeeChange = useCallback((feeId) => {
    setSelectedFeeFilter((prev) => (prev === feeId ? "" : feeId));
  }, []);

  const handleClearAll = useCallback(() => {
    setSearchQuery("");
    setSelectedCategories([]);
    setSelectedDate("");
    setSelectedDepartment("all");
    setSelectedVenue("all");
    setSelectedFeeFilter("");
    if (onClearAll) onClearAll();
  }, [onClearAll]);

  const handleApplyFilters = useCallback(() => {
    const filters = {
      search: searchQuery,
      categories: selectedCategories,
      date: selectedDate,
      department: selectedDepartment,
      venues: selectedVenue !== "all" ? [selectedVenue] : [],
      fee: selectedFeeFilter,
    };
    if (onApplyFilters) onApplyFilters(filters);
  }, [
    searchQuery,
    selectedCategories,
    selectedDate,
    selectedDepartment,
    selectedVenue,
    selectedFeeFilter,
    onApplyFilters,
  ]);

  // Automatically apply filters when selection changes (debounced)
  useEffect(() => {
    const timer = setTimeout(() => {
      handleApplyFilters();
    }, 500); // 500ms debounce
    return () => clearTimeout(timer);
  }, [handleApplyFilters]);

  const hasActiveFilters =
    searchQuery ||
    selectedCategories.length > 0 ||
    selectedDate ||
    selectedDepartment !== "all" ||
    selectedVenue !== "all" ||
    selectedFeeFilter;

  return (
    <aside
      className={`sidebar-filters${isOpen ? " sidebar-filters--open" : ""}`}
      aria-label="Event filters"
    >
      <div className="filters-header">
        <h2 className="filters-title">
          <svg
            className="filter-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <polygon
              points="22,3 2,3 10,12.46 10,19 14,21 14,12.46"
              strokeWidth="2"
            />
          </svg>
          Filters
        </h2>
        <div className="filters-header__actions">
          {hasActiveFilters && (
            <button className="clear-all-btn" onClick={handleClearAll}>
              Clear All
            </button>
          )}
          {/* Phones only: closes the full-screen filter panel */}
          <button type="button" className="filters-close-btn" onClick={onClose} aria-label="Close filters">
            ✕
          </button>
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Search Events</h3>
        <div className="search-input-wrapper">
          <svg
            className="search-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <circle cx="11" cy="11" r="8" strokeWidth="2" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" strokeWidth="2" />
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder="Search by title, keywords..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Category</h3>
        <div className="checkbox-group">
          {categories.map((category) => (
            <label key={category.id} className="checkbox-item">
              <input
                type="checkbox"
                checked={selectedCategories.includes(category.id)}
                onChange={() => handleCategoryChange(category.id)}
              />
              <span className="checkbox-custom"></span>
              <span className="checkbox-label">{category.label}</span>
              {typeof category.count === "number" && (
                <span className="checkbox-count">({category.count})</span>
              )}
            </label>
          ))}
          {categories.length === 0 && (
            <p className="filter-empty">No categories found</p>
          )}
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Date</h3>
        <div className="radio-group">
          {DATE_FILTERS.map((date) => (
            <label key={date.id} className="radio-item">
              <input
                type="radio"
                name="date-filter"
                checked={selectedDate === date.id}
                onChange={() => handleDateChange(date.id)}
              />
              <span className="radio-custom"></span>
              <span className="radio-label">{date.label}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Department</h3>
        <div className="dropdown-wrapper">
          <button
            className="dropdown-trigger"
            onClick={() => setIsDepartmentOpen(!isDepartmentOpen)}
          >
            <span>
              {departments.find((d) => d.id === selectedDepartment)?.label ||
                "All Departments"}
            </span>
            <svg
              className={`dropdown-arrow ${isDepartmentOpen ? "open" : ""}`}
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <polyline points="6,9 12,15 18,9" strokeWidth="2" />
            </svg>
          </button>
          {isDepartmentOpen && (
            <div className="dropdown-menu">
              {departments.map((dept) => (
                <button
                  key={dept.id}
                  className={`dropdown-item ${selectedDepartment === dept.id ? "active" : ""}`}
                  onClick={() => {
                    setSelectedDepartment(dept.id);
                    setIsDepartmentOpen(false);
                  }}
                >
                  {dept.label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Venue</h3>
        <div className="dropdown-wrapper">
          <button
            className="dropdown-trigger"
            onClick={() => setIsVenueOpen(!isVenueOpen)}
          >
            <span>
              {venues.find((v) => v.id === selectedVenue)?.label ||
                "All Venues"}
            </span>
            <svg
              className={`dropdown-arrow ${isVenueOpen ? "open" : ""}`}
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <polyline points="6,9 12,15 18,9" strokeWidth="2" />
            </svg>
          </button>
          {isVenueOpen && (
            <div className="dropdown-menu">
              <button
                className={`dropdown-item ${selectedVenue === "all" ? "active" : ""}`}
                onClick={() => {
                  setSelectedVenue("all");
                  setIsVenueOpen(false);
                }}
              >
                All Venues
              </button>
              {venues.map((venue) => (
                <button
                  key={venue.id}
                  className={`dropdown-item ${selectedVenue === venue.id ? "active" : ""}`}
                  onClick={() => {
                    setSelectedVenue(venue.id);
                    setIsVenueOpen(false);
                  }}
                >
                  {venue.label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="filter-section">
        <h3 className="filter-section-title">Registration Fee</h3>
        <div className="checkbox-group">
          {FEE_FILTERS.map((fee) => (
            <label key={fee.id} className="checkbox-item">
              <input
                type="checkbox"
                checked={selectedFeeFilter === fee.id}
                onChange={() => handleFeeChange(fee.id)}
              />
              <span className="checkbox-custom"></span>
              <span className="checkbox-label">{fee.label}</span>
            </label>
          ))}
        </div>
      </div>

      <button
        className="apply-filters-btn"
        onClick={() => {
          handleApplyFilters();
          onClose?.(); // on phones, show the results
        }}
      >
        <svg
          className="check-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
        >
          <polyline points="20,6 9,17 4,12" strokeWidth="2" />
        </svg>
        Apply Filters
      </button>
    </aside>
  );
});

SidebarFilters.displayName = "SidebarFilters";

export default SidebarFilters;
