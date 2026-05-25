import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    // Declaracion datos 
    static int numHilos;
    static int totalNodos;
    static int nodoInicial;
    static int nodoFinal;

    static ArrayList<conexion> listaServidor = new ArrayList<>();
    static AtomicIntegerArray distancias;

    public static void leer_txt(String ruta){
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            br.readLine();  
            numHilos = Integer.parseInt(br.readLine().trim());
            totalNodos = Integer.parseInt(br.readLine().trim());             
            nodoInicial = Integer.parseInt(br.readLine().trim());
            nodoFinal = Integer.parseInt(br.readLine().trim());

            // array para las distancias
            distancias = new AtomicIntegerArray(totalNodos);  

            // todas las distancias parten en 0 menos el nodo inicial
            int inf = Integer.MAX_VALUE / 2;
            for(int i = 0; i < totalNodos; i++){
                distancias.set(i, inf); // distancias.set(nodo, valor)
            }
            distancias.set(nodoInicial, 0);

        
            String line; 
            while ((line = br.readLine()) != null){
                line = line.trim();
                if (line.isEmpty()) continue;   

                String[] partes = line.split(" ");
                
                int u = Integer.parseInt(partes[0]); // servidor de origen
                int v = Integer.parseInt(partes[1]); // servidor de destino
                int w = Integer.parseInt(partes[2]); // costo

                listaServidor.add(new conexion(u, v, w)); // 
            }

            System.out.println("--> [OK] Carga completa. Conexiones en memoria: " + listaServidor.size());

        } catch (IOException e) {
            System.err.println("error al leer el archivo: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            leer_txt(args[0]);
        } else {
            System.out.println("pasa nombre del archivo");
        }
    }
}


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
    }
}