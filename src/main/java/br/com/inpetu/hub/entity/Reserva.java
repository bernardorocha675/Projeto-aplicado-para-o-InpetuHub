package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "reserva")
public class Reserva {
  @Id
  @Column(name = "id_reserva")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_empreendedor")
  public Empreendedor empreendedor;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_espaco")
  public Espaco espaco;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id")
  public Persona persona;

  @ManyToOne
  @JoinColumn(name = "id_colaborador_responsavel")
  public Colaborador colaboradorResponsavel;

  @Column(nullable = false)
  public LocalDate data;

  @Column(nullable = false)
  public LocalTime horaInicio;

  @Column(nullable = false)
  public LocalTime horaFim;

  public LocalDateTime dataHoraSolicitacao;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  public StatusReserva statusReserva;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  public StatusPagamento statusPagamento;

  public String titulo;

  @Column(length = 2000)
  public String objetivo;

  @Column(length = 2000)
  public String observacoes;

  @Column(length = 2000)
  public String motivoRecusa;

  public int numeroParticipantes;
  public boolean inscricaoPaga;
  public boolean financiado;
  public boolean academico;
  public boolean usaCota;

  @Column(precision = 10, scale = 2)
  public BigDecimal horasCota;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorBase;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorFinal;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorSinal;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorRestante;

  public int percentualSinal;
  public int percentualLocacao;
  public int percentualOperacao;
  public int percentualLimpeza;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorOperacao;

  @Column(precision = 12, scale = 2)
  public BigDecimal valorLimpeza;

  public boolean taxasConfiguradas;

  public Long getId() {
    return id;
  }

  public Empreendedor getEmpreendedor() {
    return empreendedor;
  }

  public void setEmpreendedor(Empreendedor value) {
    empreendedor = value;
  }

  public Espaco getEspaco() {
    return espaco;
  }

  public void setEspaco(Espaco value) {
    espaco = value;
  }

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona value) {
    persona = value;
  }

  public Colaborador getColaboradorResponsavel() {
    return colaboradorResponsavel;
  }

  public void setColaboradorResponsavel(Colaborador value) {
    colaboradorResponsavel = value;
  }

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate value) {
    data = value;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime value) {
    horaInicio = value;
  }

  public LocalTime getHoraFim() {
    return horaFim;
  }

  public void setHoraFim(LocalTime value) {
    horaFim = value;
  }

  public LocalDateTime getDataHoraSolicitacao() {
    return dataHoraSolicitacao;
  }

  public void setDataHoraSolicitacao(LocalDateTime value) {
    dataHoraSolicitacao = value;
  }

  public StatusReserva getStatusReserva() {
    return statusReserva;
  }

  public void setStatusReserva(StatusReserva value) {
    statusReserva = value;
  }

  public StatusPagamento getStatusPagamento() {
    return statusPagamento;
  }

  public void setStatusPagamento(StatusPagamento value) {
    statusPagamento = value;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String value) {
    titulo = value;
  }

  public String getObjetivo() {
    return objetivo;
  }

  public void setObjetivo(String value) {
    objetivo = value;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public void setObservacoes(String value) {
    observacoes = value;
  }

  public String getMotivoRecusa() {
    return motivoRecusa;
  }

  public void setMotivoRecusa(String value) {
    motivoRecusa = value;
  }

  public int getNumeroParticipantes() {
    return numeroParticipantes;
  }

  public void setNumeroParticipantes(int value) {
    numeroParticipantes = value;
  }

  public boolean getInscricaoPaga() {
    return inscricaoPaga;
  }

  public void setInscricaoPaga(boolean value) {
    inscricaoPaga = value;
  }

  public boolean getFinanciado() {
    return financiado;
  }

  public void setFinanciado(boolean value) {
    financiado = value;
  }

  public boolean getAcademico() {
    return academico;
  }

  public void setAcademico(boolean value) {
    academico = value;
  }

  public boolean getUsaCota() {
    return usaCota;
  }

  public void setUsaCota(boolean value) {
    usaCota = value;
  }

  public BigDecimal getHorasCota() {
    return horasCota;
  }

  public void setHorasCota(BigDecimal value) {
    horasCota = value;
  }

  public BigDecimal getValorBase() {
    return valorBase;
  }

  public void setValorBase(BigDecimal value) {
    valorBase = value;
  }

  public BigDecimal getValorFinal() {
    return valorFinal;
  }

  public void setValorFinal(BigDecimal value) {
    valorFinal = value;
  }

  public BigDecimal getValorSinal() {
    return valorSinal;
  }

  public void setValorSinal(BigDecimal value) {
    valorSinal = value;
  }

  public BigDecimal getValorRestante() {
    return valorRestante;
  }

  public void setValorRestante(BigDecimal value) {
    valorRestante = value;
  }

  public int getPercentualSinal() {
    return percentualSinal;
  }

  public void setPercentualSinal(int value) {
    percentualSinal = value;
  }

  public int getPercentualLocacao() {
    return percentualLocacao;
  }

  public void setPercentualLocacao(int value) {
    percentualLocacao = value;
  }

  public int getPercentualOperacao() {
    return percentualOperacao;
  }

  public void setPercentualOperacao(int value) {
    percentualOperacao = value;
  }

  public int getPercentualLimpeza() {
    return percentualLimpeza;
  }

  public void setPercentualLimpeza(int value) {
    percentualLimpeza = value;
  }

  public BigDecimal getValorOperacao() {
    return valorOperacao;
  }

  public void setValorOperacao(BigDecimal value) {
    valorOperacao = value;
  }

  public BigDecimal getValorLimpeza() {
    return valorLimpeza;
  }

  public void setValorLimpeza(BigDecimal value) {
    valorLimpeza = value;
  }

  public boolean getTaxasConfiguradas() {
    return taxasConfiguradas;
  }

  public void setTaxasConfiguradas(boolean value) {
    taxasConfiguradas = value;
  }
}
