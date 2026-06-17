import React from 'react';
import './ValidationDetailsModal.css';

const ValidationDetailsModal = ({ validation, onClose }) => {
    const formatDateTime = (dateTime) => {
        if (!dateTime) return '-';
        const date = new Date(dateTime);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: true
        });
    };

    const getStatusClass = (status) => {
        switch (status) {
            case 'VALID':
                return 'status-valid';
            case 'INVALID':
                return 'status-invalid';
            default:
                return 'status-unknown';
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Validation Details</h2>
                    <button className="close-button" onClick={onClose}>
                        &times;
                    </button>
                </div>

                <div className="modal-body">
                    <div className="detail-row">
                        <span className="detail-label">Validation ID:</span>
                        <span className="detail-value">{validation.id}</span>
                    </div>

                    <div className="detail-row">
                        <span className="detail-label">Order ID:</span>
                        <span className="detail-value">{validation.orderId}</span>
                    </div>

                    <div className="detail-row">
                        <span className="detail-label">Customer ID:</span>
                        <span className="detail-value">
                            {validation.customerId || 'Not available'}
                        </span>
                    </div>

                    <div className="detail-row">
                        <span className="detail-label">Validation Status:</span>
                        <span className={`detail-value status-badge ${getStatusClass(validation.validationStatus)}`}>
                            {validation.validationStatus}
                        </span>
                    </div>

                    <div className="detail-row">
                        <span className="detail-label">Validation Message:</span>
                        <span className="detail-value message-value">
                            {validation.validationMessage || 'No message available'}
                        </span>
                    </div>

                    <div className="detail-row">
                        <span className="detail-label">Validated At:</span>
                        <span className="detail-value">{formatDateTime(validation.validatedAt)}</span>
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="close-modal-button" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ValidationDetailsModal;
