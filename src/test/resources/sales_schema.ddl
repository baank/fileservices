CREATE TABLE sales (
    transaction_date TIMESTAMP
    ,product NVARCHAR(10)
    ,price INTEGER
    ,payment_type NVARCHAR(20)
    ,name NVARCHAR(20)
    ,city NVARCHAR(50)
    ,state NVARCHAR(20)
    ,country NVARCHAR(20)
    ,account_created TIMESTAMP
    ,last_login TIMESTAMP
    ,latitude NUMERIC
    ,longitude NUMERIC
    ,src_date DATE
);