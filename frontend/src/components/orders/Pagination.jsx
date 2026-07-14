import React from 'react';
import './Pagination.css';

const Pagination = ({ currentPage, totalPages, totalElements, pageSize, onPageChange, onPageSizeChange }) => {
  const handlePrevious = () => {
    if (currentPage > 0) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages - 1) {
      onPageChange(currentPage + 1);
    }
  };

  const handlePageClick = (page) => {
    onPageChange(page);
  };

  const handlePageSizeChange = (e) => {
    onPageSizeChange(parseInt(e.target.value));
  };

  // Generate page numbers to display
  const getPageNumbers = () => {
    const pages = [];
    const maxPagesToShow = 5;
    
    if (totalPages <= maxPagesToShow) {
      // Show all pages if total pages are less than max
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show first page
      pages.push(0);
      
      // Calculate range around current page
      let start = Math.max(1, currentPage - 1);
      let end = Math.min(totalPages - 2, currentPage + 1);
      
      // Adjust range if near start or end
      if (currentPage <= 2) {
        end = 3;
      } else if (currentPage >= totalPages - 3) {
        start = totalPages - 4;
      }
      
      // Add ellipsis if needed
      if (start > 1) {
        pages.push('...');
      }
      
      // Add middle pages
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      
      // Add ellipsis if needed
      if (end < totalPages - 2) {
        pages.push('...');
      }
      
      // Add last page
      pages.push(totalPages - 1);
    }
    
    return pages;
  };

  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="pagination-container">
      <div className="pagination-info">
        Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} results
      </div>
      
      <div className="pagination-controls">
        <button
          className="pagination-btn"
          onClick={handlePrevious}
          disabled={currentPage === 0}
        >
          Previous
        </button>

        <div className="pagination-pages">
          {getPageNumbers().map((page, index) => (
            page === '...' ? (
              <span key={`ellipsis-${index}`} className="pagination-ellipsis">...</span>
            ) : (
              <button
                key={page}
                className={`pagination-page ${currentPage === page ? 'active' : ''}`}
                onClick={() => handlePageClick(page)}
              >
                {page + 1}
              </button>
            )
          ))}
        </div>

        <button
          className="pagination-btn"
          onClick={handleNext}
          disabled={currentPage === totalPages - 1}
        >
          Next
        </button>
      </div>

      <div className="pagination-size">
        <label htmlFor="pageSize">Rows per page:</label>
        <select
          id="pageSize"
          value={pageSize}
          onChange={handlePageSizeChange}
        >
          <option value="5">5</option>
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </select>
      </div>
    </div>
  );
};

export default Pagination;
