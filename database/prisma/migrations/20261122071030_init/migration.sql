-- CreateEnum
CREATE TYPE "Role" AS ENUM ('ADMIN', 'MANAGER', 'EMPLOYEE');

-- CreateEnum
CREATE TYPE "AccountStatus" AS ENUM ('ACTIVE', 'INACTIVE');

-- CreateEnum
CREATE TYPE "RenewalFrequency" AS ENUM ('ANNUAL', 'QUARTERLY');

-- CreateEnum
CREATE TYPE "ExcessLeaveAction" AS ENUM ('EXPIRE', 'ENCASH');

-- CreateEnum
CREATE TYPE "LeaveRequestStatus" AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLATION_PENDING', 'CANCELLED');

-- CreateEnum
CREATE TYPE "TransactionType" AS ENUM ('CREDIT', 'DEBIT', 'CARRY_FORWARD', 'ADJUSTMENT', 'EXPIRY', 'ENCASHMENT', 'RESTORATION');

-- CreateTable
CREATE TABLE "Employees" (
    "emp_id" SERIAL NOT NULL,
    "name" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "department_id" INTEGER NOT NULL,
    "designation" TEXT NOT NULL,
    "manager_id" INTEGER,
    "date_of_joining" DATE NOT NULL,

    CONSTRAINT "Employees_pkey" PRIMARY KEY ("emp_id")
);

-- CreateTable
CREATE TABLE "User_Accounts" (
    "account_id" SERIAL NOT NULL,
    "emp_id" INTEGER,
    "username" TEXT NOT NULL,
    "password_hash" TEXT NOT NULL,
    "role" "Role" NOT NULL,
    "account_status" "AccountStatus" NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT "User_Accounts_pkey" PRIMARY KEY ("account_id")
);

-- CreateTable
CREATE TABLE "Departments" (
    "department_id" SERIAL NOT NULL,
    "department_name" TEXT NOT NULL,
    "department_manager_id" INTEGER,

    CONSTRAINT "Departments_pkey" PRIMARY KEY ("department_id")
);

-- CreateTable
CREATE TABLE "Leave_Types" (
    "leave_type_id" SERIAL NOT NULL,
    "leave_type_name" TEXT NOT NULL,
    "is_paid" BOOLEAN NOT NULL,

    CONSTRAINT "Leave_Types_pkey" PRIMARY KEY ("leave_type_id")
);

-- CreateTable
CREATE TABLE "Employee_Leave" (
    "employee_leave_id" SERIAL NOT NULL,
    "emp_id" INTEGER NOT NULL,
    "leave_type_id" INTEGER NOT NULL,
    "allocated_days" DECIMAL(5,2) NOT NULL,
    "leave_balance" DECIMAL(5,2) NOT NULL,
    "renewal_frequency" "RenewalFrequency" NOT NULL,
    "carry_forward_allowed" BOOLEAN NOT NULL DEFAULT false,
    "carry_forward_limit" DECIMAL(5,2),
    "maximum_balance" DECIMAL(5,2),
    "excess_leave_action" "ExcessLeaveAction",
    "days_used" DECIMAL(5,2) NOT NULL DEFAULT 0,

    CONSTRAINT "Employee_Leave_pkey" PRIMARY KEY ("employee_leave_id")
);

-- CreateTable
CREATE TABLE "Leave_Requests" (
    "request_id" SERIAL NOT NULL,
    "emp_id" INTEGER NOT NULL,
    "leave_type_id" INTEGER NOT NULL,
    "start_date" DATE NOT NULL,
    "end_date" DATE NOT NULL,
    "total_days" DECIMAL(5,2) NOT NULL,
    "reason" TEXT NOT NULL,
    "status" "LeaveRequestStatus" NOT NULL DEFAULT 'PENDING',
    "applied_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "approved_by" INTEGER,
    "approved_at" TIMESTAMP(3),
    "cancellation_requested_at" TIMESTAMP(3),
    "cancellation_approved_by" INTEGER,
    "cancelled_at" TIMESTAMP(3),

    CONSTRAINT "Leave_Requests_pkey" PRIMARY KEY ("request_id")
);

