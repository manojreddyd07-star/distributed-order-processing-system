import React, { useState, useEffect } from 'react';
import FailedEventsTable from '../../components/dlq/FailedEventsTable';
import DlqDetailsModal from '../../components/dlq/DlqDetailsModal';
import { getAllFailedEvents } from '../../services/dlqApi';
import './DLQPage.css';

const DLQPage = () => {
  const [failedEvents, setFailedEvents] = useState([]);
  const [filteredEvents, setFilteredEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [serviceFilter, setServiceFilter] = useState('ALL');
  const [selectedEvent, setSelectedEvent] = useState(null);

  const fetchFailedEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllFailedEvents();
      setFailedEvents(data);
      setFilteredEvents(data);
    } catch (err) {
      setError('Failed to fetch failed events. Please try again later.');
      console.error('Error fetching failed events:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFailedEvents();
    // Refresh every 30 seconds
    const interval = setInterval(fetchFailedEvents, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    let filtered = failedEvents;

    // Apply service filter
    if (serviceFilter !== 'ALL') {
      filtered = filtered.filter(event => event.serviceName === serviceFilter);
    }

    setFilteredEvents(filtered);
  }, [serviceFilter, failedEvents]);

  const handleRefresh = () => {
    fetchFailedEvents();
  };

  const handleServiceFilterChange = (event) => {
    setServiceFilter(event.target.value);
  };

  const handleEventClick = (event) => {
    setSelectedEvent(event);
  };

  const handleCloseModal = () => {
    setSelectedEvent(null);
  };

  // Get unique service names for filter
  const uniqueServices = [...new Set(failedEvents.map(e => e.serviceName))];

  return (
    <div className="dlq-page">
      <div className="dlq-page-header">
        <div className="header-content">
          <h1>Dead Letter Queue (DLQ)</h1>
          <p>Failed events that have exhausted all retry attempts</p>
        </div>
        <button 
          className="refresh-button" 
          onClick={handleRefresh}
          disabled={loading}
        >
          {loading ? '🔄 Refreshing...' : '🔄 Refresh'}
        </button>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-icon">⚠️</span>
          {error}
        </div>
      )}

      <div className="filters-section">
        <div className="filter-group">
          <label htmlFor="service-filter">Service:</label>
          <select
            id="service-filter"
            value={serviceFilter}
            onChange={handleServiceFilterChange}
            className="filter-select"
          >
            <option value="ALL">All Services</option>
            {uniqueServices.map(service => (
              <option key={service} value={service}>{service}</option>
            ))}
          </select>
        </div>

        <div className="stats-summary">
          <span className="stat-item">
            Total Failed: <strong>{filteredEvents.length}</strong>
          </span>
        </div>
      </div>

      <div className="dlq-content">
        {loading && failedEvents.length === 0 ? (
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading failed events...</p>
          </div>
        ) : (
          <FailedEventsTable 
            failedEvents={filteredEvents} 
            onEventClick={handleEventClick}
          />
        )}
      </div>

      {selectedEvent && (
        <DlqDetailsModal 
          event={selectedEvent} 
          onClose={handleCloseModal} 
        />
      )}
    </div>
  );
};

export default DLQPage;
