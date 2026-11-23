# Postman guide – full walkthrough

Base URL: `http://localhost:8080` · every request with a body: **Body → raw → JSON**.

For a clean run, start with an empty database (IDs below assume that):
```sql
DROP DATABASE leave_db;  CREATE DATABASE leave_db;
```
If you keep old data, use the real IDs from the `GET` endpoints instead.

Today is Sunday 22 Nov 2026, so the leave dates below are in the future. Public holidays in `application.properties`: 25 Dec 2026, 1 Jan 2027, 26 Jan 2027.

---
## 0. Department Manager (Step 1) – do this first

### 0.1 Create departments
`POST http://localhost:8080/api/departments`
```json
{ "departmentName": "Information Technology" }
```
✅ 201, `departmentManagerId: null`. Create a second one (id 2):
```json
{ "departmentName": "Human Resources" }
```

### 0.2 Create employees (all in department 1)
`POST http://localhost:8080/api/employees?departmentId=1`
```json
{ "name": "Priya",  "email": "priya@company.com",  "designation": "Engineering Manager", "dateOfJoining": "2024-01-10" }
```
```json
{ "name": "Rahul",  "email": "rahul@company.com",  "designation": "Software Developer",  "dateOfJoining": "2026-11-22" }
```
```json
{ "name": "Anita",  "email": "anita@company.com",  "designation": "Senior Developer",    "dateOfJoining": "2024-03-01" }
```
```json
{ "name": "Admin User", "email": "admin@company.com", "designation": "HR Admin",        "dateOfJoining": "2023-05-01" }
```
→ ids 1 Priya, 2 Rahul, 3 Anita, 4 Admin User.

### 0.3 Reporting managers (already existed)
`PUT http://localhost:8080/api/employees/2/manager/1` → Rahul reports to Priya  
`PUT http://localhost:8080/api/employees/3/manager/1` → Anita reports to Priya  
`GET http://localhost:8080/api/employees/manager/1` → Rahul and Anita

### 0.4 Assign the DEPARTMENT manager
`PUT http://localhost:8080/api/departments/1/manager/3`  
(no body) ✅ `departmentManagerId: 3, departmentManagerName: "Anita"`.  
Anita is department manager but Rahul's *reporting* manager is Priya – two different concepts.

Negative tests:
* `PUT http://localhost:8080/api/departments/2/manager/3` → ❌ **400** (employee belongs to another department)
* `PUT http://localhost:8080/api/departments/1/manager/99` → ❌ **404** employee not found
* `PUT http://localhost:8080/api/departments/99/manager/3` → ❌ **404** department not found
* `GET http://localhost:8080/api/departments` → no infinite JSON

---
## 1. Accounts (Step 2)
`POST http://localhost:8080/api/accounts`
```json
{ "employeeId": 4, "username": "admin.user", "password": "Admin@12345", "role": "ADMIN" }
```
```json
{ "employeeId": 1, "username": "priya", "password": "Manager@123", "role": "MANAGER" }
```
```json
{ "employeeId": 2, "username": "rahul", "password": "Employee@123", "role": "EMPLOYEE" }
```
```json
{ "username": "sysadmin", "password": "Admin@12345", "role": "ADMIN" }
```
✅ All 201 (the last is an ADMIN without an employee). Responses never contain the password. Account ids 1–4.

Negative:
* same username again → ❌ 409
* `{ "employeeId": 2, "username": "rahul2", "password": "Employee@123", "role": "EMPLOYEE" }` → ❌ 409 (one account per employee)
* `{ "username": "ghost", "password": "Employee@123", "role": "EMPLOYEE" }` → ❌ 400 (employeeId required for non-admin)
* `{ "username": "ab", "password": "123", "role": "EMPLOYEE" }` → ❌ 400 with `fieldErrors`

Deactivate / activate (no body): `PUT /api/accounts/3/deactivate` · `PUT /api/accounts/3/activate`

