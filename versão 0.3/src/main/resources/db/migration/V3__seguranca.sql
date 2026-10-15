ALTER TABLE colaborador ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE colaborador ADD COLUMN mfa_secret VARCHAR(512);
ALTER TABLE colaborador ADD COLUMN mfa_recovery VARCHAR(2000);
ALTER TABLE colaborador ADD COLUMN mfa_counter BIGINT NOT NULL DEFAULT -1;
ALTER TABLE reserva ADD COLUMN expira_em DATETIME(6);
CREATE TABLE bootstrap_seguro (id INT PRIMARY KEY, concluido BOOLEAN NOT NULL);
INSERT INTO bootstrap_seguro(id,concluido) VALUES(1,false);
CREATE TABLE auditoria (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 instante DATETIME(6) NOT NULL,
 autor_id BIGINT,
 evento VARCHAR(100) NOT NULL,
 alvo VARCHAR(100) NOT NULL,
 alvo_id BIGINT,
 resultado VARCHAR(30) NOT NULL,
 detalhes VARCHAR(2000)
);
CREATE INDEX idx_auditoria_instante ON auditoria(instante);
CREATE TABLE solicitacao_publica (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 referencia VARCHAR(64) NOT NULL UNIQUE,
 chave VARCHAR(64) NOT NULL UNIQUE,
 espaco_id BIGINT, persona_id BIGINT, data DATE, hora_inicio TIME, hora_fim TIME,
 nome VARCHAR(255), cnpj VARCHAR(14), email VARCHAR(255), telefone VARCHAR(255),
 empresa VARCHAR(255), cidade VARCHAR(255), segmento VARCHAR(255), cargo VARCHAR(255),
 titulo VARCHAR(255), objetivo VARCHAR(2000), observacoes VARCHAR(2000),
 numero_participantes INT NOT NULL, inscricao_paga BOOLEAN NOT NULL,
 financiado BOOLEAN NOT NULL, academico BOOLEAN NOT NULL,
 criada_em DATETIME(6) NOT NULL, expira_em DATETIME(6) NOT NULL,
 codigo_expira_em DATETIME(6), justificativa VARCHAR(2000),
 email_confirmado BOOLEAN NOT NULL, codigo_hash VARCHAR(128), tentativas INT NOT NULL,
 estado VARCHAR(30) NOT NULL, reserva_id BIGINT
);
CREATE INDEX idx_solicitacao_estado ON solicitacao_publica(estado,expira_em);
-- Reconciliar alocacoes provisórias da versão 01: somente aprovadas ocuparão cotas.
INSERT INTO movimentacao_cota(persona_id,reserva_id,data_hora,mes_referencia,quantidade_horas,tipo,descricao)
 SELECT persona_id,reserva_id,CURRENT_TIMESTAMP,mes_referencia,-SUM(quantidade_horas),
 'LIBERACAO_PENDENTE','Migração de segurança: liberação de alocação provisória'
 FROM movimentacao_cota WHERE tipo IN ('RESERVA_PENDENTE','LIBERACAO_PENDENTE')
 GROUP BY persona_id,reserva_id,mes_referencia HAVING SUM(quantidade_horas)<>0;
UPDATE cota_mensal SET horas_reservadas_pendentes=0;
