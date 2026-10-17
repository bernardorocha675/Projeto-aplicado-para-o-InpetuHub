package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "atendimento")
public class Atendimento {
  @Id
  @Column(name = "id_atendimento")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_empreendedor")
  public Empreendedor empreendedor;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_colaborador")
  public Colaborador colaborador;

  public LocalDate data;

  @Column(length = 2000)
  public String descricao;

  @Column(length = 2000)
  public String observacoes;

  @Column(length = 2000)
  public String encaminhamentos;

  public Long getId() {
    return id;
  }

  public Empreendedor getEmpreendedor() {
    return empreendedor;
  }

  public void setEmpreendedor(Empreendedor value) {
    empreendedor = value;
  }

  public Colaborador getColaborador() {
    return colaborador;
  }

  public void setColaborador(Colaborador value) {
    colaborador = value;
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

  public String getObservacoes() {
    return observacoes;
  }

  public void setObservacoes(String value) {
    observacoes = value;
  }

  public String getEncaminhamentos() {
    return encaminhamentos;
  }

  public void setEncaminhamentos(String value) {
    encaminhamentos = value;
  }
}
