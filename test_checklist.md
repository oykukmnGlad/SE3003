### Campus Studio & Equipment Booking System (ReserveStudio)
**Document:** MVP Test Checklist & Manual Test Cases  
**Owner:** Eylül Ela Ata (Tester/Debugger)  
**Target:** WP-05 / EST-04 (Test Checklist & Manual Testing - 6 Hours)  
**Tested by:** [name] | **Date:** [dd.mm.2026]  
**Environment:** [browser and version], local MySQL 8, backend started with `mvn spring-boot:run`, app at http://localhost:8080  
**Evidence:** [link or folder with screenshots]

---

#### 1. User Authentication Tests
- [x] **TC-01:** Can users sign up and log into the system with valid credentials? — **PASS**
- [x] **TC-02:** Does the system display appropriate error messages for incorrect passwords or emails? — **PASS**
- [x] **TC-03:** Are users prevented from logging in when required fields (email or password) are left blank? — **PASS**

#### 2. Studio & Slot Management Tests
- [x] **TC-04:** Are the main studio's hourly calendar and availability status correctly listed on the screen? — **PASS**
- [x] **TC-05:** Are users prevented from booking past hours or dates? — **PASS** (note: [date picker blocks past days / backend rejects past slots])
- [x] **TC-06:** Do occupied time slots appear as "Unavailable" or locked on the screen? — **PASS**

#### 3. Conflict Resolution & Overlap Defense Tests
- [x] **TC-07:** When two different users attempt to book the exact same studio and time slot simultaneously (Race Condition / Double-booking), does the database UNIQUE constraint and backend validation block the second user? — **PASS** (evidence: automated ConcurrentBookingTest, 20 parallel requests: 1 success, 19 conflicts; manual two-browser check: [done by ... / not done])
- [x] **TC-08:** Does a time slot immediately close for other users right after a successful booking? — **PASS**

#### 4. UI & Usability Edge Cases
- [x] **TC-09:** Is a confirmation message successfully displayed to the user once the booking process is complete? — **PASS**
- [x] **TC-10:** Can a user successfully select an available time slot and submit a valid booking request without unexpected errors? — **PASS**

#### 5. Booking Cancellation Tests
- [ ] **TC-11:** Can a user cancel an upcoming booking from My Bookings, and does the slot become available again for booking? — [PASS / FAIL / not run]
- [ ] **TC-12:** Are past reservations listed only under "Past Reservations", without a Cancel button? — [PASS / FAIL / not run]

---

#### Summary
- Total test cases: 12 | Passed: [ ] | Failed: [ ] | Not run: [ ]
- Bugs found: [none / list]
