package com.fixpoint.components;

public class ApplicationErrorHandler {
    public static void handle(Throwable ex) {
        if (hasDatabaseConnectionError(ex)) {
            System.err.println("\n--------------------------------------------------");
            System.err.println("ERROR: No se pudo conectar a la base de datos PostgreSQL.");
            System.err.println("Verifica que el motor o contenedor de la base de datos esté encendido y accesible en localhost:5432.");
            System.err.println("--------------------------------------------------\n");
        }
        // Acá se deben agregar más tipos de errores personalizados en el futuro
    }

    private static boolean hasDatabaseConnectionError(Throwable ex) {
        while (ex != null) {
            if (ex.getMessage() != null && ex.getMessage().contains("Connection to")) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }
}