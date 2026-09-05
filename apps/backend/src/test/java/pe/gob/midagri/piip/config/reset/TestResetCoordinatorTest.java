package pe.gob.midagri.piip.config.reset;

import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.junit.jupiter.api.Test;

class TestResetCoordinatorTest {
    @Test void identifica942SoloDesdeLaCausaSqlReal() {
        assertThat(TestResetCoordinator.oracleCode(new CommandAcceptanceException("drop", new SQLException("missing", "42000", 942)))).isEqualTo(942);
        assertThat(TestResetCoordinator.oracleCode(new CommandAcceptanceException("ORA-00942 solo en texto"))).isZero();
        assertThat(TestResetCoordinator.oracleCode(new CommandAcceptanceException("otro", new SQLException("fatal", "42000", 955)))).isEqualTo(955);
    }

    @Test void soloTolera942EnDropAllowlistedActualDespuesDelPreflightIncluidoUsuario() {
        assertThat(TestResetSchemaFilterProvider.ALLOWLIST).hasSize(19);
        CommandAcceptanceException missing = new CommandAcceptanceException("drop", new SQLException("missing", "42000", 942));
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.DROP, "DOCUMENTO", "DOCUMENTO", missing)).isTrue();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(false, TestResetStage.DROP, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.PREFLIGHT, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.CREATE, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.VALIDATE_EMPTY, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.SEED, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.POST_VALIDATION, "DOCUMENTO", "DOCUMENTO", missing)).isFalse();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.DROP, "USUARIO", "USUARIO", missing)).isTrue();
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.DROP, "DOCUMENTO", "NOTIFICACION", missing)).isFalse();
        CommandAcceptanceException otherOracle = new CommandAcceptanceException("drop", new SQLException("fatal", "42000", 955));
        assertThat(TestResetCoordinator.isRecoverableMissingTable(true, TestResetStage.DROP, "DOCUMENTO", "DOCUMENTO", otherOracle)).isFalse();
    }

    @Test void unaEtapaFallidaDetieneLaSecuenciaYUnaNuevaEjecucionParteDesdeElInicio() {
        AtomicInteger completed = new AtomicInteger();
        assertThatThrownBy(() -> {
            TestResetCoordinator.stage("PREFLIGHT", completed::incrementAndGet);
            TestResetCoordinator.stage("DROP:DOCUMENTO", () -> { throw new IllegalStateException("fallo controlado"); });
            completed.incrementAndGet();
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("DROP:DOCUMENTO");
        assertThat(completed).hasValue(1);

        completed.set(0);
        TestResetCoordinator.stage("PREFLIGHT", completed::incrementAndGet);
        TestResetCoordinator.stage("DROP:DOCUMENTO", completed::incrementAndGet);
        TestResetCoordinator.stage("CREATE:DOCUMENTO", completed::incrementAndGet);
        TestResetCoordinator.stage("SEED", completed::incrementAndGet);
        TestResetCoordinator.stage("POST_VALIDATION", completed::incrementAndGet);
        assertThat(completed).hasValue(5);
    }
}
