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
