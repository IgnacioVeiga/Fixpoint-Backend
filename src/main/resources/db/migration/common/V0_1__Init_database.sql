-- =============================================================
-- Proyecto: FixPoint
-- Descripción: Sistema de gestión de reparaciones para taller
-- Fecha de creación: 2025-07-28
-- DB: PostgreSQL
-- =============================================================

CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;

-- =====================
-- Clientes del sistema
-- =====================
CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    dni VARCHAR(20),
    phone VARCHAR(30),
    email TEXT,
    address TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ========================================
-- Tickets de reparación asociados a clientes
-- ========================================
CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    device_type TEXT NOT NULL,         -- ej: "televisor", "celular", etc.
    brand TEXT,
    model TEXT,
    serial_number TEXT,
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    problem_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'received',  -- estados: received, diagnosing, waiting_parts, repairing, repaired, returned, cancelled
    needs_contract BOOLEAN DEFAULT FALSE,
    contract_signed BOOLEAN DEFAULT FALSE,
    created_by TEXT,
    last_updated TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- Registro de eventos o actualizaciones por cada ticket
-- =====================================================
CREATE TABLE IF NOT EXISTS ticket_logs (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    description TEXT NOT NULL,
    author TEXT
);

-- ======================================
-- Archivos adjuntos relacionados a tickets (fotos, PDFs, etc.)
-- ======================================
CREATE TABLE IF NOT EXISTS attachments (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    filepath TEXT NOT NULL,  -- ruta relativa o absoluta al archivo
    file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('photo', 'contract', 'invoice', 'other')),
    uploaded_at TIMESTAMP DEFAULT NOW()
);

-- ======================================
-- Inventario del taller (componentes y piezas)
-- ======================================
CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    component_type TEXT,         -- ej: capacitor, display, board, cable
    description TEXT,
    condition VARCHAR(20) NOT NULL CHECK (condition IN ('new', 'used', 'damaged')),
    source TEXT,                 -- ej: donado, comprado, reciclado
    quantity INTEGER NOT NULL DEFAULT 1,
    location TEXT,               -- ubicación física (caja A, estante 2, etc.)
    added_at TIMESTAMP DEFAULT NOW()
);

-- ===================================================
-- Relación entre tickets y componentes del inventario utilizados
-- ===================================================
CREATE TABLE IF NOT EXISTS ticket_parts (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    inventory_id INTEGER NOT NULL REFERENCES inventory(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    note TEXT
);

-- ======================================
-- Indices útiles para búsquedas frecuentes
-- ======================================
CREATE INDEX IF NOT EXISTS idx_tickets_client ON tickets(client_id);
CREATE INDEX IF NOT EXISTS idx_ticket_logs_ticket ON ticket_logs(ticket_id);
CREATE INDEX IF NOT EXISTS idx_attachments_ticket ON attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_parts_ticket ON ticket_parts(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_parts_inventory ON ticket_parts(inventory_id);

-- ======================================
-- Comentarios finales:
-- - No se incluyen usuarios ya que el sistema es usado solo por 2 personas
--   y se asumió autenticación externa o previa.
-- - Las fotos/archivos pueden ser manejadas en sistema de archivos,
--   y solo se guarda su ruta en la tabla `attachments`.
-- - Se evita información redundante: por ejemplo, `brand` y `model` van en `tickets`, no en un catálogo.
-- - El diseño admite múltiples acciones (logs), fotos y piezas por ticket.
-- ======================================
