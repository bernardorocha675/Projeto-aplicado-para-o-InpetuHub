package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "movimentacao_cota")
public class MovimentacaoCota {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id")
  public Persona persona;

  @ManyToOne
  @JoinColumn(name = "reserva_id")
  public Reserva reserva;

  public LocalDateTime dataHora;
  public LocalDate mesReferencia;

  @Column(precision = 10, scale = 2)
  public BigDecimal quantidadeHoras;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  public TipoMovimentacao tipo;

  public String descricao;

  public Long getId() {
    return id;
  }

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona value) {
    persona = value;
  }

  public Reserva getReserva() {
    return reserva;
  }

  public void setReserva(Reserva value) {
    reserva = value;
  }

  public LocalDateTime getDataHora() {
    return dataHora;
  }

  public void setDataHora(LocalDateTime value) {
    dataHora = value;
  }

  public LocalDate getMesReferencia() {
    return mesReferencia;
  }

  public void setMesReferencia(LocalDate value) {
    mesReferencia = value;
  }

  public BigDecimal getQuantidadeHoras() {
    return quantidadeHoras;
  }

  public void setQuantidadeHoras(BigDecimal value) {
    quantidadeHoras = value;
  }

  public TipoMovimentacao getTipo() {
    return tipo;
  }

  public void setTipo(TipoMovimentacao value) {
    tipo = value;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String value) {
    descricao = value;
  }
}
