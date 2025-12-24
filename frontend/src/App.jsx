import { useState } from 'react'
import './App.css'
import CaseMonitoring from './components/CaseMonitoring'
import DailyReportUpload from './components/DailyReportUpload'
import MRTProcessing from './components/MRTProcessing'
import LetterProcessing from './components/LetterProcessing'
import Header from './components/Header'

function App() {
  const [activeTab, setActiveTab] = useState('dailyreport');
  
  // Lifted state for all components to persist across tab switches
  const [dailyReportState, setDailyReportState] = useState({
    result: null,
    error: null,
    selectedFile: null
  });
  
  const [caseMonitoringState, setCaseMonitoringState] = useState({
    result: null,
    error: null
  });
  
  const [mrtState, setMrtState] = useState({
    result: null,
    error: null
  });
  
  const [letterState, setLetterState] = useState({
    result: null,
    error: null
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
        </div>
      </div>
      <main className="main-content">
        {activeTab === 'dailyreport' && (
          <DailyReportUpload 
            persistedState={dailyReportState}
            onStateChange={setDailyReportState}
          />
        )}
        {activeTab === 'dataentry' && (
          <CaseMonitoring 
            persistedState={caseMonitoringState}
            onStateChange={setCaseMonitoringState}
          />
        )}
        {activeTab === 'mrt' && (
          <MRTProcessing 
            persistedState={mrtState}
            onStateChange={setMrtState}
          />
        )}
        {activeTab === 'letter' && (
          <LetterProcessing 
            persistedState={letterState}
            onStateChange={setLetterState}
          />
        )}
      </main>
    </div>
  )
}

export default App
