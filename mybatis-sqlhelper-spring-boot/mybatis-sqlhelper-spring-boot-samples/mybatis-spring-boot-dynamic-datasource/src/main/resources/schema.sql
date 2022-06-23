
drop table if exists city;
drop table if exists hotel;

create table city (id int primary key auto_increment, name varchar, state varchar, country varchar,tenant_id varchar);
create table hotel (city int, name varchar, address varchar, zip varchar,is_deleted char(1));
