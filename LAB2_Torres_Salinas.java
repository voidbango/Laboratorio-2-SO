import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger; 
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/*
    ESTRUCTURAS COMPARTIDAS Y DATOS GLOBALES
    -> almacena las variables de control, el grafo en memoria, las distancias atómicas
       y los mecanismos de sincronización (barrera y contador de cambios) que usan los hilos
*/
public class LAB2_Torres_Salinas {
    static int numHilos;
    static int totalNodos;
    static int nodoInicial;
    static int nodoFinal;
    static int[] predecesores;

    static ArrayList<conexion> listaServidor = new ArrayList<>();
    static AtomicIntegerArray distancias;
    static CyclicBarrier barrera; 
    static AtomicInteger optimizacionesEnIteracion = new AtomicInteger(0); 
    static int iteracionActual = 0;    
    static final int INF = Integer.MAX_VALUE / 3;

    /*
        NOMBRE FUNCIÓN: leer_txt
        -> lee el archivo .txt de la red de servidores, extrae las variables principales,
           inicializa el tablero de distancias/predecesores y carga la lista de conexiones
    */
    public static void leer_txt(String ruta){
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            br.readLine(); 
            numHilos = Integer.parseInt(br.readLine().trim());
            totalNodos = Integer.parseInt(br.readLine().trim());             
            nodoInicial = Integer.parseInt(br.readLine().trim());
            nodoFinal = Integer.parseInt(br.readLine().trim());

            distancias = new AtomicIntegerArray(totalNodos);  
            predecesores = new int[totalNodos];
            
            for(int i = 0; i < totalNodos; i++){
                distancias.set(i, INF); 
                predecesores[i] = -1; 
            }
            distancias.set(nodoInicial, 0);
        
            String line; 
            while ((line = br.readLine()) != null){
                line = line.trim();
                if (line.isEmpty()) continue;   

                String[] partes = line.split(" ");
                
                int u = Integer.parseInt(partes[0]); 
                int v = Integer.parseInt(partes[1]); 
                int w = Integer.parseInt(partes[2]); 

                listaServidor.add(new conexion(u, v, w)); 
            }

            System.out.println("Carga completa, conexiones cargadas: " + listaServidor.size());

        } catch (IOException e) {
            System.err.println("error al leer el archivo: " + e.getMessage());
        }
    }

    /*
        NOMBRE FUNCIÓN: main
        -> activa la lectura, inicializa la barrera cíclica, calcula las tajadas de trabajo,
        arranca y espera a los hilos, y escribe el reporte final.
    */
    public static void main(String[] args) {
        long tiempoInicio = System.currentTimeMillis();
        
        if (args.length > 0) {
            leer_txt(args[0]);
            
            System.out.println("=== A. Inicio del Rastreo ===");
            System.out.println("Despliegue de Hilos: " + numHilos + " hilos creados para procesar el grafo.");
            System.out.println("Estado de Memoria: Estructuras compartidas inicializadas correctamente.");
            System.out.println("Carga del Mapa: " + totalNodos + " servidores y " + listaServidor.size() + " conexiones cargadas.");
            System.out.println("========================================\n");

            /*
                SINCRONIZACION EN BARRERA / PROGRESO X ITERACIONES
                -> codigo ejecutado al cerrar cada iteración. Muestra la sincronización,
                   el número N de optimizaciones y detecta si caímos en un ciclo negativo.
            */
            System.out.println("=== B. Progreso por Iteraciones ===");
            barrera = new CyclicBarrier(numHilos, new Runnable() {
                @Override
                public void run() {
                    System.out.println("Sincronización en Barrera: Iteración " + iteracionActual + ": Todos los hilos han llegado a la barrera de sincronización.");
                    
                    int cambiosRonda = optimizacionesEnIteracion.get();
                    System.out.println("Actualizaciones de Red: Hilos de análisis: " + cambiosRonda + " latencias optimizadas en esta ronda.\n");
                    
                    // DETECCIÓN CICLO NEGATIVO 
                    if (iteracionActual >= totalNodos - 1 && cambiosRonda > 0) {
                        System.out.println("se ha detectado un ciclo de peso negativo (una trampa de Lecter)!!!!");
                        System.out.println("--> El rastreo se detendrá inmediatamente para evitar un bucle infinito.\n");
                        iteracionActual = totalNodos + 100; // forzamos parada inmediata!!
                        barrera.reset(); 
                        return;
                    }
                    if (cambiosRonda == 0) {
                        iteracionActual = totalNodos + 5; 
                    } else {
                        optimizacionesEnIteracion.set(0); 
                        iteracionActual++;
                    }
                }
            });

            /*
                LANZAMIENTO Y SINCRONIZACIÓN DE HILOS
                -> divide equitativamente las conexiones entre los hilos (manejando el residuo),
                   lanza las unidades de ejecución en paralelo y espera su retorno con join().
            */
            Thread[] hilos = new Thread[numHilos];
            int totalAristas = listaServidor.size();
            int corte = totalAristas / numHilos;
            int residuo = totalAristas % numHilos; 

            int indiceActual = 0;
            for (int i = 0; i < numHilos; i++) {
                int inicio = indiceActual;
                int fin = inicio + corte + (i == numHilos - 1 ? residuo : 0);
                indiceActual = fin;

                hilos[i] = new Thread(new Procesador(i, inicio, fin));
                hilos[i].start(); 
            }

            for (int i = 0; i < numHilos; i++) {
                try {
                    hilos[i].join();
                } catch (InterruptedException e) {
                    System.err.println("Error esperando al hilo " + i);
                }
            }

            long tiempoFin = System.currentTimeMillis();
            long tiempoTotal = tiempoFin - tiempoInicio;
            int latenciaFinal = distancias.get(nodoFinal);

            /*
               RECONSTRUCCIÓN DEL CAMINO ÓPTIMO
                -> si el destino es alcanzable, rastrea la ruta a la inversa desde el nodo final
                   siguiendo el rastro de los predecesores hasta llegar al punto de origen.
            */
            java.util.List<Integer> camino = new ArrayList<>();
            if (latenciaFinal != INF && iteracionActual == totalNodos + 5) { 
                int paso = nodoFinal;
                while (paso != -1) {
                    camino.add(paso);
                    paso = predecesores[paso];
                }
                java.util.Collections.reverse(camino); 
            }

            /*
                REPORTES DE SALIDA
                -> despliega en la consola las métricas de rendimiento con la ruta encontrada
                   y escribe el archivo de evidencia final 'salidaThread.txt'.
            */
            System.out.println("=== C. Informe Final de Captura ===");
            if (iteracionActual > totalNodos + 50) { 
                System.out.println("Ruta Recuperada: Abortado por ciclo negativo.");
                System.out.println("Latencia Total: INF (Bucle Infinito)");
            } else if (latenciaFinal == INF) {
                System.out.println("Ruta Recuperada: No existe una ruta posible hacia el objetivo.");
                System.out.println("Latencia Total: Inalcanzable");
            } else {
                StringBuilder sbRuta = new StringBuilder();
                for (int i = 0; i < camino.size(); i++) {
                    sbRuta.append(camino.get(i));
                    if (i < camino.size() - 1) sbRuta.append("→");
                }
                System.out.println("Ruta Recuperada: " + sbRuta.toString()); 
                System.out.println("Latencia Total: " + latenciaFinal);
            }
            System.out.println("Métricas de Rendimiento: " + tiempoTotal + " milisegundos.");
            System.out.println("========================================\n");

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("salidaThread.txt"))) {
                if (iteracionActual > totalNodos + 50) {
                    pw.println("Ciclo negativo detectado");
                    pw.println("INF");
                } else if (latenciaFinal == INF) {
                    pw.println("No existe ruta");
                    pw.println("INF");
                } else {
                    StringBuilder sbRuta = new StringBuilder();
                    for (int i = 0; i < camino.size(); i++) {
                        sbRuta.append(camino.get(i));
                        if (i < camino.size() - 1) sbRuta.append("→");
                    }
                    pw.println(sbRuta.toString());   
                    pw.println(latenciaFinal);       
                }
                System.out.println("--> [OK] Archivo 'salidaThread.txt' generado de forma exitosa.");
            } catch (IOException e) {
                System.err.println("Error al escribir el archivo de evidencia: " + e.getMessage());
            }

        } else {
            System.out.println("Por favor, pasa el nombre del archivo. Ejemplo: java LAB2_Torres_Salinas red_servidores.txt");
        }
    }
}

