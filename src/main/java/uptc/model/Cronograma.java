package uptc.model;

import java.time.LocalDate;

/** Fechas estimadas de una actividad. Es un record: equals compara las fechas. */
public record Cronograma(LocalDate inicio, LocalDate fin) {
}
