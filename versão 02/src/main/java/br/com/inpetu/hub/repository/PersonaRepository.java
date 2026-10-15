package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
  Optional<Persona> findByCodigo(String codigo);
}
