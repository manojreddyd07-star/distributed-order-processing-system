import React from 'react';
import './OrderRow.css';

const OrderRow = React.memo(({ order }) => {
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'PROCESSING':
        return 'status-processing';
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      default:
        return '';
    }
  };

  return (
    <tr className="order-row">
      <td className="order-id">#{order.id}</td>
      <td>{order.customerId}</td>
      <td>
        <span className={`status-badge ${getStatusClass(order.orderStatus)}`}>
          {order.orderStatus}
        </span>
      </td>
      <td className="total-amount">{formatCurrency(order.totalAmount)}</td>
      <td className="created-at">{formatDateTime(order.createdAt)}</td>
    </tr>
  );
});

OrderRow.displayName = 'OrderRow';

export default OrderRow;
