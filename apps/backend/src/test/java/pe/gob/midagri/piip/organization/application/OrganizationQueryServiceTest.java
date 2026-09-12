package pe.gob.midagri.piip.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationQueryServiceTest {
    @Mock InstitutionRepository institutions;
    @Mock ExecutingUnitRepository executingUnits;
    @Mock OrganizationalUnitRepository organizationalUnits;
    @Mock LocalAuthorizationService authorization;

    @Test
    void filtersInstitutionsAndUnitsByTheExactAuthenticatedScope() {
        InstitutionEntity visible = institution(10L, "MIDAGRI");
        InstitutionEntity hidden = institution(20L, "OTRA");
        ExecutingUnitEntity unit = unit(100L, visible);
        LocalAccessContext access = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L)));
        when(authorization.requireAuthenticatedRole()).thenReturn(access);
        when(institutions.findAll()).thenReturn(List.of(visible, hidden));
        when(executingUnits.findAll()).thenReturn(List.of(unit));

        OrganizationQueryService service = new OrganizationQueryService(institutions, executingUnits, organizationalUnits, authorization);

        assertThat(service.institutions()).extracting(OrganizationReadModels.InstitutionView::id).containsExactly(10L);
        assertThat(service.executingUnits()).extracting(OrganizationReadModels.ExecutingUnitView::id).containsExactly(100L);
    }

    @Test
    void organizationalUnitsHideActiveUnitsWithBlankOrNullAcronym() {
        ExecutingUnitEntity unit = unit(100L, institution(10L, "MIDAGRI"));
        OrganizationalUnitEntity complete = organizationalUnit(201L, unit, "UO-1", "Unidad con sigla", "U1");
        OrganizationalUnitEntity blank = organizationalUnit(202L, unit, "UO-2", "Unidad con sigla en blanco", "   ");
        OrganizationalUnitEntity missing = organizationalUnit(203L, unit, "UO-3", "Unidad sin sigla", null);
        when(organizationalUnits.findByExecutingUnitIdAndActiveTrueAndAcronymIsNotNullOrderByName(100L))
            .thenReturn(List.of(complete, blank, missing));
        OrganizationQueryService service = new OrganizationQueryService(institutions, executingUnits, organizationalUnits, authorization);

        List<OrganizationReadModels.OrganizationalUnitView> result = service.organizationalUnits(100L);

        // FR-012: el catálogo ofrece solo unidades con sigla no vacía; la sigla en blanco supera el
        // filtro del repositorio y la null se cubre además como defensa en profundidad del servicio.
        assertThat(result).extracting(OrganizationReadModels.OrganizationalUnitView::id).containsExactly(201L);
        assertThat(result).extracting(OrganizationReadModels.OrganizationalUnitView::acronym).containsExactly("U1");
        verify(organizationalUnits).findByExecutingUnitIdAndActiveTrueAndAcronymIsNotNullOrderByName(100L);
    }

    private OrganizationalUnitEntity organizationalUnit(Long id, ExecutingUnitEntity unit, String code, String name,
            String acronym) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(unit, code, name, acronym);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private InstitutionEntity institution(Long id, String code) {
        InstitutionEntity value = new InstitutionEntity(code, code);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private ExecutingUnitEntity unit(Long id, InstitutionEntity institution) {
        ExecutingUnitEntity value = new ExecutingUnitEntity(institution, "UE-" + id, "Unidad");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