---
## 2. Leave types (Step 3)
`POST http://localhost:8080/api/leave-types`
```json
{ "leaveTypeName": "General Paid Leave", "isPaid": true }
```
```json
{ "leaveTypeName": "Sick Leave", "isPaid": true }
```
```json
{ "leaveTypeName": "Unpaid Leave", "isPaid": false }
```
```json
{ "leaveTypeName": "Maternity Leave", "isPaid": true }
```
→ ids 1 General, 2 Sick, 3 Unpaid, 4 Maternity. Duplicate name → ❌ 409.

---
## 3. Employee leave: policy + balance (Step 4)
`POST http://localhost:8080/api/employee-leaves`
```json
{ "employeeId": 2, "leaveTypeId": 1 }
```
✅ **Defaults**: 15 days, ANNUAL, no carry forward, EXPIRE. `leaveBalance: 15.0`. (employeeLeaveId 1 = Rahul)

Second one, with a custom policy (employeeLeaveId 2 = Priya):
```json
{ "employeeId": 1, "leaveTypeId": 1, "allocatedDays": 15, "renewalFrequency": "ANNUAL",
  "carryForwardAllowed": true, "carryForwardLimit": 5, "maximumBalance": 18, "excessLeaveAction": "ENCASH" }
```
Negative:
* same employee + type again → ❌ 409
* `{ "employeeId": 2, "leaveTypeId": 3 }` (unpaid) → ❌ 400

Ledger: `GET http://localhost:8080/api/employee-leaves/1/transactions` → one **CREDIT +15**, balanceAfter 15.

---
## 4. Leave requests (Step 5, 6, 9)
All with Rahul (employee 2, reporting manager Priya = 1).

### 4.1 Apply – Mon 30 Nov → Wed 2 Dec = 3 working days
`POST http://localhost:8080/api/leave-requests`
```json
{ "employeeId": 2, "leaveTypeId": 1, "startDate": "2026-11-30", "endDate": "2026-12-02", "reason": "Family function" }
```
✅ 201, `status: PENDING`, `totalDays: 3.0`, `reportingManagerId: 1`. Balance still 15.

### 4.2 Weekends are excluded
```json
{ "employeeId": 2, "leaveTypeId": 1, "startDate": "2026-12-04", "endDate": "2026-12-07" }
```
Fri 4 → Mon 7 = **2.0** days (Sat/Sun skipped). Then cancel it: `PUT /api/leave-requests/{thatId}/cancel` → CANCELLED.  
Only a weekend (`2026-12-05` → `2026-12-06`) → ❌ 400.

### 4.3 Negative tests
* overlap: `{ "employeeId": 2, "leaveTypeId": 1, "startDate": "2026-12-01", "endDate": "2026-12-01" }` → ❌ **409**
* `startDate` after `endDate` → ❌ **400**
* too many days: `{ "employeeId": 2, "leaveTypeId": 1, "startDate": "2026-12-07", "endDate": "2026-12-31" }` (18 working days, 25 Dec skipped) → ❌ **409** insufficient balance
* missing fields / bad date format → ❌ 400
* employee 3 with leave type 1 (no balance row) → ❌ 400

### 4.4 Half day
```json
{ "employeeId": 2, "leaveTypeId": 1, "startDate": "2026-12-09", "endDate": "2026-12-09", "totalDays": 0.5, "reason": "Doctor" }
```
✅ `totalDays: 0.5`. `totalDays: 0.3` → ❌ 400.

### 4.5 Manager inbox
`GET http://localhost:8080/api/leave-requests/manager/1/pending` → Rahul's pending requests.

### 4.6 Approve (id 1 = the 3-day request)
* Wrong person – Anita is *department* manager but not Rahul's reporting manager:  
  `PUT http://localhost:8080/api/leave-requests/1/approve?approverId=3` → ❌ **403**
* Self-approval: `…/approve?approverId=2` → ❌ 403
* Correct: `PUT http://localhost:8080/api/leave-requests/1/approve?approverId=1` → ✅ `APPROVED`
* `GET /api/employee-leaves/1` → `leaveBalance: 12.0, daysUsed: 3.0`
* `GET /api/employee-leaves/1/transactions` → CREDIT +15, then **DEBIT −3.0**, balanceAfter 12.0, with `requestId: 1`
* Approve again → ❌ 409 (not pending)

