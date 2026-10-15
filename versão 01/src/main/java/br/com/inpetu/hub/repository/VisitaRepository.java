package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface VisitaRepository extends JpaRepository<Visita, Long> {
  List<Visita> findByEmpreendedorIdOrderByDataDesc(Long id);
}
