package dev.adrielgro;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        String fileName = "ips.txt"; // Archivo que contiene las direcciones IP
        ArrayList<String> ips = new ArrayList<>(); // Se almacenan las direcciones IP en este arreglo

        int limitCams = 0; // Limite de camaras/ventanas a mostrar al usuario

        // Lectura de argumentos
        if(args.length == 1) // Limite de Camaras
            if(args[0] != null && !args[0].isEmpty())
                limitCams = Integer.parseInt(args[0]);

        // Lectura del archivo de las direcciones IP y las metemos al arreglo
        try {
            FileInputStream fstream = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                ips.add(strLine);
            }
            br.close();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Archivo ips.txt no encontrado!");
            System.exit(0);
        }

        Collections.shuffle(ips); // Desordenamos la lista de direcciones IP al azar

        /* Creamos un hilo de la clase VideoThread por cada IP */
        for(int i = 0; i < ips.size(); i++) {

            if(limitCams != 0) // Si es diferente de 0, entonces poner limites con un maximo
                while(VideoThread.openCamsNow >= limitCams)
                    System.out.print(""); // TODO: Es necesario realizar alguna accion, para que no se suspenda la ejecucion de esta clase (no se porque no funciona el while sin cuerpo)

            Runnable r = new VideoThread(ips.get(i)); // Instanciamos la clase con el constructor
            Thread t = new Thread(r); // Instanciamos un nuevo hilo con la clase anterior
            t.start(); // Iniciamos el hilo

            VideoThread.openCamsNow++; // Contador de las ventanas/camaras abiertas (static)
        }
    }
}
