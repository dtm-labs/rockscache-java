create table employee(
    id bigint,
    first_name varchar(20) not null,
    last_name varchar(20) not null
) engine=innodb;

alter table employee
    add constraint pk_employee
        primary key(id);

insert into employee(id, first_name, last_name) values
    (1, 'Nick', 'Fury'),
    (2, 'Phil', 'Coulson'),
    (3, 'Clint', 'Barton'),
    (4, 'Maria', 'Hill'),
    (5, 'Melinda', 'May'),
    (6, 'Daisy', 'Johnson'),
    (7, 'Grant', 'Ward'),
    (8, 'Leopold', 'Fitz'),
    (9, 'Jemma', 'Simmons')
;