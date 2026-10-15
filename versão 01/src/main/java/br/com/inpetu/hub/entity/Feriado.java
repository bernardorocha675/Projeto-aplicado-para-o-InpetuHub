package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "feriado")
public class Feriado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true)
  public LocalDate data;

  public String descricao;
  public boolean ativo;

  public Long getId() {
    return id;
  }

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate value) {
    data = value;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String value) {
    descricao = value;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
