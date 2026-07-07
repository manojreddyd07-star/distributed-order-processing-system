import React from 'react';
import './FailedEventsTable.css';

const FailedEventsTable = ({ failedEvents, onEventClick }) => {
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const truncateText = (text, maxLength = 100) => {
    if (!text) return 'N/A';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  if (failedEvents.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">✅</div>
        <h3>No Failed Events</h3>
        <p>All events are processing successfully!</p>
      </div>
    );
  }

  return (
    <div className="failed-events-table-container">
      <table className="failed-events-table">
        <thead>
          <tr>
            <th>Event ID</th>
            <th>Event Type</th>
            <th>Service Name</th>
            <th>Error Message</th>
            <th>Failed At</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {failedEvents.map((event, index) => (
            <tr key={event.id || index}>
              <td className="event-id">{event.eventId}</td>
              <td className="event-type">{event.eventType}</td>
              <td className="service-name">
                <span className={`service-badge ${event.serviceName?.toLowerCase()}`}>
                  {event.serviceName}
                </span>
              </td>
              <td className="error-message" title={event.errorMessage}>
                {truncateText(event.errorMessage, 80)}
              </td>
              <td className="failed-at">{formatTimestamp(event.failedAt)}</td>
              <td className="actions">
                <button 
                  className="details-button"
                  onClick={() => onEventClick(event)}
                >
                  View Details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default FailedEventsTable;
