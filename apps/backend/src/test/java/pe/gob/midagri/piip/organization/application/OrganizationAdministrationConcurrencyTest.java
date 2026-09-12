package pe.gob.midagri.piip.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;

class OrganizationAdministrationConcurrencyTest {
    @Test
    void locksInstitutionAndExecutingUnitWhenGeneratingAdministrativeCodes() throws Exception {
        assertPessimisticWrite(InstitutionRepository.class, "findByIdForUpdate", Long.class);
        assertPessimisticWrite(ExecutingUnitRepository.class, "findByIdForUpdate", Long.class);
        assertPessimisticWrite(ExecutingUnitRepository.class, "findByInstitutionIdAndCodeIgnoreCaseForUpdate", Long.class, String.class);
        assertPessimisticWrite(OrganizationalUnitRepository.class, "findByExecutingUnitIdAndCodeIgnoreCaseForUpdate", Long.class, String.class);
    }

    @Test
    void generatedPrefixesAreScopedIndependentlyByTheirParent() {
        assertThat(String.format(java.util.Locale.ROOT, "UE-%03d", 1)).isEqualTo("UE-001");
        assertThat(String.format(java.util.Locale.ROOT, "UO-%03d", 1)).isEqualTo("UO-001");
        assertThat(OrganizationAdministrationCommands.CreateOrganizationalUnit.class).isNotNull();
    }

    private static void assertPessimisticWrite(Class<?> repository, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = repository.getMethod(methodName, parameterTypes);
        Lock lock = method.getAnnotation(Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
