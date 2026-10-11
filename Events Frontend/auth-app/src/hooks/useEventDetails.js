import { useState, useEffect, useCallback } from "react";
import {
  fetchEventById,
  fetchAboutEvent,
  fetchEventDetails,
  fetchSchedule,
  fetchFaqs,
  fetchHighlights,
  fetchSpeakers,
  fetchWhatToBring,
} from "../api/eventDetailsService";

/**
 * Hook that renders the event header immediately from navigation state
 * and fetches all secondary data (about, details, schedule, faqs, highlights, speakers, whatToBring).
 */
const useEventDetails = (eventId, initialEvent = null) => {
  const [event, setEvent] = useState(initialEvent);
  const [about, setAbout] = useState(null);
  const [details, setDetails] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [faqs, setFaqs] = useState([]);
  const [highlights, setHighlights] = useState([]);
  const [speakers, setSpeakers] = useState([]);
  const [whatToBring, setWhatToBring] = useState([]);

  const [loading, setLoading] = useState(!initialEvent);
  const [scheduleLoading, setScheduleLoading] = useState(true);
  const [faqsLoading, setFaqsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!eventId) return;
      setScheduleLoading(true);
      setFaqsLoading(true);
      setError(null);

      // Always fetch the event from the backend so fields like isRegistered,
      // registeredCount etc. are always accurate (the navigation-state initialEvent
      // may be stale or missing those live fields).  We only show the page-level
      // loading spinner when we have no cached data to show yet.
      if (!initialEvent) setLoading(true);

      const promises = [
        fetchEventById(eventId),
        fetchAboutEvent(eventId),
        fetchEventDetails(eventId),
        fetchSchedule(eventId),
        fetchFaqs(eventId),
        fetchHighlights(eventId),
        fetchSpeakers(eventId),
        fetchWhatToBring(eventId),
      ];

      try {
        const [
          eventR,
          aboutR,
          detailsR,
          scheduleR,
          faqsR,
          highlightsR,
          speakersR,
          wtbR,
        ] = await Promise.allSettled(promises);

        if (cancelled) return;

        if (eventR.status === "fulfilled") setEvent(eventR.value);
        else setError(eventR.reason?.message || "Failed to load event");

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
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) {
          setLoading(false);
          setScheduleLoading(false);
          setFaqsLoading(false);
        }
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [eventId]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Individual refetch helpers ─────────────────────────────────────────
  const refetchAbout = useCallback(async () => {
    if (!eventId) return;
    try {
      setAbout(await fetchAboutEvent(eventId));
    } catch {
      /* ignore */
    }
  }, [eventId]);

  const refetchSchedule = useCallback(async () => {
    if (!eventId) return;
    setScheduleLoading(true);
    try {
      setSchedule(await fetchSchedule(eventId));
    } finally {
      setScheduleLoading(false);
    }
  }, [eventId]);

  const refetchFaqs = useCallback(async () => {
    if (!eventId) return;
    setFaqsLoading(true);
    try {
      setFaqs(await fetchFaqs(eventId));
    } finally {
      setFaqsLoading(false);
    }
  }, [eventId]);

  const refetchEvent = useCallback(async () => {
    if (!eventId) return;
    try {
      setEvent(await fetchEventById(eventId));
    } catch {
      /* ignore */
    }
  }, [eventId]);

  return {
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
  };
};

export default useEventDetails;
