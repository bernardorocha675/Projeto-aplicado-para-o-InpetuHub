package br.com.inpetu.hub.service;

import br.com.inpetu.hub.dto.ReservaForm;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.repository.*;
import org.springframework.stereotype.Service;

@Service
public class EmpreendedorService {
  private final EmpreendedorRepository repo;

  public EmpreendedorService(EmpreendedorRepository r) {
    repo = r;
  }

  public Empreendedor vincular(ReservaForm f) {
    br.com.inpetu.hub.validation.ContactRules.validate(f.nome,f.email,f.telefone);
    var doc = br.com.inpetu.hub.validation.Documentos.normalizar(f.tipoDocumento, f.cnpj);
    return repo.doPedido(f)
        .orElseGet(
            () -> {
              Empreendedor e = new Empreendedor();
              e.tipoDocumento = doc.tipo();
              e.cnpj = doc.numero();
              e.nome = f.nome;
              e.email = f.email;
              e.telefone = f.telefone;
              e.empresa = f.empresa;
              e.cidade = f.cidade;
              e.segmento = f.segmento;
              e.cargo = f.cargo;
              e.ativo = true;
              return repo.save(e);
            });
  }
}
