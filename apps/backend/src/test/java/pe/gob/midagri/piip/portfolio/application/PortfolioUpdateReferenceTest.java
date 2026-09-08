package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ResponsibleUnitUpdate;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

class PortfolioUpdateReferenceTest {
    @Test
    void rejectsEmptyResponsibleUnitListWithoutDeletingCurrentAssociations() {
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        ResponsibleUnitService service = new ResponsibleUnitService(responsible, mock(OrganizationalUnitRepository.class));

        assertThatThrownBy(() -> service.replace(record(), List.of()))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("INVALID_SIZE"))
            .hasMessageContaining("al menos una unidad");
        verifyNoInteractions(responsible);
    }

    @Test
    void rejectsDuplicateResponsibleUnitIdentifyingTheDuplicatedRow() {
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        OrganizationalUnitRepository organizational = mock(OrganizationalUnitRepository.class);
        ResponsibleUnitService service = new ResponsibleUnitService(responsible, organizational);

        assertThatThrownBy(() -> service.replace(record(), List.of(new ResponsibleUnitUpdate(8L), new ResponsibleUnitUpdate(8L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception -> {
                assertThat(exception.getReason()).isEqualTo("DUPLICATED_UNIT");
                assertThat(exception.getReferenceField()).isEqualTo("responsibleUnits[2]");
                assertThat(exception.getReferenceId()).isEqualTo(8L);
            })
            .hasMessageContaining("fila 2").hasMessageContaining("fila 1");
        verifyNoInteractions(responsible, organizational);
    }

    @Test
    void replacesWithTwoUnitsOfTheSameExecutingUnitPersistingContinuousOrder() {
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        OrganizationalUnitRepository organizational = mock(OrganizationalUnitRepository.class);
        ExecutingUnitEntity same = executingUnit(5L);
        when(organizational.findHistoricalById(8L)).thenReturn(Optional.of(organizationalUnit(8L, same, true)));
        when(organizational.findHistoricalById(9L)).thenReturn(Optional.of(organizationalUnit(9L, same, true)));
        when(responsible.findByRecordIdOrderByDisplayOrder(any())).thenReturn(List.of());
        ResponsibleUnitService service = new ResponsibleUnitService(responsible, organizational);

        service.replace(record(), List.of(new ResponsibleUnitUpdate(8L), new ResponsibleUnitUpdate(9L)));

        var saved = ArgumentCaptor.forClass(ResponsibleUnitEntity.class);
        verify(responsible, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(unit -> unit.getOrganizationalUnit().getId())
            .containsExactly(8L, 9L);
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
    }

    @Test
    void replacesWithTheSubmittedUnitAndRejectsInactiveReferences() {
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        OrganizationalUnitRepository organizational = mock(OrganizationalUnitRepository.class);
        ExecutingUnitEntity same = executingUnit(5L);
        OrganizationalUnitEntity first = organizationalUnit(8L, same, true);
        when(organizational.findHistoricalById(8L)).thenReturn(Optional.of(first));
        when(responsible.findByRecordIdOrderByDisplayOrder(any())).thenReturn(List.of());
        ResponsibleUnitService service = new ResponsibleUnitService(responsible, organizational);

        service.replace(record(), List.of(new ResponsibleUnitUpdate(8L)));

        verify(responsible).save(argThat(value -> value.getOrganizationalUnit() == first && value.getDisplayOrder() == 1));
        ReflectionTestUtils.setField(first, "active", false);
        assertThatThrownBy(() -> service.replace(record(), List.of(new ResponsibleUnitUpdate(8L))))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("inactiva");
    }

    private PortfolioRecordEntity record() {
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-REF-2026", executingUnit(5L), "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 50L);
        return record;
    }

    private ExecutingUnitEntity executingUnit(Long id) {
        InstitutionEntity institution = new InstitutionEntity("INST-REF-" + id, "Institución");
        ReflectionTestUtils.setField(institution, "id", id + 100L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-REF-" + id, "Unidad");
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private OrganizationalUnitEntity organizationalUnit(Long id, ExecutingUnitEntity unit, boolean active) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(unit, "UO-REF-" + id, "Unidad " + id, "U");
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "active", active);
        return value;
    }
}
