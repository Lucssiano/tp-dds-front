// ==========================================================================
// Funciones de Ayuda
// ==========================================================================
function formatearFechaParaArgentina(fechaString) {
    if (!fechaString) return 'Sin fecha';
    try {
        const fecha = new Date(fechaString);
        const opciones = { day: '2-digit', month: '2-digit', year: 'numeric' };
        return fecha.toLocaleDateString('es-AR', opciones);
    } catch (error) {
        return 'Fecha inválida';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log("🚀 Iniciando mapa.js...");

    const mapElement = document.getElementById('mapid');
    if (!mapElement) return;

    // 1. INICIALIZAR MAPA
    const worldBounds = L.latLngBounds(L.latLng(-90, -180), L.latLng(90, 180));
    const map = L.map('mapid', {
        minZoom: 2,
        maxBounds: worldBounds
    }).setView([-34.6, -58.38], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        noWrap: true
    }).addTo(map);

    // 2. OBTENER ELEMENTOS
    const toggleSwitch = document.getElementById('modo-navegacion-switch');
    const dateRangeInput = document.getElementById('date-range');
    const categorySelect = document.getElementById('category');
    const sourceSelect = document.getElementById('source');

    // 3. DEFINIR LÓGICA DE FILTROS (RECARGA PAGINA)
    let fp;

    function aplicarFiltros() {
        console.log("🔄 Aplicando filtros...");
        const url = new URL(window.location.origin + window.location.pathname);

        // A. MODO (Curado / Irrestricto)
        if (toggleSwitch) {
            const modo = toggleSwitch.checked ? 'IRRESTRICTA' : 'CURADA';
            url.searchParams.set('modo', modo);
        } else {
            // Si no hay switch (Mapa Global), limpiamos el parametro 'modo' si existiera
            url.searchParams.delete('modo');
        }

        // B. FECHAS
        if (fp && fp.selectedDates.length === 2) {
            const formatear = (fecha) => fecha.toISOString().split('T')[0];
            url.searchParams.set('fechaAcontecimientoDesde', formatear(fp.selectedDates[0]));
            url.searchParams.set('fechaAcontecimientoHasta', formatear(fp.selectedDates[1]));
        }

        // C. CATEGORÍA (Corrección: enviar 'categorias' en plural)
        if (categorySelect && categorySelect.value) {
            url.searchParams.set('categorias', categorySelect.value);
        }

        // D. FUENTE (Corrección: enviar 'fuentes' en plural)
        if (sourceSelect && sourceSelect.value) {
            url.searchParams.set('fuentes', sourceSelect.value);
        }

        // Recargamos la página con los nuevos parámetros
        window.location.href = url.toString();
    }

    // 4. CONFIGURAR LISTENERS Y ESTADO INICIAL

    // Restaurar estado del Switch
    if (toggleSwitch) {
        toggleSwitch.addEventListener('change', () => {
            // Esperamos 400ms (un poco más que la transición CSS de 0.3s) para que la animación se vea fluida antes de recargar.
            setTimeout(() => {
                aplicarFiltros();
            }, 400);
        });
        //
    }

    // Restaurar estado de Fechas
    if (dateRangeInput) {
        const fDesde = mapElement.dataset.fechaDesde;
        const fHasta = mapElement.dataset.fechaHasta;

        fp = flatpickr(dateRangeInput, {
            mode: "range",
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            locale: "es",
            defaultDate: (fDesde && fHasta) ? [fDesde, fHasta] : [],
            onClose: function(selectedDates) {
                if (selectedDates.length === 2) aplicarFiltros();
            }
        });
    }

    // Listeners para selects
    if (categorySelect) categorySelect.addEventListener('change', aplicarFiltros);
    if (sourceSelect) sourceSelect.addEventListener('change', aplicarFiltros);


    // 5. DIBUJAR PINES
    const hechosJson = mapElement.dataset.hechos;
    let hechos = [];

    try {
        if (hechosJson) hechos = JSON.parse(hechosJson);
    } catch (e) {
        console.error("❌ Error JSON:", e);
    }


    if (hechos.length > 0) {
        hechos.forEach(fact => {
            const ubicacion = fact.ubicacionDTO;
            if (ubicacion && ubicacion.latitud != null) {
                const marker = L.marker([parseFloat(ubicacion.latitud), parseFloat(ubicacion.longitud)]).addTo(map);
                // marker.bindPopup(`<b>${fact.titulo}</b>`);

                marker.on('click', () => {

                    // Lógica para llenar modal
                    document.getElementById('modal-title').textContent = fact.titulo;
                    document.getElementById('modal-date').textContent = formatearFechaParaArgentina(fact.fechaHecho);
                    document.getElementById('modal-location').textContent = `${ubicacion.provincia || ''}, ${ubicacion.municipio || ''}`;
                    document.getElementById('modal-source').textContent =
                        (fact.fuentes && fact.fuentes.length > 0)
                            ? fact.fuentes.map(f => f.nombre).join(', ')
                            : 'Desconocidas';
                    document.getElementById('modal-description').textContent = fact.descripcion || '';
                    document.getElementById('modal-category').textContent = fact.categoria || '';

                    const verHechoBtn = document.getElementById('ver-hecho-btn');
                    if(verHechoBtn) verHechoBtn.href = `/hechos/${fact.id}/detalle`;

                    const reportBtn = document.getElementById('report-button');
                    if(reportBtn) reportBtn.dataset.hechoId = fact.id;

                    document.getElementById('fact-modal').style.display = "block";
                });
            }
        });
    }

    // 6. LÓGICA BOTÓN LIMPIAR FILTROS
    const btnLimpiar = document.getElementById('btn-limpiar-filtros');

    if (btnLimpiar) {
        btnLimpiar.addEventListener('click', () => {
            console.log("🧹 Limpiando filtros...");

            // 1. Resetear inputs visualmente
            if (categorySelect) categorySelect.value = "";
            if (sourceSelect) sourceSelect.value = "";
            if (fp) fp.clear(); // Limpia Flatpickr

            // 2. Construir URL limpia
            const url = new URL(window.location.origin + window.location.pathname);

            // 3. ¿Qué hacemos con el MODO?
            // Si estamos en una colección, generalmente queremos mantener el modo actual
            // (no resetearlo a Curado violentamente), pero quitar el resto de filtros.
            // Si prefieres resetear TODO (incluido volver a Curado), borra este bloque 'if'.
            if (toggleSwitch) {
                // Mantenemos el modo que el usuario ya tenía seleccionado
                const currentMode = url.searchParams.get('modo');
                if (currentMode) {
                    url.searchParams.set('modo', currentMode);
                }
            }

            // Nota: Al crear 'new URL' basado en location.pathname,
            // automáticamente se eliminan todos los searchParams anteriores
            // (fecha, categorias, fuentes), excepto los que volvamos a setear explícitamente.

            // 4. Recargar
            window.location.href = url.toString();
        });
    }

    // LÓGICA DE MODALES SIMPLIFICADA
    const factModal = document.getElementById('fact-modal');
    const reportButton = document.getElementById('report-button'); // El botón rojo del modal de detalle

    // 1. Configurar botón de reporte (REDIRECCIÓN DIRECTA)
    if (reportButton) {
        reportButton.addEventListener('click', () => {
            const hechoId = reportButton.dataset.hechoId;
            if (hechoId) {
                // ¡Nos vamos directo al formulario! Sin preguntas extra.
                window.location.href = `/hechos/${hechoId}/solicitud-eliminacion`;
            }
        });
    }

    // 2. Cerrar Modal con la X o clic afuera
    const closeBtns = document.querySelectorAll('.modal__close'); // Ya no buscamos .modal__close-report
    closeBtns.forEach(btn => {
        btn.onclick = () => {
            if(factModal) factModal.style.display = "none";
        };
    });

    window.onclick = (event) => {
        if (factModal && event.target === factModal) {
            factModal.style.display = "none";
        }
    };
});