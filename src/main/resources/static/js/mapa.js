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
    return 'Fecha inválida';
  }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log("🚀 Iniciando mapa.js...");

    const mapElement = document.getElementById('mapid');

    if (!mapElement) {
        console.error("❌ No se encontró el elemento #mapid. El mapa no puede renderizarse.");
        return;
    }

    // 1. INICIALIZAR MAPA (Lo hacemos primero para asegurar que se vea)
    const worldBounds = L.latLngBounds(L.latLng(-90, -180), L.latLng(90, 180));
    const map = L.map('mapid', {
        minZoom: 2,
        maxBounds: worldBounds
    }).setView([-34.6, -58.38], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        noWrap: true
    }).addTo(map);

    console.log("✅ Mapa inicializado correctamente.");

    // 2. OBTENER ELEMENTOS DEL DOM (Filtros)
    const toggleSwitch = document.getElementById('modo-navegacion-switch');
    const dateRangeInput = document.getElementById('date-range');
    const categorySelect = document.getElementById('category');
    const sourceSelect = document.getElementById('source');

    // 3. DEFINIR LÓGICA DE FILTROS
    let fp; // Variable para el calendario

    function aplicarFiltros() {
        console.log("🔄 Aplicando filtros...");
        const url = new URL(window.location.origin + window.location.pathname);

        // Modo
        if (toggleSwitch) {
            const modo = toggleSwitch.checked ? 'IRRESTRICTO' : 'CURADO';
            url.searchParams.set('modo', modo);
        }

        // Fechas (usando los nombres correctos para el backend)
        if (fp && fp.selectedDates.length === 2) {
            const formatear = (fecha) => fecha.toISOString().split('T')[0];
            url.searchParams.set('fechaAcontecimientoDesde', formatear(fp.selectedDates[0]));
            url.searchParams.set('fechaAcontecimientoHasta', formatear(fp.selectedDates[1]));
        }

        // Otros filtros
        if (categorySelect && categorySelect.value) {
            url.searchParams.set('categoria', categorySelect.value);
        }
        if (sourceSelect && sourceSelect.value) {
            url.searchParams.set('fuente', sourceSelect.value);
        }

        window.location.href = url.toString();
    }

    // 4. CONFIGURAR LISTENERS DE FILTROS
    // Solo agregamos eventos si los elementos existen en el HTML
    if (toggleSwitch) {
        const modoActual = mapElement.dataset.modo;
        toggleSwitch.checked = (modoActual === 'IRRESTRICTO');
        toggleSwitch.addEventListener('change', aplicarFiltros);
    }

    if (dateRangeInput) {
        const fechaDesdeActual = mapElement.dataset.fechaDesde;
        const fechaHastaActual = mapElement.dataset.fechaHasta;

        fp = flatpickr(dateRangeInput, {
            mode: "range",
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            locale: "es",
            defaultDate: (fechaDesdeActual && fechaHastaActual) ? [fechaDesdeActual, fechaHastaActual] : [],
            onClose: function(selectedDates) {
                if (selectedDates.length === 2) aplicarFiltros();
            }
        });
    }

    if (categorySelect) categorySelect.addEventListener('change', aplicarFiltros);
    if (sourceSelect) sourceSelect.addEventListener('change', aplicarFiltros);


    // 5. PROCESAR Y DIBUJAR PINES
    const hechosJson = mapElement.dataset.hechos;
    let hechos = [];

    try {
        if (hechosJson) {
            hechos = JSON.parse(hechosJson);
            console.log(`📦 Se recibieron ${hechos.length} hechos.`);
        } else {
            console.warn("⚠️ El atributo data-hechos está vacío.");
        }
    } catch (e) {
        console.error("❌ Error al parsear el JSON de hechos:", e);
    }

    if (hechos.length > 0) {
        hechos.forEach(fact => {
            const ubicacion = fact.ubicacionOutputDTO;
            if (ubicacion && ubicacion.latitud != null && ubicacion.longitud != null) {
                const lat = parseFloat(ubicacion.latitud);
                const lon = parseFloat(ubicacion.longitud);

                const marker = L.marker([lat, lon]).addTo(map);
                marker.bindPopup(`<b>${fact.titulo}</b>`);

                marker.on('click', () => {
                    // Llenar modal
                    document.getElementById('modal-title').textContent = fact.titulo;
                    document.getElementById('modal-date').textContent = formatearFechaParaArgentina(fact.fechaHecho);
                    document.getElementById('modal-location').textContent = `${ubicacion.provincia || ''}, ${ubicacion.municipio || ''}`;


                    document.getElementById('modal-source').textContent = fact.fuente;
                    document.getElementById('modal-description').textContent = fact.descripcion || '';

                    // 👉 Setear link "Ver Hecho"
                    const verHechoBtn = document.getElementById('ver-hecho-btn');
                    verHechoBtn.href = `/hechos/${fact.id}/detalle`;

                    // 👉 Guardar el id para el botón "Solicitar eliminación"
                        if (reportButton) {
                            reportButton.dataset.hechoId = fact.id;
                        }

                    document.getElementById('fact-modal').style.display = "block";
                });

            }
        });
    } else {
        console.log("ℹ️ No hay hechos para mostrar.");
    }

    // 6. LÓGICA DE MODALES
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