-- Datos de prueba: se ejecutan en cada arranque de la API (spring.sql.init.mode: always).
-- Cada INSERT pregunta primero si el dato ya existe, asi que repetirlos no duplica nada.
-- Todos los usuarios tienen la clave: clave1234 (guardada cifrada con BCrypt).
--
-- Organizadores : organizador1@eventos.com, organizador2@eventos.com
-- Participantes : participante1@mail.com, participante2@mail.com, participante3@mail.com

-- ============ USUARIOS (credenciales y rol) ============
INSERT INTO users (email, password, role)
SELECT 'organizador1@eventos.com', '$2a$10$gvBWrfafSebAiMqhMSpCPuFHFOz5MGHP5k6Cb3FenxTPRtFSFlG/O', 'ORGANIZER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'organizador1@eventos.com');

INSERT INTO users (email, password, role)
SELECT 'organizador2@eventos.com', '$2a$10$gvBWrfafSebAiMqhMSpCPuFHFOz5MGHP5k6Cb3FenxTPRtFSFlG/O', 'ORGANIZER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'organizador2@eventos.com');

INSERT INTO users (email, password, role)
SELECT 'participante1@mail.com', '$2a$10$gvBWrfafSebAiMqhMSpCPuFHFOz5MGHP5k6Cb3FenxTPRtFSFlG/O', 'PARTICIPANT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'participante1@mail.com');

INSERT INTO users (email, password, role)
SELECT 'participante2@mail.com', '$2a$10$gvBWrfafSebAiMqhMSpCPuFHFOz5MGHP5k6Cb3FenxTPRtFSFlG/O', 'PARTICIPANT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'participante2@mail.com');

INSERT INTO users (email, password, role)
SELECT 'participante3@mail.com', '$2a$10$gvBWrfafSebAiMqhMSpCPuFHFOz5MGHP5k6Cb3FenxTPRtFSFlG/O', 'PARTICIPANT'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'participante3@mail.com');

-- ============ ORGANIZADORES (perfil) ============
INSERT INTO organizers (user_id, name, document_id, email, phone, website)
SELECT u.id, 'Eventos Lima SAC', '20512345678', 'organizador1@eventos.com', '014567890', 'https://eventoslima.example.com'
FROM users u
WHERE u.email = 'organizador1@eventos.com'
  AND NOT EXISTS (SELECT 1 FROM organizers WHERE document_id = '20512345678');

INSERT INTO organizers (user_id, name, document_id, email, phone, website)
SELECT u.id, 'Producciones Andinas', '20598765432', 'organizador2@eventos.com', '084123456', 'https://andinas.example.com'
FROM users u
WHERE u.email = 'organizador2@eventos.com'
  AND NOT EXISTS (SELECT 1 FROM organizers WHERE document_id = '20598765432');

-- ============ PARTICIPANTES (perfil) ============
INSERT INTO participants (user_id, name, document_id, email, phone)
SELECT u.id, 'Carlos Ramirez Soto', '70123456', 'participante1@mail.com', '987654321'
FROM users u
WHERE u.email = 'participante1@mail.com'
  AND NOT EXISTS (SELECT 1 FROM participants WHERE document_id = '70123456');

INSERT INTO participants (user_id, name, document_id, email, phone)
SELECT u.id, 'Maria Torres Vega', '70234567', 'participante2@mail.com', '987111222'
FROM users u
WHERE u.email = 'participante2@mail.com'
  AND NOT EXISTS (SELECT 1 FROM participants WHERE document_id = '70234567');

INSERT INTO participants (user_id, name, document_id, email, phone)
SELECT u.id, 'Luis Fernandez Diaz', '70345678', 'participante3@mail.com', '987333444'
FROM users u
WHERE u.email = 'participante3@mail.com'
  AND NOT EXISTS (SELECT 1 FROM participants WHERE document_id = '70345678');

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
