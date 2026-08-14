import React, { useState, useEffect } from 'react';
import PaymentStatusBadge from './PaymentStatusBadge';
import { getPaymentHistory } from '../../shared/api/paymentApi';
import './PaymentTable.css';

const PaymentTable = ({ refreshTrigger }) => {
  const [payments, setPayments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPayments();
  }, [refreshTrigger]);

  const fetchPayments = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getPaymentHistory();
      setPayments(data);
    } catch (err) {
      setError(err.message || 'Failed to load payments');
      console.error('Error fetching payments:', err);
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

  // Format amount as currency
  const formatAmount = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="payment-table-container">
        <div className="loading-message">Loading payments...</div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="payment-table-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchPayments} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (payments.length === 0) {
    return (
      <div className="payment-table-container">
        <div className="empty-message">No payments found</div>
      </div>
    );
  }

  // Payments table
  return (
    <div className="payment-table-container">
      <div className="table-actions">
        <button onClick={fetchPayments} className="refresh-button">
          Refresh
        </button>
        <span className="payment-count">{payments.length} payment(s)</span>
      </div>
      
      <table className="payment-table">
        <thead>
          <tr>
            <th>Payment ID</th>
            <th>Order ID</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Created At</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((payment) => (
            <tr key={payment.id}>
              <td className="payment-id">{payment.paymentId}</td>
              <td className="order-id">{payment.orderId}</td>
              <td className="amount">{formatAmount(payment.amount)}</td>
              <td className="status">
                <PaymentStatusBadge status={payment.paymentStatus} />
              </td>
              <td className="created-at">{formatDateTime(payment.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PaymentTable;
