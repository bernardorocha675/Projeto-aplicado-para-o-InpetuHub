package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.security.LocalHostFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocalHostFilterTest {
  MockHttpServletResponse request(String host) throws Exception {
    var req = new MockHttpServletRequest("GET", "/login");
    req.setServerName(host);
    var res = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    new LocalHostFilter().doFilter(req, res, chain);
    if (chain.getRequest() != null) res.setStatus(200);
    return res;
  }

  @Test
  void aceitaSomenteNomesDoProprioComputador() throws Exception {
    for (String host : new String[] {"127.0.0.1", "localhost", "LOCALHOST", "[::1]"})
      assertEquals(200, request(host).getStatus(), host);
  }

  @Test
  void recusaDominioExternoApontadoParaLoopback() throws Exception {
    for (String host : new String[] {"atacante.example", "127.0.0.1.atacante.example", "192.168.0.10"}) {
      var res = request(host);
      assertEquals(400, res.getStatus(), host);
      assertTrue(res.getContentAsString().contains("iniciador"));
    }
  }
}
