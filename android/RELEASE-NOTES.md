# InPETU Hub Android 0.1.0-experimental.1

**EXPERIMENTAL — aplicativo independente, com banco no próprio celular.**

APK nativo para Android 8.0 ou superior. Não precisa de computador, VS Code, Java externo, servidor ou internet para funcionar. Não possui permissão de acesso à rede.

Inclui cinco acessos de demonstração, visualização da agenda por data, cadastro/edição de espaços e capacidade, solicitação/aprovação/recusa/cancelamento de reservas, conferência manual do vínculo, cotas, penalidade de cancelamento, preços/taxas/feriados, registro manual de pagamento e conclusão. Apenas reservas aprovadas ocupam horários e usam cota. Pendentes expiram em 24 horas.

Logs locais automáticos contêm versão, códigos e tipos de erro, sem copiar formulários/senhas. Exportação de log sanitizado e backup privado; restauração somente numa instalação vazia. Banco, reservas e pessoas são locais a este aparelho: não existe sincronização com o desktop ou outros celulares.

## Instalar

Baixe `InPETU-Hub-Android-0.1.0-experimental.1.apk`, abra no Android e permita a instalação por esse navegador/gerenciador de arquivos quando o sistema solicitar. Não é uma versão da Play Store. Abra **InPETU Experimental**. Guia completo: docs/ANDROID.md.

Acessos locais de demonstração: milena/milena040, bernardo/bernardo040, joaof/joaof040, joaoe/joaoe040 e guilherme/guilherme040. Bernardo é o administrador. Não utilize dados confidenciais.

## Limites desta primeira versão

Não inclui CRM de atendimentos/visitas, edição de uma reserva existente, SMTP, links públicos, segundo fator, importação do banco desktop, envio de logs ao GitHub ou atualização automática. Para alterar data/horário de reserva, cancele e crie outra, observando a penalidade aplicável. Alterar limite de cota recalcula os meses anteriores com o novo limite, diferentemente dos limites mensais persistidos no desktop. Tabela de referência inicial é 2026; cadastre valores aprovados para outro ano. Não constitui aceite de produção.

A assinatura é privada. O arquivo `assinatura-android-backup.p7m` é um backup criptografado para recuperação pelo responsável; não contém uma chave aberta. Futuras publicações exigem restaurar essa assinatura e configurar os secrets de manutenção, sem gerar outra identidade silenciosamente. Nunca publique o ZIP privado de recuperação entregue separadamente.

O desktop continua na versão estável 0.4.5. Esta prerelease não substitui releases anteriores nem é oferecida como atualização automática do desktop. Consulte o relatório da CI desta tag para a evidência do emulador; não equivale a teste em todos os aparelhos físicos.
