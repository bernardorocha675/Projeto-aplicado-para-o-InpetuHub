package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CotaMensalRepository extends JpaRepository<CotaMensal, Long> {
  Optional<CotaMensal> findByPersonaIdAndAnoAndMes(Long personaId, int ano, int mes);

  List<CotaMensal> findByPersonaIdOrderByAnoAscMesAsc(Long personaId);
}
