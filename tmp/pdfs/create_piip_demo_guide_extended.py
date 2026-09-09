from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


OUTPUT = Path("output/pdf/guion-demostracion-en-vivo-piip-final.pdf")
PAGE_W, PAGE_H = A4
NAVY = HexColor("#103B5C")
BLUE = HexColor("#1E73B7")
TEAL = HexColor("#1CA6A3")
GREEN = HexColor("#4D9B64")
GOLD = HexColor("#E5A642")
INK = HexColor("#173042")
MUTED = HexColor("#587080")
LINE = HexColor("#C9DCE6")
PALE = HexColor("#EEF5F8")
PALE_BLUE = HexColor("#E8F2FA")
PALE_GREEN = HexColor("#EDF7F0")


def rounded(c, x, y, w, h, fill, radius=10, stroke=None):
    c.setFillColor(fill)
    c.setStrokeColor(stroke or fill)
    c.roundRect(x, y, w, h, radius, fill=1, stroke=1 if stroke else 0)


def text(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFillColor(color)
    c.setFont(font, size)
    c.drawString(x, y, value)


def centered(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFillColor(color)
    c.setFont(font, size)
    c.drawCentredString(x, y, value)


def wrap(c, value, font, size, max_width):
    words = value.split()
    lines, current = [], ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if stringWidth(candidate, font, size) <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def paragraph(c, value, x, y, max_width, size=8, leading=9.4, color=MUTED, font="Helvetica"):
    for line in wrap(c, value, font, size, max_width):
        text(c, line, x, y, size, color, font)
        y -= leading
    return y


def check(c, x, y, label, color):
    c.setFillColor(color)
    c.circle(x, y + 2, 4, fill=1, stroke=0)
    text(c, label, x + 11, y - 1, 7.5, INK)


def step(c, number, title, timing, show, say, x, y, color):
    rounded(c, x, y, 523, 49, white, 9, LINE)
    c.setFillColor(color)
    c.circle(x + 20, y + 32, 12, fill=1, stroke=0)
    centered(c, str(number), x + 20, y + 28, 8.8, white, "Helvetica-Bold")
    text(c, title, x + 42, y + 33, 9.1, INK, "Helvetica-Bold")
    rounded(c, x + 176, y + 26, 45, 14, PALE_BLUE, 8)
    centered(c, timing, x + 198.5, y + 30.5, 6.4, BLUE, "Helvetica-Bold")
    text(c, "MUESTRA", x + 42, y + 18, 6.0, color, "Helvetica-Bold")
    paragraph(c, show, x + 98, y + 18, 397, 6.8, 7.4, INK)
    text(c, "DILO ASÍ", x + 42, y + 6, 5.6, color, "Helvetica-Bold")
    paragraph(c, f'“{say}”', x + 78, y + 6, 417, 6.8, 7.3, MUTED)


def state_table(c):
    x, y, w, h = 36, 127, 523, 77
    rounded(c, x, y, w, h, white, 10, LINE)
    text(c, "INICIATIVA", x + 15, y + h - 14, 7.0, TEAL, "Helvetica-Bold")
    initiative_lines = (
        ("Presentado", "Espera una decisión."),
        ("Iniciativa aprobada", "Puede originar un proyecto."),
        ("No Admisible / Iniciativa archivada", "Se cierra."),
    )
    yy = y + h - 27
    for label, description in initiative_lines:
        text(c, label, x + 15, yy, 6.2, INK, "Helvetica-Bold")
        text(c, description, x + 205, yy, 6.2, MUTED)
        yy -= 13

    project_x = x + 270
    text(c, "PROYECTO", project_x + 15, y + h - 14, 7.0, GREEN, "Helvetica-Bold")
    project_lines = (
        ("Proyecto en ejecución", "En curso."),
        ("Producto aprobado", "Aceptado; sigue el cierre."),
        ("Producto no aprobado / Suspendido", "Puede continuar o cancelarse."),
        ("Cancelado / Finalizado", "Se cierra."),
    )
    yy = y + h - 27
    for label, description in project_lines:
        text(c, label, project_x + 15, yy, 6.2, INK, "Helvetica-Bold")
        text(c, description, project_x + 170, yy, 6.2, MUTED)
        yy -= 11


def draw_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("PIIP - Ruta de demostracion en vivo")
    c.setAuthor("MIDAGRI - PIIP")
    c.setFillColor(HexColor("#F4F9FB"))
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 108, PAGE_W, 108, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.circle(PAGE_W - 30, PAGE_H - 23, 34, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 5, PAGE_H - 76, 25, fill=1, stroke=0)

    rounded(c, 36, PAGE_H - 42, 111, 20, TEAL, 10)
    centered(c, "GUION PERSONAL", 91.5, PAGE_H - 36, 7, white, "Helvetica-Bold")
    text(c, "PIIP", 36, PAGE_H - 73, 25, white, "Helvetica-Bold")
    text(c, "Demostración en vivo - 8 a 10 minutos", 101, PAGE_H - 64, 14, white, "Helvetica-Bold")
    text(c, "Muestra el valor del registro, los expedientes y los estados sin cambiar decisiones.", 36, PAGE_H - 88, 8.2, HexColor("#D7EAF6"))

    rounded(c, 36, 684, 523, 42, PALE, 9, LINE)
    text(c, "PASO 0 - APERTURA Y CONTEXTO", 51, 710, 7.4, GOLD, "Helvetica-Bold")
    paragraph(c, "Buenos días/tardes. Presentaremos el avance de PIIP. Hoy la información se reúne de forma transitoria en un Excel; PIIP busca organizar iniciativas y proyectos en una plataforma para consultarlos y darles seguimiento.", 51, 697, 486, 7.9, 9.0, INK)

    text(c, "ANTES DE EMPEZAR", 36, 662, 9.7, NAVY, "Helvetica-Bold")
    for index, label in enumerate(("Aplicación abierta", "Sesión iniciada por el expositor", "Iniciativa y proyecto de demostración", "Archivos de prueba sin datos sensibles")):
        check(c, 53 + (index % 2) * 255, 638 - (index // 2) * 18, label, TEAL if index < 2 else BLUE)

    text(c, "RUTA DE DEMOSTRACIÓN", 36, 593, 9.7, NAVY, "Helvetica-Bold")
    text(c, "Muestra una idea por pantalla. Los estados se explican; no se modifican durante la demo.", 36, 581, 7.7, MUTED)

    step(c, 1, "Inicio: visión del portafolio", "65 s", "Inicio: UE activa, registros, filtros, estados y notificaciones.", "Esta es la página de Inicio. Aquí vemos, en un solo lugar, los registros y su situación actual.", 36, 528, BLUE)
    step(c, 2, "Registrar una iniciativa", "90 s", "Iniciativas > Nueva iniciativa: datos principales y ficha inicial.", "Desde aquí registramos una nueva iniciativa y adjuntamos la ficha que la sustenta.", 36, 472, TEAL)
    step(c, 3, "Documentos y expediente", "100 s", "Documentos: bandeja y expediente documental de la iniciativa.", "Aquí revisamos los archivos cargados o pendientes. Estos conteos son informativos y no indican, por sí solos, una obligación.", 36, 416, GOLD)
    step(c, 4, "Proyecto y expediente propio", "100 s", "Proyectos: proyecto de demostración y sus documentos.", "El proyecto tiene su propia información y documentos. Los de la iniciativa no se copian aquí.", 36, 360, GREEN)
    step(c, 5, "Auditoría", "80 s", "Auditoría: filtros e historial de eventos del expediente.", "Aquí revisamos qué ocurrió y quién realizó cada acción registrada. Esta vista es para Administrador PIIP.", 36, 304, GOLD)
    step(c, 6, "Cierre del recorrido", "35 s", "Relación entre la iniciativa, el proyecto y sus expedientes.", "PIIP organiza la información, separa los documentos y deja evidencia de las acciones realizadas.", 36, 248, NAVY)

    text(c, "ESTADOS: SOLO EXPLICAR", 36, 214, 9.5, NAVY, "Helvetica-Bold")
    state_table(c)

    rounded(c, 36, 51, 523, 35, PALE_BLUE, 9, LINE)
    text(c, "PLAN DE RESPALDO", 51, 70, 7.1, BLUE, "Helvetica-Bold")
    text(c, "Si falla la sesión o conexión, abre la guía conceptual y explica esta misma ruta sin ejecutar acciones.", 51, 58, 7.5, INK)
    text(c, "PIIP - Ruta de demostración en vivo", 36, 20, 7, MUTED)
    text(c, "Uso del expositor", 478, 20, 7, MUTED)
    c.showPage()
    c.save()


if __name__ == "__main__":
    draw_pdf()