/*
    CLASE conexion
    -> almacena los índices numéricos del servidor origen,
     el servidor destino y su latencia.
*/
class conexion {
    int nodo_origen;
    int nodo_destino;
    int peso_latencia;  

    public conexion(int nodo_origen, int nodo_destino, int peso_latencia){
        this.nodo_origen = nodo_origen;
        this.nodo_destino = nodo_destino;
        this.peso_latencia = peso_latencia;
    }
}

/*
    CLASE TRABAJADORA / HILO: Procesador
    -> implementa Runnable. Ejecuta el algoritmo de Bellman-Ford relajando de forma paralela 
       su rango exclusivo de aristas y sincronizando con sus pares mediante la barrera.
*/
class Procesador implements Runnable {
    private int PID;
    private int inicio;
    private int fin;

    public Procesador(int PID, int inicio, int fin) {
        this.PID = PID;
        this.inicio = inicio;
        this.fin = fin;
    }

    @Override
    public void run() {
        System.out.println("-> [Hilo " + PID + "] Iniciado. Rango asignado: " + inicio + " a " + (fin - 1));

        while (LAB2_Torres_Salinas.iteracionActual < LAB2_Torres_Salinas.totalNodos) {
            
            for (int i = inicio; i < fin; i++) {
                conexion conex = LAB2_Torres_Salinas.listaServidor.get(i);
                int u = conex.nodo_origen;
                int v = conex.nodo_destino;
                int w = conex.peso_latencia;

                int distU = LAB2_Torres_Salinas.distancias.get(u);
                
                if (distU != LAB2_Torres_Salinas.INF) {
                    int nuevaDist = distU + w;
                    
                    while (true) {
                        int distVActual = LAB2_Torres_Salinas.distancias.get(v);
                        
                        if (nuevaDist < distVActual) {
                            if (LAB2_Torres_Salinas.distancias.compareAndSet(v, distVActual, nuevaDist)) {
                                synchronized (LAB2_Torres_Salinas.predecesores) {
                                    LAB2_Torres_Salinas.predecesores[v] = u; 
                                }
                                LAB2_Torres_Salinas.optimizacionesEnIteracion.incrementAndGet(); 
                                break; 
                            }
                        } else {
                            break; 
                        }
                    }
                }
            }

            try {
                LAB2_Torres_Salinas.barrera.await();
            } catch (Exception e) {
                break;
            }
        }
    }
}