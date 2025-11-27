# Quick Start Guide

Get the Withdrawal Support Automation system up and running in minutes!

## 📋 Prerequisites Checklist

- [ ] Java 17+ installed (`java -version`)
- [ ] Maven 3.6+ installed (`mvn -version`)
- [ ] Node.js 18+ installed (`node -v`)
- [ ] MongoDB running (`mongosh --eval "db.version()"`)

## 🚀 5-Minute Setup

### Step 1: Configure Backend (2 minutes)

```bash
# Navigate to backend
cd backend

# Copy the example configuration
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Edit the configuration file with your credentials
# Use your favorite editor (vim, nano, code, etc.)
nano src/main/resources/application-local.properties
```

**Required configurations:**
```properties
# Update these with your actual values:
spring.data.mongodb.uri=mongodb://localhost:27017/withdrawal_support
api.dataentry.url=https://your-data-entry-api.com
api.dataentry.key=YOUR_ACTUAL_API_KEY
api.casedetails.url=https://your-case-details-api.com
api.casedetails.key=YOUR_ACTUAL_API_KEY
api.onbase.url=https://your-onbase-api.com
api.onbase.key=YOUR_ACTUAL_API_KEY
api.onbase.username=YOUR_USERNAME
api.onbase.password=YOUR_PASSWORD
```

### Step 2: Start Backend (1 minute)

```bash
# In the backend directory
mvn clean install
mvn spring-boot:run
```

✅ Backend should now be running at `http://localhost:8080`

### Step 3: Start Frontend (1 minute)

Open a **new terminal window**:

```bash
# Navigate to frontend
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

✅ Frontend should now be running at `http://localhost:3000`

### Step 4: Test the System (1 minute)

1. Open your browser to `http://localhost:3000`
2. You should see the "Withdrawal Support Automation" dashboard
3. Click the **"Process Cases"** button
4. Watch the magic happen! 🎉

## 🎯 What Happens When You Click "Process Cases"?

```
1. Frontend → POST /api/cases/process-dataentry-waiting → Backend
2. Backend fetches all waiting cases from Data Entry API
3. For each case:
   - Gets case details (ID + variables)
   - Checks OnBase status
   - Takes appropriate action
   - Verifies MongoDB status
   - Flags stale cases (> 2 days)
4. Backend → Returns results → Frontend
5. Frontend displays beautiful dashboard with statistics
```

## 🔍 Verification Steps

### Check Backend Health

```bash
curl http://localhost:8080/api/cases/health
# Should return: "Service is running"
```

### Check MongoDB Connection

```bash
mongosh
use withdrawal_support
db.cases.countDocuments()
```

### Check Frontend

Open `http://localhost:3000` in your browser

## 🐛 Common Issues & Solutions

### Issue: Backend won't start

**Error:** `Cannot connect to MongoDB`
```bash
# Solution: Start MongoDB
# macOS
brew services start mongodb-community

# Linux
sudo systemctl start mongod

# Windows
net start MongoDB
```

**Error:** `Port 8080 already in use`
```bash
# Solution: Change port in application.properties
server.port=8081
```

### Issue: Frontend won't start

**Error:** `Cannot find module`
```bash
# Solution: Reinstall dependencies
rm -rf node_modules package-lock.json
npm install
```

**Error:** `Port 3000 already in use`
```bash
# Solution: The terminal will prompt you to use another port
# Press 'y' to use the suggested port
```

### Issue: API calls failing

**Error:** `404 or Network Error`
```bash
# Verify backend is running
curl http://localhost:8080/api/cases/health

# Check Vite proxy configuration in vite.config.js
# Ensure target is 'http://localhost:8080'
```

## 📊 Testing with Mock Data (Optional)

If you don't have access to real APIs yet, you can create a mock mode:

1. Create mock endpoints in your backend
2. Use sample data for testing
3. Verify the UI works correctly

## 🎨 What You Should See

### Initial Dashboard
- Purple gradient header
- "Withdrawal Support Automation" title
- "Process Cases" button with gradient
- Clean, modern interface

### After Processing
- 4 statistics cards (Total, Successful, Failed, Manual Review)
- Summary message
- Detailed table with all cases
- Status badges with colors
- Manual review indicators

## ⚙️ Configuration Options

### Change Days Threshold

Edit `backend/src/main/resources/application.properties`:
```properties
business.days-threshold=3  # Change from 2 to 3 days
```

### Change Port Numbers

**Backend:**
```properties
# application.properties
server.port=8081
```

**Frontend:**
```javascript
// vite.config.js
server: {
  port: 3001
}
```

## 🔄 Development Workflow

### Backend Changes
```bash
# Backend will auto-reload with spring-boot-devtools
# Just save your changes and the server restarts
```

### Frontend Changes
```bash
# Vite hot-reloads automatically
# Save your files and see changes instantly in browser
```

## 📦 Production Deployment

### Backend Production Build
```bash
cd backend
mvn clean package
java -jar target/withdrawal-support-automation-1.0.0.jar
```

### Frontend Production Build
```bash
cd frontend
npm run build
# Deploy the 'dist' folder to your hosting service
```

## 🎓 Next Steps

1. ✅ System is running
2. 📖 Read the main [README.md](README.md) for detailed documentation
3. 🎨 Customize the UI colors and branding
4. 🔐 Add authentication (future enhancement)
5. 📊 Add more monitoring features
6. 🚀 Deploy to production

## 💡 Pro Tips

1. **Use separate terminals** for backend and frontend for easy monitoring
2. **Check logs** when things go wrong - they're very descriptive
3. **Start with MongoDB** before starting the backend
4. **Clear browser cache** if you see stale data
5. **Use Chrome DevTools** Network tab to debug API calls

## 📞 Need Help?

- Check the detailed [README.md](README.md)
- Review [backend/README.md](backend/README.md) for backend specifics
- Review [frontend/README.md](frontend/README.md) for frontend specifics
- Check application logs for error details

## ✨ Success Indicators

✅ Backend console shows: "Started WithdrawalSupportAutomationApplication"
✅ Frontend shows: "VITE ... ready in ... ms"
✅ Browser opens to beautiful dashboard
✅ Process Cases button is clickable
✅ Test processing returns results

**Congratulations! Your system is ready to automate case processing! 🎉**





