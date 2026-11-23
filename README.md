# Online Leave Management System – Backend

Java 21 · Spring Boot · Maven · Spring Data JPA / Hibernate · PostgreSQL 16 (`leave_db`) · Lombok · Jakarta Validation

Covers Steps 1–9 of the plan (everything except Spring Security / JWT, which is Step 10).

## Run it
1. Create the database once: `CREATE DATABASE leave_db;`
2. Put your PostgreSQL password in `src/main/resources/application.properties`.
3. `./mvnw spring-boot:run` (or press Run in IntelliJ on `LeaveManagementApplication`).
   Hibernate creates/updates the 7 tables automatically (`ddl-auto=update`).
4. Test with `POSTMAN_GUIDE.md`.

Tip: `mvn package` also runs `LeaveManagementApplicationTests`, which needs PostgreSQL running. Use `-DskipTests` if it is not.

## What changed in your existing code (and why)
| Change | Reason |
|---|---|
| Controllers now return **DTOs** (`dto/` package) instead of entities | With `Department.departmentManager` the JSON would loop forever (Employee → Department → Employee → …). DTOs also stop callers from sending `manager`/`departmentManager` inside a request body. |
| `RuntimeException` → custom exceptions + `GlobalExceptionHandler` | Proper 404 / 400 / 409 / 403 instead of 500. |
| `DepartmentController` now uses a `DepartmentService` | Same pattern as Employee; business rules live in services. |
| Lombok `@Getter/@Setter/@RequiredArgsConstructor` | It was already in your pom but unused. |
| Removed `spring.jpa.database-platform` | Hibernate 7 detects PostgreSQL itself (the line only produced a warning). |
| Email is trimmed + lower-cased; names are trimmed | `Rahul@x.com` and `rahul@x.com` are the same person. |
| Reporting-manager **loop check** | A→B and B→A is prevented (small safety addition to your existing rules). |
| `existsByDepartmentName` → `existsByDepartmentNameIgnoreCase` | "IT" and "it" are the same department. |

Your existing URLs and request bodies are unchanged (`POST /api/employees?departmentId=1`, `PUT /api/employees/2/manager/1`, …).

## Package map
| Package | Contents |
|---|---|
| `entity` | 7 entities + enums (`Role`, `AccountStatus`, `LeaveStatus`, `TransactionType`, `RenewalFrequency`, `ExcessLeaveAction`) |
| `repository` | One Spring Data repository per entity |
| `dto` | Request records (with validation) and response records |
| `service` | All business rules. `LeaveTransactionService` is the **only** class allowed to change a balance. `WorkingDayService` = weekends + public holidays |
| `controller` | REST endpoints (thin – they only call services) |
| `exception` | Custom exceptions, `ErrorResponse`, `GlobalExceptionHandler` |

## Endpoints
| Area | Endpoints |
|---|---|
| Departments | `POST /api/departments` · `GET /api/departments` · `GET /api/departments/{id}` · **`PUT /api/departments/{departmentId}/manager/{employeeId}`** |
| Employees | `POST /api/employees?departmentId=` · `GET /api/employees` · `GET /api/employees/{id}` · `PUT /api/employees/{employeeId}/manager/{managerId}` · `GET /api/employees/manager/{managerId}` |
| Accounts | `POST /api/accounts` · `GET /api/accounts` · `GET /api/accounts/{id}` · `PUT /api/accounts/{id}/deactivate` · `PUT /api/accounts/{id}/activate` |
| Leave types | `POST /api/leave-types` · `GET /api/leave-types` · `GET /api/leave-types/{id}` |
| Employee leave (policy + balance) | `POST /api/employee-leaves` · `GET /api/employee-leaves` · `GET /api/employee-leaves/{id}` · `GET /api/employee-leaves/employee/{employeeId}` · `PUT /api/employee-leaves/{id}/policy` · `POST /api/employee-leaves/{id}/adjust` · `POST /api/employee-leaves/{id}/renew` · `GET /api/employee-leaves/{id}/transactions` |
| Leave requests | `POST /api/leave-requests` · `GET /api/leave-requests` · `GET /api/leave-requests/{id}` · `GET /api/leave-requests/employee/{employeeId}` · `GET /api/leave-requests/manager/{managerId}/pending` · `PUT …/{id}/approve?approverId=` · `PUT …/{id}/reject?approverId=` · `PUT …/{id}/cancel` · `PUT …/{id}/request-cancellation` · `PUT …/{id}/approve-cancellation?approverId=` · `PUT …/{id}/reject-cancellation?approverId=` |

`approverId` is the employee id of the person deciding. It exists only because there is no login yet; in Step 10 it is replaced by the logged-in user.

## How leave works
```
apply            -> PENDING            (no balance change)
approve          -> APPROVED           (ONE transaction: status + balance deduction + DEBIT row)
reject           -> REJECTED
cancel           -> CANCELLED          (only from PENDING)
request-cancellation (from APPROVED)   -> CANCELLATION_PENDING
approve-cancellation                   -> CANCELLED (ONE transaction: status + balance restore + RESTORATION row)
reject-cancellation                    -> back to APPROVED
```
* Only the employee's **reporting manager** may approve/reject – never the department manager automatically.
* Weekends and public holidays are not counted. `totalDays` is optional; send it for half days (`0.5`, `1.5`, …).
* Unpaid leave never touches a balance and never creates a transaction.
* Balance changes use a database row lock, so two simultaneous approvals cannot overspend.

## Open design questions (my assumptions – please confirm with your team)
1. **Employee with no reporting manager** (e.g. a top manager): the design says the request goes to the reporting manager "if they have one", but not what happens otherwise. I assumed an employee with an **ACTIVE ADMIN account** may decide it. Because `approverId` is an employee id, that admin needs an employee record.
2. **`approvedBy` / `approvedAt`** are also filled on rejection (no approval-history table, no separate "rejected by" column).
3. **`CARRY_FORWARD` transaction**: the carried days just stay in the balance, so this row is a marker (days = carried amount, `balanceAfter` unchanged). The design example "Carry forward +5" can be read two ways; tell me if your team means something else.
4. **Renewal** is triggered by an admin calling `/renew` (no scheduler). `renewalFrequency` is stored as policy but not acted on automatically.
5. **Public holidays** come from `leave.public-holidays` in `application.properties` (no table). `WorkingDayService` is the single place to plug in a real external source.
6. **Admin adjustments** are recorded as `ADJUSTMENT` rows, but the design has no column for *which admin* or *why*, so neither is stored.
7. A pending request does **not** reserve balance; balance is checked when applying and again when approving.
8. Approved leave can be sent for cancellation even if its dates are in the past – no rule was given.
9. No endpoint changes an employee's department or edits a leave type (not in the plan).

## Not done yet
* **Step 10 – Spring Security, login, JWT, role restrictions.** Only BCrypt hashing (`spring-security-crypto`) is included so passwords are never stored in plain text. "Inactive accounts cannot be used" is enforced where possible without login (inactive employees cannot apply, inactive approvers cannot decide).
* Automated tests beyond the default `contextLoads`.
