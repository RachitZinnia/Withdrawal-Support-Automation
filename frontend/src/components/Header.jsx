import React from 'react';
import { FileText } from 'lucide-react';
import './Header.css';

const Header = () => {
  return (
    <header className="header">
      <div className="header-content">
        <div className="header-brand">
          <FileText className="header-icon" size={32} />
          <div>
            <h1 className="header-title">Withdrawal Support Automation</h1>
            <p className="header-subtitle">Daily Case Monitoring System</p>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;





