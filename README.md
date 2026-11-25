# ReserveStudio — Campus Studio & Equipment Booking System
> **Active code:** `reservestudio/` (backend + frontend). The folders `Backend/`, `reservestudio-backend2/`, `frontend/` and `Db/` are older drafts kept for history.

**Course:** SE3003 – Software Project Management  
**Team:** ReserveStudio  

## Team Members & Roles
* **Öykü Kaman** (230717004) – Team Lead & Frontend Developer
* **Mete Salman** (230717033) – Backend Developer
* **Ahmet Efe Yavuz** (230717046) – Backend Developer
* **Eylül Ela Ata** (240717063) – Tester / Debugger

## Project Structure
* `reservestudio/` — **Active & integrated codebase**
  * `backend/` — Java Spring Boot REST API, JUnit integration/concurrency tests, and MySQL schema (`backend/db/reservestudio_db.sql`)
  * `frontend/` — HTML, CSS, and JavaScript client pages (`index.html`, `signup.html`, `booking.html`, `dashboard.html`, `api.js`)
* `test_checklist.md` — Manual QA test checklist (`TC-01` – `TC-12`)
* `.pdf` — Milestone reports (`M0`, `M1`, `M2`, `M3`) and weekly progress reports

## How to Run Locally
1. **Database Setup (MySQL 8+):**
   ```bash
   mysql -u root -p < reservestudio/backend/db/reservestudio_db.sql
   ```
2. **Start the backend:**
   ```bash
   cd reservestudio/backend
   mvn spring-boot:run
   ```
3. **Open the app:** http://localhost:8080
