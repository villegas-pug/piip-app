from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


OUTPUT = Path("output/pdf/ayuda-memoria-visual-piip-midagri-final.pdf")
PAGE_W, PAGE_H = A4

NAVY = HexColor("#103B5C")
BLUE = HexColor("#1E73B7")
TEAL = HexColor("#1CA6A3")
GREEN = HexColor("#4D9B64")
GOLD = HexColor("#E5A642")
INK = HexColor("#173042")
MUTED = HexColor("#5E7280")
PALE = HexColor("#EEF5F8")
PALE_BLUE = HexColor("#E8F2FA")
PALE_GREEN = HexColor("#EDF7F0")
LINE = HexColor("#C9DCE6")
LIGHT = HexColor("#F8FBFC")


def rounded(c, x, y, w, h, fill, radius=10, stroke=None):
    c.setFillColor(fill)
    c.setStrokeColor(stroke or fill)
    c.roundRect(x, y, w, h, radius, fill=1, stroke=1 if stroke else 0)


def text(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFont(font, size)
    c.setFillColor(color)
    c.drawString(x, y, value)


def centered(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFont(font, size)
    c.setFillColor(color)
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


def paragraph(c, value, x, y, max_width, size=8.8, leading=11, color=MUTED, font="Helvetica"):
    for line in wrap(c, value, font, size, max_width):
        text(c, line, x, y, size, color, font)
        y -= leading
    return y


def icon_target(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.8)
    for r in (10, 6, 2):
        c.circle(x, y, r, stroke=1, fill=0)


def icon_folder(c, x, y, color):
    c.setStrokeColor(color)
    c.setFillColor(white)
    c.setLineWidth(1.6)
    c.roundRect(x - 11, y - 7, 22, 15, 2, fill=0, stroke=1)
    c.line(x - 10, y + 5, x - 2, y + 5)
    c.line(x - 2, y + 5, x, y + 8)
    c.line(x, y + 8, x + 5, y + 8)


def icon_user(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.7)
    c.circle(x, y + 5, 4, stroke=1, fill=0)
    c.arc(x - 9, y - 11, x + 9, y + 4, 0, 180)


def icon_chart(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.7)
    c.line(x - 10, y - 8, x - 10, y + 10)
    c.line(x - 10, y - 8, x + 11, y - 8)
    c.setFillColor(color)
    c.rect(x - 5, y - 6, 4, 8, fill=1, stroke=0)
    c.rect(x + 2, y - 6, 4, 13, fill=1, stroke=0)
    c.rect(x + 9, y - 6, 4, 18, fill=1, stroke=0)


def card(c, x, y, w, h, title, body, accent, icon):
    rounded(c, x, y, w, h, LIGHT, 11, LINE)
    c.setFillColor(accent)
    c.circle(x + 20, y + h - 20, 13, fill=1, stroke=0)
    icon(c, x + 20, y + h - 20, white)
    text(c, title, x + 40, y + h - 17, 10.5, INK, "Helvetica-Bold")
    paragraph(c, body, x + 15, y + h - 52, w - 30, 8.7, 10.7, MUTED)


def comparison_card(c, x, y, w, h):
    rounded(c, x, y, w, h, LIGHT, 11, LINE)
    c.setFillColor(GOLD)
    c.circle(x + 20, y + h - 20, 13, fill=1, stroke=0)
    icon_chart(c, x + 20, y + h - 20, white)
    text(c, "ANTES Y CON PIIP", x + 40, y + h - 17, 9.7, INK, "Helvetica-Bold")
    text(c, "HOY", x + 15, y + h - 41, 7.2, GOLD, "Helvetica-Bold")
    paragraph(c, "Excel transitorio: cada caso es una fila y los documentos se enlazan externamente.", x + 15, y + h - 52, w - 30, 7.3, 8.5, MUTED)
    text(c, "CON PIIP", x + 15, y + 22, 7.2, TEAL, "Helvetica-Bold")
    paragraph(c, "Registro organizado para consultar y dar seguimiento.", x + 15, y + 12, w - 30, 7.1, 8.2, INK)


def flow_step(c, index, title, caption, y, color):
    x = 53
    c.setFillColor(color)
    c.circle(x, y + 12, 15, fill=1, stroke=0)
    centered(c, str(index), x, y + 8, 10, white, "Helvetica-Bold")
    rounded(c, 78, y - 7, 440, 34, white, 9, LINE)
    text(c, title, 91, y + 10, 9.3, INK, "Helvetica-Bold")
    text(c, caption, 91, y - 1, 7.7, MUTED)
    if index < 5:
        c.setStrokeColor(LINE)
        c.setLineWidth(2)
        c.line(x, y - 7, x, y - 18)
        c.setFillColor(LINE)
        c.setStrokeColor(LINE)
        c.line(x - 3, y - 14, x, y - 18)
        c.line(x + 3, y - 14, x, y - 18)


def status_pill(c, x, y, w, title, body, accent):
    rounded(c, x, y, w, 24, white, 7, LINE)
    text(c, title, x + 8, y + 14, 6.1, accent, "Helvetica-Bold")
    paragraph(c, body, x + 8, y + 6, w - 16, 6.1, 6.7, INK)


def draw_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("Ayuda memoria visual PIIP - MIDAGRI")
    c.setAuthor("MIDAGRI - PIIP")

    c.setFillColor(HexColor("#F4F9FB"))
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 110, PAGE_W, 110, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.circle(PAGE_W - 38, PAGE_H - 27, 36, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 3, PAGE_H - 69, 28, fill=1, stroke=0)

    rounded(c, 36, PAGE_H - 49, 67, 22, TEAL, 11)
    centered(c, "GUÍA RÁPIDA", 69.5, PAGE_H - 42, 7.5, white, "Helvetica-Bold")
    text(c, "PIIP", 36, PAGE_H - 78, 29, white, "Helvetica-Bold")
    text(c, "Portafolio Institucional de MIDAGRI", 106, PAGE_H - 68, 15, white, "Helvetica-Bold")
    text(c, "Registra y organiza iniciativas y proyectos; las decisiones las toman las personas.", 36, PAGE_H - 94, 9.0, HexColor("#D7EAF6"))

    rounded(c, 36, 678, 523, 50, white, 8, HexColor("#D7EAF6"))
    text(c, "CÓMO INICIAR", 49, 716, 6.9, TEAL, "Helvetica-Bold")
    paragraph(c, "Buenos días/tardes. En esta oportunidad presentaremos el avance del proyecto PIIP, el Portafolio Institucional de MIDAGRI. Actualmente, la información se viene consolidando de manera transitoria en un Excel. Con PIIP buscamos organizarla en una plataforma que facilite el registro, la consulta, el seguimiento y la trazabilidad de iniciativas y proyectos. Primero revisaremos su propósito y luego veremos su recorrido principal en una demostración.", 49, 705, 490, 6.35, 6.8, INK)

    card(c, 36, 590, 169, 97, "¿Qué es?", "Una plataforma para reunir las iniciativas y proyectos del portafolio institucional.", BLUE, icon_folder)
    card(c, 220, 590, 169, 97, "¿Para qué sirve?", "Para consultar la información y darle seguimiento con mayor orden.", TEAL, icon_target)
    comparison_card(c, 404, 590, 155, 97)

    text(c, "¿QUÉ PUEDE HACER LA PERSONA USUARIA?", 36, 565, 10.5, NAVY, "Helvetica-Bold")
    actions = [
        ("Registrar", "iniciativas y proyectos"),
        ("Cargar", "información y documentos"),
        ("Consultar", "el portafolio y sus registros"),
        ("Dar seguimiento", "a decisiones, tareas y avance"),
    ]
    x = 36
    colors = [BLUE, TEAL, GREEN, GOLD]
    for i, (verb, detail) in enumerate(actions):
        rounded(c, x, 520, 125, 32, PALE_BLUE if i % 2 == 0 else PALE_GREEN, 8)
        c.setFillColor(colors[i])
        c.circle(x + 14, 536, 5, fill=1, stroke=0)
        text(c, verb, x + 26, 538, 8.3, INK, "Helvetica-Bold")
        text(c, detail, x + 26, 527, 7.2, MUTED)
        x += 133

    text(c, "FLUJO GENERAL DE USO", 36, 492, 10.5, NAVY, "Helvetica-Bold")
    text(c, "Cada paso tiene un propósito claro. La aprobación y el proyecto vinculado son acciones separadas.", 36, 478, 8.2, MUTED)
    steps = [
        ("Registrar iniciativa", "Completar la información y adjuntar la ficha que la sustenta.", BLUE),
        ("Queda presentada", "La iniciativa entra al portafolio y recibe su propio código.", TEAL),
        ("Registrar decisión", "La decisión se registra de forma separada.", GOLD),
        ("Iniciativa aprobada", "Si corresponde, puede dar origen a un proyecto.", GREEN),
        ("Crear proyecto vinculado", "Se registra por separado y tiene sus propios documentos.", NAVY),
    ]
    y = 417
    for idx, (title, caption, color) in enumerate(steps, start=1):
        flow_step(c, idx, title, caption, y, color)
        y -= 42

    text(c, "¿QUÉ OCURRE CON LOS ESTADOS?", 36, 204, 9.2, NAVY, "Helvetica-Bold")
    status_pill(c, 36, 174, 253, "INICIATIVA CERRADA", "No Admisible o Archivada: la iniciativa no continúa.", TEAL)
    status_pill(c, 306, 174, 253, "INICIATIVA VINCULADA", "Con proyecto vinculado, su estado ya no cambia.", BLUE)
    status_pill(c, 36, 146, 253, "PROYECTO PUEDE CONTINUAR", "Suspendido o Producto no aprobado: puede retomarse o cancelarse.", GOLD)
    status_pill(c, 306, 146, 253, "PROYECTO CERRADO", "Cancelado o Finalizado: el proyecto no se reabre.", GREEN)

    rounded(c, 36, 86, 255, 50, PALE, 12, LINE)
    text(c, "EJEMPLO SIMPLE", 51, 120, 9.0, NAVY, "Helvetica-Bold")
    paragraph(c, "María registra una iniciativa y adjunta su ficha. Queda Presentada. Si se aprueba, puede crearse un proyecto vinculado. Ambos se consultan por separado.", 51, 108, 224, 7.1, 8.3, INK)

    rounded(c, 306, 86, 253, 50, NAVY, 12)
    text(c, "BENEFICIOS PARA MIDAGRI", 321, 120, 9.0, white, "Helvetica-Bold")
    benefits = ["Información en un solo lugar", "Consulta y seguimiento más simples", "Mejor trazabilidad de las decisiones"]
    yy = 111
    for benefit in benefits:
        c.setFillColor(TEAL)
        c.circle(325, yy + 2, 3, fill=1, stroke=0)
        text(c, benefit, 335, yy - 1, 7.0, white)
        yy -= 8

    rounded(c, 36, 21, 523, 57, white, 12, LINE)
    c.setFillColor(BLUE)
    c.circle(57, 57, 11, fill=1, stroke=0)
    icon_user(c, 57, 54, white)
    text(c, "CÓMO EXPLICARLO EN 1 MINUTO", 78, 60, 9.5, NAVY, "Helvetica-Bold")
    speech = ("PIIP reúne en un solo lugar la información de iniciativas y proyectos de MIDAGRI. Primero registramos una iniciativa "
              "y sus documentos. La iniciativa queda Presentada y la decisión se registra por separado. Si se aprueba y corresponde "
              "continuar, se crea un proyecto vinculado, con información y documentos propios. Así, el portafolio queda más ordenado "
              "y es más fácil de consultar y seguir.")
    paragraph(c, speech, 78, 45, 463, 7.1, 8.3, INK)

    text(c, "PIIP - Portafolio Institucional de MIDAGRI", 36, 10, 7.2, MUTED)
    text(c, "Ayuda memoria para exposición", 430, 10, 7.2, MUTED)
    c.showPage()
    c.save()


if __name__ == "__main__":
    draw_pdf()
