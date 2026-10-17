package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.dto.ReservaForm;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SolicitacaoPublica {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true)
  public String referencia;

  @Column(unique = true)
  public String chave;

  @Embedded public ReservaForm form;
  public LocalDateTime criadaEm, expiraEm, codigoExpiraEm;
  public boolean emailConfirmado;
  public String codigoHash;
  public int tentativas;
  public String estado;
  public Long reservaId;

  @Column(length = 2000)
  public String justificativa;
}
