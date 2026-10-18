package br.com.inpetu.hub.validation;

import br.com.inpetu.hub.exception.RegraException;
import java.util.Map;

/** Formatos de contato compartilhados pelo formulário público e pelo cadastro interno. */
public final class ContactRules {
  private ContactRules() {}
  // Somente entrada nova: não invalidar a persistência de solicitações históricas.
  public interface Input {}
  public static final String NAME = "[\\p{L}][\\p{L}\\p{M} .'’\\x2d]*";
  public static final String EMAIL = "[^\\s@]+@(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}\\x2d]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}]{2,63}";
  public static final String PHONE = "(?=(?:[^0-9]*[0-9]){10,15}[^0-9]*$)\\+?[0-9\\(\\) .\\x2d]+";
  public static Map<String,String> patterns() { return Map.of("nome",NAME,"email",EMAIL,"telefone",PHONE); }
  public static void validate(String name,String email,String phone) {
    if(name==null || !name.matches(NAME)) throw new RegraException("Nome: informe letras, sem números. Acentos, espaços, apóstrofos e hífens são permitidos.");
    if(email==null || !email.matches(EMAIL)) throw new RegraException("Email: informe um endereço completo, como nome@instituicao.com.br.");
    if(phone==null || !phone.matches(PHONE)) throw new RegraException("Telefone: informe de 10 a 15 dígitos, incluindo DDD. Letras não são permitidas.");
  }
}
