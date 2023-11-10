## 简单语句条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT *
FROM people

-- ⇊

SELECT *
FROM people
WHERE people.tenant_id = 1
```
## leftJoinTest条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT p.*
FROM people p
	LEFT JOIN dept d ON p.dept_id = d.dept_id

-- ⇊

SELECT p.*
FROM people p
	LEFT JOIN dept d
	ON p.dept_id = d.dept_id
		AND d.tenant_id = 1
WHERE p.tenant_id = 1
```
## rightJoinTest条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT p.*
FROM people p
	RIGHT JOIN dept d ON p.dept_id = d.dept_id

-- ⇊

SELECT p.*
FROM people p
	RIGHT JOIN dept d
	ON p.dept_id = d.dept_id
		AND p.tenant_id = 1
WHERE d.tenant_id = 1
```
## innerJoinTest条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT p.*
FROM people p
	JOIN dept d ON p.dept_id = d.dept_id

-- ⇊

SELECT p.*
FROM people p
	JOIN dept d ON p.dept_id = d.dept_id
WHERE p.tenant_id = 1
	AND d.tenant_id = 1
```
## outerJoinTest条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT p.*
FROM people p
	FULL JOIN dept d ON p.dept_id = d.dept_id

-- ⇊

SELECT p.*
FROM people p
	FULL JOIN dept d
	ON p.dept_id = d.dept_id
		AND p.tenant_id = 1
		AND d.tenant_id = 1
```
## in 条件注入
```sql
-- [mysql] [columnName=dept_id] [op="in"] [value=(1,2,3)]
SELECT *
FROM people

-- ⇊

SELECT *
FROM people
WHERE people.dept_id IN (1, 2, 3)
```
## in 子查询条件注入
```sql
-- [mysql] [columnName=dept_id] [op="in"] [value=(select dept_id from dept where user_id=1)]
SELECT *
FROM people

-- ⇊

SELECT *
FROM people
WHERE people.dept_id IN (
	SELECT dept_id
	FROM dept
	WHERE user_id = 1
)
```
## 多表删除转逻辑删除
```sql
-- [mysql] [columnName=is_deleted] [op="="] [value=false]
DELETE mv
FROM mv
	LEFT JOIN track ON mv.mvid = track.trkid
WHERE track.trkid IS NULL

-- ⇊

UPDATE mv
	LEFT JOIN track
	ON mv.mvid = track.trkid
		AND track.is_deleted = false
SET mv.is_deleted = true
WHERE track.trkid IS NULL
	AND mv.is_deleted = false
```
## 有别名转逻辑删除
```sql
-- [mysql] [columnName=is_deleted] [op="="] [value=false]
DELETE FROM mv s
WHERE s.trkid IS NULL

-- ⇊

UPDATE mv s
SET s.is_deleted = true
WHERE s.trkid IS NULL
	AND s.is_deleted = false
```
## 无别名转逻辑删除
```sql
-- [mysql] [columnName=is_deleted] [op="="] [value=false]
DELETE FROM mv
WHERE trkid IS NULL

-- ⇊

UPDATE mv
SET is_deleted = true
WHERE trkid IS NULL
	AND mv.is_deleted = false
```
## update 不明set Item(替换set Item)条件和Update注入
```sql
-- [postgresql] [columnName=tenant_id] [op="="] [value=1]
UPDATE tb t
SET tenant_id = m._seqnum
FROM (
	(SELECT m.idFieldName, row_number() OVER (ORDER BY m.sortSnFieldName, m.updatedTimeFieldName) AS _seqnum
	FROM tb m
	WHERE m.parentIdFieldName IS NULL)
) m
WHERE t.idFieldName = m.idFieldName

-- ⇊

UPDATE tb t
SET tenant_id = 1
FROM (
	(SELECT m.idFieldName, row_number() OVER (ORDER BY m.sortSnFieldName, m.updatedTimeFieldName) AS _seqnum
	FROM tb m
	WHERE m.parentIdFieldName IS NULL
		AND m.tenant_id = 1)
) m
WHERE t.idFieldName = m.idFieldName
	AND t.tenant_id = 1
```
## update 不明set Item.条件和Update注入
```sql
-- [postgresql] [columnName=tenant_id] [op="="] [value=1]
UPDATE tb t
SET sort_sn = m._seqnum
FROM (
	(SELECT m.id, row_number() OVER (ORDER BY m.sort_sn, m.updated_time DESC) AS _seqnum
	FROM "tb" m
	WHERE m.parent IS NULL)
) m
WHERE t.id = m.id

-- ⇊

UPDATE tb t
SET sort_sn = m._seqnum, tenant_id = 1
FROM (
	(SELECT m.id, row_number() OVER (ORDER BY m.sort_sn, m.updated_time DESC) AS _seqnum
	FROM "tb" m
	WHERE m.parent IS NULL
		AND m.tenant_id = 1)
) m
WHERE t.id = m.id
	AND t.tenant_id = 1
