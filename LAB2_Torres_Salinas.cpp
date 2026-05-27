#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <unistd.h>
#include <sys/wait.h>
#include <chrono>
#include <climits>
#include <algorithm>
#include <csignal>

using namespace std;

// Constante para representar el infinito
const int INF = INT_MAX / 2;

struct Arista {
    int u, v, peso;
};

struct Actualizacion {
    int v, nueva_distancia, padre;
};

// Estructura segura para manejar los File Descriptors en C++
struct PipeBidireccional {
    int bajada[2];
    int subida[2];
};

//Funciones de Seguridad para evitar Deadlocks por lecturas parciales 
bool escribir_todo(int fd, const void* buffer, size_t size) {
    const char* ptr = static_cast<const char*>(buffer);
    size_t bytes_escritos = 0;
    while (bytes_escritos < size) {
        ssize_t result = write(fd, ptr + bytes_escritos, size - bytes_escritos);
        if (result <= 0) return false;
        bytes_escritos += result;
    }
    return true;
}

bool leer_todo(int fd, void* buffer, size_t size) {
    char* ptr = static_cast<char*>(buffer);
    size_t bytes_leidos = 0;
    while (bytes_leidos < size) {
        ssize_t result = read(fd, ptr + bytes_leidos, size - bytes_leidos);
        if (result <= 0) return false;
        bytes_leidos += result;
    }
    return true;
}

