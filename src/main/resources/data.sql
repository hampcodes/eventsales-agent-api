-- Datos de prueba: se ejecutan en cada arranque de la API (spring.sql.init.mode: always).
-- Cada INSERT pregunta primero si el dato ya existe, asi que repetirlos no duplica nada.
-- Las claves de las cuentas de demostracion no se escriben aqui: solo su hash BCrypt.

-- ============ USUARIOS (credenciales y rol) ============
INSERT INTO users (email, password, role)
SELECT 'marcela.quispe@eventoslima.example', '$2a$10$waF.MuAzqenAjhg9DWZLqudc7kGLzLV98DRe/pEsjGiqEtHW4chS2', 'ORGANIZER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'marcela.quispe@eventoslima.example');

INSERT INTO users (email, password, role)
SELECT 'jorge.salazar@andinas.example', '$2a$10$ZdCRkrdHHj2f/sMBNhV3X.rQAEj8uMDjuIkNwkngHDDzlKzjB9/dO', 'ORGANIZER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'jorge.salazar@andinas.example');

INSERT INTO users (email, password, role)
SELECT 'henry.mendoza@eventsales.example', '$2a$10$RujZDdQ98M/.Fk8DDyZ.UOuYQGeMhjUfvkdPGcNYIuLkD9zvlAPTy', 'PARTICIPANT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'henry.mendoza@eventsales.example');

INSERT INTO users (email, password, role)
SELECT 'lucia.paredes@eventsales.example', '$2a$10$inEDs5x4eoxPQuj7iUwOSesp.s35kv7/czSaEIaryl.N/tCx57VUC', 'PARTICIPANT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'lucia.paredes@eventsales.example');

-- ============ ORGANIZADORES (perfil) ============
INSERT INTO organizers (user_id, name, document_id, email, phone, website)
SELECT u.id, 'Eventos Lima SAC', '20512345678', 'marcela.quispe@eventoslima.example', '014567890', 'https://eventoslima.example.com'
FROM users u
WHERE u.email = 'marcela.quispe@eventoslima.example'
  AND NOT EXISTS (SELECT 1 FROM organizers WHERE document_id = '20512345678');

INSERT INTO organizers (user_id, name, document_id, email, phone, website)
SELECT u.id, 'Producciones Andinas', '20598765432', 'jorge.salazar@andinas.example', '084123456', 'https://andinas.example.com'
FROM users u
WHERE u.email = 'jorge.salazar@andinas.example'
  AND NOT EXISTS (SELECT 1 FROM organizers WHERE document_id = '20598765432');

-- ============ PARTICIPANTES (perfil) ============
INSERT INTO participants (user_id, name, document_id, email, phone)
SELECT u.id, 'Henry Antonio Mendoza Puerta', '70123456', 'henry.mendoza@eventsales.example', '987654321'
FROM users u
WHERE u.email = 'henry.mendoza@eventsales.example'
  AND NOT EXISTS (SELECT 1 FROM participants WHERE document_id = '70123456');

INSERT INTO participants (user_id, name, document_id, email, phone)
SELECT u.id, 'Lucia Paredes Rojas', '70234567', 'lucia.paredes@eventsales.example', '987111222'
FROM users u
WHERE u.email = 'lucia.paredes@eventsales.example'
  AND NOT EXISTS (SELECT 1 FROM participants WHERE document_id = '70234567');

-- ============ CATEGORIAS ============
-- name es unico: ON CONFLICT DO NOTHING evita duplicados. Van antes que los eventos.
INSERT INTO categories (name) VALUES ('Musica') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Gastronomia') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Teatro') ON CONFLICT (name) DO NOTHING;

-- ============ EVENTOS ============
-- No se siembran entradas ni pagos: available_capacity = capacity_max
-- category_id apunta a una categoria sembrada arriba (Teatro queda sin eventos)
INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Festival de Rock Lima', CURRENT_DATE + 30, 'Lima', 200, 200, 120.00,
       'https://picsum.photos/seed/festival-rock/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Musica')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Festival de Rock Lima');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Concierto Sinfonico Arequipa', CURRENT_DATE + 45, 'Arequipa', 80, 80, 90.00,
       'https://picsum.photos/seed/sinfonico/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Musica')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Concierto Sinfonico Arequipa');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Feria Gastronomica Cusco', CURRENT_DATE + 60, 'Cusco', 150, 150, 35.00,
       'https://picsum.photos/seed/feria-gastronomica/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Gastronomia')
FROM organizers o
WHERE o.document_id = '20598765432'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Feria Gastronomica Cusco');

-- Corrige las imagenes de ejemplo antiguas (img.example.com no existe) en bases ya creadas.
-- Es seguro repetirlo: solo toca las filas que aun tienen la direccion vieja.
UPDATE events SET image_url = 'https://picsum.photos/seed/festival-rock/800/450'
WHERE image_url = 'https://img.example.com/festival-rock.jpg';
UPDATE events SET image_url = 'https://picsum.photos/seed/sinfonico/800/450'
WHERE image_url = 'https://img.example.com/sinfonico.jpg';
UPDATE events SET image_url = 'https://picsum.photos/seed/feria-gastronomica/800/450'
WHERE image_url = 'https://img.example.com/feria-gastronomica.jpg';

-- ============ MAS EVENTOS ============
INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Obra La Casa de Bernarda Alba', CURRENT_DATE + 20, 'Lima', 120, 120, 55.00,
       'https://picsum.photos/seed/obra-bernarda/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Teatro')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Obra La Casa de Bernarda Alba');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Stand Up Peruano en Lima', CURRENT_DATE + 25, 'Lima', 90, 90, 45.00,
       'https://picsum.photos/seed/standup/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Teatro')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Stand Up Peruano en Lima');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'El Principito para Ninos', CURRENT_DATE + 40, 'Arequipa', 100, 100, 30.00,
       'https://picsum.photos/seed/principito/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Teatro')
FROM organizers o
WHERE o.document_id = '20598765432'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'El Principito para Ninos');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Noche de Cumbia en Piura', CURRENT_DATE + 35, 'Piura', 300, 300, 45.00,
       'https://picsum.photos/seed/cumbia-piura/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Musica')
FROM organizers o
WHERE o.document_id = '20598765432'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Noche de Cumbia en Piura');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Concierto de Salsa Chiclayo', CURRENT_DATE + 50, 'Chiclayo', 220, 220, 70.00,
       'https://picsum.photos/seed/salsa-chiclayo/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Musica')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Concierto de Salsa Chiclayo');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Festival de Folklore Puno', CURRENT_DATE + 70, 'Puno', 400, 400, 25.00,
       'https://picsum.photos/seed/folklore-puno/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Musica')
FROM organizers o
WHERE o.document_id = '20598765432'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Festival de Folklore Puno');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Expo Cevicheria Lima', CURRENT_DATE + 55, 'Lima', 400, 400, 40.00,
       'https://picsum.photos/seed/expo-cevicheria/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Gastronomia')
FROM organizers o
WHERE o.document_id = '20512345678'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Expo Cevicheria Lima');

INSERT INTO events (organizer_id, name, date, city, capacity_max, available_capacity, base_price, image_url, category_id)
SELECT o.id, 'Festival del Pisco Ica', CURRENT_DATE + 65, 'Ica', 250, 250, 50.00,
       'https://picsum.photos/seed/pisco-ica/800/450',
       (SELECT c.id FROM categories c WHERE c.name = 'Gastronomia')
FROM organizers o
WHERE o.document_id = '20598765432'
  AND NOT EXISTS (SELECT 1 FROM events WHERE name = 'Festival del Pisco Ica');
