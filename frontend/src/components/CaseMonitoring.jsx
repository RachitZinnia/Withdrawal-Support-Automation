import React, { useState } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, Clock, Search } from 'lucide-react';
import axios from 'axios';
import './CaseMonitoring.css';

const CaseMonitoring = () => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const handleProcessCases = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    setSearchQuery(''); // Reset search when processing new cases

    try {
      const response = await axios.post('/api/cases/process-dataentry-waiting');
      setResult(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to process cases');
    } finally {
      setLoading(false);
    }
  };

  // Filter case details based on search query
  const filteredDetails = result?.details?.filter(detail => {
    if (!searchQuery.trim()) return true;
    
    const query = searchQuery.toLowerCase();
    return (
      detail.caseReference?.toLowerCase().includes(query) ||
      detail.caseId?.toLowerCase().includes(query) ||
      detail.documentNumber?.toLowerCase().includes(query) ||
      detail.status?.toLowerCase().includes(query) ||
      detail.action?.toLowerCase().includes(query) ||
      detail.message?.toLowerCase().includes(query) ||
      detail.reviewReason?.toLowerCase().includes(query)
    );
  }) || [];

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

          {/* Document Action Lists */}
          {(result.documentNumbersToCancel?.length > 0 || 
            result.documentNumbersToReturning?.length > 0 ||
            result.documentNumbersToComplete?.length > 0 ||
            result.documentNumbersForManualReview?.length > 0) && (
            <div className="action-lists-section">
              <h4 className="details-title">Document Action Lists</h4>
              
              <div className="action-lists-grid">
                {/* Documents to Cancel */}
                {result.documentNumbersToCancel && result.documentNumbersToCancel.length > 0 && (
                  <div className="action-list-card">
                    <div className="action-list-header cancel-header">
                      <XCircle size={20} />
                      <h5>Documents to Cancel ({result.documentNumbersToCancel.length})</h5>
                    </div>
                    <div className="action-list-content">
                      <ul className="document-list">
                        {result.documentNumbersToCancel.map((docNum, index) => (
                          <li key={index} className="document-item">{docNum}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}

                {/* Documents to Send to Returning */}
                {result.documentNumbersToReturning && result.documentNumbersToReturning.length > 0 && (
                  <div className="action-list-card">
                    <div className="action-list-header returning-header">
                      <RefreshCw size={20} />
                      <h5>Documents to Send to Returning ({result.documentNumbersToReturning.length})</h5>
                    </div>
                    <div className="action-list-content">
                      <ul className="document-list">
                        {result.documentNumbersToReturning.map((docNum, index) => (
                          <li key={index} className="document-item">{docNum}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}

                {/* Documents to Send to Complete */}
                {result.documentNumbersToComplete && result.documentNumbersToComplete.length > 0 && (
                  <div className="action-list-card">
                    <div className="action-list-header complete-header">
                      <CheckCircle size={20} />
                      <h5>Documents to Send to Complete ({result.documentNumbersToComplete.length})</h5>
                    </div>
                    <div className="action-list-content">
                      <ul className="document-list">
                        {result.documentNumbersToComplete.map((docNum, index) => (
                          <li key={index} className="document-item">{docNum}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}

                {/* Documents for Manual Review */}
                {result.documentNumbersForManualReview && result.documentNumbersForManualReview.length > 0 && (
                  <div className="action-list-card">
                    <div className="action-list-header review-header">
                      <AlertCircle size={20} />
                      <h5>Documents for Manual Review ({result.documentNumbersForManualReview.length})</h5>
                    </div>
                    <div className="action-list-content">
                      <ul className="document-list">
                        {result.documentNumbersForManualReview.map((docNum, index) => (
                          <li key={index} className="document-item">{docNum}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {result.details && result.details.length > 0 && (
            <div className="details-section">
              <div className="details-header">
                <h4 className="details-title">Case Details</h4>
                <div className="search-container">
                  <Search className="search-icon" size={18} />
                  <input
                    type="text"
                    className="search-input"
                    placeholder="Search by case reference, ID, document number, status, action, or message..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                  {searchQuery && (
                    <button 
                      className="clear-search"
                      onClick={() => setSearchQuery('')}
                      aria-label="Clear search"
                    >
                      <XCircle size={16} />
                    </button>
                  )}
                </div>
              </div>

              {filteredDetails.length > 0 ? (
                <>
                  <div className="search-results-info">
                    Showing {filteredDetails.length} of {result.details.length} cases
                  </div>
                  <div className="table-container">
                    <table className="details-table">
                      <thead>
                        <tr>
                          <th>Case Reference</th>
                          <th>Case ID</th>
                          <th>Document Number</th>
                          <th>Status</th>
                          <th>Action</th>
                          <th>Message</th>
                          <th>Manual Review</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredDetails.map((detail, index) => (
                          <tr key={index}>
                            <td className="case-ref">{detail.caseReference || '-'}</td>
                            <td className="case-id">{detail.caseId || '-'}</td>
                            <td className="document-number">{detail.documentNumber || '-'}</td>
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
                </>
              ) : (
                <div className="no-results">
                  <AlertCircle size={24} />
                  <p>No cases found matching "{searchQuery}"</p>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CaseMonitoring;





