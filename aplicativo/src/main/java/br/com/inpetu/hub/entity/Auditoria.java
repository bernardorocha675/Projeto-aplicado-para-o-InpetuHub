package br.com.inpetu.hub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Auditoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public LocalDateTime instante;
  public Long autorId;
  public String evento;
  public String alvo;
  public Long alvoId;
  public String resultado;

  @Column(length = 2000)
  public String detalhes;
}
