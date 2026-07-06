import React, { useState, useEffect } from 'react';
import RetryMetricsCard from '../../components/retry/RetryMetricsCard';
import RetryAttemptsTable from '../../components/retry/RetryAttemptsTable';
import { getAllRetryRecords } from '../../services/retryApi';
import './RetryPage.css';

const RetryPage = () => {
  const [retryRecords, setRetryRecords] = useState([]);
  const [filteredRecords, setFilteredRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [serviceFilter, setServiceFilter] = useState('ALL');

  const fetchRetryRecords = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllRetryRecords();
      setRetryRecords(data);
      setFilteredRecords(data);
    } catch (err) {
      setError('Failed to fetch retry records. Please try again later.');
      console.error('Error fetching retry records:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRetryRecords();
    // Refresh every 10 seconds
    const interval = setInterval(fetchRetryRecords, 10000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    let filtered = retryRecords;

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(record => record.retryStatus === statusFilter);
    }

    // Apply service filter
    if (serviceFilter !== 'ALL') {
      filtered = filtered.filter(record => record.serviceName === serviceFilter);
    }

    setFilteredRecords(filtered);
  }, [statusFilter, serviceFilter, retryRecords]);

  const handleRefresh = () => {
    fetchRetryRecords();
  };

  const handleStatusFilterChange = (event) => {
    setStatusFilter(event.target.value);
  };

  const handleServiceFilterChange = (event) => {
    setServiceFilter(event.target.value);
  };

  // Get unique service names for filter
  const uniqueServices = [...new Set(retryRecords.map(r => r.serviceName))];

  return (
    <div className="retry-page">
      <div className="retry-page-header">
        <div className="header-content">
          <h1>Retry Dashboard</h1>
          <p>Monitor and track event retry attempts across all services</p>
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
          <span>⚠️</span>
          <p>{error}</p>
        </div>
      )}

      <RetryMetricsCard retryRecords={retryRecords} />

      <div className="filters-section">
        <div className="filter-group">
          <label htmlFor="status-filter">Filter by Status:</label>
          <select 
            id="status-filter" 
            value={statusFilter} 
            onChange={handleStatusFilterChange}
            className="filter-select"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="SUCCESS">Success</option>
            <option value="EXHAUSTED">Exhausted</option>
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="service-filter">Filter by Service:</label>
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

        <div className="records-count">
          Showing {filteredRecords.length} of {retryRecords.length} records
        </div>
      </div>

      {loading && retryRecords.length === 0 ? (
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading retry records...</p>
        </div>
      ) : (
        <RetryAttemptsTable retryRecords={filteredRecords} />
      )}
    </div>
  );
};

export default RetryPage;
