/**
 * On phones, Mobile.css shows each table row as a card ("Column: value" lines), so columns such as
 * Actions/Approve don't sit off-screen. This labels every cell with its column header (data-label) and
 * marks the table with .rt-cards; it keeps up with tables React renders or updates later.
 */
const labelTable = (table) => {
  const headers = [...table.querySelectorAll(":scope > thead th")].map((th) => th.textContent.trim());
  if (!headers.length) return;
  table.classList.add("rt-cards");
  for (const row of table.querySelectorAll(":scope > tbody > tr")) {
    [...row.children].forEach((cell, i) => {
      // Full-width cells ("No events found") get no label
      const label = cell.colSpan > 1 ? "" : headers[i] || "";
      if (cell.getAttribute("data-label") !== label) cell.setAttribute("data-label", label);
    });
  }
};

export function startResponsiveTables() {
  const labelAll = () => document.querySelectorAll("table").forEach(labelTable);
  labelAll();
  let queued = false;
  // Only childList changes are observed, so setting data-label doesn't retrigger this
  new MutationObserver(() => {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => {
      queued = false;
      labelAll();
    });
  }).observe(document.body, { childList: true, subtree: true });
}
