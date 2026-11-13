Mini aplicacion para listar las impresoras desde el puerto 8095


Paso 1:

ejecutar el jar

Paso 2:

copiar el jar a la carpeta deploy

paso 3:

Ejecutar este comando, estando en la carpeta pserver: esta ne modo consola, es para el .exe

jpackage ^
--type exe ^
--name "Server Ticket" ^
--app-version 1.0.4 ^
--input "deploy" ^
--main-jar "pserver-0.0.1-SNAPSHOT.jar" ^
--win-menu ^
--win-shortcut ^
--win-dir-chooser ^
--win-console ^
--dest "dist" ^
--java-options "-Dapp.path=\"$ROOTDIR\Server Ticket.exe\""

Paso 4:

Ejecutar este comando, estando en la carpeta pserver es para el .exe

jpackage ^
--type exe ^
--name "Server Ticket" ^
--app-version 1.0.4 ^
--input "deploy" ^
--main-jar "pserver-0.0.1-SNAPSHOT.jar" ^
--win-menu ^
--win-shortcut ^
--win-dir-chooser ^
--dest "dist" ^
--icon "src/main/resources/icon.ico" ^
--java-options "-Dapp.path=\"$ROOTDIR\Server Ticket.exe\""


Terminal de tarjeta (PAX A30, ECRT)

El agente tambien conversa con la terminal de tarjeta para el POS. Endpoints: GET /ecrt/health, POST /ecrt/sale, POST /ecrt/void y POST /ecrt/close.

Configuracion: icono de la bandeja, opcion "Terminal de tarjeta...". Se guarda en la carpeta del usuario, archivo .pserver/ecrt.properties (ejemplo en ecrt.properties.example).

- IP y puerto: los muestra la terminal en "Datos de conexion", con la terminal en modo esclavo.
- Clave del agente: se genera sola la primera vez. Hay que copiarla en el POS de esa PC (boton "Tarjeta").
- El dominio del POS debe estar en "Editar dominio CORS...".

Solo atiende peticiones /ecrt hechas desde la misma PC.
