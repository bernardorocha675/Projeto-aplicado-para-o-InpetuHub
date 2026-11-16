package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

/** Lê do banco o que as regras de reserva precisam: personas, configuração, preços, feriados, cota e ocupação. */
public final class DadosDoBanco {

    /** Persona com o nome, para as telas. */
    public record PersonaComNome(Persona persona, String nome) {
    }

    private interface Leitor<T> {

        T ler(ResultSet linha) throws SQLException;
    }

    private final DataSource banco;

    public DadosDoBanco(DataSource banco) {
        this.banco = banco;
    }

    public List<PersonaComNome> personas() {
        return lista("select id, codigo, cota_mensal_horas, nome from persona order by codigo, id", List.of(),
                DadosDoBanco::persona);
    }

    public Optional<PersonaComNome> persona(long id) {
        return lista("select id, codigo, cota_mensal_horas, nome from persona where id = ?", List.of(id),
                DadosDoBanco::persona).stream().findFirst();
    }

    public Configuracao configuracao() {
        return lista("select horas_por_turno, abertura, fechamento, taxa_operacao, taxa_limpeza, insumos_limpeza"
                + " from configuracao where id = 1", List.of(),
                linha -> new Configuracao(linha.getInt(1), linha
                        .getTime(2).toLocalTime(), linha.getTime(3).toLocalTime(), linha.getBigDecimal(4),
                        linha
                                .getBigDecimal(5),
                        linha.getBigDecimal(6)))
                .stream().findFirst().orElseThrow(
                        () -> new IllegalStateException("Configuração do Hub não encontrada."));
    }

    public TabelaPrecos precos() {
        TabelaPrecos tabela = new TabelaPrecos();
        lista("select ano, quantidade_turnos, percentual, valor from tabela_preco where ativo = true", List.of(),
                linha -> tabela.adicionar(linha.getInt(1), linha.getInt(2), linha.getInt(3), linha.getBigDecimal(4)));
        return tabela;
    }

    public Set<LocalDate> feriados() {
        return new HashSet<>(lista("select data from feriado where ativo = true", List.of(), linha -> linha.getDate(1)
                .toLocalDate()));
    }

    /** Os movimentos de cota da persona, em ordem de gravação. */
    public List<Movimento> movimentos(long personaId) {
        return lista("select tipo, mes_referencia, quantidade_horas, descricao from movimentacao_cota"
                + " where persona_id = ? order by mes_referencia, id", List.of(personaId),
                linha -> new Movimento(
                        TipoMovimento.valueOf(linha.getString(1)), YearMonth.from(linha.getDate(2).toLocalDate()), linha
                                .getBigDecimal(3),
                        linha.getString(4)));
    }

    /** Horários das reservas APROVADAS do espaço no dia (sem a reserva {@code ignorar}, numa alteração). */
    public List<Intervalo> aprovadas(long espacoId, LocalDate data, long ignorar) {
        return lista("select hora_inicio, hora_fim from reserva where id_espaco = ? and data = ?"
                + " and status_reserva = 'APROVADA' and id_reserva <> ? order by hora_inicio",
                List.of(espacoId, Date
                        .valueOf(data), ignorar),
                linha -> new Intervalo(linha.getTime(1).toLocalTime(), linha.getTime(
                        2).toLocalTime()));
    }

    private static PersonaComNome persona(ResultSet linha) throws SQLException {
        BigDecimal cota = linha.getBigDecimal(3);
        return new PersonaComNome(new Persona(linha.getLong(1), linha.getString(2), cota), linha.getString(4));
    }

    private <T> List<T> lista(String consulta, List<Object> valores, Leitor<T> leitor) {
        List<T> saida = new ArrayList<>();
        try (Connection conexao = banco.getConnection(); PreparedStatement sql = conexao.prepareStatement(consulta)) {
            for (int i = 0; i < valores.size(); i++) {
                sql.setObject(i + 1, valores.get(i));
            }
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    saida.add(leitor.ler(linha));
                }
            }
            return saida;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler os dados de reserva.", falha);
        }
    }
}
