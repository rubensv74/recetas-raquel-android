# Sistema visual Golden Editorial de Recetoria

**Fuente de verdad:** `docs/brand/recetoria-golden-editorial-v2/`
**Versión:** 2.0
**Fecha de adopción:** 2026-08-16

## Estrategia

La identidad se aplica desde el tema Material 3 y no mediante colores repetidos
en cada pantalla. Los componentes existentes conservan comportamiento, test tags
y contratos. Solo las superficies cuya semántica no cabe en `ColorScheme` —los
tres estados de seguridad alimentaria— usan la paleta complementaria expuesta
por `RecetoriaTheme.safety`.

## Roles cromáticos

| Rol | Token | Aplicación |
|---|---|---|
| Texto y estructura | Espresso Ink `#211B16` | títulos, navegación, texto e iconos estructurales |
| Marca y acción | Toasted Bronze `#9E621C` | acción principal, foco e identidad |
| Favorito y selección | Antique Gold `#C58A2A` | estrellas, filtros y selecciones |
| Fondo | Soft Ivory `#FAF7F2` | fondo general, icono y splash |
| Neutral | Warm Stone `#E8E0D6` | bordes, divisores y superficies secundarias |
| Presencia | Burnt Amber `#A95010` | presencia o derivado identificado |
| Puede contener | Informative Blue `#33709F` | declaración de fabricante y posible reactividad |
| Crítico | Critical Red `#B5473C` | error, revisión requerida o imposibilidad de evaluar |

Los tonos nocturnos son derivados cálidos accesibles. El fondo oscuro es marrón
espresso, no negro. Bronce y oro permanecen como acentos limitados.

## Tipografía

Los encabezados editoriales y nombres de recetas usan la serif del sistema.
Navegación, formularios, botones, chips, información técnica, normativa y de
seguridad usan `sans-serif`/Roboto. El paquete recomienda Manrope pero no incluye
los binarios; no se añade una fuente descargable para mantener el arranque
offline y evitar una dependencia frágil.

## Icono y splash

- foreground y geometría proceden literalmente del paquete v2;
- las capas adaptativas aplican un inset uniforme del 20 % para mantener el
  símbolo íntegro bajo máscaras circulares, redondeadas y squircle;
- el fondo adaptativo es Soft Ivory y no incluye una máscara dibujada;
- la capa monocroma conserva la geometría oficial;
- Android 12+ usa el símbolo oficial y fondo marfil en el splash del sistema;
- Android 8–11 usa el fondo de ventana correspondiente;
- los raster legacy proceden del PNG oficial de 512 px.

## Seguridad alimentaria

La UI distingue presencia, posible presencia y revisión crítica con color,
etiqueta, pictograma y explicación. Las relaciones, evidencias, fuentes y avisos
no se modifican. La ausencia de coincidencias sigue sin presentarse como garantía
de seguridad individual.

## Excepciones deliberadas

- Se mantienen namespace, application ID y nombres técnicos internos para evitar
  una migración arquitectónica sin valor funcional.
- El lema no aparece en el splash breve del sistema; la guía v2 permite omitirlo.
- Manrope queda pendiente de que el paquete incluya archivos de fuente y licencia.
- Las capturas comerciales de Google Play se posponen hasta cerrar la interfaz.
