package pe.gob.midagri.piip.portfolio.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;

/** Commands de aplicación independientes del binding HTTP para edición parcial. */
public final class PortfolioUpdateCommands {
    private PortfolioUpdateCommands() {}

    public record FieldUpdate<T>(boolean present, T value) {
        public static <T> FieldUpdate<T> absent() { return new FieldUpdate<>(false, null); }
        public static <T> FieldUpdate<T> of(T value) { return new FieldUpdate<>(true, value); }
    }

    public record ResponsibleUnitUpdate(Long organizationalUnitId) {
        public ResponsibleUnitUpdate {
            Objects.requireNonNull(organizationalUnitId, "organizationalUnitId");
        }
    }

    public record InitiativeUpdateCommand(long version, FieldUpdate<String> name,
            FieldUpdate<Long> solutionTypeId, FieldUpdate<Long> sourceId, FieldUpdate<LocalDate> startDate,
            FieldUpdate<String> responsible, FieldUpdate<Long> peiObjectiveId, FieldUpdate<Long> poiActivityId,
            FieldUpdate<List<ResponsibleUnitUpdate>> responsibleUnits, FieldUpdate<String> description,
            FieldUpdate<String> note, FieldUpdate<DigitalComponent> digitalComponent) {
        public boolean hasEditableField() {
            return name.present() || solutionTypeId.present() || sourceId.present() || startDate.present()
                || responsible.present() || peiObjectiveId.present() || poiActivityId.present()
                || responsibleUnits.present() || description.present() || note.present() || digitalComponent.present();
        }
    }

    public record ProjectUpdateCommand(long version, FieldUpdate<String> name,
            FieldUpdate<Long> solutionTypeId, FieldUpdate<Long> sourceId, FieldUpdate<LocalDate> startDate,
            FieldUpdate<String> responsible, FieldUpdate<Long> peiObjectiveId, FieldUpdate<Long> poiActivityId,
            FieldUpdate<List<ResponsibleUnitUpdate>> responsibleUnits, FieldUpdate<String> description,
            FieldUpdate<String> keyResults, FieldUpdate<String> note, FieldUpdate<DigitalComponent> digitalComponent) {
        public boolean hasEditableField() {
            return name.present() || solutionTypeId.present() || sourceId.present() || startDate.present()
                || responsible.present() || peiObjectiveId.present() || poiActivityId.present()
                || responsibleUnits.present() || description.present() || keyResults.present()
                || note.present() || digitalComponent.present();
        }
    }
}
