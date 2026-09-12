package pe.gob.midagri.piip.organization.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationReadModels;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationService;

import static pe.gob.midagri.piip.organization.api.OrganizationAdministrationDtos.*;

class OrganizationAdministrationControllerTest {
    private final OrganizationAdministrationService service = mock(OrganizationAdministrationService.class);
    private final OrganizationAdministrationHttpMapper mapper = new OrganizationAdministrationHttpMapper();
    private final OrganizationAdministrationController controller = new OrganizationAdministrationController(service, mapper);

    @Test
    void rejectsContextAndParentFieldsInAdministrativePayloads() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> objectMapper.readValue(
            "{\"name\":\"UE\",\"code\":\"UE-001\"}", CreateExecutingUnitRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
        assertThatThrownBy(() -> objectMapper.readValue(
            "{\"name\":\"UO\",\"acronym\":\"UO\",\"active\":true,\"parentId\":3}",
            CreateOrganizationalUnitRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    @Test
    void mapsAdministrativeResponsesWithoutParentOrPersistenceTypes() {
        var institution = new OrganizationAdministrationReadModels.InstitutionReference(10L, "MIDAGRI", "MIDAGRI");
        var unit = new OrganizationAdministrationReadModels.ExecutingUnit(100L, "UE-001", "UE", true, 0,
            Instant.parse("2026-09-11T12:00:00Z"), Instant.parse("2026-09-11T12:00:00Z"), 0L, institution);
        when(service.executingUnits(10L)).thenReturn(List.of(unit));

        var response = controller.executingUnits(10L);

        assertThat(response).singleElement().satisfies(value -> {
            assertThat(value.code()).isEqualTo("UE-001");
            assertThat(value.institution().id()).isEqualTo(10L);
        });
        assertThat(OrganizationAdministrationDtos.OrganizationalUnitResponse.class.getDeclaredFields())
            .extracting(java.lang.reflect.Field::getName).doesNotContain("parent", "parentId");
    }
}
