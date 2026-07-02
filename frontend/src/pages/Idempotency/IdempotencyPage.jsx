import React, { useState } from 'react';
import DuplicateEventTable from '../../components/idempotency/DuplicateEventTable';
import './IdempotencyPage.css';

const IdempotencyPage = () => {
  const [statusFilter, setStatusFilter] = useState('ALL');

  const handleFilterChange = (event) => {
    setStatusFilter(event.target.value);
  };

  return (
    <div className="idempotency-page">
      <div className="idempotency-page-header">
        <h1>Idempotency Records</h1>
        <p>Track duplicate event processing across all services</p>
      </div>

      <div className="filter-section">
        <label htmlFor="status-filter">Filter by Status:</label>
        <select 
          id="status-filter" 
          value={statusFilter} 
          onChange={handleFilterChange}
          className="status-filter-select"
        >
          <option value="ALL">All Statuses</option>
          <option value="PROCESSED">Processed</option>
          <option value="FAILED">Failed</option>
          <option value="PROCESSING">Processing</option>
        </select>
      </div>

      <DuplicateEventTable statusFilter={statusFilter} />
    </div>
  );
};

export default IdempotencyPage;
