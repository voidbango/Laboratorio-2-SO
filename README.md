# Laboratorio 2 - SISTEMAS OPERATIVOS
## Integrantes
* **Verónica Torres  ROL: 202372503-5** 
* **Victor Salinas  ROL: 202204580-9** 
---

## Descripción General

Este laboratorio implementa el algoritmo de **Bellman-Ford paralelo** en dos versiones
distintas para encontrar la ruta de menor latencia en una red de servidores dirigida:

- **Motor Alpha (C++):** Paralelismo mediante procesos independientes con `fork()` y comunicación vía `pipes`.
- **Motor Beta (Java):** Paralelismo mediante hilos (`threads`) con memoria compartida y sincronización atómica.

---
## MOTOR ALPHA (C++)
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
## Instrucciones de Ejecución (Ubuntu)

### 1. Preparar el mapa de entrada
Tener archivo de configuración (por ejemplo, `red_servidores.txt`) en la misma carpeta que el código fuente.

### 2. Compilar el código
Usa el compilador de Java para generar los bytes lógicos:
```bash
javac LAB2_Torres_Salinas.java
