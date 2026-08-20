package pe.gob.midagri.piip.architecture;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.midagri.piip.documents.api.DocumentController;
import pe.gob.midagri.piip.documents.api.DocumentDtos.NotApplicableRequest;
import pe.gob.midagri.piip.identity.api.UserAdministrationController;
import pe.gob.midagri.piip.organization.api.OrganizationController;
import pe.gob.midagri.piip.portfolio.api.PortfolioController;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ApprovalRequest;
import pe.gob.midagri.piip.work.api.NotificationController;
import pe.gob.midagri.piip.work.api.WorkController;

import static org.assertj.core.api.Assertions.assertThat;

class HttpParameterBindingTest {
    @Test
    void declaresThePublicNameForEveryPathQueryAndMultipartBinding() throws NoSuchMethodException {
        List<Binding> bindings = List.of(
            binding(OrganizationController.class, "organizationalUnits", 0, RequestParam.class, "executingUnitId", Long.class),
            binding(DocumentController.class, "list", 0, PathVariable.class, "recordCode", String.class),
            binding(DocumentController.class, "upload", 0, PathVariable.class, "recordCode", String.class, Long.class, MultipartFile.class),
            binding(DocumentController.class, "upload", 1, PathVariable.class, "documentTypeId", String.class, Long.class, MultipartFile.class),
            binding(DocumentController.class, "upload", 2, RequestPart.class, "file", String.class, Long.class, MultipartFile.class),
            binding(DocumentController.class, "notApplicable", 0, PathVariable.class, "recordCode", String.class, Long.class, NotApplicableRequest.class),
            binding(DocumentController.class, "notApplicable", 1, PathVariable.class, "documentTypeId", String.class, Long.class, NotApplicableRequest.class),
            binding(DocumentController.class, "publication", 0, PathVariable.class, "recordCode", String.class, Long.class, boolean.class, long.class),
            binding(DocumentController.class, "publication", 1, PathVariable.class, "versionId", String.class, Long.class, boolean.class, long.class),
            binding(DocumentController.class, "publication", 2, RequestParam.class, "published", String.class, Long.class, boolean.class, long.class),
            binding(DocumentController.class, "publication", 3, RequestParam.class, "version", String.class, Long.class, boolean.class, long.class),
            binding(DocumentController.class, "download", 0, PathVariable.class, "recordCode", String.class, Long.class),
            binding(DocumentController.class, "download", 1, PathVariable.class, "versionId", String.class, Long.class),
            binding(PortfolioController.class, "initiatives", 0, RequestParam.class, "q", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "initiatives", 1, RequestParam.class, "status", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "initiatives", 2, RequestParam.class, "executingUnitId", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "initiatives", 3, RequestParam.class, "page", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "initiatives", 4, RequestParam.class, "size", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "initiative", 0, PathVariable.class, "code", String.class),
            binding(PortfolioController.class, "approve", 0, PathVariable.class, "code", String.class, ApprovalRequest.class),
            binding(PortfolioController.class, "projects", 0, RequestParam.class, "q", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "projects", 1, RequestParam.class, "status", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "projects", 2, RequestParam.class, "executingUnitId", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "projects", 3, RequestParam.class, "page", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "projects", 4, RequestParam.class, "size", String.class, String.class, Long.class, int.class, int.class),
            binding(PortfolioController.class, "project", 0, PathVariable.class, "code", String.class),
            binding(UserAdministrationController.class, "suspend", 0, PathVariable.class, "scopeId", Long.class, long.class),
            binding(UserAdministrationController.class, "suspend", 1, RequestParam.class, "version", Long.class, long.class),
            binding(WorkController.class, "complete", 0, PathVariable.class, "taskId", Long.class, long.class),
            binding(WorkController.class, "complete", 1, RequestParam.class, "version", Long.class, long.class),
            binding(WorkController.class, "reassign", 0, PathVariable.class, "taskId", Long.class, WorkController.ReassignRequest.class),
            binding(NotificationController.class, "read", 0, PathVariable.class, "id", Long.class)
        );

        assertThat(bindings).hasSize(32);
        for (Binding binding : bindings) {
            Method method = binding.controller().getDeclaredMethod(binding.method(), binding.parameterTypes());
            Annotation annotation = method.getParameters()[binding.parameterIndex()].getAnnotation(binding.annotationType());

            assertThat(annotation)
                .as("%s#%s parameter %s", binding.controller().getSimpleName(), binding.method(), binding.parameterIndex())
                .isNotNull();
            assertThat(publicName(annotation)).isEqualTo(binding.publicName());
        }
    }

    private static Binding binding(Class<?> controller, String method, int parameterIndex,
            Class<? extends Annotation> annotationType, String publicName, Class<?>... parameterTypes) {
        return new Binding(controller, method, parameterIndex, annotationType, publicName, parameterTypes);
    }

    private static String publicName(Annotation annotation) {
        return switch (annotation) {
            case PathVariable pathVariable -> pathVariable.value();
            case RequestParam requestParam -> requestParam.value();
            case RequestPart requestPart -> requestPart.value();
            default -> throw new IllegalArgumentException("Anotación HTTP no soportada: " + annotation.annotationType().getName());
        };
    }

    private record Binding(Class<?> controller, String method, int parameterIndex,
            Class<? extends Annotation> annotationType, String publicName, Class<?>[] parameterTypes) {
    }
}
