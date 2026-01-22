import React, { useState } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, ArrowRight, RotateCcw, CheckSquare, FilePlus, FileText, FileCheck } from 'lucide-react';
import axios from 'axios';
import './MoveCaseStatus.css';

const MoveCaseStatus = ({ persistedState, onStateChange }) => {
  const [loading, setLoading] = useState(persistedState?.loading || false);
  const [result, setResult] = useState(persistedState?.result || null);
  const [error, setError] = useState(persistedState?.error || null);
  const [selectedAction, setSelectedAction] = useState(persistedState?.selectedAction || null);
  const [documentNumbers, setDocumentNumbers] = useState(persistedState?.documentNumbers || '');

  // Action options configuration
  const actionOptions = [
    {
      id: 'closeBpmFollowUp',
      title: 'Close BPM Follow Up Task for Documents',
      description: 'Close the BPM follow-up task associated with the provided document numbers',
      icon: CheckSquare,
      color: 'primary',
      endpoint: '/api/case/status/close/followup'
    },
    {
      id: 'moveToCpReturning',
      title: 'Move Case to CP Returning',
      description: 'Move the case status to CP Returning for the provided document numbers',
      icon: RotateCcw,
      color: 'warning',
      endpoint: '/api/case/status/move/returning'
    }
  ];

  // Helper to update both local and parent state
  const updateState = (newState) => {
    if (newState.result !== undefined) setResult(newState.result);
    if (newState.error !== undefined) setError(newState.error);
    if (newState.loading !== undefined) setLoading(newState.loading);
    if (newState.selectedAction !== undefined) setSelectedAction(newState.selectedAction);
    if (newState.documentNumbers !== undefined) setDocumentNumbers(newState.documentNumbers);
    onStateChange?.(prev => ({ ...prev, ...newState }));
  };

  const handleActionSelect = (action) => {
    updateState({ 
      selectedAction: action, 
      error: null, 
      result: null 
    });
  };

  const handleDocumentNumbersChange = (e) => {
    const value = e.target.value;
    setDocumentNumbers(value);
    onStateChange?.(prev => ({ ...prev, documentNumbers: value }));
  };

  const parseDocumentNumbers = () => {
    return documentNumbers
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0);
  };

  const handleSubmit = async () => {
    if (!selectedAction) {
      updateState({ error: 'Please select an action first' });
      return;
    }

    const docNumbers = parseDocumentNumbers();
    if (docNumbers.length === 0) {
      updateState({ error: 'Please enter at least one document number' });
      return;
    }

    updateState({ loading: true, error: null, result: null });

    try {
      const response = await axios.post(selectedAction.endpoint, {
        documentNumbers: docNumbers
      });
      updateState({ result: response.data, loading: false });
    } catch (err) {
      updateState({ 
        error: err.response?.data?.message || err.message || `Failed to ${selectedAction.title.toLowerCase()}`,
        loading: false 
      });
    }
  };

  const handleReset = () => {
    updateState({
      selectedAction: null,
      documentNumbers: '',
      result: null,
      error: null
    });
  };

  const getActionIcon = (action) => {
    const Icon = action.icon;
    return <Icon size={24} />;
  };

  return (
    <div className="move-case-status">
      <div className="monitoring-header">
        <h2 className="section-title">Move Case Status</h2>
        <p className="section-description">
          Select an action and provide document numbers to update case statuses in bulk
        </p>
      </div>

      {/* Action Options */}
      <div className="action-options-section">
        <h3 className="action-options-title">Select Action</h3>
        <div className="action-options-grid">
          {actionOptions.map((action) => (
            <div
              key={action.id}
              className={`action-option-card ${selectedAction?.id === action.id ? 'selected' : ''} action-${action.color}`}
              onClick={() => handleActionSelect(action)}
            >
              <div className={`action-option-icon icon-${action.color}`}>
                {getActionIcon(action)}
              </div>
              <div className="action-option-content">
                <h4 className="action-option-title">{action.title}</h4>
                <p className="action-option-description">{action.description}</p>
              </div>
              {selectedAction?.id === action.id && (
                <div className="selected-indicator">
                  <CheckCircle size={20} />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Document Numbers Input */}
      {selectedAction && (
        <div className="document-input-section">
          <div className="input-card">
            <div className="input-header">
              <div className="input-title-wrapper">
                <ArrowRight size={20} className="input-title-icon" />
                <h3 className="input-title">Enter Document Numbers</h3>
              </div>
              <span className="selected-action-badge">
                {selectedAction.title}
              </span>
            </div>
            <p className="input-description">
              Enter document numbers below, one per line. Example format: 20260105-F-735042
            </p>
            <textarea
              className="document-textarea"
              placeholder={`Enter document numbers here, one per line:\n20260105-F-735042\n20260106-1-794062\n...`}
              value={documentNumbers}
              onChange={handleDocumentNumbersChange}
              rows={10}
              disabled={loading}
            />
            <div className="input-footer">
              <span className="document-count">
                {parseDocumentNumbers().length} document(s) entered
              </span>
              <div className="input-actions">
                <button
                  className="reset-button"
                  onClick={handleReset}
                  disabled={loading}
                >
                  Reset
                </button>
                <button
                  className={`submit-button ${loading ? 'loading' : ''}`}
                  onClick={handleSubmit}
                  disabled={loading || parseDocumentNumbers().length === 0}
                >
                  {loading ? (
                    <>
                      <RefreshCw className="button-icon spinning" size={20} />
                      Processing...
                    </>
                  ) : (
                    <>
                      <Play className="button-icon" size={20} />
                      Submit
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="alert alert-error">
          <XCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Results Section */}
      {result && (
        <div className="results-section">
          <h3 className="results-title">Processing Results</h3>
          
          <div className="stats-grid">
            <div className="stat-card stat-total">
              <div className="stat-icon">
                <FileCheck size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Total Submitted</p>
                <p className="stat-value">{result.totalSubmitted || parseDocumentNumbers().length}</p>
              </div>
            </div>

            <div className="stat-card stat-success">
              <div className="stat-icon">
                <CheckCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Successful</p>
                <p className="stat-value">{result.successCount || 0}</p>
              </div>
            </div>

            <div className="stat-card stat-failed">
              <div className="stat-icon">
                <XCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Failed</p>
                <p className="stat-value">{result.failedCount || 0}</p>
              </div>
            </div>

            <div className="stat-card stat-osc">
              <div className="stat-icon">
                <FilePlus size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Create OSC</p>
                <p className="stat-value">{result.createOscCount || 0}</p>
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
          {(result.successfulDocuments?.length > 0 || result.failedDocuments?.length > 0 || result.createOscDocuments?.length > 0) && (
            <div className="result-lists-section">
              <div className="result-lists-grid three-columns">
                {/* Successful Documents */}
                {result.successfulDocuments?.length > 0 && (
                  <div className="result-list-card success-card">
                    <div className="list-header success-header">
                      <CheckCircle size={20} />
                      <h5>Successful ({result.successfulDocuments.length})</h5>
                    </div>
                    <div className="result-list">
                      {result.successfulDocuments.map((doc, idx) => (
                        <div key={idx} className="result-item success-item">
                          {typeof doc === 'object' ? doc.documentNumber : doc}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Failed Documents */}
                {result.failedDocuments?.length > 0 && (
                  <div className="result-list-card failed-card">
                    <div className="list-header failed-header">
                      <XCircle size={20} />
                      <h5>Failed ({result.failedDocuments.length})</h5>
                    </div>
                    <div className="result-list">
                      {result.failedDocuments.map((doc, idx) => (
                        <div key={idx} className="result-item failed-item">
                          <span className="doc-number">
                            {typeof doc === 'object' ? doc.documentNumber : doc}
                          </span>
                          {typeof doc === 'object' && doc.reason && (
                            <span className="failure-reason">{doc.reason}</span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Create OSC Documents */}
                {result.createOscDocuments?.length > 0 && (
                  <div className="result-list-card osc-card">
                    <div className="list-header osc-header">
                      <FilePlus size={20} />
                      <h5>Create OSC ({result.createOscDocuments.length})</h5>
                    </div>
                    <div className="result-list">
                      {result.createOscDocuments.map((doc, idx) => (
                        <div key={idx} className="result-item osc-item">
                          <span className="doc-number">
                            {typeof doc === 'object' ? doc.documentNumber : doc}
                          </span>
                          {typeof doc === 'object' && (
                            <div className="osc-details">
                              {doc.oscType && (
                                <span className="osc-type-badge">{doc.oscType}</span>
                              )}
                              {doc.reason && (
                                <span className="osc-reason">{doc.reason}</span>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MoveCaseStatus;
