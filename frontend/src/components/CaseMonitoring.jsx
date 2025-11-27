import React, { useState } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, Clock } from 'lucide-react';
import axios from 'axios';
import './CaseMonitoring.css';

const CaseMonitoring = () => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleProcessCases = async () => {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await axios.post('/api/cases/process-dataentry-waiting');
      setResult(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to process cases');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="case-monitoring">
      <div className="monitoring-header">
        <h2 className="section-title">Data Entry Waiting Cases</h2>
        <p className="section-description">
          Monitor and process cases waiting for data entry validation
        </p>
      </div>

      <div className="action-card">
        <div className="action-card-content">
          <div className="action-info">
            <h3 className="action-title">Process Waiting Cases</h3>
            <p className="action-description">
              This will process all data entry waiting cases, check their status in OnBase,
              verify MongoDB records, and flag cases requiring manual review.
            </p>
          </div>
          <button
            className={`process-button ${loading ? 'loading' : ''}`}
            onClick={handleProcessCases}
            disabled={loading}
          >
            {loading ? (
              <>
                <RefreshCw className="button-icon spinning" size={20} />
                Processing...
              </>
            ) : (
              <>
                <Play className="button-icon" size={20} />
                Process Cases
              </>
            )}
          </button>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          <XCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {result && (
        <div className="results-section">
          <h3 className="results-title">Processing Results</h3>
          
          <div className="stats-grid">
            <div className="stat-card stat-total">
              <div className="stat-icon">
                <Clock size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Total Cases</p>
                <p className="stat-value">{result.totalCases}</p>
              </div>
            </div>

            <div className="stat-card stat-success">
              <div className="stat-icon">
                <CheckCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Successful</p>
                <p className="stat-value">{result.successfulCases}</p>
              </div>
            </div>

            <div className="stat-card stat-failed">
              <div className="stat-icon">
                <XCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Failed</p>
                <p className="stat-value">{result.failedCases}</p>
              </div>
            </div>

            <div className="stat-card stat-review">
              <div className="stat-icon">
                <AlertCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Manual Review</p>
                <p className="stat-value">{result.manualReviewRequired}</p>
              </div>
            </div>
          </div>

          {result.message && (
            <div className="alert alert-info">
              <AlertCircle size={20} />
              <span>{result.message}</span>
            </div>
          )}

          {result.details && result.details.length > 0 && (
            <div className="details-section">
              <h4 className="details-title">Case Details</h4>
              <div className="table-container">
                <table className="details-table">
                  <thead>
                    <tr>
                      <th>Case Reference</th>
                      <th>Case ID</th>
                      <th>Status</th>
                      <th>Action</th>
                      <th>Message</th>
                      <th>Manual Review</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.details.map((detail, index) => (
                      <tr key={index}>
                        <td className="case-ref">{detail.caseReference || '-'}</td>
                        <td className="case-id">{detail.caseId || '-'}</td>
                        <td>
                          <span className={`status-badge status-${detail.status?.toLowerCase()}`}>
                            {detail.status}
                          </span>
                        </td>
                        <td className="action-cell">{detail.action || '-'}</td>
                        <td className="message-cell">{detail.message || '-'}</td>
                        <td className="text-center">
                          {detail.requiresManualReview ? (
                            <span className="review-badge">
                              <AlertCircle size={16} />
                              {detail.reviewReason || 'Required'}
                            </span>
                          ) : (
                            <span className="no-review">No</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CaseMonitoring;





