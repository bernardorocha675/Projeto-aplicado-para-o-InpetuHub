package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "historico_reserva")
public class HistoricoReserva {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "reserva_id")
  public Reserva reserva;

  public LocalDateTime dataHora;
  public String acao;

  @Column(length = 2000)
  public String descricao;

  @ManyToOne
  @JoinColumn(name = "id_colaborador")
  public Colaborador colaborador;

  public Long getId() {
    return id;
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

  public String getAcao() {
    return acao;
  }

  public void setAcao(String value) {
    acao = value;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String value) {
    descricao = value;
  }

  public Colaborador getColaborador() {
    return colaborador;
  }

  public void setColaborador(Colaborador value) {
    colaborador = value;
  }
}
