-- Catálogo inicial de medicamentos (mezcla de POS y NO POS)
INSERT INTO solicitudes.medicamentos (nombre, es_pos, activo) VALUES
    ('Acetaminofén 500 mg tabletas',        TRUE,  TRUE),
    ('Ibuprofeno 400 mg tabletas',          TRUE,  TRUE),
    ('Amoxicilina 500 mg cápsulas',         TRUE,  TRUE),
    ('Losartán 50 mg tabletas',             TRUE,  TRUE),
    ('Metformina 850 mg tabletas',          TRUE,  TRUE),
    ('Adalimumab 40 mg jeringa prellenada', FALSE, TRUE),
    ('Pembrolizumab 100 mg/4 mL',           FALSE, TRUE),
    ('Rivaroxabán 20 mg tabletas',          FALSE, TRUE),
    ('Semaglutida 1 mg pluma inyectable',   FALSE, TRUE),
    ('Medicamento descontinuado (demo)',    TRUE,  FALSE);
