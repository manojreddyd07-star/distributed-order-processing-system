import React, { useState, useEffect } from 'react';
import { replayEvent, getAvailableTopics } from '../../services/replayApi';
import { getAllFailedEvents } from '../../services/dlqApi';
import './ReplayPage.css';

const ReplayPage = () => {
  const [eventId, setEventId] = useState('');
  const [eventType, setEventType] = useState('');
  const [replayTopic, setReplayTopic] = useState('');
  const [serviceName, setServiceName] = useState('');
  const [loading, setLoading] = useState(false);
  const [replayStatus, setReplayStatus] = useState(null);
  const [notification, setNotification] = useState(null);
  const [failedEvents, setFailedEvents] = useState([]);
  const [selectedEvent, setSelectedEvent] = useState(null);

  const availableTopics = getAvailableTopics();

  useEffect(() => {
    fetchFailedEvents();
  }, []);

  const fetchFailedEvents = async () => {
    try {
      const events = await getAllFailedEvents();
      setFailedEvents(events);
    } catch (error) {
      console.error('Error fetching failed events:', error);
    }
  };

  const handleEventSelect = (e) => {
    const selectedEventId = e.target.value;
    if (selectedEventId) {
      const event = failedEvents.find(ev => ev.eventId === selectedEventId);
      if (event) {
        setSelectedEvent(event);
        setEventId(event.eventId);
        setEventType(event.eventType);
        setServiceName(event.serviceName);
        // Clear previous replay topic selection
        setReplayTopic('');
      }
    } else {
      setSelectedEvent(null);
      setEventId('');
      setEventType('');
      setServiceName('');
      setReplayTopic('');
    }
  };

  const handleReplaySubmit = async (e) => {
    e.preventDefault();
    
    // Validate inputs
    if (!eventId || !replayTopic || !serviceName) {
      showNotification('error', 'Please fill in all required fields');
      return;
    }

    setLoading(true);
    setReplayStatus(null);
    setNotification(null);

    try {
      const replayRequest = {
        eventId,
        eventType,
        replayTopic,
      };

      const response = await replayEvent(serviceName, replayRequest);
      
      if (response.success) {
        setReplayStatus(response);
        showNotification('success', `Event ${eventId} replayed successfully to ${replayTopic}`);
        
        // Reset form after successful replay
        setTimeout(() => {
          resetForm();
        }, 3000);
      } else {
        showNotification('error', response.message || 'Failed to replay event');
      }
    } catch (error) {
      showNotification('error', error.message || 'Failed to replay event. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const showNotification = (type, message) => {
    setNotification({ type, message });
    
    // Auto-hide notification after 5 seconds
    setTimeout(() => {
      setNotification(null);
    }, 5000);
  };

  const resetForm = () => {
    setEventId('');
    setEventType('');
    setReplayTopic('');
    setServiceName('');
    setSelectedEvent(null);
    setReplayStatus(null);
  };

  return (
    <div className="replay-page">
      <div className="replay-page-header">
        <div className="header-content">
          <h1>Event Replay</h1>
          <p>Replay failed events to recover from errors</p>
        </div>
      </div>

      {notification && (
        <div className={`notification ${notification.type}`}>
          <span className="notification-icon">
            {notification.type === 'success' ? '✓' : '⚠️'}
          </span>
          <span className="notification-message">{notification.message}</span>
          <button 
            className="notification-close"
            onClick={() => setNotification(null)}
          >
            ×
          </button>
        </div>
      )}

      <div className="replay-content">
        <div className="replay-form-container">
          <h2>Replay Event Form</h2>
          
          <form onSubmit={handleReplaySubmit} className="replay-form">
            <div className="form-group">
              <label htmlFor="event-select">
                Select Failed Event <span className="required">*</span>
              </label>
              <select
                id="event-select"
                value={selectedEvent ? selectedEvent.eventId : ''}
                onChange={handleEventSelect}
                className="form-input"
                disabled={loading}
              >
                <option value="">-- Select an event --</option>
                {failedEvents.map((event) => (
                  <option key={event.id} value={event.eventId}>
                    {event.eventId} - {event.eventType} ({event.serviceName})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="event-id">
                Event ID <span className="required">*</span>
              </label>
              <input
                type="text"
                id="event-id"
                value={eventId}
                onChange={(e) => setEventId(e.target.value)}
                placeholder="Enter event ID"
                className="form-input"
                disabled={loading}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="event-type">Event Type</label>
              <input
                type="text"
                id="event-type"
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
                placeholder="Enter event type (optional)"
                className="form-input"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="service-name">
                Service Name <span className="required">*</span>
              </label>
              <select
                id="service-name"
                value={serviceName}
                onChange={(e) => setServiceName(e.target.value)}
                className="form-input"
                disabled={loading}
                required
              >
                <option value="">-- Select service --</option>
                <option value="validation-service">validation-service</option>
                <option value="payment-service">payment-service</option>
                <option value="inventory-service">inventory-service</option>
                <option value="fulfillment-service">fulfillment-service</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="replay-topic">
                Replay Topic <span className="required">*</span>
              </label>
              <select
                id="replay-topic"
                value={replayTopic}
                onChange={(e) => setReplayTopic(e.target.value)}
                className="form-input"
                disabled={loading}
                required
              >
                <option value="">-- Select replay topic --</option>
                {availableTopics.map((topic) => (
                  <option key={topic.value} value={topic.value}>
                    {topic.label}
                  </option>
                ))}
              </select>
              <small className="form-help">
                Select the Kafka topic to replay the event to
              </small>
            </div>

            <div className="form-actions">
              <button
                type="submit"
                className="btn-replay"
                disabled={loading || !eventId || !replayTopic || !serviceName}
              >
                {loading ? (
                  <>
                    <span className="spinner-small"></span>
                    Replaying...
                  </>
                ) : (
                  <>🔄 Replay Event</>
                )}
              </button>
              
              <button
                type="button"
                className="btn-reset"
                onClick={resetForm}
                disabled={loading}
              >
                Reset
              </button>
            </div>
          </form>
        </div>

        {selectedEvent && (
          <div className="event-details-container">
            <h2>Selected Event Details</h2>
            <div className="event-details">
              <div className="detail-row">
                <span className="detail-label">Event ID:</span>
                <span className="detail-value">{selectedEvent.eventId}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Event Type:</span>
                <span className="detail-value">{selectedEvent.eventType}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Service:</span>
                <span className="detail-value">{selectedEvent.serviceName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Failed At:</span>
                <span className="detail-value">
                  {new Date(selectedEvent.failedAt).toLocaleString()}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Error Message:</span>
                <span className="detail-value error-message">
                  {selectedEvent.errorMessage}
                </span>
              </div>
            </div>
          </div>
        )}

        {replayStatus && replayStatus.success && (
          <div className="replay-status-container success">
            <h2>✓ Replay Successful</h2>
            <div className="status-details">
              <div className="detail-row">
                <span className="detail-label">Event ID:</span>
                <span className="detail-value">{replayStatus.eventId}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Event Type:</span>
                <span className="detail-value">{replayStatus.eventType}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Replayed To:</span>
                <span className="detail-value">{replayStatus.replayTopic}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Replayed At:</span>
                <span className="detail-value">
                  {new Date(replayStatus.replayedAt).toLocaleString()}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReplayPage;
