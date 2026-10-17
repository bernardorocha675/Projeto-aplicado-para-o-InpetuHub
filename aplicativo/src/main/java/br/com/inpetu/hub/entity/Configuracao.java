package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "configuracao")
public class Configuracao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public int horasPorTurno;
  public LocalTime abertura;
  public LocalTime fechamento;

  @Column(precision = 12, scale = 2)
  public BigDecimal taxaOperacao;

  @Column(precision = 12, scale = 2)
  public BigDecimal taxaLimpeza;

  @Column(precision = 12, scale = 2)
  public BigDecimal insumosLimpeza;

  public Long getId() {
    return id;
  }

  public int getHorasPorTurno() {
    return horasPorTurno;
  }

  public void setHorasPorTurno(int value) {
    horasPorTurno = value;
  }

  public LocalTime getAbertura() {
    return abertura;
  }

  public void setAbertura(LocalTime value) {
    abertura = value;
  }

  public LocalTime getFechamento() {
    return fechamento;
  }

  public void setFechamento(LocalTime value) {
    fechamento = value;
  }

  public BigDecimal getTaxaOperacao() {
    return taxaOperacao;
  }

  public void setTaxaOperacao(BigDecimal value) {
    taxaOperacao = value;
  }

  public BigDecimal getTaxaLimpeza() {
    return taxaLimpeza;
  }

  public void setTaxaLimpeza(BigDecimal value) {
    taxaLimpeza = value;
  }

  public BigDecimal getInsumosLimpeza() {
    return insumosLimpeza;
  }

  public void setInsumosLimpeza(BigDecimal value) {
    insumosLimpeza = value;
  }
}
