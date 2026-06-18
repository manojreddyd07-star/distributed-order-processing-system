import React, { useState, useEffect } from 'react';
import ValidationStatusBadge from './ValidationStatusBadge';
import { getValidationHistory } from '../../services/validationApi';
import './ValidationTable.css';

const ValidationTable = ({ refreshTrigger }) => {
  const [validations, setValidations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchValidations();
  }, [refreshTrigger]);

  const fetchValidations = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getValidationHistory();
      setValidations(data);
    } catch (err) {
      setError(err.message || 'Failed to load validations');
      console.error('Error fetching validations:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Format date/time for display
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="validation-table-container">
        <div className="loading-message">Loading validations...</div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="validation-table-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchValidations} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (validations.length === 0) {
    return (
      <div className="validation-table-container">
        <div className="empty-message">No validations found</div>
      </div>
    );
  }

  // Validations table
  return (
    <div className="validation-table-container">
      <div className="table-actions">
        <button onClick={fetchValidations} className="refresh-button">
          Refresh
        </button>
      </div>
      <table className="validation-table">
        <thead>
          <tr>
            <th>Validation ID</th>
            <th>Order ID</th>
            <th>Status</th>
            <th>Message</th>
            <th>Validated At</th>
          </tr>
        </thead>
        <tbody>
          {validations.map((validation) => (
            <tr key={validation.id}>
              <td>{validation.id}</td>
              <td>{validation.orderId}</td>
              <td>
                <ValidationStatusBadge status={validation.validationStatus} />
              </td>
              <td>{validation.validationMessage}</td>
              <td>{formatDateTime(validation.validatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ValidationTable;
