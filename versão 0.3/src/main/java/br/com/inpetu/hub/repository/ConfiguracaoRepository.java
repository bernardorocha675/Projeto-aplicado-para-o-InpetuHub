package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Configuracao c where c.id=1")
  Configuracao bloquear();
}
