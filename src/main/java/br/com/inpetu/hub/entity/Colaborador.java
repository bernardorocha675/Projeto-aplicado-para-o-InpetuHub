package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "colaborador")
public class Colaborador {
  @Id
  @Column(name = "id_colaborador")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String nome;
  public String email;

  @Column(nullable = false, unique = true)
  public String login;

  @Column(nullable = false)
  public String senha;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  public Perfil perfil;

  public boolean ativo;

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    email = value;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String value) {
    login = value;
  }

  public String getSenha() {
    return senha;
  }

  public void setSenha(String value) {
    senha = value;
  }

  public Perfil getPerfil() {
    return perfil;
  }

  public void setPerfil(Perfil value) {
    perfil = value;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
