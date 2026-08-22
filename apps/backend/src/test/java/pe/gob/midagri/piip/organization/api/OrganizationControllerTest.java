package pe.gob.midagri.piip.organization.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.organization.application.OrganizationQueryService;
import pe.gob.midagri.piip.organization.application.OrganizationReadModels;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {
    @Mock OrganizationQueryService service;

    @Test
    void mapsOrganizationalUnitsWithoutExposingPersistenceEntities() {
        when(service.organizationalUnits(100L)).thenReturn(List.of(
            new OrganizationReadModels.OrganizationalUnitView(1L, "UO-1", "Unidad", true, "U1", null, 100L)));

        var response = new OrganizationController(service).organizationalUnits(100L);

        assertThat(response).singleElement().satisfies(value -> {
            assertThat(value.id()).isEqualTo(1L);
            assertThat(value.executingUnitId()).isEqualTo(100L);
        });
    }
}
