package br.com.inpetu.hub.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import br.com.inpetu.hub.validation.ContactRules;
import org.springframework.format.annotation.DateTimeFormat;

@jakarta.persistence.Embeddable
public class ReservaForm {
  @NotNull public Long espacoId;
  @NotNull public Long personaId;
  @NotNull @FutureOrPresent
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) public LocalDate data;
  @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) public LocalTime horaInicio;
  @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) public LocalTime horaFim;

  @NotBlank
  @Size(max = 255)
  @Pattern(groups = ContactRules.Input.class, regexp = ContactRules.NAME, message = "Informe um nome com letras, sem números.")
  public String nome;

  @NotBlank
  @Pattern(regexp = "[0-9]{14}", message = "Informe 14 dígitos no CNPJ")
  public String cnpj;

  @NotBlank
  @Email
  @Size(max = 255)
  @Pattern(groups = ContactRules.Input.class, regexp = ContactRules.EMAIL, message = "Informe um email completo, como nome@instituicao.com.br.")
  public String email;

  @NotBlank
  @Size(max = 255)
  @Pattern(groups = ContactRules.Input.class, regexp = ContactRules.PHONE, message = "Informe o telefone com DDD ou, fora do Brasil, com + e o código do país, sem letras.")
  public String telefone;

  @NotBlank
  @Size(max = 255)
  public String empresa;

  @NotBlank
  @Size(max = 255)
  public String cidade;

  @Size(max = 255)
  public String segmento;

  @Size(max = 255)
  public String cargo;

  @NotBlank
  @Size(max = 255)
  public String titulo;

  @NotBlank
  @Size(max = 2000)
  @jakarta.persistence.Column(length = 2000)
  public String objetivo;

  @Size(max = 2000)
  @jakarta.persistence.Column(length = 2000)
  public String observacoes;

  @Min(1)
  public int numeroParticipantes = 1;

  public boolean inscricaoPaga;
  public boolean financiado;
  public boolean academico;

  public Long getEspacoId() {
    return espacoId;
  }

  public void setEspacoId(Long v) {
    espacoId = v;
  }

  public Long getPersonaId() {
    return personaId;
  }

  public void setPersonaId(Long v) {
    personaId = v;
  }

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate v) {
    data = v;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime v) {
    horaInicio = v;
  }

  public LocalTime getHoraFim() {
    return horaFim;
  }

  public void setHoraFim(LocalTime v) {
    horaFim = v;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String v) {
    nome = v;
  }

  public String getCnpj() {
    return cnpj;
  }

  public void setCnpj(String v) {
    cnpj = v;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getTelefone() {
    return telefone;
  }

  public void setTelefone(String v) {
    telefone = v;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String v) {
    empresa = v;
  }

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String v) {
    cidade = v;
  }

  public String getSegmento() {
    return segmento;
  }

  public void setSegmento(String v) {
    segmento = v;
  }

  public String getCargo() {
    return cargo;
  }

  public void setCargo(String v) {
    cargo = v;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String v) {
    titulo = v;
  }

  public String getObjetivo() {
    return objetivo;
  }

  public void setObjetivo(String v) {
    objetivo = v;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public void setObservacoes(String v) {
    observacoes = v;
  }

  public int getNumeroParticipantes() {
    return numeroParticipantes;
  }

  public void setNumeroParticipantes(int v) {
    numeroParticipantes = v;
  }

  public boolean getInscricaoPaga() {
    return inscricaoPaga;
  }

  public void setInscricaoPaga(boolean v) {
    inscricaoPaga = v;
  }

  public boolean getFinanciado() {
    return financiado;
  }

  public void setFinanciado(boolean v) {
    financiado = v;
  }

  public boolean getAcademico() {
    return academico;
  }

  public void setAcademico(boolean v) {
    academico = v;
  }
}
