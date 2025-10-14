// ==========================================================================
// Funciones de Ayuda (Helpers)
// ==========================================================================
function formatearFechaParaArgentina(fechaString) {
  if (!fechaString) return 'Sin fecha';
  try {
    const fecha = new Date(fechaString);
    const opciones = { day: '2-digit', month: '2-digit', year: 'numeric' };
    return fecha.toLocaleDateString('es-AR', opciones);
  } catch (error) {
    console.error("Error al parsear la fecha:", fechaString, error);
    return 'Fecha inválida';
  }
}

// ==========================================================================
// Lógica Principal de la Página
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    console.log("Cargando mapa.js...");

    // --- OBTENER ELEMENTOS DEL DOM ---
    const mapElement = document.getElementById('mapid');
    const toggleSwitch = document.getElementById('modo-navegacion-switch');
    const dateRangeInput = document.getElementById('date-range');
    const categorySelect = document.getElementById('category');
    const sourceSelect = document.getElementById('source');

    /**
     * Función central que lee todos los filtros y recarga la página.
     */
    function aplicarFiltros() {
            const url = new URL(window.location.origin + window.location.pathname);

            // 1. Añadir el modo (Curado/Irrestricto)
            const modo = toggleSwitch.checked ? 'IRRESTRICTO' : 'CURADO';
            url.searchParams.set('modo', modo);

            // 2. Añadir el rango de fechas con los nombres correctos
            const fechasSeleccionadas = fp.selectedDates;
            if (fechasSeleccionadas.length === 2) {
                const formatear = (fecha) => fecha.toISOString().split('T')[0]; // Formato YYYY-MM-DD

                // --- AQUÍ ESTÁ EL CAMBIO ---
                url.searchParams.set('fechaAcontecimientoDesde', formatear(fechasSeleccionadas[0]));
                url.searchParams.set('fechaAcontecimientoHasta', formatear(fechasSeleccionadas[1]));
            }

            console.log("Recargando con nueva URL:", url.toString());
            window.location.href = url.toString();
        }

    // --- INICIALIZACIÓN DE FILTROS Y EVENTOS ---

    // 1. Inicializar el switch de Modo
    const modoActual = mapElement.dataset.modo;
    if (modoActual) {
        toggleSwitch.checked = (modoActual === 'IRRESTRICTO');
    }
    toggleSwitch.addEventListener('change', aplicarFiltros);

    // 2. Inicializar el calendario (flatpickr)
    const fechaDesdeActual = mapElement.dataset.fechaDesde;
    const fechaHastaActual = mapElement.dataset.fechaHasta;
    const fp = flatpickr(dateRangeInput, {
        mode: "range",
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d/m/Y",
        locale: "es",
        defaultDate: (fechaDesdeActual && fechaHastaActual) ? [fechaDesdeActual, fechaHastaActual] : [],
        onClose: function(selectedDates) {
            if (selectedDates.length === 2) {
                aplicarFiltros();
            }
        }
    });


    // --- LÓGICA DEL MAPA Y MODALES ---
    const hechosJson = mapElement.dataset.hechos;
    const hechos = JSON.parse(hechosJson);
    const worldBounds = L.latLngBounds(L.latLng(-90, -180), L.latLng(90, 180));
    const map = L.map('mapid', { minZoom: 2, maxBounds: worldBounds }).setView([-34.6, -58.38], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        noWrap: true
    }).addTo(map);

    if (hechos && hechos.length > 0) {
        hechos.forEach(fact => {
            const ubicacion = fact.ubicacionOutputDTO;
            if (ubicacion && ubicacion.latitud != null && ubicacion.longitud != null) {
                const marker = L.marker([parseFloat(ubicacion.latitud), parseFloat(ubicacion.longitud)]).addTo(map);
                marker.bindPopup(`<b>${fact.titulo}</b>`);
                marker.on('click', () => {
                    document.getElementById('modal-title').textContent = fact.titulo;
                    document.getElementById('modal-date').textContent = formatearFechaParaArgentina(fact.fechaHecho);
                    document.getElementById('modal-location').textContent = `${ubicacion.provincia || ''}, ${ubicacion.municipio || ''}`;
                    const fuentesTexto = Array.isArray(fact.fuentes) ? fact.fuentes.join(', ') : fact.fuentes;
                    document.getElementById('modal-source').textContent = fuentesTexto || 'Desconocida';
                    document.getElementById('modal-description').textContent = fact.descripcion || '';
                    document.getElementById('fact-modal').style.display = "block";
                });
            }
        });
    } else {
        console.warn("No hay hechos para mostrar en el mapa");
    }

    // Lógica para cerrar modales
    const factModal = document.getElementById('fact-modal');
    const reportModal = document.getElementById('report-modal');
    const reportButton = document.getElementById('report-button');
    const closeFactModalBtn = factModal.querySelector('.modal__close');
    const closeReportModalBtn = reportModal.querySelector('.modal__close-report');

    reportButton.addEventListener('click', () => {
        factModal.style.display = 'none';
        reportModal.style.display = 'block';
    });
    closeFactModalBtn.onclick = () => { factModal.style.display = "none"; };
    closeReportModalBtn.onclick = () => { reportModal.style.display = "none"; };
    window.onclick = (event) => {
        if (event.target == factModal) { factModal.style.display = "none"; }
        if (event.target == reportModal) { reportModal.style.display = "none"; }
    };
});