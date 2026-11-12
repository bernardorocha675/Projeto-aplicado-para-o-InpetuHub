-- Modelo do InPETU Hub 1.x, consolidado das migrations V1 a V6 do sistema antigo (aplicativo/, linha 0.5.x).
-- Começa limpo (decisão 4 do plano): nada é importado do banco antigo.
-- Diferenças em relação ao antigo: o número do documento se chama "documento" (antes "cnpj") e vem com
-- "tipo_documento"; não há colunas de MFA nem a tabela importacao_conta; o espaço já nasce com "mobiliario".
-- O mesmo SQL roda no MySQL e no H2 em modo MySQL.

CREATE TABLE empreendedor (
    id_empreendedor BIGINT AUTO_INCREMENT PRIMARY KEY,
    documento VARCHAR(30) NULL UNIQUE,
    tipo_documento VARCHAR(20),
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefone VARCHAR(255),
    cidade VARCHAR(255),
    empresa VARCHAR(255),
    segmento VARCHAR(255),
    cargo VARCHAR(255),
    ativo BOOLEAN NOT NULL
);

CREATE TABLE espaco (
    id_espaco BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_espaco VARCHAR(255) NOT NULL,
    capacidade INT NOT NULL,
    mobiliario VARCHAR(255),
    disponibilidade BOOLEAN NOT NULL,
    ativo BOOLEAN NOT NULL,
    CHECK (capacidade > 0)
);

CREATE TABLE colaborador (
    id_colaborador BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255),
    email VARCHAR(255),
    login VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(255),
    ativo BOOLEAN NOT NULL,
    security_version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE persona (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(255) NOT NULL UNIQUE,
    nome VARCHAR(255),
    descricao VARCHAR(255),
    cota_mensal_horas DECIMAL(10, 2)
);

CREATE TABLE configuracao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    horas_por_turno INT NOT NULL,
    abertura TIME,
    fechamento TIME,
    taxa_operacao DECIMAL(12, 2),
    taxa_limpeza DECIMAL(12, 2),
    insumos_limpeza DECIMAL(12, 2)
);

CREATE TABLE feriado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data DATE UNIQUE,
    descricao VARCHAR(255),
    ativo BOOLEAN NOT NULL
);

CREATE TABLE tabela_preco (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ano INT NOT NULL,
    quantidade_turnos INT NOT NULL,
    percentual INT NOT NULL,
    valor DECIMAL(12, 2),
    ativo BOOLEAN NOT NULL,
    UNIQUE (ano, quantidade_turnos, percentual)
);

CREATE TABLE reserva (
    id_reserva BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_empreendedor BIGINT NOT NULL,
    id_espaco BIGINT NOT NULL,
    persona_id BIGINT NOT NULL,
    id_colaborador_responsavel BIGINT,
    data DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    data_hora_solicitacao DATETIME(6),
    expira_em DATETIME(6),
    status_reserva VARCHAR(255),
    status_pagamento VARCHAR(255),
    titulo VARCHAR(255),
    objetivo VARCHAR(2000),
    observacoes VARCHAR(2000),
    motivo_recusa VARCHAR(2000),
    numero_participantes INT NOT NULL,
    inscricao_paga BOOLEAN NOT NULL,
    financiado BOOLEAN NOT NULL,
    academico BOOLEAN NOT NULL,
    usa_cota BOOLEAN NOT NULL,
    horas_cota DECIMAL(10, 2),
    valor_base DECIMAL(12, 2),
    valor_final DECIMAL(12, 2),
    valor_sinal DECIMAL(12, 2),
    valor_restante DECIMAL(12, 2),
    percentual_sinal INT NOT NULL,
    percentual_locacao INT NOT NULL,
    percentual_operacao INT NOT NULL,
    percentual_limpeza INT NOT NULL,
    valor_operacao DECIMAL(12, 2),
    valor_limpeza DECIMAL(12, 2),
    taxas_configuradas BOOLEAN NOT NULL,
    CHECK (hora_fim > hora_inicio),
    CHECK (numero_participantes > 0),
    FOREIGN KEY (id_empreendedor) REFERENCES empreendedor (id_empreendedor),
    FOREIGN KEY (id_espaco) REFERENCES espaco (id_espaco),
    FOREIGN KEY (persona_id) REFERENCES persona (id),
    FOREIGN KEY (id_colaborador_responsavel) REFERENCES colaborador (id_colaborador)
);

