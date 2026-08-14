import React, { useState, useEffect } from 'react';
import EventHistoryTable from './EventHistoryTable';
import { getAllAuditEvents } from '../../services/auditApi';
import './OrderEventTimeline.css';

const OrderEventTimeline = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadEvents();
  }, []);

  const loadEvents = async () => {
    setLoading(true);
    try {
      const response = await getAllAuditEvents(0, 20);
      setEvents((response.events || []).map(event => ({
        ...event,
        eventTimestamp: event.createdAt,
      })));
    } catch (error) {
      console.error('Error loading order events:', error);
      setEvents([]);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadEvents();
  };

  return (
    <div className="order-event-timeline">
      <div className="timeline-header">
        <h2>Order Event Timeline</h2>
        <button 
          className="refresh-button" 
          onClick={handleRefresh}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : '🔄 Refresh'}
        </button>
      </div>
      
      <div className="timeline-content">
        <EventHistoryTable events={events} loading={loading} />
      </div>

      {!loading && events.length > 0 && (
        <div className="timeline-footer">
          <p>Showing {events.length} event{events.length !== 1 ? 's' : ''}</p>
        </div>
      )}
    </div>
  );
};

export default OrderEventTimeline;
