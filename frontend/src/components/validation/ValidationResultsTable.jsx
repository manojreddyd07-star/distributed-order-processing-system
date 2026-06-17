import React, { useState } from 'react';
import './ValidationResultsTable.css';
import ValidationDetailsModal from './ValidationDetailsModal';

const ValidationResultsTable = ({ validations, loading, selectedStatus, onStatusFilterChange }) => {
    const [selectedValidation, setSelectedValidation] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleRowClick = (validation) => {
        setSelectedValidation(validation);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedValidation(null);
    };

    const getStatusBadgeClass = (status) => {
        switch (status) {
            case 'VALID':
                return 'status-badge status-valid';
            case 'INVALID':
                return 'status-badge status-invalid';
            default:
                return 'status-badge status-unknown';
        }
    };

    const formatDateTime = (dateTime) => {
        if (!dateTime) return '-';
        const date = new Date(dateTime);
        return date.toLocaleString();
    };

    if (loading) {
        return (
            <div className="validation-table-container">
                <div className="loading-state">
                    <div className="spinner"></div>
                    <p>Loading validation results...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="validation-table-container">
            <div className="table-header">
                <h2>Validation Results</h2>
                <div className="filter-controls">
                    <label htmlFor="status-filter">Filter by Status:</label>
                    <select
                        id="status-filter"
                        value={selectedStatus}
                        onChange={(e) => onStatusFilterChange(e.target.value)}
                        className="status-filter"
                    >
                        <option value="ALL">All</option>
                        <option value="VALID">Valid</option>
                        <option value="INVALID">Invalid</option>
                    </select>
                </div>
            </div>

            {validations.length === 0 ? (
                <div className="empty-state">
                    <p>No validation results found</p>
                </div>
            ) : (
                <div className="table-wrapper">
                    <table className="validation-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Order ID</th>
                                <th>Customer ID</th>
                                <th>Status</th>
                                <th>Message</th>
                                <th>Validated At</th>
                            </tr>
                        </thead>
                        <tbody>
                            {validations.map((validation) => (
                                <tr
                                    key={validation.id}
                                    onClick={() => handleRowClick(validation)}
                                    className="table-row-clickable"
                                >
                                    <td>{validation.id}</td>
                                    <td>{validation.orderId}</td>
                                    <td>{validation.customerId || '-'}</td>
                                    <td>
                                        <span className={getStatusBadgeClass(validation.validationStatus)}>
                                            {validation.validationStatus}
                                        </span>
                                    </td>
                                    <td className="message-cell">
                                        {validation.validationMessage || '-'}
                                    </td>
                                    <td>{formatDateTime(validation.validatedAt)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {isModalOpen && selectedValidation && (
                <ValidationDetailsModal
                    validation={selectedValidation}
                    onClose={handleCloseModal}
                />
            )}
        </div>
    );
};

export default ValidationResultsTable;
