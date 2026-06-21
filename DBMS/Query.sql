-- Departments Table
CREATE TABLE departments
    (department_id SERIAL PRIMARY KEY, 
     department_name VARCHAR(50) UNIQUE NOT NULL);

-- Insert records into departments
INSERT INTO departments(department_name) VALUES 
            ('Sales'), ('Engineering'), ('Human Resourses');

-- Employees Table
CREATE TABLE employees
     (employee_id SERIAL PRIMARY KEY,
	 first_name VARCHAR(50) NOT NULL,
	 last_name VARCHAR(50) NOT NULL,
	 department_id INT, 
	 salary DECIMAL(10,2) NOT NULL,
	 joining_date DATE NOT NULL,
	 FOREIGN KEY(department_id) REFERENCES departments(department_id));		

-- Insert records for employees
INSERT INTO employees(first_name, last_name, department_id, salary, joining_date)
            VALUES ('Somya', 'Rawat', 1, 48000.00, '2024-07-10'),
			       ('Kartikey', 'Singh', 1, 58000.00, '2021-05-20'),
			       ('Harshita', 'Pundhir', 2, 60000.00, '2020-03-11'),
				   ('Sambhav', 'Tiwari', 2, 59000.00, '2023-04-19'),
                   ('Luv', 'Sharma', 3, 63000.00, '2021-01-01'),
				   ('Kush', 'Rawat', 3, 64000.00, '2020-12-12');

-- Projects Table
CREATE TABLE projects
     (project_id SERIAL PRIMARY KEY,
	 project_name VARCHAR(50) NOT NULL,
	 department_id INT,
	 status VARCHAR(50) DEFAULT 'ongoing',
	 FOREIGN KEY(department_id) REFERENCES departments(department_id));	

-- Insert records for projects 
INSERT INTO projects(project_name, department_id, status) VALUES
            ('Payroll System', 1, 'ongoing'),
			('Address Book', 2, 'completed'),
			('Book Store', 3, 'ongoing');

-- Basic Commands
SELECT * FROM employees;

INSERT INTO departments(department_name) VALUES ('Human Resources');

UPDATE employees SET salary = 60000 WHERE employee_id = 5;

DELETE FROM projects WHERE project_name = 'Outdated Project';


-- Clauses and Constraints
SELECT first_name, last_name FROM employees WHERE joining_date > '2020-12-31';

SELECT * FROM employees WHERE salary BETWEEN 40000 AND 70000;

SELECT * FROM employees WHERE department_id = 1;

SELECT * FROM Employees WHERE first_name LIKE 'A%';

SELECT * FROM Employees ORDER BY salary DESC LIMIT 3;

SELECT * FROM Projects WHERE status <> 'completed';


-- Joins
SELECT e.employee_id, e.first_name, e.last_name, d.department_name
FROM Employees e
JOIN Departments d ON 
e.department_id = d.department_id;

SELECT p.project_name, d.department_name
FROM Projects p
JOIN Departments d ON 
p.department_id = d.department_id;

SELECT e.employee_id, e.first_name, e.last_name
FROM Employees e
JOIN projects p ON 
e.department_id = p.project_id
WHERE p.project_name = 'New Project';


-- Aggregate Functions
SELECT d.department_name, SUM(e.salary) AS total_salary
FROM Employees e
JOIN Departments d ON e.department_id = d.department_id
GROUP BY d.department_name;

SELECT AVG(salary) FROM Employees;

SELECT d.department_name, COUNT(e.employee_id)
FROM Departments d
LEFT JOIN Employees e ON 
d.department_id = e.department_id
GROUP BY d.department_name;


-- Subqueries
SELECT * FROM Employees WHERE salary = ( SELECT MAX(salary) FROM Employees );

SELECT department_id FROM Employees GROUP BY department_id HAVING COUNT(*) > 5;

SELECT * FROM Employees WHERE salary > ( SELECT AVG(salary) FROM Employees );


-- Constraints and Validation
ALTER TABLE Projects ADD CONSTRAINT unique_project_name UNIQUE (project_name);

ALTER TABLE Projects DROP COLUMN status;


-- Complex Queries
SELECT * FROM Employees WHERE department_id = NULL;

SELECT * FROM Employees WHERE joining_date <= CURRENT_DATE - INTERVAL '5 years';

SELECT d.department_name FROM Departments d
LEFT JOIN Projects p ON 
d.department_id = p.department_id
WHERE p.project_id = NULL;


-- Advanced Filtering
SELECT * FROM Employees WHERE last_name LIKE '%S%';

SELECT * FROM Projects WHERE project_name LIKE '%System';


-- Data Ordering
SELECT * FROM Employees ORDER BY joining_date DESC;

SELECT * FROM Employees ORDER BY salary ASC;


-- Group By and Having
SELECT department_id, COUNT(*) AS employee_count FROM Employees
GROUP BY department_id HAVING COUNT(*) > 10;

-- ALTER TABLE Projects
-- ADD COLUMN status VARCHAR(20) DEFAULT 'ongoing';

-- UPDATE Projects SET status = 'ongoing' WHERE status IS NULL;

SELECT d.department_name, COUNT(p.project_id) AS ongoing_projects
FROM Departments d
JOIN Projects p ON 
d.department_id = p.department_id
WHERE p.status = 'ongoing'
GROUP BY d.department_name;