#!/bin/bash

# Withdrawal Support Automation - Setup Script
# This script helps you set up the development environment

set -e

echo "🚀 Withdrawal Support Automation - Setup Script"
echo "================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."
echo ""

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo -e "${GREEN}✓${NC} Java $JAVA_VERSION installed"
    else
        echo -e "${RED}✗${NC} Java 17 or higher required (found Java $JAVA_VERSION)"
        exit 1
    fi
else
    echo -e "${RED}✗${NC} Java not found. Please install Java 17 or higher"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | grep "Apache Maven" | awk '{print $3}')
    echo -e "${GREEN}✓${NC} Maven $MVN_VERSION installed"
else
    echo -e "${RED}✗${NC} Maven not found. Please install Maven 3.6+"
    exit 1
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        echo -e "${GREEN}✓${NC} Node.js $(node -v) installed"
    else
        echo -e "${YELLOW}⚠${NC}  Node.js 18+ recommended (found $(node -v))"
    fi
else
    echo -e "${RED}✗${NC} Node.js not found. Please install Node.js 18+"
    exit 1
fi

# Check npm
if command -v npm &> /dev/null; then
    echo -e "${GREEN}✓${NC} npm $(npm -v) installed"
else
    echo -e "${RED}✗${NC} npm not found"
    exit 1
fi

# Check MongoDB
if command -v mongosh &> /dev/null; then
    echo -e "${GREEN}✓${NC} MongoDB Shell installed"
else
    echo -e "${YELLOW}⚠${NC}  MongoDB Shell not found. Make sure MongoDB is installed and running"
fi

echo ""
echo "================================================"
echo ""

# Setup Backend
echo "🔧 Setting up Backend..."
echo ""

cd backend

# Check if configuration exists
if [ ! -f "src/main/resources/application-local.properties" ]; then
    echo "Creating application-local.properties from example..."
    cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
    echo -e "${YELLOW}⚠${NC}  Please edit backend/src/main/resources/application-local.properties with your API credentials"
    echo ""
else
    echo -e "${GREEN}✓${NC} Configuration file already exists"
fi

# Build backend
echo "Building backend (this may take a few minutes)..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Backend built successfully"
else
    echo -e "${RED}✗${NC} Backend build failed"
    exit 1
fi

cd ..

echo ""
echo "================================================"
echo ""

# Setup Frontend
echo "🎨 Setting up Frontend..."
echo ""

cd frontend

# Install dependencies
echo "Installing frontend dependencies..."
npm install

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Frontend dependencies installed"
else
    echo -e "${RED}✗${NC} Frontend setup failed"
    exit 1
fi

cd ..

echo ""
echo "================================================"
echo ""
echo -e "${GREEN}✅ Setup completed successfully!${NC}"
echo ""
echo "📝 Next Steps:"
echo ""
echo "1. Configure your API credentials:"
echo "   Edit: backend/src/main/resources/application-local.properties"
echo ""
echo "2. Start MongoDB (if not already running):"
echo "   macOS:   brew services start mongodb-community"
echo "   Linux:   sudo systemctl start mongod"
echo "   Windows: net start MongoDB"
echo ""
echo "3. Start the backend (in one terminal):"
echo "   cd backend"
echo "   mvn spring-boot:run"
echo ""
echo "4. Start the frontend (in another terminal):"
echo "   cd frontend"
echo "   npm run dev"
echo ""
echo "5. Open your browser to: http://localhost:3000"
echo ""
echo "📖 For detailed instructions, see QUICKSTART.md"
echo ""
echo "🎉 Happy coding!"





