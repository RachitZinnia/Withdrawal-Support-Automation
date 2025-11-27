# Business Days Calculation

This document explains how the system calculates business days (excluding weekends and holidays) for stale case detection.

## 🎯 Problem Statement

When checking if a case is "stale" (older than 2 days), we need to count **business days** not **calendar days**.

### ❌ Calendar Days (Wrong)
```
Case last updated: Thursday, Nov 7
Today: Monday, Nov 11
Calendar days passed: 4 days
Result: Stale (> 2 days)
```

### ✅ Business Days (Correct)
```
Case last updated: Thursday, Nov 7
Today: Monday, Nov 11
Business days passed: 2 days (Fri, Mon - excludes Sat/Sun)
Result: NOT stale (= 2 days, not > 2 days)
```

## 🏗️ Implementation

### BusinessDaysCalculator Utility

**File:** `backend/src/main/java/com/withdrawal/support/util/BusinessDaysCalculator.java`

#### Main Method: `isOlderThanBusinessDays()`

```java
public static boolean isOlderThanBusinessDays(LocalDateTime dateTime, int businessDaysThreshold) {
    LocalDate targetDate = dateTime.toLocalDate();
    LocalDate today = LocalDate.now();
    
    int businessDaysPassed = calculateBusinessDaysBetween(targetDate, today);
    
    return businessDaysPassed > businessDaysThreshold;
}
```

#### Core Method: `calculateBusinessDaysBetween()`

```java
public static int calculateBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
    int businessDays = 0;
    LocalDate currentDate = startDate;
    
    // Iterate through each day
    while (currentDate.isBefore(endDate)) {
        if (isBusinessDay(currentDate)) {  // Exclude weekends & holidays
            businessDays++;
        }
        currentDate = currentDate.plusDays(1);
    }
    
    return businessDays;
}
```

#### Helper Method: `isBusinessDay()`

```java
public static boolean isBusinessDay(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    
    // Exclude weekends
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
        return false;
    }
    
    // Exclude holidays
    if (HOLIDAYS.contains(date)) {
        return false;
    }
    
    return true;
}
```

## 📅 Examples

### Example 1: Within Same Week
```
Last Updated: Monday, Nov 11 at 9:00 AM
Today:        Wednesday, Nov 13 at 3:00 PM
Threshold:    2 business days

Days between:
- Monday → Tuesday (business day) = 1
- Tuesday → Wednesday (business day) = 2

Business days passed: 2
Result: NOT stale (2 is not > 2)
```

### Example 2: Over Weekend
```
Last Updated: Thursday, Nov 7 at 2:00 PM
Today:        Monday, Nov 11 at 10:00 AM
Threshold:    2 business days

Days between:
- Thursday → Friday (business day) = 1
- Friday → Saturday (WEEKEND - excluded)
- Saturday → Sunday (WEEKEND - excluded)
- Sunday → Monday (business day) = 2

Business days passed: 2
Result: NOT stale (2 is not > 2)
```

### Example 3: Stale Case
```
Last Updated: Tuesday, Nov 5 at 1:00 PM
Today:        Monday, Nov 11 at 10:00 AM
Threshold:    2 business days

Days between:
- Tuesday → Wednesday (business day) = 1
- Wednesday → Thursday (business day) = 2
- Thursday → Friday (business day) = 3
- Friday → Saturday (WEEKEND - excluded)
- Saturday → Sunday (WEEKEND - excluded)
- Sunday → Monday (business day) = 4

Business days passed: 4
Result: STALE (4 > 2) → Manual Review Required
```

### Example 4: With Holiday
```
Last Updated: Wednesday, Dec 24 at 10:00 AM
Today:        Monday, Dec 30 at 9:00 AM
Threshold:    2 business days

Days between:
- Wed Dec 24 → Thu Dec 25 (HOLIDAY - Christmas - excluded)
- Thu Dec 25 → Fri Dec 26 (business day) = 1
- Fri Dec 26 → Sat Dec 27 (WEEKEND - excluded)
- Sat Dec 27 → Sun Dec 28 (WEEKEND - excluded)
- Sun Dec 28 → Mon Dec 29 (business day) = 2
- Mon Dec 29 → Mon Dec 30 (business day) = 3

Business days passed: 3
Result: STALE (3 > 2) → Manual Review Required
```

## 🎯 Integration with CaseMongoService

### Updated Method

```java
public CaseAnalysisResult analyzeCaseFromMongo(String documentNumber, int businessDaysThreshold) {
    Optional<CaseInstanceDocument> caseInstance = 
        caseInstanceRepository.findByDocumentNumber(documentNumber);
    
    if (caseInstance.isEmpty()) {
        return CaseAnalysisResult.builder()
                .found(false)
                .isStale(false)
                .build();
    }
    
    CaseInstanceDocument document = caseInstance.get();
    LocalDateTime lastUpdated = document.getLastUpdated();
    
    // Use BusinessDaysCalculator for accurate calculation
    boolean isStale = BusinessDaysCalculator.isOlderThanBusinessDays(
            lastUpdated, 
            businessDaysThreshold
    );
    
    return CaseAnalysisResult.builder()
            .found(true)
            .caseStatus(document.getCaseStatus())
            .isInProgress(checkIfInProgress(document))
            .isStale(isStale)  // ← Using business days calculation
            .lastUpdated(lastUpdated)
            .build();
}
```

## 🏢 Holiday Configuration

### Current Implementation

Holidays are defined in the `BusinessDaysCalculator` class:

