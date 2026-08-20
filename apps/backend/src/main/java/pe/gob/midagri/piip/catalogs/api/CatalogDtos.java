package pe.gob.midagri.piip.catalogs.api;

import java.util.List;

public final class CatalogDtos {
    private CatalogDtos() {}
    public record PersistentCatalogItemResponse(Long id, String code, String name, int displayOrder, boolean active) {}
    public record TechnicalCatalogItemResponse(String code, String name, int displayOrder, boolean active) {}
    public record CatalogBundleResponse(
        List<TechnicalCatalogItemResponse> recordTypes,
        List<PersistentCatalogItemResponse> solutionTypes,
        List<PersistentCatalogItemResponse> sources,
        List<PersistentCatalogItemResponse> peiObjectives,
        List<PersistentCatalogItemResponse> poiActivities,
        List<PersistentCatalogItemResponse> documentTypes) {}
}
