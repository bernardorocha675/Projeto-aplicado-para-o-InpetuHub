-- Execute como administrador do MySQL. Substitua a senha de exemplo antes de executar.
CREATE DATABASE IF NOT EXISTS inpetu_hub_v02 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'inpetu'@'localhost' IDENTIFIED BY 'TROQUE_ESTA_SENHA_LOCAL';
GRANT ALL PRIVILEGES ON inpetu_hub_v02.* TO 'inpetu'@'localhost';
