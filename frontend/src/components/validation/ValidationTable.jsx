import React, { useState, useEffect } from 'react';
import ValidationStatusBadge from './ValidationStatusBadge';
import './ValidationTable.css';

const ValidationTable = ({ refreshTrigger }) => {
  const [validations, setValidations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Mock data for initial development
  // TODO: Replace with actual API call to validation-service
  useEffect(() => {
    fetchValidations();
  }, [refreshTrigger]);

  const fetchValidations = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Fetch from validation-service API
      const response = await fetch('http://localhost:8081/api/validations');
      
      if (!response.ok) {
        throw new Error('Failed to fetch validations');
      }
      
      const data = await response.json();
      setValidations(data);
    } catch (err) {
      setError(err.message || 'Failed to load validations');
      
      // Fallback to mock data for development
      const mockData = [
        {
          id: 1,
          orderId: 101,
          validationStatus: 'VALIDATED',
          validationMessage: 'Order validated successfully',
          validatedAt: '2024-01-15T10:30:00'
        },
        {
          id: 2,
          orderId: 102,
          validationStatus: 'FAILED',
          validationMessage: 'Invalid customer ID',
          validatedAt: '2024-01-15T10:31:00'
        },
        {
          id: 3,
          orderId: 103,
          validationStatus: 'VALIDATED',
          validationMessage: 'Order validated successfully',
          validatedAt: '2024-01-15T10:32:00'
        }
      ];
      setValidations(mockData);
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
