package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
  Optional<Colaborador> findByLogin(String login);

  Optional<Colaborador> findByLoginIgnoreCase(String login);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Colaborador c where c.id=:id")
  Optional<Colaborador> locked(@Param("id") Long id);
}
