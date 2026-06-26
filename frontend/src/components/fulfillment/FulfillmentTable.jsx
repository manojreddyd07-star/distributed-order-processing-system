import React, { useState, useEffect } from 'react';
import FulfillmentStatusBadge from './FulfillmentStatusBadge';
import './FulfillmentTable.css';

const FulfillmentTable = ({ refreshTrigger }) => {
  const [fulfillments, setFulfillments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchFulfillments();
  }, [refreshTrigger]);

  const fetchFulfillments = async () => {
    setIsLoading(true);
    
    try {
      // Using mock data as requested
      const mockData = [
        {
          id: 1,
          fulfillmentId: 'FUL-001',
          orderId: 'ORD-001',
          status: 'COMPLETED',
          createdAt: new Date().toISOString()
        },
        {
          id: 2,
          fulfillmentId: 'FUL-002',
          orderId: 'ORD-002',
          status: 'PENDING',
          createdAt: new Date(Date.now() - 3600000).toISOString()
        },
        {
          id: 3,
          fulfillmentId: 'FUL-003',
          orderId: 'ORD-003',
          status: 'IN_PROGRESS',
          createdAt: new Date(Date.now() - 7200000).toISOString()
        },
        {
          id: 4,
          fulfillmentId: 'FUL-004',
          orderId: 'ORD-004',
          status: 'COMPLETED',
          createdAt: new Date(Date.now() - 10800000).toISOString()
        },
        {
          id: 5,
          fulfillmentId: 'FUL-005',
          orderId: 'ORD-005',
          status: 'SHIPPED',
          createdAt: new Date(Date.now() - 14400000).toISOString()
        },
        {
          id: 6,
          fulfillmentId: 'FUL-006',
          orderId: 'ORD-006',
          status: 'PENDING',
          createdAt: new Date(Date.now() - 18000000).toISOString()
        },
        {
          id: 7,
          fulfillmentId: 'FUL-007',
          orderId: 'ORD-007',
          status: 'DELIVERED',
          createdAt: new Date(Date.now() - 21600000).toISOString()
        },
        {
          id: 8,
          fulfillmentId: 'FUL-008',
          orderId: 'ORD-008',
          status: 'IN_PROGRESS',
          createdAt: new Date(Date.now() - 25200000).toISOString()
        }
      ];
      
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 500));
      setFulfillments(mockData);
    } catch (err) {
      console.error('Error loading fulfillments:', err);
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
      <div className="fulfillment-table-container">
        <div className="loading-message">Loading fulfillments...</div>
      </div>
    );
  }

  // Empty state
  if (fulfillments.length === 0) {
    return (
      <div className="fulfillment-table-container">
        <div className="empty-message">No fulfillments found</div>
      </div>
    );
  }

  // Main table view
  return (
    <div className="fulfillment-table-container">
      <div className="table-header">
        <h2>Fulfillment Records</h2>
        <div className="table-info">
          Total: <strong>{fulfillments.length}</strong> fulfillments
        </div>
      </div>
      
      <div className="table-wrapper">
        <table className="fulfillment-table">
          <thead>
            <tr>
              <th>Fulfillment ID</th>
              <th>Order ID</th>
              <th>Status</th>
              <th>Created Date</th>
            </tr>
          </thead>
          <tbody>
            {fulfillments.map((fulfillment) => (
              <tr key={fulfillment.id} className="fulfillment-row">
                <td className="fulfillment-id">{fulfillment.fulfillmentId}</td>
                <td className="order-id">{fulfillment.orderId}</td>
                <td>
                  <FulfillmentStatusBadge status={fulfillment.status} />
                </td>
                <td className="created-date">
                  {formatDateTime(fulfillment.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FulfillmentTable;
