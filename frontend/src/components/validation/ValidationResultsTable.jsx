import React, { useState, useEffect } from 'react';
import './ValidationResultsTable.css';
import ValidationDetailsModal from './ValidationDetailsModal';
import { getValidationHistory } from '../../services/validationApi';

const ValidationResultsTable = () => {
    const [validations, setValidations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedStatus, setSelectedStatus] = useState('ALL');
    const [selectedValidation, setSelectedValidation] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    useEffect(() => {
        fetchValidations();
    }, []);

    const fetchValidations = async () => {
        setLoading(true);
        setError(null);
        
        try {
            const data = await getValidationHistory();
            setValidations(data);
        } catch (err) {
            setError(err.message || 'Failed to load validation history');
            console.error('Error fetching validation history:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleStatusFilterChange = (status) => {
        setSelectedStatus(status);
    };

    const handleRefresh = () => {
        fetchValidations();
    };

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

    // Filter validations based on selected status
    const filteredValidations = selectedStatus === 'ALL'
        ? validations
        : validations.filter(v => v.validationStatus === selectedStatus);

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

    if (error) {
        return (
            <div className="validation-table-container">
                <div className="error-state">
                    <p className="error-message">{error}</p>
                    <button onClick={handleRefresh} className="retry-button">
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="validation-table-container">
            <div className="table-header">
                <h2>Validation Results</h2>
                <div className="header-controls">
                    <div className="filter-controls">
                        <label htmlFor="status-filter">Filter by Status:</label>
                        <select
                            id="status-filter"
                            value={selectedStatus}
                            onChange={(e) => handleStatusFilterChange(e.target.value)}
                            className="status-filter"
                        >
                            <option value="ALL">All</option>
                            <option value="VALID">Valid</option>
                            <option value="INVALID">Invalid</option>
                        </select>
                    </div>
                    <button onClick={handleRefresh} className="refresh-button">
                        Refresh
                    </button>
                </div>
            </div>

            {filteredValidations.length === 0 ? (
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
                                <th>Status</th>
                                <th>Message</th>
                                <th>Validated At</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredValidations.map((validation) => (
                                <tr
                                    key={validation.id}
                                    onClick={() => handleRowClick(validation)}
                                    className="table-row-clickable"
                                >
                                    <td>{validation.id}</td>
                                    <td>{validation.orderId}</td>
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
