import React, { useState, useEffect } from 'react';
import EventHistoryTable from './EventHistoryTable';
import './OrderEventTimeline.css';

const OrderEventTimeline = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  // Mock event data generator
  const generateMockEvents = () => {
    const mockEvents = [
      {
        eventId: 'evt-' + Math.random().toString(36).substr(2, 9),
        eventType: 'ORDER_CREATED',
        orderId: 1001,
        eventTimestamp: new Date(Date.now() - 5000).toISOString(),
      },
      {
        eventId: 'evt-' + Math.random().toString(36).substr(2, 9),
        eventType: 'ORDER_CREATED',
        orderId: 1002,
        eventTimestamp: new Date(Date.now() - 10000).toISOString(),
      },
      {
        eventId: 'evt-' + Math.random().toString(36).substr(2, 9),
        eventType: 'ORDER_CREATED',
        orderId: 1003,
        eventTimestamp: new Date(Date.now() - 15000).toISOString(),
      },
      {
        eventId: 'evt-' + Math.random().toString(36).substr(2, 9),
        eventType: 'ORDER_CREATED',
        orderId: 1004,
        eventTimestamp: new Date(Date.now() - 20000).toISOString(),
      },
      {
        eventId: 'evt-' + Math.random().toString(36).substr(2, 9),
        eventType: 'ORDER_CREATED',
        orderId: 1005,
        eventTimestamp: new Date(Date.now() - 25000).toISOString(),
      },
    ];

    return mockEvents.sort((a, b) => 
      new Date(b.eventTimestamp) - new Date(a.eventTimestamp)
    );
  };

  // Load mock events on component mount
  useEffect(() => {
    loadEvents();
  }, []);

  const loadEvents = () => {
    setLoading(true);
    
    // Simulate API call delay
    setTimeout(() => {
      const mockData = generateMockEvents();
      setEvents(mockData);
      setLoading(false);
    }, 500);
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
