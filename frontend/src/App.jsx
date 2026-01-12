import { useState } from 'react'
import './App.css'
import CaseMonitoring from './components/CaseMonitoring'
import DailyReportUpload from './components/DailyReportUpload'
import MRTProcessing from './components/MRTProcessing'
import LetterProcessing from './components/LetterProcessing'
import EmailProcessing from './components/EmailProcessing'
import MoveCaseStatus from './components/MoveCaseStatus'
import Header from './components/Header'

function App() {
  const [activeTab, setActiveTab] = useState('dailyreport');
  
  // Lifted state for all components to persist across tab switches
  const [dailyReportState, setDailyReportState] = useState({
    result: null,
    error: null,
    selectedFile: null,
    loading: false
  });
  
  const [caseMonitoringState, setCaseMonitoringState] = useState({
    result: null,
    error: null,
    loading: false
  });
  
  const [mrtState, setMrtState] = useState({
    result: null,
    error: null,
    loading: false
  });
  
  const [letterState, setLetterState] = useState({
    result: null,
    error: null,
    loading: false
  });
  
  const [emailState, setEmailState] = useState({
    result: null,
    error: null,
    loading: false
  });

  const [moveCaseState, setMoveCaseState] = useState({
    result: null,
    error: null,
    loading: false,
    selectedAction: null,
    documentNumbers: ''
  });

  return (
    <div className="app">
      <Header />
      <div className="tabs-container">
        <div className="tabs">
          <button 
            className={`tab ${activeTab === 'dailyreport' ? 'active' : ''}`}
            onClick={() => setActiveTab('dailyreport')}
          >
            Daily Report Monitoring
          </button>
          <button 
            className={`tab ${activeTab === 'dataentry' ? 'active' : ''}`}
            onClick={() => setActiveTab('dataentry')}
          >
            Data Entry Waiting Cases
          </button>
          <button 
            className={`tab ${activeTab === 'mrt' ? 'active' : ''}`}
            onClick={() => setActiveTab('mrt')}
          >
            MRT & GIACT Processing
          </button>
          <button 
            className={`tab ${activeTab === 'letter' ? 'active' : ''}`}
            onClick={() => setActiveTab('letter')}
          >
            Letter Generation
          </button>
          <button 
            className={`tab ${activeTab === 'email' ? 'active' : ''}`}
            onClick={() => setActiveTab('email')}
          >
            Email Resolution
          </button>
          <button 
            className={`tab ${activeTab === 'movecase' ? 'active' : ''}`}
            onClick={() => setActiveTab('movecase')}
          >
            Move Case Status
          </button>
        </div>
      </div>
      <main className="main-content">
        {activeTab === 'dailyreport' && (
          <DailyReportUpload 
            persistedState={dailyReportState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setDailyReportState(prev => updater(prev));
              } else {
                setDailyReportState(updater);
              }
            }}
          />
        )}
        {activeTab === 'dataentry' && (
          <CaseMonitoring 
            persistedState={caseMonitoringState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setCaseMonitoringState(prev => updater(prev));
              } else {
                setCaseMonitoringState(updater);
              }
            }}
          />
        )}
        {activeTab === 'mrt' && (
          <MRTProcessing 
            persistedState={mrtState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setMrtState(prev => updater(prev));
              } else {
                setMrtState(updater);
              }
            }}
          />
        )}
        {activeTab === 'letter' && (
          <LetterProcessing 
            persistedState={letterState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setLetterState(prev => updater(prev));
              } else {
                setLetterState(updater);
              }
            }}
          />
        )}
        {activeTab === 'email' && (
          <EmailProcessing 
            persistedState={emailState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setEmailState(prev => updater(prev));
              } else {
                setEmailState(updater);
              }
            }}
          />
        )}
        {activeTab === 'movecase' && (
          <MoveCaseStatus 
            persistedState={moveCaseState}
            onStateChange={(updater) => {
              if (typeof updater === 'function') {
                setMoveCaseState(prev => updater(prev));
              } else {
                setMoveCaseState(updater);
              }
            }}
          />
        )}
      </main>
    </div>
  )
}

export default App
