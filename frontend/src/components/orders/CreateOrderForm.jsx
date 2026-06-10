import React, { useState } from 'react';
import './CreateOrderForm.css';

const CreateOrderForm = () => {
  // useState for Customer ID
  const [customerId, setCustomerId] = useState('');
  
  // useState for Total Amount
  const [totalAmount, setTotalAmount] = useState('');
  
  // useState for Loading State
  const [isLoading, setIsLoading] = useState(false);
  
  // useState for Success Message
  const [successMessage, setSuccessMessage] = useState('');
  
  // useState for error messages
  const [errors, setErrors] = useState({});
  
  // Validate form fields
  const validateForm = () => {
    const newErrors = {};
    
    // Required field validation for Customer ID
    if (!customerId || customerId.trim() === '') {
      newErrors.customerId = 'Customer ID is required';
    }
    
    // Required field validation for Total Amount
    if (!totalAmount || totalAmount.trim() === '') {
      newErrors.totalAmount = 'Total Amount is required';
    } 
    // Amount greater than zero validation
    else if (parseFloat(totalAmount) <= 0) {
      newErrors.totalAmount = 'Total Amount must be greater than zero';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };
  
  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Clear previous messages
    setSuccessMessage('');
    
    // Validate form
    if (!validateForm()) {
      return;
    }
    
    // Set loading state
    setIsLoading(true);
    
    try {
      // TODO: API call will be implemented later
      // Simulating API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Show success notification
      setSuccessMessage('Order created successfully!');
      
      // Reset form
      setCustomerId('');
      setTotalAmount('');
      setErrors({});
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccessMessage('');
      }, 3000);
      
    } catch (error) {
      setErrors({ submit: 'Failed to create order. Please try again.' });
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="create-order-form-container">
      <h2>Create New Order</h2>
      
      <form onSubmit={handleSubmit} className="create-order-form">
        {/* Customer ID Form Field */}
        <div className="form-group">
          <label htmlFor="customerId">
            Customer ID <span className="required">*</span>
          </label>
          <input
            type="number"
            id="customerId"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            className={errors.customerId ? 'error' : ''}
            disabled={isLoading}
            placeholder="Enter customer ID"
          />
          {errors.customerId && (
            <span className="error-message">{errors.customerId}</span>
          )}
        </div>
        
        {/* Total Amount Form Field */}
        <div className="form-group">
          <label htmlFor="totalAmount">
            Total Amount <span className="required">*</span>
          </label>
          <input
            type="number"
            id="totalAmount"
            step="0.01"
            value={totalAmount}
            onChange={(e) => setTotalAmount(e.target.value)}
            className={errors.totalAmount ? 'error' : ''}
            disabled={isLoading}
            placeholder="Enter total amount"
          />
          {errors.totalAmount && (
            <span className="error-message">{errors.totalAmount}</span>
          )}
        </div>
        
        {/* Submit Error */}
        {errors.submit && (
          <div className="error-message submit-error">{errors.submit}</div>
        )}
        
        {/* Submit Button */}
        <button 
          type="submit" 
          className="submit-button"
          disabled={isLoading}
        >
          {/* Loading Spinner Placeholder */}
          {isLoading ? (
            <span className="loading-spinner">
              <span className="spinner"></span>
              Creating...
            </span>
          ) : (
            'Create Order'
          )}
        </button>
        
        {/* Success Notification Placeholder */}
        {successMessage && (
          <div className="success-notification">
            {successMessage}
          </div>
        )}
      </form>
    </div>
  );
};

export default CreateOrderForm;
