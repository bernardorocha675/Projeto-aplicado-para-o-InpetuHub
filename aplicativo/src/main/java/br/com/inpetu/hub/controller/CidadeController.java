package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.service.CidadeService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Sugestões para o campo Cidade do pedido de reserva e do cadastro de empreendedores. */
@RestController
public class CidadeController {
  private final CidadeService cidades;

  public CidadeController(CidadeService cidades) {
    this.cidades = cidades;
  }

  @GetMapping("/cidades")
  public List<CidadeService.Cidade> sugestoes(@RequestParam(name = "q", required = false) String q) {
    return cidades.buscar(q);
  }
}
