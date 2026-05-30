# Variables
CXX      = g++
CXXFLAGS = -Wall -Wextra -std=c++11 -O2

# Nombres
TARGET_ALPHA = LAB2_Torres_Salinas
JAVA_FILE    = LAB2_Torres_Salinas.java

# Compilar ambos motores
all: $(TARGET_ALPHA) java

# Motor Alpha (C++)
$(TARGET_ALPHA): LAB2_Torres_Salinas.cpp
	$(CXX) $(CXXFLAGS) -o $(TARGET_ALPHA) LAB2_Torres_Salinas.cpp

# Motor Beta (Java)
java:
	javac $(JAVA_FILE)

# Ejecutar Motor Alpha
run-alpha:
	./$(TARGET_ALPHA) $(FILE)

# Ejecutar Motor Beta
run-java:
	java LAB2_Torres_Salinas $(FILE)

# Limpiar todo
clean:
	rm -f $(TARGET_ALPHA) *.class salidaFork.txt salidaThread.txt