CREATE TABLE atendimento (
    id_atendimento BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_empreendedor BIGINT NOT NULL,
    id_colaborador BIGINT NOT NULL,
    data DATE,
    descricao VARCHAR(2000),
    observacoes VARCHAR(2000),
    encaminhamentos VARCHAR(2000),
    FOREIGN KEY (id_empreendedor) REFERENCES empreendedor (id_empreendedor),
    FOREIGN KEY (id_colaborador) REFERENCES colaborador (id_colaborador)
);

CREATE TABLE visita (
    id_visita BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_empreendedor BIGINT NOT NULL,
    id_colaborador BIGINT NOT NULL,
    data DATE,
    horario TIME,
    objetivo VARCHAR(2000),
    participantes INT NOT NULL,
    status VARCHAR(255),
    FOREIGN KEY (id_empreendedor) REFERENCES empreendedor (id_empreendedor),
    FOREIGN KEY (id_colaborador) REFERENCES colaborador (id_colaborador)
);

CREATE TABLE cota_mensal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL,
    ano INT NOT NULL,
    mes INT NOT NULL,
    horas_totais DECIMAL(10, 2),
    horas_utilizadas DECIMAL(10, 2),
    horas_reservadas_pendentes DECIMAL(10, 2),
    saldo_devedor DECIMAL(10, 2),
    UNIQUE (persona_id, ano, mes),
    FOREIGN KEY (persona_id) REFERENCES persona (id)
);

CREATE TABLE movimentacao_cota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL,
    reserva_id BIGINT,
    data_hora DATETIME(6),
    mes_referencia DATE,
    quantidade_horas DECIMAL(10, 2),
    tipo VARCHAR(255),
    descricao VARCHAR(255),
    FOREIGN KEY (persona_id) REFERENCES persona (id),
    FOREIGN KEY (reserva_id) REFERENCES reserva (id_reserva)
);

CREATE TABLE historico_reserva (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    data_hora DATETIME(6),
    acao VARCHAR(255),
    descricao VARCHAR(2000),
    id_colaborador BIGINT,
    FOREIGN KEY (reserva_id) REFERENCES reserva (id_reserva),
    FOREIGN KEY (id_colaborador) REFERENCES colaborador (id_colaborador)
);

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

CREATE TABLE solicitacao_publica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    referencia VARCHAR(64) NOT NULL UNIQUE,
    chave VARCHAR(64) NOT NULL UNIQUE,
    espaco_id BIGINT,
    persona_id BIGINT,
    data DATE,
    hora_inicio TIME,
    hora_fim TIME,
    nome VARCHAR(255),
    documento VARCHAR(30),
    tipo_documento VARCHAR(20),
    email VARCHAR(255),
    telefone VARCHAR(255),
    empresa VARCHAR(255),
    cidade VARCHAR(255),
    segmento VARCHAR(255),
    cargo VARCHAR(255),
    titulo VARCHAR(255),
    objetivo VARCHAR(2000),
    observacoes VARCHAR(2000),
    numero_participantes INT NOT NULL,
    inscricao_paga BOOLEAN NOT NULL,
    financiado BOOLEAN NOT NULL,
    academico BOOLEAN NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    codigo_expira_em DATETIME(6),
    justificativa VARCHAR(2000),
    email_confirmado BOOLEAN NOT NULL,
    codigo_hash VARCHAR(128),
    tentativas INT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    reserva_id BIGINT
);

-- Marca se as contas iniciais já foram criadas (as contas em si vêm na entrega 4).
CREATE TABLE bootstrap_seguro (
    id INT PRIMARY KEY,
    concluido BOOLEAN NOT NULL
);

