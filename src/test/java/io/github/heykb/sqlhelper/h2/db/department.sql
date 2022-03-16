DROP TABLE IF EXISTS department;
CREATE TABLE department (
  dept_id varchar(255)  NOT NULL,
  dept_name varchar(255) ,
  tenant_id varchar(255) ,
  created_time timestamp(6),
  created_by varchar(255) ,
  updated_time timestamp(6),
  updated_by varchar(255) ,
)
;