int main(int argc, char* argv[]) {
    // Ignorar SIGPIPE para evitar que el Padre muera si un Hijo se desconecta antes
    signal(SIGPIPE, SIG_IGN);

    auto start_time = chrono::high_resolution_clock::now();

    if (argc != 2) {
        cerr << "Uso: " << argv[0] << " <archivo_red.txt>" << endl;
        return 1;
    }

    ifstream archivo(argv[1]);
    if (!archivo.is_open()) {
        cerr << "Error al abrir el archivo." << endl;
        return 1;
    }

    int num_procesos, num_hilos, V, nodo_inicio, nodo_final;
    archivo >> num_procesos >> num_hilos >> V >> nodo_inicio >> nodo_final;

    vector<Arista> aristas;
    int u, v, w;
    while (archivo >> u >> v >> w) {
        aristas.push_back({u, v, w});
    }
    archivo.close();

    int E = aristas.size();

    // A. Inicio del Rastreo
    cout << "A. Inicio del Rastreo" << endl;
    cout << "Identificación del proceso padre: " << getpid() << endl;
    cout << "Carga del mapa: " << V << " servidores y " << E << " conexiones cargadas." << endl;

    // 1. crear todos los pipes primero
    vector<PipeBidireccional> pipes(num_procesos);
    vector<pid_t> pids(num_procesos);

    for (int i = 0; i < num_procesos; ++i) {
        if (pipe(pipes[i].bajada) == -1 || pipe(pipes[i].subida) == -1) {
            cerr << "Error creando pipes." << endl;
            return 1;
        }
    }

    // 2. crear los forks
    for (int i = 0; i < num_procesos; ++i) {
        pid_t pid = fork();
        if (pid < 0) {
            cerr << "Error en fork." << endl;
            return 1;
        }

        if (pid == 0) {
            // codigo del hijo
            // cierra todos los FDs que no va a usar este hijo
            for (int j = 0; j < num_procesos; ++j) {
                if (j == i) {
                    // de su propio pipe, el hijo solo lee de bajada y escribe de subida
                    close(pipes[j].bajada[1]); 
                    close(pipes[j].subida[0]); 
                } else {
                    // de los pipes de sus hermanos, cierra todo
                    close(pipes[j].bajada[0]);
                    close(pipes[j].bajada[1]);
                    close(pipes[j].subida[0]);
                    close(pipes[j].subida[1]);
                }
            }

            int chunk = E / num_procesos;
            int inicio = i * chunk;
            int fin = (i == num_procesos - 1) ? E : (i + 1) * chunk;

            vector<int> dist_local(V);
            
            for (int iter = 1; iter <= V; ++iter) {
                if (!leer_todo(pipes[i].bajada[0], dist_local.data(), V * sizeof(int))) break;

                vector<Actualizacion> act_locales;

                for (int j = inicio; j < fin; ++j) {
                    int origen = aristas[j].u;
                    int destino = aristas[j].v;
                    int peso = aristas[j].peso;

                    if (dist_local[origen] != INF && dist_local[origen] + peso < dist_local[destino]) {
                        act_locales.push_back({destino, dist_local[origen] + peso, origen});
                        dist_local[destino] = dist_local[origen] + peso; 
                    }
                }

                int num_acts = act_locales.size();
                if (!escribir_todo(pipes[i].subida[1], &num_acts, sizeof(int))) break;
                
                if (num_acts > 0) {
                    if (!escribir_todo(pipes[i].subida[1], act_locales.data(), num_acts * sizeof(Actualizacion))) break;
                }
            }

            close(pipes[i].bajada[0]);
            close(pipes[i].subida[1]);
            exit(0); 
        } else {
            // codigo del padre
            pids[i] = pid;
        }
    }

    // 3. El padre cierra los extremos que no usa después de crear a todos
    for (int i = 0; i < num_procesos; ++i) {
        close(pipes[i].bajada[0]); // Padre no lee de bajada
        close(pipes[i].subida[1]); // Padre no escribe de subida
    }

    cout << "Creación de equipos: " << num_procesos << " procesos hijos creados exitosamente mediante fork() y pipes abiertos." << endl;
    cout << "\nB. Progreso por iteraciones" << endl;

    vector<int> distancias(V, INF);
    vector<int> padres(V, -1);
    distancias[nodo_inicio] = 0;

    bool ciclo_negativo = false;

    for (int iter = 1; iter <= V; ++iter) {
        if (iter < V) {
            cout << "Iteración " << iter << ": Los procesos hijos están analizando las latencias..." << endl;
        }

        // 1. Enviar datos asegurando transferencia completa
        for (int i = 0; i < num_procesos; ++i) {
            escribir_todo(pipes[i].bajada[1], distancias.data(), V * sizeof(int));
        }

        bool hubo_cambios = false;

        // 2. Recibir datos de manera síncrona
        for (int i = 0; i < num_procesos; ++i) {
            int num_acts = 0;
            if (!leer_todo(pipes[i].subida[0], &num_acts, sizeof(int))) continue;

            if (num_acts > 0) {
                vector<Actualizacion> recibidos(num_acts);
                if (leer_todo(pipes[i].subida[0], recibidos.data(), num_acts * sizeof(Actualizacion))) {
                    for (const auto& act : recibidos) {
                        if (act.nueva_distancia < distancias[act.v]) {
                            distancias[act.v] = act.nueva_distancia;
                            padres[act.v] = act.padre;
                            hubo_cambios = true;
                        }
                    }
                }
            }
            if (iter < V) {
                cout << "Datos recibidos del Hijo [" << pids[i] << "]: " << num_acts << " rutas actualizadas." << endl;
            }
        }

        if (iter == V && hubo_cambios) {
            ciclo_negativo = true;
            cout << "¡Alerta! Se ha detectado un ciclo de peso negativo (Trampa de Lecter). Abortando rastreo." << endl;
            break;
        }

        if (!hubo_cambios && iter < V) {
            break; 
        }
    }

    for (int i = 0; i < num_procesos; ++i) {
        close(pipes[i].bajada[1]);
        close(pipes[i].subida[0]);
        waitpid(pids[i], NULL, 0);
    }

    auto end_time = chrono::high_resolution_clock::now();
    auto duracion = chrono::duration_cast<chrono::milliseconds>(end_time - start_time).count();

    // C. Informe Final
    cout << "\nC. Informe final de captura" << endl;
    if (ciclo_negativo) {
        cout << "Rastreo fallido debido a trampas en la red." << endl;
    } else if (distancias[nodo_final] == INF) {
        cout << "No existe ruta alcanzable hacia el objetivo." << endl;
    } else {
        vector<int> ruta;
        for (int at = nodo_final; at != -1; at = padres[at]) {
            ruta.push_back(at);
        }
        reverse(ruta.begin(), ruta.end());

        cout << "Ruta Recuperada: ";
        for (size_t i = 0; i < ruta.size(); ++i) {
            cout << ruta[i] << (i == ruta.size() - 1 ? "" : " -> ");
        }
        cout << "\nLatencia Total: " << distancias[nodo_final] << endl;
        cout << "Métricas de Rendimiento: " << duracion << " ms" << endl;

        ofstream salida("salida-Fork.txt");
        if (salida.is_open()) {
            for (size_t i = 0; i < ruta.size(); ++i) {
                salida << ruta[i] << (i == ruta.size() - 1 ? "" : " -> ");
            }
            salida << "\n" << distancias[nodo_final] << "\n";
            salida.close();
        }
    }

    return 0;
}