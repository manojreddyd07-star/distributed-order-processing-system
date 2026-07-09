import React, { useState, useEffect } from 'react';
import AuditTimeline from '../../components/audit/AuditTimeline';
import AuditFilters from '../../components/audit/AuditFilters';
import { getAllAuditEvents, getFilteredAuditEvents } from '../../services/auditApi';
import './AuditPage.css';

const AuditPage = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    eventType: null,
    serviceName: null,
    status: null,
  });
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    totalItems: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const [pageSize] = useState(20);

  useEffect(() => {
    loadAuditEvents();
  }, [filters, pagination.currentPage]);

  const loadAuditEvents = async () => {
    setLoading(true);
    setError(null);

    try {
      let response;
      const hasFilters = filters.eventType || filters.serviceName || filters.status;

      if (hasFilters) {
        response = await getFilteredAuditEvents(
          filters,
          pagination.currentPage,
          pageSize
        );
      } else {
        response = await getAllAuditEvents(pagination.currentPage, pageSize);
      }

      setEvents(response.events || []);
      setPagination({
        currentPage: response.currentPage,
        totalPages: response.totalPages,
        totalItems: response.totalItems,
        hasNext: response.hasNext,
        hasPrevious: response.hasPrevious,
      });
    } catch (err) {
      console.error('Error loading audit events:', err);
      setError('Failed to load audit events. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    setPagination((prev) => ({ ...prev, currentPage: 0 }));
  };

  const handleClearFilters = () => {
    setFilters({
      eventType: null,
      serviceName: null,
      status: null,
    });
    setPagination((prev) => ({ ...prev, currentPage: 0 }));
  };

  const handleNextPage = () => {
    if (pagination.hasNext) {
      setPagination((prev) => ({ ...prev, currentPage: prev.currentPage + 1 }));
    }
  };

  const handlePreviousPage = () => {
    if (pagination.hasPrevious) {
      setPagination((prev) => ({ ...prev, currentPage: prev.currentPage - 1 }));
    }
  };

  const handleRefresh = () => {
    loadAuditEvents();
  };

  return (
    <div className="audit-page">
      <div className="audit-header">
        <h1>Audit Trail</h1>
        <button className="refresh-btn" onClick={handleRefresh} disabled={loading}>
          {loading ? 'Loading...' : '↻ Refresh'}
        </button>
      </div>

      <div className="audit-stats">
        <div className="stat-card">
          <div className="stat-label">Total Events</div>
          <div className="stat-value">{pagination.totalItems}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Current Page</div>
          <div className="stat-value">
            {pagination.currentPage + 1} / {pagination.totalPages || 1}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Events Displayed</div>
          <div className="stat-value">{events.length}</div>
        </div>
      </div>

      <div className="audit-content">
        <aside className="audit-sidebar">
          <AuditFilters
            filters={filters}
            onFilterChange={handleFilterChange}
            onClearFilters={handleClearFilters}
          />
        </aside>

        <main className="audit-main">
          {loading && (
            <div className="loading-state">
              <div className="spinner"></div>
              <p>Loading audit events...</p>
            </div>
          )}

          {error && !loading && (
            <div className="error-state">
              <p className="error-message">{error}</p>
              <button onClick={handleRefresh}>Try Again</button>
            </div>
          )}

          {!loading && !error && (
            <>
              <AuditTimeline events={events} />

              {pagination.totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="pagination-btn"
                    onClick={handlePreviousPage}
                    disabled={!pagination.hasPrevious}
                  >
                    ← Previous
                  </button>
                  <span className="pagination-info">
                    Page {pagination.currentPage + 1} of {pagination.totalPages}
                  </span>
                  <button
                    className="pagination-btn"
                    onClick={handleNextPage}
                    disabled={!pagination.hasNext}
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          )}
        </main>
      </div>
    </div>
  );
};

export default AuditPage;
