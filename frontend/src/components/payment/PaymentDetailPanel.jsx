import React from 'react';
import PaymentStatusBadge from './PaymentStatusBadge';
import './PaymentDetailPanel.css';

const PaymentDetailPanel = ({ payment }) => {
  // If no payment is selected, show a message
  if (!payment) {
    return (
      <div className="payment-detail-panel">
        <div className="no-selection">
          <p>Select a payment transaction to view details</p>
        </div>
      </div>
    );
  }

  // Format date/time for display
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  // Format amount as currency
  const formatAmount = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  };

  return (
    <div className="payment-detail-panel">
      <div className="panel-header">
        <h2>Payment Details</h2>
        <PaymentStatusBadge status={payment.paymentStatus} />
      </div>

      <div className="panel-content">
        {/* Payment ID */}
        <div className="detail-row">
          <div className="detail-label">Payment ID</div>
          <div className="detail-value payment-id-value">{payment.paymentId}</div>
        </div>

        {/* Order ID */}
        <div className="detail-row">
          <div className="detail-label">Order ID</div>
          <div className="detail-value order-id-value">{payment.orderId}</div>
        </div>

        {/* Amount */}
        <div className="detail-row">
          <div className="detail-label">Amount</div>
          <div className="detail-value amount-value">{formatAmount(payment.amount)}</div>
        </div>

        {/* Payment Status */}
        <div className="detail-row">
          <div className="detail-label">Payment Status</div>
          <div className="detail-value">
            <span className="status-text">{payment.paymentStatus}</span>
          </div>
        </div>

        {/* Created Date */}
        <div className="detail-row">
          <div className="detail-label">Created Date</div>
          <div className="detail-value created-date-value">
            {formatDateTime(payment.createdAt)}
          </div>
        </div>

        {/* Additional Information Section */}
        <div className="additional-info">
          <h3>Additional Information</h3>
          <div className="info-grid">
            <div className="info-item">
              <span className="info-label">Transaction Type</span>
              <span className="info-value">Payment</span>
            </div>
            <div className="info-item">
              <span className="info-label">Processing Method</span>
              <span className="info-value">Automated</span>
            </div>
            <div className="info-item">
              <span className="info-label">Database ID</span>
              <span className="info-value">{payment.id}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PaymentDetailPanel;
