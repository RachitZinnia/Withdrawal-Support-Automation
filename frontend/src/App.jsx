import { useState } from 'react'
import './App.css'
import CaseMonitoring from './components/CaseMonitoring'
import DailyReportUpload from './components/DailyReportUpload'
import Header from './components/Header'

function App() {
  const [activeTab, setActiveTab] = useState('dataentry');

  return (
    <div className="app">
      <Header />
      <div className="tabs-container">
        <div className="tabs">
          <button 
            className={`tab ${activeTab === 'dataentry' ? 'active' : ''}`}
            onClick={() => setActiveTab('dataentry')}
          >
            Data Entry Waiting Cases
          </button>
          <button 
            className={`tab ${activeTab === 'dailyreport' ? 'active' : ''}`}
            onClick={() => setActiveTab('dailyreport')}
          >
            Daily Report Monitoring
          </button>
        </div>
      </div>
      <main className="main-content">
        {activeTab === 'dataentry' && <CaseMonitoring />}
        {activeTab === 'dailyreport' && <DailyReportUpload />}
      </main>
    </div>
  )
}

export default App