-- CreateTable
CREATE TABLE "Leave_Transactions" (
    "transaction_id" SERIAL NOT NULL,
    "emp_id" INTEGER NOT NULL,
    "leave_type_id" INTEGER NOT NULL,
    "request_id" INTEGER,
    "transaction_type" "TransactionType" NOT NULL,
    "days" DECIMAL(5,2) NOT NULL,
    "balance_after" DECIMAL(5,2),
    "transaction_date" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Leave_Transactions_pkey" PRIMARY KEY ("transaction_id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Employees_email_key" ON "Employees"("email");

-- CreateIndex
CREATE UNIQUE INDEX "User_Accounts_emp_id_key" ON "User_Accounts"("emp_id");

-- CreateIndex
CREATE UNIQUE INDEX "User_Accounts_username_key" ON "User_Accounts"("username");

-- CreateIndex
CREATE UNIQUE INDEX "Departments_department_name_key" ON "Departments"("department_name");

-- CreateIndex
CREATE UNIQUE INDEX "Departments_department_manager_id_key" ON "Departments"("department_manager_id");

-- CreateIndex
CREATE UNIQUE INDEX "Leave_Types_leave_type_name_key" ON "Leave_Types"("leave_type_name");

-- CreateIndex
CREATE UNIQUE INDEX "Employee_Leave_emp_id_leave_type_id_key" ON "Employee_Leave"("emp_id", "leave_type_id");

-- AddForeignKey
ALTER TABLE "Employees" ADD CONSTRAINT "Employees_department_id_fkey" FOREIGN KEY ("department_id") REFERENCES "Departments"("department_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Employees" ADD CONSTRAINT "Employees_manager_id_fkey" FOREIGN KEY ("manager_id") REFERENCES "Employees"("emp_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "User_Accounts" ADD CONSTRAINT "User_Accounts_emp_id_fkey" FOREIGN KEY ("emp_id") REFERENCES "Employees"("emp_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Departments" ADD CONSTRAINT "Departments_department_manager_id_fkey" FOREIGN KEY ("department_manager_id") REFERENCES "Employees"("emp_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Employee_Leave" ADD CONSTRAINT "Employee_Leave_emp_id_fkey" FOREIGN KEY ("emp_id") REFERENCES "Employees"("emp_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Employee_Leave" ADD CONSTRAINT "Employee_Leave_leave_type_id_fkey" FOREIGN KEY ("leave_type_id") REFERENCES "Leave_Types"("leave_type_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Requests" ADD CONSTRAINT "Leave_Requests_emp_id_fkey" FOREIGN KEY ("emp_id") REFERENCES "Employees"("emp_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Requests" ADD CONSTRAINT "Leave_Requests_leave_type_id_fkey" FOREIGN KEY ("leave_type_id") REFERENCES "Leave_Types"("leave_type_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Requests" ADD CONSTRAINT "Leave_Requests_approved_by_fkey" FOREIGN KEY ("approved_by") REFERENCES "Employees"("emp_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Requests" ADD CONSTRAINT "Leave_Requests_cancellation_approved_by_fkey" FOREIGN KEY ("cancellation_approved_by") REFERENCES "Employees"("emp_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Transactions" ADD CONSTRAINT "Leave_Transactions_emp_id_fkey" FOREIGN KEY ("emp_id") REFERENCES "Employees"("emp_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Transactions" ADD CONSTRAINT "Leave_Transactions_leave_type_id_fkey" FOREIGN KEY ("leave_type_id") REFERENCES "Leave_Types"("leave_type_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Leave_Transactions" ADD CONSTRAINT "Leave_Transactions_request_id_fkey" FOREIGN KEY ("request_id") REFERENCES "Leave_Requests"("request_id") ON DELETE SET NULL ON UPDATE CASCADE;
