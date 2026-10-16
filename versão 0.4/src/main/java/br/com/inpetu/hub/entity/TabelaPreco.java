package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(
    name = "tabela_preco",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ano", "quantidade_turnos", "percentual"}))
public class TabelaPreco {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public int ano;
  public int quantidadeTurnos;
  public int percentual;

  @Column(precision = 12, scale = 2)
  public BigDecimal valor;

  public boolean ativo;

  public Long getId() {
    return id;
  }

  public int getAno() {
    return ano;
  }

  public void setAno(int value) {
    ano = value;
  }

  public int getQuantidadeTurnos() {
    return quantidadeTurnos;
  }

  public void setQuantidadeTurnos(int value) {
    quantidadeTurnos = value;
  }

  public int getPercentual() {
    return percentual;
  }

  public void setPercentual(int value) {
    percentual = value;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal value) {
    valor = value;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
