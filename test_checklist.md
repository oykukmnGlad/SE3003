### Campus Studio & Equipment Booking System (ReserveStudio)
**Document:** MVP Test Checklist & Manual Test Cases  
**Owner:** Eylül Ela Ata (Tester/Debugger)  
**Target:** WP-05 / EST-04 (Test Checklist & Manual Testing - 6 Hours)

---

#### 1. User Authentication Tests
- [ ] **TC-01:** Can users sign up and log into the system with valid credentials?
- [ ] **TC-02:** Does the system display appropriate error messages for incorrect passwords or emails?
- [ ] **TC-03:** Are users prevented from logging in when required fields (email or password) are left blank?

#### 2. Studio & Slot Management Tests
- [ ] **TC-04:** Are the main studio's hourly calendar and availability status correctly listed on the screen?
- [ ] **TC-05:** Are users prevented from booking past hours or dates?
- [ ] **TC-06:** Do occupied time slots appear as "Reserved" or locked on the screen?

#### 3. Conflict Resolution & Overlap Defense Tests
- [ ] **TC-07:** When two different users attempt to book the exact same studio and time slot simultaneously (Race Condition / Double-booking), does the database UNIQUE constraint and backend validation block the second user?
- [ ] **TC-08:** Does a time slot immediately close for other users right after a successful booking?

#### 4. UI & Usability Edge Cases
- [ ] **TC-09:** Do UI elements render correctly without breaking or shifting on mobile/tablet web browsers?
- [ ] **TC-10:** Is a confirmation message successfully displayed to the user once the booking process is complete?
