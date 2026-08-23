package pe.gob.midagri.piip.portfolio.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.TechnicalCatalogItemResponse;
import pe.gob.midagri.piip.portfolio.application.InitiativeApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioQueryService;
import pe.gob.midagri.piip.portfolio.application.ProjectApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.InitiativeUpdateCommand;
import pe.gob.midagri.piip.portfolio.application.PortfolioUpdateCommands.ProjectUpdateCommand;
import pe.gob.midagri.piip.shared.api.ApiExceptionHandler;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerUpdateContractTest {
    @Mock InitiativeApplicationService initiatives;
    @Mock ProjectApplicationService projects;
    @Mock PortfolioQueryService queries;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PortfolioController(queries, initiatives, projects))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void acceptsSparseInitiativePatchAndReturnsCompleteRepresentation() throws Exception {
        when(initiatives.update(anyString(), any())).thenReturn(response("I-001-2026", "Nueva iniciativa", 1L));

        mvc.perform(patch("/initiatives/I-001-2026")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"name\":\"Nueva iniciativa\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("I-001-2026"))
            .andExpect(jsonPath("$.name").value("Nueva iniciativa"))
            .andExpect(jsonPath("$.version").value(1));

        ArgumentCaptor<InitiativeUpdateCommand> command = ArgumentCaptor.forClass(InitiativeUpdateCommand.class);
        verify(initiatives).update(org.mockito.ArgumentMatchers.eq("I-001-2026"), command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().version()).isZero();
        org.assertj.core.api.Assertions.assertThat(command.getValue().name().present()).isTrue();
        org.assertj.core.api.Assertions.assertThat(command.getValue().description().present()).isFalse();
    }

    @Test
    void acceptsSparseProjectPatchWithExplicitNullForOptionalField() throws Exception {
        when(projects.update(anyString(), any())).thenReturn(response("P-001-2026", "Proyecto", 2L));

        mvc.perform(patch("/projects/P-001-2026")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"peiObjectiveId\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("P-001-2026"))
            .andExpect(jsonPath("$.version").value(2));

        ArgumentCaptor<ProjectUpdateCommand> command = ArgumentCaptor.forClass(ProjectUpdateCommand.class);
        verify(projects).update(org.mockito.ArgumentMatchers.eq("P-001-2026"), command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().peiObjectiveId().present()).isTrue();
        org.assertj.core.api.Assertions.assertThat(command.getValue().peiObjectiveId().value()).isNull();
    }

    @Test
    void rejectsUnknownTechnicalPropertyWithBadRequest() throws Exception {
        mvc.perform(patch("/initiatives/I-001-2026")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"status\":\"Presentado\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void mapsUpdateFailuresToTheDeclaredProblemStatuses() throws Exception {
        doThrow(new AccessDeniedException("fuera de ámbito")).when(initiatives).update(anyString(), any());
        performInitiativePatch().andExpect(status().isForbidden());

        doThrow(new NotFoundException("inexistente")).when(initiatives).update(anyString(), any());
        performInitiativePatch().andExpect(status().isNotFound());

        doThrow(new StaleVersionException()).when(initiatives).update(anyString(), any());
        performInitiativePatch().andExpect(status().isConflict());

        doThrow(new BusinessRuleException("no editable")).when(initiatives).update(anyString(), any());
        performInitiativePatch().andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions performInitiativePatch() throws Exception {
        return mvc.perform(patch("/initiatives/I-001-2026")
            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"name\":\"Cambio\"}"));
    }

    private PortfolioDtos.PortfolioRecordResponse response(String code, String name, long version) {
        return new PortfolioDtos.PortfolioRecordResponse(new TechnicalCatalogItemResponse("INITIATIVE", "Iniciativa", 0, true),
            code, "NA", name, null, null, null, null, null, null, List.of(), "Descripción", null, null,
            "Presentado", "No aplica", "No", null, null, null, null, null, null, 7L, "UE", null, version);
    }
}
