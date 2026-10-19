package br.com.inpetu.hub.service;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.security.LoginNames;
import jakarta.persistence.EntityManager;
import java.lang.reflect.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrudService {
  public record Campo(
      String nome, String rotulo, String tipo, boolean obrigatorio, Map<String, String> opcoes) {}

  public record Secao(String titulo, Class<?> classe, List<Campo> campos) {}

  private final EntityManager em;
  private final PasswordEncoder encoder;
  private final ConfiguracaoRepository config;
  private final CotaService cotas;
  private final Clock clock;
  private final br.com.inpetu.hub.security.AuditService audit;

  public CrudService(
      EntityManager e,
      PasswordEncoder p,
      ConfiguracaoRepository c,
      CotaService q,
      Clock clock,
      br.com.inpetu.hub.security.AuditService audit) {
    this.audit = audit;
    em = e;
    encoder = p;
    config = c;
    cotas = q;
    this.clock = clock;
  }

  private Campo campo(String n, String r, String t) {
    return new Campo(n, r, t, true, Map.of());
  }

  private Campo opcional(String n, String r, String t) {
    return new Campo(n, r, t, false, Map.of());
  }

  private Campo escolha(String n, String r, Map<String, String> opcoes) {
    return new Campo(n, r, "select", true, opcoes);
  }

  private Map<String, String> relacao(Class<?> c, String campo) {
    Map<String, String> m = new LinkedHashMap<>();
    for (Object o : em.createQuery("from " + c.getSimpleName() + " order by id", c).getResultList())
      try {
        m.put(c.getField("id").get(o).toString(), String.valueOf(c.getField(campo).get(o)));
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    return m;
  }

  public Secao secao(String tipo) {
    return switch (tipo) {
      case "empreendedores" ->
          new Secao(
              "Empreendedores",
              Empreendedor.class,
              List.of(
                  campo("nome", "Nome", "text"),
                  campo("cnpj", "CNPJ (14 dígitos)", "text"),
                  campo("empresa", "Empresa/instituição", "text"),
                  campo("email", "Email", "email"),
                  campo("telefone", "Telefone", "text"),
                  campo("cidade", "Cidade", "text"),
                  opcional("segmento", "Segmento", "text"),
                  opcional("cargo", "Cargo", "text"),
                  campo("ativo", "Ativo", "checkbox")));
      case "espacos" ->
          new Secao(
              "Espaços",
              Espaco.class,
              List.of(
                  campo("nomeEspaco", "Nome do espaço", "text"),
                  campo("capacidade", "Capacidade", "number"),
                  campo("disponibilidade", "Disponível para reservas", "checkbox"),
                  campo("ativo", "Ativo", "checkbox")));
      case "atendimentos" ->
          new Secao(
              "Atendimentos",
              Atendimento.class,
              List.of(
                  escolha("empreendedor", "Empreendedor", relacao(Empreendedor.class, "nome")),
                  escolha("colaborador", "Colaborador", relacao(Colaborador.class, "nome")),
                  campo("data", "Data", "date"),
                  campo("descricao", "Descrição", "textarea"),
                  opcional("observacoes", "Observações", "textarea"),
                  opcional("encaminhamentos", "Encaminhamentos", "textarea")));
      case "visitas" ->
          new Secao(
              "Visitas",
              Visita.class,
              List.of(
                  escolha("empreendedor", "Empreendedor", relacao(Empreendedor.class, "nome")),
                  escolha("colaborador", "Colaborador", relacao(Colaborador.class, "nome")),
                  campo("data", "Data", "date"),
                  campo("horario", "Horário", "time"),
                  campo("objetivo", "Objetivo", "textarea"),
                  campo("participantes", "Participantes", "number"),
                  escolha(
                      "status",
                      "Status",
                      Map.of(
                          "AGENDADA",
                          "Agendada",
                          "REALIZADA",
                          "Realizada",
                          "CANCELADA",
                          "Cancelada"))));
      case "personas" ->
          new Secao(
              "Personas e cotas",
              Persona.class,
              List.of(
                  campo("codigo", "Código", "text"),
                  campo("nome", "Nome", "text"),
                  opcional("descricao", "Descrição", "text"),
                  opcional("cotaMensalHoras", "Cota mensal (horas)", "number")));
      case "precos" ->
          new Secao(
              "Tabela de preços",
              TabelaPreco.class,
              List.of(
                  campo("ano", "Ano", "number"),
                  campo("quantidadeTurnos", "Quantidade de turnos", "number"),
                  escolha(
                      "percentual", "Percentual", Map.of("100", "100%", "75", "75%", "50", "50%")),
                  campo("valor", "Valor (R$)", "number"),
                  campo("ativo", "Ativo", "checkbox")));
      case "feriados" ->
          new Secao(
              "Feriados",
              Feriado.class,
              List.of(
                  campo("data", "Data", "date"),
                  campo("descricao", "Descrição", "text"),
                  campo("ativo", "Ativo", "checkbox")));
      case "colaboradores" ->
          new Secao(
              "Colaboradores",
              Colaborador.class,
              List.of(
                  campo("nome", "Nome", "text"),
                  opcional("email", "Email", "email"),
                  campo("login", "Login", "text"),
                  opcional("senha", "Senha de 8–64 caracteres (vazia mantém a atual)", "password"),
                  escolha(
                      "perfil", "Perfil", Map.of("ADMIN", "Admin", "COLABORADOR", "Colaborador")),
                  campo("ativo", "Ativo", "checkbox")));
      case "configuracao" ->
          new Secao(
              "Configuração de valores e expediente",
              Configuracao.class,
              List.of(
                  campo("horasPorTurno", "Horas por turno (validar com o Hub)", "number"),
                  campo("abertura", "Abertura", "time"),
                  campo("fechamento", "Fechamento", "time"),
                  opcional("taxaOperacao", "Operação por reserva (R$)", "number"),
                  opcional("taxaLimpeza", "Limpeza por reserva (R$)", "number"),
                  opcional("insumosLimpeza", "Insumos de limpeza — P2 acadêmico (R$)", "number")));
      default -> throw new RegraException("Seção não encontrada.");
    };
  }

  public List<?> listar(String tipo, String busca) {
    Secao s = secao(tipo);
    List<?> lista =
        em.createQuery("from " + s.classe.getSimpleName() + " order by id desc", s.classe)
            .getResultList();
    if (busca == null || busca.isBlank()) return lista;
    String q = busca.toLowerCase(Locale.ROOT);
    return lista.stream()
        .filter(
            o ->
                s.campos.stream()
                    .anyMatch(
                        c ->
                            !c.nome.equals("senha")
                                && valor(o, c.nome).toLowerCase(Locale.ROOT).contains(q)))
        .toList();
  }

  public Object obter(String tipo, Long id) {
    Object o = em.find(secao(tipo).classe, id);
    if (o == null) throw new RegraException("Registro não encontrado.");
    return o;
  }

  public String valor(Object o, String campo) {
    if (o == null || campo.equals("senha")) return "";
    if (o instanceof Map<?, ?> dados) return String.valueOf(dados.get(campo));
    try {
      Object v = o.getClass().getField(campo).get(o);
      if (v == null) return "";
      if (v instanceof Empreendedor e) return e.id.toString();
      if (v instanceof Colaborador c) return c.id.toString();
      return String.valueOf(v);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public String exibicao(Object o, String campo) {
    try {
      Object v = o.getClass().getField(campo).get(o);
      if (v instanceof Empreendedor e) return e.nome;
      if (v instanceof Colaborador c) return c.nome;
      return valor(o, campo);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Transactional
  public void salvar(String tipo, Long id, Map<String, String> dados, Colaborador autor) {
    config.bloquear();
    Secao s = secao(tipo);
    if (tipo.equals("configuracao") && (id == null || id != 1L))
      throw new RegraException("A configuração é única.");
    try {
      Long actorId = autor.id;
      boolean actorAdmin = autor.perfil == Perfil.ADMIN;
      Object o = id == null ? s.classe.getDeclaredConstructor().newInstance() : obter(tipo, id);
      String before =
          o instanceof Espaco e
              ? "ativo=" + e.ativo + ",disponibilidade=" + e.disponibilidade
              : null;
      for (Campo campo : s.campos) {
        String raw = dados.getOrDefault(campo.nome, "").trim();
        Field f = s.classe.getField(campo.nome);
        Class<?> t = f.getType();
        if (campo.obrigatorio && !campo.tipo.equals("checkbox") && raw.isBlank())
          throw new RegraException("Preencha: " + campo.rotulo);
        if (raw.length() > (campo.tipo.equals("textarea") ? 2000 : 255))
          throw new RegraException("Campo muito longo: " + campo.rotulo);
        // Mesmo formato do formulário público e do cadastro de empreendedores.
        if (campo.tipo.equals("email")
            && !raw.isBlank()
            && !raw.matches(br.com.inpetu.hub.validation.ContactRules.EMAIL))
          throw new RegraException(
              "Email: informe um endereço completo, como nome@instituicao.com.br.");
        if (campo.tipo.equals("select") && !campo.opcoes.containsKey(raw))
          throw new RegraException("Opção inválida: " + campo.rotulo);
        if (campo.nome.equals("senha")) {
          if (raw.isBlank() && id != null) continue;
          if (raw.length() < 8
              || raw.length() > 64
              || raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new RegraException("A senha deve ter de 8 a 64 caracteres (até 72 bytes).");
          f.set(o, encoder.encode(raw));
          continue;
        }
        Object v;
        if (t == boolean.class) v = raw.equals("on") || raw.equals("true");
        else if (t == String.class) v = raw;
        else if (t == int.class && o instanceof Espaco && campo.nome.equals("capacidade")) {
          try {
            if (!raw.matches("[0-9]+")) throw new NumberFormatException();
            v = Integer.valueOf(raw);
            if ((Integer) v < 1) throw new NumberFormatException();
          } catch (NumberFormatException ex) {
            throw new RegraException("Capacidade deve ser um número inteiro positivo.");
          }
        } else if (t == int.class) v = Integer.valueOf(raw);
        else if (t == BigDecimal.class) v = raw.isBlank() ? null : new BigDecimal(raw);
        else if (t == LocalDate.class) v = LocalDate.parse(raw);
        else if (t == LocalTime.class) v = LocalTime.parse(raw);
        else if (t == Perfil.class) v = Perfil.valueOf(raw);
        else v = em.find(t, Long.valueOf(raw));
        f.set(o, v);
      }
      if (o instanceof Colaborador c) {
        c.login = LoginNames.normalize(c.login);
        if (!LoginNames.valid(c.login))
          throw new RegraException("Use um usuário com 4–120 letras, números ou . _ @ + -.");
        if (id != null
            && id.equals(actorId)
            && actorAdmin
            && (!c.ativo || c.perfil != Perfil.ADMIN))
          throw new RegraException("Você não pode remover seu próprio acesso de gestor.");
        c.securityVersion++;
      }
      validar(o, autor);
      if (id == null) em.persist(o);
      else em.merge(o);
      em.flush();
      Long targetId = (Long) s.classe.getField("id").get(o);
      String details =
          o instanceof Espaco e
              ? before + " -> ativo=" + e.ativo + ",disponibilidade=" + e.disponibilidade
              : null;
      audit.record(
          actorId,
          id == null ? "CADASTRO_CRIADO" : "CADASTRO_ALTERADO",
          tipo,
          targetId,
          "OK",
          details);
      if (o instanceof Persona p && p.cotaMensalHoras != null) {
        YearMonth atual = YearMonth.now(clock);
        for (CotaMensal cm :
            em.createQuery("from CotaMensal c where c.persona.id=:id", CotaMensal.class)
                .setParameter("id", p.id)
                .getResultList())
          if (!YearMonth.of(cm.ano, cm.mes).isBefore(atual)) cm.horasTotais = p.cotaMensalHoras;
        cotas.reconstruir(p, atual, true);
      }
    } catch (RegraException ex) {
      throw ex;
    } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
      throw new RegraException("Valor ou data inválido.");
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void validar(Object o, Colaborador autor) {
    if (o instanceof Empreendedor e) {
      br.com.inpetu.hub.validation.ContactRules.validate(e.nome,e.email,e.telefone);
      if (!e.cnpj.matches("[0-9]{14}")) throw new RegraException("CNPJ deve conter 14 dígitos.");
      long duplicados =
          em.createQuery(
                  "select count(e) from Empreendedor e where e.cnpj=:cnpj and (:id is null or"
                      + " e.id<>:id)",
                  Long.class)
              .setParameter("cnpj", e.cnpj)
              .setParameter("id", e.id)
              .getSingleResult();
      if (duplicados > 0) throw new RegraException("CNPJ já cadastrado.");
    }
    if (o instanceof Espaco e && e.capacidade < 1)
      throw new RegraException("Capacidade deve ser positiva.");
    if (o instanceof Visita v && v.participantes < 1)
      throw new RegraException("Participantes deve ser positivo.");
    if (o instanceof Persona p) {
      if (!Set.of("P1", "P2", "P3", "P4", "P5", "P6").contains(p.codigo))
        throw new RegraException("Utilize as personas P1–P6.");
      if ((p.codigo.equals("P1") || p.codigo.equals("P6")) && p.cotaMensalHoras != null)
        throw new RegraException("P1 e P6 não usam cota.");
      if (!p.codigo.equals("P1")
          && !p.codigo.equals("P6")
          && (p.cotaMensalHoras == null || p.cotaMensalHoras.signum() < 0))
        throw new RegraException("Informe uma cota não negativa.");
    }
    if (o instanceof TabelaPreco p
        && (p.ano < 2026
            || p.ano > 2100
            || p.quantidadeTurnos < 1
            || p.quantidadeTurnos > 24
            || p.valor.signum() < 0)) throw new RegraException("Ano, turnos ou valor inválido.");
    if (o instanceof Configuracao c) {
      if (c.horasPorTurno < 1 || c.horasPorTurno > 24 || !c.fechamento.isAfter(c.abertura))
        throw new RegraException("Expediente ou duração de turno inválidos.");
      for (BigDecimal v : Arrays.asList(c.taxaOperacao, c.taxaLimpeza, c.insumosLimpeza))
        if (v != null && v.signum() < 0) throw new RegraException("Taxas não podem ser negativas.");
    }
    if (o instanceof Colaborador c) {
      if (c.id != null
          && c.id.equals(autor.id)
          && (!c.ativo || c.perfil != Perfil.ADMIN)
          && autor.perfil == Perfil.ADMIN)
        throw new RegraException("Você não pode remover seu próprio acesso de gestor.");
    }
  }

  @Transactional
  public void excluir(String tipo, Long id) {
    config.bloquear();
    if (!Set.of("visitas", "atendimentos", "espacos", "feriados", "precos").contains(tipo))
      throw new RegraException("Use edição/ativação para preservar o histórico deste cadastro.");
    Object o = obter(tipo, id);
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    Long actor =
        auth != null && auth.getPrincipal() instanceof br.com.inpetu.hub.security.AccountPrincipal p
            ? p.id()
            : null;
    audit.record(actor, "CADASTRO_EXCLUIDO", tipo, id, "OK", null);
    em.remove(o);
    em.flush();
  }
}
