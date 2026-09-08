package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;

/**
 * Pruebas de la postvalidación sintética del reset (FR-027/FR-029/FR-030): el conjunto válido pasa,
 * los datos incompletos, duplicados o con asociación incorrecta fallan de forma segura por la vía
 * del stage POST_VALIDATION y una segunda ejecución sobre el mismo conjunto vuelve a pasar.
 */
class TestResetPostValidationTest {

    @Test
    void conjuntoSinteticoValidoPasaLaPostvalidacion() {
        assertThatCode(() -> postvalidar(semillaValida())).doesNotThrowAnyException();
    }

    @Test
    void segundaEjecucionSobreElMismoConjuntoVuelveAPasarSinDuplicados() {
        Semilla semilla = semillaValida();

        // La segunda ejecución autorizada reutiliza el mismo conjunto MERGE sin duplicados (FR-029);
        // la repetibilidad de la postvalidación lo confirma sin estado residual entre corridas.
        assertThatCode(() -> postvalidar(semilla)).doesNotThrowAnyException();
        assertThatCode(() -> postvalidar(semilla)).doesNotThrowAnyException();
    }

    @Test
    void unidadSinteticaConSiglaVaciaONulaFallaDeFormaSegura() {
        ExecutingUnitEntity ejecutora = ue("UE-001");
        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidad(ejecutora, "UE-001-UO-02", "   ", true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética UE-001-UO-02 no tiene sigla registrada");

        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidad(ejecutora, "UE-001-UO-02", null, true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética UE-001-UO-02 no tiene sigla registrada");
    }

    @Test
    void asociacionUnidadEjecutoraIncorrectaFallaDeFormaSegura() {
        ExecutingUnitEntity ejecutora = ue("UE-002");
        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidad(ejecutora, "UE-002-UO-02", "UO2", true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética UE-001-UO-01 no pertenece a la Unidad Ejecutora esperada UE-002");
    }

    @Test
    void datosFaltantesFallaDeFormaSegura() {
        ExecutingUnitEntity ejecutora = ue("UE-001");
        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "   ", "UO1", true), unidad(ejecutora, "UE-001-UO-02", "UO2", true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética de la Unidad Ejecutora UE-001 tiene el código vacío");

        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidadConNombre(ejecutora, "UE-001-UO-02", "  ", "UO2", true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética UE-001-UO-02 tiene el nombre vacío");

        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Ejecutora sintética UE-001 no dispone de dos Unidades Orgánicas activas");
    }

    @Test
    void codigoDuplicadoDentroDeLaUnidadEjecutoraFallaDeFormaSegura() {
        ExecutingUnitEntity ejecutora = ue("UE-001");
        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidad(ejecutora, "UE-001-UO-01", "UO2", true)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Orgánica sintética UE-001-UO-01 está duplicada en la Unidad Ejecutora UE-001");
    }

    @Test
    void menosDeDosUnidadesActivasPorUnidadEjecutoraFallaDeFormaSegura() {
        ExecutingUnitEntity ejecutora = ue("UE-001");
        assertThatThrownBy(() -> TestResetCoordinator.stage(TestResetStage.POST_VALIDATION.name(),
            () -> postvalidar(semillaDe(ejecutora,
                unidad(ejecutora, "UE-001-UO-01", "UO1", true), unidad(ejecutora, "UE-001-UO-02", "UO2", false)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Falló la etapa test-reset POST_VALIDATION")
            .hasRootCauseMessage("La Unidad Ejecutora sintética UE-001 no dispone de dos Unidades Orgánicas activas");
    }

    private static void postvalidar(Semilla semilla) {
        TestResetCoordinator.validateSyntheticOrganization(semilla.unidadesEjecutoras(), semilla.unidadesOrganicas()::get);
    }

    /** Conjunto sintético vigente del seed: dos UEs con dos UOs activas cada una, código, nombre y sigla completos. */
    private static Semilla semillaValida() {
        ExecutingUnitEntity primera = ue("UE-001");
        ExecutingUnitEntity segunda = ue("UE-002");
        return new Semilla(List.of(primera, segunda), Map.of(
            primera.getId(), List.of(unidad(primera, "UE-001-UO-01", "UO1", true), unidad(primera, "UE-001-UO-02", "UO2", true)),
            segunda.getId(), List.of(unidad(segunda, "UE-002-UO-01", "UO1", true), unidad(segunda, "UE-002-UO-02", "UO2", true))));
    }

    private static Semilla semillaDe(ExecutingUnitEntity ejecutora, OrganizationalUnitEntity... unidades) {
        Map<Long, List<OrganizationalUnitEntity>> porEjecutora = new HashMap<>();
        porEjecutora.put(ejecutora.getId(), List.of(unidades));
        return new Semilla(List.of(ejecutora), porEjecutora);
    }

    private static ExecutingUnitEntity ue(String codigo) {
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI", "MIDAGRI");
        ExecutingUnitEntity value = new ExecutingUnitEntity(institution, codigo, codigo);
        ReflectionTestUtils.setField(value, "id", (long) codigo.hashCode());
        return value;
    }

    /** Unidad sintética con nombre igual al código, como el seed versionado. */
    private static OrganizationalUnitEntity unidad(ExecutingUnitEntity ejecutora, String codigo, String sigla, boolean activa) {
        return unidadConNombre(ejecutora, codigo, codigo, sigla, activa);
    }

    private static OrganizationalUnitEntity unidadConNombre(ExecutingUnitEntity ejecutora, String codigo, String nombre,
            String sigla, boolean activa) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(ejecutora, codigo, nombre, sigla);
        ReflectionTestUtils.setField(value, "active", activa);
        return value;
    }

    private record Semilla(List<ExecutingUnitEntity> unidadesEjecutoras,
            Map<Long, List<OrganizationalUnitEntity>> unidadesOrganicas) {}
}
