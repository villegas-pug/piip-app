import { expect, test } from '@playwright/test';

interface HttpEvidence {
  method: string;
  url: string;
  status: number;
  correlationId: string | null;
}

test('navega al proyecto después de aprobar sin crash y conserva evidencia HTTP', async ({ page }, testInfo) => {
  const initiativeCode = process.env.PIIP_E2E_INITIATIVE_CODE;
  test.skip(
    !initiativeCode || process.env.PIIP_E2E_ALLOW_MUTATIONS !== 'true',
    'Requiere PIIP_E2E_INITIATIVE_CODE y PIIP_E2E_ALLOW_MUTATIONS=true; la prueba aprueba un registro real.',
  );

  const httpResponses: HttpEvidence[] = [];
  const pendingResponseEvidence: Promise<void>[] = [];
  const consoleErrors: string[] = [];
  const requestFailures: string[] = [];

  page.on('response', (response) => {
    const responseUrl = response.url();
    if (!responseUrl.includes('/api/')) return;
    const evidence = (async () => {
      const headers = await response.allHeaders();
      httpResponses.push({
        method: response.request().method(),
        url: responseUrl,
        status: response.status(),
        correlationId: headers['x-correlation-id'] ?? null,
      });
    })();
    pendingResponseEvidence.push(evidence);
  });
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('requestfailed', (request) => {
    requestFailures.push(`${request.method()} ${request.url()}: ${request.failure()?.errorText ?? 'falló sin detalle'}`);
  });

  await page.goto(`/iniciativas/${encodeURIComponent(initiativeCode!)}?action=approve`, { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Confirmar aprobación' }).click();
  await expect(page.getByRole('heading', { name: 'Iniciativa aprobada' })).toBeVisible();
  await page.getByRole('button', { name: 'Crear proyecto ahora' }).click();

  await expect(page).toHaveURL(new RegExp(`/proyectos/nuevo/derivado/${encodeURIComponent(initiativeCode!)}$`));
  await expect(page.getByRole('heading', { name: new RegExp(`Crear proyecto desde ${initiativeCode}`) })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: new RegExp(`Crear proyecto desde ${initiativeCode}`) })).toBeVisible();

  await Promise.all(pendingResponseEvidence);
  await testInfo.attach('http-evidence.json', {
    body: JSON.stringify({ httpResponses, consoleErrors, requestFailures }, null, 2),
    contentType: 'application/json',
  });

  expect(requestFailures).toEqual([]);
  expect(consoleErrors).toEqual([]);
  expect(httpResponses).toEqual(expect.arrayContaining([
    expect.objectContaining({
      method: 'POST',
      status: 200,
      url: expect.stringContaining(`/initiatives/${initiativeCode}/approval`),
    }),
  ]));
  expect(httpResponses.filter((response) => response.correlationId).length).toBeGreaterThan(0);
});
