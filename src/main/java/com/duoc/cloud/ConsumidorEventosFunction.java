package com.duoc.cloud;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConsumidorEventosFunction {

    public static final String ROL_POR_DEFECTO = "1";

    private static final String DB_USER = "ADMIN";
    private static final String DB_PASSWORD = "Duoc1234$$3210";
    private static final String DB_URL = "jdbc:oracle:thin:@bdmicroservicios_low";

    @FunctionName("ProcessEventGridEvent")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") String content,
            final ExecutionContext context) {
        
        Logger logger = context.getLogger();
        logger.info("Funcion con Event Grid trigger ejecutada.");

        // Deserializar el contenido
        Gson gson = new Gson();
        JsonObject eventGridEvent = gson.fromJson(content, JsonObject.class);

        // Logear los detalles del evento
        logger.info("Evento recibido: " + eventGridEvent.toString());

        // Procesar la data del evento
        String eventType = eventGridEvent.get("eventType").getAsString();
        String data = eventGridEvent.get("data").toString();

        logger.info("Tipo de evento: " + eventType);
        logger.info("Data del evento: " + data);

        JsonObject datos = leerData(eventGridEvent.get("data"));

        try (Connection conn = conectar()) {
            if ("Usuarios.UsuarioCreado".equals(eventType)) {
                String idUsuario = datos.get("id_usuario").getAsString();
                int filas = asignarRolPorDefecto(conn, idUsuario);
                logger.info("Usuario " + idUsuario + ": rol por defecto asignado (" + filas + ")");
            } else if ("Roles.RolEliminado".equals(eventType)) {
                String idRol = datos.get("id_rol").getAsString();
                boolean eliminarRol = datos.has("eliminar_rol") && "true".equals(datos.get("eliminar_rol").getAsString());
                int filas = quitarRol(conn, idRol, eliminarRol);
                logger.info("Rol " + idRol + " eliminado: se actualizaron " + filas + " usuario(s)");
            }
        } catch (SQLException e) {
            logger.severe("Error procesando evento: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static int asignarRolPorDefecto(Connection conn, String idUsuario) throws SQLException {
        String sql = "UPDATE USUARIOS SET ID_ROL = ? WHERE ID_USUARIO = ? AND ID_ROL IS NULL";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ROL_POR_DEFECTO);
            pstmt.setString(2, idUsuario);
            return pstmt.executeUpdate();
        }
    }

    public static int quitarRol(Connection conn, String idRol, boolean eliminarRol) throws SQLException {
        int filas;
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE USUARIOS SET ID_ROL = NULL WHERE ID_ROL = ?")) {
            pstmt.setString(1, idRol);
            filas = pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() != 1407) throw e;
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE USUARIOS SET ID_ROL = ? WHERE ID_ROL = ?")) {
                pstmt.setString(1, ROL_POR_DEFECTO);
                pstmt.setString(2, idRol);
                filas = pstmt.executeUpdate();
            }
        }
        if (eliminarRol) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM ROLES WHERE ID_ROL = ?")) {
                pstmt.setString(1, idRol);
                pstmt.executeUpdate();
            }
        }
        return filas;
    }

    private static JsonObject leerData(JsonElement data) {
        if (data == null || data.isJsonNull()) return new JsonObject();
        if (data.isJsonObject()) return data.getAsJsonObject();
        try {
            JsonElement el = JsonParser.parseString(data.getAsString());
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static Connection conectar() throws SQLException {
        String walletPath = getWalletPath();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("oracle.net.tns_admin", walletPath);
        props.setProperty("oracle.net.wallet_location", "(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=" + walletPath + ")))");
        return DriverManager.getConnection(DB_URL, props);
    }

    private static String getWalletPath() {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "wallet");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
                String[] files = {"cwallet.sso", "ewallet.p12", "tnsnames.ora", "sqlnet.ora", "ewallet.pem"};
                for (String fileName : files) {
                    InputStream in = ConsumidorEventosFunction.class.getClassLoader().getResourceAsStream("wallet/" + fileName);
                    if (in != null) {
                        File targetFile = new File(tempDir, fileName);
                        Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return tempDir.getAbsolutePath().replace("\\", "/");
        } catch (Exception e) {
            return "C:/wallet";
        }
    }
}
