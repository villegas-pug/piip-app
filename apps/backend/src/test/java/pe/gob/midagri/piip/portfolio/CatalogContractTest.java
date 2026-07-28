package pe.gob.midagri.piip.portfolio;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.portfolio.domain.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogContractTest {
    @Test
    void preservesTheSixExcelCatalogs() {
        assertThat(labels(RecordType.values())).containsExactly("Iniciativa", "Proyecto");
        assertThat(labels(SolutionType.values())).containsExactly("Solución potencial o adaptable", "Solución por definir", "No aplica");
        assertThat(labels(SourceOrigin.values())).containsExactly("Ficha de iniciativa de innovación pública", "Concurso interno", "Innovación abierta", "Propuesta de jefatura o directivos", "Otros", "Convocatoria");
        assertThat(labels(PortfolioStatus.values())).containsExactly("Presentado", "Iniciativa aprobada", "Iniciativa archivada", "Proyecto en ejecución", "Producto aprobado", "Producto no aprobado", "Suspendido", "Cancelado", "Finalizado", "No Aplicable", "No Admisible");
        assertThat(labels(FinalProductType.values())).containsExactly("Prototipo de solución conceptualizada", "Solución funcional", "NA");
        assertThat(labels(DigitalComponent.values())).containsExactly("Si", "No");
    }

    @Test
    void frontendKeepsTheSameCanonicalValues() throws Exception {
        String source = Files.readString(Path.of("..", "frontend", "src", "app", "core", "piip.catalogs.ts"));
        for (LabeledCatalog value : allValues()) assertThat(source).contains("'" + value.label() + "'");
    }

    private List<String> labels(LabeledCatalog[] values) { return Arrays.stream(values).map(LabeledCatalog::label).toList(); }
    private List<LabeledCatalog> allValues() {
        List<LabeledCatalog> values = new ArrayList<>();
        values.addAll(Arrays.asList(RecordType.values())); values.addAll(Arrays.asList(SolutionType.values()));
        values.addAll(Arrays.asList(SourceOrigin.values())); values.addAll(Arrays.asList(PortfolioStatus.values()));
        values.addAll(Arrays.asList(FinalProductType.values())); values.addAll(Arrays.asList(DigitalComponent.values()));
        return values;
    }
}
