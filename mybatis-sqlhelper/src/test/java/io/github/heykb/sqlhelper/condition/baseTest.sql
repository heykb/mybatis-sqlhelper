DROP TABLE IF EXISTS people;
CREATE TABLE people (
  name varchar(255) ,
  age int,
  email varchar(255) ,
  id varchar(255)  NOT NULL,
  tenant_id varchar(255),
  dept_id varchar(255),
  created_time timestamp(6),
  created_by varchar(255) ,
  updated_time timestamp(6),
  updated_by varchar(255),
  is_deleted char(1) default 'N' not null,
  CONSTRAINT people_pkey PRIMARY KEY (id)
)
;


INSERT INTO people VALUES ('tony', 10, 'tony@qq.com', '1', 'tenant_1', 'dept1', '2021-11-12 12:18:14.235029', 'admin', NULL, 'admin','N');
INSERT INTO people VALUES ('tom', 20, 'tom@qq.com', '2', 'tenant_2', 'dept2', '2021-11-12 12:18:14.235029', 'admin', NULL, 'admin','N');

