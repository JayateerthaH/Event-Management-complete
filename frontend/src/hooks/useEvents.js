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

        // One page at a time from the server, with every filter applied there (several categories/venues
        // included). This used to download all events (size 9999) and filter/paginate in the browser.
        const result = await fetchFilteredEvents({
          page,
          size: pageSize,
          search: activeFilters.search || "",
          categories: selectedCategories,
          venues: selectedVenues,
          department,
          dateRange: activeFilters.date || "",
          costType: mapCostFilter(activeFilters.fee),
        });
        const content = Array.isArray(result.content) ? result.content : [];
        const totalPagesVal = result.totalPages;
        const totalElementsVal = result.totalElements;
        const currentPageVal = result.currentPage;

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
