# Laboratorio 2 - SISTEMAS OPERATIVOS
## Integrantes
* **Verónica Torres  ROL: 202373503-5** 
* **Victor Salinas  ROL: 202204580-9** 
---

## Descripción General

Este laboratorio implementa el algoritmo de **Bellman-Ford paralelo** en dos versiones
distintas para encontrar la ruta de menor latencia en una red de servidores dirigida:

- **Motor Alpha (C++):** Paralelismo mediante procesos independientes con `fork()` y comunicación vía `pipes`.
- **Motor Beta (Java):** Paralelismo mediante hilos (`threads`) con memoria compartida y sincronización atómica.

---
## MOTOR ALPHA (C++)

### Descripción

Implementación multiproceso del algoritmo de Bellman-Ford en C++. Las conexiones de la red se dividen mediante segmentación 
estática entre $N$ procesos hijos creados con `fork()`. Debido al aislamiento de memoria impuesto por el sistema operativo, 
los procesos interactúan con un proceso padre coordinador mediante paso de mensajes a través de tuberías, sincronizando el 
estado global de las latencias iteración tras iteración.

### Arquitectura Concurrente e IPC

| Mecanismo | Herramienta C/C++ | Propósito |
|---|---|---|
| Creación de procesos | `fork()` | Despliegue de unidades de trabajo con aislamiento de memoria (Copy-On-Write). |
| Intercomunicación | `pipe()` | Canales IPC para enviar el vector de distancias y recibir conteos de optimización. |
| Sincronización iterativa | Lectura bloqueante (`read`) | Actúa como barrera natural; los hijos pausan su ejecución hasta recibir datos del padre. |
| Control de recursos | `waitpid()` / `close()` | Recolección de estados de salida (prevención de procesos zombie) y cierre seguro de descriptores de archivo. |
---



## MOTOR BETA (Java)

### Descripción

Implementación multihilo del algoritmo de Bellman-Ford en Java. El grafo de servidores
se divide equitativamente entre N hilos, cada uno relaja su porción de aristas en
paralelo. Los hilos se sincronizan al final de cada iteración mediante una barrera
cíclica antes de avanzar a la siguiente ronda.

### Arquitectura Concurrente

| Mecanismo | Clase Java | Propósito |
|---|---|---|
| Distancias compartidas | `AtomicIntegerArray` | Actualizaciones lock-free con `compareAndSet()` |
| Sincronización por ronda | `CyclicBarrier` | Garantiza que todos los hilos terminen antes de avanzar |
| Conteo de mejoras | `AtomicInteger` | Detecta convergencia y parada anticipada |
| Predecesores | `int[]` + `synchronized` | Reconstrucción coherente del camino óptimo |
---

### Supuestos

- El archivo de entrada tiene exactamente el formato especificado (ver sección Formato de Entrada).
- La primera línea del archivo corresponde al número de procesos para C, y la segunda al número de hilos para Java.
- Los nodos están numerados desde 0.
- El grafo puede contener aristas de peso negativo, pero se detecta y reporta cualquier ciclo negativo.
- Si el nodo destino es inalcanzable, se reporta latencia infinita.
- Se asume que el número de hilos es al menos 1.

---
## Instrucciones de Ejecución (Ubuntu/Linux)

### Requisitos previos
- Java JDK 11 o superior
- g++ con soporte C++11
- `make` instalado (`sudo apt install make`)

### Compilar y ejecutar con Make

```bash
# Compilar ambos motores
make

# Ejecutar Motor Alpha (C++)
make run-alpha FILE=red_servidores.txt

# Ejecutar Motor Beta (Java)
make run-java FILE=red_servidores.txt

# Limpiar archivos compilados
make clean
```

### Compilación manual (sin Make)

```bash
# Motor Alpha (C++)
g++ -Wall -Wextra -std=c++11 -O2 -o LAB2_Torres_Salinas LAB2_Torres_Salinas.cpp
./LAB2_Torres_Salinas red_servidores.txt

# Motor Beta (Java)
javac LAB2_Torres_Salinas.java
java LAB2_Torres_Salinas red_servidores.txt
```


