-- Espaços reais do InPETU Hub (documento "Nome dos Espaços", 20/10/2026). Migration aditiva:
-- só cria os nomes que ainda não existem (sem diferenciar maiúsculas) e nunca altera espaços,
-- reservas ou capacidades já cadastrados. Capacidade = cadeiras; o restante fica em mobiliário.
ALTER TABLE espaco ADD COLUMN mobiliario VARCHAR(255);

INSERT INTO espaco (nome_espaco, capacidade, disponibilidade, ativo, mobiliario)
SELECT v.nome, v.capacidade, TRUE, TRUE, v.mobiliario
FROM (
  SELECT 'Auditório' AS nome, 114 AS capacidade, '114 cadeiras' AS mobiliario
  UNION ALL SELECT 'Sala de reunião', 16, '16 cadeiras'
  UNION ALL SELECT 'Coworking', 35, '35 cadeiras'
  UNION ALL SELECT 'Sala de Reunião 1 (coworking)', 6, '6 cadeiras'
  UNION ALL SELECT 'Sala de Reunião 2 (coworking)', 6, '6 cadeiras'
  UNION ALL SELECT 'Sala Pré-incubação', 34, '34 cadeiras'
  UNION ALL SELECT 'Sala Maker VIA', 32, '32 cadeiras e 6 sofás'
  UNION ALL SELECT 'Sala VIA hub', 15, '15 cadeiras, 7 sofás e 1 arquibancada'
  UNION ALL SELECT 'Sala Distrito de Inovação', 42, '42 cadeiras'
  UNION ALL SELECT 'Sala diária de trabalho', 18, '14 cadeiras e 4 cadeiras brancas na mesa redonda'
) v
WHERE NOT EXISTS (SELECT 1 FROM espaco e WHERE LOWER(e.nome_espaco) = LOWER(v.nome));
