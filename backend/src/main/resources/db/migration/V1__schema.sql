CREATE TABLE departments (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(40) UNIQUE NOT NULL, prefix CHAR(1) UNIQUE NOT NULL);
CREATE TABLE roles (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(60) UNIQUE NOT NULL);
CREATE TABLE employee_code_sequences (department_id BIGINT PRIMARY KEY REFERENCES departments(id), last_number BIGINT NOT NULL DEFAULT 0 CHECK(last_number>=0));
CREATE TABLE employees (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 employee_code VARCHAR(30) UNIQUE NOT NULL,
 full_name VARCHAR(100) NOT NULL,
 email VARCHAR(160) NOT NULL,
 normalized_email VARCHAR(160) UNIQUE NOT NULL,
 mobile VARCHAR(10) NOT NULL CHECK(mobile ~ '^[0-9]{10}$'),
 department_id BIGINT NOT NULL REFERENCES departments(id),
 role_id BIGINT NOT NULL REFERENCES roles(id),
 joining_date DATE NOT NULL,
 annual_salary NUMERIC(14,2) NOT NULL CHECK(annual_salary>0),
 status VARCHAR(10) NOT NULL CHECK(status IN ('Active','Inactive')),
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE employee_personal_details (
 employee_id BIGINT PRIMARY KEY REFERENCES employees(id),
 aadhaar_ciphertext TEXT NOT NULL, pan_ciphertext TEXT NOT NULL,
 date_of_birth DATE NOT NULL, gender VARCHAR(20) NOT NULL CHECK(gender IN ('Male','Female','Other')),
 address VARCHAR(500) NOT NULL
);
CREATE TABLE users (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 username VARCHAR(100) UNIQUE NOT NULL, password_hash VARCHAR(100) NOT NULL,
 access_role VARCHAR(10) NOT NULL CHECK(access_role IN ('Admin','Manager','Employee')),
 employee_id BIGINT UNIQUE REFERENCES employees(id), is_active BOOLEAN NOT NULL DEFAULT true,
 CHECK(access_role <> 'Employee' OR employee_id IS NOT NULL)
);
CREATE TABLE employee_transfer_history (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, employee_id BIGINT NOT NULL REFERENCES employees(id),
 old_department_id BIGINT NOT NULL REFERENCES departments(id), new_department_id BIGINT NOT NULL REFERENCES departments(id),
 old_employee_code VARCHAR(30) NOT NULL, new_employee_code VARCHAR(30) NOT NULL,
 transfer_date DATE NOT NULL, reason VARCHAR(500) NOT NULL,
 performed_by_user_id BIGINT NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK(old_department_id<>new_department_id)
);
CREATE INDEX idx_employee_department ON employees(department_id);
CREATE INDEX idx_employee_role ON employees(role_id);
CREATE INDEX idx_employee_status ON employees(status);
CREATE INDEX idx_employee_joining ON employees(joining_date);
CREATE INDEX idx_employee_salary ON employees(annual_salary);
CREATE INDEX idx_history_employee ON employee_transfer_history(employee_id, transfer_date DESC);
INSERT INTO departments(name,prefix) VALUES ('IT','I'),('HR','H'),('Finance','F'),('Sales','S'),('Admin','A');
INSERT INTO roles(name) VALUES ('Software Developer'),('Team Lead'),('Operations Executive'),('Analyst');
INSERT INTO employee_code_sequences(department_id) SELECT id FROM departments;