```
## select unix_timestamp条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
SELECT unix_timestamp(current_timestamp()) * 1000 AS c_timestamp
```
## 多表Delete4条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
DELETE FROM mv
USING mv
	LEFT JOIN track ON mv.mvid = track.trkid
WHERE track.trkid IS NULL

-- ⇊

DELETE FROM mv
USING mv
	LEFT JOIN track
	ON mv.mvid = track.trkid
		AND track.tenant_id = 1
WHERE track.trkid IS NULL
	AND mv.tenant_id = 1
```
## 多表Delete3条件注入
```sql
-- [sqlserver] [columnName=tenant_id] [op="="] [value=1]
DELETE mv FROM mv
	LEFT JOIN track ON mv.mvid = track.trkid
WHERE track.trkid IS NULL

-- ⇊

DELETE mv FROM mv
	LEFT JOIN track
	ON mv.mvid = track.trkid
		AND track.tenant_id = 1
WHERE track.trkid IS NULL
	AND mv.tenant_id = 1
```
## 多表Delete2条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
DELETE FROM mv
USING mv, track
WHERE track.trkid = mv.mvid

-- ⇊

DELETE FROM mv
USING mv, track
WHERE track.trkid = mv.mvid
	AND mv.tenant_id = 1
	AND track.tenant_id = 1
```
## 多表Delete1条件注入
```sql
-- [sqlserver] [columnName=tenant_id] [op="="] [value=1]
DELETE mv FROM mv, track
WHERE track.trkid = mv.mvid

-- ⇊

DELETE mv FROM mv, track
WHERE track.trkid = mv.mvid
	AND mv.tenant_id = 1
	AND track.tenant_id = 1
```
## 多表Update6条件注入
```sql
-- [sqlserver] [columnName=tenant_id] [op="="] [value=1]
UPDATE s, c
SET s.class_name = 'test00', c.stu_name = 'test00'
FROM student s, class c
WHERE s.class_id = c.id

-- ⇊

UPDATE s, c
SET s.class_name = 'test00', c.stu_name = 'test00', s.tenant_id = 1, c.tenant_id = 1
FROM student s, class c
WHERE s.class_id = c.id
	AND s.tenant_id = 1
	AND c.tenant_id = 1
```
## 多表Update5条件注入
```sql
-- [sqlserver] [columnName=tenant_id] [op="="] [value=1]
UPDATE s, c
SET s.class_name = 'test00', c.stu_name = 'test00'
FROM student s, class c
WHERE s.class_id = c.id

-- ⇊

UPDATE s, c
SET s.class_name = 'test00', c.stu_name = 'test00'
FROM student s, class c
WHERE s.class_id = c.id
	AND s.tenant_id = 1
	AND c.tenant_id = 1
```
## 多表Update4条件注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
UPDATE student s, class c
SET s.class_name = 'test00', c.stu_name = 'test00'
WHERE s.class_id = c.id

-- ⇊

UPDATE student s, class c
SET s.class_name = 'test00', c.stu_name = 'test00'
WHERE s.class_id = c.id
	AND s.tenant_id = 1
	AND c.tenant_id = 1
```
## 多表Update3条件注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
UPDATE student s
JOIN class c ON s.class_id = c.id 
SET s.class_name = 'test11'

-- ⇊

UPDATE student s
JOIN class c ON s.class_id = c.id 
SET s.class_name = 'test11'
WHERE s.tenant_id = 1
	AND c.tenant_id = 1
```
## 多表Update2条件注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
UPDATE student s
LEFT JOIN class c ON s.class_id = c.id 
SET s.class_name = 'test22', c.stu_name = 'test22'

-- ⇊

UPDATE student s
LEFT JOIN class c ON s.class_id = c.id
	AND c.tenant_id = 1 
SET s.class_name = 'test22', c.stu_name = 'test22'
WHERE s.tenant_id = 1
```
## 多表Update1条件注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
UPDATE student
LEFT JOIN class ON student.class_id = class.id 
SET student.class_name = 'test22', class.stu_name = 'test22'

-- ⇊

UPDATE student
LEFT JOIN class ON student.class_id = class.id
	AND class.tenant_id = 1 
SET student.class_name = 'test22', class.stu_name = 'test22'
WHERE student.tenant_id = 1
```
## 多表Insert first when。条件和Insert注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
INSERT FIRST 
	WHEN object_id > 5 THEN
		INTO suppliers
			(supplier_id, supplier_name)
		VALUES (?, 'IBM')
	WHEN object_id > 10 THEN
		INTO suppliers
			(supplier_id, supplier_name)
		VALUES (?, ?)
	ELSE
		INTO customers
			(customer_id, customer_name, city)
		VALUES (999999, 'Anderson Construction', 'New York')
