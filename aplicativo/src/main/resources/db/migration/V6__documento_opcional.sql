-- Documento do empreendedor (22/10/2026): CNPJ, CPF, documento estrangeiro ou sem documento.
-- Migration aditiva: a coluna cnpj continua com o mesmo nome e os mesmos valores (agora guarda o
-- número de qualquer tipo), passa a aceitar vazio e até 30 caracteres; o número segue único.
-- Cadastros e pedidos já gravados ficam marcados como CNPJ.
ALTER TABLE empreendedor MODIFY COLUMN cnpj VARCHAR(30) NULL;
ALTER TABLE empreendedor ADD COLUMN tipo_documento VARCHAR(20);
UPDATE empreendedor SET tipo_documento = 'CNPJ' WHERE cnpj IS NOT NULL;

ALTER TABLE solicitacao_publica MODIFY COLUMN cnpj VARCHAR(30) NULL;
ALTER TABLE solicitacao_publica ADD COLUMN tipo_documento VARCHAR(20);
UPDATE solicitacao_publica SET tipo_documento = 'CNPJ' WHERE cnpj IS NOT NULL AND cnpj <> '';