CREATE INDEX idx_empreendedor_email ON empreendedor (email);
CREATE INDEX idx_reserva_data ON reserva (data);
CREATE INDEX idx_reserva_espaco_data ON reserva (id_espaco, data);
CREATE INDEX idx_reserva_status ON reserva (status_reserva);
CREATE INDEX idx_reserva_empreendedor ON reserva (id_empreendedor);
CREATE INDEX idx_mov_persona_mes ON movimentacao_cota (persona_id, mes_referencia);
CREATE INDEX idx_hist_reserva ON historico_reserva (reserva_id);
CREATE INDEX idx_auditoria_instante ON auditoria (instante);
CREATE INDEX idx_solicitacao_estado ON solicitacao_publica (estado, expira_em);

-- Dados iniciais -----------------------------------------------------------------------------------------------

INSERT INTO persona (codigo, nome, descricao, cota_mensal_horas) VALUES
    ('P1', 'InPETU Hub', 'Atividades do Hub', NULL),
    ('P2', 'UFSC outros', 'Atividades da UFSC', 30),
    ('P3', 'Ecossistema Sapiens Parque', 'Ecossistema Sapiens', 20),
    ('P4', 'Governo, ICTs e 3º setor', 'Instituições e governo', 10),
    ('P5', 'Parceiros do InPETU', 'Parceiros', 10),
    ('P6', 'Externos', 'Público externo', NULL);

INSERT INTO configuracao (id, horas_por_turno, abertura, fechamento, taxa_operacao, taxa_limpeza, insumos_limpeza)
    VALUES (1, 4, '08:00', '20:00', NULL, NULL, NULL);

INSERT INTO tabela_preco (ano, quantidade_turnos, percentual, valor, ativo) VALUES
    (2026, 1, 100, 1600, TRUE), (2026, 1, 75, 1200, TRUE), (2026, 1, 50, 800, TRUE),
    (2026, 2, 100, 2800, TRUE), (2026, 2, 75, 2100, TRUE), (2026, 2, 50, 1400, TRUE),
    (2026, 3, 100, 3600, TRUE), (2026, 3, 75, 2700, TRUE), (2026, 3, 50, 1800, TRUE);

INSERT INTO feriado (data, descricao, ativo) VALUES
    ('2026-10-12', 'Nossa Senhora Aparecida', TRUE),
    ('2026-11-02', 'Finados', TRUE),
    ('2026-11-15', 'Proclamação da República', TRUE),
    ('2026-11-20', 'Consciência Negra', TRUE),
    ('2026-12-25', 'Natal', TRUE);

-- Os 10 espaços reais do Hub (documento "Nome dos Espaços", 20/10/2026). Capacidade = cadeiras.
INSERT INTO espaco (nome_espaco, capacidade, mobiliario, disponibilidade, ativo) VALUES
    ('Auditório', 114, '114 cadeiras', TRUE, TRUE),
    ('Sala de reunião', 16, '16 cadeiras', TRUE, TRUE),
    ('Coworking', 35, '35 cadeiras', TRUE, TRUE),
    ('Sala de Reunião 1 (coworking)', 6, '6 cadeiras', TRUE, TRUE),
    ('Sala de Reunião 2 (coworking)', 6, '6 cadeiras', TRUE, TRUE),
    ('Sala Pré-incubação', 34, '34 cadeiras', TRUE, TRUE),
    ('Sala Maker VIA', 32, '32 cadeiras e 6 sofás', TRUE, TRUE),
    ('Sala VIA hub', 15, '15 cadeiras, 7 sofás e 1 arquibancada', TRUE, TRUE),
    ('Sala Distrito de Inovação', 42, '42 cadeiras', TRUE, TRUE),
    ('Sala diária de trabalho', 18, '14 cadeiras e 4 cadeiras brancas na mesa redonda', TRUE, TRUE);

INSERT INTO bootstrap_seguro (id, concluido) VALUES (1, FALSE);
