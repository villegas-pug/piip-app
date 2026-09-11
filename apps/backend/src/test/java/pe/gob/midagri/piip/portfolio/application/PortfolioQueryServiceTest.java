package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@ExtendWith(MockitoExtension.class)
class PortfolioQueryServiceTest {
    @Mock pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository records;
    @Mock PortfolioReadModelAssembler assembler;
    @Mock LocalAuthorizationService authorization;

    private PortfolioQueryService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioQueryService(records,
            new PortfolioApplicationSupport(authorization, java.time.Clock.systemUTC()), assembler);
        lenient().when(authorization.requireAuthenticatedRole()).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        lenient().when(records.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));
    }

    @Test
    void listFiltersByCodeAndPreservesPagination() {
        var result = service.list(RecordType.PROJECT, "x", "PRESENTED", 100L, 1, 10, "updatedAt", "asc");

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    void listRejectsLegacyLabelsWithNotFoundCause() {
        assertThatThrownBy(() -> service.list(RecordType.INITIATIVE, null, "Presentado", null, 0, 10, "updatedAt", "desc"))
            .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                assertThat(exception.getProblemCode()).isEqualTo(ProblemCode.PORTFOLIO_STATUS_NOT_FOUND));
    }

    @Test
    void listAcceptsAnInactiveCatalogCodeByDirectCall() {
        // parseStatus no consulta la actividad: un código del catálogo inactivo sigue consultable (D6).
        var result = service.list(RecordType.INITIATIVE, null, "INITIATIVE_ARCHIVED", null, 0, 10, "updatedAt", "desc");

        assertThat(result.totalElements()).isZero();
    }

    @Test
    void eligibleInitiativesSelectByCodeRegardlessOfCatalogMetadata() {
        when(authorization.require(RoleCode.ADMINISTRADOR_PIIP)).thenReturn(actor());
        PortfolioRecordEntity approved = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-APPROVED", unit(), "Iniciativa aprobada");
        ReflectionTestUtils.setField(approved, "id", 7L);
        approved.approve();
        // statusCatalog es null (registro transitorio sin metadata); la elegibilidad usa exclusivamente el código.
        when(records.findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType.INITIATIVE, PortfolioStatus.INITIATIVE_APPROVED))
            .thenReturn(List.of(approved));
        when(records.existsByOriginRecordId(7L)).thenReturn(false);
        when(assembler.toResponse(approved)).thenReturn(response("I-APPROVED"));

        assertThat(service.eligibleInitiatives()).extracting(PortfolioRecordResponse::code).containsExactly("I-APPROVED");
    }

    private ExecutingUnitEntity unit() {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ReflectionTestUtils.setField(institution, "id", 100L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 10L);
        return unit;
    }

    private LocalAccessContext actor() {
        return new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 100L, 10L)));
    }

    private PortfolioRecordResponse response(String code) {
        return new PortfolioRecordResponse(null, code, null, null, null, null, null, null, null, null,
            List.of(), null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, 0L);
    }
}