SELECT object_id
FROM t;

-- ⇊

INSERT FIRST 
	WHEN object_id > 5 THEN
		INTO suppliers
			(supplier_id, supplier_name, tenant_id)
		VALUES (?, 'IBM', 1)
	WHEN object_id > 10 THEN
		INTO suppliers
			(supplier_id, supplier_name, tenant_id)
		VALUES (?, ?, 1)
	ELSE
		INTO customers
			(customer_id, customer_name, city, tenant_id)
		VALUES (999999, 'Anderson Construction', 'New York', 1)
SELECT object_id
FROM t
WHERE t.tenant_id = 1;
```
## 多表Insert all when。条件和Insert注入
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
INSERT ALL 
	WHEN object_id > 5 THEN
		INTO suppliers
			(supplier_id, supplier_name)
		VALUES (?, 'IBM')
	WHEN object_id > 10 THEN
		INTO suppliers
			(supplier_id, supplier_name)
		VALUES (?, ?)
	ELSE
		INTO customers
			(customer_id, customer_name, city)
		VALUES (999999, 'Anderson Construction', 'New York')
SELECT object_id
FROM t;

-- ⇊

INSERT ALL 
	WHEN object_id > 5 THEN
		INTO suppliers
			(supplier_id, supplier_name, tenant_id)
		VALUES (?, 'IBM', 1)
	WHEN object_id > 10 THEN
		INTO suppliers
			(supplier_id, supplier_name, tenant_id)
		VALUES (?, ?, 1)
	ELSE
		INTO customers
			(customer_id, customer_name, city, tenant_id)
		VALUES (999999, 'Anderson Construction', 'New York', 1)
SELECT object_id
FROM t
WHERE t.tenant_id = 1;
```
## 多表Insert all
```sql
-- [oracle] [columnName=tenant_id] [op="="] [value=1]
INSERT ALL 
	INTO suppliers
		(supplier_id, supplier_name)
	VALUES (1000, 'IBM')
	INTO suppliers
		(supplier_id, supplier_name)
	VALUES (2000, 'Microsoft')
	INTO suppliers
		(supplier_id, supplier_name)
	VALUES (3000, 'Google')
SELECT *
FROM dual;

-- ⇊

INSERT ALL 
	INTO suppliers
		(supplier_id, supplier_name, tenant_id)
	VALUES (1000, 'IBM', 1)
	INTO suppliers
		(supplier_id, supplier_name, tenant_id)
	VALUES (2000, 'Microsoft', 1)
	INTO suppliers
		(supplier_id, supplier_name, tenant_id)
	VALUES (3000, 'Google', 1)
SELECT *
FROM dual
WHERE dual.tenant_id = 1;
```
## qq用户（732811911）提交条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
UPDATE chart_view cv, chart_view_cache cve
SET cv.` NAME ` = cve.` NAME `, cv.title = cve.title, cv.scene_id = cve.scene_id
WHERE cve.ID = cv.ID
	AND cv.ID IN (?, ?)

-- ⇊

UPDATE chart_view cv, chart_view_cache cve
SET cv.` NAME ` = cve.` NAME `, cv.title = cve.title, cv.scene_id = cve.scene_id
WHERE cve.ID = cv.ID
	AND cv.ID IN (?, ?)
	AND cv.tenant_id = 1
	AND cve.tenant_id = 1
```
## 更新语句set value中的查询。条件和Update注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
UPDATE Persons
SET PersonCityName = (
	SELECT AddressList.PostCode
	FROM AddressList
	WHERE AddressList.PersonId = Persons.PersonId
)

-- ⇊

UPDATE Persons
SET PersonCityName = (
	SELECT AddressList.PostCode
	FROM AddressList
	WHERE AddressList.PersonId = Persons.PersonId
		AND AddressList.tenant_id = 1
), tenant_id = 1
WHERE Persons.tenant_id = 1
```
## queryInInsert条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
INSERT INTO Customers (CustomerName, City, Country)
SELECT SupplierName, City, Country
FROM Suppliers s

-- ⇊

INSERT INTO Customers (CustomerName, City, Country)
SELECT SupplierName, City, Country
FROM Suppliers s
WHERE s.tenant_id = 1
```
## queryInInsert条件注入
```sql
-- [mysql] [columnName=tenant_id] [op="="] [value=1]
INSERT INTO Customers (CustomerName, City, Country)
SELECT SupplierName, City, Country
FROM Suppliers

-- ⇊

INSERT INTO Customers (CustomerName, City, Country)
SELECT SupplierName, City, Country
FROM Suppliers
WHERE Suppliers.tenant_id = 1
```
