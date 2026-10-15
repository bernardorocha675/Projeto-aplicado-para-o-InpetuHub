package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
  List<Auditoria> findTop200ByOrderByInstanteDesc();
}
