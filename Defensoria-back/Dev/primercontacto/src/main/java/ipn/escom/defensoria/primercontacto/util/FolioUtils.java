package ipn.escom.defensoria.primercontacto.util;

import java.time.LocalDate;

public class FolioUtils {

    private FolioUtils() {
        // Constructor privado para evitar instanciación
    }

    public static String generarFolioOficial(Long id) {
        int year = LocalDate.now().getYear();
        return String.format("DDP-EXP-%d-%03d", year, id);
    }
}