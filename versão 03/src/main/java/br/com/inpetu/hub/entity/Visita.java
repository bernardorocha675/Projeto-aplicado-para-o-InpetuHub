package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "visita")
public class Visita {
  @Id
  @Column(name = "id_visita")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_empreendedor")
  public Empreendedor empreendedor;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_colaborador")
  public Colaborador colaborador;

  public LocalDate data;
  public LocalTime horario;

  @Column(length = 2000)
  public String objetivo;

  public int participantes;
  public String status;

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

  public LocalTime getHorario() {
    return horario;
  }

  public void setHorario(LocalTime value) {
    horario = value;
  }

  public String getObjetivo() {
    return objetivo;
  }

  public void setObjetivo(String value) {
    objetivo = value;
  }

  public int getParticipantes() {
    return participantes;
  }

  public void setParticipantes(int value) {
    participantes = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }
}
