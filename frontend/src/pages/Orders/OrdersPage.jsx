import React, { useState, useEffect, useCallback, useMemo } from 'react';
import OrderTable from '../../components/orders/OrderTable';
import OrderEventTimeline from '../../components/orders/OrderEventTimeline';
import SearchFilters from '../../components/orders/SearchFilters';
import Pagination from '../../components/orders/Pagination';
import { searchOrders } from '../../services/orderApi';
import './OrdersPage.css';

const OrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchParams, setSearchParams] = useState({});

  // Memoize the fetch function to avoid recreating it on every render
  const fetchOrders = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const params = {
        ...searchParams,
        page: currentPage,
        size: pageSize
      };
      
      const response = await searchOrders(params);
      
      setOrders(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err.message || 'Failed to load orders');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, searchParams]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Memoize callbacks to prevent unnecessary re-renders
  const handleSearch = useCallback((filters) => {
    setSearchParams(filters);
    setCurrentPage(0); // Reset to first page on new search
  }, []);

  const handleReset = useCallback(() => {
    setSearchParams({});
    setCurrentPage(0);
  }, []);

  const handlePageChange = useCallback((newPage) => {
    setCurrentPage(newPage);
  }, []);

  const handlePageSizeChange = useCallback((newSize) => {
    setPageSize(newSize);
    setCurrentPage(0); // Reset to first page when changing page size
  }, []);

  // Memoize the content to render
  const content = useMemo(() => {
    if (isLoading) {
      return <div className="loading-message">Loading orders...</div>;
    }
    
    if (error) {
      return (
        <div className="error-message">
          <p>{error}</p>
          <button onClick={fetchOrders} className="retry-button">
            Retry
          </button>
        </div>
      );
    }
    
    if (orders.length === 0) {
      return <div className="empty-message">No orders found</div>;
    }
    
    return (
      <>
        <OrderTable orders={orders} />
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={handlePageChange}
          onPageSizeChange={handlePageSizeChange}
        />
      </>
    );
  }, [isLoading, error, orders, currentPage, totalPages, totalElements, pageSize, handlePageChange, handlePageSizeChange, fetchOrders]);

  return (
    <div className="orders-page">
      <div className="orders-page-header">
        <h1>Orders</h1>
        <p>Manage and view all orders in the system</p>
      </div>

      <SearchFilters onSearch={handleSearch} onReset={handleReset} />

      {content}

      <OrderEventTimeline />
    </div>
  );
};

export default OrdersPage;
