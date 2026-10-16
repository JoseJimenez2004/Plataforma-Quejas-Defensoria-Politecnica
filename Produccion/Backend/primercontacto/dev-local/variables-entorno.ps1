# Variables de entorno para correr primercontacto EN LOCAL contra la base de dev-local.
# Solo sirven para desarrollo: en el servidor la configuracion real vive en sus config-files
# y NO debe reemplazarse.
#
# Uso (PowerShell, desde Backend/primercontacto):
#   . .\dev-local\variables-entorno.ps1
#   .\mvnw.cmd spring-boot:run

$env:DB_URL      = "jdbc:postgresql://localhost:5433/defensoria_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "defensoria_local"

# Secreto SOLO local (el del servidor es otro). Los tokens de prueba se firman con este mismo
# valor -- ver dev-local/token-prueba.js.
$env:JWT_SECRET  = "gKxw6SOEVL3NQQzDdXk22-UPYsUnafSyQx0eBbgVrN4fqnrh"
