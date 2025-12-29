import React, { useState } from 'react';
import { Play, RefreshCw, CheckCircle, XCircle, AlertCircle, Download, FileSpreadsheet, Clock, Search } from 'lucide-react';
import axios from 'axios';
import './LetterProcessing.css';

const LetterProcessing = ({ persistedState, onStateChange }) => {
  const [loading, setLoading] = useState(persistedState?.loading || false);
  const [downloading, setDownloading] = useState(false);
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
      const response = await axios.post('/api/letter/process');
      updateState({ result: response.data, loading: false });
    } catch (err) {
      updateState({ 
        error: err.response?.data?.message || err.message || 'Failed to process letter cases',
        loading: false 
      });
    }
  };

  const handleDownloadExcel = async () => {
    setDownloading(true);
    updateState({ error: null });

    try {
      const response = await axios.get('/api/letter/process/excel', {
        responseType: 'blob',
      });

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      // Get filename from response headers or generate one
      const contentDisposition = response.headers['content-disposition'];
      let filename = 'letter_generation_data.xlsx';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1].replace(/['"]/g, '');
        }
      }
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
    } catch (err) {
      updateState({ error: err.response?.data?.message || err.message || 'Failed to download Excel file' });
    } finally {
      setDownloading(false);
    }
  };

  // Filter data based on search query
  const filteredData = result?.data?.filter(item => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    return (
      item.correspondenceCorrelationId?.toLowerCase().includes(query) ||
      item.documentNumber?.toLowerCase().includes(query) ||
      item.carrier?.toLowerCase().includes(query) ||
      item.contractNumber?.toLowerCase().includes(query) ||
      item.deliveryType?.toLowerCase().includes(query) ||
      item.xmlFileName?.toLowerCase().includes(query)
    );
  }) || [];

  return (
    <div className="letter-processing">
      <div className="monitoring-header">
        <h2 className="section-title">Letter Generation Processing</h2>
        <p className="section-description">
          Process letter waiting cases and generate Excel report with letter generation data
        </p>
      </div>

      <div className="action-card">
        <div className="action-card-content">
          <div className="action-info">
            <h3 className="action-title">Process Letter Waiting Cases</h3>
            <p className="action-description">
              This will process all letter waiting cases, extract correspondence correlation IDs,
              fetch letter generation variables (CARRIER, CONTRACT_NUMBER, deliveryType, XML_FILE_NAME),
              and generate a downloadable Excel report.
            </p>
          </div>
          <div className="action-buttons">
            <button
              className={`process-button ${loading ? 'loading' : ''}`}
              onClick={handleProcessCases}
              disabled={loading || downloading}
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
            <button
              className={`download-button ${downloading ? 'loading' : ''}`}
              onClick={handleDownloadExcel}
              disabled={loading || downloading}
            >
              {downloading ? (
                <>
                  <RefreshCw className="button-icon spinning" size={20} />
                  Generating...
                </>
              ) : (
                <>
                  <Download className="button-icon" size={20} />
                  Process & Download Excel
                </>
              )}
            </button>
          </div>
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
                <FileSpreadsheet size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Total Processed</p>
                <p className="stat-value">{result.totalProcessed}</p>
              </div>
            </div>

            <div className="stat-card stat-success">
              <div className="stat-icon">
                <CheckCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">CARRIER Found</p>
                <p className="stat-value">{result.carrierFound}</p>
              </div>
            </div>

            <div className="stat-card stat-info">
              <div className="stat-icon">
                <Clock size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">CONTRACT_NUMBER</p>
                <p className="stat-value">{result.contractNumberFound}</p>
              </div>
            </div>

            <div className="stat-card stat-warning">
              <div className="stat-icon">
                <AlertCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">deliveryType</p>
                <p className="stat-value">{result.deliveryTypeFound}</p>
              </div>
            </div>
          </div>

          {result.message && (
            <div className="alert alert-info">
              <AlertCircle size={20} />
              <span>{result.message}</span>
            </div>
          )}

          {/* Data Table */}
          {result.data && result.data.length > 0 && (
            <div className="details-section">
              <div className="details-header">
                <h4 className="details-title">Letter Generation Data</h4>
                <div className="search-container">
                  <Search className="search-icon" size={18} />
                  <input
                    type="text"
                    className="search-input"
                    placeholder="Search by document number, carrier, contract..."
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

              {filteredData.length > 0 ? (
                <>
                  <div className="search-results-info">
                    Showing {filteredData.length} of {result.data.length} records
                  </div>
                  <div className="table-container">
                    <table className="details-table">
                      <thead>
                        <tr>
                          <th>Document Number</th>
                          <th>Correlation ID</th>
                          <th>CARRIER</th>
                          <th>CONTRACT_NUMBER</th>
                          <th>deliveryType</th>
                          <th>XML_FILE_NAME</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredData.map((item, index) => (
                          <tr key={index}>
                            <td className="document-number">{item.documentNumber || '-'}</td>
                            <td className="correlation-id">{item.correspondenceCorrelationId || '-'}</td>
                            <td>{item.carrier || '-'}</td>
                            <td>{item.contractNumber || '-'}</td>
                            <td>
                              {item.deliveryType ? (
                                <span className={`delivery-badge delivery-${item.deliveryType.toLowerCase()}`}>
                                  {item.deliveryType}
                                </span>
                              ) : '-'}
                            </td>
                            <td className="xml-filename">{item.xmlFileName || '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              ) : (
                <div className="no-results">
                  <AlertCircle size={24} />
                  <p>No records found matching "{searchQuery}"</p>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default LetterProcessing;

