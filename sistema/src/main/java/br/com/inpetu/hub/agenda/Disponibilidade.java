package br.com.inpetu.hub.agenda;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;

/**
 * Monta a agenda de um intervalo de dias: para cada espaço reservável, os horários ocupados (só reservas APROVADAS) e
 * os livres dentro do expediente. Espaço inativo ou indisponível some da agenda; sem nenhum, o dia fica "SEM_ESPACOS",
 * que não quer dizer ocupado. Os nomes dos estados são os mesmos do antigo, lidos pelo agenda.js.
 */
final class Disponibilidade {

    private record Ocupacao(long espaco, LocalDate data, LocalTime inicio, LocalTime fim, String titulo) {
    }

    private final DataSource banco;
    private final EspacoRepositorio espacos;

    Disponibilidade(DataSource banco) {
        this.banco = banco;
        this.espacos = new EspacoRepositorio(banco);
    }

    /** O JSON de /disponibilidade, como mapas e listas, de {@code inicio} a {@code fim} (inclusive). */
    Map<String, Object> consultar(LocalDate inicio, LocalDate fim, Long espacoId) {
        LocalTime[] expediente = expediente();
        LocalTime abertura = expediente[0];
        LocalTime fechamento = expediente[1];
        List<Espaco> salas = espacos.reservaveis().stream()
                .filter(e -> espacoId == null || e.id() == espacoId)
                .toList();
        List<Ocupacao> ocupacoes = aprovadas(inicio, fim);
        Set<LocalDate> feriados = feriados(inicio, fim);
        List<Object> dias = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            List<Object> salasDoDia = new ArrayList<>();
            boolean temReserva = false;
            boolean temLivre = false;
            for (Espaco sala : salas) {
                List<Object> livres = new ArrayList<>();
                List<Object> ocupados = new ArrayList<>();
                LocalTime cursor = abertura;
                for (Ocupacao ocupacao : ocupacoes) {
                    if (ocupacao.espaco() != sala.id() || !ocupacao.data().equals(dia)) {
                        continue;
                    }
                    LocalTime comeca = ocupacao.inicio().isBefore(abertura) ? abertura : ocupacao.inicio();
                    LocalTime termina = ocupacao.fim().isAfter(fechamento) ? fechamento : ocupacao.fim();
                    if (comeca.isAfter(cursor)) {
                        livres.add(intervalo(cursor, comeca));
                    }
                    if (termina.isAfter(cursor)) {
                        cursor = termina;
                    }
                    Map<String, Object> evento = intervalo(ocupacao.inicio(), ocupacao.fim());
                    evento.put("titulo", ocupacao.titulo() == null ? "" : ocupacao.titulo());
                    evento.put("status", "APROVADA");
                    ocupados.add(evento);
                }
                if (cursor.isBefore(fechamento)) {
                    livres.add(intervalo(cursor, fechamento));
                }
                temReserva |= !ocupados.isEmpty();
                temLivre |= !livres.isEmpty();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", sala.id());
                item.put("nome", sala.nome());
                item.put("capacidade", sala.capacidade());
                item.put("mobiliario", sala.mobiliario() == null ? "" : sala.mobiliario());
                item.put("livres", livres);
                item.put("ocupados", ocupados);
                salasDoDia.add(item);
            }
            String estado = salas.isEmpty()
                    ? "SEM_ESPACOS"
                    : !temLivre ? "INDISPONÍVEL" : temReserva ? "PARCIAL" : "DISPONÍVEL";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("data", dia.toString());
            item.put("estado", estado);
            item.put("feriado", feriados.contains(dia));
            item.put("espacos", salasDoDia);
            dias.add(item);
        }
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("dias", dias);
        saida.put("abertura", abertura.toString());
        saida.put("fechamento", fechamento.toString());
        return saida;
    }

    private static Map<String, Object> intervalo(LocalTime inicio, LocalTime fim) {
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("inicio", inicio.toString());
        saida.put("fim", fim.toString());
        return saida;
    }

    private LocalTime[] expediente() {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "select abertura, fechamento from configuracao where id = 1");
                ResultSet linha = sql.executeQuery()) {
            if (!linha.next()) {
                throw new IllegalStateException("Configuração do Hub não encontrada.");
            }
            return new LocalTime[]{linha.getTime(1).toLocalTime(), linha.getTime(2).toLocalTime()};
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler o expediente.", falha);
        }
    }

    private List<Ocupacao> aprovadas(LocalDate inicio, LocalDate fim) {
        List<Ocupacao> saida = new ArrayList<>();
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "select id_espaco, data, hora_inicio, hora_fim, titulo from reserva where status_reserva = 'APROVADA'"
                                + " and data between ? and ? order by data, hora_inicio, id_reserva")) {
            sql.setDate(1, Date.valueOf(inicio));
            sql.setDate(2, Date.valueOf(fim));
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    saida.add(new Ocupacao(linha.getLong(1), linha.getDate(2).toLocalDate(), linha.getTime(3)
                            .toLocalTime(), linha.getTime(4).toLocalTime(), linha.getString(5)));
                }
            }
            return saida;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler as reservas.", falha);
        }
    }

    private Set<LocalDate> feriados(LocalDate inicio, LocalDate fim) {
        Set<LocalDate> saida = new HashSet<>();
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "select data from feriado where ativo = true and data between ? and ?")) {
            sql.setDate(1, Date.valueOf(inicio));
            sql.setDate(2, Date.valueOf(fim));
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    saida.add(linha.getDate(1).toLocalDate());
                }
            }
            return saida;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler os feriados.", falha);
        }
    }
}
