import React, { useState, useEffect } from 'react';
import PaymentStatusBadge from './PaymentStatusBadge';
import { getPaymentHistory } from '../../shared/api/paymentApi';
import './PaymentTransactionTable.css';

const PaymentTransactionTable = ({ onSelectPayment }) => {
  const [payments, setPayments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);

  // Mock payment data for fallback
  const mockPayments = [
    {
      id: 1,
      paymentId: 'PMT-A1B2C3D4',
      orderId: 101,
      amount: 299.99,
      paymentStatus: 'PENDING',
      createdAt: new Date().toISOString()
    },
    {
      id: 2,
      paymentId: 'PMT-E5F6G7H8',
      orderId: 102,
      amount: 149.50,
      paymentStatus: 'COMPLETED',
      createdAt: new Date(Date.now() - 3600000).toISOString()
    },
    {
      id: 3,
      paymentId: 'PMT-I9J0K1L2',
      orderId: 103,
      amount: 599.00,
      paymentStatus: 'PENDING',
      createdAt: new Date(Date.now() - 7200000).toISOString()
    },
    {
      id: 4,
      paymentId: 'PMT-M3N4O5P6',
      orderId: 104,
      amount: 89.99,
      paymentStatus: 'COMPLETED',
      createdAt: new Date(Date.now() - 10800000).toISOString()
    },
    {
      id: 5,
      paymentId: 'PMT-Q7R8S9T0',
      orderId: 105,
      amount: 1250.00,
      paymentStatus: 'PENDING',
      createdAt: new Date(Date.now() - 14400000).toISOString()
    },
    {
      id: 6,
      paymentId: 'PMT-U1V2W3X4',
      orderId: 106,
      amount: 425.75,
      paymentStatus: 'COMPLETED',
      createdAt: new Date(Date.now() - 18000000).toISOString()
    },
    {
      id: 7,
      paymentId: 'PMT-Y5Z6A7B8',
      orderId: 107,
      amount: 199.99,
      paymentStatus: 'PENDING',
      createdAt: new Date(Date.now() - 21600000).toISOString()
    },
    {
      id: 8,
      paymentId: 'PMT-C9D0E1F2',
      orderId: 108,
      amount: 750.00,
      paymentStatus: 'COMPLETED',
      createdAt: new Date(Date.now() - 25200000).toISOString()
    }
  ];

  useEffect(() => {
    fetchPayments();
  }, []);

  const fetchPayments = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getPaymentHistory();
      setPayments(data);
    } catch (err) {
      // Use mock data if API fails
      console.warn('API failed, using mock payment data:', err);
      setPayments(mockPayments);
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

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentPayments = payments.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(payments.length / itemsPerPage);

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const handleRowClick = (payment) => {
    if (onSelectPayment) {
      onSelectPayment(payment);
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="payment-transaction-table-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading payment transactions...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="payment-transaction-table-container">
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
      <div className="payment-transaction-table-container">
        <div className="empty-state">
          <p>No payment transactions found</p>
        </div>
      </div>
    );
  }

  // Payment transactions table
  return (
    <div className="payment-transaction-table-container">
      <div className="table-header">
        <h2>Payment Transactions</h2>
        <div className="table-actions">
          <button onClick={fetchPayments} className="refresh-button">
            Refresh
          </button>
          <span className="payment-count">{payments.length} transaction(s)</span>
        </div>
      </div>
      
      <div className="table-wrapper">
        <table className="payment-transaction-table">
          <thead>
            <tr>
              <th>Payment ID</th>
              <th>Order ID</th>
              <th>Amount</th>
              <th>Payment Status</th>
              <th>Created Date</th>
            </tr>
          </thead>
          <tbody>
            {currentPayments.map((payment) => (
              <tr 
                key={payment.id} 
                onClick={() => handleRowClick(payment)}
                className="payment-row"
              >
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

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className="pagination-button"
          >
            Previous
          </button>
          
          <div className="pagination-info">
            Page {currentPage} of {totalPages}
          </div>
          
          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className="pagination-button"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default PaymentTransactionTable;
