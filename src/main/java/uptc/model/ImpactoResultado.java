package uptc.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Resultado de simular un cambio (RF08). No modifica el proyecto real. */
public record ImpactoResultado(LocalDate finAntes,
                               LocalDate finDespues,
                               List<String> actividadesAfectadas) {

    public long diasDeDiferencia() {
        return ChronoUnit.DAYS.between(finAntes, finDespues);
    }
}
