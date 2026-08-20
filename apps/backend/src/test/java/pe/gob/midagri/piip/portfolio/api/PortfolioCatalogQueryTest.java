package pe.gob.midagri.piip.portfolio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.portfolio.application.*;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.*;

class PortfolioCatalogQueryTest {
    @Test void listadosConservanSoloLosCincoParametrosHttpAprobados() {
        for (String methodName : List.of("initiatives", "projects")) {
            var method = Arrays.stream(PortfolioController.class.getDeclaredMethods()).filter(value -> value.getName().equals(methodName)).findFirst().orElseThrow();
            assertThat(Arrays.stream(method.getParameters()).map(parameter -> parameter.getAnnotation(RequestParam.class).value()).toList())
                .containsExactly("q", "status", "executingUnitId", "page", "size");
        }
    }

    @Test void lecturaHistoricaExponeReferenciaEstructuradaAunqueEsteInactiva() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        InstitutionEntity institution = new InstitutionEntity("I", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 5L);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative("I-01", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 7L);
        ReflectionTestUtils.setField(record.getSolutionType(), "id", 11L);
        ReflectionTestUtils.setField(record.getSolutionType(), "active", false);
        ReflectionTestUtils.setField(record.getSourceOrigin(), "id", 12L);
        when(records.findByCodeIgnoreCase("I-01")).thenReturn(Optional.of(record));
        when(responsible.findByRecordIdOrderByDisplayOrder(7L)).thenReturn(List.of());

        PortfolioService service = new PortfolioService(records, responsible, mock(ExecutingUnitRepository.class),
            mock(OrganizationalUnitRepository.class), mock(pe.gob.midagri.piip.identity.persistence.UserRepository.class),
            mock(WorkTaskRepository.class), mock(NotificationRepository.class), mock(DocumentRepository.class),
            mock(CodeGeneratorService.class), authorization, mock(AuditService.class), mock(CatalogReferenceService.class),
            mock(DocumentTypeRepository.class));
        var response = service.get("I-01");

        assertThat(response.solutionType().id()).isEqualTo(11L);
        assertThat(response.solutionType().code()).isEqualTo("TO_BE_DEFINED");
        assertThat(response.solutionType().active()).isFalse();
        assertThat(response.source().id()).isEqualTo(12L);
    }
}
