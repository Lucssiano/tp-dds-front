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
            // Nota: El controller espera "IRRESTRICTA" (con A)
            const modo = toggleSwitch.checked ? 'IRRESTRICTA' : 'CURADA';
            url.searchParams.set('modo', modo); // Usamos 'modo' pq asi lo definimos en el Controller (@RequestParam defaultValue)
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
        const modoActual = mapElement.dataset.modo; // Viene del th:data-modo
        // Validamos contra 'IRRESTRICTA' que es lo que manda el back
        toggleSwitch.checked = (modoActual === 'IRRESTRICTA');
        toggleSwitch.addEventListener('change', aplicarFiltros);
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


    // 5. DIBUJAR PINES (Igual que antes)
    const hechosJson = mapElement.dataset.hechos;
    let hechos = [];

    try {
        if (hechosJson) hechos = JSON.parse(hechosJson);
    } catch (e) {
        console.error("❌ Error JSON:", e);
    }


    if (hechos.length > 0) {
        hechos.forEach(fact => {
            const ubicacion = fact.ubicacionOutputDTO;
            if (ubicacion && ubicacion.latitud != null) {
                const marker = L.marker([parseFloat(ubicacion.latitud), parseFloat(ubicacion.longitud)]).addTo(map);
                marker.bindPopup(`<b>${fact.titulo}</b>`);

                marker.on('click', () => {

                    // Lógica para llenar modal
                    document.getElementById('modal-title').textContent = fact.titulo;
                    document.getElementById('modal-date').textContent = formatearFechaParaArgentina(fact.fechaHecho);
                    document.getElementById('modal-location').textContent = `${ubicacion.provincia || ''}, ${ubicacion.municipio || ''}`;
                    document.getElementById('modal-source').textContent = (fact.fuentes && fact.fuentes.length > 0) ? fact.fuentes[0].nombre : 'Desconocida';
                    document.getElementById('modal-description').textContent = fact.descripcion || '';

                    const verHechoBtn = document.getElementById('ver-hecho-btn');
                    if(verHechoBtn) verHechoBtn.href = `/hechos/${fact.id}/detalle`;

                    const reportBtn = document.getElementById('report-button');
                    if(reportBtn) reportBtn.dataset.hechoId = fact.id;

                    document.getElementById('fact-modal').style.display = "block";
                });
            }
        });
    }

    // ... Lógica de cierre de modales (igual que tenías) ...
     const factModal = document.getElementById('fact-modal');
    const reportModal = document.getElementById('report-modal');
    const reportButton = document.getElementById('report-button');

    // Validamos que los botones existan antes de asignar onclick para evitar errores
    if (reportButton) {
        reportButton.addEventListener('click', () => {
            const hechoId = reportButton.dataset.hechoId;
            if (hechoId) {
                window.location.href = `/hechos/${hechoId}/solicitud-eliminacion`;
            }
        });
    }

    const closeBtns = document.querySelectorAll('.modal__close, .modal__close-report');
    closeBtns.forEach(btn => {
        btn.onclick = () => {
            if(factModal) factModal.style.display = "none";
            if(reportModal) reportModal.style.display = "none";
        };
    });

    window.onclick = (event) => {
        if (factModal && event.target == factModal) factModal.style.display = "none";
        if (reportModal && event.target == reportModal) reportModal.style.display = "none";
    };
});