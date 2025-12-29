import React, { useState } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, Mail, Clock, Search } from 'lucide-react';
import axios from 'axios';
import './EmailProcessing.css';

const EmailProcessing = ({ persistedState, onStateChange }) => {
  const [loading, setLoading] = useState(persistedState?.loading || false);
  const [result, setResult] = useState(persistedState?.result || null);
  const [error, setError] = useState(persistedState?.error || null);
  const [searchQuery, setSearchQuery] = useState('');

  // Helper to update both local and parent state
  const updateState = (newState) => {
    if (newState.result !== undefined) setResult(newState.result);
    if (newState.error !== undefined) setError(newState.error);
    if (newState.loading !== undefined) setLoading(newState.loading);
    onStateChange?.(prev => ({ ...prev, ...newState }));
  };

  const handleProcessCases = async () => {
    updateState({ loading: true, error: null, result: null });
    setSearchQuery('');

    try {
      const response = await axios.post('/api/email/process');
      updateState({ result: response.data, loading: false });
    } catch (err) {
      updateState({ 
        error: err.response?.data?.message || err.message || 'Failed to process email cases',
        loading: false 
      });
    }
  };

  // Filter documents based on search query
  const filterDocs = (docs) => {
    if (!searchQuery.trim() || !docs) return docs || [];
    const query = searchQuery.toLowerCase();
    return docs.filter(doc => doc?.toLowerCase().includes(query));
  };

  return (
    <div className="email-processing">
      <div className="monitoring-header">
        <h2 className="section-title">Email Resolution Processing</h2>
        <p className="section-description">
          Process email waiting cases and categorize them for completion, cancellation, or manual review
        </p>
      </div>

      <div className="action-card">
        <div className="action-card-content">
          <div className="action-info">
            <h3 className="action-title">Process Email Waiting Cases</h3>
            <p className="action-description">
              This will process all email resolution waiting cases, check their email category,
              and categorize them into Complete, Cancel, or Manual Review lists.
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
                <Mail size={24} />
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
                <p className="stat-label">To DV POST COMPLETE</p>
                <p className="stat-value">{result.documentNumbersToComplete?.length || 0}</p>
              </div>
            </div>

            <div className="stat-card stat-failed">
              <div className="stat-icon">
                <XCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">To Cancel</p>
                <p className="stat-value">{result.documentNumbersToCancel?.length || 0}</p>
              </div>
            </div>

            <div className="stat-card stat-review">
              <div className="stat-icon">
                <AlertCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Manual Review</p>
                <p className="stat-value">{result.documentNumbersForManualReview?.length || 0}</p>
              </div>
            </div>
          </div>

          {result.message && (
            <div className="alert alert-info">
              <AlertCircle size={20} />
              <span>{result.message}</span>
            </div>
          )}

          {/* Document Lists */}
          <div className="document-lists-section">
            <div className="details-header">
              <h4 className="details-title">Document Lists</h4>
              <div className="search-container">
                <Search className="search-icon" size={18} />
                <input
                  type="text"
                  className="search-input"
                  placeholder="Search document numbers..."
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

            <div className="document-lists-grid">
              {/* Documents to DV POST COMPLETE */}
              <div className="document-list-card complete-card">
                <div className="list-header complete-header">
                  <CheckCircle size={20} />
                  <h5>To DV POST COMPLETE ({result.documentNumbersToComplete?.length || 0})</h5>
                </div>
                <div className="document-list">
                  {filterDocs(result.documentNumbersToComplete).length > 0 ? (
                    filterDocs(result.documentNumbersToComplete).map((doc, idx) => (
                      <div key={idx} className="document-item complete-item">{doc}</div>
                    ))
                  ) : (
                    <div className="empty-list">
                      {searchQuery ? 'No matching documents' : 'No documents for DV POST COMPLETE'}
                    </div>
                  )}
                </div>
              </div>

              {/* Documents to Cancel */}
              <div className="document-list-card cancel-card">
                <div className="list-header cancel-header">
                  <XCircle size={20} />
                  <h5>To Cancel ({result.documentNumbersToCancel?.length || 0})</h5>
                </div>
                <div className="document-list">
                  {filterDocs(result.documentNumbersToCancel).length > 0 ? (
                    filterDocs(result.documentNumbersToCancel).map((doc, idx) => (
                      <div key={idx} className="document-item cancel-item">{doc}</div>
                    ))
                  ) : (
                    <div className="empty-list">
                      {searchQuery ? 'No matching documents' : 'No documents to cancel'}
                    </div>
                  )}
                </div>
              </div>

              {/* Documents for Manual Review */}
              <div className="document-list-card review-card">
                <div className="list-header review-header">
                  <AlertCircle size={20} />
                  <h5>Manual Review ({result.documentNumbersForManualReview?.length || 0})</h5>
                </div>
                <div className="document-list">
                  {filterDocs(result.documentNumbersForManualReview).length > 0 ? (
                    filterDocs(result.documentNumbersForManualReview).map((doc, idx) => (
                      <div key={idx} className="document-item review-item">{doc}</div>
                    ))
                  ) : (
                    <div className="empty-list">
                      {searchQuery ? 'No matching documents' : 'No manual review needed'}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmailProcessing;

