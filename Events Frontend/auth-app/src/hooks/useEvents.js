import { useState, useEffect, useCallback } from "react";
import {
  fetchFilteredEvents,
  fetchFilterOptions,
} from "../api/eventService";

const useEvents = (initialPage = 0, pageSize = 6) => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState({});
  const [filterOptions, setFilterOptions] = useState({
    categories: [],
    venues: [],
    departments: [{ id: "all", label: "All Departments" }],
  });

  const mapCostFilter = (value) => {
    if (value === "free") return "FREE";
    if (value === "paid") return "PAID";
    return "";
  };

  const normalize = (value) =>
    typeof value === "string" ? value.trim().toLowerCase() : "";

  const fetchEvents = useCallback(
    async (page, activeFilters = {}) => {
      setLoading(true);
      setError(null);

      try {
        const selectedCategories = Array.isArray(activeFilters.categories)
          ? activeFilters.categories
          : [];
        const selectedVenues = Array.isArray(activeFilters.venues)
          ? activeFilters.venues
          : [];

        const department =
          activeFilters.department === "all" || !activeFilters.department
            ? ""
            : activeFilters.department;

        const hasActiveFilters = Boolean(
          (activeFilters.search || "").trim() ||
            selectedCategories.length ||
            selectedVenues.length ||
            (activeFilters.date || "").trim() ||
            (activeFilters.fee || "").trim() ||
            department,
        );

        // When multiple categories/venues are selected the backend cannot
        // filter for them in one request (it only accepts a single value).
        // In that case we fetch a large result-set with the other server-side
        // filters and apply category/venue/department filtering on the client,
        // then paginate the filtered list ourselves.
        const needsClientFilter = hasActiveFilters;

        let content, totalPagesVal, totalElementsVal, currentPageVal;

        if (hasActiveFilters && needsClientFilter) {
          // Fetch a large batch — utilize server side filtering as much as possible
          const bulk = await fetchFilteredEvents({
            page: 0,
            size: 9999,
            search: activeFilters.search || "",
            // If only one category/venue selected, pass it to backend.
            // If multiple, pass empty string and filter on client.
            category: selectedCategories.length === 1 ? selectedCategories[0] : "",
            venue: selectedVenues.length === 1 ? selectedVenues[0] : "",
            department: department, // Pass department to backend
            dateRange: activeFilters.date || "",
            costType: mapCostFilter(activeFilters.fee),
          });

          let all = Array.isArray(bulk.content) ? bulk.content : [];

          // Optimization: Only apply client-side filtering for multi-selects
          // or fields not handled by backend. Trust backend for single values.

          // 1. Search (Optional refinement, but backend LIKE should suffice)
          const searchTerm = normalize(activeFilters.search);
          if (searchTerm) {
            // Keep lightweight client search just in case
            all = all.filter((ev) => {
              const haystack = [
                ev?.name,
                ev?.title,
                ev?.description,
                ev?.category,
                ev?.venue,
                ev?.eventField,
                ev?.field,
                ev?.department,
              ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();
              return haystack.includes(searchTerm);
            });
          }

          // 2. Categories (Only if multiple selected, otherwise backend handled it)
          if (selectedCategories.length > 1) {
            const set = new Set(selectedCategories.map(normalize));
            all = all.filter((ev) => set.has(normalize(ev?.category)));
          }

          // 3. Venues (Only if multiple selected)
          if (selectedVenues.length > 1) {
            const set = new Set(selectedVenues.map(normalize));
            all = all.filter((ev) => set.has(normalize(ev?.venue)));
          }

          // 4. Department - SKIP client filter (Backend handles single select correctly)
          // 5. Date - SKIP client filter (Backend handles normalization correctly)
          // 6. Fee - SKIP client filter (Backend handles FREE/PAID logic)

          // Client-side pagination
          totalElementsVal = all.length;
          totalPagesVal = Math.ceil(totalElementsVal / pageSize) || 1;
          currentPageVal = Math.min(page, totalPagesVal - 1);
          const start = currentPageVal * pageSize;
          content = all.slice(start, start + pageSize);
        } else {
          // Public listing: approved events only
          const result = await fetchFilteredEvents({ page, size: pageSize });
          content = Array.isArray(result.content) ? result.content : [];
          totalPagesVal = result.totalPages;
          totalElementsVal = result.totalElements;
          currentPageVal = result.currentPage;
        }

        setEvents(content);
        setTotalPages(totalPagesVal);
        setTotalElements(totalElementsVal);
        setCurrentPage(currentPageVal);
      } catch (err) {
        if (err.message === "SESSION_EXPIRED") return;
        setError(err.message);
        setEvents([]);
      } finally {
        setLoading(false);
      }
    },
    [pageSize],
  );

  const loadFilterOptions = useCallback(async () => {
    try {
      const options = await fetchFilterOptions();
      setFilterOptions(options);
    } catch {
      // Keep default empty options if metadata fetch fails.
      setFilterOptions((prev) => prev);
    }
  }, []);

  // Re-fetch whenever page or filters change
  useEffect(() => {
    fetchEvents(currentPage, filters);
  }, [currentPage, filters, fetchEvents]);

  useEffect(() => {
    loadFilterOptions();
  }, [loadFilterOptions]);

  const applyFilters = useCallback((newFilters) => {
    setFilters(newFilters);
    setCurrentPage(0); // reset to first page on filter change
  }, []);

  const clearFilters = useCallback(() => {
    setFilters({});
    setCurrentPage(0);
  }, []);

  const goToPage = useCallback(
    (page) => {
      if (page >= 0 && page < totalPages) setCurrentPage(page);
    },
    [totalPages],
  );

  const nextPage = useCallback(() => {
    if (currentPage < totalPages - 1) setCurrentPage((prev) => prev + 1);
  }, [currentPage, totalPages]);

  const prevPage = useCallback(() => {
    if (currentPage > 0) setCurrentPage((prev) => prev - 1);
  }, [currentPage]);

  const refresh = useCallback(() => {
    fetchEvents(currentPage, filters);
  }, [currentPage, filters, fetchEvents]);

  return {
    events,
    loading,
    error,
    currentPage,
    totalPages,
    totalElements,
    filters,
    filterOptions,
    applyFilters,
    clearFilters,
    goToPage,
    nextPage,
    prevPage,
    refresh,
    hasNextPage: currentPage < totalPages - 1,
    hasPrevPage: currentPage > 0,
  };
};

export default useEvents;
