CREATE TABLE genres (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE movies (
    id BIGINT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    overview TEXT,
    poster_path VARCHAR(500),
    backdrop_path VARCHAR(500),
    release_date VARCHAR(20),
    vote_average DECIMAL(4,2) DEFAULT 0,
    vote_count INTEGER DEFAULT 0,
    runtime INTEGER,
    original_language VARCHAR(10),
    popularity DECIMAL(10,3) DEFAULT 0,
    adult BOOLEAN DEFAULT false,
    video BOOLEAN DEFAULT false,
    status VARCHAR(30),
    active BOOLEAN DEFAULT true
);

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies(id),
    genre_id BIGINT NOT NULL REFERENCES genres(id),
    PRIMARY KEY (movie_id, genre_id)
);

-- Seed genres (TMDB genre IDs)
INSERT INTO genres (id, name) VALUES
(28, 'Acción'),
(12, 'Aventura'),
(16, 'Animación'),
(35, 'Comedia'),
(80, 'Crimen'),
(99, 'Documental'),
(18, 'Drama'),
(10751, 'Familia'),
(14, 'Fantasía'),
(36, 'Historia'),
(27, 'Terror'),
(10402, 'Música'),
(9648, 'Misterio'),
(10749, 'Romance'),
(878, 'Ciencia ficción'),
(10770, 'Película de TV'),
(53, 'Suspenso'),
(10752, 'Bélica'),
(37, 'Western');

-- Seed movies (using TMDB IDs)
INSERT INTO movies (id, title, overview, poster_path, backdrop_path, release_date, vote_average, vote_count, runtime, original_language, popularity, adult, video, status) VALUES
(1241982, 'Moana 2', 'Moana emprende un nuevo y épico viaje más allá de los mares conocidos de Motunui.', '/yh64qvLSaoycSlOe3qltn5ochT3.jpg', '/jfYov1vfiUCT18AyqHYFaadDpFt.jpg', '2024-11-27', 7.1, 3500, 100, 'en', 3200.0, false, false, 'now_playing'),
(558449, 'Gladiador II', 'Muchos años después de presenciar la muerte del admirado guerrero Máximo, Lucio se ve obligado a entrar en el Coliseo después de que su hogar sea conquistado por los emperadores que ahora gobiernan Roma con puño de hierro.', '/2cxhvwyE0RYmIm7nDSZEVJAdfZk.jpg', '/euYIwmwkmz95mnXvuf87e5876nn.jpg', '2024-11-13', 6.9, 4200, 148, 'en', 2800.0, false, false, 'now_playing'),
(912649, 'Venom: El último baile', 'Eddie y Venom están de huida. Perseguidos por sus dos mundos y sin salida, el dúo se ve obligado a tomar una devastadora decisión.', '/aosm8NMQ3UyoBVpSxyimorCQykC.jpg', '/3V4kLQg0kSqPLctI5ziYWabAZYF.jpg', '2024-10-25', 6.3, 3100, 109, 'en', 2100.0, false, false, 'now_playing'),
(823464, 'Godzilla y Kong: El nuevo imperio', 'Una odisea cinematográfica épica que unirá a Godzilla y Kong como nunca antes.', '/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg', '/lgkgwd39wROZsSHEG5A4tSBKJfR.jpg', '2024-03-29', 6.7, 3800, 115, 'en', 2400.0, false, false, 'now_playing'),
(519182, 'Mi villano favorito 4', 'Gru se enfrenta a un nuevo villano que supone la mayor amenaza que ha afrontado jamás.', '/wWba3TaojhK7NdycRhoQpsG0FaH.jpg', '/oBIQDKcqNxKckjugtmzpIIOgDMz.jpg', '2024-07-03', 7.2, 4600, 95, 'en', 3500.0, false, false, 'upcoming'),
(1022789, 'Intensamente 2', 'Conoce a las nuevas emociones de Riley cuando se convierte en adolescente.', '/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', '/xg27NrXi7VXCGUr7MG75UqLl6Vg.jpg', '2024-06-14', 7.9, 5200, 100, 'en', 4100.0, false, false, 'upcoming'),
(447273, 'Blancanieves', 'Una nueva visión live-action del cuento de hadas clásico de Disney.', '/rUBFBPJ0uBr5gCmO8I7aGEYyuJ5.jpg', '/9cqNxx0GxF0bAY0RrtNcYJr7cm2.jpg', '2025-03-21', 6.5, 1200, 110, 'en', 1800.0, false, false, 'upcoming'),
(786892, 'Furiosa: De la saga Mad Max', 'La historia del origen de Furiosa, la guerrera que se convirtió en un ícono.', '/iADOJ8Zymht2JPMoy3R7xceZprc.jpg', '/oBIQDKcqNxKckjugtmzpIIOgDMz.jpg', '2024-05-24', 7.8, 3200, 149, 'en', 2600.0, false, false, 'popular'),
(945961, 'Alien: Romulus', 'Un grupo de jóvenes coloniales del espacio se enfrenta a la forma de vida más aterradora del universo.', '/b33nnKl1GSFbao4l3fZDDqsMx0F.jpg', '/9SSEUrSqhljBMzRe4aBTh17rUaC.jpg', '2024-08-16', 7.3, 4800, 119, 'en', 3700.0, false, false, 'popular');

-- Movie-genre associations
INSERT INTO movie_genres (movie_id, genre_id) VALUES
(1241982, 16), (1241982, 12), (1241982, 10751),
(558449, 28), (558449, 12), (558449, 18),
(912649, 28), (912649, 878), (912649, 12),
(823464, 28), (823464, 878), (823464, 12),
(519182, 16), (519182, 35), (519182, 10751),
(1022789, 16), (1022789, 35), (1022789, 12),
(447273, 14), (447273, 35), (447273, 16),
(786892, 28), (786892, 12), (786892, 53),
(945961, 27), (945961, 878), (945961, 28);
