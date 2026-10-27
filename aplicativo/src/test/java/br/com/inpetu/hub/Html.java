package br.com.inpetu.hub;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/** HTML renderizado sem o recuo e as quebras de linha dos templates, que não mudam o que a tela mostra. */
final class Html {
  private Html() {}

  static String compacto(String html) {
    return html.replaceAll("\\s+", " ").replaceAll("\\s*/?>\\s*", ">").replaceAll("\\s+<", "<");
  }

  static Matcher<String> contem(String trecho) {
    return new FeatureMatcher<String, String>(Matchers.containsString(trecho), "HTML compacto", "HTML compacto") {
      @Override
      protected String featureValueOf(String html) {
        return compacto(html);
      }
    };
  }
}
