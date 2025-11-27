import React, { useState } from 'react';
import { Upload, FileText, RefreshCw, CheckCircle, XCircle, AlertCircle, Clock } from 'lucide-react';
import axios from 'axios';
import './DailyReportUpload.css';

const DailyReportUpload = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.name.toLowerCase().endsWith('.csv')) {
        setSelectedFile(file);
        setError(null);
      } else {
        setError('Please select a CSV file');
        setSelectedFile(null);
      }
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a CSV file first');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await axios.post('/api/daily-report/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setResult(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to process daily report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="daily-report-upload">
      <div className="monitoring-header">
        <h2 className="section-title">Daily Report Monitoring</h2>
        <p className="section-description">
          Upload daily report CSV to monitor and process non-matching cases
        </p>
      </div>

      <div className="upload-card">
        <div className="upload-area">
          <input
            type="file"
            id="csv-upload"
            accept=".csv"
            onChange={handleFileSelect}
            className="file-input"
          />
          <label htmlFor="csv-upload" className="file-label">
            <Upload className="upload-icon" size={32} />
            <span className="upload-text">
              {selectedFile ? selectedFile.name : 'Click to select CSV file'}
            </span>
            <span className="upload-hint">
              CSV files only - Max 10MB
            </span>
          </label>
        </div>

        {selectedFile && (
          <div className="file-selected">
            <FileText size={20} />
            <span className="file-name">{selectedFile.name}</span>
            <span className="file-size">
              ({(selectedFile.size / 1024).toFixed(2)} KB)
            </span>
          </div>
        )}

        <button
          className={`process-button ${loading ? 'loading' : ''}`}
          onClick={handleUpload}
          disabled={loading || !selectedFile}
        >
          {loading ? (
            <>
              <RefreshCw className="button-icon spinning" size={20} />
              Processing...
            </>
          ) : (
            <>
              <Upload className="button-icon" size={20} />
              Upload & Process
            </>
          )}
        </button>
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
                <FileText size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Total Rows</p>
                <p className="stat-value">{result.totalRowsInCsv}</p>
              </div>
            </div>

            <div className="stat-card stat-warning">
              <div className="stat-icon">
                <AlertCircle size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Not Matching</p>
                <p className="stat-value">{result.notMatchingRows}</p>
              </div>
            </div>

            <div className="stat-card stat-info">
              <div className="stat-icon">
                <Clock size={24} />
              </div>
              <div className="stat-content">
                <p className="stat-label">Cases Processed</p>
                <p className="stat-value">{result.processedCases}</p>
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
          </div>

          {/* Document Numbers Section */}
          <div className="document-lists-grid">
            <div className="document-list-card">
              <h4 className="list-title">
                <span className="badge badge-cancel">To Cancel ({result.documentNumbersToCancel?.length || 0})</span>
              </h4>
              <div className="document-list">
                {result.documentNumbersToCancel && result.documentNumbersToCancel.length > 0 ? (
                  result.documentNumbersToCancel.map((doc, idx) => (
                    <div key={idx} className="document-item">{doc}</div>
                  ))
                ) : (
                  <div className="empty-list">No documents to cancel</div>
                )}
              </div>
            </div>

            <div className="document-list-card">
              <h4 className="list-title">
                <span className="badge badge-returning">To Returning ({result.documentNumbersToReturning?.length || 0})</span>
              </h4>
              <div className="document-list">
                {result.documentNumbersToReturning && result.documentNumbersToReturning.length > 0 ? (
                  result.documentNumbersToReturning.map((doc, idx) => (
                    <div key={idx} className="document-item">{doc}</div>
                  ))
                ) : (
                  <div className="empty-list">No returning documents</div>
                )}
              </div>
            </div>

            <div className="document-list-card">
              <h4 className="list-title">
                <span className="badge badge-complete">To Complete ({result.documentNumbersToComplete?.length || 0})</span>
              </h4>
              <div className="document-list">
                {result.documentNumbersToComplete && result.documentNumbersToComplete.length > 0 ? (
                  result.documentNumbersToComplete.map((doc, idx) => (
                    <div key={idx} className="document-item">{doc}</div>
                  ))
                ) : (
                  <div className="empty-list">No documents to complete</div>
                )}
              </div>
            </div>

            <div className="document-list-card">
              <h4 className="list-title">
                <span className="badge badge-review">Manual Review ({result.documentNumbersForManualReview?.length || 0})</span>
              </h4>
              <div className="document-list">
                {result.documentNumbersForManualReview && result.documentNumbersForManualReview.length > 0 ? (
                  result.documentNumbersForManualReview.map((doc, idx) => (
                    <div key={idx} className="document-item">{doc}</div>
                  ))
                ) : (
                  <div className="empty-list">No manual review needed</div>
                )}
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
                      <th>Process Instance</th>
                      <th>Case ID</th>
                      <th>Client Code</th>
                      <th>OnBase Status</th>
                      <th>Category</th>
                      <th>Status</th>
                      <th>Message</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.details.map((detail, index) => (
                      <tr key={index}>
                        <td className="case-ref">{detail.caseReference || '-'}</td>
                        <td className="case-id">{detail.caseId || '-'}</td>
                        <td className="client-code">{detail.clientCode || '-'}</td>
                        <td className="onbase-status">{detail.onbaseStatus || '-'}</td>
                        <td className="category-cell">
                          <span className={`category-badge category-${detail.category?.toLowerCase()}`}>
                            {detail.category || '-'}
                          </span>
                        </td>
                        <td>
                          <span className={`status-badge status-${detail.status?.toLowerCase()}`}>
                            {detail.status}
                          </span>
                        </td>
                        <td className="message-cell">{detail.message || '-'}</td>
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

export default DailyReportUpload;




