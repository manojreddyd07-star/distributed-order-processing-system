import React, { useState } from 'react';
import './SearchFilters.css';

const SearchFilters = ({ onSearch, onReset }) => {
  const [filters, setFilters] = useState({
    customerId: '',
    orderStatus: '',
    startDate: '',
    endDate: '',
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    
    // Build search params with only non-empty values
    const searchParams = {};
    
    if (filters.customerId) {
      searchParams.customerId = filters.customerId;
    }
    if (filters.orderStatus) {
      searchParams.orderStatus = filters.orderStatus;
    }
    if (filters.startDate) {
      searchParams.startDate = new Date(filters.startDate).toISOString();
    }
    if (filters.endDate) {
      searchParams.endDate = new Date(filters.endDate).toISOString();
    }
    
    searchParams.sortBy = filters.sortBy;
    searchParams.sortDirection = filters.sortDirection;
    
    onSearch(searchParams);
  };

  const handleReset = () => {
    const resetFilters = {
      customerId: '',
      orderStatus: '',
      startDate: '',
      endDate: '',
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    };
    setFilters(resetFilters);
    onReset();
  };

  return (
    <div className="search-filters">
      <form onSubmit={handleSearch} className="filters-form">
        <div className="filters-row">
          <div className="filter-group">
            <label htmlFor="customerId">Customer ID</label>
            <input
              type="number"
              id="customerId"
              name="customerId"
              value={filters.customerId}
              onChange={handleInputChange}
              placeholder="Enter customer ID"
            />
          </div>

          <div className="filter-group">
            <label htmlFor="orderStatus">Order Status</label>
            <select
              id="orderStatus"
              name="orderStatus"
              value={filters.orderStatus}
              onChange={handleInputChange}
            >
              <option value="">All Statuses</option>
              <option value="PENDING">PENDING</option>
              <option value="VALIDATED">VALIDATED</option>
              <option value="INVENTORY_RESERVED">INVENTORY_RESERVED</option>
              <option value="PAYMENT_COMPLETED">PAYMENT_COMPLETED</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="startDate">Start Date</label>
            <input
              type="datetime-local"
              id="startDate"
              name="startDate"
              value={filters.startDate}
              onChange={handleInputChange}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="endDate">End Date</label>
            <input
              type="datetime-local"
              id="endDate"
              name="endDate"
              value={filters.endDate}
              onChange={handleInputChange}
            />
          </div>
        </div>

        <div className="filters-row">
          <div className="filter-group">
            <label htmlFor="sortBy">Sort By</label>
            <select
              id="sortBy"
              name="sortBy"
              value={filters.sortBy}
              onChange={handleInputChange}
            >
              <option value="createdAt">Created At</option>
              <option value="customerId">Customer ID</option>
              <option value="orderStatus">Order Status</option>
              <option value="totalAmount">Total Amount</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="sortDirection">Sort Direction</label>
            <select
              id="sortDirection"
              name="sortDirection"
              value={filters.sortDirection}
              onChange={handleInputChange}
            >
              <option value="DESC">Descending</option>
              <option value="ASC">Ascending</option>
            </select>
          </div>

          <div className="filter-actions">
            <button type="submit" className="btn-search">
              Search
            </button>
            <button type="button" className="btn-reset" onClick={handleReset}>
              Reset
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default SearchFilters;
