package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.SolicitacaoPublica;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoPublicaRepository extends JpaRepository<SolicitacaoPublica, Long> {
  Optional<SolicitacaoPublica> findByChave(String chave);

  Optional<SolicitacaoPublica> findByReferencia(String referencia);

  List<SolicitacaoPublica> findTop200ByEstadoOrderByCriadaEmAsc(String estado);

  boolean existsByEstado(String estado);
}
