package br.com.inpetu.hub.entity;

import br.com.inpetu.hub.enums.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "empreendedor")
public class Empreendedor {
  @Id
  @Column(name = "id_empreendedor")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // Número do documento (CNPJ, CPF ou estrangeiro); vazio quando o tipo é NENHUM.
  @Column(unique = true, length = 30)
  public String cnpj;

  @Column(length = 20)
  public String tipoDocumento;

  @Column(nullable = false)
  public String nome;

  @Column(nullable = false)
  public String email;

  public String telefone;
  public String cidade;
  public String empresa;
  public String segmento;
  public String cargo;
  public boolean ativo;

  public Long getId() {
    return id;
  }

  public String getCnpj() {
    return cnpj;
  }

  public void setCnpj(String value) {
    cnpj = value;
  }

  public String getTipoDocumento() {
    return tipoDocumento;
  }

  public void setTipoDocumento(String value) {
    tipoDocumento = value;
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

  public String getTelefone() {
    return telefone;
  }

  public void setTelefone(String value) {
    telefone = value;
  }

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String value) {
    cidade = value;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String value) {
    empresa = value;
  }

  public String getSegmento() {
    return segmento;
  }

  public void setSegmento(String value) {
    segmento = value;
  }

  public String getCargo() {
    return cargo;
  }

  public void setCargo(String value) {
    cargo = value;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
