package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TabelaPrecoRepository extends JpaRepository<TabelaPreco, Long> {
  Optional<TabelaPreco> findByAnoAndQuantidadeTurnosAndPercentualAndAtivoTrue(
      int ano, int quantidadeTurnos, int percentual);
}
