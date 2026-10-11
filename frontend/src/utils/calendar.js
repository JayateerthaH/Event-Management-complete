/**
 * Builds and downloads an iCalendar (.ics) file for an event — importable by Google Calendar,
 * Apple Calendar and Outlook (RFC 5545).
 *
 * Events are created with a date only (stored as midnight UTC), so those become all-day entries;
 * an event with a real start time becomes a timed entry lasting `durationInHours` (default 1h).
 */

const pad = (n) => String(n).padStart(2, "0");

const utcDate = (d) => `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}`;
const utcDateTime = (d) => `${utcDate(d)}T${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}${pad(d.getUTCSeconds())}Z`;

// Text values must escape backslashes, semicolons, commas and newlines
const escapeText = (value) =>
  String(value ?? "")
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\r?\n/g, "\\n");

// Lines longer than 75 octets are folded with CRLF + space
const fold = (line) => {
  const parts = [];
  let rest = line;
  while (new TextEncoder().encode(rest).length > 75) {
    let cut = 75;
    while (new TextEncoder().encode(rest.slice(0, cut)).length > 75) cut--;
    parts.push(rest.slice(0, cut));
    rest = rest.slice(cut);
  }
  parts.push(rest);
  return parts.join("\r\n ");
};

export const buildIcs = (event, { durationInHours, pageUrl } = {}) => {
  const start = new Date(event.eventDate ?? event.date);
  if (Number.isNaN(start.getTime())) throw new Error("This event has no valid date.");

  const allDay = start.getUTCHours() === 0 && start.getUTCMinutes() === 0;
  const timing = allDay
    ? [
        `DTSTART;VALUE=DATE:${utcDate(start)}`,
        `DTEND;VALUE=DATE:${utcDate(new Date(start.getTime() + 24 * 3600 * 1000))}`,
      ]
    : [
        `DTSTART:${utcDateTime(start)}`,
        `DTEND:${utcDateTime(new Date(start.getTime() + (Number(durationInHours) > 0 ? Number(durationInHours) : 1) * 3600 * 1000))}`,
      ];

  const description = [event.description, pageUrl && `Event page: ${pageUrl}`].filter(Boolean).join("\n\n");

  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//CampusEvents//Event Calendar//EN",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:event-${event.id}@campusevents`,
    `DTSTAMP:${utcDateTime(new Date())}`,
    ...timing,
    `SUMMARY:${escapeText(event.name || "Campus event")}`,
    event.venue && `LOCATION:${escapeText(event.venue)}`,
    description && `DESCRIPTION:${escapeText(description)}`,
    pageUrl && `URL:${pageUrl}`,
    "END:VEVENT",
    "END:VCALENDAR",
  ].filter(Boolean);

  return lines.map(fold).join("\r\n") + "\r\n";
};

export const downloadIcs = (event, options) => {
  const blob = new Blob([buildIcs(event, options)], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${String(event.name || "event").replace(/[^a-z0-9]+/gi, "-").replace(/^-|-$/g, "").toLowerCase() || "event"}.ics`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};
