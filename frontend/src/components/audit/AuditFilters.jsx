import React from 'react';
import './AuditFilters.css';

const AuditFilters = ({ filters, onFilterChange, onClearFilters }) => {
  const eventTypes = [
    'ORDER_CREATED',
    'VALIDATION_SUCCESS',
    'VALIDATION_FAILED',
    'PAYMENT_SUCCESS',
    'PAYMENT_FAILED',
    'INVENTORY_SUCCESS',
    'INVENTORY_FAILED',
    'FULFILLMENT_SUCCESS',
    'FULFILLMENT_FAILED',
  ];

  const services = [
    'order-service',
    'validation-service',
    'payment-service',
    'inventory-service',
    'fulfillment-service',
  ];

  const statuses = ['SUCCESS', 'FAILED', 'PENDING', 'CREATED'];

  const handleFilterChange = (filterName, value) => {
    onFilterChange({
      ...filters,
      [filterName]: value === '' ? null : value,
    });
  };

  return (
    <div className="audit-filters">
      <h3>Filters</h3>
      
      <div className="filter-group">
        <label htmlFor="eventType">Event Type</label>
        <select
          id="eventType"
          value={filters.eventType || ''}
          onChange={(e) => handleFilterChange('eventType', e.target.value)}
        >
          <option value="">All Event Types</option>
          {eventTypes.map((type) => (
            <option key={type} value={type}>
              {type.replace(/_/g, ' ')}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="serviceName">Service Name</label>
        <select
          id="serviceName"
          value={filters.serviceName || ''}
          onChange={(e) => handleFilterChange('serviceName', e.target.value)}
        >
          <option value="">All Services</option>
          {services.map((service) => (
            <option key={service} value={service}>
              {service}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="status">Status</label>
        <select
          id="status"
          value={filters.status || ''}
          onChange={(e) => handleFilterChange('status', e.target.value)}
        >
          <option value="">All Statuses</option>
          {statuses.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      <button className="clear-filters-btn" onClick={onClearFilters}>
        Clear Filters
      </button>
    </div>
  );
};

export default AuditFilters;
