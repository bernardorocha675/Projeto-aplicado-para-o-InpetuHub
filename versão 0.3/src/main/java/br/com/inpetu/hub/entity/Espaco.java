package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "espaco")
public class Espaco {
  @Id
  @Column(name = "id_espaco")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public String nomeEspaco;

  public int capacidade;
  public boolean disponibilidade;
  public boolean ativo;

  public Long getId() {
    return id;
  }

  public String getNomeEspaco() {
    return nomeEspaco;
  }

  public void setNomeEspaco(String value) {
    nomeEspaco = value;
  }

  public int getCapacidade() {
    return capacidade;
  }

  public void setCapacidade(int value) {
    capacidade = value;
  }

  public boolean getDisponibilidade() {
    return disponibilidade;
  }

  public void setDisponibilidade(boolean value) {
    disponibilidade = value;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
