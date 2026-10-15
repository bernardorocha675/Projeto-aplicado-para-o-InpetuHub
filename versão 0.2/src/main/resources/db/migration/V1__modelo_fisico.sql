CREATE TABLE empreendedor (
 id_empreendedor BIGINT AUTO_INCREMENT PRIMARY KEY,
 cnpj VARCHAR(14) NOT NULL UNIQUE,
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
 ativo BOOLEAN NOT NULL
);
CREATE TABLE persona (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 codigo VARCHAR(255) NOT NULL UNIQUE,
 nome VARCHAR(255),
 descricao VARCHAR(255),
 cota_mensal_horas DECIMAL(10,2)
);
CREATE TABLE configuracao (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 horas_por_turno INT NOT NULL,
 abertura TIME,
 fechamento TIME,
 taxa_operacao DECIMAL(12,2),
 taxa_limpeza DECIMAL(12,2),
 insumos_limpeza DECIMAL(12,2)
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
 valor DECIMAL(12,2),
 ativo BOOLEAN NOT NULL,
 UNIQUE (ano,quantidade_turnos,percentual)
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
 horas_cota DECIMAL(10,2),
 valor_base DECIMAL(12,2),
 valor_final DECIMAL(12,2),
 valor_sinal DECIMAL(12,2),
 valor_restante DECIMAL(12,2),
 percentual_sinal INT NOT NULL,
 percentual_locacao INT NOT NULL,
 percentual_operacao INT NOT NULL,
 percentual_limpeza INT NOT NULL,
 valor_operacao DECIMAL(12,2),
 valor_limpeza DECIMAL(12,2),
 taxas_configuradas BOOLEAN NOT NULL,
 CHECK (hora_fim > hora_inicio),
 CHECK (numero_participantes > 0),
 FOREIGN KEY (id_empreendedor) REFERENCES empreendedor(id_empreendedor),
 FOREIGN KEY (id_espaco) REFERENCES espaco(id_espaco),
 FOREIGN KEY (persona_id) REFERENCES persona(id),
 FOREIGN KEY (id_colaborador_responsavel) REFERENCES colaborador(id_colaborador)
);
CREATE TABLE atendimento (
 id_atendimento BIGINT AUTO_INCREMENT PRIMARY KEY,
 id_empreendedor BIGINT NOT NULL,
 id_colaborador BIGINT NOT NULL,
 data DATE,
 descricao VARCHAR(2000),
 observacoes VARCHAR(2000),
 encaminhamentos VARCHAR(2000),
 FOREIGN KEY (id_empreendedor) REFERENCES empreendedor(id_empreendedor),
 FOREIGN KEY (id_colaborador) REFERENCES colaborador(id_colaborador)
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
 FOREIGN KEY (id_empreendedor) REFERENCES empreendedor(id_empreendedor),
 FOREIGN KEY (id_colaborador) REFERENCES colaborador(id_colaborador)
);
CREATE TABLE cota_mensal (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 persona_id BIGINT NOT NULL,
 ano INT NOT NULL,
 mes INT NOT NULL,
 horas_totais DECIMAL(10,2),
 horas_utilizadas DECIMAL(10,2),
 horas_reservadas_pendentes DECIMAL(10,2),
 saldo_devedor DECIMAL(10,2),
 UNIQUE (persona_id,ano,mes),
 FOREIGN KEY (persona_id) REFERENCES persona(id)
);
CREATE TABLE movimentacao_cota (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 persona_id BIGINT NOT NULL,
 reserva_id BIGINT,
 data_hora DATETIME(6),
 mes_referencia DATE,
 quantidade_horas DECIMAL(10,2),
 tipo VARCHAR(255),
 descricao VARCHAR(255),
 FOREIGN KEY (persona_id) REFERENCES persona(id),
 FOREIGN KEY (reserva_id) REFERENCES reserva(id_reserva)
);
CREATE TABLE historico_reserva (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 reserva_id BIGINT NOT NULL,
 data_hora DATETIME(6),
 acao VARCHAR(255),
 descricao VARCHAR(2000),
 id_colaborador BIGINT,
 FOREIGN KEY (reserva_id) REFERENCES reserva(id_reserva),
 FOREIGN KEY (id_colaborador) REFERENCES colaborador(id_colaborador)
);
CREATE INDEX idx_empreendedor_email ON empreendedor(email);
CREATE INDEX idx_reserva_data ON reserva(data);
CREATE INDEX idx_reserva_espaco_data ON reserva(id_espaco,data);
CREATE INDEX idx_reserva_status ON reserva(status_reserva);
CREATE INDEX idx_reserva_empreendedor ON reserva(id_empreendedor);
CREATE INDEX idx_mov_persona_mes ON movimentacao_cota(persona_id,mes_referencia);
CREATE INDEX idx_hist_reserva ON historico_reserva(reserva_id);
