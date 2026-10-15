package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "persona")
public class Persona {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true, nullable = false)
  public String codigo;

  public String nome;
  public String descricao;

  @Column(precision = 10, scale = 2)
  public BigDecimal cotaMensalHoras;

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String value) {
    codigo = value;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String value) {
    descricao = value;
  }

  public BigDecimal getCotaMensalHoras() {
    return cotaMensalHoras;
  }

  public void setCotaMensalHoras(BigDecimal value) {
    cotaMensalHoras = value;
  }
}
