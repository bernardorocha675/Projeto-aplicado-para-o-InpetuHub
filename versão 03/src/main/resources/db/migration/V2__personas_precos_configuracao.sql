INSERT INTO persona(codigo,nome,descricao,cota_mensal_horas) VALUES
('P1','InPETU Hub','Atividades do Hub',NULL),('P2','UFSC outros','Atividades da UFSC',30),('P3','Ecossistema Sapiens Parque','Ecossistema Sapiens',20),('P4','Governo, ICTs e 3º setor','Instituições e governo',10),('P5','Parceiros do InPETU','Parceiros',10),('P6','Externos','Público externo',NULL);
INSERT INTO configuracao(id,horas_por_turno,abertura,fechamento,taxa_operacao,taxa_limpeza,insumos_limpeza) VALUES(1,4,'08:00','20:00',NULL,NULL,NULL);
INSERT INTO tabela_preco(ano,quantidade_turnos,percentual,valor,ativo) VALUES
(2026,1,100,1600,true),(2026,1,75,1200,true),(2026,1,50,800,true),
(2026,2,100,2800,true),(2026,2,75,2100,true),(2026,2,50,1400,true),
(2026,3,100,3600,true),(2026,3,75,2700,true),(2026,3,50,1800,true);
INSERT INTO feriado(data,descricao,ativo) VALUES ('2026-10-12','Nossa Senhora Aparecida',true),('2026-11-02','Finados',true),('2026-11-15','Proclamação da República',true),('2026-11-20','Consciência Negra',true),('2026-12-25','Natal',true);
