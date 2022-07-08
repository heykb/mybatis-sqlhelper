create table employees (
    id integer not null,
    is_deleted char(1) default 'N' not null
);
insert into employees(id) VALUES (1);
insert into employees(id) VALUES (2);