// Se ejecuta cuando el HTML está completamente cargado
document.addEventListener('DOMContentLoaded', () => {
  console.log("Cargando mapa.js...");

  // 1. Tomar el div del mapa y leer el string JSON
  const mapElement = document.getElementById('mapid');
  const hechosJson = mapElement.dataset.hechos;

  // 2. Convertir (parsear) el string JSON a un array de objetos JavaScript
  const hechos = JSON.parse(hechosJson);

  console.log("Hechos recibidos desde el HTML:", hechos);

  // El resto de tu código para crear el mapa
  const map = L.map('mapid').setView([-34.6, -58.38], 5);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map);

  if (!hechos || hechos.length === 0) {
    console.warn("No hay hechos para mostrar en el mapa");
    return;
  }

  hechos.forEach(fact => {
    const ubicacion = fact.ubicacionOutputDTO;

    if (ubicacion && ubicacion.latitud != null && ubicacion.longitud != null) {
      const lat = parseFloat(ubicacion.latitud);
      const lon = parseFloat(ubicacion.longitud);
      const marker = L.marker([lat, lon]).addTo(map);

      marker.bindPopup(`<b>${fact.titulo}</b>`);

      marker.on('click', () => {
        document.getElementById('modal-title').textContent = fact.titulo;
        document.getElementById('modal-date').textContent = fact.fechaHecho || 'Sin fecha';
        document.getElementById('modal-location').textContent = `${ubicacion.provincia || ''}, ${ubicacion.municipio || ''}`;
        document.getElementById('modal-source').textContent = fact.fuente || 'Desconocida';
        document.getElementById('modal-description').textContent = fact.descripcion || '';
        document.getElementById('fact-modal').style.display = "block";
      });
    }
  });

  document.querySelector('.modal__close').onclick = () => {
    document.getElementById('fact-modal').style.display = 'none';
  };
});