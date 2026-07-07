import React from 'react';
import './DlqDetailsModal.css';

const DlqDetailsModal = ({ event, onClose }) => {
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      timeZoneName: 'short'
    });
  };

  const formatPayload = (payload) => {
    if (!payload) return 'N/A';
    try {
      const parsed = JSON.parse(payload);
      return JSON.stringify(parsed, null, 2);
    } catch (e) {
      return payload;
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Failed Event Details</h2>
          <button className="close-button" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="detail-section">
            <h3>Event Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Event ID:</label>
                <span className="detail-value event-id">{event.eventId}</span>
              </div>
              <div className="detail-item">
                <label>Event Type:</label>
                <span className="detail-value">{event.eventType}</span>
              </div>
              <div className="detail-item">
                <label>Service Name:</label>
                <span className={`service-badge ${event.serviceName?.toLowerCase()}`}>
                  {event.serviceName}
                </span>
              </div>
              <div className="detail-item">
                <label>Failed At:</label>
                <span className="detail-value">{formatTimestamp(event.failedAt)}</span>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>Error Information</h3>
            <div className="error-box">
              {event.errorMessage || 'No error message available'}
            </div>
          </div>

          <div className="detail-section">
            <h3>Event Payload</h3>
            <div className="payload-box">
              <pre>{formatPayload(event.payload)}</pre>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button className="close-modal-button" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
};

export default DlqDetailsModal;
