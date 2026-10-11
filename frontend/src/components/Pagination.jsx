import { memo, useMemo } from 'react';
import '../styles/Pagination.css';

const Pagination = memo(({ 
  currentPage, 
  totalPages, 
  onPageChange,
  hasNextPage,
  hasPrevPage,
  maxVisiblePages = 5
}) => {
  const pageNumbers = useMemo(() => {
    const pages = [];
    
    if (totalPages <= maxVisiblePages) {
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      const halfVisible = Math.floor(maxVisiblePages / 2);
      let startPage = Math.max(0, currentPage - halfVisible);
      let endPage = Math.min(totalPages - 1, currentPage + halfVisible);
      
      if (currentPage <= halfVisible) {
        endPage = maxVisiblePages - 1;
      } else if (currentPage >= totalPages - halfVisible - 1) {
        startPage = totalPages - maxVisiblePages;
      }
      
      if (startPage > 0) {
        pages.push(0);
        if (startPage > 1) pages.push('...');
      }
      
      for (let i = startPage; i <= endPage; i++) {
        if (!pages.includes(i)) pages.push(i);
      }
      
      if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) pages.push('...');
        pages.push(totalPages - 1);
      }
    }
    
    return pages;
  }, [currentPage, totalPages, maxVisiblePages]);

  // Always render — even a single page shows the bar with disabled prev/next
  if (totalPages === 0 && pageNumbers.length === 0) return null;

  return (
    <nav className="pagination" aria-label="Event pages navigation">
      <button
        className="pagination-btn prev"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={!hasPrevPage}
        aria-label="Previous page"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <polyline points="15,18 9,12 15,6" strokeWidth="2"/>
        </svg>
      </button>
      
      <div className="pagination-numbers">
        {pageNumbers.map((page, index) => (
          page === '...' ? (
            <span key={`ellipsis-${index}`} className="pagination-ellipsis">...</span>
          ) : (
            <button
              key={page}
              className={`pagination-number ${currentPage === page ? 'active' : ''}`}
              onClick={() => onPageChange(page)}
              aria-current={currentPage === page ? 'page' : undefined}
              aria-label={`Page ${page + 1}`}
            >
              {page + 1}
            </button>
          )
        ))}
      </div>
      
      <button
        className="pagination-btn next"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={!hasNextPage}
        aria-label="Next page"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <polyline points="9,18 15,12 9,6" strokeWidth="2"/>
        </svg>
      </button>
    </nav>
  );
});

Pagination.displayName = 'Pagination';

export default Pagination;