### 4.7 Reject
Reject the half-day request (id 3 if you followed the order; check with `GET /api/leave-requests/employee/2`):  
`PUT http://localhost:8080/api/leave-requests/3/reject?approverId=1` → `REJECTED`, balance unchanged.

### 4.8 Cancel an APPROVED leave (needs manager approval)
1. `PUT http://localhost:8080/api/leave-requests/1/request-cancellation` → `CANCELLATION_PENDING` (balance still 12)
2. `GET /api/leave-requests/manager/1/pending` → it appears in the manager inbox
3. `PUT http://localhost:8080/api/leave-requests/1/approve-cancellation?approverId=1` → `CANCELLED`
4. `GET /api/employee-leaves/1` → `leaveBalance: 15.0, daysUsed: 0.0`
5. Ledger shows **RESTORATION +3.0**.

(`reject-cancellation?approverId=1` instead of step 3 would put it back to `APPROVED`.)

`PUT /api/leave-requests/{id}/cancel` on an APPROVED request → ❌ 409 (use request-cancellation).

### 4.9 Unpaid leave – no balance, no transaction
```json
{ "employeeId": 2, "leaveTypeId": 3, "startDate": "2026-12-14", "endDate": "2026-12-15" }
```
Approve with `approverId=1` → `APPROVED`; the ledger gets no new row.

### 4.10 Employee with no reporting manager (assumption 1 in README)
Priya has no reporting manager:
```json
{ "employeeId": 1, "leaveTypeId": 3, "startDate": "2026-12-16", "endDate": "2026-12-16" }
```
* approve with `approverId=3` (Anita, not an admin) → ❌ 403
* approve with `approverId=4` (Admin User, active ADMIN account) → ✅

### 4.11 Inactive account
`PUT /api/accounts/3/deactivate`, then apply for Rahul → ❌ 400. Reactivate with `PUT /api/accounts/3/activate`.

---
## 5. Admin adjustment, policy change, renewal
Admin adjustment on Rahul's balance (id 1):  
`POST http://localhost:8080/api/employee-leaves/1/adjust`
```json
{ "days": 2 }
```
→ balance +2 and an **ADJUSTMENT** row. `{ "days": -100 }` → ❌ 409 (cannot go negative). `{ "days": 0 }` → ❌ 400.

Change policy only: `PUT http://localhost:8080/api/employee-leaves/1/policy`
```json
{ "allocatedDays": 15, "renewalFrequency": "ANNUAL", "carryForwardAllowed": true,
  "carryForwardLimit": 5, "maximumBalance": 20, "excessLeaveAction": "EXPIRE" }
```

**Renewal demo on Priya's record (id 2: balance 15, carry limit 5, maximum 18, ENCASH):**  
`POST http://localhost:8080/api/employee-leaves/2/renew` (no body)

Expected: final `leaveBalance: 18.0`, `daysUsed: 0.0`, and the ledger (`GET /api/employee-leaves/2/transactions`):
| # | type | days | balanceAfter |
|---|---|---|---|
| 1 | CREDIT | +15 | 15 |
| 2 | ENCASHMENT | −10 | 5 (15 − 5 carried) |
| 3 | CARRY_FORWARD | 5 | 5 (marker) |
| 4 | CREDIT | +15 | 20 |
| 5 | ENCASHMENT | −2 | 18 (maximum) |

---
## 6. Error format
Every error looks like:
```json
{ "timestamp": "...", "status": 404, "error": "Not Found",
  "message": "Employee not found with id 99", "fieldErrors": {} }
```
Validation errors fill `fieldErrors`, e.g. `{ "email": "Email must be valid" }`.

| Status | Meaning |
|---|---|
| 400 | invalid input / business rule (wrong department, bad dates, invalid manager) |
| 403 | not allowed to approve this request |
| 404 | id not found |
| 409 | duplicate, overlapping leave, insufficient balance, wrong status |
