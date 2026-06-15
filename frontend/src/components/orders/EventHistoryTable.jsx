import React from 'react';
import './EventHistoryTable.css';

const EventHistoryTable = ({ events, loading }) => {
  if (loading) {
    return (
      <div className="event-history-loading">
        <p>Loading events...</p>
      </div>
    );
  }

  if (!events || events.length === 0) {
    return (
      <div className="event-history-empty">
        <p>No events found</p>
      </div>
    );
  }

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  return (
    <div className="event-history-table-container">
      <table className="event-history-table">
        <thead>
          <tr>
            <th>Event ID</th>
            <th>Event Type</th>
            <th>Order ID</th>
            <th>Event Timestamp</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.eventId}>
              <td className="event-id">{event.eventId}</td>
              <td className="event-type">
                <span className={`event-type-badge ${event.eventType.toLowerCase()}`}>
                  {event.eventType}
                </span>
              </td>
              <td className="order-id">{event.orderId}</td>
              <td className="event-timestamp">{formatTimestamp(event.eventTimestamp)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default EventHistoryTable;
