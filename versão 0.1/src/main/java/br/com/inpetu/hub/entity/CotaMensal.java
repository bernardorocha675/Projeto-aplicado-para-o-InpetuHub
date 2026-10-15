package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(
    name = "cota_mensal",
    uniqueConstraints = @UniqueConstraint(columnNames = {"persona_id", "ano", "mes"}))
public class CotaMensal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id")
  public Persona persona;

  public int ano;
  public int mes;

  @Column(precision = 10, scale = 2)
  public BigDecimal horasTotais;

  @Column(precision = 10, scale = 2)
  public BigDecimal horasUtilizadas;

  @Column(precision = 10, scale = 2)
  public BigDecimal horasReservadasPendentes;

  @Column(precision = 10, scale = 2)
  public BigDecimal saldoDevedor;

  public Long getId() {
    return id;
  }

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona value) {
    persona = value;
  }

  public int getAno() {
    return ano;
  }

  public void setAno(int value) {
    ano = value;
  }

  public int getMes() {
    return mes;
  }

  public void setMes(int value) {
    mes = value;
  }

  public BigDecimal getHorasTotais() {
    return horasTotais;
  }

  public void setHorasTotais(BigDecimal value) {
    horasTotais = value;
  }

  public BigDecimal getHorasUtilizadas() {
    return horasUtilizadas;
  }

  public void setHorasUtilizadas(BigDecimal value) {
    horasUtilizadas = value;
  }

  public BigDecimal getHorasReservadasPendentes() {
    return horasReservadasPendentes;
  }

  public void setHorasReservadasPendentes(BigDecimal value) {
    horasReservadasPendentes = value;
  }

  public BigDecimal getSaldoDevedor() {
    return saldoDevedor;
  }

  public void setSaldoDevedor(BigDecimal value) {
    saldoDevedor = value;
  }

  public BigDecimal getSaldo() {
    return horasTotais
        .subtract(horasUtilizadas)
        .subtract(horasReservadasPendentes)
        .max(BigDecimal.ZERO);
  }
}
