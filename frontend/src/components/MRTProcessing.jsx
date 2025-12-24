import React, { useState, useEffect } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, Clock, Search } from 'lucide-react';
import axios from 'axios';
import './MRTProcessing.css';

const MRTProcessing = ({ persistedState, onStateChange }) => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(persistedState?.result || null);
  const [error, setError] = useState(persistedState?.error || null);
  const [searchQuery, setSearchQuery] = useState('');

  // Update parent state when result or error changes
  useEffect(() => {
    onStateChange?.({ result, error });
  }, [result, error]);

  const handleProcessCases = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    setSearchQuery('');

    try {
      const response = await axios.post('/api/mrt/process');
      setResult(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to process MRT cases');
    } finally {
      setLoading(false);
    }
  };

  // Filter documents based on search query
  const filterDocs = (docs) => {
    if (!searchQuery.trim() || !docs) return docs || [];
    const query = searchQuery.toLowerCase();
    return docs.filter(doc => doc?.toLowerCase().includes(query));
  };

  const filteredDocs = filterDocs(result?.casesWithCompleteTasksAndEventList);

  return (
    <div className="mrt-processing">
      <div className="monitoring-header">
        <h2 className="section-title">MRT & GIACT Processing</h2>
        <p className="section-description">
          Process MRT waiting cases and identify cases with complete tasks and event received
        </p>
      </div>

      <div className="action-card">
        <div className="action-card-content">
          <div className="action-info">
            <h3 className="action-title">Process MRT Waiting Cases</h3>
            <p className="action-description">
              This will process all MRT waiting cases across different scenarios including 
              Call Out Manual Review, External PI Exception Approval, PI Management Approval, 
              and MRT Manual Review. Cases with complete tasks and event received will be collected.
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
                <p className="stat-label">Total Cases Processed</p>
                <p className="stat-value">{result.totalCasesProcessed}</p>
              </div>
            </div>

            <div className="stat-card stat-success">
              <div className="stat-icon">
                <CheckCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Complete Tasks & Event</p>
                <p className="stat-value">{result.casesWithCompleteTasksAndEvent}</p>
              </div>
            </div>
          </div>

          {result.message && (
            <div className="alert alert-info">
              <AlertCircle size={20} />
              <span>{result.message}</span>
            </div>
          )}

          {/* Cases with Complete Tasks & Event Received */}
          <div className="all-cases-section">
            <div className="details-header">
              <h4 className="details-title">
                Cases with Complete Tasks & Event Received ({result.casesWithCompleteTasksAndEventList?.length || 0})
              </h4>
              {result.casesWithCompleteTasksAndEventList && result.casesWithCompleteTasksAndEventList.length > 0 && (
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
              )}
            </div>

            {result.casesWithCompleteTasksAndEventList && result.casesWithCompleteTasksAndEventList.length > 0 ? (
              <>
                {searchQuery && (
                  <div className="search-results-info">
                    Showing {filteredDocs.length} of {result.casesWithCompleteTasksAndEventList.length} documents
                  </div>
                )}
                {filteredDocs.length > 0 ? (
                  <div className="all-docs-grid">
                    {filteredDocs.map((doc, idx) => (
                      <div key={idx} className="all-doc-item">
                        <CheckCircle size={16} className="doc-check-icon" />
                        <span>{doc}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="no-results">
                    <AlertCircle size={24} />
                    <p>No documents found matching "{searchQuery}"</p>
                  </div>
                )}
              </>
            ) : (
              <div className="empty-cases">
                <AlertCircle size={32} />
                <p>No cases with complete tasks and event received</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default MRTProcessing;