```java
private static final Set<LocalDate> HOLIDAYS = new HashSet<>();

static {
    // 2025 US Federal Holidays (example)
    HOLIDAYS.add(LocalDate.of(2025, 1, 1));   // New Year's Day
    HOLIDAYS.add(LocalDate.of(2025, 7, 4));   // Independence Day
    HOLIDAYS.add(LocalDate.of(2025, 12, 25)); // Christmas
    // Add more as needed
}
```

### Future Enhancement: External Configuration

You can move holidays to a configuration file or database:

**Option 1: application.properties**
```properties
business.holidays=2025-01-01,2025-07-04,2025-12-25
```

**Option 2: Database Table**
```sql
CREATE TABLE holidays (
    holiday_date DATE PRIMARY KEY,
    description VARCHAR(255)
);
```

**Option 3: External API**
```java
// Call holiday API to get current year's holidays
List<LocalDate> holidays = holidayService.getHolidaysForYear(2025);
```

## 📊 Comparison: Calendar Days vs Business Days

| Scenario | Last Updated | Today | Calendar Days | Business Days | Result (2 day threshold) |
|----------|-------------|-------|---------------|---------------|--------------------------|
| Same week | Mon 9am | Wed 5pm | 2 | 2 | NOT stale |
| Over weekend | Thu 2pm | Mon 10am | 4 | 2 | NOT stale |
| Longer period | Tue 1pm | Following Mon | 6 | 4 | STALE ✓ |
| With holiday | Wed before Xmas | Mon after Xmas | 5 | 3 | STALE ✓ |

## 🧪 Testing

### Test Cases

```java
// Test 1: 2 business days (not stale)
LocalDateTime lastUpdate = LocalDateTime.of(2025, 11, 7, 10, 0);  // Thursday
LocalDateTime now = LocalDateTime.of(2025, 11, 11, 15, 0);        // Monday
boolean isStale = BusinessDaysCalculator.isOlderThanBusinessDays(lastUpdate, 2);
// Expected: false (exactly 2 business days: Fri, Mon)

// Test 2: 3 business days (stale)
LocalDateTime lastUpdate = LocalDateTime.of(2025, 11, 6, 10, 0);  // Wednesday
LocalDateTime now = LocalDateTime.of(2025, 11, 11, 15, 0);        // Monday
boolean isStale = BusinessDaysCalculator.isOlderThanBusinessDays(lastUpdate, 2);
// Expected: true (3 business days: Thu, Fri, Mon)

// Test 3: Over weekend (not stale)
LocalDateTime lastUpdate = LocalDateTime.of(2025, 11, 7, 14, 0);  // Thursday 2pm
LocalDateTime now = LocalDateTime.of(2025, 11, 10, 10, 0);        // Sunday 10am
boolean isStale = BusinessDaysCalculator.isOlderThanBusinessDays(lastUpdate, 2);
// Expected: false (only 1 business day: Friday)
```

## 📝 Configuration

**application.properties**
```properties
# Business days threshold for stale case detection
business.days-threshold=2
```

This threshold now correctly counts **business days only**, excluding:
- Saturdays
- Sundays
- Configured holidays

## 🔍 Logging

The system logs business days calculations:

```
[DEBUG] Date: 2025-11-07, Today: 2025-11-11, Business days passed: 2, Threshold: 2
[INFO] Case analysis - isStale: false, lastUpdated: 2025-11-07T10:00:00
```

## ⚙️ Additional Utility Methods

### Add Business Days
```java
// Add 2 business days to a date
LocalDate futureDate = BusinessDaysCalculator.addBusinessDays(
    LocalDate.of(2025, 11, 7),  // Thursday
    2
);
// Result: Monday, Nov 11 (skips weekend)
```

### Subtract Business Days
```java
// Subtract 2 business days from a date
LocalDate pastDate = BusinessDaysCalculator.subtractBusinessDays(
    LocalDate.of(2025, 11, 11),  // Monday
    2
);
// Result: Thursday, Nov 7 (skips weekend)
```

## 🎯 Use in System

```java
// In CaseMongoService
boolean isStale = BusinessDaysCalculator.isOlderThanBusinessDays(
    lastUpdated,        // Case last updated timestamp
    2                   // Business days threshold
);

if (isStale) {
    // Case is > 2 business days old (excluding weekends)
    // → Add to manual review list
}
```

## 📊 Benefits

1. **Accurate Calculation** - Correctly excludes weekends
2. **Configurable Holidays** - Easy to add company holidays
3. **Reusable** - Can be used anywhere in the application
4. **Well-Tested Logic** - Standard business day calculation
5. **Performance** - Efficient iteration algorithm

## 🔮 Future Enhancements

### Option 1: Holiday API Integration
```java
@Service
public class HolidayService {
    // Fetch holidays from external API
    public Set<LocalDate> getHolidays(int year) {
        // Call holiday API
    }
}
```

### Option 2: Regional Holidays
```java
// Support different regions
public static boolean isBusinessDay(LocalDate date, String region) {
    // Check region-specific holidays
    return switch(region) {
        case "US" -> isUSBusinessDay(date);
        case "UK" -> isUKBusinessDay(date);
        default -> isDefaultBusinessDay(date);
    };
}
```

### Option 3: Custom Business Days Definition
```java
// Some companies count Saturday as business day
public static boolean isBusinessDay(LocalDate date, Set<DayOfWeek> workingDays) {
    return workingDays.contains(date.getDayOfWeek()) && !HOLIDAYS.contains(date);
}
```

## 📚 References

- [Java LocalDate API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDate.html)
- [Java DayOfWeek Enum](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/DayOfWeek.html)

---

**Last Updated:** November 10, 2025
**Version:** 1.6.0 (Added Business Days Calculation)




