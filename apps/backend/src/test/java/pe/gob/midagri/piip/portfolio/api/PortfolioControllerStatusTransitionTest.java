package pe.gob.midagri.piip.portfolio.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.gob.midagri.piip.portfolio.application.InitiativeApplicationService;
import pe.gob.midagri.piip.portfolio.application.PortfolioQueryService;
import pe.gob.midagri.piip.portfolio.application.ProjectApplicationService;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.shared.api.ApiExceptionHandler;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerStatusTransitionTest {
    @Mock InitiativeApplicationService initiatives;
    @Mock ProjectApplicationService projects;
    @Mock PortfolioQueryService queries;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PortfolioController(queries, initiatives, projects))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void returnsBadRequestForInvalidTransitionRequest() throws Exception {
        mvc.perform(post("/projects/P-001-2026/status-transitions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":null,\"targetStatus\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void validatesTheInitiativeTransitionRequestSeparatelyFromProjectRequest() throws Exception {
        mvc.perform(post("/initiatives/I-001-2026/status-transitions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":null,\"targetStatus\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void mapsAuthorizationFailureToForbidden() throws Exception {
        when(projects.transition(any(), any())).thenThrow(new AccessDeniedException("fuera de ámbito"));
        performValidProjectTransition().andExpect(status().isForbidden());
    }

    @Test
    void mapsMissingRecordToNotFound() throws Exception {
        when(projects.transition(any(), any())).thenThrow(new NotFoundException("Proyecto inexistente"));
        performValidProjectTransition().andExpect(status().isNotFound());
    }

    @Test
    void mapsStaleVersionToConflict() throws Exception {
        when(projects.transition(any(), any())).thenThrow(new StaleVersionException());
        performValidProjectTransition().andExpect(status().isConflict());
    }

    @Test
    void mapsDisallowedTransitionToUnprocessableEntity() throws Exception {
        when(projects.transition(any(), any())).thenThrow(new BusinessRuleException("Transición no permitida"));
        performValidProjectTransition().andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions performValidProjectTransition() throws Exception {
        return mvc.perform(post("/projects/P-001-2026/status-transitions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":0,\"targetStatus\":\"PRODUCT_APPROVED\",\"observation\":\"evaluado\"}"));
    }
}
