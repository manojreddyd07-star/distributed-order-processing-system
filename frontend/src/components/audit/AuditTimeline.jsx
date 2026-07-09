import React from 'react';
import './AuditTimeline.css';

const AuditTimeline = ({ events }) => {
  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const getStatusClass = (status) => {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return 'status-success';
      case 'FAILED':
        return 'status-failed';
      case 'PENDING':
        return 'status-pending';
      case 'CREATED':
        return 'status-created';
      default:
        return 'status-default';
    }
  };

  const getEventTypeClass = (eventType) => {
    if (eventType.includes('ORDER')) return 'event-order';
    if (eventType.includes('VALIDATION')) return 'event-validation';
    if (eventType.includes('PAYMENT')) return 'event-payment';
    if (eventType.includes('INVENTORY')) return 'event-inventory';
    if (eventType.includes('FULFILLMENT')) return 'event-fulfillment';
    return 'event-default';
  };

  if (!events || events.length === 0) {
    return (
      <div className="audit-timeline-empty">
        <p>No audit events found</p>
      </div>
    );
  }

  return (
    <div className="audit-timeline">
      {events.map((event, index) => (
        <div key={event.id || index} className="timeline-item">
          <div className={`timeline-marker ${getEventTypeClass(event.eventType)}`}>
            <div className="marker-dot"></div>
          </div>
          
          <div className="timeline-content">
            <div className="timeline-header">
              <div className="event-info">
                <span className={`event-type ${getEventTypeClass(event.eventType)}`}>
                  {event.eventType.replace(/_/g, ' ')}
                </span>
                <span className={`event-status ${getStatusClass(event.status)}`}>
                  {event.status}
                </span>
              </div>
              <div className="event-time">{formatTimestamp(event.createdAt)}</div>
            </div>

            <div className="timeline-body">
              <div className="event-details">
                <div className="detail-row">
                  <span className="detail-label">Event ID:</span>
                  <span className="detail-value">{event.eventId}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Service:</span>
                  <span className="detail-value">{event.serviceName}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Order ID:</span>
                  <span className="detail-value">{event.orderId}</span>
                </div>
                {event.message && (
                  <div className="detail-row message-row">
                    <span className="detail-label">Message:</span>
                    <span className="detail-value">{event.message}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default AuditTimeline;
