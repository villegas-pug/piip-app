package pe.gob.midagri.piip.identity.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;

class UserAdministrationControllerTest {
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
}
