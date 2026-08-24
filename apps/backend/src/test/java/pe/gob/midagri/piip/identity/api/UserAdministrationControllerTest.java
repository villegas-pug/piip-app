package pe.gob.midagri.piip.identity.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.gob.midagri.piip.identity.application.UserAdministrationReadModels;
import pe.gob.midagri.piip.identity.application.UserAdministrationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class UserAdministrationControllerTest {
    @Mock UserAdministrationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(new UserAdministrationController(service, new UserAdministrationHttpMapper())).build();
    }

    @Test
    void publishesTheExpectedAssignmentMutationRoutesWithoutUserStatusRoute() throws NoSuchMethodException {
        assertThat(UserAdministrationController.class.getDeclaredMethod("update", Long.class, long.class, AdminDtos.RoleAssignmentUpdateRequest.class)
            .getAnnotation(PutMapping.class).value()).containsExactly("/role-assignments/{scopeId}");
        assertThat(UserAdministrationController.class.getDeclaredMethod("reactivate", Long.class, long.class)
            .getAnnotation(PutMapping.class).value()).containsExactly("/role-assignments/{scopeId}/reactivation");
        assertThat(UserAdministrationController.class.getDeclaredMethods())
            .extracting(method -> method.getName())
            .doesNotContain("changeStatus");
    }

    @Test
    void publishesTheAssignmentCandidateRouteAsJson() throws NoSuchMethodException {
        GetMapping mapping = UserAdministrationController.class.getDeclaredMethod("assignmentCandidates")
            .getAnnotation(GetMapping.class);

        assertThat(mapping.value()).containsExactly("/users/assignment-candidates");
        assertThat(mapping.produces()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void publishesTheAdministrableScopesRouteAsJson() throws NoSuchMethodException {
        GetMapping mapping = UserAdministrationController.class.getDeclaredMethod("administrableScopes")
            .getAnnotation(GetMapping.class);

        assertThat(mapping.value()).containsExactly("/users/administrable-scopes");
        assertThat(mapping.produces()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void exposesCreationAndReactivationWithDistinctStatusesAndBodies() throws Exception {
        var created = new UserAdministrationReadModels.AssignmentMutationResult(UserAdministrationReadModels.AssignmentMutationStatus.CREATED, scope());
        var reactivated = new UserAdministrationReadModels.AssignmentMutationResult(UserAdministrationReadModels.AssignmentMutationStatus.REACTIVATED, scope());
        org.mockito.Mockito.when(service.assign(any())).thenReturn(created, reactivated);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/role-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userSubject\":\"subject\",\"role\":\"CONSULTA_EXTERNA\",\"institutionId\":10}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(20));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/role-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userSubject\":\"subject\",\"role\":\"CONSULTA_EXTERNA\",\"institutionId\":10}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version").value(3));
    }

    @Test
    void exposesUpdateAndReactivationAsJsonAndSuspensionWithoutBody() throws Exception {
        org.mockito.Mockito.when(service.update(org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(scope());
        org.mockito.Mockito.when(service.reactivate(20L, 2L)).thenReturn(scope());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/admin/role-assignments/20?version=2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"CONSULTA_EXTERNA\",\"institutionId\":10}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/admin/role-assignments/20/reactivation?version=2"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.active").value(true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/admin/role-assignments/20?version=2"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
    }

    private UserAdministrationReadModels.Scope scope() {
        return new UserAdministrationReadModels.Scope(20L, RoleCode.CONSULTA_EXTERNA, 10L, "Institución",
            null, "Todas", true, Instant.parse("2026-01-01T00:00:00Z"), null, 3L);
    }
}
