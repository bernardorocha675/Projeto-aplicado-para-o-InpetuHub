package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.dto.ReservaForm;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.validation.Documentos;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EmpreendedorRepository extends JpaRepository<Empreendedor, Long> {
  Optional<Empreendedor> findByCnpj(String cnpj);

  Optional<Empreendedor> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

  // Cadastro do solicitante: pelo número do documento; sem documento, pelo e-mail (o mais antigo).
  default Optional<Empreendedor> doPedido(ReservaForm f) {
    var doc = Documentos.normalizar(f.tipoDocumento, f.cnpj);
    return doc.numero() != null
        ? findByCnpj(doc.numero())
        : findFirstByEmailIgnoreCaseOrderByIdAsc(f.email);
  }
}
